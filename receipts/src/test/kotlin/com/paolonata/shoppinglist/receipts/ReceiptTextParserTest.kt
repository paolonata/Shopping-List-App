package com.paolonata.shoppinglist.receipts

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real receipts, warts included: the text below is what the recognizer
 * actually hands over, misreadings and all. Every case here failed at some
 * point in the app this parser comes from.
 */
class ReceiptTextParserTest {

    private val today = LocalDate.of(2026, 8, 16)

    private fun parse(text: String) = ReceiptTextParser.parse(text.trimIndent(), today)

    @Test
    fun `supermarket receipt gives total, date and chain`() {
        val r = parse(
            """
            ESSELUNGA S.P.A.
            VIA GIAMBOLOGNA 1 - MILANO
            LATTE INTERO 1L          1,29
            PANE CASERECCIO          2,45
            TOTALE COMPLESSIVO      12,74
            Contanti                15,00
            Resto                    2,26
            04/08/2026 18:42
            """,
        )
        assertEquals(12.74, r.amount)
        assertEquals(LocalDate.of(2026, 8, 4), r.date)
        assertEquals("Esselunga", r.merchant)
        assertEquals(ReceiptCategory.GROCERIES, r.category)
    }

    @Test
    fun `discounts and VAT must not be mistaken for the total`() {
        val r = parse(
            """
            CONAD CITY
            TOTALE ARTICOLI 7
            SCONTO FIDELITY          -2,00
            SUBTOTALE                17,95
            TOTALE EURO              15,95
            di cui IVA                2,88
            """,
        )
        assertEquals(15.95, r.amount)
        assertEquals("Conad", r.merchant)
    }

    @Test
    fun `the last total printed wins`() {
        val r = parse(
            """
            TOTALE                   30,00
            TOTALE DA PAGARE         27,50
            """,
        )
        assertEquals(27.50, r.amount)
    }

    @Test
    fun `total on the line below its label`() {
        val r = parse(
            """
            MERCATO CENTRALE
            TOTALE COMPLESSIVO
            48,30
            """,
        )
        assertEquals(48.30, r.amount)
    }

    @Test
    fun `thousands separator`() {
        assertEquals(listOf(1234.56), ReceiptTextParser.findAmounts("TOTALE 1.234,56"))
        assertEquals(listOf(12.50), ReceiptTextParser.findAmounts("TOTALE 12.50"))
    }

    @Test
    fun `month spelled out, and misread by one letter`() {
        // "may" printed, "nay" recognised: El Corte Inglés, a real case.
        val r = parse(
            """
            El Corte Ingles
            ZAPATILLAS RUNNING
            03/nay/26
            """,
        )
        assertEquals(LocalDate.of(2026, 5, 3), r.date)
        assertEquals("El Corte Inglés", r.merchant)
        assertEquals(ReceiptCategory.SHOPPING, r.category)
    }

    @Test
    fun `italian month name`() {
        val r = parse(
            """
            LIBRERIA FELTRINELLI
            12 maggio 2026
            TOTALE                   18,00
            """,
        )
        assertEquals(LocalDate.of(2026, 5, 12), r.date)
        assertEquals(18.00, r.amount)
    }

    @Test
    fun `the line saying data wins over other dates`() {
        val r = parse(
            """
            SCADENZA PRODOTTO 01/01/2026
            Data 15/08/2026  10:25
            TOTALE                    7,90
            """,
        )
        assertEquals(LocalDate.of(2026, 8, 15), r.date)
    }

    @Test
    fun `dates in the future are misreadings, not receipts`() {
        val r = parse(
            """
            FARMACIA CENTRALE
            VALIDO FINO AL 20/12/2030
            TOTALE                    7,90
            """,
        )
        assertNull(r.date)
        assertEquals("Farmacia", r.merchant)
        assertEquals(ReceiptCategory.HEALTH, r.category)
    }

    @Test
    fun `impossible dates are dropped`() {
        val r = parse("31/02/2026\nTOTALE 5,00")
        assertNull(r.date)
    }

    @Test
    fun `unknown shop falls back to the letterhead`() {
        val r = parse(
            """
            MACELLERIA DA MARIO
            VIA ROMA 12
            P.IVA 01234567890
            TOTALE                   18,60
            """,
        )
        assertEquals("Macelleria Da Mario", r.merchant)
        assertNull(r.category)
    }

    @Test
    fun `letterhead skips addresses and tax numbers`() {
        val r = parse(
            """
            DOCUMENTO COMMERCIALE
            VIA GARIBALDI 4
            PANIFICIO IL FORNO
            TOTALE                    4,20
            """,
        )
        assertEquals("Panificio Il Forno", r.merchant)
    }

    @Test
    fun `unreadable text invents nothing`() {
        val r = parse(
            """
            ????
            ~~~~
            """,
        )
        assertNull(r.amount)
        assertNull(r.date)
        assertNull(r.merchant)
    }

    @Test
    fun `empty input is not a crash`() {
        val r = ReceiptTextParser.parse(null, today)
        assertEquals(0, r.lineCount)
        assertNull(r.amount)
    }

    @Test
    fun `hotel bill with the currency spelled out`() {
        val r = parse(
            """
            POSEIDON RESORT PALACE
            Benidorm
            BANCO SABADELL
            Sabado,15/08/2026            10:25
            714,24 EUR
            VENTA
            """,
        )
        assertEquals(LocalDate.of(2026, 8, 15), r.date)
        assertTrue(r.merchant!!.contains("Poseidon", ignoreCase = true), "trovato: ${r.merchant}")
    }
}
