package com.paolonata.shoppinglist.data

import com.paolonata.shoppinglist.parser.ParsedItem
import kotlinx.coroutines.flow.Flow

class ShoppingListRepository(private val dao: ShoppingItemDao) {

    fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll()

    suspend fun getAllOnce(): List<ShoppingItem> = dao.getAllOnce()

    suspend fun toggleChecked(item: ShoppingItem) {
        if (item.isChecked) {
            // Una nuova posizione (in fondo) invece di lasciare quella vecchia: altrimenti,
            // ri-despuntando un articolo, può ritrovarsi con la stessa position di un altro e
            // l'ordinamento (isChecked ASC, position ASC) diventa non deterministico.
            val newPosition = dao.getMaxPosition() + 1
            dao.uncheckWithNewPosition(item.id, newPosition)
        } else {
            dao.setChecked(item.id, true)
        }
    }

    /** Usata dalla notifica persistente: spunta un articolo conoscendone solo l'id. */
    suspend fun markCheckedById(id: Long) {
        dao.setChecked(id, true)
    }

    suspend fun deleteItem(item: ShoppingItem) {
        dao.delete(item)
    }

    suspend fun clearChecked() {
        dao.deleteChecked()
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    /** Modifica nome e quantità di un articolo già presente. */
    suspend fun updateItemDetails(item: ShoppingItem, name: String, quantity: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.updateNameAndQuantity(item.id, trimmed, quantity.coerceAtLeast(1))
    }

    /**
     * Persiste un nuovo ordine per [orderedItems] (tipicamente la sola sezione "da comprare"):
     * riassegna la `position` in base all'indice nella lista, in un'unica transazione. Tocca
     * solo il campo `position` (non l'intera riga), così non può annullare una spunta o una
     * modifica fatta nel frattempo su un altro articolo.
     */
    suspend fun reorderItems(orderedItems: List<ShoppingItem>) {
        if (orderedItems.isEmpty()) return
        dao.updatePositions(orderedItems.map { it.id })
    }

    /**
     * Adds [parsedItems] to the list. An item whose name matches (case-insensitively) an
     * existing, still-unchecked item — already in the list, or seen earlier in this same
     * batch — has its quantity merged into that row instead of creating a duplicate entry.
     * Runs in a single DB transaction. Returns how many of [parsedItems] were actually applied.
     */
    suspend fun addParsedItems(parsedItems: List<ParsedItem>): Int =
        dao.addParsedItemsTransactional(parsedItems)
}
