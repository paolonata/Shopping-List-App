package com.paolonata.shoppinglist.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.ui.theme.BottomSheetOverlay
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.FilledPillButton
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OrganicChip
import com.paolonata.shoppinglist.ui.theme.OrganicSwitch
import com.paolonata.shoppinglist.ui.theme.PillShape

/**
 * Il foglio "Nuovo scontrino": la foto e, se se ne ha voglia, i quattro
 * campi che poi rendono l'archivio utile. Nessuno è obbligatorio — quello
 * che manca lo prova a leggere il riconoscimento del testo dalla foto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewReceiptSheet(
    categories: List<ReceiptCategoryEntity>,
    pendingPhotos: List<Uri>,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickPdf: () -> Unit,
    onSave: (title: String, amount: Double?, categoryId: String, returnDays: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(ReceiptCategoryEntity.FALLBACK_ID) }
    var resoOn by remember { mutableStateOf(false) }
    var resoDays by remember { mutableStateOf(14) }

    BottomSheetOverlay(onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.new_receipt_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Organic.text,
            )
            Text(
                text = stringResource(R.string.new_receipt_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Organic.neutral700,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoButton(
                    icon = Icons.Default.PhotoCamera,
                    label = stringResource(R.string.new_receipt_camera),
                    accent = true,
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f),
                )
                PhotoButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(R.string.new_receipt_gallery),
                    accent = false,
                    onClick = onPickPhoto,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(PillShape)
                    .clickable(onClick = onPickPdf)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Organic.accent700,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.new_receipt_pdf),
                    style = MaterialTheme.typography.labelMedium,
                    color = Organic.accent700,
                )
            }

            if (pendingPhotos.isNotEmpty()) {
                Text(
                    text = if (pendingPhotos.size == 1) {
                        stringResource(R.string.new_receipt_photo_one)
                    } else {
                        stringResource(R.string.new_receipt_photo_many, pendingPhotos.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Organic.accent2800,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(PillShape)
                        .background(Organic.accent2200)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            FieldLabel(stringResource(R.string.new_receipt_amount), top = 16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, PillShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "€",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                    color = Organic.neutral500,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (amountText.isEmpty()) {
                        Text(
                            text = "0,00",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                            color = Organic.neutral500,
                        )
                    }
                    BasicTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        textStyle = LocalTextStyle.current.merge(
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                color = Organic.text,
                            ),
                        ),
                        cursorBrush = SolidColor(Organic.accent600),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            FieldLabel(stringResource(R.string.new_receipt_where), top = 14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, PillShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.new_receipt_where_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Organic.neutral600,
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = Organic.text),
                    ),
                    cursorBrush = SolidColor(Organic.accent600),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FieldLabel(stringResource(R.string.new_receipt_category), top = 14.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                categories.forEach { c ->
                    OrganicChip(
                        label = "${c.emoji} ${c.label}",
                        active = categoryId == c.id,
                        onClick = { categoryId = c.id },
                        activeColor = Organic.accent600,
                        activeContent = Organic.onAccent,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, CardShape)
                    .clickable { resoOn = !resoOn }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.new_receipt_return_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Organic.text,
                    )
                    Text(
                        text = stringResource(R.string.new_receipt_return_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Organic.neutral700,
                    )
                }
                OrganicSwitch(checked = resoOn, onToggle = { resoOn = !resoOn })
            }

            if (resoOn) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    listOf(8, 14, 30, 60).forEach { days ->
                        val active = resoDays == days
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(PillShape)
                                .background(if (active) Organic.accent2600 else Organic.neutral100)
                                .border(
                                    1.5.dp,
                                    if (active) Organic.accent2600 else Organic.neutral300,
                                    PillShape,
                                )
                                .clickable { resoDays = days }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.new_receipt_days, days),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Organic.onAccent2 else Organic.neutral800,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FilledPillButton(
                text = stringResource(R.string.new_receipt_save),
                onClick = {
                    val amount = amountText
                        .replace(',', '.')
                        .filter { it.isDigit() || it == '.' }
                        .toDoubleOrNull()
                    onSave(title.trim(), amount, categoryId, if (resoOn) resoDays else null)
                },
                icon = Icons.Default.Check,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String, top: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Organic.neutral700,
        modifier = Modifier.padding(top = top, bottom = 6.dp),
    )
}

/** I due riquadri tratteggiati da cui arriva la foto. */
@Composable
private fun PhotoButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .clip(CardShape)
            .background(if (accent) Organic.accent100 else Organic.neutral100)
            .border(
                1.5.dp,
                if (accent) Organic.accent400 else Organic.neutral400,
                CardShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) Organic.accent800 else Organic.neutral800,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (accent) Organic.accent800 else Organic.neutral800,
        )
    }
}
