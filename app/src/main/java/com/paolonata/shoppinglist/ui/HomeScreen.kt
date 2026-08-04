package com.paolonata.shoppinglist.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.notification.NotificationPrefs
import com.paolonata.shoppinglist.notification.ShoppingListNotifier
import com.paolonata.shoppinglist.notification.ShoppingReminderPrefs
import com.paolonata.shoppinglist.notification.ShoppingReminderScheduler
import com.paolonata.shoppinglist.ui.theme.ThemeMode
import com.paolonata.shoppinglist.ui.theme.ThemePrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<ShoppingItem>,
    onAddFromText: () -> Unit,
    onAddItem: (String, Int) -> Unit,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onEditItem: (ShoppingItem, String, Int) -> Unit,
    onReorderItems: (List<ShoppingItem>) -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
) {
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val quickAddFocusRequester = remember { FocusRequester() }

    var localOrder by remember { mutableStateOf<List<Long>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(items) { if (draggingId == null) localOrder = null }

    val allToBuy = items.filter { !it.isChecked }
    val inCart = items.filter { it.isChecked }
    val toBuy = localOrder?.let { order ->
        val byId = allToBuy.associateBy { it.id }
        order.mapNotNull { byId[it] } + allToBuy.filter { it.id !in order }
    } ?: allToBuy

    // I callback dentro pointerInput(shoppingItem.id) restano "vivi" tra una ricomposizione e
    // l'altra finché l'id non cambia (il gesto non si riavvia ad ogni ricomposizione): senza
    // rememberUpdatedState, onDragStart/onDragEnd catturerebbero per closure il valore di
    // toBuy/allToBuy della PRIMA composizione, riportando il riordino a com'era all'inizio
    // ogni volta che si trascina un articolo la cui gesture non è stata reinstallata di recente.
    val currentToBuy by rememberUpdatedState(toBuy)
    val currentAllToBuy by rememberUpdatedState(allToBuy)
    val currentOnReorderItems by rememberUpdatedState(onReorderItems)

    // Quando arriva un nuovo articolo (da qui o da WhatsApp) lo si porta in vista con lo
    // scroll minimo necessario (BringIntoViewRequester), non un salto forzato in cima:
    // così, se l'utente aveva scorso per vedere "Presi", non viene sbalzato via da lì
    // se il nuovo articolo è già almeno parzialmente visibile.
    val bringIntoViewRequesters = remember { mutableStateMapOf<Long, BringIntoViewRequester>() }
    var previousToBuyIds by remember { mutableStateOf<Set<Long>>(toBuy.map { it.id }.toSet()) }
    LaunchedEffect(toBuy) {
        val currentIds = toBuy.map { it.id }.toSet()
        val newIds = currentIds - previousToBuyIds
        if (newIds.isNotEmpty()) {
            val lastNewIndex = toBuy.indexOfLast { it.id in newIds }
            if (lastNewIndex >= 0) {
                val newItemId = toBuy[lastNewIndex].id
                val requester = bringIntoViewRequesters[newItemId]
                if (requester != null) {
                    try {
                        requester.bringIntoView()
                    } catch (e: IllegalStateException) {
                        // Il layout non è (più) agganciato: niente da portare in vista.
                    }
                } else {
                    // L'articolo non è mai stato composto (lista più lunga dello schermo,
                    // quindi fuori dal viewport iniziale): nessun requester registrato per lui,
                    // quindi bringIntoView() non scorrerebbe affatto. Fallback diretto sull'indice
                    // nella LazyColumn (+1 per l'header "Da prendere" che precede gli articoli).
                    runCatching { listState.animateScrollToItem(lastNewIndex + 1) }
                }
            }
        }
        previousToBuyIds = currentIds
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MinimalHeader(
                total = items.size,
                checked = items.count { it.isChecked },
                onShare = { shareList(context, items) },
                onClearChecked = onClearChecked,
                onClearAll = onClearAll,
                items = items,
            )
        },
        bottomBar = {
            BottomQuickAddBar(
                onAdd = onAddItem,
                onFromWhatsApp = onAddFromText,
                focusRequester = quickAddFocusRequester,
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 12.dp),
            ) {
                if (toBuy.isNotEmpty()) {
                    item(key = "hdr_to_buy") {
                        MinimalSectionHeader(
                            text = stringResource(R.string.home_section_to_buy),
                            count = toBuy.size,
                            showAdd = true,
                            onAddClick = { runCatching { quickAddFocusRequester.requestFocus() } },
                        )
                    }
                    items(toBuy, key = { it.id }) { shoppingItem ->
                        val bringIntoViewRequester = remember(shoppingItem.id) { BringIntoViewRequester() }
                        DisposableEffect(shoppingItem.id) {
                            bringIntoViewRequesters[shoppingItem.id] = bringIntoViewRequester
                            onDispose {
                                bringIntoViewRequesters.remove(shoppingItem.id)
                                // Altrimenti l'altezza di un articolo eliminato resta per sempre
                                // nella mappa (piccola perdita di memoria che cresce nel tempo).
                                itemHeights.remove(shoppingItem.id)
                            }
                        }
                        val isDragging = draggingId == shoppingItem.id
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.02f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "scale",
                        )
                        ItemRow(
                            item = shoppingItem,
                            isEditing = editingItemId == shoppingItem.id,
                            isDragging = isDragging,
                            onToggleChecked = onToggleChecked,
                            onDeleteItem = onDeleteItem,
                            onStartEdit = { editingItemId = it },
                            onSaveEdit = { it2, name, qty ->
                                onEditItem(it2, name, qty)
                                editingItemId = null
                            },
                            onCancelEdit = { editingItemId = null },
                            dragHandle = {
                                DragHandleIcon(
                                    modifier = Modifier.pointerInput(shoppingItem.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingId = shoppingItem.id
                                                dragOffsetY = 0f
                                                localOrder = currentToBuy.map { it.id }
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffsetY += amount.y
                                                val order = localOrder ?: return@detectDragGesturesAfterLongPress
                                                val currentIndex = order.indexOf(shoppingItem.id)
                                                val height = itemHeights[shoppingItem.id] ?: return@detectDragGesturesAfterLongPress
                                                if (height == 0) return@detectDragGesturesAfterLongPress
                                                if (dragOffsetY > height * 0.6f && currentIndex < order.lastIndex) {
                                                    localOrder = order.toMutableList().apply { add(currentIndex + 1, removeAt(currentIndex)) }
                                                    dragOffsetY -= height
                                                } else if (dragOffsetY < -height * 0.6f && currentIndex > 0) {
                                                    localOrder = order.toMutableList().apply { add(currentIndex - 1, removeAt(currentIndex)) }
                                                    dragOffsetY += height
                                                }
                                            },
                                            onDragEnd = {
                                                val finalOrder = localOrder
                                                draggingId = null
                                                dragOffsetY = 0f
                                                if (finalOrder != null) {
                                                    val byId = currentAllToBuy.associateBy { it.id }
                                                    currentOnReorderItems(finalOrder.mapNotNull { byId[it] })
                                                }
                                            },
                                            onDragCancel = {
                                                draggingId = null
                                                dragOffsetY = 0f
                                                localOrder = null
                                            },
                                        )
                                    },
                                )
                            },
                            modifier = Modifier
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .then(if (!isDragging) Modifier.animateItem() else Modifier)
                                .onGloballyPositioned { c -> itemHeights[shoppingItem.id] = c.size.height }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }
                if (inCart.isNotEmpty()) {
                    item(key = "hdr_in_cart") {
                        MinimalSectionHeader(stringResource(R.string.home_section_in_cart), inCart.size)
                    }
                    items(inCart, key = { it.id }) { shoppingItem ->
                        ItemRow(
                            item = shoppingItem,
                            isEditing = editingItemId == shoppingItem.id,
                            isDragging = false,
                            onToggleChecked = onToggleChecked,
                            onDeleteItem = onDeleteItem,
                            onStartEdit = { editingItemId = it },
                            onSaveEdit = { it2, name, qty ->
                                onEditItem(it2, name, qty)
                                editingItemId = null
                            },
                            onCancelEdit = { editingItemId = null },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

private fun shareList(context: Context, items: List<ShoppingItem>) {
    // Solo quello che manca ancora da comprare: condividere anche quello già preso non è utile
    // a chi riceve la lista (es. il partner che va a fare la spesa) e può confondere.
    val toShare = items.filterNot { it.isChecked }
    if (toShare.isEmpty()) return
    val sb = StringBuilder(context.getString(R.string.share_title))
    for (item in toShare) {
        sb.append("\n• ").append(item.name)
        if (item.quantity > 1) sb.append(" ×").append(item.quantity)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}

/** Header minimale ed editoriale: titolo piccolo centrato, icone senza sfondo colorato. */
@Composable
private fun MinimalHeader(
    total: Int,
    checked: Int,
    onShare: () -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
    items: List<ShoppingItem>,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var notifEnabled by remember { mutableStateOf(NotificationPrefs.isEnabled(context)) }
    val permissionDeniedMessage = stringResource(R.string.notification_permission_denied)
    val notifOnMessage = stringResource(R.string.notification_enabled_toast)
    val notifOffMessage = stringResource(R.string.notification_disabled_toast)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            notifEnabled = true
            NotificationPrefs.setEnabled(context, true)
            ShoppingListNotifier.show(context, items)
            Toast.makeText(context, notifOnMessage, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, permissionDeniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun toggleNotification() {
        if (notifEnabled) {
            notifEnabled = false
            NotificationPrefs.setEnabled(context, false)
            ShoppingListNotifier.cancel(context)
            Toast.makeText(context, notifOffMessage, Toast.LENGTH_SHORT).show()
            return
        }
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsRuntimePermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifEnabled = true
            NotificationPrefs.setEnabled(context, true)
            ShoppingListNotifier.show(context, items)
            Toast.makeText(context, notifOnMessage, Toast.LENGTH_LONG).show()
        }
    }

    // Promemoria "un colpo solo" per l'intera lista (diverso dalla notifica persistente sopra).
    var reminderAt by remember { mutableStateOf(ShoppingReminderPrefs.getReminderAt(context)) }
    var reminderDialogOpen by remember { mutableStateOf(false) }
    var pendingReminderMillis by remember { mutableStateOf<Long?>(null) }
    val reminderSetTemplate = stringResource(R.string.reminder_set_toast)
    val reminderCancelledMessage = stringResource(R.string.reminder_cancelled_toast)
    val reminderPermissionDeniedMessage = stringResource(R.string.reminder_permission_denied)

    val reminderPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val millis = pendingReminderMillis
        pendingReminderMillis = null
        if (granted && millis != null) {
            ShoppingReminderScheduler.schedule(context, millis)
            reminderAt = millis
            Toast.makeText(context, String.format(reminderSetTemplate, formatReminderTime(millis)), Toast.LENGTH_LONG).show()
        } else if (!granted) {
            Toast.makeText(context, reminderPermissionDeniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun requestReminder(atMillis: Long) {
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsRuntimePermission) {
            pendingReminderMillis = atMillis
            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ShoppingReminderScheduler.schedule(context, atMillis)
            reminderAt = atMillis
            Toast.makeText(context, String.format(reminderSetTemplate, formatReminderTime(atMillis)), Toast.LENGTH_LONG).show()
        }
    }

    fun cancelReminder() {
        ShoppingReminderScheduler.cancel(context)
        reminderAt = null
        Toast.makeText(context, reminderCancelledMessage, Toast.LENGTH_SHORT).show()
    }

    fun pickCustomReminderDateTime() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        chosen.set(Calendar.HOUR_OF_DAY, hour)
                        chosen.set(Calendar.MINUTE, minute)
                        chosen.set(Calendar.SECOND, 0)
                        chosen.set(Calendar.MILLISECOND, 0)
                        requestReminder(chosen.timeInMillis)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            if (total > 0) {
                IconButton(onClick = onShare, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.Default.IosShare,
                        contentDescription = stringResource(R.string.share_chooser),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_title).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                OutlinedIconButton(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = stringResource(R.string.settings_menu_cd),
                    onClick = { menuExpanded = true },
                )
                val themeMode by ThemePrefs.mode.collectAsState()
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = { Text(stringResource(R.string.theme_menu)) },
                        enabled = false,
                        onClick = {},
                    )
                    ThemeRadioItem(
                        label = stringResource(R.string.theme_auto),
                        selected = themeMode == ThemeMode.AUTO,
                        onSelect = { ThemePrefs.set(context, ThemeMode.AUTO); menuExpanded = false },
                    )
                    ThemeRadioItem(
                        label = stringResource(R.string.theme_light),
                        selected = themeMode == ThemeMode.LIGHT,
                        onSelect = { ThemePrefs.set(context, ThemeMode.LIGHT); menuExpanded = false },
                    )
                    ThemeRadioItem(
                        label = stringResource(R.string.theme_dark),
                        selected = themeMode == ThemeMode.DARK,
                        onSelect = { ThemePrefs.set(context, ThemeMode.DARK); menuExpanded = false },
                    )
                    ThemeRadioItem(
                        label = stringResource(R.string.theme_olive),
                        selected = themeMode == ThemeMode.OLIVE,
                        onSelect = { ThemePrefs.set(context, ThemeMode.OLIVE); menuExpanded = false },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = if (notifEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = if (notifEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = {
                            Text(
                                text = if (notifEnabled) stringResource(R.string.notification_menu_toggle_off) else stringResource(R.string.notification_menu_toggle_on),
                            )
                        },
                        onClick = { menuExpanded = false; toggleNotification() },
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (reminderAt != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = {
                            val label = reminderAt?.let {
                                stringResource(R.string.reminder_menu_item_active, formatReminderTime(it))
                            } ?: stringResource(R.string.reminder_menu_item)
                            Text(text = label)
                        },
                        onClick = { menuExpanded = false; reminderDialogOpen = true },
                    )
                    if (total > 0) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_clear_checked)) },
                            onClick = { menuExpanded = false; onClearChecked() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_clear_all)) },
                            onClick = { menuExpanded = false; onClearAll() },
                        )
                    }
                }
            }
        }
        if (total > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_progress, checked, total) + " " + stringResource(R.string.home_progress_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (reminderDialogOpen) {
        AlertDialog(
            onDismissRequest = { reminderDialogOpen = false },
            title = { Text(stringResource(R.string.reminder_dialog_title)) },
            text = {
                Column {
                    ReminderOptionRow(stringResource(R.string.reminder_option_tonight, timeLabel(REMINDER_EVENING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_EVENING_HOUR, forceTomorrow = false))
                    }
                    ReminderOptionRow(stringResource(R.string.reminder_option_tomorrow_morning, timeLabel(REMINDER_MORNING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_MORNING_HOUR, forceTomorrow = true))
                    }
                    ReminderOptionRow(stringResource(R.string.reminder_option_tomorrow_evening, timeLabel(REMINDER_EVENING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_EVENING_HOUR, forceTomorrow = true))
                    }
                    ReminderOptionRow(stringResource(R.string.reminder_option_custom)) {
                        reminderDialogOpen = false
                        pickCustomReminderDateTime()
                    }
                    if (reminderAt != null) {
                        ReminderOptionRow(stringResource(R.string.reminder_option_cancel), destructive = true) {
                            reminderDialogOpen = false
                            cancelReminder()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reminderDialogOpen = false }) {
                    Text(stringResource(R.string.reminder_dialog_close))
                }
            },
        )
    }
}

private const val REMINDER_MORNING_HOUR = 9
private const val REMINDER_EVENING_HOUR = 18

@Composable
private fun ReminderOptionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

private fun timeLabel(hour: Int): String = String.format(Locale.ITALIAN, "%02d:00", hour)

/** Calcola l'orario del preset; se è già passato per oggi, scivola automaticamente al giorno dopo. */
private fun computeReminderMillis(hour: Int, forceTomorrow: Boolean): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (forceTomorrow) cal.add(Calendar.DAY_OF_YEAR, 1)
    if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

private fun formatReminderTime(millis: Long): String =
    SimpleDateFormat("EEEE d MMMM 'alle' HH:mm", Locale.ITALIAN).format(Date(millis))

/** Icona in un cerchio con bordo sottile — usata per l'unica azione "con contorno" dell'header. */
@Composable
private fun OutlinedIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ThemeRadioItem(label: String, selected: Boolean, onSelect: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                modifier = Modifier.size(18.dp),
            )
        },
        text = {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onSelect,
    )
}

