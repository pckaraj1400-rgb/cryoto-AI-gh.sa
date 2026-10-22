package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AlertCondition
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.PriceAlert
import com.example.data.model.normalizePersianDigits
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoCard
import com.example.ui.theme.CryptoCardElevated
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PriceAlertDialog(
    coins: List<Coin>,
    selectedSymbol: String,
    alerts: List<PriceAlert>,
    onAddAlert: (String, Double, CurrencyUnit, AlertCondition, String) -> Unit,
    onToggleAlert: (String) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onTestAlert: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSymbol by remember { mutableStateOf(selectedSymbol) }
    var targetCurrency by remember { mutableStateOf(CurrencyUnit.USD) }
    var condition by remember { mutableStateOf(AlertCondition.GREATER_THAN_OR_EQUAL) }

    val activeCoin = coins.find { it.symbol == currentSymbol } ?: coins.first()
    val basePrice = if (targetCurrency == CurrencyUnit.TOMAN) activeCoin.priceToman else activeCoin.priceUsd

    var targetPriceInput by remember(currentSymbol, targetCurrency) {
        mutableStateOf(String.format(Locale.US, if (targetCurrency == CurrencyUnit.TOMAN) "%.0f" else "%.2f", basePrice * 1.05))
    }
    var noteInput by remember { mutableStateOf("") }

    val activeCount = alerts.count { it.isActive && !it.isTriggered }
    val triggeredCount = alerts.count { it.isTriggered }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CryptoPrimaryGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = CryptoPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مدیریت هشدارهای هوشمند قیمت",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "دریافت آلارم صوتی و ویبره هنگام رسیدن به اهداف",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = CryptoPrimaryGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("افزودن هشدار جدید", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("هشدارها ($activeCount)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (triggeredCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = BearishRed,
                                        shape = CircleShape,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$triggeredCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // TAB 0: ADD ALERT
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        item {
                            // Coin Selector chips
                            Text("انتخاب رمزارز:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                coins.forEach { coin ->
                                    val isSel = coin.symbol == currentSymbol
                                    Surface(
                                        modifier = Modifier.clickable { currentSymbol = coin.symbol },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text(
                                            text = "${coin.symbol} (${coin.nameFa})",
                                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Current Price and Currency Unit Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("قیمت لحظه‌ای $currentSymbol:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    val curPriceFormatted = if (targetCurrency == CurrencyUnit.TOMAN) {
                                        String.format(Locale.US, "%,.0f تومان", activeCoin.priceToman)
                                    } else {
                                        String.format(Locale.US, "$%,.2f", activeCoin.priceUsd)
                                    }
                                    Text(
                                        curPriceFormatted,
                                        color = CryptoSecondaryCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                // Currency Toggle (USD or TOMAN)
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.clickable { targetCurrency = CurrencyUnit.USD },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (targetCurrency == CurrencyUnit.USD) CryptoPrimaryGold else Color.Transparent
                                    ) {
                                        Text(
                                            "دلار ($)",
                                            color = if (targetCurrency == CurrencyUnit.USD) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier.clickable { targetCurrency = CurrencyUnit.TOMAN },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (targetCurrency == CurrencyUnit.TOMAN) CryptoPrimaryGold else Color.Transparent
                                    ) {
                                        Text(
                                            "تومان",
                                            color = if (targetCurrency == CurrencyUnit.TOMAN) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Condition Selector
                            Text("شرط فعال‌سازی هشدار:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AlertCondition.values().forEach { cond ->
                                    val isSelected = cond == condition
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { condition = cond },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) {
                                            if (cond == AlertCondition.GREATER_THAN_OR_EQUAL) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                                        } else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                if (cond == AlertCondition.GREATER_THAN_OR_EQUAL) BullishGreen else BearishRed
                                            } else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = cond.titleFa,
                                                color = if (isSelected) {
                                                    if (cond == AlertCondition.GREATER_THAN_OR_EQUAL) BullishGreen else BearishRed
                                                } else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Target Price Field
                            OutlinedTextField(
                                value = targetPriceInput,
                                onValueChange = { targetPriceInput = it },
                                label = { Text("قیمت هدف هشدار (${if (targetCurrency == CurrencyUnit.TOMAN) "تومان" else "دلار"})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CryptoPrimaryGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Quick Price Buttons (+1%, +5%, -1%, -5%)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-5.0, -1.0, 1.0, 5.0).forEach { pct ->
                                    val calculated = basePrice * (1.0 + (pct / 100.0))
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                targetPriceInput = String.format(Locale.US, if (targetCurrency == CurrencyUnit.TOMAN) "%.0f" else "%.2f", calculated)
                                                if (pct > 0) condition = AlertCondition.GREATER_THAN_OR_EQUAL
                                                else condition = AlertCondition.LESS_THAN_OR_EQUAL
                                            },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text(
                                            text = if (pct > 0) "+$pct٪" else "$pct٪",
                                            color = if (pct > 0) BullishGreen else BearishRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Note Field
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("یادداشت هشدار (اختیاری، مثلا: مقاومت هفتگی)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CryptoSecondaryCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val price = targetPriceInput.normalizePersianDigits().toDoubleOrNull() ?: return@Button
                                        onAddAlert(currentSymbol, price, targetCurrency, condition, noteInput)
                                        selectedTab = 1
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CryptoPrimaryGold),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddAlert, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ثبت هشدار", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Surface(
                                    modifier = Modifier
                                        .clickable { onTestAlert() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Vibration, contentDescription = null, tint = CryptoSecondaryCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تست آلارم", color = CryptoSecondaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: ALERTS LIST
                    if (alerts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هیچ هشداری هنوز ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(alerts, key = { it.id }) { alert ->
                                val priceFormatted = if (alert.currencyUnit == CurrencyUnit.TOMAN) {
                                    String.format(Locale.US, "%,.0f تومان", alert.targetPrice)
                                } else {
                                    String.format(Locale.US, "$%,.2f", alert.targetPrice)
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (alert.isTriggered) BearishRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (alert.isTriggered) BearishRed else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = alert.symbol,
                                                    color = CryptoPrimaryGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${alert.condition.titleFa} $priceFormatted",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            if (alert.note.isNotBlank()) {
                                                Text(
                                                    text = alert.note,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (alert.isTriggered) {
                                                val timeStr = alert.triggeredAt?.let {
                                                    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it))
                                                } ?: ""
                                                Text(
                                                    text = "⚠️ فعال شده در ساعت $timeStr",
                                                    color = BearishRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!alert.isTriggered) {
                                                Switch(
                                                    checked = alert.isActive,
                                                    onCheckedChange = { onToggleAlert(alert.id) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = BullishGreen)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            IconButton(onClick = { onDeleteAlert(alert.id) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف هشدار",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
