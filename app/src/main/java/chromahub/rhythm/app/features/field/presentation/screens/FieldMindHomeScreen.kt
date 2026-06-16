package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.data.weather.WeatherUnitConverter
import fieldmind.research.app.features.field.data.learn.LearnResource
import fieldmind.research.app.features.field.data.learn.LearnLibrary
import fieldmind.research.app.features.field.data.stats.FieldMindStreaks
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToInt
import fieldmind.research.app.features.field.presentation.components.ObservationsTimelineSection
import fieldmind.research.app.features.field.presentation.components.ObservationStatsDashboard

/**
 * Loads a PNG image from Android assets folder as an ImageBitmap for display.
 * Returns null if the asset path is null or the file cannot be loaded.
 */
@Composable
internal fun rememberAssetImage(assetPath: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        if (assetPath != null) {
            try {
                val inputStream = context.assets.open(assetPath)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap?.asImageBitmap()
            } catch (_: Exception) { null }
        } else null
    }
}


// ══════════════════════════════════════════════════════════════════════
//  Today (Home) — Animated weather centerpiece + research dashboard
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    viewModel: FieldMindViewModel,
    onOpenSettings: () -> Unit,
    onNavigate: (FieldMindScreen) -> Unit,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenReader: (String, String) -> Unit = { _, _ -> }
) {
    // ── Camera-first capture state ──
    var showCamera by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var capturedPhotoMime by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var selectedCaptureCategory by remember { mutableStateOf("Bird") }
    
    // ── Note creation dialog state ──
    var showNoteDialog by remember { mutableStateOf(false) }
    
    // Centered snackbar host state
    val captureSnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val goal by viewModel.fieldSettings.dailyObservationGoal.collectAsState()
    val streaksEnabled by viewModel.fieldSettings.streaksEnabled.collectAsState()
    val todayKey = remember { today() }
    val todayCount = observations.count { it.date == todayKey }
    val yesterdayKey = remember { LocalDate.now().minusDays(1).toString() }
    val yesterdayCount = observations.count { it.date == yesterdayKey }
    val currentStreak = remember(observations, streaksEnabled) { if (streaksEnabled) FieldMindStreaks.currentStreakDays(observations.map { it.date }) else 0 }
    val activeProject = projects.firstOrNull { it.status == "Active" } ?: projects.firstOrNull()
    val learnSignals = remember(observations, questions, projects) {
        buildList {
            observations.sortedByDescending { it.timestamp }.take(10).forEach { add(it.category); add(it.subject); add(it.tags) }
            questions.sortedByDescending { it.updatedAt }.take(10).forEach { add(it.category); add(it.questionText) }
            projects.take(6).forEach { add(it.topicType); add(it.title) }
        }
    }
    val researchSessions by viewModel.researchSessions.collectAsState()
    val recommendations = remember(learnSignals) { recommendedResources(learnSignals) }
    val weatherShowTemp by viewModel.fieldSettings.weatherShowTemperature.collectAsState()
    val weatherShowCondition by viewModel.fieldSettings.weatherShowCondition.collectAsState()
    val weatherShowHumidity by viewModel.fieldSettings.weatherShowHumidity.collectAsState()
    val weatherShowWind by viewModel.fieldSettings.weatherShowWind.collectAsState()
    val weatherShowCloud by viewModel.fieldSettings.weatherShowCloudCover.collectAsState()
    val weatherShowPressure by viewModel.fieldSettings.weatherShowPressure.collectAsState()
    val tempUnit by viewModel.fieldSettings.tempUnit.collectAsState()
    val windSpeedUnit by viewModel.fieldSettings.windSpeedUnit.collectAsState()
    val developerMode by viewModel.fieldSettings.developerMode.collectAsState()

    // ── Weather state (hoisted outside LazyColumn so it persists across scroll) ──
    var homeCurrentWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var homeWeatherLoading by remember { mutableStateOf(false) }
    var homeWeatherError by remember { mutableStateOf(false) }
    var homePlaceName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Initial fetch on first composition + auto-refresh at configured interval
    // Uses cooldown in ViewModel so navigating away and back doesn't re-fetch
    LaunchedEffect(Unit) {
        // First fetch on fresh app open (forceRefresh = true only once)
        val cached = viewModel.lastWeatherSnapshot
        if (cached == null) {
            homeWeatherLoading = true
            homeCurrentWeather = viewModel.refreshWeatherFromLocation(forceRefresh = true)
            homeWeatherError = homeCurrentWeather == null
            homeWeatherLoading = false
        } else {
            homeCurrentWeather = cached
            homeWeatherError = false
        }

        val locProvider = runCatching { FieldLocationProvider(context) }.getOrNull()
        if (locProvider != null && locProvider.hasAnyLocationPermission()) {
            locProvider.lastKnownLocation()?.let { loc ->
                locProvider.resolvePlaceName(loc.latitude, loc.longitude) { place ->
                    homePlaceName = place
                }
            }
        }

        // Periodic refresh at configured interval (respects cooldown internally)
        while (true) {
            delay(30 * 60 * 1000L)
            val snapshot = viewModel.refreshWeatherFromLocation()
            if (snapshot != null) {
                homeCurrentWeather = snapshot
                homeWeatherError = false
            }
        }
    }

    val lastSession = remember(researchSessions) {
        researchSessions.filter { it.status == "Completed" }.maxByOrNull { it.endedAt ?: it.createdAt }
    }

    // ── Yesterday vs today delta ──
    val obsDelta = todayCount - yesterdayCount
    val deltaLabel = when {
        obsDelta > 0 -> "+$obsDelta vs yesterday"
        obsDelta < 0 -> "$obsDelta vs yesterday"
        todayCount > 0 -> "Same as yesterday"
        else -> ""
    }

    // ── Moon phase ──
    val moonPhase = remember { getMoonPhase(LocalDate.now()) }

    // ── Conditions nudge ──
    val conditionsNudge = remember(homeCurrentWeather) {
        homeCurrentWeather?.let { computeFieldworkNudge(it) } ?: ""
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // ── Hero Section ──
            item { HomeHeroSection(todayCount, goal, currentStreak, observations.size, questions.size, onOpenSettings, onNavigate, onCapture = { showCamera = true }, onNewNote = { showNoteDialog = true }) }

            // ── Weather as animated centerpiece ──
            item {
                LiveWeatherDashboardWidget(
                    viewModel = viewModel,
                    observations = observations,
                    onNavigate = onNavigate,
                    currentWeather = homeCurrentWeather,
                    weatherLoading = homeWeatherLoading,
                    weatherError = homeWeatherError,
                    placeName = homePlaceName,
                    onRefresh = { snapshot ->
                        homeCurrentWeather = snapshot
                        homeWeatherError = snapshot == null
                    },
                    showTemp = weatherShowTemp,
                    showCondition = weatherShowCondition,
                    showHumidity = weatherShowHumidity,
                    showWind = weatherShowWind,
                    showCloudCover = weatherShowCloud,
                    showPressure = weatherShowPressure,
                    tempUnit = tempUnit,
                    windSpeedUnit = windSpeedUnit,
                    moonPhase = moonPhase,
                    conditionsNudge = conditionsNudge,
                    sunrise = homeCurrentWeather?.sunrise,
                    sunset = homeCurrentWeather?.sunset,
                    developerMode = developerMode
                )
            }

            // ── Daily Goal with delta ──
            item { DailyGoalCard(todayCount, goal, currentStreak, deltaLabel) { onNavigate(FieldMindScreen.Observe) } }

            // ── Research Session CTA ──
            item { ResearchSessionCtaCard(
                    lastSessionLabel = if (lastSession != null) "Resume your last session" else null,
                    activeSessionName = researchSessions.firstOrNull { it.status == "Active" }?.name,
                    timerMs = 0L,
                    onStartSession = { onNavigate(FieldMindScreen.ResearchSession) }
                ) }

            // ── Widget Grid ──
            item { SectionHeader("Research areas", "Quick overview of your work") }
            item { HomeWidgetGrid(observations, notes, questions, sources, projects, reports, data) { onNavigate(it) } }
            item { HomeDataOptionsCard(data, onNavigate) }
            
            // ── Recent Captures ──
            if (observations.isNotEmpty()) {
                item { RecentCapturesCard(observations, onOpenDetail) }
            }

            // ── Learning & Reading ──
            item { RecommendedLearningCard(recommendations, onOpenReader, onSeeAll = { onNavigate(FieldMindScreen.Learn) }) }
            item { ReadingReviewCard(sources, flashcards, onNavigate) }

            // ── Observations Timeline — Full redesign with list/gallery/map/calendar ──
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(FieldMindIcons.Calendar, null, tint = FieldMindTheme.colors.project, size = 22.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Observation timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Search, filter, and explore your observations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        ObservationsTimelineSection(
                            observations = observations,
                            viewModel = viewModel,
                            onOpenDetail = onOpenDetail,
                            onStartCapture = { onNavigate(FieldMindScreen.Observe) },
                            onOpenMap = { onNavigate(FieldMindScreen.MapScreen) }
                        )
                    }
                }
            }

            // ── Observation Statistics Dashboard ──
            if (observations.isNotEmpty()) {
                item {
                    ObservationStatsDashboard(observations = observations)
                }
            }

            // ── Session Observations ──
            if (observations.isNotEmpty()) {
                item {
                    val sessionObs = remember(observations, researchSessions) {
                        val map = mutableMapOf<String, MutableList<ObservationEntity>>()
                        observations.filter { it.tags.contains("research-session") }
                            .sortedByDescending { it.timestamp }
                            .forEach { obs ->
                                val key = obs.moodOrContext.ifBlank { "Session ${obs.date}" }
                                map.getOrPut(key) { mutableListOf() }.add(obs)
                            }
                        map.toMap()
                    }
                    if (sessionObs.isNotEmpty()) {
                        SessionObservationsCard(sessionObs, researchSessions, onOpenDetail, onNavigate)
                    }
                }
            }

            // ── Current Project ──
            if (activeProject != null) {
                item {
                    CurrentProjectResearchCard(
                        project = activeProject,
                        observations = observations,
                        sources = sources,
                        data = data,
                        hypotheses = hypotheses,
                        reports = reports,
                        onOpen = { onOpenDetail("project", activeProject.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        // ── Centered Snackbar for capture confirmation ──
        SnackbarHost(
            hostState = captureSnackbarHostState,
            modifier = Modifier.align(Alignment.Center),
            snackbar = { data ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Check,
                            null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            size = 22.dp
                        )
                        Text(
                            data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                }
            }
        )
    }

    // ── Camera Dialog (full-screen, opens immediately) ──
    if (showCamera) {
        Dialog(
            onDismissRequest = { showCamera = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                securePolicy = SecureFlagPolicy.SecureOn
            )
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                FieldMindCameraV2(
                    onPhotoCaptured = { uri, mime ->
                        capturedPhotoUri = uri
                        capturedPhotoMime = mime
                        showCamera = false
                        scope.launch {
                            captureSnackbarHostState.showSnackbar("Photo captured")
                        }
                        // Delay slightly for snackbar visibility, then show category picker
                        scope.launch {
                            delay(600)
                            showCategoryPicker = true
                        }
                    },
                    onDismiss = {
                        showCamera = false
                    }
                )
            }
        }
    }

    // ── Note Creation Dialog ──
    if (showNoteDialog) {
        HomeNoteCaptureDialog(
            viewModel = viewModel,
            onDismiss = { showNoteDialog = false }
        )
    }

    // ── Category Picker Dialog ──
    if (showCategoryPicker) {
        Dialog(
            onDismissRequest = { showCategoryPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Semi-transparent scrim
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showCategoryPicker = false }
                )
                
                // Category picker bottom sheet
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Header
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.width(40.dp).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "What category?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Categorize your capture to organize your research",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Category grid
                        val categories = listOf(
                            "Bird" to FieldMindIcons.Nature,
                            "Mammal" to FieldMindIcons.Nature,
                            "Plant" to FieldMindIcons.Nature,
                            "Insect" to FieldMindIcons.Nature,
                            "Water" to FieldMindIcons.Water,
                            "Soil" to FieldMindIcons.Water,
                            "Weather" to FieldMindIcons.Weather,
                            "Other" to FieldMindIcons.Water
                        )
                        var customCategory by remember { mutableStateOf("") }
                        
                        val colors = FieldMindTheme.colors

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categories.chunked(2).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { (name, icon) ->
                                        val isSelected = selectedCaptureCategory == name
                                        val accent = colors.accentFor(name)
                                        Card(
                                            modifier = Modifier.weight(1f).clickable {
                                                selectedCaptureCategory = name
                                            },
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accent) else null,
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(
                                                Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (isSelected) accent.copy(alpha = 0.22f)
                                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        icon,
                                                        null,
                                                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        size = 22.dp
                                                    )
                                                }
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Custom category text field when "Other" is selected
                            if (selectedCaptureCategory == "Other") {
                                OutlinedTextField(
                                    value = customCategory,
                                    onValueChange = { customCategory = it },
                                    label = { Text("Specify category") },
                                    placeholder = { Text("e.g. Reptile, Amphibian, Fungus…") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.accentFor("Other"),
                                        cursorColor = colors.accentFor("Other")
                                    )
                                )
                            }
                        }

                        // Confirm button - actually create observation
                        Button(
                            onClick = {
                                val photoUri = capturedPhotoUri
                                if (photoUri != null) {
                                    val effectiveCategory = if (selectedCaptureCategory == "Other" && customCategory.isNotBlank()) customCategory else selectedCaptureCategory
                                    val effectiveSubject = "$effectiveCategory observation"
                                    val attachment = capturedPhotoUri?.let { uri ->
                                        listOf(DraftEvidenceAttachment("Photo", uri, "Camera capture", mimeType = capturedPhotoMime))
                                    } ?: emptyList()
                                    viewModel.addObservation(
                                        subject = effectiveSubject,
                                        category = effectiveCategory,
                                        facts = "Auto-captured $effectiveCategory observation from camera.",
                                        confidence = "Likely",
                                        manualLocation = "",
                                        tags = "camera, $effectiveCategory",
                                        evidence = "Photo evidence captured via camera",
                                        context = "",
                                        attachments = attachment
                                    )
                                    scope.launch {
                                        captureSnackbarHostState.showSnackbar("$effectiveCategory observation saved")
                                    }
                                }
                                showCategoryPicker = false
                                capturedPhotoUri = null
                                capturedPhotoMime = null
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(FieldMindIcons.Observation, null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Create $selectedCaptureCategory observation",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Hero Section — Welcome + Animated Stats + Settings
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeHeroSection(
    todayCount: Int,
    goal: Int,
    streakDays: Int,
    totalObs: Int,
    totalQuestions: Int,
    onOpenSettings: () -> Unit,
    onNavigate: (FieldMindScreen) -> Unit,
    onCapture: () -> Unit = {},
    onNewNote: () -> Unit = {}
) {
    val colors = FieldMindTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.positive.copy(alpha = if (colors.isDark) 0.34f else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Nature, null, tint = colors.positive, size = 34.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "FieldMind",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Observe. Question. Research clearly.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
                    }
                }
            }

            Text(
                if (totalObs == 0) "Start your first observation to begin your research story."
                else "You have $totalObs observation${if (totalObs != 1) "s" else ""} across your research.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroActionChip(
                    icon = FieldMindIcons.Camera,
                    label = "Capture",
                    accent = colors.observation,
                    modifier = Modifier.weight(1f)
                ) { onCapture() }
                HeroActionChip(
                    icon = FieldMindIcons.Note,
                    label = "Note",
                    accent = colors.source,
                    modifier = Modifier.weight(1f)
                ) { onNewNote() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Note Creation Dialog (inline, used by HomeScreen Note button)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeNoteCaptureDialog(
    viewModel: FieldMindViewModel,
    onDismiss: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var tags by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).wrapContentHeight().padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(FieldMindTheme.colors.source.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Note, null, tint = FieldMindTheme.colors.source, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("New Note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Quickly capture an idea, observation, or thought", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Category selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OptionPickerField(label = "Category", selected = category, options = observationCategories, onSelected = { category = it }, icon = FieldMindIcons.Category)
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Optional — auto-generated from content") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                // Body
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Note body") },
                    placeholder = { Text("What would you like to note?…") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    shape = RoundedCornerShape(18.dp),
                    minLines = 5
                )

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    placeholder = { Text("Comma-separated, optional") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                // Advanced options
                CollapsibleSection("Advanced options", "Attachments and metadata", expanded = showAdvanced, onToggle = { showAdvanced = !showAdvanced }) {
                    OutlinedTextField(
                        value = attachments,
                        onValueChange = { attachments = it },
                        label = { Text("Attachments") },
                        placeholder = { Text("One per line: type|caption|uri") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        minLines = 2
                    )
                }

                // Actions
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            if (body.isNotBlank() || title.isNotBlank()) {
                                haptics.confirm()
                                val fallbackTitle = body
                                    .lineSequence()
                                    .firstOrNull { it.isNotBlank() }
                                    ?.take(48)
                                    ?: "Untitled note"
                                val parsedAttachments = attachments
                                    .lineSequence()
                                    .filter { it.isNotBlank() }
                                    .map { line ->
                                        val parts = line.split("|", limit = 3)
                                        DraftEvidenceAttachment(
                                            type = parts.getOrElse(0) { "File" },
                                            caption = parts.getOrElse(1) { "" },
                                            uri = parts.getOrElse(2) { parts[0] }
                                        )
                                    }.toList()
                                viewModel.addNote(
                                    title = title.ifBlank { fallbackTitle },
                                    body = body,
                                    category = category,
                                    tags = tags,
                                    attachments = parsedAttachments,
                                    onSaved = { onDismiss() }
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        enabled = body.isNotBlank() || title.isNotBlank()
                    ) { Text("Save Note") }
                }
            }
        }
    }
}

@Composable
private fun HeroStatBubble(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    // Simple count-up animation
    val animatedValue = remember { Animatable(0f) }
    val targetFloat = value.toFloatOrNull() ?: 0f
    LaunchedEffect(targetFloat) {
        animatedValue.animateTo(
            targetValue = targetFloat,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.1f),
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier.padding(16.dp, 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                if (value.toIntOrNull() != null) animatedValue.value.roundToInt().toString() else value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroActionChip(
    icon: MaterialSymbolIcon,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        modifier = modifier,
        onClick = { haptics.light(); onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = accent, size = 20.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}



private fun timeOfDay(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Live Weather Dashboard Widget — Live Open-Meteo weather with animations
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LiveWeatherDashboardWidget(
    viewModel: FieldMindViewModel,
    observations: List<ObservationEntity>,
    onNavigate: (FieldMindScreen) -> Unit = {},
    currentWeather: WeatherSnapshot? = null,
    weatherLoading: Boolean = false,
    weatherError: Boolean = false,
    placeName: String? = null,
    onRefresh: (WeatherSnapshot?) -> Unit = {},
    showTemp: Boolean = true,
    showCondition: Boolean = true,
    showHumidity: Boolean = true,
    showWind: Boolean = true,
    showCloudCover: Boolean = true,
    showPressure: Boolean = true,
    tempUnit: String = "Celsius",
    windSpeedUnit: String = "km/h",
    moonPhase: String = "",
    conditionsNudge: String = "",
    sunrise: String? = null,
    sunset: String? = null,
    developerMode: Boolean = false
) {
    val colors = FieldMindTheme.colors
    var testWeatherCode by remember { mutableStateOf<Int?>(null) }
    var testIsNight by remember { mutableStateOf(false) }

    // Time of day awareness
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val timeOfDay = when (currentHour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }
    val isNight = timeOfDay == "night"

    // Override weather code with test value if in developer mode
    val displayWeatherCode = testWeatherCode ?: currentWeather?.weatherCode ?: 0
    val displayNight = if (developerMode) testIsNight else isNight
    val timeGreeting = when (timeOfDay) {
        "morning" -> "Good morning"
        "afternoon" -> "Good afternoon"
        "evening" -> "Good evening"
        else -> "Good night"
    }

    // Show the weather condition icon in the header — show weather icon if we have data, 
    // loading spinner ONLY if there's no data yet (first load)
    val showLoadingSpinner = weatherLoading && currentWeather == null

    // Temperature-based palette for display
    val tempDisplay = currentWeather?.temperature ?: 20.0
    val displayColors = when {
        tempDisplay < 0 -> listOf(Color(0xFF1A237E), Color(0xFF42A5F5))
        tempDisplay < 10 -> listOf(Color(0xFF1565C0), Color(0xFF64B5F6))
        tempDisplay < 20 -> listOf(Color(0xFF0D47A1), Color(0xFF66BB6A))
        tempDisplay < 30 -> listOf(Color(0xFFE65100), Color(0xFFFFB74D))
        else -> listOf(Color(0xFFBF360C), Color(0xFFE57373))
    }
    val weatherGradient = Brush.horizontalGradient(displayColors)

    val conditionColor = remember(currentWeather) {
        val code = displayWeatherCode
        when {
            code == 0 || code == 1 -> colors.positive            // Clear
            code in 2..3 -> colors.info                           // Cloudy
            code in 45..48 -> colors.warning                      // Fog
            code in 51..67 -> colors.data                         // Rain
            code in 71..86 -> colors.observation                   // Snow
            code >= 95 -> Color(0xFFE53935)                        // Thunderstorm
            else -> colors.info
        }
    }

    // Text color that adapts to the animated weather scene background
    val textOnScene = when {
        colors.isDark -> Color.White               // Dark mode: always dark scene bg
        isNight -> Color.White                      // Night scene even in light mode: dark bg
        else -> Color(0xFF1A1A3E)                    // Light mode + day scene: pastel bg, dark text
    }

    // Live indicator pulse
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        0.4f, 1.0f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "liveAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            .clickable { onNavigate(FieldMindScreen.WeatherDatabase) },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated weather scene as background
            if (currentWeather != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            Modifier.clip(RoundedCornerShape(28.dp))
                        )
                ) {
                    AnimatedWeatherScene(
                        weatherCode = displayWeatherCode,
                        temperature = currentWeather!!.temperature,
                        sunrise = currentWeather!!.sunrise,
                        sunset = currentWeather!!.sunset,
                        compact = false
                    )
                }
                // Glass-morphism scrim for better text readability
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    textOnScene.copy(alpha = 0.02f),
                                    textOnScene.copy(alpha = 0.06f),
                                    textOnScene.copy(alpha = 0.10f),
                                    textOnScene.copy(alpha = 0.06f),
                                    textOnScene.copy(alpha = 0.02f)
                                )
                            )
                        )
                )
            }

            // Content overlay
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // ── Header row with live indicator ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (currentWeather != null) weatherGradient
                            else Brush.horizontalGradient(
                                listOf(
                                    colors.info.copy(alpha = 0.14f),
                                    colors.info.copy(alpha = 0.06f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLoadingSpinner) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            weatherConditionIcon(displayWeatherCode),
                            null,
                            tint = if (currentWeather != null) textOnScene else Color.White,
                            size = 24.dp
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            placeName ?: "Live weather",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (currentWeather != null) textOnScene else MaterialTheme.colorScheme.onSurface
                        )
                        // Live pulse dot
                        if (currentWeather != null) {
                            Box(
                                Modifier.size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.positive.copy(alpha = pulseAlpha))
                            )
                        }
                    }
                    Text(
                        when {
                            weatherLoading && currentWeather != null -> "Refreshing…"
                            weatherLoading -> "Loading…"
                            currentWeather != null -> {
                                // Subtitle shows condition description only (temp shown below in main row)
                                val desc = currentWeather?.weatherDescription ?: ""
                                if (showCondition && desc.isNotBlank()) desc else "Weather data available"
                            }
                            weatherError -> "Enable location for live weather"
                            else -> "Weather unavailable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentWeather != null) textOnScene.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }

            // ── Time-of-day greeting ──
            if (currentWeather != null) {
                Text(
                    "$timeGreeting. ${currentWeather!!.weatherDescription.ifBlank { "Clear skies" }}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textOnScene.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Weather data when available ──
            if (currentWeather != null) {
                val w = currentWeather!!

                // Main temperature + condition (always visible)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTemp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                WeatherUnitConverter.formatTemp(w.temperature, tempUnit),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    brush = weatherGradient
                                )
                            )
                            Text(
                                "Temperature",
                                style = MaterialTheme.typography.labelSmall,
                                color = textOnScene.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (showCondition) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WeatherConditionImage(
                                code = displayWeatherCode,
                                isNight = displayNight,
                                size = 40.dp
                            )
                            Text(
                                w.weatherDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = textOnScene.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (showHumidity) {
                        w.humidity?.let { hum ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$hum%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.data
                                )
                                Text(
                                    "Humidity",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textOnScene.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // ── Extra metrics row ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Wind
                    if (showWind) {
                        w.windSpeed?.let { wind ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    FieldMindIcons.windIconForSpeed(wind),
                                    null,
                                    tint = colors.warning,
                                    size = 18.dp
                                )
                                Text(
                                    WeatherUnitConverter.formatWind(wind, windSpeedUnit),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.warning
                                )
                                Text(
                                    "Wind",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textOnScene.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    // Cloud cover
                    if (showCloudCover) {
                        w.cloudCover?.let { cloud ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    FieldMindIcons.Cloud,
                                    null,
                                    tint = colors.hypothesis,
                                    size = 18.dp
                                )
                                Text(
                                    "$cloud%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.hypothesis
                                )
                                Text(
                                    "Cloud cover",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textOnScene.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // ── Developer: Test weather conditions ──
                        if (developerMode) {
                            DevWeatherTestPanel(testWeatherCode, testIsNight, { testWeatherCode = it }, { testIsNight = it })
                        }
                    }
                    // Pressure
                    if (showPressure) {
                        w.pressure?.let { press ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    FieldMindIcons.Compress,
                                    null,
                                    tint = colors.project,
                                    size = 18.dp
                                )
                                Text(
                                    "%.0f hPa".format(press),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.project
                                )
                                Text(
                                    "Pressure",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textOnScene.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // ── Sunrise / Sunset ──
                if (sunrise != null || sunset != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        sunrise?.let { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(FieldMindIcons.Sunrise, null, tint = colors.warning, size = 14.dp)
                                Text("Sunrise ${formatTimeFromIso(s)}", style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.7f))
                            }
                        }
                        sunset?.let { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(FieldMindIcons.Sunset, null, tint = colors.data, size = 14.dp)
                                Text("Sunset ${formatTimeFromIso(s)}", style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.7f))
                            }
                        }
                        if (moonPhase.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    moonPhaseIcon(moonPhase),
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 14.dp
                                )
                                Text(moonPhase, style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                // ── 7-Day Forecast (tap to expand) ──
                if (w.dailyForecasts.isNotEmpty()) {
                    var expandedDayIndex by remember { mutableIntStateOf(-1) }
                    val scrollState = rememberScrollState()
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(FieldMindIcons.Calendar, null, tint = textOnScene.copy(alpha = 0.6f), size = 14.dp)
                            Text("7-day forecast — scroll for more", style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
                        }
                        // ── Day tiles row (horizontally scrollable) ──
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            w.dailyForecasts.take(7).forEachIndexed { index, day ->
                                val dayName = if (index == 0) "Today" else {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.add(java.util.Calendar.DAY_OF_MONTH, index)
                                    dayNames[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                                }
                                val isExpanded = expandedDayIndex == index
                                // Compute global min/max across all forecast days for range bar
                                val allTemps = w.dailyForecasts.take(7)
                                val globalMin = allTemps.minOfOrNull { it.temperatureMin } ?: day.temperatureMin
                                val globalMax = allTemps.maxOfOrNull { it.temperatureMax } ?: day.temperatureMax
                                val tempRange = (globalMax - globalMin).coerceAtLeast(1.0)
                                val rangeBarLeft = ((day.temperatureMin - globalMin) / tempRange).toFloat().coerceIn(0f, 1f)
                                val rangeBarRight = ((day.temperatureMax - globalMin) / tempRange).toFloat().coerceIn(0f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .width(76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isExpanded) textOnScene.copy(alpha = 0.14f) else textOnScene.copy(alpha = 0.06f))
                                        .clickable { expandedDayIndex = if (isExpanded) -1 else index }
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                        .animateContentSize()
                                ) {
                                    Text(dayName, style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                    Icon(
                                        weatherConditionIcon(day.weatherCode),
                                        null,
                                        tint = textOnScene.copy(alpha = 0.8f),
                                        size = 18.dp
                                    )
                                    // Temperature range bar — positioned within global min/max
                                    val barWidth = (rangeBarRight - rangeBarLeft).coerceIn(0.03f, 1f)
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(textOnScene.copy(alpha = 0.1f))
                                    ) {
                                        val availableWidth = this.maxWidth
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(availableWidth * barWidth)
                                                .offset(x = availableWidth * rangeBarLeft)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            colors.hypothesis.copy(alpha = 0.65f),
                                                            colors.warning.copy(alpha = 0.65f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    Text(
                                        "%.0f°".format(day.temperatureMax),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textOnScene
                                    )
                                    Text(
                                        "%.0f°".format(day.temperatureMin),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textOnScene.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        // ── Expanded detail card ──
                        AnimatedVisibility(
                            visible = expandedDayIndex in 0..6,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                        ) {
                            // Keep last valid day so content stays rendered during exit animation
                            val lastValidDay = remember { mutableStateOf<fieldmind.research.app.features.field.data.weather.DailyForecast?>(null) }
                            val expandedIdx = expandedDayIndex
                            if (expandedIdx in 0..6) lastValidDay.value = w.dailyForecasts[expandedIdx]
                            val day = lastValidDay.value ?: return@AnimatedVisibility
                                val dayName = if (expandedIdx == 0) "Today" else {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.add(java.util.Calendar.DAY_OF_MONTH, expandedIdx)
                                    dayNames[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                                }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = textOnScene.copy(alpha = 0.08f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(weatherConditionIcon(day.weatherCode), null, tint = textOnScene, size = 22.dp)
                                                Column {
                                                    Text(dayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textOnScene)
                                                    Text(day.weatherDescription, style = MaterialTheme.typography.bodySmall, color = textOnScene.copy(alpha = 0.7f))
                                                }
                                            }
                                            IconButton(onClick = { expandedDayIndex = -1 }, modifier = Modifier.size(28.dp)) {
                                                Icon(FieldMindIcons.Close, null, tint = textOnScene.copy(alpha = 0.5f), size = 16.dp)
                                            }
                                        }
                                        HorizontalDivider(color = textOnScene.copy(alpha = 0.1f))
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            ForecastDetailItem("Hi", "%.0f°".format(day.temperatureMax), textOnScene)
                                            ForecastDetailItem("Lo", "%.0f°".format(day.temperatureMin), textOnScene.copy(alpha = 0.7f))
                                            day.precipitationSum?.let { precip ->
                                                ForecastDetailItem("Rain", "%.0f mm".format(precip), FieldMindTheme.colors.data)
                                            }
                                            day.windSpeedMax?.let { wind ->
                                                ForecastDetailItem("Wind", "%.0f km/h".format(wind), FieldMindTheme.colors.warning)
                                            }
                                            day.humidityMax?.let { hum ->
                                                ForecastDetailItem("Humidity", "$hum%", FieldMindTheme.colors.data)
                                            }
                                            day.apparentTemperature?.let { feels ->
                                                ForecastDetailItem("Feels like", "%.0f°".format(feels), FieldMindTheme.colors.warning)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── Conditions nudge ──
                if (conditionsNudge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.warning.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(FieldMindIcons.Info, null, tint = colors.warning, size = 16.dp)
                            Text(
                                conditionsNudge,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── Weather observation count ──
                val weatherObsCount = observations.count { it.weatherTemperature != null }
                if (weatherObsCount > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$weatherObsCount observation${if (weatherObsCount != 1) "s" else ""} with weather data",
                            style = MaterialTheme.typography.labelSmall,
                            color = textOnScene.copy(alpha = 0.5f)
                        )
                    }
                }
            } else if (weatherError) {
                // Empty state
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Weather,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            size = 32.dp
                        )
                        Text(
                            "Enable GPS for live weather",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap to retry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ForecastDetailItem(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tint)
    }
}

internal fun weatherConditionIcon(code: Int): MaterialSymbolIcon {
    return when (code) {
        0, 1 -> FieldMindIcons.Weather         // Clear / mainly clear
        2 -> FieldMindIcons.Cloud               // Partly cloudy
        3 -> FieldMindIcons.Cloud               // Overcast
        45, 48 -> FieldMindIcons.Foggy          // Fog
        51, 53, 55, 56, 57 -> FieldMindIcons.Rainy  // Drizzle
        61, 63, 65, 66, 67 -> FieldMindIcons.Rainy  // Rain
        71, 73, 75, 77 -> FieldMindIcons.Snowy      // Snow
        80, 81, 82 -> FieldMindIcons.Rainy          // Rain showers
        85, 86 -> FieldMindIcons.Snowy              // Snow showers
        95, 96, 99 -> FieldMindIcons.Thunderstorm   // Thunderstorm
        else -> FieldMindIcons.Weather
    }
}

/**
 * Loads an icons8 PNG image for the given weather condition when available.
 * Falls back to the MaterialSymbolIcon for conditions without PNG assets.
 * Uses 100px PNGs for main display, 50px for compact mode.
 */
@Composable
internal fun WeatherConditionImage(code: Int, isNight: Boolean = false, compact: Boolean = false, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val suffix = if (compact) "50" else "100"
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val isMidnight = hour in 0..4
    val assetPath = remember(code, isNight, isMidnight, compact) {
        when {
            // Fog → dedicated fog icon
            code in 45..48 -> "moon_phases/icons8-fog-$suffix.png"
            // Night clear sky → night scene
            isNight && code <= 1 && isMidnight -> "moon_phases/icons8-midnight-$suffix.png"
            isNight && code <= 1 -> "moon_phases/icons8-night-$suffix.png"
            // Night fog → windy night
            isNight && code in 45..48 -> "moon_phases/icons8-night-wind-$suffix.png"
            // Eclipse → eclipse icon
            code <= 1 && !isNight -> "moon_phases/icons8-eclipse-$suffix.png"
            // Thunderstorm / extreme → mars-rover
            code >= 95 -> "moon_phases/icons8-mars-rover-$suffix.png"
            // Moon symbol fallback for night
            isNight -> "moon_phases/icons8-moon-symbol-$suffix.png"
            else -> null
        }
    }
    val imageBitmap = rememberAssetImage(assetPath)
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Weather condition",
            modifier = Modifier.size(size),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        Icon(weatherConditionIcon(code), null, tint = MaterialTheme.colorScheme.onSurface, size = size)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Research Pulse Card — Animated snapshot of recent activity
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchPulseCard(
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    projects: List<ProjectEntity>,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val colors = FieldMindTheme.colors
    val weekObs = observations.count { it.date >= LocalDate.now().minusDays(7).toString() }
    val openQs = questions.count { it.status != "Answered" }
    val activeProjects = projects.count { it.status == "Active" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.observation.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Streak, null, tint = colors.observation, size = 24.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Research pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Your 7-day research activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onNavigate(FieldMindScreen.Insights) }) { Text("Details") }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PulseMetric(
                    value = "$weekObs",
                    label = "Obs / week",
                    accent = colors.observation,
                    icon = FieldMindIcons.Observation
                )
                PulseMetric(
                    value = "$openQs",
                    label = "Open Qs",
                    accent = colors.question,
                    icon = FieldMindIcons.Question
                )
                PulseMetric(
                    value = "$activeProjects",
                    label = "Projects",
                    accent = colors.project,
                    icon = FieldMindIcons.Project
                )
            }
        }
    }
}

@Composable
private fun PulseMetric(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    icon: MaterialSymbolIcon
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.2f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, size = 22.dp)
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Quick Actions Row
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(onNavigate: (FieldMindScreen) -> Unit) {
    SectionHeader("Quick actions", "Map, Export, Search, Flashcards")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { QuickActionChip("Map", FieldMindIcons.Map, FieldMindTheme.colors.info, FieldMindScreen.MapScreen, onNavigate) }
        item { QuickActionChip("Export", FieldMindIcons.Export, FieldMindTheme.colors.report, FieldMindScreen.ExportStudio, onNavigate) }
        item { QuickActionChip("Search", FieldMindIcons.Search, FieldMindTheme.colors.question, FieldMindScreen.Search, onNavigate) }
        item { QuickActionChip("Review", FieldMindIcons.Flashcard, FieldMindTheme.colors.flashcard, FieldMindScreen.Learn, onNavigate) }
        item { QuickActionChip("Insights", FieldMindIcons.Insights, FieldMindTheme.colors.data, FieldMindScreen.Insights, onNavigate) }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: MaterialSymbolIcon,
    accent: androidx.compose.ui.graphics.Color,
    screen: FieldMindScreen,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        onClick = { haptics.light(); onNavigate(screen) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(36.dp)
                    .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent, size = 20.dp) }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Reading Review Card
// ═════════════════════════════════��════════════════════════════════════

@Composable
private fun ReadingReviewCard(sources: List<SourceEntity>, flashcards: List<FlashcardEntity>, onNavigate: (FieldMindScreen) -> Unit) {
    val current = sources.firstOrNull { it.readingStatus != "Read" } ?: sources.firstOrNull()
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Book, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Reading & review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Continue a source, then turn ideas into memory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniActionTile("Reading", current?.title ?: "Add a source", current?.readingStatus ?: "No source yet", FieldMindIcons.Source, Modifier.weight(1f)) { onNavigate(FieldMindScreen.Library) }
                MiniActionTile("Review", "${flashcards.size} cards", if (flashcards.isEmpty()) "Create cards" else "Start session", FieldMindIcons.Flashcard, Modifier.weight(1f)) { onNavigate(FieldMindScreen.Learn) }
            }
        }
    }
}

@Composable
private fun MiniActionTile(title: String, value: String, subtitle: String, icon: MaterialSymbolIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ObservationTimelinePreview(
    observations: List<ObservationEntity>,
    notes: List<NoteEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    val events = buildList {
        observations.take(8).forEach { add(TimelinePreviewEvent("observation", it.id, it.date, it.time, it.subject.ifBlank { "Observation" }, it.category)) }
        notes.take(4).forEach { note ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(note.updatedAt))
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            add(TimelinePreviewEvent("note", note.id, date, time, note.title.ifBlank { "Untitled note" }, "Journal"))
        }
    }.sortedWith(compareByDescending<TimelinePreviewEvent> { it.date }.thenByDescending { it.time }).take(8)

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Calendar, null, tint = FieldMindTheme.colors.project, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Observation timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Date-grouped research story preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (events.isEmpty()) {
                Text("Your timeline will group observations and notes by day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.groupBy { it.date }.entries.take(3).forEach { (date, dayEvents) ->
                    Text(date, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.project)
                    dayEvents.take(3).forEach { event ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenDetail(event.kind, event.id) }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (event.kind == "note") FieldMindTheme.colors.source else FieldMindTheme.colors.observation))
                            Column(Modifier.weight(1f)) {
                                Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${event.time} • ${event.meta}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TimelinePreviewEvent(val kind: String, val id: Long, val date: String, val time: String, val title: String, val meta: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentProjectResearchCard(
    project: ProjectEntity,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    hypotheses: List<HypothesisEntity>,
    reports: List<ReportEntity>,
    onOpen: () -> Unit
) {
    val projectObservations = observations.filter { it.projectId == project.id }
    val connectedObservations = projectObservations.size
    val evidenceCount = projectObservations.count { it.evidenceSummary.isNotBlank() } + project.attachmentUris.split(",").count { it.trim().isNotBlank() }
    val connectedData = data.count { it.projectId == project.id }
    val connectedSources = sources.count { it.relatedProjectId == project.id }
    val connectedReports = reports.count { it.projectId == project.id }

    Card(onClick = onOpen, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 24.dp)
                Column(Modifier.weight(1f)) {
                    Text("Current project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(project.status, style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.project)
            }
            Text(project.researchQuestion.ifBlank { project.objective.ifBlank { "Open the workspace to define the research question." } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectAssetChip("Observations", connectedObservations, FieldMindIcons.Observation, FieldMindTheme.colors.observation)
                ProjectAssetChip("Evidence", evidenceCount, FieldMindIcons.Camera, FieldMindTheme.colors.observation)
                ProjectAssetChip("Data", connectedData, FieldMindIcons.Data, FieldMindTheme.colors.data)
                ProjectAssetChip("Sources", connectedSources, FieldMindIcons.Source, FieldMindTheme.colors.source)
                ProjectAssetChip("Hypotheses", hypotheses.size, FieldMindIcons.Hypothesis, FieldMindTheme.colors.hypothesis)
                ProjectAssetChip("Reports", connectedReports, FieldMindIcons.Report, FieldMindTheme.colors.report)
            }
        }
    }
}

@Composable
private fun ProjectAssetChip(label: String, count: Int, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.12f)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, size = 14.dp)
        Text("$count $label", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private data class RecentEntry(val kind: String, val id: Long, val time: Long, val title: String, val sub: String, val group: String)

@Composable
private fun RecentActivityGroupCard(group: List<RecentEntry>, onOpenDetail: (String, Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val newest = group.first()
    val more = group.size - 1
    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EntityCard(
            title = newest.title,
            kind = newest.kind,
            body = newest.sub,
            meta = buildList { add(newest.group); add(recentRelativeTime(newest.time)); if (more > 0) add("+$more more ${newest.kind}${if (more == 1) "" else "s"}") },
            onClick = { onOpenDetail(newest.kind, newest.id) }
        )
        if (more > 0) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.End)) {
                Text(if (expanded) "Collapse group" else "Show $more more")
                Spacer(Modifier.size(4.dp))
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, size = 18.dp)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.drop(1).forEach { entry ->
                        EntityCard(entry.title, entry.kind, body = entry.sub, meta = listOf(recentRelativeTime(entry.time))) { onOpenDetail(entry.kind, entry.id) }
                    }
                }
            }
        }
    }
}

internal data class LearnRecommendation(val resource: LearnResource, val path: String)

internal fun recommendedResources(signals: List<String>): List<LearnRecommendation> {
    val all = LearnLibrary.flatMap { c -> c.topics.flatMap { t -> t.resources.map { Triple(c, t, it) } } }
    val words = signals.joinToString(" ").lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 3 }.toSet()
    if (words.isEmpty()) {
        return all.take(3).map { (c, t, r) -> LearnRecommendation(r, "${c.name} · ${t.name}") }
    }
    return all.map { (c, t, r) ->
        val hay = "${c.name} ${c.description} ${t.name} ${t.summary} ${r.title} ${r.why}".lowercase()
        Triple(LearnRecommendation(r, "${c.name} · ${t.name}"), words.count { hay.contains(it) }, c)
    }.sortedByDescending { it.second }
        .let { scored -> if (scored.any { it.second > 0 }) scored.filter { it.second > 0 } else scored }
        .take(3)
        .map { it.first }
}

@Composable
private fun RecommendedLearningCard(items: List<LearnRecommendation>, onOpenReader: (String, String) -> Unit, onSeeAll: () -> Unit) {
    val accent = FieldMindTheme.colors.accentFor("learn")
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { rec ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenReader(rec.resource.url, rec.resource.title) }.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(icon = learnKindIcon(rec.resource.kind), contentDescription = null, tint = accent, size = 18.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(rec.resource.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${rec.resource.kind} · ${rec.path}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(icon = FieldMindIcons.Book, contentDescription = null, tint = accent, size = 18.dp)
                }
            }
            TextButton(onClick = onSeeAll, modifier = Modifier.align(Alignment.End)) { Text("Open Learn"); Spacer(Modifier.size(4.dp)); Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 18.dp) }
        }
    }
}

@Composable
private fun DailyGoalCard(todayCount: Int, goal: Int, streakDays: Int, deltaLabel: String = "", onClick: () -> Unit) {
    val colors = FieldMindTheme.colors
    val complete = todayCount >= goal && goal > 0
    val progress = if (goal > 0) todayCount.toFloat() / goal else 0f
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val remaining = (goal - todayCount).coerceAtLeast(0)
    val ringGradient = if (complete)
        listOf(colors.positive, colors.confidenceSure, MaterialTheme.colorScheme.primary, colors.positive)
    else
        listOf(MaterialTheme.colorScheme.primary, colors.data, colors.hypothesis, MaterialTheme.colorScheme.primary)
    val bg = Brush.linearGradient(
        if (complete) listOf(MaterialTheme.colorScheme.primaryContainer, colors.confidenceSure.copy(alpha = 0.30f))
        else listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GradientProgressRing(
                progress = progress,
                centerValue = "$todayCount/$goal",
                caption = "$percent%",
                gradient = ringGradient,
                size = 104.dp
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (complete) "Daily goal met" else "Today's observations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (complete) "Great discipline — keep the streak going." else "$remaining more to hit today's goal of $goal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalStatChip(FieldMindIcons.Streak, "$streakDays day${if (streakDays == 1) "" else "s"} streak", colors.warning)
                    GoalStatChip(if (complete) FieldMindIcons.Check else FieldMindIcons.Observation, if (complete) "Done" else "$todayCount logged", if (complete) colors.positive else colors.observation)
                }
                if (deltaLabel.isNotBlank()) {
                    Text(
                        deltaLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.observation,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchSessionCtaCard(
    lastSessionLabel: String?,
    activeSessionName: String?,
    timerMs: Long,
    onStartSession: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    val isActive = activeSessionName != null
    
    fun formatTimer(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(FieldMindIcons.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 26.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isActive) "Live Session Active" else "Research Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    when {
                        isActive -> "${activeSessionName ?: "Untitled"} • ${formatTimer(timerMs)}"
                        lastSessionLabel != null -> lastSessionLabel
                        else -> "Structured capture with timer, live feed, and summary"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
            Button(
                onClick = { haptics.confirm(); onStartSession() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(if (isActive) FieldMindIcons.Capture else FieldMindIcons.Add, null, size = 18.dp)
                Spacer(Modifier.size(4.dp))
                Text(if (isActive) "Continue" else "Start")
            }
        }
    }
}

@Composable
fun GoalStatChip(icon: MaterialSymbolIcon, label: String, tint: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = if (FieldMindTheme.colors.isDark) 0.20f else 0.13f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon = icon, contentDescription = null, tint = tint, size = 14.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeWidgetGrid(
    observations: List<ObservationEntity>,
    notes: List<NoteEntity>,
    questions: List<QuestionEntity>,
    sources: List<SourceEntity>,
    projects: List<ProjectEntity>,
    reports: List<ReportEntity>,
    data: List<DataRecordEntity>,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val widgets = listOf(
        HomeWidget("Observations", "${observations.size} captured", FieldMindIcons.Observation, FieldMindTheme.colors.observation, FieldMindScreen.Insights),
        HomeWidget("Questions", "${questions.count { it.status != "Answered" }} open", FieldMindIcons.Question, FieldMindTheme.colors.question, FieldMindScreen.Questions),
        HomeWidget("Sources", "${sources.count { it.readingStatus == "Read" }}/${sources.size} read", FieldMindIcons.Source, FieldMindTheme.colors.source, FieldMindScreen.Library),
        HomeWidget("Projects", "${projects.count { it.status == "Active" }} active", FieldMindIcons.Project, FieldMindTheme.colors.project, FieldMindScreen.Projects),
        HomeWidget("Data", "${data.size} records", FieldMindIcons.Data, FieldMindTheme.colors.data, FieldMindScreen.DataTools),
        HomeWidget("Reports", "${reports.size} drafts", FieldMindIcons.Report, FieldMindTheme.colors.report, FieldMindScreen.Reports)
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
        widgets.forEach { widget -> HomeWidgetCard(widget, Modifier.weight(1f)) { onNavigate(widget.screen) } }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeDataOptionsCard(data: List<DataRecordEntity>, onNavigate: (FieldMindScreen) -> Unit) {
    val toolScreens = listOf(
        Pair(Triple("Count", "Track totals", FieldMindIcons.Add), FieldMindScreen.CounterTool),
        Pair(Triple("Measure", "Log values", FieldMindIcons.Graph), FieldMindScreen.MeasurementTool),
        Pair(Triple("Weather", "Conditions", FieldMindIcons.Weather), FieldMindScreen.WeatherLogTool),
        Pair(Triple("Species", "Survey data", FieldMindIcons.Nature), FieldMindScreen.SpeciesTool)
    )
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Data, null, tint = FieldMindTheme.colors.data, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Data tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${data.size} records • choose what you are tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onNavigate(FieldMindScreen.DataTools) }) { Text("Open all") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                toolScreens.forEach { (info, screen) ->
                    val (title, body, icon) = info
                    Surface(onClick = { onNavigate(screen) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(icon, null, tint = FieldMindTheme.colors.data, size = 20.dp)
                            Column { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeWidget(val title: String, val value: String, val icon: MaterialSymbolIcon, val color: androidx.compose.ui.graphics.Color, val screen: FieldMindScreen)

@Composable
private fun HomeWidgetCard(widget: HomeWidget, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(modifier = modifier.height(112.dp).clickable { haptics.light(); onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(widget.icon, null, tint = widget.color, size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(widget.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(widget.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Session Observations — Grouped by research session
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionObservationsCard(
    sessionObs: Map<String, List<ObservationEntity>>,
    researchSessions: List<ResearchSessionEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val colors = FieldMindTheme.colors
    var expandedSessions by remember { mutableStateOf<Set<String>>(emptySet()) }
    val totalSessionObs = sessionObs.values.sumOf { it.size }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(colors.observation.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Session, null, tint = colors.observation, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Session observations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$totalSessionObs observation${if (totalSessionObs != 1) "s" else ""} across ${sessionObs.size} session${if (sessionObs.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onNavigate(FieldMindScreen.ResearchSession) }) {
                    Text("New session")
                }
            }

            // Session groups
            sessionObs.entries.take(5).forEach { (sessionName, obs) ->
                val session = researchSessions.firstOrNull {
                    sessionName.contains(it.name, ignoreCase = true) ||
                    it.name.contains(sessionName, ignoreCase = true)
                }
                val isExpanded = sessionName in expandedSessions
                val dateLabel = obs.firstOrNull()?.let {
                    try {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date) ?: Date())
                    } catch (_: Exception) { it.date }
                } ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize().clickable {
                        expandedSessions = if (isExpanded) expandedSessions - sessionName
                        else expandedSessions + sessionName
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Session header with stats
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                FieldMindIcons.Session,
                                null,
                                tint = colors.observation,
                                size = 18.dp
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sessionName.removePrefix("Research session: "),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "${obs.size} observation${if (obs.size != 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (dateLabel.isNotBlank()) {
                                        Text(
                                            dateLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Icon(
                                if (isExpanded) FieldMindIcons.Up else FieldMindIcons.Down,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp
                            )
                        }

                        // Session duration from the session entity
                        session?.let { s ->
                            if (s.totalDurationMs > 0) {
                                val dur = s.totalDurationMs / 1000
                                val hrs = dur / 3600
                                val min = (dur % 3600) / 60
                                Text(
                                    "Duration: ${if (hrs > 0) "${hrs}h ${min}m" else "${min}m"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Observation list (expandable)
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                obs.forEach { observation ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenDetail("observation", observation.id) }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(6.dp).clip(CircleShape)
                                                .background(colors.observation)
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                observation.subject.ifBlank { observation.category },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                InfoChip(
                                                    observation.category,
                                                    icon = FieldMindIcons.iconForCategory(observation.category)
                                                )
                                                Text(
                                                    observation.time.takeIf { it.isNotBlank() } ?: "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            FieldMindIcons.Forward,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            size = 16.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show more indicator
            if (sessionObs.size > 5) {
                Text(
                    "+${sessionObs.size - 5} more session${if (sessionObs.size - 5 != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Moon Phase Calculation — Based on the age of the lunar cycle
// ══════════════════════════════════════════════════════════════════════

@Composable
internal fun DevWeatherTestPanel(
    testCode: Int?,
    testNight: Boolean,
    onCodeChange: (Int?) -> Unit,
    onNightChange: (Boolean) -> Unit
) {
    val colors = FieldMindTheme.colors
    val weatherCodes = listOf(
        0 to "Clear", 1 to "Mainly Clear", 2 to "Cloudy", 3 to "Overcast",
        45 to "Fog", 48 to "Rime Fog", 51 to "Drizzle", 61 to "Rain",
        71 to "Snow", 80 to "Rain Showers", 85 to "Snow Showers", 95 to "Thunderstorm", 99 to "Severe TS"
    )

    // Night-specific preset codes
    val nightCodes = listOf(
        0 to "Night Clear", 45 to "Night Fog", 95 to "Night Storm"
    )
    val currentLabel = if (testNight && testCode != null) {
        nightCodes.firstOrNull { it.first == testCode }?.second ?: "Custom ($testCode)"
    } else if (testCode != null) {
        weatherCodes.firstOrNull { it.first == testCode }?.second ?: "Custom ($testCode)"
    } else {
        "Live (${testCode ?: "-"})"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(FieldMindIcons.Sparkle, null, tint = colors.info, size = 16.dp)
                Text(
                    "Dev: Test conditions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.info
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (testCode != null) "Override: $currentLabel" else "Using live data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weather condition chips
            Text("Weather codes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(weatherCodes) { (code, label) ->
                    FilterChip(
                        selected = testCode == code && !testNight,
                        onClick = {
                            if (testCode == code && !testNight) {
                                onCodeChange(null)
                            } else {
                                onCodeChange(code)
                                onNightChange(false)
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (testCode == code && !testNight) {{ Icon(FieldMindIcons.Check, null, size = 14.dp) }} else null
                    )
                }
            }

            // Night mode toggle
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = testNight,
                    onClick = {
                        onNightChange(!testNight)
                        if (testCode == null) onCodeChange(0)
                    },
                    label = { Text("Night mode", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (testNight) {{ Icon(FieldMindIcons.MoonNew, null, size = 14.dp) }} else null
                )
                // Reset button
                TextButton(onClick = { onCodeChange(null); onNightChange(false) }) {
                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Show preview of the selected icons8 PNG
            if (testCode != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeatherConditionImage(code = testCode, isNight = testNight, compact = true, size = 32.dp)
                    val todayDate = remember { LocalDate.now() }; Icon(moonPhaseIcon(getMoonPhase(todayDate)), null, tint = Color.White, size = 24.dp)
                    Column {
                        Text("Icon preview (${testCode})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Night: ${if (testNight) "ON" else "OFF"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun getMoonPhase(date: LocalDate): String {
    // Calculate approximate moon phase using the Julian day of the reference new moon
    // Reference: 2000-01-06 18:14 UTC was a new moon
    val knownNewMoon = LocalDate.of(2000, 1, 6)
    val daysSince = ChronoUnit.DAYS.between(knownNewMoon, date).toDouble()
    val lunations = daysSince / 29.53058770576
    val phase = lunations - floor(lunations)
    return when {
        phase < 0.03 || phase >= 0.97 -> "New moon"
        phase < 0.20 -> "Waxing crescent"
        phase < 0.28 -> "First quarter"
        phase < 0.45 -> "Waxing gibbous"
        phase < 0.53 -> "Full moon"
        phase < 0.68 -> "Waning gibbous"
        phase < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }
}

private fun moonPhaseIcon(phase: String): MaterialSymbolIcon = when {
    phase.startsWith("New") -> FieldMindIcons.MoonNew
    phase.startsWith("Waxing crescent") || phase.startsWith("Waning crescent") -> FieldMindIcons.MoonCrescent
    phase.startsWith("First") || phase.startsWith("Last") -> FieldMindIcons.MoonQuarter
    phase.startsWith("Waxing gibbous") || phase.startsWith("Waning gibbous") -> FieldMindIcons.MoonGibbous
    phase.startsWith("Full") -> FieldMindIcons.MoonFull
    else -> FieldMindIcons.MoonNew
}

internal fun formatTimeFromIso(isoString: String): String {
    // Input format: "2026-06-15T05:03" or "2026-06-15T18:27"
    return try {
        // Extract time portion (after T) and take just the HH:mm part
        val tIndex = isoString.indexOf('T')
        if (tIndex >= 0 && tIndex + 6 <= isoString.length) {
            isoString.substring(tIndex + 1, tIndex + 6)
        } else {
            isoString.takeLast(5)
        }
    } catch (_: Exception) {
        isoString
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Fieldwork Conditions Nudge — Based on weather thresholds
// ══════════════════════════════════════════════════════════════════════

private fun computeFieldworkNudge(weather: WeatherSnapshot): String {
    val temp = weather.temperature
    val wind = weather.windSpeed
    val code = weather.weatherCode
    val parts = mutableListOf<String>()

    // Temperature nudges
    if (temp != null) {
        when {
            temp > 38 -> parts.add("Extreme heat — postpone fieldwork if possible")
            temp > 32 -> parts.add("Very hot — take frequent breaks and hydrate")
            temp < -10 -> parts.add("Dangerous cold — limit outdoor exposure")
            temp < 0 -> parts.add("Freezing — watch for ice on equipment")
            temp in 0.0..3.0 -> parts.add("Near-freezing — layer up and keep batteries warm")
        }
    }

    // Wind nudges
    if (wind != null) {
        when {
            wind > 60 -> parts.add("Gale-force winds — avoid open areas")
            wind > 40 -> parts.add("Very windy — secure loose gear")
            wind > 25 -> parts.add("Windy — consider sheltered transects")
        }
    }

    // Precipitation nudges
    when {
        code in 51..67 -> parts.add("Rain likely — bring waterproof gear")
        code in 71..86 -> parts.add("Snow — tread carefully, watch visibility")
        code >= 95 -> parts.add("Thunderstorms — seek shelter immediately")
        code in 45..48 -> parts.add("Fog — reduced visibility, use caution")
    }

    return parts.firstOrNull() ?: ""
}

// ══════════════════════════════════════════════════════════════════════
//  Recent Captures Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RecentCapturesCard(observations: List<ObservationEntity>, onOpenDetail: (String, Long) -> Unit) {
    if (observations.isEmpty()) return
    val recentObs = observations.sortedByDescending { it.timestamp }.take(3)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent captures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentObs.forEach { obs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenDetail("observation", obs.id) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FieldMindIcons.Observation,
                            null,
                            tint = FieldMindTheme.colors.observation,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                obs.subject.takeIf { it.isNotBlank() } ?: obs.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    obs.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (obs.latitude != null && obs.longitude != null) {
                                    Text("📍", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    obs.time.takeIf { it.isNotBlank() } ?: "–",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════
//  Expand Dashboard Helper Composables
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ExpandMetric(
    value: String,
    label: String,
    icon: MaterialSymbolIcon,
    accent: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = accent, size = 24.dp)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ExpandInfoChip(
    icon: MaterialSymbolIcon,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), size = 16.dp)
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
