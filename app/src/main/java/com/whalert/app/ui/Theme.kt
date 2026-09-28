package com.whalert.app.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand palette — deep teal/green (trust, calm), amber accent for warnings.
private val GreenPrimary = Color(0xFF00695C)
private val GreenOnPrimary = Color(0xFFFFFFFF)
private val GreenContainer = Color(0xFFB2DFDB)
private val Amber = Color(0xFFEF6C00)
private val Slate = Color(0xFF37474F)
private val DarkBg = Color(0xFF101418)
private val DarkSurface = Color(0xFF1B2027)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenContainer,
    secondary = Slate,
    tertiary = Amber,
    error = Color(0xFFB3261E),
    background = Color(0xFFF6F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7ECEA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D8CF),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F4A44),
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFFFFB74D),
    error = Color(0xFFCF6679),
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF262D36)
)

/** Material 3 dynamic-free theme with accessible typography scale (sp-based, font-scale friendly). */
@Composable
fun WhAlertTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        ),
        content = content
    )
}
