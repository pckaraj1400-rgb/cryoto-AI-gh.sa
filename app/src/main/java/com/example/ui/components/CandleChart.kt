package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.SignalAction
import com.example.data.model.TradeSignal
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoBgDark
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoCard
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import com.example.ui.theme.CurrentPriceColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandleChart(
    symbol: String,
    candles: List<Candle>,
    currentPrice: Double,
    signal: TradeSignal?,
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    dataSourceName: String = "Binance.US",
    dataTimestamp: String = "",
    indicators: com.example.data.model.TechnicalIndicators? = null,
    hasPaperPosition: Boolean = false
) {
    val timeframes = listOf("1m", "5m", "15m", "1h", "4h", "1d", "1w")

    // Chart viewport controls
    var visibleCandleCount by remember { mutableIntStateOf(50) } // Default 50 visible
    var scrollOffset by remember { mutableIntStateOf(0) } // 0 is rightmost (latest)
    var showAiLevels by remember { mutableStateOf(true) }
    var showSrLevels by remember { mutableStateOf(true) }
    var showEma by remember { mutableStateOf(true) }
    var showVolume by remember { mutableStateOf(true) }
    var selectedCandle by remember { mutableStateOf<Candle?>(null) }
    var touchX by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // 0. Data Source & Mode Distinction Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "منبع: $dataSourceName ${if (dataTimestamp.isNotBlank()) "• $dataTimestamp" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(
                color = if (hasPaperPosition) BullishGreen.copy(alpha = 0.15f) else CryptoSecondaryCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (hasPaperPosition) "معامله آزمایشی باز (Paper Trade)" else "سیگنال تحلیلی پیشنهادی (بدون سفارش واقعی)",
                    color = if (hasPaperPosition) BullishGreen else CryptoSecondaryCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 1. Timeframe selector & Indicator toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                timeframes.forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .pointerInput(tf) {
                                detectTapGestures { onTimeframeSelected(tf) }
                            }
                    ) {
                        Text(
                            text = tf,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Quick toggles: Zoom In, Zoom Out, Reset, AI levels
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { visibleCandleCount = max(20, visibleCandleCount - 10) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "بزرگ‌نمایی", tint = TextSecondary)
                }
                IconButton(
                    onClick = { visibleCandleCount = min(candles.size, visibleCandleCount + 10) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "کوچک‌نمایی", tint = TextSecondary)
                }
                IconButton(
                    onClick = { scrollOffset = 0; selectedCandle = null; touchX = -1f },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "کندل جاری", tint = CryptoSecondaryCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Legend / Status Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Toggle AI Levels Button
                FilterChip(
                    selected = showAiLevels,
                    onClick = { showAiLevels = !showAiLevels },
                    label = { Text("سطوح Entry/SL/TP", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CryptoSecondaryCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CryptoSecondaryCyan
                    ),
                    modifier = Modifier.height(28.dp)
                )

                FilterChip(
                    selected = showSrLevels,
                    onClick = { showSrLevels = !showSrLevels },
                    label = { Text("حمایت/مقاومت", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp)
                )

                FilterChip(
                    selected = showEma,
                    onClick = { showEma = !showEma },
                    label = { Text("EMA", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }

            // Status Indicator pill
            if (signal != null && showAiLevels) {
                Surface(
                    color = when (signal.status) {
                        com.example.data.model.SignalStatus.ACTIVE -> BullishGreen.copy(alpha = 0.2f)
                        com.example.data.model.SignalStatus.PENDING -> CryptoPrimaryGold.copy(alpha = 0.2f)
                        else -> CryptoCard
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${signal.action.titleFa} • ${signal.status.titleFa}",
                        color = when (signal.action) {
                            SignalAction.LONG -> BullishGreen
                            SignalAction.SHORT -> BearishRed
                            else -> CryptoPrimaryGold
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Candle Details HUD (If hovered or latest candle)
        val candleToDisplay = selectedCandle ?: candles.lastOrNull()
        if (candleToDisplay != null) {
            val isGreen = candleToDisplay.close >= candleToDisplay.open
            val change = ((candleToDisplay.close - candleToDisplay.open) / candleToDisplay.open) * 100.0
            val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
            val timeStr = sdf.format(Date(candleToDisplay.time))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(text = "O: ${formatPrice(candleToDisplay.open)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(text = "H: ${formatPrice(candleToDisplay.high)}", color = BullishGreen, fontSize = 10.sp)
                Text(text = "L: ${formatPrice(candleToDisplay.low)}", color = BearishRed, fontSize = 10.sp)
                Text(
                    text = "C: ${formatPrice(candleToDisplay.close)} (${String.format(Locale.US, "%+.2f%%", change)})",
                    color = if (isGreen) BullishGreen else BearishRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. MAIN CANVAS CHART
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(Color(0xFF0D111C), RoundedCornerShape(8.dp))
                .pointerInput(candles.size) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchX = change.position.x
                            // Dragging horizontally pans the historical view
                            val candleWidthPx = 15f
                            val deltaCandles = (dragAmount.x / candleWidthPx).toInt()
                            if (deltaCandles != 0) {
                                val maxScroll = max(0, candles.size - visibleCandleCount)
                                scrollOffset = (scrollOffset + deltaCandles).coerceIn(0, maxScroll)
                            }
                        },
                        onDragEnd = {
                            // Keep selection or reset
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            touchX = offset.x
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (candles.isEmpty()) return@Canvas

                val totalCandles = candles.size
                val endIndex = (totalCandles - scrollOffset).coerceIn(visibleCandleCount, totalCandles)
                val startIndex = (endIndex - visibleCandleCount).coerceIn(0, totalCandles - 1)
                val viewCandles = candles.subList(startIndex, endIndex)

                if (viewCandles.isEmpty()) return@Canvas

                val chartWidth = size.width - 65.dp.toPx() // Reserve 65dp on right for price axis
                val volumeAreaHeight = 45.dp.toPx()
                val priceAreaHeight = size.height - volumeAreaHeight - 20.dp.toPx()

                // Calculate Min & Max Price (Include Signal Levels if enabled)
                var minPrice = viewCandles.minOf { it.low }
                var maxPrice = viewCandles.maxOf { it.high }

                if (showAiLevels && signal != null && signal.action != SignalAction.DATA_UNAVAILABLE && signal.entryPrice > 0) {
                    minPrice = minOf(minPrice, signal.stopLoss, signal.entryZoneMin)
                    maxPrice = maxOf(maxPrice, signal.takeProfit3, signal.entryZoneMax)
                }

                val pricePadding = (maxPrice - minPrice) * 0.08
                minPrice -= pricePadding
                maxPrice += pricePadding
                val priceRange = max(0.00001, maxPrice - minPrice)

                val maxVolume = max(1.0, viewCandles.maxOf { it.volume })
                val candleWidth = chartWidth / viewCandles.size
                val bodyWidth = max(2f, candleWidth * 0.7f)

                fun priceToY(price: Double): Float {
                    return (priceAreaHeight - ((price - minPrice) / priceRange * priceAreaHeight)).toFloat() + 10.dp.toPx()
                }

                // ----------------------------------------------------
                // DRAW BACKGROUND GRID LINES
                // ----------------------------------------------------
                val gridLines = 5
                for (i in 0..gridLines) {
                    val p = minPrice + (priceRange * (i.toDouble() / gridLines))
                    val y = priceToY(p)
                    drawLine(
                        color = Color(0xFF1E2638),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f
                    )
                    // Draw price label on right axis
                    drawContext.canvas.nativeCanvas.drawText(
                        formatPrice(p),
                        chartWidth + 6.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#9CA3AF")
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }

                // ----------------------------------------------------
                // THE MANDATORY TRADE LEVELS VISUALIZATION (الزام اصلی)
                // Entry Zone, Entry Ref, SL, TP1, TP2, TP3
                // ----------------------------------------------------
                if (showAiLevels && signal != null && signal.action != SignalAction.DATA_UNAVAILABLE && signal.entryPrice > 0) {
                    val entryZoneTopY = priceToY(signal.entryZoneMax)
                    val entryZoneBottomY = priceToY(signal.entryZoneMin)
                    val entryY = priceToY(signal.entryPrice)
                    val slY = priceToY(signal.stopLoss)
                    val tp1Y = priceToY(signal.takeProfit1)
                    val tp2Y = priceToY(signal.takeProfit2)
                    val tp3Y = priceToY(signal.takeProfit3)

                    // 1. Shaded Entry Zone
                    val zoneHeight = abs(entryZoneBottomY - entryZoneTopY)
                    val zoneTop = min(entryZoneTopY, entryZoneBottomY)
                    drawRect(
                        color = CryptoSecondaryCyan.copy(alpha = 0.16f),
                        topLeft = Offset(0f, zoneTop),
                        size = Size(chartWidth, max(2f, zoneHeight))
                    )
                    // Dashed entry zone borders
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = CryptoSecondaryCyan.copy(alpha = 0.6f),
                        start = Offset(0f, zoneTop),
                        end = Offset(chartWidth, zoneTop),
                        strokeWidth = 1.5f,
                        pathEffect = dashedEffect
                    )
                    drawLine(
                        color = CryptoSecondaryCyan.copy(alpha = 0.6f),
                        start = Offset(0f, zoneTop + zoneHeight),
                        end = Offset(chartWidth, zoneTop + zoneHeight),
                        strokeWidth = 1.5f,
                        pathEffect = dashedEffect
                    )

                    // 2. Reference Entry Line (Solid Blue/Cyan)
                    drawLine(
                        color = Color(0xFF0EA5E9),
                        start = Offset(0f, entryY),
                        end = Offset(chartWidth, entryY),
                        strokeWidth = 2.5f
                    )
                    drawTradeLevelBadge(
                        text = "ورود مرجع: ${formatPrice(signal.entryPrice)}",
                        y = entryY,
                        bgColor = Color(0xFF0284C7),
                        chartWidth = chartWidth
                    )

                    // 3. Stop Loss (Red Dashed Line)
                    drawLine(
                        color = BearishRed,
                        start = Offset(0f, slY),
                        end = Offset(chartWidth, slY),
                        strokeWidth = 2.5f,
                        pathEffect = dashedEffect
                    )
                    drawTradeLevelBadge(
                        text = "حد ضرر (SL): ${formatPrice(signal.stopLoss)} (-${String.format(Locale.US, "%.1f", signal.slDistancePercent)}%)",
                        y = slY,
                        bgColor = BearishRed,
                        chartWidth = chartWidth
                    )

                    // 4. Take Profit 1 (Green Dashed Line)
                    drawLine(
                        color = BullishGreen,
                        start = Offset(0f, tp1Y),
                        end = Offset(chartWidth, tp1Y),
                        strokeWidth = 2f,
                        pathEffect = dashedEffect
                    )
                    drawTradeLevelBadge(
                        text = "هدف ۱ (TP1): ${formatPrice(signal.takeProfit1)} (+${String.format(Locale.US, "%.1f", signal.tp1DistancePercent)}%)",
                        y = tp1Y,
                        bgColor = BullishGreen,
                        chartWidth = chartWidth
                    )

                    // 5. Take Profit 2 (Green Dashed Line)
                    drawLine(
                        color = BullishGreen,
                        start = Offset(0f, tp2Y),
                        end = Offset(chartWidth, tp2Y),
                        strokeWidth = 2f,
                        pathEffect = dashedEffect
                    )
                    drawTradeLevelBadge(
                        text = "هدف ۲ (TP2): ${formatPrice(signal.takeProfit2)} (+${String.format(Locale.US, "%.1f", signal.tp2DistancePercent)}%)",
                        y = tp2Y,
                        bgColor = BullishGreen,
                        chartWidth = chartWidth
                    )

                    // 6. Take Profit 3 (Emerald Bold Line)
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(0f, tp3Y),
                        end = Offset(chartWidth, tp3Y),
                        strokeWidth = 2.5f,
                        pathEffect = dashedEffect
                    )
                    drawTradeLevelBadge(
                        text = "هدف ۳ (TP3): ${formatPrice(signal.takeProfit3)} (+${String.format(Locale.US, "%.1f", signal.tp3DistancePercent)}%)",
                        y = tp3Y,
                        bgColor = Color(0xFF059669),
                        chartWidth = chartWidth
                    )
                }

                // ----------------------------------------------------
                // SUPPORT & RESISTANCE (S/R) LEVELS AND PRICE ACTION
                // ----------------------------------------------------
                if (showSrLevels && indicators != null && indicators.resistance > 0 && indicators.support > 0) {
                    val resY = priceToY(indicators.resistance)
                    val supY = priceToY(indicators.support)
                    val srDashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // Resistance Line
                    drawLine(
                        color = BearishRed.copy(alpha = 0.55f),
                        start = Offset(0f, resY),
                        end = Offset(chartWidth, resY),
                        strokeWidth = 1.5f,
                        pathEffect = srDashed
                    )
                    drawTradeLevelBadge(
                        text = "مقاومت: ${formatPrice(indicators.resistance)}",
                        y = resY,
                        bgColor = BearishRed.copy(alpha = 0.75f),
                        chartWidth = chartWidth
                    )

                    // Support Line
                    drawLine(
                        color = BullishGreen.copy(alpha = 0.55f),
                        start = Offset(0f, supY),
                        end = Offset(chartWidth, supY),
                        strokeWidth = 1.5f,
                        pathEffect = srDashed
                    )
                    drawTradeLevelBadge(
                        text = "حمایت: ${formatPrice(indicators.support)}",
                        y = supY,
                        bgColor = BullishGreen.copy(alpha = 0.75f),
                        chartWidth = chartWidth
                    )
                }

                // ----------------------------------------------------
                // DRAW CANDLESTICKS & VOLUME
                // ----------------------------------------------------
                for (i in viewCandles.indices) {
                    val c = viewCandles[i]
                    val x = (i * candleWidth) + (candleWidth / 2f)
                    val isBullish = c.close >= c.open
                    val candleColor = if (isBullish) BullishGreen else BearishRed

                    val openY = priceToY(c.open)
                    val closeY = priceToY(c.close)
                    val highY = priceToY(c.high)
                    val lowY = priceToY(c.low)

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1.8f
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(2f, abs(closeY - openY))
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(x - (bodyWidth / 2f), bodyTop),
                        size = Size(bodyWidth, bodyHeight)
                    )

                    // Volume Bar (bottom area)
                    if (showVolume) {
                        val volHeight = ((c.volume / maxVolume) * volumeAreaHeight).toFloat()
                        val volY = size.height - volHeight
                        drawRect(
                            color = candleColor.copy(alpha = 0.35f),
                            topLeft = Offset(x - (bodyWidth / 2f), volY),
                            size = Size(bodyWidth, volHeight)
                        )
                    }

                    // Check if touched for crosshair
                    if (touchX >= 0f && touchX in (x - candleWidth / 2f)..(x + candleWidth / 2f)) {
                        selectedCandle = c
                        // Draw vertical crosshair line
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }

                // ----------------------------------------------------
                // CURRENT MARKET PRICE LINE (Pulsing Yellow)
                // ----------------------------------------------------
                val curY = priceToY(currentPrice)
                drawLine(
                    color = CurrentPriceColor,
                    start = Offset(0f, curY),
                    end = Offset(chartWidth, curY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
                // Pill on right axis
                drawRect(
                    color = CurrentPriceColor,
                    topLeft = Offset(chartWidth + 2.dp.toPx(), curY - 8.dp.toPx()),
                    size = Size(60.dp.toPx(), 16.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatPrice(currentPrice),
                    chartWidth + 6.dp.toPx(),
                    curY + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 21f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Educational Bottom Legend for Trade Points
        if (showAiLevels && signal != null && signal.action != SignalAction.DATA_UNAVAILABLE && signal.entryPrice > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(label = "محدوده ورود", color = CryptoSecondaryCyan)
                LegendItem(label = "نقطه ورود", color = Color(0xFF0EA5E9))
                LegendItem(label = "حد ضرر (SL)", color = BearishRed)
                LegendItem(label = "اهداف (TP1-3)", color = BullishGreen)
                LegendItem(label = "قیمت لحظه‌ای", color = CurrentPriceColor)
            }
        }

        // 5. Setup Status, Confirmation, and Non-Execution Disclaimer
        if (signal != null && signal.action.isTradeable) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تأیید ورود: ${signal.priceZoneStatusFa}",
                            color = CryptoSecondaryCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ابطال ستاپ: ${signal.invalidationCondition}",
                            color = BearishRed,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "توجه: لمس اهداف روی نمودار به‌تنهایی به معنی اجرای سفارش نیست و هیچ معامله واقعی بدون تأیید صریح شما ثبت نمی‌شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 8.5.sp
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTradeLevelBadge(
    text: String,
    y: Float,
    bgColor: Color,
    chartWidth: Float
) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 21f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val textWidth = textPaint.measureText(text)
    val badgeWidth = textWidth + 16f
    val badgeHeight = 28f
    val badgeX = 8f

    // Background pill
    drawRoundRect(
        color = bgColor.copy(alpha = 0.9f),
        topLeft = Offset(badgeX, y - (badgeHeight / 2f)),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )

    // Text
    drawContext.canvas.nativeCanvas.drawText(
        text,
        badgeX + 8f,
        y + 7f,
        textPaint
    )
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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
