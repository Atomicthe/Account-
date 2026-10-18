package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountType
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerBudget
import com.example.data.model.LedgerTransaction
import com.example.data.model.RecurringBill
import com.example.data.model.StampType
import com.example.data.model.TransactionType
import com.example.data.repository.RetroLedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class LedgerViewModel(
    private val repository: RetroLedgerRepository,
    private val userId: String
) : ViewModel() {

    private val isGuest = userId == "guest_local_user"

    private val _selectedMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAccountFilter = MutableStateFlow<String?>(null)
    val selectedAccountFilter: StateFlow<String?> = _selectedAccountFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Guest in-memory state flows
    private val now = System.currentTimeMillis()
    private val dayMs = 86400000L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private val _guestAccounts = MutableStateFlow(
        listOf(
            LedgerAccount("acc_cash", userId, "復古牛皮錢夾", AccountType.CASH.name, 3355.0, 3500.0, "NT$", "account_balance_wallet", "#7A3E26"),
            LedgerAccount("acc_bank", userId, "懷舊活期存摺", AccountType.BANK.name, 58200.0, 58200.0, "NT$", "account_balance", "#2E5A44"),
            LedgerAccount("acc_card", userId, "經典黑卡信用證", AccountType.CREDIT_CARD.name, -5530.0, 0.0, "NT$", "credit_card", "#2C1D11")
        )
    )

    private val _guestTransactions = MutableStateFlow(
        listOf(
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
    )

    private val _guestBudgets = MutableStateFlow(
        listOf(
            LedgerBudget("budget_main", userId, "ALL", 25000.0, currentMonthStr),
            LedgerBudget("budget_food", userId, "餐飲美食", 6000.0, currentMonthStr)
        )
    )

    private val _guestRecurring = MutableStateFlow(
        listOf(
            RecurringBill("rec_rent", userId, "老洋房工作室租金", 12000.0, TransactionType.EXPENSE.name, "居家水電", "acc_bank", "MONTHLY", 5L, true)
        )
    )

    val accounts: StateFlow<List<LedgerAccount>> = if (isGuest) {
        _guestAccounts.asStateFlow()
    } else {
        repository.observeAccounts(userId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val rawTransactions: StateFlow<List<LedgerTransaction>> = if (isGuest) {
        _guestTransactions.asStateFlow()
    } else {
        repository.observeTransactions(userId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val budgets: StateFlow<List<LedgerBudget>> = if (isGuest) {
        _guestBudgets.asStateFlow()
    } else {
        repository.observeBudgets(userId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val recurringBills: StateFlow<List<RecurringBill>> = if (isGuest) {
        _guestRecurring.asStateFlow()
    } else {
        repository.observeRecurring(userId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Filtered transactions for the selected month and active filters
    val filteredTransactions: StateFlow<List<LedgerTransaction>> = combine(
        rawTransactions,
        selectedMonth,
        searchQuery,
        selectedAccountFilter,
        selectedCategoryFilter
    ) { txs, month, query, accFilter, catFilter ->
        txs.filter { tx ->
            val matchesMonth = tx.date.startsWith(month)
            val matchesQuery = query.isBlank() ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.merchant.contains(query, ignoreCase = true) ||
                    tx.tags.contains(query, ignoreCase = true)
            val matchesAccount = accFilter == null || tx.accountId == accFilter || tx.toAccountId == accFilter
            val matchesCategory = catFilter == null || tx.category == catFilter

            matchesMonth && matchesQuery && matchesAccount && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (!isGuest) {
            viewModelScope.launch {
                repository.seedInitialDataIfEmpty(userId)
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountFilter.value = accountId
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun nextMonth() {
        changeMonth(1)
    }

    fun prevMonth() {
        changeMonth(-1)
    }

    private fun changeMonth(delta: Int) {
        try {
            val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = format.parse(_selectedMonth.value) ?: Date()
            cal.add(Calendar.MONTH, delta)
            _selectedMonth.value = format.format(cal.time)
        } catch (_: Exception) {}
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String = "",
        category: String,
        date: String,
        note: String,
        merchant: String,
        tags: String,
        stamp: StampType = StampType.PAID
    ) {
        val txId = "tx_${UUID.randomUUID().toString().take(8)}"
        val tx = LedgerTransaction(
            id = txId,
            userId = userId,
            type = type.name,
            amount = amount,
            accountId = accountId,
            toAccountId = toAccountId,
            category = category,
            date = date,
            timestampMs = System.currentTimeMillis(),
            note = note,
            merchant = merchant,
            tags = tags,
            stamp = stamp.name
        )

        if (isGuest) {
            _guestTransactions.value = listOf(tx) + _guestTransactions.value
            // update guest account balances
            val accs = _guestAccounts.value.toMutableList()
            when (type) {
                TransactionType.EXPENSE -> {
                    val idx = accs.indexOfFirst { it.id == accountId }
                    if (idx >= 0) {
                        accs[idx] = accs[idx].copy(balance = accs[idx].balance - amount)
                    }
                }
                TransactionType.INCOME -> {
                    val idx = accs.indexOfFirst { it.id == accountId }
                    if (idx >= 0) {
                        accs[idx] = accs[idx].copy(balance = accs[idx].balance + amount)
                    }
                }
                TransactionType.TRANSFER -> {
                    val srcIdx = accs.indexOfFirst { it.id == accountId }
                    if (srcIdx >= 0) {
                        accs[srcIdx] = accs[srcIdx].copy(balance = accs[srcIdx].balance - amount)
                    }
                    if (toAccountId.isNotBlank()) {
                        val dstIdx = accs.indexOfFirst { it.id == toAccountId }
                        if (dstIdx >= 0) {
                            accs[dstIdx] = accs[dstIdx].copy(balance = accs[dstIdx].balance + amount)
                        }
                    }
                }
            }
            _guestAccounts.value = accs
        } else {
            viewModelScope.launch {
                repository.saveTransaction(tx)
            }
        }
    }

    fun deleteTransaction(tx: LedgerTransaction) {
        if (isGuest) {
            _guestTransactions.value = _guestTransactions.value.filter { it.id != tx.id }
            val accs = _guestAccounts.value.toMutableList()
            when (tx.type) {
                TransactionType.EXPENSE.name -> {
                    val idx = accs.indexOfFirst { it.id == tx.accountId }
                    if (idx >= 0) {
                        accs[idx] = accs[idx].copy(balance = accs[idx].balance + tx.amount)
                    }
                }
                TransactionType.INCOME.name -> {
                    val idx = accs.indexOfFirst { it.id == tx.accountId }
                    if (idx >= 0) {
                        accs[idx] = accs[idx].copy(balance = accs[idx].balance - tx.amount)
                    }
                }
                TransactionType.TRANSFER.name -> {
                    val srcIdx = accs.indexOfFirst { it.id == tx.accountId }
                    if (srcIdx >= 0) {
                        accs[srcIdx] = accs[srcIdx].copy(balance = accs[srcIdx].balance + tx.amount)
                    }
                    if (tx.toAccountId.isNotBlank()) {
                        val dstIdx = accs.indexOfFirst { it.id == tx.toAccountId }
                        if (dstIdx >= 0) {
                            accs[dstIdx] = accs[dstIdx].copy(balance = accs[dstIdx].balance - tx.amount)
                        }
                    }
                }
            }
            _guestAccounts.value = accs
        } else {
            viewModelScope.launch {
                repository.deleteTransaction(tx)
            }
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        currency: String = "NT$",
        colorHex: String = "#7A3E26"
    ) {
        val accId = "acc_${UUID.randomUUID().toString().take(8)}"
        val acc = LedgerAccount(
            id = accId,
            userId = userId,
            name = name,
            type = type.name,
            balance = initialBalance,
            initialBalance = initialBalance,
            currency = currency,
            icon = type.iconName,
            color = colorHex
        )

        if (isGuest) {
            _guestAccounts.value = _guestAccounts.value + acc
        } else {
            viewModelScope.launch {
                repository.saveAccount(acc)
            }
        }
    }

    fun saveBudget(category: String, amount: Double) {
        val budget = LedgerBudget(
            id = "budget_${category}_${System.currentTimeMillis()}",
            userId = userId,
            category = category,
            amount = amount,
            month = _selectedMonth.value
        )

        if (isGuest) {
            val list = _guestBudgets.value.filter { it.category != category }
            _guestBudgets.value = list + budget
        } else {
            viewModelScope.launch {
                repository.saveBudget(budget)
            }
        }
    }

    fun addRecurringBill(
        name: String,
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: String,
        dueDay: Long
    ) {
        val bill = RecurringBill(
            id = "rec_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            name = name,
            amount = amount,
            type = type.name,
            category = category,
            accountId = accountId,
            frequency = "MONTHLY",
            dueDay = dueDay,
            isActive = true
        )

        if (isGuest) {
            _guestRecurring.value = _guestRecurring.value + bill
        } else {
            viewModelScope.launch {
                repository.saveRecurring(bill)
            }
        }
    }

    fun toggleRecurring(billId: String, active: Boolean) {
        if (isGuest) {
            _guestRecurring.value = _guestRecurring.value.map {
                if (it.id == billId) it.copy(isActive = active) else it
            }
        } else {
            viewModelScope.launch {
                repository.toggleRecurringActive(billId, active)
            }
        }
    }

    fun executeRecurringBill(bill: RecurringBill) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        addTransaction(
            type = if (bill.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = bill.amount,
            accountId = bill.accountId,
            category = bill.category,
            date = todayStr,
            note = "週期自動記帳: ${bill.name}",
            merchant = bill.name,
            tags = "#固定收支,#週期",
            stamp = StampType.APPROVED
        )
    }

    fun generateCsvExport(): String {
        val sb = StringBuilder()
        sb.append("日期,類型,類別,金額,帳戶,商家/對象,備註,標籤,印章\n")
        filteredTransactions.value.forEach { tx ->
            val accName = accounts.value.find { it.id == tx.accountId }?.name ?: tx.accountId
            sb.append("${tx.date},${tx.type},${tx.category},${tx.amount},${accName},\"${tx.merchant}\",\"${tx.note}\",\"${tx.tags}\",${tx.stamp}\n")
        }
        return sb.toString()
    }
}

class LedgerViewModelFactory(
    private val context: Context,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            val repo = RetroLedgerRepository(context)
            @Suppress("UNCHECKED_CAST")
            return LedgerViewModel(repo, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
