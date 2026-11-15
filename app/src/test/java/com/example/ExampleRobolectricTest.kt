package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SujoyPayDatabase
import com.example.data.repository.RepoResult
import com.example.data.repository.SujoyPayRepository
import com.example.network.GatewayClient
import com.example.security.CryptoVault
import com.example.security.RateLimiterAndFraudEngine
import com.example.security.SandboxServerVerifier
import com.example.security.SessionPreferences
import com.example.ui.viewmodel.AdminViewModel
import com.example.util.Formatters
import com.example.util.QrCodeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches SUJOY PAY`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SUJOY PAY", appName)
    }

    @Test
    fun `hmac gateway verification rejects tampered signature`() {
        val orderId = "ord_test_123"
        val paymentId = "pay_test_456"
        val amount = 50000L
        val nonce = "nonce123"
        val keyId = "rzp_test_key"

        val validSig = SandboxServerVerifier.signGatewayCallback(orderId, paymentId, amount, nonce, keyId)
        assertTrue(SandboxServerVerifier.verifyGatewayCallback(orderId, paymentId, amount, nonce, keyId, validSig))
        assertFalse(SandboxServerVerifier.verifyGatewayCallback(orderId, paymentId, amount + 100L, nonce, keyId, validSig))
    }

    @Test
    fun `upi qr uri round trip parses payee and amount`() {
        val uri = QrCodeUtils.buildUpiUri(
            vpa = "9876543210@sujoypay",
            payeeName = "Aarav Chatterjee",
            amountPaise = 25050L,
            note = "Dinner"
        )
        val parsed = QrCodeUtils.parseUpiPayload(uri)
        assertNotNull(parsed)
        assertEquals("9876543210@sujoypay", parsed?.payeeAddress)
        assertEquals("Aarav Chatterjee", parsed?.payeeName)
        assertEquals("250.50", parsed?.amountRupees)
    }

    @Test
    fun `otp rate limiter locks after 5 failed attempts`() {
        val mobile = "9898989898"
        RateLimiterAndFraudEngine.clearOtpAttempts(mobile)
        repeat(4) {
            val state = RateLimiterAndFraudEngine.recordOtpFailure(mobile)
            assertFalse(state.isLocked)
        }
        val lockedState = RateLimiterAndFraudEngine.recordOtpFailure(mobile)
        assertTrue(lockedState.isLocked)
        RateLimiterAndFraudEngine.clearOtpAttempts(mobile)
    }

    @Test
    fun `crypto vault pin hashing and validation`() {
        val hash = CryptoVault.hashPin("1234", "usr_1")
        assertTrue(CryptoVault.verifyPin("1234", "usr_1", hash))
        assertFalse(CryptoVault.verifyPin("9999", "usr_1", hash))
        assertTrue(Formatters.isValidIndianMobile("9876543210"))
        assertFalse(Formatters.isValidIndianMobile("1234567890"))
    }

    @Test
    fun `outgoing payments award 25 percent instant cashback while withdrawals do not`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SujoyPayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.dao()
            val repo = SujoyPayRepository(dao, SessionPreferences(context), GatewayClient())
            val loginRes = repo.loginDemoAccount(isAdmin = false)
            assertTrue(loginRes is RepoResult.Success)
            val demoUser = (loginRes as RepoResult.Success).data
            assertEquals(24850_50L, demoUser.balancePaise)
            assertEquals(474_75L, demoUser.totalCashbackEarnedPaise)

            // Pay ₹400.00 mobile recharge -> debits ₹400.00, credits ₹100.00 (25% cashback)
            val payRes = repo.payRechargeOrBill(
                userId = demoUser.id,
                type = "MOBILE_RECHARGE",
                providerName = "Jio Prepaid",
                consumerOrMobileId = "9876543210",
                amountPaise = 400_00L,
                planOrNote = "28 Days Plan",
                mpin = "1234"
            )
            assertTrue(payRes is RepoResult.Success)

            val afterPayUser = dao.getUserById(demoUser.id)!!
            assertEquals(24550_50L, afterPayUser.balancePaise)
            assertEquals(574_75L, afterPayUser.totalCashbackEarnedPaise)

            val txnsAfterPay = repo.observeUserTransactions(demoUser.id).first()
            val cashbackTxn = txnsAfterPay.firstOrNull { it.type == "CASHBACK" && it.amountPaise == 100_00L }
            assertNotNull(cashbackTxn)
            assertEquals("CREDIT", cashbackTxn?.direction)

            // Withdraw ₹200.00 -> debits ₹200.00, no cashback awarded
            val withdrawRes = repo.withdrawMoney(
                userId = demoUser.id,
                accountNumber = "123456789012",
                ifsc = "HDFC0001024",
                accountHolder = demoUser.fullName,
                amountPaise = 200_00L,
                mpin = "1234"
            )
            assertTrue(withdrawRes is RepoResult.Success)
            val afterWithdrawUser = dao.getUserById(demoUser.id)!!
            assertEquals(24350_50L, afterWithdrawUser.balancePaise)
            assertEquals(574_75L, afterWithdrawUser.totalCashbackEarnedPaise)
        } finally {
            db.close()
        }
    }

    @Test
    fun `admin password 8016 unlocks admin console and rejects 1234`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SujoyPayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.dao()
            val repo = SujoyPayRepository(dao, SessionPreferences(context), GatewayClient())
            val loginRes = repo.loginDemoAccount(isAdmin = true)
            assertTrue(loginRes is RepoResult.Success)
            val adminUser = (loginRes as RepoResult.Success).data
            assertEquals("ADMIN", adminUser.role)

            val adminViewModel = AdminViewModel(repo, adminUser.id)
            adminViewModel.unlockAdminConsole(adminUser, "1234")
            assertFalse(adminViewModel.uiState.value.isUnlocked)
            assertNotNull(adminViewModel.uiState.value.pinError)

            adminViewModel.unlockAdminConsole(adminUser, "8016")
            assertTrue(adminViewModel.uiState.value.isUnlocked)
            assertEquals(null, adminViewModel.uiState.value.pinError)
        } finally {
            db.close()
        }
    }
}
