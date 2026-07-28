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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.sp
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

    // Stato del drag & drop: ordine locale che sovrascrive l'ordine reale mentre trascini.
    var localOrder by remember { mutableStateOf<List<Long>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(items) { if (draggingId == null) localOrder = null }

    LaunchedEffect(inlineAdding, items.size) {
        if (inlineAdding) {
            runCatching { listState.animateScrollToItem(items.size + 3) }
        }
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
            CleanHeader(
                total = items.size,
                checked = items.count { it.isChecked },
                onShare = { shareList(context, items) },
                onClearChecked = onClearChecked,
                onClearAll = onClearAll,
                items = items,
            )
        },
        bottomBar = {
            BottomActions(
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (toBuy.isNotEmpty()) {
                    item(key = "hdr_to_buy") {
                        SectionHeader(stringResource(R.string.home_section_to_buy), toBuy.size)
                    }
                    items(toBuy, key = { it.id }) { shoppingItem ->
                        val isDragging = draggingId == shoppingItem.id
                        val elevation by animateFloatAsState(
                            targetValue = if (isDragging) 12f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "elev",
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "scale",
                        )
                        ItemCard(
                            item = shoppingItem,
                            isEditing = editingItemId == shoppingItem.id,
                            elevationDp = elevation.dp,
                            onToggleChecked = onToggleChecked,
                            onDeleteItem = onDeleteItem,
                            onStartEdit = { editingItemId = it },
                            onSaveEdit = { item, name, qty ->
                                onEditItem(item, name, qty)
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
                                                    localOrder = order.toMutableList().apply {
                                                        add(currentIndex + 1, removeAt(currentIndex))
                                                    }
                                                    dragOffsetY -= height
                                                } else if (dragOffsetY < -height * 0.6f && currentIndex > 0) {
                                                    localOrder = order.toMutableList().apply {
                                                        add(currentIndex - 1, removeAt(currentIndex))
                                                    }
                                                    dragOffsetY += height
                                                }
                                            },
                                            onDragEnd = {
                                                val finalOrder = localOrder
                                                draggingId = null
                                                dragOffsetY = 0f
                                                if (finalOrder != null) {
                                                    val byId = allToBuy.associateBy { it.id }
                                                    val orderedItems = finalOrder.mapNotNull { byId[it] }
                                                    onReorderItems(orderedItems)
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
                                .onGloballyPositioned { coords -> itemHeights[shoppingItem.id] = coords.size.height }
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
                        SectionHeader(stringResource(R.string.home_section_in_cart), inCart.size)
                    }
                    items(inCart, key = { it.id }) { shoppingItem ->
                        ItemCard(
                            item = shoppingItem,
                            isEditing = editingItemId == shoppingItem.id,
                            onToggleChecked = onToggleChecked,
                            onDeleteItem = onDeleteItem,
                            onStartEdit = { editingItemId = it },
                            onSaveEdit = { item, name, qty ->
                                onEditItem(item, name, qty)
                                editingItemId = null
                            },
                            onCancelEdit = { editingItemId = null },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (inlineAdding) {
                    item(key = "inline_add") {
                        InlineAddRow(
                            onAdd = onAddItem,
                            onClose = { inlineAdding = false },
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

@Composable
private fun DragHandleIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = stringResource(R.string.reorder_handle_cd),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 2.dp)
            .size(22.dp),
    )
}

@Composable
private fun BottomActions(onManual: () -> Unit, onFromWhatsApp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = stringResource(R.string.home_add_manual),
            onClick = onManual,
        )
        FilledActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ContentPaste,
            label = stringResource(R.string.home_add_whatsapp),
            onClick = onFromWhatsApp,
        )
    }
}

@Composable
private fun FilledActionButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ActionButtonContent(icon = icon, label = label, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun OutlinedActionButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ActionButtonContent(icon = icon, label = label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ActionButtonContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 2,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun CleanHeader(
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
                context,
                Manifest.permission.POST_NOTIFICATIONS,
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
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (total > 0) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_chooser),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = if (notifEnabled) {
                                        Icons.Default.NotificationsActive
                                    } else {
                                        Icons.Default.NotificationsNone
                                    },
                                    contentDescription = null,
                                    tint = if (notifEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = {
                                Text(
                                    text = if (notifEnabled) {
                                        stringResource(R.string.notification_menu_toggle_off)
                                    } else {
                                        stringResource(R.string.notification_menu_toggle_on)
                                    },
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                toggleNotification()
                            },
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_progress, checked, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val fraction = if (total == 0) 0f else checked / total.toFloat()
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(500),
                label = "progress",
            )
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, count: Int) {
    Text(
        text = "$text · $count",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (quantity > 1) onChange(quantity - 1) },
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
            text = quantity.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = { onChange(quantity + 1) },
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

@Composable
private fun ItemCard(
    item: ShoppingItem,
    isEditing: Boolean,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onStartEdit: (Long) -> Unit,
    onSaveEdit: (ShoppingItem, String, Int) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
    elevationDp: androidx.compose.ui.unit.Dp = 1.dp,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevationDp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                val checkColor by animateColorAsState(
                    targetValue = if (item.isChecked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    animationSpec = tween(200),
                    label = "checkColor",
                )
                val checkScale by animateFloatAsState(
                    targetValue = if (item.isChecked) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "checkScale",
                )
                Icon(
                    imageVector = if (item.isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = checkColor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onToggleChecked(item) }
                        .size(24.dp)
                        .graphicsLayer { scaleX = checkScale; scaleY = checkScale },
                )
                Spacer(modifier = Modifier.width(14.dp))
                val textColor by animateColorAsState(
                    targetValue = if (item.isChecked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
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
                        style = MaterialTheme.typography.bodyLarge,
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
                    QuantityPill(item.quantity)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                dragHandle?.invoke()
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_delete_item_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDeleteItem(item) }
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
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
        Icon(
            imageVector = if (item.isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        QuantityStepper(quantity = quantity) { quantity = it.coerceAtLeast(1) }
        IconButton(onClick = { submit() }) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.edit_confirm_cd),
                tint = MaterialTheme.colorScheme.primary,
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
private fun QuantityPill(quantity: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = "×$quantity",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
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
            QuantityStepper(quantity = quantity) { quantity = it.coerceAtLeast(1) }
            IconButton(onClick = { submit() }) {
                Icon(
                    imageVector = if (value.isBlank()) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
