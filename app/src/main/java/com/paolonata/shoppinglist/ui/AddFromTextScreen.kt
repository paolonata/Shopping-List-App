package com.paolonata.shoppinglist.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.parser.ParsedItem

// Un carattere di controllo (non digitabile da tastiera) come separatore: a differenza di
// "|" non può mai comparire dentro name/note e generare per sbaglio due chiavi identiche.
private fun previewKey(item: ParsedItem): String = "${item.name.trim().lowercase()}\u0000${item.note.orEmpty()}"

@Composable
fun AddFromTextScreen(
    initialText: String,
    onParse: (String) -> List<ParsedItem>,
    onConfirm: (List<ParsedItem>) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var confirmed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val parsed = remember(text) { onParse(text) }
    // Quantità modificate a mano nell'anteprima, che sopravvivono a un ricalcolo del testo.
    val quantityOverrides = remember { mutableStateMapOf<String, Int>() }
    val preview = parsed.map { item ->
        quantityOverrides[previewKey(item)]?.let { item.copy(quantity = it) } ?: item
    }
    // Se un articolo scompare dal testo (l'utente lo cancella) e poi lo riscrive identico,
    // senza questa pulizia ricomparirebbe con la vecchia quantità invece di quella nel testo.
    LaunchedEffect(parsed) {
        val currentKeys = parsed.map { previewKey(it) }.toSet()
        quantityOverrides.keys.toList().forEach { key ->
            if (key !in currentKeys) quantityOverrides.remove(key)
        }
    }

    val dictatePrompt = stringResource(R.string.add_dictate_prompt)
    val speechUnavailable = stringResource(R.string.add_speech_unavailable)
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                text = if (text.isBlank()) spoken else text.trimEnd() + "\n" + spoken
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.add_cancel_button),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.add_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                placeholder = { Text(stringResource(R.string.add_hint)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        speechLauncher.launch(buildSpeechIntent(dictatePrompt))
                    } else {
                        Toast.makeText(context, speechUnavailable, Toast.LENGTH_LONG).show()
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_dictate_button))
                }
                TextButton(onClick = {
                    // Accoda come la dettatura, invece di sovrascrivere in silenzio quanto già
                    // scritto/dettato: prima i due pulsanti si comportavano in modo incoerente.
                    val pasted = readClipboardText(context)
                    if (!pasted.isNullOrBlank()) {
                        text = if (text.isBlank()) pasted else text.trimEnd() + "\n" + pasted
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_paste_button))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (text.isNotEmpty()) {
                    TextButton(onClick = { text = ""; quantityOverrides.clear() }) {
                        Text(
                            text = stringResource(R.string.add_clear_button),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.add_dictate_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Text(
                text = pluralStringResource(R.plurals.add_preview_title, preview.size, preview.size),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )

            if (preview.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_preview_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(preview, key = { previewKey(it) }) { parsedItem ->
                        PreviewCard(
                            item = parsedItem,
                            onQuantityChange = { newQuantity ->
                                quantityOverrides[previewKey(parsedItem)] = newQuantity
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ConfirmButton(
                // Guardia contro il doppio tap: durante il Crossfade tra schermate questa
                // resta composta (e toccabile) per la durata dell'animazione, e un doppio tap
                // rapido poteva invocare onConfirm due volte, raddoppiando l'import.
                enabled = preview.isNotEmpty() && !confirmed,
                label = pluralStringResource(R.plurals.add_confirm_button, preview.size, preview.size),
                onClick = {
                    if (!confirmed) {
                        confirmed = true
                        onConfirm(preview)
                    }
                },
            )
        }
    }
}

@Composable
private fun PreviewCard(item: ParsedItem, onQuantityChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cerchio placeholder coerente con CircleCheckbox della HomeScreen
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.note?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.quantity_decrease_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(22.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { onQuantityChange(item.quantity + 1) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.quantity_increase_cd),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 38.dp),
        )
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildSpeechIntent(prompt: String): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }

private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip: ClipData? = clipboard?.primaryClip
    if (clip == null || clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
