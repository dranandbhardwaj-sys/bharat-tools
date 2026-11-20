package com.example.ui.screens.tools

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
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
import com.example.core.engine.ImageInspectInfo
import com.example.core.engine.ImageProcessResult
import com.example.core.engine.ImageToolkitEngine
import com.example.core.engine.PdfToolkitEngine
import com.example.ui.components.BharatUploadZone
import com.example.ui.components.ToolTopBar
import kotlinx.coroutines.launch

@Composable
fun ImageToolkitScreen(
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
        "Compress", "Resize", "Crop", "JPG→PNG", "PNG→JPG",
        "WebP", "Img→PDF", "Backdrop", "Rotate", "Inspect"
    )
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, tabs.lastIndex)) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<ImageInspectInfo?>(null) }
    var result by remember { mutableStateOf<ImageProcessResult?>(null) }
    var pdfResultFile by remember { mutableStateOf<java.io.File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var quality by remember { mutableFloatStateOf(75f) }
    var widthInput by remember { mutableStateOf("1080") }
    var heightInput by remember { mutableStateOf("1080") }
    var cropRatio by remember { mutableStateOf(1f to 1f) }
    var bgTolerance by remember { mutableFloatStateOf(35f) }
    var bgColorOption by remember { mutableStateOf<Int?>(null) }
    var rotateDeg by remember { mutableFloatStateOf(90f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            result = null
            pdfResultFile = null
            errorMsg = null
            scope.launch {
                ImageToolkitEngine.inspectImage(context, uri).onSuccess { info ->
                    inspectInfo = info
                    widthInput = info.width.toString()
                    heightInput = info.height.toString()
                }.onFailure { errorMsg = it.localizedMessage }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { dest ->
        val fileToSave = result?.file ?: pdfResultFile
        if (dest != null && fileToSave != null) {
            scope.launch { ExportHelper.saveFileToUri(context, fileToSave, dest) }
        }
    }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = if (isHindi) "इमेज टूलकिट" else "Image Toolkit",
                subtitle = if (isHindi) "फोटो पिकर • कोई स्टोरेज अनुमति आवश्यक नहीं" else "Android Photo Picker • Zero Storage Permissions",
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tabs) { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx; errorMsg = null },
                        label = { Text(label) },
                        modifier = Modifier.testTag("img_tab_$idx")
                    )
                }
            }

            if (selectedTab == 7 && !hasProAccess) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Subscriber Feature (₹399/month)", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Background removal and solid backdrop replacement require an active Bharat Tools ₹399/month subscription.")
                        Button(onClick = onNavigateToProPass) { Text("View ₹399/mo Subscription") }
                    }
                }
            } else {
                BharatUploadZone(
                    title = "Select Photo (Photo Picker)",
                    subtitle = "Zero storage permissions • JPG, PNG, WebP",
                    selectedSummary = inspectInfo?.let { "${it.fileName} (${it.width}×${it.height})" },
                    isProcessing = isProcessing,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("img_pick_btn")
                )

                inspectInfo?.let { info ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Dimensions: ${info.width} × ${info.height} px (${info.megapixels})", style = MaterialTheme.typography.bodyMedium)
                            Text("Aspect Ratio: ${info.aspectRatio} • Size: ${ExportHelper.formatBytes(info.fileSizeBytes)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                when (selectedTab) {
                    0, 5 -> {
                        Text("Quality: ${quality.toInt()}%", style = MaterialTheme.typography.titleSmall)
                        Slider(value = quality, onValueChange = { quality = it }, valueRange = 15f..100f)
                    }
                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = widthInput,
                                onValueChange = { widthInput = it.filter(Char::isDigit) },
                                label = { Text("Width (px)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it.filter(Char::isDigit) },
                                label = { Text("Height (px)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    2 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1:1" to (1f to 1f), "4:3" to (4f to 3f), "16:9" to (16f to 9f), "3:4" to (3f to 4f)).forEach { (label, pair) ->
                                FilterChip(
                                    selected = cropRatio == pair,
                                    onClick = { cropRatio = pair },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    7 -> {
                        Text("Color Key Tolerance: ${bgTolerance.toInt()}", style = MaterialTheme.typography.titleSmall)
                        Slider(value = bgTolerance, onValueChange = { bgTolerance = it }, valueRange = 10f..90f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = bgColorOption == null, onClick = { bgColorOption = null }, label = { Text("Transparent") })
                            FilterChip(selected = bgColorOption == AndroidColor.WHITE, onClick = { bgColorOption = AndroidColor.WHITE }, label = { Text("White") })
                            FilterChip(selected = bgColorOption == AndroidColor.parseColor("#0F2C4C"), onClick = { bgColorOption = AndroidColor.parseColor("#0F2C4C") }, label = { Text("Passport Blue") })
                        }
                    }
                    8 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(90f, 180f, 270f).forEach { deg ->
                                FilterChip(selected = rotateDeg == deg, onClick = { rotateDeg = deg }, label = { Text("${deg.toInt()}°") })
                            }
                            FilterChip(selected = flipH, onClick = { flipH = !flipH }, label = { Text("Flip H") })
                            FilterChip(selected = flipV, onClick = { flipV = !flipV }, label = { Text("Flip V") })
                        }
                    }
                }

                if (selectedTab != 9) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val uri = selectedUri
                                if (uri == null) {
                                    errorMsg = "Select a photo first."
                                    return@Button
                                }
                                scope.launch {
                                    isProcessing = true
                                    errorMsg = null
                                    pdfResultFile = null
                                    if (selectedTab == 6) {
                                        PdfToolkitEngine.imagesToPdf(context, listOf(uri))
                                            .onSuccess { res ->
                                                pdfResultFile = res.files.firstOrNull()
                                                onToolCompleted("img_to_pdf_b", "Image to PDF", res.summary)
                                            }
                                            .onFailure { errorMsg = it.localizedMessage }
                                    } else {
                                        val call = when (selectedTab) {
                                            0 -> ImageToolkitEngine.compressOrConvert(context, uri, "JPG", quality.toInt())
                                            1 -> ImageToolkitEngine.resizeImage(context, uri, widthInput.toIntOrNull() ?: 800, heightInput.toIntOrNull() ?: 800)
                                            2 -> ImageToolkitEngine.cropImageAspect(context, uri, cropRatio.first, cropRatio.second)
                                            3 -> ImageToolkitEngine.compressOrConvert(context, uri, "PNG", 100)
                                            4 -> ImageToolkitEngine.compressOrConvert(context, uri, "JPG", 92)
                                            5 -> ImageToolkitEngine.compressOrConvert(context, uri, "WEBP", quality.toInt())
                                            7 -> ImageToolkitEngine.removeOrReplaceBackground(context, uri, bgTolerance.toInt(), bgColorOption)
                                            else -> ImageToolkitEngine.rotateOrFlip(context, uri, rotateDeg, flipH, flipV)
                                        }
                                        call.onSuccess { r ->
                                            result = r
                                            onToolCompleted("img_${tabs[selectedTab].lowercase()}", tabs[selectedTab], "${r.width}×${r.height} • ${ExportHelper.formatBytes(r.outputBytes)}")
                                        }.onFailure { errorMsg = it.localizedMessage }
                                    }
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("img_run_btn")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Process Image (${tabs[selectedTab]})")
                            }
                        }
                        if (selectedUri != null || result != null || pdfResultFile != null) {
                            OutlinedButton(
                                onClick = {
                                    selectedUri = null
                                    inspectInfo = null
                                    result = null
                                    pdfResultFile = null
                                    errorMsg = null
                                },
                                modifier = Modifier.height(50.dp).testTag("img_reset_btn")
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

            result?.let { r ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Output: ${r.width}×${r.height} • ${ExportHelper.formatBytes(r.outputBytes)} (from ${ExportHelper.formatBytes(r.originalBytes)})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Image(
                            bitmap = r.preview.asImageBitmap(),
                            contentDescription = "Processed image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Fit
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { saveLauncher.launch(r.file.name) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                            Button(
                                onClick = { ExportHelper.shareFile(context, r.file, r.mimeType, "Share Image") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }

            pdfResultFile?.let { pdf ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PDF Ready: ${pdf.name} (${ExportHelper.formatBytes(pdf.length())})", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { saveLauncher.launch(pdf.name) }, modifier = Modifier.weight(1f)) { Text("Save PDF") }
                            Button(onClick = { ExportHelper.shareFile(context, pdf, "application/pdf", "Share PDF") }, modifier = Modifier.weight(1f)) { Text("Share PDF") }
                        }
                    }
                }
            }
        }
    }
}
