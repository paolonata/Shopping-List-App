package com.paolonata.shoppinglist.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.data.PlaceSearch
import com.paolonata.shoppinglist.receipts.Place
import kotlinx.coroutines.launch

/**
 * Dove hai comprato: si cerca per nome, oppure si chiede al telefono.
 *
 * Le due strade sono l'una il complemento dell'altra. «Sono qui» è quella
 * buona sul momento — quando fotografi uno scontrino sei appena uscito
 * dal negozio — mentre la ricerca serve dopo, quando svuoti il portafogli
 * a casa e devi ricordarti dove eri.
 */
@Composable
fun PlacePickerDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onPick: (Place) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun runSearch() {
        keyboard?.hide()
        if (query.isBlank()) return
        searching = true
        scope.launch {
            // Se il permesso c'è già, i risultati vicini vengono prima; se
            // non c'è, non lo si chiede per una ricerca scritta a mano.
            val near = PlaceSearch.lastKnownLocation(context)
            results = PlaceSearch.search(query, near)
            searching = false
            searched = true
        }
    }

    fun useCurrentPosition() {
        val location = PlaceSearch.lastKnownLocation(context) ?: run {
            searched = true
            results = emptyList()
            return
        }
        searching = true
        scope.launch {
            results = listOfNotNull(PlaceSearch.whereAmI(location))
            searching = false
            searched = true
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) useCurrentPosition() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.place_title)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.place_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    RoundIcon(
                        icon = Icons.Default.Search,
                        description = stringResource(R.string.place_search),
                        onClick = { runSearch() },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoundIcon(
                        icon = Icons.Default.MyLocation,
                        description = stringResource(R.string.place_here),
                        onClick = {
                            if (PlaceSearch.hasLocationPermission(context)) {
                                useCurrentPosition()
                            } else {
                                locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    searching -> Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    results.isEmpty() -> Text(
                        text = if (searched) {
                            stringResource(R.string.place_nothing)
                        } else {
                            stringResource(R.string.place_intro)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )

                    else -> LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(results) { place ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(place) }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (place.address.isNotBlank()) {
                                    Text(
                                        text = place.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(19.dp),
        )
    }
}
