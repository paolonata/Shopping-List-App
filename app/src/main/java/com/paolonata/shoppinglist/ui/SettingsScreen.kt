package com.paolonata.shoppinglist.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.data.ShoppingItem
import com.paolonata.shoppinglist.notification.NotificationPrefs
import com.paolonata.shoppinglist.notification.ShoppingListNotifier
import com.paolonata.shoppinglist.notification.ShoppingReminderPrefs
import com.paolonata.shoppinglist.notification.ShoppingReminderScheduler
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OrganicSwitch
import com.paolonata.shoppinglist.ui.theme.PillShape
import com.paolonata.shoppinglist.ui.theme.SegmentedTabs
import com.paolonata.shoppinglist.ui.theme.ThemeMode
import com.paolonata.shoppinglist.ui.theme.ThemePrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val REMINDER_MORNING_HOUR = 9
private const val REMINDER_EVENING_HOUR = 18

/**
 * Le impostazioni: i promemoria, l'aspetto, quanto spazio occupano le
 * foto e il pulsante che cancella tutto. Le voci del prototipo che sul
 * telefono non vogliono dire niente (lo spazio "protetto" del browser)
 * sono state sostituite da quelle vere dell'app.
 */
@Composable
fun SettingsScreen(
    items: List<ShoppingItem>,
    receipts: List<ReceiptWithPhotos>,
    trashed: List<ReceiptWithPhotos>,
    receiptRemindersOn: Boolean,
    onToggleReceiptReminders: () -> Unit,
    onOpenTrash: () -> Unit,
    onDeleteEverything: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeMode by ThemePrefs.mode.collectAsState()

    var notifEnabled by remember { mutableStateOf(NotificationPrefs.isEnabled(context)) }
    var reminderAt by remember { mutableStateOf(ShoppingReminderPrefs.getReminderAt(context)) }
    var reminderDialogOpen by remember { mutableStateOf(false) }
    var confirmDeleteOpen by remember { mutableStateOf(false) }
    var pendingReminderMillis by remember { mutableStateOf<Long?>(null) }

    val permissionDenied = stringResource(R.string.notification_permission_denied)
    val notifOn = stringResource(R.string.notification_enabled_toast)
    val notifOff = stringResource(R.string.notification_disabled_toast)
    val reminderSetTemplate = stringResource(R.string.reminder_set_toast)
    val reminderCancelled = stringResource(R.string.reminder_cancelled_toast)
    val reminderDenied = stringResource(R.string.reminder_permission_denied)

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            notifEnabled = true
            NotificationPrefs.setEnabled(context, true)
            ShoppingListNotifier.show(context, items)
            Toast.makeText(context, notifOn, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, permissionDenied, Toast.LENGTH_LONG).show()
        }
    }

    val reminderPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val millis = pendingReminderMillis
        pendingReminderMillis = null
        if (granted && millis != null) {
            ShoppingReminderScheduler.schedule(context, millis)
            reminderAt = millis
            Toast.makeText(context, String.format(reminderSetTemplate, formatReminderTime(millis)), Toast.LENGTH_LONG).show()
        } else if (!granted) {
            Toast.makeText(context, reminderDenied, Toast.LENGTH_LONG).show()
        }
    }

    fun needsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED

    fun toggleNotification() {
        if (notifEnabled) {
            notifEnabled = false
            NotificationPrefs.setEnabled(context, false)
            ShoppingListNotifier.cancel(context)
            Toast.makeText(context, notifOff, Toast.LENGTH_SHORT).show()
            return
        }
        if (needsPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifEnabled = true
            NotificationPrefs.setEnabled(context, true)
            ShoppingListNotifier.show(context, items)
            Toast.makeText(context, notifOn, Toast.LENGTH_LONG).show()
        }
    }

    fun requestReminder(atMillis: Long) {
        if (needsPermission()) {
            pendingReminderMillis = atMillis
            reminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ShoppingReminderScheduler.schedule(context, atMillis)
            reminderAt = atMillis
            Toast.makeText(context, String.format(reminderSetTemplate, formatReminderTime(atMillis)), Toast.LENGTH_LONG).show()
        }
    }

    fun pickCustomReminder() {
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

    val photoBytes = receipts.sumOf { entry -> entry.photos.sumOf { it.sizeBytes } }
    val photoCount = receipts.sumOf { it.photos.size }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 108.dp),
    ) {
        item("title") {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Organic.text,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        item("reminders-title") {
            SettingsSectionTitle(stringResource(R.string.settings_section_reminders))
        }

        item("reminders") {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_lockscreen_title),
                    subtitle = stringResource(R.string.settings_lockscreen_sub),
                    onClick = { toggleNotification() },
                    trailing = { OrganicSwitch(checked = notifEnabled, onToggle = { toggleNotification() }) },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Alarm,
                    title = stringResource(R.string.settings_reminder_title),
                    subtitle = reminderAt?.let {
                        stringResource(R.string.settings_reminder_active, formatReminderTime(it))
                    } ?: stringResource(R.string.settings_reminder_sub),
                    onClick = { reminderDialogOpen = true },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.settings_deadline_reminders_title),
                    subtitle = stringResource(R.string.settings_deadline_reminders_sub),
                    onClick = onToggleReceiptReminders,
                    trailing = {
                        OrganicSwitch(checked = receiptRemindersOn, onToggle = onToggleReceiptReminders)
                    },
                )
            }
        }

        item("data-title") {
            SettingsSectionTitle(stringResource(R.string.settings_section_data), top = 20.dp)
        }

        item("data") {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.DeleteOutline,
                    title = stringResource(R.string.settings_trash_title),
                    subtitle = if (trashed.isEmpty()) {
                        stringResource(R.string.settings_trash_empty)
                    } else {
                        stringResource(R.string.settings_trash_count, trashed.size)
                    },
                    onClick = onOpenTrash,
                )
            }
        }

        item("look-title") {
            SettingsSectionTitle(stringResource(R.string.settings_section_look), top = 20.dp)
        }

        item("look") {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.theme_auto),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark),
                ),
                selectedIndex = when (themeMode) {
                    ThemeMode.AUTO -> 0
                    ThemeMode.LIGHT -> 1
                    ThemeMode.DARK -> 2
                },
                onSelect = { index ->
                    ThemePrefs.set(
                        context,
                        when (index) {
                            0 -> ThemeMode.AUTO
                            1 -> ThemeMode.LIGHT
                            else -> ThemeMode.DARK
                        },
                    )
                },
            )
        }

        item("storage-title") {
            SettingsSectionTitle(stringResource(R.string.settings_section_storage), top = 20.dp)
        }

        item("storage") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, CardShape)
                    .padding(horizontal = 16.dp, vertical = 17.dp),
            ) {
                Text(
                    text = formatSize(photoBytes),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = Organic.text,
                )
                Text(
                    text = stringResource(R.string.settings_storage_sub, receipts.size, photoCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Organic.neutral700,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(PillShape)
                        .background(Organic.neutral200),
                ) {
                    // Una barra piena al massimo per un quarto: senza sapere
                    // quanto spazio resta sul telefono, riempirla tutta
                    // sarebbe un allarme inventato.
                    val fraction = (photoBytes / (200.0 * 1024 * 1024)).toFloat().coerceIn(0.02f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(9.dp)
                            .clip(PillShape)
                            .background(Organic.accent2500),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(PillShape)
                        .border(1.5.dp, Organic.accent600, PillShape)
                        .clickable { confirmDeleteOpen = true }
                        .padding(vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Organic.accent700,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_delete_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = Organic.accent700,
                    )
                }
            }
        }

        item("privacy") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Organic.accent2200)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Organic.accent2800,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.settings_privacy_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Organic.accent2800,
                    )
                    Text(
                        text = stringResource(R.string.settings_privacy_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Organic.accent2800,
                    )
                }
            }
        }

        item("footer") {
            Text(
                text = stringResource(R.string.settings_footer),
                style = MaterialTheme.typography.bodySmall,
                color = Organic.neutral600,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth(),
            )
        }
    }

    if (reminderDialogOpen) {
        AlertDialog(
            containerColor = Organic.neutral100,
            onDismissRequest = { reminderDialogOpen = false },
            title = {
                Text(
                    text = stringResource(R.string.reminder_dialog_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Organic.text,
                )
            },
            text = {
                Column {
                    ReminderOption(stringResource(R.string.reminder_option_tonight, timeLabel(REMINDER_EVENING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_EVENING_HOUR, forceTomorrow = false))
                    }
                    ReminderOption(stringResource(R.string.reminder_option_tomorrow_morning, timeLabel(REMINDER_MORNING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_MORNING_HOUR, forceTomorrow = true))
                    }
                    ReminderOption(stringResource(R.string.reminder_option_tomorrow_evening, timeLabel(REMINDER_EVENING_HOUR))) {
                        reminderDialogOpen = false
                        requestReminder(computeReminderMillis(REMINDER_EVENING_HOUR, forceTomorrow = true))
                    }
                    ReminderOption(stringResource(R.string.reminder_option_custom)) {
                        reminderDialogOpen = false
                        pickCustomReminder()
                    }
                    if (reminderAt != null) {
                        ReminderOption(stringResource(R.string.reminder_option_cancel), destructive = true) {
                            reminderDialogOpen = false
                            ShoppingReminderScheduler.cancel(context)
                            reminderAt = null
                            Toast.makeText(context, reminderCancelled, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reminderDialogOpen = false }) {
                    Text(stringResource(R.string.reminder_dialog_close), color = Organic.accent700)
                }
            },
        )
    }

    if (confirmDeleteOpen) {
        AlertDialog(
            containerColor = Organic.neutral100,
            onDismissRequest = { confirmDeleteOpen = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_all_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Organic.text,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_all_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Organic.neutral800,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteOpen = false
                    onDeleteEverything()
                }) {
                    Text(stringResource(R.string.settings_delete_all_confirm), color = Organic.accent700)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteOpen = false }) {
                    Text(stringResource(R.string.settings_delete_all_cancel), color = Organic.neutral700)
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
        color = Organic.text,
        modifier = Modifier.padding(top = top, bottom = 9.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Organic.neutral100)
            .border(1.5.dp, Organic.neutral300, CardShape),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Organic.neutral200),
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Organic.accent700,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Organic.text,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Organic.neutral700,
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Organic.neutral500,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ReminderOption(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) Organic.accent700 else Organic.text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

private fun timeLabel(hour: Int): String = String.format(Locale.ITALIAN, "%02d:00", hour)

private fun computeReminderMillis(hour: Int, forceTomorrow: Boolean): Long {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (forceTomorrow) add(Calendar.DAY_OF_YEAR, 1)
    }
    if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
    return calendar.timeInMillis
}

private fun formatReminderTime(millis: Long): String =
    SimpleDateFormat("EEEE d MMMM 'alle' HH:mm", Locale.ITALIAN).format(Date(millis))

/** Il peso delle foto, in unità che si leggono a colpo d'occhio. */
private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(Locale.ITALIAN, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> String.format(Locale.ITALIAN, "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1024L -> String.format(Locale.ITALIAN, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
