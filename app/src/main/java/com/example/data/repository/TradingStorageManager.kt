package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AlertCondition
import com.example.data.model.CurrencyUnit
import com.example.data.model.MarginType
import com.example.data.model.MarketType
import com.example.data.model.PaperAccount
import com.example.data.model.PaperPosition
import com.example.data.model.PaperTradeRecord
import com.example.data.model.PriceAlert
import com.example.data.model.RiskSettings
import com.example.data.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * مدیریت ذخیره‌سازی محلی دائمی (Local Persistence) با SharedPreferences و JSON.
 * تمام اطلاعات حساب، موقعیت‌های باز، تاریخچه معاملات، تنظیمات ریسک و هشدارهای قیمت
 * با مدیریت خطا ذخیره و بازیابی می‌شوند.
 */
class TradingStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("crypto_ai_trader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNT = "key_paper_account"
        private const val KEY_POSITIONS = "key_open_positions"
        private const val KEY_TRADE_HISTORY = "key_trade_history"
        private const val KEY_RISK_SETTINGS = "key_risk_settings"
        private const val KEY_PRICE_ALERTS = "key_price_alerts"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_CURRENCY_UNIT = "key_currency_unit"
        private const val KEY_USDT_RATE = "key_usdt_toman_rate"
    }

    // -------------------------------------------------------------
    // Paper Account
    // -------------------------------------------------------------
    fun saveAccount(account: PaperAccount) {
        try {
            val json = JSONObject().apply {
                put("initialBalanceUsd", account.initialBalanceUsd)
                put("availableBalanceUsd", account.availableBalanceUsd)
                put("totalEquityUsd", account.totalEquityUsd)
                put("realizedPnLUsd", account.realizedPnLUsd)
                put("unrealizedPnLUsd", account.unrealizedPnLUsd)
                put("maxDrawdownPercent", account.maxDrawdownPercent)
                put("winRatePercent", account.winRatePercent)
                put("totalTradesCount", account.totalTradesCount)
                put("winningTradesCount", account.winningTradesCount)
                put("losingTradesCount", account.losingTradesCount)
            }
            prefs.edit().putString(KEY_ACCOUNT, json.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving account: ${e.message}")
        }
    }

    fun loadAccount(): PaperAccount {
        return try {
            val raw = prefs.getString(KEY_ACCOUNT, null) ?: return PaperAccount()
            val json = JSONObject(raw)
            PaperAccount(
                initialBalanceUsd = json.optDouble("initialBalanceUsd", 10_000.0),
                availableBalanceUsd = json.optDouble("availableBalanceUsd", 10_000.0),
                totalEquityUsd = json.optDouble("totalEquityUsd", 10_000.0),
                realizedPnLUsd = json.optDouble("realizedPnLUsd", 0.0),
                unrealizedPnLUsd = json.optDouble("unrealizedPnLUsd", 0.0),
                maxDrawdownPercent = json.optDouble("maxDrawdownPercent", 0.0),
                winRatePercent = json.optDouble("winRatePercent", 0.0),
                totalTradesCount = json.optInt("totalTradesCount", 0),
                winningTradesCount = json.optInt("winningTradesCount", 0),
                losingTradesCount = json.optInt("losingTradesCount", 0)
            )
        } catch (e: Exception) {
            Log.e("StorageManager", "Error loading account: ${e.message}")
            PaperAccount()
        }
    }

    // -------------------------------------------------------------
    // Open Positions
    // -------------------------------------------------------------
    fun saveOpenPositions(positions: List<PaperPosition>) {
        try {
            val array = JSONArray()
            for (p in positions) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("symbol", p.symbol)
                    put("marketType", p.marketType.name)
                    put("direction", p.direction)
                    put("entryPrice", p.entryPrice)
                    put("currentPrice", p.currentPrice)
                    put("amount", p.amount)
                    put("initialAmount", p.initialAmount)
                    put("notionalValueUsd", p.notionalValueUsd)
                    put("leverage", p.leverage)
                    put("marginType", p.marginType.name)
                    put("marginUsd", p.marginUsd)
                    p.stopLoss?.let { put("stopLoss", it) }
                    p.takeProfit1?.let { put("takeProfit1", it) }
                    p.takeProfit2?.let { put("takeProfit2", it) }
                    p.takeProfit3?.let { put("takeProfit3", it) }
                    p.liquidationPrice?.let { put("liquidationPrice", it) }
                    put("unrealizedPnLUsd", p.unrealizedPnLUsd)
                    put("pnlPercent", p.pnlPercent)
                    put("entryFeeUsd", p.entryFeeUsd)
                    put("openTime", p.openTime)
                    put("tp1Triggered", p.tp1Triggered)
                    put("tp2Triggered", p.tp2Triggered)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_POSITIONS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving positions: ${e.message}")
        }
    }

    fun loadOpenPositions(): List<PaperPosition> {
        return try {
            val raw = prefs.getString(KEY_POSITIONS, null) ?: return emptyList()
            val array = JSONArray(raw)
            val list = mutableListOf<PaperPosition>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PaperPosition(
                        id = obj.getString("id"),
                        symbol = obj.getString("symbol"),
                        marketType = MarketType.valueOf(obj.optString("marketType", MarketType.SPOT.name)),
                        direction = obj.getString("direction"),
                        entryPrice = obj.getDouble("entryPrice"),
                        currentPrice = obj.getDouble("currentPrice"),
                        amount = obj.getDouble("amount"),
                        initialAmount = obj.optDouble("initialAmount", obj.getDouble("amount")),
                        notionalValueUsd = obj.getDouble("notionalValueUsd"),
                        leverage = obj.optInt("leverage", 1),
                        marginType = MarginType.valueOf(obj.optString("marginType", MarginType.ISOLATED.name)),
                        marginUsd = obj.getDouble("marginUsd"),
                        stopLoss = if (obj.has("stopLoss")) obj.getDouble("stopLoss") else null,
                        takeProfit1 = if (obj.has("takeProfit1")) obj.getDouble("takeProfit1") else null,
                        takeProfit2 = if (obj.has("takeProfit2")) obj.getDouble("takeProfit2") else null,
                        takeProfit3 = if (obj.has("takeProfit3")) obj.getDouble("takeProfit3") else null,
                        liquidationPrice = if (obj.has("liquidationPrice")) obj.getDouble("liquidationPrice") else null,
                        unrealizedPnLUsd = obj.optDouble("unrealizedPnLUsd", 0.0),
                        pnlPercent = obj.optDouble("pnlPercent", 0.0),
                        entryFeeUsd = obj.optDouble("entryFeeUsd", 0.0),
                        openTime = obj.optLong("openTime", System.currentTimeMillis()),
                        tp1Triggered = obj.optBoolean("tp1Triggered", false),
                        tp2Triggered = obj.optBoolean("tp2Triggered", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("StorageManager", "Error loading positions: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // Trade History
    // -------------------------------------------------------------
    fun saveTradeHistory(history: List<PaperTradeRecord>) {
        try {
            val array = JSONArray()
            for (t in history) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("symbol", t.symbol)
                    put("marketType", t.marketType.name)
                    put("direction", t.direction)
                    put("entryPrice", t.entryPrice)
                    put("exitPrice", t.exitPrice)
                    put("exitReason", t.exitReason)
                    put("amount", t.amount)
                    put("pnlUsd", t.pnlUsd)
                    put("pnlPercent", t.pnlPercent)
                    put("feeUsd", t.feeUsd)
                    put("openTime", t.openTime)
                    put("closeTime", t.closeTime)
                    put("isWin", t.isWin)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_TRADE_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving trade history: ${e.message}")
        }
    }

    fun loadTradeHistory(): List<PaperTradeRecord> {
        return try {
            val raw = prefs.getString(KEY_TRADE_HISTORY, null) ?: return emptyList()
            val array = JSONArray(raw)
            val list = mutableListOf<PaperTradeRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PaperTradeRecord(
                        id = obj.getString("id"),
                        symbol = obj.getString("symbol"),
                        marketType = MarketType.valueOf(obj.optString("marketType", MarketType.SPOT.name)),
                        direction = obj.getString("direction"),
                        entryPrice = obj.getDouble("entryPrice"),
                        exitPrice = obj.getDouble("exitPrice"),
                        exitReason = obj.getString("exitReason"),
                        amount = obj.getDouble("amount"),
                        pnlUsd = obj.getDouble("pnlUsd"),
                        pnlPercent = obj.getDouble("pnlPercent"),
                        feeUsd = obj.optDouble("feeUsd", 0.0),
                        openTime = obj.getLong("openTime"),
                        closeTime = obj.getLong("closeTime"),
                        isWin = obj.getBoolean("isWin")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("StorageManager", "Error loading trade history: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // Risk Settings
    // -------------------------------------------------------------
    fun saveRiskSettings(settings: RiskSettings) {
        try {
            val obj = JSONObject().apply {
                put("maxRiskPerTradePercent", settings.maxRiskPerTradePercent)
                put("maxDailyDrawdownPercent", settings.maxDailyDrawdownPercent)
                put("maxLeverageAllowed", settings.maxLeverageAllowed)
                put("maxOpenPositions", settings.maxOpenPositions)
                put("emergencyKillSwitch", settings.emergencyKillSwitch)
                put("enforceRiskRewardFilter", settings.enforceRiskRewardFilter)
                put("defaultStopLossPercent", settings.defaultStopLossPercent)
            }
            prefs.edit().putString(KEY_RISK_SETTINGS, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving risk settings: ${e.message}")
        }
    }

    fun loadRiskSettings(): RiskSettings {
        return try {
            val raw = prefs.getString(KEY_RISK_SETTINGS, null) ?: return RiskSettings()
            val obj = JSONObject(raw)
            RiskSettings(
                maxRiskPerTradePercent = obj.optDouble("maxRiskPerTradePercent", 0.5),
                maxDailyDrawdownPercent = obj.optDouble("maxDailyDrawdownPercent", 3.0),
                maxLeverageAllowed = obj.optInt("maxLeverageAllowed", 20),
                maxOpenPositions = obj.optInt("maxOpenPositions", 5),
                emergencyKillSwitch = obj.optBoolean("emergencyKillSwitch", false),
                enforceRiskRewardFilter = obj.optBoolean("enforceRiskRewardFilter", true),
                defaultStopLossPercent = obj.optDouble("defaultStopLossPercent", 2.0)
            )
        } catch (e: Exception) {
            Log.e("StorageManager", "Error loading risk settings: ${e.message}")
            RiskSettings()
        }
    }

    // -------------------------------------------------------------
    // Price Alerts
    // -------------------------------------------------------------
    fun savePriceAlerts(alerts: List<PriceAlert>) {
        try {
            val array = JSONArray()
            for (a in alerts) {
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("symbol", a.symbol)
                    put("targetPrice", a.targetPrice)
                    put("currencyUnit", a.currencyUnit.name)
                    put("condition", a.condition.name)
                    put("isActive", a.isActive)
                    put("isTriggered", a.isTriggered)
                    put("createdAt", a.createdAt)
                    a.triggeredAt?.let { put("triggeredAt", it) }
                    a.triggeredPrice?.let { put("triggeredPrice", it) }
                    put("note", a.note)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_PRICE_ALERTS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving alerts: ${e.message}")
        }
    }

    fun loadPriceAlerts(): List<PriceAlert> {
        return try {
            val raw = prefs.getString(KEY_PRICE_ALERTS, null) ?: return emptyList()
            val array = JSONArray(raw)
            val list = mutableListOf<PriceAlert>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PriceAlert(
                        id = obj.getString("id"),
                        symbol = obj.getString("symbol"),
                        targetPrice = obj.getDouble("targetPrice"),
                        currencyUnit = CurrencyUnit.valueOf(obj.optString("currencyUnit", CurrencyUnit.USD.name)),
                        condition = AlertCondition.valueOf(obj.optString("condition", AlertCondition.GREATER_THAN_OR_EQUAL.name)),
                        isActive = obj.optBoolean("isActive", true),
                        isTriggered = obj.optBoolean("isTriggered", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        triggeredAt = if (obj.has("triggeredAt")) obj.getLong("triggeredAt") else null,
                        triggeredPrice = if (obj.has("triggeredPrice")) obj.getDouble("triggeredPrice") else null,
                        note = obj.optString("note", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("StorageManager", "Error loading alerts: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // Preferences: Theme, Currency, USDT Rate
    // -------------------------------------------------------------
    fun saveThemeMode(theme: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, theme.name).apply()
    }

    fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(raw)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun saveCurrencyUnit(unit: CurrencyUnit) {
        prefs.edit().putString(KEY_CURRENCY_UNIT, unit.name).apply()
    }

    fun loadCurrencyUnit(): CurrencyUnit {
        val raw = prefs.getString(KEY_CURRENCY_UNIT, CurrencyUnit.USD.name) ?: CurrencyUnit.USD.name
        return try {
            CurrencyUnit.valueOf(raw)
        } catch (e: Exception) {
            CurrencyUnit.USD
        }
    }

    fun saveUsdtTomanRate(rate: Double) {
        prefs.edit().putFloat(KEY_USDT_RATE, rate.toFloat()).apply()
    }

    fun loadUsdtTomanRate(): Double {
        val rate = prefs.getFloat(KEY_USDT_RATE, 262_000.0f)
        return if (rate > 0) rate.toDouble() else 262_000.0
    }
}
