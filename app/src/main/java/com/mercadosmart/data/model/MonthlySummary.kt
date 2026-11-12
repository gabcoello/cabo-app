package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class MethodOverBudgetDetail(
    val methodName: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val overAmount: Double,
    val overPercent: Double
)

data class PurchasePeriodActivity(
    val periodLabel: String,
    val purchaseCount: Int,
    val totalSpent: Double,
    val isPeak: Boolean = false
)

@Entity(tableName = "monthly_summaries")
data class MonthlySummary(
    @PrimaryKey val monthKey: String, // Formato "YYYY-MM", ex: "2026-11"
    val year: Int,
    val month: Int,                   // 1..12
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val overBudgetAmount: Double = 0.0,
    val overBudgetPercent: Double = 0.0,
    val methodBudgetsJson: String = "{}", // Mapa JSON de methodName -> budgetAmount fixado para o mês
    val methodDetailsJson: String = "[]", // List<MethodOverBudgetDetail>
    val peakPeriodsJson: String = "[]",   // List<PurchasePeriodActivity>
    val isClosed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
