package com.sober.feature.settings.health

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sober.core.common.battery.BatteryOptimizationUtils
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.common.runtime.ServiceLiveness
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberCard
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberQuietButton
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberStatusRow
import com.sober.core.ui.components.SoberTextLink
import com.sober.core.ui.theme.SoberTheme
import com.sober.feature.settings.admin.SoberDeviceAdminReceiver
import kotlinx.coroutines.delay

/**
 * One place to answer "is Sober actually working on this phone?".
 *
 * The permission list in Settings reports what the system says. That is not
 * the same as what is running: Android can keep the accessibility switch on
 * while the service is not bound, and the enforcement service can be killed
 * without any switch changing. Those two cases are the ones users report as
 * "it stopped working", so this screen reads the in-process liveness flags
 * from [ServiceLiveness] instead of trusting settings alone.
 *
 * Every value is re-read when the screen resumes, because each fix happens on
 * a system screen and the user comes straight back.
 */
@Composable
fun HealthCheckScreen(
    onBack: () -> Unit,
    onStartEngine: () -> Unit,
    onPreviewBlock: () -> Unit
) {
    val context = LocalContext.current
    val colors = SoberTheme.colors

    var refreshToken by remember { mutableIntStateOf(0) }
    var nowTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The relative age is the interesting part of the accessibility row: a
    // service that is bound but has seen nothing for an hour is usually dead
    // in a way the switch does not show. Keep it honest while the screen stays
    // open without polling anything expensive.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            nowTick = SystemClock.elapsedRealtime()
        }
    }

    val statuses = remember(refreshToken) { readStatuses(context) }
    val engineRunning = ServiceLiveness.isEngineRunning
    val accessibilityConnected = ServiceLiveness.accessibilityConnectedAtMillis > 0
    val lastEventAt = ServiceLiveness.accessibilityLastEventAtMillis.takeIf { it > 0 }
    val eventAge = lastEventAt?.let { (nowTick - it).coerceAtLeast(0) }
    val accessibilityStatus = HealthCopy.accessibilityStatus(
        enabled = statuses.accessibilityEnabled,
        connected = accessibilityConnected,
        lastEventAgeMillis = eventAge
    )

    val required = listOf(
        statuses.usageAccess,
        statuses.overlay,
        statuses.notifications,
        engineRunning
    )
    val passing = required.count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.primary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        SoberScreenHeader(
            title = "Health check",
            eyebrow = "Diagnostics",
            subtitle = "What Sober can see and block right now, beyond what the switches say."
        )

        Spacer(modifier = Modifier.height(20.dp))

        SoberCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = HealthCopy.requiredSummary(passing, required.size),
                color = if (passing == required.size) colors.primary else colors.danger,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (passing == required.size) {
                    "Blocking is possible. Section rules still need Accessibility, below."
                } else {
                    "Sober cannot block until the missing item below is fixed."
                },
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Blocking", index = "01")

        Spacer(modifier = Modifier.height(4.dp))

        StatusLine(
            label = "Usage access",
            status = if (statuses.usageAccess) "Granted" else "Not granted",
            positive = statuses.usageAccess,
            action = if (statuses.usageAccess) null else {
                { OpenLink("Open usage access") { context.startActivity(PermissionUtils.getUsageStatsIntent()) } }
            }
        )
        StatusLine(
            label = "Display over other apps",
            status = if (statuses.overlay) "Granted" else "Not granted",
            positive = statuses.overlay,
            action = if (statuses.overlay) null else {
                { OpenLink("Open overlay access") { context.startActivity(PermissionUtils.getOverlayIntent(context)) } }
            }
        )
        StatusLine(
            label = "Notifications",
            status = if (statuses.notifications) "Granted" else "Not granted",
            positive = statuses.notifications,
            action = if (statuses.notifications) null else {
                {
                    OpenLink("Open notification settings") {
                        context.startActivity(PermissionUtils.getAppNotificationSettingsIntent(context))
                    }
                }
            }
        )
        StatusLine(
            label = "Enforcement engine",
            status = if (engineRunning) "Running" else "Stopped",
            positive = engineRunning,
            action = if (engineRunning) null else {
                { SoberButton(text = "Start the engine", onClick = onStartEngine) }
            },
            trailingDivider = false
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Section blocking", index = "02")

        Spacer(modifier = Modifier.height(4.dp))

        StatusLine(
            label = "Accessibility",
            status = accessibilityStatus,
            positive = statuses.accessibilityEnabled && accessibilityConnected,
            action = {
                OpenLink("Open accessibility settings") {
                    context.startActivity(PermissionUtils.getAccessibilityIntent())
                }
            },
            trailingDivider = false
        )
        Text(
            text = when {
                statuses.accessibilityEnabled && !accessibilityConnected ->
                    "The switch is on, but Android has not connected the service. " +
                        "Turn Sober off and on in Accessibility, then come back."
                !statuses.accessibilityEnabled ->
                    "Optional. Without it Sober still enforces limits, but section " +
                        "rules (Reels, Shorts) and blocked websites cannot be read."
                else ->
                    "Section rules and blocked websites are being watched."
            },
            color = if (statuses.accessibilityEnabled && !accessibilityConnected) {
                colors.danger
            } else {
                colors.textSecondary
            },
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SoberSectionHeader(title = "Staying alive", index = "03")

        Spacer(modifier = Modifier.height(4.dp))

        StatusLine(
            label = "Battery optimisation",
            status = HealthCopy.batteryStatus(statuses.batteryExempt),
            positive = statuses.batteryExempt,
            trailingDivider = false
        )
        Text(
            text = if (statuses.batteryExempt) {
                "Android will not stop Sober to save power, so limits keep working " +
                    "after the screen turns off."
            } else {
                "Android may stop Sober to save power. If it does, your limits stop " +
                    "with it — exempt Sober from battery optimisation."
            },
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        SoberQuietButton(
            text = "Battery settings",
            icon = Icons.Rounded.OpenInNew,
            onClick = {
                context.startActivity(BatteryOptimizationUtils.getBatteryOptimizationSettingsIntent())
            }
        )
        statuses.oemBatteryIntent?.let { intent ->
            SoberQuietButton(
                text = "Open ${BatteryOptimizationUtils.manufacturerLabel() ?: "vendor"} background settings",
                icon = Icons.Rounded.OpenInNew,
                onClick = { runCatching { context.startActivity(intent) } }
            )
        }
        Text(
            text = "Some phones need a second switch (autostart or 'never sleeping apps') " +
                "before blocking survives a restart.",
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Optional", index = "04")

        Spacer(modifier = Modifier.height(4.dp))

        StatusLine(
            label = "Do Not Disturb access",
            status = if (statuses.dnd) "Granted" else "Not granted",
            positive = statuses.dnd,
            action = if (statuses.dnd) null else {
                { OpenLink("Open Do Not Disturb access") { context.startActivity(PermissionUtils.getDndAccessIntent()) } }
            }
        )
        StatusLine(
            label = "Uninstall protection",
            status = if (statuses.deviceAdmin) "Active" else "Off",
            positive = statuses.deviceAdmin,
            action = if (statuses.deviceAdmin) null else {
                {
                    OpenLink("Open device admin") {
                        context.startActivity(
                            PermissionUtils.getDeviceAdminIntent(context, SoberDeviceAdminReceiver::class.java)
                        )
                    }
                }
            },
            trailingDivider = false
        )
        Text(
            text = "Bedtime mode uses Do Not Disturb. Device admin stops the app being " +
                "uninstalled; neither is needed for limits.",
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Test", index = "05")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Preview the screen Sober puts over a blocked app. It changes no " +
                "rules and blocks nothing.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        SoberButton(
            text = "Preview the block screen",
            onClick = onPreviewBlock,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun StatusLine(
    label: String,
    status: String,
    positive: Boolean,
    action: (@Composable () -> Unit)? = null,
    trailingDivider: Boolean = true
) {
    // Actions sit under their row rather than inside it: SoberStatusRow merges
    // its children into one TalkBack stop, and a button inside that stop would
    // be announced as part of the status instead of as an action.
    SoberStatusRow(label = label, statusText = status, isPositive = positive)
    if (action != null) action()
    if (trailingDivider) SoberDivider()
}

@Composable
private fun OpenLink(text: String, onClick: () -> Unit) {
    SoberTextLink(
        text = text,
        onClick = onClick,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

private data class SystemStatuses(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val accessibilityEnabled: Boolean,
    val batteryExempt: Boolean,
    val dnd: Boolean,
    val deviceAdmin: Boolean,
    val oemBatteryIntent: Intent?
)

private fun readStatuses(context: Context): SystemStatuses = SystemStatuses(
    usageAccess = PermissionUtils.hasUsageStatsPermission(context),
    overlay = PermissionUtils.hasOverlayPermission(context),
    notifications = PermissionUtils.hasPostNotificationsPermission(context),
    accessibilityEnabled = PermissionUtils.hasAccessibilityPermission(context),
    batteryExempt = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context),
    dnd = PermissionUtils.hasDndAccess(context),
    deviceAdmin = PermissionUtils.hasDeviceAdminPermission(context, SoberDeviceAdminReceiver::class.java),
    oemBatteryIntent = BatteryOptimizationUtils.getOemBatterySettingsIntent(context)
)
