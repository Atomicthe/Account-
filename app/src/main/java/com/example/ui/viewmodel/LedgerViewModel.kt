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

    val accounts: StateFlow<List<LedgerAccount>> = repository.observeAccounts(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawTransactions: StateFlow<List<LedgerTransaction>> = repository.observeTransactions(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<LedgerBudget>> = repository.observeBudgets(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringBills: StateFlow<List<RecurringBill>> = repository.observeRecurring(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        viewModelScope.launch {
            val tx = LedgerTransaction(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
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
            repository.saveTransaction(tx)
        }
    }

    fun deleteTransaction(tx: LedgerTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        currency: String = "NT$",
        colorHex: String = "#7A3E26"
    ) {
        viewModelScope.launch {
            val acc = LedgerAccount(
                id = "acc_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                name = name,
                type = type.name,
                balance = initialBalance,
                initialBalance = initialBalance,
                currency = currency,
                icon = type.iconName,
                color = colorHex
            )
            repository.saveAccount(acc)
        }
    }

    fun saveBudget(category: String, amount: Double) {
        viewModelScope.launch {
            val budget = LedgerBudget(
                id = "budget_${category}_${System.currentTimeMillis()}",
                userId = userId,
                category = category,
                amount = amount,
                month = _selectedMonth.value
            )
            repository.saveBudget(budget)
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
        viewModelScope.launch {
            val bill = RecurringBill(
                id = "rec_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                name = name,
                amount = amount,
                type = type.name,
                category = category,
                accountId = accountId,
                dueDay = dueDay,
                isActive = true
            )
            repository.saveRecurring(bill)
        }
    }

    fun toggleRecurring(billId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringActive(billId, active)
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
            sb.append("${tx.date},${tx.type},${tx.category},${tx.amount},${tx.accountId},\"${tx.merchant}\",\"${tx.note}\",\"${tx.tags}\",${tx.stamp}\n")
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
