package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccountType
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerTransaction
import com.example.data.model.StampType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RetroLedgerRepositoryRuleTest {

    private lateinit var context: Context
    private lateinit var repository: RetroLedgerRepository
    private val testUserId = "test_local_user"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = RetroLedgerRepository(context)
    }

    @Test
    fun saveAccountAndObserve() = runBlocking {
        val account = LedgerAccount(
            id = "acc_test_local_1",
            userId = testUserId,
            name = "生活日常錢包",
            type = AccountType.CASH.name,
            balance = 3500.0,
            currency = "NT$"
        )

        repository.saveAccount(account)

        val accounts = repository.observeAccounts(testUserId).first()
        val found = accounts.find { it.id == "acc_test_local_1" }
        assertNotNull(found)
        assertEquals("生活日常錢包", found?.name)
        assertEquals(3500.0, found?.balance ?: 0.0, 0.001)
    }

    @Test
    fun saveTransactionAndExportBackup() = runBlocking {
        val tx = LedgerTransaction(
            id = "tx_test_local_1",
            userId = testUserId,
            type = TransactionType.EXPENSE.name,
            amount = 180.0,
            accountId = "acc_test_local_1",
            category = "餐飲",
            date = "2026-10-19",
            timestampMs = System.currentTimeMillis(),
            note = "美味拉麵",
            merchant = "日式拉麵店",
            stamp = StampType.PAID.name
        )

        repository.saveTransaction(tx)

        val txs = repository.observeTransactions(testUserId).first()
        val found = txs.find { it.id == "tx_test_local_1" }
        assertNotNull(found)
        assertEquals(180.0, found?.amount ?: 0.0, 0.001)

        // Test export backup JSON
        val backupJson = repository.exportBackupJson(testUserId)
        assertTrue(backupJson.contains("tx_test_local_1"))
        assertTrue(backupJson.contains("local_only"))

        // Test import backup
        val restoredCount = repository.importBackupJson(testUserId, backupJson)
        assertTrue(restoredCount > 0)
    }
}
