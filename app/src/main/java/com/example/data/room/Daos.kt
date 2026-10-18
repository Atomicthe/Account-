package com.example.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun observeAccounts(userId: String): Flow<List<LedgerAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: LedgerAccount)

    @Delete
    suspend fun delete(account: LedgerAccount)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: LedgerTransaction)

    @Delete
    suspend fun delete(transaction: LedgerTransaction)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun observeBudgets(userId: String): Flow<List<LedgerBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: LedgerBudget)
}

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills WHERE userId = :userId")
    fun observeRecurring(userId: String): Flow<List<RecurringBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: RecurringBill)

    @Query("UPDATE recurring_bills SET isActive = :isActive WHERE id = :id")
    suspend fun updateActive(id: String, isActive: Boolean)
}
