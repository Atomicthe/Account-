package com.example.data.repository

import android.content.Context
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import com.example.data.model.TravelLedger
import com.example.data.room.AppDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class RetroLedgerRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val recurringBillDao = db.recurringBillDao()
    private val travelDao = db.travelDao()

    fun observeAccounts(userId: String): Flow<List<LedgerAccount>> = accountDao.observeAccounts(userId)
    suspend fun saveAccount(account: LedgerAccount) = accountDao.insert(account)
    suspend fun deleteAccount(account: LedgerAccount) = accountDao.delete(account)

    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> = transactionDao.observeTransactions(userId)
    suspend fun saveTransaction(transaction: LedgerTransaction) = transactionDao.insert(transaction)
    suspend fun deleteTransaction(transaction: LedgerTransaction) = transactionDao.delete(transaction)

    fun observeBudgets(userId: String): Flow<List<LedgerBudget>> = budgetDao.observeBudgets(userId)
    suspend fun saveBudget(budget: LedgerBudget) = budgetDao.insert(budget)
    suspend fun deleteBudget(budget: LedgerBudget) = budgetDao.delete(budget)

    fun observeRecurring(userId: String): Flow<List<RecurringBill>> = recurringBillDao.observeRecurring(userId)
    suspend fun saveRecurring(bill: RecurringBill) = recurringBillDao.insert(bill)
    suspend fun toggleRecurringActive(billId: String, active: Boolean) = recurringBillDao.updateActive(billId, active)

    fun observeTravels(userId: String): Flow<List<TravelLedger>> = travelDao.observeTravels(userId)
    suspend fun saveTravel(travel: TravelLedger) = travelDao.insert(travel)
    suspend fun deleteTravel(travel: TravelLedger) = travelDao.delete(travel)

    suspend fun clearAllData(userId: String) {
        transactionDao.deleteAll(userId)
        accountDao.deleteAll(userId)
        budgetDao.deleteAll(userId)
        recurringBillDao.deleteAll(userId)
        travelDao.deleteAll(userId)
    }

    suspend fun exportBackupJson(userId: String): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("mode", "local_only")
        root.put("userId", userId)

        val accountsArr = JSONArray()
        accountDao.getAllAccounts(userId).forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type)
            obj.put("balance", acc.balance)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("currency", acc.currency)
            obj.put("icon", acc.icon)
            obj.put("color", acc.color)
            accountsArr.put(obj)
        }
        root.put("accounts", accountsArr)

        val txArr = JSONArray()
        transactionDao.getAllTransactions(userId).forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("accountId", tx.accountId)
            obj.put("toAccountId", tx.toAccountId)
            obj.put("category", tx.category)
            obj.put("subCategory", tx.subCategory)
            obj.put("date", tx.date)
            obj.put("timestampMs", tx.timestampMs)
            obj.put("note", tx.note)
            obj.put("merchant", tx.merchant)
            obj.put("tags", tx.tags)
            obj.put("stamp", tx.stamp)
            obj.put("currency", tx.currency)
            obj.put("originalAmount", tx.originalAmount)
            obj.put("travelId", tx.travelId)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        val budgetArr = JSONArray()
        budgetDao.getAllBudgets(userId).forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("category", b.category)
            obj.put("amount", b.amount)
            obj.put("month", b.month)
            budgetArr.put(obj)
        }
        root.put("budgets", budgetArr)

        val billArr = JSONArray()
        recurringBillDao.getAllRecurring(userId).forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("amount", r.amount)
            obj.put("type", r.type)
            obj.put("category", r.category)
            obj.put("accountId", r.accountId)
            obj.put("dueDay", r.dueDay)
            obj.put("isActive", r.isActive)
            billArr.put(obj)
        }
        root.put("recurringBills", billArr)

        val travelArr = JSONArray()
        travelDao.getAllTravels(userId).forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("destination", t.destination)
            obj.put("currency", t.currency)
            obj.put("budget", t.budget)
            obj.put("startDate", t.startDate)
            obj.put("endDate", t.endDate)
            obj.put("isActive", t.isActive)
            travelArr.put(obj)
        }
        root.put("travels", travelArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(userId: String, jsonStr: String): Int {
        val root = JSONObject(jsonStr)
        var restoredCount = 0

        if (root.has("accounts")) {
            val arr = root.getJSONArray("accounts")
            val list = mutableListOf<LedgerAccount>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LedgerAccount(
                        id = o.optString("id", "acc_${System.currentTimeMillis()}_$i"),
                        userId = userId,
                        name = o.optString("name", "帳戶"),
                        type = o.optString("type", "CASH"),
                        balance = o.optDouble("balance", 0.0),
                        initialBalance = o.optDouble("initialBalance", 0.0),
                        currency = o.optString("currency", "NT$"),
                        icon = o.optString("icon", "account_balance_wallet"),
                        color = o.optString("color", "#7A3E26")
                    )
                )
            }
            if (list.isNotEmpty()) {
                accountDao.insertAll(list)
                restoredCount += list.size
            }
        }

        if (root.has("transactions")) {
            val arr = root.getJSONArray("transactions")
            val list = mutableListOf<LedgerTransaction>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LedgerTransaction(
                        id = o.optString("id", "tx_${System.currentTimeMillis()}_$i"),
                        userId = userId,
                        type = o.optString("type", "EXPENSE"),
                        amount = o.optDouble("amount", 0.0),
                        accountId = o.optString("accountId", ""),
                        toAccountId = o.optString("toAccountId", ""),
                        category = o.optString("category", "餐飲"),
                        subCategory = o.optString("subCategory", ""),
                        date = o.optString("date", "2026-10-19"),
                        timestampMs = o.optLong("timestampMs", System.currentTimeMillis()),
                        note = o.optString("note", ""),
                        merchant = o.optString("merchant", ""),
                        tags = o.optString("tags", ""),
                        stamp = o.optString("stamp", "PAID"),
                        currency = o.optString("currency", "NT$"),
                        originalAmount = o.optDouble("originalAmount", 0.0),
                        travelId = o.optString("travelId", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                transactionDao.insertAll(list)
                restoredCount += list.size
            }
        }

        if (root.has("budgets")) {
            val arr = root.getJSONArray("budgets")
            val list = mutableListOf<LedgerBudget>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LedgerBudget(
                        id = o.optString("id", "budget_${System.currentTimeMillis()}_$i"),
                        userId = userId,
                        category = o.optString("category", "ALL"),
                        amount = o.optDouble("amount", 0.0),
                        month = o.optString("month", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                budgetDao.insertAll(list)
                restoredCount += list.size
            }
        }

        if (root.has("recurringBills")) {
            val arr = root.getJSONArray("recurringBills")
            val list = mutableListOf<RecurringBill>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    RecurringBill(
                        id = o.optString("id", "rec_${System.currentTimeMillis()}_$i"),
                        userId = userId,
                        name = o.optString("name", "週期項目"),
                        amount = o.optDouble("amount", 0.0),
                        type = o.optString("type", "EXPENSE"),
                        category = o.optString("category", "居家"),
                        accountId = o.optString("accountId", ""),
                        dueDay = o.optLong("dueDay", 1L),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
            if (list.isNotEmpty()) {
                recurringBillDao.insertAll(list)
                restoredCount += list.size
            }
        }

        if (root.has("travels")) {
            val arr = root.getJSONArray("travels")
            val list = mutableListOf<TravelLedger>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TravelLedger(
                        id = o.optString("id", "travel_${System.currentTimeMillis()}_$i"),
                        userId = userId,
                        name = o.optString("name", "旅遊手帳"),
                        destination = o.optString("destination", ""),
                        currency = o.optString("currency", "JPY"),
                        budget = o.optDouble("budget", 0.0),
                        startDate = o.optString("startDate", ""),
                        endDate = o.optString("endDate", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
            if (list.isNotEmpty()) {
                travelDao.insertAll(list)
                restoredCount += list.size
            }
        }

        return restoredCount
    }
}
