package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import com.example.data.model.DefaultCategories
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import com.example.ui.components.RetroCalculatorPad
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.ui.theme.WarmSepiaLight
import com.example.ui.utils.ParsedVoiceRecord
import com.example.ui.utils.VoiceBookkeepingParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<LedgerAccount>,
    transactionToEdit: LedgerTransaction? = null,
    initialCurrency: String = "NT$",
    initialTravelId: String = "",
    initialVoiceRecord: ParsedVoiceRecord? = null,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        id: String?,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        category: String,
        date: String,
        note: String,
        merchant: String,
        tags: String,
        currency: String,
        originalAmount: Double,
        travelId: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    var txType by remember {
        mutableStateOf(
            transactionToEdit?.let { TransactionType.valueOf(it.type) }
                ?: initialVoiceRecord?.type
                ?: TransactionType.EXPENSE
        )
    }

    // Currency selection
    val baseCurrCode = when (initialCurrency) {
        "¥" -> "JPY"
        "$" -> "USD"
        "€" -> "EUR"
        "₩" -> "KRW"
        "NT$" -> "TWD"
        else -> if (initialCurrency.length == 3) initialCurrency else "TWD"
    }
    var selectedCurrencyCode by remember {
        mutableStateOf(
            transactionToEdit?.currency?.let {
                if (it == "NT$") "TWD" else if (it == "¥") "JPY" else it
            } ?: initialVoiceRecord?.detectedCurrency ?: baseCurrCode
        )
    }

    val initialAmountStr = when {
        transactionToEdit != null -> {
            if (transactionToEdit.originalAmount > 0) {
                if (transactionToEdit.originalAmount % 1.0 == 0.0) transactionToEdit.originalAmount.toInt().toString()
                else transactionToEdit.originalAmount.toString()
            } else {
                if (transactionToEdit.amount % 1.0 == 0.0) transactionToEdit.amount.toInt().toString()
                else transactionToEdit.amount.toString()
            }
        }
        initialVoiceRecord?.amount != null -> {
            if (initialVoiceRecord.amount % 1.0 == 0.0) initialVoiceRecord.amount.toInt().toString()
            else initialVoiceRecord.amount.toString()
        }
        else -> "0"
    }

    var amountInput by remember { mutableStateOf(initialAmountStr) }
    var selectedCategory by remember {
        mutableStateOf(
            transactionToEdit?.category
                ?: initialVoiceRecord?.category
                ?: DefaultCategories.expenseCategories.first().name
        )
    }
    var selectedAccountId by remember { mutableStateOf(transactionToEdit?.accountId ?: (accounts.firstOrNull()?.id ?: "")) }
    var selectedToAccountId by remember { mutableStateOf(transactionToEdit?.toAccountId ?: (accounts.getOrNull(1)?.id ?: "")) }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: initialVoiceRecord?.merchantOrNote ?: "") }
    var merchant by remember { mutableStateOf(transactionToEdit?.merchant ?: initialVoiceRecord?.merchantOrNote ?: "") }
    var selectedTags by remember { mutableStateOf(transactionToEdit?.tags?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: setOf("#日常")) }
    var dateStr by remember {
        mutableStateOf(transactionToEdit?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var recognizedSpokenText by remember { mutableStateOf<String?>(null) }

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenMatches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                recognizedSpokenText = spoken
                val parsed = VoiceBookkeepingParser.parse(spoken)
                txType = parsed.type
                parsed.amount?.let { amt ->
                    amountInput = if (amt % 1.0 == 0.0) amt.toInt().toString() else amt.toString()
                }
                parsed.category?.let { cat -> selectedCategory = cat }
                parsed.detectedCurrency?.let { curr -> selectedCurrencyCode = curr }
                if (parsed.merchantOrNote.isNotBlank()) {
                    merchant = parsed.merchantOrNote
                    note = parsed.merchantOrNote
                }
                triggerHapticFeedback(context, 30)
                Toast.makeText(context, "語音識別成功！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val customCategories by com.example.data.preferences.SettingsManager.getCustomCategories(context).collectAsState(initial = emptyList())
    val baseCategories = if (txType == TransactionType.INCOME) DefaultCategories.incomeCategories else DefaultCategories.expenseCategories
    val categoryNames = baseCategories.map { it.name } + customCategories

    var showAddCustomCat by remember { mutableStateOf(false) }

    // Custom Category Dialog with Emoji Picker
    if (showAddCustomCat) {
        var newCatName by remember { mutableStateOf("") }
        var chosenEmoji by remember { mutableStateOf("☕") }
        val presetEmojis = listOf(
            "🍜", "🍣", "☕", "🍰", "🍺", "🚗", "🚇", "✈️",
            "🏨", "🎮", "🛒", "🐾", "💊", "🎁", "👶", "⛺",
            "🎨", "💄", "👗", "🍿", "🪴", "🏋️", "⚽", "📚"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomCat = false },
            title = {
                Text(
                    text = "新增自訂分類與 EMOJI",
                    fontFamily = PlayfairFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. 選擇或輸入圖示 (Emoji):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, WarmSepia, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = chosenEmoji, fontSize = 24.sp)
                        }
                        OutlinedTextField(
                            value = chosenEmoji,
                            onValueChange = { if (it.isNotBlank()) chosenEmoji = it.take(2) },
                            label = { Text("自訂 Emoji") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Preset emoji grid
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetEmojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chosenEmoji == emoji) WarmSepia.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (chosenEmoji == emoji) 1.5.dp else 0.5.dp,
                                        color = if (chosenEmoji == emoji) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { chosenEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    Text("2. 分類名稱:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("名稱 (例如：下午茶、居酒屋)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val finalCatString = "$chosenEmoji $newCatName"
                            scope.launch {
                                com.example.data.preferences.SettingsManager.addCustomCategory(context, finalCatString)
                            }
                            selectedCategory = finalCatString
                            showAddCustomCat = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSepia)
                ) {
                    Text("確定建立", color = ParchmentSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomCat = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("確定") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Action icons (Speech & Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (initialTravelId.isNotBlank()) "旅程記帳 ✈️" else "手帳記帳",
                        fontSize = 18.sp,
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Type switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入").forEach { (type, label) ->
                            val isSel = txType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) (if (type == TransactionType.EXPENSE) com.example.ui.theme.SealingWaxRed else ForestGreen) else Color.Transparent)
                                    .clickable { txType = type }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) ParchmentSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice recognition trigger button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出記帳內容，例如：「午餐拉麵250元」或「計程車180」")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "系統無可用的語音辨識服務", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(WarmSepia.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "語音記帳",
                            tint = WarmSepia,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Recognized voice feedback banner
            if (recognizedSpokenText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ForestGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "語音識別：『$recognizedSpokenText』",
                        fontSize = 11.5.sp,
                        color = ForestGreen,
                        maxLines = 1
                    )
                }
            }

            // Currency Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyManager.supportedCurrencies.forEach { curr ->
                    val isSelected = selectedCurrencyCode == curr.code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) WarmSepia.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.2.dp else 0.5.dp,
                                color = if (isSelected) WarmSepia else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedCurrencyCode = curr.code
                                triggerHapticFeedback(context, 10)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${curr.flagEmoji} ${curr.symbol} ${curr.code}",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) WarmSepia else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amount Display with Currency Conversion
            val currDef = CurrencyManager.getCurrency(selectedCurrencyCode)
            val enteredAmt = amountInput.toDoubleOrNull() ?: 0.0
            val convertedTwdAmt = if (selectedCurrencyCode == "TWD") enteredAmt else CurrencyManager.convertToTwd(enteredAmt, selectedCurrencyCode)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${currDef.symbol} $amountInput",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CourierPrimeFont,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (selectedCurrencyCode != "TWD") {
                        val rate = CurrencyManager.getRateToTwd(selectedCurrencyCode)
                        Text(
                            text = "≈ NT$ ${String.format(Locale.US, "%,.0f", convertedTwdAmt)}",
                            fontSize = 15.sp,
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            color = VintageGold
                        )
                    }
                }

                if (selectedCurrencyCode != "TWD") {
                    val rate = CurrencyManager.getRateToTwd(selectedCurrencyCode)
                    Text(
                        text = "1 ${currDef.code} = NT$ ${String.format(Locale.US, "%.4f", rate)} • ${CurrencyManager.lastUpdatedTime}",
                        fontSize = 10.5.sp,
                        fontFamily = CourierPrimeFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Date Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showDatePicker = true }
                    .padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = WarmSepia, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("日期: $dateStr", fontSize = 12.5.sp, fontFamily = CourierPrimeFont, color = MaterialTheme.colorScheme.onSurface)
            }

            // Category Header with Add Custom Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("分類選擇", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddCustomCat = true }) {
                    Text("+ 自訂分類/Emoji", fontSize = 12.sp, color = WarmSepia, fontWeight = FontWeight.Bold)
                }
            }

            val resolveEmoji: (String) -> String = { name ->
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
                        "正職薪資" -> "💼"
                        "獎金紅利" -> "🧧"
                        "副業兼職" -> "💻"
                        "投資收益" -> "📈"
                        "中獎發票" -> "🎫"
                        else -> "📌"
                    }
                }
            }

            // Category Chips Row (Emoji only as per design requirement)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categoryNames.forEach { catName ->
                    val emoji = resolveEmoji(catName)
                    val isSelected = selectedCategory == catName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catName },
                        label = { Text(emoji, fontSize = 20.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmSepiaLight,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Merchant and Note on the SAME ROW (as requested)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商家") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Minimalist Calculator Pad
            RetroCalculatorPad(
                currentInput = amountInput,
                onValueChange = { amountInput = it },
                onDone = { /* Done input */ }
            )

            // Save Button
            Button(
                onClick = {
                    val original = amountInput.toDoubleOrNull() ?: 0.0
                    val finalTwd = if (selectedCurrencyCode == "TWD") original else CurrencyManager.convertToTwd(original, selectedCurrencyCode)
                    onSaveTransaction(
                        transactionToEdit?.id,
                        txType,
                        finalTwd,
                        selectedAccountId,
                        selectedToAccountId,
                        selectedCategory,
                        dateStr,
                        note,
                        merchant,
                        selectedTags.joinToString(","),
                        currDef.symbol,
                        original,
                        initialTravelId
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmSepia),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("儲存紀錄", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ParchmentSurface)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
