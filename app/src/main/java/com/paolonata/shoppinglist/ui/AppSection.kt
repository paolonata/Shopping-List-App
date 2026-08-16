package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R

/** Le due metà dell'app. */
enum class AppSection { LIST, RECEIPTS }

/**
 * Il passaggio da una sezione all'altra, al posto del titolo.
 *
 * Non una barra di navigazione in fondo: qui il colore non esiste e le
 * schermate sono due, quindi bastano due parole in cima — quella attiva
 * in nero, l'altra in grigio. Costa la riga che c'era già.
 */
@Composable
fun SectionSwitch(
    current: AppSection,
    onSelect: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(
            text = stringResource(R.string.section_list),
            selected = current == AppSection.LIST,
            onClick = { onSelect(AppSection.LIST) },
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        SectionLabel(
            text = stringResource(R.string.section_receipts),
            selected = current == AppSection.RECEIPTS,
            onClick = { onSelect(AppSection.RECEIPTS) },
        )
    }
}

@Composable
private fun SectionLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}
