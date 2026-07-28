package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R

// Palette bianco/nero + accento lime elettrico. Mantiene un'identità distintiva su
// entrambi i temi. In dark il fondo è quasi nero puro.
private val AccentLight = Color(0xFFCCFF00)
private val AccentDark = Color(0xFFCCFF00)
private val OnAccent = Color(0xFF0A0A0A)

fun brandGradient(): Brush = Brush.linearGradient(listOf(AccentLight, AccentLight))

// Plus Jakarta Sans: font geometrico moderno e morbido (Linear/Notion-like), meno
// "tabellone sportivo" di Space Grotesk. Ha tutti i pesi di cui abbiamo bisogno.
private val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

// Tipografia con display grande per il conteggio hero, e testo readable per i body.
private val AppTypography: Typography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccent,
    primaryContainer = AccentLight,
    onPrimaryContainer = OnAccent,
    secondary = Color(0xFF0A0A0A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFF4F4F5),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccent,
    primaryContainer = AccentDark,
    onPrimaryContainer = OnAccent,
    secondary = Color(0xFFFAFAFA),
    onSecondary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF8B8B8E),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF1F1F1F),
)

@Composable
fun ListaSpesaTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val mode by ThemePrefs.mode.collectAsState()
    val dark = when (mode) {
        ThemeMode.AUTO -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
