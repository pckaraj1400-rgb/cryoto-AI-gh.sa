package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyUnit
import com.example.data.model.DataSource
import com.example.data.model.RiskSettings
import com.example.data.model.ThemeMode
import com.example.data.model.normalizePersianDigits
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoPrimaryGold
import com.example.ui.theme.CryptoSecondaryCyan
import java.util.Locale

@Composable
fun SettingsAndRiskScreen(
    currentDataSource: DataSource,
    onDataSourceSelected: (DataSource) -> Unit,
    currencyUnit: CurrencyUnit,
    onCurrencySelected: (CurrencyUnit) -> Unit,
    usdtTomanRate: Double,
    onUsdtTomanRateChanged: (Double) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    riskSettings: RiskSettings,
    onRiskSettingsChanged: (RiskSettings) -> Unit,
    onOpenPriceAlerts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var maxRiskPercent by remember { mutableFloatStateOf(riskSettings.maxRiskPerTradePercent.toFloat()) }
    var maxDrawdownPercent by remember { mutableFloatStateOf(riskSettings.maxDailyDrawdownPercent.toFloat()) }
    var killSwitch by remember { mutableStateOf(riskSettings.emergencyKillSwitch) }
    var enforceRrFilter by remember { mutableStateOf(riskSettings.enforceRiskRewardFilter) }

    var manualRateInput by remember(usdtTomanRate) {
        mutableStateOf(String.format(Locale.US, "%.0f", usdtTomanRate))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Theme Selection (تغییر تم سیاه یا سفید)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = CryptoPrimaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیمات پوسته و تم ظاهری (Dark / Light Mode)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "امکان تغییر بین تم تاریک (سیاه) برای محیط‌های کم‌نور و تم روشن (سفید) با وضوح بالا.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSel = mode == themeMode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onThemeModeChanged(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) CryptoPrimaryGold else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    text = mode.titleFa,
                                    color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Data Source Manager (مدیریت منابع داده بازار)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = CryptoSecondaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مدیریت منابع داده بازار (Data Sources)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "با انتخاب هر منبع، نرخ‌ها، نمودار و آمار معاملات بلافاصله بر اساس سرور همان منبع دریافت و همگام می‌شوند.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DataSource.values().forEach { src ->
                        val isSel = src == currentDataSource
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (isSel) CryptoSecondaryCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isSel) CryptoSecondaryCyan else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { onDataSourceSelected(src) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = src.titleFa,
                                    color = if (isSel) CryptoSecondaryCyan else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = when (src) {
                                        DataSource.NOBITEX -> "استعلام زنده از سرور نوبیتکس (جفت‌ارزهای ریالی و تتری)"
                                        DataSource.WALLEX -> "استعلام زنده از سرور والکس (مارکت بر پایه تومان)"
                                        DataSource.ARZDIGITAL -> "مرجع جامع بازار ایران با نرخ تتر حدود ۲۶۲,۰۰۰ تومان"
                                        DataSource.BINANCE -> "فید سفارشات و کندل‌های جهانی صرافی بایننس"
                                        DataSource.COINGECKO -> "فید قیمت تجمیعی و تحلیل حجم کوین‌گکو"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSel) {
                                Surface(
                                    color = CryptoSecondaryCyan.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "منبع فعال",
                                        color = CryptoSecondaryCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. USDT/Toman Rate Calibration (تنظیم نرخ تتر به تومان)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = BullishGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیم و کالیبراسیون نرخ تتر به تومان",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "می‌توانید نرخ روز تتر را بر اساس سایت ارزدیجیتال یا بازار آزاد وارد کنید تا کل محاسبات ریالی و تومانی بر این مبنا انجام شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualRateInput,
                            onValueChange = { manualRateInput = it },
                            label = { Text("نرخ هر تتر (تومان)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CryptoPrimaryGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Button(
                            onClick = {
                                val parsed = manualRateInput.normalizePersianDigits().toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    onUsdtTomanRateChanged(parsed)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CryptoPrimaryGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("اعمال نرخ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset buttons
                    Text("انتخاب سریع بر اساس بازار:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            262_000.0 to "۲۶۲,۰۰۰ (ارزدیجیتال)",
                            160_000.0 to "۱۶۰,۰۰۰ تومان",
                            98_500.0 to "۹۸,۵۰۰ تومان"
                        ).forEach { (rate, label) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        manualRateInput = String.format(Locale.US, "%.0f", rate)
                                        onUsdtTomanRateChanged(rate)
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Price Alerts Shortcut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPriceAlerts() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CryptoPrimaryGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مدیریت هشدارهای صوتی و ویبره (Price Alerts)",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "تنظیم آلارم برای شکست سطوح، تارگت‌ها یا کف‌های حمایتی",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenPriceAlerts() },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoPrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مشاهده و افزودن", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Risk Engine Management (موتور مدیریت ریسک مستقل)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = BullishGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیمات موتور مدیریت ریسک مستقل (Risk Engine)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "قوانین قطعی کنترل سرمایه. هیچ سیگنالی بدون تایید این موتور صادر نمی‌شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Max Risk Per Trade Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حداکثر ریسک مجاز در هر معامله:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                "${String.format(Locale.US, "%.1f", maxRiskPercent)}٪ سرمایه",
                                color = CryptoPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = maxRiskPercent,
                            onValueChange = {
                                maxRiskPercent = it
                                onRiskSettingsChanged(riskSettings.copy(maxRiskPerTradePercent = it.toDouble()))
                            },
                            valueRange = 0.2f..3.0f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = CryptoPrimaryGold,
                                activeTrackColor = CryptoPrimaryGold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Max Daily Drawdown Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سقف افت سرمایه روزانه (Max Daily Drawdown):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                "${String.format(Locale.US, "%.1f", maxDrawdownPercent)}٪",
                                color = BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = maxDrawdownPercent,
                            onValueChange = {
                                maxDrawdownPercent = it
                                onRiskSettingsChanged(riskSettings.copy(maxDailyDrawdownPercent = it.toDouble()))
                            },
                            valueRange = 1.0f..10.0f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = BearishRed,
                                activeTrackColor = BearishRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Enforce R:R filter toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الزام حداقل نسبت سود به زیان (۱:۱.۵)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("رد خودکار سیگنال‌های فاقد مزیت آماری لازم", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Switch(
                            checked = enforceRrFilter,
                            onCheckedChange = {
                                enforceRrFilter = it
                                onRiskSettingsChanged(riskSettings.copy(enforceRiskRewardFilter = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BullishGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Emergency Kill Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BearishRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, BearishRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("کلید قطع اضطراری معامله (Kill Switch)", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("توقف فوری باز کردن هرگونه سفارش جدید", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Switch(
                            checked = killSwitch,
                            onCheckedChange = {
                                killSwitch = it
                                onRiskSettingsChanged(riskSettings.copy(emergencyKillSwitch = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BearishRed)
                        )
                    }
                }
            }
        }

        // 6. Version Info & Download Guidance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📱", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "نسخه سامانه معاملاتی و فایل نصبی",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CryptoPrimaryGold.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoPrimaryGold)
                        ) {
                            Text(
                                text = "نسخه ۲.۳.۰ نهایی (Build 4)",
                                color = CryptoPrimaryGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "تغییرات نسخه نهایی دیباگ‌شده (Version 2.3.0 - Build 4):",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• دیباگ و ارتقای کامل هوش مصنوعی ۱ (تحلیلگر بنیادی): تفکیک واقعیت از برداشت، تقویم رویدادهای پرریسک و اعتبارسنجی منابع", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("• دیباگ و ارتقای کامل هوش مصنوعی ۲ (تحلیلگر تکنیکال): تحلیل چندتایم‌فریمی (1m تا 1w)، تشخیص ساختار HH/HL، شکست ساختار و ۳ سناریوی مستقل", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("• دیباگ و ارتقای کامل هوش مصنوعی ۳ (تحلیلگر کمی و ریسک): فرمول قطعی حجم، مارجین، ارزش اسمی، کارمزد، فاندینگ و کنترل فاصله لیکویید", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("• ارتقای موتور تصمیم‌گیری نهایی (Final Decision Engine): ماتریس اجماع ۳ مدل، حق وتوی قطعی ریسک و راهنمای آموزشی مبتدیان", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("• امکان تغییر زنده نماد، بازار (اسپات/فیوچرز) و تایم‌فریم‌ها درون داشبورد تحلیلگران هوش مصنوعی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("• نرمال‌سازی کامل کیبورد و اعداد فارسی و اعشاری در تمام دیالوگ‌ها و فرم‌ها", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "📥 راهنمای دریافت و دانلود فایل نصبی (APK):",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "برای دانلود فایل APK نصبی بر روی گوشی، از منوی بالای پنل AI Studio گزینه Settings/Export یا Download APK را انتخاب فرمایید. هر نسخه با شناسه یکتا و افزایشی (VersionCode) شماره‌گذاری شده و بدون تداخل روی گوشی نصب می‌شود.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
