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

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAllAccounts(userId: String): List<LedgerAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: LedgerAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<LedgerAccount>)

    @Delete
    suspend fun delete(account: LedgerAccount)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMs DESC")
    suspend fun getAllTransactions(userId: String): List<LedgerTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: LedgerTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<LedgerTransaction>)

    @Delete
    suspend fun delete(transaction: LedgerTransaction)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun observeBudgets(userId: String): Flow<List<LedgerBudget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getAllBudgets(userId: String): List<LedgerBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: LedgerBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<LedgerBudget>)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Delete
    suspend fun delete(budget: LedgerBudget)
}

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills WHERE userId = :userId")
    fun observeRecurring(userId: String): Flow<List<RecurringBill>>

    @Query("SELECT * FROM recurring_bills WHERE userId = :userId")
    suspend fun getAllRecurring(userId: String): List<RecurringBill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: RecurringBill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<RecurringBill>)

    @Query("UPDATE recurring_bills SET isActive = :isActive WHERE id = :id")
    suspend fun updateActive(id: String, isActive: Boolean)

    @Query("DELETE FROM recurring_bills WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}

@Dao
interface TravelDao {
    @Query("SELECT * FROM travels WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTravels(userId: String): Flow<List<com.example.data.model.TravelLedger>>

    @Query("SELECT * FROM travels WHERE id = :id LIMIT 1")
    suspend fun getTravel(id: String): com.example.data.model.TravelLedger?

    @Query("SELECT * FROM travels WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllTravels(userId: String): List<com.example.data.model.TravelLedger>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(travel: com.example.data.model.TravelLedger)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(travels: List<com.example.data.model.TravelLedger>)

    @Delete
    suspend fun delete(travel: com.example.data.model.TravelLedger)

    @Query("DELETE FROM travels WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
