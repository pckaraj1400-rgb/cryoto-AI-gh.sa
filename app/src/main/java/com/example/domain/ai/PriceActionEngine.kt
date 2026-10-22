package com.example.domain.ai

import com.example.data.model.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * موتور تحلیلی پرایس اکشن (Price Action Engine) بر پایه قوانین قطعی و کندل‌های بسته‌شده.
 * الگوهای منفرد هرگز تضمین موفقیت تلقی نمی‌شوند.
 */
object PriceActionEngine {

    data class OrderBlockInfo(
        val type: String, // "BULLISH_OB" or "BEARISH_OB" or "NONE"
        val minPrice: Double,
        val maxPrice: Double,
        val descriptionFa: String
    )

    data class FvgInfo(
        val type: String, // "BULLISH_FVG" or "BEARISH_FVG" or "NONE"
        val minPrice: Double,
        val maxPrice: Double,
        val descriptionFa: String
    )

    data class Report(
        val valid: Boolean,
        val marketStructureFa: String,
        val swingHigh: Double,
        val previousSwingHigh: Double,
        val swingLow: Double,
        val previousSwingLow: Double,
        val structureBreakFa: String,
        val candlePatternFa: String,
        val liquiditySweepFa: String,
        val breakoutQualityFa: String,
        val orderBlockFa: String,
        val orderBlockMin: Double,
        val orderBlockMax: Double,
        val fairValueGapFa: String,
        val fvgMin: Double,
        val fvgMax: Double,
        val equalHighsLowsFa: String,
        val momentumCandleFa: String,
        val confirmationConditionFa: String,
        val invalidationConditionFa: String,
        val bullishScore: Int,
        val bearishScore: Int,
        val bullishEvidence: Boolean,
        val bearishEvidence: Boolean,
        val notes: List<String>
    )

