package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultCategories
import com.example.data.model.LedgerAccount
import com.example.data.model.StampType
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.RetroCalculatorPad
import com.example.ui.components.WaxSealBadge
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.VintageGoldLight
import com.example.ui.theme.WarmSepia
import com.example.ui.theme.WarmSepiaLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    accounts: List<LedgerAccount>,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        category: String,
        date: String,
        note: String,
        merchant: String,
        tags: String,
        stamp: StampType
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var txType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("0") }
    var selectedCategory by remember { mutableStateOf(DefaultCategories.expenseCategories.first().name) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedToAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var note by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf("#日常")) }
    var selectedStamp by remember { mutableStateOf(StampType.PAID) }
    var dateStr by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    val availableCategories = if (txType == TransactionType.INCOME) {
        DefaultCategories.incomeCategories
    } else {
        DefaultCategories.expenseCategories
    }

    val availableTags = listOf("#日常", "#聚餐", "#下午茶", "#大額", "#出差", "#娛樂", "#禮物", "#固定")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("add_transaction_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row: Type switcher & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Switcher: Expense / Income / Transfer
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        val isSelected = txType == type
                        val activeColor = when (type) {
                            TransactionType.EXPENSE -> SealingWaxRed
                            TransactionType.INCOME -> ForestGreen
                            TransactionType.TRANSFER -> VintageGold
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .clickable {
                                    triggerHapticFeedback(context, 10)
                                    txType = type
                                    if (type == TransactionType.INCOME) {
                                        selectedCategory = DefaultCategories.incomeCategories.first().name
                                        selectedStamp = StampType.APPROVED
                                    } else if (type == TransactionType.EXPENSE) {
                                        selectedCategory = DefaultCategories.expenseCategories.first().name
                                        selectedStamp = StampType.PAID
                                    } else {
                                        selectedCategory = "帳戶互轉"
                                        selectedStamp = StampType.RECEIPT
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("type_tab_${type.name}")
                        ) {
                            Text(
                                text = type.label,
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) ParchmentSurface else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WarmSepia)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vintage Receipt Display Box
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
                        Text(
                            text = "金額 • AMOUNT",
                            fontFamily = CourierPrimeFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        WaxSealBadge(stamp = selectedStamp.name, size = 32.dp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NT$",
                            fontFamily = PlayfairFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = WarmSepia
                        )
                        Text(
                            text = amountInput,
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = when (txType) {
                                TransactionType.EXPENSE -> SealingWaxRed
                                TransactionType.INCOME -> ForestGreen
                                TransactionType.TRANSFER -> WarmSepia
                            },
                            modifier = Modifier.testTag("amount_display")
                        )
                    }

                    PerforatedDivider(modifier = Modifier.padding(vertical = 6.dp))

                    // Accounts Selection Row
                    if (txType == TransactionType.TRANSFER) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("轉出帳戶", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    accounts.find { it.id == selectedAccountId }?.name ?: "選擇帳戶",
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text("➔", color = VintageGold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("轉入帳戶", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    accounts.find { it.id == selectedToAccountId }?.name ?: "選擇帳戶",
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "記帳帳戶: ${accounts.find { it.id == selectedAccountId }?.name ?: "未設定"}",
                                fontSize = 12.sp,
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmSepia
                            )
                            Text(
                                text = "日期: $dateStr",
                                fontSize = 12.sp,
                                fontFamily = CourierPrimeFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Account Choice Chips
            Text(
                text = "選擇錢包 / 帳戶",
                fontSize = 12.sp,
                fontFamily = PlayfairFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isChosen = if (txType == TransactionType.TRANSFER) selectedAccountId == acc.id else selectedAccountId == acc.id
                    FilterChip(
                        selected = isChosen,
                        onClick = {
                            selectedAccountId = acc.id
                            triggerHapticFeedback(context, 10)
                        },
                        label = { Text(acc.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmSepiaLight,
                            selectedLabelColor = WarmSepia
                        )
                    )
                }
            }

            if (txType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "選擇目標轉入帳戶",
                    fontSize = 12.sp,
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        val isChosen = selectedToAccountId == acc.id
                        FilterChip(
                            selected = isChosen,
                            onClick = {
                                selectedToAccountId = acc.id
                                triggerHapticFeedback(context, 10)
                            },
                            label = { Text(acc.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VintageGoldLight,
                                selectedLabelColor = VintageGold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Selector (if not transfer)
            if (txType != TransactionType.TRANSFER) {
                Text(
                    text = "分類科目",
                    fontSize = 12.sp,
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) WarmSepiaLight else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) WarmSepia else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategory = cat.name
                                    triggerHapticFeedback(context, 10)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = resolveCategoryIcon(cat.iconName),
                                contentDescription = cat.name,
                                tint = if (isSelected) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) WarmSepia else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Note & Merchant Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商家/對象", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmSepia,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("筆記備註", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmSepia,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableTags.forEach { tag ->
                    val isTagged = selectedTags.contains(tag)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTagged) VintageGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.8.dp, if (isTagged) VintageGold else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTags = if (isTagged) selectedTags - tag else selectedTags + tag
                                triggerHapticFeedback(context, 10)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            color = if (isTagged) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stamp selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "手帳印章:",
                    fontFamily = PlayfairFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StampType.values().forEach { st ->
                    val isChosen = selectedStamp == st
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChosen) WarmSepiaLight else Color.Transparent)
                            .border(1.dp, if (isChosen) WarmSepia else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedStamp = st
                                triggerHapticFeedback(context, 15)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = st.label,
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            color = if (isChosen) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mechanical Calculator Keypad
            RetroCalculatorPad(
                currentInput = amountInput,
                onValueChange = { amountInput = it },
                onDone = {
                    val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                    if (amountVal <= 0.0) {
                        Toast.makeText(context, "請輸入大於 0 的金額", Toast.LENGTH_SHORT).show()
                        return@RetroCalculatorPad
                    }
                    if (selectedAccountId.isBlank()) {
                        Toast.makeText(context, "請選擇一個帳戶", Toast.LENGTH_SHORT).show()
                        return@RetroCalculatorPad
                    }

                    triggerHapticFeedback(context, 40)
                    onSaveTransaction(
                        txType,
                        amountVal,
                        selectedAccountId,
                        if (txType == TransactionType.TRANSFER) selectedToAccountId else "",
                        if (txType == TransactionType.TRANSFER) "帳戶互轉" else selectedCategory,
                        dateStr,
                        note,
                        merchant,
                        selectedTags.joinToString(","),
                        selectedStamp
                    )
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Save Action Button
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                    if (amountVal <= 0.0) {
                        Toast.makeText(context, "請輸入大於 0 的金額", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedAccountId.isBlank()) {
                        Toast.makeText(context, "請選擇一個帳戶", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    triggerHapticFeedback(context, 50)
                    onSaveTransaction(
                        txType,
                        amountVal,
                        selectedAccountId,
                        if (txType == TransactionType.TRANSFER) selectedToAccountId else "",
                        if (txType == TransactionType.TRANSFER) "帳戶互轉" else selectedCategory,
                        dateStr,
                        note,
                        merchant,
                        selectedTags.joinToString(","),
                        selectedStamp
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmSepia, contentColor = ParchmentSurface)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "蓋章存檔 • SAVE RECORD",
                    fontFamily = CourierPrimeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
