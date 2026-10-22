package com.example.domain.backtest

import com.example.data.model.BacktestSummary
import com.example.data.model.BacktestTrade
import com.example.data.model.Candle
import com.example.data.model.EquityPoint
import com.example.data.model.MarketType
import kotlin.math.abs
import kotlin.math.max

/**
 * موتور بکتستینگ قطعی، مبتنی بر قوانین صریح معاملاتی و فاقد نگاه به آینده (No Look-Ahead Bias).
 * از تفکیک زمانی داده‌ها به بخش توسعه (In-Sample) و آزمون خارج از نمونه (Out-of-Sample) پشتیبانی می‌کند.
 */
object BacktestEngine {
    private const val FEE_RATE = 0.0005       // 0.05% per side (کارمزد ورود و خروج)
    private const val SLIPPAGE_RATE = 0.0005  // 0.05% per side (اسلیپیج محافظه‌کارانه)
    private const val STOP_DISTANCE_PERCENT = 0.02

    fun runBacktest(
        symbol: String,
        timeframe: String,
        strategyName: String,
        candles: List<Candle>,
        marketType: MarketType = MarketType.SPOT,
        initialCapitalUsd: Double = 10_000.0,
        riskPerTradePercent: Double = 0.5
    ): BacktestSummary {
        val validCandles = candles.filter { c ->
            c.isClosed && c.time > 0 && c.open.isFinite() && c.high.isFinite() &&
                c.low.isFinite() && c.close.isFinite() && c.volume.isFinite() &&
                c.open > 0.0 && c.close > 0.0 && c.low > 0.0 &&
                c.high >= maxOf(c.open, c.close) && c.low <= minOf(c.open, c.close)
        }.sortedBy { it.time }

        if (validCandles.size < 25 || !initialCapitalUsd.isFinite() || initialCapitalUsd <= 0.0 ||
            !riskPerTradePercent.isFinite() || riskPerTradePercent <= 0.0) {
            return emptySummary(symbol, timeframe, strategyName, marketType, validCandles, initialCapitalUsd)
        }

        // Split 70% In-sample and 30% Out-of-sample
        val splitIndex = (validCandles.size * 0.70).toInt().coerceIn(15, maxOf(15, validCandles.size - 2))
        val splitTime = validCandles[splitIndex].time

        val firstTime = validCandles.first().time
        val lastTime = validCandles.last().time
        val durationDays = max(1L, (lastTime - firstTime) / (1000L * 60 * 60 * 24))
        val periodDurationFa = "$durationDays روز (${validCandles.size} کندل)"

        var equity = initialCapitalUsd
        var peakEquity = initialCapitalUsd
        var maxDrawdown = 0.0
        val trades = mutableListOf<BacktestTrade>()
        val equityCurve = mutableListOf(EquityPoint(validCandles.first().time, equity))
        var totalGains = 0.0
        var totalLosses = 0.0
        var openTrade: BacktestTrade? = null
        var stop = 0.0
        var tp1 = 0.0
        var tp2 = 0.0
        var tp3 = 0.0
        var positionNotional = 0.0

        fun closeTrade(trade: BacktestTrade, candle: Candle, rawExit: Double, reason: String) {
            val isLong = trade.type == "LONG"
            // Adverse slippage: lower exit for long, higher exit for short
            val exitPrice = if (isLong) rawExit * (1.0 - SLIPPAGE_RATE) else rawExit * (1.0 + SLIPPAGE_RATE)
            val grossReturn = if (isLong) {
                (exitPrice - trade.entryPrice) / trade.entryPrice
            } else {
                (trade.entryPrice - exitPrice) / trade.entryPrice
            }
            val exitNotional = positionNotional * (exitPrice / trade.entryPrice)
            val costs = positionNotional * (FEE_RATE + SLIPPAGE_RATE) + exitNotional * FEE_RATE
            val netPnl = positionNotional * grossReturn - costs
            equity += netPnl
            peakEquity = max(peakEquity, equity)
            val drawdown = if (peakEquity > 0.0) (peakEquity - equity) / peakEquity * 100.0 else 0.0
            maxDrawdown = max(maxDrawdown, drawdown)
            if (netPnl > 0.0) totalGains += netPnl else totalLosses += abs(netPnl)

            trades.add(
                trade.copy(
                    exitTime = candle.time,
                    exitPrice = exitPrice,
                    exitReason = reason,
                    profitPercent = if (positionNotional > 0.0) netPnl / positionNotional * 100.0 else 0.0,
                    profitUsd = netPnl,
                    isWin = netPnl > 0.0,
                    isOutOfSample = trade.entryTime >= splitTime
                )
            )
            equityCurve.add(EquityPoint(candle.time, equity))
            openTrade = null
            positionNotional = 0.0
        }

        val isBreakout = strategyName.contains("Breakout", ignoreCase = true) || strategyName.contains("شکست")
        val isSmartMoney = strategyName.contains("Smart Money", ignoreCase = true) || strategyName.contains("اردر بلاک")

        for (i in 15 until validCandles.size) {
            val candle = validCandles[i]
            val previous = validCandles[i - 1]

            val activeTrade = openTrade
            if (activeTrade != null) {
                val isLong = activeTrade.type == "LONG"
                // Conservative ambiguity rule: if stop and target are touched in the same candle, stop hit first
                val stopHit = if (isLong) candle.low <= stop else candle.high >= stop
                val tp1Hit = if (isLong) candle.high >= tp1 else candle.low <= tp1
                val tp2Hit = if (isLong) candle.high >= tp2 else candle.low <= tp2
                val tp3Hit = if (isLong) candle.high >= tp3 else candle.low <= tp3

                if (stopHit) {
                    closeTrade(activeTrade, candle, stop, "SL")
                    continue
                }

                val targetHit = when {
                    tp3Hit -> tp3 to "TP3"
                    tp2Hit -> tp2 to "TP2"
                    tp1Hit -> tp1 to "TP1"
                    else -> null
                }
                if (targetHit != null) {
                    closeTrade(activeTrade, candle, targetHit.first, targetHit.second)
                    continue
                }
            } else {
                // Signal generation based strictly on preceding candles (no look-ahead)
                val lookback = minOf(15, i)
                val priorClosesFast = validCandles.subList(maxOf(0, i - 5), i).map { it.close }
                val fastMA = priorClosesFast.average()
                val prevFastMA = validCandles.subList(maxOf(0, i - 6), i - 1).map { it.close }.average()

                val priorClosesSlow = validCandles.subList(maxOf(0, i - lookback), i).map { it.close }
                val slowMA = priorClosesSlow.average()
                val prevSlowMA = validCandles.subList(maxOf(0, i - lookback - 1), i - 1).map { it.close }.average()

                val priorSub = validCandles.subList(maxOf(0, i - lookback), i)
                val priorHigh = priorSub.maxOf { it.high }
                val priorLow = priorSub.minOf { it.low }
                val avgVol = priorSub.map { it.volume }.average()

                val allowShort = marketType == MarketType.FUTURES_USDT_M ||
                    strategyName.contains("فیوچرز") || strategyName.contains("Futures", ignoreCase = true)

                val direction = when {
                    isBreakout -> when {
                        candle.close > priorHigh || (candle.close > previous.high && candle.volume >= avgVol * 1.1) -> "LONG"
                        allowShort && (candle.close < priorLow || (candle.close < previous.low && candle.volume >= avgVol * 1.1)) -> "SHORT"
                        else -> null
                    }
                    isSmartMoney -> when {
                        candle.close > previous.high && candle.volume > avgVol -> "LONG"
                        allowShort && candle.close < previous.low && candle.volume > avgVol -> "SHORT"
                        else -> null
                    }
                    else -> when {
                        (fastMA > slowMA && prevFastMA <= prevSlowMA) || (candle.close > fastMA && previous.close <= fastMA) -> "LONG"
                        allowShort && ((fastMA < slowMA && prevFastMA >= prevSlowMA) || (candle.close < fastMA && previous.close >= fastMA)) -> "SHORT"
                        else -> null
                    }
                }

                if (direction != null) {
                    val entry = candle.close
                    stop = if (direction == "LONG") entry * (1.0 - STOP_DISTANCE_PERCENT) else entry * (1.0 + STOP_DISTANCE_PERCENT)
                    val riskDistance = abs(entry - stop)
                    tp1 = if (direction == "LONG") entry + riskDistance else entry - riskDistance
                    tp2 = if (direction == "LONG") entry + riskDistance * 2.0 else entry - riskDistance * 2.0
                    tp3 = if (direction == "LONG") entry + riskDistance * 3.0 else entry - riskDistance * 3.0
                    val riskBudget = equity * (riskPerTradePercent / 100.0)
                    positionNotional = if (riskDistance > 0.0) riskBudget / (riskDistance / entry) else 0.0
                    if (positionNotional <= 0.0 || !positionNotional.isFinite()) continue

                    openTrade = BacktestTrade(
                        id = trades.size + 1,
                        type = direction,
                        entryTime = candle.time,
                        entryPrice = entry,
                        exitTime = 0L,
                        exitPrice = 0.0,
                        exitReason = "",
                        profitPercent = 0.0,
                        profitUsd = 0.0,
                        riskReward = 3.0,
                        isWin = false,
                        isOutOfSample = candle.time >= splitTime
                    )
                }
            }
        }

        // Close any remaining open trade at the end of the series
        openTrade?.let { trade ->
            closeTrade(trade, validCandles.last(), validCandles.last().close, "TIMEFRAME_CLOSE")
        }

        val wins = trades.count { it.isWin }
        val losses = trades.size - wins
        val winRate = if (trades.isNotEmpty()) wins.toDouble() / trades.size * 100.0 else 0.0
        val profitFactor = when {
            totalLosses > 0.0 -> totalGains / totalLosses
            totalGains > 0.0 -> 99.0
            else -> 0.0
        }
        val avgWinUsd = if (wins > 0) totalGains / wins else 0.0
        val avgLossUsd = if (losses > 0) totalLosses / losses else 0.0
        val winRateFraction = if (trades.isNotEmpty()) wins.toDouble() / trades.size else 0.0
        val lossRateFraction = 1.0 - winRateFraction
        val expectancyUsd = (winRateFraction * avgWinUsd) - (lossRateFraction * avgLossUsd)

        // In-sample vs Out-of-sample breakdown
        val inSampleTrades = trades.filter { !it.isOutOfSample }
        val outOfSampleTrades = trades.filter { it.isOutOfSample }

        val inSampleWins = inSampleTrades.count { it.isWin }
        val inSampleWinRate = if (inSampleTrades.isNotEmpty()) inSampleWins.toDouble() / inSampleTrades.size * 100.0 else 0.0
        val inSamplePnl = inSampleTrades.sumOf { it.profitUsd }
        val inSampleNetProfitPct = (inSamplePnl / initialCapitalUsd) * 100.0

        val outOfSampleWins = outOfSampleTrades.count { it.isWin }
        val outOfSampleWinRate = if (outOfSampleTrades.isNotEmpty()) outOfSampleWins.toDouble() / outOfSampleTrades.size * 100.0 else 0.0
        val outOfSamplePnl = outOfSampleTrades.sumOf { it.profitUsd }
        val outOfSampleNetProfitPct = (outOfSamplePnl / initialCapitalUsd) * 100.0

        val netProfitUsd = equity - initialCapitalUsd
        val netProfitPercent = (netProfitUsd / initialCapitalUsd) * 100.0

        return BacktestSummary(
            strategyName = strategyName,
            symbol = symbol,
            timeframe = timeframe,
            marketType = marketType,
            totalTrades = trades.size,
            winningTrades = wins,
            losingTrades = losses,
            winRatePercent = winRate,
            profitFactor = profitFactor,
            netProfitUsd = netProfitUsd,
            netProfitPercent = netProfitPercent,
            maxDrawdownPercent = maxDrawdown,
            avgTradePercent = if (trades.isNotEmpty()) trades.map { it.profitPercent }.average() else 0.0,
            avgWinUsd = avgWinUsd,
            avgLossUsd = avgLossUsd,
            expectancyUsd = expectancyUsd,
            periodDurationFa = periodDurationFa,
            inSampleWinRate = inSampleWinRate,
            outOfSampleWinRate = outOfSampleWinRate,
            inSampleNetProfitPercent = inSampleNetProfitPct,
            outOfSampleNetProfitPercent = outOfSampleNetProfitPct,
            trades = trades,
            equityCurve = equityCurve
        )
    }

    private fun emptySummary(
        symbol: String,
        timeframe: String,
        strategyName: String,
        marketType: MarketType,
        candles: List<Candle>,
        initialCapitalUsd: Double
    ) = BacktestSummary(
        strategyName = strategyName,
        symbol = symbol,
        timeframe = timeframe,
        marketType = marketType,
        totalTrades = 0,
        winningTrades = 0,
        losingTrades = 0,
        winRatePercent = 0.0,
        profitFactor = 0.0,
        netProfitUsd = 0.0,
        netProfitPercent = 0.0,
        maxDrawdownPercent = 0.0,
        avgTradePercent = 0.0,
        avgWinUsd = 0.0,
        avgLossUsd = 0.0,
        expectancyUsd = 0.0,
        periodDurationFa = "داده تاریخی کافی نیست",
        inSampleWinRate = 0.0,
        outOfSampleWinRate = 0.0,
        inSampleNetProfitPercent = 0.0,
        outOfSampleNetProfitPercent = 0.0,
        trades = emptyList(),
        equityCurve = listOf(EquityPoint(candles.firstOrNull()?.time ?: System.currentTimeMillis(), initialCapitalUsd))
    )
}
