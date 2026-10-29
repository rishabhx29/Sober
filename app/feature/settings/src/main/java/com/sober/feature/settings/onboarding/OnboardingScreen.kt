package com.sober.feature.settings.onboarding

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.sober.core.common.battery.BatteryOptimizationUtils
import com.sober.core.common.permissions.PermissionUtils
import com.sober.feature.settings.admin.SoberDeviceAdminReceiver
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberTextLink
import com.sober.core.ui.theme.SoberTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 8 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set once the user rejects the system dialog. The dialog will not be shown
    // again after a permanent denial, so the page switches to a settings link.
    var notificationsDenied by remember { mutableStateOf(false) }

    // The battery exemption is not a permission, so it is read rather than
    // requested; re-reading on resume is how the page learns the user granted
    // it on the system screen.
    var batteryExempt by remember {
        mutableStateOf(BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context))
    }
    val oemBatteryIntent = remember {
        BatteryOptimizationUtils.getOemBatterySettingsIntent(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationsDenied = false
            coroutineScope.launch { pagerState.animateScrollToPage(2) }
        } else {
            notificationsDenied = true
        }
    }

    // Single exit point for onboarding, so completion is always persisted no
    // matter which page the user finished on (device admin is skippable).
    val finishOnboarding: () -> Unit = {
        coroutineScope.launch {
            viewModel.markOnboardingComplete()
            onComplete()
        }
        Unit
    }

    // Observe lifecycle to detect when user returns from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    // Check if current page permission is granted
                    when (pagerState.currentPage) {
                        1 -> if (PermissionUtils.hasPostNotificationsPermission(context)) pagerState.animateScrollToPage(2)
                        2 -> if (PermissionUtils.hasUsageStatsPermission(context)) pagerState.animateScrollToPage(3)
                        3 -> if (PermissionUtils.hasAccessibilityPermission(context)) pagerState.animateScrollToPage(4)
                        4 -> if (PermissionUtils.hasNotificationListenerPermission(context)) pagerState.animateScrollToPage(5)
                        5 -> if (PermissionUtils.hasOverlayPermission(context)) pagerState.animateScrollToPage(6)
                        6 -> {
                            batteryExempt =
                                BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
                            // Exempt is the only state worth advancing on: the
                            // vendor autostart switch cannot be read back, so a
                            // non-exempt result still lets the user press on.
                            if (batteryExempt) pagerState.animateScrollToPage(7)
                        }
                        7 -> if (PermissionUtils.hasDeviceAdminPermission(context, SoberDeviceAdminReceiver::class.java)) finishOnboarding()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> NotificationPermissionPage(
                    isDenied = notificationsDenied,
                    onRequest = {
                        if (PermissionUtils.hasPostNotificationsPermission(context)) {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        } else {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenSettings = {
                        context.startActivity(PermissionUtils.getAppNotificationSettingsIntent(context))
                    },
                    onContinue = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    }
                )
                2 -> PermissionPage(
                    title = "Usage Access",
                    description = "Sober needs to know which apps you're using to enforce limits.",
                    onGrantClick = { context.startActivity(PermissionUtils.getUsageStatsIntent()) }
                )
                3 -> PermissionPage(
                    title = "Accessibility",
                    description = "Sober reads which app is on screen so it can close the sections " +
                        "you block, like Reels or Shorts.",
                    disclosureTitle = "Before you turn on Accessibility",
                    disclosureBody = "Sober uses the Accessibility service to see which app is on " +
                        "screen and which section of it is open, so it can close what you asked " +
                        "it to block.\n\n" +
                        "It does not read what you type, does not read your messages, and does " +
                        "not store screen content. Everything stays on this device — Sober has " +
                        "no internet permission.\n\n" +
                        "You can switch Accessibility off at any time in Android Settings. " +
                        "Sober then falls back to blocking whole apps instead of sections.",
                    onGrantClick = { context.startActivity(PermissionUtils.getAccessibilityIntent()) },
                    isOptional = true,
                    onSkip = {
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    }
                )
                4 -> PermissionPage(
                    title = "Notifications",
                    description = "Sober suppresses notifications from blocked apps during Focus Sessions.",
                    onGrantClick = { context.startActivity(PermissionUtils.getNotificationListenerIntent()) }
                )
                5 -> PermissionPage(
                    title = "Display Over Other Apps",
                    description = "This allows us to show the block screen over the apps you've restricted.",
                    onGrantClick = { context.startActivity(PermissionUtils.getOverlayIntent(context)) }
                )
                6 -> BatteryOptimizationPage(
                    isExempt = batteryExempt,
                    oemLabel = BatteryOptimizationUtils.manufacturerLabel(),
                    onOpenSystemSettings = {
                        context.startActivity(
                            BatteryOptimizationUtils.getBatteryOptimizationSettingsIntent()
                        )
                    },
                    onOpenOemSettings = oemBatteryIntent?.let { intent ->
                        { runCatching { context.startActivity(intent) } }
                    },
                    onContinue = {
                        coroutineScope.launch { pagerState.animateScrollToPage(7) }
                    }
                )
                7 -> PermissionPage(
                    title = "Device Admin (Optional)",
                    description = "Prevents you from uninstalling Sober in a moment of weakness.",
                    onGrantClick = { context.startActivity(PermissionUtils.getDeviceAdminIntent(context, SoberDeviceAdminReceiver::class.java)) },
                    isOptional = true,
                    onSkip = finishOnboarding
                )
            }
        }

        Row(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) SoberTheme.colors.primary else SoberTheme.colors.textMuted
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
        
        if (pagerState.currentPage == 0) {
            SoberButton(
                text = "Let's fix that →",
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sober.",
            color = SoberTheme.colors.primary,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Take back control of your time.",
            color = SoberTheme.colors.textSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Runtime `POST_NOTIFICATIONS` step. Notifications carry the block countdown and
 * the daily-cap warning, so the ask happens before the settings-based grants:
 * this screen can be dismissed with the system dialog, whereas the later steps
 * bounce the user out to Settings.
 */
@Composable
fun NotificationPermissionPage(
    isDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Notifications",
            color = SoberTheme.colors.primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isDenied) {
                "Notifications are off. Sober can't show the block countdown or warn you " +
                    "before you hit your daily limit. You can turn them on in Settings."
            } else {
                "Sober uses notifications for the block countdown and to warn you before " +
                    "you hit your daily limit. Nothing promotional, ever."
            },
            color = if (isDenied) SoberTheme.colors.danger else SoberTheme.colors.textSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        SoberButton(
            text = if (isDenied) "Continue anyway" else "Allow Notifications",
            onClick = if (isDenied) onContinue else onRequest,
            modifier = Modifier.fillMaxWidth()
        )
        if (isDenied) {
            Spacer(modifier = Modifier.height(16.dp))
            SoberOutlinedButton(
                text = "Open notification settings",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Battery-optimisation step.
 *
 * There is no dialog to show and nothing to grant in-app: the exemption is a
 * system setting, so the page opens the right screen and reads the result.
 * "Not now" is always offered because on many phones the exemption is not what
 * decides survival — a vendor autostart switch does — and trapping a user on a
 * page whose outcome cannot be confirmed would be worse than letting them move
 * on. The vendor link is hidden when no known panel resolves, rather than
 * opening a stock settings screen that does not contain the switch it names.
 */
@Composable
fun BatteryOptimizationPage(
    isExempt: Boolean,
    oemLabel: String?,
    onOpenSystemSettings: () -> Unit,
    onOpenOemSettings: (() -> Unit)?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Keep Sober Running",
            color = SoberTheme.colors.primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isExempt) {
                "Sober is already allowed to run in the background, so your limits survive " +
                    "the screen turning off."
            } else {
                "Phones stop background apps to save power. If that happens to Sober, your " +
                    "limits quietly stop working. Allow it to run in the background so " +
                    "blocking carries on."
            },
            color = SoberTheme.colors.textSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        SoberButton(
            text = if (isExempt) "Continue" else "Allow background running",
            onClick = if (isExempt) onContinue else onOpenSystemSettings,
            modifier = Modifier.fillMaxWidth()
        )
        if (oemLabel != null && onOpenOemSettings != null) {
            Spacer(modifier = Modifier.height(16.dp))
            SoberTextLink(
                text = "Open $oemLabel battery settings",
                onClick = onOpenOemSettings
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Some phones need a second switch, like autostart or 'never sleeping " +
                    "apps', before blocking survives.",
                color = SoberTheme.colors.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        if (!isExempt) {
            Spacer(modifier = Modifier.height(16.dp))
            SoberOutlinedButton(
                text = "Not now",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A permission step. [disclosureTitle]/[disclosureBody] add a prominent
 * disclosure dialog that must be acknowledged before the system screen opens:
 * Play requires one for Accessibility, and the acknowledgement is what makes
 * the grant an explicit choice rather than a tap through.
 */
@Composable
fun PermissionPage(
    title: String,
    description: String,
    onGrantClick: () -> Unit,
    isOptional: Boolean = false,
    onSkip: () -> Unit = {},
    disclosureTitle: String? = null,
    disclosureBody: String? = null
) {
    var showDisclosure by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = SoberTheme.colors.primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            color = SoberTheme.colors.textSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        SoberButton(
            text = if (disclosureBody != null) "Review and turn on" else "Grant Permission",
            onClick = {
                if (disclosureBody != null) showDisclosure = true else onGrantClick()
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (isOptional) {
            Spacer(modifier = Modifier.height(16.dp))
            SoberOutlinedButton(
                text = "Skip for now",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDisclosure && disclosureBody != null) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            containerColor = SoberTheme.colors.surface,
            title = {
                Text(
                    text = disclosureTitle ?: title,
                    color = SoberTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = disclosureBody,
                    color = SoberTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisclosure = false
                        onGrantClick()
                    }
                ) {
                    Text(
                        text = "I understand and agree",
                        color = SoberTheme.colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text(text = "Not now", color = SoberTheme.colors.textSecondary)
                }
            }
        )
    }
}
