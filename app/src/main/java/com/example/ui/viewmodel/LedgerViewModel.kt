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

    private val _selectedTravelId = MutableStateFlow<String?>(null)
    val selectedTravelId: StateFlow<String?> = _selectedTravelId.asStateFlow()

    val travelLedgers: StateFlow<List<com.example.data.model.TravelLedger>> = repository.observeTravels(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTravel: StateFlow<com.example.data.model.TravelLedger?> = combine(travelLedgers, selectedTravelId) { travels, id ->
        travels.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        selectedCategoryFilter,
        selectedTravelId
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val txs = args[0] as List<LedgerTransaction>
        val month = args[1] as String
        val query = args[2] as String
        val accFilter = args[3] as String?
        val catFilter = args[4] as String?
        val travelId = args[5] as String?

        txs.filter { tx ->
            val matchesTravel = if (travelId != null) tx.travelId == travelId else tx.travelId.isBlank()
            val matchesMonth = (travelId != null) || tx.date.startsWith(month)
            val matchesQuery = query.isBlank() ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.merchant.contains(query, ignoreCase = true) ||
                    tx.tags.contains(query, ignoreCase = true)
            val matchesAccount = accFilter == null || tx.accountId == accFilter || tx.toAccountId == accFilter
            val matchesCategory = catFilter == null || tx.category == catFilter

            matchesTravel && matchesMonth && matchesQuery && matchesAccount && matchesCategory
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

    fun selectTravel(travelId: String?) {
        _selectedTravelId.value = travelId
    }

    fun addTravel(
        name: String,
        destination: String,
        currency: String = "JPY",
        budget: Double = 0.0,
        startDate: String = "",
        endDate: String = ""
    ) {
        viewModelScope.launch {
            val travel = com.example.data.model.TravelLedger(
                id = "travel_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                name = name,
                destination = destination,
                currency = currency,
                budget = budget,
                startDate = startDate,
                endDate = endDate,
                isActive = true
            )
            repository.saveTravel(travel)
            _selectedTravelId.value = travel.id
        }
    }

    fun deleteTravel(travel: com.example.data.model.TravelLedger) {
        viewModelScope.launch {
            repository.deleteTravel(travel)
            if (_selectedTravelId.value == travel.id) {
                _selectedTravelId.value = null
            }
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            com.example.data.currency.CurrencyManager.refreshLiveRates()
        }
    }

    fun addTransaction(
        id: String? = null,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String = "",
        category: String,
        date: String,
        note: String,
        merchant: String,
        tags: String,
        currency: String = "NT$",
        originalAmount: Double = 0.0,
        travelId: String = ""
    ) {
        viewModelScope.launch {
            val tx = LedgerTransaction(
                id = id ?: "tx_${UUID.randomUUID().toString().take(8)}",
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
                currency = currency,
                originalAmount = if (originalAmount > 0) originalAmount else amount,
                travelId = if (travelId.isNotBlank()) travelId else (_selectedTravelId.value ?: "")
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

    fun deleteBudget(budget: LedgerBudget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
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
            toAccountId = "",
            category = bill.category,
            date = todayStr,
            note = "週期自動記帳: ${bill.name}",
            merchant = bill.name,
            tags = "#固定收支,#週期"
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

    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson(userId)
    }

    suspend fun importBackupJson(jsonStr: String): Int {
        return repository.importBackupJson(userId, jsonStr)
    }

    suspend fun clearAllData() {
        repository.clearAllData(userId)
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
