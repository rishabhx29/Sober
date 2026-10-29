package com.sober.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Drum-scroll number picker: the centred item is the selected value.
 *
 * Used for the daily-limit picker in the app detail sheet, where a +/- stepper
 * makes setting "2h 30m" a dozen taps.
 */
@Composable
fun SoberDrumPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 36.dp,
    visibleItems: Int = 3,
    formatter: (Int) -> String = { it.toString() }
) {
    val colors = SoberTheme.colors
    val values = remember(range) { range.toList() }
    val startIndex = remember(range, value) {
        (value - range.first).coerceIn(0, values.lastIndex.coerceAtLeast(0))
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Compose has no "current item" callback, so the item under the centre
    // padding is the selection. Emitting only on change prevents a feedback
    // loop with the caller's own state.
    LaunchedEffect(listState, range) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val picked = range.first + index
                if (picked in range) currentOnValueChange(picked)
            }
    }

    val sidePadding = itemHeight * ((visibleItems - 1) / 2)

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = sidePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            items(values, key = { it }) { item ->
                val isSelected = item == value
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatter(item),
                        color = if (isSelected) colors.primary else colors.textMuted,
                        fontSize = if (isSelected) 26.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Compact 7-day usage bars: navy on cream, no axis labels, one bar per day.
 *
 * Zero-usage days keep a faint stub so the row still reads as seven days
 * rather than a gap.
 */
@Composable
fun SoberMiniBarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barColor: Color? = null
) {
    if (values.isEmpty()) return
    val colors = SoberTheme.colors
    val fill = barColor ?: colors.primary
    val peak = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        values.forEach { minutes ->
            val fraction = if (minutes <= 0) 0.06f else (minutes.toFloat() / peak).coerceIn(0.08f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .heightIn(min = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (minutes <= 0) colors.textMuted.copy(alpha = 0.25f) else fill)
                )
            }
        }
    }
}
