package com.paolonata.shoppinglist.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.MainActivity
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.receipts.DeadlineKind
import com.paolonata.shoppinglist.receipts.Deadlines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Il controllo di ogni mattina: se qualche reso o garanzia sta per
 * scadere, lo dice; altrimenti tace. Poi riprogramma sé stesso per il
 * giorno dopo.
 *
 * Tacere quando non c'è niente è la parte importante: una notifica
 * quotidiana che dice "nessuna scadenza" verrebbe silenziata in tre
 * giorni, e con lei anche quelle che contano.
 */
class ReceiptDeadlineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkNow(appContext)
            } finally {
                ReceiptDeadlineScheduler.scheduleNext(appContext)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "receipt_deadlines"
        private const val NOTIFICATION_ID = 61

        suspend fun checkNow(context: Context) {
            if (!ReceiptDeadlinePrefs.isEnabled(context)) return
            val dao = ShoppingListDatabase.getInstance(context).receiptDao()
            val today = LocalDate.now()

            data class Imminent(val title: String, val kind: DeadlineKind, val text: String, val days: Long)

            val imminent = mutableListOf<Imminent>()
            dao.expiringSoon(today.plusDays(ReceiptDeadlinePrefs.WARN_WITHIN_DAYS).toString()).forEach { r ->
                val name = r.title.ifBlank { context.getString(R.string.receipt_untitled) }
                // Un reso già fatto non deve più chiamare.
                if (r.returnUntil != null && r.returnDoneAt == null) {
                    parse(r.returnUntil)?.let { date ->
                        val status = Deadlines.status(date, today)
                        if (status.days in 0..ReceiptDeadlinePrefs.WARN_WITHIN_DAYS) {
                            imminent += Imminent(name, DeadlineKind.RETURN, status.text, status.days)
                        }
                    }
                }
                if (r.warrantyUntil != null) {
                    parse(r.warrantyUntil)?.let { date ->
                        val status = Deadlines.status(date, today)
                        if (status.days in 0..ReceiptDeadlinePrefs.WARN_WITHIN_DAYS) {
                            imminent += Imminent(name, DeadlineKind.WARRANTY, status.text, status.days)
                        }
                    }
                }
            }
            if (imminent.isEmpty()) return

            val sorted = imminent.sortedBy { it.days }
            val first = sorted.first()
            val title = if (sorted.size == 1) {
                context.getString(R.string.receipt_deadline_notification_one, first.kind.label.lowercase(), first.text)
            } else {
                context.getString(R.string.receipt_deadline_notification_many, sorted.size)
            }
            showNotification(
                context,
                title = title,
                lines = sorted.map { "${it.kind.emoji}  ${it.title} · ${it.text}" },
            )
        }

        private fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()

        private fun showNotification(context: Context, title: String, lines: List<String>) {
            if (!hasPostNotificationPermission(context)) return
            ensureChannel(context)

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_OPEN_RECEIPTS, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val inbox = NotificationCompat.InboxStyle()
            lines.take(6).forEach(inbox::addLine)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(lines.firstOrNull().orEmpty())
                .setStyle(inbox)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openApp)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.receipt_deadline_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.receipt_deadline_channel_description)
                },
            )
        }

        private fun hasPostNotificationPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
