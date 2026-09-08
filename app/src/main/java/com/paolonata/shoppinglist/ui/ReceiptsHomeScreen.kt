package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.ui.theme.BigCardShape
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.MESI
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OrganicChip
import com.paolonata.shoppinglist.ui.theme.PillShape
import com.paolonata.shoppinglist.ui.theme.RoundIconButton
import com.paolonata.shoppinglist.ui.theme.SectionRule
import com.paolonata.shoppinglist.ui.theme.Tone
import com.paolonata.shoppinglist.ui.theme.daysLeft
import com.paolonata.shoppinglist.ui.theme.eur
import com.paolonata.shoppinglist.ui.theme.fmtDate
import com.paolonata.shoppinglist.ui.theme.money
import com.paolonata.shoppinglist.ui.theme.monthKey
import com.paolonata.shoppinglist.ui.theme.monthLabel
import com.paolonata.shoppinglist.ui.theme.statusOf
import com.paolonata.shoppinglist.ui.theme.toneColors
import java.io.File

/** Come si guarda l'archivio: a schede o a elenco. */
enum class ReceiptsLayout { GRID, LIST }

/**
 * L'archivio degli scontrini: quanto è stato speso in cima, poi i filtri
 * per mese e categoria, poi le foto. Le due strisce sopra il totale sono
 * il ponte con l'altra metà dell'app — quello che sta per scadere e quello
 * che manca ancora dalla lista della spesa.
 */
