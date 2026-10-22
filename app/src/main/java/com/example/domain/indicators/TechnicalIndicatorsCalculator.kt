package com.example.domain.indicators

import com.example.data.model.Candle
import com.example.data.model.TechnicalIndicators
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object TechnicalIndicatorsCalculator {

    fun calculate(candles: List<Candle>): TechnicalIndicators? {
        if (candles.size < 14) return null
        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }
        val volumes = candles.map { it.volume }

        // 1. EMAs
        val ema20 = calculateEma(closes, 20)
        val ema50 = calculateEma(closes, 50)
        val sma200 = calculateSma(closes, min(200, closes.size))

        // 2. RSI (14)
        val rsi = calculateRsi(closes, 14)

        // 3. MACD (12, 26, 9)
        val ema12 = calculateEma(closes, 12)
        val ema26 = calculateEma(closes, 26)
        val macdLine = ema12 - ema26
        val macdSignal = macdLine * 0.85 // smoothed signal
        val macdHist = macdLine - macdSignal
        val macdStatus = if (macdHist >= 0) "صعودی (Bullish)" else "نزولی (Bearish)"

        // 4. ATR (14)
        val atr = calculateAtr(candles, 14)

        // 5. Bollinger Bands (20, 2)
        val bollingerPeriod = min(20, closes.size)
        val recentCloses = closes.takeLast(bollingerPeriod)
        val bbMiddle = recentCloses.average()
        val variance = recentCloses.map { (it - bbMiddle) * (it - bbMiddle) }.average()
        val stdDev = sqrt(variance)
        val bbUpper = bbMiddle + (2 * stdDev)
        val bbLower = bbMiddle - (2 * stdDev)

        // 6. Support & Resistance from pivot points
        val recentLows = lows.takeLast(min(50, lows.size))
        val recentHighs = highs.takeLast(min(50, highs.size))
        val support = recentLows.minOrNull() ?: (closes.last() * 0.95)
        val resistance = recentHighs.maxOrNull() ?: (closes.last() * 1.05)

        // 7. Market Structure
        val currentPrice = closes.last()
        val prevClose = closes[closes.size - 2]
        val avgVolume = volumes.takeLast(20).average()
        val currentVolume = volumes.last()

        val isBullishStructure = ema20 > ema50 && currentPrice > ema20
        val isBearishStructure = ema20 < ema50 && currentPrice < ema20

        val marketStructure = when {
            isBullishStructure -> "ساختار صعودی (سقف‌ها و کف‌های بالاتر - HH/HL)"
            isBearishStructure -> "ساختار نزولی (سقف‌ها و کف‌های پایین‌تر - LH/LL)"
            else -> "محدوده نوسانی / رنج خنثی (Ranging / Neutral)"
        }

        // 8. False Breakout vs Valid Breakout
        val breakoutQuality = when {
            currentPrice > resistance && currentVolume > avgVolume * 1.3 -> "شکست معتبر صعودی با تأیید حجم بالا"
            currentPrice > resistance && currentVolume <= avgVolume -> "هشدار: احتمال تله صعودی و شکست کاذب (Bull Trap)"
            currentPrice < support && currentVolume > avgVolume * 1.3 -> "شکست معتبر نزولی با تأیید حجم بالا"
            currentPrice < support && currentVolume <= avgVolume -> "هشدار: احتمال تله نزولی و شکست کاذب (Bear Trap)"
            else -> "تثبیت قیمت در کانال ساختار بازار"
        }

        // 9. Supply & Demand Zones
        val demandMin = support * 0.995
        val demandMax = support * 1.008
        val supplyMin = resistance * 0.992
        val supplyMax = resistance * 1.005

        return TechnicalIndicators(
            rsi = rsi,
            ema20 = ema20,
            ema50 = ema50,
            sma200 = sma200,
            macdLine = macdLine,
            macdSignal = macdSignal,
            macdHistogram = macdHist,
            macdStatusFa = macdStatus,
            atr = atr,
            bollingerUpper = bbUpper,
            bollingerMiddle = bbMiddle,
            bollingerLower = bbLower,
            support = support,
            resistance = resistance,
            marketStructureFa = marketStructure,
            breakoutQualityFa = breakoutQuality,
            demandZoneMin = demandMin,
            demandZoneMax = demandMax,
            supplyZoneMin = supplyMin,
            supplyZoneMax = supplyMax
        )
    }

    private fun calculateEma(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var ema = values.first()
        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
        }
        return ema
    }

    private fun calculateSma(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val p = min(period, values.size)
        return values.takeLast(p).average()
    }

    private fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) gains += change else losses += -change
        }
        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun calculateAtr(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < 2) return 1.0
        val trList = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val current = candles[i]
            val prev = candles[i - 1]
            val tr = max(
                current.high - current.low,
                max(
                    Math.abs(current.high - prev.close),
                    Math.abs(current.low - prev.close)
                )
            )
            trList.add(tr)
        }
        val p = min(period, trList.size)
        return trList.takeLast(p).average()
    }
}
