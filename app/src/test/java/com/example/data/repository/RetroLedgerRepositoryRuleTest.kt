package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountType
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerTransaction
import com.example.data.model.StampType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroLedgerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAccount_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("tester_alice@example.com")
        val repository = RetroLedgerRepository(firestore, auth)

        val account = LedgerAccount(
            id = "acc_test_1",
            userId = uid,
            name = "復古羊皮錢夾",
            type = AccountType.CASH.name,
            balance = 1200.0,
            currency = "NT$"
        )

        val result = withTimeout(10000L) {
            repository.saveAccount(account)
        }
        if (result.isFailure) {
            throw result.exceptionOrNull()!!
        }

        val accounts = withTimeout(10000L) {
            repository.observeAccounts(uid).first { list -> list.any { it.id == "acc_test_1" } }
        }
        val found = accounts.find { it.id == "acc_test_1" }
        assertNotNull(found)
        assertEquals("復古羊皮錢夾", found?.name)
    }

    @Test
    fun saveTransaction_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("tester_bob@example.com")
        val repository = RetroLedgerRepository(firestore, auth)

        val tx = LedgerTransaction(
            id = "tx_test_1",
            userId = uid,
            type = TransactionType.EXPENSE.name,
            amount = 250.0,
            accountId = "acc_test_1",
            category = "餐飲美食",
            date = "2026-10-17",
            timestampMs = System.currentTimeMillis(),
            note = "咖啡與糕點",
            merchant = "老書房咖啡",
            stamp = StampType.PAID.name
        )

        val result = withTimeout(10000L) {
            repository.saveTransaction(tx)
        }
        assertTrue(result.isSuccess)
    }
}
