package com.paolonata.shoppinglist.notification

import android.content.Context

/**
 * Se l'avviso per le scadenze degli scontrini è acceso, e a che ora del
 * mattino guarda. Spento di default: le notifiche si chiedono, non si
 * impongono.
 */
object ReceiptDeadlinePrefs {
    private const val PREFS_NAME = "shopping_list_prefs"
    private const val KEY_ENABLED = "receipt_deadline_enabled"
    private const val KEY_HOUR = "receipt_deadline_hour"

    const val DEFAULT_HOUR = 9

    /** Quanti giorni prima della scadenza vale la pena farsi vivi. */
    const val WARN_WITHIN_DAYS = 3L

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hour(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HOUR, DEFAULT_HOUR)

    fun setHour(context: Context, hour: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, hour).apply()
    }
}
