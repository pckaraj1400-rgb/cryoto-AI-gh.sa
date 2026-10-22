package com.example.data.model

enum class MarketType(val titleFa: String, val code: String) {
    SPOT("اسپات (Spot)", "SPOT"),
    FUTURES_USDT_M("فیوچرز (USDT-M)", "FUTURES")
}

enum class CurrencyUnit(val symbolFa: String, val symbolEn: String) {
    USD("دلار", "$"),
    USDT("تتر", "₮"),
    TOMAN("تومان", "تومان"),
    IRR("ریال", "ریال")
}

enum class DataSource(val titleFa: String, val isDomestic: Boolean) {
    BINANCE("بایننس (Binance)", false),
    COINGECKO("کوین‌گکو (CoinGecko)", false),
    NOBITEX("نوبیتکس (Nobitex)", true),
    WALLEX("والکس (Wallex)", true),
    ARZDIGITAL("ارزدیجیتال (ArzDigital)", true)
}

enum class ThemeMode(val titleFa: String) {
    DARK("تاریک (سیاه)"),
    LIGHT("روشن (سفید)"),
    SYSTEM("پیرو سیستم")
}

enum class AlertCondition(val titleFa: String) {
    GREATER_THAN_OR_EQUAL("بیشتر یا مساوی (≥)"),
    LESS_THAN_OR_EQUAL("کمتر یا مساوی (≤)")
}

data class PriceAlert(
    val id: String,
    val symbol: String,
    val targetPrice: Double,
    val currencyUnit: CurrencyUnit = CurrencyUnit.USD,
    val condition: AlertCondition = AlertCondition.GREATER_THAN_OR_EQUAL,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null,
    val triggeredPrice: Double? = null,
    val note: String = ""
)

data class Coin(
    val symbol: String,
    val name: String,
    val nameFa: String,
    val pair: String,
    val priceUsd: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val nobitexPriceRial: Double = 0.0,
    val wallexPriceRial: Double = 0.0,
    val usdtRialRate: Double = 2_620_000.0 // 1 USDT = 262,000 Tomans = 2,620,000 Rials (به‌روزرسانی با نرخ اعلامی ارزدیجیتال)
) {
    val priceToman: Double get() = (priceUsd * (usdtRialRate / 10.0))
    val priceRial: Double get() = priceUsd * usdtRialRate
}

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isClosed: Boolean = true
)

data class TechnicalIndicators(
    val rsi: Double,
    val ema20: Double,
    val ema50: Double,
    val sma200: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val macdStatusFa: String,
    val atr: Double,
    val bollingerUpper: Double,
    val bollingerMiddle: Double,
    val bollingerLower: Double,
    val support: Double,
    val resistance: Double,
    val marketStructureFa: String,
    val breakoutQualityFa: String,
    val demandZoneMin: Double,
    val demandZoneMax: Double,
    val supplyZoneMin: Double,
    val supplyZoneMax: Double
)

data class NewsItem(
    val id: String,
    val title: String,
    val titleFa: String,
    val source: String,
    val timeAgoFa: String,
    val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val impact: String, // "HIGH", "MEDIUM", "LOW"
    val summaryFa: String,
    val url: String? = null
)

data class PriceComparisonItem(
    val symbol: String,
    val nameFa: String,
    val binancePriceUsd: Double,
    val coingeckoPriceUsd: Double,
    val nobitexPriceRial: Double,
    val wallexPriceRial: Double,
    val impliedRialFromGlobal: Double,
    val domesticVsGlobalDiffPercent: Double,
    val domesticVsGlobalDiffAmountToman: Double,
    val usdtTomanRate: Double,
    val lastUpdate: Long = System.currentTimeMillis()
)

fun String.normalizePersianDigits(): String {
    return this
        .replace('۰', '0')
        .replace('۱', '1')
        .replace('۲', '2')
        .replace('۳', '3')
        .replace('۴', '4')
        .replace('۵', '5')
        .replace('۶', '6')
        .replace('۷', '7')
        .replace('۸', '8')
        .replace('۹', '9')
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
        .replace('٫', '.')
        .replace("،", "")
        .replace(",", "")
        .replace(" ", "")
        .trim()
}
