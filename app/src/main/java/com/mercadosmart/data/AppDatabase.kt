package com.mercadosmart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mercadosmart.data.dao.MonthlySummaryDao
import com.mercadosmart.data.dao.PaymentBudgetDao
import com.mercadosmart.data.dao.PurchaseHistoryDao
import com.mercadosmart.data.dao.ReminderDao
import com.mercadosmart.data.dao.ShoppingItemDao
import com.mercadosmart.data.model.MonthlySummary
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.PurchaseHistory
import com.mercadosmart.data.model.Reminder
import com.mercadosmart.data.model.ShoppingItem

@Database(
    entities = [ShoppingItem::class, PurchaseHistory::class, Reminder::class, PaymentBudget::class, MonthlySummary::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun paymentBudgetDao(): PaymentBudgetDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN pricePerKg REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN weightAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN packageSize REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN packageSizeUnit TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mercado_smart.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
