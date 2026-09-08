package com.paolonata.shoppinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShoppingItem::class, Receipt::class, ReceiptPhoto::class, ReceiptCategoryEntity::class],
    version = 6,
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

        /**
         * v2 → v3: le categorie escono dal codice ed entrano nel database,
         * per poterne aggiungere di proprie. Le otto di partenza vengono
         * seminate qui, così chi aggiorna se le ritrova già al loro posto.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `receipt_categories` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `label` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `built_in` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                ReceiptCategoryEntity.defaults().forEach { c ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `receipt_categories` " +
                            "(`id`, `label`, `emoji`, `built_in`, `position`) VALUES (?, ?, ?, 1, ?)",
                        arrayOf(c.id, c.label, c.emoji, c.position),
                    )
                }
            }
        }

        /** v3 → v4: si conserva il testo letto dalla foto. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `receipts` ADD COLUMN `ocr_text` TEXT")
            }
        }

        /** v4 → v5: dove hai comprato. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `receipts` ADD COLUMN `place_name` TEXT")
                db.execSQL("ALTER TABLE `receipts` ADD COLUMN `place_address` TEXT")
                db.execSQL("ALTER TABLE `receipts` ADD COLUMN `place_lat` REAL")
                db.execSQL("ALTER TABLE `receipts` ADD COLUMN `place_lon` REAL")
            }
        }

        /**
         * v5 → v6: quattro categorie in più (viaggi, bollette, lavoro,
         * svago), che il redesign mette fra quelle di partenza. Chi le
         * aveva già rinominate non se le vede toccare: `INSERT OR IGNORE`
         * aggiunge solo quello che manca. "Altro" scivola in fondo, dove
         * il disegno la vuole.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ReceiptCategoryEntity.defaults().forEach { c ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `receipt_categories` " +
                            "(`id`, `label`, `emoji`, `built_in`, `position`) VALUES (?, ?, ?, 1, ?)",
                        arrayOf(c.id, c.label, c.emoji, c.position),
                    )
                    db.execSQL(
                        "UPDATE `receipt_categories` SET `position` = ? WHERE `id` = ? AND `built_in` = 1",
                        arrayOf(c.position, c.id),
                    )
                }
            }
        }

        fun getInstance(context: Context): ShoppingListDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingListDatabase::class.java,
                    "shopping_list.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // Chi installa da zero non passa dalle migrazioni: le
                    // categorie di partenza vanno seminate anche qui.
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            ReceiptCategoryEntity.defaults().forEach { c ->
                                db.execSQL(
                                    "INSERT OR IGNORE INTO `receipt_categories` " +
                                        "(`id`, `label`, `emoji`, `built_in`, `position`) VALUES (?, ?, ?, 1, ?)",
                                    arrayOf(c.id, c.label, c.emoji, c.position),
                                )
                            }
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
