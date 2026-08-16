package com.paolonata.shoppinglist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A photographed receipt.
 *
 * The amount is kept in cents: money in a `Double` eventually shows up as
 * 43.899999999999999, and a total that does not add up is worse than no
 * total at all.
 *
 * Dates are ISO strings (`2026-08-16`) rather than timestamps — they are
 * days, not instants, and this way they sort correctly with a plain
 * `ORDER BY`. Photos live on disk, not in here: see [ReceiptPhoto].
 */
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long? = null,
    val currency: String = "EUR",
    /** Purchase date, ISO. */
    val date: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String = "altro",
    val note: String = "",
    val favorite: Boolean = false,

    /** Return window: the deadline, plus the shortcut it came from. */
    @ColumnInfo(name = "return_until")
    val returnUntil: String? = null,
    @ColumnInfo(name = "return_days")
    val returnDays: Int? = null,
    /** When the return was actually made: from then on it stops calling. */
    @ColumnInfo(name = "return_done_at")
    val returnDoneAt: Long? = null,

    @ColumnInfo(name = "warranty_until")
    val warrantyUntil: String? = null,
    @ColumnInfo(name = "warranty_years")
    val warrantyYears: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    /** Soft delete: the bin keeps things recoverable for a while. */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
) {
    val amount: Double? get() = amountCents?.let { it / 100.0 }

    companion object {
        fun centsOf(amount: Double?): Long? = amount?.let { Math.round(it * 100) }
    }
}

/**
 * One page of a receipt. The image file itself sits in the app's private
 * storage; only its path is stored here, so the database stays small and
 * the list scrolls fast however many receipts pile up.
 */
@Entity(
    tableName = "receipt_photos",
    foreignKeys = [
        ForeignKey(
            entity = Receipt::class,
            parentColumns = ["id"],
            childColumns = ["receipt_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("receipt_id")],
)
data class ReceiptPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "receipt_id")
    val receiptId: Long,
    /** Absolute path of the JPEG in the app's private files directory. */
    val path: String,
    /** Page order within the receipt. */
    val position: Int = 0,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long = 0,
)

/** A receipt together with its pages, as the list and detail screens need it. */
data class ReceiptWithPhotos(
    @androidx.room.Embedded val receipt: Receipt,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "receipt_id")
    val photos: List<ReceiptPhoto>,
)
