package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalAction
import com.example.data.model.TradeSignal
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun SignalCard(
    signal: TradeSignal,
    onExecuteInPaperTrading: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLong = signal.isLong
    val isShort = signal.isShort
    val isTradeable = signal.action.isTradeable

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isLong) BullishGreen.copy(alpha = 0.5f) else if (isShort) BearishRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .testTag("signal_card_${signal.symbol}")
    ) {
        // Header: Direction, Status, Confidence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isLong) BullishGreen.copy(alpha = 0.2f) else if (isShort) BearishRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLong) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = BullishGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (isShort) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = BearishRed)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = signal.action.titleFa,
                            color = if (isLong) BullishGreen else if (isShort) BearishRed else CryptoPrimaryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = signal.marketType.titleFa,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Confidence score & R:R
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "امتیاز اطمینان تحلیلی: ${signal.confidenceScore}٪",
                    color = CryptoPrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "ریسک به ریوارد: ${signal.riskRewardRatio}",
                    color = CryptoSecondaryCyan,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (signal.action == SignalAction.DATA_UNAVAILABLE) {
            Surface(
                color = CryptoPrimaryGold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "داده معتبر بازار در دسترس نیست؛ تا دریافت حداقل ۳۰ کندل معتبر، قیمت ورود، حد ضرر و اهداف محاسبه یا نمایش داده نمی‌شوند.",
                    color = CryptoPrimaryGold,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
        // THE CORE TRADE LEVELS GRID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LevelColumn(
                title = "محدوده ورود (Zone)",
                value = "${formatPrice(signal.entryZoneMin)} - ${formatPrice(signal.entryZoneMax)}",
                color = CryptoSecondaryCyan
            )
            LevelColumn(
                title = "نقطه ورود مرجع",
                value = "$${formatPrice(signal.entryPrice)}",
                color = Color(0xFF0EA5E9)
            )
            LevelColumn(
                title = "حد ضرر (SL)",
                value = "$${formatPrice(signal.stopLoss)}",
                color = BearishRed
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Take Profit 1, 2, 3 Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LevelColumn(
                title = "هدف اول (TP1 - ۴۰٪)",
                value = "$${formatPrice(signal.takeProfit1)} (+${String.format(Locale.US, "%.1f", signal.tp1DistancePercent)}%)",
                color = BullishGreen
            )
            LevelColumn(
                title = "هدف دوم (TP2 - ۳۰٪)",
                value = "$${formatPrice(signal.takeProfit2)} (+${String.format(Locale.US, "%.1f", signal.tp2DistancePercent)}%)",
                color = BullishGreen
            )
            LevelColumn(
                title = "هدف سوم (TP3 - ۳۰٪)",
                value = "$${formatPrice(signal.takeProfit3)} (+${String.format(Locale.US, "%.1f", signal.tp3DistancePercent)}%)",
                color = Color(0xFF10B981)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Capital & Risk summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "حجم خرید پیشنهادی: $${formatPrice(signal.suggestedPositionSizeUsd)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = "حداکثر زیان در SL: $${formatPrice(signal.maxRiskAmountUsd)}",
                color = BearishRed.copy(alpha = 0.9f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Three AI summary notes
        Text(
            text = "تحلیل بنیادی: ${signal.fundamentalVerdict}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "تحلیل تکنیکال: ${signal.technicalVerdict}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "موتور کمی و ریسک: ${signal.quantVerdict}",
            color = CryptoSecondaryCyan,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        if (signal.invalidationCondition.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "شرط ابطال سناریو: ${signal.invalidationCondition}",
                color = CryptoPrimaryGold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        }

        // 1-Click Action to Paper Trading
        if (isTradeable && signal.action != SignalAction.DATA_UNAVAILABLE && signal.entryPrice > 0) {
            Button(
                onClick = onExecuteInPaperTrading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_paper_trade_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLong) BullishGreen else BearishRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "اجرای فوری در پیپرتریدینگ (Paper Trade)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LevelColumn(
    title: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1000) {
        String.format(Locale.US, "%,.1f", price)
    } else if (price >= 1) {
        String.format(Locale.US, "%.2f", price)
    } else {
        String.format(Locale.US, "%.4f", price)
    }
}
