package fieldmind.research.app.features.field.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.data.database.entity.ResearchSessionEntity
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.presentation.components.FieldMindMotion

private fun formatElapsed(startedAt: Long): String {
    val ms = System.currentTimeMillis() - startedAt
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

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
    data object WeatherDatabase : FieldMindScreen("field_weather_database", "Weather", FieldMindIcons.Weather)
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
    data object SettingsUnits : FieldMindScreen("field_settings_units", "Units", FieldMindIcons.Settings)
    data object SettingsWeather : FieldMindScreen("field_settings_weather", "Weather", FieldMindIcons.Weather)
    data object SettingsMap : FieldMindScreen("field_settings_map", "Map", FieldMindIcons.Map)
    data object SettingsDataIntegrity : FieldMindScreen("field_settings_data_integrity", "Data Integrity", FieldMindIcons.Archive)
    data object SettingsDeveloper : FieldMindScreen("field_settings_developer", "Developer", FieldMindIcons.Sparkle)
    data object SettingsSpeciesPacks : FieldMindScreen("field_settings_species_packs", "Species Packs", FieldMindIcons.Download)
    data object SettingsSpeciesId : FieldMindScreen("field_settings_species_id", "Species ID", FieldMindIcons.Nature)

    // Group 1: Interactive Data Tools
    data object CounterTool : FieldMindScreen("field_counter_tool", "Counter", FieldMindIcons.Add)
    data object MeasurementTool : FieldMindScreen("field_measurement_tool", "Measure", FieldMindIcons.Graph)
    data object WeatherLogTool : FieldMindScreen("field_weather_log_tool", "Weather", FieldMindIcons.Weather)
    data object SpeciesTool : FieldMindScreen("field_species_tool", "Species", FieldMindIcons.Nature)
    data object ChecklistTool : FieldMindScreen("field_checklist_tool", "Checklist", FieldMindIcons.Check)
    data object EventLogTool : FieldMindScreen("field_event_log_tool", "Event Log", FieldMindIcons.List)
    data object SiteLogTool : FieldMindScreen("field_site_log_tool", "Site Log", FieldMindIcons.Map)
    data object ComparisonTable : FieldMindScreen("field_comparison_table", "Comparison", FieldMindIcons.Data)
    data object SpeciesBrowser : FieldMindScreen("field_species_browser", "Species Browser", FieldMindIcons.Nature)
    data object TaxonomicBrowser : FieldMindScreen("field_taxonomic_browser", "Taxonomic Browser", FieldMindIcons.Category)
    data object FieldLog : FieldMindScreen("field_log", "Field Log", FieldMindIcons.List)
}

private val bottomTabs = listOf(
    FieldMindScreen.Home,
    FieldMindScreen.Observe,
    FieldMindScreen.Projects,
    FieldMindScreen.Insights,
    FieldMindScreen.Library
)

