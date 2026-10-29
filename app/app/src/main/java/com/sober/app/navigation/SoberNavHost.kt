package com.sober.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sober.app.SoberStartViewModel
import com.sober.core.ui.theme.SoberTheme
import com.sober.feature.apps.AppsScreen
import com.sober.feature.focus.FocusScreen
import com.sober.feature.home.HomeScreen
import com.sober.feature.settings.SettingsScreen
import com.sober.feature.settings.customrules.CustomRulesScreen
import com.sober.feature.settings.onboarding.OnboardingScreen
import com.sober.feature.settings.schedules.SchedulesScreen
import com.sober.feature.stats.StatsScreen
import com.sober.system.services.core.SoberCoreService

object SoberDestinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val APPS = "apps"
    const val FOCUS = "focus"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SCHEDULES = "schedules"
    const val CUSTOM_RULES = "custom_rules"
}

/**
 * Builds the graph once the persisted onboarding flag is known.
 *
 * The start destination is computed a single time (via [remember]) rather than
 * tracking the flag live: navigation graphs cannot have their start destination
 * changed after construction, and re-keying the whole NavHost on flag changes
 * would rebuild and reset the back stack mid-session.
 */
@Composable
fun SoberNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startViewModel: SoberStartViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val onboardingComplete by startViewModel.isOnboardingComplete.collectAsState(initial = null)

    // Wait for the stored flag. Rendering nothing for one frame is better than
    // flashing onboarding at a returning user.
    val resolved = onboardingComplete ?: return

    val startDestination = remember(resolved) {
        if (resolved) SoberDestinations.HOME else SoberDestinations.ONBOARDING
    }

    // The engine also starts on boot via SoberBootReceiver, but nothing started
    // it on a fresh install, so the app did nothing until the device rebooted.
    LaunchedEffect(resolved) {
        if (resolved) SoberCoreService.start(context)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SoberDestinations.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(SoberDestinations.HOME) {
                        popUpTo(SoberDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(SoberDestinations.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(SoberDestinations.SETTINGS) },
                // Same options as tapping the Focus tab, so Home's CTA and the
                // bottom bar do not produce two different Focus back stacks.
                onStartFocusSession = {
                    navController.navigate(SoberDestinations.FOCUS) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(SoberDestinations.APPS) {
            AppsScreen()
        }
        composable(SoberDestinations.FOCUS) {
            FocusScreen()
        }
        composable(SoberDestinations.STATS) {
            StatsScreen()
        }
        composable(SoberDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSchedules = { navController.navigate(SoberDestinations.SCHEDULES) },
                onOpenCustomRules = { navController.navigate(SoberDestinations.CUSTOM_RULES) }
            )
        }
        composable(SoberDestinations.SCHEDULES) {
            SchedulesScreen(onBack = { navController.popBackStack() })
        }
        composable(SoberDestinations.CUSTOM_RULES) {
            CustomRulesScreen(onBack = { navController.popBackStack() })
        }
    }
}