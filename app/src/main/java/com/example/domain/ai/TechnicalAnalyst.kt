package com.example.domain.ai

import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.MarketType
import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators
import com.example.domain.indicators.TechnicalIndicatorsCalculator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * هوش مصنوعی دوم: تحلیلگر تکنیکال (Technical AI Analyst)
 *
 * مستقل از نتایج بنیادی، نمودار و ساختار بازار را بر اساس کندل‌های بسته‌شده تحلیل می‌کند.
 * داده‌های مستقل تایم‌فریم‌های دیگر هنوز متصل نیستند؛ بنابراین برچسب‌های روند از افق‌های میانگین در تایم‌فریم انتخابی می‌آیند.
 */
object TechnicalAnalyst {

    data class ScenarioDetails(
        val titleFa: String,
        val probabilityPercent: Int,
        val activationConditionFa: String,
        val targetLevel: Double,
        val invalidationLevel: Double,
        val descriptionFa: String
    )

    data class TechnicalReport(
        val symbol: String,
        val selectedTimeframe: String, // 1m, 5m, 15m, 1h, 4h, 1d, 1w
        val marketType: MarketType,
        val mainTrendDirectionFa: String, // روند بلندمدت‌تر بر اساس میانگین‌های همین تایم‌فریم
        val intermediateTrendDirectionFa: String, // روند تایم‌فریم میانی (1h)
        val entryTimeframeDirectionFa: String, // روند تایم‌فریم ورود (15m/5m)
        val isTimeframeConflicted: Boolean,
        val timeframeConflictExplanationFa: String?,
        val marketStructureFa: String, // صعودی، نزولی، رنج
        val higherHighsAndLowsFa: String, // برچسب HH, HL, LH, LL
        val marketStructureBreakFa: String, // شکست ساختار (MSB / CHoCH)
        val breakoutQualityFa: String, // شکست معتبر یا احتمال تله کاذب
        val supportLevel: Double,
        val resistanceLevel: Double,
        val demandZoneMin: Double,
        val demandZoneMax: Double,
        val supplyZoneMin: Double,
        val supplyZoneMax: Double,
        val probableLiquidityZonesFa: String,
        val volumeAnalysisFa: String,
        val indicators: TechnicalIndicators,
        val candidateAction: SignalAction,
        val bullishScenario: ScenarioDetails,
        val bearishScenario: ScenarioDetails,
        val neutralScenario: ScenarioDetails,
        val activeScenarioNameFa: String,
        val entryZoneMin: Double,
        val entryZoneMax: Double,
        val referenceEntry: Double,
        val stopLoss: Double,
        val tp1: Double,
        val tp2: Double,
        val tp3: Double,
        val riskRewardRatio: String,
        val invalidationConditionFa: String,
        val confirmingEvidences: List<String>,
        val opposingFactors: List<String>,
        val educationalExplanationForBeginners: String,
        val priceZoneStatusFa: String // «در محدوده ورود» یا «در انتظار رسیدن قیمت به محدوده»
    )

