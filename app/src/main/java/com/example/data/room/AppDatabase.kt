package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import com.example.data.model.UserProfile

@Database(entities = [UserProfile::class, LedgerAccount::class, LedgerTransaction::class, LedgerBudget::class, RecurringBill::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringBillDao(): RecurringBillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retro_budget_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
