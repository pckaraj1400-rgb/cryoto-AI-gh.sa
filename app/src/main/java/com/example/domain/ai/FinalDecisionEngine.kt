package com.example.domain.ai

import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.MarketType
import com.example.data.model.NewsItem
import com.example.data.model.PaperPosition
import com.example.data.model.RiskSettings
import com.example.data.model.SignalAction
import com.example.data.model.SignalStatus
import com.example.data.model.TradeSignal
import java.util.UUID
import kotlin.math.min

/**
 * ۷. موتور تصمیم‌گیری نهایی (Final Decision Engine)
 *
 * مهم‌ترین بخش سامانه معاملاتی:
 * ترکیب خروجی سه تحلیلگر مستقل و تبدیل به یک گزارش نهایی جامع و قابل‌بررسی.
 * کنترل‌های ریسک بر تمام مدل‌ها اولویت دارند و رأی اکثریت به‌تنهایی مجوز ورود نیست.
 */
object FinalDecisionEngine {

    data class ComprehensiveReport(
        val signal: TradeSignal,
        val fundamentalReport: FundamentalAnalyst.FundamentalReport,
        val technicalReport: TechnicalAnalyst.TechnicalReport,
        val quantReport: QuantitativeRiskEngine.QuantResult,
        val consensusAction: SignalAction,
        val consensusConfidenceScore: Int,
        val consensusSummaryFa: String,
        val isVetoTriggered: Boolean,
        val vetoReasonFa: String?,
        val pros: List<String>,
        val cons: List<String>,
        val beginnerGuideFa: String
    )

    fun generateSignal(
        coin: Coin,
        candles: List<Candle>,
        marketType: MarketType,
        timeframe: String,
        accountBalanceUsd: Double,
        riskSettings: RiskSettings,
        currentNewsList: List<NewsItem> = emptyList(),
        openPositions: List<PaperPosition> = emptyList(),
        currentDailyDrawdownPercent: Double = 0.0
    ): TradeSignal {
        val report = evaluateComprehensive(
            coin = coin,
            candles = candles,
            marketType = marketType,
            timeframe = timeframe,
            accountBalanceUsd = accountBalanceUsd,
            riskSettings = riskSettings,
            currentNewsList = currentNewsList,
            openPositions = openPositions,
            currentDailyDrawdownPercent = currentDailyDrawdownPercent
        )
        return report.signal
    }

