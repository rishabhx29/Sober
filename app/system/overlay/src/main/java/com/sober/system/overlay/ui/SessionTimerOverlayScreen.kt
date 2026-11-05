package com.sober.system.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberFilterChips
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.theme.SoberTheme

/**
 * The length picker shown before a session-timer app opens.
 *
 * It is a deliberate decision, not a delay: nothing here counts down, so the
 * only way past it is to name a length or go home. The window is not focusable
 * (see [SessionTimerOverlayController]), so back cannot dismiss it either.
 */
@Composable
fun SessionTimerOverlayScreen(
    appLabel: String,
    durations: List<Int>,
    defaultMinutes: Int,
    restMinutes: Int,
    onStart: (Int) -> Unit,
    onClose: () -> Unit
) {
    val colors = SoberTheme.colors
    var selected by remember { mutableIntStateOf(defaultMinutes) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SoberSectionLabel(text = "Session timer")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = appLabel,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "How long do you need?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When the session ends, $appLabel closes for " +
                    "$restMinutes min before it asks again.",
                fontSize = 14.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))
            SoberFilterChips(
                options = durations,
                selected = selected,
                onSelect = { selected = it },
                label = { "$it min" }
            )

            Spacer(modifier = Modifier.height(28.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SoberButton(
                        text = "Start $selected min session",
                        onClick = { onStart(selected) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SoberOutlinedButton(
                        text = "Not now — go home",
                        onClick = onClose
                    )
                }
            }
        }
    }
}
