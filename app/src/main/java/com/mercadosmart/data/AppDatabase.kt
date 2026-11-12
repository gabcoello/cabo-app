package com.mercadosmart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mercadosmart.data.dao.PurchaseHistoryDao
import com.mercadosmart.data.dao.ReminderDao
import com.mercadosmart.data.dao.ShoppingItemDao
import com.mercadosmart.data.model.PurchaseHistory
import com.mercadosmart.data.model.Reminder
import com.mercadosmart.data.model.ShoppingItem

@Database(
    entities = [ShoppingItem::class, PurchaseHistory::class, Reminder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mercado_smart.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