@Composable
fun ReceiptsHomeScreen(
    receipts: List<ReceiptWithPhotos>,
    categories: List<ReceiptCategoryEntity>,
    deadlines: List<UpcomingDeadline>,
    itemsToBuy: Int,
    onOpen: (Long) -> Unit,
    onGoDeadlines: () -> Unit,
    onGoSpesa: () -> Unit,
    onGoStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var month by remember { mutableStateOf(ALL) }
    var category by remember { mutableStateOf(ALL) }
    var layout by remember { mutableStateOf(ReceiptsLayout.GRID) }

    val catById = categories.associateBy { it.id }
    val trimmedQuery = query.trim().lowercase()

    val filtered = receipts.filter { entry ->
        val r = entry.receipt
        if (trimmedQuery.isNotEmpty()) {
            return@filter (r.title + " " + r.note + " " + (r.amount ?: "") + " " + (r.placeName ?: ""))
                .lowercase().contains(trimmedQuery)
        }
        if (month != ALL && monthKey(r.date) != month) return@filter false
        when (category) {
            ALL -> true
            FAV -> r.favorite
            else -> r.categoryId == category
        }
    }

    val withAmount = filtered.mapNotNull { it.receipt.amount }
    val total = withAmount.sum()
    val missing = filtered.size - withAmount.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 22.dp, bottom = 108.dp),
    ) {
        item("header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(PillShape)
                        .background(Organic.accent600),
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Organic.onAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.receipts_home_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Organic.text,
                    )
                    Text(
                        text = stringResource(R.string.receipts_home_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Organic.neutral700,
                    )
                }
                RoundIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = stringResource(R.string.receipts_search_cd),
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    },
                )
            }
        }

        if (searchOpen) {
            item("search") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(Organic.neutral100)
                        .border(1.5.dp, Organic.neutral300, PillShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Organic.neutral600,
                        modifier = Modifier.size(17.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.receipts_search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Organic.neutral600,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.merge(
                                MaterialTheme.typography.bodyLarge.copy(color = Organic.text),
                            ),
                            cursorBrush = SolidColor(Organic.accent600),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        val imminent = deadlines.filter { it.status.days in 0..3 }
        if (imminent.isNotEmpty()) {
            item("alert") {
                StripButton(
                    background = Organic.accent200,
                    contentColor = Organic.accent800,
                    border = null,
                    onClick = onGoDeadlines,
                    leading = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Organic.accent800,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    text = if (imminent.size == 1) {
                        val d = imminent.first()
                        "${d.kind.label} ${d.status.text} · ${d.entry.receipt.title}"
                    } else {
                        stringResource(R.string.receipts_alert_many, imminent.size)
                    },
                )
            }
        }

        if (itemsToBuy > 0) {
            item("spesastrip") {
                StripButton(
                    background = Organic.accent2100,
                    contentColor = Organic.accent2800,
                    border = Organic.accent2300,
                    onClick = onGoSpesa,
                    leading = { Text("🛒", fontSize = 16.sp) },
                    text = if (itemsToBuy == 1) {
                        stringResource(R.string.receipts_spesa_strip_one)
                    } else {
                        stringResource(R.string.receipts_spesa_strip_many, itemsToBuy)
                    },
                )
            }
        }

        item("total") {
            val previousKey = if (month != ALL) {
                val p = month.split("-")
                val y = p.getOrNull(0)?.toIntOrNull()
                val m = p.getOrNull(1)?.toIntOrNull()
                if (y != null && m != null) {
                    val d = java.time.LocalDate.of(y, m, 1).minusMonths(1)
                    "%04d-%02d".format(d.year, d.monthValue)
                } else {
                    null
                }
            } else {
                null
            }
            val previousTotal = previousKey?.let { key ->
                receipts.filter { monthKey(it.receipt.date) == key }.mapNotNull { it.receipt.amount }.sum()
            } ?: 0.0
            val delta = if (previousKey != null && previousTotal > 0 && total > 0 && trimmedQuery.isEmpty()) {
                val percent = Math.round((total - previousTotal) / previousTotal * 100).toInt()
                val sign = if (percent > 0) "+" else "−"
                val previousMonthName = MESI.getOrElse(
                    (previousKey.split("-").getOrNull(1)?.toIntOrNull() ?: 1) - 1,
                ) { "" }
                stringResource(R.string.receipts_delta, sign + Math.abs(percent), previousMonthName)
            } else if (withAmount.isNotEmpty()) {
                stringResource(R.string.receipts_average, eur(total / withAmount.size))
            } else {
                stringResource(R.string.receipts_no_amounts)
            }
            TotalCard(
                delta = delta,
                label = when {
                    trimmedQuery.isNotEmpty() -> stringResource(R.string.receipts_total_results)
                    month == ALL -> stringResource(R.string.receipts_total_archive)
                    else -> monthLabel(month)
                },
                total = total,
                count = filtered.size,
                missing = missing,
                receipts = receipts,
                referenceMonth = if (month == ALL) monthKey(java.time.LocalDate.now().toString()) else month,
                onClick = onGoStats,
            )
        }

        item("months") {
            val keys = receipts.map { monthKey(it.receipt.date) }.distinct().sortedDescending()
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            ) {
                MonthChip(
                    label = stringResource(R.string.receipts_month_all),
                    sub = if (receipts.size == 1) {
                        stringResource(R.string.receipts_count_one)
                    } else {
                        stringResource(R.string.receipts_count_many, receipts.size)
                    },
                    active = month == ALL,
                    onClick = { month = ALL },
                )
                keys.forEach { key ->
                    val monthTotal = receipts
                        .filter { monthKey(it.receipt.date) == key }
                        .mapNotNull { it.receipt.amount }
                        .sum()
                    MonthChip(
                        label = MESI.getOrElse(key.split("-").getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0) { key },
                        sub = eur(monthTotal),
                        active = month == key,
                        onClick = { month = key },
                    )
                }
            }
        }

        item("chips") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    OrganicChip(
                        label = stringResource(R.string.receipts_cat_all),
                        active = category == ALL,
                        onClick = { category = ALL },
                    )
                    if (receipts.any { it.receipt.favorite }) {
                        OrganicChip(
                            label = stringResource(R.string.receipts_cat_fav),
                            active = category == FAV,
                            onClick = { category = FAV },
                        )
                    }
                    categories.filter { c -> receipts.any { it.receipt.categoryId == c.id } }.forEach { c ->
                        OrganicChip(
                            label = "${c.emoji} ${c.label}",
                            active = category == c.id,
                            onClick = { category = c.id },
                        )
                    }
                }
                RoundIconButton(
                    icon = if (layout == ReceiptsLayout.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = stringResource(R.string.receipts_layout_cd),
                    onClick = {
                        layout = if (layout == ReceiptsLayout.GRID) ReceiptsLayout.LIST else ReceiptsLayout.GRID
                    },
                    size = 34.dp,
                )
            }
        }

        if (trimmedQuery.isNotEmpty()) {
            item("results") {
                Text(
                    text = if (filtered.isEmpty()) {
                        stringResource(R.string.receipts_no_results, query)
                    } else if (filtered.size == 1) {
                        stringResource(R.string.receipts_results_one, query)
                    } else {
                        stringResource(R.string.receipts_results_many, filtered.size, query)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Organic.neutral700,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                )
            }
        }

        val groups = filtered.groupBy { monthKey(it.receipt.date) }.toList()
            .sortedByDescending { it.first }

        groups.forEach { (key, entries) ->
            item("h-$key") {
                SectionRule(
                    title = monthLabel(key),
                    trailing = eur(entries.mapNotNull { it.receipt.amount }.sum()),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                )
            }
            if (layout == ReceiptsLayout.GRID) {
                val rows = entries.chunked(2)
                items(rows.size, key = { "g-$key-$it" }) { index ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    ) {
                        rows[index].forEach { entry ->
                            ReceiptGridCard(
                                entry = entry,
                                category = catById[entry.receipt.categoryId],
                                onClick = { onOpen(entry.receipt.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rows[index].size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(entries.size, key = { "l-$key-$it" }) { index ->
                    val entry = entries[index]
                    ReceiptListRow(
                        entry = entry,
                        category = catById[entry.receipt.categoryId],
                        onClick = { onOpen(entry.receipt.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.5.dp),
                    )
                }
            }
        }

        if (groups.isEmpty() && trimmedQuery.isEmpty()) {
            item("empty") {
                Text(
                    text = stringResource(R.string.receipts_empty_filters),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Organic.neutral700,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(34.dp),
                )
            }
        }
    }
}

private const val ALL = "all"
private const val FAV = "fav"

/** Le due strisce cliccabili sopra il totale. */
@Composable
private fun StripButton(
    background: Color,
    contentColor: Color,
    border: Color?,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.5.dp, border, CardShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
    ) {
        leading()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Il riquadro terracotta col totale del periodo e la sparkline. */
@Composable
private fun TotalCard(
    delta: String,
    label: String,
    total: Double,
    count: Int,
    missing: Int,
    receipts: List<ReceiptWithPhotos>,
    referenceMonth: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = BigCardShape,
        color = Organic.accent600,
        shadowElevation = 6.dp,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 46.dp, y = (-56).dp)
                    .size(170.dp)
                    .clip(PillShape)
                    .background(Organic.onAccent.copy(alpha = 0.09f)),
            )
            Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.7.sp),
                    color = Organic.onAccent.copy(alpha = 0.85f),
                )
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "€",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Organic.onAccent.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 5.dp, end = 4.dp),
                    )
                    Text(
                        text = money(total),
                        style = MaterialTheme.typography.displayLarge,
                        color = Organic.onAccent,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 9.dp),
                ) {
                    Text(
                        text = if (count == 1) {
                            stringResource(R.string.receipts_count_one)
                        } else {
                            stringResource(R.string.receipts_count_many, count)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Organic.onAccent,
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(PillShape)
                            .background(Organic.onAccent.copy(alpha = 0.5f)),
                    )
                    Text(
                        text = delta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Organic.onAccent,
                    )
                }
                if (missing > 0) {
                    Text(
                        text = if (missing == 1) {
                            stringResource(R.string.receipts_missing_one)
                        } else {
                            stringResource(R.string.receipts_missing_many, missing)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Organic.onAccent,
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .clip(PillShape)
                            .background(Organic.onAccent.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Sparkline(
                    receipts = receipts,
                    referenceMonth = referenceMonth,
                    onClick = onClick,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

/** Gli ultimi sei mesi come colonnine: il mese di riferimento è pieno. */
@Composable
private fun Sparkline(
    receipts: List<ReceiptWithPhotos>,
    referenceMonth: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts = referenceMonth.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return
    val monthNumber = parts.getOrNull(1)?.toIntOrNull() ?: return
    val data = (5 downTo 0).map { back ->
        val date = java.time.LocalDate.of(year, monthNumber, 1).minusMonths(back.toLong())
        val key = "%04d-%02d".format(date.year, date.monthValue)
        key to receipts.filter { monthKey(it.receipt.date) == key }.mapNotNull { it.receipt.amount }.sum()
    }
    val max = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick),
    ) {
        data.forEach { (key, value) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((6.0 + value / max * 40.0).toFloat().dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(
                        if (key == referenceMonth) Organic.onAccent else Organic.onAccent.copy(alpha = 0.34f),
                    ),
            )
        }
    }
}

@Composable
private fun MonthChip(label: String, sub: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(PillShape)
            .background(if (active) Organic.accent600 else Color.Transparent)
            .border(1.5.dp, if (active) Organic.accent600 else Organic.neutral300, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Organic.onAccent else Organic.neutral800,
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) Organic.onAccent.copy(alpha = 0.75f) else Organic.neutral600,
        )
    }
}

/** La scheda con la foto: importo sopra la sfumatura, titolo e data sotto. */
@Composable
private fun ReceiptGridCard(
    entry: ReceiptWithPhotos,
    category: ReceiptCategoryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = entry.receipt
    val days = if (r.returnUntil != null && r.returnDoneAt == null) daysLeft(r.returnUntil) else null
    val untitled = stringResource(R.string.receipts_untitled)
    val showChip = days != null && days <= 14 && days >= -3
    Surface(
        shape = CardShape,
        color = Organic.neutral100,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Organic.neutral300),
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(
                        Brush.linearGradient(listOf(Organic.neutral200, Organic.neutral300)),
                    ),
            ) {
                Thumbnail(path = entry.photos.minByOrNull { it.position }?.path)
                if (showChip && days != null) {
                    val status = statusOf(days)
                    val (bg, fg) = toneColors(status?.first ?: Tone.PLAIN)
                    Text(
                        text = "↩️ " + when {
                            days < 0L -> stringResource(R.string.receipts_chip_expired)
                            days == 0L -> stringResource(R.string.receipts_chip_today)
                            days == 1L -> stringResource(R.string.receipts_chip_one_day)
                            else -> stringResource(R.string.receipts_chip_days, days.toInt())
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = fg,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(PillShape)
                            .background(bg)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
                if (r.favorite) {
                    Text(
                        text = "⭐",
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 9.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x8C201E1D)),
                            ),
                        )
                        .padding(start = 11.dp, end = 11.dp, top = 16.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = if (r.amount != null) eur(r.amount) else stringResource(R.string.receipts_add_amount),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
                        color = Color(0xFFFFF8EF),
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 11.dp)) {
                Text(
                    text = "${category?.emoji ?: "🧾"} ${r.title.ifBlank { untitled }}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Organic.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = fmtDate(r.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = Organic.neutral700,
                )
            }
        }
    }
}

