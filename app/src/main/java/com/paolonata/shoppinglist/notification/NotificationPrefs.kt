package com.paolonata.shoppinglist.notification

import android.content.Context

/** Ricorda se l'utente ha attivato il promemoria persistente in notifica. */
object NotificationPrefs {
    private const val PREFS_NAME = "shopping_list_prefs"
    private const val KEY_ENABLED = "notification_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
