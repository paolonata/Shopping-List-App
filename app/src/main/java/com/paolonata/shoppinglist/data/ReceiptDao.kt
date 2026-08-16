package com.paolonata.shoppinglist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    /** Everything still in the archive, newest purchase first. */
    @Transaction
    @Query("SELECT * FROM receipts WHERE deleted_at IS NULL ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ReceiptWithPhotos>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeTrashed(): Flow<List<ReceiptWithPhotos>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptWithPhotos?>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptWithPhotos?

    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Update
    suspend fun update(receipt: Receipt)

    @Insert
    suspend fun insertPhotos(photos: List<ReceiptPhoto>)

    @Query("DELETE FROM receipt_photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)

    @Query("SELECT * FROM receipt_photos WHERE receipt_id = :receiptId ORDER BY position ASC")
    suspend fun photosOf(receiptId: Long): List<ReceiptPhoto>

    @Query("UPDATE receipts SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long)

    @Query("UPDATE receipts SET deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, now: Long)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun purge(id: Long)

    /** Receipts binned longer than the grace period: they can go for good. */
    @Transaction
    @Query("SELECT * FROM receipts WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun expiredTrash(before: Long): List<ReceiptWithPhotos>

    @Query("UPDATE receipts SET return_done_at = :doneAt, updated_at = :now WHERE id = :id")
    suspend fun setReturnDone(id: Long, doneAt: Long?, now: Long)

    @Query("UPDATE receipts SET favorite = :favorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)
}
