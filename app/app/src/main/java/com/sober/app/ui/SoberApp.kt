package com.sober.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sober.app.navigation.SoberDestinations
import com.sober.app.navigation.SoberNavHost
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.ui.theme.SoberTheme

data class BottomNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun SoberApp(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val themeMode by themeViewModel.mode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SoberTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()

        val tabs = listOf(
            BottomNavTab(SoberDestinations.HOME, "Home", Icons.Rounded.Home),
            BottomNavTab(SoberDestinations.APPS, "Apps", Icons.Rounded.Apps),
            BottomNavTab(SoberDestinations.FOCUS, "Focus", Icons.Rounded.Shield),
            BottomNavTab(SoberDestinations.STATS, "Stats", Icons.Rounded.BarChart)
        )

        Scaffold(
            containerColor = SoberTheme.colors.background,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route

                val showBottomBar = currentRoute in tabs.map { it.route }

                if (showBottomBar) {
                    NavigationBar(
                        containerColor = SoberTheme.colors.surface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SoberTheme.colors.primary,
                                    selectedTextColor = SoberTheme.colors.primary,
                                    unselectedIconColor = SoberTheme.colors.textMuted,
                                    unselectedTextColor = SoberTheme.colors.textMuted,
                                    indicatorColor = SoberTheme.colors.background
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            SoberNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
