package fieldmind.research.app.features.field.presentation.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.CapturedLocation
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.features.field.presentation.components.FieldMindCameraV2
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.data.vision.SpeciesClassifier
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesImageAnalyzer
import fieldmind.research.app.features.field.data.vision.SpeciesMatch
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import fieldmind.research.app.features.field.presentation.screens.species.SpeciesIdentificationSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ══════════════════════════════════════════════════════════════════════
//  Research Session — Timer-based multi-observation mode
// ══════════════════════════════════════════════════════════════════════

/**
 * A research session lets the user capture multiple observations in rapid succession
 * with a running timer. Each observation is auto-timestamped and linked to the session.
 *
 * Flow:
 * 1. Start session → optional name, linked project
 * 2. Timer runs → add observations rapidly (subject + facts only)
 * 3. Session summary at end: all observations, time spent, photos taken
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearchSessionScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val haptics = rememberFieldMindHaptics()
    // Use centralized snackbar system (swipeable, auto-dismiss, no stacking)
    val snackbarHelper = rememberFieldMindSnackbar()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationProvider = remember { FieldLocationProvider(context) }
    val projects by viewModel.projects.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val activeStoredSession = remember(researchSessions) { researchSessions.firstOrNull { it.status == "Active" } }

    // Session state
    var sessionActive by remember { mutableStateOf(activeStoredSession != null) }
    var sessionCreating by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var sessionElapsedMs by remember { mutableLongStateOf(0L) }
    var sessionPaused by remember { mutableStateOf(false) }
    var pausedAccumulatedMs by remember { mutableLongStateOf(0L) }
    var observationCount by remember { mutableIntStateOf(0) }
    var sessionStartedAt by remember { mutableLongStateOf(activeStoredSession?.startedAt ?: 0L) }
    var activeSessionId by remember { mutableStateOf<Long?>(activeStoredSession?.id) }
    var showSummary by remember { mutableStateOf(false) }

    LaunchedEffect(activeStoredSession?.id) {
        activeStoredSession?.let { stored ->
            activeSessionId = stored.id
            sessionActive = true
            sessionName = stored.name
            selectedProjectId = stored.projectId
            sessionStartedAt = stored.startedAt
            observationCount = maxOf(observationCount, stored.observationCount)
        }
    }

    // Quick observation input
    var quickSubject by remember { mutableStateOf("") }
    var quickFacts by remember { mutableStateOf("") }
    var quickCategory by remember { mutableStateOf("Other") }
    var quickAttachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    var quickLocation by remember { mutableStateOf<CapturedLocation?>(null) }
    var quickPlaceName by remember { mutableStateOf("") }
    var quickWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var captureStatus by remember { mutableStateOf("Ready") }

    // ── Metadata auto-fetch confirmation ──
    var showMetadataConfirm by remember { mutableStateOf(false) }
    var metadataAutoFetching by remember { mutableStateOf(false) }
    var metadataStatus by remember { mutableStateOf("Ready") }

    // ── Species identification state ──
    val speciesDatabase = remember { SpeciesDatabase(context) }
    val speciesImageAnalyzer = remember { SpeciesImageAnalyzer(context) }
    val speciesClassifier = remember { SpeciesClassifier(context, speciesDatabase, speciesImageAnalyzer) }
    var showSpeciesSearch by remember { mutableStateOf(false) }
    var speciesIdImageUri by remember { mutableStateOf<String?>(null) }
    var identifiedSpecies by remember { mutableStateOf<SpeciesMatch?>(null) }

    // ── Advanced fields state ──
    var speciesName by remember { mutableStateOf("") }
    var speciesConfidence by remember { mutableStateOf("") }
    var quickTags by remember { mutableStateOf("") }
    var behavior by remember { mutableStateOf("") }
    var lifeStage by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var habitatType by remember { mutableStateOf("") }
    var weatherOverride by remember { mutableStateOf("") }

    var showAdvanced by remember { mutableStateOf(false) }

    // ── Species record lookup (for inline info card + detail sheet) ──
    var selectedSpeciesRecord by remember { mutableStateOf<SpeciesRecord?>(null) }
    var showTaxonomy by remember { mutableStateOf(false) }
    var showSpeciesDetail by remember { mutableStateOf(false) }

    // ── In-app camera & audio recording state ──
    var showInAppCamera by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    fun addSessionAttachment(attachment: DraftEvidenceAttachment) {
        quickAttachments = quickAttachments + attachment
        snackbarHelper.show("${attachment.type} ready for next observation")
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        uris.forEach { uri -> addSessionAttachment(DraftEvidenceAttachment("Gallery", uri.toString(), "Session media")) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addSessionAttachment(DraftEvidenceAttachment("File", it.toString(), "Session attachment"))
        }
    }

    // Audio recording launcher (requests permission, starts recording)
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createFieldMindFile(context, "session_audio", ".m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            runCatching {
                newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                newRecorder.setOutputFile(file.absolutePath)
                newRecorder.prepare()
                newRecorder.start()
                audioFile = file
                recorder = newRecorder
                recording = true
            }.onFailure {
                newRecorder.release()
                snackbarHelper.showError("Could not start recording: ${it.localizedMessage}")
            }
        } else {
            snackbarHelper.showError("Audio permission denied.")
        }
    }

    // Recording timer
    LaunchedEffect(recording) {
        if (recording) {
            recordSeconds = 0
            while (recording) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    fun fetchSessionLocation(fetchWeather: Boolean = false) {
        captureStatus = "Fetching GPS…"
        locationProvider.requestCurrentLocation { captured ->
            quickLocation = captured
            if (captured == null) {
                captureStatus = "GPS unavailable or permission missing"
                snackbarHelper.show(captureStatus)
                return@requestCurrentLocation
            }
            captureStatus = captured.asDisplayText()
            locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                quickPlaceName = place.orEmpty()
            }
            if (fetchWeather) {
                scope.launch {
                    quickWeather = viewModel.fetchWeatherSnapshot(captured.latitude, captured.longitude)
                    captureStatus = quickWeather?.asDisplayText() ?: "Weather unavailable"
                }
            }
        }
    }

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    // Timer with pause/resume support
    LaunchedEffect(sessionActive, sessionPaused) {
        if (sessionActive && !sessionPaused) {
            if (sessionStartedAt == 0L) {
                sessionStartedAt = System.currentTimeMillis()
            }
            val baseStart = sessionStartedAt
            while (sessionActive && !sessionPaused) {
                delay(1000)
                sessionElapsedMs = pausedAccumulatedMs + (System.currentTimeMillis() - baseStart)
                val label = if (sessionPaused) "Paused" else "Running"
                showResearchSessionNotification(context, sessionName.ifBlank { "Research Session" }, "$label • ${formatTime(sessionElapsedMs)} • $observationCount obs")
            }
        }
    }

    // ── Look up full species record when speciesName changes ──
    LaunchedEffect(speciesName) {
        if (speciesName.isNotBlank() && speciesName != selectedSpeciesRecord?.commonName) {
            val results = speciesDatabase.search(speciesName, limit = 5)
            selectedSpeciesRecord = results.firstOrNull {
                it.commonName.equals(speciesName, ignoreCase = true) ||
                    it.scientificName.equals(speciesName, ignoreCase = true)
            }
        } else if (speciesName.isBlank()) {
            selectedSpeciesRecord = null
            showTaxonomy = false
        }
    }

    fun smartSessionName(): String {
        val project = projects.firstOrNull { it.id == selectedProjectId }
        val categoryHint = quickCategory.takeIf { it != "Other" } ?: project?.topicType?.takeIf { it.isNotBlank() }
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        return listOfNotNull(categoryHint, project?.title).joinToString(" • ").ifBlank { "Field session" } + " · $time"
    }

    fun performAutoFetch() {
        metadataAutoFetching = true
        metadataStatus = "Acquiring GPS…"
        locationProvider.requestCurrentLocation { captured ->
            quickLocation = captured
            if (captured == null) {
                metadataStatus = "GPS unavailable — check permissions"
                metadataAutoFetching = false
                snackbarHelper.show(metadataStatus)
                return@requestCurrentLocation
            }
            metadataStatus = "GPS acquired — fetching weather…"
            captureStatus = captured.asDisplayText()
            locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                quickPlaceName = place.orEmpty()
            }
            scope.launch {
                val snapshot = viewModel.fetchWeatherSnapshot(captured.latitude, captured.longitude)
                quickWeather = snapshot
                if (snapshot != null) {
                    viewModel.saveWeatherSnapshot(snapshot, captured.latitude, captured.longitude)
                    metadataStatus = "GPS + Weather acquired"
                    snackbarHelper.show("Weather: ${snapshot.temperature}°C, ${snapshot.weatherDescription}")
                } else {
                    metadataStatus = "GPS acquired, weather unavailable"
                }
                metadataAutoFetching = false
            }
        }
    }

    fun startSession() {
        val name = sessionName.ifBlank { smartSessionName() }
        sessionCreating = true
        sessionStartedAt = System.currentTimeMillis()
        sessionElapsedMs = 0
        observationCount = 0
        sessionName = name
        viewModel.addResearchSession(name, selectedProjectId) { id ->
            activeSessionId = id
            sessionActive = true
            sessionCreating = false
        }
        showResearchSessionNotification(context, name, "Research session is running")
        haptics.confirm()
        // Show metadata auto-fetch confirmation
        showMetadataConfirm = true
    }

    fun endSession() {
        sessionActive = false
        val sessionId = activeSessionId
        if (sessionId != null) {
            viewModel.endResearchSession(sessionId, observationCount, sessionElapsedMs)
        }
        cancelResearchSessionNotification(context)
        showSummary = true
        haptics.confirm()
    }

    fun saveQuickObservation() {
        if (quickSubject.isBlank() && quickFacts.isBlank()) return
        val structuredJson = org.json.JSONObject().apply {
            if (speciesName.isNotBlank()) put("speciesName", speciesName)
            if (speciesConfidence.isNotBlank()) put("speciesConfidence", speciesConfidence)
            if (behavior.isNotBlank()) put("behavior", behavior)
            if (lifeStage.isNotBlank()) put("lifeStage", lifeStage)
            if (sex.isNotBlank()) put("sex", sex)
            if (habitatType.isNotBlank()) put("habitatType", habitatType)
            if (weatherOverride.isNotBlank()) put("weatherOverride", weatherOverride)
        }.toString()
        val tagsStr = listOfNotNull(
            "research-session",
            quickTags.takeIf { it.isNotBlank() }
        ).joinToString(",")
        viewModel.addObservation(
            subject = quickSubject.ifBlank { quickCategory },
            category = quickCategory,
            facts = quickFacts.ifBlank { "Quick session observation" },
            confidence = defaultConfidence,
            manualLocation = quickPlaceName.ifBlank { quickLocation?.coordinateText().orEmpty() },
            tags = tagsStr,
            evidence = quickAttachments.joinToString { it.type },
            context = "Research session: $sessionName",
            projectId = selectedProjectId,
            latitude = quickLocation?.latitude,
            longitude = quickLocation?.longitude,
            attachments = quickAttachments,
            weather = quickWeather,
            structuredDetailsJson = structuredJson,
            startedAt = sessionStartedAt.takeIf { it > 0L },
            endedAt = System.currentTimeMillis(),
            durationMs = sessionElapsedMs,
            timeNote = "Captured at ${formatTime(sessionElapsedMs)} in $sessionName"
        ) { observationId ->
            activeSessionId?.let { viewModel.linkObservationToSession(it, observationId) }
            observationCount++
            quickSubject = ""
            quickFacts = ""
            quickAttachments = emptyList()
            snackbarHelper.showWithAction("Observation #$observationCount saved", "Undo") {
                viewModel.archiveObservation(observationId)
            }
        }
    }

    // ── Back handler with Save & Exit confirmation ──
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (sessionActive) {
            showExitConfirm = true
        } else {
            onBack()
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            icon = { Icon(icon = FieldMindIcons.Bolt, contentDescription = null, size = 28.dp) },
            title = { Text("Active research session") },
            text = {
                Text(
                    "You have an active session with $observationCount observation${if (observationCount != 1) "s" else ""} and ${formatTime(sessionElapsedMs)} elapsed. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        endSession()
                        onBack()
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Save and exit") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        sessionActive = false
                        activeSessionId?.let { viewModel.endResearchSession(it, observationCount, sessionElapsedMs) }
                        cancelResearchSessionNotification(context)
                        showExitConfirm = false
                        onBack()
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showExitConfirm = false }) {
                        Text("Keep editing")
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FieldScreenHeader(
                    "Research Session",
                    "Capture multiple observations with a running timer.",
                    icon = FieldMindIcons.Bolt,
                    actionIcon = FieldMindIcons.Back,
                    onAction = {
                        if (sessionActive) {
                            showExitConfirm = true
                        } else {
                            onBack()
                        }
                    }
                )
            }

            if (showSummary) {
                // Session summary
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Session Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MetricTile("Duration", formatTime(sessionElapsedMs), FieldMindIcons.Calendar, Modifier.weight(1f))
                                MetricTile("Observations", "$observationCount", FieldMindIcons.Observation, Modifier.weight(1f))
                            }
                            if (sessionName.isNotBlank()) {
                                Text("Session: $sessionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Button(onClick = { showSummary = false; sessionElapsedMs = 0; observationCount = 0; activeSessionId = null; sessionStartedAt = 0L }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Start new session")
                            }
                        }
                    }
                }
            } else if (sessionCreating) {
                // Loading while session is being created
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                "Starting session…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Initializing research session",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (!sessionActive) {
                // Session setup
                item {
                    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Start a research session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Capture observations rapidly while the timer tracks your field time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FieldTextField(sessionName, { sessionName = it }, "Session name (optional)", supportingText = "Auto-named if left blank")
                            if (projects.isNotEmpty()) {
                                Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OptionPickerField(label = "Project", selected = projects.firstOrNull { it.id == selectedProjectId }?.title ?: "No project", options = listOf("No project") + projects.map { it.title }, onSelected = { selected -> selectedProjectId = projects.firstOrNull { it.title == selected }?.id }, icon = FieldMindIcons.Project, modifier = Modifier.fillMaxWidth())
                            }
                            Button(onClick = ::startSession, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Icon(FieldMindIcons.Bolt, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Start session")
                            }
                        }
                    }
                }
            } else {
                // Active session — timer card with pause/end
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Pulsing timer dot
                            val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
                            val pulseAlpha by infiniteTransition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")
                            Box(Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha)))
                            Column(Modifier.weight(1f)) {
                                Text(sessionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(formatTime(sessionElapsedMs), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("$observationCount observation${if (observationCount != 1) "s" else ""} captured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                            }
                            // Pause / End controls
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (sessionPaused) {
                                            // Resume: restart from accumulated time
                                            pausedAccumulatedMs = sessionElapsedMs
                                            sessionStartedAt = System.currentTimeMillis()
                                            sessionPaused = false
                                        } else {
                                            // Pause: capture accumulated time
                                            pausedAccumulatedMs = sessionElapsedMs
                                            sessionPaused = true
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                    )
                                ) {
                                    Icon(
                                        if (sessionPaused) MaterialSymbolIcon("play_arrow", filled = true) else FieldMindIcons.Pause,
                                        null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        size = 20.dp
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = ::endSession,
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Icon(FieldMindIcons.Stop, null, tint = MaterialTheme.colorScheme.error, size = 20.dp)
                                }
                            }
                        }
                        Text(captureStatus + if (quickAttachments.isNotEmpty()) " • ${quickAttachments.size} attachment${if (quickAttachments.size == 1) "" else "s"}" else "", modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                    }
                }

                // ── Metadata Auto-Fetch Confirmation Card ──
                if (showMetadataConfirm) {
                    item {
                        SessionMetadataConfirmCard(
                            onConfirm = {
                                showMetadataConfirm = false
                                performAutoFetch()
                            },
                            onSkip = {
                                showMetadataConfirm = false
                                metadataStatus = "Skipped — use GPS/Weather buttons"
                            }
                        )
                    }
                }

                // ── Location & Weather Status (MOVED TO TOP) ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                Column(Modifier.weight(1f)) {
                                    Text("Location & weather", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (metadataAutoFetching) {
                                        Text(metadataStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (quickLocation == null && !metadataAutoFetching) {
                                    Surface(
                                        onClick = { performAutoFetch() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = FieldMindTheme.colors.info.copy(alpha = 0.12f)
                                    ) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(FieldMindIcons.Weather, null, tint = FieldMindTheme.colors.info, size = 16.dp)
                                            Text("Fetch all", style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.info, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SessionMetaChip(
                                    label = "GPS",
                                    acquired = quickLocation != null,
                                    detail = quickLocation?.let { "${it.accuracyMeters?.toInt() ?: "?"}m" } ?: "Tap to fetch",
                                    icon = FieldMindIcons.Location,
                                    accent = if (quickLocation != null) FieldMindTheme.colors.positive else FieldMindTheme.colors.warning,
                                    onTap = if (quickLocation == null && !metadataAutoFetching) { { fetchSessionLocation(fetchWeather = false) } } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                SessionMetaChip(
                                    label = "Weather",
                                    acquired = quickWeather != null,
                                    detail = quickWeather?.let { "${it.temperature}°C" } ?: "Tap to fetch",
                                    icon = FieldMindIcons.Weather,
                                    accent = if (quickWeather != null) FieldMindTheme.colors.positive else FieldMindTheme.colors.warning,
                                    onTap = if (quickLocation != null && quickWeather == null && !metadataAutoFetching) {
                                        { fetchSessionLocation(fetchWeather = true) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                SessionMetaChip(
                                    label = "Time",
                                    acquired = true,
                                    icon = FieldMindIcons.Calendar,
                                    accent = FieldMindTheme.colors.positive,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // ── Evidence Tools — separate box below metadata ──
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(FieldMindIcons.Bolt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                Text("Evidence tools", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Row 1: Camera, Gallery, Attach
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SessionEvidenceButton(
                                    onClick = { showInAppCamera = true },
                                    icon = FieldMindIcons.Camera,
                                    label = "Camera",
                                    modifier = Modifier.weight(1f)
                                )
                                SessionEvidenceButton(
                                    onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    icon = FieldMindIcons.Gallery,
                                    label = "Gallery",
                                    modifier = Modifier.weight(1f)
                                )
                                SessionEvidenceButton(
                                    onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) },
                                    icon = FieldMindIcons.File,
                                    label = "Attach",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Row 2: Audio only (GPS/Weather moved to metadata card)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SessionEvidenceButton(
                                    onClick = {
                                        if (recording) {
                                            val file = audioFile
                                            runCatching { recorder?.stop() }
                                            recorder?.release()
                                            recorder = null
                                            recording = false
                                            file?.let {
                                                addSessionAttachment(DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4"))
                                            }
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    icon = if (recording) FieldMindIcons.Stop else FieldMindIcons.Mic,
                                    label = if (recording) "Stop" else "Audio",
                                    accent = if (recording) MaterialTheme.colorScheme.error else FieldMindTheme.colors.observation,
                                    modifier = Modifier.weight(1f)
                                )
                                SessionEvidenceButton(
                                    onClick = { fetchSessionLocation(fetchWeather = false) },
                                    icon = FieldMindIcons.Location,
                                    label = "GPS",
                                    modifier = Modifier.weight(1f)
                                )
                                SessionEvidenceButton(
                                    onClick = { fetchSessionLocation(fetchWeather = true) },
                                    icon = FieldMindIcons.Weather,
                                    label = "Weather",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Recording indicator
                            if (recording) {
                                RecordingIndicator(recordSeconds)
                            }
                        }
                    }
                }

                // Quick observation form
                item {
                    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Quick observation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Type what you see. Tap save. Repeat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OptionPickerField(label = "Category", selected = quickCategory, options = observationCategories, onSelected = { quickCategory = it }, icon = FieldMindIcons.Category)
                            FieldTextField(quickSubject, { quickSubject = it }, "Subject", supportingText = "e.g. Crow on wire")
                            FieldTextField(quickFacts, { quickFacts = it }, "Facts", minLines = 2, supportingText = "What did you observe?")
                            // ── Advanced details collapsible ──
                            TextButton(
                                onClick = { showAdvanced = !showAdvanced },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                        Text("Advanced details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(if (showAdvanced) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                }
                            }
                            AnimatedVisibility(visible = showAdvanced) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    // Species name + search button
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FieldTextField(speciesName, { speciesName = it }, "Species name",
                                            modifier = Modifier.weight(1f),
                                            supportingText = "Common or scientific name")
                                        IconButton(
                                            onClick = { speciesIdImageUri = null; showSpeciesSearch = true },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                                        }
                                    }

                                    // ── Selected species info card ──
                                    val curRecord = selectedSpeciesRecord
                                    AnimatedVisibility(visible = curRecord != null && speciesName.isNotBlank()) {
                                        curRecord?.let { record ->
                                            SpeciesInfoCard(
                                                record = record,
                                                showTaxonomy = showTaxonomy,
                                                onToggleTaxonomy = { showTaxonomy = !showTaxonomy },
                                                onOpenDetail = { showSpeciesDetail = true }
                                            )
                                        }
                                    }
                                    FieldTextField(speciesConfidence, { speciesConfidence = it }, "Species confidence", supportingText = "e.g. High, 85%")
                                    FieldTextField(quickTags, { quickTags = it }, "Tags", supportingText = "Comma-separated keywords")

                                    // Behavior
                                    Text("Behavior", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OptionPickerField(label = "Behavior", selected = behavior, options = listOf("Foraging", "Resting", "Flying", "Hunting", "Socializing", "Mating", "Nesting", "Moving", "Calling", "Feeding", "Other"), onSelected = { behavior = it }, icon = FieldMindIcons.Trend, modifier = Modifier.fillMaxWidth())

                                    // Life Stage
                                    Text("Life stage", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OptionPickerField(label = "Life stage", selected = lifeStage, options = listOf("Adult", "Juvenile", "Larva", "Pupa", "Nymph", "Egg", "Fledgling", "Subadult"), onSelected = { lifeStage = it }, icon = FieldMindIcons.Question, modifier = Modifier.fillMaxWidth())

                                    // Sex
                                    Text("Sex", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OptionPickerField(label = "Sex", selected = sex, options = listOf("Male", "Female", "Unknown", "Hermaphrodite"), onSelected = { sex = it }, icon = FieldMindIcons.Question, modifier = Modifier.fillMaxWidth())

                                    // Habitat Type
                                    Text("Habitat type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OptionPickerField(label = "Habitat", selected = habitatType, options = listOf("Forest", "Grassland", "Wetland", "Desert", "Marine", "Freshwater", "Urban", "Agricultural", "Coastal", "Mountain", "Cave"), onSelected = { habitatType = it }, icon = FieldMindIcons.Nature, modifier = Modifier.fillMaxWidth())

                                    // Weather override
                                    FieldTextField(weatherOverride, { weatherOverride = it }, "Weather override", supportingText = "e.g. Partly cloudy, light breeze")
                                }
                            }

                            Button(
                                onClick = ::saveQuickObservation,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = quickSubject.isNotBlank() || quickFacts.isNotBlank()
                            ) {
                                Icon(FieldMindIcons.Add, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save observation")
                            }
                        }
                    }
                }
            }
        }
    }


    // ── Species Detail Sheet ──
    if (showSpeciesDetail) {
        val detailRecord = selectedSpeciesRecord
        if (detailRecord != null) {
            SpeciesDetailSheet(
                record = detailRecord,
                onDismiss = { showSpeciesDetail = false }
            )
        }
    }

    // ── Species Identification Sheet ──
    if (showSpeciesSearch) {
        SpeciesIdentificationSheet(
            imageUri = speciesIdImageUri,
            classifier = speciesClassifier,
            database = speciesDatabase,
            onSelectSpecies = { match ->
                speciesName = match.commonName
                speciesConfidence = if (match.confidence > 0) "${match.confidence}" else ""
                identifiedSpecies = match
                showSpeciesSearch = false
            },
            onDismiss = { showSpeciesSearch = false }
        )
    }

    // ── In-app CameraX overlay ──
    if (showInAppCamera) {
        Dialog(
            onDismissRequest = { showInAppCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    addSessionAttachment(
                        DraftEvidenceAttachment("Photo", uri, "Camera photo", mimeType = mimeType)
                    )
                    showInAppCamera = false
                },
                onDismiss = { showInAppCamera = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


// ══════════════════════════════════════════════════════════════════════
//  Session Metadata Confirm Card — Shown on session start
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionMetadataConfirmCard(
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(FieldMindTheme.colors.info.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Weather, null, tint = FieldMindTheme.colors.info, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Acquire location & weather?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Auto-fetch GPS coordinates and current weather for this session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(FieldMindIcons.Location, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Fetch now")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Session Meta Chip — Compact metadata status chip
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionMetaChip(
    label: String,
    acquired: Boolean,
    detail: String = "",
    icon: MaterialSymbolIcon,
    accent: androidx.compose.ui.graphics.Color,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (onTap != null) onTap() },
        shape = RoundedCornerShape(14.dp),
        color = if (acquired) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (acquired) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (acquired) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = if (acquired) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private const val RESEARCH_SESSION_CHANNEL_ID = "fieldmind_research_session"
private const val RESEARCH_SESSION_NOTIFICATION_ID = 4107

private fun showResearchSessionNotification(context: Context, title: String, text: String) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(RESEARCH_SESSION_CHANNEL_ID, "Research sessions", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val intent = Intent(context, fieldmind.research.app.activities.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, RESEARCH_SESSION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(RESEARCH_SESSION_NOTIFICATION_ID, notification)
    }
}

private fun cancelResearchSessionNotification(context: Context) {
    runCatching { NotificationManagerCompat.from(context).cancel(RESEARCH_SESSION_NOTIFICATION_ID) }
}

// ══════════════════════════════════════════════════════════════════════
//  Session Evidence Button — Compact action button for attachments
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionEvidenceButton(
    onClick: () -> Unit,
    icon: MaterialSymbolIcon,
    label: String,
    accent: androidx.compose.ui.graphics.Color = FieldMindTheme.colors.observation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, size = 16.dp)
            Spacer(Modifier.size(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Recording Indicator — Animated recording UI
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RecordingIndicator(seconds: Int) {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(initialValue = 1f, targetValue = 0.25f, animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recDot")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(10.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Text("Recording…", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.weight(1f))
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
