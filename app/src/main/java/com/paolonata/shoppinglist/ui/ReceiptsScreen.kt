package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.receipts.DeadlineLevel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ITALIAN)
private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

/**
 * L'archivio degli scontrini.
 *
 * Stessa grammatica della lista: intestazione con il titolo al centro,
 * sezioni in `titleLarge` col conteggio accanto, righe separate da linee
 * sottili e niente colore. Qui le sezioni sono i mesi, e a destra di
 * ognuna c'è quanto si è speso — l'unica cifra che si guarda davvero
 * scorrendo un archivio di spese.
 */
@Composable
fun ReceiptsScreen(
    receipts: List<ReceiptWithPhotos>,
    deadlines: List<UpcomingDeadline>,
    section: AppSection,
    onSectionChange: (AppSection) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickPdf: () -> Unit,
    onOpen: (Long) -> Unit,
    categories: List<ReceiptCategoryEntity>,
    remindersOn: Boolean,
    onToggleReminders: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    // Le categorie che compaiono davvero: filtrare per una casella vuota
    // non serve a nessuno, e la riga resta corta.
    val used = remember(receipts, categories) {
        categories.filter { c -> receipts.any { it.receipt.categoryId == c.id } }
    }
    val byId = remember(categories) { categories.associateBy { it.id } }
    val shown = remember(receipts, filter) {
        if (filter == null) receipts else receipts.filter { it.receipt.categoryId == filter }
    }
    val byMonth = remember(shown) { groupByMonth(shown) }
    val urgent = remember(deadlines) { deadlines.filter { it.status.urgent } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    SectionSwitch(
                        current = section,
                        onSelect = onSectionChange,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    IconButton(
                        onClick = onToggleReminders,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = if (remindersOn) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsNone
                            },
                            contentDescription = stringResource(R.string.receipt_reminders_cd),
                            tint = if (remindersOn) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        bottomBar = {
            CaptureBar(
                onTakePhoto = onTakePhoto,
                onPickPhoto = onPickPhoto,
                onPickPdf = onPickPdf,
            )
        },
    ) { padding ->
        if (receipts.isEmpty()) {
            ReceiptsEmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
            ) {
                if (urgent.isNotEmpty()) {
                    item(key = "deadlines") { DeadlineBanner(urgent) }
                }
                if (used.size > 1) {
                    item(key = "filters") {
                        CategoryFilters(
                            used = used,
                            selected = filter,
                            onSelect = { filter = it },
                        )
                    }
                }
                byMonth.forEach { (month, entries) ->
                    item(key = "hdr_$month") {
                        MonthHeader(month = month, entries = entries)
                    }
                    items(entries, key = { it.receipt.id }) { entry ->
                        ReceiptRow(
                            entry = entry,
                            category = byId[entry.receipt.categoryId],
                            onClick = { onOpen(entry.receipt.id) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/**
 * Il filtro per categoria: una riga di pastiglie che scorre, quella
 * attiva piena. Compare solo quando le categorie in archivio sono più di
 * una — con tutti scontrini della spesa non filtrerebbe niente.
 */
@Composable
private fun CategoryFilters(
    used: List<ReceiptCategoryEntity>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterPill(
                label = stringResource(R.string.filter_all),
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(used, key = { it.id }) { category ->
            FilterPill(
                label = "${category.emoji}  ${category.label}",
                selected = selected == category.id,
                onClick = { onSelect(if (selected == category.id) null else category.id) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.onBackground)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun MonthHeader(month: String, entries: List<ReceiptWithPhotos>) {
    val total = entries.mapNotNull { it.receipt.amountCents }.sum()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = month.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entries.size.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (total > 0) {
            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReceiptRow(
    entry: ReceiptWithPhotos,
    category: ReceiptCategoryEntity?,
    onClick: () -> Unit,
) {
    val receipt = entry.receipt
    val date = runCatching { LocalDate.parse(receipt.date) }.getOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReceiptThumbnail(path = entry.photos.minByOrNull { it.position }?.path)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receipt.title.ifBlank { stringResource(R.string.receipt_untitled) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (category != null) {
                        append(category.emoji)
                        append(' ')
                        append(category.label)
                    }
                    if (date != null) {
                        if (isNotEmpty()) append(" · ")
                        append(date.format(DAY_FORMAT))
                    }
                    if (entry.photos.size > 1) {
                        append(" · ")
                        append(entry.photos.size)
                        append(" foto")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = receipt.amountCents?.let { formatMoney(it) } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (receipt.amountCents != null) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * La miniatura. Le foto stanno su disco, quindi si leggono qui a bassa
 * risoluzione: caricare 1800 px per un riquadro da 44 dp riempirebbe la
 * memoria scorrendo l'elenco.
 */
@Composable
private fun ReceiptThumbnail(path: String?) {
    val bitmap = remember(path) { path?.let { decodeThumbnail(it) } }
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
    ) {
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
}

private fun decodeThumbnail(path: String): android.graphics.Bitmap? = runCatching {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 220) sample *= 2
    android.graphics.BitmapFactory.decodeFile(
        path,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
    )
}.getOrNull()

/**
 * Quello che sta per scadere, in cima all'archivio. Senza colore: il peso
 * del testo basta a distinguerlo, ed è coerente col resto dell'app.
 */
@Composable
private fun DeadlineBanner(urgent: List<UpcomingDeadline>) {
    val first = urgent.first()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = first.kind.emoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (urgent.size == 1) {
                    stringResource(R.string.deadline_one, first.kind.label.lowercase(), first.status.text)
                } else {
                    stringResource(R.string.deadline_many, urgent.size, first.status.text)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = first.entry.receipt.title.ifBlank { stringResource(R.string.receipt_untitled) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Barra fissa in basso, gemella di quella della lista: le due strade per entrare. */
@Composable
private fun CaptureBar(
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickPdf: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaptureButton(
            label = stringResource(R.string.receipts_take_photo),
            icon = Icons.Default.PhotoCamera,
            primary = true,
            onClick = onTakePhoto,
            modifier = Modifier.weight(1f),
        )
        CaptureButton(
            label = stringResource(R.string.receipts_from_gallery),
            icon = Icons.Default.PhotoLibrary,
            primary = false,
            onClick = onPickPhoto,
            modifier = Modifier.weight(1f),
        )
        // Le ricevute che arrivano per email non si fotografano.
        IconOnlyButton(
            icon = Icons.Default.PictureAsPdf,
            contentDescription = stringResource(R.string.receipts_from_pdf),
            onClick = onPickPdf,
        )
    }
}

@Composable
private fun IconOnlyButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun CaptureButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .then(
                if (primary) {
                    Modifier.background(MaterialTheme.colorScheme.onBackground)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ReceiptsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.receipts_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.receipts_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun groupByMonth(receipts: List<ReceiptWithPhotos>): List<Pair<String, List<ReceiptWithPhotos>>> =
    receipts
        .groupBy { entry ->
            runCatching { LocalDate.parse(entry.receipt.date).format(MONTH_FORMAT) }
                .getOrDefault("")
        }
        .toList()

/** 4390 → "43,90 €". Gli importi sono in centesimi: niente virgole ballerine. */
internal fun formatMoney(cents: Long): String =
    String.format(Locale.ITALIAN, "%.2f €", cents / 100.0)

internal fun DeadlineLevel.isPast(): Boolean = this == DeadlineLevel.EXPIRED
