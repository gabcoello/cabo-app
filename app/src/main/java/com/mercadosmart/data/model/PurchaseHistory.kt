package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa um item individual dentro de uma compra finalizada.
 * Armazenado como JSON dentro de PurchaseHistory para simplicidade.
 */
data class PurchaseItem(
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "un",
    val price: Double = 0.0,
    val category: String = "Outros",
    val pricePerKg: Double = 0.0,
    val weightAmount: Double = 0.0,
    val packageSize: Double = 0.0,
    val packageSizeUnit: String? = ""
) {
    fun formattedMeasure(): String {
        val safeUnit = (unit as String?).orEmpty().ifBlank { "un" }
        val safePkgUnit = packageSizeUnit.orEmpty()
        return when {
            (safeUnit == "kg" || safeUnit == "g") && weightAmount > 0 -> {
                val w = if (weightAmount % 1.0 == 0.0) weightAmount.toInt().toString() else String.format("%.3f", weightAmount).trimEnd('0').trimEnd(',', '.')
                val base = "$w $safeUnit"
                if (pricePerKg > 0) "$base • R$ ${String.format("%.2f", pricePerKg)}/kg" else base
            }
            (safeUnit == "pct" || safeUnit == "cx") && packageSize > 0 && safePkgUnit.isNotBlank() -> {
                val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                val s = if (packageSize % 1.0 == 0.0) packageSize.toInt().toString() else packageSize.toString()
                "$q $safeUnit ($s$safePkgUnit) × R$ ${String.format("%.2f", price)}"
            }
            else -> {
                val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                "$q $safeUnit × R$ ${String.format("%.2f", price)}"
            }
        }
    }
}

/**
 * Registro completo de uma compra finalizada com local, data, total e itens.
 */
@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val place: String,
    val total: Double,
    val paymentMethod: String = "",
    val notes: String = "",
    val itemsJson: String = "[]" // JSON serializado de List<PurchaseItem>
)
