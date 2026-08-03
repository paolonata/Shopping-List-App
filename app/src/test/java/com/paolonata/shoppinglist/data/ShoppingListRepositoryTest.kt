package com.paolonata.shoppinglist.data

import com.paolonata.shoppinglist.parser.ParsedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeShoppingItemDao : ShoppingItemDao {
    private val state = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ShoppingItem>> = state

    override suspend fun getAllOnce(): List<ShoppingItem> = state.value

    override suspend fun getUnchecked(): List<ShoppingItem> = state.value.filter { !it.isChecked }

    override suspend fun getById(id: Long): ShoppingItem? = state.value.firstOrNull { it.id == id }

    override suspend fun insertAll(items: List<ShoppingItem>) {
        val withIds = items.map { it.copy(id = nextId++) }
        state.value = state.value + withIds
    }

    override suspend fun update(item: ShoppingItem) {
        state.value = state.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(item: ShoppingItem) {
        state.value = state.value.filterNot { it.id == item.id }
    }

    override suspend fun deleteChecked() {
        state.value = state.value.filterNot { it.isChecked }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    override suspend fun getMaxPosition(): Long = state.value.maxOfOrNull { it.position } ?: 0

    override suspend fun setChecked(id: Long, checked: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isChecked = checked) else it }
    }

    override suspend fun uncheckWithNewPosition(id: Long, position: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(isChecked = false, position = position) else it }
    }

    override suspend fun updateNameAndQuantity(id: Long, name: String, quantity: Int) {
        state.value = state.value.map { if (it.id == id) it.copy(name = name, quantity = quantity) else it }
    }

    override suspend fun setPosition(id: Long, position: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(position = position) else it }
    }
}

class ShoppingListRepositoryTest {

    @Test
    fun `adding items twice merges quantity into the existing unchecked row`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Pere", quantity = 2)))
        repository.addParsedItems(listOf(ParsedItem(name = "pere", quantity = 3)))

        val stored = dao.getUnchecked()
        assertEquals(1, stored.size)
        assertEquals(5, stored.single().quantity)
    }

    @Test
    fun `a checked item is not merged into and instead a new row is created`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Latte", quantity = 1)))
        repository.toggleChecked(dao.getUnchecked().single())
        repository.addParsedItems(listOf(ParsedItem(name = "Latte", quantity = 1)))

        assertEquals(1, dao.getUnchecked().size)
    }

    @Test
    fun `manual items are preserved when later importing from whatsapp`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        // Aggiunta manuale
        repository.addParsedItems(listOf(ParsedItem(name = "Latte"), ParsedItem(name = "Pane")))
        // Importazione da WhatsApp (più articoli, uno in comune)
        repository.addParsedItems(
            listOf(ParsedItem(name = "Pane"), ParsedItem(name = "Mele", quantity = 2), ParsedItem(name = "Uova")),
        )
        // Seconda importazione da WhatsApp
        repository.addParsedItems(listOf(ParsedItem(name = "Acqua")))

        val names = dao.getUnchecked().map { it.name }.toSet()
        assertEquals(setOf("Latte", "Pane", "Mele", "Uova", "Acqua"), names)
        // "Pane" unito (non duplicato), niente è stato cancellato
        assertEquals(1, dao.getUnchecked().count { it.name == "Pane" })
    }

    @Test
    fun `clearing checked items only removes checked rows`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Pane"), ParsedItem(name = "Uova")))
        repository.toggleChecked(dao.getUnchecked().first { it.name == "Pane" })
        repository.clearChecked()

        val remaining = dao.getUnchecked()
        assertEquals(1, remaining.size)
        assertEquals("Uova", remaining.single().name)
    }

    @Test
    fun `updateItemDetails changes name and quantity`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Latte")))
        val item = dao.getUnchecked().single()
        repository.updateItemDetails(item, "Latte di soia", 3)

        val updated = dao.getUnchecked().single()
        assertEquals("Latte di soia", updated.name)
        assertEquals(3, updated.quantity)
    }

    @Test
    fun `updateItemDetails ignores a blank name`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Latte")))
        val item = dao.getUnchecked().single()
        repository.updateItemDetails(item, "   ", 5)

        val unchanged = dao.getUnchecked().single()
        assertEquals("Latte", unchanged.name)
        assertEquals(1, unchanged.quantity)
    }

    @Test
    fun `reorderItems persists the new order via position`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(
            listOf(ParsedItem(name = "Pane"), ParsedItem(name = "Latte"), ParsedItem(name = "Uova")),
        )
        val current = dao.getUnchecked().sortedBy { it.position }
        val reversed = current.reversed()

        repository.reorderItems(reversed)

        val afterReorder = dao.getUnchecked().sortedBy { it.position }
        assertEquals(reversed.map { it.name }, afterReorder.map { it.name })
    }

    @Test
    fun `markCheckedById marks the item as checked`() = runBlocking {
        val dao = FakeShoppingItemDao()
        val repository = ShoppingListRepository(dao)

        repository.addParsedItems(listOf(ParsedItem(name = "Pane")))
        val item = dao.getUnchecked().single()

        repository.markCheckedById(item.id)

        assertEquals(true, dao.getById(item.id)?.isChecked)
    }
}
