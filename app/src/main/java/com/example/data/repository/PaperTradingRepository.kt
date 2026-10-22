package com.example.data.repository

import com.example.data.model.MarginType
import com.example.data.model.MarketType
import com.example.data.model.PaperAccount
import com.example.data.model.PaperPosition
import com.example.data.model.PaperTradeRecord
import com.example.data.model.RiskSettings
import com.example.data.model.TradeSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

class PaperTradingRepository(
    private val storageManager: TradingStorageManager? = null
) {

    private val _account = MutableStateFlow(storageManager?.loadAccount() ?: PaperAccount())
    val account = _account.asStateFlow()

    private val _openPositions = MutableStateFlow<List<PaperPosition>>(storageManager?.loadOpenPositions() ?: emptyList())
    val openPositions = _openPositions.asStateFlow()

    private val _tradeHistory = MutableStateFlow<List<PaperTradeRecord>>(storageManager?.loadTradeHistory() ?: emptyList())
    val tradeHistory = _tradeHistory.asStateFlow()

    private fun persistState() {
        storageManager?.let { sm ->
            sm.saveAccount(_account.value)
            sm.saveOpenPositions(_openPositions.value)
            sm.saveTradeHistory(_tradeHistory.value)
        }
    }

    fun resetAccount(initialBalance: Double = 10_000.0) {
        _account.value = PaperAccount(
            initialBalanceUsd = initialBalance,
            availableBalanceUsd = initialBalance,
            totalEquityUsd = initialBalance,
            realizedPnLUsd = 0.0,
            unrealizedPnLUsd = 0.0,
            maxDrawdownPercent = 0.0,
            winRatePercent = 0.0,
            totalTradesCount = 0,
            winningTradesCount = 0,
            losingTradesCount = 0
        )
        _openPositions.value = emptyList()
        _tradeHistory.value = emptyList()
        persistState()
    }

    fun openPosition(
        symbol: String,
        marketType: MarketType,
        direction: String, // "LONG" or "SHORT" or "SPOT_BUY"
        entryPrice: Double,
        amount: Double,
        leverage: Int = 1,
        marginType: MarginType = MarginType.ISOLATED,
        stopLoss: Double? = null,
        tp1: Double? = null,
        tp2: Double? = null,
        tp3: Double? = null
    ): Result<PaperPosition> {
        if (!entryPrice.isFinite() || !amount.isFinite() || entryPrice <= 0.0 || amount <= 0.0) {
            return Result.failure(IllegalArgumentException("قیمت ورود و حجم باید اعداد مثبت و معتبر باشند."))
        }
        if (leverage < 1 || leverage > 125) {
            return Result.failure(IllegalArgumentException("اهرم انتخاب‌شده معتبر نیست."))
        }
        val normalizedDirection = if (marketType == MarketType.SPOT && direction == "LONG") "SPOT_BUY" else direction
        if (marketType == MarketType.SPOT && (normalizedDirection != "SPOT_BUY" || leverage != 1)) {
            return Result.failure(IllegalArgumentException("در بازار اسپات فقط خرید بدون اهرم پشتیبانی می‌شود."))
        }
        if (marketType == MarketType.FUTURES_USDT_M && normalizedDirection !in setOf("LONG", "SHORT")) {
            return Result.failure(IllegalArgumentException("جهت معامله فیوچرز باید LONG یا SHORT باشد."))
        }
        if (normalizedDirection == "LONG" || normalizedDirection == "SPOT_BUY") {
            if (stopLoss != null && (!stopLoss.isFinite() || stopLoss <= 0.0 || stopLoss >= entryPrice)) {
                return Result.failure(IllegalArgumentException("حد ضرر خرید باید پایین‌تر از قیمت ورود باشد."))
            }
            val targets = listOfNotNull(tp1, tp2, tp3)
            if (targets.any { !it.isFinite() || it <= entryPrice } || targets.zipWithNext().any { it.first >= it.second }) {
                return Result.failure(IllegalArgumentException("اهداف خرید باید معتبر و به ترتیب بالاتر از قیمت ورود باشند."))
            }
        } else {
            if (stopLoss != null && (!stopLoss.isFinite() || stopLoss <= entryPrice)) {
                return Result.failure(IllegalArgumentException("حد ضرر فروش باید بالاتر از قیمت ورود باشد."))
            }
            val targets = listOfNotNull(tp1, tp2, tp3)
            if (targets.any { !it.isFinite() || it <= 0.0 || it >= entryPrice } || targets.zipWithNext().any { it.first <= it.second }) {
                return Result.failure(IllegalArgumentException("اهداف فروش باید معتبر و به ترتیب پایین‌تر از قیمت ورود باشند."))
            }
        }
        val notionalValue = entryPrice * amount
        val requiredMargin = if (marketType == MarketType.FUTURES_USDT_M) {
            notionalValue / leverage
        } else {
            notionalValue
        }

        val fee = notionalValue * 0.0005 // 0.05% entry fee, charged once

        if (_account.value.availableBalanceUsd < (requiredMargin + fee)) {
            return Result.failure(Exception("موجودی حساب آزمایشی برای این سفارش کافی نیست. مارجین موردنیاز: $${String.format(java.util.Locale.US, "%.2f", requiredMargin)}"))
        }

        // Calculate liquidation price for Futures
        var liqPrice: Double? = null
        if (marketType == MarketType.FUTURES_USDT_M && leverage > 1) {
            val mmr = 0.005
            liqPrice = if (normalizedDirection == "LONG") {
                entryPrice * (1.0 - (1.0 / leverage) + mmr)
            } else {
                entryPrice * (1.0 + (1.0 / leverage) - mmr)
            }
        }

        val newPosition = PaperPosition(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            marketType = marketType,
            direction = normalizedDirection,
            entryPrice = entryPrice,
            currentPrice = entryPrice,
            amount = amount,
            notionalValueUsd = notionalValue,
            leverage = leverage,
            marginType = marginType,
            marginUsd = requiredMargin,
            stopLoss = stopLoss,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            takeProfit3 = tp3,
            liquidationPrice = liqPrice,
            // Entry fee is deducted from available balance below; don't subtract it a second time as unrealized PnL.
            unrealizedPnLUsd = 0.0,
            pnlPercent = 0.0,
            entryFeeUsd = fee,
            openTime = System.currentTimeMillis()
        )

        // Deduct margin + fee
        val curAccount = _account.value
        _account.value = curAccount.copy(
            availableBalanceUsd = curAccount.availableBalanceUsd - requiredMargin - fee,
            totalEquityUsd = curAccount.totalEquityUsd - fee
        )

        _openPositions.value = _openPositions.value + newPosition
        persistState()
        return Result.success(newPosition)
    }

    // Direct 1-Click transfer from TradeSignal to Paper Trading!
    fun openFromSignal(signal: TradeSignal, accountBalanceFraction: Double = 0.2): Result<PaperPosition> {
        if (!signal.action.isTradeable || signal.entryPrice <= 0.0 || !signal.entryPrice.isFinite()) {
            return Result.failure(IllegalArgumentException("سیگنال قابل اجرا نیست یا قیمت ورود معتبر ندارد."))
        }
        val entry = signal.entryPrice
        val amount = if (signal.suggestedPositionSizeUsd > 0) {
            signal.suggestedPositionSizeUsd / entry
        } else {
            (_account.value.availableBalanceUsd * accountBalanceFraction) / entry
        }

        val direction = if (signal.marketType == MarketType.SPOT) {
            "SPOT_BUY"
        } else if (signal.isLong) {
            "LONG"
        } else {
            "SHORT"
        }

        return openPosition(
            symbol = signal.symbol,
            marketType = signal.marketType,
            direction = direction,
            entryPrice = entry,
            amount = amount,
            leverage = if (signal.marketType == MarketType.FUTURES_USDT_M) signal.recommendedLeverage else 1,
            marginType = MarginType.ISOLATED,
            stopLoss = signal.stopLoss,
            tp1 = signal.takeProfit1,
            tp2 = signal.takeProfit2,
            tp3 = signal.takeProfit3
        )
    }

    fun closePosition(positionId: String, exitPrice: Double, exitReason: String = "دستی"): Boolean {
        val pos = _openPositions.value.find { it.id == positionId } ?: return false
        if (!exitPrice.isFinite() || exitPrice <= 0.0) return false
        val isLong = pos.direction == "LONG" || pos.direction == "SPOT_BUY"

        val pnlUsd = if (isLong) {
            pos.amount * (exitPrice - pos.entryPrice)
        } else {
            pos.amount * (pos.entryPrice - exitPrice)
        }

        val closeFee = (exitPrice * pos.amount) * 0.0005
        val accountPnlAtClose = pnlUsd - closeFee
        val netPnlAfterAllFees = pnlUsd - closeFee - pos.entryFeeUsd
        val pnlPercent = if (pos.marginUsd > 0) (netPnlAfterAllFees / pos.marginUsd) * 100.0 else 0.0

        val tradeRecord = PaperTradeRecord(
            id = UUID.randomUUID().toString(),
            symbol = pos.symbol,
            marketType = pos.marketType,
            direction = pos.direction,
            entryPrice = pos.entryPrice,
            exitPrice = exitPrice,
            exitReason = exitReason,
            amount = pos.amount,
            pnlUsd = netPnlAfterAllFees,
            pnlPercent = pnlPercent,
            feeUsd = closeFee + pos.entryFeeUsd,
            openTime = pos.openTime,
            closeTime = System.currentTimeMillis(),
            isWin = netPnlAfterAllFees > 0
        )

        // Return margin + netPnL to account
        val curAcc = _account.value
        // Entry fee was already deducted when the position opened; only gross PnL minus exit fee changes cash now.
        val newAvailable = curAcc.availableBalanceUsd + pos.marginUsd + accountPnlAtClose
        val newRealized = curAcc.realizedPnLUsd + netPnlAfterAllFees
        val newTrades = _tradeHistory.value + tradeRecord
        val winCount = newTrades.count { it.isWin }
        val winRate = (winCount.toDouble() / newTrades.size) * 100.0

        _openPositions.value = _openPositions.value.filter { it.id != positionId }
        _tradeHistory.value = newTrades

        _account.value = curAcc.copy(
            availableBalanceUsd = max(0.0, newAvailable),
            realizedPnLUsd = newRealized,
            totalTradesCount = newTrades.size,
            winningTradesCount = winCount,
            losingTradesCount = newTrades.size - winCount,
            winRatePercent = winRate
        )

        recalculateAccountEquity()
        persistState()
        return true
    }

    private fun closePartialPosition(positionId: String, exitPrice: Double, closeAmount: Double, reason: String): Boolean {
        val pos = _openPositions.value.find { it.id == positionId } ?: return false
        if (!exitPrice.isFinite() || exitPrice <= 0.0 || !closeAmount.isFinite() || closeAmount <= 0.0) return false
        val amountToClose = minOf(closeAmount, pos.amount)
        if (amountToClose <= 0.0) return false
        val fraction = amountToClose / pos.amount
        val marginReleased = pos.marginUsd * fraction
        val entryFeeAllocated = pos.entryFeeUsd * fraction
        val isLong = pos.direction == "LONG" || pos.direction == "SPOT_BUY"
        val grossPnl = if (isLong) amountToClose * (exitPrice - pos.entryPrice) else amountToClose * (pos.entryPrice - exitPrice)
        val closeFee = exitPrice * amountToClose * 0.0005
        val cashPnl = grossPnl - closeFee
        val netPnl = grossPnl - closeFee - entryFeeAllocated
        val pnlPercent = if (marginReleased > 0.0) netPnl / marginReleased * 100.0 else 0.0
        val record = PaperTradeRecord(
            id = UUID.randomUUID().toString(), symbol = pos.symbol, marketType = pos.marketType,
            direction = pos.direction, entryPrice = pos.entryPrice, exitPrice = exitPrice,
            exitReason = reason, amount = amountToClose, pnlUsd = netPnl, pnlPercent = pnlPercent,
            feeUsd = closeFee + entryFeeAllocated, openTime = pos.openTime,
            closeTime = System.currentTimeMillis(), isWin = netPnl > 0.0
        )
        val cur = _account.value
        val newHistory = _tradeHistory.value + record
        val wins = newHistory.count { it.isWin }
        val remainingAmount = (pos.amount - amountToClose).coerceAtLeast(0.0)
        val remainingPosition = if (remainingAmount <= pos.initialAmount * 1e-9) null else pos.copy(
            amount = remainingAmount,
            notionalValueUsd = remainingAmount * pos.entryPrice,
            marginUsd = (pos.marginUsd - marginReleased).coerceAtLeast(0.0),
            entryFeeUsd = (pos.entryFeeUsd - entryFeeAllocated).coerceAtLeast(0.0),
            tp1Triggered = pos.tp1Triggered || reason == "هدف اول (TP1)",
            tp2Triggered = pos.tp2Triggered || reason == "هدف دوم (TP2)"
        )
        _openPositions.value = _openPositions.value.filterNot { it.id == positionId } + listOfNotNull(remainingPosition)
        _tradeHistory.value = newHistory
        _account.value = cur.copy(
            availableBalanceUsd = max(0.0, cur.availableBalanceUsd + marginReleased + cashPnl),
            realizedPnLUsd = cur.realizedPnLUsd + netPnl,
            totalTradesCount = newHistory.size,
            winningTradesCount = wins,
            losingTradesCount = newHistory.size - wins,
            winRatePercent = wins.toDouble() / newHistory.size * 100.0
        )
        recalculateAccountEquity()
        persistState()
        return true
    }

    fun updateLivePrices(priceMap: Map<String, Double>) {
        if (_openPositions.value.isEmpty()) return
        // Work from a snapshot because closing a target mutates the StateFlow collection.
        for (snapshot in _openPositions.value.toList()) {
            var pos = _openPositions.value.find { it.id == snapshot.id } ?: continue
            val livePrice = priceMap[pos.symbol] ?: continue
            if (!livePrice.isFinite() || livePrice <= 0.0) continue
            val isLong = pos.direction == "LONG" || pos.direction == "SPOT_BUY"

            val stopPrice = pos.stopLoss
            val stopHit = stopPrice != null && (if (isLong) livePrice <= stopPrice else livePrice >= stopPrice)
            if (stopHit) {
                closePosition(pos.id, stopPrice!!, "حد ضرر (SL)")
                continue
            }

            // Exit 40% at TP1, 30% at TP2, and the remaining position at TP3.
            val tp1Price = pos.takeProfit1
            val tp1Hit = tp1Price != null && (if (isLong) livePrice >= tp1Price else livePrice <= tp1Price)
            if (tp1Hit && !pos.tp1Triggered) {
                closePartialPosition(pos.id, tp1Price!!, minOf(pos.amount, pos.initialAmount * 0.40), "هدف اول (TP1)")
                pos = _openPositions.value.find { it.id == snapshot.id } ?: continue
            }

            val tp2Price = pos.takeProfit2
            val tp2Hit = tp2Price != null && (if (isLong) livePrice >= tp2Price else livePrice <= tp2Price)
            if (tp2Hit && !pos.tp2Triggered) {
                closePartialPosition(pos.id, tp2Price!!, minOf(pos.amount, pos.initialAmount * 0.30), "هدف دوم (TP2)")
                pos = _openPositions.value.find { it.id == snapshot.id } ?: continue
            }

            val tp3Price = pos.takeProfit3
            val tp3Hit = tp3Price != null && (if (isLong) livePrice >= tp3Price else livePrice <= tp3Price)
            if (tp3Hit) {
                closePosition(pos.id, tp3Price!!, "هدف سوم (TP3)")
                continue
            }
        }

        _openPositions.value = _openPositions.value.map { pos ->
            val livePrice = priceMap[pos.symbol] ?: return@map pos
            if (!livePrice.isFinite() || livePrice <= 0.0) return@map pos
            val isLong = pos.direction == "LONG" || pos.direction == "SPOT_BUY"
            val pnl = if (isLong) pos.amount * (livePrice - pos.entryPrice) else pos.amount * (pos.entryPrice - livePrice)
            val pnlPct = if (pos.marginUsd > 0.0) pnl / pos.marginUsd * 100.0 else 0.0
            pos.copy(currentPrice = livePrice, unrealizedPnLUsd = pnl, pnlPercent = pnlPct)
        }
        recalculateAccountEquity()
    }

    private fun recalculateAccountEquity() {
        val totalUnrealized = _openPositions.value.sumOf { it.unrealizedPnLUsd }
        val lockedMargin = _openPositions.value.sumOf { it.marginUsd }
        val acc = _account.value
        val equity = acc.availableBalanceUsd + lockedMargin + totalUnrealized

        val peak = max(acc.initialBalanceUsd, equity)
        val drawdown = if (peak > 0) ((peak - equity) / peak) * 100.0 else 0.0

        _account.value = acc.copy(
            totalEquityUsd = equity,
            unrealizedPnLUsd = totalUnrealized,
            maxDrawdownPercent = max(acc.maxDrawdownPercent, drawdown)
        )
        persistState()
    }
}
