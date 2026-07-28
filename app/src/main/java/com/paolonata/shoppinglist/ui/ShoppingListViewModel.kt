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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(
        ShoppingListDatabase.getInstance(application).shoppingItemDao(),
    )

    val items: StateFlow<List<ShoppingItem>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val checkedCount: StateFlow<Int> = items
        .map { list -> list.count { it.isChecked } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

    fun addItemsFromText(text: String, onDone: (count: Int) -> Unit = {}) {
        val parsed = WhatsAppListParser.parse(text)
        addParsedItems(parsed, onDone)
    }

    /** Aggiunge direttamente una lista già interpretata (es. l'anteprima con quantità modificate). */
    fun addParsedItems(items: List<ParsedItem>, onDone: (count: Int) -> Unit = {}) {
        viewModelScope.launch {
            repository.addParsedItems(items)
            onDone(items.size)
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
