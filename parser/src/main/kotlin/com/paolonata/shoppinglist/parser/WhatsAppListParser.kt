package com.paolonata.shoppinglist.parser

/**
 * A single grocery item extracted from a raw block of chat text.
 */
data class ParsedItem(
    val name: String,
    val quantity: Int = 1,
    val note: String? = null,
)

/**
 * Turns the kind of free-form text people paste from a WhatsApp chat
 * ("Acqua bambini, pollo pannato/nuggets, iogurt Danone, ... 2 pere, 2 mele")
 * into a clean, ordered list of shopping items.
 *
 * The parser makes no assumptions about a strict format: it copes with
 * comma-separated runs, one-item-per-line messages, WhatsApp's bold, italic
 * and strikethrough markers, chat export timestamps
 * ("[20:20, 23/07/2026] Mamma: ..."), leading quantities ("2 pere"),
 * bullet/numbered lines and parenthetical notes.
 */
object WhatsAppListParser {

    private val exportTimestampBracket =
        Regex("""^\[\d{1,2}:\d{2}(:\d{2})?,?\s*[^]]*]\s*[^:]{1,60}:\s*""")
    private val exportTimestampDash =
        Regex("""^\d{1,2}:\d{2}(:\d{2})?\s*-\s*[^:]{1,60}:\s*""")
    private val bulletPrefix = Regex("""^[-•*‣▪◦]\s+""")
    private val numberedPrefix = Regex("""^\d+[.)]\s+""")
    private val emphasisMarkers = Regex("""[*_~]""")
    private val parenthetical = Regex("""\(([^()]*)\)""")
    private val leadingQuantity = Regex("""^(\d+)\s*[xX×]?\s+(.+)$""")
    private val edgePunctuation = Regex("""^[\s,;.\-–—:]+|[\s,;.\-–—:]+$""")
    private val whitespaceRun = Regex("""\s+""")

    fun parse(rawText: String): List<ParsedItem> {
        if (rawText.isBlank()) return emptyList()

        val items = mutableListOf<ParsedItem>()
        rawText.lines().forEach { rawLine ->
            val line = stripChatNoise(rawLine)
            if (line.isNotBlank()) {
                splitOnTopLevelCommas(line).forEach { token ->
                    parseToken(token)?.let { items.add(it) }
                }
            }
        }
        return mergeDuplicates(items)
    }

    private fun stripChatNoise(line: String): String {
        var result = line.trim()
        result = result.replace(exportTimestampBracket, "")
        result = result.replace(exportTimestampDash, "")
        result = emphasisMarkers.replace(result, "")
        result = result.replace(bulletPrefix, "")
        result = result.replace(numberedPrefix, "")
        return result.trim()
    }

    /** Splits [line] on commas, but never on a comma that sits inside parentheses. */
    private fun splitOnTopLevelCommas(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (char in line) {
            when (char) {
                '(' -> {
                    depth++
                    current.append(char)
                }
                ')' -> {
                    if (depth > 0) depth--
                    current.append(char)
                }
                ',' -> {
                    if (depth == 0) {
                        tokens.add(current.toString())
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        if (current.isNotBlank()) tokens.add(current.toString())
        return tokens
    }

    private fun parseToken(rawToken: String): ParsedItem? {
        var text = rawToken.trim()
        if (text.isEmpty()) return null

        val note = parenthetical.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        text = parenthetical.replace(text, " ")
        text = whitespaceRun.replace(text, " ").trim()
        text = text.replace(edgePunctuation, "")
        if (text.isEmpty()) return null

        var quantity = 1
        val quantityMatch = leadingQuantity.find(text)
        if (quantityMatch != null) {
            quantity = quantityMatch.groupValues[1].toIntOrNull() ?: 1
            text = quantityMatch.groupValues[2].trim()
        }
        text = text.replace(edgePunctuation, "")
        if (text.isEmpty()) return null

        val name = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        return ParsedItem(name = name, quantity = quantity, note = note)
    }

    private fun mergeDuplicates(items: List<ParsedItem>): List<ParsedItem> {
        val order = LinkedHashMap<Pair<String, String?>, ParsedItem>()
        for (item in items) {
            val key = item.name.lowercase() to item.note
            val existing = order[key]
            order[key] = if (existing == null) {
                item
            } else {
                existing.copy(quantity = existing.quantity + item.quantity)
            }
        }
        return order.values.toList()
    }
}
