package com.paolonata.shoppinglist.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.parser.ParsedItem
import com.paolonata.shoppinglist.ui.theme.BottomSheetOverlay
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.FilledPillButton
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OutlinePill
import com.paolonata.shoppinglist.ui.theme.PillShape

/**
 * Il foglio "Nuova lista": si incolla il messaggio della spesa e, mentre
 * si scrive, sotto compare la lista come verrà. Prende il posto della
 * vecchia schermata intera di import: l'operazione è breve e non merita
 * di far sparire la lista che si sta guardando.
 */
@Composable
fun WhatsAppSheet(
    initialText: String,
    onParse: (String) -> List<ParsedItem>,
    onConfirm: (List<ParsedItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var confirmed by remember { mutableStateOf(false) }
    val parsed = remember(text) { onParse(text) }

    BottomSheetOverlay(onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.wa_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Organic.text,
            )
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                text = stringResource(R.string.wa_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Organic.neutral700,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp)
                    .clip(CardShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, CardShape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.wa_sheet_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Organic.neutral600,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = Organic.text),
                    ),
                    cursorBrush = SolidColor(Organic.accent600),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 14.dp),
            ) {
                OutlinePill(stringResource(R.string.wa_sheet_paste)) {
                    val pasted = readClipboardText(context)
                    if (!pasted.isNullOrBlank()) {
                        text = if (text.isBlank()) pasted else text.trimEnd() + "\n" + pasted
                    }
                }
                OutlinePill(stringResource(R.string.wa_sheet_clear)) { text = "" }
            }

            Text(
                text = pluralStringResource(R.plurals.add_preview_title, parsed.size, parsed.size),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                color = Organic.text,
                modifier = Modifier.padding(bottom = 9.dp),
            )

            if (parsed.isEmpty()) {
                Text(
                    text = stringResource(R.string.wa_sheet_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Organic.neutral700,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                parsed.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(Organic.neutral100)
                            .border(1.5.dp, Organic.neutral300, CardShape)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Organic.text,
                            )
                            if (!item.note.isNullOrBlank()) {
                                Text(
                                    text = item.note!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Organic.neutral700,
                                )
                            }
                        }
                        if (item.quantity > 1) {
                            Text(
                                text = "×${item.quantity}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Organic.accent2800,
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(Organic.accent2200)
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.padding(top = 16.dp))
            FilledPillButton(
                text = if (parsed.isEmpty()) {
                    stringResource(R.string.wa_sheet_confirm_empty)
                } else {
                    pluralStringResource(R.plurals.add_confirm_button, parsed.size, parsed.size)
                },
                onClick = {
                    if (!confirmed && parsed.isNotEmpty()) {
                        confirmed = true
                        onConfirm(parsed)
                    }
                },
                container = Organic.accent2600,
                content = Organic.onAccent2,
                icon = Icons.Default.Check,
                enabled = parsed.isNotEmpty() && !confirmed,
            )
        }
    }
}

private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip: ClipData? = clipboard?.primaryClip
    if (clip == null || clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
