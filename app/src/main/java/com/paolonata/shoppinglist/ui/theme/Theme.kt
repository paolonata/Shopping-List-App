package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R

// ══ Organic ══
// Il design system del redesign: terra cotta e salvia su fondo crema, forme
// molto arrotondate, Caprasimo sui titoli e Figtree per il resto. Le rampe
// 100-900 sono generate in OKLCH su un'unica scala di luminosità, quindi lo
// stesso gradino di ruoli diversi "pesa" uguale: si usano quelle, non tinte
// improvvisate.

private val Caprasimo = FontFamily(Font(R.font.caprasimo_regular, FontWeight.Normal))

private val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

/**
 * Le rampe del design system, che [androidx.compose.material3.ColorScheme]
 * non sa rappresentare: lì ci sono i ruoli (primary, surface…), qui i
 * gradini. Le schermate leggono da qui tramite [Organic].
 */
data class OrganicPalette(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
    val accent2: Color,

    val neutral100: Color,
    val neutral200: Color,
    val neutral300: Color,
    val neutral400: Color,
    val neutral500: Color,
    val neutral600: Color,
    val neutral700: Color,
    val neutral800: Color,
    val neutral900: Color,

    val accent100: Color,
    val accent200: Color,
    val accent300: Color,
    val accent400: Color,
    val accent500: Color,
    val accent600: Color,
    val accent700: Color,
    val accent800: Color,
    val accent900: Color,

    val accent2100: Color,
    val accent2200: Color,
    val accent2300: Color,
    val accent2400: Color,
    val accent2500: Color,
    val accent2600: Color,
    val accent2700: Color,
    val accent2800: Color,
    val accent2900: Color,

    /** Il crema che sta sopra il terracotta pieno (accent-100). */
    val onAccent: Color,
    /** Il verde chiarissimo che sta sopra la salvia piena. */
    val onAccent2: Color,
    val dark: Boolean,
)

private val LightPalette = OrganicPalette(
    bg = Color(0xFFF5EAD8),
    surface = Color(0xFFEBDDC5),
    text = Color(0xFF201E1D),
    accent = Color(0xFFC67139),
    accent2 = Color(0xFF7A8A5E),

    neutral100 = Color(0xFFF9F4ED),
    neutral200 = Color(0xFFEEE7DB),
    neutral300 = Color(0xFFDCD3C4),
    neutral400 = Color(0xFFC0B6A5),
    neutral500 = Color(0xFFA19786),
    neutral600 = Color(0xFF82796A),
    neutral700 = Color(0xFF645C50),
    neutral800 = Color(0xFF474238),
    neutral900 = Color(0xFF2E2B25),

    accent100 = Color(0xFFFFF2EB),
    accent200 = Color(0xFFFFE1D0),
    accent300 = Color(0xFFFFC6A5),
    accent400 = Color(0xFFF6A06B),
    accent500 = Color(0xFFD67F48),
    accent600 = Color(0xFFB2622D),
    accent700 = Color(0xFF8C491A),
    accent800 = Color(0xFF643312),
    accent900 = Color(0xFF402310),

    accent2100 = Color(0xFFF0FAE1),
    accent2200 = Color(0xFFE1EECC),
    accent2300 = Color(0xFFCCDBB2),
    accent2400 = Color(0xFFAEBF92),
    accent2500 = Color(0xFF8FA073),
    accent2600 = Color(0xFF728157),
    accent2700 = Color(0xFF56633F),
    accent2800 = Color(0xFF3D472B),
    accent2900 = Color(0xFF272E1B),

    onAccent = Color(0xFFFFF2EB),
    onAccent2 = Color(0xFFF4F8EA),
    dark = false,
)

