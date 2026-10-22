package com.example.domain.ai

import com.example.data.model.Coin
import com.example.data.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * هوش مصنوعی اول: تحلیلگر بنیادی و خبری (Fundamental AI Analyst)
 *
 * وظیفه این عامل بررسی عوامل بنیادی، اخبار، رویدادهای کلان اقتصادی و آنچین،
 * تفکیک واقعیت‌های تأییدشده از برداشت‌های تحلیلی، ارزیابی اعتبار منابع و
 * مشخص‌سازی زمان‌های پرریسک با عدم‌قطعیت بالا است.
 */
object FundamentalAnalyst {

    data class NewsSourceCredibility(
        val name: String,
        val credibilityScorePercent: Int, // اعتبار منبع بر حسب درصد
        val publishTimeFa: String,
        val fetchTimeFa: String,
        val url: String? = null
    )

    data class EconomicOrNetworkEvent(
        val titleFa: String,
        val category: String, // "اقتصادی کلان"، "رویداد شبکه"، "آنلاک توکن"
        val releaseTimeFa: String,
        val impactLevel: String, // "بسیار بالا"، "متوسط"، "پایین"
        val shouldAvoidTradingUntilRelease: Boolean,
        val descriptionFa: String
    )

    data class FundamentalReport(
        val symbol: String,
        val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL", "UNCERTAIN"
        val sentimentFa: String,
        val summaryFa: String,
        val impactHorizonFa: String,
        val confirmedFacts: List<String>,
        val analyticalInterpretations: List<String>,
        val keyDriversFa: List<String>,
        val pros: List<String>,
        val cons: List<String>,
        val upcomingEvents: List<EconomicOrNetworkEvent>,
        val eventsToAvoidTrading: List<String>,
        val sourcesWithCredibility: List<NewsSourceCredibility>,
        val limitationsAndMissingData: List<String>,
        val highRiskWarning: Boolean,
        val confidenceScore: Int, // امتیاز شواهد، نه احتمال کالیبره‌شده موفقیت
        val contradictoryNewsDetected: Boolean,
        val contradictoryNewsDescriptionFa: String? = null
    )

    fun analyze(coin: Coin, currentNewsList: List<NewsItem> = emptyList()): FundamentalReport {
        val sym = coin.symbol.uppercase()
        val nowTimeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val relevantNews = currentNewsList.filter { news ->
            news.titleFa.contains(sym, ignoreCase = true) ||
                news.title.contains(sym, ignoreCase = true) ||
                news.titleFa.contains(coin.nameFa, ignoreCase = true) ||
                news.title.contains(coin.name, ignoreCase = true)
        }
        val positive = relevantNews.count { it.sentiment.equals("BULLISH", ignoreCase = true) }
        val negative = relevantNews.count { it.sentiment.equals("BEARISH", ignoreCase = true) }
        val neutral = relevantNews.count { it.sentiment.equals("NEUTRAL", ignoreCase = true) }
        val contradiction = positive > 0 && negative > 0
        val sentiment = when {
            relevantNews.isEmpty() -> "UNCERTAIN"
            positive > negative -> "BULLISH"
            negative > positive -> "BEARISH"
            else -> "NEUTRAL"
        }
        val sentimentFa = when (sentiment) {
            "BULLISH" -> "تمایل مثبت بر اساس اخبار دریافت‌شده؛ تأیید مستقل نشده"
            "BEARISH" -> "تمایل منفی بر اساس اخبار دریافت‌شده؛ تأیید مستقل نشده"
            "NEUTRAL" -> "اخبار دریافت‌شده جهت غالب روشنی ندارند"
            else -> "نامشخص؛ فید خبری معتبر در دسترس نیست"
        }
        val sources = relevantNews.map { news ->
            NewsSourceCredibility(
                name = news.source.ifBlank { "منبع نامشخص" },
                credibilityScorePercent = 0, // no source-verification service is configured
                publishTimeFa = news.timeAgoFa.ifBlank { "زمان انتشار نامشخص" },
                fetchTimeFa = nowTimeStr,
                url = news.url
            )
        }.distinctBy { it.name }
        val interpretations = when {
            relevantNews.isEmpty() -> listOf("بدون فید خبری معتبر، جهت بنیادی قابل نتیجه‌گیری نیست.")
            contradiction -> listOf("اخبار مثبت و منفی هم‌زمان وجود دارند؛ نتیجه بنیادی متناقض است.")
            else -> listOf("جهت خبرها بر اساس برچسب منبع ورودی جمع‌بندی شده و به معنی تأیید صحت خبر یا پیش‌بینی قیمت نیست.")
        }
        val pros = if (positive > 0) listOf("تعداد خبرهای دارای برچسب مثبت: $positive") else emptyList()
        val cons = buildList {
            if (negative > 0) add("تعداد خبرهای دارای برچسب منفی: $negative")
            if (contradiction) add("خبرهای متناقض مشاهده شده‌اند؛ نتیجه را با احتیاط تفسیر کن.")
            if (relevantNews.isEmpty()) add("هیچ خبر معتبر و قابل‌استنادی برای این دارایی دریافت نشده است.")
        }
        val limitations = buildList {
            if (relevantNews.isEmpty()) add("اتصال به فید خبری زنده در پروژه پیکربندی نشده است؛ هیچ رویداد یا خبر آینده‌ای تأیید نشده است.")
            add("اعتبار منبع، صحت خبر و زمان انتشار به‌صورت مستقل راستی‌آزمایی نمی‌شوند.")
            add("تقویم رویدادهای اقتصادی و آنچین به سرویس زنده متصل نیست؛ رویدادهای آینده قابل تأیید نیستند.")
        }
        return FundamentalReport(
            symbol = sym,
            sentiment = sentiment,
            sentimentFa = sentimentFa,
            summaryFa = if (relevantNews.isEmpty()) {
                "تحلیل بنیادی ناقص است؛ فید خبری معتبر پیکربندی نشده و هیچ خبر یا رویداد ساختگی جایگزین نمی‌شود."
            } else {
                "بررسی ${relevantNews.size} خبر مرتبط: مثبت=$positive، منفی=$negative، خنثی=$neutral. این جمع‌بندی برچسب خبرهاست و تأیید مستقل محسوب نمی‌شود."
            },
            impactHorizonFa = "افق زمانی نامشخص؛ تقویم رویداد و فید زنده در دسترس نیست",
            confirmedFacts = emptyList(),
            analyticalInterpretations = interpretations,
            keyDriversFa = if (relevantNews.isEmpty()) emptyList() else relevantNews.take(5).map { it.titleFa },
            pros = pros,
            cons = cons,
            upcomingEvents = emptyList(),
            eventsToAvoidTrading = emptyList(),
            sourcesWithCredibility = sources,
            limitationsAndMissingData = limitations,
            highRiskWarning = relevantNews.isEmpty() || contradiction,
            confidenceScore = when {
                relevantNews.isEmpty() -> 0
                contradiction -> 25
                relevantNews.size < 3 -> 35
                else -> 50
            },
            contradictoryNewsDetected = contradiction,
            contradictoryNewsDescriptionFa = if (contradiction) "اخبار مثبت و منفی هم‌زمان دریافت شده‌اند؛ صحت و استقلال آن‌ها بررسی نشده است." else null
        )
    }

}
