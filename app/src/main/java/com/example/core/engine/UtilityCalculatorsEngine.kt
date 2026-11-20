package com.example.core.engine

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.pow

object UtilityCalculatorsEngine {

    fun evaluateExpression(expr: String): Result<Double> = runCatching {
        val clean = expr.replace("×", "*").replace("÷", "/").replace(" ", "")
        require(clean.isNotEmpty()) { "Empty expression" }
        var pos = -1
        var ch = 0

        fun nextChar() {
            pos++
            ch = if (pos < clean.length) clean[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        lateinit var parseExpression: () -> Double
        lateinit var parseTerm: () -> Double
        lateinit var parseFactor: () -> Double

        parseFactor = {
            if (eat('+'.code)) parseFactor()
            else if (eat('-'.code)) -parseFactor()
            else {
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    require(eat(')'.code)) { "Mismatched parentheses" }
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, pos).toDouble()
                } else {
                    throw IllegalArgumentException("Unexpected character")
                }
                if (eat('^'.code)) x = x.pow(parseFactor())
                x
            }
        }

        parseTerm = {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val d = parseFactor()
                        require(d != 0.0) { "Division by zero" }
                        x /= d
                    }
                    eat('%'.code) -> x %= parseFactor()
                    else -> break
                }
            }
            x
        }

        parseExpression = {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> break
                }
            }
            x
        }

        nextChar()
        val result = parseExpression()
        require(pos >= clean.length) { "Unexpected trailing characters" }
        result
    }

    data class AgeResult(val years: Int, val months: Int, val days: Int, val totalDays: Long, val daysToNextBirthday: Long)

    fun calculateAge(birthYear: Int, birthMonth: Int, birthDay: Int): Result<AgeResult> = runCatching {
        val dob = Calendar.getInstance().apply {
            isLenient = false
            set(birthYear, birthMonth - 1, birthDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        require(!dob.after(today)) { "Birth date cannot be in the future." }

        var years = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - dob.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months--
            val prevMonth = (today.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            years--
            months += 12
        }

        val totalDays = TimeUnit.MILLISECONDS.toDays(today.timeInMillis - dob.timeInMillis)
        val nextBday = Calendar.getInstance().apply {
            set(today.get(Calendar.YEAR), birthMonth - 1, birthDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(today)) add(Calendar.YEAR, 1)
        }
        val toNext = TimeUnit.MILLISECONDS.toDays(nextBday.timeInMillis - today.timeInMillis)
        AgeResult(years, months, days, totalDays, toNext)
    }

    data class DateDiffResult(val years: Int, val months: Int, val days: Int, val totalDays: Long, val totalWeeks: Double)

    fun calculateDateDiff(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Result<DateDiffResult> = runCatching {
        val c1 = Calendar.getInstance().apply {
            isLenient = false
            set(y1, m1 - 1, d1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val c2 = Calendar.getInstance().apply {
            isLenient = false
            set(y2, m2 - 1, d2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (from, to) = if (c1.after(c2)) c2 to c1 else c1 to c2
        var years = to.get(Calendar.YEAR) - from.get(Calendar.YEAR)
        var months = to.get(Calendar.MONTH) - from.get(Calendar.MONTH)
        var days = to.get(Calendar.DAY_OF_MONTH) - from.get(Calendar.DAY_OF_MONTH)
        if (days < 0) {
            months--
            val prev = (to.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            days += prev.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            years--
            months += 12
        }
        val totalDays = abs(TimeUnit.MILLISECONDS.toDays(to.timeInMillis - from.timeInMillis))
        DateDiffResult(years, months, days, totalDays, totalDays / 7.0)
    }

    data class GstResult(val baseAmount: Double, val gstAmount: Double, val cgst: Double, val sgst: Double, val totalAmount: Double)

    fun calculateGst(amount: Double, ratePercent: Double, inclusive: Boolean): GstResult {
        return if (inclusive) {
            val base = amount * (100.0 / (100.0 + ratePercent))
            val gst = amount - base
            GstResult(base, gst, gst / 2.0, gst / 2.0, amount)
        } else {
            val gst = amount * (ratePercent / 100.0)
            GstResult(amount, gst, gst / 2.0, gst / 2.0, amount + gst)
        }
    }

    data class EmiResult(val monthlyEmi: Double, val totalInterest: Double, val totalPayable: Double)

    fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): Result<EmiResult> = runCatching {
        require(principal > 0 && tenureMonths > 0 && annualRatePercent >= 0) { "Enter valid loan amount, rate, and tenure." }
        if (annualRatePercent == 0.0) {
            val emi = principal / tenureMonths
            return@runCatching EmiResult(emi, 0.0, principal)
        }
        val r = annualRatePercent / 12.0 / 100.0
        val factor = (1 + r).pow(tenureMonths)
        val emi = principal * r * factor / (factor - 1)
        val total = emi * tenureMonths
        EmiResult(emi, total - principal, total)
    }

    data class BmiResult(val bmi: Double, val categoryEn: String, val categoryHi: String)

    fun calculateBmi(weightKg: Double, heightCm: Double): Result<BmiResult> = runCatching {
        require(weightKg > 0 && heightCm > 20) { "Enter valid weight (kg) and height (cm)." }
        val hM = heightCm / 100.0
        val bmi = weightKg / (hM * hM)
        val (en, hi) = when {
            bmi < 18.5 -> "Underweight" to "कम वजन"
            bmi < 25.0 -> "Normal weight" to "सामान्य वजन"
            bmi < 30.0 -> "Overweight" to "अधिक वजन"
            else -> "Obese" to "मोटापा"
        }
        BmiResult(bmi, en, hi)
    }

    fun generatePassword(
        length: Int,
        useUpper: Boolean,
        useLower: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean
    ): Result<Pair<String, Int>> = runCatching {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#$%^&*()-_=+[]{}?"
        val pool = buildString {
            if (useUpper) append(upper)
            if (useLower) append(lower)
            if (useDigits) append(digits)
            if (useSymbols) append(symbols)
        }
        require(pool.isNotEmpty()) { "Select at least one character set." }
        val rng = SecureRandom()
        val len = length.coerceIn(6, 64)
        val pwd = buildString {
            repeat(len) { append(pool[rng.nextInt(pool.length)]) }
        }
        val entropyBits = (len * kotlin.math.log2(pool.length.toDouble())).toInt()
        pwd to entropyBits
    }

    fun generateRandomNumbers(min: Int, max: Int, count: Int, unique: Boolean): Result<List<Int>> = runCatching {
        require(min <= max) { "Min must be <= Max." }
        val rangeSize = (max.toLong() - min.toLong() + 1L)
        require(count in 1..100) { "Count must be 1–100." }
        if (unique) require(count.toLong() <= rangeSize) { "Range too small for $count unique numbers." }
        val rng = SecureRandom()
        if (unique) {
            val set = linkedSetOf<Int>()
            while (set.size < count) {
                set.add(min + rng.nextInt(rangeSize.toInt()))
            }
            set.toList()
        } else {
            List(count) { min + rng.nextInt(rangeSize.toInt()) }
        }
    }

    fun convertTimeZones(sourceZoneId: String, hour: Int, minute: Int): List<Pair<String, String>> {
        val zones = listOf(
            "Asia/Kolkata" to "IST (India)",
            "UTC" to "UTC",
            "America/New_York" to "EST/EDT (New York)",
            "America/Los_Angeles" to "PST/PDT (Los Angeles)",
            "Europe/London" to "GMT/BST (London)",
            "Asia/Dubai" to "GST (Dubai)",
            "Asia/Singapore" to "SGT (Singapore)",
            "Asia/Tokyo" to "JST (Tokyo)"
        )
        val baseCal = Calendar.getInstance(TimeZone.getTimeZone(sourceZoneId)).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
        }
        return zones.map { (id, label) ->
            val fmt = SimpleDateFormat("hh:mm a • EEE, dd MMM", Locale.ENGLISH).apply {
                timeZone = TimeZone.getTimeZone(id)
            }
            label to fmt.format(baseCal.time)
        }
    }
}
