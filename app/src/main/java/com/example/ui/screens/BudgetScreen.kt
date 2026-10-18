package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.CourierPrimeFont
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
fun BudgetScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var budgetAmountInput by remember { mutableStateOf("") }

    val currentMonthBudgets = budgets.filter { it.month == currentMonth }
    val overallBudget = currentMonthBudgets.find { it.category == "ALL" }?.amount ?: 25000.0

    // Total expense this month
    val totalExpense = transactions
        .filter { it.type == TransactionType.EXPENSE.name }
        .sumOf { it.amount }

    val remainingOverall = overallBudget - totalExpense
    val overallProgress = if (overallBudget > 0) (totalExpense / overallBudget).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverBudget = totalExpense > overallBudget

    // Daily recommended allowable
    val daysRemaining = 15 // midpoint representation
    val dailyAllowable = if (remainingOverall > 0) remainingOverall / daysRemaining else 0.0

    if (showAddBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = {
                Text(
                    text = "設定科目預算上限",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("請選擇預算科目與金額：", fontSize = 12.sp)

                    // Category dropdown/chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "總預算", "餐飲美食" to "餐飲", "日常購物" to "購物", "休閒娛樂" to "娛樂").forEach { (code, lbl) ->
                            val isSel = selectedCategory == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepiaLight else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSel) WarmSepia else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { selectedCategory = code }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(lbl, fontSize = 12.sp, color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmountInput,
                        onValueChange = { budgetAmountInput = it },
                        label = { Text("預算金額 (NT$)", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.saveBudget(selectedCategory, amt)
                            showAddBudgetDialog = false
                            budgetAmountInput = ""
                            triggerHapticFeedback(context, 30)
                        } else {
                            Toast.makeText(context, "請輸入有效金額", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("儲存預算", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall Budget Gauge Card
        item {
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentMonth 月度總預算",
                            fontFamily = PlayfairFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                selectedCategory = "ALL"
                                budgetAmountInput = overallBudget.toInt().toString()
                                showAddBudgetDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("修改總額", fontSize = 11.sp, color = WarmSepia, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (isOverBudget) "已超支！" else "剩餘可用",
                                fontSize = 12.sp,
                                color = if (isOverBudget) SealingWaxRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", remainingOverall)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = if (isOverBudget) SealingWaxRed else ForestGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "預算限額",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", overallBudget)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WarmSepia
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isOverBudget) SealingWaxRed else (if (overallProgress > 0.8f) VintageGold else ForestGreen),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "已支出: NT$ ${String.format(Locale.US, "%,.0f", totalExpense)} (${String.format(Locale.US, "%.1f", overallProgress * 100)}%)",
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "建議每日額度: NT$ ${String.format(Locale.US, "%,.0f", dailyAllowable)}",
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            color = VintageGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Category Budgets Title & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "科目分項預算 • CATEGORY LIMITS",
                    fontFamily = CourierPrimeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = VintageGold
                )
                Button(
                    onClick = {
                        selectedCategory = "餐飲美食"
                        budgetAmountInput = "6000"
                        showAddBudgetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp).testTag("add_category_budget_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增科目", fontSize = 11.sp, color = ParchmentSurface)
                }
            }
        }

        val categoryBudgets = currentMonthBudgets.filter { it.category != "ALL" }
        if (categoryBudgets.isEmpty()) {
            item {
                ParchmentCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "尚未設定特定科目預算\n可點選上方「新增科目」為餐飲、購物等設定額度",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(categoryBudgets, key = { it.id }) { b ->
                val spentOnCategory = transactions
                    .filter { it.type == TransactionType.EXPENSE.name && it.category == b.category }
                    .sumOf { it.amount }
                val progress = if (b.amount > 0) (spentOnCategory / b.amount).coerceIn(0.0, 1.0).toFloat() else 0f
                val isWarning = progress >= 0.8f

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(WarmSepia.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = resolveCategoryIcon(
                                            when (b.category) {
                                                "餐飲美食" -> "restaurant"
                                                "日常購物" -> "shopping_bag"
                                                "交通通勤" -> "directions_subway"
                                                "休閒娛樂" -> "movie"
                                                else -> "home"
                                            }
                                        ),
                                        contentDescription = b.category,
                                        tint = WarmSepia,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = b.category,
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (isWarning) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = SealingWaxRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (spentOnCategory > b.amount) "已超額" else "預警",
                                        color = SealingWaxRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (spentOnCategory > b.amount) SealingWaxRed else ForestGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "已用: NT$ ${String.format(Locale.US, "%,.0f", spentOnCategory)}",
                                fontSize = 11.5.sp,
                                fontFamily = CourierPrimeFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "上限: NT$ ${String.format(Locale.US, "%,.0f", b.amount)}",
                                fontSize = 11.5.sp,
                                fontFamily = CourierPrimeFont,
                                color = WarmSepia,
                                fontWeight = FontWeight.Bold
                            )
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
