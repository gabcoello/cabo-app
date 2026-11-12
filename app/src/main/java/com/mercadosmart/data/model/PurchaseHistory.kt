package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa um item individual dentro de uma compra finalizada.
 * Armazenado como JSON dentro de PurchaseHistory para simplicidade.
 */
data class PurchaseItem(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "un",
    val price: Double = 0.0,
    val category: String = "Outros"
)

/**
 * Registro completo de uma compra finalizada com local, data, total e itens.
 */
@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val place: String,
    val total: Double,
    val notes: String = "",
    val itemsJson: String = "[]" // JSON serializado de List<PurchaseItem>
)
