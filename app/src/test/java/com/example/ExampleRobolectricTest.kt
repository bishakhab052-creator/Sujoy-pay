package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoVault
import com.example.security.RateLimiterAndFraudEngine
import com.example.security.SandboxServerVerifier
import com.example.util.Formatters
import com.example.util.QrCodeUtils
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
}
