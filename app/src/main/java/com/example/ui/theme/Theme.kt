package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = CryptoPrimaryGold,
    onPrimary = Color.Black,
    primaryContainer = CryptoCardElevatedDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CryptoSecondaryCyan,
    onSecondary = Color.Black,
    secondaryContainer = CryptoCardDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CryptoFuturesPurple,
    onTertiary = Color.White,
    background = CryptoBgDark,
    onBackground = TextPrimaryDark,
    surface = CryptoSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CryptoCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CryptoBorderDark,
    error = BearishRed,
    onError = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = CryptoPrimaryGold,
    onPrimary = Color.Black,
    primaryContainer = CryptoCardElevatedLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = CryptoSecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = CryptoCardLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = CryptoFuturesPurple,
    onTertiary = Color.White,
    background = CryptoBgLight,
    onBackground = TextPrimaryLight,
    surface = CryptoSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CryptoCardElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CryptoBorderLight,
    error = BearishRed,
    onError = Color.White
)

@Composable
fun CryptoTradingTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
