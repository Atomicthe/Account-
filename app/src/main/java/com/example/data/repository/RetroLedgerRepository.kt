package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.BillFrequency
import com.example.data.model.AccountType
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import com.example.data.model.StampType
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.google.firebase.auth.FirebaseAuth

class RetroLedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- ACCOUNTS ---

    fun observeAccounts(userId: String): Flow<List<LedgerAccount>> = flow {
        val path = "users/$userId/accounts"
        emitAll(
            db.collection("users").document(userId).collection("accounts")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(LedgerAccount::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAccount(account: LedgerAccount): Result<String> {
        val uid = requireUserId()
        val accountId = if (account.id.isNotBlank()) account.id else "acc_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$uid/accounts/$accountId"
        val docRef = db.collection("users").document(uid).collection("accounts").document(accountId)
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to accountId,
                "userId" to uid,
                "name" to account.name,
                "type" to account.type,
                "balance" to account.balance,
                "initialBalance" to account.initialBalance,
                "currency" to account.currency,
                "icon" to account.icon,
                "color" to account.color,
                "createdAt" to (account.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(accountId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/accounts/$accountId"
        val docRef = db.collection("users").document(uid).collection("accounts").document(accountId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- TRANSACTIONS ---

    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LedgerTransaction::class.java).sortedByDescending { it.timestampMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveTransaction(tx: LedgerTransaction): Result<String> {
        val uid = requireUserId()
        val txId = if (tx.id.isNotBlank()) tx.id else "tx_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$uid/transactions/$txId"
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to txId,
                "userId" to uid,
                "type" to tx.type,
                "amount" to tx.amount,
                "accountId" to tx.accountId,
                "toAccountId" to tx.toAccountId,
                "category" to tx.category,
                "subCategory" to tx.subCategory,
                "date" to tx.date,
                "timestampMs" to tx.timestampMs,
                "note" to tx.note,
                "merchant" to tx.merchant,
                "tags" to tx.tags,
                "stamp" to tx.stamp,
                "createdAt" to (tx.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Update account balances accordingly
            updateAccountBalancesForTx(tx)

            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateAccountBalancesForTx(tx: LedgerTransaction) {
        val uid = requireUserId()
        val accountsCol = db.collection("users").document(uid).collection("accounts")

        when (tx.type) {
            TransactionType.EXPENSE.name -> {
                val accRef = accountsCol.document(tx.accountId)
                val snapshot = accRef.get().await()
                if (snapshot.exists()) {
                    val currentBal = (snapshot.get("balance") as? Number)?.toDouble() ?: 0.0
                    accRef.update("balance", currentBal - tx.amount, "updatedAt", FieldValue.serverTimestamp()).await()
                }
            }
            TransactionType.INCOME.name -> {
                val accRef = accountsCol.document(tx.accountId)
                val snapshot = accRef.get().await()
                if (snapshot.exists()) {
                    val currentBal = (snapshot.get("balance") as? Number)?.toDouble() ?: 0.0
                    accRef.update("balance", currentBal + tx.amount, "updatedAt", FieldValue.serverTimestamp()).await()
                }
            }
            TransactionType.TRANSFER.name -> {
                // Source minus
                val srcRef = accountsCol.document(tx.accountId)
                val srcSnap = srcRef.get().await()
                if (srcSnap.exists()) {
                    val srcBal = (srcSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                    srcRef.update("balance", srcBal - tx.amount, "updatedAt", FieldValue.serverTimestamp()).await()
                }
                // Target plus
                if (tx.toAccountId.isNotBlank()) {
                    val dstRef = accountsCol.document(tx.toAccountId)
                    val dstSnap = dstRef.get().await()
                    if (dstSnap.exists()) {
                        val dstBal = (dstSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                        dstRef.update("balance", dstBal + tx.amount, "updatedAt", FieldValue.serverTimestamp()).await()
                    }
                }
            }
        }
    }

    suspend fun deleteTransaction(tx: LedgerTransaction): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/transactions/${tx.id}"
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        return try {
            docRef.delete().await()
            // Reverse account balance effect
            val accountsCol = db.collection("users").document(uid).collection("accounts")
            when (tx.type) {
                TransactionType.EXPENSE.name -> {
                    val accRef = accountsCol.document(tx.accountId)
                    val snap = accRef.get().await()
                    if (snap.exists()) {
                        val bal = (snap.get("balance") as? Number)?.toDouble() ?: 0.0
                        accRef.update("balance", bal + tx.amount).await()
                    }
                }
                TransactionType.INCOME.name -> {
                    val accRef = accountsCol.document(tx.accountId)
                    val snap = accRef.get().await()
                    if (snap.exists()) {
                        val bal = (snap.get("balance") as? Number)?.toDouble() ?: 0.0
                        accRef.update("balance", bal - tx.amount).await()
                    }
                }
                TransactionType.TRANSFER.name -> {
                    val srcRef = accountsCol.document(tx.accountId)
                    val srcSnap = srcRef.get().await()
                    if (srcSnap.exists()) {
                        val bal = (srcSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                        srcRef.update("balance", bal + tx.amount).await()
                    }
                    if (tx.toAccountId.isNotBlank()) {
                        val dstRef = accountsCol.document(tx.toAccountId)
                        val dstSnap = dstRef.get().await()
                        if (dstSnap.exists()) {
                            val bal = (dstSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                            dstRef.update("balance", bal - tx.amount).await()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- BUDGETS ---

    fun observeBudgets(userId: String): Flow<List<LedgerBudget>> = flow {
        val path = "users/$userId/budgets"
        emitAll(
            db.collection("users").document(userId).collection("budgets")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(LedgerBudget::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveBudget(budget: LedgerBudget): Result<String> {
        val uid = requireUserId()
        val budgetId = if (budget.id.isNotBlank()) budget.id else "b_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$uid/budgets/$budgetId"
        val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to budgetId,
                "userId" to uid,
                "category" to budget.category,
                "amount" to budget.amount,
                "month" to budget.month,
                "createdAt" to (budget.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(budgetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- RECURRING BILLS ---

    fun observeRecurring(userId: String): Flow<List<RecurringBill>> = flow {
        val path = "users/$userId/recurring"
        emitAll(
            db.collection("users").document(userId).collection("recurring")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(RecurringBill::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveRecurring(bill: RecurringBill): Result<String> {
        val uid = requireUserId()
        val billId = if (bill.id.isNotBlank()) bill.id else "rec_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$uid/recurring/$billId"
        val docRef = db.collection("users").document(uid).collection("recurring").document(billId)
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to billId,
                "userId" to uid,
                "name" to bill.name,
                "amount" to bill.amount,
                "type" to bill.type,
                "category" to bill.category,
                "accountId" to bill.accountId,
                "frequency" to bill.frequency,
                "dueDay" to bill.dueDay,
                "isActive" to bill.isActive,
                "createdAt" to (bill.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(billId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleRecurringActive(billId: String, isActive: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("recurring").document(billId)
        return try {
            docRef.update("isActive", isActive, "updatedAt", FieldValue.serverTimestamp()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SEEDING INITIAL STARTER RETRO LEDGER DATA ---

    suspend fun seedInitialDataIfEmpty(userId: String) {
        val accountsCol = db.collection("users").document(userId).collection("accounts")
        val existing = accountsCol.limit(1).get().await()
        if (!existing.isEmpty) return

        // 1. Create Default Wallets
        val cashAcc = LedgerAccount(
            id = "acc_cash",
            userId = userId,
            name = "復古牛皮錢夾",
            type = AccountType.CASH.name,
            balance = 3500.0,
            initialBalance = 3500.0,
            currency = "NT$",
            color = "#7A3E26"
        )
        val bankAcc = LedgerAccount(
            id = "acc_bank",
            userId = userId,
            name = "懷舊活期存摺",
            type = AccountType.BANK.name,
            balance = 58200.0,
            initialBalance = 58200.0,
            currency = "NT$",
            color = "#2E5A44"
        )
        val cardAcc = LedgerAccount(
            id = "acc_card",
            userId = userId,
            name = "經典黑卡信用證",
            type = AccountType.CREDIT_CARD.name,
            balance = -4280.0,
            initialBalance = 0.0,
            currency = "NT$",
            color = "#2C1D11"
        )

        saveAccount(cashAcc)
        saveAccount(bankAcc)
        saveAccount(cardAcc)

        // 2. Default Overall Budget
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val defaultBudget = LedgerBudget(
            id = "budget_main",
            userId = userId,
            category = "ALL",
            amount = 25000.0,
            month = currentMonth
        )
        saveBudget(defaultBudget)

        // 3. Sample Transactions
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val sampleTxs = listOf(
            LedgerTransaction(
                id = "tx_seed_1",
                userId = userId,
                type = TransactionType.EXPENSE.name,
                amount = 180.0,
                accountId = "acc_cash",
                category = "餐飲美食",
                date = dateFormat.format(Date(now)),
                timestampMs = now - 3600000L,
                note = "炭焙曼特寧手沖咖啡與肉桂捲",
                merchant = "時光老街咖啡館",
                tags = "#手沖,#下午茶",
                stamp = StampType.PAID.name
            ),
            LedgerTransaction(
                id = "tx_seed_2",
                userId = userId,
                type = TransactionType.EXPENSE.name,
                amount = 65.0,
                accountId = "acc_cash",
                category = "交通通勤",
                date = dateFormat.format(Date(now)),
                timestampMs = now - 14400000L,
                note = "經典復古路面電車票",
                merchant = "城郊輕軌",
                tags = "#通勤",
                stamp = StampType.RECEIPT.name
            ),
            LedgerTransaction(
                id = "tx_seed_3",
                userId = userId,
                type = TransactionType.EXPENSE.name,
                amount = 1250.0,
                accountId = "acc_card",
                category = "日常購物",
                date = dateFormat.format(Date(now - dayMs)),
                timestampMs = now - dayMs,
                note = "手工精裝羊皮手帳本與金屬鋼筆墨水",
                merchant = "百年文具行",
                tags = "#文具,#收藏",
                stamp = StampType.VIP.name
            ),
            LedgerTransaction(
                id = "tx_seed_4",
                userId = userId,
                type = TransactionType.INCOME.name,
                amount = 45000.0,
                accountId = "acc_bank",
                category = "正職薪資",
                date = dateFormat.format(Date(now - dayMs * 3)),
                timestampMs = now - dayMs * 3,
                note = "十月份月俸結算入帳",
                merchant = "經典總會",
                tags = "#薪資,#入帳",
                stamp = StampType.APPROVED.name
            )
        )

        for (tx in sampleTxs) {
            val txDoc = db.collection("users").document(userId).collection("transactions").document(tx.id)
            val payload = hashMapOf<String, Any>(
                "id" to tx.id,
                "userId" to userId,
                "type" to tx.type,
                "amount" to tx.amount,
                "accountId" to tx.accountId,
                "toAccountId" to tx.toAccountId,
                "category" to tx.category,
                "subCategory" to tx.subCategory,
                "date" to tx.date,
                "timestampMs" to tx.timestampMs,
                "note" to tx.note,
                "merchant" to tx.merchant,
                "tags" to tx.tags,
                "stamp" to tx.stamp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            txDoc.set(payload).await()
        }

        // 4. Sample Recurring Bill
        val sampleBill = RecurringBill(
            id = "rec_rent",
            userId = userId,
            name = "老洋房工作室租金",
            amount = 12000.0,
            type = TransactionType.EXPENSE.name,
            category = "居家水電",
            accountId = "acc_bank",
            frequency = BillFrequency.MONTHLY.name,
            dueDay = 5L,
            isActive = true
        )
        saveRecurring(sampleBill)
    }
}
