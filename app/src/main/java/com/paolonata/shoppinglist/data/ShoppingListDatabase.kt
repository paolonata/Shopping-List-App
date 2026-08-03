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
                )
                    // Nessuna migrazione scritta ancora: se lo schema cambia senza questo,
                    // l'app crasha all'avvio su ogni telefono con dati già salvati. I dati
                    // locali non sono critici (nessun backend), quindi ricreare il DB è
                    // preferibile a un crash loop.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
