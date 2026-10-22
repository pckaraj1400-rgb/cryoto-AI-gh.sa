package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BacktestSummary
import com.example.data.model.Candle
import com.example.data.model.MarketType
import com.example.domain.backtest.BacktestEngine
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BacktestScreen(
    symbol: String,
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    val strategies = listOf(
        "ترکیب پولبک به EMA50 و واگرایی RSI / مومنتوم",
        "شکست پرقدرت مقاومت و تایید حجم (Breakout)",
        "تقاطع میانگین‌های متحرک EMA20/EMA50",
        "استراتژی فیوچرز دوطرفه پرایس اکشن (BOS & OB)"
    )
    var selectedStrategy by remember { mutableStateOf(strategies.first()) }
    var selectedMarketType by remember { mutableStateOf(MarketType.SPOT) }
    var selectedTf by remember { mutableStateOf("1h") }

    var backtestResult by remember(symbol, selectedStrategy, selectedMarketType, candles) {
        mutableStateOf<BacktestSummary?>(
            BacktestEngine.runBacktest(
                symbol = symbol,
                timeframe = selectedTf,
                strategyName = selectedStrategy,
                candles = candles,
                marketType = selectedMarketType
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Strategy Setup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueryStats, contentDescription = null, tint = CryptoPrimaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "موتور ارزیابی تاریخی و بکتست (Backtest)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ارزیابی استراتژی بر اساس داده‌های تاریخی واقعی بدون سوگیری نگاه به آینده (No Look-Ahead) همراه با تفکیک آزمون خارج از نمونه (Out-of-Sample).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Market Type Selector
                    Text("نوع بازار بکتست:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMarketType == MarketType.SPOT,
                            onClick = { selectedMarketType = MarketType.SPOT },
                            label = { Text("اسپات (فقط لانگ)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CryptoSecondaryCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CryptoSecondaryCyan
                            )
                        )
                        FilterChip(
                            selected = selectedMarketType == MarketType.FUTURES_USDT_M,
                            onClick = { selectedMarketType = MarketType.FUTURES_USDT_M },
                            label = { Text("فیوچرز (دوطرفه)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CryptoPrimaryGold.copy(alpha = 0.2f),
                                selectedLabelColor = CryptoPrimaryGold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("انتخاب استراتژی:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    strategies.forEach { st ->
                        val isSel = st == selectedStrategy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (isSel) CryptoPrimaryGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { selectedStrategy = st }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = st,
                                color = if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            backtestResult = BacktestEngine.runBacktest(
                                symbol = symbol,
                                timeframe = selectedTf,
                                strategyName = selectedStrategy,
                                candles = candles,
                                marketType = selectedMarketType
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_backtest_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CryptoPrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اجرای محاسبات بکتست روی $symbol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Summary Performance Metrics Card
        val result = backtestResult
        if (result != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "شاخص‌های عملکرد آماری استراتژی:",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = CryptoPrimaryGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = result.periodDurationFa,
                                    color = CryptoPrimaryGold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(
                                label = "نرخ برد (Win Rate)",
                                value = "${String.format(Locale.US, "%.1f", result.winRatePercent)}٪",
                                color = if (result.winRatePercent >= 50) BullishGreen else BearishRed
                            )
                            MetricBox(
                                label = "فاکتور سود (PF)",
                                value = String.format(Locale.US, "%.2f", result.profitFactor),
                                color = CryptoPrimaryGold
                            )
                            MetricBox(
                                label = "سود خالص ($)",
                                value = "${if (result.netProfitUsd >= 0) "+" else ""}$${formatPrice(result.netProfitUsd)}",
                                color = if (result.netProfitUsd >= 0) BullishGreen else BearishRed
                            )
                            MetricBox(
                                label = "حداکثر افت (Max DD)",
                                value = "${String.format(Locale.US, "%.1f", result.maxDrawdownPercent)}٪",
                                color = BearishRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(
                                label = "امید ریاضی (Expectancy)",
                                value = "${if (result.expectancyUsd >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", result.expectancyUsd)}",
                                color = if (result.expectancyUsd >= 0) BullishGreen else BearishRed
                            )
                            MetricBox(
                                label = "میانگین معامله برنده",
                                value = "+$${formatPrice(result.avgWinUsd)}",
                                color = BullishGreen
                            )
                            MetricBox(
                                label = "میانگین معامله بازنده",
                                value = "-$${formatPrice(result.avgLossUsd)}",
                                color = BearishRed
                            )
                            MetricBox(
                                label = "بازده درصدی",
                                value = String.format(Locale.US, "%+.1f%%", result.netProfitPercent),
                                color = if (result.netProfitPercent >= 0) BullishGreen else BearishRed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // In-Sample vs Out-of-Sample Split Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Science, contentDescription = null, tint = CryptoSecondaryCyan, modifier = Modifier.padding(end = 4.dp))
                                    Text(
                                        text = "اعتبارسنجی دو مرحله‌ای (Train / Forward Validation):",
                                        color = CryptoSecondaryCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "توسعه (In-Sample 70%): برد ${String.format(Locale.US, "%.1f", result.inSampleWinRate)}% | بازده ${String.format(Locale.US, "%+.1f%%", result.inSampleNetProfitPercent)}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "آزمون (Out-of-Sample 30%): برد ${String.format(Locale.US, "%.1f", result.outOfSampleWinRate)}% | بازده ${String.format(Locale.US, "%+.1f%%", result.outOfSampleNetProfitPercent)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تعداد کل معاملات: ${result.totalTrades}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("برنده: ${result.winningTrades}  |  بازنده: ${result.losingTrades}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Executed Trades List
            item {
                Text(
                    text = "تاریخچه معاملات شبیه‌سازی‌شده (${result.trades.size})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(result.trades.reversed()) { trade ->
                val sdf = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
                val timeStr = sdf.format(Date(trade.entryTime))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (trade.isWin) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${trade.type} • ${trade.exitReason}",
                                    color = if (trade.isWin) BullishGreen else BearishRed,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            if (trade.isOutOfSample) {
                                Surface(
                                    color = CryptoSecondaryCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Out-of-Sample",
                                        color = CryptoSecondaryCyan,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(text = timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ورود: $${formatPrice(trade.entryPrice)} ← خروج: $${formatPrice(trade.exitPrice)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (trade.profitUsd >= 0) "+" else ""}$${formatPrice(trade.profitUsd)}",
                            color = if (trade.isWin) BullishGreen else BearishRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%+.1f%%", trade.profitPercent),
                            color = if (trade.isWin) BullishGreen else BearishRed,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
