package com.paolonata.shoppinglist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, position ASC")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE isChecked = 0")
    suspend fun getUnchecked(): List<ShoppingItem>

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
}
