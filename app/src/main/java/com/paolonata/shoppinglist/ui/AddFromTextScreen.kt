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
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.parser.ParsedItem
import com.paolonata.shoppinglist.ui.theme.brandGradient

private fun previewKey(item: ParsedItem): String = "${item.name.trim().lowercase()}|${item.note.orEmpty()}"

@Composable
fun AddFromTextScreen(
    initialText: String,
    onParse: (String) -> List<ParsedItem>,
    onConfirm: (List<ParsedItem>) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val context = LocalContext.current
    val parsed = remember(text) { onParse(text) }
    // Quantità modificate a mano nell'anteprima, che sopravvivono a un ricalcolo del testo
    // (le voci non più presenti restano semplicemente inutilizzate).
    val quantityOverrides = remember { mutableStateMapOf<String, Int>() }
    val preview = parsed.map { item ->
        quantityOverrides[previewKey(item)]?.let { item.copy(quantity = it) } ?: item
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
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.add_cancel_button),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
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
                TextButton(onClick = { text = readClipboardText(context) ?: text }) {
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
                text = stringResource(R.string.add_preview_title, preview.size),
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
                enabled = preview.isNotEmpty(),
                label = stringResource(R.string.add_confirm_button, preview.size),
                onClick = { onConfirm(preview) },
            )
        }
    }
}

@Composable
private fun PreviewCard(item: ParsedItem, onQuantityChange: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                modifier = Modifier.size(28.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { onQuantityChange(item.quantity + 1) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.quantity_increase_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (enabled) {
        Modifier.background(brandGradient())
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .then(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
