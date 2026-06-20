package fieldmind.research.app.features.field.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.CapturedLocation
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.vision.PhashDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesClassifier
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesImageAnalyzer
import fieldmind.research.app.features.field.data.vision.SpeciesMatch
import fieldmind.research.app.features.field.presentation.screens.species.SpeciesIdentificationSheet
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import java.io.File
import kotlin.coroutines.resume
import androidx.compose.ui.text.input.KeyboardType

// ══════════════════════════════════════════════════════════════════════
//  Capture — Evidence-First Redesign with Live Timer
// ══════════════════════════════════════════════════════════════════════

/**
 * Redesigned ObserveScreen with evidence-first layout.
 *
 * Flow:
 * 1. Evidence buttons (Camera, Gallery, File, Mic) shown as primary action
 * 2. After capturing evidence, form auto-expands with preview at top
 * 3. Live timer runs persistently in the form header during the session
 * 4. Categories are collapsible with multi-select presets
 * 5. Save observation with all metadata
 */

// ── Capture state ──
@Parcelize
private data class CaptureSessionState(
    val isActive: Boolean = false,
    val step: CaptureStep = CaptureStep.Evidence,
    val subject: String = "",
    val facts: String = "",
    val category: String = "Other",
    val confidence: String = "Certain",
    val tags: String = "",
    val evidence: String = "",
    val fieldContext: String = "",
    val manualLocation: String = "",
    val attachments: List<DraftEvidenceAttachment> = emptyList(),
    // Enhanced fields matching the full spec
    val speciesName: String = "",
    val speciesConfidence: String = "Likely",
    val behavior: String = "",
    val lifeStage: String = "",
    val sex: String = "",
    val habitatType: String = "",
    val conservationStatus: String = "",
    val observationQuality: String = "Good",
    val weatherOverride: String = "Auto",
    val count: String = "",
    val distanceFromObserver: String = "10m",
    val observationChecklist: Set<String> = emptySet(),
    val measurements: Map<String, String> = emptyMap(),
    val followUpSchedule: String = "None",
    val qualityScore: Int = 0,
    val isDraft: Boolean = false,
    val draftId: Long? = null,
    val autoSaveTimer: Int = 0,
    // Species identification state
    var speciesIdProgress: Float = 0f,
    var speciesIdResults: List<String> = emptyList(),
    var speciesIdRunning: Boolean = false,
    // Live timer state
    val timerStartedAt: Long? = null,
    val timerAccumulatedMs: Long = 0L,
    val timerRunning: Boolean = false,
    val sessionObservationCount: Int = 0
) : Parcelable

@Parcelize
private enum class CaptureStep : Parcelable { Evidence, Details, Complete }

