package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val inrFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    private val dateTimeFormat by lazy {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN"))
    }

    private val shortDateFormat by lazy {
        SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))
    }

    fun formatInr(paise: Long): String {
        val rupees = paise / 100.0
        return inrFormat.format(rupees)
    }

    fun formatInrCompact(paise: Long): String {
        val rupees = paise / 100.0
        return if (paise % 100L == 0L) {
            "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(paise / 100L)
        } else {
            inrFormat.format(rupees)
        }
    }

    fun parseRupeesToPaise(input: String): Long? {
        val sanitized = input.trim().replace(",", "")
        if (sanitized.isEmpty()) return null
        val parsed = sanitized.toBigDecimalOrNull() ?: return null
        if (parsed <= java.math.BigDecimal.ZERO) return null
        return try {
            parsed.multiply(java.math.BigDecimal(100)).longValueExact()
        } catch (_: ArithmeticException) {
            (parsed.toDouble() * 100.0).toLong()
        }
    }

    fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))

    fun formatShortDate(epochMillis: Long): String = shortDateFormat.format(Date(epochMillis))

    fun isValidIndianMobile(mobile: String): Boolean {
        val clean = mobile.trim().removePrefix("+91").filter { it.isDigit() }
        return clean.matches(Regex("^[6-9]\\d{9}$"))
    }

    fun normalizeMobile(mobile: String): String {
        val digits = mobile.trim().removePrefix("+91").filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun maskMobile(mobile: String): String {
        val clean = normalizeMobile(mobile)
        return if (clean.length == 10) "+91 ••••• ${clean.takeLast(4)}" else mobile
    }

    fun isValidUpiId(upiId: String): Boolean {
        return upiId.trim().matches(Regex("^[a-zA-Z0-9._\\-]{2,64}@[a-zA-Z0-9]{2,32}$"))
    }

    fun isValidIfsc(ifsc: String): Boolean {
        return ifsc.trim().uppercase(Locale.US).matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))
    }

    fun isValidBankAccount(account: String): Boolean {
        return account.trim().matches(Regex("^\\d{9,18}$"))
    }
}
