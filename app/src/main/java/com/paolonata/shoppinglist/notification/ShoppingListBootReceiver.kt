package com.paolonata.shoppinglist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.data.ShoppingListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * La notifica persistente e gli allarmi di [android.app.AlarmManager] non sopravvivono a un
 * riavvio del telefono (il sistema li elimina insieme a tutto lo stato delle app in
 * background). Se l'utente aveva attivato la notifica persistente e/o un promemoria per la
 * lista, li ripristiniamo qui non appena il boot è completo. Lo stesso vale per il controllo
 * quotidiano delle scadenze degli scontrini.
 */
class ShoppingListBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (NotificationPrefs.isEnabled(appContext)) {
                    val repository = ShoppingListRepository(
                        ShoppingListDatabase.getInstance(appContext).shoppingItemDao(),
                    )
                    ShoppingListNotifier.show(appContext, repository.getAllOnce())
                }
                ShoppingReminderScheduler.rescheduleIfNeeded(appContext)
                ReceiptDeadlineScheduler.rescheduleIfNeeded(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
