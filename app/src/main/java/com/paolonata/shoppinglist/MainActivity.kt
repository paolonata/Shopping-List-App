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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.paolonata.shoppinglist.notification.NotificationPrefs
import com.paolonata.shoppinglist.notification.ReceiptDeadlinePrefs
import com.paolonata.shoppinglist.notification.ReceiptDeadlineScheduler
import com.paolonata.shoppinglist.notification.ShoppingListNotifier
import com.paolonata.shoppinglist.ui.AppTab
import com.paolonata.shoppinglist.ui.AppTabBar
import com.paolonata.shoppinglist.ui.DeadlinesScreen
import com.paolonata.shoppinglist.ui.NewReceiptFab
import com.paolonata.shoppinglist.ui.NewReceiptSheet
import com.paolonata.shoppinglist.ui.ReceiptDetailScreen
import com.paolonata.shoppinglist.ui.ReceiptsHomeScreen
import com.paolonata.shoppinglist.ui.ReceiptsViewModel
import com.paolonata.shoppinglist.ui.SettingsScreen
import com.paolonata.shoppinglist.ui.ShoppingListViewModel
import com.paolonata.shoppinglist.ui.SpesaScreen
import com.paolonata.shoppinglist.ui.StatsScreen
import com.paolonata.shoppinglist.ui.TrashSheet
import com.paolonata.shoppinglist.ui.WhatsAppSheet
import com.paolonata.shoppinglist.ui.theme.ListaSpesaTheme
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OrganicToast
import com.paolonata.shoppinglist.ui.theme.ThemePrefs
import java.io.File

