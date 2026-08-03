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
 * La notifica persistente non sopravvive a un riavvio del telefono (il sistema la elimina
 * insieme a tutto lo stato delle app in background). Se l'utente l'aveva attivata, la
 * ripostiamo qui non appena il boot è completo, invece di lasciarla sparita finché non riapre
 * l'app a mano.
 */
class ShoppingListBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!NotificationPrefs.isEnabled(context.applicationContext)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ShoppingListRepository(
                    ShoppingListDatabase.getInstance(context.applicationContext).shoppingItemDao(),
                )
                ShoppingListNotifier.show(context.applicationContext, repository.getAllOnce())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
