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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    var inlineAdding by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var localOrder by remember { mutableStateOf<List<Long>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(items) { if (draggingId == null) localOrder = null }
    LaunchedEffect(inlineAdding, items.size) {
        if (inlineAdding) runCatching { listState.animateScrollToItem(items.size + 3) }
    }

    val allToBuy = items.filter { !it.isChecked }
    val inCart = items.filter { it.isChecked }
    val toBuy = localOrder?.let { order ->
        val byId = allToBuy.associateBy { it.id }
        order.mapNotNull { byId[it] } + allToBuy.filter { it.id !in order }
    } ?: allToBuy

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HeroHeader(
                total = items.size,
                checked = items.count { it.isChecked },
                onShare = { shareList(context, items) },
                onClearChecked = onClearChecked,
                onClearAll = onClearAll,
                items = items,
            )
        },
        bottomBar = {
            BottomActionsPill(
                onManual = { inlineAdding = true },
                onFromWhatsApp = onAddFromText,
            )
        },
    ) { padding ->
        if (items.isEmpty() && !inlineAdding) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (toBuy.isNotEmpty()) {
                    item(key = "hdr_to_buy") {
                        MinimalSectionHeader(stringResource(R.string.home_section_to_buy), toBuy.size)
                    }
                    items(toBuy, key = { it.id }) { shoppingItem ->
                        val isDragging = draggingId == shoppingItem.id
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.04f else 1f,
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
                if (inlineAdding) {
                    item(key = "inline_add") {
                        InlineAddRow(onAdd = onAddItem, onClose = { inlineAdding = false })
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

@Composable
private fun HeroHeader(
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
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (total > 0) {
                IconChipButton(icon = Icons.Default.IosShare, onClick = onShare)
                Spacer(modifier = Modifier.width(4.dp))
                Box {
                    IconChipButton(icon = Icons.Default.MoreHoriz, onClick = { menuExpanded = true })
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.home_progress, checked, total),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.home_progress_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun IconChipButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MinimalSectionHeader(text: String, count: Int) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BottomActionsPill(onManual: () -> Unit, onFromWhatsApp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Azione primaria: sinistra ampia con accento lime
        PrimaryPillButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = stringResource(R.string.home_add_manual),
            onClick = onManual,
        )
        // Secondaria: fondo neutro/bordo netto
        SecondaryPillButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ContentPaste,
            label = stringResource(R.string.home_add_whatsapp),
            onClick = onFromWhatsApp,
        )
    }
}

@Composable
private fun PrimaryPillButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SecondaryPillButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Checkbox quadrata arrotondata: quando spuntata diventa piena lime con la spunta nera. */
@Composable
private fun SquareCheckbox(
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "cbBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180),
        label = "cbBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cbScale",
    )
    Box(
        modifier = modifier
            .size(26.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
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
            .size(22.dp),
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
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = spring(),
        label = "elev",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = elevation.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isEditing) {
            EditItemRow(
                item = item,
                onSave = { name, quantity -> onSaveEdit(item, name, quantity) },
                onCancel = onCancelEdit,
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SquareCheckbox(isChecked = item.isChecked, onToggle = { onToggleChecked(item) })
                Spacer(modifier = Modifier.width(14.dp))
                val textColor by animateColorAsState(
                    targetValue = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "textColor",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onStartEdit(item.id) },
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
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
                    QuantityBadge(item.quantity, dimmed = item.isChecked)
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
    }
}

@Composable
private fun QuantityBadge(quantity: Int, dimmed: Boolean = false) {
    val bg = if (dimmed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground
    val fg = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "×$quantity",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
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
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareCheckbox(isChecked = item.isChecked, onToggle = {})
        Spacer(modifier = Modifier.width(14.dp))
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
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
            // Blob lime con carrellino testuale
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🛒",
                    style = MaterialTheme.typography.displayMedium,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall,
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

@Composable
private fun InlineAddRow(onAdd: (String) -> Unit, onClose: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun submit() {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(if (quantity > 1) "$quantity $trimmed" else trimmed)
            value = ""
            quantity = 1
        } else {
            onClose()
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareCheckbox(isChecked = false, onToggle = {})
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
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
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            QuantityStepper(quantity = quantity) { quantity = it.coerceAtLeast(1) }
            IconButton(onClick = { submit() }) {
                Icon(
                    imageVector = if (value.isBlank()) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
