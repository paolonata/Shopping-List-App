package com.paolonata.shoppinglist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.paolonata.shoppinglist.parser.ParsedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, position ASC")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, position ASC")
    suspend fun getAllOnce(): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE isChecked = 0")
    suspend fun getUnchecked(): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getById(id: Long): ShoppingItem?

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(position), 0) FROM shopping_items")
    suspend fun getMaxPosition(): Long

    // Query mirate a un singolo campo invece di @Update sull'intera riga: @Update riscrive
    // name/quantity/note/isChecked/position con lo snapshot che la UI aveva in mano, quindi
    // due azioni concorrenti (es. spunta da notifica + fine di un drag) potevano annullarsi a
    // vicenda ("lost update"). Con una query mirata, ogni azione tocca solo il proprio campo.
    @Query("UPDATE shopping_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE shopping_items SET isChecked = 0, position = :position WHERE id = :id")
    suspend fun uncheckWithNewPosition(id: Long, position: Long)

    @Query("UPDATE shopping_items SET name = :name, quantity = :quantity WHERE id = :id")
    suspend fun updateNameAndQuantity(id: Long, name: String, quantity: Int)

    @Query("UPDATE shopping_items SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Long)

    /** Riassegna in blocco (una transazione) le posizioni degli articoli riordinati. */
    @Transaction
    suspend fun updatePositions(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setPosition(id, index.toLong()) }
    }

    /**
     * Unisce/inserisce [parsedItems] in un'unica transazione: un articolo il cui nome
     * corrisponde (case-insensitive) a un articolo non ancora spuntato — già in lista o
     * comparso prima nello stesso batch — ha la quantità sommata invece di creare una riga
     * duplicata. Senza @Transaction, due batch concorrenti (es. import + aggiunta rapida quasi
     * simultanei) potevano leggere lo stesso snapshot "nessun articolo ancora" e creare due
     * righe separate invece di sommare le quantità in una sola.
     *
     * Ritorna il numero di articoli di [parsedItems] effettivamente applicati (uniti o
     * inseriti), per un conteggio corretto da mostrare all'utente.
     */
    @Transaction
    suspend fun addParsedItemsTransactional(parsedItems: List<ParsedItem>): Int {
        if (parsedItems.isEmpty()) return 0

        val existingByName = getUnchecked().associateBy { it.name.trim().lowercase() }.toMutableMap()
        val stagedByName = LinkedHashMap<String, ShoppingItem>()
        var nextPosition = getMaxPosition()
        var appliedCount = 0

        for (parsed in parsedItems) {
            val trimmedName = parsed.name.trim()
            if (trimmedName.isEmpty()) continue
            appliedCount++
            val key = trimmedName.lowercase()

            val staged = stagedByName[key]
            if (staged != null) {
                stagedByName[key] = staged.copy(
                    quantity = staged.quantity + parsed.quantity,
                    note = staged.note ?: parsed.note,
                )
                continue
            }

            val existing = existingByName[key]
            if (existing != null) {
                val merged = existing.copy(
                    quantity = existing.quantity + parsed.quantity,
                    note = existing.note ?: parsed.note,
                )
                update(merged)
                existingByName[key] = merged
            } else {
                nextPosition += 1
                stagedByName[key] = ShoppingItem(
                    name = trimmedName,
                    quantity = parsed.quantity,
                    note = parsed.note,
                    position = nextPosition,
                )
            }
        }

        if (stagedByName.isNotEmpty()) insertAll(stagedByName.values.toList())
        return appliedCount
    }
}