@Composable
private fun MinimalSectionHeader(
    text: String,
    count: Int,
    showAdd: Boolean = false,
    onAddClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showAdd) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.section_add_item_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Barra fissa in basso: icona per importare da WhatsApp + campo di testo sempre pronto
 * per aggiungere un articolo a mano, con stepper per la quantità mentre si scrive. */
@Composable
private fun BottomQuickAddBar(
    onAdd: (String, Int) -> Unit,
    onFromWhatsApp: () -> Unit,
    focusRequester: FocusRequester,
) {
    var value by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }

    fun submit() {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) {
            // La quantità viaggia come parametro separato, non più concatenata al testo: prima
            // "2 mele" + stepper portato a 3 diventava il testo "3 2 mele", che il parser
            // interpretava come articolo "2 mele" con quantità 3 invece di "Mele" ×3.
            onAdd(trimmed, quantity)
            value = ""
            quantity = 1
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFromWhatsApp) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = stringResource(R.string.home_add_whatsapp),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    value = newValue
                    // Altrimenti una quantità impostata e poi il testo cancellato a mano (non
                    // con "invio"/✓) restava appesa e si applicava al prossimo articolo scritto.
                    if (newValue.isBlank()) quantity = 1
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.quick_add_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (value.isNotBlank()) {
                QuantityStepper(quantity = quantity) { quantity = it.coerceAtLeast(1) }
                IconButton(onClick = { submit() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.edit_confirm_cd),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Checkbox circolare: contorno vuoto quando da prendere, piena (onBackground) con
 * spunta quando presa — coerente con l'estetica "checklist" editoriale monocromatica. */
@Composable
private fun CircleCheckbox(
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        animationSpec = tween(180),
        label = "cbBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180),
        label = "cbBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cbScale",
    )
    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun DragHandleIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = stringResource(R.string.reorder_handle_cd),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp, end = 2.dp)
            .size(20.dp),
    )
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (quantity > 1) onChange(quantity - 1) }, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.quantity_decrease_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onChange(quantity + 1) }, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.quantity_increase_cd),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    isEditing: Boolean,
    isDragging: Boolean,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onStartEdit: (Long) -> Unit,
    onSaveEdit: (ShoppingItem, String, Int) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isEditing) {
            EditItemRow(
                item = item,
                onSave = { name, quantity -> onSaveEdit(item, name, quantity) },
                onCancel = onCancelEdit,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleCheckbox(
                    isChecked = item.isChecked,
                    onToggle = { onToggleChecked(item) },
                )
                Spacer(modifier = Modifier.width(14.dp))
                val textColor by animateColorAsState(
                    targetValue = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "textColor",
                )
                Column(
                    // Il tap per modificare è solo qui, non su tutta la riga: prima, un tap
                    // breve sulla maniglia di trascinamento (che intercetta solo long-press+drag,
                    // non i tap normali) "cadeva" sul clickable della riga e apriva la modifica
                    // per errore invece di iniziare/ignorare il drag.
                    modifier = Modifier.weight(1f).clickable { onStartEdit(item.id) },
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        color = textColor,
                    )
                    item.note?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (item.quantity > 1) {
                    QuantityPillOutline(item.quantity)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                dragHandle?.invoke()
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_delete_item_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Area di tocco allargata (10dp di padding invece di 6dp) rispetto alla sola
                    // icona da 14dp: prima il bersaglio toccabile reale era di soli ~26dp,
                    // sotto il minimo consigliato.
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDeleteItem(item) }
                        .padding(10.dp)
                        .size(14.dp),
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 42.dp),
        )
    }
}

@Composable
private fun QuantityPillOutline(quantity: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = "×$quantity",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditItemRow(item: ShoppingItem, onSave: (String, Int) -> Unit, onCancel: () -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableStateOf(item.quantity) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun submit() {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) onSave(trimmed, quantity) else onCancel()
    }

    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleCheckbox(isChecked = item.isChecked, onToggle = {})
        Spacer(modifier = Modifier.width(14.dp))
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
        )
        QuantityStepper(quantity = quantity) { quantity = it.coerceAtLeast(1) }
        IconButton(onClick = { submit() }) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.edit_confirm_cd),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.edit_cancel_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🛒", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
