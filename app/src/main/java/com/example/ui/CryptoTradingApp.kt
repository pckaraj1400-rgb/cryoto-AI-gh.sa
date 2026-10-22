package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.ui.components.PriceAlertDialog
import com.example.ui.components.TopMarketBar
import com.example.ui.screens.AiAnalystsScreen
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.ChartAndTradeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MarketComparisonScreen
import com.example.ui.screens.PaperTradingScreen
import com.example.ui.screens.SettingsAndRiskScreen
import com.example.ui.theme.BearishRed
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

enum class AppScreen(val titleFa: String) {
    DASHBOARD("داشبورد"),
    CHART("نمودار"),
    AI("هوش مصنوعی"),
    PAPER("حساب آزمایشی"),
    BACKTEST("بکتست"),
    COMPARISON("مقایسه صرافی‌ها"),
    SETTINGS("تنظیمات و ریسک")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoTradingApp(
    viewModel: CryptoTradingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showPriceAlertsDialog by remember { mutableStateOf(false) }

    val coins by viewModel.coins.collectAsStateWithLifecycle()
    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val selectedMarketType by viewModel.selectedMarketType.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()
    val riskSettings by viewModel.riskSettings.collectAsStateWithLifecycle()
    val candles by viewModel.candles.collectAsStateWithLifecycle()
    val indicators by viewModel.indicators.collectAsStateWithLifecycle()
    val activeSignal by viewModel.activeSignal.collectAsStateWithLifecycle()
    val paperAccount by viewModel.paperAccount.collectAsStateWithLifecycle()
    val openPositions by viewModel.openPositions.collectAsStateWithLifecycle()
    val tradeHistory by viewModel.tradeHistory.collectAsStateWithLifecycle()
    val priceComparisons by viewModel.priceComparisons.collectAsStateWithLifecycle()
    val news by viewModel.news.collectAsStateWithLifecycle()
    val primaryDataSource by viewModel.primaryDataSource.collectAsStateWithLifecycle()
    val dataStatus by viewModel.dataStatus.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val usdtTomanRate by viewModel.usdtTomanRate.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val priceAlerts by viewModel.priceAlerts.collectAsStateWithLifecycle()
    val recentlyTriggeredAlert by viewModel.recentlyTriggeredAlert.collectAsStateWithLifecycle()

    val currentCoin = coins.find { it.symbol == selectedSymbol } ?: coins.firstOrNull() ?: return

    val activeAlertsCount = priceAlerts.count { it.isActive && !it.isTriggered }
    val triggeredAlertsCount = priceAlerts.count { it.isTriggered }

    // BackHandler to return to Dashboard if on another screen
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    // Force RTL for Persian Interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CryptoAI Trader",
                                    color = CryptoPrimaryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = " • سامانه هوشمند معاملاتی",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        actions = {
                            // Quick Theme Toggle Button (سیاه / سفید)
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "تغییر تم سیاه یا سفید",
                                    tint = CryptoPrimaryGold
                                )
                            }

                            // Price Alerts Bell with Badge
                            IconButton(
                                onClick = { showPriceAlertsDialog = true },
                                modifier = Modifier.testTag("alerts_button")
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "هشدارهای قیمت",
                                        tint = if (triggeredAlertsCount > 0) BearishRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (triggeredAlertsCount > 0) {
                                        Surface(
                                            color = BearishRed,
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {}
                                    }
                                }
                            }

