package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.core.auth.signOutUser
import com.example.core.i18n.UiStrings
import com.example.data.model.AppConfig
import com.example.data.model.PurchaseRecord
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.ui.theme.MonoFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileAndPurchaseScreen(
    profile: UserProfile,
    purchases: List<PurchaseRecord>,
    tickets: List<SupportTicket>,
    appConfig: AppConfig,
    isAdmin: Boolean,
    strings: UiStrings,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSubmitSubscriptionVerification: (method: String, refId: String, consentAccepted: Boolean, notes: String) -> Unit,
    onCancelSubscription: (activePurchaseId: String?) -> Unit,
    onSetAutoRenewConsent: (Boolean) -> Unit,
    onRequestRefund: (purchaseId: String, reason: String) -> Unit,
    onSubmitSupportTicket: (category: String, subject: String, message: String) -> Unit,
    onApplyReferral: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var paymentMethod by remember { mutableStateOf("upi_autopay") }
    var utrInput by remember { mutableStateOf("") }
    var purchaseNotes by remember { mutableStateOf("") }
    var consentAccepted by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var referralInput by remember { mutableStateOf("") }
    var ticketCategory by remember { mutableStateOf("bug") }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketMsg by remember { mutableStateOf("") }

    val activePurchase = remember(purchases) { purchases.firstOrNull { it.status == "active" } }
    val pendingPurchase = remember(purchases) { purchases.firstOrNull { it.status == "pending" } }
    val latestFailedPurchase = remember(purchases) { purchases.firstOrNull()?.takeIf { it.status == "failed" } }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel ₹399/month Subscription?") },
            text = {
                Text("Your recurring billing consent will be turned off immediately, and your subscription status will update to Cancelled with no further monthly charges.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelSubscription(activePurchase?.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_cancel_sub_btn")
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Plan")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile & Subscription Status Badge
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            BoxIconInitial(profile.displayName)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        val statusLabel = when {
                            profile.hasProAccess -> "ACTIVE • ₹${appConfig.priceInr}/MO (${profile.daysRemaining}d left)"
                            profile.subscriptionStatus == "expired" || profile.isSubscriptionExpiredByDate -> "EXPIRED"
                            profile.subscriptionStatus == "cancelled" -> "CANCELLED"
                            profile.subscriptionStatus == "pending" || pendingPurchase != null -> "PENDING VERIFICATION"
                            else -> "FREE PLAN"
                        }
                        Surface(
                            color = if (profile.hasProAccess) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Renewal Reminder / Expiry Banner
        if (profile.subscriptionStatus == "expired" ||
            profile.subscriptionStatus == "cancelled" ||
            profile.isSubscriptionExpiredByDate ||
            (profile.hasProAccess && profile.daysRemaining in 1L..5L)
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        val msg = when {
                            profile.subscriptionStatus == "expired" || profile.isSubscriptionExpiredByDate ->
                                "Subscription Expired: Paid Pro features are paused according to subscription terms. Submit a user-approved ₹399/month renewal below to restore access."
                            profile.subscriptionStatus == "cancelled" ->
                                "Subscription Cancelled: Recurring billing is off. You can re-subscribe anytime with explicit consent."
                            else ->
                                "Renewal Reminder: Your ₹399/month cycle ends in ${profile.daysRemaining} day(s)."
                        }
                        Text(msg, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Admin Console Shortcut (Verified Admins Only)
        if (isAdmin) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(strings.adminDashboard, style = MaterialTheme.typography.titleMedium)
                            Text("Subscribers, ₹399/mo verification, MRR vs Net Profit & config", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onOpenAdmin, modifier = Modifier.testTag("open_admin_btn")) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Language & Theme Preferences
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = profile.preferredLanguage == "en",
                            onClick = { onSetLanguage("en") },
                            label = { Text("English") },
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                        FilterChip(
                            selected = profile.preferredLanguage == "hi",
                            onClick = { onSetLanguage("hi") },
                            label = { Text("हिन्दी (Hindi)") },
                            modifier = Modifier.testTag("lang_hi_chip")
                        )
                    }
                    Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = profile.themeMode == "system", onClick = { onSetTheme("system") }, label = { Text(strings.themeSystem) })
                        FilterChip(selected = profile.themeMode == "light", onClick = { onSetTheme("light") }, label = { Text(strings.themeLight) })
                        FilterChip(selected = profile.themeMode == "dark", onClick = { onSetTheme("dark") }, label = { Text(strings.themeDark) })
                    }
                }
            }
        }

        // ₹399/month Recurring Subscription Management & Server-Verified Billing
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.subscriptionTitle} • ${strings.priceMonthly}", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(strings.subscriptionTermsShort, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Billing & Refund Policy: Card details are never stored. Paid access activates strictly after server verification of your UPI Autopay or payment-provider mandate — never from an unverified client response. Eligible 7-day refund requests may be submitted from Billing History.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (profile.hasProAccess) {
                        val endLabel = profile.subscriptionPeriodEnd?.toDate()?.let { dateFmt.format(it) } ?: "Active Cycle"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Current Billing Cycle Ends: $endLabel (${profile.daysRemaining}d left)", style = MaterialTheme.typography.labelLarge)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("User-Approved Monthly Renewal", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = profile.autoRenewConsent,
                                        onCheckedChange = onSetAutoRenewConsent,
                                        modifier = Modifier.testTag("auto_renew_switch")
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("cancel_sub_btn")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel Monthly Subscription")
                        }
                    } else {
                        if (latestFailedPurchase != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Payment Failed: Reference ${latestFailedPurchase.referenceId} could not be verified. Please retry with a valid UPI Autopay mandate or gateway reference.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (pendingPurchase != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Pending Verification: ${pendingPurchase.referenceId} (${pendingPurchase.periodLabel}). Pro features unlock once server verification completes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = paymentMethod == "upi_autopay", onClick = { paymentMethod = "upi_autopay" }, label = { Text("UPI Autopay") })
                            FilterChip(selected = paymentMethod == "gateway_mandate", onClick = { paymentMethod = "gateway_mandate" }, label = { Text("Gateway Mandate") })
                            FilterChip(selected = paymentMethod == "upi_reference", onClick = { paymentMethod = "upi_reference" }, label = { Text("UPI UTR") })
                        }
                        OutlinedTextField(
                            value = utrInput,
                            onValueChange = { utrInput = it },
                            label = { Text("UPI Mandate / UTR / Gateway Reference ID") },
                            modifier = Modifier.fillMaxWidth().testTag("purchase_ref_input")
                        )
                        OutlinedTextField(
                            value = purchaseNotes,
                            onValueChange = { purchaseNotes = it },
                            label = { Text("Optional Billing Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { consentAccepted = !consentAccepted }
                        ) {
                            Checkbox(
                                checked = consentAccepted,
                                onCheckedChange = { consentAccepted = it },
                                modifier = Modifier.testTag("subscription_consent_checkbox")
                            )
                            Text(
                                "I approve the ₹399/month recurring subscription, renewal reminders, and disclosed cancellation & refund terms.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = {
                                onSubmitSubscriptionVerification(paymentMethod, utrInput, consentAccepted, purchaseNotes)
                                utrInput = ""; purchaseNotes = ""; consentAccepted = false
                            },
                            enabled = consentAccepted && utrInput.trim().length >= 4,
                            modifier = Modifier.fillMaxWidth().testTag("submit_purchase_btn")
                        ) {
                            Text("Submit ₹399/mo Subscription Request")
                        }
                    }
                }
            }
        }

        // Billing History & Payment Receipts
        if (purchases.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Billing History & Receipts", style = MaterialTheme.typography.titleMedium)
                }
            }
            items(purchases, key = { it.id }) { rec ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("₹${rec.amountInr}/month • ${rec.status.uppercase()}", style = MaterialTheme.typography.titleSmall)
                        if (rec.periodLabel.isNotBlank()) {
                            Text("Billing Cycle: ${rec.periodLabel} (${rec.billingCycle})", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Ref: ${rec.referenceId} (${rec.paymentMethod})", fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodySmall)
                        if (rec.receiptNumber.isNotBlank()) {
                            Text("Receipt: ${rec.receiptNumber}", fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (rec.status == "failed") {
                            Text("Payment failed — please retry above with a valid UPI Autopay or gateway reference.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (rec.status == "active") {
                            OutlinedButton(
                                onClick = { onRequestRefund(rec.id, "Monthly subscription 7-day refund policy request") },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Request 7-Day Refund")
                            }
                        }
                    }
                }
            }
        }

        // Referral Attribution (Verified Subscriptions Only — Non-MLM)
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.referralTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Single-tier attribution tied only to verified active subscriptions (compliant with Indian law; no multi-level payouts).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your Code: ${profile.referralCode}", fontFamily = MonoFontFamily, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString("https://bharattools.in/?ref=${profile.referralCode}"))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Link")
                        }
                    }
                    if (profile.referredByCode.isNullOrBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = referralInput,
                                onValueChange = { referralInput = it },
                                label = { Text("Have a referral code?") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { onApplyReferral(referralInput); referralInput = "" }, enabled = referralInput.trim().length >= 4) {
                                Text("Apply")
                            }
                        }
                    } else {
                        Text("Referred by: ${profile.referredByCode}", style = MaterialTheme.typography.bodySmall, fontFamily = MonoFontFamily)
                    }
                }
            }
        }

        // Help & Support Tickets
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.supportTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Support Email: ${appConfig.supportEmail}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bug", "billing", "feature", "refund").forEach { cat ->
                            FilterChip(selected = ticketCategory == cat, onClick = { ticketCategory = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(ticketSubject, { ticketSubject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ticketMsg, { ticketMsg = it }, label = { Text("Describe your issue") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onSubmitSupportTicket(ticketCategory, ticketSubject, ticketMsg)
                            ticketSubject = ""; ticketMsg = ""
                        },
                        enabled = ticketSubject.trim().length >= 3 && ticketMsg.trim().length >= 5,
                        modifier = Modifier.fillMaxWidth().testTag("submit_ticket_btn")
                    ) { Text("Submit Ticket") }
                }
            }
        }

        items(tickets, key = { it.id }) { t ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${t.subject} • [${t.status.uppercase()}]", style = MaterialTheme.typography.titleSmall)
                    Text(t.message, style = MaterialTheme.typography.bodySmall)
                    if (t.adminReply.isNotBlank()) {
                        Text("Admin Reply: ${t.adminReply}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Privacy Settings & Sign Out
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.privacyTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "• PDF, Image, Text, and QR tools run locally on your Android device.\n• Media selection uses the zero-permission Android Photo Picker and SAF.\n• Account profile, favorites, subscription billing records, and tickets are isolated per UID in Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { signOutUser(credentialManager, scope, onSignedOut) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_btn")
            ) {
                Text(strings.signOut)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BoxIconInitial(name: String) {
    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "B",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
