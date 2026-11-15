package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SujoyPayDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE upiId = :upiId LIMIT 1")
    suspend fun getUserByUpiId(upiId: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET balancePaise = balancePaise + :deltaPaise WHERE id = :userId")
    suspend fun adjustUserBalance(userId: String, deltaPaise: Long)

    @Query("UPDATE users SET dailySpentPaise = dailySpentPaise + :amountPaise WHERE id = :userId")
    suspend fun incrementDailySpent(userId: String, amountPaise: Long)

    @Query("UPDATE users SET totalCashbackEarnedPaise = totalCashbackEarnedPaise + :cashbackPaise WHERE id = :userId")
    suspend fun incrementCashbackEarned(userId: String, cashbackPaise: Long)

    @Query("UPDATE users SET isFrozen = :frozen WHERE id = :userId")
    suspend fun setUserFrozen(userId: String, frozen: Boolean)

    @Query("UPDATE users SET kycStatus = :kycStatus WHERE id = :userId")
    suspend fun setUserKycStatus(userId: String, kycStatus: String)

    @Query("UPDATE users SET encryptedBankInfo = :encrypted WHERE id = :userId")
    suspend fun updateEncryptedBankInfo(userId: String, encrypted: String)

    @Query("UPDATE users SET mpinHash = :newHash WHERE id = :userId")
    suspend fun updateMpinHash(userId: String, newHash: String)

    // --- Transactions ---
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isFlagged = 1 OR riskScore >= 70 ORDER BY createdAt DESC")
    fun observeFlaggedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId AND createdAt >= :sinceMillis")
    suspend fun countRecentTransactions(userId: String, sinceMillis: Long): Int

    @Query("SELECT * FROM transactions WHERE orderId = :orderId LIMIT 1")
    suspend fun getTransactionByOrderId(orderId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :txnId LIMIT 1")
    suspend fun getTransactionById(txnId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(txn: TransactionEntity)

    @Query("UPDATE transactions SET isFlagged = :flagged, riskScore = :riskScore WHERE id = :txnId")
    suspend fun updateTransactionFlag(txnId: String, flagged: Boolean, riskScore: Int)

    // --- Payment Orders ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: PaymentOrderEntity)

    @Query("SELECT * FROM payment_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): PaymentOrderEntity?

    @Update
    suspend fun updateOrder(order: PaymentOrderEntity)

    // --- Complaints ---
    @Query("SELECT * FROM complaints WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserComplaints(userId: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun observeAllComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComplaint(complaint: ComplaintEntity)

    @Query("SELECT * FROM complaints WHERE id = :complaintId LIMIT 1")
    suspend fun getComplaintById(complaintId: String): ComplaintEntity?

    // --- Payment Settings ---
    @Query("SELECT * FROM payment_settings WHERE id = 1 LIMIT 1")
    fun observePaymentSettings(): Flow<PaymentSettingsEntity?>

    @Query("SELECT * FROM payment_settings WHERE id = 1 LIMIT 1")
    suspend fun getPaymentSettings(): PaymentSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentSettings(settings: PaymentSettingsEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 200")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // --- Atomic Settlement Transactions ---
    @Transaction
    suspend fun settleVerifiedAddMoney(
        order: PaymentOrderEntity,
        updatedTxn: TransactionEntity,
        creditUserId: String,
        creditAmountPaise: Long,
        auditLog: AuditLogEntity
    ) {
        updateOrder(order)
        upsertTransaction(updatedTxn)
        adjustUserBalance(creditUserId, creditAmountPaise)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun markOrderFailed(
        order: PaymentOrderEntity,
        updatedTxn: TransactionEntity,
        auditLog: AuditLogEntity
    ) {
        updateOrder(order)
        upsertTransaction(updatedTxn)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun executeAtomicDebitAndOptionalCredit(
        senderId: String,
        debitPaise: Long,
        recipientId: String?,
        creditPaise: Long,
        senderTxn: TransactionEntity,
        recipientTxn: TransactionEntity?,
        auditLog: AuditLogEntity,
        cashbackPaise: Long = 0L,
        cashbackTxn: TransactionEntity? = null
    ) {
        adjustUserBalance(senderId, -debitPaise)
        incrementDailySpent(senderId, debitPaise)
        upsertTransaction(senderTxn)
        if (cashbackPaise > 0L && cashbackTxn != null) {
            adjustUserBalance(senderId, cashbackPaise)
            incrementCashbackEarned(senderId, cashbackPaise)
            upsertTransaction(cashbackTxn)
        }
        if (recipientId != null && recipientTxn != null && creditPaise > 0L) {
            adjustUserBalance(recipientId, creditPaise)
            upsertTransaction(recipientTxn)
        }
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun executeAdminRefund(
        complaint: ComplaintEntity,
        refundTxn: TransactionEntity,
        userId: String,
        refundPaise: Long,
        auditLog: AuditLogEntity
    ) {
        upsertComplaint(complaint)
        upsertTransaction(refundTxn)
        adjustUserBalance(userId, refundPaise)
        insertAuditLog(auditLog)
    }
}
