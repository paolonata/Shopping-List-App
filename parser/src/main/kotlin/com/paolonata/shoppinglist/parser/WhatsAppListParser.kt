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
    private val bulletPrefix = Regex("""^[-•‣▪◦]\s+""")
    private val numberedPrefix = Regex("""^\d+[.)]\s+""")
    // Solo ai bordi di una "parola" (non tra due caratteri alfanumerici), altrimenti un
    // nome come "pasta_barilla" perderebbe l'underscore diventando "Pastabarilla".
    private val emphasisMarkers = Regex("""(?<!\w)[*_~]|[*_~](?!\w)""")
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

    /**
     * Splits [line] on commas, but never on a comma that sits inside parentheses, nor on a
     * comma used as decimal separator between two digits ("1,5 l").
     *
     * If the line has unbalanced parentheses (e.g. a stray "(" or a text emoticon like ":("),
     * tracking depth would never return to 0 and every remaining comma on the line would be
     * swallowed, collapsing the rest of the message into a single garbage item. In that case
     * we fall back to a plain split, ignoring parentheses as grouping for this line only.
     */
    private fun splitOnTopLevelCommas(line: String): List<String> {
        if (line.count { it == '(' } != line.count { it == ')' }) {
            return line.split(',')
        }
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (i in line.indices) {
            val char = line[i]
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
                    val decimalComma = current.isNotEmpty() && current.last().isDigit() &&
                        i + 1 < line.length && line[i + 1].isDigit()
                    if (depth == 0 && !decimalComma) {
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

        // Più parentesi nello stesso token diventano note multiple unite (nessuna persa),
        // es. "pane (integrale) (2 confezioni)" -> nota "integrale; 2 confezioni".
        val note = parenthetical.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .joinToString("; ")
            .takeIf { it.isNotEmpty() }
        text = parenthetical.replace(text, " ")
        text = whitespaceRun.replace(text, " ").trim()
        text = text.replace(edgePunctuation, "")
        if (text.isEmpty()) return null

        var quantity = 1
        val quantityMatch = leadingQuantity.find(text)
        if (quantityMatch != null) {
            // Un numero troppo grande per un Int (toIntOrNull() -> null) non è una quantità
            // sensata: meglio lasciarlo nel nome piuttosto che cancellarlo silenziosamente.
            val parsedQuantity = quantityMatch.groupValues[1].toIntOrNull()
            if (parsedQuantity != null) {
                quantity = parsedQuantity.coerceAtLeast(1)
                text = quantityMatch.groupValues[2].trim()
            }
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