@Composable
fun FieldMindApp(appSettings: AppSettings, viewModel: FieldMindViewModel) {
    val onboardingCompleted by appSettings.onboardingCompleted.collectAsState()
    var appUnlocked by remember { mutableStateOf(!viewModel.fieldSettings.privacyLockEnabled.value) }
    val privacyEnabled by viewModel.fieldSettings.privacyLockEnabled.collectAsState()
    LaunchedEffect(privacyEnabled) { if (!privacyEnabled) appUnlocked = true }
    if (!onboardingCompleted) {
        FieldMindOnboardingScreen(
            settings = viewModel.fieldSettings,
            onFinish = { appSettings.setOnboardingCompleted(true) }
        )
    } else {
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

/** Navigate to a non-tab destination. Does NOT use restoreState to avoid the
 * "NavBackStackEntry destroyed" crash that occurs when the navigation library
 * tries to access a destroyed entry's ViewModelStore during state restoration. */
private fun NavHostController.navigateToDestination(route: String) {
    navigate(route) {
        launchSingleTop = true
        // Intentionally no restoreState — prevents crash when navigating back
        // to a destination whose NavBackStackEntry ViewModel was already disposed.
    }
}

@Composable
fun FieldMindNavigation(viewModel: FieldMindViewModel, onResetOnboarding: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val haptics = rememberFieldMindHaptics()
    // Positive-list approach: only show bottom nav on the 5 main tab routes.
    // Hide on everything else (settings, tools, detail screens, etc.) to avoid
    // bottom nav appearing on screens where it doesn't belong.
    val showChrome = currentRoute in listOf(
        FieldMindScreen.Home.route,
        FieldMindScreen.Observe.route,
        FieldMindScreen.Projects.route,
        FieldMindScreen.Insights.route,
        FieldMindScreen.Library.route
    ) && !(currentRoute == FieldMindScreen.Observe.route && viewModel.captureSessionActive)
    val hideChrome = !showChrome

    // ── Capture session navigation guard ──
    var showNavigateConfirm by remember { mutableStateOf(false) }
    var pendingNavRoute by remember { mutableStateOf<String?>(null) }

    fun navigateToTab(route: String) {
        // Protect against accidental navigation while a capture session is active
        if (currentRoute == FieldMindScreen.Observe.route && viewModel.captureSessionActive && route != FieldMindScreen.Observe.route) {
            pendingNavRoute = route
            showNavigateConfirm = true
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun isSelected(screen: FieldMindScreen) =
        currentDestination?.hierarchy?.any { it.route == screen.route } == true

    // ── Navigation confirmation dialog (for active capture session) ──
    if (showNavigateConfirm) {
        AlertDialog(
            onDismissRequest = {
                showNavigateConfirm = false
                pendingNavRoute = null
            },
            icon = { Icon(icon = FieldMindIcons.Info, contentDescription = null, size = 28.dp) },
            title = { Text("Active capture session") },
            text = {
                Text(
                    "You have an active observation session with unsaved data. Navigate away and lose your progress?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCaptureSessionActive(false)
                        showNavigateConfirm = false
                        pendingNavRoute?.let { navController.navigate(it) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        } }
                        pendingNavRoute = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard & navigate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNavigateConfirm = false
                    pendingNavRoute = null
                }) { Text("Stay on Capture") }
            }
        )
    }

    // Observe screen visibility settings so nav bar reflects user customizations
    val screenVisibility by viewModel.fieldSettings.screenVisibility.collectAsState()

    // Filter bottom tabs based on user's screen visibility preferences
    val visibleTabs = remember(bottomTabs, screenVisibility) {
        bottomTabs.filter { tab ->
            when (tab.route) {
                FieldMindScreen.Observe.route -> screenVisibility.showCapture
                FieldMindScreen.Projects.route -> screenVisibility.showProjects
                FieldMindScreen.Insights.route -> screenVisibility.showInsights
                FieldMindScreen.Library.route -> screenVisibility.showLibrary
                else -> true // Home always visible
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 720.dp
        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                if (!hideChrome) {
                    Surface(
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        NavigationRail(
                            header = {
                                Spacer(Modifier.height(8.dp))
                            }
                        ) {
                            visibleTabs.forEach { screen ->
                                val selected = isSelected(screen)
                                RailNavTabItem(
                                    screen = screen,
                                    selected = selected,
                                    onClick = { haptics.light(); navigateToTab(screen.route) }
                                )
                            }
                        }
                    }
                }
                FieldMindNavHost(navController, viewModel, onResetOnboarding, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!hideChrome) {
                        // ── True glassmorphic pill-shaped floating bottom nav bar ──
                        // Frosted-glass effect: semi-transparent surface with visible blur,
                        // subtle border to define the edge in light themes, generous rounded
                        // corners, lifted off bottom with safe-area-aware padding.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                                .navigationBarsPadding()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(34.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                    alpha = if (isSystemInDarkTheme()) 0.50f else 0.40f
                                ),
                                tonalElevation = 3.dp,
                                shadowElevation = 8.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 0.5.dp,
                                    color = if (isSystemInDarkTheme())
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
                                    else
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    visibleTabs.forEach { screen ->
                                        val selected = isSelected(screen)
                                        FloatingNavTabItem(
                                            screen = screen,
                                            selected = selected,
                                            onClick = { haptics.light(); navigateToTab(screen.route) }
                                        )
                                    }
                                }
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



/**
 * Glassmorphic pill-shaped floating nav tab item.
 * Active tab shows a prominent pill background with filled icon + bold text.
 * Inactive tabs show subtle icons with alpha. Interactive press animations.
 */
@Composable
private fun FloatingNavTabItem(
    screen: FieldMindScreen,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing),
        label = "navPillAlpha"
    )
    val iconSize by animateFloatAsState(
        targetValue = if (selected) 28f else 22f,
        animationSpec = tween(durationMillis = FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing),
        label = "navIconSize"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(durationMillis = FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing),
        label = "navTextAlpha"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !selected) 0.92f else 1f,
        animationSpec = if (isPressed) 
            tween(durationMillis = FieldMindMotion.durationMicro, easing = FastOutSlowInEasing) 
        else 
            FieldMindMotion.expressiveSpring,
        label = "navPressScale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() }
            )
            .defaultMinSize(minWidth = 60.dp, minHeight = 56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active pill background
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 36.dp)
                .background(
                    color = if (selected) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f * pillAlpha) 
                    else 
                        Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon = if (selected) screen.icon.copy(filled = true) else screen.icon,
                contentDescription = screen.label,
                tint = if (selected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                size = iconSize.dp,
                weight = if (selected) 500 else 400
            )
        }
        Text(
            screen.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha * 0.78f),
            maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = textAlpha.coerceIn(0.5f, 1f) }
        )
    }
    
    // Track press state for press animation
    LaunchedEffect(selected) {
        if (selected) isPressed = false
    }
}

