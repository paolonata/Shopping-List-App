package com.paolonata.shoppinglist.notification

import android.content.Context

/**
 * Ricorda l'orario (epoch millis) dell'eventuale promemoria "un colpo solo" impostato
 * dall'utente per l'intera lista della spesa (non per singolo articolo). `null`/assente
 * significa nessun promemoria attivo.
 */
object ShoppingReminderPrefs {
    private const val PREFS_NAME = "shopping_list_prefs"
    private const val KEY_REMINDER_AT = "reminder_at_millis"

    fun getReminderAt(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_REMINDER_AT, -1L)
        return value.takeIf { it > 0 }
    }

    fun setReminderAt(context: Context, atMillis: Long?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (atMillis == null) {
            prefs.remove(KEY_REMINDER_AT)
        } else {
            prefs.putLong(KEY_REMINDER_AT, atMillis)
        }
        prefs.apply()
    }
}
