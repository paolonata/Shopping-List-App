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
 * Riceve il tocco su una riga della notifica persistente e spunta l'articolo, funzionando
 * anche se l'app non è aperta in primo piano (né il telefono sbloccato del tutto).
 */
class ShoppingListActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_ITEM) return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ShoppingListRepository(
                    ShoppingListDatabase.getInstance(context.applicationContext).shoppingItemDao(),
                )
                repository.markCheckedById(itemId)
                if (NotificationPrefs.isEnabled(context.applicationContext)) {
                    val items = repository.getAllOnce()
                    ShoppingListNotifier.show(context.applicationContext, items)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ITEM = "com.paolonata.shoppinglist.action.TOGGLE_ITEM"
        const val EXTRA_ITEM_ID = "extra_item_id"
    }
}
