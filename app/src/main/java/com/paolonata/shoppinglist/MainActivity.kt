package com.paolonata.shoppinglist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.paolonata.shoppinglist.notification.NotificationPrefs
import com.paolonata.shoppinglist.notification.ReceiptDeadlinePrefs
import com.paolonata.shoppinglist.notification.ReceiptDeadlineScheduler
import com.paolonata.shoppinglist.notification.ShoppingListNotifier
import com.paolonata.shoppinglist.ui.AddFromTextScreen
import com.paolonata.shoppinglist.ui.AppSection
import com.paolonata.shoppinglist.ui.HomeScreen
import com.paolonata.shoppinglist.ui.ReceiptDetailScreen
import com.paolonata.shoppinglist.ui.ReceiptsScreen
import com.paolonata.shoppinglist.ui.ReceiptsViewModel
import com.paolonata.shoppinglist.ui.ShoppingListViewModel
import com.paolonata.shoppinglist.ui.theme.ListaSpesaTheme
import com.paolonata.shoppinglist.ui.theme.ThemePrefs
import java.io.File

/** Quante pagine si possono scegliere in un colpo dalla galleria. */
private const val MAX_PAGES = 10

private sealed interface Screen {
    data object Home : Screen
    data class AddFromText(val initialText: String) : Screen
    data class ReceiptDetail(val id: Long) : Screen
}

