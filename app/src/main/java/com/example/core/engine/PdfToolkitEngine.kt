package com.example.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.Matrix as PdfMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfResult(
    val files: List<File>,
    val summary: String,
    val originalBytes: Long = 0L,
    val outputBytes: Long = 0L
)

object PdfToolkitEngine {

    private fun copyUriToTempPdf(context: Context, uri: Uri, tag: String): Pair<File, Long> {
        val size = ExportHelper.getFileSize(context, uri)
        if (size > ExportHelper.MAX_INPUT_FILE_BYTES) {
            throw IllegalArgumentException("File exceeds 25 MB limit (${ExportHelper.formatBytes(size)})")
        }
        val out = File(context.cacheDir, "in_${tag}_${System.nanoTime()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { inp ->
            FileOutputStream(out).use { fos -> inp.copyTo(fos) }
        } ?: error("Unable to read selected PDF")
        if (out.length() > ExportHelper.MAX_INPUT_FILE_BYTES) {
            out.delete()
            throw IllegalArgumentException("File exceeds 25 MB safety limit.")
        }
        return out to out.length()
    }

    suspend fun getPageCount(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, _) = copyUriToTempPdf(context, uri, "count")
            try {
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer -> renderer.pageCount }
                }
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun renderPreviewPage(context: Context, uri: Uri, pageIndex: Int, maxWidth: Int = 1080): Result<Pair<Bitmap, Int>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (temp, _) = copyUriToTempPdf(context, uri, "prev")
                try {
                    ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            val total = renderer.pageCount
                            require(total > 0) { "PDF has 0 pages" }
                            val safeIndex = pageIndex.coerceIn(0, total - 1)
                            renderer.openPage(safeIndex).use { page ->
                                val scale = (maxWidth.toFloat() / page.width.coerceAtLeast(1)).coerceIn(0.5f, 2.5f)
                                val w = (page.width * scale).toInt().coerceAtLeast(1)
                                val h = (page.height * scale).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp to total
                            }
                        }
                    }
                } finally {
                    temp.delete()
                }
            }
        }

    suspend fun mergePdfs(context: Context, uris: List<Uri>): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(uris.size >= 2) { "Select at least 2 PDF files to merge." }
            val temps = mutableListOf<File>()
            var totalIn = 0L
            try {
                val outFile = ExportHelper.createExportFile(context, "bharat_merged_${System.currentTimeMillis()}.pdf")
                val merger = PDFMergerUtility()
                merger.destinationFileName = outFile.absolutePath
                uris.forEachIndexed { idx, uri ->
                    val (f, sz) = copyUriToTempPdf(context, uri, "m$idx")
                    temps.add(f)
                    totalIn += sz
                    merger.addSource(f)
                }
                merger.mergeDocuments(null)
                PdfResult(
                    files = listOf(outFile),
                    summary = "Merged ${uris.size} PDFs • ${ExportHelper.formatBytes(outFile.length())}",
                    originalBytes = totalIn,
                    outputBytes = outFile.length()
                )
            } finally {
                temps.forEach { it.delete() }
            }
        }
    }

    suspend fun splitPdf(context: Context, uri: Uri): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, sz) = copyUriToTempPdf(context, uri, "split")
            try {
                val outputs = mutableListOf<File>()
                PDDocument.load(temp).use { doc ->
                    val count = doc.numberOfPages
                    require(count >= 1) { "PDF is empty" }
                    for (i in 0 until count.coerceAtMost(30)) {
                        val single = PDDocument()
                        single.addPage(doc.getPage(i))
                        val out = ExportHelper.createExportFile(context, "split_p${i + 1}_${System.currentTimeMillis()}.pdf")
                        single.save(out)
                        single.close()
                        outputs.add(out)
                    }
                }
                PdfResult(
                    files = outputs,
                    summary = "Split into ${outputs.size} single-page PDFs",
                    originalBytes = sz,
                    outputBytes = outputs.sumOf { it.length() }
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun compressPdf(context: Context, uri: Uri, quality: Int = 65): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, origSize) = copyUriToTempPdf(context, uri, "comp")
            try {
                val out = ExportHelper.createExportFile(context, "compressed_${System.currentTimeMillis()}.pdf")
                val pdfDoc = PdfDocument()
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val dpiScale = when {
                            quality >= 80 -> 1.35f
                            quality >= 50 -> 1.0f
                            else -> 0.75f
                        }
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                val w = (page.width * dpiScale).toInt().coerceAtLeast(200)
                                val h = (page.height * dpiScale).toInt().coerceAtLeast(200)
                                val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                raw.eraseColor(Color.WHITE)
                                page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val baos = java.io.ByteArrayOutputStream()
                                raw.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(25, 95), baos)
                                raw.recycle()
                                val jpegBytes = baos.toByteArray()
                                val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

                                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                                val outPage = pdfDoc.startPage(pageInfo)
                                val srcRect = android.graphics.Rect(0, 0, decoded.width, decoded.height)
                                val dstRect = android.graphics.Rect(0, 0, page.width, page.height)
                                outPage.canvas.drawBitmap(decoded, srcRect, dstRect, null)
                                pdfDoc.finishPage(outPage)
                                decoded.recycle()
                            }
                        }
                    }
                }
                FileOutputStream(out).use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                PdfResult(
                    files = listOf(out),
                    summary = "Compressed from ${ExportHelper.formatBytes(origSize)} to ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun pdfToImages(context: Context, uri: Uri, asPng: Boolean = true): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, origSize) = copyUriToTempPdf(context, uri, "p2i")
            try {
                val outFiles = mutableListOf<File>()
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val limit = renderer.pageCount.coerceAtMost(25)
                        for (i in 0 until limit) {
                            renderer.openPage(i).use { page ->
                                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val ext = if (asPng) "png" else "jpg"
                                val out = ExportHelper.createExportFile(context, "page_${i + 1}_${System.currentTimeMillis()}.$ext")
                                FileOutputStream(out).use { fos ->
                                    bmp.compress(
                                        if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                        92,
                                        fos
                                    )
                                }
                                bmp.recycle()
                                outFiles.add(out)
                            }
                        }
                    }
                }
                PdfResult(
                    files = outFiles,
                    summary = "Exported ${outFiles.size} image(s)",
                    originalBytes = origSize,
                    outputBytes = outFiles.sumOf { it.length() }
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun imagesToPdf(context: Context, imageUris: List<Uri>): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(imageUris.isNotEmpty()) { "Pick at least 1 image." }
            val pdfDoc = PdfDocument()
            val a4W = 595
            val a4H = 842
            imageUris.forEachIndexed { idx, uri ->
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: error("Could not decode image #${idx + 1}")
                val pageInfo = PdfDocument.PageInfo.Builder(a4W, a4H, idx + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                val margin = 24f
                val availW = a4W - margin * 2
                val availH = a4H - margin * 2
                val scale = minOf(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = (a4W - drawW) / 2f
                val top = (a4H - drawH) / 2f
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(left, top)
                }
                page.canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
                pdfDoc.finishPage(page)
                bmp.recycle()
            }
            val out = ExportHelper.createExportFile(context, "images_to_pdf_${System.currentTimeMillis()}.pdf")
            FileOutputStream(out).use { pdfDoc.writeTo(it) }
            pdfDoc.close()
            PdfResult(
                files = listOf(out),
                summary = "Created ${imageUris.size}-page A4 PDF (${ExportHelper.formatBytes(out.length())})",
                outputBytes = out.length()
            )
        }
    }

    suspend fun rotatePdf(context: Context, uri: Uri, degrees: Int): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, origSize) = copyUriToTempPdf(context, uri, "rot")
            try {
                val out = ExportHelper.createExportFile(context, "rotated_${degrees}_${System.currentTimeMillis()}.pdf")
                PDDocument.load(temp).use { doc ->
                    for (page in doc.pages) {
                        page.rotation = (page.rotation + degrees) % 360
                    }
                    doc.save(out)
                }
                PdfResult(
                    files = listOf(out),
                    summary = "Rotated all pages by $degrees° • ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun extractOrReorderPages(
        context: Context,
        uri: Uri,
        pageSpec: String,
        actionLabel: String
    ): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (temp, origSize) = copyUriToTempPdf(context, uri, "ext")
            try {
                val out = ExportHelper.createExportFile(context, "${actionLabel}_${System.currentTimeMillis()}.pdf")
                PDDocument.load(temp).use { src ->
                    val total = src.numberOfPages
                    val indices = parsePageSpec(pageSpec, total)
                    require(indices.isNotEmpty()) { "Enter valid page numbers (1–$total), e.g. 1,3-4" }
                    val dst = PDDocument()
                    indices.forEach { oneBased -> dst.addPage(src.getPage(oneBased - 1)) }
                    dst.save(out)
                    dst.close()
                }
                PdfResult(
                    files = listOf(out),
                    summary = "Saved pages [$pageSpec] • ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun addWatermark(context: Context, uri: Uri, watermarkText: String): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val text = watermarkText.trim().ifEmpty { "BHARAT TOOLS" }
            val (temp, origSize) = copyUriToTempPdf(context, uri, "wm")
            try {
                val out = ExportHelper.createExportFile(context, "watermarked_${System.currentTimeMillis()}.pdf")
                PDDocument.load(temp).use { doc ->
                    for (page in doc.pages) {
                        val box = page.mediaBox
                        val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 38f)
                        cs.setNonStrokingColor(180, 180, 180)
                        val cx = box.width / 4f
                        val cy = box.height / 3f
                        cs.setTextMatrix(PdfMatrix.getRotateInstance(Math.toRadians(35.0), cx, cy))
                        cs.showText(text)
                        cs.endText()
                        cs.close()
                    }
                    doc.save(out)
                }
                PdfResult(
                    files = listOf(out),
                    summary = "Applied watermark \"$text\" • ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun protectPdf(context: Context, uri: Uri, userPass: String): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(userPass.length >= 4) { "Password must be at least 4 characters." }
            val (temp, origSize) = copyUriToTempPdf(context, uri, "prot")
            try {
                val out = ExportHelper.createExportFile(context, "protected_${System.currentTimeMillis()}.pdf")
                PDDocument.load(temp).use { doc ->
                    val ap = AccessPermission()
                    val spp = StandardProtectionPolicy(userPass, userPass, ap).apply {
                        encryptionKeyLength = 128
                    }
                    doc.protect(spp)
                    doc.save(out)
                }
                PdfResult(
                    files = listOf(out),
                    summary = "Encrypted with 128-bit password • ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun unlockPdf(context: Context, uri: Uri, currentPassword: String): Result<PdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(currentPassword.isNotEmpty()) { "Enter the authorized PDF password to unlock." }
            val (temp, origSize) = copyUriToTempPdf(context, uri, "unlk")
            try {
                val out = ExportHelper.createExportFile(context, "unlocked_${System.currentTimeMillis()}.pdf")
                PDDocument.load(temp, currentPassword).use { doc ->
                    doc.isAllSecurityToBeRemoved = true
                    doc.save(out)
                }
                PdfResult(
                    files = listOf(out),
                    summary = "Password removed • ${ExportHelper.formatBytes(out.length())}",
                    originalBytes = origSize,
                    outputBytes = out.length()
                )
            } finally {
                temp.delete()
            }
        }
    }

    private fun parsePageSpec(spec: String, maxPages: Int): List<Int> {
        val result = mutableListOf<Int>()
        spec.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            if ("-" in token) {
                val parts = token.split("-").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 2) {
                    val start = parts[0].coerceIn(1, maxPages)
                    val end = parts[1].coerceIn(1, maxPages)
                    if (start <= end) {
                        for (p in start..end) result.add(p)
                    } else {
                        for (p in start downTo end) result.add(p)
                    }
                }
            } else {
                token.toIntOrNull()?.let { p ->
                    if (p in 1..maxPages) result.add(p)
                }
            }
        }
        return result
    }
}
