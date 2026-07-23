package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ShoppingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<ShoppingItem>,
    onAddFromText: () -> Unit,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val checkedCount = items.count { it.isChecked }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_title)) },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_menu_clear_checked)) },
                                onClick = { menuExpanded = false; onClearChecked() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_menu_clear_all)) },
                                onClick = { menuExpanded = false; onClearAll() },
                            )
                        }
                    },
                )
                if (items.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { if (items.isEmpty()) 0f else checkedCount / items.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.home_add_fab)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onAddFromText,
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp)) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.home_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            val toBuy = items.filter { !it.isChecked }
            val inCart = items.filter { it.isChecked }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (toBuy.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.home_section_to_buy)) }
                    items(toBuy, key = { it.id }) { shoppingItem ->
                        ShoppingItemRow(shoppingItem, onToggleChecked, onDeleteItem)
                    }
                }
                if (inCart.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.home_section_in_cart)) }
                    items(inCart, key = { it.id }) { shoppingItem ->
                        ShoppingItemRow(shoppingItem, onToggleChecked, onDeleteItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    onToggleChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = { onToggleChecked(item) })
        Column(modifier = Modifier.weight(1f)) {
            val label = if (item.quantity > 1) "${item.name}  ×${item.quantity}" else item.name
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            item.note?.let { note ->
                Text(
                    text = stringResource(R.string.add_quantity_note, note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onDeleteItem(item) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.home_delete_item_cd),
            )
        }
    }
}
