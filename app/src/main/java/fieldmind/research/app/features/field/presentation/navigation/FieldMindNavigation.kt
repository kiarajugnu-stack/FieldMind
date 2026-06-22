package fieldmind.research.app.features.field.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.AnimatedContentTransitionScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fieldmind.research.app.features.field.presentation.components.FieldMindSnackbarProvider
import fieldmind.research.app.features.field.presentation.components.SwipeBackHost
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.screens.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.data.database.entity.ResearchSessionEntity
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.SharedTransitionLayout
import fieldmind.research.app.features.field.presentation.components.FieldMindMotion
import fieldmind.research.app.features.field.presentation.components.LocalPrivacyTypingEnabled
import fieldmind.research.app.features.field.presentation.components.PrivacyTextInputWrapper
import fieldmind.research.app.features.field.presentation.components.liquidGlassRefraction
import androidx.compose.runtime.CompositionLocalProvider

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

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
    data object SettingsScreenVisibility : FieldMindScreen("field_settings_screen_visibility", "Screen Visibility", FieldMindIcons.Visibility)
    data object SettingsAbout : FieldMindScreen("field_settings_about", "About", FieldMindIcons.Info)
    data object SettingsUnits : FieldMindScreen("field_settings_units", "Units", FieldMindIcons.Settings)
    data object SettingsWeather : FieldMindScreen("field_settings_weather", "Weather", FieldMindIcons.Weather)
    data object SettingsMap : FieldMindScreen("field_settings_map", "Map", FieldMindIcons.Map)
    data object SettingsDataIntegrity : FieldMindScreen("field_settings_data_integrity", "Data Integrity", FieldMindIcons.Archive)
    data object SettingsDeveloper : FieldMindScreen("field_settings_developer", "Developer", FieldMindIcons.Sparkle)
    data object SettingsSpeciesPacks : FieldMindScreen("field_settings_species_packs", "Species Packs", FieldMindIcons.Download)
    data object SettingsSpeciesId : FieldMindScreen("field_settings_species_id", "Species ID", FieldMindIcons.Nature)
    data object SettingsAutoGen : FieldMindScreen("field_settings_auto_gen", "Auto generation", FieldMindIcons.Sparkle)

    // ── Creation screens (converted from dialogs) ──
    data object NewProject : FieldMindScreen("field_new_project", "New Project", FieldMindIcons.Project)
    data object NewQuestion : FieldMindScreen("field_new_question", "New Question", FieldMindIcons.Question)
    data object NewHypothesis : FieldMindScreen("field_new_hypothesis", "New Hypothesis", FieldMindIcons.Hypothesis)
    data object NewDataRecord : FieldMindScreen("field_new_data_record", "New Data Record", FieldMindIcons.Data)
    data object NewReport : FieldMindScreen("field_new_report", "New Report", FieldMindIcons.Report)

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
    data object TimerTool : FieldMindScreen("field_timer", "Timer", FieldMindIcons.Timer)

    // ── Canvas note editor ──
    data object Canvas : FieldMindScreen("field_canvas/{noteId}", "Canvas", MaterialSymbolIcon("dashboard_customize"))
}

private val bottomTabs = listOf(
    FieldMindScreen.Home,
    FieldMindScreen.Observe,
    FieldMindScreen.Projects,
    FieldMindScreen.Insights,
    FieldMindScreen.Library
)