    fun evaluateComprehensive(
        coin: Coin,
        candles: List<Candle>,
        marketType: MarketType,
        timeframe: String,
        accountBalanceUsd: Double,
        riskSettings: RiskSettings,
        currentNewsList: List<NewsItem> = emptyList(),
        openPositions: List<PaperPosition> = emptyList(),
        currentDailyDrawdownPercent: Double = 0.0
    ): ComprehensiveReport {
        // گام ۱: اجرای مستقل تحلیل بنیادی و خبری
        val fundamental = FundamentalAnalyst.analyze(coin, currentNewsList)

        // گام ۲: اجرای مستقل تحلیل تکنیکال چندتایم‌فریمی
        val technical = TechnicalAnalyst.analyze(coin, candles, marketType, timeframe)

        // گام ۳: اجرای محاسبات قطعی کمی و مدیریت ریسک
        val isLong = technical.candidateAction == SignalAction.LONG
        val effectiveLev = if (marketType == MarketType.FUTURES_USDT_M) 5 else 1
        val quant = QuantitativeRiskEngine.evaluate(
            accountBalanceUsd = accountBalanceUsd,
            marketType = marketType,
            isLong = isLong,
            entryPrice = technical.referenceEntry,
            stopLoss = technical.stopLoss,
            tp1 = technical.tp1,
            tp2 = technical.tp2,
            tp3 = technical.tp3,
            leverage = effectiveLev,
            riskSettings = riskSettings,
            currentOpenPositions = openPositions,
            currentDailyDrawdownPercent = currentDailyDrawdownPercent
        )

        // گام ۴: موتور اجماع و اعمال قواعد وتو
        val prosList = mutableListOf<String>()
        val consList = mutableListOf<String>()

        prosList.addAll(fundamental.pros)
        prosList.addAll(technical.confirmingEvidences)

        consList.addAll(fundamental.cons)
        consList.addAll(technical.opposingFactors)

        var isVeto = false
        var vetoReason: String? = null
        val finalAction: SignalAction
        var confidenceScore: Int

        // اصل ۱: اولویت قطعی وتوی مدیریت ریسک
        if (!quant.isApproved) {
            isVeto = true
            vetoReason = quant.rejectionReasonFa
            finalAction = SignalAction.NO_TRADE
            confidenceScore = 35
        }
        // اصل ۲: نبود فید بنیادی معتبر نباید به‌عنوان تأیید خنثی تعبیر شود.
        else if (fundamental.sentiment == "UNCERTAIN") {
            finalAction = SignalAction.WAIT
            confidenceScore = 30
            consList.add("داده بنیادی و خبری معتبر در دسترس نیست؛ تا اتصال فید قابل‌راستی‌آزمایی از ورود خودداری شود.")
        }
        // اصل ۳: رویدادهای با عدم‌قطعیت بالا (بلک‌اوت خبری قبل از انتشار داده‌های بحرانی)
        else if (fundamental.eventsToAvoidTrading.isNotEmpty() && fundamental.highRiskWarning) {
            isVeto = true
            vetoReason = "رویداد پرریسک در پیش است؛ اجتناب از معامله تا تثبیت خبر: ${fundamental.eventsToAvoidTrading.first()}"
            finalAction = SignalAction.WAIT
            confidenceScore = 45
        }
        // اصل ۴: تعارض شدید تایم‌فریم بالاتر با تایم‌فریم ورود
        else if (technical.isTimeframeConflicted) {
            finalAction = SignalAction.WAIT
            confidenceScore = 55
            consList.add("تعارض بین روند اصلی بازار و جهت سیگنال ورود؛ توصیه به صبر")
        }
        // اصل ۵: سیگنال تکنیکال خنثی یا در انتظار
        else if (technical.candidateAction == SignalAction.WAIT) {
            finalAction = SignalAction.WAIT
            confidenceScore = 60
        }
        // اصل ۶: تأییدیه کامل هر سه تحلیلگر
        else {
            finalAction = technical.candidateAction
            val baseConfidence = (fundamental.confidenceScore + 85) / 2
            confidenceScore = min(95, if (fundamental.sentiment == "BULLISH" && finalAction == SignalAction.LONG) {
                baseConfidence + 5
            } else if (fundamental.sentiment == "BEARISH" && finalAction == SignalAction.SHORT) {
                baseConfidence + 5
            } else {
                baseConfidence - 5
            })
        }

        // بررسی وضعیت رسیدن قیمت به محدوده ورود
        val currentPrice = coin.priceUsd
        val isInsideZone = currentPrice in technical.entryZoneMin..technical.entryZoneMax
        val initialStatus = if (finalAction.isTradeable) {
            if (isInsideZone) SignalStatus.ACTIVE else SignalStatus.PENDING
        } else {
            SignalStatus.PENDING
        }

        val priceZoneStatusFa = if (isInsideZone) {
            "قیمت در محدوده بهینه ورود قرار دارد (آماده اجرا)"
        } else if (currentPrice > technical.entryZoneMax) {
            "در انتظار اصلاح قیمت به باکس ورود ($${formatPrice(technical.entryZoneMin)} - $${formatPrice(technical.entryZoneMax)})"
        } else {
            "در انتظار بالا آمدن قیمت به باکس ورود ($${formatPrice(technical.entryZoneMin)} - $${formatPrice(technical.entryZoneMax)})"
        }

        val consensusSummaryFa = if (isVeto) {
            "وتوی معامله: ${vetoReason ?: "نقض الزامات ایمنی ریسک"}"
        } else if (finalAction == SignalAction.WAIT) {
            "وضعیت صبر و رصد: بازار فاقد تأییدیه هم‌زمان تمام تحلیلگران است."
        } else {
            "تأیید هم‌گرای تحلیلگران: سیگنال ${finalAction.titleFa} با نسبت ریسک به ریوارد ${quant.riskRewardRatio} و انطباق با معیارهای مدیریت سرمایه تأیید شد."
        }

        val beginnerGuide = if (finalAction.isTradeable) {
            "برای ورود امن: از پله اول ورود در $${formatPrice(technical.entryZoneMin)} استفاده کنید. حد ضرر را روی $${formatPrice(technical.stopLoss)} تنظیم نمایید. به محض لمس تارگت اول در $${formatPrice(technical.tp1)}، ۴۰٪ سود را ذخیره کرده و حد ضرر معامله را به نقطه ورود (ریسک‌فری) منتقل کنید."
        } else {
            "توصیه به معامله‌گران: در حال حاضر از ورود به موقعیت جدید خودداری فرمایید و منتظر ایجاد شرایط ایده‌آل و تأیید هم‌زمان تحلیل تکنیکال و فیلترهای ریسک باشید."
        }

        val signal = TradeSignal(
            id = UUID.randomUUID().toString(),
            symbol = coin.symbol,
            marketType = marketType,
            action = finalAction,
            timeframe = timeframe,
            entryZoneMin = technical.entryZoneMin,
            entryZoneMax = technical.entryZoneMax,
            entryPrice = technical.referenceEntry,
            stopLoss = technical.stopLoss,
            takeProfit1 = technical.tp1,
            takeProfit2 = technical.tp2,
            takeProfit3 = technical.tp3,
            currentPrice = currentPrice,
            riskRewardRatio = quant.riskRewardRatio,
            suggestedPositionSizeUsd = quant.suggestedPositionSizeUsd,
            maxRiskAmountUsd = quant.maxRiskAmountUsd,
            recommendedLeverage = quant.leverageRecommended,
            status = initialStatus,
            confidenceScore = confidenceScore,
            fundamentalVerdict = "${fundamental.sentimentFa}: ${fundamental.summaryFa}",
            technicalVerdict = "${technical.mainTrendDirectionFa} | ${technical.marketStructureFa} | ${technical.activeScenarioNameFa}",
            quantVerdict = quant.summaryFa + if (quant.rejectionReasonFa != null) " [علت رد: ${quant.rejectionReasonFa}]" else "",
            invalidationCondition = technical.invalidationConditionFa,
            isLiveTrade = false,
            notionalValueUsd = quant.notionalValueUsd,
            initialMarginUsd = quant.initialMarginUsd,
            estimatedFeesUsd = quant.estimatedEntryExitFeesUsd,
            priceZoneStatusFa = priceZoneStatusFa
        )

        return ComprehensiveReport(
            signal = signal,
            fundamentalReport = fundamental,
            technicalReport = technical,
            quantReport = quant,
            consensusAction = finalAction,
            consensusConfidenceScore = confidenceScore,
            consensusSummaryFa = consensusSummaryFa,
            isVetoTriggered = isVeto,
            vetoReasonFa = vetoReason,
            pros = prosList,
            cons = consList,
            beginnerGuideFa = beginnerGuide
        )
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 100.0) {
            String.format(java.util.Locale.US, "%,.2f", price)
        } else if (price >= 1.0) {
            String.format(java.util.Locale.US, "%.4f", price)
        } else {
            String.format(java.util.Locale.US, "%.6f", price)
        }
    }
}