// Sul fondo scuro le rampe si percorrono al contrario: i gradini chiari
// diventano il testo, quelli scuri i riempimenti. Gli accenti restano gli
// stessi colori, ma si sceglie un gradino più chiaro perché il contrasto
// col fondo scuro regge (il readme del design system dice esattamente
// questo: 400 invece di 600 su fondo scuro).
private val DarkPalette = LightPalette.copy(
    bg = Color(0xFF1B1916),
    surface = Color(0xFF2E2B25),
    text = Color(0xFFF7F1E6),

    neutral100 = Color(0xFF272420),
    neutral200 = Color(0xFF332F29),
    neutral300 = Color(0xFF474238),
    neutral400 = Color(0xFF645C50),
    neutral500 = Color(0xFF82796A),
    neutral600 = Color(0xFFA19786),
    neutral700 = Color(0xFFC0B6A5),
    neutral800 = Color(0xFFDCD3C4),
    neutral900 = Color(0xFFF9F4ED),

    accent200 = Color(0xFF4A2A16),
    accent300 = Color(0xFF6A3C1E),
    accent600 = Color(0xFFD67F48),
    accent700 = Color(0xFFF6A06B),
    accent800 = Color(0xFFFFC6A5),

    accent2200 = Color(0xFF303A24),
    accent2300 = Color(0xFF3D472B),
    accent2600 = Color(0xFF8FA073),
    accent2700 = Color(0xFFAEBF92),
    accent2800 = Color(0xFFCCDBB2),

    onAccent = Color(0xFF2A1408),
    onAccent2 = Color(0xFF1E2413),
    dark = true,
)

private val LocalOrganicPalette = staticCompositionLocalOf { LightPalette }

/** Le rampe del design system, dentro un composable. */
val Organic: OrganicPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalOrganicPalette.current

/** I raggi del design system: contenitori tondi, bottoni a pillola. */
object OrganicShape {
    val sm = 8.dp
    val md = 16.dp
    val lg = 28.dp
    val pill = 999.dp
}

private fun typographyOf(): Typography = Typography(
    // Le cifre grandi delle card di riepilogo.
    displayLarge = TextStyle(fontFamily = Caprasimo, fontSize = 46.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Caprasimo, fontSize = 42.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = Caprasimo, fontSize = 40.sp, lineHeight = 40.sp),
    // I titoli delle schermate e dei fogli.
    headlineLarge = TextStyle(fontFamily = Caprasimo, fontSize = 29.sp, lineHeight = 33.sp),
    headlineMedium = TextStyle(fontFamily = Caprasimo, fontSize = 22.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = Caprasimo, fontSize = 19.sp, lineHeight = 22.sp),
    // I titoli di sezione, sempre in Caprasimo.
    titleLarge = TextStyle(fontFamily = Caprasimo, fontSize = 16.sp, lineHeight = 19.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontSize = 11.5.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
)

private fun schemeOf(p: OrganicPalette) = if (p.dark) {
    darkColorScheme(
        primary = p.accent600,
        onPrimary = p.onAccent,
        primaryContainer = p.accent200,
        onPrimaryContainer = p.accent800,
        secondary = p.accent2600,
        onSecondary = p.onAccent2,
        secondaryContainer = p.accent2200,
        onSecondaryContainer = p.accent2800,
        background = p.bg,
        onBackground = p.text,
        surface = p.neutral100,
        onSurface = p.text,
        surfaceVariant = p.neutral200,
        onSurfaceVariant = p.neutral700,
        outline = p.neutral300,
        outlineVariant = p.neutral200,
        error = p.accent600,
        onError = p.onAccent,
    )
} else {
    lightColorScheme(
        primary = p.accent600,
        onPrimary = p.onAccent,
        primaryContainer = p.accent200,
        onPrimaryContainer = p.accent800,
        secondary = p.accent2600,
        onSecondary = p.onAccent2,
        secondaryContainer = p.accent2200,
        onSecondaryContainer = p.accent2800,
        background = p.bg,
        onBackground = p.text,
        surface = p.neutral100,
        onSurface = p.text,
        surfaceVariant = p.neutral200,
        onSurfaceVariant = p.neutral700,
        outline = p.neutral300,
        outlineVariant = p.neutral200,
        error = p.accent700,
        onError = p.onAccent,
    )
}

@Composable
fun ListaSpesaTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val mode by ThemePrefs.mode.collectAsState()
    val palette = when (mode) {
        ThemeMode.AUTO -> if (systemDark) DarkPalette else LightPalette
        ThemeMode.LIGHT -> LightPalette
        ThemeMode.DARK -> DarkPalette
    }
    CompositionLocalProvider(LocalOrganicPalette provides palette) {
        MaterialTheme(
            colorScheme = schemeOf(palette),
            typography = typographyOf(),
            content = content,
        )
    }
}
