package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.i18n.UiStrings
import com.example.data.catalog.ToolCatalog
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.ToolCategory
import com.example.data.model.ToolDefinition
import com.example.data.model.UserProfile
import com.example.ui.components.BadgeTone
import com.example.ui.components.BharatBadge
import com.example.ui.components.BharatBrandHeroBanner
import com.example.ui.components.BharatCard
import com.example.ui.components.BharatEmptyState
import com.example.ui.components.BharatSectionReveal
import com.example.ui.components.ToolGridCard
import com.example.ui.components.categoryIcon
import com.example.ui.theme.BharatTheme
import com.example.ui.theme.MonoFontFamily

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    announcements: List<Announcement>,
    appConfig: AppConfig,
    strings: UiStrings,
    searchQuery: String,
    selectedCategory: ToolCategory?,
    recentSearches: List<String>,
    totalHistoryCount: Int,
    onSearchChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onSelectCategory: (ToolCategory?) -> Unit,
    onOpenTool: (ToolDefinition) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onUpgradeClick: () -> Unit
) {
    val isHindi = profile.preferredLanguage == "hi"
    val tokens = BharatTheme.colors
    val focusManager = LocalFocusManager.current

    val filteredTools = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        ToolCatalog.allTools.filter { tool ->
            val catMatch = selectedCategory == null || tool.category == selectedCategory
            val qMatch = q.isEmpty() ||
                tool.titleEn.lowercase().contains(q) ||
                tool.titleHi.lowercase().contains(q) ||
                tool.descEn.lowercase().contains(q) ||
                tool.descHi.lowercase().contains(q) ||
                tool.category.titleEn.lowercase().contains(q)
            catMatch && qMatch
        }
    }
    val popularTools = remember { ToolCatalog.allTools.filter { it.popular } }
    val recentToolObjs = remember(profile.recentTools) {
        profile.recentTools.mapNotNull { ToolCatalog.byId(it) }.take(8)
    }
    val favoriteToolObjs = remember(profile.favorites) {
        profile.favorites.mapNotNull { ToolCatalog.byId(it) }.take(8)
    }
    val quickActionTools = remember {
        listOfNotNull(
            ToolCatalog.byId("pdf_merge"),
            ToolCatalog.byId("img_compress"),
            ToolCatalog.byId("qr_upi"),
            ToolCatalog.byId("util_gst")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 960.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (appConfig.maintenanceMode) {
                item {
                    Surface(
                        color = tokens.dangerContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Maintenance notice: Cloud sync may be temporarily slower. All on-device tools remain available.",
                            color = tokens.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                BharatSectionReveal {
                    BharatBrandHeroBanner(
                        title = "${strings.appName} • ${profile.displayName.substringBefore(" ")}",
                        subtitle = strings.tagline,
                        isPro = profile.hasProAccess,
                        isHindi = isHindi,
                        onUpgradeClick = onUpgradeClick
                    )
                }
            }

            // Usage Activity & Membership Status Strip
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BharatCard(
                        modifier = Modifier.weight(1f),
                        onClick = onUpgradeClick
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isHindi) "सदस्यता" else "Membership", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                            val planLabel = when {
                                profile.hasProAccess -> "ACTIVE (${profile.daysRemaining}d)"
                                profile.subscriptionStatus == "pending" -> "PENDING"
                                profile.subscriptionStatus == "expired" || profile.isSubscriptionExpiredByDate -> "EXPIRED"
                                profile.subscriptionStatus == "cancelled" -> "CANCELLED"
                                else -> "FREE PLAN"
                            }
                            BharatBadge(
                                text = planLabel,
                                tone = if (profile.hasProAccess) BadgeTone.SUCCESS else BadgeTone.PRIMARY
                            )
                        }
                    }
                    BharatCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isHindi) "उपयोग गतिविधि" else "Activity", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = tokens.secondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("$totalHistoryCount runs", style = MaterialTheme.typography.titleSmall, fontFamily = MonoFontFamily)
                            }
                        }
                    }
                    BharatCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isHindi) "पसंदीदा" else "Favorites", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = tokens.warning, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${profile.favorites.size} pinned", style = MaterialTheme.typography.titleSmall, fontFamily = MonoFontFamily)
                            }
                        }
                    }
                }
            }

            if (profile.subscriptionStatus == "expired" ||
                profile.subscriptionStatus == "cancelled" ||
                profile.isSubscriptionExpiredByDate ||
                (profile.hasProAccess && profile.daysRemaining in 1L..5L)
            ) {
                item {
                    val reminderText = when {
                        profile.subscriptionStatus == "expired" || profile.isSubscriptionExpiredByDate ->
                            if (isHindi) "आपकी ₹399/माह सदस्यता समाप्त हो गई है। प्रो टूल जारी रखने के लिए नवीनीकरण करें।"
                            else "Your ₹399/month subscription has expired. Renew with your consent to restore Pro tool access."
                        profile.subscriptionStatus == "cancelled" ->
                            if (isHindi) "आपकी सदस्यता रद्द कर दी गई है। आप कभी भी ₹399/माह प्लान पुनः सक्रिय कर सकते हैं।"
                            else "Your ₹399/month subscription is cancelled. Re-subscribe anytime from Membership."
                        else ->
                            if (isHindi) "नवीनीकरण अनुस्मारक: आपकी मासिक सदस्यता में ${profile.daysRemaining} दिन शेष हैं।"
                            else "Renewal Reminder: ${profile.daysRemaining} day(s) remaining in your ₹399/month billing cycle."
                    }
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onUpgradeClick)
                    ) {
                        Text(
                            text = reminderText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (announcements.isNotEmpty()) {
                item {
                    val latest = announcements.first()
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (isHindi && latest.titleHi.isNotBlank()) latest.titleHi else latest.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    if (isHindi && latest.bodyHi.isNotBlank()) latest.bodyHi else latest.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Global Search + Recent Searches
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search tools") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clearSearch)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onSubmitSearch(searchQuery)
                            focusManager.clearFocus()
                        }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.surface,
                            unfocusedContainerColor = tokens.surface,
                            focusedBorderColor = tokens.secondary,
                            unfocusedBorderColor = tokens.border
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("home_search_input")
                    )

                    if (recentSearches.isNotEmpty() && searchQuery.isBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (isHindi) "हाल की खोजें" else "Recent Searches",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                            TextButton(onClick = onClearRecentSearches) {
                                Text(if (isHindi) "साफ़ करें" else "Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentSearches) { q ->
                                AssistChip(
                                    onClick = { onSearchChange(q) },
                                    label = { Text(q) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            if (searchQuery.isBlank() && selectedCategory == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isHindi) "त्वरित क्रियाएं" else "Quick Actions", style = MaterialTheme.typography.titleMedium)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickActionTools, key = { "qa_${it.id}" }) { tool ->
                                Surface(
                                    onClick = { onOpenTool(tool) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = tokens.surface,
                                    border = BorderStroke(1.dp, tokens.border)
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(categoryIcon(tool.category), contentDescription = null, tint = tool.category.accentColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isHindi) tool.titleHi else tool.titleEn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text(strings.allTools) },
                            modifier = Modifier.testTag("cat_chip_all")
                        )
                    }
                    items(ToolCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                            leadingIcon = {
                                Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (isHindi) cat.titleHi else cat.titleEn) },
                            modifier = Modifier.testTag("cat_chip_${cat.id}")
                        )
                    }
                }
            }

            if (searchQuery.isBlank() && selectedCategory == null) {
                item {
                    Text(strings.popularTools, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(popularTools, key = { it.id }) { tool ->
                            ToolGridCard(
                                tool = tool,
                                isHindi = isHindi,
                                isFavorite = tool.id in profile.favorites,
                                hasProAccess = profile.hasProAccess,
                                onClick = { onOpenTool(tool) },
                                onToggleFavorite = { onToggleFavorite(tool.id) },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }

                if (recentToolObjs.isNotEmpty()) {
                    item {
                        Text(strings.recentlyUsed, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentToolObjs, key = { "rec_${it.id}" }) { tool ->
                                Surface(
                                    onClick = { onOpenTool(tool) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = tokens.surfaceSoft,
                                    border = BorderStroke(1.dp, tokens.border)
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(categoryIcon(tool.category), contentDescription = null, tint = tool.category.accentColor, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isHindi) tool.titleHi else tool.titleEn, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                if (favoriteToolObjs.isNotEmpty()) {
                    item {
                        Text(strings.favoritesTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(favoriteToolObjs, key = { "fav_${it.id}" }) { tool ->
                                ToolGridCard(
                                    tool = tool,
                                    isHindi = isHindi,
                                    isFavorite = true,
                                    hasProAccess = profile.hasProAccess,
                                    onClick = { onOpenTool(tool) },
                                    onToggleFavorite = { onToggleFavorite(tool.id) },
                                    modifier = Modifier.width(200.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedCategory?.let { if (isHindi) it.titleHi else it.titleEn } ?: strings.allTools,
                        style = MaterialTheme.typography.titleLarge
                    )
                    BharatBadge(text = "${filteredTools.size} tools", tone = BadgeTone.NEUTRAL)
                }
            }

            if (filteredTools.isEmpty()) {
                item {
                    BharatEmptyState(
                        title = "No matching tools found",
                        subtitle = "Try searching for PDF, GST, UPI QR, EMI, or clear your filter.",
                        icon = Icons.Default.SearchOff,
                        actionLabel = strings.clearSearch,
                        onAction = {
                            onSearchChange("")
                            onSelectCategory(null)
                        }
                    )
                }
            } else {
                val chunked = filteredTools.chunked(columns)
                items(chunked) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { tool ->
                            ToolGridCard(
                                tool = tool,
                                isHindi = isHindi,
                                isFavorite = tool.id in profile.favorites,
                                hasProAccess = profile.hasProAccess,
                                onClick = {
                                    if (searchQuery.isNotBlank()) onSubmitSearch(searchQuery)
                                    onOpenTool(tool)
                                },
                                onToggleFavorite = { onToggleFavorite(tool.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
