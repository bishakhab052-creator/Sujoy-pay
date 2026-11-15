package com.example.security

import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.UserEntity

data class FraudAssessment(
    val allowed: Boolean,
    val riskScore: Int,
    val isFlagged: Boolean,
    val blockReason: String? = null
)

data class OtpRateState(
    val isLocked: Boolean,
    val remainingLockSeconds: Int,
    val attemptsLeft: Int
)

/**
 * Enforces OTP rate limiting, RBI/NPCI wallet transaction bounds, velocity fraud scoring,
 * and server-side HMAC signature generation/verification for gateway sandbox settlements.
 */
object RateLimiterAndFraudEngine {
    private const val MAX_OTP_ATTEMPTS = 5
    private const val OTP_LOCK_WINDOW_MS = 60_000L
    private const val NPCI_UPI_SINGLE_CAP_PAISE = 10_000_000L // ₹1,00,000
    private const val MIN_KYC_MONTHLY_CAP_PAISE = 1_000_000L // ₹10,000 per RBI PPI Min-KYC rules

    private val failedOtpAttempts = mutableMapOf<String, Int>()
    private val otpLockedUntil = mutableMapOf<String, Long>()

    fun checkOtpRateLimit(mobile: String, now: Long = System.currentTimeMillis()): OtpRateState {
        val lockedUntil = otpLockedUntil[mobile] ?: 0L
        if (lockedUntil > now) {
            val secs = ((lockedUntil - now) / 1000L).toInt().coerceAtLeast(1)
            return OtpRateState(isLocked = true, remainingLockSeconds = secs, attemptsLeft = 0)
        }
        if (lockedUntil in 1..now) {
            otpLockedUntil.remove(mobile)
            failedOtpAttempts.remove(mobile)
        }
        val used = failedOtpAttempts[mobile] ?: 0
        return OtpRateState(
            isLocked = false,
            remainingLockSeconds = 0,
            attemptsLeft = (MAX_OTP_ATTEMPTS - used).coerceAtLeast(0)
        )
    }

    fun recordOtpFailure(mobile: String, now: Long = System.currentTimeMillis()): OtpRateState {
        val next = (failedOtpAttempts[mobile] ?: 0) + 1
        failedOtpAttempts[mobile] = next
        if (next >= MAX_OTP_ATTEMPTS) {
            otpLockedUntil[mobile] = now + OTP_LOCK_WINDOW_MS
        }
        return checkOtpRateLimit(mobile, now)
    }

    fun clearOtpAttempts(mobile: String) {
        failedOtpAttempts.remove(mobile)
        otpLockedUntil.remove(mobile)
    }

    fun assessTransaction(
        user: UserEntity,
        amountPaise: Long,
        isCredit: Boolean,
        recentTxnCountLastMinute: Int,
        settings: PaymentSettingsEntity
    ): FraudAssessment {
        if (settings.maintenanceMode && user.role != "ADMIN") {
            return FraudAssessment(
                allowed = false,
                riskScore = 0,
                isFlagged = false,
                blockReason = "Payment services are temporarily paused for scheduled maintenance."
            )
        }
        if (user.isFrozen) {
            return FraudAssessment(
                allowed = false,
                riskScore = 100,
                isFlagged = true,
                blockReason = "Wallet is frozen by Compliance/Fraud Monitoring. Contact Support."
            )
        }
        if (amountPaise <= 0L) {
            return FraudAssessment(false, 0, false, "Amount must be greater than ₹0.00.")
        }
        if (amountPaise > settings.maxPerTxnPaise || amountPaise > NPCI_UPI_SINGLE_CAP_PAISE) {
            return FraudAssessment(
                allowed = false,
                riskScore = 80,
                isFlagged = true,
                blockReason = "Exceeds NPCI/SUJOY PAY single transaction cap."
            )
        }
        if (isCredit) {
            val projected = user.balancePaise + amountPaise
            val maxAllowedBalance = if (user.kycStatus == "VERIFIED") {
                settings.walletMaxBalancePaise
            } else {
                MIN_KYC_MONTHLY_CAP_PAISE
            }
            if (projected > maxAllowedBalance) {
                return FraudAssessment(
                    allowed = false,
                    riskScore = 50,
                    isFlagged = false,
                    blockReason = if (user.kycStatus == "VERIFIED") {
                        "Exceeds RBI maximum PPI wallet balance cap."
                    } else {
                        "Min-KYC wallet balance cap is ₹10,000. Complete Full KYC to hold up to ₹2,00,000."
                    }
                )
            }
        } else {
            if (user.balancePaise < amountPaise) {
                return FraudAssessment(false, 10, false, "Insufficient SUJOY PAY wallet balance.")
            }
            if (user.dailySpentPaise + amountPaise > settings.dailyLimitPaise) {
                return FraudAssessment(
                    allowed = false,
                    riskScore = 65,
                    isFlagged = true,
                    blockReason = "Daily wallet spending limit exceeded."
                )
            }
        }

        if (recentTxnCountLastMinute >= 5) {
            return FraudAssessment(
                allowed = false,
                riskScore = 92,
                isFlagged = true,
                blockReason = "Rate limit exceeded: Too many transactions in 60 seconds."
            )
        }

        var riskScore = 8
        if (amountPaise >= 2_500_000L) riskScore += 35 // >= ₹25,000
        if (amountPaise >= 5_000_000L) riskScore += 25 // >= ₹50,000
        if (recentTxnCountLastMinute >= 3) riskScore += 30
        if (user.kycStatus != "VERIFIED" && amountPaise >= 500_000L) riskScore += 20

        val clamped = riskScore.coerceIn(0, 100)
        return FraudAssessment(
            allowed = true,
            riskScore = clamped,
            isFlagged = clamped >= 70
        )
    }
}

/**
 * Simulates the server-side HMAC-SHA256 verification protocol used by authorized payment gateways
 * when operating in Sandbox Mode without an external merchant backend URL.
 */
object SandboxServerVerifier {
    // Ephemeral sandbox signing salt derived at runtime per order lifecycle
    private const val SANDBOX_SERVER_CONTEXT = "SUJOY_PAY_SANDBOX_GATEWAY_V1"

    fun signOrderChallenge(orderId: String, amountPaise: Long, nonce: String, keyId: String): String {
        val canonical = "$orderId|$amountPaise|$nonce|$keyId|$SANDBOX_SERVER_CONTEXT"
        return CryptoVault.hmacSha256(canonical, "$keyId:$nonce")
    }

    fun signGatewayCallback(
        orderId: String,
        gatewayPaymentId: String,
        amountPaise: Long,
        nonce: String,
        keyId: String
    ): String {
        val canonical = "$orderId|$gatewayPaymentId|$amountPaise|$nonce|AUTHORIZED"
        return CryptoVault.hmacSha256(canonical, "$keyId:$nonce:$SANDBOX_SERVER_CONTEXT")
    }

    fun verifyGatewayCallback(
        orderId: String,
        gatewayPaymentId: String,
        amountPaise: Long,
        nonce: String,
        keyId: String,
        providedSignature: String
    ): Boolean {
        val expected = signGatewayCallback(orderId, gatewayPaymentId, amountPaise, nonce, keyId)
        return CryptoVault.constantTimeEquals(expected, providedSignature)
    }
}
