package com.paolonata.shoppinglist.receipts

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What kind of spending a receipt is about. */
enum class ReceiptCategory(val id: String, val emoji: String, val label: String) {
    GROCERIES("spesa", "🛒", "Spesa"),
    DINING("ristoro", "🍽️", "Bar & Rist."),
    TRANSPORT("trasporti", "🚗", "Trasporti"),
    TECH("tech", "💻", "Tecnologia"),
    SHOPPING("shopping", "🛍️", "Shopping"),
    HOME("casa", "🏠", "Casa"),
    HEALTH("salute", "💊", "Salute"),
    OTHER("altro", "🧾", "Altro");

    companion object {
        fun fromId(id: String?): ReceiptCategory = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

/**
 * The three things worth reading off a receipt, plus the category we can
 * guess from the shop's name. Any of them may be missing: leaving a field
 * empty is always better than filling it with the wrong value.
 */
data class ParsedReceipt(
    val amount: Double? = null,
    val date: LocalDate? = null,
    val merchant: String? = null,
    val category: ReceiptCategory? = null,
    val lineCount: Int = 0,
)

/**
 * Turns the raw text of a photographed receipt — as handed over by ML Kit's
 * on-device recognizer — into the fields you would otherwise type by hand.
 *
 * Receipts are printed by thousands of different tills, so there is no format
 * to rely on: the parser works by weighing clues. A line saying "TOTALE
 * COMPLESSIVO" is worth more than one saying "totale"; a line saying "sconto"
 * is worth nothing at all. The recognizer itself makes mistakes, especially on
 * faded thermal paper, so month names are matched allowing one wrong letter.
 *
 * To fix a chain that reads badly, add a row to [BRANDS] or a clue to
 * [TOTAL_HINTS] and run `:receipts:test` — the suite checks real receipts.
 */
object ReceiptTextParser {

    private data class Brand(val re: Regex, val name: String?, val category: ReceiptCategory)

    /** Known chains: they give a clean name and a category for free. */
    private val BRANDS = listOf(
        Brand(Regex("esselunga", RegexOption.IGNORE_CASE), "Esselunga", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bcoop\b|ipercoop""", RegexOption.IGNORE_CASE), "Coop", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bconad\b""", RegexOption.IGNORE_CASE), "Conad", ReceiptCategory.GROCERIES),
        Brand(Regex("carrefour", RegexOption.IGNORE_CASE), "Carrefour", ReceiptCategory.GROCERIES),
        Brand(Regex("""\blidl\b""", RegexOption.IGNORE_CASE), "Lidl", ReceiptCategory.GROCERIES),
        Brand(Regex("eurospin", RegexOption.IGNORE_CASE), "Eurospin", ReceiptCategory.GROCERIES),
        Brand(Regex("""penny\s*market|\bpenny\b""", RegexOption.IGNORE_CASE), "Penny Market", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bpam\b|panorama""", RegexOption.IGNORE_CASE), "Pam", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bdespar\b|eurospar""", RegexOption.IGNORE_CASE), "Despar", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bcrai\b""", RegexOption.IGNORE_CASE), "Crai", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bbennet\b""", RegexOption.IGNORE_CASE), "Bennet", ReceiptCategory.GROCERIES),
        Brand(Regex("""\btigros\b""", RegexOption.IGNORE_CASE), "Tigros", ReceiptCategory.GROCERIES),
        Brand(Regex("""\btodis\b""", RegexOption.IGNORE_CASE), "Todis", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bin\s?s\s?mercato|\bmd\b""", RegexOption.IGNORE_CASE), "MD", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bgigante\b""", RegexOption.IGNORE_CASE), "Il Gigante", ReceiptCategory.GROCERIES),
        Brand(Regex("""\bfarmacia\b|parafarmacia""", RegexOption.IGNORE_CASE), "Farmacia", ReceiptCategory.HEALTH),
        Brand(Regex("decathlon", RegexOption.IGNORE_CASE), "Decathlon", ReceiptCategory.SHOPPING),
        Brand(Regex("""corte\s*ingl[eé]s""", RegexOption.IGNORE_CASE), "El Corte Inglés", ReceiptCategory.SHOPPING),
        Brand(Regex("""primark|bershka|stradivarius|pull\s*&\s*bear""", RegexOption.IGNORE_CASE), null, ReceiptCategory.SHOPPING),
        Brand(Regex("""\bzara\b|h\s?&\s?m|\bovs\b""", RegexOption.IGNORE_CASE), null, ReceiptCategory.SHOPPING),
        Brand(Regex("""\bikea\b""", RegexOption.IGNORE_CASE), "IKEA", ReceiptCategory.HOME),
        Brand(Regex("""leroy\s*merlin|obi\b|bricocenter""", RegexOption.IGNORE_CASE), null, ReceiptCategory.HOME),
        Brand(Regex("""mediaworld|media\s*world""", RegexOption.IGNORE_CASE), "MediaWorld", ReceiptCategory.TECH),
        Brand(Regex("""unieuro|euronics|trony""", RegexOption.IGNORE_CASE), null, ReceiptCategory.TECH),
        Brand(Regex("""\bq8\b|\beni\b|\besso\b|tamoil|\bip\s+distributore|agip""", RegexOption.IGNORE_CASE), null, ReceiptCategory.TRANSPORT),
        Brand(Regex("""autostrad|telepass|parcheggi|\batac\b|\bgtt\b|trenitalia|italo\b""", RegexOption.IGNORE_CASE), null, ReceiptCategory.TRANSPORT),
        Brand(Regex("""ristorante|trattoria|pizzeria|osteria|\bbar\b|caff[eè]""", RegexOption.IGNORE_CASE), null, ReceiptCategory.DINING),
        Brand(Regex("amazon", RegexOption.IGNORE_CASE), "Amazon", ReceiptCategory.SHOPPING),
        Brand(Regex("""hotel|resort|\bb&b\b|albergo""", RegexOption.IGNORE_CASE), null, ReceiptCategory.OTHER),
    )

    private data class TotalHint(val re: Regex, val score: Int)

    /** Lines that announce the total, from the most trustworthy down. */
    private val TOTAL_HINTS = listOf(
        TotalHint(Regex("""totale\s+complessivo""", RegexOption.IGNORE_CASE), 100),
        TotalHint(Regex("""totale\s+(da\s+pagare|documento)""", RegexOption.IGNORE_CASE), 95),
        TotalHint(Regex("""totale\s+euro""", RegexOption.IGNORE_CASE), 90),
        TotalHint(Regex("""^\s*totale\b""", RegexOption.IGNORE_CASE), 85),
        TotalHint(Regex("""\btotale\b""", RegexOption.IGNORE_CASE), 70),
        TotalHint(Regex("""importo\s+(pagato|totale)""", RegexOption.IGNORE_CASE), 65),
        TotalHint(Regex("""^\s*(contanti|carta|bancomat|pagamento\s+elettronico)\b""", RegexOption.IGNORE_CASE), 40),
    )

    /** Lines to ignore while hunting for the total: they lead astray. */
    private val TOTAL_TRAPS = Regex(
        """sconto|sconti|risparmi|subtotale|sub\s*totale|resto|non\s+riscoss|iva\b|imponibile|arrotondament|punti|saldo\s+punti|totale\s+articoli|pezzi""",
        RegexOption.IGNORE_CASE,
    )

    /** Words that rule a line out as the shop's name. */
    private val MERCHANT_TRAPS = Regex(
        """scontrino|documento\s+commerciale|ricevuta|fattura|p\.?\s?iva|part\.?\s?iva|cod\.?\s?fisc|c\.?f\.?[:\s]|\bvia\b|\bviale\b|\bpiazza\b|\bcorso\b|\btel\b|telefono|www\.|@|codice|registratore|matricola|cassa\b|operatore|addetto|\bora\b|scontr|rt\s*\d|\bn[.°]\s*\d""",
        RegexOption.IGNORE_CASE,
    )

    private val AMOUNT = Regex("""(?<![\d.,])(\d{1,3}(?:[.\s]\d{3})+|\d+)[.,](\d{2})(?![\d.,])""")
    private val NUMERIC_DATE = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})(?!\d)""")
    private val TEXTUAL_DATE = Regex("""(?<!\d)(\d{1,2})\s*[/\-. ]\s*([A-Za-zÀ-ù]{3,10})\.?\s*[/\-. ]\s*(\d{2,4})(?!\d)""")
    private val TIME_OF_DAY = Regex("""\d{1,2}[:.]\d{2}""")
    private val DATE_WORD = Regex("""\bdata\b|\bfecha\b""", RegexOption.IGNORE_CASE)
    private val COMPANY_SUFFIX = Regex("""\b(s\.?r\.?l\.?|s\.?p\.?a\.?|s\.?n\.?c\.?|s\.?a\.?s\.?)\b""", RegexOption.IGNORE_CASE)

    /**
     * Month names in Italian, Spanish and English. Tills often print
     * "03/mag/26" instead of "03/05/26", and the recognizer happily turns
     * "may" into "nay" or "mar" into "rnar".
     */
    private val MONTH_NAMES = listOf(
        listOf("gen", "ene", "jan"),
        listOf("feb"),
        listOf("mar", "mrz"),
        listOf("apr", "abr"),
        listOf("mag", "may"),
        listOf("giu", "jun"),
        listOf("lug", "jul"),
        listOf("ago", "aug"),
        listOf("set", "sep"),
        listOf("ott", "oct"),
        listOf("nov"),
        listOf("dic", "dez", "dec"),
    )

    /**
     * Reads [rawText] and returns whatever could be established with
     * confidence. [today] is injectable so the tests do not drift with the
     * calendar: dates in the future are receipts read wrong, not receipts.
     */
    fun parse(rawText: String?, today: LocalDate = LocalDate.now()): ParsedReceipt {
        val text = rawText.orEmpty()
        val lines = text.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedReceipt()

        val (name, category) = findMerchant(lines, text)
        return ParsedReceipt(
            amount = findTotal(lines),
            date = findDate(lines, today),
            merchant = name,
            category = category,
            lineCount = lines.size,
        )
    }

    /** Every money amount on a line, e.g. "1.234,56" or "12.50". */
    fun findAmounts(line: String): List<Double> =
        AMOUNT.findAll(line).mapNotNull { m ->
            val whole = m.groupValues[1].replace(Regex("""[.\s]"""), "")
            "$whole.${m.groupValues[2]}".toDoubleOrNull()
        }.toList()

    fun findTotal(lines: List<String>): Double? {
        var bestValue: Double? = null
        var bestScore = Double.NEGATIVE_INFINITY

        lines.forEachIndexed { i, line ->
            if (TOTAL_TRAPS.containsMatchIn(line)) return@forEachIndexed
            val hint = TOTAL_HINTS.firstOrNull { it.re.containsMatchIn(line) } ?: return@forEachIndexed

            // The amount sits on the same line; when it does not, it is
            // usually on the next one.
            var amounts = findAmounts(line)
            var row = i
            val next = lines.getOrNull(i + 1)
            if (amounts.isEmpty() && next != null && !TOTAL_TRAPS.containsMatchIn(next)) {
                amounts = findAmounts(next)
                row = i + 1
            }
            val value = amounts.lastOrNull() ?: return@forEachIndexed
            if (value <= 0 || value > 100_000) return@forEachIndexed

            // With equal clues the lower line wins: on a receipt the final
            // total is the last one printed.
            val score = hint.score + row * 0.1
            if (score > bestScore) {
                bestScore = score
                bestValue = value
            }
        }
        return bestValue
    }

    fun findDate(lines: List<String>, today: LocalDate = LocalDate.now()): LocalDate? {
        val tomorrow = today.plusDays(1)
        val floor = today.minusYears(10)
        val candidates = mutableListOf<Pair<LocalDate, Double>>()

        fun consider(line: String, index: Int, day: Int, month: Int, rawYear: String) {
            val year = if (rawYear.length == 2) 2000 + rawYear.toInt() else rawYear.toIntOrNull() ?: return
            if (month !in 1..12 || day !in 1..31) return
            val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return
            if (date.isAfter(tomorrow) || date.isBefore(floor)) return
            // A line that says "data", or that carries a time as well, is the
            // right one; a month spelled out is hard to mistake, so it counts more.
            val score = (if (DATE_WORD.containsMatchIn(line)) 10.0 else 0.0) +
                (if (TIME_OF_DAY.containsMatchIn(line)) 5.0 else 0.0) -
                index * 0.01
            candidates += date to score
        }

        lines.forEachIndexed { i, line ->
            NUMERIC_DATE.findAll(line).forEach { m ->
                consider(line, i, m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])
            }
            TEXTUAL_DATE.findAll(line).forEach { m ->
                monthFromWord(m.groupValues[2])?.let { month ->
                    consider(line, i, m.groupValues[1].toInt(), month, m.groupValues[3])
                }
            }
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    /** From "mag", "may", "nay" or "maggio" to a month number (1-12). */
    fun monthFromWord(token: String): Int? {
        val clean = token.lowercase()
            .replace("à", "a").replace("è", "e").replace("é", "e")
            .replace("ì", "i").replace("ò", "o").replace("ù", "u")
            .replace("rn", "m") // "rn" read in place of "m"
            .replace(Regex("[^a-z]"), "")
            .take(3)
        if (clean.length < 3) return null

        MONTH_NAMES.forEachIndexed { i, names -> if (clean in names) return i + 1 }
        // No exact match: allow a single wrong letter.
        MONTH_NAMES.forEachIndexed { i, names ->
            if (names.any { offByOneLetter(clean, it) }) return i + 1
        }
        return null
    }

    private fun offByOneLetter(a: String, b: String): Boolean {
        if (a == b) return true
        if (a.length != b.length) return false
        var different = 0
        for (i in a.indices) {
            if (a[i] != b[i] && ++different > 1) return false
        }
        return different == 1
    }

    private fun findMerchant(lines: List<String>, wholeText: String): Pair<String?, ReceiptCategory?> {
        for (brand in BRANDS) {
            if (!brand.re.containsMatchIn(wholeText)) continue
            if (brand.name != null) return brand.name to brand.category
            // A chain with no fixed name: take the line it appears on, cleaned up.
            val line = lines.firstOrNull { brand.re.containsMatchIn(it) }
            return (line?.let(::cleanName)) to brand.category
        }

        // No known chain: the first lines are the letterhead.
        for (line in lines.take(6)) {
            if (MERCHANT_TRAPS.containsMatchIn(line)) continue
            val clean = cleanName(line)
            if (clean != null && clean.length >= 3 && clean.any { it.isLetter() }) return clean to null
        }
        return null to null
    }

    private fun cleanName(line: String): String? {
        val clean = line
            .replace(Regex("""[*#|_]+"""), " ")
            .replace(COMPANY_SUFFIX, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .take(40)
            .trim()
        if (clean.isEmpty()) return null
        // ALL CAPS becomes Title Case: easier to read down a list.
        return if (clean == clean.uppercase()) titleCase(clean) else clean
    }

    private fun titleCase(text: String): String {
        val out = StringBuilder(text.length)
        var startOfWord = true
        for (ch in text.lowercase()) {
            out.append(if (startOfWord) ch.uppercaseChar() else ch)
            startOfWord = ch == ' ' || ch == '\'' || ch == '’' || ch == '-'
        }
        return out.toString()
    }

    /** ISO date, the shape the database stores. */
    fun LocalDate.toIso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)
}
