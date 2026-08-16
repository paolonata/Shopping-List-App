package com.paolonata.shoppinglist.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.data.ReceiptRepository
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.data.Receipt
import com.paolonata.shoppinglist.data.ShoppingListDatabase
import com.paolonata.shoppinglist.receipts.DeadlineKind
import com.paolonata.shoppinglist.receipts.DeadlineStatus
import com.paolonata.shoppinglist.receipts.Deadlines
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A deadline that is still worth mentioning, with the receipt it belongs to. */
data class UpcomingDeadline(
    val entry: ReceiptWithPhotos,
    val kind: DeadlineKind,
    val status: DeadlineStatus,
)

class ReceiptsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReceiptRepository(
        application,
        ShoppingListDatabase.getInstance(application).receiptDao(),
    )

    val receipts: StateFlow<List<ReceiptWithPhotos>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<ReceiptCategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * What is about to run out. A return already made is not a deadline
     * any more: it would keep calling for nothing.
     */
    val deadlines: StateFlow<List<UpcomingDeadline>> = repository.observeAll()
        .map { list ->
            val today = LocalDate.now()
            list.flatMap { entry ->
                buildList {
                    val r = entry.receipt
                    if (r.returnUntil != null && r.returnDoneAt == null) {
                        parse(r.returnUntil)?.let {
                            add(UpcomingDeadline(entry, DeadlineKind.RETURN, Deadlines.status(it, today)))
                        }
                    }
                    if (r.warrantyUntil != null) {
                        parse(r.warrantyUntil)?.let {
                            add(UpcomingDeadline(entry, DeadlineKind.WARRANTY, Deadlines.status(it, today)))
                        }
                    }
                }
            }.sortedBy { it.status.days }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Il cestino si svuota da solo dopo la finestra di ripensamento.
        viewModelScope.launch { repository.purgeExpiredTrash() }
    }

    fun addFromPhotos(sources: List<Uri>, onSaved: (Long) -> Unit = {}) {
        if (sources.isEmpty()) return
        viewModelScope.launch {
            // La lettura automatica arriva nella tappa successiva: per ora lo
            // scontrino nasce con la sua foto e la data di oggi, che è già
            // abbastanza per non perderlo.
            val id = repository.createFrom(sources, parsed = null)
            onSaved(id)
        }
    }

    fun open(id: Long) = repository.observeById(id)

    fun save(receipt: Receipt) {
        viewModelScope.launch { repository.save(receipt) }
    }

    fun createCategory(label: String, emoji: String) {
        viewModelScope.launch { repository.createCategory(label, emoji) }
    }

    fun updateCategory(category: ReceiptCategoryEntity, label: String, emoji: String) {
        viewModelScope.launch { repository.updateCategory(category, label, emoji) }
    }

    fun deleteCategory(category: ReceiptCategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category.id) }
    }

    fun setReturnDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setReturnDone(id, done) }
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch { repository.moveToTrash(id) }
    }

    private fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()
}