    fun analyze(input: List<Candle>): Report {
        val candles = input.filter { it.isClosed && validCandle(it) }.sortedBy { it.time }
        if (candles.size < 30) return Report(
            valid = false,
            marketStructureFa = "داده بسته‌شده کافی نیست",
            swingHigh = 0.0,
            previousSwingHigh = 0.0,
            swingLow = 0.0,
            previousSwingLow = 0.0,
            structureBreakFa = "برای تحلیل ساختار بازار حداقل ۳۰ کندل بسته‌شده لازم است",
            candlePatternFa = "نامشخص",
            liquiditySweepFa = "نامشخص",
            breakoutQualityFa = "داده ناکافی",
            orderBlockFa = "نامشخص",
            orderBlockMin = 0.0,
            orderBlockMax = 0.0,
            fairValueGapFa = "نامشخص",
            fvgMin = 0.0,
            fvgMax = 0.0,
            equalHighsLowsFa = "نامشخص",
            momentumCandleFa = "نامشخص",
            confirmationConditionFa = "نیاز به دریافت کندل‌های بیشتر",
            invalidationConditionFa = "داده ناکافی",
            bullishScore = 0,
            bearishScore = 0,
            bullishEvidence = false,
            bearishEvidence = false,
            notes = listOf("تحلیل پرایس اکشن به‌دلیل نبود داده کافی انجام نشد")
        )

        val start = maxOf(2, candles.size - 80)
        val highs = mutableListOf<Pair<Int, Double>>()
        val lows = mutableListOf<Pair<Int, Double>>()
        for (i in start until candles.size - 2) {
            val c = candles[i]
            if ((i - 2..i + 2).all { j -> j == i || c.high >= candles[j].high }) highs += i to c.high
            if ((i - 2..i + 2).all { j -> j == i || c.low <= candles[j].low }) lows += i to c.low
        }
        val recentHighs = highs.takeLast(2).map { it.second }
        val recentLows = lows.takeLast(2).map { it.second }
        val swingHigh = recentHighs.lastOrNull() ?: candles.takeLast(30).maxOf { it.high }
        val previousSwingHigh = recentHighs.getOrNull(recentHighs.lastIndex - 1) ?: swingHigh
        val swingLow = recentLows.lastOrNull() ?: candles.takeLast(30).minOf { it.low }
        val previousSwingLow = recentLows.getOrNull(recentLows.lastIndex - 1) ?: swingLow

        val higherHigh = recentHighs.size >= 2 && recentHighs.last() > recentHighs.first()
        val lowerHigh = recentHighs.size >= 2 && recentHighs.last() < recentHighs.first()
        val higherLow = recentLows.size >= 2 && recentLows.last() > recentLows.first()
        val lowerLow = recentLows.size >= 2 && recentLows.last() < recentLows.first()

        // Equal Highs / Equal Lows (Liquidity pools)
        val hasEqualHighs = recentHighs.size >= 2 && abs(recentHighs.last() - recentHighs.first()) / recentHighs.first() < 0.0025
        val hasEqualLows = recentLows.size >= 2 && abs(recentLows.last() - recentLows.first()) / recentLows.first() < 0.0025
        val equalHighsLowsFa = when {
            hasEqualHighs && hasEqualLows -> "سقف‌ها و کف‌های برابر هم‌زمان (رنج با استخر نقدینگی دوطرفه)"
            hasEqualHighs -> "سقف‌های برابر (EQH)؛ استخر نقدینگی Buy-side بالای سطح"
            hasEqualLows -> "کف‌های برابر (EQL)؛ استخر نقدینگی Sell-side زیر سطح"
            else -> "سقف و کف برابر بارزی در نوسانات اخیر مشاهده نشد"
        }

        val structure = when {
            higherHigh && higherLow -> "ساختار صعودی (HH/HL)"
            lowerHigh && lowerLow -> "ساختار نزولی (LH/LL)"
            else -> "ساختار خنثی یا رنج؛ سقف/کف‌های اخیر هم‌جهت نیستند"
        }

        val last = candles.last()
        val previous = candles[candles.lastIndex - 1]
        val body = abs(last.close - last.open).coerceAtLeast(1e-12)
        val range = (last.high - last.low).coerceAtLeast(1e-12)
        val upperWick = last.high - maxOf(last.open, last.close)
        val lowerWick = minOf(last.open, last.close) - last.low

        // Candle Pattern
        val pattern = when {
            previous.close < previous.open && last.close > last.open && last.close >= previous.open && last.open <= previous.close -> "انگالفینگ صعودی (Bullish Engulfing)"
            previous.close > previous.open && last.close < last.open && last.close <= previous.open && last.open >= previous.close -> "انگالفینگ نزولی (Bearish Engulfing)"
            body / range <= 0.35 && lowerWick >= body * 2.0 && last.close >= last.low + range * 0.70 -> "پین‌بار صعودی / رد قیمت از کف (Bullish Pinbar)"
            body / range <= 0.35 && upperWick >= body * 2.0 && last.close <= last.low + range * 0.30 -> "پین‌بار نزولی / رد قیمت از سقف (Bearish Pinbar)"
            else -> "الگوی کندلی منفرد شاخصی ثبت نشد"
        }
        val bullishPattern = pattern.contains("صعودی")
        val bearishPattern = pattern.contains("نزولی")

        // Momentum candle detection
        val recentRanges = candles.takeLast(15).map { it.high - it.low }
        val avgRange = if (recentRanges.isNotEmpty()) recentRanges.average() else range
        val isMomentum = body >= range * 0.65 && range >= avgRange * 1.5
        val momentumCandleFa = when {
            isMomentum && last.close > last.open -> "کندل اسپایک مومنتوم پرقدرت صعودی"
            isMomentum && last.close < last.open -> "کندل اسپایک مومنتوم پرقدرت نزولی"
            else -> "مومنتوم کندل‌ها در محدوده میانگین است"
        }

        // Liquidity Sweep
        val sweptHigh = last.high > swingHigh && last.close < swingHigh
        val sweptLow = last.low < swingLow && last.close > swingLow
        val sweep = when {
            sweptLow -> "جمع‌آوری احتمالی نقدینگی زیر کف و بسته‌شدن مجدد بالای سطح (Bullish Sweep)"
            sweptHigh -> "جمع‌آوری احتمالی نقدینگی بالای سقف و بسته‌شدن زیر سطح (Bearish Sweep)"
            else -> "شکست نقدینگی واضحی در آخرین کندل بسته‌شده دیده نشد"
        }

        val avgVolume = candles.takeLast(20).map { it.volume }.average()
        val highVolume = avgVolume > 0.0 && last.volume >= avgVolume * 1.3
        val breakout = when {
            last.close > swingHigh && highVolume -> "شکست صعودی با کلوز بالای سقف و حجم بالاتر از میانگین"
            last.close > swingHigh -> "کلوز بالای سقف بدون تأیید قوی حجم؛ نیازمند پولبک/تأیید"
            last.high > swingHigh && last.close <= swingHigh -> "احتمال شکست جعلی صعودی؛ نفوذ به سقف بدون کلوز بالای آن"
            last.close < swingLow && highVolume -> "شکست نزولی با کلوز زیر کف و حجم بالاتر از میانگین"
            last.close < swingLow -> "کلوز زیر کف بدون تأیید قوی حجم؛ نیازمند تأیید"
            last.low < swingLow && last.close >= swingLow -> "احتمال شکست جعلی نزولی؛ نفوذ به کف و برگشت بالای آن"
            else -> "قیمت داخل محدوده سقف و کف اخیر است"
        }

        val bullishBreak = last.close > swingHigh
        val bearishBreak = last.close < swingLow
        val structureBreak = when {
            bullishBreak && lowerHigh && lowerLow -> "CHoCH صعودی احتمالی: کلوز بالای سقف ساختاری در روند نزولی"
            bullishBreak -> "BOS صعودی: کلوز کندل بسته‌شده بالای آخرین سقف نوسانی"
            bearishBreak && higherHigh && higherLow -> "CHoCH نزولی احتمالی: کلوز زیر کف ساختاری در روند صعودی"
            bearishBreak -> "BOS نزولی: کلوز کندل بسته‌شده زیر آخرین کف نوسانی"
            sweptLow -> "احتمال تغییر کوتاه‌مدت پس از sweep کف؛ شکست ساختار هنوز تأیید نشده"
            sweptHigh -> "احتمال تغییر کوتاه‌مدت پس از sweep سقف؛ شکست ساختار هنوز تأیید نشده"
            else -> "BOS/CHoCH تأییدشده‌ای در آخرین کلوز دیده نشد"
        }

        // Order Block (OB) Detection
        val ob = detectOrderBlock(candles)

        // Fair Value Gap (FVG) Detection
        val fvg = detectFairValueGap(candles)

        // Confirmation and Invalidation Conditions
        val confirmationFa = when {
            bullishBreak || (higherHigh && higherLow) -> "تثبیت حداقل یک کندل بسته‌شده بالای ${String.format(java.util.Locale.US, "%.2f", swingHigh)} یا پولبک بدون شکست به ${String.format(java.util.Locale.US, "%.2f", ob.maxPrice.takeIf { it > 0 } ?: swingLow)}"
            bearishBreak || (lowerHigh && lowerLow) -> "تثبیت کندل بسته‌شده زیر ${String.format(java.util.Locale.US, "%.2f", swingLow)} یا پولبک به محدوده عرضه"
            else -> "خروج قیمت با کندل مومنتوم از محدوده رنج بین ${String.format(java.util.Locale.US, "%.2f", swingLow)} و ${String.format(java.util.Locale.US, "%.2f", swingHigh)}"
        }

        val invalidationFa = when {
            bullishBreak || (higherHigh && higherLow) -> "کلوز کندل زیر سطح ${String.format(java.util.Locale.US, "%.2f", swingLow)} ساختار را کاملاً باطل می‌کند"
            bearishBreak || (lowerHigh && lowerLow) -> "کلوز کندل بالای سطح ${String.format(java.util.Locale.US, "%.2f", swingHigh)} ساختار نزولی را باطل می‌کند"
            else -> "نفوذ و کلوز خارج از محدوده رنج معتبر جاری"
        }

        var bull = 0
        var bear = 0
        if (higherHigh && higherLow) bull += 2
        if (lowerHigh && lowerLow) bear += 2
        if (bullishBreak || sweptLow) bull += 2
        if (bearishBreak || sweptHigh) bear += 2
        if (bullishPattern) bull += 1
        if (bearishPattern) bear += 1
        if (highVolume && bullishBreak) bull += 1
        if (highVolume && bearishBreak) bear += 1
        if (ob.type == "BULLISH_OB") bull += 1
        if (ob.type == "BEARISH_OB") bear += 1
        if (fvg.type == "BULLISH_FVG") bull += 1
        if (fvg.type == "BEARISH_FVG") bear += 1

        val notes = buildList {
            add("ساختار: $structure")
            add("کندل: $pattern")
            add("نقدینگی: $sweep")
            add("شکست: $breakout")
            add("اردر بلاک: ${ob.descriptionFa}")
            add("شکاف ارزش منصفانه (FVG): ${fvg.descriptionFa}")
            add("استخرهای نقدینگی: $equalHighsLowsFa")
        }

        return Report(
            valid = true,
            marketStructureFa = structure,
            swingHigh = swingHigh,
            previousSwingHigh = previousSwingHigh,
            swingLow = swingLow,
            previousSwingLow = previousSwingLow,
            structureBreakFa = structureBreak,
            candlePatternFa = pattern,
            liquiditySweepFa = sweep,
            breakoutQualityFa = breakout,
            orderBlockFa = ob.descriptionFa,
            orderBlockMin = ob.minPrice,
            orderBlockMax = ob.maxPrice,
            fairValueGapFa = fvg.descriptionFa,
            fvgMin = fvg.minPrice,
            fvgMax = fvg.maxPrice,
            equalHighsLowsFa = equalHighsLowsFa,
            momentumCandleFa = momentumCandleFa,
            confirmationConditionFa = confirmationFa,
            invalidationConditionFa = invalidationFa,
            bullishScore = min(100, bull * 12),
            bearishScore = min(100, bear * 12),
            bullishEvidence = bullishBreak || sweptLow || bullishPattern || (higherHigh && higherLow),
            bearishEvidence = bearishBreak || sweptHigh || bearishPattern || (lowerHigh && lowerLow),
            notes = notes
        )
    }

