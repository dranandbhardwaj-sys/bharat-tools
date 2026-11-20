package com.example.core.engine

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil

data class TextStats(
    val words: Int,
    val charsWithSpaces: Int,
    val charsNoSpaces: Int,
    val sentences: Int,
    val paragraphs: Int,
    val lines: Int,
    val readMinutes: Int
)

enum class DiffType { SAME, ADDED, REMOVED }
data class DiffLine(val lineNumber: Int, val type: DiffType, val text: String)

object TextToolkitEngine {

    fun analyze(text: String): TextStats {
        val trimmed = text.trim()
        val words = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
        val charsWith = text.length
        val charsNo = text.count { !it.isWhitespace() }
        val sentences = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("[.!?।]+")).count { it.trim().isNotEmpty() }
        val paragraphs = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\n\\s*\\n")).count { it.trim().isNotEmpty() }
        val lines = if (text.isEmpty()) 0 else text.lines().size
        val readMin = if (words == 0) 0 else ceil(words / 200.0).toInt().coerceAtLeast(1)
        return TextStats(words, charsWith, charsNo, sentences, paragraphs, lines, readMin)
    }

    fun toTitleCase(text: String): String =
        text.lowercase().split(Regex("(\\s+)")).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    fun toSentenceCase(text: String): String {
        val sb = StringBuilder()
        var capNext = true
        for (ch in text.lowercase()) {
            if (capNext && ch.isLetter()) {
                sb.append(ch.titlecaseChar())
                capNext = false
            } else {
                sb.append(ch)
            }
            if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') capNext = true
        }
        return sb.toString()
    }

    fun toToggleCase(text: String): String =
        text.map { ch ->
            when {
                ch.isUpperCase() -> ch.lowercaseChar()
                ch.isLowerCase() -> ch.uppercaseChar()
                else -> ch
            }
        }.joinToString("")

    fun removeExtraSpaces(text: String): String =
        text.lines()
            .map { it.trim().replace(Regex("[ \\t]+"), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    fun reverseCharacters(text: String): String = text.reversed()

    fun reverseWords(text: String): String =
        text.trim().split(Regex("\\s+")).reversed().joinToString(" ")

    fun cleanText(text: String, stripHtml: Boolean = true, stripPunctuation: Boolean = false): String {
        var out = text
        if (stripHtml) {
            out = out.replace(Regex("<[^>]*>"), " ")
        }
        if (stripPunctuation) {
            out = out.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        }
        return removeExtraSpaces(out)
    }

    fun removeDuplicateLines(text: String, caseSensitive: Boolean = true, sortOutput: Boolean = false): String {
        val seen = HashSet<String>()
        val unique = mutableListOf<String>()
        for (line in text.lines()) {
            val trimmed = line.trimEnd()
            if (trimmed.isEmpty()) continue
            val key = if (caseSensitive) trimmed else trimmed.lowercase()
            if (seen.add(key)) unique.add(trimmed)
        }
        if (sortOutput) unique.sortWith(String.CASE_INSENSITIVE_ORDER)
        return unique.joinToString("\n")
    }

    fun compareTexts(left: String, right: String): List<DiffLine> {
        val lLines = left.lines()
        val rLines = right.lines()
        val max = maxOf(lLines.size, rLines.size)
        val diffs = mutableListOf<DiffLine>()
        for (i in 0 until max) {
            val l = lLines.getOrNull(i)
            val r = rLines.getOrNull(i)
            when {
                l == r && l != null -> diffs.add(DiffLine(i + 1, DiffType.SAME, l))
                else -> {
                    if (l != null) diffs.add(DiffLine(i + 1, DiffType.REMOVED, l))
                    if (r != null) diffs.add(DiffLine(i + 1, DiffType.ADDED, r))
                }
            }
        }
        return diffs
    }

    fun formatAddLineNumbers(text: String): String =
        text.lines().mapIndexed { idx, line -> "${idx + 1}. $line" }.joinToString("\n")

    fun formatBullets(text: String): String =
        text.lines().filter { it.isNotBlank() }.joinToString("\n") { "• ${it.trim()}" }

    fun formatPrettyJson(text: String): Result<String> = runCatching {
        val trimmed = text.trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> throw IllegalArgumentException("Input is not a valid JSON object or array.")
        }
    }
}
