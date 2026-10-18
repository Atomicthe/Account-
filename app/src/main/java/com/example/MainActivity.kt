package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddTransactionSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.RecurringScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.RetroLedgerTheme
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.LedgerViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RetroLedgerTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    var isGuest by remember { mutableStateOf(false) }

    if (currentUser == null && !isGuest) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            onGuestClick = {
                isGuest = true
            }
        )
    } else {
        MainAppScreen(
            userId = if (isGuest) "guest_local_user" else currentUser!!.uid,
            onSignOut = {
                isGuest = false
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onSignOutComplete = {
                        currentUser = null
                    }
                )
            }
        )
    }
}

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppScreen(
    userId: String,
    onSignOut: () -> Unit
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
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
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
                    4 -> RecurringScreen(viewModel = viewModel, onSignOutClicked = onSignOut)
                }
            }

            if (showAddSheet) {
                AddTransactionSheet(
                    accounts = accounts,
                    onDismiss = { showAddSheet = false },
                    onSaveTransaction = { type, amount, accountId, toAccountId, category, date, note, merchant, tags, stamp ->
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            accountId = accountId,
                            toAccountId = toAccountId,
                            category = category,
                            date = date,
                            note = note,
                            merchant = merchant,
                            tags = tags,
                            stamp = stamp
                        )
                    }
                )
            }
        }
    }
}