@Composable
fun ObserveScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    compactFieldMode: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    if (compactFieldMode) { FieldModeScreen(viewModel, onBack ?: {}); return }

    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val mediaEnabled by viewModel.fieldSettings.mediaAttachmentsEnabled.collectAsState()

    val haptics = rememberFieldMindHaptics()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // GPS location & accuracy
    val locationProvider = remember { FieldLocationProvider(context) }

    // Core session state — uses rememberSaveable to survive configuration changes
    var session by rememberSaveable { mutableStateOf(CaptureSessionState()) }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }

    // Sync captureSessionActive with local session state on navigation to Observe screen.
    // This prevents the nav bar from hiding when navigating to Observe without an active
    // session — handles stale captureSessionActive state from incomplete cleanup paths.
    LaunchedEffect(Unit) {
        if (!session.isActive) {
            viewModel.setCaptureSessionActive(false)
        }
    }

    var showEvidenceForm by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf("Other")) }

    // Camera dialog state
    var showInAppCamera by remember { mutableStateOf(false) }

    // ── Species identification state ──
    val speciesDatabase = remember { SpeciesDatabase(context) }
    val speciesImageAnalyzer = remember { SpeciesImageAnalyzer(context) }
    val speciesPhashDb = remember { PhashDatabase(context) }
    val speciesClassifier = remember { SpeciesClassifier(context, speciesDatabase, speciesImageAnalyzer, speciesPhashDb) }
    var showSpeciesId by remember { mutableStateOf(false) }
    var speciesIdImageUri by remember { mutableStateOf<String?>(null) }
    var identifiedSpecies by remember { mutableStateOf<SpeciesMatch?>(null) }

    // ── Weather snapshot from catalog cache ──
    var weatherSnapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherFetching by remember { mutableStateOf(false) }

    // ── Observations state (collected for reactive stats dashboard) ──
    val observations by viewModel.observations.collectAsState()

    // ── Action: add attachment ──
    fun addAttachment(attachment: DraftEvidenceAttachment) {
        session = session.copy(attachments = session.attachments + attachment)
        showFastSnackbar(snackbar, scope, "${attachment.type} attached")
    }

    // ── Audio recording state ──
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(recording) { if (recording) { recordSeconds = 0; while (recording) { delay(1000); recordSeconds++ } } }

    val audioImportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addAttachment(durableEvidenceAttachment(context, "Audio", uri, "Imported field audio"))
        }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createFieldMindFile(context, "audio", ".m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            runCatching {
                newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                newRecorder.setOutputFile(file.absolutePath)
                newRecorder.prepare()
                newRecorder.start()
                audioFile = file; recorder = newRecorder; recording = true
            }.onFailure {
                newRecorder.release()
                showFastSnackbar(snackbar, scope, "Could not start recording: ${it.localizedMessage}")
            }
        } else showFastSnackbar(snackbar, scope, "Audio permission denied.")
    }

    fun toggleRecording() {
        if (recording) {
            val file = audioFile
            runCatching { recorder?.stop() }; recorder?.release(); recorder = null; recording = false
            file?.let { addAttachment(DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4")) }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Media picker and file picker launchers (MUST be at composable scope) ──
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { uri ->
            addAttachment(
                durableEvidenceAttachment(context, "Gallery", uri, "Selected media")
            )
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            addAttachment(
                durableEvidenceAttachment(context, "File", uri, "Reference file")
            )
        }
    }

    // ── Metadata auto-fetch confirmation dialog ──
    var showMetadataConfirm by remember { mutableStateOf(false) }
    var metadataAutoFetching by remember { mutableStateOf(false) }
    var metadataStatus by remember { mutableStateOf("Ready") }
    var gpsFetching by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    fun performAutoFetch() {
        if (!locationProvider.isGpsEnabled()) {
            showGpsDialog = true
            metadataAutoFetching = false
            return
        }
        metadataAutoFetching = true
        metadataStatus = "Acquiring GPS…"
        if (locationProvider.hasAnyLocationPermission()) {
            locationProvider.requestCurrentLocation { loc ->
                if (loc != null) {
                    capturedLocation = loc
                    metadataStatus = "GPS acquired (${loc.accuracyMeters?.toInt() ?: "?"}m accuracy) — fetching weather…"
                    scope.launch {
                        weatherFetching = true
                        val snapshot = viewModel.fetchWeatherSnapshot(loc.latitude, loc.longitude)
                        weatherSnapshot = snapshot
                        weatherFetching = false
                        if (snapshot != null) {
                            // Log to offline weather catalog
                            viewModel.saveWeatherSnapshot(snapshot, loc.latitude, loc.longitude)
                            metadataStatus = "GPS + Weather acquired"
                            showFastSnackbar(snackbar, scope, "Weather: ${snapshot.temperature}°C, ${snapshot.weatherDescription}")
                        } else {
                            metadataStatus = "GPS acquired, weather unavailable"
                        }
                        metadataAutoFetching = false
                    }
                    locationProvider.resolvePlaceName(loc.latitude, loc.longitude) { place ->
                        if (!place.isNullOrBlank()) {
                            capturedLocation = loc.copy(placeName = place)
                        }
                    }
                } else {
                    metadataStatus = "GPS unavailable — check permissions"
                    metadataAutoFetching = false
                }
            }
        } else {
            metadataStatus = "Location permission required"
            metadataAutoFetching = false
        }
    }

    // ── Action: start evidence capture ──
    fun startCapture() {
        haptics.light()
        session = session.copy(isActive = true, step = CaptureStep.Evidence)
        showEvidenceForm = true
        viewModel.setCaptureSessionActive(true)
        // Auto-start timer if not already running
        if (!session.timerRunning && session.timerStartedAt == null) {
            session = session.copy(timerStartedAt = System.currentTimeMillis(), timerRunning = true)
        }
        // Show metadata auto-fetch confirmation
        showMetadataConfirm = true
    }
    // ── Action: save observation ──
    fun saveObservation() {
        val s = session
        if (s.subject.isBlank() && s.facts.isBlank()) {
            showFastSnackbar(snackbar, scope, "Add a subject or facts before saving.")
            return
        }
        haptics.confirm()
        val now = System.currentTimeMillis()
        val liveElapsed = s.timerAccumulatedMs +
            if (s.timerRunning) (now - (s.timerStartedAt ?: now)) else 0L

        // Pack all enhanced capture fields into structuredDetailsJson
        val structuredJson = run {
            val fields = mutableMapOf<String, String>()
            if (s.speciesName.isNotBlank()) fields["speciesName"] = s.speciesName
            if (s.speciesConfidence != "Likely") fields["speciesConfidence"] = s.speciesConfidence
            if (s.behavior.isNotBlank()) fields["behavior"] = s.behavior
            if (s.lifeStage.isNotBlank()) fields["lifeStage"] = s.lifeStage
            if (s.sex.isNotBlank()) fields["sex"] = s.sex
            if (s.habitatType.isNotBlank()) fields["habitatType"] = s.habitatType
            if (s.conservationStatus.isNotBlank()) fields["conservationStatus"] = s.conservationStatus
            if (s.observationQuality != "Good") fields["observationQuality"] = s.observationQuality
            if (s.weatherOverride != "Auto") fields["weatherOverride"] = s.weatherOverride
            if (s.count.isNotBlank()) fields["count"] = s.count
            if (s.distanceFromObserver != "10m") fields["distanceFromObserver"] = s.distanceFromObserver
            if (s.followUpSchedule != "None") fields["followUpSchedule"] = s.followUpSchedule
            if (s.qualityScore > 0) fields["qualityScore"] = s.qualityScore.toString()
            if (s.observationChecklist.isNotEmpty()) fields["observationChecklist"] = s.observationChecklist.joinToString(",")
            if (s.measurements.isNotEmpty()) fields["measurements"] = s.measurements.entries.joinToString(";") { "${it.key}=${it.value}" }
            try {
                org.json.JSONObject(fields.toMap()).toString()
            } catch (_: Exception) { "" }
        }

        viewModel.addObservation(
            subject = s.subject.ifBlank { s.facts.take(36) },
            category = s.category,
            facts = s.facts,
            confidence = s.confidence,
            manualLocation = s.manualLocation,
            tags = s.tags,
            evidence = s.evidence,
            context = s.fieldContext,
            attachments = s.attachments,
            durationMs = liveElapsed.takeIf { it > 0L },
            latitude = capturedLocation?.latitude,
            longitude = capturedLocation?.longitude,
            structuredDetailsJson = structuredJson,
            timeNote = "Captured via observation session"
        ) {
            session = session.copy(
                subject = "", speciesName = "", facts = "", tags = "", evidence = "",
                fieldContext = "", manualLocation = "", attachments = emptyList(),
                sessionObservationCount = session.sessionObservationCount + 1
            )
            showFastSnackbar(snackbar, scope, "Observation saved! Session: ${session.sessionObservationCount + 1}")
        }
    }

    // ── System back handler with unsaved data confirmation ──
    val hasDirtyContent = session.isActive && (
        session.subject.isNotBlank() || session.facts.isNotBlank() ||
        session.attachments.isNotEmpty() || session.tags.isNotBlank() ||
        session.fieldContext.isNotBlank() || session.manualLocation.isNotBlank()
    )
    var showExitConfirm by remember { mutableStateOf(false) }
    var showSessionExitConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (hasDirtyContent) {
            showExitConfirm = true
        } else if (session.isActive) {
            showSessionExitConfirm = true
        } else {
            onBack?.invoke()
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            icon = { Icon(icon = FieldMindIcons.Info, contentDescription = null, size = 28.dp) },
            title = { Text("Unsaved observation") },
            text = {
                Text(
                    "You have an active observation with unsaved data. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveObservation()
                        showExitConfirm = false
                        onBack?.invoke()
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Save and exit") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.setCaptureSessionActive(false)
                        session = CaptureSessionState()
                        showEvidenceForm = false
                        showExitConfirm = false
                        onBack?.invoke()
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

    // ── Session exit confirm (active session, clean form) ──
    if (showSessionExitConfirm) {
        AlertDialog(
            onDismissRequest = { showSessionExitConfirm = false },
            icon = {
                Icon(
                    icon = FieldMindIcons.Timer,
                    contentDescription = null,
                    size = 28.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Active observation session") },
            text = {
                Text(
                    "You have an active observation session in progress. Leaving now will discard the session and any timer data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSessionExitConfirm = false }) {
                    Text("Stay on Capture")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setCaptureSessionActive(false)
                    session = CaptureSessionState()
                    showEvidenceForm = false
                    showSessionExitConfirm = false
                    onBack?.invoke()
                }) {
                    Text("Discard session", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header ──
                item {
                    StandardScreenHeader(
                    title = "Observation",
                    subtitle = "Capture evidence, time, place, weather, then add facts.",
                    icon = FieldMindIcons.Capture
                )
                }

                // ── Live Timer (persistent when active) ──
                if (session.isActive) {
                    item {
                        LiveObservationTimer(
                            timerStartedAt = session.timerStartedAt,
                            timerAccumulatedMs = session.timerAccumulatedMs,
                            timerRunning = session.timerRunning,
                            observationCount = session.sessionObservationCount,
                            onStart = {
                                if (!session.timerRunning) {
                                    session = session.copy(
                                        timerStartedAt = System.currentTimeMillis(),
                                        timerRunning = true
                                    )
                                }
                            },
                            onPause = {
                                val startedAt = session.timerStartedAt
                                if (session.timerRunning && startedAt != null) {
                                    val elapsed = session.timerAccumulatedMs +
                                        (System.currentTimeMillis() - startedAt)
                                    session = session.copy(
                                        timerAccumulatedMs = elapsed,
                                        timerRunning = false,
                                        timerStartedAt = null
                                    )
                                }
                            },
                            onReset = {
                                session = session.copy(
                                    timerStartedAt = System.currentTimeMillis(),
                                    timerAccumulatedMs = 0L,
                                    timerRunning = true,
                                    sessionObservationCount = 0
                                )
                            },
                            onClose = {
                                session = CaptureSessionState()
                                showEvidenceForm = false
                                viewModel.setCaptureSessionActive(false)
                            }
                        )
                    }
                } else {
                    // ── Start capture button ──
                    item {
                        Button(
                            onClick = { startCapture() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 20.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Start observation session")
                        }
                    }
                }

                // ── Observation stats overview (hidden during active session to avoid clutter) ──
                if (observations.isNotEmpty() && !session.isActive) {
                    item {
                        ObservationStatsDashboard(
                            observations = observations
                        )
                    }
                }                    // ── Evidence-First Input ──
                if (showEvidenceForm) {
                    // Evidence capture buttons (always visible when form is open)
                    item {
                        EvidenceCaptureRow(
                            mediaEnabled = mediaEnabled,
                            attachments = session.attachments,
                            onLaunchCamera = {
                                haptics.light()
                                showInAppCamera = true
                            },
                            onLaunchGallery = {
                                haptics.light()
                                mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            onLaunchFile = {
                                haptics.light()
                                filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*"))
                            },
                            onLaunchAudio = { toggleRecording() },
                            onLaunchAudioImport = { haptics.light(); audioImportPicker.launch(arrayOf("audio/*")) },
                            onOpenSpeciesSearch = {
                                speciesIdImageUri = null
                                showSpeciesId = true
                            },
                            onIdentifyFromPhoto = { uri ->
                                if (uri != null) {
                                    speciesIdImageUri = uri
                                    showSpeciesId = true
                                }
                            },
                            isRecording = recording,
                            recordSeconds = recordSeconds,
                            onRemoveAttachment = { idx ->
                                session = session.copy(
                                    attachments = session.attachments.filterIndexed { i, _ -> i != idx }
                                )
                            },
                            onCaptionChange = { idx, caption ->
                                session = session.copy(
                                    attachments = session.attachments.mapIndexed { i, item ->
                                        if (i == idx) item.copy(caption = caption) else item
                                    }
                                )
                            }
                        )
                    }

                    // ── Auto Metadata Status — MOVED TO TOP ──
                    item {
                        AutoMetadataStatusCard(
                            hasGps = capturedLocation != null,
                            hasWeather = weatherSnapshot != null,
                            hasTimestamp = true,
                            gpsAccuracy = capturedLocation?.accuracyMeters,
                            weatherDetail = weatherSnapshot?.asDisplayText(),
                            autoFetching = metadataAutoFetching,
                            gpsFetching = gpsFetching,
                            statusText = metadataStatus,
                            onFetchGps = {
                                if (!locationProvider.isGpsEnabled()) {
                                    showGpsDialog = true
                                } else if (locationProvider.hasAnyLocationPermission()) {
                                    gpsFetching = true
                                    metadataStatus = "Acquiring GPS…"
                                    locationProvider.requestCurrentLocation { loc ->
                                        gpsFetching = false
                                        if (loc != null) {
                                            capturedLocation = loc
                                            metadataStatus = "GPS acquired (${loc.accuracyMeters?.toInt() ?: "?"}m)"
                                            showFastSnackbar(snackbar, scope, "GPS acquired")
                                        } else {
                                            metadataStatus = "GPS unavailable — check permissions"
                                        }
                                    }
                                } else {
                                    showFastSnackbar(snackbar, scope, "Location permission required")
                                }
                            },
                            onFetchWeather = {
                                val loc = capturedLocation
                                if (loc != null) {
                                    scope.launch {
                                        weatherFetching = true
                                        metadataStatus = "Fetching weather…"
                                        val snapshot = viewModel.fetchWeatherSnapshot(loc.latitude, loc.longitude)
                                        weatherSnapshot = snapshot
                                        weatherFetching = false
                                        if (snapshot != null) {
                                            viewModel.saveWeatherSnapshot(snapshot, loc.latitude, loc.longitude)
                                            metadataStatus = "Weather acquired"
                                            showFastSnackbar(snackbar, scope, "Weather: ${snapshot.temperature}°C, ${snapshot.weatherDescription}")
                                        } else {
                                            metadataStatus = "Weather unavailable"
                                        }
                                    }
                                } else {
                                    showFastSnackbar(snackbar, scope, "Acquire GPS location first")
                                }
                            },
                            onFetchBoth = { performAutoFetch() }
                        )
                    }

                    // ── Quick Classification Grid (always visible) ──
                    item {
                        QuickClassificationGrid(
                            selectedCategory = session.category,
                            onCategorySelected = { session = session.copy(category = it) }
                        )
                    }

                    // ── Enhanced Observation Form (with species autocomplete) ──
                    item {
                        EnhancedObservationForm(
                            session = session,
                            onSessionChange = { session = it },
                            onSave = { saveObservation() },
                            speciesDatabase = speciesDatabase,
                            onOpenSpeciesSearch = {
                                speciesIdImageUri = null
                                showSpeciesId = true
                            },
                            identifiedSpecies = identifiedSpecies,
                            onSelectSpecies = { match ->
                                identifiedSpecies = match
                                session = session.copy(
                                    speciesName = match.commonName,
                                    category = match.category
                                )
                            }
                        )
                    }

                    // ── Species Identification Live Card ──
                    item {
                        SpeciesIdentificationLiveCard(
                            attachments = session.attachments,
                            progress = session.speciesIdProgress,
                            results = session.speciesIdResults,
                            isRunning = session.speciesIdRunning,
                            identifiedSpecies = identifiedSpecies,
                            onRunIdentification = { uri ->
                                speciesIdImageUri = uri
                                showSpeciesId = true
                            }
                        )
                    }
                }

                // ── Metadata auto-fetch confirmation dialog ──
                if (showMetadataConfirm) {
                    item {
                        AutoFetchConfirmCard(
                            onConfirm = {
                                showMetadataConfirm = false
                                performAutoFetch()
                            },
                            onSkip = {
                                showMetadataConfirm = false
                                metadataStatus = "Skipped — tap chips to fetch"
                            }
                        )
                    }
                }

                // ── Empty state (only when no form is open AND no saved observations) ──
                if (!showEvidenceForm && !session.isActive && observations.isEmpty()) {
                    item {
                        EmptyState(
                            "No observations yet",
                            "Start a session below to capture evidence and log observations.",
                            icon = FieldMindIcons.Observation
                        )
                    }
                }
            }

            // ── Top snackbar overlay ──
            FieldMindSnackbarOverlay(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }

    // ── Species identification sheet ──
    if (showSpeciesId) {
        SpeciesIdentificationSheet(
            imageUri = speciesIdImageUri,
            classifier = speciesClassifier,
            database = speciesDatabase,
            phashDatabase = speciesPhashDb,
            onSelectSpecies = { match ->
                identifiedSpecies = match
                session = session.copy(
                    subject = match.commonName,
                    speciesName = match.commonName,
                    category = match.category,
                    speciesConfidence = when {
                        match.confidence >= 0.8f -> "Certain"
                        match.confidence >= 0.5f -> "Likely"
                        else -> "Unsure"
                    }
                )
                showFastSnackbar(snackbar, scope, "Identified as ${match.commonName}")
            },
            onDismiss = {
                showSpeciesId = false
                speciesIdImageUri = null
            }
        )
    }

    // ── In-app camera overlay with multi-capture mode ──
    if (showInAppCamera) {
        Dialog(
            onDismissRequest = { showInAppCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    // Add to session attachments; camera may stay open via post-capture dialog
                    addAttachment(
                        DraftEvidenceAttachment("Photo", uri, "Camera photo", mimeType = mimeType)
                    )
                },
                onSpeciesCaptured = { uri, mimeType, speciesName, category, confidence, notes ->
                    // Add photo as attachment and pre-fill species details
                    addAttachment(
                        DraftEvidenceAttachment("Photo", uri, speciesName.ifBlank { "Camera photo" }, mimeType = mimeType)
                    )
                    if (speciesName.isNotBlank()) {
                        session = session.copy(
                            speciesName = speciesName,
                            category = if (category != "Other") category else session.category,
                            subject = if (session.subject.isBlank()) speciesName else session.subject
                        )
                    }
                },
                onDismiss = { showInAppCamera = false },
                modifier = Modifier.fillMaxSize(),
                multiCaptureMode = true
            )
        }
    }

    // ── GpsOffDialog ──
    if (showGpsDialog) {
        GpsOffDialog(onDismiss = { showGpsDialog = false })
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Live Observation Timer — persistent header component
// ══════════════════════════════════════════════════════════════════════

/**
 * Persistent live timer shown at the top of the capture form.
 * Displays elapsed time (updating every second) and session observation count.
 */
@Composable
private fun LiveObservationTimer(
    timerStartedAt: Long?,
    timerAccumulatedMs: Long,
    timerRunning: Boolean,
    observationCount: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    // Compute live elapsed time
    val liveElapsedMs = remember(timerStartedAt, timerAccumulatedMs, timerRunning) {
        if (timerRunning && timerStartedAt != null) {
            timerAccumulatedMs + (System.currentTimeMillis() - timerStartedAt)
        } else {
            timerAccumulatedMs
        }
    }

    // Force recomposition every second when running
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (true) {
                delay(500)
                tick++
            }
        }
    }

    // Recompute elapsed with tick dependency
    val startedAt = timerStartedAt
    val displayElapsed = if (timerRunning && startedAt != null) {
        @Suppress("UNUSED_EXPRESSION")
        tick // Force recomposition
        timerAccumulatedMs + (System.currentTimeMillis() - startedAt)
    } else {
        timerAccumulatedMs
    }

    val formatted = formatDurationCompact(displayElapsed)
    val isRunning = timerRunning

    // Pulsing dot animation when running
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Timer icon with pulsing dot
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon = FieldMindIcons.Timer,
                    contentDescription = "Timer",
                    tint = MaterialTheme.colorScheme.primary,
                    size = 26.dp
                )
                if (isRunning) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = pulseAlpha }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // Elapsed time (large)
            Column(Modifier.weight(1f)) {
                Text(
                    formatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "$observationCount observation${if (observationCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRunning) {
                        Text(
                            "● Recording",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isRunning) {
                    FilledTonalIconButton(
                        onClick = onPause,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            icon = MaterialSymbolIcon("pause"),
                            contentDescription = "Pause",
                            size = 18.dp
                        )
                    }
                } else if (displayElapsed > 0L) {
                    FilledTonalIconButton(
                        onClick = onStart,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            icon = MaterialSymbolIcon("play_arrow"),
                            contentDescription = "Resume",
                            size = 18.dp
                        )
                    }
                } else {
                    FilledTonalIconButton(
                        onClick = onStart,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            icon = MaterialSymbolIcon("play_arrow"),
                            contentDescription = "Start",
                            size = 18.dp
                        )
                    }
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        icon = MaterialSymbolIcon("restart_alt"),
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        icon = FieldMindIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                }
            }
        }
    }
    
}

private fun formatDurationCompact(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

// ══════════════════════════════════════════════════════════════════════
//  Evidence Capture Row — Primary action buttons
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun EvidenceCaptureRow(
    mediaEnabled: Boolean,
    attachments: List<DraftEvidenceAttachment>,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onLaunchFile: () -> Unit,
    onLaunchAudio: () -> Unit = {},
    onLaunchAudioImport: () -> Unit = {},
    isRecording: Boolean = false,
    recordSeconds: Int = 0,
    onRemoveAttachment: (Int) -> Unit,
    onCaptionChange: (Int, String) -> Unit,
    onOpenSpeciesSearch: () -> Unit = {},
    onIdentifyFromPhoto: (String?) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Camera, null, tint = FieldMindTheme.colors.observation, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Add evidence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (attachments.isEmpty()) "Capture or attach files first, then add details." else "${attachments.size} item${if (attachments.size != 1) "s" else ""} attached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Evidence action buttons — large, prominent
            if (mediaEnabled) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EvidenceButton(
                        onClick = onLaunchCamera,
                        icon = FieldMindIcons.Camera,
                        label = "Camera",
                        accent = FieldMindTheme.colors.observation,
                        modifier = Modifier.weight(1f)
                    )
                    EvidenceButton(
                        onClick = onLaunchGallery,
                        icon = FieldMindIcons.Gallery,
                        label = "Gallery",
                        accent = FieldMindTheme.colors.info,
                        modifier = Modifier.weight(1f)
                    )
                    EvidenceButton(
                        onClick = onLaunchFile,
                        icon = FieldMindIcons.File,
                        label = "File",
                        accent = FieldMindTheme.colors.data,
                        modifier = Modifier.weight(1f)
                    )
                    EvidenceButton(
                        onClick = onLaunchAudio,
                        icon = if (isRecording) FieldMindIcons.Stop else FieldMindIcons.Mic,
                        label = if (isRecording) "$recordSeconds" else "Audio",
                        accent = if (isRecording) MaterialTheme.colorScheme.error else FieldMindTheme.colors.accentFor("observation"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Audio import button (shown when not recording)
            if (!isRecording) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onLaunchAudioImport) {
                        Icon(FieldMindIcons.Mic, null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Import audio", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Species identification button (always visible when form is active)
            Spacer(Modifier.height(8.dp))
            SpeciesIdButton(
                attachments = attachments,
                identifiedSpecies = null,
                onIdentifyFromPhoto = onIdentifyFromPhoto,
                onOpenSearch = onOpenSpeciesSearch
            )

            // Attachment previews
            if (attachments.isNotEmpty()) {
                AttachmentPreviewList(
                    items = attachments,
                    onCaptionChange = onCaptionChange,
                    onRemove = onRemoveAttachment
                )
            }
        }
    }
}

@Composable
private fun EvidenceButton(
    onClick: () -> Unit,
    icon: MaterialSymbolIcon,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.24f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon = icon, contentDescription = null, tint = accent, size = 20.dp)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Quick Observation Form
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickObservationForm(
    subject: String,
    onSubjectChange: (String) -> Unit,
    facts: String,
    onFactsChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    confidence: String,
    onConfidenceChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    evidenceSummary: String,
    onEvidenceChange: (String) -> Unit,
    fieldContext: String,
    onFieldContextChange: (String) -> Unit,
    manualLocation: String,
    onLocationChange: (String) -> Unit,
    projects: List<ProjectEntity>,
    onSave: () -> Unit,
    // Phase 3 fields
    speciesConfidence: String = "Likely",
    onSpeciesConfidenceChange: (String) -> Unit = {},
    distanceFromObserver: String = "10m",
    onDistanceChange: (String) -> Unit = {},
    observationChecklist: Set<String> = emptySet(),
    onChecklistChange: (Set<String>) -> Unit = {},
    measurements: Map<String, String> = emptyMap(),
    onMeasurementChange: (String, String) -> Unit = { _, _ -> },
    followUpSchedule: String = "None",
    onFollowUpChange: (String) -> Unit = {},
    qualityScore: Int = 0
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showStructured by remember { mutableStateOf(false) }
    var showProtocols by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf<FieldProtocol?>(null) }
    var protocolData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val hasEvidence = facts.isNotBlank()
    val hasLocation = manualLocation.isNotBlank()
    val hasMeasurements = measurements.values.any { it.isNotBlank() }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Form header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Edit, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                }
                Text("Observation details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Quality Score (Phase 3)
            QualityScoreCard(qualityScore)
            MissingFieldsChecklist(
                hasSubject = subject.isNotBlank(),
                hasEvidence = hasEvidence,
                hasLocation = hasLocation,
                hasWeather = false,
                hasMeasurements = hasMeasurements,
                hasNotes = facts.isNotBlank(),
                hasDuration = false,
                hasConfidence = confidence.isNotBlank()
            )

            // ── Core fields ──
            FieldTextField(subject, onSubjectChange, "Subject", supportingText = "e.g. Crow on wire")
            FactsInterpretationBanner()
            FieldTextField(facts, onFactsChange, "Facts-only notes", minLines = 3,
                supportingText = "What did you see/hear/measure?")

            // ── Collapsible categories ──
            Row(
                Modifier.fillMaxWidth().clickable { showCategories = !showCategories },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(FieldMindIcons.Category, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                    Text("Category: $category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    if (showCategories) FieldMindIcons.Up else FieldMindIcons.Down,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp
                )
            }
            AnimatedVisibility(visible = showCategories, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OptionPickerField(label = "Category", selected = category, options = observationCategories, onSelected = onCategoryChange, icon = FieldMindIcons.Category)
                    Spacer(Modifier.height(8.dp))
                    OptionPickerField(label = "Confidence", selected = confidence, options = confidenceOptions, onSelected = onConfidenceChange, icon = FieldMindIcons.Check)
                }
            }

            // ── Structured Fields (Phase 3) ──
            Row(
                Modifier.fillMaxWidth().clickable { showStructured = !showStructured },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Structured details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Icon(
                    if (showStructured) FieldMindIcons.Up else FieldMindIcons.Down,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp
                )
            }
            AnimatedVisibility(visible = showStructured, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpeciesConfidenceSelector(speciesConfidence, onSpeciesConfidenceChange)
                    DistanceSelector(distanceFromObserver, onDistanceChange)
                    ObservationChecklistPicker(observationChecklist, onChecklistChange)
                    MeasurementsInputSection(measurements, onMeasurementChange)
                    FollowUpScheduler(followUpSchedule, onFollowUpChange)
                }
            }

            // ── Advanced fields (collapsible) ──
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Icon(
                    if (showAdvanced) FieldMindIcons.Up else FieldMindIcons.Down,
                    null, size = 18.dp
                )
                Spacer(Modifier.size(6.dp))
                Text(if (showAdvanced) "Hide additional fields" else "Show additional fields (tags, location, context)")
            }
            AnimatedVisibility(visible = showAdvanced, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FieldTextField(tags, onTagsChange, "Tags", supportingText = "Comma-separated: birds, behavior, evening")
                    FieldTextField(manualLocation, onLocationChange, "Place / location")
                    MultiSelectPickerField(label = "Context presets", selected = if (fieldContext.isBlank()) emptySet() else fieldContext.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(), options = contextPresets, onSelectionChanged = { onFieldContextChange(it.joinToString(", ")) }, subtitle = "Select field conditions", icon = FieldMindIcons.Info, showSearch = false)
                    FieldTextField(fieldContext, onFieldContextChange, "Context / mood", minLines = 2)
                    FieldTextField(evidenceSummary, onEvidenceChange, "Evidence summary", minLines = 2)
                }
            }

            // ── Protocol picker button ──
            Row(
                Modifier.fillMaxWidth().clickable { showProtocols = !showProtocols },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                    Text(
                        if (selectedProtocol != null) "Protocol: ${selectedProtocol!!.name}" else "Start from protocol",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    if (showProtocols) FieldMindIcons.Up else FieldMindIcons.Down,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp
                )
            }

            // Protocol steps (when selected)
            if (selectedProtocol != null) {
                val protocol = selectedProtocol!!
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(protocol.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        protocol.steps.forEach { step ->
                            ProtocolStepField(
                                step = step,
                                value = protocolData[step.id].orEmpty(),
                                onValueChange = { onMeasurementChange("protocol_${step.id}", it) }, // Reuse measurement pathway
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Protocol picker dialog
            if (showProtocols) {
                ProtocolPicker(
                    selectedId = selectedProtocol?.id,
                    onSelect = { protocol ->
                        selectedProtocol = protocol
                        if (protocol != null) {
                            onCategoryChange(protocol.suggestedCategory)
                            onTagsChange(protocol.defaultTags)
                            protocolData = protocol.steps.associate { it.id to "" }
                        }
                        showProtocols = false
                    },
                    onDismiss = { showProtocols = false }
                )
            }

            // ── Save button ──
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Save observation")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Quick Classification Grid — Prominent category grid per spec
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickClassificationGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val colors = FieldMindTheme.colors
    data class CategoryItem(val name: String, val icon: MaterialSymbolIcon)
    val displayCategories = listOf(
        CategoryItem("Bird", FieldMindIcons.Bird),
        CategoryItem("Mammal", FieldMindIcons.Animal),
        CategoryItem("Reptile", FieldMindIcons.Animal),
        CategoryItem("Amphibian", FieldMindIcons.Animal),
        CategoryItem("Insect", FieldMindIcons.Insect),
        CategoryItem("Plant", FieldMindIcons.Plant),
        CategoryItem("Fungus", FieldMindIcons.Plant),
        CategoryItem("Habitat", FieldMindIcons.Nature)
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Category, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                Text("Quick classification", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            // Responsive grid: chunk into 4-per-row tiles
            displayCategories.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        val accent = colors.accentFor(cat.name)
                        Surface(
                            onClick = { onCategorySelected(cat.name) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accent) else null,
                            tonalElevation = 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    icon = cat.icon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 22.dp
                                )
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        // Balance the row if fewer than 4 items
                        if (row.size < 4 && cat == row.last()) {
                            repeat(4 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Auto-Fetch Confirmation Card — Shown on session start
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun AutoFetchConfirmCard(
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
                    Text("Auto-fetch GPS coordinates and current weather for your observation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
//  Auto Metadata Status Card — Enhanced with weather detail, fetch-all, and confirmation
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun AutoMetadataStatusCard(
    hasGps: Boolean,
    hasWeather: Boolean,
    hasTimestamp: Boolean,
    gpsAccuracy: Float?,
    weatherDetail: String? = null,
    autoFetching: Boolean = false,
    gpsFetching: Boolean = false,
    statusText: String = "Ready",
    onFetchGps: () -> Unit,
    onFetchWeather: () -> Unit = {},
    onFetchBoth: () -> Unit = {}
) {
    val colors = FieldMindTheme.colors
    val showWeatherConfirm = remember { mutableStateOf(false) }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                Column(Modifier.weight(1f)) {
                    Text("Location & weather", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (autoFetching) {
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (!hasGps && !autoFetching) {
                    Surface(
                        onClick = onFetchBoth,
                        shape = RoundedCornerShape(12.dp),
                        color = colors.info.copy(alpha = 0.12f)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(FieldMindIcons.Weather, null, tint = colors.info, size = 16.dp)
                            Text("Fetch all", style = MaterialTheme.typography.labelSmall, color = colors.info, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetadataStatusChip(
                    label = "GPS",
                    acquired = hasGps,
                    detail = if (gpsFetching) "Fetching…" else if (hasGps && gpsAccuracy != null) "±${gpsAccuracy.toInt()}m" else if (hasGps) "Acquired" else null,
                    icon = FieldMindIcons.Location,
                    accent = if (hasGps) colors.positive else colors.warning,
                    onTap = if (!hasGps && !autoFetching && !gpsFetching) onFetchGps else null,
                    modifier = Modifier.weight(1f)
                )
                MetadataStatusChip(
                    label = "Weather",
                    acquired = hasWeather,
                    detail = weatherDetail ?: if (hasWeather) "Logged" else null,
                    icon = FieldMindIcons.Weather,
                    accent = if (hasWeather) colors.positive else colors.warning,
                    onTap = if (hasGps && !hasWeather && !autoFetching) {
                        { showWeatherConfirm.value = true }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                MetadataStatusChip(
                    label = "Time",
                    acquired = hasTimestamp,
                    icon = FieldMindIcons.Calendar,
                    accent = if (hasTimestamp) colors.positive else colors.warning,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Weather fetch confirmation dialog
            if (showWeatherConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showWeatherConfirm.value = false },
                    icon = { Icon(FieldMindIcons.Weather, null, tint = MaterialTheme.colorScheme.primary, size = 28.dp) },
                    title = { Text("Fetch weather data?") },
                    text = { Text("Fetch current weather conditions from your location. This will use GPS coordinates.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showWeatherConfirm.value = false
                                onFetchWeather()
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Fetch weather") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWeatherConfirm.value = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun MetadataStatusChip(
    label: String,
    acquired: Boolean,
    detail: String? = null,
    icon: MaterialSymbolIcon,
    accent: Color,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        onClick = { if (onTap != null) { haptics.light(); onTap() } },
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
                detail ?: if (acquired) "Acquired" else "Tap to fetch",
                style = MaterialTheme.typography.labelSmall,
                color = if (acquired) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Identification Live Card — Non-blocking ID progress
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SpeciesIdentificationLiveCard(
    attachments: List<DraftEvidenceAttachment>,
    progress: Float,
    results: List<String>,
    isRunning: Boolean,
    identifiedSpecies: SpeciesMatch?,
    onRunIdentification: (String) -> Unit
) {
    val hasPhoto = attachments.any { it.isImage() }
    val colors = FieldMindTheme.colors

    if (!hasPhoto && identifiedSpecies == null) return

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (identifiedSpecies != null)
                colors.observation.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    FieldMindIcons.Nature,
                    null,
                    tint = if (identifiedSpecies != null) colors.observation else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp
                )
                Text(
                    if (identifiedSpecies != null) "Identified: ${identifiedSpecies.commonName}"
                    else if (isRunning) "Identifying species…"
                    else "Species identification",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isRunning && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
                Text(
                    "${(progress * 100).toInt()}% — Analyzing photo…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (results.isNotEmpty() && identifiedSpecies == null) {
                results.forEach { result ->
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (identifiedSpecies != null) {
                Text(
                    "${identifiedSpecies.scientificName} • ${(identifiedSpecies.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.observation
                )
            }

            if (hasPhoto && identifiedSpecies == null && !isRunning) {
                Button(
                    onClick = {
                        attachments.firstOrNull { it.isImage() }?.uri?.let { onRunIdentification(it) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(FieldMindIcons.Nature, null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Identify from photo")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Enhanced Observation Form — Full form with species autocomplete
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnhancedObservationForm(
    session: CaptureSessionState,
    onSessionChange: (CaptureSessionState) -> Unit,
    onSave: () -> Unit,
    speciesDatabase: SpeciesDatabase? = null,
    onOpenSpeciesSearch: () -> Unit = {},
    identifiedSpecies: SpeciesMatch? = null,
    onSelectSpecies: ((SpeciesMatch) -> Unit)? = null
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var showSpeciesSearch by remember { mutableStateOf(false) }
    var selectedSpecies by remember { mutableStateOf<String?>(null) }
    var speciesSuggestions by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchingSpecies by remember { mutableStateOf(false) }

    // ── Selected species record (full info from database) ──
    var selectedSpeciesRecord by remember { mutableStateOf<SpeciesRecord?>(null) }
    var showTaxonomy by remember { mutableStateOf(false) }
    var showSpeciesDetailSheet by remember { mutableStateOf(false) }

    // Look up full species record when speciesName changes
    LaunchedEffect(session.speciesName) {
        if (session.speciesName.isNotBlank() && session.speciesName != selectedSpeciesRecord?.commonName) {
            val db = speciesDatabase
            if (db != null) {
                val results = db.search(session.speciesName, limit = 5)
                selectedSpeciesRecord = results.firstOrNull { 
                    it.commonName.equals(session.speciesName, ignoreCase = true) ||
                    it.scientificName.equals(session.speciesName, ignoreCase = true)
                }
            }
        } else if (session.speciesName.isBlank()) {
            selectedSpeciesRecord = null
            showTaxonomy = false
        }
    }

    // Reset when session clears after save
    LaunchedEffect(session.speciesName) {
        if (session.speciesName.isBlank()) {
            selectedSpecies = null
            showSpeciesSearch = false
            speciesSuggestions = emptyList()
            showSuggestions = false
            selectedSpeciesRecord = null
            showTaxonomy = false
        }
    }

    // Species search with debounce
    val speciesSearchQuery = remember { mutableStateOf("") }
    LaunchedEffect(speciesSearchQuery.value) {
        if (speciesSearchQuery.value.length >= 2) {
            searchingSpecies = true
            kotlinx.coroutines.delay(300) // debounce
            val db = speciesDatabase
            if (db != null && speciesSearchQuery.value.isNotBlank()) {
                val results = db.search(speciesSearchQuery.value, limit = 5)
                speciesSuggestions = results
                showSuggestions = results.isNotEmpty()
            }
            searchingSpecies = false
        } else {
            speciesSuggestions = emptyList()
            showSuggestions = false
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Form header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Edit, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Observation details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Fill in what you observed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Subject Name (independent from species) ──
            FieldTextField(
                session.subject,
                { onSessionChange(session.copy(subject = it)) },
                "Subject name",
                supportingText = "e.g. House Crow carrying twig"
            )

            // ── Species Search with Autocomplete ──
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedSpecies ?: session.speciesName,
                    onValueChange = { query ->
                        selectedSpecies = query
                        speciesSearchQuery.value = query
                        onSessionChange(session.copy(speciesName = query))
                        showSpeciesSearch = query.length >= 2
                    },
                    label = { Text("Species") },
                    placeholder = { Text("Search species…") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (searchingSpecies) {
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                            IconButton(onClick = onOpenSpeciesSearch, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    FieldMindIcons.Nature,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 20.dp
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                // ── Species suggestion dropdown (up to 5 results) ──
                AnimatedVisibility(visible = showSuggestions) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            speciesSuggestions.take(5).forEach { record ->
                                Surface(
                                    onClick = {
                                        selectedSpecies = record.commonName
                                        speciesSearchQuery.value = record.commonName
                                        onSessionChange(
                                            session.copy(
                                                speciesName = record.commonName,
                                                subject = if (session.subject.isBlank()) record.commonName else session.subject,
                                                category = record.category
                                            )
                                        )
                                        showSuggestions = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(FieldMindIcons.Nature, null, tint = FieldMindTheme.colors.observation, size = 16.dp)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(record.commonName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (record.scientificName.isNotBlank()) {
                                                    Text(record.scientificName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (record.category != "Other") {
                                                    Text(record.category, style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.accentFor(record.category))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Species search helper ──
                if (showSpeciesSearch && !showSuggestions && speciesSearchQuery.value.isNotBlank()) {
                    Surface(
                        onClick = onOpenSpeciesSearch,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(FieldMindIcons.Search, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Open species catalog", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ── Selected species info card ──
                val currentRecord = selectedSpeciesRecord
                AnimatedVisibility(visible = currentRecord != null && speciesSearchQuery.value.isNotBlank()) {
                    currentRecord?.let { record ->
                        SpeciesInfoCard(
                            record = record,
                            showTaxonomy = showTaxonomy,
                            onToggleTaxonomy = { showTaxonomy = !showTaxonomy },
                            onOpenDetail = { showSpeciesDetailSheet = true }
                        )
                    }
                }
            }

            // ── Category Dropdown ──
            OptionPickerField(
                label = "Category",
                selected = session.category,
                options = expandedObservationCategories,
                onSelected = { onSessionChange(session.copy(category = it)) },
                icon = FieldMindIcons.Category
            )

            // ── Confidence Dropdown ──
            OptionPickerField(
                label = "Confidence",
                selected = session.confidence,
                options = expandedConfidenceOptions,
                onSelected = { onSessionChange(session.copy(confidence = it)) },
                icon = FieldMindIcons.Check
            )

            // ── Behavior Dropdown ──
            OptionPickerField(
                label = "Behavior",
                selected = session.behavior.ifBlank { "Select behavior…" },
                options = behaviorOptions,
                onSelected = { onSessionChange(session.copy(behavior = it)) },
                icon = FieldMindIcons.Trend
            )

            // ── Life Stage + Sex row ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionPickerField(
                    label = "Life stage",
                    selected = session.lifeStage.ifBlank { "Select…" },
                    options = lifeStageOptions,
                    onSelected = { onSessionChange(session.copy(lifeStage = it)) },
                    icon = FieldMindIcons.Question,
                    modifier = Modifier.weight(1f)
                )
                OptionPickerField(
                    label = "Sex",
                    selected = session.sex.ifBlank { "Select…" },
                    options = sexOptions,
                    onSelected = { onSessionChange(session.copy(sex = it)) },
                    icon = FieldMindIcons.Question,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Habitat + Quality row ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionPickerField(
                    label = "Habitat",
                    selected = session.habitatType.ifBlank { "Select…" },
                    options = habitatTypeOptions,
                    onSelected = { onSessionChange(session.copy(habitatType = it)) },
                    icon = FieldMindIcons.Nature,
                    modifier = Modifier.weight(1f)
                )
                OptionPickerField(
                    label = "Quality",
                    selected = session.observationQuality,
                    options = observationQualityOptions,
                    onSelected = { onSessionChange(session.copy(observationQuality = it)) },
                    icon = FieldMindIcons.Check,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Weather Override ──
            OptionPickerField(
                label = "Weather",
                selected = session.weatherOverride,
                options = weatherConditionOptions,
                onSelected = { onSessionChange(session.copy(weatherOverride = it)) },
                icon = FieldMindIcons.Weather
            )

            // ── Facts-only notes ──
            FactsInterpretationBanner()
            FieldTextField(
                session.facts,
                { onSessionChange(session.copy(facts = it)) },
                "Facts-only notes",
                minLines = 3,
                supportingText = "What did you see/hear/measure? Keep guesses out."
            )

            // ── Tags ──
            FieldTextField(
                session.tags,
                { onSessionChange(session.copy(tags = it)) },
                "Tags",
                supportingText = "Comma-separated: birds, behavior, evening"
            )

            // ── Advanced fields (location, context) ──
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Icon(if (showAdvanced) FieldMindIcons.Up else FieldMindIcons.Down, null, size = 18.dp)
                Spacer(Modifier.size(6.dp))
                Text(if (showAdvanced) "Hide location & context" else "Show location & context")
            }
            AnimatedVisibility(visible = showAdvanced, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FieldTextField(
                        session.manualLocation,
                        { onSessionChange(session.copy(manualLocation = it)) },
                        "Place / location"
                    )
                    MultiSelectPickerField(label = "Context presets", selected = if (session.fieldContext.isBlank()) emptySet() else session.fieldContext.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(), options = contextPresets, onSelectionChanged = { onSessionChange(session.copy(fieldContext = it.joinToString(", "))) }, subtitle = "Select field conditions", icon = FieldMindIcons.Info, showSearch = false)
                    FieldTextField(
                        session.fieldContext,
                        { onSessionChange(session.copy(fieldContext = it)) },
                        "Context / mood",
                        minLines = 2
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Save Draft + Save Obs row per spec ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onSessionChange(session.copy(isDraft = true))
                        onSave()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    enabled = session.subject.isNotBlank() || session.facts.isNotBlank()
                ) {
                    Icon(FieldMindIcons.Archive, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Save draft")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    enabled = session.subject.isNotBlank() || session.facts.isNotBlank()
                ) {
                    Icon(FieldMindIcons.Check, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Save observation")
                }
            }

            // ── Species detail sheet dialog ──
            if (showSpeciesDetailSheet) {
                val detailRecord = selectedSpeciesRecord
                if (detailRecord != null) {
                    SpeciesDetailSheet(
                        record = detailRecord,
                        onDismiss = { showSpeciesDetailSheet = false }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Quality Score Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QualityScoreCard(score: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                    Text("Quality", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Text("$score%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp)).height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Missing Fields Checklist
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MissingFieldsChecklist(
    hasSubject: Boolean,
    hasEvidence: Boolean,
    hasLocation: Boolean,
    hasWeather: Boolean,
    hasMeasurements: Boolean,
    hasNotes: Boolean,
    hasDuration: Boolean,
    hasConfidence: Boolean
) {
    val missing = buildList {
        if (!hasSubject) add("subject")
        if (!hasEvidence) add("evidence")
        if (!hasLocation) add("location")
        if (!hasNotes) add("notes")
        if (!hasConfidence) add("confidence")
    }
    if (missing.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 16.dp)
            Text("Missing: ${missing.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
        }
    }
}

internal fun calculateObservationQuality(
    hasSubject: Boolean,
    hasEvidence: Int,
    hasLocation: Boolean,
    hasWeather: Boolean,
    hasMeasurements: Boolean,
    hasNotes: Boolean,
    hasDuration: Boolean,
    hasConfidence: Boolean
): Int {
    val checks = listOf(hasSubject, hasEvidence > 0, hasLocation, hasWeather, hasMeasurements, hasNotes, hasDuration, hasConfidence)
    val passed = checks.count { it }
    return (passed * 100) / checks.size
}

// ══════════════════════════════════════════════════════════════════════
//  Field Mode (unchanged from original)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FieldModeScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val gpsMode by viewModel.fieldSettings.gpsMode.collectAsState()
    val autoWeatherEnabled by viewModel.fieldSettings.autoWeatherEnabled.collectAsState()
    val locationMode by viewModel.fieldSettings.locationMode.collectAsState()
    // Field mode defaults from Settings
    val fieldModeDefaultSession by viewModel.fieldSettings.fieldModeDefaultSession.collectAsState()
    val fieldModeAutoStartTimer by viewModel.fieldSettings.fieldModeAutoStartTimer.collectAsState()
    val fieldModeObservationSpacing by viewModel.fieldSettings.fieldModeObservationSpacing.collectAsState()

    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()
    var showFull by remember { mutableStateOf(false) }
    var quickSnapCategory by remember { mutableStateOf(observationCategories.first()) }
    var quickSnapUri by remember { mutableStateOf<Uri?>(null) }
    var showQuickSnapCategory by remember { mutableStateOf(false) }
    var showQuickSnapCamera by remember { mutableStateOf(false) }
    var quickSnapStatus by remember { mutableStateOf<String?>(null) }
    val locationProvider = remember { FieldLocationProvider(context) }
    val canAutoLocate = gpsMode != "Off" && locationMode != "Manual only"

    // ── Observation spacing cooldown (prevents rapid duplicate saves) ──
    var lastSaveTime by remember { mutableLongStateOf(0L) }
    val spacingMs = remember(fieldModeObservationSpacing) {
        when (fieldModeObservationSpacing) {
            "30s" -> 30_000L
            "1m" -> 60_000L
            "5m" -> 300_000L
            else -> 0L
        }
    }

    // ── Auto-start timer when field mode opens ──
    var sessionTimerStarted by remember { mutableStateOf(false) }
    var timerStartTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(fieldModeAutoStartTimer) {
        if (fieldModeAutoStartTimer && !sessionTimerStarted) {
            timerStartTime = System.currentTimeMillis()
            sessionTimerStarted = true
        }
    }
    // Tick every second when timer is running
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(sessionTimerStarted) {
        if (sessionTimerStarted && fieldModeAutoStartTimer) {
            while (true) {
                delay(1000)
                tick++
            }
        }
    }
    val elapsedFormatted = remember(tick, timerStartTime, sessionTimerStarted) {
        if (sessionTimerStarted && fieldModeAutoStartTimer && timerStartTime > 0L) {
            val elapsed = System.currentTimeMillis() - timerStartTime
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 60000) % 60
            val hours = elapsed / 3600000
            if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
            else "%02d:%02d".format(minutes, seconds)
        } else "00:00"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StandardScreenHeader(
                    title = "Field mode",
                    subtitle = when {
                        fieldModeAutoStartTimer && sessionTimerStarted ->
                            "$fieldModeDefaultSession mode · $elapsedFormatted elapsed"
                        fieldModeDefaultSession != "Quick capture" ->
                            "$fieldModeDefaultSession mode — one tap logs an observation"
                        else -> "One tap logs an observation. Add details later."
                    },
                    icon = FieldMindIcons.Bolt,
                    trailing = {
                        BackButton(onClick = onBack, icon = FieldMindIcons.Close, contentDescription = "Close")
                    }
                )
                }
                // Auto-start timer indicator
                if (fieldModeAutoStartTimer && sessionTimerStarted && timerStartTime > 0L) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(FieldMindIcons.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp)
                                Text("Session timer active — $elapsedFormatted", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                if (fieldModeObservationSpacing != "None") {
                                    Text("${fieldModeObservationSpacing} spacing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
                if (showFull) {
                    item { ObservationCaptureCard(viewModel = viewModel, compact = true) { showFull = false } }
                } else {
                    item { Text("Tap a type to save instantly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    item {
                        Button(onClick = { haptics.light(); showQuickSnapCategory = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Icon(icon = FieldMindIcons.Camera, contentDescription = null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Quick snap")
                        }
                    }
                    items(observationCategories.chunked(2)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { category ->
                                FieldModeButton(category, Modifier.weight(1f)) {
                                    // Apply spacing cooldown
                                    val now = System.currentTimeMillis()
                                    if (spacingMs > 0L && (now - lastSaveTime) < spacingMs) {
                                        scope.launch {
                                            snackbar.showSnackbar("Spacing: wait ${fieldModeObservationSpacing} between saves")
                                        }
                                        return@FieldModeButton
                                    }
                                    lastSaveTime = now
                                    haptics.confirm()
                                    viewModel.addObservation(
                                        subject = category, category = category,
                                        facts = "Quick field capture — add details later.",
                                        confidence = defaultConfidence, manualLocation = "", tags = "",
                                        evidence = "", context = ""
                                    ) { savedId ->
                                        scope.launch {
                                            val result = snackbar.showSnackbar("Saved $category", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) viewModel.archiveObservation(savedId)
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showFull = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Add full details instead")
                        }
                    }
                }
            }

            // ── Top snackbar overlay ──
            FieldMindSnackbarOverlay(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
    if (showQuickSnapCategory) {
        AlertDialog(
            onDismissRequest = { showQuickSnapCategory = false },
            icon = { Icon(icon = FieldMindIcons.Camera, contentDescription = null) },
            title = { Text("Choose quick snap category") },
            text = {
                QuickSnapCategoryPicker(
                    selectedCategory = quickSnapCategory,
                    onCategorySelected = { quickSnapCategory = it }
                )
            },
            confirmButton = { Button(onClick = { showQuickSnapCategory = false; showQuickSnapCamera = true }) { Text("Open in-app camera") } },
            dismissButton = { TextButton(onClick = { showQuickSnapCategory = false }) { Text("Cancel") } }
        )
    }


    if (showQuickSnapCamera) {
        Dialog(
            onDismissRequest = { showQuickSnapCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    scope.launch {
                        showQuickSnapCamera = false
                        quickSnapStatus = if (canAutoLocate) "Locating…" else "Saved without location"
                        val captured = if (canAutoLocate && locationProvider.hasAnyLocationPermission())
                            awaitCurrentLocation(locationProvider) else null
                        val weather = if (captured != null && autoWeatherEnabled) {
                            quickSnapStatus = "Fetching weather…"
                            viewModel.fetchWeatherSnapshot(captured.latitude, captured.longitude)
                        } else null
                        quickSnapStatus = when {
                            captured == null -> "Saved without location"
                            autoWeatherEnabled && weather == null -> "Weather unavailable"
                            else -> "Metadata attached"
                        }
                        viewModel.addObservation(
                            subject = quickSnapCategory,
                            category = quickSnapCategory,
                            facts = "Quick snap — add details later.",
                            confidence = defaultConfidence,
                            manualLocation = captured?.asDisplayText().orEmpty(),
                            tags = "quick-snap",
                            evidence = "Camera quick snap",
                            context = quickSnapStatus.orEmpty(),
                            latitude = captured?.latitude,
                            longitude = captured?.longitude,
                            weather = weather,
                            attachments = listOf(
                                DraftEvidenceAttachment("Photo", uri, "Quick snap", mimeType = mimeType)
                            )
                        ) {
                            showFastSnackbar(
                                snackbar, scope,
                                "Quick snap saved as $quickSnapCategory • ${quickSnapStatus.orEmpty()}"
                            )
                        }
                    }
                },
                onDismiss = { showQuickSnapCamera = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    quickSnapStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { quickSnapStatus = null },
            icon = { Icon(icon = FieldMindIcons.Check, contentDescription = null) },
            title = { Text("Quick snap metadata") },
            text = { Text("$quickSnapCategory • $status") },
            confirmButton = { TextButton(onClick = { quickSnapStatus = null }) { Text("Done") } }
        )
    }
}
@Composable
private fun QuickSnapCategoryPicker(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Pick the observation type for this photo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        observationCategories.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { category ->
                    val selected = selectedCategory == category
                    val accent = FieldMindTheme.colors.accentFor(category)
                    Surface(
                        onClick = { onCategorySelected(category) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                icon = FieldMindIcons.iconForCategory(category),
                                contentDescription = null,
                                tint = accent,
                                size = 18.dp
                            )
                            Text(
                                category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private suspend fun awaitCurrentLocation(provider: FieldLocationProvider): CapturedLocation? = suspendCancellableCoroutine { cont ->
    provider.requestCurrentLocation { captured -> if (cont.isActive) cont.resume(captured) }
}

@Composable
private fun FieldModeButton(category: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = FieldMindTheme.colors.accentFor("observation")
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon = FieldMindIcons.iconForCategory(category), contentDescription = null, tint = accent, size = 24.dp)
            }
            Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun ObservationCaptureCard(viewModel: FieldMindViewModel, compact: Boolean, initialCategory: String? = null, snapFirst: Boolean = false, onSaved: () -> Unit) {
    // Note: This function is called from FieldModeScreen's LazyColumn item.
    // All Modifier.weight calls are inside Row/Column scopes where they work correctly.
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    val defaultCategory by viewModel.fieldSettings.defaultCategory.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val mediaEnabled by viewModel.fieldSettings.mediaAttachmentsEnabled.collectAsState()
    val audioEnabled by viewModel.fieldSettings.audioRecordingEnabled.collectAsState()
    val autoWeatherEnabled by viewModel.fieldSettings.autoWeatherEnabled.collectAsState()
    var subject by remember { mutableStateOf("") }
    var category by remember(defaultCategory, initialCategory) { mutableStateOf(initialCategory ?: defaultCategory) }
    var facts by remember { mutableStateOf("") }
    var confidence by remember(defaultConfidence) { mutableStateOf(defaultConfidence.takeIf { it in confidenceOptions } ?: "Likely") }
    var observationMode by remember { mutableStateOf("Single observation") }
    var count by remember { mutableStateOf("") }
    var speciesConfidence by remember { mutableStateOf("Likely") }
    var observerDistance by remember { mutableStateOf("10m") }
    var checklist by remember { mutableStateOf(setOf("Seen")) }
    var height by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var colorDetail by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("None") }
    var manualLocation by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }
    var weatherSnapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherStatus by remember { mutableStateOf("Weather not fetched") }
    var fetchingWeather by remember { mutableStateOf(false) }
    var stopwatchStartedAt by remember { mutableStateOf<Long?>(null) }
    var stopwatchAccumulatedMs by remember { mutableLongStateOf(0L) }
    var stopwatchRunning by remember { mutableStateOf(false) }
    var manualDurationMinutes by remember { mutableStateOf("") }
    var changeAtMinutes by remember { mutableStateOf("") }
    var timeNote by remember { mutableStateOf("") }
    var showStructured by remember { mutableStateOf(false) }
    var structuredDetails by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var tags by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var fieldContext by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    var showInAppCamera by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    val locationProvider = remember { FieldLocationProvider(context) }
    val haptics = rememberFieldMindHaptics()
    val startLocating = {
        locating = true
        locationProvider.requestCurrentLocation { captured ->
            locating = false
            if (captured != null) {
                capturedLocation = captured; manualLocation = captured.asDisplayText()
                if (autoWeatherEnabled) {
                    fetchingWeather = true; weatherStatus = "Fetching weather…"
                    scope.launch { weatherSnapshot = viewModel.fetchWeatherSnapshot(captured.latitude, captured.longitude); weatherStatus = weatherSnapshot?.asDisplayText() ?: "Weather unavailable"; fetchingWeather = false }
                }
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                    if (!place.isNullOrBlank()) { val withPlace = captured.copy(placeName = place); capturedLocation = withPlace; manualLocation = withPlace.asDisplayText() }
                }
            }
            showFastSnackbar(snackbar, scope, captured?.let { loc -> 
                if (loc.accuracyMeters != null) "Location acquired ±${loc.accuracyMeters.toInt()}m" else "Location captured."
            } ?: "Couldn't get a fix.")
        }
    }
    LaunchedEffect(recording) { if (recording) { recordSeconds = 0; while (recording) { delay(1000); recordSeconds++ } } }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> if (uris.isEmpty()) scope.launch { snackbar.showSnackbar("Gallery selection cancelled.") }; attachments = attachments + uris.map { durableEvidenceAttachment(context, "Gallery", it, "Selected media") } }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; attachments = attachments + durableEvidenceAttachment(context, "File", uri, "Reference file / PDF") } }
    val audioImportPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri == null) scope.launch { snackbar.showSnackbar("Audio import cancelled.") } else { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; attachments = attachments + durableEvidenceAttachment(context, "Audio", uri, "Imported field audio") } }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> if (result.values.any { it }) startLocating(); else scope.launch { snackbar.showSnackbar("Location denied.") } }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createFieldMindFile(context, "audio", ".m4a"); val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            runCatching { newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); newRecorder.setOutputFile(file.absolutePath); newRecorder.prepare(); newRecorder.start(); audioFile = file; recorder = newRecorder; recording = true }
                .onFailure { newRecorder.release(); scope.launch { snackbar.showSnackbar("Could not start recording: ${it.localizedMessage}") } }
        } else scope.launch { snackbar.showSnackbar("Audio permission denied.") }
    }
    if (showInAppCamera) { Dialog(onDismissRequest = { showInAppCamera = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) { FieldMindCameraV2(onPhotoCaptured = { uri, mimeType -> attachments = attachments + DraftEvidenceAttachment("Photo", uri, "Camera photo", mimeType = mimeType); showInAppCamera = false; scope.launch { snackbar.showSnackbar("Photo captured.") } }, onDismiss = { showInAppCamera = false }, modifier = Modifier.fillMaxSize()) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnackbarHost(snackbar)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon = FieldMindIcons.Observation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 24.dp) }
                    Column(Modifier.weight(1f)) {
                        Text(if (compact) "Quick field note" else if (snapFirst) "Snap evidence" else "New observation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (snapFirst) "Evidence first, then facts-only observation notes." else "Date and time are stamped automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (snapFirst && mediaEnabled) {
                    CaptureStep("Evidence first", "Start with camera, gallery, or files before writing facts.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showInAppCamera = true }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp) }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp) }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp) }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                    }
                }
                val qualityMissing = listOfNotNull(
                    "subject".takeIf { subject.isBlank() },
                    "facts".takeIf { facts.isBlank() },
                    "location".takeIf { manualLocation.isBlank() && capturedLocation == null },
                    "weather".takeIf { weatherSnapshot == null },
                    "evidence".takeIf { attachments.isEmpty() }
                )
                ObservationQualityCard(score = ((5 - qualityMissing.size) * 20).coerceIn(0, 100), missing = qualityMissing)
                CaptureStep(if (snapFirst) "Subject & confidence" else "Subject", "What did you observe, and how sure are you?", FieldMindIcons.iconForCategory(category)) {
                    FieldTextField(subject, { value -> subject = value; tags = autoObservationTags(value, facts, category, tags) }, "Subject", supportingText = "Example: Crow on wire")
                    Text("Capture mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); OptionPickerField(label = "Capture mode", selected = observationMode, options = listOf("Single observation", "Each photo = observation"), onSelected = { observationMode = it }, icon = FieldMindIcons.Check, modifier = Modifier.fillMaxWidth())
                    if (!compact) { Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); OptionPickerField(label = "Category", selected = category, options = observationCategories, onSelected = { category = it; tags = autoObservationTags(subject, facts, it, tags) }, icon = FieldMindIcons.Category, modifier = Modifier.fillMaxWidth()) }
                    Text("Confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); OptionPickerField(label = "Confidence", selected = confidence, options = confidenceOptions, onSelected = { confidence = it }, icon = FieldMindIcons.Check, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(count, { count = it }, "Count", modifier = Modifier.weight(1f), decimalPlaces = 0, supportingText = "Number seen")
                        NumberField(observerDistance, { observerDistance = it }, "Distance (m)", modifier = Modifier.weight(1f), decimalPlaces = 0, suffix = "m", supportingText = "2, 10, 50, 100")
                    }
                    Text("Species confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); OptionPickerField(label = "Species confidence", selected = speciesConfidence, options = confidenceOptions, onSelected = { speciesConfidence = it }, icon = FieldMindIcons.Check, modifier = Modifier.fillMaxWidth())
                }
                CaptureStep(if (snapFirst) "Facts after evidence" else "Facts", "Record only what you observed — keep guesses out.", FieldMindIcons.Edit) {
                    FactsInterpretationBanner(); FieldTextField(facts, { value -> facts = value; tags = autoObservationTags(subject, value, category, tags) }, "Facts-only notes", minLines = if (compact) 3 else 5, supportingText = "Write only what you saw/heard/measured.")
                    ObservationChecklist(checklist) { checklist = it }
                    if (!compact) { MoodDropdown(fieldContext, { fieldContext = it }); FieldTextField(fieldContext, { fieldContext = it }, "Mood / field context", minLines = 2) }
                }
                CaptureStep("Location", "GPS is optional; manual place names work offline.", FieldMindIcons.Location) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { manualLocation = ""; capturedLocation = null }, Modifier.weight(1f)) { Text("Manual") }
                        FilledTonalButton(onClick = { if (locating) return@FilledTonalButton; if (locationProvider.hasAnyLocationPermission()) startLocating(); else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, modifier = Modifier.weight(1f), enabled = !locating) {
                            if (locating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer); Spacer(Modifier.size(6.dp)); Text("Locating…") } else { Icon(icon = FieldMindIcons.Location, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Use GPS") } }
                    }
                    capturedLocation?.let { loc -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.confidenceSure, size = 16.dp); Text(loc.asDisplayText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    FieldTextField(manualLocation, { manualLocation = it }, "Place / GPS note")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { val loc = capturedLocation; if (loc != null) { fetchingWeather = true; weatherStatus = "Fetching weather…"; scope.launch { weatherSnapshot = viewModel.fetchWeatherSnapshot(loc.latitude, loc.longitude); weatherStatus = weatherSnapshot?.asDisplayText() ?: "Weather unavailable"; fetchingWeather = false } } else scope.launch { snackbar.showSnackbar("Capture GPS before fetching weather.") } }, enabled = !fetchingWeather, modifier = Modifier.weight(1f)) { Text(if (fetchingWeather) "Weather…" else "Fetch weather") }
                        Text(weatherStatus, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CaptureStep("Timing", "Use the stopwatch or enter field timing manually.", FieldMindIcons.Timer) {
                    val liveElapsed = stopwatchAccumulatedMs + if (stopwatchRunning) (System.currentTimeMillis() - (stopwatchStartedAt ?: System.currentTimeMillis())) else 0L
                    Text(formatDurationCompact(liveElapsed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { if (!stopwatchRunning) { stopwatchStartedAt = System.currentTimeMillis(); stopwatchRunning = true } }, Modifier.weight(1f)) { Text(if (stopwatchAccumulatedMs == 0L) "Start" else "Resume") }
                        OutlinedButton(onClick = { if (stopwatchRunning) { stopwatchAccumulatedMs += System.currentTimeMillis() - (stopwatchStartedAt ?: System.currentTimeMillis()); stopwatchRunning = false } }, Modifier.weight(1f)) { Text("Pause") }
                        OutlinedButton(onClick = { stopwatchStartedAt = null; stopwatchAccumulatedMs = 0L; stopwatchRunning = false }, Modifier.weight(1f)) { Text("Reset") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FieldTextField(manualDurationMinutes, { manualDurationMinutes = it }, "Manual min", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number); FieldTextField(changeAtMinutes, { changeAtMinutes = it }, "Change +min", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) }
                    FieldTextField(timeNote, { timeNote = it }, "Timing note", minLines = 2)
                }
                CaptureStep("Structured details", observationCategoryDefinitions.firstOrNull { it.label == category }?.prompt ?: "Add category-specific fields.", FieldMindIcons.Data) {
                    TextButton(onClick = { showStructured = !showStructured }) { Text(if (showStructured) "Hide structured fields" else "Add structured details") }
                    if (showStructured) { observationCategoryDefinitions.firstOrNull { it.label == category }?.fields.orEmpty().forEach { field -> FieldTextField(structuredDetails[field.key].orEmpty(), { value -> structuredDetails = structuredDetails + (field.key to value) }, field.label, supportingText = field.hint) } }
                }
                CaptureStep("Measurements", "Structured measurements keep field evidence comparable.", FieldMindIcons.Graph) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(height, { height = it }, "Height (cm)", modifier = Modifier.weight(1f), decimalPlaces = 1, suffix = "cm")
                        NumberField(width, { width = it }, "Width (cm)", modifier = Modifier.weight(1f), decimalPlaces = 1, suffix = "cm")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(length, { length = it }, "Length (cm)", modifier = Modifier.weight(1f), decimalPlaces = 1, suffix = "cm")
                        NumberField(diameter, { diameter = it }, "Diameter (cm)", modifier = Modifier.weight(1f), decimalPlaces = 1, suffix = "cm")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(weight, { weight = it }, "Weight (g)", modifier = Modifier.weight(1f), decimalPlaces = 1, suffix = "g")
                        FieldTextField(colorDetail, { colorDetail = it }, "Color", modifier = Modifier.weight(1f))
                    }
                }
                CaptureStep("Follow-up", "Turn needs follow-up into an actionable reminder note.", FieldMindIcons.Notifications) { OptionPickerField(label = "Follow-up", selected = followUp, options = listOf("None", "Tomorrow", "3 days", "1 week", "Custom"), onSelected = { followUp = it }, icon = FieldMindIcons.Calendar, modifier = Modifier.fillMaxWidth()) }
                if (mediaEnabled && !snapFirst) {
                    CaptureStep("Evidence", "Back your observation with photos, files, or a voice note.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showInAppCamera = true }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp) }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp) }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp) }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                        
                        // Audio recording section
                        if (audioEnabled) {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(FieldMindIcons.Mic, null, tint = FieldMindTheme.colors.observation, size = 20.dp)
                                        Text("Voice note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        if (recording) {
                                            RecordingIndicator(recordSeconds)
                                        }
                                    }
                                    Button(onClick = {
                                        if (recording) { val file = audioFile; runCatching { recorder?.stop() }; recorder?.release(); recorder = null; recording = false; file?.let { attachments = attachments + DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4") }; scope.launch { snackbar.showSnackbar("Voice note attached.") } }
                                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) audioPermission.launch(Manifest.permission.RECORD_AUDIO) else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }, modifier = Modifier.fillMaxWidth(), colors = if (recording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) else ButtonDefaults.buttonColors()) {
                                        Icon(icon = if (recording) FieldMindIcons.Stop else FieldMindIcons.Mic, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text(if (recording) "Stop recording" else "Start recording voice note")
                                    }
                                    OutlinedButton(onClick = { audioImportPicker.launch(arrayOf("audio/*")) }, Modifier.fillMaxWidth()) { Icon(icon = FieldMindIcons.Mic, contentDescription = "Import audio", size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Or import audio file") }
                                }
                            }
                        }
                    }
                }
                CaptureStep("Connect & tag", "Summarize the evidence, tag it, and link a project.", FieldMindIcons.Link) {
                    FieldTextField(evidence, { evidence = it }, "Evidence summary"); FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated: birds, behavior, evening")
                    if (projects.isNotEmpty()) { Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); OptionPickerField(label = "Project", selected = projects.firstOrNull { it.id == projectId }?.title ?: "No project", options = listOf("No project") + projects.map { it.title }, onSelected = { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }, icon = FieldMindIcons.Project, modifier = Modifier.fillMaxWidth()) }
                }
                Button(onClick = {
                    if (subject.isBlank() || facts.isBlank()) scope.launch { snackbar.showSnackbar("Subject and factual notes are required.") } else { haptics.confirm()
                        val now = System.currentTimeMillis(); val liveElapsed = stopwatchAccumulatedMs + if (stopwatchRunning) (now - (stopwatchStartedAt ?: now)) else 0L
                        val manualDurationMs = manualDurationMinutes.toDoubleOrNull()?.let { (it * 60_000).toLong() }; val durationMs = manualDurationMs ?: liveElapsed.takeIf { it > 0L }
                        val changeDurationMs = changeAtMinutes.toDoubleOrNull()?.let { (it * 60_000).toLong() }
                        val enrichedDetails = structuredDetails + mapOf(
                            "captureMode" to observationMode,
                            "count" to count,
                            "speciesConfidence" to speciesConfidence,
                            "distanceFromObserver" to observerDistance,
                            "checklist" to checklist.joinToString(", "),
                            "height" to height,
                            "width" to width,
                            "length" to length,
                            "diameter" to diameter,
                            "weight" to weight,
                            "color" to colorDetail,
                            "followUp" to followUp
                        ).filterValues { it.isNotBlank() && it != "None" }
                        val finalTags = autoObservationTags(subject, facts, category, tags)
                        val finalEvidence = listOf(evidence, "Evidence count: ${attachments.size}", "Checklist: ${checklist.joinToString()}").filter { it.isNotBlank() }.joinToString(" | ")
                        viewModel.addObservation(subject, category, facts, confidence, manualLocation, finalTags, finalEvidence, fieldContext, projectId, capturedLocation?.latitude, capturedLocation?.longitude, attachments, weatherSnapshot, enrichedDetails.toJsonObject(), startedAt = stopwatchStartedAt, endedAt = if (durationMs != null) now else null, durationMs = durationMs, changeObservedAt = changeDurationMs?.let { (stopwatchStartedAt ?: now) + it }, changeDurationMs = changeDurationMs, timeNote = listOf(timeNote, "Follow-up: $followUp".takeIf { followUp != "None" }).filterNotNull().joinToString(" | ")) {
                            subject = ""; facts = ""; manualLocation = ""; tags = ""; evidence = ""; fieldContext = ""; attachments = emptyList(); capturedLocation = null; weatherSnapshot = null; structuredDetails = emptyMap(); stopwatchStartedAt = null; stopwatchAccumulatedMs = 0L; stopwatchRunning = false
                            scope.launch { snackbar.showSnackbar("Observation saved to your archive.") }; onSaved()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save observation")
                }
            }
        }
    }
}

// ── Helpers ──


@Composable
private fun ObservationQualityCard(score: Int, missing: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Observation Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$score%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp)))
            Text(if (missing.isEmpty()) "Complete core evidence captured." else "Missing: ${missing.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ObservationChecklist(selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    val options = listOf("Seen", "Heard", "Smelled", "Touched", "Measured")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Observation checklist", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MultiSelectPickerField(
            label = "How did you observe it?",
            selected = selected,
            options = options,
            onSelectionChanged = onSelected,
            accentColor = MaterialTheme.colorScheme.primary,
            subtitle = "Select all that apply"
        )
    }
}

@Composable
private fun MoodDropdown(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Mood / context preset", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(value.ifBlank { "Choose context" }, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Icon(FieldMindIcons.Down, null, size = 18.dp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                contextPresets.forEach { preset ->
                    DropdownMenuItem(text = { Text(preset) }, onClick = { onValueChange(preset); expanded = false })
                }
            }
        }
    }
}

private fun autoObservationTags(subject: String, facts: String, category: String, current: String): String {
    val base = current.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
    observationCategoryDefinitions.firstOrNull { it.label == category }?.defaultTags?.let { base.addAll(it) }
    val text = "$subject $facts".lowercase()
    val signals = listOf(
        "bird" to listOf("bird", "crow", "sparrow", "call", "nest", "feather"),
        "fungi" to listOf("mushroom", "fungus", "cap", "gill", "stem"),
        "water" to listOf("water", "pond", "river", "rain", "flow"),
        "insect" to listOf("ant", "bee", "butterfly", "insect", "larva"),
        "weather" to listOf("cloud", "wind", "rain", "temperature", "humid"),
        "behavior" to listOf("feeding", "carrying", "calling", "moving", "gathering")
    )
    signals.forEach { (tag, words) -> if (words.any { it in text }) base.add(tag) }
    return base.take(12).joinToString(", ")
}

@Composable
private fun FactsInterpretationBanner() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon = FieldMindIcons.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 18.dp)
        Text("Facts vs. interpretation: log what you sensed here; save guesses as a question or hypothesis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun RecordingIndicator(seconds: Int) {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(initialValue = 1f, targetValue = 0.25f, animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recDot")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(12.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Text("Recording…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.weight(1f))
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun Map<String, String>.toJsonObject(): String = entries
    .filter { it.value.isNotBlank() }
    .joinToString(prefix = "{", postfix = "}") { (key, value) -> "\"${key.replace("\"", "")}\":\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }

private fun DraftEvidenceAttachment.isImage(): Boolean =
    mimeType?.startsWith("image/") == true || type.equals("Photo", true) || type.equals("Gallery", true) ||
    Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)(\\?.*)?$", RegexOption.IGNORE_CASE).containsMatchIn(uri)

// ══════════════════════════════════════════════════════════════════════
//  Species Identification Button
// ══════════════════════════════════════════════════════════════════════

@Composable
internal fun SpeciesIdButton(
    attachments: List<DraftEvidenceAttachment>,
    identifiedSpecies: SpeciesMatch?,
    onIdentifyFromPhoto: (String?) -> Unit,
    onOpenSearch: () -> Unit
) {
    val colors = FieldMindTheme.colors
    val hasPhoto = attachments.any { it.isImage() }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (identifiedSpecies != null)
                colors.observation.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    FieldMindIcons.Nature,
                    null,
                    tint = if (identifiedSpecies != null) colors.observation else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 22.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (identifiedSpecies != null) "Identified: ${identifiedSpecies.commonName}"
                        else "Identify species",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (identifiedSpecies != null) "Confidence: ${(identifiedSpecies.confidence * 100).toInt()}% • ${identifiedSpecies.scientificName}"
                        else if (hasPhoto) "Tap to identify from attached photo"
                        else "Add a photo first, then identify",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onIdentifyFromPhoto(attachments.firstOrNull { it.isImage() }?.uri) },
                    enabled = hasPhoto && identifiedSpecies == null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(FieldMindIcons.Camera, null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("From photo", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(FieldMindIcons.Search, null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Search by name", style = MaterialTheme.typography.labelSmall)
                }
                if (identifiedSpecies != null) {
                    FilledTonalIconButton(
                        onClick = { /* Clear identification */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(FieldMindIcons.Close, null, size = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AttachmentPreviewList(items: List<DraftEvidenceAttachment>, onCaptionChange: (Int, String) -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.isImage()) {
                            AsyncImage(model = item.uri, contentDescription = item.caption.ifBlank { "Attached image" }, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                        } else {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                Icon(icon = if (item.type.equals("Audio", true)) FieldMindIcons.Mic else FieldMindIcons.File, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 26.dp)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            InfoChip(item.type, icon = if (item.isImage()) FieldMindIcons.Gallery else FieldMindIcons.File)
                            Text(item.uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton({ onRemove(index) }) { Text("Remove") }
                    }
                    FieldTextField(item.caption, { onCaptionChange(index, it) }, "Caption")
                }
            }
        }
    }
}
