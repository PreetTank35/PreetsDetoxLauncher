package com.preet.detoxlauncher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.ui.block.DetoxBlockActivity
import com.preet.detoxlauncher.ui.customicon.IconCustomizationScreen
import com.preet.detoxlauncher.ui.customicon.IconCustomizationViewModel
import com.preet.detoxlauncher.ui.customicon.IconPackPickerScreen
import com.preet.detoxlauncher.ui.dashboard.InsightsDashboardScreen
import com.preet.detoxlauncher.ui.dashboard.InsightsDashboardViewModel
import com.preet.detoxlauncher.ui.detox.DetoxSettingsScreen
import com.preet.detoxlauncher.ui.detox.DetoxSettingsViewModel
import com.preet.detoxlauncher.ui.drawer.AppDrawerScreen
import com.preet.detoxlauncher.ui.drawer.AppDrawerViewModel
import com.preet.detoxlauncher.ui.focus.FocusScreen
import com.preet.detoxlauncher.ui.focus.FocusViewModel
import com.preet.detoxlauncher.ui.home.HomeScreen
import com.preet.detoxlauncher.ui.home.HomeViewModel
import com.preet.detoxlauncher.ui.onboarding.OnboardingScreen
import com.preet.detoxlauncher.ui.onboarding.OnboardingViewModel
import com.preet.detoxlauncher.ui.settings.SettingsScreen
import com.preet.detoxlauncher.ui.settings.SettingsViewModel
import com.preet.detoxlauncher.ui.theme.PreetsDetoxLauncherTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val appDrawerViewModel: AppDrawerViewModel by viewModels()
    private val insightsViewModel: InsightsDashboardViewModel by viewModels()
    private val focusViewModel: FocusViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val iconCustomizationViewModel: IconCustomizationViewModel by viewModels()
    private val detoxSettingsViewModel: DetoxSettingsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val app = application as DetoxApplication
            val settingsFlow by app.userPreferences.settingsFlow.collectAsState(
                initial = com.preet.detoxlauncher.data.preferences.LauncherSettings()
            )

            PreetsDetoxLauncherTheme(
                isPureAmoled = settingsFlow.isPureAmoled,
                accentIndex = settingsFlow.accentColorIndex,
                isLightMode = settingsFlow.isLightMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherNavigation()
                }
            }
        }
    }

    @Composable
    private fun LauncherNavigation() {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "home",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300)) }
        ) {
            // 1. Home Screen
            composable("home") {
                BackHandler(enabled = true) {
                    // Consumes back press on home so launcher never closes
                }
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenDrawer = { navController.navigate("drawer") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenAnalytics = { navController.navigate("insights") },
                    onStrictBlockRequired = { pkg, reason, resetTime ->
                        launchStrictBlock(pkg, reason, resetTime)
                    }
                )
            }

            // 2. App Drawer Screen
            composable("drawer") {
                BackHandler { navController.popBackStack() }
                AppDrawerScreen(
                    viewModel = appDrawerViewModel,
                    onNavigateToIconCustomization = { pkg ->
                        navController.navigate("custom_icon/$pkg")
                    },
                    onNavigateToDetoxRule = { pkg ->
                        navController.navigate("detox_rule/$pkg")
                    },
                    onStrictBlockRequired = { pkg, reason, resetTime ->
                        launchStrictBlock(pkg, reason, resetTime)
                    }
                )
            }

            // 3. Focus Pomodoro Screen
            composable("focus") {
                BackHandler { navController.popBackStack() }
                FocusScreen(
                    viewModel = focusViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Insights Dashboard Screen
            composable("insights") {
                BackHandler { navController.popBackStack() }
                InsightsDashboardScreen(
                    viewModel = insightsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 5. Settings Screen
            composable("settings") {
                BackHandler { navController.popBackStack() }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToIconPacks = { navController.navigate("icon_packs") }
                )
            }

            // 6. Icon Pack Picker Screen
            composable("icon_packs") {
                BackHandler { navController.popBackStack() }
                IconPackPickerScreen(
                    viewModel = iconCustomizationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 7. Individual Icon Customization Screen
            composable(
                route = "custom_icon/{packageName}",
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                BackHandler { navController.popBackStack() }
                IconCustomizationScreen(
                    packageName = pkg,
                    viewModel = iconCustomizationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 8. Detox Boundary Settings Screen
            composable(
                route = "detox_rule/{packageName}",
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                BackHandler { navController.popBackStack() }
                DetoxSettingsScreen(
                    packageName = pkg,
                    viewModel = detoxSettingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 9. Onboarding Screen
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    private fun launchStrictBlock(packageName: String, reason: String, resetTime: String) {
        val intent = Intent(this, DetoxBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DetoxBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(DetoxBlockActivity.EXTRA_REASON, reason)
            putExtra(DetoxBlockActivity.EXTRA_RESET_TIME, resetTime)
        }
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // If system home button is pressed, smoothly refresh usage metrics
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeViewModel.refreshUsageMetrics()
        }
    }
}
