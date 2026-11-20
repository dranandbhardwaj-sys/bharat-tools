package com.example.ui.screens.tools

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.engine.ExportHelper
import com.example.core.engine.QrToolkitEngine
import com.example.ui.components.ToolTopBar
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.launch

@Composable
fun QrToolkitScreen(
    initialSubTab: Int,
    isHindi: Boolean,
    hasProAccess: Boolean,
    onNavigateToProPass: () -> Unit,
    onToolCompleted: (toolId: String, title: String, summary: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs = listOf("URL", "Text", "Phone", "Wi-Fi", "Email", "WhatsApp", "UPI ₹")
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, tabs.lastIndex)) }

    var urlInput by remember { mutableStateOf("https://bharattools.in") }
    var textInput by remember { mutableStateOf("Namaste from Bharat Tools!") }
    var phoneInput by remember { mutableStateOf("+919876543210") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var wifiSec by remember { mutableStateOf("WPA") }
    var emailTo by remember { mutableStateOf("") }
    var emailSub by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var waPhone by remember { mutableStateOf("") }
    var waMsg by remember { mutableStateOf("") }
    var upiVpa by remember { mutableStateOf("") }
    var upiName by remember { mutableStateOf("") }
    var upiAmount by remember { mutableStateOf("") }
    var upiNote by remember { mutableStateOf("") }

    val colorPresets = listOf(
        "Classic" to ("#000000" to "#FFFFFF"),
        "Indigo" to ("#0F2C4C" to "#F9F7F3"),
        "Saffron" to ("#D85A00" to "#FFF8F2"),
        "Teal" to ("#006A60" to "#F2FBF9")
    )
    var colorIdx by remember { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedPayload by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val savePngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { dest ->
        val bmp = qrBitmap
        if (dest != null && bmp != null) {
            scope.launch {
                QrToolkitEngine.saveQrPng(context, bmp).onSuccess { f -> ExportHelper.saveFileToUri(context, f, dest) }
            }
        }
    }
    val saveSvgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { dest ->
        if (dest != null && generatedPayload.isNotBlank()) {
            val (_, pair) = colorPresets[colorIdx]
            QrToolkitEngine.generateQrSvg(generatedPayload, pair.first, pair.second).onSuccess { svg ->
                scope.launch { ExportHelper.saveTextToUri(context, svg, dest) }
            }
        }
    }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = if (isHindi) "QR कोड टूलकिट" else "QR Code Toolkit",
                subtitle = "PNG + Vector SVG • UPI, Wi-Fi, WhatsApp",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tabs) { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx; errorMsg = null },
                        label = { Text(label) },
                        modifier = Modifier.testTag("qr_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Website URL") }, modifier = Modifier.fillMaxWidth())
                1 -> OutlinedTextField(value = textInput, onValueChange = { textInput = it }, label = { Text("Plain Text") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                2 -> OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                3 -> {
                    OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Wi-Fi Name (SSID)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = wifiPass, onValueChange = { wifiPass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WPA", "WEP", "nopass").forEach { sec ->
                            FilterChip(selected = wifiSec == sec, onClick = { wifiSec = sec }, label = { Text(sec) })
                        }
                    }
                }
                4 -> {
                    OutlinedTextField(value = emailTo, onValueChange = { emailTo = it }, label = { Text("Recipient Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emailSub, onValueChange = { emailSub = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emailBody, onValueChange = { emailBody = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth())
                }
                5 -> {
                    OutlinedTextField(value = waPhone, onValueChange = { waPhone = it }, label = { Text("WhatsApp Number with country code (e.g. 9198...)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = waMsg, onValueChange = { waMsg = it }, label = { Text("Pre-filled Message") }, modifier = Modifier.fillMaxWidth())
                }
                6 -> {
                    OutlinedTextField(value = upiVpa, onValueChange = { upiVpa = it }, label = { Text("UPI ID / VPA (e.g. name@upi)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upiName, onValueChange = { upiName = it }, label = { Text("Payee Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = upiAmount, onValueChange = { upiAmount = it }, label = { Text("Amount (₹ optional)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = upiNote, onValueChange = { upiNote = it }, label = { Text("Note") }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Text("QR Color Theme", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(colorPresets) { idx, (name, _) ->
                    FilterChip(selected = colorIdx == idx, onClick = { colorIdx = idx }, label = { Text(name) })
                }
            }

            Button(
                onClick = {
                    val payload = when (selectedTab) {
                        0 -> QrToolkitEngine.buildUrlPayload(urlInput)
                        1 -> textInput.trim()
                        2 -> QrToolkitEngine.buildPhonePayload(phoneInput)
                        3 -> if (wifiSsid.isBlank()) "" else QrToolkitEngine.buildWifiPayload(wifiSsid, wifiPass, wifiSec)
                        4 -> if (emailTo.isBlank()) "" else QrToolkitEngine.buildEmailPayload(emailTo, emailSub, emailBody)
                        5 -> if (waPhone.isBlank()) "" else QrToolkitEngine.buildWhatsAppPayload(waPhone, waMsg)
                        else -> if (upiVpa.isBlank()) "" else QrToolkitEngine.buildUpiPayload(upiVpa, upiName, upiAmount, upiNote)
                    }
                    val (_, hexPair) = colorPresets[colorIdx]
                    val fg = AndroidColor.parseColor(hexPair.first)
                    val bg = AndroidColor.parseColor(hexPair.second)
                    QrToolkitEngine.generateQrBitmap(payload, 720, fg, bg)
                        .onSuccess { bmp ->
                            qrBitmap = bmp
                            generatedPayload = payload
                            errorMsg = null
                            onToolCompleted("qr_${tabs[selectedTab].lowercase()}", "${tabs[selectedTab]} QR", payload)
                        }
                        .onFailure { errorMsg = it.localizedMessage }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("qr_generate_btn")
            ) {
                Text("Generate ${tabs[selectedTab]} QR Code")
            }

            errorMsg?.let { err ->
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }

            qrBitmap?.let { bmp ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier.size(220.dp)
                            )
                        }
                        Text(generatedPayload, style = MaterialTheme.typography.bodySmall, fontFamily = MonoFontFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { savePngLauncher.launch("bharat_qr.png") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("PNG")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        QrToolkitEngine.saveQrPng(context, bmp).onSuccess { f ->
                                            ExportHelper.shareFile(context, f, "image/png", "Share QR Code")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                        if (hasProAccess) {
                            OutlinedButton(
                                onClick = { saveSvgLauncher.launch("bharat_qr.svg") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Download Vector SVG (Pro)")
                            }
                        } else {
                            OutlinedButton(onClick = onNavigateToProPass, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SVG Vector Export requires ₹399/mo Subscription")
                            }
                        }
                    }
                }
            }
        }
    }
}
