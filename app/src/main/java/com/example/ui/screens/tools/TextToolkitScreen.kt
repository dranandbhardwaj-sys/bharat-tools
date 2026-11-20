package com.example.ui.screens.tools

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.core.engine.DiffType
import com.example.core.engine.TextToolkitEngine
import com.example.ui.components.ToolTopBar
import com.example.ui.theme.MonoFontFamily
import java.util.Locale

@Composable
fun TextToolkitScreen(
    initialSubTab: Int,
    isHindi: Boolean,
    onToolCompleted: (toolId: String, title: String, summary: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tabs = listOf("Counters", "Case", "Clean & Reverse", "Compare", "Dedup", "Speech (TTS)", "Format")
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, tabs.lastIndex)) }
    var text by remember { mutableStateOf("") }
    var compareRight by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var ttsLang by remember { mutableStateOf("en") }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
        engine
    }
    DisposableEffect(Unit) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    val stats = remember(text) { TextToolkitEngine.analyze(text) }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = if (isHindi) "टेक्स्ट टूलकिट" else "Text Toolkit",
                subtitle = "${stats.words} words • ${stats.charsWithSpaces} chars",
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
                        onClick = { selectedTab = idx; statusMsg = null },
                        label = { Text(label) },
                        modifier = Modifier.testTag("txt_tab_$idx")
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (selectedTab == 3) "Original Text (A)" else "Enter or paste text") },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth().testTag("txt_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(text))
                        statusMsg = "Copied!"
                    },
                    enabled = text.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                OutlinedButton(onClick = { text = ""; compareRight = ""; statusMsg = null }) {
                    Text("Clear")
                }
            }

            when (selectedTab) {
                0 -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Words: ${stats.words}", style = MaterialTheme.typography.titleMedium)
                            Text("Characters (with spaces): ${stats.charsWithSpaces}", fontFamily = MonoFontFamily)
                            Text("Characters (no spaces): ${stats.charsNoSpaces}", fontFamily = MonoFontFamily)
                            Text("Sentences: ${stats.sentences} • Paragraphs: ${stats.paragraphs} • Lines: ${stats.lines}")
                            Text("Estimated Reading Time: ~${stats.readMinutes} min")
                        }
                    }
                }
                1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { text = text.uppercase(); onToolCompleted("txt_upper", "Uppercase", "${text.length} chars") }, modifier = Modifier.weight(1f)) { Text("UPPER") }
                        Button(onClick = { text = text.lowercase(); onToolCompleted("txt_lower", "Lowercase", "${text.length} chars") }, modifier = Modifier.weight(1f)) { Text("lower") }
                        Button(onClick = { text = TextToolkitEngine.toTitleCase(text); onToolCompleted("txt_title", "Title Case", "${text.length} chars") }, modifier = Modifier.weight(1f)) { Text("Title") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { text = TextToolkitEngine.toSentenceCase(text) }, modifier = Modifier.weight(1f)) { Text("Sentence case") }
                        OutlinedButton(onClick = { text = TextToolkitEngine.toToggleCase(text) }, modifier = Modifier.weight(1f)) { Text("tOGGLE cASE") }
                    }
                }
                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { text = TextToolkitEngine.removeExtraSpaces(text) }, modifier = Modifier.weight(1f)) { Text("Trim Spaces") }
                        Button(onClick = { text = TextToolkitEngine.cleanText(text, stripHtml = true) }, modifier = Modifier.weight(1f)) { Text("Strip HTML") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { text = TextToolkitEngine.cleanText(text, stripHtml = true, stripPunctuation = true) }, modifier = Modifier.weight(1f)) { Text("Clean Symbols") }
                        OutlinedButton(onClick = { text = TextToolkitEngine.reverseCharacters(text) }, modifier = Modifier.weight(1f)) { Text("Reverse Chars") }
                        OutlinedButton(onClick = { text = TextToolkitEngine.reverseWords(text) }, modifier = Modifier.weight(1f)) { Text("Reverse Words") }
                    }
                }
                3 -> {
                    OutlinedTextField(
                        value = compareRight,
                        onValueChange = { compareRight = it },
                        label = { Text("Modified Text (B)") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val diffs = remember(text, compareRight) { TextToolkitEngine.compareTexts(text, compareRight) }
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Diff Result", style = MaterialTheme.typography.titleSmall)
                            diffs.take(40).forEach { line ->
                                val bg = when (line.type) {
                                    DiffType.ADDED -> Color(0x282E7D32)
                                    DiffType.REMOVED -> Color(0x28D32F2F)
                                    DiffType.SAME -> Color.Transparent
                                }
                                val prefix = when (line.type) {
                                    DiffType.ADDED -> "+ "
                                    DiffType.REMOVED -> "- "
                                    DiffType.SAME -> "  "
                                }
                                Text(
                                    "$prefix${line.text}",
                                    fontFamily = MonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                4 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { text = TextToolkitEngine.removeDuplicateLines(text, caseSensitive = true, sortOutput = false) }, modifier = Modifier.weight(1f)) {
                            Text("Remove Duplicates")
                        }
                        OutlinedButton(onClick = { text = TextToolkitEngine.removeDuplicateLines(text, caseSensitive = false, sortOutput = true) }, modifier = Modifier.weight(1f)) {
                            Text("Dedup & Sort A-Z")
                        }
                    }
                }
                5 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ttsLang == "en", onClick = { ttsLang = "en" }, label = { Text("English") })
                        FilterChip(selected = ttsLang == "hi", onClick = { ttsLang = "hi" }, label = { Text("हिन्दी (Hindi)") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (ttsReady && text.isNotBlank()) {
                                    tts.language = if (ttsLang == "hi") Locale("hi", "IN") else Locale.ENGLISH
                                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bharat_tts")
                                    onToolCompleted("txt_tts", "Text-to-Speech", "Spoke ${stats.words} words ($ttsLang)")
                                } else {
                                    statusMsg = "Enter text first or wait for TTS engine."
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("tts_speak_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Speak")
                        }
                        OutlinedButton(onClick = { tts.stop() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }
                6 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { text = TextToolkitEngine.formatAddLineNumbers(text) }, modifier = Modifier.weight(1f)) { Text("1. Numbering") }
                        Button(onClick = { text = TextToolkitEngine.formatBullets(text) }, modifier = Modifier.weight(1f)) { Text("• Bullets") }
                        OutlinedButton(
                            onClick = {
                                TextToolkitEngine.formatPrettyJson(text)
                                    .onSuccess { text = it; statusMsg = "Formatted JSON" }
                                    .onFailure { statusMsg = it.localizedMessage }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("{ } JSON") }
                    }
                }
            }

            statusMsg?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
