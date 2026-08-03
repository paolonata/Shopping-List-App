package com.paolonata.shoppinglist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.data.ShoppingListRepository
import com.paolonata.shoppinglist.parser.ParsedItem
import com.paolonata.shoppinglist.parser.WhatsAppListParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(
        ShoppingListDatabase.getInstance(application).shoppingItemDao(),
    )

    val items: StateFlow<List<ShoppingItem>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Text handed off from a share-sheet intent (e.g. shared from WhatsApp), consumed once. */
    private val _pendingShareText = MutableStateFlow<String?>(null)
    val pendingShareText: StateFlow<String?> = _pendingShareText

    fun onShareTextReceived(text: String) {
        _pendingShareText.value = text
    }

    fun consumePendingShareText() {
        _pendingShareText.value = null
    }

    fun previewParse(text: String) = WhatsAppListParser.parse(text)

    /**
     * [quantityOverride] è la quantità scelta con lo stepper nella barra di aggiunta rapida:
     * si applica solo se diverso dal default (1), così "2 mele" scritto a testo continua a dare
     * quantità 2 quando lo stepper non è stato toccato, ma se l'utente lo porta a 3 il testo
     * intero ("2 mele") diventa il nome e non viene ri-concatenato/ri-parsato come "3 2 mele".
     */
    fun addItemsFromText(text: String, quantityOverride: Int = 1, onDone: (count: Int) -> Unit = {}) {
        val parsed = WhatsAppListParser.parse(text).let { items ->
            if (quantityOverride > 1) items.map { it.copy(quantity = quantityOverride) } else items
        }
        addParsedItems(parsed, onDone)
    }

    /** Aggiunge direttamente una lista già interpretata (es. l'anteprima con quantità modificate). */
    fun addParsedItems(items: List<ParsedItem>, onDone: (count: Int) -> Unit = {}) {
        viewModelScope.launch {
            // Il conteggio effettivo (articoli uniti o creati) invece di items.size: rimane
            // corretto anche se qualche articolo viene scartato (es. nome vuoto).
            val appliedCount = repository.addParsedItems(items)
            onDone(appliedCount)
        }
    }

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch { repository.toggleChecked(item) }
    }

    fun updateItem(item: ShoppingItem, name: String, quantity: Int) {
        viewModelScope.launch { repository.updateItemDetails(item, name, quantity) }
    }

    fun reorderItems(orderedItems: List<ShoppingItem>) {
        viewModelScope.launch { repository.reorderItems(orderedItems) }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun clearChecked() {
        viewModelScope.launch { repository.clearChecked() }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
