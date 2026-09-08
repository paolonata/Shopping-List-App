package com.paolonata.shoppinglist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.paolonata.shoppinglist.data.ReceiptCategoryEntity
import com.paolonata.shoppinglist.data.ReceiptWithPhotos
import com.paolonata.shoppinglist.ui.theme.BigCardShape
import com.paolonata.shoppinglist.ui.theme.CardShape
import com.paolonata.shoppinglist.ui.theme.MESI_BREVI
import com.paolonata.shoppinglist.ui.theme.Organic
import com.paolonata.shoppinglist.ui.theme.PillShape
import com.paolonata.shoppinglist.ui.theme.SegmentedTabs
import com.paolonata.shoppinglist.ui.theme.eur
import com.paolonata.shoppinglist.ui.theme.money
import com.paolonata.shoppinglist.ui.theme.monthKey
import java.time.LocalDate

/** Quanto se n'è andato, e dove. Tre finestre: mese, anno, sempre. */
@Composable
fun StatsScreen(
    receipts: List<ReceiptWithPhotos>,
    categories: List<ReceiptCategoryEntity>,
    modifier: Modifier = Modifier,
) {
    var range by remember { mutableStateOf(0) }
    val today = LocalDate.now()
    val thisMonth = "%04d-%02d".format(today.year, today.monthValue)

    val inRange = receipts.filter {
        when (range) {
            0 -> monthKey(it.receipt.date) == thisMonth
            1 -> it.receipt.date.startsWith(today.year.toString())
            else -> true
        }
    }
    val amounts = inRange.mapNotNull { it.receipt.amount }
    val total = amounts.sum()
    val top = inRange.filter { it.receipt.amount != null }.maxByOrNull { it.receipt.amount!! }

    val rangeLabel = when (range) {
        0 -> stringResource(R.string.stats_range_month)
        1 -> stringResource(R.string.stats_range_year, today.year)
        else -> stringResource(R.string.stats_range_all)
    }

    // Gli ultimi dodici mesi fino a quello corrente, anche quelli vuoti:
    // un buco nel grafico dice quanto quelli pieni.
    val months12 = (11 downTo 0).map { back ->
        val date = today.withDayOfMonth(1).minusMonths(back.toLong())
        val key = "%04d-%02d".format(date.year, date.monthValue)
        Triple(
            key,
            MESI_BREVI[date.monthValue - 1],
            receipts.filter { monthKey(it.receipt.date) == key }.mapNotNull { it.receipt.amount }.sum(),
        )
    }
    val max12 = months12.maxOfOrNull { it.third }?.takeIf { it > 0 } ?: 1.0

    val perCategory = categories.map { c ->
        val list = inRange.filter { it.receipt.categoryId == c.id }
        Triple(c, list.size, list.mapNotNull { it.receipt.amount }.sum())
    }.filter { it.second > 0 && it.third > 0 }.sortedByDescending { it.third }
    val maxCategory = perCategory.maxOfOrNull { it.third }?.takeIf { it > 0 } ?: 1.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 108.dp),
    ) {
        item("title") {
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Organic.text,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        item("tabs") {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.stats_tab_month),
                    stringResource(R.string.stats_tab_year),
                    stringResource(R.string.stats_tab_all),
                ),
                selectedIndex = range,
                onSelect = { range = it },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        item("total") {
            Surface(
                shape = BigCardShape,
                color = Organic.accent2600,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "€",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                            color = Organic.onAccent2.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp),
                        )
                        Text(
                            text = money(total),
                            style = MaterialTheme.typography.displaySmall,
                            color = Organic.onAccent2,
                        )
                    }
                    Text(
                        text = if (inRange.size == 1) {
                            stringResource(R.string.stats_sub_one, rangeLabel)
                        } else {
                            stringResource(R.string.stats_sub_many, rangeLabel, inRange.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Organic.onAccent2.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(Organic.onAccent2.copy(alpha = 0.28f)),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = eur(if (amounts.isNotEmpty()) total / amounts.size else 0.0),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Organic.onAccent2,
                            )
                            Text(
                                text = stringResource(R.string.stats_average),
                                style = MaterialTheme.typography.bodySmall,
                                color = Organic.onAccent2.copy(alpha = 0.85f),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (top != null) eur(top.receipt.amount) else "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Organic.onAccent2,
                            )
                            Text(
                                text = if (top != null) {
                                    stringResource(R.string.stats_top_with, top.receipt.title)
                                } else {
                                    stringResource(R.string.stats_top)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Organic.onAccent2.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item("trend-title") {
            Text(
                text = stringResource(R.string.stats_trend_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                color = Organic.text,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
        }

        item("trend") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, CardShape)
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                ) {
                    months12.forEach { (key, label, value) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 16.dp)
                                    .fillMaxWidth()
                                    .height((4.0 + value / max12 * 90.0).toFloat().dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp,
                                        ),
                                    )
                                    .background(
                                        if (key == thisMonth) Organic.accent600 else Organic.accent2400,
                                    ),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = Organic.neutral600,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        item("cats-title") {
            Text(
                text = stringResource(R.string.stats_categories_title, rangeLabel),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                color = Organic.text,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
        }

        item("cats") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Organic.neutral100)
                    .border(1.5.dp, Organic.neutral300, CardShape)
                    .padding(horizontal = 16.dp, vertical = 17.dp),
            ) {
                if (perCategory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Organic.neutral700,
                    )
                }
                perCategory.forEachIndexed { index, (category, _, value) ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.emoji, fontSize = 13.sp)
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Organic.text,
                                modifier = Modifier
                                    .padding(start = 7.dp)
                                    .weight(1f),
                            )
                            Text(
                                text = eur(value),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Organic.neutral700,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(PillShape)
                                .background(Organic.neutral200),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((value / maxCategory).toFloat().coerceIn(0.02f, 1f))
                                    .height(9.dp)
                                    .clip(PillShape)
                                    .background(
                                        if (index == 0) Organic.accent600 else Organic.accent2500,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
