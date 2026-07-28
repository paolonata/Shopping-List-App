package com.paolonata.shoppinglist.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.MainActivity
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingItem

/**
 * Notifica persistente della lista della spesa. Approccio ridisegnato per essere
 * **massimamente leggibile sul lock screen**:
 *
 * - NON usiamo custom RemoteViews. Sul lockscreen il system UI li ignora / li
 *   trasforma in un blob di testo poco leggibile.
 * - Usiamo [NotificationCompat.InboxStyle]: righe separate con il font/spacing del
 *   sistema, che risulta pulito sia in dark che in light su qualsiasi device.
 * - I primi 3 articoli sono anche esposti come [NotificationCompat.Action]:
 *   diventano pulsanti visibili anche sul lock screen (spuntano l'articolo con un
 *   tap; su alcuni device serve prima lo sblocco per confermare).
 */
object ShoppingListNotifier {
    private const val CHANNEL_ID = "shopping_list_reminder_v3"
    private const val NOTIFICATION_ID = 42
    private const val INBOX_MAX_ROWS = 5
    private const val ACTION_MAX = 3
    private const val BRAND_COLOR = 0xFFCCFF00.toInt()

    fun show(context: Context, items: List<ShoppingItem>) {
        if (!hasPostNotificationPermission(context)) return
        ensureChannel(context)

        val unchecked = items.filter { !it.isChecked }
        val total = items.size
        val checked = total - unchecked.size

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(R.string.notification_title, checked, total)
        val summary = when {
            unchecked.isEmpty() -> context.getString(R.string.notification_all_done_body)
            unchecked.size == 1 -> context.getString(R.string.notification_one_left)
            else -> context.getString(R.string.notification_n_left, unchecked.size)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(summary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)

        if (unchecked.isNotEmpty()) {
            // Righe separate con font sistema, leggibili su qualsiasi tema/device
            val inbox = NotificationCompat.InboxStyle().setSummaryText(summary)
            unchecked.take(INBOX_MAX_ROWS).forEach { inbox.addLine("•  ${it.displayName()}") }
            if (unchecked.size > INBOX_MAX_ROWS) {
                inbox.addLine(context.getString(R.string.notification_more_items, unchecked.size - INBOX_MAX_ROWS))
            }
            builder.setStyle(inbox)

            // Pulsanti Action per i primi articoli — visibili anche sul lock screen
            unchecked.take(ACTION_MAX).forEach { item ->
                val actionLabel = context.getString(R.string.notification_check_action, item.shortDisplayName())
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        0, // icona nulla: molti launcher/lockscreen la nascondono comunque
                        actionLabel,
                        toggleItemPendingIntent(context, item.id),
                    ).build(),
                )
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            // Cleanup dei vecchi channel per non lasciare rumore nei setting
            listOf("shopping_list_reminder", "shopping_list_reminder_v2").forEach {
                runCatching { manager.deleteNotificationChannel(it) }
            }
        }
    }

    private fun ShoppingItem.displayName(): String =
        if (quantity > 1) "$name  ×$quantity" else name

    /** Versione più corta per il testo del pulsante Action (max ~14 char). */
    private fun ShoppingItem.shortDisplayName(): String {
        val short = if (name.length > 14) name.take(13).trimEnd() + "…" else name
        return if (quantity > 1) "✓ $short ×$quantity" else "✓ $short"
    }

    private fun toggleItemPendingIntent(context: Context, itemId: Long): PendingIntent {
        val intent = Intent(context, ShoppingListActionReceiver::class.java).apply {
            action = ShoppingListActionReceiver.ACTION_TOGGLE_ITEM
            data = Uri.parse("shoppinglist://item/$itemId")
            putExtra(ShoppingListActionReceiver.EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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
