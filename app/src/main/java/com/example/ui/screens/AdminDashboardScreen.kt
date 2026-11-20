package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppConfig
import com.example.data.model.UiState
import com.example.ui.components.ToolTopBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AdminViewModel
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    currentConfig: AppConfig,
    onBack: () -> Unit
) {
    val usersState by adminViewModel.usersState.collectAsStateWithLifecycle()
    val purchasesState by adminViewModel.purchasesState.collectAsStateWithLifecycle()
    val ticketsState by adminViewModel.ticketsState.collectAsStateWithLifecycle()
    val referralsState by adminViewModel.referralsState.collectAsStateWithLifecycle()
    val banner by adminViewModel.statusBanner.collectAsStateWithLifecycle()

    val users = (usersState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
    val referrals = (referralsState as? UiState.Success)?.data ?: emptyList()

    val activeSubscribers = users.count { it.hasProAccess }
    val cancelledCount = users.count { it.subscriptionStatus == "cancelled" }
    val expiredCount = users.count { it.subscriptionStatus == "expired" || it.isSubscriptionExpiredByDate }
    val failedPaymentsCount = purchases.count { it.status == "failed" }
    val refundedCount = purchases.count { it.status == "refunded" }

    val grossMonthlyRevenue = activeSubscribers * 399
    val gstAmount = (grossMonthlyRevenue * 18.0 / 118.0).roundToInt()
    val gatewayFees = (grossMonthlyRevenue * 0.02).roundToInt()
    val refundDeduction = refundedCount * 399

    var hostingCostInput by remember { mutableStateOf("1500") }
    var apiCostInput by remember { mutableStateOf("800") }
    var supportCostInput by remember { mutableStateOf("2000") }
    var marketingCostInput by remember { mutableStateOf("3000") }
    var otherOpCostInput by remember { mutableStateOf("1000") }

    val hostingCost = hostingCostInput.toIntOrNull() ?: 0
    val apiCost = apiCostInput.toIntOrNull() ?: 0
    val supportCost = supportCostInput.toIntOrNull() ?: 0
    val marketingCost = marketingCostInput.toIntOrNull() ?: 0
    val otherOpCost = otherOpCostInput.toIntOrNull() ?: 0
    val totalOperatingAndTaxDeductions =
        gstAmount + gatewayFees + refundDeduction + hostingCost + apiCost + supportCost + marketingCost + otherOpCost
    val estimatedNetProfit = grossMonthlyRevenue - totalOperatingAndTaxDeductions

    var annTitle by remember { mutableStateOf("") }
    var annTitleHi by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }
    var annBodyHi by remember { mutableStateOf("") }
    var trialLimitInput by remember(currentConfig) { mutableStateOf(currentConfig.trialToolsLimit.toString()) }
    var maintenanceMode by remember(currentConfig) { mutableStateOf(currentConfig.maintenanceMode) }
    var supportEmailInput by remember(currentConfig) { mutableStateOf(currentConfig.supportEmail) }
    var replyMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = "Admin Dashboard",
                subtitle = "Protected by Firestore isAdmin() • ₹399/mo SaaS",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            banner?.let { msg ->
                item {
                    ElevatedCard {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(msg, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { adminViewModel.clearBanner() }) { Text("OK") }
                        }
                    }
                }
            }

            // Subscriber & Revenue Summary Metrics
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Users", "${users.size}", Modifier.weight(1f))
                    MetricBox("Active Subs", "$activeSubscribers", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Gross MRR", "₹$grossMonthlyRevenue", Modifier.weight(1f))
                    MetricBox("Net Profit", "₹$estimatedNetProfit", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Cancelled/Exp", "$cancelledCount / $expiredCount", Modifier.weight(1f))
                    MetricBox("Failed/Ref", "$failedPaymentsCount / $refundedCount • Ref: ${referrals.size}", Modifier.weight(1f))
                }
            }

            // Gross Revenue vs Actual Net Profit Breakdown Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Monthly Revenue vs Net Profit Model (₹399/subscriber)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Gross MRR = $activeSubscribers active subscribers × ₹399 = ₹$grossMonthlyRevenue",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = MonoFontFamily
                        )
                        HorizontalDivider()
                        ExpenseRow("GST & Applicable Taxes (18% est.)", "₹$gstAmount")
                        ExpenseRow("Payment Gateway Fees (~2%)", "₹$gatewayFees")
                        ExpenseRow("Refunds ($refundedCount × ₹399)", "₹$refundDeduction")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hostingCostInput,
                                onValueChange = { hostingCostInput = it.filter(Char::isDigit) },
                                label = { Text("Hosting (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = apiCostInput,
                                onValueChange = { apiCostInput = it.filter(Char::isDigit) },
                                label = { Text("API Usage (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = supportCostInput,
                                onValueChange = { supportCostInput = it.filter(Char::isDigit) },
                                label = { Text("Support (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = marketingCostInput,
                                onValueChange = { marketingCostInput = it.filter(Char::isDigit) },
                                label = { Text("Marketing (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = otherOpCostInput,
                            onValueChange = { otherOpCostInput = it.filter(Char::isDigit) },
                            label = { Text("Other Operating Expenses (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider()
                        ExpenseRow("Total Deductions & Expenses", "-₹$totalOperatingAndTaxDeductions")
                        ExpenseRow("Estimated Monthly Net Profit", "₹$estimatedNetProfit")
                    }
                }
            }

            // Subscription Billing & Verification Queue
            item { Text("Subscriptions & Billing Queue (${purchases.size})", style = MaterialTheme.typography.titleLarge) }
            items(purchases, key = { it.id }) { p ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${p.userEmail} • ₹${p.amountInr}/mo • ${p.status.uppercase()}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Cycle: ${p.periodLabel.ifBlank { "Monthly" }} • Consent: ${if (p.userConsentAccepted) "Approved" else "No"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Ref: ${p.referenceId} (${p.paymentMethod}) • Receipt: ${p.receiptNumber.ifBlank { "-" }}",
                            fontFamily = MonoFontFamily,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { adminViewModel.updateSubscriptionStatus(p.id, p.userId, "active") },
                                modifier = Modifier.testTag("admin_approve_${p.id}")
                            ) { Text("Activate") }
                            OutlinedButton(onClick = { adminViewModel.updateSubscriptionStatus(p.id, p.userId, "failed") }) {
                                Text("Fail")
                            }
                            OutlinedButton(onClick = { adminViewModel.updateSubscriptionStatus(p.id, p.userId, "expired") }) {
                                Text("Expire")
                            }
                            OutlinedButton(onClick = { adminViewModel.updateSubscriptionStatus(p.id, p.userId, "refunded") }) {
                                Text("Refund")
                            }
                        }
                    }
                }
            }

            // Support Tickets
            item { Text("Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleLarge) }
            items(tickets, key = { it.id }) { t ->
                val draft = replyMap[t.id] ?: t.adminReply
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${t.subject} [${t.status}] • ${t.userEmail}", style = MaterialTheme.typography.titleSmall)
                        Text(t.message, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { replyMap = replyMap + (t.id to it) },
                            label = { Text("Admin Reply") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { adminViewModel.replyTicket(t.id, "resolved", draft) }) { Text("Resolve & Reply") }
                            OutlinedButton(onClick = { adminViewModel.replyTicket(t.id, "in_progress", draft) }) { Text("In Progress") }
                        }
                    }
                }
            }

            // Announcements Publisher
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Publish Announcement", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(annTitle, { annTitle = it }, label = { Text("Title (EN)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(annTitleHi, { annTitleHi = it }, label = { Text("Title (HI)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(annBody, { annBody = it }, label = { Text("Message (EN)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(annBodyHi, { annBodyHi = it }, label = { Text("Message (HI)") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                adminViewModel.publishAnnouncement(annTitle, annTitleHi, annBody, annBodyHi)
                                annTitle = ""; annTitleHi = ""; annBody = ""; annBodyHi = ""
                            },
                            enabled = annTitle.trim().length >= 2 && annBody.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Broadcast Announcement") }
                    }
                }
            }

            // App Configuration
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("App Configuration (₹399/month Plan)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(trialLimitInput, { trialLimitInput = it.filter(Char::isDigit) }, label = { Text("Free Tools Limit") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(supportEmailInput, { supportEmailInput = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Maintenance Banner Mode")
                        }
                        Button(
                            onClick = {
                                adminViewModel.saveAppConfig(trialLimitInput.toIntOrNull() ?: 45, maintenanceMode, supportEmailInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Global Config") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontFamily = MonoFontFamily)
        }
    }
}

@Composable
private fun ExpenseRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = MonoFontFamily)
    }
}
