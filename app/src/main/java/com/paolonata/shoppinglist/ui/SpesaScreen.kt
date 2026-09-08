package com.paolonata.shoppinglist.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.ui.theme.BigCardShape
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OutlinePill
import com.paolonata.shoppinglist.ui.theme.PillShape
import com.paolonata.shoppinglist.ui.theme.SectionRule

/**
 * La lista della spesa: il progresso in evidenza, le due sezioni
 * "Da prendere"/"Presi" e la barra di aggiunta rapida sempre a portata di
 * pollice. È la scheda che si apre per prima, perché è quella che si usa
 * dentro al supermercato.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpesaScreen(
    items: List<ShoppingItem>,
    onOpenWhatsApp: () -> Unit,
    onAddItem: (String, Int) -> Unit,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onEditItem: (ShoppingItem, String, Int) -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
    onShare: () -> Unit,
    onPhotographReceipt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toBuy = items.filter { !it.isChecked }
    val inCart = items.filter { it.isChecked }
    var editing by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 22.dp, bottom = 108.dp,
            ),
        ) {
            item("header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(PillShape)
                            .background(Organic.accent2600),
                    ) {
                        Text("🛒", fontSize = 17.sp)
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.spesa_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Organic.text,
                        )
                        Text(
                            text = stringResource(R.string.spesa_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Organic.neutral700,
                        )
                    }
                }
            }

            item("hero") {
                SpesaHeroCard(
                    total = items.size,
                    done = inCart.size,
                    onOpenWhatsApp = onOpenWhatsApp,
                )
            }

            item("actions") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                ) {
                    OutlinePill(stringResource(R.string.spesa_clear_checked), onClearChecked)
                    OutlinePill(stringResource(R.string.spesa_share), onShare)
                    OutlinePill(
                        text = stringResource(R.string.spesa_clear_all),
                        onClick = onClearAll,
                        borderColor = Organic.accent400,
                        contentColor = Organic.accent700,
                    )
                }
            }

            if (items.isNotEmpty() && toBuy.isEmpty()) {
                item("alldone") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(CardShape)
                            .background(Organic.accent200)
                            .clickable(onClick = onPhotographReceipt)
                            .padding(horizontal = 15.dp, vertical = 14.dp),
                    ) {
                        Text("🎉", fontSize = 17.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.spesa_all_done_title),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Organic.accent800,
                            )
                            Text(
                                text = stringResource(R.string.spesa_all_done_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Organic.accent800,
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item("empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.spesa_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Organic.text,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.spesa_empty_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Organic.neutral700,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            if (toBuy.isNotEmpty()) {
                item("h-tobuy") {
                    SectionRule(
                        title = stringResource(R.string.spesa_section_to_buy),
                        trailing = toBuy.size.toString(),
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                    )
                }
                items(toBuy, key = { "b" + it.id }) { item ->
                    SpesaRow(
                        item = item,
                        editing = editing == item.id,
                        onToggle = { onToggleChecked(item) },
                        onDelete = { onDeleteItem(item) },
                        onStartEdit = { editing = item.id },
                        onSaveEdit = { name, qty -> onEditItem(item, name, qty); editing = null },
                        onCancelEdit = { editing = null },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            if (inCart.isNotEmpty()) {
                item("h-incart") {
                    SectionRule(
                        title = stringResource(R.string.spesa_section_taken),
                        trailing = inCart.size.toString(),
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                    )
                }
                items(inCart, key = { "c" + it.id }) { item ->
                    SpesaRow(
                        item = item,
                        editing = editing == item.id,
                        onToggle = { onToggleChecked(item) },
                        onDelete = { onDeleteItem(item) },
                        onStartEdit = { editing = item.id },
                        onSaveEdit = { name, qty -> onEditItem(item, name, qty); editing = null },
                        onCancelEdit = { editing = null },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }

        QuickAddBar(
            onAdd = onAddItem,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

/** Il riquadro salvia col conteggio dei presi e il tasto per WhatsApp. */
@Composable
private fun SpesaHeroCard(total: Int, done: Int, onOpenWhatsApp: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) done.toFloat() / total else 0f,
        label = "progress",
    )
    Surface(
        shape = BigCardShape,
        color = Organic.accent2600,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            // Il cerchio chiarissimo che sborda in alto a destra: è la
            // "forma morbida" con cui il design system decora le superfici.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-50).dp)
                    .size(150.dp)
                    .clip(PillShape)
                    .background(Organic.onAccent2.copy(alpha = 0.09f)),
            )
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)) {
                Text(
                    text = if (total > 0) {
                        stringResource(R.string.spesa_hero_label)
                    } else {
                        stringResource(R.string.spesa_hero_label_empty)
                    }.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.7.sp),
                    color = Organic.onAccent2.copy(alpha = 0.85f),
                )
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = done.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Organic.onAccent2,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.spesa_hero_of_total, total),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = Organic.onAccent2.copy(alpha = 0.65f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(PillShape)
                        .background(Organic.onAccent2.copy(alpha = 0.24f)),
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                                .height(9.dp)
                                .clip(PillShape)
                                .background(Organic.onAccent2),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(Organic.onAccent2)
                        .clickable(onClick = onOpenWhatsApp)
                        .padding(vertical = 13.dp, horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Organic.accent2800,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.spesa_paste_whatsapp),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        color = Organic.accent2800,
                    )
                }
            }
        }
    }
}

