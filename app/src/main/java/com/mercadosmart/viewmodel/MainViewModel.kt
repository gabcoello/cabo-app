package com.mercadosmart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercadosmart.data.AppDatabase
import com.mercadosmart.data.model.MethodOverBudgetDetail
import com.mercadosmart.data.model.MonthlySummary
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.PaymentTypes
import com.mercadosmart.data.model.PurchaseHistory
import com.mercadosmart.data.model.PurchaseItem
import com.mercadosmart.data.model.PurchasePeriodActivity
import com.mercadosmart.data.model.Reminder
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ProductPriceRecord(
    val place: String,
    val date: Long,
    val price: Double,
    val unit: String
)

data class ProductComparison(
    val name: String,
    val minPrice: Double,
    val bestPlace: String,
    val allPricesEqual: Boolean,
    val totalRecords: Int,
    val records: List<ProductPriceRecord>
)

data class ExpenseByMethod(
    val methodName: String,
    val totalSpent: Double,
    val purchaseCount: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val shoppingDao = db.shoppingItemDao()
    private val historyDao = db.purchaseHistoryDao()
    private val reminderDao = db.reminderDao()
    private val paymentBudgetDao = db.paymentBudgetDao()
    private val monthlySummaryDao = db.monthlySummaryDao()
    private val gson = Gson()

    // Resumos mensais armazenados (mês atual aberto + meses anteriores fechados)
    val monthlySummaries: StateFlow<List<MonthlySummary>> = monthlySummaryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthSummary: StateFlow<MonthlySummary?> = monthlySummaries
        .map { list ->
            val key = currentMonthKey()
            list.firstOrNull { it.monthKey == key } ?: list.firstOrNull()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            syncMonthlySummaries()
        }
    }

    // Orçamentos / Métodos de pagamento cadastrados
    val paymentBudgets: StateFlow<List<PaymentBudget>> = paymentBudgetDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Somatório total disponível para gastar
    val totalBudget: StateFlow<Double> = paymentBudgets
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Lista atual de compras
    val shoppingItems: StateFlow<List<ShoppingItem>> = shoppingDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Histórico de compras
    val historyList: StateFlow<List<PurchaseHistory>> = historyDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total de gastos acumulados (todas as compras feitas)
    val totalExpenses: StateFlow<Double> = historyList
        .map { list -> list.sumOf { it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Gastos detalhados por meio de pagamento
    val expensesByMethod: StateFlow<List<ExpenseByMethod>> = historyList
        .map { list ->
            list.groupBy { purchase ->
                purchase.paymentMethod.ifBlank { "Não informado" }
            }.map { (method, purchases) ->
                ExpenseByMethod(
                    methodName = method,
                    totalSpent = purchases.sumOf { it.total },
                    purchaseCount = purchases.size
                )
            }.sortedByDescending { it.totalSpent }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lembretes
    val reminders: StateFlow<List<Reminder>> = reminderDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Comparador de Preços (calculado a partir do histórico)
    val priceComparisons: StateFlow<List<ProductComparison>> = historyList.combine(MutableStateFlow("")) { history, _ ->
        val map = mutableMapOf<String, MutableList<ProductPriceRecord>>()

        history.forEach { purchase ->
            val type = object : TypeToken<List<PurchaseItem>>() {}.type
            val items: List<PurchaseItem> = try {
                gson.fromJson(purchase.itemsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            items.forEach { item ->
                val key = item.name.trim().lowercase()
                val list = map.getOrPut(key) { mutableListOf() }
                list.add(
                    ProductPriceRecord(
                        place = purchase.place,
                        date = purchase.date,
                        price = item.price,
                        unit = item.unit
                    )
                )
            }
        }

        map.map { (key, records) ->
            val sorted = records.sortedByDescending { it.date }
            val validPrices = sorted.filter { it.price > 0 }
            val minPrice = if (validPrices.isNotEmpty()) validPrices.minOf { it.price } else 0.0
            val bestPlace = validPrices.firstOrNull { it.price == minPrice }?.place ?: "-"
            val allPricesEqual = validPrices.size > 1 && validPrices.map { it.price }.distinct().size == 1

            ProductComparison(
                name = sorted.firstOrNull()?.let { records.first().place; key.replaceFirstChar { it.uppercase() } } ?: key,
                minPrice = minPrice,
                bestPlace = bestPlace,
                allPricesEqual = allPricesEqual,
                totalRecords = sorted.size,
                records = sorted
            )
        }.sortedByDescending { it.totalRecords }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ações de itens
    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.insert(item)
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.update(item)
        }
    }

    fun toggleItemCheck(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.update(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.delete(item)
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            shoppingDao.deleteAll()
        }
    }

    // Finalizar Compra
    fun finishPurchase(
        place: String,
        notes: String,
        currentItems: List<ShoppingItem>,
        total: Double,
        paymentBudgetId: Long? = null,
        fallbackPaymentType: String? = null
    ) {
        viewModelScope.launch {
            var paymentMethodName = ""
            if (paymentBudgetId != null) {
                val budget = paymentBudgetDao.getById(paymentBudgetId)
                if (budget != null) {
                    paymentMethodName = budget.displayName()
                    val newAmount = (budget.amount - total).coerceAtLeast(0.0)
                    paymentBudgetDao.update(
                        budget.copy(
                            amount = newAmount,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } else if (!fallbackPaymentType.isNullOrBlank()) {
                paymentMethodName = PaymentTypes.shortLabel(fallbackPaymentType)
            }

            val purchaseItems = currentItems.map {
                PurchaseItem(
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    price = it.price,
                    category = it.category
                )
            }

            val history = PurchaseHistory(
                place = place.trim(),
                total = total,
                paymentMethod = paymentMethodName,
                notes = notes.trim(),
                itemsJson = gson.toJson(purchaseItems)
            )

            historyDao.insert(history)

            // Deletar itens que foram marcados
            shoppingDao.deleteChecked()

            syncMonthlySummaries()
        }
    }

    // Ações de Orçamento / Métodos de Pagamento
    fun savePaymentBudget(budget: PaymentBudget) {
        viewModelScope.launch {
            val savedId = paymentBudgetDao.insert(budget.copy(updatedAt = System.currentTimeMillis()))
            val savedBudget = budget.copy(id = if (budget.id > 0) budget.id else savedId)
            if (savedBudget.rechargeDay != null && savedBudget.rechargeAmount > 0.0) {
                AlarmScheduler.scheduleBudgetRecharge(getApplication(), savedBudget)
            } else {
                AlarmScheduler.cancelBudgetRecharge(getApplication(), savedBudget)
            }
            syncMonthlySummaries(refreshCurrentBudgetsFromActive = true)
        }
    }

    fun deletePaymentBudget(budget: PaymentBudget) {
        viewModelScope.launch {
            AlarmScheduler.cancelBudgetRecharge(getApplication(), budget)
            paymentBudgetDao.delete(budget)
            syncMonthlySummaries(refreshCurrentBudgetsFromActive = true)
        }
    }

    // Ações de Histórico
    fun deleteHistory(history: PurchaseHistory) {
        viewModelScope.launch {
            historyDao.delete(history)
            syncMonthlySummaries()
        }
    }

    private fun currentMonthKey(): String {
        val cal = Calendar.getInstance()
        return String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun monthKeyForTimestamp(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    /**
     * Abre o resumo do mês atual automaticamente, fecha meses anteriores
     * e atualiza os cálculos de excesso por meio de pagamento e períodos de maior atividade.
     */
    private suspend fun syncMonthlySummaries(refreshCurrentBudgetsFromActive: Boolean = false) {
        val nowCal = Calendar.getInstance()
        val curYear = nowCal.get(Calendar.YEAR)
        val curMonth = nowCal.get(Calendar.MONTH) + 1
        val curKey = String.format("%04d-%02d", curYear, curMonth)

        val allHistory = historyDao.getAllOnce()
        val activeBudgets = paymentBudgetDao.getAllOnce()
        val existingSummaries = monthlySummaryDao.getAllOnce().associateBy { it.monthKey }

        val historyByMonth = allHistory.groupBy { monthKeyForTimestamp(it.date) }
        val allMonthKeys = (historyByMonth.keys + existingSummaries.keys + curKey).toSortedSet()

        allMonthKeys.forEach { monthKey ->
            val parts = monthKey.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: curYear
            val month = parts.getOrNull(1)?.toIntOrNull() ?: curMonth
            val isCurrentMonth = monthKey == curKey

            val existing = existingSummaries[monthKey]
            val monthPurchases = historyByMonth[monthKey].orEmpty()

            // Se já é um mês passado fechado e não houve alteração no histórico dele, mantém intacto
            if (!isCurrentMonth && existing != null && existing.isClosed && existing.totalSpent == monthPurchases.sumOf { it.total }) {
                return@forEach
            }

            val mapType = object : TypeToken<Map<String, Double>>() {}.type
            val storedBudgetsMap: Map<String, Double> = try {
                existing?.methodBudgetsJson?.let { gson.fromJson(it, mapType) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            val spentByMethod = monthPurchases
                .groupBy { it.paymentMethod.ifBlank { "Não informado" } }
                .mapValues { (_, list) -> list.sumOf { it.total } }

            val methodBudgetsMap: Map<String, Double> = when {
                isCurrentMonth && (existing == null || refreshCurrentBudgetsFromActive) -> {
                    activeBudgets.associate { budget ->
                        val name = budget.displayName()
                        val alreadySpent = spentByMethod[name] ?: 0.0
                        name to (budget.amount + alreadySpent)
                    }
                }
                storedBudgetsMap.isNotEmpty() -> storedBudgetsMap
                isCurrentMonth -> activeBudgets.associate { it.displayName() to it.amount }
                else -> emptyMap()
            }

            val allMethods = (methodBudgetsMap.keys + spentByMethod.keys).distinct()
            val methodDetails = allMethods.map { method ->
                val budgetVal = methodBudgetsMap[method] ?: 0.0
                val spentVal = spentByMethod[method] ?: 0.0
                val overVal = (spentVal - budgetVal).coerceAtLeast(0.0)
                val overPct = when {
                    overVal <= 0.0 -> 0.0
                    budgetVal > 0.0 -> (overVal / budgetVal) * 100.0
                    else -> 100.0
                }
                MethodOverBudgetDetail(
                    methodName = method,
                    budgetAmount = budgetVal,
                    spentAmount = spentVal,
                    overAmount = overVal,
                    overPercent = overPct
                )
            }.sortedByDescending { it.overAmount }

            val totalBudget = methodBudgetsMap.values.sum()
            val totalSpent = monthPurchases.sumOf { it.total }
            val overBudgetAmount = (totalSpent - totalBudget).coerceAtLeast(0.0)
            val overBudgetPercent = when {
                overBudgetAmount <= 0.0 -> 0.0
                totalBudget > 0.0 -> (overBudgetAmount / totalBudget) * 100.0
                else -> 100.0
            }

            val peakPeriods = computePeakPeriods(monthPurchases)

            monthlySummaryDao.insertOrUpdate(
                MonthlySummary(
                    monthKey = monthKey,
                    year = year,
                    month = month,
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                    overBudgetAmount = overBudgetAmount,
                    overBudgetPercent = overBudgetPercent,
                    methodBudgetsJson = gson.toJson(methodBudgetsMap),
                    methodDetailsJson = gson.toJson(methodDetails),
                    peakPeriodsJson = gson.toJson(peakPeriods),
                    isClosed = !isCurrentMonth,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun computePeakPeriods(purchases: List<PurchaseHistory>): List<PurchasePeriodActivity> {
        if (purchases.isEmpty()) return emptyList()

        val weekBuckets = linkedMapOf(
            "1ª Semana (01–07)" to mutableListOf<PurchaseHistory>(),
            "2ª Semana (08–14)" to mutableListOf<PurchaseHistory>(),
            "3ª Semana (15–21)" to mutableListOf<PurchaseHistory>(),
            "4ª Semana (22–31)" to mutableListOf<PurchaseHistory>()
        )

        purchases.forEach { purchase ->
            val cal = Calendar.getInstance().apply { timeInMillis = purchase.date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val bucketKey = when {
                day <= 7 -> "1ª Semana (01–07)"
                day <= 14 -> "2ª Semana (08–14)"
                day <= 21 -> "3ª Semana (15–21)"
                else -> "4ª Semana (22–31)"
            }
            weekBuckets[bucketKey]?.add(purchase)
        }

        val activeBuckets = weekBuckets.mapNotNull { (label, list) ->
            if (list.isEmpty()) null else Triple(label, list.size, list.sumOf { it.total })
        }

        val maxCount = activeBuckets.maxOfOrNull { it.second } ?: 0
        val maxSpentInTopCount = activeBuckets.filter { it.second == maxCount }.maxOfOrNull { it.third } ?: 0.0

        return activeBuckets
            .sortedWith(compareByDescending<Triple<String, Int, Double>> { it.second }.thenByDescending { it.third })
            .map { (label, count, spent) ->
                PurchasePeriodActivity(
                    periodLabel = label,
                    purchaseCount = count,
                    totalSpent = spent,
                    isPeak = count == maxCount && spent == maxSpentInTopCount
                )
            }
    }

    fun repeatPurchase(itemsJson: String) {
        viewModelScope.launch {
            val type = object : TypeToken<List<PurchaseItem>>() {}.type
            val items: List<PurchaseItem> = try {
                gson.fromJson(itemsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val shoppingItems = items.map {
                ShoppingItem(
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    price = it.price,
                    category = it.category,
                    isChecked = false
                )
            }
            shoppingDao.insertAll(shoppingItems)
        }
    }

    // Ações de Lembrete
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = reminderDao.insert(reminder)
            val savedReminder = reminder.copy(id = id)
            if (savedReminder.isActive) {
                AlarmScheduler.scheduleReminder(getApplication(), savedReminder)
            }
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val newActive = !reminder.isActive
            val updated = reminder.copy(isActive = newActive)
            reminderDao.update(updated)

            if (newActive) {
                AlarmScheduler.scheduleReminder(getApplication(), updated)
            } else {
                AlarmScheduler.cancelReminder(getApplication(), updated)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            AlarmScheduler.cancelReminder(getApplication(), reminder)
            reminderDao.delete(reminder)
        }
    }

    // Buscar histórico de preço de um item específico
    fun getLatestPriceFor(name: String): ProductPriceRecord? {
        val cleanName = name.trim().lowercase()
        val match = priceComparisons.value.firstOrNull { it.name.lowercase() == cleanName }
        return match?.records?.firstOrNull { it.price > 0 }
    }
}
