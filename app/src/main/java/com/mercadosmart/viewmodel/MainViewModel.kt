package com.mercadosmart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercadosmart.data.AppDatabase
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.PaymentTypes
import com.mercadosmart.data.model.PurchaseHistory
import com.mercadosmart.data.model.PurchaseItem
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
    private val gson = Gson()

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
        }
    }

    fun deletePaymentBudget(budget: PaymentBudget) {
        viewModelScope.launch {
            AlarmScheduler.cancelBudgetRecharge(getApplication(), budget)
            paymentBudgetDao.delete(budget)
        }
    }

    // Ações de Histórico
    fun deleteHistory(history: PurchaseHistory) {
        viewModelScope.launch {
            historyDao.delete(history)
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
