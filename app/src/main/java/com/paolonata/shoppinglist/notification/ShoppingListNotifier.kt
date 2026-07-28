package com.paolonata.shoppinglist.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.MainActivity
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingItem

/**
 * Costruisce e aggiorna la notifica persistente della lista della spesa: mostra gli articoli
 * ancora da comprare e permette di spuntarli con un tocco, senza aprire l'app né sbloccare
 * del tutto il telefono (se le notifiche sul lock screen sono attive nel sistema).
 */
object ShoppingListNotifier {
    private const val CHANNEL_ID = "shopping_list_reminder"
    private const val NOTIFICATION_ID = 42
    private const val COLLAPSED_MAX_ROWS = 3
    private const val EXPANDED_MAX_ROWS = 8

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_progress, checked, total))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(buildListView(context, unchecked, checked, total, COLLAPSED_MAX_ROWS))
            .setCustomBigContentView(buildListView(context, unchecked, checked, total, EXPANDED_MAX_ROWS))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun buildListView(
        context: Context,
        unchecked: List<ShoppingItem>,
        checked: Int,
        total: Int,
        maxRows: Int,
    ): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_list)
        view.setTextViewText(
            R.id.notif_header,
            context.getString(R.string.notification_progress, checked, total),
        )

        // Reset del contenitore ad ogni build per non accumulare righe negli update
        view.removeAllViews(R.id.notif_rows_container)

        unchecked.take(maxRows).forEach { item ->
            val row = RemoteViews(context.packageName, R.layout.notification_row)
            val label = if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name
            row.setTextViewText(R.id.notif_row_text, label)
            row.setOnClickPendingIntent(R.id.notif_row_root, toggleItemPendingIntent(context, item.id))
            view.addView(R.id.notif_rows_container, row)
        }

        val extra = unchecked.size - maxRows
        if (extra > 0) {
            view.setTextViewText(
                R.id.notif_more,
                context.getString(R.string.notification_more_items, extra),
            )
            view.setViewVisibility(R.id.notif_more, android.view.View.VISIBLE)
        } else {
            view.setViewVisibility(R.id.notif_more, android.view.View.GONE)
        }

        return view
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
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
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
