package fieldmind.research.app.features.field.presentation.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.CapturedLocation
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationProvider = remember { FieldLocationProvider(context) }
    val projects by viewModel.projects.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val activeStoredSession = remember(researchSessions) { researchSessions.firstOrNull { it.status == "Active" } }

    // Session state
    var sessionActive by remember { mutableStateOf(activeStoredSession != null) }
    var sessionName by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var sessionElapsedMs by remember { mutableLongStateOf(0L) }
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

    fun addSessionAttachment(attachment: DraftEvidenceAttachment) {
        quickAttachments = quickAttachments + attachment
        scope.launch { snackbar.showSnackbar("${attachment.type} ready for next observation") }
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
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addSessionAttachment(DraftEvidenceAttachment("Audio", it.toString(), "Session audio"))
        }
    }

    fun fetchSessionLocation(fetchWeather: Boolean = false) {
        captureStatus = "Fetching GPS…"
        locationProvider.requestCurrentLocation { captured ->
            quickLocation = captured
            if (captured == null) {
                captureStatus = "GPS unavailable or permission missing"
                scope.launch { snackbar.showSnackbar(captureStatus) }
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

    // Timer
    LaunchedEffect(sessionActive) {
        if (sessionActive) {
            if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
            while (sessionActive) {
                delay(1000)
                sessionElapsedMs = System.currentTimeMillis() - sessionStartedAt
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

    fun smartSessionName(): String {
        val project = projects.firstOrNull { it.id == selectedProjectId }
        val categoryHint = quickCategory.takeIf { it != "Other" } ?: project?.topicType?.takeIf { it.isNotBlank() }
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        return listOfNotNull(categoryHint, project?.title).joinToString(" • ").ifBlank { "Field session" } + " · $time"
    }

    fun startSession() {
        val name = sessionName.ifBlank { smartSessionName() }
        sessionActive = true
        sessionStartedAt = System.currentTimeMillis()
        sessionElapsedMs = 0
        observationCount = 0
        sessionName = name
        viewModel.addResearchSession(name, selectedProjectId) { id -> activeSessionId = id }
        showResearchSessionNotification(context, name, "Research session is running")
        haptics.confirm()
    }

    fun endSession() {
        sessionActive = false
        activeSessionId?.let { viewModel.endResearchSession(it, observationCount, sessionElapsedMs) }
        cancelResearchSessionNotification(context)
        showSummary = true
        haptics.confirm()
    }

    fun saveQuickObservation() {
        if (quickSubject.isBlank() && quickFacts.isBlank()) return
        viewModel.addObservation(
            subject = quickSubject.ifBlank { quickCategory },
            category = quickCategory,
            facts = quickFacts.ifBlank { "Quick session observation" },
            confidence = defaultConfidence,
            manualLocation = quickPlaceName.ifBlank { quickLocation?.coordinateText().orEmpty() },
            tags = "research-session",
            evidence = quickAttachments.joinToString { it.type },
            context = "Research session: $sessionName",
            projectId = selectedProjectId,
            latitude = quickLocation?.latitude,
            longitude = quickLocation?.longitude,
            attachments = quickAttachments,
            weather = quickWeather,
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
            scope.launch { snackbar.showSnackbar("Observation #$observationCount saved") }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
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
                    onAction = onBack
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
                                ChoiceChips(
                                    listOf("No project") + projects.map { it.title },
                                    projects.firstOrNull { it.id == selectedProjectId }?.title ?: "No project"
                                ) { selected ->
                                    selectedProjectId = projects.firstOrNull { it.title == selected }?.id
                                }
                            }
                            Button(onClick = ::startSession, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Icon(FieldMindIcons.Bolt, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Start session")
                            }
                        }
                    }
                }
            } else {
                // Active session — timer + quick input
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
                        }
                        FlowRow(Modifier.padding(horizontal = 20.dp, vertical = 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, label = { Text("Camera/photo") }, leadingIcon = { Icon(FieldMindIcons.Camera, null, size = 18.dp) })
                            AssistChip(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, label = { Text("Gallery") }, leadingIcon = { Icon(FieldMindIcons.Gallery, null, size = 18.dp) })
                            AssistChip(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, label = { Text("Attach") }, leadingIcon = { Icon(FieldMindIcons.File, null, size = 18.dp) })
                            AssistChip(onClick = { audioPicker.launch(arrayOf("audio/*")) }, label = { Text("Audio") }, leadingIcon = { Icon(FieldMindIcons.Mic, null, size = 18.dp) })
                            AssistChip(onClick = { fetchSessionLocation(fetchWeather = false) }, label = { Text("GPS") }, leadingIcon = { Icon(FieldMindIcons.Location, null, size = 18.dp) })
                            AssistChip(onClick = { fetchSessionLocation(fetchWeather = true) }, label = { Text("Weather") }, leadingIcon = { Icon(FieldMindIcons.Weather, null, size = 18.dp) })
                        }
                        Text(captureStatus + if (quickAttachments.isNotEmpty()) " • ${quickAttachments.size} attachment${if (quickAttachments.size == 1) "" else "s"}" else "", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.End) {
                            Button(onClick = ::endSession, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(16.dp)) {
                                Text("End")
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
                            ChoiceChips(observationCategories, quickCategory) { quickCategory = it }
                            FieldTextField(quickSubject, { quickSubject = it }, "Subject", supportingText = "e.g. Crow on wire")
                            FieldTextField(quickFacts, { quickFacts = it }, "Facts", minLines = 2, supportingText = "What did you observe?")
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
        val notification = NotificationCompat.Builder(context, RESEARCH_SESSION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(text)
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
