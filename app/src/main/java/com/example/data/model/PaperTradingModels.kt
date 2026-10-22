package com.example.data.model

enum class OrderType(val titleFa: String) {
    MARKET("قیمت لحظه‌ای (Market)"),
    LIMIT("سفارش محدود (Limit)"),
    STOP_MARKET("حد ضرر شناور (Stop-Market)")
}

enum class MarginType(val titleFa: String) {
    ISOLATED("ایزوله (Isolated)"),
    CROSS("کراس (Cross)")
}

data class PaperAccount(
    val initialBalanceUsd: Double = 10_000.0,
    val availableBalanceUsd: Double = 10_000.0,
    val totalEquityUsd: Double = 10_000.0,
    val realizedPnLUsd: Double = 0.0,
    val unrealizedPnLUsd: Double = 0.0,
    val maxDrawdownPercent: Double = 0.0,
    val winRatePercent: Double = 0.0,
    val totalTradesCount: Int = 0,
    val winningTradesCount: Int = 0,
    val losingTradesCount: Int = 0
)

data class PaperPosition(
    val id: String,
    val symbol: String,
    val marketType: MarketType,
    val direction: String, // "LONG" or "SHORT" or "SPOT_BUY"
    val entryPrice: Double,
    val currentPrice: Double,
    val amount: Double,
    val initialAmount: Double = amount,
    val notionalValueUsd: Double,
    val leverage: Int = 1,
    val marginType: MarginType = MarginType.ISOLATED,
    val marginUsd: Double,
    val stopLoss: Double? = null,
    val takeProfit1: Double? = null,
    val takeProfit2: Double? = null,
    val takeProfit3: Double? = null,
    val liquidationPrice: Double? = null,
    val unrealizedPnLUsd: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val fundingFeePaid: Double = 0.0,
    val entryFeeUsd: Double = 0.0,
    val tp1Triggered: Boolean = false,
    val tp2Triggered: Boolean = false,
    val openTime: Long = System.currentTimeMillis()
)

data class PaperTradeRecord(
    val id: String,
    val symbol: String,
    val marketType: MarketType,
    val direction: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val exitReason: String, // "TP1", "TP2", "TP3", "SL", "دستی", "لیکویید فرضی"
    val amount: Double,
    val pnlUsd: Double,
    val pnlPercent: Double,
    val feeUsd: Double,
    val openTime: Long,
    val closeTime: Long = System.currentTimeMillis(),
    val isWin: Boolean = pnlUsd > 0
)

data class RiskSettings(
    val maxRiskPerTradePercent: Double = 0.5, // 0.5% default as specified
    val maxDailyDrawdownPercent: Double = 3.0,
    val maxLeverageAllowed: Int = 20,
    val maxOpenPositions: Int = 5,
    val emergencyKillSwitch: Boolean = false,
    val enforceRiskRewardFilter: Boolean = true, // Min 1:1.5 R:R
    val defaultStopLossPercent: Double = 2.0
)
