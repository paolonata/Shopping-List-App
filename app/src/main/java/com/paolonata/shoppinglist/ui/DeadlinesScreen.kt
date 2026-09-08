package com.paolonata.shoppinglist.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paolonata.shoppinglist.R
import com.paolonata.shoppinglist.receipts.DeadlineKind
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.PillShape
import com.paolonata.shoppinglist.ui.theme.SectionRule
import com.paolonata.shoppinglist.ui.theme.SegmentedTabs
import com.paolonata.shoppinglist.ui.theme.Tone
import com.paolonata.shoppinglist.ui.theme.TonePill
import com.paolonata.shoppinglist.ui.theme.eur
import com.paolonata.shoppinglist.ui.theme.fmtDate
import com.paolonata.shoppinglist.ui.theme.statusOf

/**
 * Fino a quando si è in tempo. Un reso già fatto smette di comparire: se
 * restasse continuerebbe a chiamare per nulla.
 */
@Composable
fun DeadlinesScreen(
    deadlines: List<UpcomingDeadline>,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(0) }

    val visible = when (filter) {
        1 -> deadlines.filter { it.kind == DeadlineKind.RETURN }
        2 -> deadlines.filter { it.kind == DeadlineKind.WARRANTY }
        else -> deadlines
    }
    val urgent = deadlines.count { it.status.days in 0..7 }
    val valid = deadlines.count { it.status.days >= 0 }

    val sections = listOf<Triple<String, (Long) -> Boolean, Int>>(
        Triple("recent", { d: Long -> d < 0 && d >= -60 }, R.string.deadlines_section_recent),
        Triple("today", { d: Long -> d in 0..1 }, R.string.deadlines_section_today),
        Triple("week", { d: Long -> d in 2..7 }, R.string.deadlines_section_week),
        Triple("month", { d: Long -> d in 8..30 }, R.string.deadlines_section_month),
        Triple("later", { d: Long -> d > 30 }, R.string.deadlines_section_later),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 108.dp),
    ) {
        item("title") {
            Text(
                text = stringResource(R.string.deadlines_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Organic.text,
            )
            Text(
                text = stringResource(R.string.deadlines_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Organic.neutral700,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
        }

        item("tabs") {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.deadlines_tab_all),
                    stringResource(R.string.deadlines_tab_returns),
                    stringResource(R.string.deadlines_tab_warranty),
                ),
                selectedIndex = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        item("counters") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                CounterCard(
                    value = urgent.toString(),
                    label = stringResource(R.string.deadlines_counter_urgent),
                    modifier = Modifier.weight(1f),
                )
                CounterCard(
                    value = valid.toString(),
                    label = stringResource(R.string.deadlines_counter_valid),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item("note") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(if (urgent > 0) Organic.accent200 else Organic.accent2200)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Text(text = if (urgent > 0) "⏳" else "✓", fontSize = 15.sp)
                Column {
                    Text(
                        text = if (urgent > 0) {
                            stringResource(R.string.deadlines_note_urgent_title)
                        } else {
                            stringResource(R.string.deadlines_note_calm_title)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (urgent > 0) Organic.accent800 else Organic.accent2800,
                    )
                    Text(
                        text = when {
                            urgent == 1 -> stringResource(R.string.deadlines_note_urgent_one)
                            urgent > 1 -> stringResource(R.string.deadlines_note_urgent_many, urgent)
                            else -> stringResource(R.string.deadlines_note_calm_text)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (urgent > 0) Organic.accent800 else Organic.accent2800,
                    )
                }
            }
        }

        sections.forEach { (key, test, titleRes) ->
            val entries = visible.filter { test(it.status.days) }
            if (entries.isNotEmpty()) {
                item("h-$key") {
                    SectionRule(
                        title = stringResource(titleRes),
                        trailing = entries.size.toString(),
                        titleStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                        modifier = Modifier.padding(top = 20.dp, bottom = 9.dp),
                    )
                }
                items(entries.size, key = { "$key-$it" }) { index ->
                    DeadlineRow(
                        deadline = entries[index],
                        onClick = { onOpen(entries[index].entry.receipt.id) },
                        modifier = Modifier.padding(bottom = 9.dp),
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item("empty") {
                Text(
                    text = stringResource(R.string.deadlines_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Organic.neutral700,
                    modifier = Modifier.padding(top = 30.dp),
                )
            }
        }
    }
}

@Composable
private fun CounterCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Organic.neutral100)
            .border(1.5.dp, Organic.neutral300, CardShape)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 27.sp),
            color = Organic.text,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Organic.neutral700,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun DeadlineRow(
    deadline: UpcomingDeadline,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = deadline.entry.receipt
    val tone = statusOf(deadline.status.days)?.first ?: Tone.PLAIN
    val untitled = stringResource(R.string.receipts_untitled)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Organic.neutral100)
            .border(1.5.dp, Organic.neutral300, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Organic.accent2200),
        ) {
            Text(text = deadline.kind.emoji, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = r.title.ifBlank { untitled },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Organic.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val date = if (deadline.kind == DeadlineKind.RETURN) r.returnUntil else r.warrantyUntil
            val preposition = if (deadline.kind == DeadlineKind.RETURN) {
                stringResource(R.string.deadlines_prep_return)
            } else {
                stringResource(R.string.deadlines_prep_warranty)
            }
            val amountSuffix = r.amount?.let { " · " + eur(it) } ?: ""
            Text(
                text = "${deadline.kind.label} $preposition ${fmtDate(date)}$amountSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = Organic.neutral700,
            )
        }
        TonePill(text = deadline.status.text, tone = tone)
    }
}
