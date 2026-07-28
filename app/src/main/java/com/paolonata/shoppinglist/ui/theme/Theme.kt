package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R

// Palette "2026": neutri puri (bianco quasi assoluto / nero quasi assoluto) con un
// unico accento vibrante lime, associato a "fresco / spesa" e molto attuale.
private val AccentLight = Color(0xFFCCFF00) // acid lime, cioè giallo-verde saturo
private val AccentDark = Color(0xFFCCFF00)  // stesso in dark per consistenza brand
private val OnAccent = Color(0xFF0A0A0A)    // testo sopra accento: nero pieno

fun brandGradient(): Brush = Brush.linearGradient(listOf(AccentLight, AccentLight))

// Space Grotesk: font geometrico moderno (Vercel/Stripe/Uber). Il Medium fa da
// SemiBold quando serve (Space Grotesk non ha uno static SemiBold in questa fonte).
private val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_light, FontWeight.Light),
    Font(R.font.spacegrotesk_regular, FontWeight.Normal),
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_medium, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold),
    Font(R.font.spacegrotesk_bold, FontWeight.ExtraBold),
)

// Typography con scala più contrastata (grandi titoli display + testo compatto),
// tracking negativo sui titoli come nei design "2026".
private val AppTypography: Typography = Typography(
    displayLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 57.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 45.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
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
fun ListaSpesaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
