package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StampType
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentBorder
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia

fun triggerHapticFeedback(context: Context, durationMs: Long = 20) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    } catch (_: Exception) {}
}

@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
fun WaxSealBadge(
    stamp: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val stampEnum = try {
        StampType.valueOf(stamp)
    } catch (_: Exception) {
        StampType.NONE
    }
    if (stampEnum == StampType.NONE) return

    val sealColor = when (stampEnum) {
        StampType.PAID -> SealingWaxRed
        StampType.APPROVED -> ForestGreen
        StampType.RECEIPT -> WarmSepia
        StampType.VIP -> VintageGold
        else -> WarmSepia
    }

    Box(
        modifier = modifier
            .size(size)
            .rotate(-8f)
            .border(1.5.dp, sealColor.copy(alpha = 0.85f), CircleShape)
            .padding(2.dp)
            .border(0.8.dp, sealColor.copy(alpha = 0.5f), CircleShape)
            .background(sealColor.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stampEnum.label,
            color = sealColor,
            fontFamily = CourierPrimeFont,
            fontWeight = FontWeight.Bold,
            fontSize = if (size > 40.dp) 11.sp else 8.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PerforatedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 1f),
            end = androidx.compose.ui.geometry.Offset(size.width, 1f),
            strokeWidth = 2f,
            pathEffect = pathEffect
        )
    }
}

fun resolveCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "directions_subway" -> Icons.Default.DirectionsSubway
        "movie" -> Icons.Default.Movie
        "home" -> Icons.Default.Home
        "medical_services" -> Icons.Default.MedicalServices
        "menu_book" -> Icons.Default.MenuBook
        "card_giftcard" -> Icons.Default.CardGiftcard
        "work" -> Icons.Default.Work
        "savings" -> Icons.Default.Savings
        "laptop_mac" -> Icons.Default.LaptopMac
        "trending_up" -> Icons.Default.TrendingUp
        "military_tech" -> Icons.Default.MilitaryTech
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "payments" -> Icons.Default.Payments
        "attach_money" -> Icons.Default.AttachMoney
        else -> Icons.Default.MoreHoriz
    }
}

@Composable
fun RetroCalculatorPad(
    modifier: Modifier = Modifier,
    currentInput: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    fun handlePress(key: String) {
        triggerHapticFeedback(context, 15)
        when (key) {
            "AC" -> onValueChange("0")
            "⌫" -> {
                if (currentInput.length <= 1) onValueChange("0")
                else onValueChange(currentInput.dropLast(1))
            }
            "+" -> {
                if (!currentInput.endsWith("+") && !currentInput.endsWith("-")) {
                    onValueChange("$currentInput+")
                }
            }
            "-" -> {
                if (!currentInput.endsWith("+") && !currentInput.endsWith("-")) {
                    onValueChange("$currentInput-")
                }
            }
            "=" -> {
                try {
                    val evaluated = evaluateSimpleExpression(currentInput)
                    onValueChange(evaluated)
                } catch (_: Exception) {}
            }
            "." -> {
                val lastSegment = currentInput.split('+', '-').lastOrNull() ?: ""
                if (!lastSegment.contains('.')) {
                    onValueChange(currentInput + ".")
                }
            }
            else -> { // Digits 0-9
                if (currentInput == "0") {
                    onValueChange(key)
                } else {
                    onValueChange(currentInput + key)
                }
            }
        }
    }

    val rows = listOf(
        listOf("7", "8", "9", "⌫"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("AC", "0", ".", "=")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key in listOf("AC", "⌫", "+", "-", "=")
                    val isEquals = key == "="

                    // Minimalist MUJI outline: unified, simple, clean neutral border
                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

                    val btnBg = when {
                        isEquals -> WarmSepia
                        isAction -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val textColor = when {
                        isEquals -> ParchmentSurface
                        isAction -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("calc_key_$key")
                            .clip(RoundedCornerShape(8.dp))
                            .background(btnBg)
                            .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
                            .clickable {
                                handlePress(key)
                                if (key == "=") onDone()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = textColor,
                            fontFamily = if (isAction && !isEquals) PlayfairFont else CourierPrimeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }
}

private fun evaluateSimpleExpression(expr: String): String {
    val clean = expr.trimEnd('+', '-')
    if (!clean.contains('+') && !clean.contains('-')) return clean

    var total = 0.0
    var currentOp = '+'
    var currentToken = ""

    for (c in clean) {
        if (c == '+' || c == '-') {
            val num = currentToken.toDoubleOrNull() ?: 0.0
            total = if (currentOp == '+') total + num else total - num
            currentOp = c
            currentToken = ""
        } else {
            currentToken += c
        }
    }
    if (currentToken.isNotEmpty()) {
        val num = currentToken.toDoubleOrNull() ?: 0.0
        total = if (currentOp == '+') total + num else total - num
    }

    return if (total % 1.0 == 0.0) total.toLong().toString() else String.format(java.util.Locale.US, "%.2f", total)
}
