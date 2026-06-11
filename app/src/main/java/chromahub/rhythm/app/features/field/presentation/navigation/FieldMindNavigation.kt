package chromahub.rhythm.app.features.field.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.features.field.presentation.screens.*
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * FieldMind destinations. Five primary lifecycle tabs (Today → Capture → Projects → Library →
 * Insights) are surfaced in the navigation bar/rail; the remaining destinations are reached from
 * within those tabs, the capture FAB, or the overflow.
 */
sealed class FieldMindScreen(val route: String, val label: String, val icon: MaterialSymbolIcon) {
    data object Home : FieldMindScreen("field_today", "Today", FieldMindIcons.Today)
    data object Observe : FieldMindScreen("field_capture", "Capture", FieldMindIcons.Capture)
    data object Projects : FieldMindScreen("field_projects", "Projects", FieldMindIcons.Projects)
    data object Library : FieldMindScreen("field_library", "Library", FieldMindIcons.Library)
    data object Insights : FieldMindScreen("field_insights", "Insights", FieldMindIcons.Insights)

    data object Learn : FieldMindScreen("field_learn", "Learn", FieldMindIcons.School)
    data object FieldMode : FieldMindScreen("field_mode", "Field Mode", FieldMindIcons.Bolt)
    data object Questions : FieldMindScreen("field_questions", "Questions", FieldMindIcons.Question)
    data object Hypotheses : FieldMindScreen("field_hypotheses", "Hypotheses", FieldMindIcons.Hypothesis)
    data object DataTools : FieldMindScreen("field_data_tools", "Data", FieldMindIcons.Data)
    data object Analysis : FieldMindScreen("field_analysis", "Analysis", FieldMindIcons.Trend)
    data object Reports : FieldMindScreen("field_reports", "Reports", FieldMindIcons.Report)
    data object Search : FieldMindScreen("field_search", "Search", FieldMindIcons.Search)
    data object BackupExport : FieldMindScreen("field_backup_export", "Export", FieldMindIcons.Export)
    data object Progress : FieldMindScreen("field_progress", "Progress", FieldMindIcons.Check)
    data object Flashcards : FieldMindScreen("field_flashcards_session", "Review", FieldMindIcons.Flashcard)
    data object Settings : FieldMindScreen("field_settings", "Settings", FieldMindIcons.Settings)
}

private val bottomTabs = listOf(
    FieldMindScreen.Home,
    FieldMindScreen.Observe,
    FieldMindScreen.Projects,
    FieldMindScreen.Library,
    FieldMindScreen.Insights
)

@Composable
fun FieldMindApp(appSettings: AppSettings, viewModel: FieldMindViewModel) {
    val onboardingCompleted by appSettings.onboardingCompleted.collectAsState()
    if (!onboardingCompleted) FieldMindOnboardingScreen(onFinish = { appSettings.setOnboardingCompleted(true) }) else FieldMindNavigation(viewModel = viewModel, onResetOnboarding = { appSettings.setOnboardingCompleted(false) })
}

private fun NavHostController.navigateToTab(route: String) {
    val current = currentDestination?.route
    if (current == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Navigate to a non-tab destination, de-duplicating taps so the page always opens reliably. */
private fun NavHostController.navigateToDestination(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}

@Composable
fun FieldMindNavigation(viewModel: FieldMindViewModel, onResetOnboarding: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val hideChrome = currentRoute == FieldMindScreen.Settings.route ||
        currentRoute == FieldMindScreen.FieldMode.route ||
        currentRoute == FieldMindScreen.Flashcards.route ||
        currentRoute?.startsWith("field_detail/") == true

    fun isSelected(screen: FieldMindScreen) =
        currentDestination?.hierarchy?.any { it.route == screen.route } == true

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 720.dp
        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                if (!hideChrome) {
                    NavigationRail(
                        header = {
                            FloatingActionButton(
                                onClick = { navController.navigateToDestination(FieldMindScreen.FieldMode.route) },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) { Icon(icon = FieldMindIcons.Bolt, contentDescription = "Field mode capture", size = 26.dp) }
                        }
                    ) {
                        bottomTabs.forEach { screen ->
                            val selected = isSelected(screen)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navController.navigateToTab(screen.route) },
                                icon = { Icon(icon = if (selected) screen.icon.filled() else screen.icon, contentDescription = screen.label, size = 24.dp) },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
                FieldMindNavHost(navController, viewModel, onResetOnboarding, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                    if (!hideChrome) {
                        ExtendedFloatingActionButton(
                            onClick = { navController.navigateToDestination(FieldMindScreen.FieldMode.route) },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            icon = { Icon(icon = FieldMindIcons.Bolt, contentDescription = null, size = 22.dp) },
                            text = { Text("Capture") }
                        )
                    }
                },
                bottomBar = {
                    if (!hideChrome) {
                        NavigationBar {
                            bottomTabs.forEach { screen ->
                                val selected = isSelected(screen)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navController.navigateToTab(screen.route) },
                                    icon = { Icon(icon = if (selected) screen.icon.filled() else screen.icon, contentDescription = screen.label, size = 24.dp) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                FieldMindNavHost(navController, viewModel, onResetOnboarding, Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun FieldMindNavHost(
    navController: NavHostController,
    viewModel: FieldMindViewModel,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = FieldMindScreen.Home.route,
        modifier = modifier,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) }
    ) {
        val openDetail: (String, Long) -> Unit = { kind, id -> navController.navigateToDestination("field_detail/$kind/$id") }
        composable(FieldMindScreen.Home.route) { HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Observe.route) { ObserveScreen(viewModel = viewModel, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Projects.route) { ProjectsScreen(viewModel = viewModel, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Library.route) { KnowledgeLibraryScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Insights.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Learn.route) { KnowledgeLibraryScreen(viewModel = viewModel, startTab = 3, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.FieldMode.route) { ObserveScreen(viewModel = viewModel, compactFieldMode = true, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Questions.route) { ProjectsScreen(viewModel = viewModel, startTab = 1, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Hypotheses.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
        composable(FieldMindScreen.DataTools.route) { ProjectsScreen(viewModel = viewModel, startTab = 3, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Analysis.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Reports.route) { ProjectsScreen(viewModel = viewModel, startTab = 4, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Search.route) { ArchiveScreen(viewModel = viewModel, onOpenDetail = openDetail) }
        composable(FieldMindScreen.BackupExport.route) { BackupExportScreen(viewModel = viewModel) }
        composable(FieldMindScreen.Progress.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Flashcards.route) { FlashcardSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.Settings.route) { FieldMindSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onResetOnboarding = onResetOnboarding) }
        composable("field_detail/{kind}/{id}") { entry ->
            val kind = entry.arguments?.getString("kind") ?: "observation"
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            DetailScreen(kind = kind, id = id, viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail)
        }
    }
}
