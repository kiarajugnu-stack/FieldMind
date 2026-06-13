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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.CapturedLocation
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.io.File
// ══════════════════════════════════════════════════════════════════════
//  Capture / Field mode
// ══════════════════════════════════════════════════════════════════════

private enum class CaptureState { Idle, ChooseMode, ChooseCategory, Snap, Note }

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
    val defaultCategory by viewModel.fieldSettings.defaultCategory.collectAsState()
    var captureState by remember { mutableStateOf(CaptureState.Idle) }
    var selectedCategory by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    val haptics = rememberFieldMindHaptics()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FieldScreenHeader("Capture", "Start with Snap evidence or a free-form Note, then add category and details.", icon = FieldMindIcons.Capture) }
        item { PrimaryCaptureEntry(captureState, onStart = { haptics.light(); captureState = CaptureState.ChooseMode }, onClose = { haptics.light(); captureState = CaptureState.Idle }) }
        when (captureState) {
            CaptureState.ChooseMode -> item { CaptureModeChooser(
                onSnap = { haptics.light(); captureState = CaptureState.ChooseCategory },
                onNote = { haptics.light(); captureState = CaptureState.Note }
            ) }
            CaptureState.ChooseCategory -> item { CategoryFirstCard(selectedCategory, onCategory = { selectedCategory = it }, onSnap = { haptics.light(); captureState = CaptureState.Snap }) }
            CaptureState.Snap -> item { ObservationCaptureCard(viewModel = viewModel, compact = false, initialCategory = selectedCategory, snapFirst = true) { captureState = CaptureState.Idle } }
            CaptureState.Note -> item { NoteCaptureCard(viewModel = viewModel, initialCategory = selectedCategory) { captureState = CaptureState.Idle } }
            CaptureState.Idle -> Unit
        }
        item { SectionHeader("Recent captures", "${observations.size} observations • ${notes.size} notes") }
        if (observations.isEmpty() && notes.isEmpty()) item { EmptyState("No captures yet", "Snap factual evidence or draft a note. Observations stay facts-only; notes stay free-form.", icon = FieldMindIcons.Observation, actionLabel = "Start capture") { captureState = CaptureState.ChooseMode } }
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
                meta = buildList { add(item.category); add("${item.date} ${item.time}"); if (item.manualLocation.isNotBlank()) add(item.manualLocation); if (item.tags.isNotBlank()) add(item.tags) },
                onClick = { onOpenDetail("observation", item.id) }
            )
        }
    }
}

@Composable
private fun PrimaryCaptureEntry(state: CaptureState, onStart: () -> Unit, onClose: () -> Unit) {
    Button(onClick = if (state == CaptureState.Idle) onStart else onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Icon(icon = if (state == CaptureState.Idle) FieldMindIcons.Add else FieldMindIcons.Close, contentDescription = null, size = 20.dp)
        Spacer(Modifier.size(8.dp))
        Text(if (state == CaptureState.Idle) "Quick capture" else "Close capture")
    }
}

@Composable
private fun CaptureModeChooser(onSnap: () -> Unit, onNote: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("What are you capturing?", "Choose the smallest path that matches the moment.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CaptureModeTile("Snap", "Photo, gallery, or file first", FieldMindIcons.Camera, FieldMindTheme.colors.observation, Modifier.weight(1f), onSnap)
                CaptureModeTile("Note", "Title, facts, tags first", FieldMindIcons.Note, FieldMindTheme.colors.source, Modifier.weight(1f), onNote)
            }
        }
    }
}

@Composable
private fun CaptureModeTile(title: String, body: String, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(132.dp).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.24f else 0.14f)), contentAlignment = Alignment.Center) { Icon(icon = icon, contentDescription = null, tint = color, size = 26.dp) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryFirstCard(category: String, onCategory: (String) -> Unit, onSnap: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Choose a category", "This labels the evidence before the full snap form.")
            ChoiceChips(observationCategories, category, onSelected = onCategory)
            Button(onClick = onSnap, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Continue to snap evidence") }
        }
    }
}

