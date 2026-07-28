package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.paolonata.shoppinglist.R

// Palette moderna e neutra ispirata a Notion/Linear: neutri "zinc" (grigi puri, non caldi
// né freddi) con un accento indigo elegante. Sostituisce il rosso corallo di brand.
private val AccentLight = Color(0xFF4F46E5) // indigo-600
private val AccentDark = Color(0xFF818CF8)  // indigo-400 (più chiaro sul fondo scuro)

/**
 * Un gradiente più discreto sul viola-indigo, usato solo su elementi decorativi
 * (empty state). Non più sui bottoni: preferiamo tinte piene per una lettura più netta.
 */
fun brandGradient(): Brush = Brush.linearGradient(
    listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
)

private val OpenSans = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_regular, FontWeight.Medium),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
)

private val AppTypography: Typography
    get() {
        val d = Typography()
        return Typography(
            displayLarge = d.displayLarge.copy(fontFamily = OpenSans),
            displayMedium = d.displayMedium.copy(fontFamily = OpenSans),
            displaySmall = d.displaySmall.copy(fontFamily = OpenSans),
            headlineLarge = d.headlineLarge.copy(fontFamily = OpenSans),
            headlineMedium = d.headlineMedium.copy(fontFamily = OpenSans),
            headlineSmall = d.headlineSmall.copy(fontFamily = OpenSans),
            titleLarge = d.titleLarge.copy(fontFamily = OpenSans),
            titleMedium = d.titleMedium.copy(fontFamily = OpenSans),
            titleSmall = d.titleSmall.copy(fontFamily = OpenSans),
            bodyLarge = d.bodyLarge.copy(fontFamily = OpenSans),
            bodyMedium = d.bodyMedium.copy(fontFamily = OpenSans),
            bodySmall = d.bodySmall.copy(fontFamily = OpenSans),
            labelLarge = d.labelLarge.copy(fontFamily = OpenSans),
            labelMedium = d.labelMedium.copy(fontFamily = OpenSans),
            labelSmall = d.labelSmall.copy(fontFamily = OpenSans),
        )
    }

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF), // indigo-100
    onPrimaryContainer = Color(0xFF312E81), // indigo-900
    secondary = Color(0xFF52525B), // zinc-600
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA), // zinc-50
    onBackground = Color(0xFF18181B), // zinc-900
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5), // zinc-100
    onSurfaceVariant = Color(0xFF71717A), // zinc-500
    outline = Color(0xFFE4E4E7), // zinc-200
    outlineVariant = Color(0xFFF4F4F5), // zinc-100
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF1E1B4B), // indigo-950
    primaryContainer = Color(0xFF3730A3), // indigo-800
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFA1A1AA), // zinc-400
    onSecondary = Color(0xFF18181B),
    background = Color(0xFF09090B), // zinc-950
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF18181B), // zinc-900
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF27272A), // zinc-800
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF3F3F46), // zinc-700
    outlineVariant = Color(0xFF27272A),
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
