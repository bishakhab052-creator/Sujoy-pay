package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val mobileNumber: String,
    val email: String,
    val upiId: String,
    val kycStatus: String, // VERIFIED, MIN_KYC, PENDING
    val role: String, // USER, ADMIN
    val isFrozen: Boolean = false,
    val balancePaise: Long = 0L,
    val dailySpentPaise: Long = 0L,
    val totalCashbackEarnedPaise: Long = 0L,
    val mpinHash: String,
    val encryptedBankInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val utrNumber: String,
    val orderId: String,
    val type: String, // ADD_MONEY, WITHDRAW, SEND_MONEY, RECEIVE_MONEY, MOBILE_RECHARGE, DTH_RECHARGE, ELECTRICITY_BILL, WATER_BILL, REFUND
    val direction: String, // CREDIT, DEBIT
    val status: String, // SUCCESS, PENDING, FAILED
    val amountPaise: Long,
    val feePaise: Long = 0L,
    val counterpartyName: String,
    val counterpartyHandle: String,
    val gatewayMethod: String, // GPAY_UPI, PHONEPE_UPI, OTHER_UPI, SUJOY_WALLET, BANK_IMPS, BILL_BBPS
    val gatewaySignature: String = "",
    val riskScore: Int = 5,
    val isFlagged: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_orders")
data class PaymentOrderEntity(
    @PrimaryKey val orderId: String,
    val userId: String,
    val amountPaise: Long,
    val gatewayMethod: String,
    val status: String, // CREATED, UPI_INITIATED, VERIFIED, FAILED
    val serverNonce: String,
    val hmacSignature: String,
    val upiIntentUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val transactionId: String,
    val utrNumber: String,
    val category: String,
    val description: String,
    val status: String, // OPEN, IN_REVIEW, RESOLVED, REJECTED
    val adminRemarks: String = "",
    val refundRequested: Boolean = false,
    val refundStatus: String = "NONE", // NONE, APPROVED, PROCESSED, DENIED
    val amountPaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_settings")
data class PaymentSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val sandboxModeEnabled: Boolean = true,
    val gpayUpiEnabled: Boolean = true,
    val phonePeUpiEnabled: Boolean = true,
    val otherUpiEnabled: Boolean = true,
    val withdrawalsEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val maxPerTxnPaise: Long = 10_000_000L, // ₹1,00,000 NPCI limit
    val dailyLimitPaise: Long = 10_000_000L,
    val walletMaxBalancePaise: Long = 20_000_000L, // ₹2,00,000 RBI Full KYC limit
    val convenienceFeeBps: Int = 0, // 0% for UPI
    val cashbackEnabled: Boolean = true,
    val cashbackPercent: Int = 25,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "SYSTEM"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorUserId: String,
    val actorRole: String,
    val action: String,
    val targetId: String,
    val details: String,
    val severity: String, // INFO, WARN, CRITICAL
    val createdAt: Long = System.currentTimeMillis()
)