@Composable
fun FieldMindApp(appSettings: AppSettings, viewModel: FieldMindViewModel, requestedDestination: String? = null) {
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
            val privacyTyping by viewModel.fieldSettings.privacyTypingEnabled.collectAsState()
            CompositionLocalProvider(LocalPrivacyTypingEnabled provides privacyTyping) {
                PrivacyTextInputWrapper {
                    FieldMindSnackbarProvider { _ ->
                        FieldMindNavigation(viewModel = viewModel, requestedDestination = requestedDestination, onResetOnboarding = { appSettings.setOnboardingCompleted(false); appUnlocked = false })
                    }
                }
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
fun FieldMindNavigation(viewModel: FieldMindViewModel, requestedDestination: String? = null, onResetOnboarding: () -> Unit) {
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
        // Skip navigation if already on the target tab — prevents the
        // inclusive=true + restoreState=true cycle that causes flickering
        // and state-restoration failures when tapping the current tab.
        if (currentRoute == route) return

        // Protect against accidental navigation while a capture session is active
        if (currentRoute == FieldMindScreen.Observe.route && viewModel.captureSessionActive) {
            pendingNavRoute = route
            showNavigateConfirm = true
            return
        }
        // Pop everything up to the start destination then navigate to the target tab.
        // Only use inclusive=true when navigating to a non-start tab that's nested
        // below it. For the start destination itself, skip entirely (handled above).
        val startDest = navController.graph.startDestinationRoute ?: FieldMindScreen.Home.route
        navController.navigate(route) {
            popUpTo(startDest) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(requestedDestination) {
        when (requestedDestination) {
            FieldMindScreen.FieldMode.route, "field_mode" -> navController.navigateToDestination(FieldMindScreen.FieldMode.route)
            "field_timer" -> navController.navigateToDestination(FieldMindScreen.ResearchSession.route)
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
                            popUpTo(navController.graph.startDestinationRoute ?: FieldMindScreen.Home.route) {
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
                FieldMindScreen.FieldMode.route -> true
                FieldMindScreen.Insights.route -> screenVisibility.showInsights
                FieldMindScreen.Library.route -> screenVisibility.showLibrary
                else -> true // Home always visible
            }
        }
    }

    // ── HazeState for backdrop blur on the floating nav pill ──
    val hazeState = remember { HazeState() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp
        if (expanded) {
            Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // Tablet rail — liquid-glass side panel. .hazeChild blurs the
                // NavHost content behind the rail (captured via .haze() below);
                // .liquidGlassRefraction() applies GPU displacement & specular.
                if (!hideChrome) {
                    Surface(
                        shape = RoundedCornerShape(size = 24.dp),
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.6.dp,
                            color = if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .width(IntrinsicSize.Min)
                            .hazeChild(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 24.dp,
                                    noiseFactor = 0.04f,
                                    tints = listOf(
                                        HazeTint(
                                            color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                                alpha = if (isSystemInDarkTheme()) 0.88f else 0.93f
                                            )
                                        )
                                    )
                                )
                            )
                            .liquidGlassRefraction()
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
                // Content — blur source (captured by Haze for the rail's glass)
                FieldMindNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    onResetOnboarding = onResetOnboarding,
                    modifier = Modifier.weight(1f).haze(state = hazeState)
                )
            }
        } else {
            // ── True floating overlay nav bar with liquid glass effect ──
            // We use a raw Box instead of Scaffold so Android never draws a
            // solid rectangular bottom-bar background behind the pill.
            // The content fills the full screen edge-to-edge; the pill is
            // overlaid at the bottom with real backdrop blur via Haze.
            //
            // IMPORTANT: .haze() is ONLY on the NavHost content, NOT the outer
            // Box — this ensures the pill and its shadow/glow layers are never
            // captured into the blur source, preventing visible layer artifacts.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Content — fills full screen edge-to-edge (blur source ONLY)
                FieldMindNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    onResetOnboarding = onResetOnboarding,
                    modifier = Modifier.fillMaxSize().haze(state = hazeState)
                )

                // Floating pill — liquid glass with Haze blur + GPU refraction
                if (!hideChrome) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .wrapContentHeight(align = Alignment.Bottom)
                    ) {
                        // Glassmorphic nav pill — real backdrop blur via Haze with
                        // GPU liquid-glass displacement, specular highlights, and
                        // Fresnel edge glow via the .liquidGlassRefraction() modifier.
                        Surface(
                            shape = RoundedCornerShape(34.dp),
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp)
                                .clip(RoundedCornerShape(34.dp))
                                .hazeChild(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 24.dp,
                                        noiseFactor = 0.04f,
                                        tints = listOf(
                                            HazeTint(
                                                color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                                    alpha = if (isSystemInDarkTheme()) 0.88f else 0.93f
                                                )
                                            )
                                        )
                                    )
                                )
                                .liquidGlassRefraction()
                        ) {
                            LiquidNavRow(
                                visibleTabs = visibleTabs,
                                isSelected = { screen -> isSelected(screen) },
                                onTabClick = { screen ->
                                    haptics.light()
                                    navigateToTab(screen.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



/** Actual per-tab position+width from onGloballyPositioned, used for
 * precise blob centering regardless of Arrangement inter-item spacing. */
private data class TabBounds(val x: Float, val width: Float)

/**
 * Liquid glassmorphism nav row with animated blob indicator.
 * Draws a fluid rounded pill that slides between active tab positions
 * with spring physics, creating the "liquid/blob" micro-interaction.
 */
@Composable
private fun LiquidNavRow(
    visibleTabs: List<FieldMindScreen>,
    isSelected: (FieldMindScreen) -> Boolean,
    onTabClick: (FieldMindScreen) -> Unit
) {
    // Calculate selected index directly (no remember wrapper — isSelected lambda
    // captures currentDestination and changes every frame)
    val selectedIndex = visibleTabs.indexOfFirst { isSelected(it) }.coerceAtLeast(0)

    // ── Animate the indicator position with spring physics for liquid feel ──
    val animatedPosition = remember { Animatable(0f) }
    val animSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
    LaunchedEffect(selectedIndex) {
        animatedPosition.animateTo(selectedIndex.toFloat(), animSpec)
    }

    // ── Item bounds tracked via onGloballyPositioned ──
    // positionInRoot() (minus parent's root position) gives the actual
    // x-offset within the Row, which automatically accounts for
    // Arrangement.SpaceEvenly inter-item gaps.
    val tabBounds = remember { mutableStateListOf<TabBounds>() }

    // Capture color scheme in composable scope (Canvas DrawScope is not composable)
    val blobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // ── Liquid blob indicator drawn behind the tabs ──
        // (Canvas is drawn first, so it appears behind the Row)
        // Uses actual per-tab x-position (from onGloballyPositioned) for
        // precise centering regardless of inter-item spacing, with smooth
        // interpolation of both position and width between tab stops.
        if (tabBounds.isNotEmpty() && selectedIndex < tabBounds.size) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val pos = animatedPosition.value.coerceIn(0f, (tabBounds.size - 1).toFloat())
                val leftIdx = pos.toInt().coerceIn(0, tabBounds.size - 1)
                val rightIdx = (leftIdx + 1).coerceAtMost(tabBounds.size - 1)
                val fraction = pos - leftIdx

                val leftBounds = tabBounds[leftIdx]
                val rightBounds = tabBounds.getOrElse(rightIdx) { leftBounds }

                // Interpolate center position
                val leftCenter = leftBounds.x + leftBounds.width / 2f
                val rightCenter = rightBounds.x + rightBounds.width / 2f
                val centerX = if (leftIdx == rightIdx) leftCenter
                    else leftCenter + (rightCenter - leftCenter) * fraction

                // Also interpolate width smoothly between tabs so the blob
                // doesn't abruptly jump when selection changes mid-animation.
                val indicatorWidth = leftBounds.width + (rightBounds.width - leftBounds.width) * fraction
                val indicatorHeight = size.height * 0.82f
                val indicatorY = (size.height - indicatorHeight) / 2f

                drawRoundRect(
                    color = blobColor,
                    topLeft = Offset(centerX - indicatorWidth / 2f, indicatorY),
                    size = Size(indicatorWidth, indicatorHeight),
                    cornerRadius = CornerRadius(indicatorHeight / 2f, indicatorHeight / 2f)
                )
            }
        }

        // ── Tab items drawn on top of the indicator ──
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleTabs.forEachIndexed { index, screen ->
                val selected = isSelected(screen)
                var isPressed by remember { mutableStateOf(false) }

                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed && !selected) 0.92f else 1f,
                    animationSpec = if (isPressed)
                        tween(durationMillis = FieldMindMotion.durationMicro, easing = FastOutSlowInEasing)
                    else
                        FieldMindMotion.expressiveSpring,
                    label = "tabScale_$index"
                )

                Column(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val width = coordinates.size.width.toFloat()
                            // positionInParent() was removed from Compose. Calculate
                            // relative position by subtracting parent's root position.
                            // positionInRoot/positionInWindow were removed in Compose BOM 2026.05.01.
                            // Use localToWindow(Offset.Zero) to compute relative x-position.
                            val childWindow = coordinates.localToWindow(Offset.Zero)
                            val parentWindow = coordinates.parentCoordinates?.localToWindow(Offset.Zero) ?: Offset.Zero
                            val x = (childWindow - parentWindow).x
                            if (tabBounds.size <= index) {
                                while (tabBounds.size <= index) tabBounds.add(TabBounds(0f, 0f))
                            }
                            tabBounds[index] = TabBounds(x, width)
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                isPressed = true
                                onTabClick(screen)
                            }
                        )
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .defaultMinSize(minWidth = 60.dp, minHeight = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(width = 48.dp, height = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon = if (selected) screen.icon.copy(filled = true) else screen.icon,
                            contentDescription = screen.label,
                            tint = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            size = if (selected) 28.dp else 22.dp,
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
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1
                    )
                }

                // Reset press state when selection changes
                LaunchedEffect(selected) {
                    if (selected) isPressed = false
                }
            }
        }
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

