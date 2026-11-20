package com.example.core.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ExportHelper {
    const val MAX_INPUT_FILE_BYTES = 25L * 1024L * 1024L // 25 MB safety guard

    fun getDisplayName(context: Context, uri: Uri): String {
        var name = "file"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = c.getString(idx) ?: name
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) return c.getLong(idx)
            }
        }
        return -1L
    }

    suspend fun saveFileToUri(context: Context, source: File, destUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    source.inputStream().use { inp -> inp.copyTo(out) }
                } ?: error("Cannot open destination stream")
                Unit
            }
        }

    suspend fun saveTextToUri(context: Context, text: String, destUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open destination stream")
                Unit
            }
        }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val target = if (file.parentFile == exportDir) file else file.copyTo(File(exportDir, file.name), overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun createExportFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, fileName)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
