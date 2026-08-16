package com.paolonata.shoppinglist.receipts

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeadlinesTest {

    private val today = LocalDate.of(2026, 8, 16)

    @Test
    fun `return and warranty from the purchase date`() {
        val purchase = LocalDate.of(2026, 8, 16)
        assertEquals(LocalDate.of(2026, 9, 15), Deadlines.returnDeadline(purchase, 30))
        assertEquals(LocalDate.of(2028, 8, 16), Deadlines.warrantyDeadline(purchase, 2))
    }

    @Test
    fun `wording follows how close it is`() {
        fun text(days: Long) = Deadlines.status(today.plusDays(days), today).text
        assertEquals("scade oggi", text(0))
        assertEquals("scade domani", text(1))
        assertEquals("fra 3 giorni", text(3))
        assertEquals("fra 20 giorni", text(20))
        assertEquals("scaduto ieri", text(-1))
        assertEquals("scaduto 5 giorni fa", text(-5))
        assertTrue(text(400).endsWith("anno") || text(400).endsWith("anni"), text(400))
    }

    @Test
    fun `only what is close enough calls for attention`() {
        assertTrue(Deadlines.status(today.plusDays(2), today).urgent)
        assertTrue(Deadlines.status(today, today).urgent)
        assertTrue(!Deadlines.status(today.plusDays(20), today).urgent)
        assertTrue(!Deadlines.status(today.minusDays(1), today).urgent)
    }

    @Test
    fun `changing the purchase date moves the deadlines with it`() {
        // The bug this test exists for: the deadline used to stay pinned to
        // the date typed first, so the countdown kept running from the wrong day.
        val corrected = LocalDate.of(2026, 7, 18)
        val (ret, war) = Deadlines.realign(corrected, returnDays = 30, warrantyYears = 2)
        assertEquals(LocalDate.of(2026, 8, 17), ret)
        assertEquals(LocalDate.of(2028, 7, 18), war)
    }

    @Test
    fun `a date typed by hand is left alone`() {
        val (ret, war) = Deadlines.realign(LocalDate.of(2026, 7, 18), returnDays = null, warrantyYears = null)
        assertNull(ret)
        assertNull(war)
    }

    @Test
    fun `old receipts have their shortcut inferred from the dates`() {
        val purchase = LocalDate.of(2026, 8, 16)
        assertEquals(30, Deadlines.presetFromDates(purchase, purchase.plusDays(30), DeadlineKind.RETURN))
        assertEquals(2, Deadlines.presetFromDates(purchase, purchase.plusYears(2), DeadlineKind.WARRANTY))
        // 17 days is no shortcut: it was typed by hand, and must stay that way.
        assertNull(Deadlines.presetFromDates(purchase, purchase.plusDays(17), DeadlineKind.RETURN))
    }
}
