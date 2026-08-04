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
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.data.ShoppingListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fa scattare il promemoria "un colpo solo" per l'intera lista, programmato da
 * [ShoppingReminderScheduler]. A differenza della notifica persistente questa è una notifica
 * normale (scartabile, con suono/vibrazione di default), non `setOngoing`.
 */
class ShoppingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fireNow(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "shopping_list_reminder_oneoff"
        private const val NOTIFICATION_ID = 43

        /** Costruisce e mostra la notifica, poi consuma il promemoria (è un colpo solo). */
        suspend fun fireNow(context: Context) {
            val repository = ShoppingListRepository(
                ShoppingListDatabase.getInstance(context).shoppingItemDao(),
            )
            showNotification(context, repository.getAllOnce())
            ShoppingReminderPrefs.setReminderAt(context, null)
        }

        private fun showNotification(context: Context, items: List<ShoppingItem>) {
            if (!hasPostNotificationPermission(context)) return
            ensureChannel(context)

            val unchecked = items.filter { !it.isChecked }
            val body = when {
                unchecked.isEmpty() -> context.getString(R.string.reminder_notification_body_empty)
                else -> context.resources.getQuantityString(
                    R.plurals.reminder_notification_body,
                    unchecked.size,
                    unchecked.size,
                )
            }

            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            }
            manager.createNotificationChannel(channel)
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
