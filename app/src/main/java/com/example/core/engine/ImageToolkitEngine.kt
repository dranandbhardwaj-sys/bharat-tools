package com.example.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

data class ImageInspectInfo(
    val fileName: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val aspectRatio: String,
    val megapixels: String
)

data class ImageProcessResult(
    val file: File,
    val preview: Bitmap,
    val originalBytes: Long,
    val outputBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)

object ImageToolkitEngine {

    private fun decodeBitmapSafely(context: Context, uri: Uri, maxDim: Int = 2400): Pair<Bitmap, Long> {
        val size = ExportHelper.getFileSize(context, uri)
        if (size > ExportHelper.MAX_INPUT_FILE_BYTES) {
            throw IllegalArgumentException("Image exceeds 25 MB limit.")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Unable to decode selected image")
        val rawSize = if (size > 0) size else (bmp.byteCount.toLong() / 4L)
        return bmp to rawSize
    }

    suspend fun inspectImage(context: Context, uri: Uri): Result<ImageInspectInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val name = ExportHelper.getDisplayName(context, uri)
            val bytes = ExportHelper.getFileSize(context, uri).coerceAtLeast(0L)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val w = opts.outWidth.coerceAtLeast(1)
            val h = opts.outHeight.coerceAtLeast(1)
            val g = gcd(w, h)
            val mp = (w.toDouble() * h.toDouble()) / 1_000_000.0
            ImageInspectInfo(
                fileName = name,
                width = w,
                height = h,
                fileSizeBytes = bytes,
                aspectRatio = "${w / g}:${h / g}",
                megapixels = String.format("%.2f MP", mp)
            )
        }
    }

    suspend fun compressOrConvert(
        context: Context,
        uri: Uri,
        format: String,
        quality: Int
    ): Result<ImageProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (bmp, origBytes) = decodeBitmapSafely(context, uri)
            val (compFormat, ext, mime) = when (format.uppercase()) {
                "PNG" -> Triple(Bitmap.CompressFormat.PNG, "png", "image/png")
                "WEBP" -> {
                    val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    Triple(fmt, "webp", "image/webp")
                }
                else -> Triple(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg")
            }
            val prepared = if (ext == "jpg") flattenAlphaWhite(bmp) else bmp
            val out = ExportHelper.createExportFile(context, "bharat_img_${System.currentTimeMillis()}.$ext")
            FileOutputStream(out).use { fos -> prepared.compress(compFormat, quality.coerceIn(10, 100), fos) }
            if (prepared !== bmp) bmp.recycle()
            ImageProcessResult(out, prepared, origBytes, out.length(), prepared.width, prepared.height, mime)
        }
    }

    suspend fun resizeImage(
        context: Context,
        uri: Uri,
        targetW: Int,
        targetH: Int
    ): Result<ImageProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(targetW in 16..4096 && targetH in 16..4096) { "Width and height must be 16–4096 px." }
            val (bmp, origBytes) = decodeBitmapSafely(context, uri)
            val resized = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
            if (resized !== bmp) bmp.recycle()
            val out = ExportHelper.createExportFile(context, "resized_${targetW}x${targetH}_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { resized.compress(Bitmap.CompressFormat.PNG, 95, it) }
            ImageProcessResult(out, resized, origBytes, out.length(), resized.width, resized.height, "image/png")
        }
    }

    suspend fun cropImageAspect(
        context: Context,
        uri: Uri,
        ratioW: Float,
        ratioH: Float
    ): Result<ImageProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (bmp, origBytes) = decodeBitmapSafely(context, uri)
            val srcRatio = bmp.width.toFloat() / bmp.height.toFloat()
            val dstRatio = ratioW / ratioH
            val cropW: Int
            val cropH: Int
            if (srcRatio > dstRatio) {
                cropH = bmp.height
                cropW = (cropH * dstRatio).toInt().coerceIn(1, bmp.width)
            } else {
                cropW = bmp.width
                cropH = (cropW / dstRatio).toInt().coerceIn(1, bmp.height)
            }
            val startX = ((bmp.width - cropW) / 2).coerceAtLeast(0)
            val startY = ((bmp.height - cropH) / 2).coerceAtLeast(0)
            val cropped = Bitmap.createBitmap(bmp, startX, startY, cropW, cropH)
            if (cropped !== bmp) bmp.recycle()
            val out = ExportHelper.createExportFile(context, "cropped_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { cropped.compress(Bitmap.CompressFormat.PNG, 95, it) }
            ImageProcessResult(out, cropped, origBytes, out.length(), cropped.width, cropped.height, "image/png")
        }
    }

    suspend fun rotateOrFlip(
        context: Context,
        uri: Uri,
        degrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): Result<ImageProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (bmp, origBytes) = decodeBitmapSafely(context, uri)
            val m = Matrix().apply {
                postRotate(degrees)
                postScale(if (flipHorizontal) -1f else 1f, if (flipVertical) -1f else 1f)
            }
            val transformed = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (transformed !== bmp) bmp.recycle()
            val out = ExportHelper.createExportFile(context, "rotated_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { transformed.compress(Bitmap.CompressFormat.PNG, 95, it) }
            ImageProcessResult(out, transformed, origBytes, out.length(), transformed.width, transformed.height, "image/png")
        }
    }

    suspend fun removeOrReplaceBackground(
        context: Context,
        uri: Uri,
        tolerance: Int,
        replacementColor: Int?
    ): Result<ImageProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (bmp, origBytes) = decodeBitmapSafely(context, uri, maxDim = 1200)
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            // Sample corner color as background key
            val keyColor = pixels[0]
            val kr = Color.red(keyColor)
            val kg = Color.green(keyColor)
            val kb = Color.blue(keyColor)
            val rep = replacementColor ?: Color.TRANSPARENT

            for (i in pixels.indices) {
                val p = pixels[i]
                val dr = abs(Color.red(p) - kr)
                val dg = abs(Color.green(p) - kg)
                val db = abs(Color.blue(p) - kb)
                if ((dr + dg + db) / 3 <= tolerance) {
                    pixels[i] = rep
                }
            }
            val resultBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            resultBmp.setPixels(pixels, 0, w, 0, 0, w, h)
            bmp.recycle()
            val out = ExportHelper.createExportFile(context, "bg_util_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { resultBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ImageProcessResult(out, resultBmp, origBytes, out.length(), w, h, "image/png")
        }
    }

    private fun flattenAlphaWhite(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        c.drawBitmap(src, 0f, 0f, Paint())
        return out
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