/** Permette a `screen` di sopravvivere a rotazione/ricreazione dell'Activity. */
private val ScreenSaver = Saver<Screen, List<String>>(
    save = { screen ->
        when (screen) {
            is Screen.Home -> listOf("home")
            is Screen.AddFromText -> listOf("add", screen.initialText)
            is Screen.ReceiptDetail -> listOf("receipt", screen.id.toString())
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0)) {
            "add" -> Screen.AddFromText(saved.getOrElse(1) { "" })
            "receipt" -> saved.getOrNull(1)?.toLongOrNull()?.let { Screen.ReceiptDetail(it) } ?: Screen.Home
            else -> Screen.Home
        }
    },
)

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()
    private val receiptsViewModel: ReceiptsViewModel by viewModels()

    /** Dove la fotocamera di sistema scrive lo scatto in arrivo. */
    private var pendingCapture: Uri? = null

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
                    var section by rememberSaveable {
                        mutableStateOf(
                            if (intent?.getBooleanExtra(EXTRA_OPEN_RECEIPTS, false) == true) {
                                AppSection.RECEIPTS
                            } else {
                                AppSection.LIST
                            },
                        )
                    }
                    val items by viewModel.items.collectAsState()
                    val receipts by receiptsViewModel.receipts.collectAsState()
                    val deadlines by receiptsViewModel.deadlines.collectAsState()
                    val categories by receiptsViewModel.categories.collectAsState()
                    val pendingShareText by viewModel.pendingShareText.collectAsState()
                    val context = LocalContext.current

                    var remindersOn by remember { mutableStateOf(ReceiptDeadlinePrefs.isEnabled(this@MainActivity)) }
                    val remindersOnMessage = stringResource(R.string.receipt_reminders_on)
                    val remindersOffMessage = stringResource(R.string.receipt_reminders_off)

                    val notificationPermission = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { granted ->
                        if (granted) {
                            ReceiptDeadlineScheduler.enable(this@MainActivity)
                            remindersOn = true
                        }
                    }

                    val savedMessage = stringResource(R.string.receipts_saved_toast)
                    val pdfFailedMessage = stringResource(R.string.receipts_pdf_failed)
                    val rescanOkMessage = stringResource(R.string.receipt_rescan_ok)
                    val rescanEmptyMessage = stringResource(R.string.receipt_rescan_empty)
                    fun saved() = Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()

                    // La fotocamera di sistema scrive nel file che le passiamo;
                    // la galleria restituisce direttamente le immagini scelte.
                    val takePicture = rememberLauncherForActivityResult(
                        ActivityResultContracts.TakePicture(),
                    ) { ok ->
                        val uri = pendingCapture
                        pendingCapture = null
                        if (ok && uri != null) {
                            receiptsViewModel.addFromPhotos(listOf(uri)) { saved() }
                        }
                    }

                    val pickPhotos = rememberLauncherForActivityResult(
                        ActivityResultContracts.PickMultipleVisualMedia(MAX_PAGES),
                    ) { uris ->
                        if (uris.isNotEmpty()) receiptsViewModel.addFromPhotos(uris) { saved() }
                    }

                    // Il selettore di foto non mostra i PDF: per quelli serve
                    // il selettore di documenti del sistema.
                    val pickPdf = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri != null) {
                            receiptsViewModel.addFromPdf(uri) { id ->
                                Toast.makeText(
                                    this@MainActivity,
                                    if (id != null) savedMessage else pdfFailedMessage,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }

                    BackHandler(enabled = screen is Screen.AddFromText || screen is Screen.ReceiptDetail) {
                        screen = Screen.Home
                    }

                    // Dagli scontrini, Indietro torna alla lista invece di
                    // chiudere l'app: è la schermata da cui si è partiti.
                    BackHandler(enabled = screen is Screen.Home && section == AppSection.RECEIPTS) {
                        section = AppSection.LIST
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
                            is Screen.Home -> if (section == AppSection.RECEIPTS) {
                                ReceiptsScreen(
                                    receipts = receipts,
                                    deadlines = deadlines,
                                    section = section,
                                    onSectionChange = { section = it },
                                    onTakePhoto = {
                                        val uri = newCaptureUri()
                                        pendingCapture = uri
                                        takePicture.launch(uri)
                                    },
                                    onPickPhoto = {
                                        pickPhotos.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) },
                                    onOpen = { screen = Screen.ReceiptDetail(it) },
                                    categories = categories,
                                    remindersOn = remindersOn,
                                    onToggleReminders = {
                                        if (remindersOn) {
                                            ReceiptDeadlineScheduler.disable(context)
                                            remindersOn = false
                                            Toast.makeText(context, remindersOffMessage, Toast.LENGTH_SHORT).show()
                                        } else if (needsNotificationPermission(context)) {
                                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            ReceiptDeadlineScheduler.enable(context)
                                            remindersOn = true
                                            Toast.makeText(context, remindersOnMessage, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                )
                            } else HomeScreen(
                                items = items,
                                section = section,
                                onSectionChange = { section = it },
                                onAddFromText = { screen = Screen.AddFromText("") },
                                onAddItem = { text, quantity -> viewModel.addItemsFromText(text, quantity) },
                                onToggleChecked = viewModel::toggleChecked,
                                onDeleteItem = viewModel::deleteItem,
                                onEditItem = viewModel::updateItem,
                                onReorderItems = viewModel::reorderItems,
                                onClearChecked = viewModel::clearChecked,
                                onClearAll = viewModel::clearAll,
                            )

                            is Screen.ReceiptDetail -> {
                                val entry = receipts.firstOrNull { it.receipt.id == current.id }
                                if (entry == null) {
                                    // Cestinato mentre era aperto: si torna all'archivio.
                                    LaunchedEffect(current.id) { screen = Screen.Home }
                                } else {
                                    ReceiptDetailScreen(
                                        entry = entry,
                                        categories = categories,
                                        onBack = { screen = Screen.Home },
                                        onSave = receiptsViewModel::save,
                                        onReturnDone = { receiptsViewModel.setReturnDone(current.id, it) },
                                        onDelete = {
                                            receiptsViewModel.moveToTrash(current.id)
                                            screen = Screen.Home
                                        },
                                        onRescan = {
                                            receiptsViewModel.rescan(current.id) { found ->
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    if (found) rescanOkMessage else rescanEmptyMessage,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        },
                                        onCreateCategory = receiptsViewModel::createCategory,
                                        onUpdateCategory = receiptsViewModel::updateCategory,
                                        onDeleteCategory = receiptsViewModel::deleteCategory,
                                    )
                                }
                            }

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

    companion object {
        /** Impostato dalla notifica delle scadenze: apre l'app già sugli scontrini. */
        const val EXTRA_OPEN_RECEIPTS = "open_receipts"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * Un file nuovo per ogni scatto, dentro la memoria privata dell'app, e
     * un permesso temporaneo alla fotocamera per scriverci: la galleria del
     * telefono non si riempie di scontrini.
     */
    private fun needsNotificationPermission(context: android.content.Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

    private fun newCaptureUri(): Uri {
        val dir = File(filesDir, "captures").apply { mkdirs() }
        val file = File(dir, "scatto-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                viewModel.onShareTextReceived(sharedText)
            }
        }
    }
}
