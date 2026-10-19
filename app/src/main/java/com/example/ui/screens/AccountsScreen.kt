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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.AccountType
import com.example.data.model.LedgerAccount
import com.example.data.model.StampType
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
import com.example.ui.theme.VintageGoldLight
import com.example.ui.theme.WarmSepia
import com.example.ui.theme.WarmSepiaLight
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // New account form state
    var newAccountName by remember { mutableStateOf("") }
    var newAccountType by remember { mutableStateOf(AccountType.CASH) }
    var newAccountInitialBalance by remember { mutableStateOf("") }

    // Transfer form state
    var transferFromId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var transferToId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var transferAmount by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }

    val totalAssets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
    val totalLiabilities = accounts.filter { it.balance < 0 }.sumOf { -it.balance }
    val netWorth = totalAssets - totalLiabilities

    // Add Account Dialog
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = {
                Text("新增錢包/帳戶", fontFamily = PlayfairFont, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("帳戶名稱 (如: 羊皮錢包、郵局存摺)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Text("帳戶種類：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountType.values().forEach { t ->
                            val isSel = newAccountType == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepiaLight else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSel) WarmSepia else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { newAccountType = t }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(t.label.take(4), fontSize = 11.sp, color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newAccountInitialBalance,
                        onValueChange = { newAccountInitialBalance = it },
                        label = { Text("初始餘額 (NT$)") },
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
                        if (newAccountName.isBlank()) {
                            Toast.makeText(context, "請輸入帳戶名稱", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val bal = newAccountInitialBalance.toDoubleOrNull() ?: 0.0
                        viewModel.addAccount(
                            name = newAccountName,
                            type = newAccountType,
                            initialBalance = bal
                        )
                        showAddAccountDialog = false
                        newAccountName = ""
                        newAccountInitialBalance = ""
                        triggerHapticFeedback(context, 30)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確認建立", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Quick Transfer Dialog
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Text("帳戶資金互轉", fontFamily = PlayfairFont, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("轉出帳戶：", fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            val isSel = transferFromId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepiaLight else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSel) WarmSepia else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { transferFromId = acc.id }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(acc.name, fontSize = 11.sp, color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Text("轉入帳戶：", fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.filter { it.id != transferFromId }.forEach { acc ->
                            val isSel = transferToId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) VintageGoldLight else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSel) VintageGold else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { transferToId = acc.id }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(acc.name, fontSize = 11.sp, color = if (isSel) VintageGold else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("轉帳金額 (NT$)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("轉帳備註 (可選)") },
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
                        val amt = transferAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "請輸入有效金額", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (transferFromId.isBlank() || transferToId.isBlank()) {
                            Toast.makeText(context, "請選擇來源與目標帳戶", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        viewModel.addTransaction(
                            type = TransactionType.TRANSFER,
                            amount = amt,
                            accountId = transferFromId,
                            toAccountId = transferToId,
                            category = "帳戶互轉",
                            date = todayStr,
                            note = transferNote.ifBlank { "帳戶互轉" },
                            merchant = "內部資金調度",
                            tags = "#轉帳"
                        )
                        showTransferDialog = false
                        transferAmount = ""
                        transferNote = ""
                        triggerHapticFeedback(context, 40)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確認轉帳", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
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
        // Net Worth Card
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
                    Text(
                        text = "總淨資產 • NET WORTH",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NT$ ${String.format(Locale.US, "%,.0f", netWorth)}",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = if (netWorth >= 0) ForestGreen else SealingWaxRed
                    )

                    PerforatedDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("正資產總額", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "NT$ ${String.format(Locale.US, "%,.0f", totalAssets)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ForestGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("負債 (信用卡等)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "NT$ ${String.format(Locale.US, "%,.0f", totalLiabilities)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SealingWaxRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (accounts.size >= 2) {
                                    transferFromId = accounts.first().id
                                    transferToId = accounts[1].id
                                    showTransferDialog = true
                                } else {
                                    Toast.makeText(context, "請先建立至少兩個帳戶以進行互轉", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VintageGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("transfer_button")
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("帳戶互轉", fontSize = 13.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAddAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("add_account_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增錢包", fontSize = 13.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Accounts List Title
        item {
            Text(
                text = "各錢包與資產帳目 • ACCOUNTS",
                fontFamily = CourierPrimeFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = VintageGold
            )
        }

        items(accounts, key = { it.id }) { acc ->
            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WarmSepia.copy(alpha = 0.12f))
                                .border(1.dp, WarmSepia.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveCategoryIcon(acc.icon),
                                contentDescription = acc.name,
                                tint = WarmSepia,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = acc.name,
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AccountType.values().find { it.name == acc.type }?.label ?: acc.type,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NT$ ${String.format(Locale.US, "%,.0f", acc.balance)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (acc.balance >= 0) ForestGreen else SealingWaxRed
                        )
                        Text(
                            text = if (acc.balance >= 0) "資產結餘" else "尚欠帳款",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
