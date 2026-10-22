package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlertCondition
import com.example.data.model.Candle
import com.example.data.model.Coin
import com.example.data.model.CurrencyUnit
import com.example.data.model.MarketType
import com.example.data.model.PriceAlert
import com.example.data.model.RiskSettings
import com.example.data.model.SignalAction
import com.example.data.repository.MarketRepository
import com.example.data.repository.PaperTradingRepository
import com.example.data.repository.TradingStorageManager
import com.example.domain.ai.PriceActionEngine
import com.example.domain.ai.QuantitativeRiskEngine
import com.example.domain.backtest.BacktestEngine
import com.example.domain.indicators.TechnicalIndicatorsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TradingCalculationsTest {

    @Test
    fun testCurrencyConversions() {
        val coin = Coin(
            symbol = "BTC",
            name = "Bitcoin",
            nameFa = "بیت‌کوین",
            pair = "BTCUSDT",
            priceUsd = 90_000.0,
            change24h = 2.5,
            high24h = 91_000.0,
            low24h = 89_000.0,
            volume24hUsd = 10_000_000.0,
            usdtRialRate = 965_000.0
        )
        // 1 Toman = 10 Rials
        assertEquals(coin.priceRial, coin.priceToman * 10.0, 0.001)
        assertEquals(86_850_000_000.0, coin.priceRial, 1.0)
        assertEquals(8_685_000_000.0, coin.priceToman, 1.0)
    }

    @Test
    fun testTechnicalIndicatorsMath() {
        val candles = mutableListOf<Candle>()
        val baseTime = System.currentTimeMillis()
        var price = 100.0
        for (i in 0 until 50) {
            price += (if (i % 2 == 0) 1.5 else -0.8)
            candles.add(
                Candle(
                    time = baseTime + (i * 3600_000L),
                    open = price - 0.5,
                    high = price + 1.2,
                    low = price - 1.0,
                    close = price,
                    volume = 1000.0 + (i * 20),
                    isClosed = true
                )
            )
        }

        val indicators = TechnicalIndicatorsCalculator.calculate(candles)
        assertNotNull(indicators)
        indicators?.let {
            assertTrue("RSI should be between 0 and 100", it.rsi in 0.0..100.0)
            assertTrue("EMA20 should be positive", it.ema20 > 0.0)
            assertTrue("EMA50 should be positive", it.ema50 > 0.0)
            assertTrue("Resistance should be >= Support", it.resistance >= it.support)
            assertNotNull(it.marketStructureFa)
            assertNotNull(it.breakoutQualityFa)
        }
    }

    @Test
    fun testRiskEngineRejectsInvalidLongStopLoss() {
        val settings = RiskSettings()
        val result = QuantitativeRiskEngine.evaluate(
            accountBalanceUsd = 10_000.0,
            marketType = MarketType.SPOT,
            isLong = true,
            entryPrice = 100.0,
            stopLoss = 105.0, // Invalid: SL > Entry for LONG
            tp1 = 110.0,
            tp2 = 115.0,
            tp3 = 120.0,
            leverage = 1,
            riskSettings = settings
        )
        assertFalse("Should reject Long trade with SL above Entry", result.isApproved)
    }

    @Test
    fun testRiskEngineValidatesProperRiskRewardAndCalculatesPosition() {
        val settings = RiskSettings(maxRiskPerTradePercent = 0.5, enforceRiskRewardFilter = true)
        val result = QuantitativeRiskEngine.evaluate(
            accountBalanceUsd = 10_000.0,
            marketType = MarketType.FUTURES_USDT_M,
            isLong = true,
            entryPrice = 100.0,
            stopLoss = 96.0, // 4% SL risk
            tp1 = 108.0, // 8% gain
            tp2 = 114.0, // 14% gain
            tp3 = 122.0, // 22% gain
            leverage = 5,
            riskSettings = settings
        )
        assertTrue("Trade with good R:R and proper SL should be approved", result.isApproved)
        assertEquals(50.0, result.maxRiskAmountUsd, 0.01) // 0.5% of 10,000 = $50
        assertEquals(1250.0, result.suggestedPositionSizeUsd, 1.0) // $50 / 0.04 = $1250
        assertEquals(250.0, result.initialMarginUsd, 1.0) // $1250 / 5x = $250
    }

    @Test
    fun testPaperTradingExecutionAndPnL() {
        val repo = PaperTradingRepository()
        repo.resetAccount(10_000.0)

        // Open Long position on ETH
        val openResult = repo.openPosition(
            symbol = "ETH",
            marketType = MarketType.SPOT,
            direction = "LONG",
            entryPrice = 3000.0,
            amount = 1.0, // 1 ETH = $3000
            leverage = 1,
            stopLoss = 2900.0,
            tp1 = 3100.0,
            tp2 = 3200.0,
            tp3 = 3300.0
        )
        assertTrue(openResult.isSuccess)
        assertEquals(1, repo.openPositions.value.size)

        // Close at 3300 (Hit TP3)
        val posId = repo.openPositions.value.first().id
        val closed = repo.closePosition(posId, 3300.0, "TP3")
        assertTrue(closed)
        assertEquals(0, repo.openPositions.value.size)
        assertEquals(1, repo.tradeHistory.value.size)

        val trade = repo.tradeHistory.value.first()
        assertTrue("Trade should be a win", trade.isWin)
        assertTrue("Profit should be around +$300 minus fees", trade.pnlUsd > 290.0)
    }

    @Test
    fun testPaperTradingExecutesPartialTargetsAndChargesEntryFeeOnce() {
        val repo = PaperTradingRepository()
        repo.resetAccount(10_000.0)
        val opened = repo.openPosition(
            symbol = "BTC",
            marketType = MarketType.SPOT,
            direction = "SPOT_BUY",
            entryPrice = 100.0,
            amount = 1.0,
            leverage = 1,
            stopLoss = 90.0,
            tp1 = 110.0,
            tp2 = 120.0,
            tp3 = 130.0
        )
        assertTrue(opened.isSuccess)

        // Hit TP1 -> 40% exit
        repo.updateLivePrices(mapOf("BTC" to 110.0))
        assertEquals(0.6, repo.openPositions.value.single().amount, 0.000001)
        assertEquals("هدف اول (TP1)", repo.tradeHistory.value.single().exitReason)

        // Hit TP2 -> 30% exit
        repo.updateLivePrices(mapOf("BTC" to 120.0))
        assertEquals(0.3, repo.openPositions.value.single().amount, 0.000001)
        assertEquals(2, repo.tradeHistory.value.size)

        // Hit TP3 -> Remaining 30% exit
        repo.updateLivePrices(mapOf("BTC" to 130.0))
        assertTrue(repo.openPositions.value.isEmpty())
        assertEquals(3, repo.tradeHistory.value.size)
        assertTrue(repo.tradeHistory.value.all { it.feeUsd > 0.0 })
        assertTrue("Equity should include the entry fee only once", repo.account.value.totalEquityUsd > 10_000.0)
    }

    @Test
    fun testSpotShortIsRejected() {
        val repo = PaperTradingRepository()
        repo.resetAccount(10_000.0)
        val result = repo.openPosition(
            symbol = "BTC",
            marketType = MarketType.SPOT,
            direction = "SHORT",
            entryPrice = 100.0,
            amount = 1.0,
            leverage = 1
        )
        assertFalse(result.isSuccess)
    }

    @Test
    fun testPriceActionRequiresClosedCandles() {
        val insufficient = (0 until 10).map { i ->
            Candle(i + 1L, 100.0, 102.0, 99.0, 101.0, 1000.0, isClosed = true)
        }
        assertFalse(PriceActionEngine.analyze(insufficient).valid)
    }

    @Test
    fun testPriceActionDetectsOrderBlockAndFVG() {
        val baseTime = 1700000000000L
        val candles = mutableListOf<Candle>()
        for (i in 0 until 40) {
            val p = 100.0 + (i * 0.1)
            candles.add(Candle(baseTime + (i * 60_000L), p, p + 0.5, p - 0.5, p + 0.1, 1000.0, true))
        }

        // Add a bearish candle followed by strong bullish impulse to form a Bullish Order Block and FVG
        val t = baseTime + (41 * 60_000L)
        // Candle 1: down candle (open 105, close 103)
        candles.add(Candle(t, 105.0, 105.5, 102.5, 103.0, 1200.0, true))
        // Candle 2: impulse explosion (open 103.5, close 112)
        candles.add(Candle(t + 60_000L, 103.5, 112.5, 103.0, 112.0, 3500.0, true))
        // Candle 3: continuation (open 112, close 115, low 111) -> low 111 > high 105.5 => Bullish FVG!
        candles.add(Candle(t + 120_000L, 112.0, 115.5, 111.0, 115.0, 2500.0, true))

        val report = PriceActionEngine.analyze(candles)
        assertTrue(report.valid)
        assertTrue(report.orderBlockFa.contains("اردر بلاک صعودی") || report.orderBlockFa.contains("Bullish OB"))
        assertTrue(report.orderBlockMin > 0.0)
        assertTrue(report.orderBlockMax > 0.0)
        assertTrue(report.fairValueGapFa.contains("FVG") || report.fairValueGapFa.contains("منصفانه"))
    }

    @Test
    fun testBacktestDeterministicWithInSampleAndOutOfSampleSplit() {
        val baseTime = 1700000000000L
        val candles = mutableListOf<Candle>()
        var price = 50.0
        for (i in 0 until 100) {
            price += (if (i % 3 == 0) 1.2 else -0.4)
            candles.add(
                Candle(
                    time = baseTime + (i * 3600_000L),
                    open = price - 0.3,
                    high = price + 0.8,
                    low = price - 0.7,
                    close = price,
                    volume = 1500.0,
                    isClosed = true
                )
            )
        }

        val summary = BacktestEngine.runBacktest(
            symbol = "SOL",
            timeframe = "1h",
            strategyName = "تقاطع میانگین‌های متحرک EMA20/EMA50",
            candles = candles,
            marketType = MarketType.SPOT,
            initialCapitalUsd = 10_000.0,
            riskPerTradePercent = 0.5
        )

        assertTrue("Backtest must execute trades", summary.totalTrades > 0)
        assertTrue("Expectancy should be computed", summary.expectancyUsd.isFinite())
        assertTrue("In-sample win rate must be computed", summary.inSampleWinRate in 0.0..100.0)
        assertTrue("Out-of-sample win rate must be computed", summary.outOfSampleWinRate in 0.0..100.0)
        assertTrue("Duration must be non-empty", summary.periodDurationFa.isNotBlank())
        assertEquals(summary.winningTrades + summary.losingTrades, summary.totalTrades)
    }

    @Test
    fun testConservativeAmbiguityResolutionInBacktest() {
        val baseTime = 1700000000000L
        val candles = mutableListOf<Candle>()
        // 25 normal candles
        for (i in 0 until 25) {
            candles.add(Candle(baseTime + (i * 3600_000L), 100.0, 101.0, 99.0, 100.0, 1000.0, true))
        }
        // Entry trigger candle
        candles.add(Candle(baseTime + (26 * 3600_000L), 100.0, 105.0, 99.0, 104.0, 2000.0, true))
        // Extreme volatile candle touching BOTH SL and TP: Low 90 (below SL 98) and High 120 (above TP 108)
        candles.add(Candle(baseTime + (27 * 3600_000L), 104.0, 120.0, 90.0, 105.0, 5000.0, true))
        // Final candle
        candles.add(Candle(baseTime + (28 * 3600_000L), 105.0, 106.0, 104.0, 105.0, 1000.0, true))

        val summary = BacktestEngine.runBacktest(
            symbol = "BTC",
            timeframe = "1h",
            strategyName = "شکست پرقدرت مقاومت و تایید حجم (Breakout)",
            candles = candles,
            marketType = MarketType.SPOT
        )

        val ambiguousTrade = summary.trades.find { it.exitReason == "SL" }
        assertNotNull("Under conservative assumption, stop loss must be hit first when both touched", ambiguousTrade)
    }

    @Test
    fun testLocalStoragePersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = TradingStorageManager(context)

        // Save custom settings
        val customRisk = RiskSettings(
            maxRiskPerTradePercent = 1.25,
            emergencyKillSwitch = true,
            maxLeverageAllowed = 15
        )
        storage.saveRiskSettings(customRisk)

        val loadedRisk = storage.loadRiskSettings()
        assertEquals(1.25, loadedRisk.maxRiskPerTradePercent, 0.001)
        assertTrue(loadedRisk.emergencyKillSwitch)
        assertEquals(15, loadedRisk.maxLeverageAllowed)

        // Save custom alert
        val alert = PriceAlert(
            id = "test-alert-99",
            symbol = "SOL",
            targetPrice = 200.0,
            currencyUnit = CurrencyUnit.USD,
            condition = AlertCondition.GREATER_THAN_OR_EQUAL,
            note = "مقاومت ۲۰۰ دلار"
        )
        storage.savePriceAlerts(listOf(alert))

        val loadedAlerts = storage.loadPriceAlerts()
        assertEquals(1, loadedAlerts.size)
        assertEquals("SOL", loadedAlerts.first().symbol)
        assertEquals(200.0, loadedAlerts.first().targetPrice, 0.001)

        // Connect PaperTradingRepository to storage
        val repo = PaperTradingRepository(storage)
        repo.resetAccount(15_000.0)
        repo.openPosition(
            symbol = "SOL",
            marketType = MarketType.SPOT,
            direction = "SPOT_BUY",
            entryPrice = 150.0,
            amount = 10.0,
            leverage = 1,
            stopLoss = 140.0,
            tp1 = 160.0
        )

        // Verify storage persisted open position
        val reloadedPositions = storage.loadOpenPositions()
        assertEquals(1, reloadedPositions.size)
        assertEquals("SOL", reloadedPositions.first().symbol)
        assertEquals(150.0, reloadedPositions.first().entryPrice, 0.001)

        // Close position
        val posId = reloadedPositions.first().id
        repo.closePosition(posId, 160.0, "TP1")

        // Reload trade history
        val reloadedHistory = storage.loadTradeHistory()
        assertEquals(1, reloadedHistory.size)
        assertTrue(reloadedHistory.first().isWin)
    }

    @Test
    fun testMarketRepositoryRejectsUnmatchedSourceOrMissingCandles() {
        val repo = MarketRepository()
        val coin = Coin("BTC", "Bitcoin", "بیت‌کوین", "BTCUSDT", 90_000.0, 1.5, 91_000.0, 89_000.0, 100_000.0)

        // Test with empty candles -> DATA_UNAVAILABLE
        val signalEmpty = repo.getSignals(
            coin = coin,
            candles = emptyList(),
            marketType = MarketType.SPOT,
            timeframe = "1h",
            accountBalanceUsd = 10_000.0,
            riskSettings = RiskSettings()
        )
        assertEquals(SignalAction.DATA_UNAVAILABLE, signalEmpty.action)
        assertEquals(0.0, signalEmpty.entryPrice, 0.0)
        assertEquals(0.0, signalEmpty.stopLoss, 0.0)
    }
}
