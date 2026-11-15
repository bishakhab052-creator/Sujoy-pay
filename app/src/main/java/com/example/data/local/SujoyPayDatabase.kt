package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.CryptoVault
import com.example.security.SandboxServerVerifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        PaymentOrderEntity::class,
        ComplaintEntity::class,
        PaymentSettingsEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SujoyPayDatabase : RoomDatabase() {
    abstract fun dao(): SujoyPayDao

    companion object {
        @Volatile
        private var INSTANCE: SujoyPayDatabase? = null
        private val seedMutex = Mutex()

        fun getInstance(context: Context): SujoyPayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SujoyPayDatabase::class.java,
                    "sujoy_pay_encrypted_db"
                ).build().also { INSTANCE = it }
            }
        }

        suspend fun ensureSeeded(dao: SujoyPayDao) {
            seedMutex.withLock {
                if (dao.getPaymentSettings() != null) return

                val now = System.currentTimeMillis()
                val hour = 3_600_000L

                dao.upsertPaymentSettings(
                    PaymentSettingsEntity(
                        id = 1,
                        sandboxModeEnabled = true,
                        gpayUpiEnabled = true,
                        phonePeUpiEnabled = true,
                        otherUpiEnabled = true,
                        withdrawalsEnabled = true,
                        maintenanceMode = false,
                        maxPerTxnPaise = 10_000_000L,
                        dailyLimitPaise = 10_000_000L,
                        walletMaxBalancePaise = 20_000_000L,
                        convenienceFeeBps = 0,
                        updatedAt = now,
                        updatedBy = "SYSTEM_INIT"
                    )
                )

                val primaryUserId = "usr_sujoy_demo"
                val adminUserId = "usr_sujoy_admin"
                val secondUserId = "usr_priya_02"

                dao.upsertUser(
                    UserEntity(
                        id = primaryUserId,
                        fullName = "Aarav Chatterjee",
                        mobileNumber = "9876543210",
                        email = "aarav.chatterjee@sujoypay.in",
                        upiId = "9876543210@sujoypay",
                        kycStatus = "VERIFIED",
                        role = "USER",
                        isFrozen = false,
                        balancePaise = 24850_50L, // ₹24,850.50
                        dailySpentPaise = 1899_00L,
                        mpinHash = CryptoVault.hashPin("1234", primaryUserId),
                        encryptedBankInfo = CryptoVault.encryptSensitive("HDFC Bank •••• 4821 | HDFC0001024"),
                        createdAt = now - 30 * 24 * hour
                    )
                )

                dao.upsertUser(
                    UserEntity(
                        id = adminUserId,
                        fullName = "Sujoy Compliance Admin",
                        mobileNumber = "9000000001",
                        email = "admin@sujoypay.in",
                        upiId = "admin@sujoypay",
                        kycStatus = "VERIFIED",
                        role = "ADMIN",
                        isFrozen = false,
                        balancePaise = 50000_00L,
                        dailySpentPaise = 0L,
                        mpinHash = CryptoVault.hashPin("1234", adminUserId),
                        encryptedBankInfo = CryptoVault.encryptSensitive("ICICI Bank •••• 9910 | ICIC0000011"),
                        createdAt = now - 60 * 24 * hour
                    )
                )

                dao.upsertUser(
                    UserEntity(
                        id = secondUserId,
                        fullName = "Priya Nair",
                        mobileNumber = "9811223344",
                        email = "priya.nair@sujoypay.in",
                        upiId = "9811223344@sujoypay",
                        kycStatus = "MIN_KYC",
                        role = "USER",
                        isFrozen = false,
                        balancePaise = 6420_00L,
                        dailySpentPaise = 500_00L,
                        mpinHash = CryptoVault.hashPin("1234", secondUserId),
                        encryptedBankInfo = CryptoVault.encryptSensitive("SBI •••• 7732 | SBIN0004120"),
                        createdAt = now - 15 * 24 * hour
                    )
                )

                val sig1 = SandboxServerVerifier.signOrderChallenge("ord_seed_01", 10000_00L, "seed1", "sandbox")
                val seedTxns = listOf(
                    TransactionEntity(
                        id = "txn_seed_01",
                        userId = primaryUserId,
                        utrNumber = "UTR419283746501",
                        orderId = "ord_seed_01",
                        type = "ADD_MONEY",
                        direction = "CREDIT",
                        status = "SUCCESS",
                        amountPaise = 10000_00L,
                        counterpartyName = "Wallet Top-up via Google Pay UPI",
                        counterpartyHandle = "sujoypay.merchant@icici",
                        gatewayMethod = "GPAY_UPI",
                        gatewaySignature = sig1,
                        riskScore = 10,
                        isFlagged = false,
                        note = "Verified UPI Gateway Load",
                        createdAt = now - 2 * hour
                    ),
                    TransactionEntity(
                        id = "txn_seed_02",
                        userId = primaryUserId,
                        utrNumber = "UTR419283746502",
                        orderId = "ord_seed_02",
                        type = "MOBILE_RECHARGE",
                        direction = "DEBIT",
                        status = "SUCCESS",
                        amountPaise = 299_00L,
                        counterpartyName = "Jio Prepaid Recharge",
                        counterpartyHandle = "9876543210 • 28 Days 2GB/day",
                        gatewayMethod = "BILL_BBPS",
                        gatewaySignature = sig1,
                        riskScore = 5,
                        isFlagged = false,
                        note = "Mobile Recharge",
                        createdAt = now - 6 * hour
                    ),
                    TransactionEntity(
                        id = "txn_seed_03",
                        userId = primaryUserId,
                        utrNumber = "UTR419283746503",
                        orderId = "ord_seed_03",
                        type = "ELECTRICITY_BILL",
                        direction = "DEBIT",
                        status = "SUCCESS",
                        amountPaise = 1600_00L,
                        counterpartyName = "WBSEDCL Electricity",
                        counterpartyHandle = "Consumer #774120984",
                        gatewayMethod = "BILL_BBPS",
                        gatewaySignature = sig1,
                        riskScore = 12,
                        isFlagged = false,
                        note = "Monthly Power Bill",
                        createdAt = now - 20 * hour
                    ),
                    TransactionEntity(
                        id = "txn_seed_04",
                        userId = primaryUserId,
                        utrNumber = "UTR419283746504",
                        orderId = "ord_seed_04",
                        type = "ADD_MONEY",
                        direction = "CREDIT",
                        status = "PENDING",
                        amountPaise = 2500_00L,
                        counterpartyName = "PhonePe UPI Gateway",
                        counterpartyHandle = "sujoypay.merchant@icici",
                        gatewayMethod = "PHONEPE_UPI",
                        gatewaySignature = "",
                        riskScore = 18,
                        isFlagged = false,
                        note = "Awaiting NPCI callback",
                        createdAt = now - 28 * hour
                    ),
                    TransactionEntity(
                        id = "txn_seed_05",
                        userId = primaryUserId,
                        utrNumber = "UTR419283746505",
                        orderId = "ord_seed_05",
                        type = "SEND_MONEY",
                        direction = "DEBIT",
                        status = "FAILED",
                        amountPaise = 1200_00L,
                        counterpartyName = "Rohan Verma",
                        counterpartyHandle = "rohan.verma@okaxis",
                        gatewayMethod = "OTHER_UPI",
                        gatewaySignature = "",
                        riskScore = 22,
                        isFlagged = false,
                        note = "Bank server timeout - no amount debited",
                        createdAt = now - 40 * hour
                    ),
                    TransactionEntity(
                        id = "txn_seed_06",
                        userId = secondUserId,
                        utrNumber = "UTR419283746506",
                        orderId = "ord_seed_06",
                        type = "SEND_MONEY",
                        direction = "DEBIT",
                        status = "SUCCESS",
                        amountPaise = 48000_00L,
                        counterpartyName = "High Value Transfer",
                        counterpartyHandle = "vendor99@ybl",
                        gatewayMethod = "SUJOY_WALLET",
                        gatewaySignature = sig1,
                        riskScore = 78,
                        isFlagged = true,
                        note = "High velocity pattern flagged by Fraud Engine",
                        createdAt = now - 5 * hour
                    )
                )
                seedTxns.forEach { dao.upsertTransaction(it) }

                dao.upsertComplaint(
                    ComplaintEntity(
                        id = "cmp_seed_01",
                        userId = primaryUserId,
                        userName = "Aarav Chatterjee",
                        transactionId = "txn_seed_04",
                        utrNumber = "UTR419283746504",
                        category = "Pending Add Money",
                        description = "PhonePe UPI intent completed on bank app, awaiting wallet settlement confirmation.",
                        status = "OPEN",
                        adminRemarks = "",
                        refundRequested = true,
                        refundStatus = "NONE",
                        amountPaise = 2500_00L,
                        createdAt = now - 26 * hour,
                        updatedAt = now - 26 * hour
                    )
                )

                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "aud_seed_01",
                        actorUserId = "SYSTEM",
                        actorRole = "SYSTEM",
                        action = "SANDBOX_LEDGER_INIT",
                        targetId = "PAYMENT_GATEWAY",
                        details = "Initialized SUJOY PAY Sandbox Gateway & RBI/NPCI limits.",
                        severity = "INFO",
                        createdAt = now - 60 * hour
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "aud_seed_02",
                        actorUserId = secondUserId,
                        actorRole = "USER",
                        action = "FRAUD_VELOCITY_ALERT",
                        targetId = "txn_seed_06",
                        details = "Flagged ₹48,000.00 transfer (Risk Score 78/100) for compliance review.",
                        severity = "WARN",
                        createdAt = now - 5 * hour
                    )
                )
            }
        }
    }
}