/** La stessa cosa, ma stretta: emoji, titolo, sottotitolo, importo. */
@Composable
private fun ReceiptListRow(
    entry: ReceiptWithPhotos,
    category: ReceiptCategoryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = entry.receipt
    val untitled = stringResource(R.string.receipts_untitled)
    val photosSuffix = if (entry.photos.size > 1) " · ${entry.photos.size} foto" else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Organic.neutral100)
            .border(1.5.dp, Organic.neutral300, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Organic.neutral200, Organic.neutral300))),
        ) {
            val path = entry.photos.minByOrNull { it.position }?.path
            if (path != null) {
                Thumbnail(path = path)
            } else {
                Text(text = category?.emoji ?: "🧾", fontSize = 17.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = r.title.ifBlank { untitled },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Organic.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${category?.label ?: "Altro"} · ${fmtDate(r.date)}$photosSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = Organic.neutral700,
            )
        }
        Text(
            text = if (r.amount != null) eur(r.amount) else stringResource(R.string.receipts_add_amount),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp),
            color = Organic.text,
            maxLines = 1,
        )
    }
}

/**
 * La foto, letta a bassa risoluzione: caricarla intera per un riquadro da
 * poche decine di dp riempirebbe la memoria scorrendo l'archivio.
 */
@Composable
private fun Thumbnail(path: String?) {
    val bitmap = remember(path) { path?.let { decodeThumbnail(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun decodeThumbnail(path: String): android.graphics.Bitmap? = runCatching {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 420) sample *= 2
    android.graphics.BitmapFactory.decodeFile(
        path,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
    )
}.getOrNull()
