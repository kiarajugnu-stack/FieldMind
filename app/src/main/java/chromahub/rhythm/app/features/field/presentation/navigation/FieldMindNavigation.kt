package chromahub.rhythm.app.features.field.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import chromahub.rhythm.app.features.field.presentation.screens.ArchiveScreen
import chromahub.rhythm.app.features.field.presentation.screens.CaptureScreen
import chromahub.rhythm.app.features.field.presentation.screens.FieldMindOnboardingScreen
import chromahub.rhythm.app.features.field.presentation.screens.DetailScreen
import chromahub.rhythm.app.features.field.presentation.screens.FieldMindSettingsScreen
import chromahub.rhythm.app.features.field.presentation.screens.HomeScreen
import chromahub.rhythm.app.features.field.presentation.screens.KnowledgeLibraryScreen
import chromahub.rhythm.app.features.field.presentation.screens.ResearchScreen
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings

sealed class FieldMindScreen(val route: String, val label: String, val icon: String) {
    data object Home : FieldMindScreen("field_home", "Home", "⌂")
    data object Capture : FieldMindScreen("field_capture", "Capture", "+")
    data object Research : FieldMindScreen("field_research", "Research", "?")
    data object Library : FieldMindScreen("field_library", "Library", "□")
    data object Archive : FieldMindScreen("field_archive", "Archive", "⌕")
    data object Settings : FieldMindScreen("field_settings", "Settings", "⚙")
}

private val bottomTabs = listOf(
    FieldMindScreen.Home,
    FieldMindScreen.Capture,
    FieldMindScreen.Research,
    FieldMindScreen.Library,
    FieldMindScreen.Archive
)

@Composable
fun FieldMindApp(appSettings: AppSettings, viewModel: FieldMindViewModel) {
    val onboardingCompleted by appSettings.onboardingCompleted.collectAsState()

    if (!onboardingCompleted) {
        FieldMindOnboardingScreen(onFinish = { appSettings.setOnboardingCompleted(true) })
    } else {
        FieldMindNavigation(viewModel = viewModel, onResetOnboarding = { appSettings.setOnboardingCompleted(false) })
    }
}

@Composable
fun FieldMindNavigation(
    viewModel: FieldMindViewModel,
    onResetOnboarding: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute != FieldMindScreen.Settings.route) {
                NavigationBar {
                    bottomTabs.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(FieldMindScreen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(screen.icon, style = MaterialTheme.typography.titleMedium) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FieldMindScreen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(220)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(220)) }
        ) {
            composable(FieldMindScreen.Home.route) {
                HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigate(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigate(it.route) })
            }
            composable(FieldMindScreen.Capture.route) { CaptureScreen(viewModel = viewModel) }
            composable(FieldMindScreen.Research.route) {
                ResearchScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") })
            }
            composable(FieldMindScreen.Library.route) {
                KnowledgeLibraryScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") })
            }
            composable(FieldMindScreen.Archive.route) {
                ArchiveScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") })
            }
            composable(FieldMindScreen.Settings.route) {
                FieldMindSettingsScreen(onBack = { navController.popBackStack() }, onResetOnboarding = onResetOnboarding)
            }
            composable("field_detail/{kind}/{id}") { entry ->
                val kind = entry.arguments?.getString("kind") ?: "observation"
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                DetailScreen(kind = kind, id = id, viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
