package com.paolonata.shoppinglist.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Il controllo quotidiano delle scadenze.
 *
 * Un allarme solo, che ogni mattina guarda l'archivio, invece di un
 * allarme per ogni scontrino: gli scontrini sono tanti e le loro scadenze
 * cambiano (si corregge una data, si segna un reso come fatto), e tenere
 * allineata una sveglia per ciascuno sarebbe un modo elaborato di
 * sbagliare. Così c'è una cosa sola da programmare, e la verità sta
 * sempre nel database.
 *
 * Come per il promemoria della lista, l'allarme è inesatto: che l'avviso
 * arrivi alle 9:00 o alle 9:20 non cambia niente, e si evita il permesso
 * per gli allarmi esatti.
 */
object ReceiptDeadlineScheduler {

    fun enable(context: Context, hour: Int = ReceiptDeadlinePrefs.hour(context)) {
        val appContext = context.applicationContext
        ReceiptDeadlinePrefs.setEnabled(appContext, true)
        ReceiptDeadlinePrefs.setHour(appContext, hour)
        scheduleNext(appContext)
    }

    fun disable(context: Context) {
        val appContext = context.applicationContext
        ReceiptDeadlinePrefs.setEnabled(appContext, false)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(pendingIntent(appContext))
    }

    /** Programma il prossimo controllo. Da richiamare anche dopo ogni avviso. */
    fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        if (!ReceiptDeadlinePrefs.isEnabled(appContext)) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextCheckMillis(ReceiptDeadlinePrefs.hour(appContext)),
            pendingIntent(appContext),
        )
    }

    /** Gli allarmi non sopravvivono al riavvio: qui si rimette in piedi. */
    fun rescheduleIfNeeded(context: Context) = scheduleNext(context)

    private fun nextCheckMillis(hour: Int): Long {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReceiptDeadlineReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val REQUEST_CODE = 71
}
