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

    override suspend fun getUnchecked(): List<ShoppingItem> = state.value.filter { !it.isChecked }

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
}
