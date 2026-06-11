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
import chromahub.rhythm.app.features.field.presentation.screens.*
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings

sealed class FieldMindScreen(val route: String, val label: String, val icon: String) {
    data object Home : FieldMindScreen("field_home", "Home", "⌂")
    data object Observe : FieldMindScreen("field_observe", "Observe", "+")
    data object Projects : FieldMindScreen("field_projects", "Projects", "◇")
    data object Library : FieldMindScreen("field_library", "Library", "□")
    data object Learn : FieldMindScreen("field_learn", "Learn", "△")
    data object FieldMode : FieldMindScreen("field_mode", "Field Mode", "+")
    data object Questions : FieldMindScreen("field_questions", "Questions", "?")
    data object Hypotheses : FieldMindScreen("field_hypotheses", "Hypotheses", "H")
    data object DataTools : FieldMindScreen("field_data_tools", "Data", "#")
    data object Analysis : FieldMindScreen("field_analysis", "Analysis", "≈")
    data object Reports : FieldMindScreen("field_reports", "Reports", "R")
    data object Search : FieldMindScreen("field_search", "Search", "⌕")
    data object BackupExport : FieldMindScreen("field_backup_export", "Export", "⇩")
    data object Progress : FieldMindScreen("field_progress", "Progress", "✓")
    data object Settings : FieldMindScreen("field_settings", "Settings", "⚙")
}

private val bottomTabs = listOf(
    FieldMindScreen.Home,
    FieldMindScreen.Observe,
    FieldMindScreen.Projects,
    FieldMindScreen.Library,
    FieldMindScreen.Learn
)

@Composable
fun FieldMindApp(appSettings: AppSettings, viewModel: FieldMindViewModel) {
    val onboardingCompleted by appSettings.onboardingCompleted.collectAsState()
    if (!onboardingCompleted) FieldMindOnboardingScreen(onFinish = { appSettings.setOnboardingCompleted(true) }) else FieldMindNavigation(viewModel = viewModel, onResetOnboarding = { appSettings.setOnboardingCompleted(false) })
}

@Composable
fun FieldMindNavigation(viewModel: FieldMindViewModel, onResetOnboarding: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val hideBottom = currentRoute == FieldMindScreen.Settings.route || currentRoute?.startsWith("field_detail/") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottom) {
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
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) }
        ) {
            composable(FieldMindScreen.Home.route) { HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigate(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigate(it.route) }) }
            composable(FieldMindScreen.Observe.route) { ObserveScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Projects.route) { ProjectsScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Library.route) { KnowledgeLibraryScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Learn.route) { LearnScreen(viewModel = viewModel, onNavigate = { navController.navigate(it.route) }) }
            composable(FieldMindScreen.FieldMode.route) { ObserveScreen(viewModel = viewModel, compactFieldMode = true, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Questions.route) { ProjectsScreen(viewModel = viewModel, startTab = 1, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Hypotheses.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.DataTools.route) { ProjectsScreen(viewModel = viewModel, startTab = 3, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Analysis.route) { AnalysisScreen(viewModel = viewModel) }
            composable(FieldMindScreen.Reports.route) { ProjectsScreen(viewModel = viewModel, startTab = 5, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.Search.route) { ArchiveScreen(viewModel = viewModel, onOpenDetail = { kind, id -> navController.navigate("field_detail/$kind/$id") }) }
            composable(FieldMindScreen.BackupExport.route) { BackupExportScreen(viewModel = viewModel) }
            composable(FieldMindScreen.Progress.route) { LearnScreen(viewModel = viewModel, onNavigate = { navController.navigate(it.route) }) }
            composable(FieldMindScreen.Settings.route) { FieldMindSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onResetOnboarding = onResetOnboarding) }
            composable("field_detail/{kind}/{id}") { entry ->
                val kind = entry.arguments?.getString("kind") ?: "observation"
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                DetailScreen(kind = kind, id = id, viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
