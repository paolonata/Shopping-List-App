package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.ui.theme.BottomSheetOverlay
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.OutlinePill
import com.paolonata.shoppinglist.ui.theme.eur
import com.paolonata.shoppinglist.ui.theme.fmtDate

/**
 * Il cestino: quello che è stato eliminato resta qui per qualche giorno,
 * poi sparisce da solo. Da qui lo si rimette a posto, o lo si butta
 * davvero senza aspettare.
 */
@Composable
fun TrashSheet(
    trashed: List<ReceiptWithPhotos>,
    onRestore: (Long) -> Unit,
    onPurge: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheetOverlay(onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.trash_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Organic.text,
            )
            Text(
                text = stringResource(R.string.trash_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Organic.neutral700,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            if (trashed.isEmpty()) {
                Text(
                    text = stringResource(R.string.trash_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Organic.neutral700,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                trashed.forEach { entry ->
                    val r = entry.receipt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(Organic.neutral100)
                            .border(1.5.dp, Organic.neutral300, CardShape)
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.title.ifBlank { stringResource(R.string.receipts_untitled) },
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Organic.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = fmtDate(r.date) + (r.amount?.let { " · " + eur(it) } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = Organic.neutral700,
                            )
                        }
                        OutlinePill(
                            text = stringResource(R.string.trash_restore),
                            onClick = { onRestore(r.id) },
                        )
                        OutlinePill(
                            text = stringResource(R.string.trash_purge),
                            onClick = { onPurge(r.id) },
                            borderColor = Organic.accent400,
                            contentColor = Organic.accent700,
                        )
                    }
                }
            }
        }
    }
}
