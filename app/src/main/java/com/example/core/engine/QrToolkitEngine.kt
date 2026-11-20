package com.example.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object QrToolkitEngine {

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun buildUrlPayload(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    fun buildPhonePayload(phone: String): String = "tel:${phone.trim()}"

    fun buildWifiPayload(ssid: String, password: String, security: String = "WPA"): String =
        "WIFI:T:$security;S:${escapeWifi(ssid)};P:${escapeWifi(password)};;"

    fun buildEmailPayload(email: String, subject: String, body: String): String {
        val q = listOfNotNull(
            subject.takeIf { it.isNotBlank() }?.let { "subject=${urlEncode(it)}" },
            body.takeIf { it.isNotBlank() }?.let { "body=${urlEncode(it)}" }
        ).joinToString("&")
        return if (q.isEmpty()) "mailto:${email.trim()}" else "mailto:${email.trim()}?$q"
    }

    fun buildWhatsAppPayload(phoneDigits: String, message: String): String {
        val clean = phoneDigits.filter { it.isDigit() }
        return if (message.isBlank()) "https://wa.me/$clean"
        else "https://wa.me/$clean?text=${urlEncode(message.trim())}"
    }

    fun buildUpiPayload(vpa: String, payeeName: String, amount: String, note: String): String {
        val params = mutableListOf(
            "pa=${urlEncode(vpa.trim())}",
            "pn=${urlEncode(payeeName.trim().ifEmpty { "Merchant" })}",
            "cu=INR"
        )
        amount.trim().toDoubleOrNull()?.takeIf { it > 0 }?.let {
            params.add("am=${String.format("%.2f", it)}")
        }
        if (note.isNotBlank()) {
            params.add("tn=${urlEncode(note.trim())}")
        }
        return "upi://pay?${params.joinToString("&")}"
    }

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 720,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Result<Bitmap> = runCatching {
        require(content.isNotBlank()) { "QR content cannot be empty." }
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) fgColor else bgColor
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }

    fun generateQrSvg(content: String, fgHex: String = "#000000", bgHex: String = "#FFFFFF"): Result<String> = runCatching {
        require(content.isNotBlank()) { "QR content cannot be empty." }
        val hints = mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = matrix.width
        val h = matrix.height
        val path = StringBuilder()
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (matrix[x, y]) {
                    path.append("M${x},${y}h1v1h-1z")
                }
            }
        }
        """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h" shape-rendering="crispEdges">
        |  <rect width="$w" height="$h" fill="$bgHex"/>
        |  <path d="$path" fill="$fgHex"/>
        |</svg>
        """.trimMargin()
    }

    suspend fun saveQrPng(context: Context, bmp: Bitmap): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val file = ExportHelper.createExportFile(context, "bharat_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file
        }
    }

    private fun escapeWifi(s: String): String =
        s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:")
}