    private fun detectOrderBlock(candles: List<Candle>): OrderBlockInfo {
        if (candles.size < 5) return OrderBlockInfo("NONE", 0.0, 0.0, "شناسایی نشد")
        val n = candles.size

        // Search backward for the most recent order block in the last 20 candles
        for (i in (n - 2) downTo max(1, n - 20)) {
            val c = candles[i]
            val next = candles[i + 1]

            // Bullish Order Block: Last bearish candle before strong displacement upward
            if (c.close < c.open && next.close > next.open && next.close > c.high) {
                return OrderBlockInfo(
                    type = "BULLISH_OB",
                    minPrice = c.low,
                    maxPrice = c.high,
                    descriptionFa = "اردر بلاک صعودی (Bullish OB) در محدوده ${String.format(java.util.Locale.US, "%.2f", c.low)} تا ${String.format(java.util.Locale.US, "%.2f", c.high)}"
                )
            }

            // Bearish Order Block: Last bullish candle before strong displacement downward
            if (c.close > c.open && next.close < next.open && next.close < c.low) {
                return OrderBlockInfo(
                    type = "BEARISH_OB",
                    minPrice = c.low,
                    maxPrice = c.high,
                    descriptionFa = "اردر بلاک نزولی (Bearish OB) در محدوده ${String.format(java.util.Locale.US, "%.2f", c.low)} تا ${String.format(java.util.Locale.US, "%.2f", c.high)}"
                )
            }
        }
        return OrderBlockInfo("NONE", 0.0, 0.0, "اردر بلاک فعالی در محدوده نوسانات اخیر مشاهده نشد")
    }

