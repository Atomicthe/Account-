package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val currency: String = "NT$",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AccountType(val label: String, val iconName: String) {
    CASH("現金錢包", "account_balance_wallet"),
    BANK("銀行活期", "account_balance"),
    CREDIT_CARD("經典信用卡", "credit_card"),
    E_WALLET("電子支付/悠遊卡", "payments"),
    SAVINGS("懷舊存摺", "savings")
}

@IgnoreExtraProperties
data class LedgerAccount(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = AccountType.CASH.name,
    val balance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val currency: String = "NT$",
    val icon: String = "account_balance_wallet",
    val color: String = "#7A3E26",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TransactionType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("轉帳")
}

enum class StampType(val label: String, val colorHex: String) {
    NONE("無印章", "#8C857B"),
    PAID("PAID", "#9E2A2B"),
    APPROVED("APPROVED", "#2E5A44"),
    RECEIPT("RECEIPT", "#7A3E26"),
    VIP("SPECIAL", "#C8963E")
}

@IgnoreExtraProperties
data class LedgerTransaction(
    val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val accountId: String = "",
    val toAccountId: String = "",
    val category: String = "餐飲",
    val subCategory: String = "",
    val date: String = "", // e.g. "2026-10-17"
    val timestampMs: Long = 0L,
    val note: String = "",
    val merchant: String = "",
    val tags: String = "", // comma-separated e.g. "#日常,#咖啡"
    val stamp: String = StampType.PAID.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LedgerBudget(
    val id: String = "",
    val userId: String = "",
    val category: String = "ALL", // "ALL" or specific category
    val amount: Double = 0.0,
    val month: String = "", // "YYYY-MM"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BillFrequency(val label: String) {
    MONTHLY("每月"),
    WEEKLY("每週"),
    YEARLY("每年")
}

@IgnoreExtraProperties
data class RecurringBill(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "居家",
    val accountId: String = "",
    val frequency: String = BillFrequency.MONTHLY.name,
    val dueDay: Long = 1L, // 1..31
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CategoryDef(
    val name: String,
    val iconName: String,
    val type: TransactionType,
    val colorHex: String
)

object DefaultCategories {
    val expenseCategories = listOf(
        CategoryDef("餐飲美食", "restaurant", TransactionType.EXPENSE, "#8C482A"),
        CategoryDef("日常購物", "shopping_bag", TransactionType.EXPENSE, "#B85D19"),
        CategoryDef("交通通勤", "directions_subway", TransactionType.EXPENSE, "#2E5A44"),
        CategoryDef("休閒娛樂", "movie", TransactionType.EXPENSE, "#5C4B77"),
        CategoryDef("居家水電", "home", TransactionType.EXPENSE, "#3D5A80"),
        CategoryDef("醫療保健", "medical_services", TransactionType.EXPENSE, "#9E2A2B"),
        CategoryDef("學習進修", "menu_book", TransactionType.EXPENSE, "#C8963E"),
        CategoryDef("社交人情", "card_giftcard", TransactionType.EXPENSE, "#8B575C"),
        CategoryDef("其它支出", "more_horiz", TransactionType.EXPENSE, "#665C54")
    )

    val incomeCategories = listOf(
        CategoryDef("正職薪資", "work", TransactionType.INCOME, "#2E5A44"),
        CategoryDef("獎金紅利", "savings", TransactionType.INCOME, "#C8963E"),
        CategoryDef("副業兼職", "laptop_mac", TransactionType.INCOME, "#3D5A80"),
        CategoryDef("投資收益", "trending_up", TransactionType.INCOME, "#8C482A"),
        CategoryDef("中獎發票", "military_tech", TransactionType.INCOME, "#B85D19"),
        CategoryDef("其它收入", "attach_money", TransactionType.INCOME, "#665C54")
    )
}
