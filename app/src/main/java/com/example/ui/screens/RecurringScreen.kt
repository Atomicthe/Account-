package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringBill
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
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
fun RecurringScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recurringBills by viewModel.recurringBills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showAddBillDialog by remember { mutableStateOf(false) }
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var billCategory by remember { mutableStateOf("居家水電") }
    var billDueDay by remember { mutableStateOf("5") }

    if (showAddBillDialog) {
        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = {
                Text("新增固定週期帳單", fontFamily = PlayfairFont, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        label = { Text("項目名稱 (如: 房租、Netflix)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { billAmount = it },
                        label = { Text("金額 (NT$)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmSepia,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = billDueDay,
                        onValueChange = { billDueDay = it },
                        label = { Text("每月扣款日 (1~31)") },
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
                        val amt = billAmount.toDoubleOrNull() ?: 0.0
                        val day = billDueDay.toLongOrNull() ?: 1L
                        if (billName.isBlank() || amt <= 0.0) {
                            Toast.makeText(context, "請填寫完整資訊", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addRecurringBill(
                            name = billName,
                            amount = amt,
                            type = TransactionType.EXPENSE,
                            category = billCategory,
                            accountId = accounts.firstOrNull()?.id ?: "",
                            dueDay = day
                        )
                        showAddBillDialog = false
                        billName = ""
                        billAmount = ""
                        triggerHapticFeedback(context, 30)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("儲存週期項目", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
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
        // Recurring Header & Add
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
                        Column {
                            Text(
                                text = "固定收支與週期帳單",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "RECURRING BILLS & SUBSCRIPTIONS",
                                fontFamily = CourierPrimeFont,
                                fontSize = 11.sp,
                                color = VintageGold
                            )
                        }

                        Button(
                            onClick = { showAddBillDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("add_recurring_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增項目", fontSize = 12.sp, color = ParchmentSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "在此管理每月房租、水電或軟體訂閱。可點選「一鍵入帳」將該筆帳單快速轉化為今日手帳記錄。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // List of Recurring Bills
        if (recurringBills.isEmpty()) {
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
                            text = "暫無固定週期帳單，點擊上方「新增項目」建立",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recurringBills, key = { it.id }) { bill ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bill.name,
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "每月 ${bill.dueDay} 日",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 11.sp,
                                    color = VintageGold
                                )
                            }
                            Text(
                                text = "NT$ ${String.format(Locale.US, "%,.0f", bill.amount)} • ${bill.category}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (bill.type == TransactionType.INCOME.name) ForestGreen else SealingWaxRed
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Execute button
                            Button(
                                onClick = {
                                    viewModel.executeRecurringBill(bill)
                                    Toast.makeText(context, "已成功記錄：${bill.name}", Toast.LENGTH_SHORT).show()
                                    triggerHapticFeedback(context, 40)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmSepiaLight),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("execute_bill_${bill.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Execute",
                                    tint = WarmSepia,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("記帳", fontSize = 11.sp, color = WarmSepia, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = bill.isActive,
                                onCheckedChange = { viewModel.toggleRecurring(bill.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ParchmentSurface,
                                    checkedTrackColor = WarmSepia
                                )
                            )
                        }
                    }
                }
            }
        }

        // Tools & Account Settings Card
        item {
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
                        text = "進階工具與手帳設定 • TOOLS",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VintageGold
                    )

                    Button(
                        onClick = {
                            val csv = viewModel.generateCsvExport()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csv)
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "分享手帳 CSV 報表"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmSepia.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("匯出本月記帳 CSV 檔案", color = WarmSepia, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("開啟生物辨識/指紋解鎖", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        val scope = rememberCoroutineScope()
                        val isBiometricEnabled by com.example.data.preferences.SettingsManager.isBiometricEnabled(context).collectAsState(initial = false)
                        
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    com.example.data.preferences.SettingsManager.setBiometricEnabled(context, enabled)
                                }
                            }
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
