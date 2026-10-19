package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import com.example.data.model.TravelLedger
import com.example.data.preferences.SettingsManager
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.resolveCategoryIcon
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
import com.example.ui.utils.ParsedVoiceRecord
import com.example.ui.utils.VoiceBookkeepingParser
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedAccountFilter by viewModel.selectedAccountFilter.collectAsState()
    val travels by viewModel.travelLedgers.collectAsState()
    val selectedTravel by viewModel.selectedTravel.collectAsState()

    var showFilterBar by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<LedgerTransaction?>(null) }
    var txToEdit by remember { mutableStateOf<LedgerTransaction?>(null) }
    var showCreateTravelDialog by remember { mutableStateOf(false) }
    var travelToDelete by remember { mutableStateOf<TravelLedger?>(null) }

    // Direct Voice Bookkeeping Sheet
    var showVoiceSheet by remember { mutableStateOf(false) }
    var parsedVoiceRecord by remember { mutableStateOf<ParsedVoiceRecord?>(null) }

    // Speech Recognizer Launcher for Direct Screen Mic
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenMatches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val parsed = VoiceBookkeepingParser.parse(spoken)
                parsedVoiceRecord = parsed
                showVoiceSheet = true
                triggerHapticFeedback(context, 30)
                Toast.makeText(context, "語音辨識完成：『$spoken』", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isTravelMode = selectedTravel != null

    // Summary numbers for current view
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Group transactions by date
    val groupedByDate = transactions.groupBy { it.date }.toSortedMap(compareByDescending { it })

    // Delete Transaction Dialog
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = {
                Text(
                    text = "刪除此筆記錄？",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("刪除後相應帳戶餘額將自動校正回歸。") },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete?.let { viewModel.deleteTransaction(it) }
                        txToDelete = null
                        triggerHapticFeedback(context, 30)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("確認刪除", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete Travel Dialog
    travelToDelete?.let { travel ->
        AlertDialog(
            onDismissRequest = { travelToDelete = null },
            title = { Text("結束/刪除此趟旅程？", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除「${travel.name}」嗎？該旅程內的記帳記錄將被歸檔保留。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTravel(travel)
                        travelToDelete = null
                        triggerHapticFeedback(context, 25)
                        Toast.makeText(context, "已刪除旅程記帳", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SealingWaxRed)
                ) {
                    Text("確定刪除", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { travelToDelete = null }) { Text("取消") }
            }
        )
    }

    // Create New Travel Dialog
    if (showCreateTravelDialog) {
        var travelName by remember { mutableStateOf("") }
        var travelDestination by remember { mutableStateOf("") }
        var travelCurrency by remember { mutableStateOf("JPY") }
        var travelBudgetInput by remember { mutableStateOf("") }
        var travelStartDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var travelEndDate by remember { mutableStateOf("") }

        val presetTripEmojis = listOf("🗼", "✈️", "🏰", "🍁", "🌸", "🏖️", "🍜", "🏔️")

        AlertDialog(
            onDismissRequest = { showCreateTravelDialog = false },
            title = {
                Text(
                    text = "建立新旅遊記帳 ✈️",
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
                    Text("獨立記錄每趟旅程的開銷、預算與外幣：", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = travelName,
                        onValueChange = { travelName = it },
                        label = { Text("旅程名稱 (例如：東京賞楓五日遊 🗼)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick emoji insertion for trip
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTripEmojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (!travelName.contains(emoji)) {
                                            travelName = "$travelName $emoji".trim()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = travelDestination,
                        onValueChange = { travelDestination = it },
                        label = { Text("目的地 (例如：日本東京、法國巴黎)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("選擇該趟旅程預設幣別：", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WarmSepia)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyManager.supportedCurrencies.forEach { curr ->
                            val isSel = travelCurrency == curr.code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WarmSepia.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (isSel) 1.2.dp else 0.5.dp,
                                        color = if (isSel) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { travelCurrency = curr.code }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${curr.flagEmoji} ${curr.code} (${curr.name})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val selectedCurrDef = CurrencyManager.getCurrency(travelCurrency)
                    OutlinedTextField(
                        value = travelBudgetInput,
                        onValueChange = { travelBudgetInput = it },
                        label = { Text("旅程總預算 (${selectedCurrDef.symbol} ${selectedCurrDef.code})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = travelStartDate,
                        onValueChange = { travelStartDate = it },
                        label = { Text("出發日期 (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (travelName.isNotBlank()) {
                            val bAmt = travelBudgetInput.toDoubleOrNull() ?: 0.0
                            viewModel.addTravel(
                                name = travelName,
                                destination = travelDestination,
                                currency = travelCurrency,
                                budget = bAmt,
                                startDate = travelStartDate,
                                endDate = travelEndDate
                            )
                            showCreateTravelDialog = false
                            triggerHapticFeedback(context, 30)
                            Toast.makeText(context, "已成功建立旅程：$travelName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "請輸入旅程名稱", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確認建立", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTravelDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mode Switcher: [📓 日常手帳] vs [✈️ 旅遊記帳]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(3.dp)
            ) {
                // Daily Ledger Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isTravelMode) WarmSepia else Color.Transparent)
                        .clickable {
                            viewModel.selectTravel(null)
                            triggerHapticFeedback(context, 10)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (!isTravelMode) ParchmentSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "日常手帳",
                            fontSize = 12.sp,
                            fontWeight = if (!isTravelMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isTravelMode) ParchmentSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Travel Ledger Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isTravelMode) WarmSepia else Color.Transparent)
                        .clickable {
                            if (travels.isNotEmpty() && selectedTravel == null) {
                                viewModel.selectTravel(travels.first().id)
                            } else if (travels.isEmpty()) {
                                showCreateTravelDialog = true
                            }
                            triggerHapticFeedback(context, 10)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = if (isTravelMode) ParchmentSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (travels.isNotEmpty()) "旅遊記帳 (${travels.size})" else "旅遊記帳",
                            fontSize = 12.sp,
                            fontWeight = if (isTravelMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTravelMode) ParchmentSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Voice Bookkeeping Action Button
            IconButton(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出記帳內容，例如：「晚餐拉麵250元」或「計程車180」")
                        }
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "本機無可用語音辨識服務", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WarmSepia.copy(alpha = 0.12f))
                    .border(1.dp, WarmSepia.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "語音記帳",
                    tint = WarmSepia,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Travel Selection Chips Row (Visible in Travel Mode)
        if (isTravelMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                travels.forEach { travel ->
                    val isSel = selectedTravel?.id == travel.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) WarmSepia.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSel) 1.2.dp else 0.5.dp,
                                color = if (isSel) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.selectTravel(travel.id) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${travel.name} [${travel.currency}]",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add Trip Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarmSepia)
                        .clickable { showCreateTravelDialog = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ParchmentSurface, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("+ 新增旅程", fontSize = 11.sp, color = ParchmentSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Banner / Overview Card
        ParchmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                val ledgerTitle by SettingsManager.getLedgerTitle(context).collectAsState(initial = "記帳")
                val isLocalOnlyMode by SettingsManager.isLocalOnlyMode(context).collectAsState(initial = true)
                val isTitleHintDismissed by SettingsManager.isTitleEditHintDismissed(context).collectAsState(initial = false)
                var showEditTitleDialog by remember { mutableStateOf(false) }
                var showStorageModeDialog by remember { mutableStateOf(false) }
                var editedTitleInput by remember { mutableStateOf(ledgerTitle) }

                if (showStorageModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showStorageModeDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLocalOnlyMode) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (isLocalOnlyMode) ForestGreen else VintageGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLocalOnlyMode) "本機紀錄模式（無雲端）" else "雲端同步模式",
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isLocalOnlyMode)
                                        "目前使用 100% 本機端儲存模式，所有帳目與手帳記錄直接保留於手機 SQLite 資料庫，完全不上傳任何雲端伺服器，保護個人財務隱私。"
                                    else
                                        "目前啟用雲端同步備份模式。",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showStorageModeDialog = false }) {
                                Text("我知道了", color = WarmSepia)
                            }
                        }
                    )
                }

                if (showEditTitleDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditTitleDialog = false },
                        title = { Text("修改手帳標題", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = editedTitleInput,
                                onValueChange = { editedTitleInput = it },
                                label = { Text("標題名稱") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (editedTitleInput.isNotBlank()) {
                                        scope.launch {
                                            SettingsManager.setLedgerTitle(context, editedTitleInput)
                                        }
                                        showEditTitleDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                            ) { Text("確定", color = ParchmentSurface) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditTitleDialog = false }) { Text("取消") }
                        }
                    )
                }

                if (isTravelMode && selectedTravel != null) {
                    val trip = selectedTravel!!
                    val tripCurrDef = CurrencyManager.getCurrency(trip.currency)
                    val tripTotalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf {
                        if (it.originalAmount > 0) it.originalAmount else it.amount
                    }
                    val convertedTotalTwd = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                    val tripBudget = trip.budget
                    val budgetProgress = if (tripBudget > 0) (tripTotalExpense / tripBudget).coerceIn(0.0, 1.0).toFloat() else 0f

                    // Travel Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trip.name,
                                    fontFamily = PlayfairFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarmSepia.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${tripCurrDef.flagEmoji} ${tripCurrDef.code}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmSepia
                                    )
                                }
                            }
                            if (trip.destination.isNotBlank()) {
                                Text(
                                    text = "目的地: ${trip.destination} • ${transactions.size} 筆花費",
                                    fontSize = 10.5.sp,
                                    fontFamily = CourierPrimeFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { travelToDelete = trip },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除旅程",
                                tint = EspressoMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Spend vs Budget in Trip Currency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("旅程支出", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${tripCurrDef.symbol} ${String.format(Locale.US, "%,.0f", tripTotalExpense)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SealingWaxRed
                            )
                            if (trip.currency != "TWD") {
                                Text(
                                    text = "≈ NT$ ${String.format(Locale.US, "%,.0f", convertedTotalTwd)}",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 11.sp,
                                    color = VintageGold
                                )
                            }
                        }

                        if (tripBudget > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("預算上限", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${tripCurrDef.symbol} ${String.format(Locale.US, "%,.0f", tripBudget)}",
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = WarmSepia
                                )
                                val remaining = tripBudget - tripTotalExpense
                                Text(
                                    text = if (remaining >= 0) "剩餘: ${tripCurrDef.symbol} ${String.format(Locale.US, "%,.0f", remaining)}" else "已超額！",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 10.5.sp,
                                    color = if (remaining >= 0) ForestGreen else SealingWaxRed
                                )
                            }
                        }
                    }

                    if (tripBudget > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (tripTotalExpense > tripBudget) SealingWaxRed else ForestGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                } else {
                    // Standard Daily Ledger Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title on left (Click hint automatically disappears after first click)
                        Column(
                            modifier = Modifier.clickable {
                                editedTitleInput = ledgerTitle
                                showEditTitleDialog = true
                                scope.launch {
                                    SettingsManager.setTitleEditHintDismissed(context, true)
                                }
                            }
                        ) {
                            Text(
                                text = ledgerTitle,
                                fontFamily = PlayfairFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (!isTitleHintDismissed) "點擊修改標題 • ${transactions.size} 筆" else "${transactions.size} 筆紀錄",
                                    fontFamily = CourierPrimeFont,
                                    fontSize = 10.sp,
                                    color = VintageGold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .clickable { showStorageModeDialog = true }
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isLocalOnlyMode) ForestGreen else VintageGold)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isLocalOnlyMode) "本機無雲端" else "雲端同步",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Month Navigator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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

                    // Monthly Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("本月收入", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "+$${String.format(Locale.US, "%,.0f", totalIncome)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ForestGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("本月支出", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "-$${String.format(Locale.US, "%,.0f", totalExpense)}",
                                fontFamily = CourierPrimeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SealingWaxRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("收支結餘", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(
                        text = if (isTravelMode) "此趟旅程尚無記帳記錄" else "本月份尚無手帳記錄",
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "點選右下方「＋」或上方語音按鈕即可快速記帳",
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

                        ParchmentCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            onClick = {
                                triggerHapticFeedback(context, 15)
                                txToEdit = tx
                            }
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
                                    // Circular category icon
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
                                            // Merchant and Note on the SAME ROW (as requested)
                                            if (tx.merchant.isNotBlank() || tx.note.isNotBlank()) {
                                                val details = buildString {
                                                    if (tx.merchant.isNotBlank()) append(tx.merchant)
                                                    if (tx.merchant.isNotBlank() && tx.note.isNotBlank()) append(" · ")
                                                    if (tx.note.isNotBlank()) append(tx.note)
                                                }
                                                Text(
                                                    text = " • $details",
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
                                                text = accName,
                                                fontFamily = CourierPrimeFont,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (tx.tags.isNotBlank()) {
                                                Text(
                                                    text = " • ${tx.tags}",
                                                    fontFamily = CourierPrimeFont,
                                                    fontSize = 11.sp,
                                                    color = VintageGold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (tx.originalAmount > 0 && tx.currency != "NT$") {
                                            Text(
                                                text = "$prefix${tx.currency} ${if (tx.originalAmount % 1.0 == 0.0) String.format(Locale.US, "%,.0f", tx.originalAmount) else String.format(Locale.US, "%,.2f", tx.originalAmount)}",
                                                fontFamily = CourierPrimeFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = amountColor
                                            )
                                            Text(
                                                text = "≈ NT$ ${String.format(Locale.US, "%,.0f", tx.amount)}",
                                                fontFamily = CourierPrimeFont,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "$prefix$${String.format(Locale.US, "%,.0f", tx.amount)}",
                                                fontFamily = CourierPrimeFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.5.sp,
                                                color = amountColor
                                            )
                                        }
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

        // Voice Bookkeeping Sheet
        if (showVoiceSheet) {
            AddTransactionSheet(
                accounts = accounts,
                initialVoiceRecord = parsedVoiceRecord,
                initialCurrency = selectedTravel?.currency ?: (parsedVoiceRecord?.detectedCurrency ?: "NT$"),
                initialTravelId = selectedTravel?.id ?: "",
                onDismiss = {
                    showVoiceSheet = false
                    parsedVoiceRecord = null
                },
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
                    showVoiceSheet = false
                    parsedVoiceRecord = null
                }
            )
        }

        // Edit Transaction Sheet
        if (txToEdit != null) {
            AddTransactionSheet(
                accounts = accounts,
                transactionToEdit = txToEdit,
                initialCurrency = txToEdit?.currency ?: "NT$",
                initialTravelId = txToEdit?.travelId ?: "",
                onDismiss = { txToEdit = null },
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
                    txToEdit = null
                }
            )
        }
    }
}
