package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object PaymentTypes {
    const val TICKET_ALIMENTACAO = "Ticket Alimentação"
    const val TICKET_REFEICAO = "Ticket Refeição"
    const val CREDIT = "Crédito"
    const val DEBIT = "Débito"
    const val CASH_PIX = "Dinheiro/Pix"

    val all = listOf(TICKET_ALIMENTACAO, TICKET_REFEICAO, CREDIT, DEBIT, CASH_PIX)

    fun shortLabel(type: String): String = when (type) {
        TICKET_ALIMENTACAO -> "Ticket Alim."
        TICKET_REFEICAO -> "Ticket Ref."
        CREDIT -> "Crédito"
        DEBIT -> "Débito"
        CASH_PIX -> "Dinheiro/Pix"
        else -> type
    }
}

@Entity(tableName = "payment_budgets")
data class PaymentBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // Ticket Alimentação, Ticket Refeição, Crédito, Débito, Dinheiro/Pix
    val label: String = "", // Apelido opcional, ex: "Alelo", "Nubank"
    val amount: Double = 0.0,
    val rechargeDay: Int? = null, // Dia do mês (1 a 31) em que o saldo é recarregado
    val rechargeAmount: Double = 0.0, // Valor da recarga mensal
    val lastRechargeMonthKey: String? = null, // Ex: "2026-11" para evitar recarga duplicada no mês
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun displayName(): String {
        val short = PaymentTypes.shortLabel(type)
        return if (label.isNotBlank()) "$short • ${label.trim()}" else short
    }
}
