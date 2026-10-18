package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.WaxSealBadge
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.EspressoMuted
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.ui.theme.WarmSepiaLight
import com.example.ui.viewmodel.LedgerViewModel
import java.util.Locale

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedAccountFilter by viewModel.selectedAccountFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()

    var showFilterBar by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<LedgerTransaction?>(null) }

    // Summary numbers for current month
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Group transactions by date
    val groupedByDate = transactions.groupBy { it.date }.toSortedMap(compareByDescending { it })

    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = {
                Text(
                    text = "刪除此筆手帳記錄？",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("刪除後相應帳戶餘額將自動校正回歸。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete?.let { viewModel.deleteTransaction(it) }
                        txToDelete = null
                        triggerHapticFeedback(context, 30)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("確認撕毀刪除", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Retro Banner / Month Control
        ParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Banner header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmSepia.copy(alpha = 0.15f))
                                .border(1.dp, WarmSepia, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "卷",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                color = WarmSepia,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "時光記帳手冊",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "RECORD NO. ${transactions.size} ENTRIES",
                                fontFamily = CourierPrimeFont,
                                fontSize = 10.sp,
                                color = VintageGold
                            )
                        }
                    }

                    // Month navigator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.prevMonth()
                                triggerHapticFeedback(context, 10)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Prev month",
                                tint = WarmSepia,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = currentMonth,
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = {
                                viewModel.nextMonth()
                                triggerHapticFeedback(context, 10)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next month",
                                tint = WarmSepia,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                PerforatedDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Monthly Statement Overview (Income, Expense, Balance)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "本月收入",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+$${String.format(Locale.US, "%,.0f", totalIncome)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ForestGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "本月支出",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-$${String.format(Locale.US, "%,.0f", totalExpense)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SealingWaxRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "收支結餘",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (netBalance >= 0) "+" else ""}$${String.format(Locale.US, "%,.0f", netBalance)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (netBalance >= 0) ForestGreen else SealingWaxRed
                        )
                    }
                }
            }
        }

        // Search & Filter Bar Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showFilterBar = !showFilterBar },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Toggle filter",
                        tint = if (showFilterBar) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (searchQuery.isNotBlank() || selectedAccountFilter != null) "已啟用篩選" else "快速篩選與搜尋",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Export to CSV Share Sheet
            IconButton(
                onClick = {
                    val csv = viewModel.generateCsvExport()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csv)
                        type = "text/csv"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "匯出手帳記錄 CSV")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export CSV",
                    tint = WarmSepia,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showFilterBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("搜尋備註、類別、商家或標籤...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmSepia,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                // Accounts filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedAccountFilter == null,
                        onClick = { viewModel.setAccountFilter(null) },
                        label = { Text("全部錢包", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmSepiaLight,
                            selectedLabelColor = WarmSepia
                        )
                    )
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountFilter == acc.id,
                            onClick = { viewModel.setAccountFilter(acc.id) },
                            label = { Text(acc.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarmSepiaLight,
                                selectedLabelColor = WarmSepia
                            )
                        )
                    }
                }
            }
        }

        // Transactions List with Daily Groups
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_wax_seal_stamp),
                        contentDescription = "Empty",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "本月份尚無手帳記錄",
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "點選右下方「＋」印章按鈕開始快速記帳",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (date, txs) ->
                    val dayExpense = txs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                    val dayIncome = txs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

                    item(key = "header_$date") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date,
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = WarmSepia
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayIncome > 0) {
                                    Text(
                                        text = "+$${String.format(Locale.US, "%,.0f", dayIncome)}",
                                        fontFamily = CourierPrimeFont,
                                        fontSize = 11.5.sp,
                                        color = ForestGreen
                                    )
                                }
                                if (dayExpense > 0) {
                                    Text(
                                        text = "-$${String.format(Locale.US, "%,.0f", dayExpense)}",
                                        fontFamily = CourierPrimeFont,
                                        fontSize = 11.5.sp,
                                        color = SealingWaxRed
                                    )
                                }
                            }
                        }
                    }

                    items(txs, key = { it.id }) { tx ->
                        val accName = accounts.find { it.id == tx.accountId }?.name ?: "帳戶"
                        val toAccName = accounts.find { it.id == tx.toAccountId }?.name ?: ""

                        ParchmentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback(context, 15)
                                },
                            backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Category circular icon
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(WarmSepia.copy(alpha = 0.1f))
                                            .border(1.dp, WarmSepia.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = resolveCategoryIcon(
                                                when (tx.category) {
                                                    "餐飲美食" -> "restaurant"
                                                    "日常購物" -> "shopping_bag"
                                                    "交通通勤" -> "directions_subway"
                                                    "休閒娛樂" -> "movie"
                                                    "居家水電" -> "home"
                                                    "醫療保健" -> "medical_services"
                                                    "學習進修" -> "menu_book"
                                                    "社交人情" -> "card_giftcard"
                                                    "正職薪資" -> "work"
                                                    "獎金紅利" -> "savings"
                                                    "副業兼職" -> "laptop_mac"
                                                    "投資收益" -> "trending_up"
                                                    "中獎發票" -> "military_tech"
                                                    else -> "more_horiz"
                                                }
                                            ),
                                            contentDescription = tx.category,
                                            tint = WarmSepia,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (tx.type == TransactionType.TRANSFER.name) "帳戶互轉" else tx.category,
                                                fontFamily = PlayfairFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (tx.merchant.isNotBlank()) {
                                                Text(
                                                    text = " • ${tx.merchant}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = if (tx.type == TransactionType.TRANSFER.name) "$accName ➔ $toAccName" else accName,
                                                fontFamily = CourierPrimeFont,
                                                fontSize = 11.sp,
                                                color = VintageGold
                                            )
                                            if (tx.note.isNotBlank()) {
                                                Text(
                                                    text = " | ${tx.note}",
                                                    fontSize = 11.sp,
                                                    color = EspressoMuted
                                                )
                                            }
                                        }

                                        if (tx.tags.isNotBlank()) {
                                            Text(
                                                text = tx.tags.split(",").joinToString(" "),
                                                fontSize = 10.sp,
                                                color = WarmSepia,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Amount display
                                    val prefix = when (tx.type) {
                                        TransactionType.EXPENSE.name -> "-"
                                        TransactionType.INCOME.name -> "+"
                                        else -> ""
                                    }
                                    val amountColor = when (tx.type) {
                                        TransactionType.EXPENSE.name -> SealingWaxRed
                                        TransactionType.INCOME.name -> ForestGreen
                                        else -> WarmSepia
                                    }

                                    Text(
                                        text = "$prefix$${String.format(Locale.US, "%,.0f", tx.amount)}",
                                        fontFamily = CourierPrimeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        color = amountColor
                                    )

                                    if (tx.stamp != "NONE") {
                                        WaxSealBadge(stamp = tx.stamp, size = 28.dp)
                                    }

                                    IconButton(
                                        onClick = { txToDelete = tx },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = EspressoMuted.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }
}
