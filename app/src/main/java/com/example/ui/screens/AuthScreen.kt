package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.auth.GoogleSignInButton
import com.example.core.i18n.AppStrings
import com.example.ui.components.BadgeTone
import com.example.ui.components.BharatBadge
import com.example.ui.components.BharatButton
import com.example.ui.components.BharatCard
import com.example.ui.components.rememberReducedMotion
import com.example.ui.theme.BharatTheme
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class LandingPreviewCategory(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tools: List<Pair<String, String>>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val tokens = BharatTheme.colors
    val strings = AppStrings.english
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedPreviewIdx by remember { mutableIntStateOf(0) }
    var infoDialogTopic by remember { mutableStateOf<String?>(null) }
    var showLoginModal by remember { mutableStateOf(false) }

    var toolsSectionY by remember { mutableIntStateOf(0) }
    var featuresSectionY by remember { mutableIntStateOf(0) }
    var pricingSectionY by remember { mutableIntStateOf(0) }
    var aboutSectionY by remember { mutableIntStateOf(0) }

    val floatingOffset = if (reducedMotion) {
        0f
    } else {
        val inf = rememberInfiniteTransition(label = "heroFloat")
        val y by inf.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatY"
        )
        y
    }

    val previewCategories = remember {
        listOf(
            LandingPreviewCategory(
                "PDF", Icons.Default.PictureAsPdf, Color(0xFFDC2626),
                listOf("PDF Merge" to "Combine documents cleanly", "PDF Compress" to "Reduce size on-device", "PDF Protect" to "128-bit password lock")
            ),
            LandingPreviewCategory(
                "Image", Icons.Default.Image, Color(0xFF2563EB),
                listOf("Image Compressor" to "Quality-guided compression", "WebP Converter" to "Modern fast export", "Backdrop Tool" to "Solid color & cutout")
            ),
            LandingPreviewCategory(
                "Text", Icons.Default.TextFields, Color(0xFF1E3A8A),
                listOf("Word Counter" to "Reading time & stats", "Text Compare" to "Line-by-line diff", "Text-to-Speech" to "EN & HI voice output")
            ),
            LandingPreviewCategory(
                "QR", Icons.Default.QrCode2, Color(0xFF7C3AED),
                listOf("UPI ₹ QR" to "BHIM & VPA payment QR", "Wi-Fi QR" to "Instant guest login", "SVG Vector" to "Scalable crisp export")
            ),
            LandingPreviewCategory(
                "Calculator", Icons.Default.Calculate, Color(0xFF0D9488),
                listOf("GST Calculator" to "CGST / SGST breakdown", "EMI Calculator" to "Monthly loan schedule", "Currency FX" to "Live ECB exchange rates")
            ),
            LandingPreviewCategory(
                "Productivity", Icons.Default.Bolt, Color(0xFFD97706),
                listOf("Password Gen" to "CSPRNG strong secrets", "Unit Converter" to "Gaj, Bigha, Metric", "Focus Timer" to "Countdown & stopwatch")
            )
        )
    }

    infoDialogTopic?.let { topic ->
        val (title, body) = when (topic) {
            "About" -> "About Bharat Tools" to "Bharat Tools is a privacy-first digital utility platform for students, professionals, creators, and small businesses across India and globally. Core PDF, Image, Text, QR, and calculator workflows execute directly on your device."
            "Privacy Policy" -> "Privacy Policy" to "Files you select in PDF, Image, Text, and QR tools are processed locally on your device using Android Photo Picker and Storage Access Framework without uploading documents to external servers. Account preferences, favorites, history, and subscription records are stored securely per authenticated UID in Cloud Firestore."
            "Terms" -> "Subscription & Access Terms" to "Core utilities are free to use. The Bharat Tools Monthly Plan is ₹399/month recurring, activated strictly after server verification of your user-approved UPI Autopay or payment-provider mandate. Subscriptions renew monthly only with user consent and can be cancelled anytime in Account settings."
            "Refund Policy" -> "Refund Policy" to "Active ₹399/month subscriptions include a clearly disclosed 7-day refund request flow in Billing History. Approved refunds revert paid subscriber privileges cleanly at verification."
            else -> "Contact & Support" to "Need billing, technical, or account help? Sign in to open a tracked support ticket or email support@bharattools.in."
        }
        AlertDialog(
            onDismissRequest = { infoDialogTopic = null },
            title = { Text(title) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { infoDialogTopic = null }) { Text("Close") }
            }
        )
    }

    if (showLoginModal) {
        AlertDialog(
            onDismissRequest = { showLoginModal = false },
            title = { Text("Sign in to Bharat Tools") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Continue with your Google account to unlock your synced dashboard, favorites, history, and optional ₹399/month membership.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    GoogleSignInButton(
                        label = strings.signInButton,
                        onAuthSuccess = {
                            errorMessage = null
                            showLoginModal = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg -> errorMessage = msg }
                    )
                    errorMessage?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLoginModal = false }) { Text("Cancel") }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. SaaS Header Bar
            Surface(
                color = tokens.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1120.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Bharat Tools Logo", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Bharat Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("₹399/month", style = MaterialTheme.typography.labelSmall, color = tokens.secondary, fontFamily = MonoFontFamily)
                        }
                    }

                    if (isWide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { scope.launch { scrollState.animateScrollTo(toolsSectionY) } }) { Text("Tools") }
                            TextButton(onClick = { scope.launch { scrollState.animateScrollTo(featuresSectionY) } }) { Text("Features") }
                            TextButton(onClick = { scope.launch { scrollState.animateScrollTo(pricingSectionY) } }) { Text("Pricing") }
                            TextButton(onClick = { scope.launch { scrollState.animateScrollTo(aboutSectionY) } }) { Text("About") }
                            TextButton(onClick = { showLoginModal = true }) { Text("Login") }
                            BharatButton(text = "Get Started", onClick = { showLoginModal = true })
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { showLoginModal = true }) { Text("Login") }
                            Box {
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("landing_menu_btn")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Navigation Menu")
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Tools") }, onClick = {
                                        menuExpanded = false; scope.launch { scrollState.animateScrollTo(toolsSectionY) }
                                    })
                                    DropdownMenuItem(text = { Text("Features") }, onClick = {
                                        menuExpanded = false; scope.launch { scrollState.animateScrollTo(featuresSectionY) }
                                    })
                                    DropdownMenuItem(text = { Text("Pricing (₹399/mo)") }, onClick = {
                                        menuExpanded = false; scope.launch { scrollState.animateScrollTo(pricingSectionY) }
                                    })
                                    DropdownMenuItem(text = { Text("About") }, onClick = {
                                        menuExpanded = false; infoDialogTopic = "About"
                                    })
                                    DropdownMenuItem(text = { Text("Get Started") }, onClick = {
                                        menuExpanded = false; showLoginModal = true
                                    })
                                }
                            }
                        }
                    }
                }
            }

            // Main Centered Container
            Column(
                modifier = Modifier
                    .widthIn(max = 1080.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // 2. Hero Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF101F52), Color(0xFF1E3A8A), Color(0xFF43238A))
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(color = Color(0x333B82F6), radius = size.height * 0.65f, center = Offset(size.width * 0.88f, size.height * 0.15f))
                        drawCircle(color = Color(0x28A78BFA), radius = size.height * 0.5f, center = Offset(size.width * 0.15f, size.height * 0.92f))
                    }

                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BharatBadge(
                            text = "60+ ON-DEVICE UTILITIES • ₹399/MONTH",
                            tone = BadgeTone.PRIMARY,
                            icon = Icons.Default.AutoAwesome
                        )
                        Text(
                            text = "Every Digital Tool. One Smart Platform.",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transform your everyday digital tasks with powerful, simple and smart tools.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE0E7FF)
                        )

                        // Primary Google Sign-In + Explore CTAs
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoogleSignInButton(
                                label = strings.signInButton,
                                onAuthSuccess = {
                                    errorMessage = null
                                    onAuthSuccess()
                                },
                                onAuthError = { msg -> errorMessage = msg }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                BharatButton(
                                    text = "Explore Tools",
                                    onClick = { scope.launch { scrollState.animateScrollTo(toolsSectionY) } },
                                    containerColor = Color(0xFF2563EB),
                                    modifier = Modifier.weight(1f).testTag("landing_explore_btn")
                                )
                                BharatButton(
                                    text = "Start Membership",
                                    onClick = { scope.launch { scrollState.animateScrollTo(pricingSectionY) } },
                                    containerColor = Color(0xFF7C3AED),
                                    modifier = Modifier.weight(1f).testTag("landing_membership_btn")
                                )
                            }
                        }

                        errorMessage?.let { err ->
                            Surface(
                                color = tokens.dangerContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = tokens.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                        }

                        // Subtle floating preview pills
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.offset { IntOffset(0, floatingOffset.roundToInt()) }
                        ) {
                            listOf("PDF Merge & Lock", "UPI ₹ QR Generator", "GST & EMI Calc", "Image WebP & Crop").forEach { chip ->
                                Surface(
                                    color = Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x38FFFFFF))
                                ) {
                                    Text(
                                        chip,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Interactive Tool Preview Section
                Column(
                    modifier = Modifier.onGloballyPositioned { toolsSectionY = it.positionInParent().y.roundToInt() },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Interactive Tool Preview", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Explore ready-to-run utilities across PDF, Image, Text, QR, Calculators, and Productivity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(previewCategories.size) { idx ->
                            val cat = previewCategories[idx]
                            FilterChip(
                                selected = selectedPreviewIdx == idx,
                                onClick = { selectedPreviewIdx = idx },
                                leadingIcon = { Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    val activeCat = previewCategories[selectedPreviewIdx]
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeCat.tools.forEach { (title, desc) ->
                            BharatCard(onClick = { showLoginModal = true }) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(activeCat.color.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(activeCat.icon, contentDescription = null, tint = activeCat.color)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleMedium)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                                    }
                                    BharatBadge(text = "Sign in to run", tone = BadgeTone.PRIMARY)
                                }
                            }
                        }
                    }
                }

                // 4. Benefits Section
                Column(
                    modifier = Modifier.onGloballyPositioned { featuresSectionY = it.positionInParent().y.roundToInt() },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Built for Speed, Privacy & Clarity", style = MaterialTheme.typography.headlineMedium)
                    val benefits = listOf(
                        Triple("Fast", "Zero round-trips for PDF, Image, Text & QR tools.", Icons.Default.Bolt),
                        Triple("Secure", "Zero-trust Firestore rules & Google Sign-In.", Icons.Default.Security),
                        Triple("Easy to use", "Clean distraction-free workspaces with clear controls.", Icons.Default.CheckCircle),
                        Triple("Mobile friendly", "Touch-friendly 48dp targets & responsive grids.", Icons.Default.PhoneAndroid),
                        Triple("Privacy focused", "Documents stay on your Android device.", Icons.Default.VerifiedUser),
                        Triple("Smart utilities", "Live ECB currency rates, GST, EMI & bilingual EN/HI.", Icons.Default.AutoAwesome)
                    )
                    benefits.chunked(if (isWide) 3 else 2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { (bTitle, bDesc, bIcon) ->
                                BharatCard(modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(bIcon, contentDescription = null, tint = tokens.secondary, modifier = Modifier.size(22.dp))
                                        Text(bTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(bDesc, style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Transparent Monthly Pricing Section (₹399/month)
                Column(
                    modifier = Modifier.onGloballyPositioned { pricingSectionY = it.positionInParent().y.roundToInt() },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Transparent Monthly Membership", style = MaterialTheme.typography.headlineMedium)
                    BharatCard(borderColor = tokens.secondary.copy(alpha = 0.45f)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bharat Tools Monthly Plan", style = MaterialTheme.typography.titleLarge)
                                BharatBadge(text = "RECURRING", tone = BadgeTone.ACCENT)
                            }
                            Text("₹399 / month", style = MaterialTheme.typography.displaySmall, fontFamily = MonoFontFamily, color = tokens.primary)
                            Text(
                                "• Includes all 60 tools plus Pro PDF Encrypt/Unlock, Image Backdrop Utility, and Vector SVG QR Export.\n" +
                                "• Core utilities remain free; Pro features require an Active server-verified subscription.\n" +
                                "• Limits: Up to 25 MB per PDF on-device for fast mobile memory performance.\n" +
                                "• User-approved billing, instant cancellation anytime, renewal reminders, and a 7-day refund policy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            BharatButton(
                                text = "Subscribe Now • ₹399/month",
                                onClick = { showLoginModal = true },
                                icon = Icons.Default.Lock,
                                containerColor = tokens.accent,
                                modifier = Modifier.fillMaxWidth().testTag("landing_subscribe_btn")
                            )
                        }
                    }
                }

                // 6. Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { aboutSectionY = it.positionInParent().y.roundToInt() },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = tokens.border)
                    Text("Bharat Tools • Every Digital Tool. One Smart Platform.", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("About", "Contact", "Privacy Policy", "Terms", "Refund Policy", "Support").forEach { link ->
                            Text(
                                text = link,
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.secondary,
                                modifier = Modifier
                                    .clickable { infoDialogTopic = link }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
