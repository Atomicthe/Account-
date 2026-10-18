package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia
import com.example.ui.viewmodel.LedgerViewModel
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val currentMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()

    var analysisType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val relevantTxs = transactions.filter { it.type == analysisType.name }
    val totalAmount = relevantTxs.sumOf { it.amount }

    // Category breakdown
    val categoryTotals = relevantTxs
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Daily spending for trend chart
    val dailyTotals = relevantTxs
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedBy { it.first }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Switcher: Expense Analysis vs Income Analysis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                val isSelected = analysisType == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) (if (t == TransactionType.EXPENSE) SealingWaxRed else ForestGreen) else Color.Transparent)
                        .clickable { analysisType = t }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${t.label}統計分析",
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) ParchmentSurface else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Summary Card with Donut Chart
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
                            text = "${currentMonth}月 總計",
                            fontFamily = PlayfairFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "NT$ ${String.format(Locale.US, "%,.0f", totalAmount)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = if (analysisType == TransactionType.EXPENSE) SealingWaxRed else ForestGreen
                        )
                    }

                    // Average daily
                    val dayCount = 30
                    val dailyAvg = if (totalAmount > 0) totalAmount / dayCount else 0.0
                    Column(horizontalAlignment = Alignment.End) {
                        Text("日均支出", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "NT$ ${String.format(Locale.US, "%,.0f", dailyAvg)}",
                            fontFamily = CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = WarmSepia
                        )
                    }
                }

                PerforatedDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Custom Donut Chart
                if (totalAmount > 0 && categoryTotals.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(150.dp)) {
                                var startAngle = -90f
                                categoryTotals.forEachIndexed { idx, (_, amt) ->
                                    val sweep = (amt / totalAmount).toFloat() * 360f
                                    val color = vintageColors[idx % vintageColors.size]
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 30f, cap = StrokeCap.Butt),
                                        size = Size(size.width, size.height)
                                    )
                                    startAngle += sweep
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${categoryTotals.size} 個科目",
                                    fontFamily = PlayfairFont,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "100%",
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = WarmSepia
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("本月無足夠數據繪製圓形圖", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        // Daily Trend Bar Chart Card
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
                    text = "每日走勢圖 • DAILY TREND",
                    fontFamily = CourierPrimeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = VintageGold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (dailyTotals.isNotEmpty()) {
                    val maxVal = dailyTotals.maxOfOrNull { it.second } ?: 1.0
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val barWidth = (size.width / (dailyTotals.size.coerceAtLeast(1) * 2f)).coerceIn(8f, 24f)
                        val step = size.width / dailyTotals.size.coerceAtLeast(1)

                        // Grid baseline
                        drawLine(
                            color = Color(0xFFDAC7AC),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2f
                        )

                        dailyTotals.forEachIndexed { i, (_, amt) ->
                            val barHeight = ((amt / maxVal) * (size.height - 20f)).toFloat()
                            val x = i * step + (step - barWidth) / 2f
                            val y = size.height - barHeight

                            drawRect(
                                color = WarmSepia,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暫無每日記錄", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        // Category Rankings & Percentage Bars
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
                    text = "各科目佔比明細 • CATEGORY RANKING",
                    fontFamily = CourierPrimeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = VintageGold
                )

                if (categoryTotals.isEmpty()) {
                    Text(
                        text = "無科目收支記錄",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    categoryTotals.forEachIndexed { index, (cat, amt) ->
                        val pct = if (totalAmount > 0) (amt / totalAmount).toFloat() else 0f
                        val color = vintageColors[index % vintageColors.size]

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${index + 1}. $cat",
                                        fontFamily = PlayfairFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "NT$ ${String.format(Locale.US, "%,.0f", amt)} (${String.format(Locale.US, "%.1f", pct * 100)}%)",
                                    fontFamily = CourierPrimeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = WarmSepia
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(70.dp))
    }
}
