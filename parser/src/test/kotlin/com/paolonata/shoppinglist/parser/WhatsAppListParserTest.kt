package com.paolonata.shoppinglist.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsAppListParserTest {

    @Test
    fun `parses the classic comma separated whatsapp grocery message`() {
        val text = "Acqua bambini, pollo pannato/nuggets, iogurt Danone, bicchieri riso, " +
            "2 vaschette menestra, 3 fanta, pizza, 2 kombucha, torrone al tartufo " +
            "(prendi tutti i pacchi che ci siano mi sa che stanno finendo anche la), " +
            "palla latuga, tinto normale, 2 pere, 2 mele, 2 pacchi tacchino a fette"

        val items = WhatsAppListParser.parse(text)

        assertEquals(
            listOf(
                ParsedItem("Acqua bambini"),
                ParsedItem("Pollo pannato/nuggets"),
                ParsedItem("Iogurt Danone"),
                ParsedItem("Bicchieri riso"),
                ParsedItem("Vaschette menestra", quantity = 2),
                ParsedItem("Fanta", quantity = 3),
                ParsedItem("Pizza"),
                ParsedItem("Kombucha", quantity = 2),
                ParsedItem(
                    "Torrone al tartufo",
                    note = "prendi tutti i pacchi che ci siano mi sa che stanno finendo anche la",
                ),
                ParsedItem("Palla latuga"),
                ParsedItem("Tinto normale"),
                ParsedItem("Pere", quantity = 2),
                ParsedItem("Mele", quantity = 2),
                ParsedItem("Pacchi tacchino a fette", quantity = 2),
            ),
            items,
        )
    }

    @Test
    fun `parses a second whatsapp bubble on its own line`() {
        val items = WhatsAppListParser.parse("Latte, pane\nTortine—*")

        assertEquals(
            listOf(
                ParsedItem("Latte"),
                ParsedItem("Pane"),
                ParsedItem("Tortine"),
            ),
            items,
        )
    }

    @Test
    fun `strips whatsapp bold italic and strikethrough markers`() {
        val items = WhatsAppListParser.parse("*Latte*, _pane fresco_, ~uova~")

        assertEquals(
            listOf(
                ParsedItem("Latte"),
                ParsedItem("Pane fresco"),
                ParsedItem("Uova"),
            ),
            items,
        )
    }

    @Test
    fun `strips chat export timestamps and sender names`() {
        val items = WhatsAppListParser.parse(
            "[20:20, 23/07/2026] Mamma: Latte, pane\n20:21 - Mamma: Uova",
        )

        assertEquals(
            listOf(
                ParsedItem("Latte"),
                ParsedItem("Pane"),
                ParsedItem("Uova"),
            ),
            items,
        )
    }

    @Test
    fun `understands bulleted and numbered lines`() {
        val items = WhatsAppListParser.parse("- Latte\n• 2 pane\n1. Uova\n2) Burro")

        assertEquals(
            listOf(
                ParsedItem("Latte"),
                ParsedItem("Pane", quantity = 2),
                ParsedItem("Uova"),
                ParsedItem("Burro"),
            ),
            items,
        )
    }

    @Test
    fun `merges duplicate items by summing quantities`() {
        val items = WhatsAppListParser.parse("2 pere, pane, 1 pere\npere")

        assertEquals(
            listOf(
                ParsedItem("Pere", quantity = 4),
                ParsedItem("Pane"),
            ),
            items,
        )
    }

    @Test
    fun `ignores blank and noise only tokens`() {
        val items = WhatsAppListParser.parse("Latte,, ,  , pane")

        assertEquals(listOf(ParsedItem("Latte"), ParsedItem("Pane")), items)
    }

    @Test
    fun `returns empty list for blank input`() {
        assertEquals(emptyList(), WhatsAppListParser.parse("   \n  "))
    }
}
