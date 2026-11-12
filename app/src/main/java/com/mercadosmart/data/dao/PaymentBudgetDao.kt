package com.mercadosmart.data.dao

import androidx.room.*
import com.mercadosmart.data.model.PaymentBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentBudgetDao {

    @Query("SELECT * FROM payment_budgets ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<PaymentBudget>>

    @Query("SELECT * FROM payment_budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PaymentBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: PaymentBudget): Long

    @Update
    suspend fun update(budget: PaymentBudget)

    @Delete
    suspend fun delete(budget: PaymentBudget)
}
