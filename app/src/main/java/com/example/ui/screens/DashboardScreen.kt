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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.NewsItem
import com.example.data.model.PaperAccount
import com.example.data.model.TradeSignal
import com.example.ui.components.SignalCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun DashboardScreen(
    coins: List<Coin>,
    selectedCoin: Coin,
    activeSignal: TradeSignal?,
    paperAccount: PaperAccount,
    news: List<NewsItem>,
    dataStatus: String,
    currencyUnit: CurrencyUnit,
    onNavigateToChart: (String) -> Unit,
    onNavigateToAi: () -> Unit,
    onExecutePaperTrade: (TradeSignal) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome & 3-AI Status Card
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
                            Text(
                                text = "داشبورد مرکزی تحلیل کریپتو",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "وضعیت داده‌های بازار، تحلیل تکنیکال و مدیریت ریسک",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        val hasLiveData = dataStatus.startsWith("زنده:")
                        Surface(
                            color = (if (hasLiveData) BullishGreen else CryptoPrimaryGold).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (hasLiveData) BullishGreen else CryptoPrimaryGold, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasLiveData) "داده زنده" else "داده نامعتبر/ناموجود", color = if (hasLiveData) BullishGreen else CryptoPrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dataStatus,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Status of the implemented analysis modules
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiStatusPill(
                            title = "بنیادی و خبری",
                            model = "News Feed",
                            status = if (news.isEmpty()) "پیکربندی نشده" else "اخبار دریافتی",
                            icon = Icons.Default.Newspaper,
                            color = CryptoSecondaryCyan,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAi
                        )
                        AiStatusPill(
                            title = "تکنیکال کلاسیک",
                            model = "Rule Engine",
                            status = "الگوریتمی",
                            icon = Icons.Default.AutoGraph,
                            color = CryptoPrimaryGold,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAi
                        )
                        AiStatusPill(
                            title = "کمی و ریسک",
                            model = "Deterministic",
                            status = "کنترل قطعی",
                            icon = Icons.Default.Shield,
                            color = BullishGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAi
                        )
                    }
                }
            }
        }

        // 2. Paper Trading Account Quick Overview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ارزش کل حساب آزمایشی (Paper Equity)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", paperAccount.totalEquityUsd)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("سود/زیان تحقق‌یافته", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            val pnl = paperAccount.realizedPnLUsd
                            Text(
                                text = "${if (pnl >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", pnl)}",
                                color = if (pnl >= 0) BullishGreen else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("نرخ برد (Win Rate)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", paperAccount.winRatePercent)}٪",
                                color = CryptoPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Highlighted Live Signal (with Entry, SL, TP1, TP2, TP3)
        if (activeSignal != null) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فرصت معاملاتی فعال ${activeSignal.symbol}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "مشاهده در نمودار >",
                            color = CryptoSecondaryCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onNavigateToChart(activeSignal.symbol) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SignalCard(
                        signal = activeSignal,
                        onExecuteInPaperTrading = { onExecutePaperTrade(activeSignal) }
                    )
                }
            }
        }

        // 4. Crypto Markets Quick Table
        item {
            Text(
                text = "قیمت لحظه‌ای بازار و تغییرات ۲۴ ساعته",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(coins) { coin ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChart(coin.symbol) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = coin.symbol.take(3),
                                color = CryptoPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = coin.nameFa, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${coin.symbol}/USDT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val displayPrice = when (currencyUnit) {
                            CurrencyUnit.USD -> "$${formatPrice(coin.priceUsd)}"
                            CurrencyUnit.USDT -> "${formatPrice(coin.priceUsd)} ₮"
                            CurrencyUnit.TOMAN -> "${formatPrice(coin.priceToman)} تومان"
                            CurrencyUnit.IRR -> "${formatPrice(coin.priceRial)} ریال"
                        }
                        Text(text = displayPrice, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = String.format(Locale.US, "%+.2f%%", coin.change24h),
                            color = if (coin.change24h >= 0) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 5. Latest Market News
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "آخرین اخبار و رویدادهای بنیادی",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (news.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "فید خبر زنده هنوز پیکربندی نشده است. هیچ خبر یا زمان انتشار ساختگی نمایش داده نمی‌شود؛ تحلیل بنیادی تا اتصال منبع معتبر ناقص خواهد بود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        items(news.take(3)) { newsItem ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = newsItem.source, color = CryptoSecondaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = newsItem.timeAgoFa, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = newsItem.titleFa, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = newsItem.summaryFa, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AiStatusPill(
    title: String,
    model: String,
    status: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = model, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = status, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1_000_000) {
        String.format(Locale.US, "%,.0f", price)
    } else if (price >= 1000) {
        String.format(Locale.US, "%,.1f", price)
    } else if (price >= 1) {
        String.format(Locale.US, "%.2f", price)
    } else {
        String.format(Locale.US, "%.4f", price)
    }
}
