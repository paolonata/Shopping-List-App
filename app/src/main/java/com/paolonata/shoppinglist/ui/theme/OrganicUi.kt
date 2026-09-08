package com.paolonata.shoppinglist.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/* ══ forme ══ */

val PillShape = RoundedCornerShape(OrganicShape.pill)
val CardShape = RoundedCornerShape(OrganicShape.md)
val BigCardShape = RoundedCornerShape(OrganicShape.lg)

/* ══ numeri e date, all'italiana ══ */

private val itMoney: NumberFormat
    get() = NumberFormat.getNumberInstance(Locale.ITALIAN).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

/** "84,32" — solo le cifre, per le card dove l'euro è un glifo a parte. */
fun money(value: Double?): String = if (value == null) "—" else itMoney.format(value)

/** "€ 84,32". */
fun eur(value: Double?): String = if (value == null) "—" else "€ " + itMoney.format(value)

private val DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

val MESI = listOf(
    "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
    "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
)

val MESI_BREVI = listOf(
    "gen", "feb", "mar", "apr", "mag", "giu",
    "lug", "ago", "set", "ott", "nov", "dic",
)

fun fmtDate(iso: String?): String = parseIso(iso)?.format(DATE_SHORT) ?: "—"

fun fmtLong(iso: String?): String = parseIso(iso)?.let {
    "${it.dayOfMonth} ${MESI[it.monthValue - 1]} ${it.year}"
} ?: "—"

fun parseIso(iso: String?): LocalDate? =
    iso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

/** La chiave del mese ("2026-09") con cui si raggruppa l'archivio. */
fun monthKey(iso: String?): String = iso?.take(7) ?: ""

fun monthLabel(key: String): String {
    val parts = key.split("-")
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return key
    return "${MESI[m - 1]} ${parts[0]}"
}

fun daysLeft(iso: String?, today: LocalDate = LocalDate.now()): Long? =
    parseIso(iso)?.let { ChronoUnit.DAYS.between(today, it) }

/* ══ pillole di stato ══ */

/**
 * Quanto stringe una scadenza. Sono i cinque toni del design: dal
 * terracotta pieno di "scade oggi" al grigio di quello che è già passato.
 */
enum class Tone { HOT, WARM, CALM, PLAIN, GREY }

/** Le stesse parole del prototipo: "fra 3 giorni", "scaduto ieri". */
fun statusOf(days: Long?): Pair<Tone, String>? {
    if (days == null) return null
    if (days < 0) {
        val gone = -days
        return Tone.GREY to if (gone == 1L) "scaduto ieri" else "scaduto $gone giorni fa"
    }
    if (days == 0L) return Tone.HOT to "scade oggi"
    if (days == 1L) return Tone.HOT to "scade domani"
    if (days <= 7) return Tone.WARM to "fra $days giorni"
    if (days <= 30) return Tone.CALM to "fra $days giorni"
    if (days <= 60) return Tone.PLAIN to "fra $days giorni"
    val mesi = Math.round(days / 30.44)
    if (mesi <= 20) return Tone.PLAIN to "fra $mesi mesi"
    val anni = Math.round(days / 365.25)
    return Tone.PLAIN to if (anni == 1L) "fra un anno" else "fra $anni anni"
}

@Composable
fun toneColors(tone: Tone): Pair<Color, Color> = when (tone) {
    Tone.HOT -> Organic.accent600 to Organic.onAccent
    Tone.WARM -> Organic.accent300 to Organic.accent800
    Tone.CALM -> Organic.accent2200 to Organic.accent2800
    Tone.PLAIN -> Organic.neutral200 to Organic.neutral700
    Tone.GREY -> Organic.neutral300 to Organic.neutral700
}

@Composable
fun TonePill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val (bg, fg) = toneColors(tone)
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

/* ══ mattoni ══ */

/**
 * Il titolo di sezione del design: parola in Caprasimo, filo sottile che
 * arriva fino in fondo, conteggio a destra.
 */
@Composable
fun SectionRule(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = titleStyle, color = Organic.text)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .clip(PillShape)
                .background(Organic.neutral300),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = Organic.neutral700,
            )
        }
    }
}

/** Il bottone a pillola col solo bordo: "Rimuovi quelli presi", "Cancella". */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    contentColor: Color? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor ?: Organic.neutral800,
        maxLines = 1,
        modifier = modifier
            .clip(PillShape)
            .border(1.5.dp, borderColor ?: Organic.neutral300, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

/** Il bottone pieno a pillola, largo quanto la riga. */
@Composable
fun FilledPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color? = null,
    content: Color? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val bg = container ?: Organic.accent600
    val fg = content ?: Organic.onAccent
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(if (enabled) bg else Organic.neutral300)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) fg else Organic.neutral600,
        )
    }
}

/** Il bottoncino tondo col bordo che il design mette negli header. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    filled: Boolean = false,
    tint: Color? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(PillShape)
            .then(if (filled) Modifier.background(Organic.neutral100) else Modifier)
            .border(1.5.dp, Organic.neutral300, PillShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: Organic.neutral800,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Il controllo segmentato dentro la sua pillola grigia. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(Organic.neutral200)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (active) Organic.accent600 else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp, horizontal = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Organic.onAccent else Organic.neutral700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** La pillola-filtro delle categorie e dei mesi. */
@Composable
fun OrganicChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    activeContent: Color? = null,
) {
    val bg = if (active) (activeColor ?: Organic.accent2600) else Color.Transparent
    val fg = if (active) (activeContent ?: Organic.onAccent2) else Organic.neutral800
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .border(1.5.dp, if (active) bg else Organic.neutral300, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

/** La superficie chiara e bordata su cui poggia quasi tutto. */
@Composable
fun OrganicCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    background: Color? = null,
    border: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = shape,
        color = background ?: Organic.neutral100,
        border = BorderStroke(1.5.dp, border ?: Organic.neutral300),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        content()
    }
}

/** La striscia informativa colorata (avvisi, note, privacy). */
@Composable
fun NoteStrip(
    leading: @Composable () -> Unit,
    title: String?,
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
        trailing?.invoke()
    }
}

/** L'interruttore del design: pista a pillola e pallino che scorre. */
@Composable
fun OrganicSwitch(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = modifier
            .width(46.dp)
            .height(27.dp)
            .clip(PillShape)
            .background(if (checked) Organic.accent2600 else Organic.neutral300)
            .clickable(onClick = onToggle)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(21.dp)
                .clip(PillShape)
                .background(Organic.neutral100),
        )
    }
}

/**
 * Il foglio che sale dal basso: fondo scurito che si può toccare per
 * chiudere, e sopra la superficie color carta con la maniglia.
 */
@Composable
fun BottomSheetOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x6B201E1D)),
    ) {
        // Il foglio non copre mai del tutto quello che c'è sotto: si deve
        // sempre vedere che si è "sopra" a qualcosa, e che basta toccare
        // fuori per tornarci.
        val sheetMaxHeight = maxHeight * 0.9f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )
        Surface(
            shape = RoundedCornerShape(topStart = OrganicShape.lg, topEnd = OrganicShape.lg),
            color = Organic.bg,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight),
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 26.dp, top = 8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(PillShape)
                            .background(Organic.neutral400),
                    )
                }
                content()
            }
        }
    }
}

/** Il messaggio scuro a pillola che compare in basso dopo un'azione. */
@Composable
fun OrganicToast(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, PillShape)
            .clip(PillShape)
            .background(Organic.neutral900)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Check,
            contentDescription = null,
            tint = Organic.accent2300,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Organic.neutral100,
            modifier = Modifier.heightIn(min = 0.dp),
        )
    }
}
