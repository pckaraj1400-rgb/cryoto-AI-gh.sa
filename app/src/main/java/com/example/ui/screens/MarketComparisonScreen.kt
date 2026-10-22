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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceComparisonItem
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun MarketComparisonScreen(
    comparisons: List<PriceComparisonItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Explanatory Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = CryptoSecondaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مقایسه چندمنبعی و اختلاف قیمت جهانی و داخلی",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "محاسبه دقیق قیمت بر حسب دلار، تتر، ریال و تومان (هر تومان = ۱۰ ریال). تفکیک قیمت حاصل از تبدیل نرخ تتر و قیمت معامله‌شده واقعی در بازارهای نوبیتکس و والکس.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CryptoPrimaryGold, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "هشدار واقع‌گرایانه: اختلاف قیمت نباید سود قطعی آربیتراژ تلقی شود؛ کارمزد انتقال شبکه، اسپرد صرافی، نقدشوندگی و ریسک زمان جابه‌جایی باید لحاظ شوند.",
                                color = CryptoPrimaryGold,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Only display a rate when it came from a real comparison result.
        if (comparisons.isNotEmpty()) {
            item {
                val usdtToman = comparisons.first().usdtTomanRate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نرخ مرجع تتر به تومان (USDT/TOMAN):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("${formatNumber(usdtToman)} تومان", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        item {
            Text(
                text = "جدول مقایسه منابع برای دارایی‌های برتر:",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (comparisons.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "مقایسه معتبر فعلاً در دسترس نیست. برای جلوگیری از نمایش اختلاف قیمت ساختگی، این بخش فقط پس از دریافت مستقل و زمان‌دار قیمت از هر صرافی پر می‌شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(comparisons) { item ->
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
                            text = "${item.nameFa} (${item.symbol})",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = if (item.domesticVsGlobalDiffPercent >= 0) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "اختلاف: ${String.format(Locale.US, "%+.2f%%", item.domesticVsGlobalDiffPercent)}",
                                color = if (item.domesticVsGlobalDiffPercent >= 0) BullishGreen else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row of Sources
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("قیمت بایننس (Binance)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("$${formatNumber(item.binancePriceUsd)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Column {
                            Text("شاخص کوین‌گکو", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("$${formatNumber(item.coingeckoPriceUsd)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Column {
                            Text("معادل تومانی جهانی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val impliedToman = item.impliedRialFromGlobal / 10.0
                            Text("${formatNumber(impliedToman)} ت", color = CryptoSecondaryCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Domestic Exchanges Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("بازار نوبیتکس (Nobitex)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val nobitexToman = item.nobitexPriceRial / 10.0
                            Text("${formatNumber(nobitexToman)} تومان", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("بازار والکس (Wallex)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val wallexToman = item.wallexPriceRial / 10.0
                            Text("${formatNumber(wallexToman)} تومان", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("اختلاف ریالی در هر واحد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${formatNumber(item.domesticVsGlobalDiffAmountToman)} تومان", color = CryptoPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatNumber(value: Double): String {
    return if (value >= 1_000_000_000) {
        String.format(Locale.US, "%,.0f", value)
    } else if (value >= 1_000_000) {
        String.format(Locale.US, "%,.0f", value)
    } else if (value >= 1000) {
        String.format(Locale.US, "%,.1f", value)
    } else if (value >= 1) {
        String.format(Locale.US, "%.2f", value)
    } else {
        String.format(Locale.US, "%.4f", value)
    }
}
