package com.paolonata.shoppinglist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paolonata.shoppinglist.receipts.ReceiptCategory

/**
 * Una categoria di spesa.
 *
 * Le otto di partenza vivevano in un enum, cioè nel codice: andavano bene
 * finché erano l'unica cosa possibile, ma una categoria che ti inventi tu
 * è un dato, e i dati stanno nel database. Le standard restano
 * riconoscibili da [builtIn] — si possono rinominare e cambiare di icona,
 * ma non eliminare, perché la lettura automatica degli scontrini ci
 * assegna sopra le sue ipotesi.
 */
@Entity(tableName = "receipt_categories")
data class ReceiptCategoryEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val emoji: String,
    @ColumnInfo(name = "built_in")
    val builtIn: Boolean = false,
    val position: Int = 0,
) {
    companion object {
        /** Le otto di partenza, con cui si semina il database. */
        fun defaults(): List<ReceiptCategoryEntity> =
            ReceiptCategory.entries.mapIndexed { index, c ->
                ReceiptCategoryEntity(
                    id = c.id,
                    label = c.label,
                    emoji = c.emoji,
                    builtIn = true,
                    position = index,
                )
            }

        /** Dove finiscono gli scontrini di una categoria eliminata. */
        const val FALLBACK_ID = "altro"
    }
}