// ── Route-aware transition system ──

/** Categories for determining transition animations between screens. */
private enum class RouteCategory { Tab, SettingsHub, SettingsSubPage, Detail, Tool, Creation, Other }

private fun categorizeRoute(route: String): RouteCategory = when (route) {
    FieldMindScreen.Home.route, FieldMindScreen.Observe.route,
    FieldMindScreen.Projects.route, FieldMindScreen.Library.route,
    FieldMindScreen.Insights.route -> RouteCategory.Tab
    FieldMindScreen.Settings.route -> RouteCategory.SettingsHub
    FieldMindScreen.MapScreen.route, FieldMindScreen.ExportStudio.route,
    FieldMindScreen.Reader.route -> RouteCategory.Other
    else -> when {
        route.startsWith("field_settings") -> RouteCategory.SettingsSubPage
        route.startsWith("field_detail/") -> RouteCategory.Detail
        route.startsWith("field_new_") -> RouteCategory.Creation
        route in listOf(
            FieldMindScreen.CounterTool.route, FieldMindScreen.MeasurementTool.route,
            FieldMindScreen.WeatherLogTool.route, FieldMindScreen.SpeciesTool.route,
            FieldMindScreen.ChecklistTool.route, FieldMindScreen.EventLogTool.route,
            FieldMindScreen.SiteLogTool.route, FieldMindScreen.ComparisonTable.route,
            FieldMindScreen.SpeciesBrowser.route, FieldMindScreen.TaxonomicBrowser.route,
            FieldMindScreen.FieldLog.route, FieldMindScreen.TimerTool.route,
            FieldMindScreen.Flashcards.route, FieldMindScreen.ResearchSession.route,
            FieldMindScreen.WeatherDatabase.route
        ) -> RouteCategory.Tool
        else -> RouteCategory.Other
    }
}

