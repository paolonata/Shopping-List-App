package com.paolonata.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.paolonata.shoppinglist.notification.NotificationPrefs
import com.paolonata.shoppinglist.notification.ShoppingListNotifier
import com.paolonata.shoppinglist.ui.AddFromTextScreen
import com.paolonata.shoppinglist.ui.HomeScreen
import com.paolonata.shoppinglist.ui.ShoppingListViewModel
import com.paolonata.shoppinglist.ui.theme.ListaSpesaTheme
import com.paolonata.shoppinglist.ui.theme.ThemePrefs

private sealed interface Screen {
    data object Home : Screen
    data class AddFromText(val initialText: String) : Screen
}

/** Permette a `screen` di sopravvivere a rotazione/ricreazione dell'Activity. */
private val ScreenSaver = Saver<Screen, List<String>>(
    save = { screen ->
        when (screen) {
            is Screen.Home -> listOf("home")
            is Screen.AddFromText -> listOf("add", screen.initialText)
        }
    },
    restore = { saved ->
        if (saved.getOrNull(0) == "add") Screen.AddFromText(saved.getOrElse(1) { "" }) else Screen.Home
    },
)

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.init(this)
        // Solo alla primissima creazione: altrimenti, dopo una rotazione (che ricrea
        // l'Activity ma conserva lo stesso Intent originale), l'app rientrerebbe da sola
        // nella schermata di import da WhatsApp perdendo le modifiche in corso.
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }

        setContent {
            ListaSpesaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
                    val items by viewModel.items.collectAsState()
                    val pendingShareText by viewModel.pendingShareText.collectAsState()

                    BackHandler(enabled = screen is Screen.AddFromText) {
                        screen = Screen.Home
                    }

                    LaunchedEffect(pendingShareText) {
                        pendingShareText?.let { sharedText ->
                            screen = Screen.AddFromText(sharedText)
                            viewModel.consumePendingShareText()
                        }
                    }

                    // Tiene aggiornata la notifica persistente (se attiva) ogni volta che la
                    // lista cambia, così riflette anche le modifiche fatte dentro l'app.
                    LaunchedEffect(items) {
                        if (NotificationPrefs.isEnabled(this@MainActivity)) {
                            ShoppingListNotifier.show(this@MainActivity, items)
                        }
                    }

                    Crossfade(targetState = screen, label = "screen") { current ->
                        when (current) {
                            is Screen.Home -> HomeScreen(
                                items = items,
                                onAddFromText = { screen = Screen.AddFromText("") },
                                onAddItem = { text, quantity -> viewModel.addItemsFromText(text, quantity) },
                                onToggleChecked = viewModel::toggleChecked,
                                onDeleteItem = viewModel::deleteItem,
                                onEditItem = viewModel::updateItem,
                                onReorderItems = viewModel::reorderItems,
                                onClearChecked = viewModel::clearChecked,
                                onClearAll = viewModel::clearAll,
                            )

                            is Screen.AddFromText -> AddFromTextScreen(
                                initialText = current.initialText,
                                onParse = viewModel::previewParse,
                                onConfirm = { parsedItems ->
                                    viewModel.addParsedItems(parsedItems) { count ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            resources.getQuantityString(R.plurals.add_items_added_toast, count, count),
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
