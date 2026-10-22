package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.MarginType
import com.example.data.model.MarketType
import com.example.data.model.normalizePersianDigits
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun OrderPlacementDialog(
    symbol: String,
    currentPrice: Double,
    availableBalanceUsd: Double,
    defaultMarketType: MarketType,
    defaultStopLoss: Double? = null,
    defaultTp1: Double? = null,
    defaultTp2: Double? = null,
    defaultTp3: Double? = null,
    onDismiss: () -> Unit,
    onConfirmOrder: (
        marketType: MarketType,
        direction: String,
        amount: Double,
        leverage: Int,
        stopLoss: Double?,
        tp1: Double?,
        tp2: Double?,
        tp3: Double?
    ) -> Unit
) {
    var marketType by remember { mutableStateOf(defaultMarketType) }
    var direction by remember { mutableStateOf(if (defaultMarketType == MarketType.SPOT) "SPOT_BUY" else "LONG") }
    var leverage by remember { mutableIntStateOf(5) }
    var orderAmountUsdText by remember { mutableStateOf("1000") }
    var stopLossText by remember { mutableStateOf(defaultStopLoss?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var tp1Text by remember { mutableStateOf(defaultTp1?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var tp2Text by remember { mutableStateOf(defaultTp2?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var tp3Text by remember { mutableStateOf(defaultTp3?.let { String.format(Locale.US, "%.2f", it) } ?: "") }

    val amountUsd = orderAmountUsdText.normalizePersianDigits().toDoubleOrNull() ?: 0.0
    val amountUnits = if (currentPrice > 0) amountUsd / currentPrice else 0.0
    val requiredMargin = if (marketType == MarketType.FUTURES_USDT_M) amountUsd / leverage else amountUsd

    val estLiq = if (marketType == MarketType.FUTURES_USDT_M && leverage > 1) {
        val mmr = 0.005
        if (direction == "LONG") {
            currentPrice * (1.0 - (1.0 / leverage) + mmr)
        } else {
            currentPrice * (1.0 + (1.0 / leverage) - mmr)
        }
    } else null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت سفارش آزمایشی $symbol",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "قیمت: $${String.format(Locale.US, "%,.2f", currentPrice)}",
                        color = CryptoPrimaryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Market Type Selector (Spot vs Futures)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (marketType == MarketType.SPOT) CryptoPrimaryGold else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                marketType = MarketType.SPOT
                                direction = "SPOT_BUY"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بازار اسپات (Spot)",
                            color = if (marketType == MarketType.SPOT) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (marketType == MarketType.FUTURES_USDT_M) CryptoSecondaryCyan else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                marketType = MarketType.FUTURES_USDT_M
                                if (direction == "SPOT_BUY") direction = "LONG"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "فیوچرز (USDT-M)",
                            color = if (marketType == MarketType.FUTURES_USDT_M) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Long vs Short (if Futures)
                if (marketType == MarketType.FUTURES_USDT_M) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { direction = "LONG" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "LONG") BullishGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (direction == "LONG") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("خرید / لانگ (Long)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { direction = "SHORT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "SHORT") BearishRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (direction == "SHORT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فروش / شورت (Short)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Leverage Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اهرم معاملاتی (Leverage):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("${leverage}x", color = CryptoPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = leverage.toFloat(),
                            onValueChange = { leverage = it.toInt() },
                            valueRange = 1f..20f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = CryptoPrimaryGold,
                                activeTrackColor = CryptoPrimaryGold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Amount Input (USD)
                OutlinedTextField(
                    value = orderAmountUsdText,
                    onValueChange = { orderAmountUsdText = it },
                    label = { Text("ارزش اسمی سفارش (دلار)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoPrimaryGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = CryptoPrimaryGold
                    ),
                    singleLine = true
                )

                // Available & Margin details
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "موجودی در دسترس: $${String.format(Locale.US, "%,.2f", availableBalanceUsd)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "مارجین موردنیاز: $${String.format(Locale.US, "%.2f", requiredMargin)}",
                        color = if (requiredMargin <= availableBalanceUsd) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (estLiq != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قیمت لیکوییدیشن تقریبی: $${String.format(Locale.US, "%.2f", estLiq)}",
                        color = BearishRed,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Stop Loss & Take Profit (TP1, TP2, TP3)
                Text(
                    text = "تعیین حد ضرر و اهداف سود (اختیاری):",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("حد ضرر (SL)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BearishRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BearishRed
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tp1Text,
                        onValueChange = { tp1Text = it },
                        label = { Text("تارگت ۱ (TP1)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BullishGreen
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tp2Text,
                        onValueChange = { tp2Text = it },
                        label = { Text("تارگت ۲ (TP2)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BullishGreen
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tp3Text,
                        onValueChange = { tp3Text = it },
                        label = { Text("تارگت ۳ (TP3)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF10B981)
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Cancel & Submit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (amountUnits > 0 && requiredMargin <= availableBalanceUsd) {
                                onConfirmOrder(
                                    marketType,
                                    direction,
                                    amountUnits,
                                    if (marketType == MarketType.FUTURES_USDT_M) leverage else 1,
                                    stopLossText.normalizePersianDigits().toDoubleOrNull(),
                                    tp1Text.normalizePersianDigits().toDoubleOrNull(),
                                    tp2Text.normalizePersianDigits().toDoubleOrNull(),
                                    tp3Text.normalizePersianDigits().toDoubleOrNull()
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (direction == "SHORT") BearishRed else BullishGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_order_button")
                    ) {
                        Text("ثبت پوزیشن در حساب آزمایشی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
