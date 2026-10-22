package com.example.data.model

enum class SignalAction(val titleFa: String, val isTradeable: Boolean) {
    LONG("خرید / لانگ (LONG)", true),
    SHORT("فروش / شورت (SHORT)", true),
    WAIT("در انتظار شکل‌گیری (WAIT)", false),
    NO_TRADE("عدم ورود / ریسک بالا (NO TRADE)", false),
    DATA_UNAVAILABLE("داده در دسترس نیست (DATA UNAVAILABLE)", false)
}

enum class SignalStatus(val titleFa: String) {
    PENDING("در انتظار ورود"),
    ACTIVE("فعال / در جریان معامله"),
    HIT_TP1("لمس تارگت اول (TP1)"),
    HIT_TP2("لمس تارگت دوم (TP2)"),
    HIT_TP3("تکمیل تمام اهداف (TP3)"),
    STOPPED_OUT("اصابت حد ضرر (SL)"),
    CANCELLED("منقضی / لغو شده")
}

data class TradeSignal(
    val id: String,
    val symbol: String,
    val marketType: MarketType,
    val action: SignalAction,
    val timeframe: String,
    // THE CORE MANDATORY TRADE LEVELS
    val entryZoneMin: Double,
    val entryZoneMax: Double,
    val entryPrice: Double, // نقطه ورود مرجع
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val takeProfit3: Double,
    val currentPrice: Double,
    val riskRewardRatio: String, // e.g. "1:2.8"
    val suggestedPositionSizeUsd: Double,
    val maxRiskAmountUsd: Double,
    val recommendedLeverage: Int = 1,
    val status: SignalStatus = SignalStatus.PENDING,
    val confidenceScore: Int = 78, // heuristic score 0-100; not a calibrated probability
    val fundamentalVerdict: String = "",
    val technicalVerdict: String = "",
    val quantVerdict: String = "",
    val invalidationCondition: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val isLiveTrade: Boolean = false,
    val notionalValueUsd: Double = 0.0,
    val initialMarginUsd: Double = 0.0,
    val estimatedFeesUsd: Double = 0.0,
    val priceZoneStatusFa: String = ""
) {
    val isLong: Boolean get() = action == SignalAction.LONG
    val isShort: Boolean get() = action == SignalAction.SHORT

    // Distance calculations
    val slDistancePercent: Double
        get() = if (entryPrice > 0) Math.abs((entryPrice - stopLoss) / entryPrice * 100.0) else 0.0

    val tp1DistancePercent: Double
        get() = if (entryPrice > 0) Math.abs((takeProfit1 - entryPrice) / entryPrice * 100.0) else 0.0

    val tp2DistancePercent: Double
        get() = if (entryPrice > 0) Math.abs((takeProfit2 - entryPrice) / entryPrice * 100.0) else 0.0

    val tp3DistancePercent: Double
        get() = if (entryPrice > 0) Math.abs((takeProfit3 - entryPrice) / entryPrice * 100.0) else 0.0
}
