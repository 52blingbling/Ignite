package com.efa.assistant

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.efa.assistant.core.designsystem.EFATheme
import com.efa.assistant.core.designsystem.ThemeMode
import com.efa.assistant.feature.analytics.AnalyticsScreen
import com.efa.assistant.feature.focus.FocusScreen
import com.efa.assistant.feature.home.HomeScreen
import com.efa.assistant.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    @Named("encrypted_prefs")
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 初始化读取本地主题模式
            val initialModeStr = sharedPreferences.getString("theme_mode", "DARK") ?: "DARK"
            var currentThemeMode by remember {
                mutableStateOf(
                    when (initialModeStr) {
                        "LIGHT" -> ThemeMode.LIGHT
                        "AMOLED" -> ThemeMode.AMOLED
                        else -> ThemeMode.DARK
                    }
                )
            }

            EFATheme(themeMode = currentThemeMode) {
                MainAppLayout(
                    initialThemeMode = currentThemeMode,
                    onThemeChanged = { currentThemeMode = it }
                )
            }
        }
    }
}

@Composable
fun MainAppLayout(
    initialThemeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 全屏专注倒计时页面必须隐藏底部导航栏，绝对排除界面视觉分心
    val currentRoute = currentDestination?.route ?: ""
    val showBottomBar = !currentRoute.startsWith("focus")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // 今日聚焦 (Home)
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "今日聚焦") },
                        label = { Text("今日聚焦") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // 习惯透视 (Analytics)
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "习惯透视") },
                        label = { Text("习惯透视") },
                        selected = currentDestination?.hierarchy?.any { it.route == "analytics" } == true,
                        onClick = {
                            navController.navigate("analytics") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // 设置 (Settings)
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToFocus = { missionId, actionId ->
                        navController.navigate("focus/$missionId/$actionId")
                    }
                )
            }
            composable("analytics") {
                AnalyticsScreen()
            }
            composable("settings") {
                SettingsScreen(onThemeChanged = onThemeChanged)
            }
            composable(
                route = "focus/{missionId}/{actionId}",
                arguments = listOf(
                    navArgument("missionId") { type = NavType.StringType },
                    navArgument("actionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val missionId = backStackEntry.arguments?.getString("missionId") ?: ""
                val actionId = backStackEntry.arguments?.getString("actionId") ?: ""
                FocusScreen(
                    missionId = missionId,
                    actionId = actionId,
                    onBackToHome = {
                        navController.popBackStack("home", false)
                    }
                )
            }
        }
    }
}
