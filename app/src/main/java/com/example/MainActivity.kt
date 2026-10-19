package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddTransactionSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.RecurringScreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.RetroLedgerTheme
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.data.preferences.SettingsManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.LedgerViewModelFactory

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val isDarkMode by SettingsManager.isDarkModeEnabled(context).collectAsState(initial = false)
            val fontSizeOffset by SettingsManager.getFontSizeOffset(context).collectAsState(initial = 0)

            com.example.ui.theme.ThemedApp(
                darkTheme = isDarkMode,
                fontSizeOffset = fontSizeOffset
            ) {
                val isBiometricEnabled by remember { SettingsManager.isBiometricEnabled(context) }.collectAsState(initial = false)
                val password by remember { SettingsManager.getPassword(context) }.collectAsState(initial = null)
                var isUnlocked by remember { mutableStateOf(false) }

                if ((isBiometricEnabled || !password.isNullOrEmpty()) && !isUnlocked) {
                    com.example.ui.screens.LockScreen(
                        onUnlock = { isUnlocked = true },
                        activity = this,
                        password = password,
                        isBiometricEnabled = isBiometricEnabled
                    )
                } else {
                    MainAppScreen(userId = "local_user")
                }
            }
        }
    }
}

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppScreen(
    userId: String
) {
    val context = LocalContext.current
    val viewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModelFactory(context, userId)
    )

    val selectedTab by viewModel.selectedTab.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val navTabs = listOf(
        NavTabItem("手帳", Icons.Default.AutoStories, "nav_tab_journal"),
        NavTabItem("統計", Icons.Default.PieChart, "nav_tab_analytics"),
        NavTabItem("預算", Icons.Default.Tune, "nav_tab_budget"),
        NavTabItem("錢包", Icons.Default.AccountBalanceWallet, "nav_tab_accounts"),
        NavTabItem("週期", Icons.Default.DateRange, "nav_tab_recurring")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = WarmSepia,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
            ) {
                navTabs.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.setSelectedTab(index)
                            triggerHapticFeedback(context, 15)
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontFamily = PlayfairFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WarmSepia,
                            selectedTextColor = WarmSepia,
                            indicatorColor = WarmSepia.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        triggerHapticFeedback(context, 25)
                        showAddSheet = true
                    },
                    containerColor = SealingWaxRed,
                    contentColor = ParchmentSurface,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .border(1.5.dp, VintageGold, CircleShape)
                        .testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增記帳",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "ScreenTransition") { tabIndex ->
                when (tabIndex) {
                    0 -> LedgerScreen(viewModel = viewModel)
                    1 -> AnalyticsScreen(viewModel = viewModel)
                    2 -> BudgetScreen(viewModel = viewModel)
                    3 -> AccountsScreen(viewModel = viewModel)
                    4 -> RecurringScreen(
                        viewModel = viewModel
                    )
                }
            }

            val selectedTravel by viewModel.selectedTravel.collectAsState()

            if (showAddSheet) {
                AddTransactionSheet(
                    accounts = accounts,
                    initialCurrency = selectedTravel?.currency ?: "NT$",
                    initialTravelId = selectedTravel?.id ?: "",
                    onDismiss = { showAddSheet = false },
                    onSaveTransaction = { id, type, amount, accountId, toAccountId, category, date, note, merchant, tags, currency, originalAmount, travelId ->
                        viewModel.addTransaction(
                            id = id,
                            type = type,
                            amount = amount,
                            accountId = accountId,
                            toAccountId = toAccountId,
                            category = category,
                            date = date,
                            note = note,
                            merchant = merchant,
                            tags = tags,
                            currency = currency,
                            originalAmount = originalAmount,
                            travelId = travelId
                        )
                    }
                )
            }
        }
    }
}
