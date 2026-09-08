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

    /** Quello che è stato buttato e si può ancora recuperare. */
    val trashed: StateFlow<List<ReceiptWithPhotos>> = repository.observeTrashed()
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
            // Lo scontrino viene salvato subito; la lettura del testo
            // arriva un attimo dopo e i campi si riempiono da soli.
            val id = repository.createFrom(sources)
            onSaved(id)
        }
    }

    /** Lo scontrino creato a mano dal foglio, con i campi già compilati. */
    fun createManual(
        sources: List<Uri>,
        title: String,
        amount: Double?,
        categoryId: String,
        returnDays: Int?,
        onSaved: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            onSaved(repository.createManual(sources, title, amount, categoryId, returnDays))
        }
    }

    /** Importa una ricevuta in PDF. Il callback riceve null se non si apre. */
    fun addFromPdf(uri: Uri, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch { onDone(repository.createFromPdf(uri)) }
    }

    fun open(id: Long) = repository.observeById(id)

    fun save(receipt: Receipt) {
        viewModelScope.launch { repository.save(receipt) }
    }

    /** Rilegge la foto: serve quando il primo scatto era storto o scuro. */
    fun rescan(id: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onDone(repository.rescan(id)) }
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

    fun restore(id: Long) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun purge(id: Long) {
        viewModelScope.launch { repository.purge(id) }
    }

    /**
     * Cancella davvero tutto: scontrini, foto e cestino. Serve prima di
     * prestare o rivendere il telefono, ed è irreversibile — chi la chiama
     * deve aver già chiesto conferma.
     */
    fun deleteEverything(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            (repository.allIncludingTrash()).forEach { repository.purge(it) }
            onDone()
        }
    }

    private fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()
}
