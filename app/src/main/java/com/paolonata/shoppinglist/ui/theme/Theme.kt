package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette ispirata a Todoist: rosso di brand, superfici bianche/scure pulite,
// grigi tenui per testo secondario e separatori.
private val TodoistRed = Color(0xFFDC4C3E)
private val TodoistRedDark = Color(0xFFE5675A)

private val LightColors = lightColorScheme(
    primary = TodoistRed,
    onPrimary = Color(0xFFFFFFFF),
    secondary = TodoistRed,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202020),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFF6F6F6),
    onSurfaceVariant = Color(0xFF808080),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFECECEC),
)

private val DarkColors = darkColorScheme(
    primary = TodoistRedDark,
    onPrimary = Color(0xFFFFFFFF),
    secondary = TodoistRedDark,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF1C1C1C),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF333333),
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