    fun analyze(
        coin: Coin,
        candles: List<Candle>,
        marketType: MarketType = MarketType.SPOT,
        timeframe: String = "1h"
    ): TechnicalReport {
        val closedCandles = candles.filter { it.isClosed }.sortedBy { it.time }
        require(closedCandles.size >= 30) { "تحلیل تکنیکال به حداقل ۳۰ کندل بسته‌شده معتبر نیاز دارد." }
        require(coin.priceUsd.isFinite() && coin.priceUsd > 0.0) { "قیمت بازار معتبر نیست." }
        val currentPrice = coin.priceUsd

        // Indicators and price action are computed only from closed candles; no synthetic fallback values.
        val indicators = TechnicalIndicatorsCalculator.calculate(closedCandles)
            ?: throw IllegalArgumentException("داده کافی برای محاسبه شاخص‌ها وجود ندارد.")
        val priceAction = PriceActionEngine.analyze(closedCandles)
        require(priceAction.valid) { "تحلیل پرایس اکشن به داده بسته‌شده کافی نیاز دارد." }

        val atr = max(indicators.atr, currentPrice * 0.01)
        val ema20 = indicators.ema20
        val ema50 = indicators.ema50
        val sma200 = indicators.sma200
        val rsi = indicators.rsi
        val support = indicators.support
        val resistance = indicators.resistance

        // 1. مقایسه افق‌های میانگین در تایم‌فریم انتخابی؛ داده تایم‌فریم مستقل هنوز در دسترس نیست.
        val isHigherTrendBullish = currentPrice > sma200 && ema50 > sma200
        val isIntermediateBullish = ema20 >= ema50 && rsi >= 46.0
        val isEntryTimeframeBullish = currentPrice >= ema20

        val mainTrendFa = if (isHigherTrendBullish) "تمایل صعودی بر اساس میانگین بلندمدت" else "تمایل نزولی/اصلاحی بر اساس میانگین بلندمدت"
        val intermediateTrendFa = if (isIntermediateBullish) "صعودی با مومنتوم مثبت" else "نزولی با فشار فروش موضعی"
        val entryTimeframeFa = when (timeframe) {
            "1m", "5m" -> if (isEntryTimeframeBullish) "اسکالپ سریع صعودی" else "اسکالپ نزولی در کف‌های مقطعی"
            "15m", "1h" -> if (isEntryTimeframeBullish) "روند معاملاتی روزانه صعودی" else "روند روزانه نزولی"
            "4h", "1d", "1w" -> if (isHigherTrendBullish) "سویینگ صعودی میان‌مدت" else "سویینگ نزولی کلان"
            else -> "روند متعادل"
        }

        // تعارض چند تایم‌فریمی (Timeframe Conflict)
        val isTimeframeConflicted = (isHigherTrendBullish && !isIntermediateBullish) || (!isHigherTrendBullish && isIntermediateBullish)
        val conflictExplanation = if (isTimeframeConflicted) {
            "تعارض میان جهت میانگین‌های کوتاه‌مدت و بلندمدت در همین تایم‌فریم؛ احتیاط بیشتر یا کاهش حجم لازم است."
        } else null

        // 2. تعیین سقف‌ها و کف‌ها (HH, HL, LH, LL)
        val higherHighsLowsFa = "آخرین سقف نوسانی: $${formatPrice(priceAction.swingHigh)} | سقف قبلی: $${formatPrice(priceAction.previousSwingHigh)} | آخرین کف نوسانی: $${formatPrice(priceAction.swingLow)} | کف قبلی: $${formatPrice(priceAction.previousSwingLow)}"

        // Price-action structure break, liquidity sweep and false-breakout detection.
        val msbFa = priceAction.structureBreakFa
        val liquidityAbove = priceAction.swingHigh * 1.01
        val liquidityBelow = priceAction.swingLow * 0.99
        val liquidityZonesFa = "نواحی بالقوه نقدینگی بالای سقف $${formatPrice(liquidityAbove)} و زیر کف $${formatPrice(liquidityBelow)}؛ ${priceAction.liquiditySweepFa}"
        val avgVolume = closedCandles.takeLast(20).map { it.volume }.average()
        val lastVolumeRatio = if (avgVolume > 0.0) closedCandles.last().volume / avgVolume else 0.0
        val volumeAnalysisFa = when {
            lastVolumeRatio >= 1.3 -> "حجم آخرین کندل بسته‌شده ${String.format(Locale.US, "%.2f", lastVolumeRatio)} برابر میانگین ۲۰ کندل است؛ تأیید حجم قوی‌تر است."
            rsi > 65.0 -> "مومنتوم بالاست، اما حجم به‌تنهایی شکست را تأیید نمی‌کند."
            rsi < 38.0 -> "مومنتوم نزولی/اشباع فروش دیده می‌شود؛ برگشت قیمت نیازمند تأیید کندلی است."
            else -> "حجم آخرین کندل تأیید قوی شکست نمی‌دهد؛ ${priceAction.candlePatternFa}."
        }

        // 5. تعیین سه سناریوی مستقل (صعودی، نزولی، خنثی)
        val bullishScenario = ScenarioDetails(
            titleFa = "سناریوی صعودی (Bullish Scenario)",
            probabilityPercent = if (isIntermediateBullish) 65 else 35,
            activationConditionFa = "تثبیت قیمت و کلوز کندل بالاتر از ناحیه $${formatPrice(currentPrice * 1.003)} با تأیید حجم",
            targetLevel = currentPrice + (atr * 3.5),
            invalidationLevel = support * 0.992,
            descriptionFa = "حرکت پرقدرت از ناحیه تقاضا به سمت مقاومت‌های بالاتر و ثبت سقف جدید محلی."
        )

        val bearishScenario = ScenarioDetails(
            titleFa = "سناریوی نزولی (Bearish Scenario)",
            probabilityPercent = if (!isIntermediateBullish) 60 else 30,
            activationConditionFa = "از دست رفتن سطح حمایت $${formatPrice(support)} و نفوذ به زیر کف $${formatPrice(priceAction.swingLow)}",
            targetLevel = currentPrice - (atr * 3.5),
            invalidationLevel = resistance * 1.008,
            descriptionFa = "فشار فروشندگان برای شکار استاپ‌لاس‌های انباشته‌شده در زیر کف ساختار."
        )

        val neutralScenario = ScenarioDetails(
            titleFa = "سناریوی خنثی / رنج (Range Bound Scenario)",
            probabilityPercent = 25,
            activationConditionFa = "تداوم نوسان بین سقف مقاومت $${formatPrice(resistance)} و کف حمایت $${formatPrice(support)}",
            targetLevel = currentPrice,
            invalidationLevel = resistance * 1.02,
            descriptionFa = "نوسان‌گیری در باند بولینگر میانی و تثبیت انرژی تا زمان خروج قطعی از دامنه تراکم."
        )

        // 6. تعیین سطوح معاملاتی (Entry, SL, TP1, TP2, TP3)
        val longBias = if (marketType == MarketType.SPOT) {
            isIntermediateBullish || rsi in 42.0..62.0
        } else {
            isIntermediateBullish
        }
        val isCandidateLong = longBias && priceAction.bullishEvidence && priceAction.bullishScore >= priceAction.bearishScore
        val isCandidateShort = marketType == MarketType.FUTURES_USDT_M && !isIntermediateBullish &&
            priceAction.bearishEvidence && priceAction.bearishScore >= priceAction.bullishScore

        val candidateAction: SignalAction
        val refEntry: Double
        val entryMin: Double
        val entryMax: Double
        val sl: Double
        val tp1: Double
        val tp2: Double
        val tp3: Double

        if (marketType == MarketType.SPOT) {
            candidateAction = if (isCandidateLong) SignalAction.LONG else SignalAction.WAIT
            refEntry = currentPrice
            entryMin = currentPrice * 0.994
            entryMax = currentPrice * 1.003
            sl = max(support * 0.99, currentPrice - (atr * 1.7))
            tp1 = currentPrice + (atr * 2.1)
            tp2 = currentPrice + (atr * 3.8)
            tp3 = currentPrice + (atr * 5.6)
        } else {
            // Futures USDT-M
            if (isCandidateLong) {
                candidateAction = SignalAction.LONG
                refEntry = currentPrice
                entryMin = currentPrice * 0.994
                entryMax = currentPrice * 1.004
                sl = max(support * 0.991, currentPrice - (atr * 1.6))
                tp1 = currentPrice + (atr * 2.0)
                tp2 = currentPrice + (atr * 3.7)
                tp3 = currentPrice + (atr * 5.5)
            } else if (isCandidateShort) {
                candidateAction = SignalAction.SHORT
                refEntry = currentPrice
                entryMin = currentPrice * 0.996
                entryMax = currentPrice * 1.006
                sl = min(resistance * 1.009, currentPrice + (atr * 1.6))
                tp1 = currentPrice - (atr * 2.0)
                tp2 = currentPrice - (atr * 3.7)
                tp3 = currentPrice - (atr * 5.5)
            } else {
                candidateAction = SignalAction.WAIT
                refEntry = currentPrice
                entryMin = currentPrice * 0.997
                entryMax = currentPrice * 1.003
                sl = currentPrice - atr
                tp1 = currentPrice + atr
                tp2 = currentPrice + atr * 2.0
                tp3 = currentPrice + atr * 3.0
            }
        }

        val slDistance = abs(refEntry - sl)
        val tpWeighted = (abs(tp1 - refEntry) * 0.40) + (abs(tp2 - refEntry) * 0.30) + (abs(tp3 - refEntry) * 0.30)
        val rr = if (slDistance > 0) tpWeighted / slDistance else 2.0
        val rrString = "1:${String.format(Locale.US, "%.1f", rr)}"

        // بررسی وضعیت رسیدن قیمت به محدوده ورود
        val isInsideZone = currentPrice in entryMin..entryMax
        val priceZoneStatusFa = if (isInsideZone) {
            "قیمت در محدوده بهینه ورود قرار دارد (آماده اجرای معامله)"
        } else if (currentPrice > entryMax) {
            "قیمت بالاتر از محدوده ورود است (در انتظار اصلاح و برگشت قیمت به باکس ورود)"
        } else {
            "قیمت پایین‌تر از محدوده ورود است (در انتظار تأیید بازگشت صعودی به ناحیه)"
        }

        val invalidationConditionFa = if (candidateAction == SignalAction.LONG) {
            "کلوز کندل در تایم‌فریم $timeframe زیر سطح $${formatPrice(sl)} تحلیل را به صورت خودکار باطل می‌کند."
        } else {
            "نفوذ و کلوز کندل بالاتر از مقاومت در $${formatPrice(sl)} سناریوی شورت را کاملاً نامعتبر می‌سازد."
        }

        val confirmingEvidences = mutableListOf<String>().apply {
            if (isIntermediateBullish) add("تقاطع مثبت و حمایت مووینگ‌های ۲۰ و ۵۰ در نمودار")
            if (rsi in 45.0..65.0) add("قرارگیری شاخص RSI در محدوده شتاب بهینه بدون اشباع هیجانی")
            add("تلاقی ناحیه تقاضا (Demand) با میانگین متحرک کلیدی")
            addAll(priceAction.notes)
            if (!isTimeframeConflicted) add("هم‌راستایی ساختار روند در تایم‌فریم انتخابی با جهت کلی بازار")
        }

        val opposingFactors = mutableListOf<String>().apply {
            if (isTimeframeConflicted) add("تعارض با جهت حرکت در تایم‌فریم بالاتر")
            add("احتمال تست مجدد سطح نقدینگی در حوالی حمایت/مقاومت")
            if (priceAction.bullishScore == priceAction.bearishScore) add("پرایس اکشن جهت غالب تأییدشده‌ای ندارد")
            if (rsi > 68.0 || rsi < 32.0) add("ورود به محدوده نوسانی پرریسک در اندیکاتورهای مومنتوم")
        }

        val educationalExplanation = if (candidateAction == SignalAction.LONG) {
            "راهنمای آموزشی: بازار پس از تست محدوده حمایتی، کف بالاتری ثبت کرده است. ورود در این ناحیه با حد ضرر منطقی در $${formatPrice(sl)} انجام می‌شود تا در صورت نقض فرضیه با کمترین زیان خارج شویم."
        } else if (candidateAction == SignalAction.SHORT) {
            "راهنمای آموزشی: ناتوانی در شکست سقف و بازگشت به زیر مقاومت نشان‌دهنده ورود فروشندگان است. هدف‌ها در پله‌های حمایتی پایین‌تر برای سیو سود مرحله‌ای تنظیم شده‌اند."
        } else {
            "راهنمای آموزشی: بازار در وضعیت رنج قرار دارد. بهترین اقدام در شرایط فعلی، صبر تا تأیید خروج قیمت از تراکم و رسیدن به سطوح بهینه است."
        }

        return TechnicalReport(
            symbol = coin.symbol,
            selectedTimeframe = timeframe,
            marketType = marketType,
            mainTrendDirectionFa = mainTrendFa,
            intermediateTrendDirectionFa = intermediateTrendFa,
            entryTimeframeDirectionFa = entryTimeframeFa,
            isTimeframeConflicted = isTimeframeConflicted,
            timeframeConflictExplanationFa = conflictExplanation,
            marketStructureFa = priceAction.marketStructureFa,
            higherHighsAndLowsFa = higherHighsLowsFa,
            marketStructureBreakFa = msbFa,
            breakoutQualityFa = priceAction.breakoutQualityFa,
            supportLevel = support,
            resistanceLevel = resistance,
            demandZoneMin = indicators.demandZoneMin,
            demandZoneMax = indicators.demandZoneMax,
            supplyZoneMin = indicators.supplyZoneMin,
            supplyZoneMax = indicators.supplyZoneMax,
            probableLiquidityZonesFa = liquidityZonesFa,
            volumeAnalysisFa = volumeAnalysisFa,
            indicators = indicators,
            candidateAction = candidateAction,
            bullishScenario = bullishScenario,
            bearishScenario = bearishScenario,
            neutralScenario = neutralScenario,
            activeScenarioNameFa = if (candidateAction == SignalAction.LONG) bullishScenario.titleFa else if (candidateAction == SignalAction.SHORT) bearishScenario.titleFa else neutralScenario.titleFa,
            entryZoneMin = entryMin,
            entryZoneMax = entryMax,
            referenceEntry = refEntry,
            stopLoss = sl,
            tp1 = tp1,
            tp2 = tp2,
            tp3 = tp3,
            riskRewardRatio = rrString,
            invalidationConditionFa = invalidationConditionFa,
            confirmingEvidences = confirmingEvidences,
            opposingFactors = opposingFactors,
            educationalExplanationForBeginners = educationalExplanation,
            priceZoneStatusFa = priceZoneStatusFa
        )
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 100.0) {
            String.format(Locale.US, "%,.2f", price)
        } else if (price >= 1.0) {
            String.format(Locale.US, "%.4f", price)
        } else {
            String.format(Locale.US, "%.6f", price)
        }
    }
}
