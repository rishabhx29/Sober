package com.sober.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
            BottomNavTab(SoberDestinations.STATS, "Stats", Icons.Rounded.BarChart),
            BottomNavTab(SoberDestinations.SETTINGS, "Settings", Icons.Rounded.Settings)
        )

        Scaffold(
            containerColor = SoberTheme.colors.background,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route

                val showBottomBar = currentRoute in tabs.map { it.route }

                if (showBottomBar) {
                    // A floating capsule rather than an edge-to-edge bar: it
                    // lifts the tabs off the page, keeps the content's bottom
                    // edge visible and reads as one control instead of five
                    // separate icons.
                    val colors = SoberTheme.colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            shadowElevation = 10.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy
                                        ?.any { it.route == tab.route } == true
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(vertical = 7.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (selected) colors.primary else Color.Transparent
                                            )
                                            .selectable(
                                                selected = selected,
                                                role = Role.Tab,
                                                onClick = {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(
                                                            navController.graph
                                                                .findStartDestination().id
                                                        ) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            ),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            // The label names the tab; a
                                            // description would say it twice.
                                            contentDescription = null,
                                            tint = if (selected) colors.onPrimary else colors.textMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (selected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Medium
                                            },
                                            color = if (selected) colors.onPrimary else colors.textMuted
                                        )
                                    }
                                }
                            }
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