/** Compute the enter transition animation based on route category pair. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.routeEnterTransition(): EnterTransition {
    val fromRoute = initialState.destination.route ?: ""
    val toRoute = targetState.destination.route ?: ""
    val fromCat = categorizeRoute(fromRoute)
    val toCat = categorizeRoute(toRoute)
    val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
    val fadeSpec = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)

    return when {
        // Tab ↔ Tab: horizontal slide based on index direction
        fromCat == RouteCategory.Tab && toCat == RouteCategory.Tab -> {
            val direction = primaryTabDirection(fromRoute, toRoute)
            if (direction == 0)
                fadeIn(animationSpec = FieldMindMotion.expressiveFloat) +
                scaleIn(initialScale = 0.97f, animationSpec = FieldMindMotion.expressiveFloat)
            else
                slideInHorizontally(slideSpec) { direction * it / 4 } + fadeIn(fadeSpec)
        }
        // Tab → sub-screen (settings, tools, details): slide from right
        fromCat == RouteCategory.Tab && toCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation
        ) -> slideInHorizontally(slideSpec) { it / 4 } + fadeIn(fadeSpec)
        // Settings hub ↔ subpage: fade-through
        fromCat == RouteCategory.SettingsHub && toCat == RouteCategory.SettingsSubPage ->
            fadeIn(animationSpec = FieldMindMotion.expressiveFloat)
        fromCat == RouteCategory.SettingsSubPage && toCat == RouteCategory.SettingsHub ->
            fadeIn(animationSpec = FieldMindMotion.expressiveFloat)
        // Sub-screen → back to tab: slide from left
        toCat == RouteCategory.Tab && fromCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation, RouteCategory.Other
        ) -> slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeSpec)
        // Default: fade + slight scale
        else -> fadeIn(animationSpec = FieldMindMotion.expressiveFloat) +
            scaleIn(initialScale = 0.97f, animationSpec = FieldMindMotion.expressiveFloat)
    }
}

/** Compute the exit transition animation based on route category pair. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.routeExitTransition(): ExitTransition {
    val fromRoute = initialState.destination.route ?: ""
    val toRoute = targetState.destination.route ?: ""
    val fromCat = categorizeRoute(fromRoute)
    val toCat = categorizeRoute(toRoute)
    val fadeSpec = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)

    return when {
        fromCat == RouteCategory.Tab && toCat == RouteCategory.Tab -> {
            val direction = primaryTabDirection(fromRoute, toRoute)
            if (direction == 0) fadeOut(fadeSpec)
            else {
                val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
                slideOutHorizontally(slideSpec) { -direction * it / 5 } + fadeOut(fadeSpec)
            }
        }
        fromCat == RouteCategory.Tab && toCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation
        ) -> {
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            slideOutHorizontally(slideSpec) { -it / 5 } + fadeOut(fadeSpec)
        }
        fromCat == RouteCategory.SettingsHub && toCat == RouteCategory.SettingsSubPage ->
            fadeOut(fadeSpec)
        fromCat == RouteCategory.SettingsSubPage && toCat == RouteCategory.SettingsHub ->
            fadeOut(fadeSpec)
        toCat == RouteCategory.Tab && fromCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation, RouteCategory.Other
        ) -> {
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            slideOutHorizontally(slideSpec) { it / 4 } + fadeOut(fadeSpec)
        }
        else -> fadeOut(fadeSpec)
    }
}

/** Compute the pop-enter transition (reverse navigation). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.routePopEnterTransition(): EnterTransition {
    val fromRoute = initialState.destination.route ?: ""
    val toRoute = targetState.destination.route ?: ""
    val fromCat = categorizeRoute(fromRoute)
    val toCat = categorizeRoute(toRoute)
    val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
    val fadeSpec = tween<Float>(FieldMindMotion.durationSubtle, easing = FastOutSlowInEasing)

    return when {
        fromCat == RouteCategory.Tab && toCat == RouteCategory.Tab -> {
            val direction = primaryTabDirection(toRoute, fromRoute)
            slideInHorizontally(slideSpec) { -direction * it / 5 } + fadeIn(fadeSpec)
        }
        // Pop back from sub-screen to tab: slide from left
        toCat == RouteCategory.Tab && fromCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation
        ) -> slideInHorizontally(slideSpec) { -it / 5 } + fadeIn(fadeSpec)
        // Pop back from subpage to settings hub
        toCat == RouteCategory.SettingsHub && fromCat == RouteCategory.SettingsSubPage ->
            fadeIn(animationSpec = FieldMindMotion.expressiveFloat)
        // Pop back from settings subpage to another settings subpage?
        else -> slideInHorizontally(slideSpec) { -it / 5 } + fadeIn(fadeSpec)
    }
}

/** Compute the pop-exit transition (reverse navigation). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.routePopExitTransition(): ExitTransition {
    val fromRoute = initialState.destination.route ?: ""
    val toRoute = targetState.destination.route ?: ""
    val fromCat = categorizeRoute(fromRoute)
    val toCat = categorizeRoute(toRoute)
    val fadeSpec = tween<Float>(FieldMindMotion.durationMicro, easing = FastOutSlowInEasing)

    return when {
        fromCat == RouteCategory.Tab && toCat == RouteCategory.Tab -> {
            val direction = primaryTabDirection(toRoute, fromRoute)
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            slideOutHorizontally(slideSpec) { direction * it / 4 } + fadeOut(fadeSpec)
        }
        // Pop back: tab exits to the right
        fromCat == RouteCategory.Tab && toCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail
        ) -> {
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            slideOutHorizontally(slideSpec) { it / 4 } + fadeOut(fadeSpec)
        }
        toCat == RouteCategory.Tab && fromCat in listOf(
            RouteCategory.SettingsHub, RouteCategory.SettingsSubPage,
            RouteCategory.Tool, RouteCategory.Detail, RouteCategory.Creation
        ) -> {
            val slideSpec = tween<IntOffset>(FieldMindMotion.durationStandard, easing = FastOutSlowInEasing)
            slideOutHorizontally(slideSpec) { it / 4 } + fadeOut(fadeSpec)
        }
        else -> fadeOut(fadeSpec)
    }
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

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = FieldMindScreen.Home.route,
            modifier = modifier,
            enterTransition = { routeEnterTransition() },
            exitTransition = { routeExitTransition() },
            popEnterTransition = { routePopEnterTransition() },
            popExitTransition = { routePopExitTransition() }
        ) {
        composable(FieldMindScreen.Home.route) { HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.Settings.route) }, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Observe.route) { ObserveScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Projects.route) { ProjectsScreen(viewModel = viewModel, onOpenDetail = openDetail, onStartSession = { navController.navigateToDestination(FieldMindScreen.ResearchSession.route) }, onNavigate = { navController.navigateToDestination(it.route) }) }
        composable(FieldMindScreen.Library.route) { KnowledgeLibraryScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail, onOpenReader = openReader) }
        composable(FieldMindScreen.Insights.route) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) }
        composable(FieldMindScreen.Learn.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { FieldMindLearnScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenReader = openReader) } }
        composable(FieldMindScreen.Reader.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { LearnReaderScreen(url = readerTarget.first, title = readerTarget.second, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.FieldMode.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { ObserveScreen(viewModel = viewModel, compactFieldMode = true, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) } }
        composable(FieldMindScreen.Questions.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { QuestionsScreen(viewModel = viewModel, onOpenDetail = openDetail) } }
composable(FieldMindScreen.Hypotheses.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { QuestionsScreen(viewModel = viewModel, onOpenDetail = openDetail) } }
composable(FieldMindScreen.DataTools.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { DataToolsHubScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) } }
composable(FieldMindScreen.Analysis.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { ProjectsScreen(viewModel = viewModel, startTab = 0, onOpenDetail = openDetail, onNavigate = { navController.navigateToDestination(it.route) }) } }        composable(FieldMindScreen.Reports.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { FieldMindReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.Search.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { ArchiveScreen(viewModel = viewModel, onOpenDetail = openDetail, onOpenReader = openReader) } }
        composable(FieldMindScreen.MapScreen.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { MapFieldScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) } }
        composable(FieldMindScreen.ExportStudio.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { BackupAndRestoreScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.Changelog.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { FieldMindChangelogScreen(onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.Progress.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { InsightsScreen(viewModel = viewModel, onNavigate = { navController.navigateToDestination(it.route) }, onOpenDetail = openDetail) } }
        composable(FieldMindScreen.Flashcards.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { FlashcardSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.ResearchSession.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { ResearchSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenDetail = openDetail) } }
        composable(FieldMindScreen.WeatherDatabase.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { WeatherDatabaseScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenSettings = { navController.navigateToDestination(FieldMindScreen.SettingsWeather.route) }, onOpenDetail = openDetail) } }
        composable(FieldMindScreen.Settings.route) {
            SwipeBackHost(onBack = { navController.popBackStack() }) {
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
                onOpenBackup = { navController.navigateToDestination(FieldMindScreen.ExportStudio.route) },
                onOpenSecurity = { navController.navigateToDestination(FieldMindScreen.SettingsSecurity.route) },
                onOpenChangelog = { navController.navigateToDestination(FieldMindScreen.Changelog.route) },
                onOpenUnits = { navController.navigateToDestination(FieldMindScreen.SettingsUnits.route) },
                onOpenScreenVisibility = { navController.navigateToDestination(FieldMindScreen.SettingsScreenVisibility.route) },
                onOpenMap = { navController.navigateToDestination(FieldMindScreen.SettingsMap.route) },
                onOpenDataIntegrity = { navController.navigateToDestination(FieldMindScreen.SettingsDataIntegrity.route) },
                onOpenDeveloper = { navController.navigateToDestination(FieldMindScreen.SettingsDeveloper.route) },
                onOpenSpeciesPacks = { navController.navigateToDestination(FieldMindScreen.SettingsSpeciesPacks.route) },
                onOpenSpeciesId = { navController.navigateToDestination(FieldMindScreen.SettingsSpeciesId.route) },
                onOpenAutoGen = { navController.navigateToDestination(FieldMindScreen.SettingsAutoGen.route) }
            )
        } }
        composable(FieldMindScreen.SettingsProfile.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { ProfileSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsAppearance.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { AppearanceSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsCapture.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { CaptureDefaultsSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsAi.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { AiAssistantSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsLocalModel.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { LocalModelSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsBackup.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { BackupImportSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenExport = { navController.navigateToDestination(FieldMindScreen.ExportStudio.route) }) } }
        composable(FieldMindScreen.SettingsSecurity.route) {
            SwipeBackHost(onBack = { navController.popBackStack() }) {
            SecuritySettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() })
        } }
        composable(FieldMindScreen.SettingsScreenVisibility.route) {
            SwipeBackHost(onBack = { navController.popBackStack() }) {
            ScreenVisibilitySettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() })
        } }
        composable(FieldMindScreen.SettingsAbout.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { AboutPage(onBack = { navController.popBackStack() }, onOpenChangelog = { navController.navigateToDestination(FieldMindScreen.Changelog.route) }) } }
        composable(FieldMindScreen.SettingsUnits.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { UnitsFormatSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsWeather.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { WeatherSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsMap.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { MapSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsDataIntegrity.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { DataIntegritySettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SettingsDeveloper.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { DeveloperSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) } }
        composable(FieldMindScreen.SpeciesBrowser.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { SpeciesBrowserScreen(onBack = { navController.popBackStack() }, onOpenDetail = { id -> navController.navigateToDestination("field_species_detail/$id") }) } }
        composable(FieldMindScreen.TaxonomicBrowser.route) { SwipeBackHost(onBack = { navController.popBackStack() }) { TaxonomicBrowserScreen(onBack = { navController.popBackStack() }, onOpenDetail = { id -> navController.navigateToDestination("field_species_detail/$id") }) } }
        composable("field_species_detail/{speciesId}") { entry ->
            val speciesId = entry.arguments?.getString("speciesId") ?: ""
            SwipeBackHost(onBack = { navController.popBackStack() }) {
            SpeciesDetailScreen(speciesId = speciesId, onBack = { navController.popBackStack() })
        }
        composable(FieldMindScreen.SettingsSpeciesPacks.route) { SpeciesPackSettingsPage(onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsSpeciesId.route) { SpeciesIdentificationSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SettingsAutoGen.route) { AutoGenerationSettingsPage(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.CounterTool.route) { CounterToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.MeasurementTool.route) { MeasurementToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.WeatherLogTool.route) { WeatherLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
                composable(FieldMindScreen.NewProject.route) { NewProjectScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.NewQuestion.route) { NewQuestionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.NewHypothesis.route) { NewHypothesisScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.NewDataRecord.route) { NewDataRecordScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.NewReport.route) { NewReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
composable(FieldMindScreen.SpeciesTool.route) { SpeciesToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onOpenBrowser = { navController.navigateToDestination(FieldMindScreen.SpeciesBrowser.route) }, onOpenTaxonomicBrowser = { navController.navigateToDestination(FieldMindScreen.TaxonomicBrowser.route) }) }
        composable(FieldMindScreen.ChecklistTool.route) { ChecklistToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.EventLogTool.route) { EventLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.SiteLogTool.route) { SiteLogToolScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.ComparisonTable.route) { ComparisonTableScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(FieldMindScreen.TimerTool.route) { TimerToolScreen(onBack = { navController.popBackStack() }) }
        composable("field_canvas/{noteId}") { entry ->
            val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
            CanvasScreen(
                noteId = noteId,
                fieldViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenLinkedEntity = { kind, id ->
                    navController.navigateToDestination("field_detail/$kind/$id")
                }
            )
        }
        composable(FieldMindScreen.FieldLog.route) { FieldLogScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenDetail = openDetail,
                        onOpenExport = { navController.navigateToDestination(FieldMindScreen.ExportStudio.route) }
                    ) } }
        composable("field_detail/{kind}/{id}") { entry ->
            val kind = entry.arguments?.getString("kind") ?: "observation"
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            DetailScreen(
                kind = kind, id = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenDetail = openDetail,
                onOpenReader = openReader,
                onOpenCanvas = { noteId ->
                    navController.navigateToDestination("field_canvas/$noteId")
                }
            )
        }
    }
}
