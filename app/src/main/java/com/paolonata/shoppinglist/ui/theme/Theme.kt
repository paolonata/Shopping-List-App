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

// Palette editoriale monocromatica (bianco/nero/grigio), ispirata a to-do app minimali
// (Things/TickTick): niente colore acceso, il contrasto stesso è l'accento. "primary"
// coincide con onBackground/onto quindi checkbox, pulsanti e bordi diventano neri su
// sfondo chiaro e bianchi su sfondo scuro senza dover ritintare ogni componente a mano.
private val InkLight = Color(0xFF0A0A0A)
private val InkDark = Color(0xFFFAFAFA)

fun brandGradient(): Brush = Brush.linearGradient(listOf(InkLight, InkLight))

// Plus Jakarta Sans: font geometrico moderno e morbido (Linear/Notion-like).
private val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

// Tipografia editoriale, meno "poster" della versione precedente: niente tracking
// negativo aggressivo, pesi più regolari per un look da app di produttività pulita.
private val AppTypography: Typography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 50.sp),
    displayMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 40.sp),
    displaySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 25.sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

private val LightColors = lightColorScheme(
    primary = InkLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECECEC),
    onPrimaryContainer = InkLight,
    secondary = InkLight,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = InkLight,
    surface = Color(0xFFFFFFFF),
    onSurface = InkLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFFE2E2E5),
    outlineVariant = Color(0xFFEFEFEF),
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF232323),
    onPrimaryContainer = InkDark,
    secondary = InkDark,
    onSecondary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = InkDark,
    surface = Color(0xFF0A0A0A),
    onSurface = InkDark,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFF2C2C2E),
    outlineVariant = Color(0xFF1C1C1E),
)

// Tema "Olivastro": stessa struttura del tema chiaro (sfondo bianco), ma con l'inchiostro
// (primary/onBackground e i grigi) spostato su un grigio-oliva caldo invece del nero puro.
// Stesso colore usato per la nuova icona dell'app, per coerenza tra icona e tema.
private val OliveInk = Color(0xFF44473A)

private val OliveColors = lightColorScheme(
    primary = OliveInk,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E3D3),
    onPrimaryContainer = OliveInk,
    secondary = OliveInk,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = OliveInk,
    surface = Color(0xFFFFFFFF),
    onSurface = OliveInk,
    surfaceVariant = Color(0xFFEFEEE3),
    onSurfaceVariant = Color(0xFF8C8F78),
    outline = Color(0xFFDEDDCB),
    outlineVariant = Color(0xFFEFEEE3),
)

@Composable
fun ListaSpesaTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val mode by ThemePrefs.mode.collectAsState()
    val colors = when (mode) {
        ThemeMode.AUTO -> if (systemDark) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.OLIVE -> OliveColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
