package com.paolonata.shoppinglist.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.Receipt
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.receipts.DeadlineKind
import com.paolonata.shoppinglist.receipts.Deadlines
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)
private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALIAN)

/**
 * Uno scontrino aperto.
 *
 * L'ordine risponde alla domanda per cui si apre uno scontrino: prima
 * *cos'è* (importo, negozio, quando), poi *cosa ci si può fare adesso* —
 * se il tempo per il reso è ancora aperto — poi la prova, cioè la foto, e
 * infine tutto il resto, modificabile riga per riga.
 */
@Composable
fun ReceiptDetailScreen(
    entry: ReceiptWithPhotos,
    categories: List<ReceiptCategoryEntity>,
    onBack: () -> Unit,
    onSave: (Receipt) -> Unit,
    onReturnDone: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRescan: () -> Unit,
    onCreateCategory: (String, String) -> Unit,
    onUpdateCategory: (ReceiptCategoryEntity, String, String) -> Unit,
    onDeleteCategory: (ReceiptCategoryEntity) -> Unit,
) {
    val context = LocalContext.current
    val receipt = entry.receipt
    val category = categories.firstOrNull { it.id == receipt.categoryId }
    val purchase = remember(receipt.date) { runCatching { LocalDate.parse(receipt.date) }.getOrNull() }

    var editingTitle by remember { mutableStateOf(false) }
    var editingAmount by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    var choosingCategory by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var zoomedPhoto by remember { mutableStateOf<String?>(null) }
    var showingOcr by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .height(48.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { shareReceipt(context, entry) }) {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = stringResource(R.string.receipt_share),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.receipt_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            item(key = "head") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        text = receipt.amountCents?.let { formatMoney(it) }
                            ?: stringResource(R.string.receipt_add_amount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (receipt.amountCents != null) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { editingAmount = true }
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        text = receipt.title.ifBlank { stringResource(R.string.receipt_untitled) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { editingTitle = true }
                            .padding(vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            category?.let { "${it.emoji} ${it.label}" },
                            purchase?.format(LONG_DATE) ?: receipt.date,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Cosa si può fare adesso: solo se il reso è ancora aperto.
            if (receipt.returnUntil != null) {
                item(key = "action") {
                    ReturnBanner(receipt = receipt, onReturnDone = onReturnDone)
                }
            }

            if (entry.photos.isNotEmpty()) {
                item(key = "photos") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(entry.photos.sortedBy { it.position }, key = { it.id }) { photo ->
                            ReceiptPage(path = photo.path, onClick = { zoomedPhoto = photo.path })
                        }
                    }
                }
            }

            item(key = "fields") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    DetailRow(
                        label = stringResource(R.string.receipt_field_category),
                        value = category?.let { "${it.emoji}  ${it.label}" } ?: "—",
                        onClick = { choosingCategory = true },
                    )
                    DetailRow(
                        label = stringResource(R.string.receipt_field_date),
                        value = purchase?.format(SHORT_DATE) ?: receipt.date,
                        onClick = { pickDate(context, purchase) { onSave(receipt.copy(date = it.toString())) } },
                    )
                    DetailRow(
                        label = stringResource(R.string.receipt_field_note),
                        value = receipt.note.ifBlank { "—" },
                        onClick = { editingNote = true },
                    )
                    DetailRow(
                        label = stringResource(R.string.receipt_field_photos),
                        value = pagesLabel(context, entry),
                        onClick = null,
                    )
                    if (entry.photos.isNotEmpty()) {
                        DetailRow(
                            label = stringResource(R.string.receipt_rescan),
                            value = stringResource(R.string.receipt_rescan_hint),
                            onClick = onRescan,
                        )
                    }
                    if (!receipt.ocrText.isNullOrBlank()) {
                        DetailRow(
                            label = stringResource(R.string.receipt_ocr_text),
                            value = stringResource(R.string.receipt_ocr_text_hint),
                            onClick = { showingOcr = true },
                        )
                    }
                }
            }

            item(key = "deadlines") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        text = stringResource(R.string.receipt_deadlines_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    DeadlineEditor(
                        kind = DeadlineKind.RETURN,
                        deadline = receipt.returnUntil,
                        presets = Deadlines.RETURN_PRESETS,
                        presetLabel = { stringResource(R.string.preset_days, it) },
                        chosen = receipt.returnDays,
                        onChoose = { days ->
                            val base = purchase ?: LocalDate.now()
                            onSave(
                                receipt.copy(
                                    returnDays = days,
                                    returnUntil = days?.let { Deadlines.returnDeadline(base, it).toString() },
                                    returnDoneAt = null,
                                ),
                            )
                        },
                    )
                    DeadlineEditor(
                        kind = DeadlineKind.WARRANTY,
                        deadline = receipt.warrantyUntil,
                        presets = Deadlines.WARRANTY_PRESETS,
                        presetLabel = { stringResource(R.string.preset_years, it) },
                        chosen = receipt.warrantyYears,
                        onChoose = { years ->
                            val base = purchase ?: LocalDate.now()
                            onSave(
                                receipt.copy(
                                    warrantyYears = years,
                                    warrantyUntil = years?.let { Deadlines.warrantyDeadline(base, it).toString() },
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    if (editingTitle) {
        TextFieldDialog(
            title = stringResource(R.string.receipt_field_shop),
            initial = receipt.title,
            onDismiss = { editingTitle = false },
            onConfirm = { onSave(receipt.copy(title = it.trim())); editingTitle = false },
        )
    }
    if (editingNote) {
        TextFieldDialog(
            title = stringResource(R.string.receipt_field_note),
            initial = receipt.note,
            onDismiss = { editingNote = false },
            onConfirm = { onSave(receipt.copy(note = it.trim())); editingNote = false },
        )
    }
    if (editingAmount) {
        TextFieldDialog(
            title = stringResource(R.string.receipt_field_amount),
            initial = receipt.amountCents?.let { String.format(Locale.ITALIAN, "%.2f", it / 100.0) }.orEmpty(),
            numeric = true,
            onDismiss = { editingAmount = false },
            onConfirm = { typed ->
                val cents = parseAmountToCents(typed)
                onSave(receipt.copy(amountCents = cents))
                editingAmount = false
            },
        )
    }
    if (choosingCategory) {
        CategoryPickerDialog(
            categories = categories,
            currentId = receipt.categoryId,
            onDismiss = { choosingCategory = false },
            onPick = { onSave(receipt.copy(categoryId = it.id)); choosingCategory = false },
            onCreate = onCreateCategory,
            onUpdate = onUpdateCategory,
            onDelete = onDeleteCategory,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.receipt_delete_title)) },
            text = { Text(stringResource(R.string.receipt_delete_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.receipt_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (showingOcr) {
        val text = receipt.ocrText.orEmpty()
        AlertDialog(
            onDismissRequest = { showingOcr = false },
            title = { Text(stringResource(R.string.receipt_ocr_text)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { shareText(context, text); showingOcr = false }) {
                    Text(stringResource(R.string.receipt_ocr_share))
                }
            },
            dismissButton = {
                TextButton(onClick = { showingOcr = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    zoomedPhoto?.let { path ->
        PhotoViewer(path = path, onDismiss = { zoomedPhoto = null })
    }
}

@Composable
private fun ReturnBanner(receipt: Receipt, onReturnDone: (Boolean) -> Unit) {
    val date = runCatching { LocalDate.parse(receipt.returnUntil) }.getOrNull() ?: return
    val status = Deadlines.status(date)
    val done = receipt.returnDoneAt != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (done) 1.dp else 1.5.dp,
                color = if (done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (done) {
                    stringResource(R.string.receipt_return_done)
                } else {
                    stringResource(R.string.receipt_return_open, status.text)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
            Text(
                text = stringResource(R.string.receipt_return_by, date.format(SHORT_DATE)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        PillButton(
            label = if (done) stringResource(R.string.undo) else stringResource(R.string.receipt_return_mark_done),
            filled = !done,
            onClick = { onReturnDone(!done) },
        )
    }
}

/**
 * Una scadenza si accende scegliendo una scorciatoia e si spegne
 * ritoccando quella accesa. La scorciatoia resta salvata, non solo la
 * data: se poi si corregge la data d'acquisto, la scadenza la segue.
 */
@Composable
private fun DeadlineEditor(
    kind: DeadlineKind,
    deadline: String?,
    presets: List<Int>,
    presetLabel: @Composable (Int) -> String,
    chosen: Int?,
    onChoose: (Int?) -> Unit,
) {
    val date = deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = kind.emoji, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = kind.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = date?.let { "${it.format(SHORT_DATE)} · ${Deadlines.status(it).text}" } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                val selected = chosen == preset
                PillButton(
                    label = presetLabel(preset),
                    filled = selected,
                    small = true,
                    onClick = { onChoose(if (selected) null else preset) },
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: (() -> Unit)?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ReceiptPage(path: String, onClick: () -> Unit) {
    val bitmap = remember(path) { decodePreview(path) }
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
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

/** La foto a schermo intero: è lì che uno scontrino si legge davvero. */
@Composable
private fun PhotoViewer(path: String, onDismiss: () -> Unit) {
    val bitmap = remember(path) { decodeFull(path) } ?: return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    filled: Boolean,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(MaterialTheme.colorScheme.onBackground)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 7.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TextFieldDialog(
    title: String,
    initial: String,
    numeric: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = !numeric && title.length < 100,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun pickDate(context: Context, current: LocalDate?, onPicked: (LocalDate) -> Unit) {
    val start = current ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        start.year,
        start.monthValue - 1,
        start.dayOfMonth,
    ).apply {
        // Uno scontrino di domani non esiste.
        datePicker.maxDate = Calendar.getInstance().timeInMillis
    }.show()
}

/**
 * Manda fuori lo scontrino: le foto come allegato e i dati come testo,
 * che è quello che serve sia per una nota spese sia per scrivere al
 * negozio.
 */
private fun shareReceipt(context: Context, entry: ReceiptWithPhotos) {
    val receipt = entry.receipt
    val summary = buildList {
        add(receipt.title.ifBlank { context.getString(R.string.receipt_untitled) })
        receipt.amountCents?.let { add(formatMoney(it)) }
        add(runCatching { LocalDate.parse(receipt.date).format(SHORT_DATE) }.getOrDefault(receipt.date))
    }.joinToString(" · ")

    val uris = ArrayList<android.net.Uri>()
    entry.photos.sortedBy { it.position }.forEach { photo ->
        runCatching {
            uris += FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(photo.path),
            )
        }
    }

    val intent = if (uris.size > 1) {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    } else {
        Intent(Intent.ACTION_SEND).apply { uris.firstOrNull()?.let { putExtra(Intent.EXTRA_STREAM, it) } }
    }.apply {
        type = if (uris.isEmpty()) "text/plain" else "image/jpeg"
        putExtra(Intent.EXTRA_TEXT, summary)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.receipt_share)))
}

/** Il testo letto, da mandare a chi può correggere il riconoscimento. */
private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.receipt_ocr_share)))
}

private fun pagesLabel(context: Context, entry: ReceiptWithPhotos): String {
    val pages = entry.photos.size
    val bytes = entry.photos.sumOf { it.sizeBytes }
    val size = if (bytes >= 1_048_576) {
        String.format(Locale.ITALIAN, "%.1f MB", bytes / 1_048_576.0)
    } else {
        "${(bytes / 1024).coerceAtLeast(1)} KB"
    }
    return context.resources.getQuantityString(R.plurals.receipt_pages, pages, pages) + " · " + size
}

private fun parseAmountToCents(typed: String): Long? {
    val clean = typed.trim().replace(" ", "").replace("€", "").replace(',', '.')
    if (clean.isEmpty()) return null
    return clean.toDoubleOrNull()?.let { Receipt.centsOf(it) }
}

private fun decodePreview(path: String): android.graphics.Bitmap? = runCatching {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 600) sample *= 2
    android.graphics.BitmapFactory.decodeFile(
        path,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
    )
}.getOrNull()

private fun decodeFull(path: String): android.graphics.Bitmap? =
    runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()
