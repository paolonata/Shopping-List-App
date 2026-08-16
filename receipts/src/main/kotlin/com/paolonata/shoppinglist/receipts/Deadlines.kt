package com.paolonata.shoppinglist.receipts

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How pressing a deadline is. Drives both the wording and the colour. */
enum class DeadlineLevel { EXPIRED, TODAY, TOMORROW, THIS_WEEK, THIS_MONTH, LATER }

/** A deadline read in plain words: "fra 3 giorni", "scaduto ieri". */
data class DeadlineStatus(
    val level: DeadlineLevel,
    val days: Long,
    val text: String,
) {
    /** Worth an alert on the home screen. */
    val urgent: Boolean get() = level == DeadlineLevel.TODAY ||
        level == DeadlineLevel.TOMORROW ||
        level == DeadlineLevel.THIS_WEEK
}

/** The two clocks that run on a receipt after you have paid. */
enum class DeadlineKind(val emoji: String, val label: String) {
    RETURN("↩️", "Reso"),
    WARRANTY("🛡️", "Garanzia"),
}

/**
 * Everything about "am I still in time?".
 *
 * A receipt keeps the *preset* you picked — 30 days, 2 years — next to the
 * computed date, and not only the date. It matters: correct the purchase
 * date a week later and the deadline has to follow, otherwise the countdown
 * keeps ticking from the day you first typed it. That was a real bug in the
 * app this logic comes from, so the recomputation lives here, tested.
 */
object Deadlines {

    /** Shortcuts offered when turning the return window on. */
    val RETURN_PRESETS = listOf(8, 14, 30, 60)

    /** Shortcuts offered for the warranty, in years. */
    val WARRANTY_PRESETS = listOf(1, 2, 3, 5)

    fun returnDeadline(purchase: LocalDate, days: Int): LocalDate = purchase.plusDays(days.toLong())

    fun warrantyDeadline(purchase: LocalDate, years: Int): LocalDate = purchase.plusYears(years.toLong())

    fun daysLeft(deadline: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, deadline)

    fun status(deadline: LocalDate, today: LocalDate = LocalDate.now()): DeadlineStatus {
        val days = daysLeft(deadline, today)
        return when {
            days < 0 -> {
                val gone = -days
                DeadlineStatus(
                    DeadlineLevel.EXPIRED, days,
                    if (gone == 1L) "scaduto ieri" else "scaduto $gone giorni fa",
                )
            }
            days == 0L -> DeadlineStatus(DeadlineLevel.TODAY, days, "scade oggi")
            days == 1L -> DeadlineStatus(DeadlineLevel.TOMORROW, days, "scade domani")
            days <= 7 -> DeadlineStatus(DeadlineLevel.THIS_WEEK, days, "fra $days giorni")
            days <= 30 -> DeadlineStatus(DeadlineLevel.THIS_MONTH, days, "fra $days giorni")
            days <= 60 -> DeadlineStatus(DeadlineLevel.LATER, days, "fra $days giorni")
            // Past two months the day count says nothing: months, then round years.
            days < 365 -> DeadlineStatus(DeadlineLevel.LATER, days, "fra ${(days / 30).coerceAtLeast(2)} mesi")
            else -> {
                val years = days / 365
                DeadlineStatus(
                    DeadlineLevel.LATER, days,
                    if (years == 1L) "fra 1 anno" else "fra $years anni",
                )
            }
        }
    }

    /**
     * Recomputes the deadlines after the purchase date changed.
     *
     * Only the ones that came from a preset move: a date typed by hand is a
     * decision, and moving it under the user's feet would be worse than
     * leaving it stale.
     */
    fun realign(
        purchase: LocalDate,
        returnDays: Int?,
        warrantyYears: Int?,
    ): Pair<LocalDate?, LocalDate?> = Pair(
        returnDays?.let { returnDeadline(purchase, it) },
        warrantyYears?.let { warrantyDeadline(purchase, it) },
    )

    /**
     * Guesses which preset a deadline came from, for receipts saved before
     * the app started keeping track of it. Returns null when no shortcut
     * matches exactly — meaning the date was typed by hand.
     */
    fun presetFromDates(purchase: LocalDate?, deadline: LocalDate?, kind: DeadlineKind): Int? {
        if (purchase == null || deadline == null) return null
        return when (kind) {
            DeadlineKind.RETURN ->
                RETURN_PRESETS.firstOrNull { returnDeadline(purchase, it) == deadline }
            DeadlineKind.WARRANTY ->
                WARRANTY_PRESETS.firstOrNull { warrantyDeadline(purchase, it) == deadline }
        }
    }
}
