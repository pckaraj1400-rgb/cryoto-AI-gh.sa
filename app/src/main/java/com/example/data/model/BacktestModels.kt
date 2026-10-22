package com.example.data.model

data class BacktestTrade(
    val id: Int,
    val type: String, // "LONG" or "SHORT"
    val entryTime: Long,
    val entryPrice: Double,
    val exitTime: Long,
    val exitPrice: Double,
    val exitReason: String, // "TP1", "TP2", "TP3", "SL", "TIMEFRAME_CLOSE"
    val profitPercent: Double,
    val profitUsd: Double,
    val riskReward: Double,
    val isWin: Boolean,
    val isOutOfSample: Boolean = false
)

data class EquityPoint(
    val time: Long,
    val equity: Double
)

data class BacktestSummary(
    val strategyName: String,
    val symbol: String,
    val timeframe: String,
    val marketType: MarketType = MarketType.SPOT,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val profitFactor: Double,
    val netProfitUsd: Double,
    val netProfitPercent: Double,
    val maxDrawdownPercent: Double,
    val avgTradePercent: Double,
    val avgWinUsd: Double,
    val avgLossUsd: Double,
    val expectancyUsd: Double, // امید ریاضی هر معامله: (WinRate * AvgWin) - (LossRate * AvgLoss)
    val periodDurationFa: String,
    val inSampleWinRate: Double,
    val outOfSampleWinRate: Double,
    val inSampleNetProfitPercent: Double,
    val outOfSampleNetProfitPercent: Double,
    val trades: List<BacktestTrade>,
    val equityCurve: List<EquityPoint>
)
