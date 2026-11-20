package com.example.ui.screens.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.engine.ExportHelper
import com.example.core.engine.PdfResult
import com.example.core.engine.PdfToolkitEngine
import com.example.ui.components.BharatCard
import com.example.ui.components.BharatUploadZone
import com.example.ui.components.ToolTopBar
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PdfToolkitScreen(
    initialSubTab: Int,
    isHindi: Boolean,
    hasProAccess: Boolean,
    onNavigateToProPass: () -> Unit,
    onToolCompleted: (toolId: String, title: String, summary: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "Merge", "Split", "Compress", "PDF→Img", "Img→PDF",
        "Rotate", "Extract", "Reorder", "Watermark", "Protect", "Unlock", "Preview", "Export"
    )
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, tabs.lastIndex)) }
    var selectedPdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var pdfResult by remember { mutableStateOf<PdfResult?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }

    var compressQuality by remember { mutableFloatStateOf(65f) }
    var rotateDeg by remember { mutableIntStateOf(90) }
    var pageSpec by remember { mutableStateOf("1") }
    var watermarkText by remember { mutableStateOf("BHARAT TOOLS") }
    var passwordInput by remember { mutableStateOf("") }
    var exportAsPng by remember { mutableStateOf(true) }

    val multiPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            selectedPdfs = uris
            errorMsg = null
            pdfResult = null
        }
    }
    val singlePdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedPdfs = listOf(uri)
            errorMsg = null
            pdfResult = null
            previewPage = 0
            scope.launch {
                PdfToolkitEngine.renderPreviewPage(context, uri, 0).onSuccess { (bmp, tot) ->
                    previewBitmap = bmp
                    totalPages = tot
                }
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
            errorMsg = null
        }
    }
    var filePendingSave by remember { mutableStateOf<File?>(null) }
    val saveDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { dest ->
        val src = filePendingSave
        if (dest != null && src != null) {
            scope.launch {
                ExportHelper.saveFileToUri(context, src, dest)
                    .onFailure { errorMsg = it.localizedMessage }
            }
        }
    }

    fun runTask(toolId: String, title: String, block: suspend () -> Result<PdfResult>) {
        scope.launch {
            isProcessing = true
            errorMsg = null
            block().onSuccess { res ->
                pdfResult = res
                onToolCompleted(toolId, title, res.summary)
            }.onFailure { err ->
                errorMsg = err.localizedMessage ?: "PDF processing failed"
            }
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = if (isHindi) "PDF टूलकिट" else "PDF Toolkit",
                subtitle = if (isHindi) "पूर्णतः डिवाइस पर सुरक्षित प्रोसेसिंग" else "100% On-Device Processing (25 MB max)",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                itemsIndexed(tabs) { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = {
                            selectedTab = idx
                            errorMsg = null
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("pdf_tab_$idx")
                    )
                }
            }

            // Pro lock check for Protect (9) and Unlock (10)
            if ((selectedTab == 9 || selectedTab == 10) && !hasProAccess) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Subscriber Feature (₹399/month)", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "PDF 128-bit password encryption and authorized PDF unlocking require an active Bharat Tools monthly subscription (₹399/month). No client-side bypasses are permitted.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onNavigateToProPass, modifier = Modifier.testTag("pdf_pro_upgrade_btn")) {
                            Text("View ₹399/mo Subscription")
                        }
                    }
                }
            } else {
                // Input upload zone
                if (selectedTab == 4) {
                    BharatUploadZone(
                        title = "Select Images (Photo Picker)",
                        subtitle = "Zero-permission picker • PNG, JPG, WebP",
                        selectedSummary = if (selectedImages.isEmpty()) null else "${selectedImages.size} image(s) ready for A4 PDF",
                        isProcessing = isProcessing,
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("pdf_pick_images_btn")
                    )
                } else {
                    val summary = when {
                        selectedPdfs.isEmpty() -> null
                        selectedPdfs.size == 1 -> "${ExportHelper.getDisplayName(context, selectedPdfs[0])} (${if (totalPages > 0) "$totalPages pages" else "Ready"})"
                        else -> "${selectedPdfs.size} PDFs ready (${tabs[selectedTab]})"
                    }
                    BharatUploadZone(
                        title = if (selectedTab == 0) "Select 2+ PDF Files to Merge" else "Select a PDF File",
                        subtitle = "100% on-device processing • Max 25 MB per file",
                        selectedSummary = summary,
                        isProcessing = isProcessing,
                        onClick = {
                            if (selectedTab == 0) multiPdfPicker.launch(arrayOf("application/pdf"))
                            else singlePdfPicker.launch(arrayOf("application/pdf"))
                        },
                        modifier = Modifier.testTag("pdf_pick_file_btn")
                    )
                }

                // Merge File List with Reordering & Removal
                if (selectedTab == 0 && selectedPdfs.isNotEmpty()) {
                    BharatCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Merge Order (${selectedPdfs.size} files)", style = MaterialTheme.typography.titleSmall)
                            selectedPdfs.forEachIndexed { idx, uri ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${idx + 1}. ${ExportHelper.getDisplayName(context, uri)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            if (idx > 0) {
                                                val list = selectedPdfs.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx - 1, item)
                                                selectedPdfs = list
                                            }
                                        },
                                        enabled = idx > 0
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                    }
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            if (idx < selectedPdfs.lastIndex) {
                                                val list = selectedPdfs.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx + 1, item)
                                                selectedPdfs = list
                                            }
                                        },
                                        enabled = idx < selectedPdfs.lastIndex
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                    }
                                    androidx.compose.material3.IconButton(
                                        onClick = { selectedPdfs = selectedPdfs.filterIndexed { i, _ -> i != idx } }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Tool-specific controls
                when (selectedTab) {
                    2 -> {
                        Text("Compression Quality: ${compressQuality.toInt()}%", style = MaterialTheme.typography.titleSmall)
                        Slider(value = compressQuality, onValueChange = { compressQuality = it }, valueRange = 25f..90f)
                    }
                    3 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = exportAsPng, onClick = { exportAsPng = true }, label = { Text("PNG (Lossless)") })
                            FilterChip(selected = !exportAsPng, onClick = { exportAsPng = false }, label = { Text("JPG (Compact)") })
                        }
                    }
                    5 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(90, 180, 270).forEach { deg ->
                                FilterChip(selected = rotateDeg == deg, onClick = { rotateDeg = deg }, label = { Text("${deg}°") })
                            }
                        }
                    }
                    6, 7 -> {
                        OutlinedTextField(
                            value = pageSpec,
                            onValueChange = { pageSpec = it },
                            label = { Text(if (selectedTab == 6) "Pages to extract (e.g. 1,3-5)" else "New page order (e.g. 3,1,2)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    8 -> {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    9, 10 -> {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(if (selectedTab == 9) "Set PDF password (4+ chars)" else "Enter authorized PDF password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    11 -> {
                        if (previewBitmap != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val next = (previewPage - 1).coerceAtLeast(0)
                                        previewPage = next
                                        selectedPdfs.firstOrNull()?.let { uri ->
                                            scope.launch {
                                                PdfToolkitEngine.renderPreviewPage(context, uri, next).onSuccess { (b, t) ->
                                                    previewBitmap = b; totalPages = t
                                                }
                                            }
                                        }
                                    },
                                    enabled = previewPage > 0
                                ) { Text("Prev") }
                                Text("Page ${previewPage + 1} of $totalPages", style = MaterialTheme.typography.titleSmall)
                                OutlinedButton(
                                    onClick = {
                                        val next = (previewPage + 1).coerceAtMost((totalPages - 1).coerceAtLeast(0))
                                        previewPage = next
                                        selectedPdfs.firstOrNull()?.let { uri ->
                                            scope.launch {
                                                PdfToolkitEngine.renderPreviewPage(context, uri, next).onSuccess { (b, t) ->
                                                    previewBitmap = b; totalPages = t
                                                }
                                            }
                                        }
                                    },
                                    enabled = previewPage < totalPages - 1
                                ) { Text("Next") }
                            }
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Action + Reset row
                if (selectedTab != 11) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                when (selectedTab) {
                                    0 -> runTask("pdf_merge", "PDF Merge") { PdfToolkitEngine.mergePdfs(context, selectedPdfs) }
                                    1 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_split", "PDF Split") { PdfToolkitEngine.splitPdf(context, u) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    2 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_compress", "PDF Compress") { PdfToolkitEngine.compressPdf(context, u, compressQuality.toInt()) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    3 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_to_image", "PDF to Image") { PdfToolkitEngine.pdfToImages(context, u, exportAsPng) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    4 -> runTask("image_to_pdf", "Image to PDF") { PdfToolkitEngine.imagesToPdf(context, selectedImages) }
                                    5 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_rotate", "PDF Rotate") { PdfToolkitEngine.rotatePdf(context, u, rotateDeg) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    6 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_extract", "PDF Extract") { PdfToolkitEngine.extractOrReorderPages(context, u, pageSpec, "extracted") }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    7 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_reorder", "PDF Reorder") { PdfToolkitEngine.extractOrReorderPages(context, u, pageSpec, "reordered") }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    8 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_watermark", "PDF Watermark") { PdfToolkitEngine.addWatermark(context, u, watermarkText) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    9 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_protect", "PDF Protect") { PdfToolkitEngine.protectPdf(context, u, passwordInput) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    10 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_unlock", "PDF Unlock") { PdfToolkitEngine.unlockPdf(context, u, passwordInput) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                    12 -> selectedPdfs.firstOrNull()?.let { u ->
                                        runTask("pdf_download", "PDF Export") { PdfToolkitEngine.rotatePdf(context, u, 0) }
                                    } ?: run { errorMsg = "Pick a PDF file first." }
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("pdf_run_action_btn")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Text("Run ${tabs[selectedTab]}")
                            }
                        }
                        if (selectedPdfs.isNotEmpty() || selectedImages.isNotEmpty() || pdfResult != null) {
                            OutlinedButton(
                                onClick = {
                                    selectedPdfs = emptyList()
                                    selectedImages = emptyList()
                                    pdfResult = null
                                    previewBitmap = null
                                    totalPages = 0
                                    errorMsg = null
                                },
                                modifier = Modifier.height(50.dp).testTag("pdf_reset_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset workspace")
                            }
                        }
                    }
                }
            }

            errorMsg?.let { err ->
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }

            pdfResult?.let { res ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ready for Export", style = MaterialTheme.typography.titleMedium)
                        Text(res.summary, style = MaterialTheme.typography.bodyMedium)
                        res.files.take(5).forEach { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        filePendingSave = file
                                        saveDocLauncher.launch(file.name)
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Save")
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save")
                                    }
                                    Button(onClick = {
                                        val mime = when (file.extension.lowercase()) {
                                            "png" -> "image/png"
                                            "jpg", "jpeg" -> "image/jpeg"
                                            else -> "application/pdf"
                                        }
                                        ExportHelper.shareFile(context, file, mime, "Share ${file.name}")
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                }
                            }
                        }
                        if (res.files.size > 5) {
                            Text("+ ${res.files.size - 5} more files in export cache", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