/** Quante pagine si possono scegliere in un colpo dalla galleria. */
private const val MAX_PAGES = 10

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()
    private val receiptsViewModel: ReceiptsViewModel by viewModels()

    /** Dove la fotocamera di sistema scrive lo scatto in arrivo. */
    private var pendingCapture: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.init(this)
        // Solo alla primissima creazione: dopo una rotazione l'Intent è lo
        // stesso di prima, e riaprire il foglio di import perderebbe quello
        // che si stava facendo.
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }

        setContent {
            ListaSpesaTheme {
                val context = LocalContext.current

                var tab by rememberSaveable {
                    mutableStateOf(
                        if (intent?.getBooleanExtra(EXTRA_OPEN_RECEIPTS, false) == true) {
                            AppTab.RECEIPTS
                        } else {
                            AppTab.SPESA
                        },
                    )
                }
                var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
                var waSheetOpen by rememberSaveable { mutableStateOf(false) }
                var waSheetText by rememberSaveable { mutableStateOf("") }
                var newReceiptOpen by rememberSaveable { mutableStateOf(false) }
                var trashOpen by rememberSaveable { mutableStateOf(false) }
                var toast by remember { mutableStateOf<String?>(null) }
                val pendingPhotos = remember { mutableStateListOf<Uri>() }

                val items by viewModel.items.collectAsState()
                val receipts by receiptsViewModel.receipts.collectAsState()
                val deadlines by receiptsViewModel.deadlines.collectAsState()
                val categories by receiptsViewModel.categories.collectAsState()
                val trashed by receiptsViewModel.trashed.collectAsState()
                val pendingShareText by viewModel.pendingShareText.collectAsState()

                var remindersOn by remember { mutableStateOf(ReceiptDeadlinePrefs.isEnabled(this@MainActivity)) }
                val remindersOnMessage = stringResource(R.string.receipt_reminders_on)
                val remindersOffMessage = stringResource(R.string.receipt_reminders_off)
                val savedMessage = stringResource(R.string.receipts_saved_toast)
                val pdfFailedMessage = stringResource(R.string.receipts_pdf_failed)
                val rescanOkMessage = stringResource(R.string.receipt_rescan_ok)
                val rescanEmptyMessage = stringResource(R.string.receipt_rescan_empty)
                val listSharedMessage = stringResource(R.string.spesa_shared_toast)
                val checkedRemovedMessage = stringResource(R.string.spesa_checked_removed_toast)
                val listClearedMessage = stringResource(R.string.spesa_cleared_toast)
                val everythingDeletedMessage = stringResource(R.string.settings_deleted_all_toast)

                fun flash(text: String) {
                    toast = text
                }

                LaunchedEffect(toast) {
                    if (toast != null) {
                        kotlinx.coroutines.delay(2200)
                        toast = null
                    }
                }

                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        ReceiptDeadlineScheduler.enable(this@MainActivity)
                        remindersOn = true
                    }
                }

                val takePicture = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture(),
                ) { ok ->
                    val uri = pendingCapture
                    pendingCapture = null
                    if (ok && uri != null) pendingPhotos.add(uri)
                }

                val pickPhotos = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia(MAX_PAGES),
                ) { uris ->
                    pendingPhotos.addAll(uris)
                }

                // Il selettore di foto non mostra i PDF: per quelli serve
                // il selettore di documenti del sistema.
                val pickPdf = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri != null) {
                        newReceiptOpen = false
                        receiptsViewModel.addFromPdf(uri) { id ->
                            flash(if (id != null) savedMessage else pdfFailedMessage)
                        }
                    }
                }

                BackHandler(enabled = detailId != null) { detailId = null }
                BackHandler(enabled = detailId == null && waSheetOpen) { waSheetOpen = false }
                BackHandler(enabled = detailId == null && !waSheetOpen && newReceiptOpen) {
                    newReceiptOpen = false
                    pendingPhotos.clear()
                }
                BackHandler(enabled = detailId == null && !waSheetOpen && !newReceiptOpen && trashOpen) {
                    trashOpen = false
                }
                // Da qualunque altra scheda, Indietro riporta alla spesa
                // invece di chiudere l'app: è la schermata da cui si parte.
                BackHandler(
                    enabled = detailId == null && !waSheetOpen && !newReceiptOpen && !trashOpen &&
                        tab != AppTab.SPESA,
                ) {
                    tab = AppTab.SPESA
                }

                LaunchedEffect(pendingShareText) {
                    pendingShareText?.let { sharedText ->
                        waSheetText = sharedText
                        waSheetOpen = true
                        tab = AppTab.SPESA
                        viewModel.consumePendingShareText()
                    }
                }

                // Tiene aggiornata la notifica persistente (se attiva) ogni
                // volta che la lista cambia.
                LaunchedEffect(items) {
                    if (NotificationPrefs.isEnabled(this@MainActivity)) {
                        ShoppingListNotifier.show(this@MainActivity, items)
                    }
                }

                val urgent = deadlines.count { it.status.days in 0..7 }
                val toBuy = items.count { !it.isChecked }

                Surface(color = Organic.bg, modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .statusBarsPadding(),
                            ) {
                                Crossfade(targetState = tab, label = "tab") { current ->
                                    when (current) {
                                        AppTab.SPESA -> SpesaScreen(
                                            items = items,
                                            onOpenWhatsApp = { waSheetText = ""; waSheetOpen = true },
                                            onAddItem = { text, quantity -> viewModel.addItemsFromText(text, quantity) },
                                            onToggleChecked = viewModel::toggleChecked,
                                            onDeleteItem = viewModel::deleteItem,
                                            onEditItem = viewModel::updateItem,
                                            onClearChecked = {
                                                viewModel.clearChecked()
                                                flash(checkedRemovedMessage)
                                            },
                                            onClearAll = {
                                                viewModel.clearAll()
                                                flash(listClearedMessage)
                                            },
                                            onShare = {
                                                shareList(context, items)
                                                flash(listSharedMessage)
                                            },
                                            onPhotographReceipt = {
                                                tab = AppTab.RECEIPTS
                                                newReceiptOpen = true
                                            },
                                        )

                                        AppTab.RECEIPTS -> ReceiptsHomeScreen(
                                            receipts = receipts,
                                            categories = categories,
                                            deadlines = deadlines,
                                            itemsToBuy = toBuy,
                                            onOpen = { detailId = it },
                                            onGoDeadlines = { tab = AppTab.DEADLINES },
                                            onGoSpesa = { tab = AppTab.SPESA },
                                            onGoStats = { tab = AppTab.STATS },
                                        )

                                        AppTab.DEADLINES -> DeadlinesScreen(
                                            deadlines = deadlines,
                                            onOpen = { detailId = it },
                                        )

                                        AppTab.STATS -> StatsScreen(
                                            receipts = receipts,
                                            categories = categories,
                                        )

                                        AppTab.SETTINGS -> SettingsScreen(
                                            items = items,
                                            receipts = receipts,
                                            trashed = trashed,
                                            receiptRemindersOn = remindersOn,
                                            onToggleReceiptReminders = {
                                                if (remindersOn) {
                                                    ReceiptDeadlineScheduler.disable(context)
                                                    remindersOn = false
                                                    flash(remindersOffMessage)
                                                } else if (needsNotificationPermission(context)) {
                                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    ReceiptDeadlineScheduler.enable(context)
                                                    remindersOn = true
                                                    flash(remindersOnMessage)
                                                }
                                            },
                                            onOpenTrash = { trashOpen = true },
                                            onDeleteEverything = {
                                                receiptsViewModel.deleteEverything()
                                                viewModel.clearAll()
                                                flash(everythingDeletedMessage)
                                            },
                                        )
                                    }
                                }

                                if (tab != AppTab.SPESA && detailId == null && !newReceiptOpen && !waSheetOpen) {
                                    NewReceiptFab(
                                        onClick = { newReceiptOpen = true },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 18.dp, bottom = 18.dp),
                                    )
                                }

                                toast?.let { message ->
                                    OrganicToast(
                                        text = message,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(horizontal = 20.dp, vertical = 18.dp),
                                    )
                                }
                            }

                            AppTabBar(
                                current = tab,
                                urgentCount = urgent,
                                onSelect = { tab = it },
                                modifier = Modifier.navigationBarsPadding(),
                            )
                        }

                        detailId?.let { id ->
                            val entry = receipts.firstOrNull { it.receipt.id == id }
                            if (entry == null) {
                                // Cestinato mentre era aperto: si torna all'archivio.
                                LaunchedEffect(id) { detailId = null }
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    ReceiptDetailScreen(
                                        entry = entry,
                                        categories = categories,
                                        onBack = { detailId = null },
                                        onSave = receiptsViewModel::save,
                                        onReturnDone = { receiptsViewModel.setReturnDone(id, it) },
                                        onDelete = {
                                            receiptsViewModel.moveToTrash(id)
                                            detailId = null
                                        },
                                        onRescan = {
                                            receiptsViewModel.rescan(id) { found ->
                                                flash(if (found) rescanOkMessage else rescanEmptyMessage)
                                            }
                                        },
                                        onCreateCategory = receiptsViewModel::createCategory,
                                        onUpdateCategory = receiptsViewModel::updateCategory,
                                        onDeleteCategory = receiptsViewModel::deleteCategory,
                                    )
                                }
                            }
                        }

                        if (waSheetOpen) {
                            WhatsAppSheet(
                                initialText = waSheetText,
                                onParse = viewModel::previewParse,
                                onConfirm = { parsedItems ->
                                    viewModel.addParsedItems(parsedItems) { count ->
                                        flash(
                                            resources.getQuantityString(
                                                R.plurals.add_items_added_toast, count, count,
                                            ),
                                        )
                                    }
                                    waSheetOpen = false
                                    waSheetText = ""
                                },
                                onDismiss = { waSheetOpen = false },
                            )
                        }

                        if (newReceiptOpen) {
                            NewReceiptSheet(
                                categories = categories,
                                pendingPhotos = pendingPhotos.toList(),
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
                                onSave = { title, amount, categoryId, returnDays ->
                                    receiptsViewModel.createManual(
                                        sources = pendingPhotos.toList(),
                                        title = title,
                                        amount = amount,
                                        categoryId = categoryId,
                                        returnDays = returnDays,
                                    ) { flash(savedMessage) }
                                    pendingPhotos.clear()
                                    newReceiptOpen = false
                                    tab = AppTab.RECEIPTS
                                },
                                onDismiss = {
                                    newReceiptOpen = false
                                    pendingPhotos.clear()
                                },
                            )
                        }

                        if (trashOpen) {
                            TrashSheet(
                                trashed = trashed,
                                onRestore = { receiptsViewModel.restore(it) },
                                onPurge = { receiptsViewModel.purge(it) },
                                onDismiss = { trashOpen = false },
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

    private fun needsNotificationPermission(context: android.content.Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

    /**
     * Un file nuovo per ogni scatto, dentro la memoria privata dell'app, e
     * un permesso temporaneo alla fotocamera per scriverci: la galleria del
     * telefono non si riempie di scontrini.
     */
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

/** Manda la lista fuori dall'app: solo quello che manca ancora. */
private fun shareList(context: android.content.Context, items: List<com.paolonata.shoppinglist.data.ShoppingItem>) {
    val toBuy = items.filter { !it.isChecked }
    if (toBuy.isEmpty()) return
    val text = toBuy.joinToString("\n") { item ->
        val quantity = if (item.quantity > 1) " ×${item.quantity}" else ""
        "• ${item.name}$quantity"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_chooser)),
    )
}
