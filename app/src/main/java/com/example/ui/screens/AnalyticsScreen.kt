package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.EspressoMuted
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.ui.viewmodel.LedgerViewModel
import java.util.Locale
import kotlin.math.max

enum class ReportTab(val title: String) {
    MONTHLY("月度收支概覽"),
    ANNUAL("年度消費趨勢"),
    BALANCE_SHEET("資產負債表")
}

@Composable
fun AnalyticsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMonth by viewModel.selectedMonth.collectAsState()
    val allTransactions by viewModel.rawTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var activeTab by remember { mutableStateOf(ReportTab.MONTHLY) }
    var selectedYear by remember { mutableStateOf(currentMonth.take(4)) }
    var monthlyChartType by remember { mutableStateOf(0) } // 0: 圓餅圖, 1: 長條圖
    var monthlyAnalysisType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) } // for annual line chart click

    val vintageColors = listOf(
        WarmSepia,
        VintageGold,
        ForestGreen,
        Color(0xFF5C4B77),
        Color(0xFF3D5A80),
        SealingWaxRed,
        Color(0xFFB85D19),
        Color(0xFF8B575C),
        Color(0xFF665C54)
    )

    // Export report function
    fun shareCurrentReport() {
        val sb = StringBuilder()
        when (activeTab) {
            ReportTab.MONTHLY -> {
                val expenseTxs = filteredTransactions.filter { it.type == TransactionType.EXPENSE.name }
                val incomeTxs = filteredTransactions.filter { it.type == TransactionType.INCOME.name }
                val totalExp = expenseTxs.sumOf { it.amount }
                val totalInc = incomeTxs.sumOf { it.amount }
                val net = totalInc - totalExp

                sb.append("【時光帳本 • 月度收支分析報表】\n")
                sb.append("報表月份：$currentMonth\n")
                sb.append("-----------------------------\n")
                sb.append("本月總收入：NT$ ${String.format(Locale.US, "%,.0f", totalInc)}\n")
                sb.append("本月總支出：NT$ ${String.format(Locale.US, "%,.0f", totalExp)}\n")
                sb.append("本月結餘：NT$ ${String.format(Locale.US, "%,.0f", net)}\n")
                if (totalInc > 0) {
                    val rate = (net / totalInc * 100).coerceAtLeast(0.0)
                    sb.append("本月儲蓄率：${String.format(Locale.US, "%.1f", rate)}%\n")
                }
                sb.append("-----------------------------\n")
                sb.append("主要支出科目排行：\n")
                val topCats = expenseTxs.groupBy { it.category }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                topCats.take(5).forEachIndexed { i, (cat, amt) ->
                    val pct = if (totalExp > 0) (amt / totalExp * 100) else 0.0
                    sb.append("${i + 1}. $cat: NT$ ${String.format(Locale.US, "%,.0f", amt)} (${String.format(Locale.US, "%.1f", pct)}%)\n")
                }
            }
            ReportTab.ANNUAL -> {
                val yearTxs = allTransactions.filter { it.date.startsWith(selectedYear) }
                val yearExp = yearTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                val yearInc = yearTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
                val yearNet = yearInc - yearExp

                sb.append("【時光帳本 • 年度消費趨勢報表】\n")
                sb.append("報表年度：$selectedYear 年\n")
                sb.append("-----------------------------\n")
                sb.append("全年度總收入：NT$ ${String.format(Locale.US, "%,.0f", yearInc)}\n")
                sb.append("全年度總支出：NT$ ${String.format(Locale.US, "%,.0f", yearExp)}\n")
                sb.append("全年度總結餘：NT$ ${String.format(Locale.US, "%,.0f", yearNet)}\n")
                sb.append("月均支出：NT$ ${String.format(Locale.US, "%,.0f", yearExp / 12)}\n")
                sb.append("-----------------------------\n")
                sb.append("各月份收支摘要：\n")
                for (m in 1..12) {
                    val mStr = String.format(Locale.US, "%s-%02d", selectedYear, m)
                    val mTxs = yearTxs.filter { it.date.startsWith(mStr) }
                    val inc = mTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
                    val exp = mTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                    if (inc > 0 || exp > 0) {
                        sb.append("${m}月 - 收入: NT$ ${String.format(Locale.US, "%,.0f", inc)} | 支出: NT$ ${String.format(Locale.US, "%,.0f", exp)}\n")
                    }
                }
            }
            ReportTab.BALANCE_SHEET -> {
                val assetAccounts = accounts.filter { it.type != AccountType.CREDIT_CARD.name }
                val liabilityAccounts = accounts.filter { it.type == AccountType.CREDIT_CARD.name }
                val totalAssets = assetAccounts.sumOf { max(0.0, it.balance) }
                val totalLiabilities = liabilityAccounts.sumOf { if (it.balance < 0) -it.balance else it.balance }
                val netWorth = totalAssets - totalLiabilities

                sb.append("【時光帳本 • 個人資產負債表】\n")
                sb.append("結算日期：$currentMonth\n")
                sb.append("-----------------------------\n")
                sb.append("淨資產淨值：NT$ ${String.format(Locale.US, "%,.0f", netWorth)}\n")
                sb.append("總資產 (Assets)：NT$ ${String.format(Locale.US, "%,.0f", totalAssets)}\n")
                sb.append("總負債 (Liabilities)：NT$ ${String.format(Locale.US, "%,.0f", totalLiabilities)}\n")
                val debtRatio = if (totalAssets > 0) (totalLiabilities / totalAssets * 100) else 0.0
                sb.append("資產負債比：${String.format(Locale.US, "%.1f", debtRatio)}%\n")
                sb.append("-----------------------------\n")
                sb.append("資產帳戶明細：\n")
                assetAccounts.forEach { acc ->
                    sb.append("• ${acc.name} (${acc.type}): NT$ ${String.format(Locale.US, "%,.0f", acc.balance)}\n")
                }
                sb.append("負債帳戶明細：\n")
                liabilityAccounts.forEach { acc ->
                    sb.append("• ${acc.name} (${acc.type}): NT$ ${String.format(Locale.US, "%,.0f", acc.balance)}\n")
                }
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "分享財務視覺化報表"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header & Export Action Button
        ParchmentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "財務報表與視覺化分析",
                            fontFamily = PlayfairFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FINANCIAL REPORTS & INSIGHTS",
                            fontFamily = CourierPrimeFont,
                            fontSize = 10.5.sp,
                            color = VintageGold
                        )
                    }

                    Button(
                        onClick = { shareCurrentReport() },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("匯出報表", fontSize = 12.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Three-tab segment control (Muji aesthetic)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReportTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    activeTab = tab
                                    triggerHapticFeedback(context, 10)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                fontFamily = PlayfairFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // TAB 1: MONTHLY OVERVIEW
        if (activeTab == ReportTab.MONTHLY) {
            val relevantTxs = filteredTransactions.filter { it.type == monthlyAnalysisType.name }
            val totalAmount = relevantTxs.sumOf { it.amount }
            val monthlyTotalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val monthlyTotalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val netSavings = monthlyTotalIncome - monthlyTotalExpense

            val categoryTotals = relevantTxs
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            // Filter row: Month navigator & Chart switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month navigator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.prevMonth(); triggerHapticFeedback(context, 10) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(16.dp))
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
                        onClick = { viewModel.nextMonth(); triggerHapticFeedback(context, 10) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(16.dp))
                    }
                }

                // Switcher: Donut vs Bar Chart
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { monthlyChartType = 0; triggerHapticFeedback(context, 10) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (monthlyChartType == 0) WarmSepia.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(Icons.Default.DonutLarge, contentDescription = "圓餅圖", tint = if (monthlyChartType == 0) WarmSepia else EspressoMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { monthlyChartType = 1; triggerHapticFeedback(context, 10) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (monthlyChartType == 1) WarmSepia.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "長條圖", tint = if (monthlyChartType == 1) WarmSepia else EspressoMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expense vs Income toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(TransactionType.EXPENSE to "支出概覽", TransactionType.INCOME to "收入概覽").forEach { (type, label) ->
                    val isSelected = monthlyAnalysisType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) (if (type == TransactionType.EXPENSE) SealingWaxRed else ForestGreen) else Color.Transparent)
                            .clickable {
                                monthlyAnalysisType = type
                                triggerHapticFeedback(context, 10)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = PlayfairFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) ParchmentSurface else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Monthly Balance Comparison Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("月度總收支對比", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "結餘 NT$ ${String.format(Locale.US, "%,.0f", netSavings)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (netSavings >= 0) ForestGreen else SealingWaxRed
                            )
                        }
                        if (monthlyTotalIncome > 0) {
                            val savingsRate = (netSavings / monthlyTotalIncome * 100).coerceAtLeast(0.0)
                            Text(
                                text = "儲蓄率 ${String.format(Locale.US, "%.1f", savingsRate)}%",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarmSepia
                            )
                        }
                    }

                    // Dual progress comparison bar
                    val totalTurnover = (monthlyTotalIncome + monthlyTotalExpense).coerceAtLeast(1.0)
                    val incomeRatio = (monthlyTotalIncome / totalTurnover).toFloat()
                    val expenseRatio = (monthlyTotalExpense / totalTurnover).toFloat()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(incomeRatio.coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .background(ForestGreen)
                        )
                        Box(
                            modifier = Modifier
                                .weight(expenseRatio.coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .background(SealingWaxRed)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "收入: +$${String.format(Locale.US, "%,.0f", monthlyTotalIncome)}",
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            color = ForestGreen
                        )
                        Text(
                            text = "支出: -$${String.format(Locale.US, "%,.0f", monthlyTotalExpense)}",
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            color = SealingWaxRed
                        )
                    }
                }
            }

            // Main Visual Chart Card: Donut or Bar
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (monthlyChartType == 0) "科目分佈圓餅圖 • DISTRIBUTION" else "科目金額長條圖 • COMPARISON",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )

                    if (totalAmount > 0 && categoryTotals.isNotEmpty()) {
                        if (monthlyChartType == 0) {
                            // Donut Chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(160.dp)) {
                                    var startAngle = -90f
                                    categoryTotals.forEachIndexed { idx, (_, amt) ->
                                        val sweep = (amt / totalAmount).toFloat() * 360f
                                        val color = vintageColors[idx % vintageColors.size]
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 32f, cap = StrokeCap.Butt),
                                            size = Size(size.width, size.height)
                                        )
                                        startAngle += sweep
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${monthlyAnalysisType.label}總計",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.0f", totalAmount)}",
                                        fontFamily = CourierPrimeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (monthlyAnalysisType == TransactionType.EXPENSE) SealingWaxRed else ForestGreen
                                    )
                                }
                            }
                        } else {
                            // Horizontal Bar Chart
                            val maxCategoryAmt = categoryTotals.maxOfOrNull { it.second } ?: 1.0
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                categoryTotals.take(6).forEachIndexed { idx, (cat, amt) ->
                                    val barRatio = (amt / maxCategoryAmt).toFloat()
                                    val color = vintageColors[idx % vintageColors.size]

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = "NT$ ${String.format(Locale.US, "%,.0f", amt)}",
                                                fontFamily = CourierPrimeFont,
                                                fontSize = 12.sp,
                                                color = color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(barRatio)
                                                    .height(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Category ranking breakdown
                        PerforatedDivider(modifier = Modifier.padding(vertical = 6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryTotals.forEachIndexed { index, (cat, amt) ->
                                val pct = (amt / totalAmount).toFloat()
                                val color = vintageColors[index % vintageColors.size]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        text = "NT$ ${String.format(Locale.US, "%,.0f", amt)} (${String.format(Locale.US, "%.1f", pct * 100)}%)",
                                        fontFamily = CourierPrimeFont,
                                        fontSize = 12.sp,
                                        color = WarmSepia,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("本月份無${monthlyAnalysisType.label}記錄", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // TAB 2: ANNUAL TREND LINE CHART
        if (activeTab == ReportTab.ANNUAL) {
            val yearTxs = allTransactions.filter { it.date.startsWith(selectedYear) }
            val yearExp = yearTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val yearInc = yearTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val yearNet = yearInc - yearExp

            // 12 months array calculation
            data class MonthData(val monthNum: Int, val monthLabel: String, val expense: Double, val income: Double)
            val monthlyData = (1..12).map { m ->
                val mStr = String.format(Locale.US, "%s-%02d", selectedYear, m)
                val mTxs = yearTxs.filter { it.date.startsWith(mStr) }
                val exp = mTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                val inc = mTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
                MonthData(m, "${m}月", exp, inc)
            }

            val maxVal = max(
                monthlyData.maxOfOrNull { max(it.expense, it.income) } ?: 1000.0,
                1000.0
            )

            // Year selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val yr = selectedYear.toIntOrNull() ?: 2026
                            selectedYear = (yr - 1).toString()
                            triggerHapticFeedback(context, 10)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$selectedYear 年",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            val yr = selectedYear.toIntOrNull() ?: 2026
                            selectedYear = (yr + 1).toString()
                            triggerHapticFeedback(context, 10)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(16.dp))
                    }
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SealingWaxRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("支出走勢", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ForestGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("收入走勢", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Annual Totals Hero Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$selectedYear 年度收支總計",
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("年度支出", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", yearExp)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SealingWaxRed
                            )
                        }
                        Column {
                            Text("年度收入", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", yearInc)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ForestGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("年度結餘", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", yearNet)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (yearNet >= 0) ForestGreen else SealingWaxRed
                            )
                        }
                    }
                }
            }

            // 12-Month Line Chart (折線圖) Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "12個月消費與收入走勢折線圖 • ANNUAL TREND",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )

                    // Canvas Line Chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val w = size.width
                        val h = size.height - 30f // bottom margin for labels
                        val stepX = w / 11f

                        // Draw background horizontal grid lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = h * (i / gridLines.toFloat())
                            drawLine(
                                color = Color(0xFFE8DFD1),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        // Expense Line Path
                        val expPath = Path()
                        val incPath = Path()

                        monthlyData.forEachIndexed { i, d ->
                            val x = i * stepX
                            val expY = h - (d.expense / maxVal * h).toFloat()
                            val incY = h - (d.income / maxVal * h).toFloat()

                            if (i == 0) {
                                expPath.moveTo(x, expY)
                                incPath.moveTo(x, incY)
                            } else {
                                expPath.lineTo(x, expY)
                                incPath.lineTo(x, incY)
                            }
                        }

                        // Draw Lines
                        drawPath(expPath, SealingWaxRed, style = Stroke(width = 5f, cap = StrokeCap.Round))
                        drawPath(incPath, ForestGreen, style = Stroke(width = 5f, cap = StrokeCap.Round))

                        // Draw dots at each month node
                        monthlyData.forEachIndexed { i, d ->
                            val x = i * stepX
                            val expY = h - (d.expense / maxVal * h).toFloat()
                            val incY = h - (d.income / maxVal * h).toFloat()

                            // Expense dot
                            drawCircle(SealingWaxRed, radius = 6f, center = Offset(x, expY))
                            drawCircle(Color.White, radius = 3f, center = Offset(x, expY))

                            // Income dot
                            drawCircle(ForestGreen, radius = 6f, center = Offset(x, incY))
                            drawCircle(Color.White, radius = 3f, center = Offset(x, incY))
                        }
                    }

                    // Month X-Axis Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        monthlyData.forEachIndexed { idx, d ->
                            Text(
                                text = "${d.monthNum}月",
                                fontSize = 9.sp,
                                fontFamily = CourierPrimeFont,
                                color = if (selectedMonthIndex == idx) WarmSepia else EspressoMuted,
                                fontWeight = if (selectedMonthIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable {
                                    selectedMonthIndex = idx
                                    triggerHapticFeedback(context, 10)
                                }
                            )
                        }
                    }

                    // Selected Month Detail Inspector
                    val inspectedMonth = selectedMonthIndex?.let { monthlyData.getOrNull(it) }
                    if (inspectedMonth != null) {
                        PerforatedDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "選定月份: ${selectedYear}年${inspectedMonth.monthNum}月",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "支出: -$${String.format(Locale.US, "%,.0f", inspectedMonth.expense)}  |  收入: +$${String.format(Locale.US, "%,.0f", inspectedMonth.income)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = WarmSepia
                            )
                        }
                    }
                }
            }

            // 12-Month Table Breakdown Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "各月份收支明細表 • MONTHLY TABLE",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )

                    monthlyData.forEach { d ->
                        val net = d.income - d.expense
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedYear}年${d.monthNum}月",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "-$${String.format(Locale.US, "%,.0f", d.expense)}",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 12.sp,
                                    color = if (d.expense > 0) SealingWaxRed else EspressoMuted
                                )
                                Text(
                                    text = "+$${String.format(Locale.US, "%,.0f", d.income)}",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 12.sp,
                                    color = if (d.income > 0) ForestGreen else EspressoMuted
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%,.0f", net)}",
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (net >= 0) ForestGreen else SealingWaxRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: BALANCE SHEET (資產負債表)
        if (activeTab == ReportTab.BALANCE_SHEET) {
            val assetAccounts = accounts.filter { it.type != AccountType.CREDIT_CARD.name }
            val liabilityAccounts = accounts.filter { it.type == AccountType.CREDIT_CARD.name }

            val totalAssets = assetAccounts.sumOf { max(0.0, it.balance) }
            val totalLiabilities = liabilityAccounts.sumOf { if (it.balance < 0) -it.balance else it.balance }
            val netWorth = totalAssets - totalLiabilities

            val debtRatio = if (totalAssets > 0) (totalLiabilities / totalAssets * 100).coerceAtMost(100.0) else 0.0
            val healthStatus = when {
                debtRatio < 20.0 -> "財務極度健全"
                debtRatio < 40.0 -> "資產配置良好"
                debtRatio < 70.0 -> "負債比例尚可"
                else -> "負債比偏高，建議控管支出"
            }

            // Net Worth Hero Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "個人淨資產總額 • NET WORTH",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )
                    Text(
                        text = "NT$ ${String.format(Locale.US, "%,.0f", netWorth)}",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = if (netWorth >= 0) WarmSepia else SealingWaxRed
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "負債比率: ${String.format(Locale.US, "%.1f", debtRatio)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (debtRatio > 50.0) SealingWaxRed else ForestGreen
                        )
                        Text(
                            text = healthStatus,
                            fontSize = 11.5.sp,
                            fontFamily = PlayfairFont,
                            color = VintageGold
                        )
                    }

                    // Assets vs Liabilities visual bar
                    val totalBar = (totalAssets + totalLiabilities).coerceAtLeast(1.0)
                    val assetRatio = (totalAssets / totalBar).toFloat()
                    val liabilityRatio = (totalLiabilities / totalBar).toFloat()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(assetRatio.coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .background(ForestGreen)
                        )
                        Box(
                            modifier = Modifier
                                .weight(liabilityRatio.coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .background(SealingWaxRed)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ForestGreen))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "總資產: NT$ ${String.format(Locale.US, "%,.0f", totalAssets)}",
                                fontSize = 11.5.sp,
                                fontFamily = CourierPrimeFont,
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SealingWaxRed))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "總負債: NT$ ${String.format(Locale.US, "%,.0f", totalLiabilities)}",
                                fontSize = 11.5.sp,
                                fontFamily = CourierPrimeFont,
                                color = SealingWaxRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Assets Breakdown List Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "資產細目 • ASSETS",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ForestGreen
                        )
                        Text(
                            text = "${assetAccounts.size} 個資產帳戶",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (assetAccounts.isEmpty()) {
                        Text(
                            text = "尚未建立資產帳戶，可至「錢包」分頁新增現金或銀行帳戶",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        assetAccounts.forEach { acc ->
                            val pct = if (totalAssets > 0) (acc.balance / totalAssets * 100) else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (acc.type) {
                                            AccountType.CASH.name -> Icons.Default.AccountBalanceWallet
                                            AccountType.SAVINGS.name -> Icons.Default.AccountBalance
                                            else -> Icons.Default.AccountBalance
                                        },
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = acc.type, fontSize = 10.sp, color = VintageGold)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "NT$ ${String.format(Locale.US, "%,.0f", acc.balance)}",
                                        fontFamily = CourierPrimeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", pct)}%",
                                        fontFamily = CourierPrimeFont,
                                        fontSize = 10.sp,
                                        color = EspressoMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liabilities Breakdown List Card
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "負債細目 • LIABILITIES",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SealingWaxRed
                        )
                        Text(
                            text = "${liabilityAccounts.size} 個負債/信用卡帳戶",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (liabilityAccounts.isEmpty()) {
                        Text(
                            text = "暫無負債或信用卡借款項目",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        liabilityAccounts.forEach { acc ->
                            val amt = if (acc.balance < 0) -acc.balance else acc.balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = SealingWaxRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "信用卡應付帳款", fontSize = 10.sp, color = VintageGold)
                                    }
                                }
                                Text(
                                    text = "NT$ ${String.format(Locale.US, "%,.0f", amt)}",
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SealingWaxRed
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}
