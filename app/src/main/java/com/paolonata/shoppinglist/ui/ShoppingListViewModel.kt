package com.paolonata.shoppinglist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.data.ShoppingListRepository
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
        viewModelScope.launch {
            repository.addParsedItems(parsed)
            onDone(parsed.size)
        }
    }

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch { repository.toggleChecked(item) }
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
