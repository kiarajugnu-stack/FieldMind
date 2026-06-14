package fieldmind.research.app.features.field.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fieldmind.research.app.features.field.presentation.components.FieldMindSnackbarProvider
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.screens.*
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * FieldMind destinations. Four primary lifecycle tabs (Today → Capture → Workspace → Library)
 * are surfaced in the navigation bar/rail; the remaining destinations are reached from within
 * those tabs, the capture FAB, or the overflow.
 */
sealed class FieldMindScreen(val route: String, val label: String, val icon: MaterialSymbolIcon) {
    data object Home : FieldMindScreen("field_today", "Today", FieldMindIcons.Today)
    data object Observe : FieldMindScreen("field_capture", "Capture", FieldMindIcons.Capture)
    data object Projects : FieldMindScreen("field_projects", "Workspace", FieldMindIcons.Projects)
    data object Library : FieldMindScreen("field_library", "Library", FieldMindIcons.Library)
    data object Insights : FieldMindScreen("field_insights", "Insights", FieldMindIcons.Insights)
    data object MapScreen : FieldMindScreen("field_map", "Map", FieldMindIcons.Map)
    data object ExportStudio : FieldMindScreen("field_export_studio", "Export", FieldMindIcons.Export)

    data object Learn : FieldMindScreen("field_learn", "Learn", FieldMindIcons.School)
    data object FieldMode : FieldMindScreen("field_mode", "Field Mode", FieldMindIcons.Bolt)
    data object Questions : FieldMindScreen("field_questions", "Questions", FieldMindIcons.Question)
    data object Hypotheses : FieldMindScreen("field_hypotheses", "Hypotheses", FieldMindIcons.Hypothesis)
    data object DataTools : FieldMindScreen("field_data_tools", "Data", FieldMindIcons.Data)
    data object Analysis : FieldMindScreen("field_analysis", "Analysis", FieldMindIcons.Trend)
    data object Reports : FieldMindScreen("field_reports", "Reports", FieldMindIcons.Report)
    data object Search : FieldMindScreen("field_search", "Search", FieldMindIcons.Search)
    data object Changelog : FieldMindScreen("field_changelog", "What's new", FieldMindIcons.Info)
    data object Progress : FieldMindScreen("field_progress", "Progress", FieldMindIcons.Check)
    data object Flashcards : FieldMindScreen("field_flashcards_session", "Review", FieldMindIcons.Flashcard)
    data object ResearchSession : FieldMindScreen("field_research_session", "Session", FieldMindIcons.Bolt)
    data object Reader : FieldMindScreen("field_reader", "Reader", FieldMindIcons.Book)
    data object Settings : FieldMindScreen("field_settings", "Settings", FieldMindIcons.Settings)
    data object SettingsProfile : FieldMindScreen("field_settings_profile", "Profile", FieldMindIcons.Nature)
    data object SettingsAppearance : FieldMindScreen("field_settings_appearance", "Appearance", FieldMindIcons.Palette)
    data object SettingsCapture : FieldMindScreen("field_settings_capture", "Capture", FieldMindIcons.Capture)
    data object SettingsAi : FieldMindScreen("field_settings_ai", "AI Assistant", FieldMindIcons.Sparkle)
    data object SettingsLocalModel : FieldMindScreen("field_settings_local_model", "Local Model", FieldMindIcons.Download)
    data object SettingsBackup : FieldMindScreen("field_settings_backup", "Backup & Import", FieldMindIcons.Archive)
    data object SettingsSecurity : FieldMindScreen("field_settings_security", "Security", FieldMindIcons.Lock)
    data object SettingsAbout : FieldMindScreen("field_settings_about", "About", FieldMindIcons.Info)
}

private const val NavTransitionDurationMillis = 180

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
    var appUnlocked by remember { mutableStateOf(!viewModel.fieldSettings.privacyLockEnabled.value) }
    val privacyEnabled by viewModel.fieldSettings.privacyLockEnabled.collectAsState()
    LaunchedEffect(privacyEnabled) { if (!privacyEnabled) appUnlocked = true }
    if (!onboardingCompleted) {
        FieldMindOnboardingScreen(onFinish = { appSettings.setOnboardingCompleted(true) })        } else {
            FieldMindAppLock(
                settings = viewModel.fieldSettings,
                isUnlocked = appUnlocked,
                onUnlock = { appUnlocked = true }
            ) {
                FieldMindSnackbarProvider { _ ->
                    FieldMindNavigation(viewModel = viewModel, onResetOnboarding = { appSettings.setOnboardingCompleted(false); appUnlocked = false })
                }
            }
        }
}

