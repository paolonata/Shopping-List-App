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

// Look vibrante e moderno: gradiente caldo corallo -> rosso lampone come firma
// del brand, usato con parsimonia (FAB, empty state, pulsante di conferma).
val BrandGradientStart = Color(0xFFFF6A5E)
val BrandGradientEnd = Color(0xFFF5325B)

fun brandGradient(): Brush = Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))

// Font Montserrat (bundle in res/font).
private val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
)

private val AppTypography: Typography
    get() {
        val d = Typography()
        return Typography(
            displayLarge = d.displayLarge.copy(fontFamily = Montserrat),
            displayMedium = d.displayMedium.copy(fontFamily = Montserrat),
            displaySmall = d.displaySmall.copy(fontFamily = Montserrat),
            headlineLarge = d.headlineLarge.copy(fontFamily = Montserrat),
            headlineMedium = d.headlineMedium.copy(fontFamily = Montserrat),
            headlineSmall = d.headlineSmall.copy(fontFamily = Montserrat),
            titleLarge = d.titleLarge.copy(fontFamily = Montserrat),
            titleMedium = d.titleMedium.copy(fontFamily = Montserrat),
            titleSmall = d.titleSmall.copy(fontFamily = Montserrat),
            bodyLarge = d.bodyLarge.copy(fontFamily = Montserrat),
            bodyMedium = d.bodyMedium.copy(fontFamily = Montserrat),
            bodySmall = d.bodySmall.copy(fontFamily = Montserrat),
            labelLarge = d.labelLarge.copy(fontFamily = Montserrat),
            labelMedium = d.labelMedium.copy(fontFamily = Montserrat),
            labelSmall = d.labelSmall.copy(fontFamily = Montserrat),
        )
    }

// Sfondi caldi (carta) invece del grigio Android di default.
private val LightColors = lightColorScheme(
    primary = Color(0xFFF5325B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFFF6A5E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF7F4),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF3ECE7),
    onSurfaceVariant = Color(0xFF8A807A),
    outline = Color(0xFFD4C9C2),
    outlineVariant = Color(0xFFEBE2DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7286),
    onPrimary = Color(0xFF2A0710),
    secondary = Color(0xFFFF8A7E),
    onSecondary = Color(0xFF2A0710),
    background = Color(0xFF16130F),
    onBackground = Color(0xFFEDE7E3),
    surface = Color(0xFF221E1B),
    onSurface = Color(0xFFEDE7E3),
    surfaceVariant = Color(0xFF2C2723),
    onSurfaceVariant = Color(0xFFA79E97),
    outline = Color(0xFF544D47),
    outlineVariant = Color(0xFF322C28),
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
