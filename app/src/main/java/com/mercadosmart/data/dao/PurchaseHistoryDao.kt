package com.mercadosmart.data.dao

import androidx.room.*
import com.mercadosmart.data.model.PurchaseHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseHistoryDao {

    @Query("SELECT * FROM purchase_history ORDER BY date DESC")
    fun getAll(): Flow<List<PurchaseHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PurchaseHistory): Long

    @Delete
    suspend fun delete(history: PurchaseHistory)

    @Query("DELETE FROM purchase_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM purchase_history ORDER BY date DESC")
    suspend fun getAllOnce(): List<PurchaseHistory>
}
