package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Look vibrante e moderno: gradiente caldo corallo -> rosso lampone come firma
// del brand, usato su header, FAB ed empty state.
val BrandGradientStart = Color(0xFFFF6A5E)
val BrandGradientEnd = Color(0xFFF5325B)

fun brandGradient(): Brush = Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))

private val LightColors = lightColorScheme(
    primary = Color(0xFFF5325B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFFF6A5E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFF0F0F3),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFFC9C9CE),
    outlineVariant = Color(0xFFE9E9EE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7286),
    onPrimary = Color(0xFF2A0710),
    secondary = Color(0xFFFF8A7E),
    onSecondary = Color(0xFF2A0710),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF2C2C2C),
)

@Composable
fun ListaSpesaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
