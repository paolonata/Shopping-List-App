package com.paolonata.shoppinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShoppingItem::class, Receipt::class, ReceiptPhoto::class],
    version = 2,
    exportSchema = false,
)
abstract class ShoppingListDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao

    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var instance: ShoppingListDatabase? = null

        /**
         * v1 → v2: arrivano gli scontrini.
         *
         * Scritta a mano invece di `fallbackToDestructiveMigration`, che
         * qui non è più accettabile: la lista della spesa si rifà in due
         * minuti, le foto degli scontrini no. Aggiunge soltanto tabelle
         * nuove, quindi non tocca niente di quello che c'era già.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `receipts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount_cents` INTEGER,
                        `currency` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `category_id` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `return_until` TEXT,
                        `return_days` INTEGER,
                        `return_done_at` INTEGER,
                        `warranty_until` TEXT,
                        `warranty_years` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `deleted_at` INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `receipt_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `receipt_id` INTEGER NOT NULL,
                        `path` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `size_bytes` INTEGER NOT NULL,
                        FOREIGN KEY(`receipt_id`) REFERENCES `receipts`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_receipt_photos_receipt_id` ON `receipt_photos` (`receipt_id`)",
                )
            }
        }

        fun getInstance(context: Context): ShoppingListDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingListDatabase::class.java,
                    "shopping_list.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
