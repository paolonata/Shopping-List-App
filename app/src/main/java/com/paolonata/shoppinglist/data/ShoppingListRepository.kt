package com.paolonata.shoppinglist.data

import com.paolonata.shoppinglist.parser.ParsedItem
import kotlinx.coroutines.flow.Flow

class ShoppingListRepository(private val dao: ShoppingItemDao) {

    fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll()

    suspend fun getAllOnce(): List<ShoppingItem> = dao.getAllOnce()

    suspend fun toggleChecked(item: ShoppingItem) {
        dao.update(item.copy(isChecked = !item.isChecked))
    }

    /** Usata dalla notifica persistente: spunta un articolo conoscendone solo l'id. */
    suspend fun markCheckedById(id: Long) {
        val item = dao.getById(id) ?: return
        if (!item.isChecked) dao.update(item.copy(isChecked = true))
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
        dao.update(item.copy(name = trimmed, quantity = quantity.coerceAtLeast(1)))
    }

    /**
     * Persiste un nuovo ordine per [orderedItems] (tipicamente la sola sezione "da comprare"):
     * riassegna la `position` in base all'indice nella lista, in un'unica transazione.
     */
    suspend fun reorderItems(orderedItems: List<ShoppingItem>) {
        if (orderedItems.isEmpty()) return
        val withNewPositions = orderedItems.mapIndexed { index, item ->
            item.copy(position = index.toLong())
        }
        dao.updateAll(withNewPositions)
    }

    /**
     * Adds [parsedItems] to the list. An item whose name matches (case-insensitively) an
     * existing, still-unchecked item has its quantity merged into that row instead of
     * creating a duplicate entry.
     */
    suspend fun addParsedItems(parsedItems: List<ParsedItem>) {
        if (parsedItems.isEmpty()) return

        val existingByName = dao.getUnchecked().associateBy { it.name.trim().lowercase() }
        var nextPosition = dao.getMaxPosition()
        val toInsert = mutableListOf<ShoppingItem>()

        for (parsed in parsedItems) {
            val key = parsed.name.trim().lowercase()
            val existing = existingByName[key]
            if (existing != null) {
                dao.update(
                    existing.copy(
                        quantity = existing.quantity + parsed.quantity,
                        note = existing.note ?: parsed.note,
                    ),
                )
            } else {
                nextPosition += 1
                toInsert.add(
                    ShoppingItem(
                        name = parsed.name,
                        quantity = parsed.quantity,
                        note = parsed.note,
                        position = nextPosition,
                    ),
                )
            }
        }
        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert)
        }
    }
}
