package com.paolonata.shoppinglist.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Programma/annulla il promemoria "un colpo solo" per l'intera lista della spesa.
 *
 * Usa [AlarmManager.setAndAllowWhileIdle] (inesatto) invece di un allarme esatto: per un
 * "ricordami di comprare i pannolini" arrivare con qualche minuto di ritardo in Doze non è un
 * problema, e così si evita tutta la gestione del permesso `SCHEDULE_EXACT_ALARM`
 * (richiesto esplicitamente dall'utente su Android 12+ per gli allarmi esatti).
 */
object ShoppingReminderScheduler {

    fun schedule(context: Context, triggerAtMillis: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(appContext))
        ShoppingReminderPrefs.setReminderAt(appContext, triggerAtMillis)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(pendingIntent(appContext))
        ShoppingReminderPrefs.setReminderAt(appContext, null)
    }

    /** Da richiamare al boot: gli allarmi di [AlarmManager] non sopravvivono al riavvio. */
    suspend fun rescheduleIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val triggerAtMillis = ShoppingReminderPrefs.getReminderAt(appContext) ?: return
        if (triggerAtMillis <= System.currentTimeMillis()) {
            // Il telefono era spento quando sarebbe dovuto scattare: meglio avvisare subito
            // in ritardo che perdere silenziosamente il promemoria.
            ShoppingReminderReceiver.fireNow(appContext)
        } else {
            schedule(appContext, triggerAtMillis)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ShoppingReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
