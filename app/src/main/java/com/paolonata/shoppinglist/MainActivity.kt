package com.paolonata.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.paolonata.shoppinglist.ui.AddFromTextScreen
import com.paolonata.shoppinglist.ui.HomeScreen
import com.paolonata.shoppinglist.ui.ShoppingListViewModel
import com.paolonata.shoppinglist.ui.theme.ListaSpesaTheme

private sealed interface Screen {
    data object Home : Screen
    data class AddFromText(val initialText: String) : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)

        setContent {
            ListaSpesaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    val items by viewModel.items.collectAsState()
                    val pendingShareText by viewModel.pendingShareText.collectAsState()

                    LaunchedEffect(pendingShareText) {
                        pendingShareText?.let { sharedText ->
                            screen = Screen.AddFromText(sharedText)
                            viewModel.consumePendingShareText()
                        }
                    }

                    Crossfade(targetState = screen, label = "screen") { current ->
                        when (current) {
                            is Screen.Home -> HomeScreen(
                                items = items,
                                onAddFromText = { screen = Screen.AddFromText("") },
                                onAddItem = { viewModel.addItemsFromText(it) },
                                onToggleChecked = viewModel::toggleChecked,
                                onDeleteItem = viewModel::deleteItem,
                                onClearChecked = viewModel::clearChecked,
                                onClearAll = viewModel::clearAll,
                            )

                            is Screen.AddFromText -> AddFromTextScreen(
                                initialText = current.initialText,
                                onParse = viewModel::previewParse,
                                onConfirm = { text ->
                                    viewModel.addItemsFromText(text) { count ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.add_items_added_toast, count),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    screen = Screen.Home
                                },
                                onCancel = { screen = Screen.Home },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                viewModel.onShareTextReceived(sharedText)
            }
        }
    }
}
