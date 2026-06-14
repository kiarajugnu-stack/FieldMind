package fieldmind.research.app.features.field.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import fieldmind.research.app.features.field.data.vision.SpeciesClassifier
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
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
import java.io.File
import kotlin.coroutines.resume

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
private data class CaptureSessionState(
    val isActive: Boolean = false,
    val step: CaptureStep = CaptureStep.Evidence,
    val subject: String = "",
    val facts: String = "",
    val category: String = "Other",
    val confidence: String = "Sure",
    val tags: String = "",
    val evidence: String = "",
    val fieldContext: String = "",
    val manualLocation: String = "",
    val attachments: List<DraftEvidenceAttachment> = emptyList(),
    // Phase 3: Structured observation fields
    val speciesConfidence: String = "Likely",
    val distanceFromObserver: String = "10m",
    val observationChecklist: Set<String> = emptySet(),
    val measurements: Map<String, String> = emptyMap(),
    val followUpSchedule: String = "None",
    val qualityScore: Int = 0,
    // Live timer state
    val timerStartedAt: Long? = null,
    val timerAccumulatedMs: Long = 0L,
    val timerRunning: Boolean = false,
    val sessionObservationCount: Int = 0
)

private enum class CaptureStep { Evidence, Details, Complete }

@Composable
fun ObserveScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    compactFieldMode: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    if (compactFieldMode) { FieldModeScreen(viewModel, onBack ?: {}); return }

    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val mediaEnabled by viewModel.fieldSettings.mediaAttachmentsEnabled.collectAsState()

    val haptics = rememberFieldMindHaptics()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // GPS location & accuracy
    val locationProvider = remember { FieldLocationProvider(context) }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }

    // Core session state
    var session by remember { mutableStateOf(CaptureSessionState()) }
    var showEvidenceForm by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf("Other")) }

    // Camera dialog state
    var showInAppCamera by remember { mutableStateOf(false) }

    // ── Species identification state ──
    val speciesClassifier = remember { SpeciesClassifier(context) }
    val speciesDatabase = remember { SpeciesDatabase(context) }
    var showSpeciesId by remember { mutableStateOf(false) }
    var speciesIdImageUri by remember { mutableStateOf<String?>(null) }
    var identifiedSpecies by remember { mutableStateOf<SpeciesMatch?>(null) }

    // ── Action: add attachment ──
    fun addAttachment(attachment: DraftEvidenceAttachment) {
        session = session.copy(attachments = session.attachments + attachment)
        showFastSnackbar(snackbar, scope, "${attachment.type} attached")
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

    // ── Action: start evidence capture ──
    fun startCapture() {
        haptics.light()
        session = session.copy(isActive = true, step = CaptureStep.Evidence)
        showEvidenceForm = true
        // Auto-start timer if not already running
        if (!session.timerRunning && session.timerStartedAt == null) {
            session = session.copy(timerStartedAt = System.currentTimeMillis(), timerRunning = true)
        }
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
            durationMs = liveElapsed.takeIf { it > 0L }
        ) {
            session = session.copy(
                subject = "", facts = "", tags = "", evidence = "",
                fieldContext = "", manualLocation = "", attachments = emptyList(),
                sessionObservationCount = session.sessionObservationCount + 1
            )
            showFastSnackbar(snackbar, scope, "Observation saved! Session: ${session.sessionObservationCount + 1}")
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                FieldScreenHeader(
                    "Observation",
                    "Capture evidence, time, place, weather, then add facts.",
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

            // ── Evidence-First Input ──
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

                // ── Quick Observation Form (auto-expanded after evidence) ──
                item {
                    // Phase 3: Calculate quality score
                    val qualityScore = calculateObservationQuality(
                        hasSubject = session.subject.isNotBlank(),
                        hasEvidence = session.attachments.size,
                        hasLocation = session.manualLocation.isNotBlank(),
                        hasWeather = false,  // Will be added via weather fetch
                        hasMeasurements = session.measurements.values.any { it.isNotBlank() },
                        hasNotes = session.facts.isNotBlank(),
                        hasDuration = false,
                        hasConfidence = session.confidence.isNotBlank()
                    )
                    
                    QuickObservationForm(
                        subject = session.subject,
                        onSubjectChange = { session = session.copy(subject = it) },
                        facts = session.facts,
                        onFactsChange = { session = session.copy(facts = it) },
                        category = session.category,
                        onCategoryChange = { session = session.copy(category = it) },
                        confidence = session.confidence,
                        onConfidenceChange = { session = session.copy(confidence = it) },
                        tags = session.tags,
                        onTagsChange = { session = session.copy(tags = it) },
                        evidenceSummary = session.evidence,
                        onEvidenceChange = { session = session.copy(evidence = it) },
                        fieldContext = session.fieldContext,
                        onFieldContextChange = { session = session.copy(fieldContext = it) },
                        manualLocation = session.manualLocation,
                        onLocationChange = { session = session.copy(manualLocation = it) },
                        projects = projects,
                        onSave = { saveObservation() },
                        // Phase 3 parameters
                        speciesConfidence = session.speciesConfidence,
                        onSpeciesConfidenceChange = { session = session.copy(speciesConfidence = it) },
                        distanceFromObserver = session.distanceFromObserver,
                        onDistanceChange = { session = session.copy(distanceFromObserver = it) },
                        observationChecklist = session.observationChecklist,
                        onChecklistChange = { session = session.copy(observationChecklist = it) },
                        measurements = session.measurements,
                        onMeasurementChange = { key, value -> 
                            val updated = session.measurements.toMutableMap()
                            updated[key] = value
                            session = session.copy(measurements = updated)
                        },
                        followUpSchedule = session.followUpSchedule,
                        onFollowUpChange = { session = session.copy(followUpSchedule = it) },
                        qualityScore = qualityScore
                    )
                }
            }

            // ── Recent captures ──
            item {
                SectionHeader(
                    "Recent observations",
                    "${observations.size} observations • ${notes.size} notes"
                )
            }

            if (observations.isEmpty() && notes.isEmpty()) {
                item {
                    EmptyState(
                        "No observations yet",
                        "Snap evidence or write facts. Observations stay facts-only; notes stay free-form.",
                        icon = FieldMindIcons.Observation,
                        actionLabel = "Start observation"
                    ) { startCapture() }
                }
            }

            items(notes.take(6), key = { "note-${it.id}" }) { item ->
                EntityCard(
                    title = item.title,
                    kind = "note",
                    body = item.body.take(140).ifBlank { "No body yet." },
                    meta = listOf(item.category, recentRelativeTime(item.updatedAt)),
                    onClick = { onOpenDetail("note", item.id) }
                )
            }

            items(observations.take(10), key = { "obs-${it.id}" }) { item ->
                EntityCard(
                    title = item.subject,
                    kind = "observation",
                    body = item.factsOnlyNotes.take(140).ifBlank { "No factual notes recorded." },
                    confidence = item.confidenceLevel,
                    meta = buildList {
                        add(item.category)
                        add("${item.date} ${item.time}")
                        if (item.manualLocation.isNotBlank()) add(item.manualLocation)
                        if (item.tags.isNotBlank()) add(item.tags)
                    },
                    onClick = { onOpenDetail("observation", item.id) }
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

    // ── Species identification sheet ──
    if (showSpeciesId && speciesIdImageUri != null) {
        SpeciesIdentificationSheet(
            imageUri = speciesIdImageUri,
            classifier = speciesClassifier,
            database = speciesDatabase,
            onSelectSpecies = { match ->
                identifiedSpecies = match
                session = session.copy(
                    subject = match.commonName,
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

    // ── In-app camera overlay ──
    if (showInAppCamera) {
        Dialog(
            onDismissRequest = { showInAppCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    addAttachment(
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
    onRemoveAttachment: (Int) -> Unit,
    onCaptionChange: (Int, String) -> Unit
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
                }
            }
            // Species identification button (always visible when form is active)
            Spacer(Modifier.height(8.dp))
            SpeciesIdButton(
                attachments = attachments,
                identifiedSpecies = null,
                onIdentifyFromPhoto = { uri ->
                    // This will be called from the ObserveScreen
                },
                onOpenSearch = { }
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
                    ChoiceChips(observationCategories, category, onSelected = onCategoryChange)
                    Text("Confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(confidenceOptions, confidence, onSelected = onConfidenceChange)
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
                    ChoiceChips(contextPresets, fieldContext) {
                        onFieldContextChange(if (fieldContext.isBlank()) it else "$fieldContext, $it")
                    }
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

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FieldScreenHeader(
                    "Field mode",
                    when {
                        fieldModeAutoStartTimer && sessionTimerStarted ->
                            "$fieldModeDefaultSession mode · $elapsedFormatted elapsed"
                        fieldModeDefaultSession != "Quick capture" ->
                            "$fieldModeDefaultSession mode — one tap logs an observation"
                        else -> "One tap logs an observation. Add details later."
                    },
                    icon = FieldMindIcons.Bolt,
                    actionIcon = FieldMindIcons.Close,
                    onAction = onBack
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
    }
    // ── Top snackbar overlay ──
    FieldMindSnackbarOverlay(
        hostState = snackbar,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    )
    }
    if (showQuickSnapCategory) {
        AlertDialog(
            onDismissRequest = { showQuickSnapCategory = false },
            icon = { Icon(icon = FieldMindIcons.Camera, contentDescription = null) },
            title = { Text("Choose quick snap category") },
            text = { ChoiceChips(observationCategories, quickSnapCategory) { quickSnapCategory = it } },
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
                    Text("Capture mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); ChoiceChips(listOf("Single observation", "Each photo = observation"), observationMode) { observationMode = it }
                    if (!compact) { Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); ChoiceChips(observationCategories, category) { category = it; tags = autoObservationTags(subject, facts, it, tags) } }
                    Text("Confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); ChoiceChips(confidenceOptions, confidence) { confidence = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(count, { count = it }, "Count", modifier = Modifier.weight(1f), decimalPlaces = 0, supportingText = "Number seen")
                        NumberField(observerDistance, { observerDistance = it }, "Distance (m)", modifier = Modifier.weight(1f), decimalPlaces = 0, suffix = "m", supportingText = "2, 10, 50, 100")
                    }
                    Text("Species confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); ChoiceChips(confidenceOptions, speciesConfidence) { speciesConfidence = it }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FieldTextField(manualDurationMinutes, { manualDurationMinutes = it }, "Manual min", modifier = Modifier.weight(1f)); FieldTextField(changeAtMinutes, { changeAtMinutes = it }, "Change +min", modifier = Modifier.weight(1f)) }
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
                CaptureStep("Follow-up", "Turn needs follow-up into an actionable reminder note.", FieldMindIcons.Notifications) { ChoiceChips(listOf("None", "Tomorrow", "3 days", "1 week", "Custom"), followUp) { followUp = it } }
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
                    if (projects.isNotEmpty()) { Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id } }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObservationChecklist(selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    val options = listOf("Seen", "Heard", "Smelled", "Touched", "Measured")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Observation checklist", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onSelected(if (option in selected) selected - option else selected + option) },
                    label = { Text(option) },
                    leadingIcon = if (option in selected) ({ Icon(FieldMindIcons.Check, null, size = 16.dp) }) else null
                )
            }
        }
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
