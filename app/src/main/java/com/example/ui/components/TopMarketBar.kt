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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.DataSource
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun TopMarketBar(
    coins: List<Coin>,
    selectedSymbol: String,
    onCoinSelected: (String) -> Unit,
    currencyUnit: CurrencyUnit,
    onCurrencyChange: (CurrencyUnit) -> Unit,
    currentDataSource: DataSource,
    onDataSourceChange: (DataSource) -> Unit,
    usdtTomanRate: Double,
    dataStatus: String,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    activeAlertsCount: Int = 0,
    triggeredAlertsCount: Int = 0,
    onOpenAlerts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSourceMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(vertical = 8.dp)
    ) {
        // Top status and interactive row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status indicator with manual refresh trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onRefresh() }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isRefreshing) CryptoPrimaryGold else BullishGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRefreshing) "در حال استعلام نرخ‌های زنده..." else dataStatus,
                    color = if (isRefreshing) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "بروزرسانی زنده",
                    tint = CryptoSecondaryCyan,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Quick Data Source Selector & Alerts Button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Interactive Data Source Chip
                Box {
                    Surface(
                        modifier = Modifier.clickable { showSourceMenu = true },
                        shape = RoundedCornerShape(6.dp),
                        color = CryptoSecondaryCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoSecondaryCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CryptoSecondaryCyan, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentDataSource.titleFa.substringBefore(" ("),
                                color = CryptoSecondaryCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false }
                    ) {
                        DataSource.values().forEach { src ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = src.titleFa,
                                        color = if (src == currentDataSource) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (src == currentDataSource) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    onDataSourceChange(src)
                                    showSourceMenu = false
                                }
                            )
                        }
                    }
                }

                // Alerts Bell Icon with Badge
                Box {
                    Surface(
                        modifier = Modifier.clickable { onOpenAlerts() },
                        shape = RoundedCornerShape(6.dp),
                        color = if (triggeredAlertsCount > 0) BearishRed.copy(alpha = 0.18f) else CryptoPrimaryGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (triggeredAlertsCount > 0) BearishRed else CryptoPrimaryGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "هشدار قیمت",
                                tint = if (triggeredAlertsCount > 0) BearishRed else CryptoPrimaryGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (triggeredAlertsCount > 0) "!هشدار ($triggeredAlertsCount)" else "آلارم ($activeAlertsCount)",
                                color = if (triggeredAlertsCount > 0) BearishRed else CryptoPrimaryGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Currency Selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    CurrencyUnit.values().forEach { unit ->
                        val isSelected = unit == currencyUnit
                        Surface(
                            modifier = Modifier.clickable { onCurrencyChange(unit) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = unit.symbolFa,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 6.dp))

        // Horizontal scrolling coins ticker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            coins.forEach { coin ->
                val isSelected = coin.symbol == selectedSymbol
                val isUp = coin.change24h >= 0

                val displayPrice = when (currencyUnit) {
                    CurrencyUnit.USD -> "$${formatNumber(coin.priceUsd)}"
                    CurrencyUnit.USDT -> "${formatNumber(coin.priceUsd)} ₮"
                    CurrencyUnit.TOMAN -> "${formatNumber(coin.priceToman)} ت"
                    CurrencyUnit.IRR -> "${formatNumber(coin.priceRial)} ر"
                }

                Surface(
                    modifier = Modifier.clickable { onCoinSelected(coin.symbol) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = coin.symbol,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = coin.nameFa,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = displayPrice,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%+.1f%%", coin.change24h),
                            color = if (isUp) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    return if (value >= 1_000_000) {
        String.format(Locale.US, "%,.0f", value)
    } else if (value >= 1000) {
        String.format(Locale.US, "%,.1f", value)
    } else if (value >= 1) {
        String.format(Locale.US, "%.2f", value)
    } else {
        String.format(Locale.US, "%.4f", value)
    }
}
