package com.example.ui.screens.tools

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.core.engine.UtilityCalculatorsEngine
import com.example.data.network.CurrencyApiClient
import com.example.ui.components.ToolTopBar
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UtilityToolsScreen(
    initialSubTab: Int,
    isHindi: Boolean,
    onToolCompleted: (toolId: String, title: String, summary: String) -> Unit,
    onBack: () -> Unit
) {
    val tabs = listOf(
        "Calculator", "Age", "Date Diff", "Units", "Percent",
        "BMI", "GST ₹", "EMI ₹", "Currency", "Time Zone",
        "Password", "Random", "Stopwatch", "Timer"
    )
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, tabs.lastIndex)) }

    Scaffold(
        topBar = {
            ToolTopBar(
                title = if (isHindi) "दस्तावेज़ व उपयोगिताएं" else "Document & Utility Tools",
                subtitle = tabs[selectedTab],
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
                        onClick = { selectedTab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("util_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> CalculatorPanel(onToolCompleted)
                1 -> AgeCalculatorPanel(onToolCompleted)
                2 -> DateDiffPanel(onToolCompleted)
                3 -> UnitConverterPanel()
                4 -> PercentagePanel()
                5 -> BmiPanel()
                6 -> GstPanel(onToolCompleted)
                7 -> EmiPanel(onToolCompleted)
                8 -> CurrencyPanel(onToolCompleted)
                9 -> TimeZonePanel()
                10 -> PasswordGenPanel(onToolCompleted)
                11 -> RandomGenPanel()
                12 -> StopwatchPanel()
                13 -> TimerPanel()
            }
        }
    }
}

@Composable
private fun CalculatorPanel(onLog: (String, String, String) -> Unit) {
    var expr by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("0") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(expr.ifEmpty { "0" }, style = MaterialTheme.typography.titleMedium, fontFamily = MonoFontFamily)
                Spacer(Modifier.height(6.dp))
                Text(output, style = MaterialTheme.typography.displaySmall, fontFamily = MonoFontFamily)
            }
        }
        val rows = listOf(
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("C", "0", ".", "+"),
            listOf("(", ")", "^", "=")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                "C" -> { expr = ""; output = "0" }
                                "=" -> UtilityCalculatorsEngine.evaluateExpression(expr)
                                    .onSuccess { v ->
                                        val formatted = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.4f", v).trimEnd('0').trimEnd('.')
                                        output = formatted
                                        onLog("util_calc", "Calculator", "$expr = $formatted")
                                    }
                                    .onFailure { output = "Error" }
                                else -> expr += key
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(key, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}

@Composable
private fun AgeCalculatorPanel(onLog: (String, String, String) -> Unit) {
    var year by remember { mutableStateOf("1998") }
    var month by remember { mutableStateOf("8") }
    var day by remember { mutableStateOf("15") }
    var result by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(day, { day = it.filter(Char::isDigit) }, label = { Text("Day") }, modifier = Modifier.weight(1f))
            OutlinedTextField(month, { month = it.filter(Char::isDigit) }, label = { Text("Month") }, modifier = Modifier.weight(1f))
            OutlinedTextField(year, { year = it.filter(Char::isDigit) }, label = { Text("Year") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            UtilityCalculatorsEngine.calculateAge(year.toIntOrNull() ?: 2000, month.toIntOrNull() ?: 1, day.toIntOrNull() ?: 1)
                .onSuccess { r ->
                    result = "${r.years} Years, ${r.months} Months, ${r.days} Days\nTotal Days: ${r.totalDays}\nNext Birthday in ${r.daysToNextBirthday} days"
                    onLog("util_age", "Age Calculator", "${r.years}y ${r.months}m ${r.days}d")
                }
                .onFailure { result = it.localizedMessage }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate Age") }
        result?.let { ElevatedCard { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) } }
    }
}

@Composable
private fun DateDiffPanel(onLog: (String, String, String) -> Unit) {
    var d1 by remember { mutableStateOf("1") }; var m1 by remember { mutableStateOf("1") }; var y1 by remember { mutableStateOf("2025") }
    var d2 by remember { mutableStateOf("20") }; var m2 by remember { mutableStateOf("11") }; var y2 by remember { mutableStateOf("2026") }
    var result by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Start Date (DD / MM / YYYY)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(d1, { d1 = it.filter(Char::isDigit) }, label = { Text("DD") }, modifier = Modifier.weight(1f))
            OutlinedTextField(m1, { m1 = it.filter(Char::isDigit) }, label = { Text("MM") }, modifier = Modifier.weight(1f))
            OutlinedTextField(y1, { y1 = it.filter(Char::isDigit) }, label = { Text("YYYY") }, modifier = Modifier.weight(1f))
        }
        Text("End Date (DD / MM / YYYY)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(d2, { d2 = it.filter(Char::isDigit) }, label = { Text("DD") }, modifier = Modifier.weight(1f))
            OutlinedTextField(m2, { m2 = it.filter(Char::isDigit) }, label = { Text("MM") }, modifier = Modifier.weight(1f))
            OutlinedTextField(y2, { y2 = it.filter(Char::isDigit) }, label = { Text("YYYY") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            UtilityCalculatorsEngine.calculateDateDiff(
                y1.toIntOrNull() ?: 2025, m1.toIntOrNull() ?: 1, d1.toIntOrNull() ?: 1,
                y2.toIntOrNull() ?: 2026, m2.toIntOrNull() ?: 1, d2.toIntOrNull() ?: 1
            ).onSuccess { r ->
                result = "${r.years} Years, ${r.months} Months, ${r.days} Days\nTotal Days: ${r.totalDays} (${String.format("%.1f", r.totalWeeks)} weeks)"
                onLog("util_datediff", "Date Diff", "${r.totalDays} days")
            }.onFailure { result = it.localizedMessage }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate Difference") }
        result?.let { ElevatedCard { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) } }
    }
}

@Composable
private fun UnitConverterPanel() {
    var category by remember { mutableIntStateOf(0) }
    var inputVal by remember { mutableStateOf("1") }
    val cats = listOf("Length", "Weight", "Area (India)", "Data")
    val value = inputVal.toDoubleOrNull() ?: 0.0
    val conversions = when (category) {
        0 -> listOf("Meter" to value, "Kilometer" to value / 1000.0, "Foot" to value * 3.28084, "Inch" to value * 39.3701)
        1 -> listOf("Kilogram" to value, "Gram" to value * 1000.0, "Pound" to value * 2.20462, "Tola (11.66g)" to (value * 1000.0) / 11.6638)
        2 -> listOf("Sq. Meter" to value, "Sq. Foot" to value * 10.7639, "Gaj (Sq. Yard)" to value * 1.19599, "Acre" to value / 4046.86)
        else -> listOf("MB" to value, "KB" to value * 1024.0, "GB" to value / 1024.0, "TB" to value / (1024.0 * 1024.0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(cats) { idx, c -> FilterChip(selected = category == idx, onClick = { category = idx }, label = { Text(c) }) }
        }
        OutlinedTextField(inputVal, { inputVal = it }, label = { Text("Base Value (${conversions.first().first})") }, modifier = Modifier.fillMaxWidth())
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                conversions.forEach { (unit, res) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(unit, style = MaterialTheme.typography.bodyLarge)
                        Text(String.format("%.4f", res), fontFamily = MonoFontFamily, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PercentagePanel() {
    var x by remember { mutableStateOf("18") }
    var y by remember { mutableStateOf("1000") }
    val xv = x.toDoubleOrNull() ?: 0.0
    val yv = y.toDoubleOrNull() ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(x, { x = it }, label = { Text("X (%)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(y, { y = it }, label = { Text("Y (Value)") }, modifier = Modifier.weight(1f))
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$x% of $y = ${String.format("%.2f", xv * yv / 100.0)}", style = MaterialTheme.typography.titleMedium, fontFamily = MonoFontFamily)
                Text("$y + $x% = ${String.format("%.2f", yv * (1 + xv / 100.0))}", fontFamily = MonoFontFamily)
                Text("$y - $x% = ${String.format("%.2f", yv * (1 - xv / 100.0))}", fontFamily = MonoFontFamily)
            }
        }
    }
}

@Composable
private fun BmiPanel() {
    var weight by remember { mutableStateOf("68") }
    var height by remember { mutableStateOf("170") }
    val res = UtilityCalculatorsEngine.calculateBmi(weight.toDoubleOrNull() ?: 0.0, height.toDoubleOrNull() ?: 0.0).getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(height, { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.weight(1f))
        }
        res?.let { r ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BMI: ${String.format("%.1f", r.bmi)} — ${r.categoryEn} (${r.categoryHi})", style = MaterialTheme.typography.titleLarge)
                    Text("Informational only. Consult a qualified healthcare professional for medical advice.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GstPanel(onLog: (String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("1000") }
    var rate by remember { mutableStateOf(18.0) }
    var inclusive by remember { mutableStateOf(false) }
    val gst = UtilityCalculatorsEngine.calculateGst(amount.toDoubleOrNull() ?: 0.0, rate, inclusive)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5.0, 12.0, 18.0, 28.0).forEach { r ->
                FilterChip(selected = rate == r, onClick = { rate = r }, label = { Text("${r.toInt()}%") })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = inclusive, onCheckedChange = { inclusive = it })
            Spacer(Modifier.width(8.dp))
            Text(if (inclusive) "GST Inclusive (Reverse)" else "GST Exclusive (+Add GST)")
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Base Amount: ₹${String.format("%.2f", gst.baseAmount)}", fontFamily = MonoFontFamily)
                Text("CGST (${rate / 2}%): ₹${String.format("%.2f", gst.cgst)}", fontFamily = MonoFontFamily)
                Text("SGST (${rate / 2}%): ₹${String.format("%.2f", gst.sgst)}", fontFamily = MonoFontFamily)
                Text("Total GST: ₹${String.format("%.2f", gst.gstAmount)}", style = MaterialTheme.typography.titleMedium, fontFamily = MonoFontFamily)
                Text("Grand Total: ₹${String.format("%.2f", gst.totalAmount)}", style = MaterialTheme.typography.headlineSmall, fontFamily = MonoFontFamily)
            }
        }
        Button(onClick = { onLog("util_gst", "GST ${rate.toInt()}%", "Total ₹${String.format("%.2f", gst.totalAmount)}") }, modifier = Modifier.fillMaxWidth()) {
            Text("Save to History")
        }
    }
}

@Composable
private fun EmiPanel(onLog: (String, String, String) -> Unit) {
    var principal by remember { mutableStateOf("500000") }
    var rate by remember { mutableStateOf("9.5") }
    var months by remember { mutableStateOf("60") }
    val emi = UtilityCalculatorsEngine.calculateEmi(
        principal.toDoubleOrNull() ?: 0.0,
        rate.toDoubleOrNull() ?: 0.0,
        months.toIntOrNull() ?: 0
    ).getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(principal, { principal = it }, label = { Text("Loan Principal (₹)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(rate, { rate = it }, label = { Text("Annual Interest (%)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(months, { months = it }, label = { Text("Tenure (Months)") }, modifier = Modifier.weight(1f))
        }
        emi?.let { e ->
            val p = principal.toDoubleOrNull() ?: 1.0
            val pRatio = (p / e.totalPayable.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Monthly EMI: ₹${String.format("%.2f", e.monthlyEmi)}", style = MaterialTheme.typography.headlineSmall, fontFamily = MonoFontFamily)
                    LinearProgressIndicator(progress = { pRatio }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text("Principal: ₹${String.format("%.0f", p)} • Interest: ₹${String.format("%.0f", e.totalInterest)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Total Payable: ₹${String.format("%.0f", e.totalPayable)}", style = MaterialTheme.typography.titleMedium, fontFamily = MonoFontFamily)
                }
            }
            Button(onClick = { onLog("util_emi", "EMI Calculator", "EMI ₹${String.format("%.0f", e.monthlyEmi)}/mo") }, modifier = Modifier.fillMaxWidth()) {
                Text("Save EMI Summary")
            }
        }
    }
}

@Composable
private fun CurrencyPanel(onLog: (String, String, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("1000") }
    var base by remember { mutableStateOf("INR") }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var rateDate by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun fetchRates(b: String) {
        scope.launch {
            loading = true
            error = null
            runCatching { CurrencyApiClient.api.getLatestRates(b) }
                .onSuccess { res ->
                    rates = res.rates
                    rateDate = res.date
                }
                .onFailure { error = "Offline or rate API unavailable (${it.localizedMessage})" }
            loading = false
        }
    }
    LaunchedEffect(base) { fetchRates(base) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount ($base)") }, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY").forEach { c ->
                item { FilterChip(selected = base == c, onClick = { base = c }, label = { Text(c) }) }
            }
        }
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (rates.isNotEmpty()) {
            val amt = amount.toDoubleOrNull() ?: 0.0
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Live ECB Rates ($rateDate)", style = MaterialTheme.typography.titleSmall)
                    listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD").filter { it != base && rates.containsKey(it) }.forEach { target ->
                        val converted = amt * (rates[target] ?: 0.0)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(target, style = MaterialTheme.typography.titleMedium)
                            Text(String.format("%.2f", converted), fontFamily = MonoFontFamily, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Button(onClick = { onLog("util_currency", "Currency ($base)", "Checked live FX rates for $amount $base") }, modifier = Modifier.fillMaxWidth()) {
                Text("Log Conversion")
            }
        }
    }
}

@Composable
private fun TimeZonePanel() {
    var hour by remember { mutableFloatStateOf(10f) }
    var minute by remember { mutableFloatStateOf(30f) }
    var zone by remember { mutableStateOf("Asia/Kolkata") }
    val conversions = remember(zone, hour, minute) {
        UtilityCalculatorsEngine.convertTimeZones(zone, hour.toInt(), minute.toInt())
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Asia/Kolkata" to "IST", "UTC" to "UTC", "America/New_York" to "EST", "Europe/London" to "GMT").forEach { (id, lbl) ->
                item { FilterChip(selected = zone == id, onClick = { zone = id }, label = { Text(lbl) }) }
            }
        }
        Text("Hour: ${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}")
        Slider(value = hour, onValueChange = { hour = it }, valueRange = 0f..23f)
        Slider(value = minute, onValueChange = { minute = it }, valueRange = 0f..59f)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                conversions.forEach { (label, time) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(time, fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordGenPanel(onLog: (String, String, String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var len by remember { mutableFloatStateOf(16f) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var generated by remember { mutableStateOf("") }
    var entropy by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Length: ${len.toInt()} chars")
        Slider(value = len, onValueChange = { len = it }, valueRange = 8f..40f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(upper, { upper = it }); Text("A-Z")
            Checkbox(lower, { lower = it }); Text("a-z")
            Checkbox(digits, { digits = it }); Text("0-9")
            Checkbox(symbols, { symbols = it }); Text("!@#")
        }
        Button(onClick = {
            UtilityCalculatorsEngine.generatePassword(len.toInt(), upper, lower, digits, symbols)
                .onSuccess { (p, e) ->
                    generated = p; entropy = e
                    onLog("util_password", "Password Gen", "Generated ${len.toInt()}-char CSPRNG password")
                }
        }, modifier = Modifier.fillMaxWidth()) { Text("Generate Client-Side Password") }
        if (generated.isNotEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(generated, fontFamily = MonoFontFamily, style = MaterialTheme.typography.titleMedium)
                    Text("Estimated Entropy: ~$entropy bits (SecureRandom)", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(generated)) }) { Text("Copy Password") }
                }
            }
        }
    }
}

@Composable
private fun RandomGenPanel() {
    var min by remember { mutableStateOf("1") }
    var max by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("5") }
    var unique by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(min, { min = it }, label = { Text("Min") }, modifier = Modifier.weight(1f))
            OutlinedTextField(max, { max = it }, label = { Text("Max") }, modifier = Modifier.weight(1f))
            OutlinedTextField(count, { count = it }, label = { Text("Count") }, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(unique, { unique = it })
            Spacer(Modifier.width(8.dp))
            Text("Unique numbers only")
        }
        Button(onClick = {
            UtilityCalculatorsEngine.generateRandomNumbers(min.toIntOrNull() ?: 1, max.toIntOrNull() ?: 100, count.toIntOrNull() ?: 1, unique)
                .onSuccess { result = it.joinToString(", ") }
                .onFailure { result = it.localizedMessage ?: "Error" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Generate Numbers") }
        if (result.isNotEmpty()) ElevatedCard { Text(result, Modifier.padding(16.dp), fontFamily = MonoFontFamily, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun StopwatchPanel() {
    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var laps by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(running) {
        while (running) {
            delay(50L)
            elapsedMs += 50L
        }
    }

    val secs = (elapsedMs / 1000) % 60
    val mins = (elapsedMs / 60000) % 60
    val centis = (elapsedMs % 1000) / 10
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            String.format("%02d:%02d.%02d", mins, secs, centis),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = MonoFontFamily
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { laps = listOf(elapsedMs) + laps }, enabled = elapsedMs > 0) { Text("Lap") }
            OutlinedButton(onClick = { running = false; elapsedMs = 0L; laps = emptyList() }) { Text("Reset") }
        }
        laps.forEachIndexed { idx, ms ->
            val s = (ms / 1000) % 60; val m = (ms / 60000) % 60; val c = (ms % 1000) / 10
            Text("Lap ${laps.size - idx}: ${String.format("%02d:%02d.%02d", m, s, c)}", fontFamily = MonoFontFamily)
        }
    }
}

@Composable
private fun TimerPanel() {
    var totalSeconds by remember { mutableLongStateOf(300L) }
    var remainingSeconds by remember { mutableLongStateOf(300L) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0L) running = false
        }
    }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            String.format("%02d:%02d", mins, secs),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = MonoFontFamily
        )
        if (remainingSeconds == 0L) {
            Text("Time's up!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60L to "1m", 300L to "5m", 600L to "10m", 1500L to "25m").forEach { (s, label) ->
                FilterChip(
                    selected = totalSeconds == s,
                    onClick = { running = false; totalSeconds = s; remainingSeconds = s },
                    label = { Text(label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { if (remainingSeconds > 0) running = !running }) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { running = false; remainingSeconds = totalSeconds }) { Text("Reset") }
        }
    }
}
