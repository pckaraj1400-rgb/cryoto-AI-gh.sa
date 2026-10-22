package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.MarketType
import com.example.data.model.NewsItem
import com.example.data.model.PaperPosition
import com.example.data.model.RiskSettings
import com.example.data.model.SignalAction
import com.example.data.model.TradeSignal
import com.example.domain.ai.FinalDecisionEngine
import com.example.domain.ai.FundamentalAnalyst
import com.example.domain.ai.QuantitativeRiskEngine
import com.example.domain.ai.TechnicalAnalyst
import com.example.ui.components.SignalCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun AiAnalystsScreen(
    coin: Coin,
    coins: List<Coin>,
    onSelectCoin: (String) -> Unit,
    candles: List<Candle>,
    signal: TradeSignal?,
    news: List<NewsItem>,
    riskSettings: RiskSettings,
    availableBalanceUsd: Double,
    selectedMarketType: MarketType,
    onMarketTypeChanged: (MarketType) -> Unit,
    selectedTimeframe: String,
    onTimeframeChanged: (String) -> Unit,
    openPositions: List<PaperPosition> = emptyList(),
    onExecutePaperTrade: (TradeSignal) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "موتور تصمیم‌گیری",
        "۱: بنیادی و خبر",
        "۲: تکنیکال",
        "۳: کمی و ریسک"
    )

    val timeframes = listOf("1m", "5m", "15m", "1h", "4h", "1d", "1w")

    if (candles.count { it.isClosed } < 30 || coin.priceUsd <= 0.0 || signal?.action == SignalAction.DATA_UNAVAILABLE) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("تحلیل قابل اتکا در دسترس نیست", color = CryptoPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "برای جلوگیری از ساختن تحلیل ظاهراً معتبر، این صفحه تا دریافت حداقل ۳۰ کندل بسته‌شده معتبر و قیمت زنده همخوان، گزارش یا سطوح معاملاتی تولید نمی‌کند.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
        return
    }

    // تولید گزارش جامع با موتور تصمیم‌گیری نهایی
    val comprehensiveReport = remember(
        coin, candles, news, selectedMarketType, selectedTimeframe,
        riskSettings, availableBalanceUsd, openPositions
    ) {
        FinalDecisionEngine.evaluateComprehensive(
            coin = coin,
            candles = candles,
            marketType = selectedMarketType,
            timeframe = selectedTimeframe,
            accountBalanceUsd = availableBalanceUsd,
            riskSettings = riskSettings,
            currentNewsList = news,
            openPositions = openPositions,
            currentDailyDrawdownPercent = 0.0
        )
    }

    val fundamental = comprehensiveReport.fundamentalReport
    val technical = comprehensiveReport.technicalReport
    val quant = comprehensiveReport.quantReport
    val activeSignal = comprehensiveReport.signal

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // نوار انتخاب ارز، بازار و تایم‌فریم
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // سطر انتخاب ارزها
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                coins.forEach { c ->
                    val isSelected = c.symbol == coin.symbol
                    Surface(
                        color = if (isSelected) CryptoPrimaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CryptoPrimaryGold else Color.Transparent
                        ),
                        modifier = Modifier
                            .clickable { onSelectCoin(c.symbol) }
                            .testTag("ai_coin_chip_${c.symbol}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = c.symbol,
                                color = if (isSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$${formatPrice(c.priceUsd)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // سطر بازار (اسپات / فیوچرز) و تایم‌فریم‌ها (1m, 5m, 15m, 1h, 4h, 1d, 1w)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه‌های اسپات و فیوچرز
                Surface(
                    color = if (selectedMarketType == MarketType.SPOT) CryptoSecondaryCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedMarketType == MarketType.SPOT) CryptoSecondaryCyan else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onMarketTypeChanged(MarketType.SPOT) }
                ) {
                    Text(
                        text = "اسپات (Spot)",
                        color = if (selectedMarketType == MarketType.SPOT) CryptoSecondaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (selectedMarketType == MarketType.FUTURES_USDT_M) CryptoPrimaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedMarketType == MarketType.FUTURES_USDT_M) CryptoPrimaryGold else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onMarketTypeChanged(MarketType.FUTURES_USDT_M) }
                ) {
                    Text(
                        text = "فیوچرز (USDT-M)",
                        color = if (selectedMarketType == MarketType.FUTURES_USDT_M) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // تایم‌فریم‌ها
                timeframes.forEach { tf ->
                    val isTfSelected = tf == selectedTimeframe
                    Surface(
                        color = if (isTfSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTfSelected) CryptoPrimaryGold else Color.Transparent
                        ),
                        modifier = Modifier.clickable { onTimeframeChanged(tf) }
                    ) {
                        Text(
                            text = tf,
                            color = if (isTfSelected) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isTfSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // تب‌بار ماژول‌های هوش مصنوعی
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CryptoPrimaryGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CryptoPrimaryGold,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // ==========================================
                    // تب ۰: موتور تصمیم‌گیری نهایی (Final Decision Engine)
                    // ==========================================
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Gavel, contentDescription = null, tint = CryptoPrimaryGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "موتور تصمیم‌گیری نهایی (Final Decision Engine)",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "این موتور خروجی سه تحلیلگر بنیادی، تکنیکال و کمی را تجمیع نموده و با بررسی تلاقی شواهد و حق وتوی قطعی ریسک، تصمیم نهایی را تعیین می‌کند.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // بنر نتیجه اجماع و وتو
                                val action = comprehensiveReport.consensusAction
                                val isVeto = comprehensiveReport.isVetoTriggered

                                Surface(
                                    color = if (isVeto) BearishRed.copy(alpha = 0.15f)
                                    else if (action == SignalAction.LONG) BullishGreen.copy(alpha = 0.15f)
                                    else if (action == SignalAction.SHORT) BearishRed.copy(alpha = 0.15f)
                                    else CryptoPrimaryGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isVeto) BearishRed else if (action.isTradeable) BullishGreen else CryptoPrimaryGold
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isVeto) Icons.Default.Warning else if (action.isTradeable) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Rule,
                                                    contentDescription = null,
                                                    tint = if (isVeto) BearishRed else if (action == SignalAction.LONG) BullishGreen else if (action == SignalAction.SHORT) BearishRed else CryptoPrimaryGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "تصمیم نهایی: ${action.titleFa}",
                                                    color = if (isVeto) BearishRed else if (action == SignalAction.LONG) BullishGreen else if (action == SignalAction.SHORT) BearishRed else CryptoPrimaryGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Text(
                                                text = "اطمینان کالیبره‌شده: ${comprehensiveReport.consensusConfidenceScore}٪",
                                                color = CryptoPrimaryGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = comprehensiveReport.consensusSummaryFa,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "وضعیت قیمت: ${activeSignal.priceZoneStatusFa}",
                                            color = CryptoSecondaryCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // کارت وضعیت ۳ تحلیلگر مستقل (Matrix Voting)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ماتریس ارزیابی سه تحلیلگر مستقل:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnalystSummaryRow(
                                    name = "۱. تحلیلگر بنیادی و خبری",
                                    status = fundamental.sentimentFa,
                                    isPositive = fundamental.sentiment == "BULLISH",
                                    score = "${fundamental.confidenceScore}٪"
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                AnalystSummaryRow(
                                    name = "۲. تحلیلگر تکنیکال چندتایم‌فریمی",
                                    status = "${technical.mainTrendDirectionFa} • ${technical.candidateAction.titleFa}",
                                    isPositive = technical.candidateAction.isTradeable,
                                    score = technical.selectedTimeframe
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                AnalystSummaryRow(
                                    name = "۳. تحلیلگر کمی و کنترل ریسک",
                                    status = if (quant.isApproved) "تأییدیه ریسک صادر شد (R:R ${quant.riskRewardRatio})" else "رد شده توسط کنترل‌های ریسک",
                                    isPositive = quant.isApproved,
                                    score = "حق وتو"
                                )
                            }
                        }
                    }

                    // کارت سطوح معامله نهایی و اجرای پیپرتریدینگ
                    item {
                        Text(
                            text = "سیگنال تولیدشده با سطوح کامل ورود و خروج:",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SignalCard(
                            signal = activeSignal,
                            onExecuteInPaperTrading = { onExecutePaperTrade(activeSignal) }
                        )
                    }

                    // جدول شواهد موافق و مخالف
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "شواهد تأییدکننده و عوامل مخالف:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                comprehensiveReport.pros.take(3).forEach { pro ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = pro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }

                                comprehensiveReport.cons.take(3).forEach { con ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = BearishRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = con, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // راهنمای ساده و آموزشی برای کاربر مبتدی
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = CryptoPrimaryGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "راهنمای ساده و آموزشی برای کاربر مبتدی:", color = CryptoPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comprehensiveReport.beginnerGuideFa,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ==========================================
                    // تب ۱: تحلیلگر بنیادی و خبری (Fundamental AI Analyst)
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Newspaper, contentDescription = null, tint = CryptoSecondaryCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تحلیلگر بنیادی و خبری (Fundamental AI Analyst)",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "جهت اثر بنیادی: ${fundamental.sentimentFa}",
                                        color = if (fundamental.sentiment == "BULLISH") BullishGreen else if (fundamental.sentiment == "BEARISH") BearishRed else CryptoPrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "اطمینان: ${fundamental.confidenceScore}٪",
                                        color = CryptoPrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fundamental.summaryFa,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "بازه زمانی اثر احتمالی: ${fundamental.impactHorizonFa}",
                                    color = CryptoSecondaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (fundamental.contradictoryNewsDetected && fundamental.contradictoryNewsDescriptionFa != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = CryptoPrimaryGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = CryptoPrimaryGold, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = fundamental.contradictoryNewsDescriptionFa,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // تفکیک واقعیت تأییدشده از برداشت تحلیلی (Facts vs Interpretations)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "واقعیت‌های تأییدشده با منبع معتبر (Confirmed Facts):",
                                        color = BullishGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                fundamental.confirmedFacts.forEach { fact ->
                                    Text(text = "• $fact", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = CryptoPrimaryGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "برداشت‌های تحلیلی و استنتاج مدل (Interpretations):",
                                        color = CryptoPrimaryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                fundamental.analyticalInterpretations.forEach { interp ->
                                    Text(text = "• $interp", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // رویدادهای پیش رو و رویدادهایی که تا زمان انتشار باید از معامله اجتناب کرد
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = BearishRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تقویم رویدادها، آنلاک توکن و شرایط اجتناب از معامله:",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                fundamental.upcomingEvents.forEach { ev ->
                                    Surface(
                                        color = if (ev.shouldAvoidTradingUntilRelease) BearishRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = ev.titleFa, fontWeight = FontWeight.Bold, color = if (ev.shouldAvoidTradingUntilRelease) BearishRed else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                                Text(text = ev.releaseTimeFa, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = ev.descriptionFa, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                            if (ev.shouldAvoidTradingUntilRelease) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "⛔ هشدار: تا زمان انتشار و آرامش بازار از ورود به پوزیشن اجتناب کنید.", color = BearishRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // منابع و میزان اعتبار آن‌ها (Sources & Credibility)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "منابع خبری، زمان انتشار، دریافت و درجه اعتبار:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                fundamental.sourcesWithCredibility.forEach { src ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = src.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(text = "انتشار: ${src.publishTimeFa} | دریافت داده: ${src.fetchTimeFa}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                        }
                                        Surface(
                                            color = BullishGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(text = "اعتبار: ${src.credibilityScorePercent}٪", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Text(text = "محدودیت‌های تحلیل و اطلاعات مفقود:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                fundamental.limitationsAndMissingData.forEach { lim ->
                                    Text(text = "⚠️ $lim", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ==========================================
                    // تب ۲: تحلیلگر تکنیکال (Technical AI Analyst)
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoGraph, contentDescription = null, tint = CryptoPrimaryGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تحلیلگر تکنیکال چندتایم‌فریمی (${technical.selectedTimeframe})",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // ساختار سه‌گانه چند تایم‌فریمی
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TimeframeTrendBadge(
                                        title = "تایم کلان (4h/1d)",
                                        status = technical.mainTrendDirectionFa,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimeframeTrendBadge(
                                        title = "تایم میانی (1h)",
                                        status = technical.intermediateTrendDirectionFa,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimeframeTrendBadge(
                                        title = "تایم ورود (${technical.selectedTimeframe})",
                                        status = technical.entryTimeframeDirectionFa,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (technical.isTimeframeConflicted && technical.timeframeConflictExplanationFa != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = BearishRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = technical.timeframeConflictExplanationFa,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ساختار بازار: ${technical.marketStructureFa}",
                                    color = CryptoPrimaryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = technical.higherHighsAndLowsFa,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تغییر ساختار (MSB / CHoCH): ${technical.marketStructureBreakFa}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "کیفیت شکست: ${technical.breakoutQualityFa}",
                                    color = CryptoSecondaryCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // سه سناریوی تکنیکال با شرط فعال‌سازی (صعودی، نزولی، خنثی)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "۳ سناریوی مستقل با شروط فعال‌شدن:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                ScenarioCard(
                                    scenario = technical.bullishScenario,
                                    color = BullishGreen,
                                    isActive = technical.candidateAction == SignalAction.LONG
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ScenarioCard(
                                    scenario = technical.bearishScenario,
                                    color = BearishRed,
                                    isActive = technical.candidateAction == SignalAction.SHORT
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ScenarioCard(
                                    scenario = technical.neutralScenario,
                                    color = CryptoPrimaryGold,
                                    isActive = technical.candidateAction == SignalAction.WAIT
                                )
                            }
                        }
                    }

                    // نواحی عرضه، تقاضا و استخر نقدینگی
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "نواحی عرضه، تقاضا و استخرهای نقدینگی:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(BullishGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                            .border(1.dp, BullishGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("ناحیه تقاضا (Demand)", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("$${formatPrice(technical.demandZoneMin)} - $${formatPrice(technical.demandZoneMax)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(BearishRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                            .border(1.dp, BearishRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("ناحیه عرضه (Supply)", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("$${formatPrice(technical.supplyZoneMin)} - $${formatPrice(technical.supplyZoneMax)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "نواحی نقدینگی احتمالی: ${technical.probableLiquidityZonesFa}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "شرط ابطال تحلیل: ${technical.invalidationConditionFa}",
                                    color = BearishRed,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // ==========================================
                    // تب ۳: تحلیلگر کمی و مدیریت ریسک (Quantitative AI & Risk Analyst)
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = BullishGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تحلیلگر کمی و مدیریت ریسک (کد قطعی و تست‌پذیر)",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = if (quant.isApproved) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (quant.isApproved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (quant.isApproved) BullishGreen else BearishRed
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (quant.isApproved) "تأییدیه کمی ریسک: معامله مجاز است" else "رد توسط فیلتر ریسک قطعی",
                                                color = if (quant.isApproved) BullishGreen else BearishRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = quant.summaryFa,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // جدول محاسبات مالی (Financial Math Breakdown)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MathItemRow("سرمایه در معرض ریسک (${riskSettings.maxRiskPerTradePercent}٪)", "$${formatPrice(quant.maxRiskAmountUsd)}")
                                    MathItemRow("حجم موقعیت (فرمول فاصله استاپ)", "$${formatPrice(quant.suggestedPositionSizeUsd)}")
                                    MathItemRow("ارزش اسمی پوزیشن (Notional Value)", "$${formatPrice(quant.notionalValueUsd)}")
                                    MathItemRow("مارجین اولیه تقریبی (Margin)", "$${formatPrice(quant.initialMarginUsd)}")
                                    MathItemRow("نسبت سود به زیان وزن‌دار (R:R)", quant.riskRewardRatio)
                                    MathItemRow("کارمزد تقریبی ورود و خروج", "$${formatPrice(quant.estimatedEntryExitFeesUsd)}")
                                    if (selectedMarketType == MarketType.FUTURES_USDT_M) {
                                        MathItemRow("برآورد فاندینگ ریت (هر ۸ ساعت)", "$${formatPrice(quant.estimatedFundingFeePer8hUsd)}")
                                        if (quant.estimatedLiquidationPrice != null) {
                                            MathItemRow("قیمت لیکویید تقریبی", "$${formatPrice(quant.estimatedLiquidationPrice)}")
                                            MathItemRow("فاصله ایمنی حد ضرر تا لیکویید", "${String.format(Locale.US, "%.1f", quant.distanceToLiquidationPercent ?: 0.0)}٪")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "کنترل‌های ایمنی پورتفوی و سقف زیان:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "• ${quant.dailyLossLimitStatusFa}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text(text = "• ${quant.openPositionsRiskStatusFa}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text(text = "• ${quant.dataQualityStatusFa}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }

                    // چک‌لیست قواعد اجباری مدیریت سرمایه
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = CryptoPrimaryGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "قواعد اجباری مدیریت سرمایه (تست‌های پاس‌شده):",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                quant.capitalPreservationRulesChecked.forEach { rule ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = rule, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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

@Composable
private fun AnalystSummaryRow(name: String, status: String, isPositive: Boolean, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = status, color = if (isPositive) BullishGreen else BearishRed, fontSize = 11.sp)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = score,
                color = CryptoPrimaryGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun TimeframeTrendBadge(title: String, status: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = status, color = CryptoPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ScenarioCard(scenario: TechnicalAnalyst.ScenarioDetails, color: Color, isActive: Boolean) {
    Surface(
        color = if (isActive) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) color else Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = scenario.titleFa, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(text = "احتمال وقوع: ${scenario.probabilityPercent}٪", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "شرط فعال‌سازی: ${scenario.activationConditionFa}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = scenario.descriptionFa, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MathItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 100.0) {
        String.format(Locale.US, "%,.2f", price)
    } else if (price >= 1.0) {
        String.format(Locale.US, "%.4f", price)
    } else {
        String.format(Locale.US, "%.6f", price)
    }
}
