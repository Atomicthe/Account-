package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultCategories
import com.example.data.model.LedgerBudget
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsManager
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
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val customCategories by SettingsManager.getCustomCategories(context).collectAsState(initial = emptyList())

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showOverallBudgetDialog by remember { mutableStateOf(false) }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var budgetToDelete by remember { mutableStateOf<LedgerBudget?>(null) }

    var selectedBudgetCategory by remember { mutableStateOf("餐飲美食") }
    var budgetAmountInput by remember { mutableStateOf("") }
    var overallBudgetInput by remember { mutableStateOf("") }

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

    val allCategoryOptions = remember(customCategories) {
        DefaultCategories.expenseCategories.map { it.name } + customCategories
    }

    val resolveCategoryEmoji: (String) -> String = { name ->
        val trimmed = name.trim()
        if (trimmed.contains(" ")) {
            trimmed.substringBefore(" ")
        } else {
            when (trimmed) {
                "餐飲美食" -> "🍜"
                "日常購物" -> "🛍️"
                "交通通勤" -> "🚇"
                "休閒娛樂" -> "🎬"
                "居家水電" -> "💡"
                "醫療保健" -> "💊"
                "學習進修" -> "📚"
                "社交人情" -> "🎁"
                else -> "📌"
            }
        }
    }

    // 1. Overall Budget Dialog
    if (showOverallBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showOverallBudgetDialog = false },
            title = {
                Text(
                    text = "設定月度總預算",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "為 $currentMonth 設定每月最高可支配額度：",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = overallBudgetInput,
                        onValueChange = { overallBudgetInput = it },
                        label = { Text("總預算上限 (NT$)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = overallBudgetInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.saveBudget("ALL", amt)
                            showOverallBudgetDialog = false
                            triggerHapticFeedback(context, 20)
                        } else {
                            Toast.makeText(context, "請輸入有效的預算金額", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確定儲存", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverallBudgetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 2. Custom Category Dialog with Emoji, Icons & Colors
    if (showAddCustomCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var chosenEmoji by remember { mutableStateOf("☕") }
        var chosenIconName by remember { mutableStateOf("restaurant") }
        var chosenColorHex by remember { mutableStateOf("#7A3E26") }

        val presetEmojis = listOf(
            "🍜", "☕", "🍰", "🍣", "🍺", "🛍️", "👗", "💄",
            "🚇", "🚗", "✈️", "🎬", "🎮", "🏠", "💡", "💊",
            "📚", "🎁", "👶", "🐱", "🐶", "🪴", "🏋️", "⚽"
        )

        val systemIcons = listOf(
            "restaurant" to "餐飲",
            "shopping_bag" to "購物",
            "directions_subway" to "交通",
            "movie" to "娛樂",
            "home" to "居家",
            "medical_services" to "醫療",
            "menu_book" to "進修",
            "card_giftcard" to "禮品",
            "savings" to "儲蓄",
            "laptop_mac" to "數位",
            "payments" to "消費",
            "trending_up" to "收益"
        )

        val systemColors = listOf(
            "#7A3E26" to "暖褐",
            "#2E5A44" to "森綠",
            "#C8963E" to "古銅金",
            "#9E2A2B" to "茜紅",
            "#3D5A80" to "岩藍",
            "#5C4B77" to "紫檀",
            "#B85D19" to "赤土",
            "#665C54" to "茶灰"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomCategoryDialog = false },
            title = {
                Text(
                    text = "新增自訂分類與 EMOJI",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(chosenColorHex)).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = chosenEmoji, fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = if (newCatName.isNotBlank()) newCatName else "預覽分類名稱",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "圖示: $chosenIconName • 色調: $chosenColorHex",
                                    fontSize = 10.sp,
                                    fontFamily = CourierPrimeFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 1. Emoji Selection & Input
                    Text("1. 選擇或輸入 Emoji:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WarmSepia)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, WarmSepia, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = chosenEmoji, fontSize = 22.sp)
                        }
                        OutlinedTextField(
                            value = chosenEmoji,
                            onValueChange = { if (it.isNotBlank()) chosenEmoji = it.take(2) },
                            label = { Text("自行輸入 Emoji") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        presetEmojis.forEach { emoji ->
                            val isSel = chosenEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepia.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (isSel) 1.5.dp else 0.5.dp,
                                        color = if (isSel) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { chosenEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    // 2. Category Name
                    Text("2. 分類名稱:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WarmSepia)
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("例如：下午茶、寵物貓狗、露營") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. System Category Icon
                    Text("3. 系統關聯圖示:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WarmSepia)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        systemIcons.forEach { (iconKey, label) ->
                            val isSel = chosenIconName == iconKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepia.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (isSel) 1.2.dp else 0.5.dp,
                                        color = if (isSel) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { chosenIconName = iconKey }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = resolveCategoryIcon(iconKey),
                                        contentDescription = label,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 4. System Color Choice
                    Text("4. 專屬主題色彩:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WarmSepia)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        systemColors.forEach { (colorHex, colorLabel) ->
                            val parsedCol = Color(android.graphics.Color.parseColor(colorHex))
                            val isSel = chosenColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsedCol)
                                    .border(
                                        width = if (isSel) 2.dp else 0.5.dp,
                                        color = if (isSel) ParchmentSurface else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { chosenColorHex = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = colorLabel, tint = ParchmentSurface, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val finalCatString = "$chosenEmoji $newCatName"
                            scope.launch {
                                SettingsManager.addCustomCategory(context, finalCatString)
                            }
                            selectedBudgetCategory = finalCatString
                            showAddCustomCategoryDialog = false
                            triggerHapticFeedback(context, 30)
                            Toast.makeText(context, "已成功建立自訂分類：$finalCatString", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "請輸入分類名稱", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確認建立", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomCategoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 3. Add / Edit Category Budget Dialog
    if (showAddBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = {
                Text(
                    text = "設定科目預算上限",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("選擇預算科目：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            showAddBudgetDialog = false
                            showAddCustomCategoryDialog = true
                        }) {
                            Text("+ 自訂新分類", fontSize = 11.5.sp, color = WarmSepia, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Category selection chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allCategoryOptions.forEach { catName ->
                            val isSel = selectedBudgetCategory == catName
                            val emoji = resolveCategoryEmoji(catName)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedBudgetCategory = catName },
                                label = {
                                    Text(
                                        text = "$emoji $catName",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarmSepiaLight,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = budgetAmountInput,
                        onValueChange = { budgetAmountInput = it },
                        label = { Text("預算金額上限 (NT$)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedBudgetCategory.isNotBlank()) {
                            viewModel.saveBudget(selectedBudgetCategory, amt)
                            showAddBudgetDialog = false
                            budgetAmountInput = ""
                            triggerHapticFeedback(context, 20)
                            Toast.makeText(context, "已儲存 $selectedBudgetCategory 預算：NT$ ${String.format(Locale.US, "%,.0f", amt)}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "請輸入有效的金額與科目", Toast.LENGTH_SHORT).show()
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

    // 4. Delete Custom Category Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("刪除自訂分類", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除「$cat」自訂分類嗎？過往記錄不會受影響。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            SettingsManager.deleteCustomCategory(context, cat)
                        }
                        categoryToDelete = null
                        Toast.makeText(context, "已刪除自訂分類", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("刪除", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("取消") }
            }
        )
    }

    // 5. Delete Category Budget Confirmation
    budgetToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("移除科目預算", fontWeight = FontWeight.Bold) },
            text = { Text("確定要移除「${b.category}」的預算上限嗎？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBudget(b)
                        budgetToDelete = null
                        Toast.makeText(context, "已移除預算項目", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("確定移除", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) { Text("取消") }
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
                                overallBudgetInput = overallBudget.toInt().toString()
                                showOverallBudgetDialog = true
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

        // Custom Categories & Emoji Section Card
        item {
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
                                text = "自訂分類與 EMOJI 管理",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自選圖示、主題色彩與專屬 Emoji",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddCustomCategoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = ParchmentSurface)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("新增自訂", fontSize = 11.sp, color = ParchmentSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customCategories.isEmpty()) {
                        Text(
                            text = "目前尚未建立自訂分類，點選上方「新增自訂」即可建立專屬的 Emoji 與圖示分類！",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customCategories.forEach { catName ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = catName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "刪除分類",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { categoryToDelete = catName }
                                    )
                                }
                            }
                        }
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
                        selectedBudgetCategory = allCategoryOptions.firstOrNull() ?: "餐飲美食"
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
                    Text("新增科目預算", fontSize = 11.sp, color = ParchmentSurface)
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
                            text = "尚未設定特定科目預算\n可點選上方「新增科目預算」為餐飲、購物或自訂分類設定額度",
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
                val emoji = resolveCategoryEmoji(b.category)

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
                                    Text(text = emoji, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = b.category,
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = {
                                        selectedBudgetCategory = b.category
                                        budgetAmountInput = b.amount.toInt().toString()
                                        showAddBudgetDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改預算",
                                        tint = WarmSepia,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { budgetToDelete = b },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "刪除預算",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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
                            color = if (spentOnCategory > b.amount) SealingWaxRed else (if (progress > 0.8f) VintageGold else ForestGreen),
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
