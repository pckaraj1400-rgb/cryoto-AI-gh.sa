package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertCondition
import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.DataSource
import com.example.data.model.MarginType
import com.example.data.model.MarketType
import com.example.data.model.NewsItem
import com.example.data.model.PaperAccount
import com.example.data.model.PaperPosition
import com.example.data.model.PaperTradeRecord
import com.example.data.model.PriceAlert
import com.example.data.model.PriceComparisonItem
import com.example.data.model.RiskSettings
import com.example.data.model.TechnicalIndicators
import com.example.data.model.ThemeMode
import com.example.data.model.TradeSignal
import com.example.data.repository.MarketRepository
import com.example.data.repository.PaperTradingRepository
import com.example.data.repository.TradingStorageManager
import com.example.domain.indicators.TechnicalIndicatorsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CryptoTradingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val storageManager = TradingStorageManager(application.applicationContext)
    private val marketRepository = MarketRepository()
    private val paperTradingRepository = PaperTradingRepository(storageManager)

    val coins: StateFlow<List<Coin>> = marketRepository.coinsState
    val paperAccount: StateFlow<PaperAccount> = paperTradingRepository.account
    val openPositions: StateFlow<List<PaperPosition>> = paperTradingRepository.openPositions
    val tradeHistory: StateFlow<List<PaperTradeRecord>> = paperTradingRepository.tradeHistory
    val primaryDataSource: StateFlow<DataSource> = marketRepository.primaryDataSource
    val dataStatus: StateFlow<String> = marketRepository.dataStatus
    val usdtTomanRate: StateFlow<Double> = marketRepository.usdtTomanRate

    // Theme Mode
    private val _themeMode = MutableStateFlow(storageManager.loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("BTC")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedMarketType = MutableStateFlow(MarketType.SPOT)
    val selectedMarketType: StateFlow<MarketType> = _selectedMarketType.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("1h")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    private val _currencyUnit = MutableStateFlow(storageManager.loadCurrencyUnit())
    val currencyUnit: StateFlow<CurrencyUnit> = _currencyUnit.asStateFlow()

    private val _riskSettings = MutableStateFlow(storageManager.loadRiskSettings())
    val riskSettings: StateFlow<RiskSettings> = _riskSettings.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _indicators = MutableStateFlow<TechnicalIndicators?>(null)
    val indicators: StateFlow<TechnicalIndicators?> = _indicators.asStateFlow()

    private val _activeSignal = MutableStateFlow<TradeSignal?>(null)
    val activeSignal: StateFlow<TradeSignal?> = _activeSignal.asStateFlow()

    private val _priceComparisons = MutableStateFlow<List<PriceComparisonItem>>(emptyList())
    val priceComparisons: StateFlow<List<PriceComparisonItem>> = _priceComparisons.asStateFlow()

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Price Alerts
    private val _priceAlerts = MutableStateFlow(
        storageManager.loadPriceAlerts().ifEmpty {
            listOf(
                PriceAlert(
                    id = "alert-1",
                    symbol = "BTC",
                    targetPrice = 85_000.0,
                    currencyUnit = CurrencyUnit.USD,
                    condition = AlertCondition.GREATER_THAN_OR_EQUAL,
                    note = "مقاومت روانی ۸۵ هزار دلار"
                ),
                PriceAlert(
                    id = "alert-2",
                    symbol = "BTC",
                    targetPrice = 80_000.0,
                    currencyUnit = CurrencyUnit.USD,
                    condition = AlertCondition.LESS_THAN_OR_EQUAL,
                    note = "شکست احتمالی کف حمایتی"
                ),
                PriceAlert(
                    id = "alert-3",
                    symbol = "ETH",
                    targetPrice = 2_500.0,
                    currencyUnit = CurrencyUnit.USD,
                    condition = AlertCondition.GREATER_THAN_OR_EQUAL,
                    note = "تارگت سود هفتگی اتریوم"
                )
            )
        }
    )
    val priceAlerts: StateFlow<List<PriceAlert>> = _priceAlerts.asStateFlow()

    private val _recentlyTriggeredAlert = MutableStateFlow<PriceAlert?>(null)
    val recentlyTriggeredAlert: StateFlow<PriceAlert?> = _recentlyTriggeredAlert.asStateFlow()

    init {
        // Load initial persisted values into repository
        val savedRate = storageManager.loadUsdtTomanRate()
        marketRepository.setUsdtTomanRate(savedRate)

        loadDataForSelectedCoin()
        _news.value = marketRepository.getNews()
        _priceComparisons.value = marketRepository.getPriceComparisons()

        // 1. Initial immediate live network fetch
        viewModelScope.launch(Dispatchers.IO) {
            marketRepository.fetchLivePrices()
            fetchKlinesFromNetwork(_selectedSymbol.value, _selectedTimeframe.value)
            marketRepository.fetchLiveNews()
            _news.value = marketRepository.getNews()
            _priceComparisons.value = marketRepository.getPriceComparisons()
            loadDataForSelectedCoin()
        }

        // 2. Periodic background live polling (every 4 seconds for prices, every 30s for news)
        viewModelScope.launch {
            var loopCount = 0
            while (true) {
                delay(4000)
                loopCount++
                try {
                    val pricesUpdated = marketRepository.fetchLivePrices()
                    if (pricesUpdated) {
                        // Update paper positions and alerts only with a successful live-price refresh
                        val currentPriceMap = coins.value.associate { it.symbol to it.priceUsd }
                        paperTradingRepository.updateLivePrices(currentPriceMap)
                        _priceComparisons.value = marketRepository.getPriceComparisons()
                        checkAlertsAgainstPrices()

                        val curCoin = coins.value.find { it.symbol == _selectedSymbol.value }
                        if (curCoin != null && _candles.value.isNotEmpty()) {
                            val updated = _candles.value.toMutableList()
                            val lastIdx = updated.lastIndex
                            val last = updated[lastIdx]
                            updated[lastIdx] = last.copy(
                                close = curCoin.priceUsd,
                                high = maxOf(last.high, curCoin.priceUsd),
                                low = minOf(last.low, curCoin.priceUsd),
                                isClosed = false
                            )
                            _candles.value = updated
                            _indicators.value = TechnicalIndicatorsCalculator.calculate(updated)
                        }
                    } else {
                        // Invalidate any previously actionable signal after a failed live-price refresh
                        loadDataForSelectedCoin()
                    }

                    // Refresh live RSS news every ~30 seconds
                    if (loopCount % 8 == 0) {
                        marketRepository.fetchLiveNews()
                        _news.value = marketRepository.getNews()
                    }
                } catch (e: Exception) {
                    // Keep the UI alive; do not generate a new actionable signal from stale data
                    loadDataForSelectedCoin()
                }
            }
        }
    }

    fun toggleTheme() {
        _themeMode.value = if (_themeMode.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        storageManager.saveThemeMode(_themeMode.value)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        storageManager.saveThemeMode(mode)
    }

    fun setUsdtTomanRate(rate: Double) {
        marketRepository.setUsdtTomanRate(rate)
        storageManager.saveUsdtTomanRate(rate)
        _priceComparisons.value = marketRepository.getPriceComparisons()
    }

    fun refreshLivePrices() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            marketRepository.fetchLivePrices()
            fetchKlinesFromNetwork(_selectedSymbol.value, _selectedTimeframe.value)
            marketRepository.fetchLiveNews()
            _news.value = marketRepository.getNews()
            _priceComparisons.value = marketRepository.getPriceComparisons()
            loadDataForSelectedCoin()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchKlinesFromNetwork(symbol: String, tf: String) {
        val liveKlines = marketRepository.fetchLiveKlines(symbol, tf, 168)
        if (liveKlines != null && liveKlines.isNotEmpty()) {
            _candles.value = liveKlines
            _indicators.value = TechnicalIndicatorsCalculator.calculate(liveKlines)
            val curCoin = coins.value.find { it.symbol == symbol } ?: coins.value.first()
            _activeSignal.value = marketRepository.getSignals(
                coin = curCoin,
                candles = liveKlines,
                marketType = _selectedMarketType.value,
                timeframe = tf,
                accountBalanceUsd = paperAccount.value.availableBalanceUsd,
                riskSettings = _riskSettings.value
            )
        }
    }

    fun selectCoin(symbol: String) {
        _selectedSymbol.value = symbol
        loadDataForSelectedCoin()
        viewModelScope.launch(Dispatchers.IO) {
            fetchKlinesFromNetwork(symbol, _selectedTimeframe.value)
        }
    }

    fun selectMarketType(marketType: MarketType) {
        _selectedMarketType.value = marketType
        loadDataForSelectedCoin()
    }

    fun selectTimeframe(tf: String) {
        _selectedTimeframe.value = tf
        loadDataForSelectedCoin()
        viewModelScope.launch(Dispatchers.IO) {
            fetchKlinesFromNetwork(_selectedSymbol.value, tf)
        }
    }

    fun selectCurrency(unit: CurrencyUnit) {
        _currencyUnit.value = unit
        storageManager.saveCurrencyUnit(unit)
    }

    fun selectDataSource(source: DataSource) {
        marketRepository.setPrimaryDataSource(source)
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            marketRepository.fetchLivePrices()
            fetchKlinesFromNetwork(_selectedSymbol.value, _selectedTimeframe.value)
            _priceComparisons.value = marketRepository.getPriceComparisons()
            loadDataForSelectedCoin()
            _isRefreshing.value = false
        }
    }

    fun updateRiskSettings(settings: RiskSettings) {
        _riskSettings.value = settings
        storageManager.saveRiskSettings(settings)
        loadDataForSelectedCoin()
    }

    fun addPriceAlert(
        symbol: String,
        targetPrice: Double,
        currencyUnit: CurrencyUnit,
        condition: AlertCondition,
        note: String
    ) {
        val newAlert = PriceAlert(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            targetPrice = targetPrice,
            currencyUnit = currencyUnit,
            condition = condition,
            isActive = true,
            note = note
        )
        _priceAlerts.value = _priceAlerts.value + newAlert
        storageManager.savePriceAlerts(_priceAlerts.value)
    }

    fun togglePriceAlert(alertId: String) {
        _priceAlerts.value = _priceAlerts.value.map {
            if (it.id == alertId) it.copy(isActive = !it.isActive) else it
        }
        storageManager.savePriceAlerts(_priceAlerts.value)
    }

    fun deletePriceAlert(alertId: String) {
        _priceAlerts.value = _priceAlerts.value.filter { it.id != alertId }
        storageManager.savePriceAlerts(_priceAlerts.value)
    }

    fun dismissTriggeredAlert() {
        _recentlyTriggeredAlert.value = null
    }

    fun clearTriggeredAlertBanner() {
        dismissTriggeredAlert()
    }

    fun testTriggerAlert(context: Context? = null) {
        val selectedCoin = coins.value.find { it.symbol == _selectedSymbol.value } ?: coins.value.firstOrNull()
        val testPrice = selectedCoin?.priceUsd ?: 90_000.0
        val testAlert = PriceAlert(
            id = "test-${System.currentTimeMillis()}",
            symbol = selectedCoin?.symbol ?: "BTC",
            targetPrice = testPrice,
            currencyUnit = CurrencyUnit.USD,
            condition = AlertCondition.GREATER_THAN_OR_EQUAL,
            note = "تست هشدار قیمتی و لرزش (آزمایشی)",
            isActive = true,
            isTriggered = true,
            triggeredAt = System.currentTimeMillis(),
            triggeredPrice = testPrice
        )
        _recentlyTriggeredAlert.value = testAlert
        triggerVibration()
    }

    private fun checkAlertsAgainstPrices() {
        val coinMap = coins.value.associateBy { it.symbol }
        val updatedAlerts = _priceAlerts.value.map { alert ->
            if (!alert.isActive || alert.isTriggered) return@map alert
            val coin = coinMap[alert.symbol] ?: return@map alert

            val comparePrice = when (alert.currencyUnit) {
                CurrencyUnit.USD, CurrencyUnit.USDT -> coin.priceUsd
                CurrencyUnit.TOMAN -> coin.priceToman
                CurrencyUnit.IRR -> coin.priceRial
            }

            val isConditionMet = when (alert.condition) {
                AlertCondition.GREATER_THAN_OR_EQUAL -> comparePrice >= alert.targetPrice
                AlertCondition.LESS_THAN_OR_EQUAL -> comparePrice <= alert.targetPrice
            }

            if (isConditionMet) {
                val triggered = alert.copy(
                    isTriggered = true,
                    triggeredAt = System.currentTimeMillis(),
                    triggeredPrice = comparePrice
                )
                _recentlyTriggeredAlert.value = triggered
                triggerVibration()
                triggered
            } else {
                alert
            }
        }
        if (updatedAlerts != _priceAlerts.value) {
            _priceAlerts.value = updatedAlerts
            storageManager.savePriceAlerts(updatedAlerts)
        }
    }

    private fun triggerVibration() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (e: Exception) {
            // Ignore vibration errors on emulators or devices without vibrator motor
        }
    }

    private fun loadDataForSelectedCoin() {
        val sym = _selectedSymbol.value
        val tf = _selectedTimeframe.value
        val mType = _selectedMarketType.value
        val coin = coins.value.find { it.symbol == sym } ?: coins.value.first()

        val candleList = marketRepository.getCandlesForCoin(sym, tf, 180)
        _candles.value = candleList
        _indicators.value = TechnicalIndicatorsCalculator.calculate(candleList)

        val signal = marketRepository.getSignals(
            coin = coin,
            candles = candleList,
            marketType = mType,
            timeframe = tf,
            accountBalanceUsd = paperAccount.value.availableBalanceUsd,
            riskSettings = _riskSettings.value
        )
        _activeSignal.value = signal
    }

    fun placePaperOrder(
        marketType: MarketType,
        direction: String,
        amount: Double,
        leverage: Int,
        stopLoss: Double?,
        tp1: Double?,
        tp2: Double?,
        tp3: Double?
    ) {
        val coin = coins.value.find { it.symbol == _selectedSymbol.value } ?: return
        paperTradingRepository.openPosition(
            symbol = coin.symbol,
            marketType = marketType,
            direction = direction,
            entryPrice = coin.priceUsd,
            amount = amount,
            leverage = leverage,
            marginType = MarginType.ISOLATED,
            stopLoss = stopLoss,
            tp1 = tp1,
            tp2 = tp2,
            tp3 = tp3
        )
    }

    fun executeSignalInPaperTrading(signal: TradeSignal) {
        paperTradingRepository.openFromSignal(signal)
    }

    fun closePaperPosition(positionId: String, exitPrice: Double) {
        paperTradingRepository.closePosition(positionId, exitPrice)
    }

    fun resetPaperAccount() {
        paperTradingRepository.resetAccount(10_000.0)
    }
}
