package com.paolonata.shoppinglist.ui

import android.Manifest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.paolonata.shoppinglist.ui.theme.ThemeMode
import com.paolonata.shoppinglist.ui.theme.ThemePrefs

@Composable
fun HomeScreen(
    items: List<ShoppingItem>,
    onAddFromText: () -> Unit,
    onAddItem: (String) -> Unit,
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
                                                localOrder = toBuy.map { it.id }
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
                                                    val byId = allToBuy.associateBy { it.id }
                                                    onReorderItems(finalOrder.mapNotNull { byId[it] })
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
    if (items.isEmpty()) return
    val sb = StringBuilder(context.getString(R.string.share_title))
    for (item in items) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
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
                OutlinedIconButton(icon = Icons.Default.MoreHoriz, onClick = { menuExpanded = true })
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
}

/** Icona in un cerchio con bordo sottile — usata per l'unica azione "con contorno" dell'header. */
@Composable
private fun OutlinedIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
            contentDescription = null,
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Barra fissa in basso: icona per importare da WhatsApp + campo di testo sempre pronto
 * per aggiungere un articolo a mano (nessun pulsante vistoso, in linea con lo stile). */
@Composable
private fun BottomQuickAddBar(
    onAdd: (String) -> Unit,
    onFromWhatsApp: () -> Unit,
    focusRequester: FocusRequester,
) {
    var value by remember { mutableStateOf("") }

    fun submit() {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed)
            value = ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
                    .clickable { onStartEdit(item.id) }
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
                Column(modifier = Modifier.weight(1f)) {
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
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDeleteItem(item) }
                        .padding(6.dp)
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
