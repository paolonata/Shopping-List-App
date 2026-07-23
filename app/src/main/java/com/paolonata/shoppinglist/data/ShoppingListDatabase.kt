package com.paolonata.shoppinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShoppingItem::class], version = 1, exportSchema = false)
abstract class ShoppingListDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile
        private var instance: ShoppingListDatabase? = null

        fun getInstance(context: Context): ShoppingListDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingListDatabase::class.java,
                    "shopping_list.db",
                ).build().also { instance = it }
            }
    }
}
