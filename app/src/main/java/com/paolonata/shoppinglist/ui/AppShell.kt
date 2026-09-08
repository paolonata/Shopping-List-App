package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.PillShape

/** Le cinque schede dell'app. */
enum class AppTab(val emoji: String, val labelRes: Int) {
    SPESA("🛒", R.string.tab_spesa),
    RECEIPTS("🧾", R.string.tab_receipts),
    DEADLINES("⏳", R.string.tab_deadlines),
    STATS("📊", R.string.tab_stats),
    SETTINGS("⚙️", R.string.tab_settings),
}

/**
 * La barra in fondo. Niente icone disegnate: le emoji dicono la stessa
 * cosa e restano leggibili anche piccolissime; quella attiva si accende
 * dentro una pastiglia colorata — terracotta per gli scontrini, salvia
 * per la spesa, che è l'altra metà dell'app.
 */
@Composable
fun AppTabBar(
    current: AppTab,
    urgentCount: Int,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Organic.neutral100)
            .padding(top = 1.5.dp)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        AppTab.entries.forEach { tab ->
            val active = tab == current
            val spesa = tab == AppTab.SPESA
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(32.dp)
                            .height(25.dp)
                            .clip(PillShape)
                            .background(
                                when {
                                    !active -> Color.Transparent
                                    spesa -> Organic.accent2200
                                    else -> Organic.accent200
                                },
                            ),
                    ) {
                        Text(text = tab.emoji, fontSize = 14.sp)
                    }
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = when {
                            !active -> Organic.neutral600
                            spesa -> Organic.accent2700
                            else -> Organic.accent700
                        },
                    )
                }
                if (tab == AppTab.DEADLINES && urgentCount > 0) {
                    Text(
                        text = urgentCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Organic.onAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 8.dp)
                            .clip(PillShape)
                            .background(Organic.accent600)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

/** Il pulsante tondo che apre il foglio del nuovo scontrino. */
@Composable
fun NewReceiptFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(60.dp)
            .shadow(12.dp, PillShape)
            .clip(PillShape)
            .background(Organic.accent600)
            .border(3.dp, Organic.bg, PillShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.fab_new_receipt_cd),
            tint = Organic.onAccent,
            modifier = Modifier.size(27.dp),
        )
    }
}
