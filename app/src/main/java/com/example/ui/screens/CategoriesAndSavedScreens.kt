package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.UiStrings
import com.example.data.catalog.ToolCatalog
import com.example.data.model.ToolCategory
import com.example.data.model.ToolDefinition
import com.example.data.model.ToolHistory
import com.example.data.model.UserProfile
import com.example.ui.components.BadgeTone
import com.example.ui.components.BharatBadge
import com.example.ui.components.BharatCard
import com.example.ui.components.BharatEmptyState
import com.example.ui.components.ToolGridCard
import com.example.ui.components.categoryIcon
import com.example.ui.theme.BharatTheme
import com.example.ui.theme.MonoFontFamily

@Composable
fun CategoriesScreen(
    isHindi: Boolean,
    strings: UiStrings,
    onSelectCategoryToBrowse: (ToolCategory) -> Unit
) {
    val tokens = BharatTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.categoriesTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isHindi) "5 समर्पित श्रेणियों में 60 संपूर्ण कार्यशील टूल" else "60 functional tools organized across 5 dedicated toolkits",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textSecondary
            )
        }
        items(ToolCategory.entries) { cat ->
            val count = remember(cat) { ToolCatalog.allTools.count { it.category == cat } }
            BharatCard(
                onClick = { onSelectCategoryToBrowse(cat) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_card_${cat.id}")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cat.accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon(cat), contentDescription = null, tint = cat.accentColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (isHindi) cat.titleHi else cat.titleEn, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (isHindi) cat.subtitleHi else cat.subtitleEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                        BharatBadge(text = "$count tools", tone = BadgeTone.PRIMARY)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open category", tint = tokens.textSecondary)
                }
            }
        }
    }
}

@Composable
fun SavedAndHistoryScreen(
    profile: UserProfile,
    history: List<ToolHistory>,
    strings: UiStrings,
    onOpenTool: (ToolDefinition) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val isHindi = profile.preferredLanguage == "hi"
    val tokens = BharatTheme.colors
    val favTools = remember(profile.favorites) { profile.favorites.mapNotNull { ToolCatalog.byId(it) } }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Tool History?") },
            text = { Text("This removes your recent tool history log from your account while keeping your pinned favorites.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear History") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = tokens.warning)
                Spacer(Modifier.width(8.dp))
                Text(strings.favoritesTitle, style = MaterialTheme.typography.headlineSmall)
            }
        }

        if (favTools.isEmpty()) {
            item {
                BharatEmptyState(
                    title = if (isHindi) "कोई पसंदीदा टूल नहीं" else "No Favorites Yet",
                    subtitle = strings.noFavoritesYet,
                    icon = Icons.Default.Star
                )
            }
        } else {
            items(favTools, key = { "saved_${it.id}" }) { tool ->
                ToolGridCard(
                    tool = tool,
                    isHindi = isHindi,
                    isFavorite = true,
                    hasProAccess = profile.hasProAccess,
                    onClick = { onOpenTool(tool) },
                    onToggleFavorite = { onToggleFavorite(tool.id) }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = tokens.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.recentlyUsed, style = MaterialTheme.typography.headlineSmall)
                }
                if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                BharatEmptyState(
                    title = if (isHindi) "अभी तक कोई इतिहास नहीं" else "No Recent Activity",
                    subtitle = strings.noHistoryYet,
                    icon = Icons.Default.History
                )
            }
        } else {
            items(history, key = { it.id }) { entry ->
                val matchedTool = remember(entry.toolId) { ToolCatalog.byId(entry.toolId) }
                BharatCard(
                    onClick = matchedTool?.let { t -> { onOpenTool(t) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(entry.summary, style = MaterialTheme.typography.bodySmall, fontFamily = MonoFontFamily, color = tokens.textSecondary)
                        }
                        if (matchedTool != null) {
                            Spacer(Modifier.width(8.dp))
                            BharatBadge(text = "Restore", tone = BadgeTone.PRIMARY, icon = Icons.Default.Restore)
                        }
                    }
                }
            }
        }
    }
}
