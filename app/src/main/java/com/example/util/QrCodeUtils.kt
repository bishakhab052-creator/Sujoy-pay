package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

data class ParsedUpiPayload(
    val payeeAddress: String,
    val payeeName: String,
    val amountRupees: String?,
    val note: String?
)

object QrCodeUtils {

    fun buildUpiUri(
        vpa: String,
        payeeName: String,
        amountPaise: Long? = null,
        transactionRef: String? = null,
        note: String? = null
    ): String {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("cu", "INR")
        if (amountPaise != null && amountPaise > 0) {
            val amountStr = String.format(java.util.Locale.US, "%.2f", amountPaise / 100.0)
            builder.appendQueryParameter("am", amountStr)
        }
        if (!transactionRef.isNullOrBlank()) {
            builder.appendQueryParameter("tr", transactionRef)
        }
        if (!note.isNullOrBlank()) {
            builder.appendQueryParameter("tn", note)
        }
        return builder.build().toString()
    }

    fun generateQrBitmap(content: String, sizePx: Int = 640): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val darkNavy = Color.rgb(10, 22, 40)
        val white = Color.WHITE
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) darkNavy else white
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    fun decodeFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: Throwable) {
            null
        }
    }

    fun decodeFromYuv(data: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: Throwable) {
            null
        }
    }

    fun parseUpiPayload(raw: String): ParsedUpiPayload? {
        val trimmed = raw.trim()
        if (Formatters.isValidUpiId(trimmed)) {
            return ParsedUpiPayload(
                payeeAddress = trimmed,
                payeeName = trimmed.substringBefore("@").replaceFirstChar { it.uppercase() },
                amountRupees = null,
                note = null
            )
        }
        if (!trimmed.startsWith("upi://", ignoreCase = true)) return null
        return try {
            val uri = Uri.parse(trimmed)
            val pa = uri.getQueryParameter("pa")?.trim().orEmpty()
            if (!Formatters.isValidUpiId(pa)) return null
            val pn = uri.getQueryParameter("pn")?.trim()?.ifBlank { null }
                ?: pa.substringBefore("@")
            val am = uri.getQueryParameter("am")?.trim()?.ifBlank { null }
            val tn = uri.getQueryParameter("tn")?.trim()?.ifBlank { null }
            ParsedUpiPayload(
                payeeAddress = pa,
                payeeName = pn,
                amountRupees = am,
                note = tn
            )
        } catch (_: Throwable) {
            null
        }
    }
}