@Composable
internal fun NoteCaptureCard(viewModel: FieldMindViewModel, initialCategory: String, onSaved: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var tags by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    val haptics = rememberFieldMindHaptics()
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        attachments = attachments + uris.map { durableEvidenceAttachment(context, "Gallery", it, "Note media") }
        scope.launch { snackbar.showSnackbar(if (uris.isEmpty()) "Gallery selection cancelled." else "Media copied into FieldMind evidence storage.") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + durableEvidenceAttachment(context, "File", uri, "Note attachment")
            scope.launch { snackbar.showSnackbar("File copied into FieldMind evidence storage.") }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnackbarHost(snackbar)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon = FieldMindIcons.Note, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("New note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Free-form notes are separate from facts-only observations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CaptureStep("Category", "Label the note before writing.", FieldMindIcons.Category) {
                    ChoiceChips(observationCategories, category) { category = it }
                }
                CaptureStep("Title, body & tags", "Prioritize what you want to remember.", FieldMindIcons.Edit) {
                    FieldTextField(title, { title = it }, "Title")
                    FieldTextField(body, { body = it }, "Body / facts / reflection", minLines = 5, supportingText = "Free-form note. Use observations for facts-only evidence.")
                    FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated tags")
                }
                CaptureStep("Links", "Optionally connect a project or source.", FieldMindIcons.Link) {
                    if (projects.isNotEmpty()) {
                        Text("Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
                    }
                    if (sources.isNotEmpty()) {
                        Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No source") + sources.map { it.title }, sources.firstOrNull { it.id == sourceId }?.title ?: "No source") { selected -> sourceId = sources.firstOrNull { it.title == selected }?.id }
                    }
                }
                CaptureStep("Optional evidence", "Attach supporting files without turning the note into an observation.", FieldMindIcons.File) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { haptics.light(); mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp) }
                        OutlinedButton(onClick = { haptics.light(); filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp) }
                    }
                    AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                }
                Button(onClick = {
                    if (title.isBlank() && body.isBlank()) scope.launch { snackbar.showSnackbar("Add a title or body before saving.") } else { haptics.confirm(); viewModel.addNote(title.ifBlank { body.take(36) }, body, category, tags, projectId, sourceId, attachments) {
                        title = ""; body = ""; tags = ""; attachments = emptyList(); projectId = null; sourceId = null
                        scope.launch { snackbar.showSnackbar("Note saved to your library.") }
                        onSaved()
                    }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save note")
                }
            }
        }
    }
}