    private fun detectFairValueGap(candles: List<Candle>): FvgInfo {
        if (candles.size < 4) return FvgInfo("NONE", 0.0, 0.0, "شناسایی نشد")
        val n = candles.size

        // Look back 15 candles for unfilled 3-candle imbalance
        for (i in (n - 1) downTo max(2, n - 15)) {
            val c1 = candles[i - 2]
            val c3 = candles[i]

            // Bullish FVG: Low of candle 3 is higher than High of candle 1
            if (c3.low > c1.high) {
                return FvgInfo(
                    type = "BULLISH_FVG",
                    minPrice = c1.high,
                    maxPrice = c3.low,
                    descriptionFa = "شکاف ارزش منصفانه صعودی (Bullish FVG) در بازه ${String.format(java.util.Locale.US, "%.2f", c1.high)} تا ${String.format(java.util.Locale.US, "%.2f", c3.low)}"
                )
            }

            // Bearish FVG: High of candle 3 is lower than Low of candle 1
            if (c3.high < c1.low) {
                return FvgInfo(
                    type = "BEARISH_FVG",
                    minPrice = c3.high,
                    maxPrice = c1.low,
                    descriptionFa = "شکاف ارزش منصفانه نزولی (Bearish FVG) در بازه ${String.format(java.util.Locale.US, "%.2f", c3.high)} تا ${String.format(java.util.Locale.US, "%.2f", c1.low)}"
                )
            }
        }
        return FvgInfo("NONE", 0.0, 0.0, "عدم وجود FVG باز در کندل‌های نزدیک")
    }

    private fun validCandle(c: Candle): Boolean = c.time > 0 && c.open.isFinite() && c.high.isFinite() &&
        c.low.isFinite() && c.close.isFinite() && c.volume.isFinite() && c.open > 0.0 && c.low > 0.0 &&
        c.high >= maxOf(c.open, c.close) && c.low <= minOf(c.open, c.close)
}
