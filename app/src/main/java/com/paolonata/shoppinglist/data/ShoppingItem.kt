package com.paolonata.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val note: String? = null,
    val isChecked: Boolean = false,
    val position: Long = 0,
)