/**
 * Rail nav tab item used in the side rail (tablet layout).
 * Mirrors the floating island style: filled rounded background for active tab.
 */
@Composable
private fun RailNavTabItem(
    screen: FieldMindScreen,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 48.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon = if (selected) screen.icon.copy(filled = true) else screen.icon,
            contentDescription = screen.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            size = if (selected) 26.dp else 22.dp
        )
        Text(
            screen.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
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
            val slideAnim = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            val fadeAnim = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)
            if (direction == 0) {
                fadeIn(animationSpec = FieldMindMotion.expressiveFloat) +
                scaleIn(initialScale = 0.97f, animationSpec = FieldMindMotion.expressiveFloat)
            } else {
                slideInHorizontally(slideAnim) { direction * it / 4 } +
                fadeIn(fadeAnim)
            }
        },
        exitTransition = {
            val direction = primaryTabDirection(initialState.destination.route, targetState.destination.route)
            val fadeAnim = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)
            if (direction == 0) {
                fadeOut(fadeAnim)
            } else {
                slideOutHorizontally(tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)) { -direction * it / 5 } +
                fadeOut(fadeAnim)
            }
        },
        popEnterTransition = {
            val direction = primaryTabDirection(targetState.destination.route, initialState.destination.route)
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            val fadeSpec = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)
            if (direction == 0) {
                slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { -it / 5 }) +
                fadeIn(animationSpec = fadeSpec)
            } else {
                slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { direction * it / 5 }) +
                fadeIn(animationSpec = fadeSpec)
            }
        },
        popExitTransition = {
            val direction = primaryTabDirection(targetState.destination.route, initialState.destination.route)
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            val fadeSpec = tween<Float>(FieldMindMotion.durationMicro, easing = FastOutSlowInEasing)
            if (direction == 0) {
                slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it / 4 }) +
                fadeOut(animationSpec = fadeSpec)
            } else {
                slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { -direction * it / 4 }) +
                fadeOut(animationSpec = fadeSpec)
            }
        }
    ) {
        composable(FieldMindScreen.Home.route) { HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Observe.route) { ObserveScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Projects.route) { ProjectsScreen(viewModel = viewModel, onOpenDetail = openDetail, onStartSession = { navController.navigateToDestination(FieldMindScreen.ResearchSession.route) }) }
        composable(FieldMindScreen.Library.route) { KnowledgeLibraryScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Insights.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Learn.route) { KnowledgeLibraryScreen(viewModel = viewModel, startTab = 3, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Reader.route) { LearnReaderScreen(url = readerTarget.first, title = readerTarget.second, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.FieldMode.route) { ObserveScreen(viewModel = viewModel, compactFieldMode = true, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Questions.route) { QuestionsScreen(viewModel = viewModel, onOpenDetail = openDetail) }
composable(FieldMindScreen.Hypotheses.route) { ProjectsScreen(viewModel = viewModel, startTab = 2, onOpenDetail = openDetail) }
composable(FieldMindScreen.DataTools.route) { DataToolsHubScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onNavigate = { navController.navigateToDestination(it.route) }) }
composable(FieldMindScreen.Analysis.route) { ProjectsScreen(viewModel = viewModel, startTab = 0, onOpenDetail = openDetail) }
composable(FieldMindScreen.Reports.route) { ProjectsScreen(viewModel = viewModel, startTab = 4, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Search.route) { ArchiveScreen(viewModel = viewModel, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.MapScreen.route) { MapFieldScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.ExportStudio.route) { BackupExportScreen(viewModel = viewModel) }
        composable(FieldMindScreen.Changelog.route) { FieldMindChangelogScreen(onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.Progress.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Flashcards.route) { FlashcardSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.ResearchSession.route) { ResearchSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.WeatherDatabase.route) { WeatherDatabaseScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.SettingsWeather.route) }, onOpenDetail = openDetail) }
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
                onOpenWeather = { navController.navigateToDestination(FieldMindScreen.SettingsWeather.route) },
                onOpenAi = { navController.navigateToDestination(FieldMindScreen.SettingsAi.route) },
                onOpenLocalModel = { navController.navigateToDestination(FieldMindScreen.SettingsLocalModel.route) },
                onOpenBackup = { navController.navigateToDestination(FieldMindScreen.SettingsBackup.route) },
                onOpenSecurity = { navController.navigateToDestination(FieldMindScreen.SettingsSecurity.route) },
                onOpenChangelog = { navController.navigateToDestination(FieldMindScreen.Changelog.route) },
                onOpenUnits = { navController.navigateToDestination(FieldMindScreen.SettingsUnits.route) },
                onOpenMap = { navController.navigateToDestination(FieldMindScreen.SettingsMap.route) },
                onOpenDataIntegrity = { navController.navigateToDestination(FieldMindScreen.SettingsDataIntegrity.route) },
                onOpenDeveloper = { navController.navigateToDestination(FieldMindScreen.SettingsDeveloper.route) },
                onOpenSpeciesPacks = { navController.navigateToDestination(FieldMindScreen.SettingsSpeciesPacks.route) },
                onOpenSpeciesId = { navController.navigateToDestination(FieldMindScreen.SettingsSpeciesId.route) }
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
        composable(FieldMindScreen.SettingsUnits.route) { UnitsFormatSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsWeather.route) { WeatherSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsMap.route) { MapSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsDataIntegrity.route) { DataIntegritySettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsDeveloper.route) { DeveloperSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SpeciesBrowser.route) { SpeciesBrowserScreen(onBack = { navController.popBackStack() }, onOpenDetail = { id -> navController.navigateToDestination("field_species_detail/$id") }) }
        composable(FieldMindScreen.TaxonomicBrowser.route) { TaxonomicBrowserScreen(onBack = { navController.popBackStack() }, onOpenDetail = { id -> navController.navigateToDestination("field_species_detail/$id") }) }
        composable("field_species_detail/{speciesId}") { entry ->
            val speciesId = entry.arguments?.getString("speciesId") ?: ""
            SpeciesDetailScreen(speciesId = speciesId, onBack = { navController.popBackStack() })
        }
        composable(FieldMindScreen.SettingsSpeciesPacks.route) { SpeciesPackSettingsPage(onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsSpeciesId.route) { SpeciesIdentificationSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.CounterTool.route) { CounterToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.MeasurementTool.route) { MeasurementToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.WeatherLogTool.route) { WeatherLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SpeciesTool.route) { SpeciesToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenBrowser = { navController.navigateToDestination(FieldMindScreen.SpeciesBrowser.route) }, onOpenTaxonomicBrowser = { navController.navigateToDestination(FieldMindScreen.TaxonomicBrowser.route) }) }
        composable(FieldMindScreen.ChecklistTool.route) { ChecklistToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.EventLogTool.route) { EventLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SiteLogTool.route) { SiteLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.ComparisonTable.route) { ComparisonTableScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.FieldLog.route) { FieldLogScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable("field_detail/{kind}/{id}") { entry ->
            val kind = entry.arguments?.getString("kind") ?: "observation"
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            DetailScreen(kind = kind, id = id, viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail, onOpenReader = openReader)
        }
    }
}
