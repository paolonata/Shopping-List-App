package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity

/**
 * Le icone proposte quando si inventa una categoria.
 *
 * Una griglia di emoji e non un elenco di icone disegnate: le emoji ci
 * sono già su ogni telefono, si vedono uguali dentro e fuori dall'app, e
 * chi non trova quella giusta può scriverne una qualunque nel campo — è
 * l'unico modo di dire «icona a scelta» senza spedire mille disegni
 * dentro l'APK.
 */
private val ICON_CHOICES = listOf(
    "🏷️", "🛒", "🍽️", "🚗", "💻", "🛍️", "🏠", "💊",
    "✈️", "🏨", "🎁", "🎬", "🎵", "📚", "🐶", "🐱",
    "👶", "🎓", "💇", "👕", "👟", "⚽", "🏋️", "🚲",
    "⛽", "🅿️", "🚌", "🚆", "🔧", "🪑", "🌱", "🔌",
    "📱", "🎮", "☕", "🍺", "🍕", "🍰", "🥐", "🧾",
    "💡", "🧼", "🧻", "💊", "🩺", "🦷", "👓", "💍",
)

/**
 * La scelta della categoria di uno scontrino, e il posto da cui se ne
 * creano di nuove: crearne una serve quasi sempre mentre si sta
 * catalogando qualcosa, non prima, in una schermata di impostazioni.
 */
@Composable
fun CategoryPickerDialog(
    categories: List<ReceiptCategoryEntity>,
    currentId: String,
    onDismiss: () -> Unit,
    onPick: (ReceiptCategoryEntity) -> Unit,
    onCreate: (String, String) -> Unit,
    onUpdate: (ReceiptCategoryEntity, String, String) -> Unit,
    onDelete: (ReceiptCategoryEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<ReceiptCategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        CategoryEditorDialog(
            initialLabel = "",
            initialEmoji = ICON_CHOICES.first(),
            title = stringResource(R.string.category_new),
            onDismiss = { creating = false },
            onConfirm = { label, emoji -> onCreate(label, emoji); creating = false },
        )
        return
    }
    editing?.let { category ->
        CategoryEditorDialog(
            initialLabel = category.label,
            initialEmoji = category.emoji,
            title = stringResource(R.string.category_edit),
            onDismiss = { editing = null },
            onConfirm = { label, emoji -> onUpdate(category, label, emoji); editing = null },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.receipt_field_category)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(categories, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(category) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = category.emoji, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (category.id == currentId) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (category.id == currentId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { editing = category }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.category_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        // Le otto di partenza non si eliminano: la lettura
                        // automatica degli scontrini ci assegna sopra le sue ipotesi.
                        if (!category.builtIn) {
                            IconButton(onClick = { onDelete(category) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.category_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                item(key = "new") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.category_new),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Nome e icona: le due sole cose che una categoria è. */
@Composable
private fun CategoryEditorDialog(
    initialLabel: String,
    initialEmoji: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var label by remember { mutableStateOf(initialLabel) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // L'icona scelta, che è anche un campo: chi vuole
                    // un'emoji che non è nella griglia se la scrive.
                    BasicTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(4) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .size(52.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 15.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.heightIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(ICON_CHOICES) { choice ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    if (choice == emoji) {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { emoji = choice },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = choice, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label, emoji) },
                enabled = label.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
