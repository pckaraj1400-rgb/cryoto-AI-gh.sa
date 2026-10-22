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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.MarketType
import com.example.data.model.TechnicalIndicators
import com.example.data.model.TradeSignal
import com.example.ui.components.CandleChart
import com.example.ui.components.OrderPlacementDialog
import com.example.ui.components.SignalCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun ChartAndTradeScreen(
    coin: Coin,
    candles: List<Candle>,
    indicators: TechnicalIndicators?,
    signal: TradeSignal?,
    marketType: MarketType,
    onMarketTypeChanged: (MarketType) -> Unit,
    selectedTimeframe: String,
    onTimeframeChanged: (String) -> Unit,
    availableBalanceUsd: Double,
    onPlaceOrder: (
        marketType: MarketType,
        direction: String,
        amount: Double,
        leverage: Int,
        stopLoss: Double?,
        tp1: Double?,
        tp2: Double?,
        tp3: Double?
    ) -> Unit,
    onExecutePaperSignal: (TradeSignal) -> Unit,
    modifier: Modifier = Modifier,
    dataSourceName: String = "Binance.US",
    dataTimestamp: String = "",
    hasPaperPosition: Boolean = false
) {
    var showOrderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Symbol & Market Type Switcher (Spot vs Futures USDT-M)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${coin.symbol}/USDT",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• ${coin.nameFa}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Text(
                        text = "$${formatPrice(coin.priceUsd)}  (${String.format(Locale.US, "%+.2f%%", coin.change24h)})",
                        color = if (coin.change24h >= 0) BullishGreen else BearishRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Market Type Buttons
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (marketType == MarketType.SPOT) CryptoPrimaryGold else Color.Transparent,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable { onMarketTypeChanged(MarketType.SPOT) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "اسپات (Spot)",
                            color = if (marketType == MarketType.SPOT) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (marketType == MarketType.FUTURES_USDT_M) Color(0xFF8B5CF6) else Color.Transparent,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable { onMarketTypeChanged(MarketType.FUTURES_USDT_M) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "فیوچرز (Futures)",
                            color = if (marketType == MarketType.FUTURES_USDT_M) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Interactive Candlestick Chart with Entry, SL, TP1, TP2, TP3 Overlay
        item {
            CandleChart(
                symbol = coin.symbol,
                candles = candles,
                currentPrice = coin.priceUsd,
                signal = signal,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeChanged,
                dataSourceName = dataSourceName,
                dataTimestamp = dataTimestamp,
                indicators = indicators,
                hasPaperPosition = hasPaperPosition
            )
        }

        // 3. Trade Action Bar (Quick Buy Long / Sell Short)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showOrderDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_order_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (marketType == MarketType.SPOT) "خرید اسپات (BUY)" else "معامله لانگ (LONG)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                if (marketType == MarketType.FUTURES_USDT_M) {
                    Button(
                        onClick = { showOrderDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "معامله شورت (SHORT)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { showOrderDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فروش دارایی (SELL)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 4. Synchronized AI Trade Signal with Entry, SL, TP1, TP2, TP3
        if (signal != null) {
            item {
                Text(
                    text = "سیگنال هوش مصنوعی و سطوح دقیق معاملاتی",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                SignalCard(
                    signal = signal,
                    onExecuteInPaperTrading = { onExecutePaperSignal(signal) }
                )
            }
        }

        // 5. Technical Indicators Readout Card
        if (indicators != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تحلیل اندیکاتورها و ساختار مارکت",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "تایم‌فریم $selectedTimeframe",
                                    color = CryptoSecondaryCyan,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid of indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IndicatorItem(
                                name = "شاخص RSI(14)",
                                value = String.format(Locale.US, "%.1f", indicators.rsi),
                                status = when {
                                    indicators.rsi > 70 -> "اشباع خرید (هشدار)"
                                    indicators.rsi < 30 -> "اشباع فروش (کف)"
                                    indicators.rsi >= 50 -> "مومنتوم صعودی"
                                    else -> "مومنتوم نزولی"
                                },
                                color = if (indicators.rsi in 40.0..60.0) CryptoPrimaryGold else if (indicators.rsi > 60) BullishGreen else BearishRed
                            )
                            IndicatorItem(
                                name = "میانگین EMA 20/50",
                                value = "${formatPrice(indicators.ema20)} / ${formatPrice(indicators.ema50)}",
                                status = if (indicators.ema20 >= indicators.ema50) "صعودی (Golden)" else "نزولی (Bear)",
                                color = if (indicators.ema20 >= indicators.ema50) BullishGreen else BearishRed
                            )
                            IndicatorItem(
                                name = "مکدی MACD(12,26)",
                                value = String.format(Locale.US, "%.2f", indicators.macdHistogram),
                                status = indicators.macdStatusFa,
                                color = if (indicators.macdHistogram >= 0) BullishGreen else BearishRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IndicatorItem(
                                name = "حمایت کلیدی (Support)",
                                value = "$${formatPrice(indicators.support)}",
                                status = "کف معتبر محلی",
                                color = BullishGreen
                            )
                            IndicatorItem(
                                name = "مقاومت کلیدی (Resistance)",
                                value = "$${formatPrice(indicators.resistance)}",
                                status = "سقف عرضه محلی",
                                color = BearishRed
                            )
                            IndicatorItem(
                                name = "دامنه نوسان ATR",
                                value = "$${formatPrice(indicators.atr)}",
                                status = "نوسان نرمال",
                                color = CryptoSecondaryCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Market structure & Breakout trap status
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "ساختار بازار: ${indicators.marketStructureFa}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "وضعیت شکست سطوح: ${indicators.breakoutQualityFa}",
                                    color = CryptoPrimaryGold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Order Placement Modal
    if (showOrderDialog) {
        OrderPlacementDialog(
            symbol = coin.symbol,
            currentPrice = coin.priceUsd,
            availableBalanceUsd = availableBalanceUsd,
            defaultMarketType = marketType,
            defaultStopLoss = signal?.stopLoss,
            defaultTp1 = signal?.takeProfit1,
            defaultTp2 = signal?.takeProfit2,
            defaultTp3 = signal?.takeProfit3,
            onDismiss = { showOrderDialog = false },
            onConfirmOrder = { mType, dir, amt, lev, sl, tp1, tp2, tp3 ->
                onPlaceOrder(mType, dir, amt, lev, sl, tp1, tp2, tp3)
            }
        )
    }
}

@Composable
private fun IndicatorItem(
    name: String,
    value: String,
    status: String,
    color: Color
) {
    Column {
        Text(text = name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text = status, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
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
