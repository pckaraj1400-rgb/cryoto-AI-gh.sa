package com.example.data.repository

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.DataSource
import com.example.data.model.MarketType
import com.example.data.model.NewsItem
import com.example.data.model.PriceComparisonItem
import com.example.data.model.RiskSettings
import com.example.data.model.TradeSignal
import com.example.domain.ai.FinalDecisionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MarketRepository {

    // Default 1 USDT = 262,000 Tomans = 2,620,000 Rials (مورد تأکید کاربر مطابق نرخ واقعی ارزدیجیتال)
    private val _usdtTomanRate = MutableStateFlow(262_000.0)
    val usdtTomanRate = _usdtTomanRate.asStateFlow()

    private val initialCoins = listOf(
        Coin("BTC", "Bitcoin", "بیت‌کوین", "BTCUSDT", 81_420.0, -1.92, 83_020.0, 81_200.0, 32_500_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("ETH", "Ethereum", "اتریوم", "ETHUSDT", 2_382.0, -2.54, 2_450.0, 2_360.0, 16_100_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("SOL", "Solana", "سولانا", "SOLUSDT", 104.0, -1.72, 108.5, 102.8, 4_900_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("BNB", "BNB", "بایننس کوین", "BNBUSDT", 728.0, 0.05, 735.0, 720.0, 1_200_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("XRP", "XRP", "ریپل", "XRPUSDT", 1.38, -1.10, 1.44, 1.35, 2_800_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("ADA", "Cardano", "کاردانو", "ADAUSDT", 0.215, -2.45, 0.224, 0.211, 650_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("DOGE", "Dogecoin", "دوج‌کوین", "DOGEUSDT", 0.079, -1.26, 0.082, 0.077, 1_100_000_000.0, 0.0, 0.0, 2_620_000.0),
        Coin("AVAX", "Avalanche", "آوالانچ", "AVAXUSDT", 9.45, -3.44, 9.85, 9.32, 420_000_000.0, 0.0, 0.0, 2_620_000.0)
    )

    private val _coinsState = MutableStateFlow(initialCoins)
    val coinsState = _coinsState.asStateFlow()

    private val _primaryDataSource = MutableStateFlow(DataSource.BINANCE)
    val primaryDataSource = _primaryDataSource.asStateFlow()

    private val _dataStatus = MutableStateFlow("در انتظار دریافت داده واقعی؛ قیمت‌های اولیه نمونه هستند و مبنای معامله نیستند")
    val dataStatus = _dataStatus.asStateFlow()

    // Cache real candlestick history
    private val candleCache = ConcurrentHashMap<String, List<Candle>>()
    @Volatile private var lastPriceFetchSucceeded: Boolean = false

    fun setPrimaryDataSource(source: DataSource) {
        _primaryDataSource.value = source
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _dataStatus.value = "سوئیچ به ${source.titleFa} ($timeStr)"
    }

    fun setUsdtTomanRate(rateInToman: Double) {
        if (rateInToman <= 0) return
        _usdtTomanRate.value = rateInToman
        val rialRate = rateInToman * 10.0
        val currentList = _coinsState.value.map { coin ->
            coin.copy(usdtRialRate = rialRate)
        }
        _coinsState.value = currentList
    }

    /**
     * Dispatch live price fetch according to selected DataSource
     */
    suspend fun fetchLivePrices(): Boolean = withContext(Dispatchers.IO) {
        val success = when (_primaryDataSource.value) {
            DataSource.NOBITEX -> fetchNobitexLive()
            DataSource.WALLEX -> fetchWallexLive()
            DataSource.ARZDIGITAL -> fetchArzDigitalLive()
            DataSource.COINGECKO -> fetchCoinGeckoLive()
            DataSource.BINANCE -> fetchBinanceLive()
        }
        lastPriceFetchSucceeded = success
        if (!success) {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            _dataStatus.value = "داده زنده دریافت نشد ($timeStr)؛ مقادیر اولیه نمونه‌اند و نباید مبنای معامله باشند"
        }
        success
    }

    /**
     * 1. Nobitex API integration
     */
    private suspend fun fetchNobitexLive(): Boolean {
        try {
            val url = URL("https://api.nobitex.ir/market/stats")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val json = JSONObject(response)
                val stats = json.optJSONObject("stats")
                if (stats != null) {
                    // Extract live USDT-RLS
                    val usdtObj = stats.optJSONObject("usdt-rls")
                    if (usdtObj != null) {
                        val usdtRls = usdtObj.optDouble("latest", 0.0)
                        if (usdtRls > 100_000) {
                            _usdtTomanRate.value = usdtRls / 10.0
                        }
                    }

                    val rialRate = _usdtTomanRate.value * 10.0
                    val currentList = _coinsState.value.toMutableList()

                    val nobitexKeys = mapOf(
                        "BTC" to "btc-rls",
                        "ETH" to "eth-rls",
                        "SOL" to "sol-rls",
                        "BNB" to "bnb-rls",
                        "XRP" to "xrp-rls",
                        "ADA" to "ada-rls",
                        "DOGE" to "doge-rls",
                        "AVAX" to "avax-rls"
                    )

                    for (i in currentList.indices) {
                        val c = currentList[i]
                        val key = nobitexKeys[c.symbol]
                        val item = key?.let { stats.optJSONObject(it) }
                        if (item != null && item.optDouble("latest", 0.0) > 0.0) {
                            val priceRial = item.optDouble("latest", 0.0)
                            val dayChange = item.optDouble("dayChange", 0.0)
                            val dayHigh = item.optDouble("dayHigh", 0.0)
                            val dayLow = item.optDouble("dayLow", 0.0)
                            val usdPrice = if (rialRate > 0) priceRial / rialRate else c.priceUsd

                            currentList[i] = c.copy(
                                priceUsd = usdPrice,
                                change24h = dayChange,
                                high24h = if (rialRate > 0) dayHigh / rialRate else c.high24h,
                                low24h = if (rialRate > 0) dayLow / rialRate else c.low24h,
                                nobitexPriceRial = priceRial,
                                wallexPriceRial = 0.0,
                                usdtRialRate = rialRate
                            )
                        } else {
                            currentList[i] = c.copy(priceUsd = 0.0, change24h = 0.0, high24h = 0.0, low24h = 0.0, nobitexPriceRial = 0.0, wallexPriceRial = 0.0)
                        }
                    }

                    _coinsState.value = currentList
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    _dataStatus.value = "زنده: نوبیتکس ($timeStr)"
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "Nobitex API error: ${e.message}")
        }

        _dataStatus.value = "خطا در دریافت داده واقعی نوبیتکس؛ داده شبیه‌سازی نمی‌شود"
        return false
    }

    /**
     * 2. Wallex API integration
     */
    private suspend fun fetchWallexLive(): Boolean {
        try {
            val url = URL("https://api.wallex.ir/v1/currencies/stats")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val json = JSONObject(response)
                val result = json.optJSONObject("result")
                if (result != null) {
                    val usdtTmnObj = result.optJSONObject("USDTTMN")
                    if (usdtTmnObj != null) {
                        val tmnPrice = usdtTmnObj.optDouble("price", 0.0)
                        if (tmnPrice > 10_000) {
                            _usdtTomanRate.value = tmnPrice
                        }
                    }

                    val rialRate = _usdtTomanRate.value * 10.0
                    val currentList = _coinsState.value.toMutableList()

                    val wallexKeys = mapOf(
                        "BTC" to "BTCTMN",
                        "ETH" to "ETHTMN",
                        "SOL" to "SOLTMN",
                        "BNB" to "BNBTMN",
                        "XRP" to "XRPTMN",
                        "ADA" to "ADATMN",
                        "DOGE" to "DOGETMN"
                    )

                    for (i in currentList.indices) {
                        val c = currentList[i]
                        val key = wallexKeys[c.symbol]
                        val item = key?.let { result.optJSONObject(it) }
                        if (item != null && item.optDouble("price", 0.0) > 0.0) {
                            val priceTmn = item.optDouble("price", 0.0)
                            val dayChange = item.optDouble("percent_change_24h", 0.0)
                            val priceRial = priceTmn * 10.0
                            val usdPrice = if (rialRate > 0) priceRial / rialRate else c.priceUsd

                            currentList[i] = c.copy(
                                priceUsd = usdPrice,
                                change24h = dayChange,
                                wallexPriceRial = priceRial,
                                nobitexPriceRial = 0.0,
                                usdtRialRate = rialRate
                            )
                        } else {
                            currentList[i] = c.copy(priceUsd = 0.0, change24h = 0.0, high24h = 0.0, low24h = 0.0, nobitexPriceRial = 0.0, wallexPriceRial = 0.0)
                        }
                    }

                    _coinsState.value = currentList
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    _dataStatus.value = "زنده: والکس ($timeStr)"
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "Wallex API error: ${e.message}")
        }

        _dataStatus.value = "خطا در دریافت داده واقعی والکس؛ داده شبیه‌سازی نمی‌شود"
        return false
    }

    /**
     * 3. ArzDigital provider status. A verified direct API is not configured.
     */
    private suspend fun fetchArzDigitalLive(): Boolean {
        // No verified public market-data API is configured for ArzDigital in this project.
        // Never label prices from another exchange or calculated estimates as ArzDigital live data.
        _dataStatus.value = "اتصال مستقیم معتبر به API ارزدیجیتال پیکربندی نشده است؛ داده زنده در دسترس نیست"
        return false
    }

    /**
     * 4. Binance US ticker API
     */
    private suspend fun fetchBinanceLive(isBackground: Boolean = false): Boolean {
        val pairList = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT", "DOGEUSDT", "AVAXUSDT")
        try {
            val url = URL("https://api.binance.us/api/v3/ticker/24hr")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CryptoAI-Android/1.0")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val jsonArray = JSONArray(response)
                val tickerMap = mutableMapOf<String, JSONObject>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val sym = item.optString("symbol")
                    if (pairList.contains(sym)) {
                        tickerMap[sym] = item
                    }
                }

                if (tickerMap.isNotEmpty()) {
                    val currentList = _coinsState.value.toMutableList()
                    val rialRate = _usdtTomanRate.value * 10.0

                    for (i in currentList.indices) {
                        val c = currentList[i]
                        val ticker = tickerMap[c.pair]
                        if (ticker != null && ticker.optDouble("lastPrice", 0.0) > 0.0) {
                            val price = ticker.optDouble("lastPrice", 0.0)
                            val change = ticker.optDouble("priceChangePercent", 0.0)
                            val high = ticker.optDouble("highPrice", 0.0)
                            val low = ticker.optDouble("lowPrice", 0.0)
                            val quoteVol = ticker.optDouble("quoteVolume", 0.0)

                            currentList[i] = c.copy(
                                priceUsd = price,
                                change24h = change,
                                high24h = high,
                                low24h = low,
                                volume24hUsd = quoteVol,
                                nobitexPriceRial = 0.0,
                                wallexPriceRial = 0.0,
                                usdtRialRate = rialRate
                            )
                        } else {
                            currentList[i] = c.copy(priceUsd = 0.0, change24h = 0.0, high24h = 0.0, low24h = 0.0, volume24hUsd = 0.0, nobitexPriceRial = 0.0, wallexPriceRial = 0.0)
                        }
                    }

                    _coinsState.value = currentList
                    if (!isBackground) {
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                        _dataStatus.value = "زنده: بایننس ($timeStr)"
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "Binance ticker error: ${e.message}")
        }

        if (!isBackground) {
            _dataStatus.value = "داده بایننس دریافت نشد؛ برای استفاده از کوین‌گکو، منبع را صریحاً تغییر بده"
        }
        return false
    }

    /**
     * 5. CoinGecko API integration
     */
    private suspend fun fetchCoinGeckoLive(): Boolean {
        try {
            val cgUrl = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,binancecoin,ripple,cardano,dogecoin,avalanche-2&vs_currencies=usd&include_24hr_change=true")
            val conn = (cgUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val json = JSONObject(response)
                val idMap = mapOf(
                    "BTC" to "bitcoin",
                    "ETH" to "ethereum",
                    "SOL" to "solana",
                    "BNB" to "binancecoin",
                    "XRP" to "ripple",
                    "ADA" to "cardano",
                    "DOGE" to "dogecoin",
                    "AVAX" to "avalanche-2"
                )

                val rialRate = _usdtTomanRate.value * 10.0
                val currentList = _coinsState.value.toMutableList()
                for (i in currentList.indices) {
                    val c = currentList[i]
                    val cgId = idMap[c.symbol]
                    if (cgId != null && json.has(cgId) && json.getJSONObject(cgId).optDouble("usd", 0.0) > 0.0) {
                        val data = json.getJSONObject(cgId)
                        val price = data.optDouble("usd", 0.0)
                        val change = data.optDouble("usd_24h_change", 0.0)
                        currentList[i] = c.copy(
                            priceUsd = price,
                            change24h = change,
                            usdtRialRate = rialRate,
                            nobitexPriceRial = 0.0,
                            wallexPriceRial = 0.0
                        )
                    } else {
                        currentList[i] = c.copy(priceUsd = 0.0, change24h = 0.0, high24h = 0.0, low24h = 0.0, volume24hUsd = 0.0, nobitexPriceRial = 0.0, wallexPriceRial = 0.0)
                    }
                }
                _coinsState.value = currentList
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                _dataStatus.value = "زنده: کوین‌گکو ($timeStr)"
                return true
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "CoinGecko error: ${e.message}")
        }

        _dataStatus.value = "خطا در دریافت داده واقعی کوین‌گکو؛ داده شبیه‌سازی نمی‌شود"
        return false
    }

    /**
     * Fetch real historical candlesticks from CoinGecko OHLC API
     */
    private suspend fun fetchCoinGeckoKlines(symbol: String, timeframe: String): List<Candle>? = withContext(Dispatchers.IO) {
        val idMap = mapOf(
            "BTC" to "bitcoin",
            "ETH" to "ethereum",
            "SOL" to "solana",
            "BNB" to "binancecoin",
            "XRP" to "ripple",
            "ADA" to "cardano",
            "DOGE" to "dogecoin",
            "AVAX" to "avalanche-2"
        )
        val coinId = idMap[symbol.uppercase()] ?: return@withContext null
        val days = when (timeframe) {
            "1m", "5m", "15m" -> "1"
            "1h", "4h" -> "7"
            "1d", "1w" -> "30"
            else -> "7"
        }
        val cacheKey = "${symbol.uppercase()}-COINGECKO-$timeframe"

        try {
            val url = URL("https://api.coingecko.com/api/v3/coins/$coinId/ohlc?vs_currency=usd&days=$days")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CryptoAI-Android/1.0")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val jsonArray = JSONArray(response)
                val list = mutableListOf<Candle>()
                for (i in 0 until jsonArray.length()) {
                    val k = jsonArray.getJSONArray(i)
                    val openTime = k.getLong(0)
                    val open = k.getDouble(1)
                    val high = k.getDouble(2)
                    val low = k.getDouble(3)
                    val close = k.getDouble(4)

                    list.add(
                        Candle(
                            time = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = 1000.0, // CoinGecko OHLC endpoint provides price levels without candle volume
                            isClosed = i < jsonArray.length() - 1
                        )
                    )
                }

                val validList = list.filter { candle ->
                    candle.open.isFinite() && candle.high.isFinite() && candle.low.isFinite() &&
                        candle.close.isFinite() && candle.open > 0 && candle.close > 0 &&
                        candle.high >= maxOf(candle.open, candle.close) &&
                        candle.low <= minOf(candle.open, candle.close) && candle.low > 0
                }
                if (validList.size >= 15) {
                    candleCache[cacheKey] = validList
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    _dataStatus.value = "کندل‌های واقعی کوین‌گکو دریافت شد ($timeStr)"
                    return@withContext validList
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "CoinGecko klines fetch error: ${e.message}")
        }

        candleCache[cacheKey]
    }

    /**
     * Fetch real historical candlesticks from Binance Klines API or CoinGecko
     */
    suspend fun fetchLiveKlines(symbol: String, timeframe: String, count: Int = 180): List<Candle>? = withContext(Dispatchers.IO) {
        if (_primaryDataSource.value == DataSource.COINGECKO) {
            return@withContext fetchCoinGeckoKlines(symbol, timeframe)
        }

        if (_primaryDataSource.value != DataSource.BINANCE) {
            _dataStatus.value = "کندل اختصاصی برای ${_primaryDataSource.value.titleFa} پشتیبانی نمی‌شود؛ برای تحلیل تکنیکال بایننس یا کوین‌گکو را انتخاب کنید"
            return@withContext null
        }

        val binanceTf = when (timeframe) {
            "1m" -> "1m"
            "5m" -> "5m"
            "15m" -> "15m"
            "1h" -> "1h"
            "4h" -> "4h"
            "1d" -> "1d"
            "1w" -> "1w"
            else -> "1h"
        }

        val pair = "${symbol.uppercase()}USDT"
        val cacheKey = "$pair-$binanceTf"

        try {
            val url = URL("https://api.binance.us/api/v3/klines?symbol=$pair&interval=$binanceTf&limit=$count")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CryptoAI-Android/1.0")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val jsonArray = JSONArray(response)
                val list = mutableListOf<Candle>()
                for (i in 0 until jsonArray.length()) {
                    val k = jsonArray.getJSONArray(i)
                    val openTime = k.getLong(0)
                    val open = k.getString(1).toDoubleOrNull() ?: 0.0
                    val high = k.getString(2).toDoubleOrNull() ?: 0.0
                    val low = k.getString(3).toDoubleOrNull() ?: 0.0
                    val close = k.getString(4).toDoubleOrNull() ?: 0.0
                    val vol = k.getString(5).toDoubleOrNull() ?: 0.0

                    list.add(
                        Candle(
                            time = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = vol,
                            isClosed = i < jsonArray.length() - 1
                        )
                    )
                }

                val validList = list.filter { candle ->
                    candle.open.isFinite() && candle.high.isFinite() && candle.low.isFinite() &&
                        candle.close.isFinite() && candle.volume.isFinite() && candle.open > 0 &&
                        candle.close > 0 && candle.high >= maxOf(candle.open, candle.close) &&
                        candle.low <= minOf(candle.open, candle.close) && candle.low > 0
                }
                if (validList.size >= 30) {
                    candleCache[cacheKey] = validList
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    _dataStatus.value = "زنده: بایننس ($timeStr) - ${validList.size} کندل معتبر"
                    return@withContext validList
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "Binance klines fetch error: ${e.message}")
        }

        // Do not silently return stale candles as current market data.
        val cached = candleCache[cacheKey]
        val intervalMillis = when (binanceTf) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "4h" -> 14_400_000L
            "1d" -> 86_400_000L
            "1w" -> 604_800_000L
            else -> 3_600_000L
        }
        if (cached != null && cached.size >= 30 && System.currentTimeMillis() - cached.last().time <= intervalMillis * 2) {
            cached
        } else {
            _dataStatus.value = "کندل معتبر و به‌روز دریافت نشد؛ تحلیل ورود مسدود است"
            null
        }
    }

    /**
     * Returns only validated, previously fetched market candles.
     * No synthetic candles are generated when the network/cache is unavailable.
     */
    fun getCandlesForCoin(symbol: String, timeframe: String, count: Int = 180): List<Candle> {
        val tf = when (timeframe) {
            "1m" -> "1m"
            "5m" -> "5m"
            "15m" -> "15m"
            "1h" -> "1h"
            "4h" -> "4h"
            "1d" -> "1d"
            "1w" -> "1w"
            else -> "1h"
        }
        val cacheKey = if (_primaryDataSource.value == DataSource.COINGECKO) {
            "${symbol.uppercase()}-COINGECKO-$tf"
        } else {
            "${symbol.uppercase()}USDT-$tf"
        }
        val cached = candleCache[cacheKey] ?: return emptyList()
        val intervalMillis = when (tf) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "4h" -> 14_400_000L
            "1d" -> 86_400_000L
            "1w" -> 604_800_000L
            else -> 3_600_000L
        }
        if (cached.size < 15 || System.currentTimeMillis() - cached.last().time > intervalMillis * 3) {
            return emptyList()
        }
        return cached.takeLast(count)
    }

    fun getSignals(
        coin: Coin,
        candles: List<Candle>,
        marketType: MarketType,
        timeframe: String,
        accountBalanceUsd: Double,
        riskSettings: RiskSettings
    ): TradeSignal {
        val sourceMatchesAnalysis = (_primaryDataSource.value == DataSource.BINANCE || _primaryDataSource.value == DataSource.COINGECKO) && lastPriceFetchSucceeded
        val minRequiredCandles = if (_primaryDataSource.value == DataSource.COINGECKO) 15 else 30
        val validCandles = sourceMatchesAnalysis && coin.priceUsd.isFinite() && coin.priceUsd > 0.0 && candles.count { it.isClosed } >= minRequiredCandles && candles.all { candle ->
            candle.open.isFinite() && candle.high.isFinite() && candle.low.isFinite() &&
                candle.close.isFinite() && candle.volume.isFinite() &&
                candle.open > 0 && candle.close > 0 && candle.high >= maxOf(candle.open, candle.close) &&
                candle.low <= minOf(candle.open, candle.close) && candle.low > 0
        }
        if (!validCandles) {
            return TradeSignal(
                id = "data-unavailable-${coin.symbol}-${System.currentTimeMillis()}",
                symbol = coin.symbol,
                marketType = marketType,
                action = com.example.data.model.SignalAction.DATA_UNAVAILABLE,
                timeframe = timeframe,
                entryZoneMin = 0.0,
                entryZoneMax = 0.0,
                entryPrice = 0.0,
                stopLoss = 0.0,
                takeProfit1 = 0.0,
                takeProfit2 = 0.0,
                takeProfit3 = 0.0,
                currentPrice = 0.0,
                riskRewardRatio = "نامشخص",
                suggestedPositionSizeUsd = 0.0,
                maxRiskAmountUsd = 0.0,
                confidenceScore = 0,
                fundamentalVerdict = "داده معتبر کافی در دسترس نیست",
                technicalVerdict = if (!sourceMatchesAnalysis) "منبع قیمت باید با منبع کندل همخوان باشد (بایننس یا کوین‌گکو) و دریافت زنده قیمت موفق باشد" else "تحلیل به حداقل $minRequiredCandles کندل معتبر بسته‌شده نیاز دارد",
                quantVerdict = "ورود به معامله مسدود است",
                invalidationCondition = "تا زمان دریافت داده معتبر و همخوان، سیگنال صادر نمی‌شود",
                priceZoneStatusFa = "داده در دسترس نیست"
            )
        }
        return FinalDecisionEngine.generateSignal(
            coin = coin,
            candles = candles,
            marketType = marketType,
            timeframe = timeframe,
            accountBalanceUsd = accountBalanceUsd,
            riskSettings = riskSettings,
            currentNewsList = getNews()
        )
    }

    // Live news state
    private val _liveNews = MutableStateFlow<List<NewsItem>>(emptyList())
    val liveNews = _liveNews.asStateFlow()

    fun getNews(): List<NewsItem> = _liveNews.value

    /**
     * دریافت اخبار زنده واقعی از فید رسمی Cointelegraph RSS با اعتبارسنجی تاریخ، منبع و احساسات
     */
    suspend fun fetchLiveNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://cointelegraph.com/rss")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/xml, text/xml")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val xmlContent = reader.readText()
                reader.close()
                conn.disconnect()

                val items = parseCointelegraphRss(xmlContent)
                if (items.isNotEmpty()) {
                    _liveNews.value = items
                    return@withContext items
                }
            }
        } catch (e: Exception) {
            Log.w("MarketRepo", "Live news fetch error: ${e.message}")
        }
        _liveNews.value
    }

    private fun parseCointelegraphRss(xml: String): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        val titleRegex = Regex("<title><!\\[CDATA\\[(.*?)\\]\\]></title>|<title>(.*?)</title>")
        val linkRegex = Regex("<link>(.*?)</link>|<guid[^>]*>(.*?)</guid>")
        val pubDateRegex = Regex("<pubDate>(.*?)</pubDate>")
        val descRegex = Regex("<description><!\\[CDATA\\[(.*?)\\]\\]></description>|<description>(.*?)</description>")

        val rfcFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        var idCounter = 1

        for (match in itemRegex.findAll(xml)) {
            val itemBlock = match.groupValues[1]

            val titleMatch = titleRegex.find(itemBlock)
            val title = (titleMatch?.groupValues?.get(1)?.ifBlank { null }
                ?: titleMatch?.groupValues?.get(2) ?: "").trim()
            if (title.isBlank()) continue

            val linkMatch = linkRegex.find(itemBlock)
            val link = (linkMatch?.groupValues?.get(1)?.ifBlank { null }
                ?: linkMatch?.groupValues?.get(2) ?: "").trim()

            val pubDateMatch = pubDateRegex.find(itemBlock)
            val pubDateStr = pubDateMatch?.groupValues?.get(1)?.trim() ?: ""

            val descMatch = descRegex.find(itemBlock)
            val rawDesc = (descMatch?.groupValues?.get(1)?.ifBlank { null }
                ?: descMatch?.groupValues?.get(2) ?: "").trim()
            val cleanDesc = rawDesc.replace(Regex("<.*?>"), "").take(140)

            // Calculate real relative time
            var timeAgoFa = "تازگی نامشخص"
            try {
                if (pubDateStr.isNotBlank()) {
                    val date = rfcFormat.parse(pubDateStr)
                    if (date != null) {
                        val diffMs = System.currentTimeMillis() - date.time
                        timeAgoFa = when {
                            diffMs < 60_000L -> "چند لحظه پیش"
                            diffMs < 3_600_000L -> "${diffMs / 60_000L} دقیقه پیش"
                            diffMs < 86_400_000L -> "${diffMs / 3_600_000L} ساعت پیش"
                            else -> "${diffMs / 86_400_000L} روز پیش"
                        }
                    }
                }
            } catch (_: Exception) { }

            // Determine sentiment and impact from title & description
            val lowerText = (title + " " + cleanDesc).lowercase(Locale.US)
            val positiveWords = listOf("surge", "rally", "gain", "bull", "high", "jump", "record", "soar", "approval", "adoption", "inflow", "partnership")
            val negativeWords = listOf("crash", "drop", "dump", "bear", "fall", "ban", "lawsuit", "hack", "fraud", "scam", "plunge", "loss", "decline", "sec")
            val isPositive = positiveWords.any { lowerText.contains(it) }
            val isNegative = negativeWords.any { lowerText.contains(it) }

            val sentiment = when {
                isPositive && !isNegative -> "BULLISH"
                isNegative && !isPositive -> "BEARISH"
                else -> "NEUTRAL"
            }

            val highImpactWords = listOf("fed", "sec", "etf", "interest rate", "inflation", "billion", "hack")
            val impact = if (highImpactWords.any { lowerText.contains(it) }) "HIGH" else "MEDIUM"

            result.add(
                NewsItem(
                    id = "news-$idCounter-${System.currentTimeMillis()}",
                    title = title,
                    titleFa = title, // Title preserved directly from authentic source
                    source = "Cointelegraph",
                    timeAgoFa = timeAgoFa,
                    sentiment = sentiment,
                    impact = impact,
                    summaryFa = cleanDesc,
                    url = link.ifBlank { null }
                )
            )
            idCounter++
            if (result.size >= 15) break
        }
        return result
    }

    /**
     * مقایسه مستقل قیمت بین صرافی‌ها بر مبنای داده‌های واقعی دریافت‌شده
     */
    fun getPriceComparisons(): List<PriceComparisonItem> {
        val currentCoins = _coinsState.value
        val rialRate = _usdtTomanRate.value * 10.0

        return currentCoins.mapNotNull { coin ->
            val binancePrice = if (coin.priceUsd > 0) coin.priceUsd else 0.0
            val impliedRial = binancePrice * rialRate
            val nobitexRial = coin.nobitexPriceRial
            val wallexRial = coin.wallexPriceRial

            val domesticRial = when {
                nobitexRial > 0 -> nobitexRial
                wallexRial > 0 -> wallexRial
                else -> 0.0
            }

            val diffPercent = if (domesticRial > 0 && impliedRial > 0) {
                ((domesticRial - impliedRial) / impliedRial) * 100.0
            } else 0.0

            val diffToman = if (domesticRial > 0 && impliedRial > 0) {
                (domesticRial - impliedRial) / 10.0
            } else 0.0

            PriceComparisonItem(
                symbol = coin.symbol,
                nameFa = coin.nameFa,
                binancePriceUsd = binancePrice,
                coingeckoPriceUsd = coin.priceUsd,
                nobitexPriceRial = nobitexRial,
                wallexPriceRial = wallexRial,
                impliedRialFromGlobal = impliedRial,
                domesticVsGlobalDiffPercent = diffPercent,
                domesticVsGlobalDiffAmountToman = diffToman,
                usdtTomanRate = _usdtTomanRate.value
            )
        }
    }

}
