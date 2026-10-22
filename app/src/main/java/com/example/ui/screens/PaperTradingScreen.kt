package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.PaperAccount
import com.example.data.model.PaperPosition
import com.example.data.model.PaperTradeRecord
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaperTradingScreen(
    account: PaperAccount,
    openPositions: List<PaperPosition>,
    tradeHistory: List<PaperTradeRecord>,
    onClosePosition: (String, Double) -> Unit,
    onResetAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Account Metrics Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "حساب معاملات آزمایشی (Paper Trading)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "شبیه‌سازی کامل بدون درگیری سرمایه واقعی با کارمزد و لغزش فرضی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }

                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "ریست حساب", tint = CryptoPrimaryGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "ارزش کل حساب (Equity)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                text = "$${formatPrice(account.totalEquityUsd)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "موجودی آزاد در دسترس", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                text = "$${formatPrice(account.availableBalanceUsd)}",
                                color = CryptoSecondaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("سود تحقق‌یافته", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val pnl = account.realizedPnLUsd
                            Text(
                                text = "${if (pnl >= 0) "+" else ""}$${formatPrice(pnl)}",
                                color = if (pnl >= 0) BullishGreen else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("سود لحظه‌ای باز", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val unPnl = account.unrealizedPnLUsd
                            Text(
                                text = "${if (unPnl >= 0) "+" else ""}$${formatPrice(unPnl)}",
                                color = if (unPnl >= 0) BullishGreen else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("نرخ برد (Win Rate)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", account.winRatePercent)}٪",
                                color = CryptoPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تعداد کل معاملات", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(
                                text = "${account.totalTradesCount}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Open Positions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پوزیشن‌های باز (${openPositions.size})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        if (openPositions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ پوزیشن بازی وجود ندارد. می‌توانید از صفحه تحلیل یا نمودار، سیگنال‌ها را در حساب آزمایشی اجرا کنید.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(openPositions) { pos ->
                val isLong = pos.direction == "LONG" || pos.direction == "SPOT_BUY"
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isLong) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${pos.direction} ${if (pos.leverage > 1) "${pos.leverage}x" else ""}",
                                        color = if (isLong) BullishGreen else BearishRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${pos.symbol}/USDT",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Close Position Button
                            Button(
                                onClick = { onClosePosition(pos.id, pos.currentPrice) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BearishRed.copy(alpha = 0.2f),
                                    contentColor = BearishRed
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("بستن پوزیشن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Details Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("قیمت ورود", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("$${formatPrice(pos.entryPrice)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Column {
                                Text("قیمت لحظه‌ای", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("$${formatPrice(pos.currentPrice)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Column {
                                Text("مارجین درگیر", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("$${formatPrice(pos.marginUsd)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("سود/زیان باز", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                val pnl = pos.unrealizedPnLUsd
                                Text(
                                    text = "${if (pnl >= 0) "+" else ""}$${formatPrice(pnl)} (${String.format(Locale.US, "%+.1f%%", pos.pnlPercent)})",
                                    color = if (pnl >= 0) BullishGreen else BearishRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // SL & TP info row
                        if (pos.stopLoss != null || pos.takeProfit1 != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (pos.stopLoss != null) {
                                    Text("SL: $${formatPrice(pos.stopLoss)}", color = BearishRed, fontSize = 10.sp)
                                }
                                if (pos.takeProfit1 != null) {
                                    Text("TP1: $${formatPrice(pos.takeProfit1)}", color = BullishGreen, fontSize = 10.sp)
                                }
                                if (pos.takeProfit2 != null) {
                                    Text("TP2: $${formatPrice(pos.takeProfit2)}", color = BullishGreen, fontSize = 10.sp)
                                }
                                if (pos.takeProfit3 != null) {
                                    Text("TP3: $${formatPrice(pos.takeProfit3)}", color = Color(0xFF10B981), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Trade History Journal Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "دفتر ثبت معاملات بسته شده (${tradeHistory.size})",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (tradeHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هنوز معامله‌ای بسته نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        } else {
            items(tradeHistory.reversed()) { trade ->
                val sdf = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
                val timeStr = sdf.format(Date(trade.closeTime))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${trade.symbol} • ${trade.direction}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = trade.exitReason,
                                    color = if (trade.isWin) BullishGreen else BearishRed,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ورود: $${formatPrice(trade.entryPrice)}  ←  خروج: $${formatPrice(trade.exitPrice)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(text = timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 9.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (trade.pnlUsd >= 0) "+" else ""}$${formatPrice(trade.pnlUsd)}",
                            color = if (trade.isWin) BullishGreen else BearishRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%+.2f%%", trade.pnlPercent),
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

    // Reset Account Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("ریست حساب آزمایشی", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "آیا مطمئن هستید که می‌خواهید تمام پوزیشن‌های باز و تاریخچه معاملات آزمایشی را پاک کرده و موجودی را به $10,000 اولیه بازگردانید؟",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("بله، ریست کن", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