@Composable
private fun FieldModeScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()
    var showFull by remember { mutableStateOf(false) }
    var quickSnapCategory by remember { mutableStateOf(observationCategories.first()) }
    var quickSnapUri by remember { mutableStateOf<Uri?>(null) }
    var showQuickSnapCategory by remember { mutableStateOf(false) }
    var showQuickSnapCamera by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { FieldScreenHeader("Field mode", "One tap logs an observation. Add details later.", icon = FieldMindIcons.Bolt, actionIcon = FieldMindIcons.Close, onAction = onBack) }
            if (showFull) {
                item { ObservationCaptureCard(viewModel = viewModel, compact = true) { showFull = false } }
            } else {
                item {
                    Text("Tap a type to save instantly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
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
                                haptics.confirm()
                                viewModel.addObservation(
                                    subject = category,
                                    category = category,
                                    facts = "Quick field capture — add details later.",
                                    confidence = defaultConfidence,
                                    manualLocation = "",
                                    tags = "",
                                    evidence = "",
                                    context = ""
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
    if (showQuickSnapCategory) {
        AlertDialog(
            onDismissRequest = { showQuickSnapCategory = false },
            icon = { Icon(icon = FieldMindIcons.Camera, contentDescription = null) },
            title = { Text("Choose quick snap category") },
            text = { ChoiceChips(observationCategories, quickSnapCategory) { quickSnapCategory = it } },
            confirmButton = {
                Button(onClick = {
                    showQuickSnapCategory = false
                    showQuickSnapCamera = true
                }) { Text("Open in-app camera") }
            },
            dismissButton = { TextButton(onClick = { showQuickSnapCategory = false }) { Text("Cancel") } }
        )
    }
    // In-app camera overlay for quick snap (V2 — full-screen, zoom, focus, grid, timer)
    if (showQuickSnapCamera) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    viewModel.addObservation(
                        subject = quickSnapCategory,
                        category = quickSnapCategory,
                        facts = "Quick snap — add details later.",
                        confidence = defaultConfidence,
                        manualLocation = "",
                        tags = "quick-snap",
                        evidence = "Camera quick snap",
                        context = "",
                        attachments = listOf(DraftEvidenceAttachment("Photo", uri, "Quick snap", mimeType = mimeType))
                    ) { scope.launch { snackbar.showSnackbar("Quick snap saved as $quickSnapCategory.") } }
                    showQuickSnapCamera = false
                },
                onDismiss = { showQuickSnapCamera = false }
            )
        }
    }
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
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
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
    var subject by remember { mutableStateOf("") }
    var category by remember(defaultCategory, initialCategory) { mutableStateOf(initialCategory ?: defaultCategory) }
    var facts by remember { mutableStateOf("") }
    var confidence by remember(defaultConfidence) { mutableStateOf(defaultConfidence) }
    var manualLocation by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }
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
                capturedLocation = captured
                manualLocation = captured.asDisplayText()
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                    if (!place.isNullOrBlank()) {
                        val withPlace = captured.copy(placeName = place)
                        capturedLocation = withPlace
                        manualLocation = withPlace.asDisplayText()
                    }
                }
            }
            scope.launch { snackbar.showSnackbar(captured?.let { "Location captured." } ?: "Couldn't get a fix. Check that location is on, then try again or type a place.") }
        }
    }

    LaunchedEffect(recording) {
        if (recording) {
            recordSeconds = 0
            while (recording) { kotlinx.coroutines.delay(1000); recordSeconds++ }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isEmpty()) scope.launch { snackbar.showSnackbar("Gallery selection cancelled.") }
        attachments = attachments + uris.map { durableEvidenceAttachment(context, "Gallery", it, "Selected media") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + durableEvidenceAttachment(context, "File", uri, "Reference file / PDF")
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        if (granted) startLocating()
        else scope.launch { snackbar.showSnackbar("Location denied. Manual place names remain available.") }
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
                audioFile = file
                recorder = newRecorder
                recording = true
            }.onFailure {
                newRecorder.release()
                scope.launch { snackbar.showSnackbar("Could not start audio recording: ${it.localizedMessage ?: "unknown error"}") }
            }
        } else scope.launch { snackbar.showSnackbar("Audio permission denied.") }
    }

    // In-app camera overlay (V2 — full-screen, zoom, focus, grid, timer, post-capture flow)
    if (showInAppCamera) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            FieldMindCameraV2(
                onPhotoCaptured = { uri, mimeType ->
                    attachments = attachments + DraftEvidenceAttachment("Photo", uri, "Camera photo", mimeType = mimeType)
                    showInAppCamera = false
                    scope.launch { snackbar.showSnackbar("Photo captured.") }
                },
                onDismiss = { showInAppCamera = false }
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnackbarHost(snackbar)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon = FieldMindIcons.Observation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (compact) "Quick field note" else if (snapFirst) "Snap evidence" else "New observation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (snapFirst) "Evidence first, then facts-only observation notes." else "Date and time are stamped automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (snapFirst && mediaEnabled) {
                    CaptureStep("Evidence first", "Start with camera, gallery, or files before writing facts.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showInAppCamera = true }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp)
                            }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp)
                            }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp)
                            }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                    }
                }

                CaptureStep(if (snapFirst) "Subject & confidence" else "Subject", "What did you observe, and how sure are you?", FieldMindIcons.iconForCategory(category)) {
                    FieldTextField(subject, { subject = it }, "Subject", supportingText = "Example: Crow on wire")
                    if (!compact) {
                        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(observationCategories, category) { category = it }
                    }
                    Text("Confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(confidenceOptions, confidence) { confidence = it }
                }

                CaptureStep(if (snapFirst) "Facts after evidence" else "Facts", "Record only what you observed — keep guesses out.", FieldMindIcons.Edit) {
                    FactsInterpretationBanner()
                    FieldTextField(facts, { facts = it }, "Facts-only notes", minLines = if (compact) 3 else 5, supportingText = "Write only what you saw/heard/measured. Put guesses in a question or hypothesis.")
                    if (!compact) {
                        Text("Context presets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(contextPresets, fieldContext) { fieldContext = if (fieldContext.isBlank()) it else "$fieldContext, $it" }
                        FieldTextField(fieldContext, { fieldContext = it }, "Mood / field context", supportingText = "Weather, light, surrounding activity, or constraints.")
                    }
                }

                CaptureStep("Location", "GPS is optional; manual place names work offline.", FieldMindIcons.Location) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { manualLocation = ""; capturedLocation = null }, Modifier.weight(1f)) { Text("Manual") }
                        FilledTonalButton(
                            onClick = {
                                if (locating) return@FilledTonalButton
                                if (locationProvider.hasAnyLocationPermission()) startLocating()
                                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !locating
                        ) {
                            if (locating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.size(6.dp)); Text("Locating…")
                            } else {
                                Icon(icon = FieldMindIcons.Location, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Use GPS")
                            }
                        }
                    }
                    capturedLocation?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.confidenceSure, size = 16.dp)
                            Text(loc.asDisplayText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FieldTextField(manualLocation, { manualLocation = it }, "Place / GPS note")
                }

                if (mediaEnabled && !snapFirst) {
                    CaptureStep("Evidence", "Back your observation with photos, files, or a voice note.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showInAppCamera = true }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp)
                            }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp)
                            }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp)
                            }
                        }
                        if (audioEnabled) {
                            if (recording) RecordingIndicator(recordSeconds)
                            Button(onClick = {
                                if (recording) {
                                    val file = audioFile
                                    runCatching { recorder?.stop() }
                                    recorder?.release(); recorder = null; recording = false
                                    file?.let { attachments = attachments + DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4") }
                                    scope.launch { snackbar.showSnackbar("Voice note attached.") }
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) audioPermission.launch(Manifest.permission.RECORD_AUDIO) else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }, modifier = Modifier.fillMaxWidth(), colors = if (recording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) else ButtonDefaults.buttonColors()) {
                                Icon(icon = if (recording) FieldMindIcons.Stop else FieldMindIcons.Mic, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text(if (recording) "Stop voice note" else "Record voice note")
                            }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                    }
                }

                CaptureStep("Connect & tag", "Summarize the evidence, tag it, and link a project.", FieldMindIcons.Link) {
                    FieldTextField(evidence, { evidence = it }, "Evidence summary")
                    FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated: birds, behavior, evening")
                    if (projects.isNotEmpty()) {
                        Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
                    }
                }

                Button(onClick = {
                    if (subject.isBlank() || facts.isBlank()) scope.launch { snackbar.showSnackbar("Subject and factual notes are required.") } else { haptics.confirm(); viewModel.addObservation(subject, category, facts, confidence, manualLocation, tags, evidence, fieldContext, projectId, capturedLocation?.latitude, capturedLocation?.longitude, attachments) {
                        subject = ""; facts = ""; manualLocation = ""; tags = ""; evidence = ""; fieldContext = ""; attachments = emptyList(); capturedLocation = null
                        scope.launch { snackbar.showSnackbar("Observation saved to your archive.") }
                        onSaved()
                    }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save observation")
                }
            }
        }
    }
}

/** Reminds the researcher to separate observed facts from interpretation. */
@Composable
private fun FactsInterpretationBanner() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon = FieldMindIcons.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 18.dp)
        Text("Facts vs. interpretation: log what you sensed here; save guesses as a question or hypothesis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** A blinking red dot + elapsed timer shown while a voice note is being recorded. */
@Composable
private fun RecordingIndicator(seconds: Int) {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recDot"
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(12.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Text("Recording…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.weight(1f))
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun DraftEvidenceAttachment.isImage(): Boolean =
    mimeType?.startsWith("image/") == true ||
        type.equals("Photo", true) || type.equals("Gallery", true) ||
        Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)(\\?.*)?$", RegexOption.IGNORE_CASE).containsMatchIn(uri)

@Composable
internal fun AttachmentPreviewList(items: List<DraftEvidenceAttachment>, onCaptionChange: (Int, String) -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.isImage()) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.caption.ifBlank { "Attached image" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
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