/**
 * Switch primary tabs deterministically: a tab tap always lands on that tab's root screen.
 * We intentionally avoid saveState/restoreState because restoring a tab's saved nested back
 * stack could re-open a previously-visited detail page instead of the tab root (the reported
 * "taps open a different page" bug).
 */
private fun NavHostController.navigateToTab(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
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
    val haptics = rememberFieldMindHaptics()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val activeResearchSession = researchSessions.firstOrNull { it.status == "Active" }
    val hideChrome = currentRoute == FieldMindScreen.Settings.route ||
        currentRoute == FieldMindScreen.FieldMode.route ||
        currentRoute == FieldMindScreen.Reader.route ||
        currentRoute == FieldMindScreen.Changelog.route ||
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
                                onClick = { haptics.light(); navController.navigateToDestination(if (activeResearchSession != null) FieldMindScreen.ResearchSession.route else FieldMindScreen.Observe.route) },
                                containerColor = if (activeResearchSession != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = if (activeResearchSession != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            ) { Icon(icon = if (activeResearchSession != null) FieldMindIcons.Timer else FieldMindIcons.Bolt, contentDescription = "Capture", size = 26.dp) }
                        }
                    ) {
                        bottomTabs.forEach { screen ->
                            val selected = isSelected(screen)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { haptics.light(); navController.navigateToTab(screen.route) },
                                icon = { AnimatedNavIcon(screen, selected) },
                                label = { AnimatedNavLabel(screen.label, selected) }
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
                            onClick = { haptics.light(); navController.navigateToDestination(if (activeResearchSession != null) FieldMindScreen.ResearchSession.route else FieldMindScreen.Observe.route) },
                            containerColor = if (activeResearchSession != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = if (activeResearchSession != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                            icon = { Icon(icon = if (activeResearchSession != null) FieldMindIcons.Timer else FieldMindIcons.Bolt, contentDescription = null, size = 22.dp) },
                            text = { Text(activeResearchSession?.let { "Live session • ${it.observationCount}" } ?: "Capture") }
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
                                    onClick = { haptics.light(); navController.navigateToTab(screen.route) },
                                    icon = { AnimatedNavIcon(screen, selected) },
                                    label = { AnimatedNavLabel(screen.label, selected) }
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
private fun AnimatedNavIcon(screen: FieldMindScreen, selected: Boolean) {
    val scale by animateFloatAsState(if (selected) 1.22f else 0.98f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "navIconScale")
    val lift by animateFloatAsState(if (selected) -3.5f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "navIconLift")
    val alpha by animateFloatAsState(if (selected) 1f else 0.72f, tween(180), label = "navIconAlpha")
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(180),
        label = "navIconTint"
    )
    Icon(
        icon = if (selected) screen.icon.filled() else screen.icon,
        contentDescription = screen.label,
        tint = tint,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationY = lift; this.alpha = alpha },
        size = if (selected) 32.dp else 28.dp,
        weight = if (selected) 650 else screen.icon.defaultWeight
    )
}

@Composable
private fun AnimatedNavLabel(label: String, selected: Boolean) {
    val scale by animateFloatAsState(if (selected) 1.04f else 0.98f, tween(220), label = "navLabelScale")
    val alpha by animateFloatAsState(if (selected) 1f else 0.76f, tween(180), label = "navLabelAlpha")
    Text(label, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha })
}

private fun primaryTabDirection(fromRoute: String?, toRoute: String?): Int {
    val fromIndex = bottomTabs.indexOfFirst { it.route == fromRoute }
    val toIndex = bottomTabs.indexOfFirst { it.route == toRoute }
    if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return 0
    return if (toIndex > fromIndex) 1 else -1
}

@Composable
private fun FieldMindNavHost(
    navController: NavHostController,
    viewModel: FieldMindViewModel,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var readerTarget by remember { mutableStateOf("" to "") }
    val openDetail: (String, Long) -> Unit = { kind, id -> navController.navigateToDestination("field_detail/$kind/$id") }
    val openReader: (String, String) -> Unit = { url, title ->
        readerTarget = url to title
        navController.navigateToDestination(FieldMindScreen.Reader.route)
    }

    NavHost(
        navController = navController,
        startDestination = FieldMindScreen.Home.route,
        modifier = modifier,
        enterTransition = {
            val direction = primaryTabDirection(initialState.destination.route, targetState.destination.route)
            if (direction == 0) fadeIn(tween(NavTransitionDurationMillis)) + scaleIn(initialScale = 0.985f, animationSpec = tween(NavTransitionDurationMillis))
            else slideInHorizontally(tween(NavTransitionDurationMillis)) { direction * it / 4 } + fadeIn(tween(NavTransitionDurationMillis))
        },
        exitTransition = {
            val direction = primaryTabDirection(initialState.destination.route, targetState.destination.route)
            if (direction == 0) fadeOut(tween(NavTransitionDurationMillis))
            else slideOutHorizontally(tween(NavTransitionDurationMillis)) { -direction * it / 5 } + fadeOut(tween(NavTransitionDurationMillis))
        },
        popEnterTransition = { fadeIn(tween(NavTransitionDurationMillis)) + scaleIn(initialScale = 0.985f, animationSpec = tween(NavTransitionDurationMillis)) },
        popExitTransition = { fadeOut(tween(NavTransitionDurationMillis)) + scaleOut(targetScale = 0.985f, animationSpec = tween(NavTransitionDurationMillis)) }
    ) {
        composable(FieldMindScreen.Home.route) { HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Observe.route) { ObserveScreen(viewModel = viewModel, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Projects.route) { ProjectsScreen(viewModel = viewModel, onOpenDetail = openDetail, onStartSession = { navController.navigateToDestination(FieldMindScreen.ResearchSession.route) }) }
        composable(FieldMindScreen.Library.route) { KnowledgeLibraryScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Insights.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Learn.route) { KnowledgeLibraryScreen(viewModel = viewModel, startTab = 3, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Reader.route) { LearnReaderScreen(url = readerTarget.first, title = readerTarget.second, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.FieldMode.route) { ObserveScreen(viewModel = viewModel, compactFieldMode = true, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Questions.route) { QuestionsScreen(viewModel = viewModel, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Hypotheses.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
        composable(FieldMindScreen.DataTools.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Analysis.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Reports.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Search.route) { ArchiveScreen(viewModel = viewModel, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.MapScreen.route) { MapFieldScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.ExportStudio.route) { BackupExportScreen(viewModel = viewModel) }
        composable(FieldMindScreen.Changelog.route) { FieldMindChangelogScreen(onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.Progress.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Flashcards.route) { FlashcardSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.ResearchSession.route) { ResearchSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Settings.route) {
            FieldMindSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onResetOnboarding = onResetOnboarding,
                onOpenExport = { navController.navigateToDestination(FieldMindScreen.ExportStudio.route) },
                onOpenAbout = { navController.navigateToDestination(FieldMindScreen.SettingsAbout.route) },
                onOpenProfile = { navController.navigateToDestination(FieldMindScreen.SettingsProfile.route) },
                onOpenAppearance = { navController.navigateToDestination(FieldMindScreen.SettingsAppearance.route) },
                onOpenCapture = { navController.navigateToDestination(FieldMindScreen.SettingsCapture.route) },
                onOpenAi = { navController.navigateToDestination(FieldMindScreen.SettingsAi.route) },
                onOpenLocalModel = { navController.navigateToDestination(FieldMindScreen.SettingsLocalModel.route) },
                onOpenBackup = { navController.navigateToDestination(FieldMindScreen.SettingsBackup.route) },
                onOpenSecurity = { navController.navigateToDestination(FieldMindScreen.SettingsSecurity.route) },
                onOpenChangelog = { navController.navigateToDestination(FieldMindScreen.Changelog.route) }
            )
        }
        composable(FieldMindScreen.SettingsProfile.route) { ProfileSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsAppearance.route) { AppearanceSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsCapture.route) { CaptureDefaultsSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsAi.route) { AiAssistantSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsLocalModel.route) { LocalModelSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsBackup.route) { BackupImportSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenExport = { navController.navigateToDestination(FieldMindScreen.ExportStudio.route) }) }
        composable(FieldMindScreen.SettingsSecurity.route) { SecuritySettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsAbout.route) { AboutPage(onBack = { navController.popBackStack() }, onOpenChangelog = { navController.navigateToDestination(FieldMindScreen.Changelog.route) }) }
        composable("field_detail/{kind}/{id}") { entry ->
            val kind = entry.arguments?.getString("kind") ?: "observation"
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            DetailScreen(kind = kind, id = id, viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail, onOpenReader = openReader)
        }
    }
}