/**
 * Una riga della lista. Il tocco spunta (sia sulla pallina sia sul nome,
 * come nel prototipo); la pressione lunga apre la modifica, che il design
 * non disegna ma che nell'app c'era già e resta utile.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpesaRow(
    item: ShoppingItem,
    editing: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: (String, Int) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editing) {
        EditRow(item = item, onSave = onSaveEdit, onCancel = onCancelEdit, modifier = modifier)
        return
    }
    val checked = item.isChecked
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (checked) Color.Transparent else Organic.neutral100)
            .border(1.5.dp, Organic.neutral300, CardShape)
            .alpha(if (checked) 0.6f else 1f)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        CircleCheck(checked = checked, onClick = onToggle)
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onToggle, onLongClick = onStartEdit),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Organic.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
            )
            if (!item.note.isNullOrBlank()) {
                Text(
                    text = item.note,
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(PillShape)
                .clickable(onClick = onDelete),
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.spesa_delete_cd),
                tint = Organic.neutral500,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** La pallina che si riempie di salvia quando l'articolo è preso. */
@Composable
private fun CircleCheck(checked: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(
        if (checked) Organic.accent2600 else Organic.neutral400,
        label = "checkBorder",
    )
    val fill by animateColorAsState(
        if (checked) Organic.accent2600 else Color.Transparent,
        label = "checkFill",
    )
    val pop by animateFloatAsState(if (checked) 1f else 0.85f, label = "checkPop")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .clip(PillShape)
            .background(fill)
            .border(2.dp, border, PillShape)
            .clickable(onClick = onClick),
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Organic.onAccent2,
                modifier = Modifier
                    .size(15.dp)
                    .scale(pop),
            )
        }
    }
}

/** Modifica di un articolo già in lista: nome e quantità, salva o annulla. */
@Composable
private fun EditRow(
    item: ShoppingItem,
    onSave: (String, Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var qty by remember(item.id) { mutableStateOf(item.quantity) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Organic.neutral100)
            .border(1.5.dp, Organic.accent400, CardShape)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(
                MaterialTheme.typography.bodyLarge.copy(
                    color = Organic.text,
                    fontWeight = FontWeight.SemiBold,
                ),
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Organic.accent600),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name, qty) }),
            modifier = Modifier.weight(1f),
        )
        QtyStepper(quantity = qty, onChange = { qty = it })
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(PillShape)
                .background(Organic.accent2600)
                .clickable { if (name.isNotBlank()) onSave(name, qty) },
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Organic.onAccent2, modifier = Modifier.size(17.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(PillShape)
                .clickable(onClick = onCancel),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Organic.neutral600,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** Lo stepper "− 1 +" dentro la sua pillola grigia. */
@Composable
fun QtyStepper(quantity: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(PillShape)
            .background(Organic.neutral200)
            .padding(3.dp),
    ) {
        StepperButton("−") { onChange((quantity - 1).coerceAtLeast(1)) }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = Organic.text,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(18.dp),
        )
        StepperButton("+") { onChange((quantity + 1).coerceAtMost(99)) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .clip(PillShape)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = Organic.neutral800,
        )
    }
}

/**
 * La barra sempre pronta in fondo: quantità, campo di testo, dettatura e
 * il "+" salvia. Il testo passa comunque dal parser, quindi "2 mele"
 * continua a valere due mele anche scritto qui.
 */
@Composable
private fun QuickAddBar(onAdd: (String, Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var text by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1) }
    var listening by remember { mutableStateOf(false) }

    val dictatePrompt = stringResource(R.string.add_dictate_prompt)
    val speechUnavailable = stringResource(R.string.add_speech_unavailable)
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        listening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                text = if (text.isBlank()) spoken else text.trimEnd() + ", " + spoken
            }
        }
    }

    fun submit() {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        onAdd(trimmed, qty)
        text = ""
        qty = 1
    }

    Column(modifier = modifier) {
        AnimatedVisibility(visible = listening) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(PillShape)
                    .background(Organic.accent600)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(PillShape)
                        .background(Organic.onAccent),
                )
                Text(
                    text = stringResource(R.string.spesa_listening),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Organic.onAccent,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, PillShape)
                .clip(PillShape)
                .background(Organic.neutral100)
                .border(1.5.dp, Organic.neutral300, PillShape)
                .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            QtyStepper(quantity = qty, onChange = { qty = it })
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.quick_add_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Organic.neutral600,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = Organic.text),
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Organic.accent600),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(PillShape)
                    .background(if (listening) Organic.accent600 else Color.Transparent)
                    .border(1.5.dp, if (listening) Organic.accent600 else Organic.neutral300, PillShape)
                    .clickable {
                        if (SpeechRecognizer.isRecognitionAvailable(context)) {
                            listening = true
                            keyboard?.hide()
                            speechLauncher.launch(buildSpeechIntent(dictatePrompt))
                        } else {
                            Toast.makeText(context, speechUnavailable, Toast.LENGTH_LONG).show()
                        }
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.spesa_dictate_cd),
                    tint = if (listening) Organic.onAccent else Organic.neutral800,
                    modifier = Modifier.size(19.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(PillShape)
                    .background(Organic.accent2600)
                    .clickable { submit() },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.spesa_add_cd),
                    tint = Organic.onAccent2,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

private fun buildSpeechIntent(prompt: String): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
