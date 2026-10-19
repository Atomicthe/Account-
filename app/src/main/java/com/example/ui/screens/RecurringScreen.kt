package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringBill
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsManager
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
import com.example.ui.theme.WarmSepiaLight
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecurringScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recurringBills by viewModel.recurringBills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isLocalOnlyMode by SettingsManager.isLocalOnlyMode(context).collectAsState(initial = true)
    val lastBackupTime by SettingsManager.getLastBackupTime(context).collectAsState(initial = null)

    var showAddBillDialog by remember { mutableStateOf(false) }
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var billCategory by remember { mutableStateOf("居家水電") }
    var billDueDay by remember { mutableStateOf("5") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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
                        value = billCategory,
                        onValueChange = { billCategory = it },
                        label = { Text("分類") },
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
                        val amount = billAmount.toDoubleOrNull() ?: 0.0
                        val day = billDueDay.toLongOrNull()?.coerceIn(1, 31) ?: 1
                        if (billName.isBlank() || amount <= 0) {
                            Toast.makeText(context, "請填寫正確帳單名稱與金額", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addRecurringBill(
                            name = billName,
                            amount = amount,
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "匯入本機備份 (JSON)",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "請在此貼上之前匯出的 JSON 備份檔案內容：",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("{\n  \"version\": 1,\n  \"accounts\": [...]\n}") },
                        textStyle = TextStyle(fontSize = 11.sp, fontFamily = CourierPrimeFont),
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
                        if (importJsonInput.isBlank()) {
                            Toast.makeText(context, "請貼上備份內容", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            try {
                                val count = viewModel.importBackupJson(importJsonInput)
                                if (count > 0) {
                                    Toast.makeText(context, "成功還原 $count 筆本機資料！", Toast.LENGTH_LONG).show()
                                    showImportDialog = false
                                    importJsonInput = ""
                                } else {
                                    Toast.makeText(context, "無有效資料可還原，請確認格式", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "解析錯誤: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("驗證並還原", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "確定清空本機所有紀錄？",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold,
                    color = SealingWaxRed
                )
            },
            text = {
                Text(
                    text = "此操作將徹底清空手機本地資料庫中的所有記帳手帳、帳戶與預算數據，且無法復原。建議您先使用上方功能匯出備份。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                            Toast.makeText(context, "本機所有資料已清空", Toast.LENGTH_SHORT).show()
                            showClearConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("確認清空", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
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
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("add_recurring_button")
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "每月 ${bill.dueDay} 日",
                                    fontSize = 11.sp,
                                    fontFamily = CourierPrimeFont,
                                    color = VintageGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "類別: ${bill.category} • 金額: NT$ ${String.format(Locale.US, "%,.0f", bill.amount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    viewModel.executeRecurringBill(bill)
                                    Toast.makeText(context, "已成功產生本表記錄！", Toast.LENGTH_SHORT).show()
                                    triggerHapticFeedback(context, 20)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmSepiaLight),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("一鍵入帳", fontSize = 11.sp, color = WarmSepia, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = bill.isActive,
                                onCheckedChange = { active ->
                                    viewModel.toggleRecurring(bill.id, active)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarmSepia,
                                    checkedTrackColor = WarmSepiaLight
                                )
                            )
                        }
                    }
                }
            }
        }

        // Data Storage & Privacy Mode Card (本機紀錄無雲端模式)
        item {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "資料儲存與隱私模式",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "LOCAL DATA & OFFLINE PRIVACY",
                                fontFamily = CourierPrimeFont,
                                fontSize = 11.sp,
                                color = VintageGold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLocalOnlyMode) ForestGreen.copy(alpha = 0.12f) else VintageGold.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocalOnlyMode) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isLocalOnlyMode) ForestGreen else VintageGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLocalOnlyMode) "純本機無雲端" else "雲端同步",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocalOnlyMode) ForestGreen else VintageGold
                            )
                        }
                    }

                    // Mode Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "本機紀錄模式 (無雲端)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isLocalOnlyMode)
                                    "已啟用：所有帳本、收支與預算紀錄100%儲存在手機本地 SQLite (Room)，無任何雲端上傳，離線隨開即用，保證個人隱私安全。"
                                else
                                    "目前切換為雲端模式（需配合雲端登入同步）。",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isLocalOnlyMode,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setLocalOnlyMode(context, enabled)
                                    Toast.makeText(
                                        context,
                                        if (enabled) "已啟用本機紀錄無雲端模式" else "已切換為雲端同步模式",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    // Local Backup Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "本機備份狀態",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lastBackupTime.isNullOrBlank()) "尚未匯出本機備份" else "最後備份：$lastBackupTime",
                            fontSize = 11.sp,
                            fontFamily = CourierPrimeFont,
                            color = if (lastBackupTime.isNullOrBlank()) EspressoMuted else ForestGreen
                        )
                    }

                    // Backup & Restore Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export JSON Backup Button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val backupJson = viewModel.exportBackupJson()
                                        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                        SettingsManager.setLastBackupTime(context, nowStr)

                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_SUBJECT, "時光手帳_本機備份_$nowStr.json")
                                            putExtra(Intent.EXTRA_TEXT, backupJson)
                                            type = "application/json"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "分享或儲存本機備份檔案"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "匯出備份失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("匯出本機備份", fontSize = 12.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                        }

                        // Import JSON Backup Button
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VintageGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("還原/匯入備份", fontSize = 12.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Clear local database option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showClearConfirmDialog = true }
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = SealingWaxRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清空本機所有紀錄", color = SealingWaxRed, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Multi-Currency & Live Exchange Rates Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "多幣別與即時匯率",
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "MULTI-CURRENCY & EXCHANGE",
                                fontSize = 10.sp,
                                fontFamily = CourierPrimeFont,
                                color = VintageGold
                            )
                        }

                        var isRefreshing by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                scope.launch {
                                    isRefreshing = true
                                    viewModel.refreshExchangeRates()
                                    isRefreshing = false
                                    Toast.makeText(context, "即時匯率更新完成！", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (isRefreshing) "更新中..." else "更新匯率", fontSize = 11.sp, color = ParchmentSurface)
                        }
                    }

                    Text(
                        text = "支援記錄多國常用外幣，並自動獲取即時國際匯率折算為預設本國幣別 (NT$) 統一檢視：",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rates table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("幣別與國家", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("即時匯率 (折合 NT$)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        com.example.ui.components.PerforatedDivider(modifier = Modifier.padding(vertical = 2.dp))

                        com.example.data.currency.CurrencyManager.supportedCurrencies.filter { it.code != "TWD" }.forEach { curr ->
                            val rate = com.example.data.currency.CurrencyManager.getRateToTwd(curr.code)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(curr.flagEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${curr.name} (${curr.symbol} ${curr.code})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "1 ${curr.code} ≈ NT$ ${String.format(Locale.US, "%.4f", rate)}",
                                    fontSize = 12.sp,
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmSepia
                                )
                            }
                        }
                    }

                    // Quick Currency Converter calculator
                    var testForeignAmt by remember { mutableStateOf("1000") }
                    var testCurrCode by remember { mutableStateOf("JPY") }
                    val numAmt = testForeignAmt.toDoubleOrNull() ?: 0.0
                    val twdConverted = com.example.data.currency.CurrencyManager.convertToTwd(numAmt, testCurrCode)

                    Text(
                        text = "即時匯率換算試算：",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmSepia
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testForeignAmt,
                            onValueChange = { testForeignAmt = it },
                            label = { Text("外幣金額") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Currency selector
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("JPY", "USD", "EUR", "KRW", "CNY", "THB").forEach { c ->
                                val isSel = testCurrCode == c
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) WarmSepia.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSel) 1.2.dp else 0.5.dp,
                                            color = if (isSel) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { testCurrCode = c }
                                        .padding(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = c,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ForestGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${testCurrCode} $testForeignAmt ≈ NT$ ${String.format(Locale.US, "%,.1f", twdConverted)} (折合新台幣)",
                            fontSize = 12.sp,
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }

                    Text(
                        text = "更新狀態：${com.example.data.currency.CurrencyManager.lastUpdatedTime}",
                        fontSize = 10.sp,
                        fontFamily = CourierPrimeFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tools & Preferences Settings Card
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
                        text = "外觀與個人化設定 • PREFERENCES",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("匯出本月記帳 CSV 報表", color = WarmSepia, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("深色模式", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        val isDarkModeEnabled by SettingsManager.isDarkModeEnabled(context).collectAsState(initial = false)

                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setDarkModeEnabled(context, enabled)
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("字體大小調整 (+ / -)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        val fontSizeOffset by SettingsManager.getFontSizeOffset(context).collectAsState(initial = 0)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        SettingsManager.setFontSizeOffset(context, (fontSizeOffset - 2).coerceAtLeast(0))
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                            ) {
                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ParchmentSurface)
                            }
                            Text(
                                text = "$fontSizeOffset",
                                fontSize = 14.sp,
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        SettingsManager.setFontSizeOffset(context, (fontSizeOffset + 2).coerceAtMost(8))
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ParchmentSurface)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("開啟生物辨識/指紋解鎖", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        val isBiometricEnabled by SettingsManager.isBiometricEnabled(context).collectAsState(initial = false)

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setBiometricEnabled(context, enabled)
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
