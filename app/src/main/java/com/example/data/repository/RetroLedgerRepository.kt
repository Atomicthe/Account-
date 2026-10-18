package com.example.data.repository

import android.content.Context
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import com.example.data.room.AppDatabase
import kotlinx.coroutines.flow.Flow

class RetroLedgerRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val recurringBillDao = db.recurringBillDao()

    fun observeAccounts(userId: String): Flow<List<LedgerAccount>> = accountDao.observeAccounts(userId)
    suspend fun saveAccount(account: LedgerAccount) = accountDao.insert(account)
    suspend fun deleteAccount(account: LedgerAccount) = accountDao.delete(account)

    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> = transactionDao.observeTransactions(userId)
    suspend fun saveTransaction(transaction: LedgerTransaction) = transactionDao.insert(transaction)
    suspend fun deleteTransaction(transaction: LedgerTransaction) = transactionDao.delete(transaction)

    fun observeBudgets(userId: String): Flow<List<LedgerBudget>> = budgetDao.observeBudgets(userId)
    suspend fun saveBudget(budget: LedgerBudget) = budgetDao.insert(budget)

    fun observeRecurring(userId: String): Flow<List<RecurringBill>> = recurringBillDao.observeRecurring(userId)
    suspend fun saveRecurring(bill: RecurringBill) = recurringBillDao.insert(bill)
    suspend fun toggleRecurringActive(billId: String, active: Boolean) = recurringBillDao.updateActive(billId, active)

    suspend fun seedInitialDataIfEmpty(userId: String) {
        // Local only, no seeding needed
    }
}
