package com.example.data.repository

import android.content.Context
import com.example.data.local.AuditLogEntity
import com.example.data.local.ComplaintEntity
import com.example.data.local.PaymentOrderEntity
import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.SujoyPayDao
import com.example.data.local.SujoyPayDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.network.GatewayClient
import com.example.security.CryptoVault
import com.example.security.OtpRateState
import com.example.security.RateLimiterAndFraudEngine
import com.example.security.SandboxServerVerifier
import com.example.security.SessionPreferences
import com.example.security.SessionState
import com.example.util.Formatters
import com.example.util.QrCodeUtils
import com.example.util.UpiAppChannel
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed interface RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>
    data class Error(val message: String) : RepoResult<Nothing>
}

data class PendingOtpChallenge(
    val mobileNumber: String,
    val fullName: String?,
    val email: String?,
    val isNewUser: Boolean,
    val sandboxOtpCode: String,
    val expiresAtMillis: Long
)

data class InitiatedAddMoneyOrder(
    val order: PaymentOrderEntity,
    val channel: UpiAppChannel,
    val gatewayKeyId: String,
    val merchantVpa: String
)

class SujoyPayRepository(
    private val dao: SujoyPayDao,
    private val sessionPrefs: SessionPreferences,
    private val gatewayClient: GatewayClient
) {
    constructor(context: Context) : this(
        dao = SujoyPayDatabase.getInstance(context).dao(),
        sessionPrefs = SessionPreferences(context.applicationContext),
        gatewayClient = GatewayClient()
    )

    private val activeOtpChallenges = mutableMapOf<String, PendingOtpChallenge>()

    val sessionFlow: Flow<SessionState> = sessionPrefs.sessionFlow
    val paymentSettingsFlow: Flow<PaymentSettingsEntity?> = dao.observePaymentSettings()

    suspend fun initializeDatabase() {
        SujoyPayDatabase.ensureSeeded(dao)
    }

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)
    fun observeUserTransactions(userId: String): Flow<List<TransactionEntity>> = dao.observeUserTransactions(userId)
    fun observeUserComplaints(userId: String): Flow<List<ComplaintEntity>> = dao.observeUserComplaints(userId)

    // Admin streams
    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()
    fun observeAllTransactions(): Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    fun observeFlaggedTransactions(): Flow<List<TransactionEntity>> = dao.observeFlaggedTransactions()
    fun observeAllComplaints(): Flow<List<ComplaintEntity>> = dao.observeAllComplaints()
    fun observeAuditLogs(): Flow<List<AuditLogEntity>> = dao.observeAuditLogs()

    suspend fun requestOtp(
        mobileRaw: String,
        fullName: String?,
        email: String?,
        isSignUp: Boolean
    ): RepoResult<PendingOtpChallenge> {
        initializeDatabase()
        val mobile = Formatters.normalizeMobile(mobileRaw)
        if (!Formatters.isValidIndianMobile(mobile)) {
            return RepoResult.Error("Enter a valid 10-digit Indian mobile number starting with 6-9.")
        }
        val rate = RateLimiterAndFraudEngine.checkOtpRateLimit(mobile)
        if (rate.isLocked) {
            return RepoResult.Error("Too many failed attempts. Try again in ${rate.remainingLockSeconds}s.")
        }
        val existingUser = dao.getUserByMobile(mobile)
        if (isSignUp && existingUser != null) {
            return RepoResult.Error("Mobile +91 $mobile is already registered. Please Login instead.")
        }
        if (!isSignUp && existingUser == null) {
            return RepoResult.Error("No SUJOY PAY account found for +91 $mobile. Please Sign Up.")
        }
        if (isSignUp && fullName.isNullOrBlank()) {
            return RepoResult.Error("Full name is required for KYC onboarding.")
        }

        val otp = (100000..999999).random().toString()
        val challenge = PendingOtpChallenge(
            mobileNumber = mobile,
            fullName = fullName?.trim(),
            email = email?.trim()?.ifBlank { "$mobile@sujoypay.in" },
            isNewUser = existingUser == null,
            sandboxOtpCode = otp,
            expiresAtMillis = System.currentTimeMillis() + 5 * 60_000L
        )
        activeOtpChallenges[mobile] = challenge
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = existingUser?.id ?: "NEW_$mobile",
                actorRole = existingUser?.role ?: "USER",
                action = "OTP_REQUESTED",
                targetId = Formatters.maskMobile(mobile),
                details = "Sandbox OTP challenge issued (5 min TTL).",
                severity = "INFO"
            )
        )
        return RepoResult.Success(challenge)
    }

    suspend fun verifyOtpAndLogin(
        mobileRaw: String,
        enteredOtp: String,
        mpinForNewUser: String
    ): RepoResult<UserEntity> {
        initializeDatabase()
        val mobile = Formatters.normalizeMobile(mobileRaw)
        val rate = RateLimiterAndFraudEngine.checkOtpRateLimit(mobile)
        if (rate.isLocked) {
            return RepoResult.Error("Account OTP temporarily locked for ${rate.remainingLockSeconds}s.")
        }
        val challenge = activeOtpChallenges[mobile]
            ?: return RepoResult.Error("OTP session expired. Request a new OTP.")
        if (System.currentTimeMillis() > challenge.expiresAtMillis) {
            activeOtpChallenges.remove(mobile)
            return RepoResult.Error("OTP expired. Request a new code.")
        }
        if (enteredOtp.trim() != challenge.sandboxOtpCode) {
            val updatedRate: OtpRateState = RateLimiterAndFraudEngine.recordOtpFailure(mobile)
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = mobile,
                    actorRole = "UNKNOWN",
                    action = "OTP_FAILED",
                    targetId = Formatters.maskMobile(mobile),
                    details = "Invalid OTP attempt. Attempts left: ${updatedRate.attemptsLeft}",
                    severity = if (updatedRate.isLocked) "CRITICAL" else "WARN"
                )
            )
            return if (updatedRate.isLocked) {
                RepoResult.Error("Maximum OTP attempts exceeded. Locked for ${updatedRate.remainingLockSeconds}s.")
            } else {
                RepoResult.Error("Invalid OTP. ${updatedRate.attemptsLeft} attempts remaining.")
            }
        }

        RateLimiterAndFraudEngine.clearOtpAttempts(mobile)
        activeOtpChallenges.remove(mobile)

        val user = if (challenge.isNewUser) {
            val cleanPin = mpinForNewUser.trim()
            if (!cleanPin.matches(Regex("^\\d{4}$"))) {
                return RepoResult.Error("Please set a 4-digit security MPIN.")
            }
            val newId = "usr_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val created = UserEntity(
                id = newId,
                fullName = challenge.fullName ?: "SUJOY User",
                mobileNumber = mobile,
                email = challenge.email ?: "$mobile@sujoypay.in",
                upiId = "$mobile@sujoypay",
                kycStatus = "MIN_KYC",
                role = "USER",
                isFrozen = false,
                balancePaise = 0L, // Zero initial balance; funds added only via verified gateway flow!
                dailySpentPaise = 0L,
                mpinHash = CryptoVault.hashPin(cleanPin, newId),
                encryptedBankInfo = ""
            )
            dao.upsertUser(created)
            created
        } else {
            dao.getUserByMobile(mobile)
                ?: return RepoResult.Error("User account record missing.")
        }

        sessionPrefs.saveSessionUserId(user.id)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = user.id,
                actorRole = user.role,
                action = "AUTH_LOGIN_SUCCESS",
                targetId = user.upiId,
                details = "Mobile OTP verified and encrypted session established.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(user)
    }

    suspend fun loginDemoAccount(isAdmin: Boolean): RepoResult<UserEntity> {
        initializeDatabase()
        val mobile = if (isAdmin) "9000000001" else "9876543210"
        val user = dao.getUserByMobile(mobile)
            ?: return RepoResult.Error("Sandbox demo account not found.")
        sessionPrefs.saveSessionUserId(user.id)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = user.id,
                actorRole = user.role,
                action = "SANDBOX_DEMO_LOGIN",
                targetId = user.upiId,
                details = "Authenticated via Sandbox ${user.role} profile.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(user)
    }

    suspend fun logout(userId: String?) {
        if (!userId.isNullOrBlank()) {
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = userId,
                    actorRole = "USER",
                    action = "AUTH_LOGOUT",
                    targetId = userId,
                    details = "Encrypted session cleared cleanly.",
                    severity = "INFO"
                )
            )
        }
        sessionPrefs.clearSession()
    }

    suspend fun setHideBalance(hidden: Boolean) = sessionPrefs.setHideBalance(hidden)
    suspend fun setDarkMode(dark: Boolean?) = sessionPrefs.setDarkMode(dark)

    // --- Verified Add Money (Server-First, Never Client-Side Credit) ---

    suspend fun initiateAddMoneyOrder(
        userId: String,
        amountPaise: Long,
        channel: UpiAppChannel
    ): RepoResult<InitiatedAddMoneyOrder> {
        val user = dao.getUserById(userId) ?: return RepoResult.Error("Session expired.")
        val settings = dao.getPaymentSettings() ?: PaymentSettingsEntity()

        val channelAllowed = when (channel) {
            UpiAppChannel.GOOGLE_PAY -> settings.gpayUpiEnabled
            UpiAppChannel.PHONEPE -> settings.phonePeUpiEnabled
            UpiAppChannel.OTHER_UPI -> settings.otherUpiEnabled
        }
        if (!channelAllowed) {
            return RepoResult.Error("${channel.displayName} is currently disabled by Admin Payment Settings.")
        }

        val recentCount = dao.countRecentTransactions(userId, System.currentTimeMillis() - 60_000L)
        val fraud = RateLimiterAndFraudEngine.assessTransaction(
            user = user,
            amountPaise = amountPaise,
            isCredit = true,
            recentTxnCountLastMinute = recentCount,
            settings = settings
        )
        if (!fraud.allowed) {
            return RepoResult.Error(fraud.blockReason ?: "Transaction blocked by risk policy.")
        }

        val methodCode = when (channel) {
            UpiAppChannel.GOOGLE_PAY -> "GPAY_UPI"
            UpiAppChannel.PHONEPE -> "PHONEPE_UPI"
            UpiAppChannel.OTHER_UPI -> "OTHER_UPI"
        }

        val orderResp = gatewayClient.createOrder(userId, amountPaise, methodCode)
        val upiUri = QrCodeUtils.buildUpiUri(
            vpa = orderResp.merchantVpa,
            payeeName = "SUJOY PAY",
            amountPaise = amountPaise,
            transactionRef = orderResp.orderId,
            note = "Wallet Topup ${orderResp.orderId}"
        )

        val order = PaymentOrderEntity(
            orderId = orderResp.orderId,
            userId = userId,
            amountPaise = amountPaise,
            gatewayMethod = methodCode,
            status = "CREATED",
            serverNonce = orderResp.serverNonce,
            hmacSignature = orderResp.challengeSignature,
            upiIntentUri = upiUri
        )
        val pendingTxn = TransactionEntity(
            id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = userId,
            utrNumber = "PENDING",
            orderId = order.orderId,
            type = "ADD_MONEY",
            direction = "CREDIT",
            status = "PENDING",
            amountPaise = amountPaise,
            counterpartyName = channel.displayName,
            counterpartyHandle = orderResp.merchantVpa,
            gatewayMethod = methodCode,
            gatewaySignature = "",
            riskScore = fraud.riskScore,
            isFlagged = fraud.isFlagged,
            note = "Awaiting gateway callback & HMAC verification"
        )
        dao.upsertOrder(order)
        dao.upsertTransaction(pendingTxn)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = user.role,
                action = "ADD_MONEY_ORDER_CREATED",
                targetId = order.orderId,
                details = "Created order for ${Formatters.formatInr(amountPaise)} via $methodCode.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(
            InitiatedAddMoneyOrder(
                order = order,
                channel = channel,
                gatewayKeyId = orderResp.gatewayKeyId,
                merchantVpa = orderResp.merchantVpa
            )
        )
    }

    /**
     * Completes an Add Money order ONLY after verifying the payment gateway HMAC-SHA256 callback signature.
     * Prevents replay/double-credit via idempotency check on order.status.
     */
    suspend fun verifyAndSettleAddMoney(
        orderId: String,
        simulateAuthorized: Boolean
    ): RepoResult<TransactionEntity> {
        val order = dao.getOrderById(orderId)
            ?: return RepoResult.Error("Gateway order not found.")
        val existingTxn = dao.getTransactionByOrderId(orderId)
            ?: return RepoResult.Error("Pending transaction record missing.")

        if (order.status == "VERIFIED") {
            return RepoResult.Success(existingTxn) // Idempotent guard against duplicate settlement!
        }

        if (!simulateAuthorized) {
            val failedOrder = order.copy(status = "FAILED", verifiedAt = System.currentTimeMillis())
            val failedTxn = existingTxn.copy(
                status = "FAILED",
                utrNumber = "CANCELLED",
                note = "UPI payment cancelled or declined at gateway. Wallet balance unchanged."
            )
            dao.markOrderFailed(
                order = failedOrder,
                updatedTxn = failedTxn,
                auditLog = AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = order.userId,
                    actorRole = "USER",
                    action = "ADD_MONEY_FAILED",
                    targetId = orderId,
                    details = "Gateway reported failed/cancelled UPI authorization.",
                    severity = "WARN"
                )
            )
            return RepoResult.Error("Payment was cancelled or declined. No funds were added.")
        }

        val gatewayPaymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val callbackSignature = SandboxServerVerifier.signGatewayCallback(
            orderId = order.orderId,
            gatewayPaymentId = gatewayPaymentId,
            amountPaise = order.amountPaise,
            nonce = order.serverNonce,
            keyId = gatewayClient.configuredKeyId
        )

        val verifyResp = gatewayClient.verifyPayment(
            orderId = order.orderId,
            gatewayPaymentId = gatewayPaymentId,
            signature = callbackSignature,
            amountPaise = order.amountPaise,
            serverNonce = order.serverNonce
        )

        if (!verifyResp.verified) {
            val failedOrder = order.copy(status = "FAILED", verifiedAt = System.currentTimeMillis())
            val failedTxn = existingTxn.copy(
                status = "FAILED",
                utrNumber = "SIG_ERR",
                note = verifyResp.message
            )
            dao.markOrderFailed(
                order = failedOrder,
                updatedTxn = failedTxn,
                auditLog = AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = order.userId,
                    actorRole = "GATEWAY",
                    action = "ADD_MONEY_SIG_REJECTED",
                    targetId = orderId,
                    details = verifyResp.message,
                    severity = "CRITICAL"
                )
            )
            return RepoResult.Error(verifyResp.message)
        }

        val verifiedOrder = order.copy(status = "VERIFIED", verifiedAt = System.currentTimeMillis())
        val settledTxn = existingTxn.copy(
            status = "SUCCESS",
            utrNumber = verifyResp.utrNumber,
            gatewaySignature = callbackSignature,
            note = "Verified via Gateway (${verifyResp.message})"
        )
        dao.settleVerifiedAddMoney(
            order = verifiedOrder,
            updatedTxn = settledTxn,
            creditUserId = order.userId,
            creditAmountPaise = order.amountPaise,
            auditLog = AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = order.userId,
                actorRole = "GATEWAY",
                action = "ADD_MONEY_VERIFIED_CREDIT",
                targetId = orderId,
                details = "Verified ${Formatters.formatInr(order.amountPaise)} (UTR: ${verifyResp.utrNumber}).",
                severity = "INFO"
            )
        )
        return RepoResult.Success(settledTxn)
    }

    // --- Send Money (UPI / Mobile) ---

    suspend fun sendMoney(
        senderId: String,
        recipientHandleRaw: String,
        recipientDisplayName: String,
        amountPaise: Long,
        note: String,
        mpin: String
    ): RepoResult<TransactionEntity> {
        val sender = dao.getUserById(senderId) ?: return RepoResult.Error("Session expired.")
        if (!CryptoVault.verifyPin(mpin.trim(), sender.id, sender.mpinHash)) {
            return RepoResult.Error("Incorrect 4-digit security MPIN.")
        }

        val handle = recipientHandleRaw.trim()
        val isMobile = Formatters.isValidIndianMobile(handle)
        val isUpi = Formatters.isValidUpiId(handle)
        if (!isMobile && !isUpi) {
            return RepoResult.Error("Enter a valid UPI ID (e.g. name@okicici) or 10-digit mobile number.")
        }

        val normalizedHandle = if (isMobile) "${Formatters.normalizeMobile(handle)}@sujoypay" else handle
        if (normalizedHandle.equals(sender.upiId, ignoreCase = true)) {
            return RepoResult.Error("Cannot send money to your own SUJOY PAY UPI ID.")
        }

        val settings = dao.getPaymentSettings() ?: PaymentSettingsEntity()
        val recentCount = dao.countRecentTransactions(senderId, System.currentTimeMillis() - 60_000L)
        val fraud = RateLimiterAndFraudEngine.assessTransaction(
            user = sender,
            amountPaise = amountPaise,
            isCredit = false,
            recentTxnCountLastMinute = recentCount,
            settings = settings
        )
        if (!fraud.allowed) {
            return RepoResult.Error(fraud.blockReason ?: "Transaction declined by fraud policy.")
        }

        val internalRecipient = dao.getUserByUpiId(normalizedHandle)
            ?: if (isMobile) dao.getUserByMobile(Formatters.normalizeMobile(handle)) else null

        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val utr = "UTR${(100000000000L..999999999999L).random()}"
        val sig = SandboxServerVerifier.signOrderChallenge(orderId, amountPaise, utr, gatewayClient.configuredKeyId)
        val displayCounterparty = recipientDisplayName.ifBlank {
            internalRecipient?.fullName ?: normalizedHandle.substringBefore("@")
        }

        val senderTxn = TransactionEntity(
            id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = senderId,
            utrNumber = utr,
            orderId = orderId,
            type = "SEND_MONEY",
            direction = "DEBIT",
            status = "SUCCESS",
            amountPaise = amountPaise,
            counterpartyName = displayCounterparty,
            counterpartyHandle = normalizedHandle,
            gatewayMethod = if (internalRecipient != null) "SUJOY_WALLET" else "OTHER_UPI",
            gatewaySignature = sig,
            riskScore = fraud.riskScore,
            isFlagged = fraud.isFlagged,
            note = note.ifBlank { "UPI Payment" }
        )

        val recipientTxn = internalRecipient?.let { rec ->
            TransactionEntity(
                id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                userId = rec.id,
                utrNumber = utr,
                orderId = orderId,
                type = "RECEIVE_MONEY",
                direction = "CREDIT",
                status = "SUCCESS",
                amountPaise = amountPaise,
                counterpartyName = sender.fullName,
                counterpartyHandle = sender.upiId,
                gatewayMethod = "SUJOY_WALLET",
                gatewaySignature = sig,
                riskScore = 5,
                isFlagged = false,
                note = note.ifBlank { "Received via SUJOY PAY" }
            )
        }

        val cashbackPaise = if (settings.cashbackEnabled && settings.cashbackPercent > 0) {
            (amountPaise * settings.cashbackPercent) / 100L
        } else 0L

        val cashbackTxn = if (cashbackPaise > 0L) {
            TransactionEntity(
                id = "txn_cb_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                userId = senderId,
                utrNumber = "CB$utr",
                orderId = orderId,
                type = "CASHBACK",
                direction = "CREDIT",
                status = "SUCCESS",
                amountPaise = cashbackPaise,
                counterpartyName = "${settings.cashbackPercent}% Instant Cashback",
                counterpartyHandle = "Reward • $displayCounterparty",
                gatewayMethod = "SUJOY_WALLET",
                gatewaySignature = sig,
                riskScore = 0,
                isFlagged = false,
                note = "${settings.cashbackPercent}% cashback on ${Formatters.formatInr(amountPaise)} payment"
            )
        } else null

        dao.executeAtomicDebitAndOptionalCredit(
            senderId = senderId,
            debitPaise = amountPaise,
            recipientId = internalRecipient?.id,
            creditPaise = amountPaise,
            senderTxn = senderTxn,
            recipientTxn = recipientTxn,
            cashbackPaise = cashbackPaise,
            cashbackTxn = cashbackTxn,
            auditLog = AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = senderId,
                actorRole = sender.role,
                action = if (fraud.isFlagged) "HIGH_RISK_TRANSFER" else "UPI_SEND_SUCCESS",
                targetId = senderTxn.id,
                details = "Sent ${Formatters.formatInr(amountPaise)} to $normalizedHandle (Cashback: ${Formatters.formatInr(cashbackPaise)}, Risk: ${fraud.riskScore}).",
                severity = if (fraud.isFlagged) "WARN" else "INFO"
            )
        )
        return RepoResult.Success(senderTxn)
    }

    // --- Withdraw Money ---

    suspend fun withdrawMoney(
        userId: String,
        accountNumber: String,
        ifsc: String,
        accountHolder: String,
        amountPaise: Long,
        mpin: String
    ): RepoResult<TransactionEntity> {
        val user = dao.getUserById(userId) ?: return RepoResult.Error("Session expired.")
        if (!CryptoVault.verifyPin(mpin.trim(), user.id, user.mpinHash)) {
            return RepoResult.Error("Incorrect 4-digit security MPIN.")
        }
        val settings = dao.getPaymentSettings() ?: PaymentSettingsEntity()
        if (!settings.withdrawalsEnabled) {
            return RepoResult.Error("Bank withdrawals are currently paused by Compliance Admin.")
        }
        if (user.kycStatus != "VERIFIED") {
            return RepoResult.Error("RBI PPI Regulations require Full KYC verification before bank withdrawal.")
        }
        if (!Formatters.isValidBankAccount(accountNumber)) {
            return RepoResult.Error("Enter a valid 9-18 digit bank account number.")
        }
        if (!Formatters.isValidIfsc(ifsc)) {
            return RepoResult.Error("Enter a valid 11-character IFSC code (e.g. HDFC0001024).")
        }

        val recentCount = dao.countRecentTransactions(userId, System.currentTimeMillis() - 60_000L)
        val fraud = RateLimiterAndFraudEngine.assessTransaction(
            user = user,
            amountPaise = amountPaise,
            isCredit = false,
            recentTxnCountLastMinute = recentCount,
            settings = settings
        )
        if (!fraud.allowed) {
            return RepoResult.Error(fraud.blockReason ?: "Withdrawal blocked by risk engine.")
        }

        val maskedAccount = "A/C •••• ${accountNumber.takeLast(4)} | ${ifsc.uppercase()}"
        val encryptedBank = CryptoVault.encryptSensitive("$accountHolder | $maskedAccount")
        dao.updateEncryptedBankInfo(userId, encryptedBank)

        val orderId = "wd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val utr = "IMPS${(100000000000L..999999999999L).random()}"
        val sig = SandboxServerVerifier.signOrderChallenge(orderId, amountPaise, utr, gatewayClient.configuredKeyId)

        val txn = TransactionEntity(
            id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = userId,
            utrNumber = utr,
            orderId = orderId,
            type = "WITHDRAW",
            direction = "DEBIT",
            status = "SUCCESS",
            amountPaise = amountPaise,
            counterpartyName = "Bank Payout (${accountHolder.ifBlank { user.fullName }})",
            counterpartyHandle = maskedAccount,
            gatewayMethod = "BANK_IMPS",
            gatewaySignature = sig,
            riskScore = fraud.riskScore,
            isFlagged = fraud.isFlagged,
            note = "IMPS Bank Withdrawal (Sandbox)"
        )

        dao.executeAtomicDebitAndOptionalCredit(
            senderId = userId,
            debitPaise = amountPaise,
            recipientId = null,
            creditPaise = 0L,
            senderTxn = txn,
            recipientTxn = null,
            auditLog = AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = user.role,
                action = "WALLET_WITHDRAW_IMPS",
                targetId = txn.id,
                details = "Withdrew ${Formatters.formatInr(amountPaise)} to $maskedAccount.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(txn)
    }

    // --- Recharges & Bills (Mobile, DTH, Electricity, Water) ---

    suspend fun payRechargeOrBill(
        userId: String,
        type: String, // MOBILE_RECHARGE, DTH_RECHARGE, ELECTRICITY_BILL, WATER_BILL
        providerName: String,
        consumerOrMobileId: String,
        amountPaise: Long,
        planOrNote: String,
        mpin: String
    ): RepoResult<TransactionEntity> {
        val user = dao.getUserById(userId) ?: return RepoResult.Error("Session expired.")
        if (!CryptoVault.verifyPin(mpin.trim(), user.id, user.mpinHash)) {
            return RepoResult.Error("Incorrect 4-digit security MPIN.")
        }
        if (consumerOrMobileId.trim().length < 5) {
            return RepoResult.Error("Enter a valid consumer / subscriber / mobile number.")
        }

        val settings = dao.getPaymentSettings() ?: PaymentSettingsEntity()
        val recentCount = dao.countRecentTransactions(userId, System.currentTimeMillis() - 60_000L)
        val fraud = RateLimiterAndFraudEngine.assessTransaction(
            user = user,
            amountPaise = amountPaise,
            isCredit = false,
            recentTxnCountLastMinute = recentCount,
            settings = settings
        )
        if (!fraud.allowed) {
            return RepoResult.Error(fraud.blockReason ?: "Bill payment declined.")
        }

        val orderId = "bbps_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val utr = "BBPS${(100000000000L..999999999999L).random()}"
        val sig = SandboxServerVerifier.signOrderChallenge(orderId, amountPaise, utr, gatewayClient.configuredKeyId)

        val txn = TransactionEntity(
            id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = userId,
            utrNumber = utr,
            orderId = orderId,
            type = type,
            direction = "DEBIT",
            status = "SUCCESS",
            amountPaise = amountPaise,
            counterpartyName = providerName,
            counterpartyHandle = consumerOrMobileId.trim(),
            gatewayMethod = "BILL_BBPS",
            gatewaySignature = sig,
            riskScore = fraud.riskScore,
            isFlagged = fraud.isFlagged,
            note = planOrNote.ifBlank { providerName }
        )

        val cashbackPaise = if (settings.cashbackEnabled && settings.cashbackPercent > 0) {
            (amountPaise * settings.cashbackPercent) / 100L
        } else 0L

        val cashbackTxn = if (cashbackPaise > 0L) {
            TransactionEntity(
                id = "txn_cb_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                userId = userId,
                utrNumber = "CB$utr",
                orderId = orderId,
                type = "CASHBACK",
                direction = "CREDIT",
                status = "SUCCESS",
                amountPaise = cashbackPaise,
                counterpartyName = "${settings.cashbackPercent}% Instant Cashback",
                counterpartyHandle = "Reward • $providerName",
                gatewayMethod = "SUJOY_WALLET",
                gatewaySignature = sig,
                riskScore = 0,
                isFlagged = false,
                note = "${settings.cashbackPercent}% cashback on $providerName bill/recharge"
            )
        } else null

        dao.executeAtomicDebitAndOptionalCredit(
            senderId = userId,
            debitPaise = amountPaise,
            recipientId = null,
            creditPaise = 0L,
            senderTxn = txn,
            recipientTxn = null,
            cashbackPaise = cashbackPaise,
            cashbackTxn = cashbackTxn,
            auditLog = AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = user.role,
                action = "BBPS_PAYMENT_$type",
                targetId = txn.id,
                details = "Paid ${Formatters.formatInr(amountPaise)} for $providerName ($consumerOrMobileId); Cashback: ${Formatters.formatInr(cashbackPaise)}.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(txn)
    }

    // --- Profile Security & Complaints ---

    suspend fun updateBankInfo(userId: String, bankLabel: String, account: String, ifsc: String): RepoResult<Unit> {
        if (!Formatters.isValidBankAccount(account)) {
            return RepoResult.Error("Bank account number must be 9 to 18 digits.")
        }
        if (!Formatters.isValidIfsc(ifsc)) {
            return RepoResult.Error("Invalid IFSC code format.")
        }
        val formatted = "${bankLabel.ifBlank { "Bank" }} •••• ${account.takeLast(4)} | ${ifsc.uppercase()}"
        dao.updateEncryptedBankInfo(userId, CryptoVault.encryptSensitive(formatted))
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = "USER",
                action = "BANK_DETAILS_UPDATED",
                targetId = userId,
                details = "Encrypted bank payout details updated via Android Keystore.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(Unit)
    }

    suspend fun updateMpin(userId: String, currentPin: String, newPin: String): RepoResult<Unit> {
        val user = dao.getUserById(userId) ?: return RepoResult.Error("User not found.")
        if (!CryptoVault.verifyPin(currentPin.trim(), user.id, user.mpinHash)) {
            return RepoResult.Error("Current MPIN is incorrect.")
        }
        if (!newPin.trim().matches(Regex("^\\d{4}$"))) {
            return RepoResult.Error("New MPIN must be exactly 4 digits.")
        }
        dao.updateMpinHash(userId, CryptoVault.hashPin(newPin.trim(), user.id))
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = user.role,
                action = "MPIN_ROTATED",
                targetId = userId,
                details = "Salted SHA-256 MPIN rotated.",
                severity = "INFO"
            )
        )
        return RepoResult.Success(Unit)
    }

    suspend fun submitComplaint(
        userId: String,
        transaction: TransactionEntity,
        category: String,
        description: String,
        requestRefund: Boolean
    ): RepoResult<ComplaintEntity> {
        val user = dao.getUserById(userId) ?: return RepoResult.Error("User not found.")
        if (description.trim().length < 8) {
            return RepoResult.Error("Please provide at least 8 characters describing the issue.")
        }
        val complaint = ComplaintEntity(
            id = "cmp_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            userId = userId,
            userName = user.fullName,
            transactionId = transaction.id,
            utrNumber = transaction.utrNumber,
            category = category,
            description = description.trim(),
            status = "OPEN",
            refundRequested = requestRefund,
            refundStatus = if (requestRefund) "PENDING_REVIEW" else "NONE",
            amountPaise = transaction.amountPaise
        )
        dao.upsertComplaint(complaint)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = userId,
                actorRole = user.role,
                action = "DISPUTE_TICKET_OPENED",
                targetId = complaint.id,
                details = "Opened complaint for ${transaction.utrNumber} (Refund requested: $requestRefund).",
                severity = "INFO"
            )
        )
        return RepoResult.Success(complaint)
    }

    // --- Admin Governance Actions ---

    suspend fun adminToggleUserFreeze(adminId: String, targetUserId: String, freeze: Boolean) {
        dao.setUserFrozen(targetUserId, freeze)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = adminId,
                actorRole = "ADMIN",
                action = if (freeze) "ADMIN_USER_FROZEN" else "ADMIN_USER_UNFROZEN",
                targetId = targetUserId,
                details = "Wallet freeze state changed to $freeze by Admin.",
                severity = if (freeze) "CRITICAL" else "WARN"
            )
        )
    }

    suspend fun adminUpdateKyc(adminId: String, targetUserId: String, newKycStatus: String) {
        dao.setUserKycStatus(targetUserId, newKycStatus)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = adminId,
                actorRole = "ADMIN",
                action = "ADMIN_KYC_UPDATED",
                targetId = targetUserId,
                details = "KYC tier updated to $newKycStatus.",
                severity = "INFO"
            )
        )
    }

    suspend fun adminUpdateSettings(adminId: String, settings: PaymentSettingsEntity) {
        dao.upsertPaymentSettings(
            settings.copy(
                id = 1,
                updatedAt = System.currentTimeMillis(),
                updatedBy = adminId
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = adminId,
                actorRole = "ADMIN",
                action = "ADMIN_SETTINGS_UPDATED",
                targetId = "PAYMENT_SETTINGS",
                details = "Updated gateway flags (Sandbox=${settings.sandboxModeEnabled}, Maint=${settings.maintenanceMode}, MaxTxn=${Formatters.formatInr(settings.maxPerTxnPaise)}).",
                severity = "WARN"
            )
        )
    }

    suspend fun adminResolveComplaintAndOptionalRefund(
        adminId: String,
        complaintId: String,
        newStatus: String,
        remarks: String,
        issueRefund: Boolean
    ): RepoResult<Unit> {
        val complaint = dao.getComplaintById(complaintId)
            ?: return RepoResult.Error("Complaint ticket not found.")
        val now = System.currentTimeMillis()
        if (issueRefund) {
            if (complaint.refundStatus == "PROCESSED") {
                return RepoResult.Error("Refund has already been processed for this ticket.")
            }
            val updatedComplaint = complaint.copy(
                status = "RESOLVED",
                adminRemarks = remarks.ifBlank { "Refund approved & credited to wallet." },
                refundStatus = "PROCESSED",
                updatedAt = now
            )
            val refundTxn = TransactionEntity(
                id = "txn_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                userId = complaint.userId,
                utrNumber = "REF${(100000000000L..999999999999L).random()}",
                orderId = "ref_${complaint.id}",
                type = "REFUND",
                direction = "CREDIT",
                status = "SUCCESS",
                amountPaise = complaint.amountPaise,
                counterpartyName = "SUJOY PAY Dispute Refund",
                counterpartyHandle = "Ref: ${complaint.utrNumber}",
                gatewayMethod = "SUJOY_WALLET",
                gatewaySignature = SandboxServerVerifier.signOrderChallenge(
                    complaint.id,
                    complaint.amountPaise,
                    "REFUND",
                    gatewayClient.configuredKeyId
                ),
                riskScore = 0,
                isFlagged = false,
                note = "Admin Refund for Ticket ${complaint.id}"
            )
            dao.executeAdminRefund(
                complaint = updatedComplaint,
                refundTxn = refundTxn,
                userId = complaint.userId,
                refundPaise = complaint.amountPaise,
                auditLog = AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = adminId,
                    actorRole = "ADMIN",
                    action = "ADMIN_REFUND_PROCESSED",
                    targetId = complaint.id,
                    details = "Credited ${Formatters.formatInr(complaint.amountPaise)} refund to ${complaint.userName}.",
                    severity = "WARN"
                )
            )
        } else {
            dao.upsertComplaint(
                complaint.copy(
                    status = newStatus,
                    adminRemarks = remarks,
                    refundStatus = if (newStatus == "REJECTED" && complaint.refundRequested) "DENIED" else complaint.refundStatus,
                    updatedAt = now
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(10)}",
                    actorUserId = adminId,
                    actorRole = "ADMIN",
                    action = "ADMIN_COMPLAINT_$newStatus",
                    targetId = complaint.id,
                    details = "Updated complaint ${complaint.id} to $newStatus.",
                    severity = "INFO"
                )
            )
        }
        return RepoResult.Success(Unit)
    }

    suspend fun adminResolveFraudFlag(adminId: String, txnId: String, clearFlag: Boolean, freezeUser: Boolean) {
        val txn = dao.getTransactionById(txnId) ?: return
        dao.updateTransactionFlag(txnId, flagged = !clearFlag, riskScore = if (clearFlag) 10 else txn.riskScore)
        if (freezeUser) {
            dao.setUserFrozen(txn.userId, true)
        }
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_${UUID.randomUUID().toString().take(10)}",
                actorUserId = adminId,
                actorRole = "ADMIN",
                action = if (clearFlag) "FRAUD_FLAG_CLEARED" else "FRAUD_CONFIRMED_FREEZE",
                targetId = txnId,
                details = "Transaction ${txn.utrNumber}: clearFlag=$clearFlag, freezeUser=$freezeUser.",
                severity = if (freezeUser) "CRITICAL" else "INFO"
            )
        )
    }
}
