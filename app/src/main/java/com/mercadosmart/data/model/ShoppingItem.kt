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
    val createdAt: Long = System.currentTimeMillis(),
    // Campos para unidade kg / g (cálculo automático pelo peso)
    val pricePerKg: Double = 0.0,
    val weightAmount: Double = 0.0,
    // Campos informativos para pacote / caixa (diferenciação no comparador de preços)
    val packageSize: Double = 0.0,
    val packageSizeUnit: String = ""
) {
    /**
     * Retorna o texto resumido da quantidade/medida para exibição nos cards.
     */
    fun formattedMeasure(): String {
        return when {
            (unit == "kg" || unit == "g") && weightAmount > 0 -> {
                val w = if (weightAmount % 1.0 == 0.0) weightAmount.toInt().toString() else String.format("%.3f", weightAmount).trimEnd('0').trimEnd(',', '.')
                val base = "$w $unit"
                if (pricePerKg > 0) "$base (R$ ${String.format("%.2f", pricePerKg)}/kg)" else base
            }
            (unit == "pct" || unit == "cx") && packageSize > 0 && packageSizeUnit.isNotBlank() -> {
                val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                val s = if (packageSize % 1.0 == 0.0) packageSize.toInt().toString() else packageSize.toString()
                "$q $unit ($s$packageSizeUnit)"
            }
            else -> {
                val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                "$q $unit"
            }
        }
    }
}
