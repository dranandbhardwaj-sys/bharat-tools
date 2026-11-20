package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ToolCategory
import com.example.data.model.ToolDefinition
import com.example.ui.theme.BharatTheme

fun categoryIcon(cat: ToolCategory): ImageVector = when (cat) {
    ToolCategory.PDF -> Icons.Default.PictureAsPdf
    ToolCategory.IMAGE -> Icons.Default.Image
    ToolCategory.TEXT -> Icons.Default.TextFields
    ToolCategory.QR -> Icons.Default.QrCode2
    ToolCategory.UTILITY -> Icons.Default.Calculate
}

@Composable
fun BharatBrandHeroBanner(
    title: String,
    subtitle: String,
    isPro: Boolean,
    isHindi: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF122359), Color(0xFF1E3A8A), Color(0xFF3B1F7A))
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color(0x363B82F6),
                radius = size.height * 0.72f,
                center = Offset(size.width * 0.90f, size.height * 0.16f)
            )
            drawCircle(
                color = Color(0x28A78BFA),
                radius = size.height * 0.55f,
                center = Offset(size.width * 0.74f, size.height * 0.96f)
            )
            val step = 28.dp.toPx()
            var x = size.width * 0.55f
            while (x < size.width) {
                drawLine(
                    color = Color(0x14FFFFFF),
                    start = Offset(x, 0f),
                    end = Offset(x + 24f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
        }

        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (isPro) Color(0xFF16A34A) else Color(0xFF2563EB),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPro) "ACTIVE SUBSCRIPTION" else "₹399/MONTH PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = if (isHindi) "ऑन-डिवाइस प्राइवेसी" else "On-Device Privacy",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDBEAFE)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE0E7FF)
            )
            if (!isPro) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    onClick = onUpgradeClick,
                    color = Color(0x28FFFFFF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x40FFFFFF)),
                    modifier = Modifier.testTag("hero_upgrade_button")
                ) {
                    Text(
                        text = if (isHindi) "₹399/माह सदस्यता शुरू करें →" else "Start ₹399/month Membership →",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolGridCard(
    tool: ToolDefinition,
    isHindi: Boolean,
    isFavorite: Boolean,
    hasProAccess: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = tool.proOnly && !hasProAccess
    val tokens = BharatTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bharatPressScale(interactionSource)
            .testTag("tool_card_${tool.id}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.category.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(tool.category),
                        contentDescription = null,
                        tint = tool.category.accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tool.proOnly) {
                        BharatBadge(
                            text = "PRO",
                            tone = if (locked) BadgeTone.ACCENT else BadgeTone.SUCCESS,
                            icon = if (locked) Icons.Default.Lock else null
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fav_btn_${tool.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Toggle favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else tokens.textSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isHindi) tool.titleHi else tool.titleEn,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (isHindi) tool.descHi else tool.descEn,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) tool.category.titleHi.substringBefore(" ") else tool.category.id.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tool.category.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHindi) "खोलें" else "Open",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = BharatTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tool_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BharatTheme.colors.surface
        )
    )
}
