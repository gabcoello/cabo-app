package com.mercadosmart.data.dao

import androidx.room.*
import com.mercadosmart.data.model.MonthlySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySummaryDao {

    @Query("SELECT * FROM monthly_summaries ORDER BY year DESC, month DESC")
    fun getAll(): Flow<List<MonthlySummary>>

    @Query("SELECT * FROM monthly_summaries ORDER BY year DESC, month DESC")
    suspend fun getAllOnce(): List<MonthlySummary>

    @Query("SELECT * FROM monthly_summaries WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getByMonthKey(monthKey: String): MonthlySummary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: MonthlySummary)
}
