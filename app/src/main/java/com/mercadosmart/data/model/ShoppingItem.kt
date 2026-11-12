package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "un",
    val price: Double = 0.0,
    val category: String = "Outros",
    val notes: String = "",
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