                            // Settings Button
                            IconButton(
                                onClick = { currentScreen = AppScreen.SETTINGS },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "تنظیمات و ریسک",
                                    tint = if (currentScreen == AppScreen.SETTINGS) CryptoPrimaryGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Market Ticker Bar with fast source & currency toggle
                    TopMarketBar(
                        coins = coins,
                        selectedSymbol = selectedSymbol,
                        onCoinSelected = { sym ->
                            viewModel.selectCoin(sym)
                            if (currentScreen == AppScreen.DASHBOARD) {
                                currentScreen = AppScreen.CHART
                            }
                        },
                        currencyUnit = currencyUnit,
                        onCurrencyChange = { viewModel.selectCurrency(it) },
                        currentDataSource = primaryDataSource,
                        onDataSourceChange = { viewModel.selectDataSource(it) },
                        usdtTomanRate = usdtTomanRate,
                        dataStatus = dataStatus,
                        onRefresh = { viewModel.refreshLivePrices() },
                        isRefreshing = isRefreshing,
                        activeAlertsCount = activeAlertsCount,
                        triggeredAlertsCount = triggeredAlertsCount,
                        onOpenAlerts = { showPriceAlertsDialog = true }
                    )

                    // High-Visibility Triggered Alert Notification Banner
                    AnimatedVisibility(
                        visible = recentlyTriggeredAlert != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        recentlyTriggeredAlert?.let { alert ->
                            val priceStr = String.format(Locale.US, "$%,.2f", alert.triggeredPrice ?: alert.targetPrice)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BearishRed.copy(alpha = 0.95f))
                                    .clickable { showPriceAlertsDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🚨 هشدار قیمت فعال شد: ${alert.symbol} به نرخ $priceStr رسید!",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearTriggeredAlertBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { currentScreen = AppScreen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "داشبورد") },
                        label = { Text("داشبورد", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CHART,
                        onClick = { currentScreen = AppScreen.CHART },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "نمودار") },
                        label = { Text("نمودار", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.AI,
                        onClick = { currentScreen = AppScreen.AI },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "هوش مصنوعی") },
                        label = { Text("تحلیلگر AI", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.PAPER,
                        onClick = { currentScreen = AppScreen.PAPER },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "حساب آزمایشی") },
                        label = { Text("آزمایشی", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.BACKTEST,
                        onClick = { currentScreen = AppScreen.BACKTEST },
                        icon = { Icon(Icons.Default.QueryStats, contentDescription = "بکتست") },
                        label = { Text("بکتست", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.COMPARISON,
                        onClick = { currentScreen = AppScreen.COMPARISON },
                        icon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "مقایسه") },
                        label = { Text("مقایسه", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CryptoPrimaryGold,
                            selectedTextColor = CryptoPrimaryGold,
                            indicatorColor = CryptoPrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        coins = coins,
                        selectedCoin = currentCoin,
                        activeSignal = activeSignal,
                        paperAccount = paperAccount,
                        news = news,
                        dataStatus = dataStatus,
                        currencyUnit = currencyUnit,
                        onNavigateToChart = { sym ->
                            viewModel.selectCoin(sym)
                            currentScreen = AppScreen.CHART
                        },
                        onNavigateToAi = { currentScreen = AppScreen.AI },
                        onExecutePaperTrade = { sig ->
                            viewModel.executeSignalInPaperTrading(sig)
                            currentScreen = AppScreen.PAPER
                        }
                    )
                    AppScreen.CHART -> ChartAndTradeScreen(
                        coin = currentCoin,
                        candles = candles,
                        indicators = indicators,
                        signal = activeSignal,
                        marketType = selectedMarketType,
                        onMarketTypeChanged = { viewModel.selectMarketType(it) },
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeChanged = { viewModel.selectTimeframe(it) },
                        availableBalanceUsd = paperAccount.availableBalanceUsd,
                        onPlaceOrder = { mType, dir, amt, lev, sl, tp1, tp2, tp3 ->
                            viewModel.placePaperOrder(mType, dir, amt, lev, sl, tp1, tp2, tp3)
                            currentScreen = AppScreen.PAPER
                        },
                        onExecutePaperSignal = { sig ->
                            viewModel.executeSignalInPaperTrading(sig)
                            currentScreen = AppScreen.PAPER
                        },
                        dataSourceName = primaryDataSource.titleFa,
                        dataTimestamp = dataStatus,
                        hasPaperPosition = openPositions.any { it.symbol == currentCoin.symbol }
                    )
                    AppScreen.AI -> AiAnalystsScreen(
                        coin = currentCoin,
                        coins = coins,
                        onSelectCoin = { viewModel.selectCoin(it) },
                        candles = candles,
                        signal = activeSignal,
                        news = news,
                        riskSettings = riskSettings,
                        availableBalanceUsd = paperAccount.availableBalanceUsd,
                        selectedMarketType = selectedMarketType,
                        onMarketTypeChanged = { viewModel.selectMarketType(it) },
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeChanged = { viewModel.selectTimeframe(it) },
                        openPositions = openPositions,
                        onExecutePaperTrade = { sig ->
                            viewModel.executeSignalInPaperTrading(sig)
                            currentScreen = AppScreen.PAPER
                        }
                    )
                    AppScreen.PAPER -> PaperTradingScreen(
                        account = paperAccount,
                        openPositions = openPositions,
                        tradeHistory = tradeHistory,
                        onClosePosition = { id, exitPrice -> viewModel.closePaperPosition(id, exitPrice) },
                        onResetAccount = { viewModel.resetPaperAccount() }
                    )
                    AppScreen.BACKTEST -> BacktestScreen(
                        symbol = currentCoin.symbol,
                        candles = candles
                    )
                    AppScreen.COMPARISON -> MarketComparisonScreen(
                        comparisons = priceComparisons
                    )
                    AppScreen.SETTINGS -> SettingsAndRiskScreen(
                        currentDataSource = primaryDataSource,
                        onDataSourceSelected = { viewModel.selectDataSource(it) },
                        currencyUnit = currencyUnit,
                        onCurrencySelected = { viewModel.selectCurrency(it) },
                        usdtTomanRate = usdtTomanRate,
                        onUsdtTomanRateChanged = { viewModel.setUsdtTomanRate(it) },
                        themeMode = themeMode,
                        onThemeModeChanged = { viewModel.setThemeMode(it) },
                        riskSettings = riskSettings,
                        onRiskSettingsChanged = { viewModel.updateRiskSettings(it) },
                        onOpenPriceAlerts = { showPriceAlertsDialog = true }
                    )
                }
            }

            // Price Alert Management Dialog
            if (showPriceAlertsDialog) {
                PriceAlertDialog(
                    coins = coins,
                    selectedSymbol = selectedSymbol,
                    alerts = priceAlerts,
                    onAddAlert = { sym, price, curr, cond, note ->
                        viewModel.addPriceAlert(sym, price, curr, cond, note)
                    },
                    onToggleAlert = { id -> viewModel.togglePriceAlert(id) },
                    onDeleteAlert = { id -> viewModel.deletePriceAlert(id) },
                    onTestAlert = { viewModel.testTriggerAlert(context) },
                    onDismiss = { showPriceAlertsDialog = false }
                )
            }
        }
    }
}
