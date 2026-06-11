package chromahub.rhythm.app.features.field.presentation.screens

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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import chromahub.rhythm.app.features.field.data.ai.GeminiResearchAssistant
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.export.FieldMindExport
import chromahub.rhythm.app.features.field.data.location.CapturedLocation
import chromahub.rhythm.app.features.field.data.location.FieldLocationProvider
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Reading Insight", "Other")
private val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
private val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
private val questionStatuses = listOf("New", "Researching", "Tested", "Answered", "Abandoned")
private val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "PDF", "Note")
private val dataTools = listOf("Counter", "Measurement Log", "Checklist", "Event Log", "Weather Log", "Site Log", "Species Tracker", "Comparison Table")
private val reportTypes = listOf("Summary", "Field Report", "Literature Review", "Project Draft", "Findings Note", "Final Report")
private val learningModules = listOf(
    "Beginner" to listOf("Scientific thinking", "Observation", "Note-taking", "Identifying bias", "Basic biology", "Basic geology", "Reading graphs", "Asking testable questions", "Variables", "Simple data collection"),
    "Intermediate" to listOf("Research design", "Sampling", "Comparison", "Classification", "Literature review", "Writing summaries", "Data interpretation"),
    "Advanced" to listOf("Proposal writing", "Structured projects", "Analysis", "Citations", "Presentation", "Field methods", "Ethics")
)

@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        "Welcome to FieldMind" to "A local-first research notebook for Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive.",
        "Research begins with evidence" to "Capture facts, media, location, questions, and confidence without inventing conclusions.",
        "Build projects" to "Turn repeated curiosity into objectives, methods, data, sources, reports, and next steps.",
        "Optional AI" to "Gemini can assist only after you add a key and confirm what to send. AI suggestions are previews, never automatic truth.",
        "Own your work" to "Everything core works offline and can be exported as Markdown, CSV, JSON, or plain text."
    )
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EntityTypeBadge("Setup ${step + 1}/${pages.size}")
                Text(pages[step].first, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(pages[step].second, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ResearchCard("Field notebook rhythm", "Home for direction, Observe for fast evidence, Projects for structure, Library for sources, Learn for skill growth.", "Rhythm UI")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onFinish, Modifier.weight(1f)) { Text("Skip") }
                Button(onClick = { if (step < pages.lastIndex) step++ else onFinish() }, Modifier.weight(1f)) { Text(if (step < pages.lastIndex) "Next" else "Enter") }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: FieldMindViewModel, onOpenSettings: () -> Unit, onNavigate: (FieldMindScreen) -> Unit) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val tags by viewModel.commonTags.collectAsState()
    val goal by viewModel.fieldSettings.dailyObservationGoal.collectAsState()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayCount = observations.count { it.date == today }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { FieldMindTopBar("FieldMind", "Observe. Question. Research clearly.", action = "Settings", onAction = onOpenSettings) }
        item { FieldProgressCard("Today’s observation", "$todayCount / $goal logged", todayCount >= goal, onClick = { onNavigate(FieldMindScreen.Observe) }) }
        item { ResearchCard(projects.firstOrNull { it.status == "Active" }?.title ?: "Start a local research project", projects.firstOrNull()?.objective?.ifBlank { "Projects collect questions, observations, sources, data, reports, and next steps." } ?: "Create a project when curiosity becomes repeated work.", "Current project", onClick = { onNavigate(FieldMindScreen.Projects) }) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Observations", observations.size.toString(), Modifier.weight(1f)); StatPill("Questions", questions.count { it.status != "Answered" }.toString(), Modifier.weight(1f)); StatPill("Sources", sources.size.toString(), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Data", data.size.toString(), Modifier.weight(1f)); StatPill("Reports", reports.size.toString(), Modifier.weight(1f)); StatPill("Cards", flashcards.size.toString(), Modifier.weight(1f)) } }
        item { FieldSectionTitle("Quick add", "Every shortcut opens a real workflow.") }
        item { ActionGrid(listOf("Today’s observation" to FieldMindScreen.Observe, "Field mode" to FieldMindScreen.FieldMode, "New project" to FieldMindScreen.Projects, "Add source" to FieldMindScreen.Library, "Question bank" to FieldMindScreen.Questions, "Search archive" to FieldMindScreen.Search)) { onNavigate(it) } }
        item { FieldSectionTitle("Reading progress") }
        item { ResearchCard("${sources.count { it.readingStatus == "Finished" }} finished • ${sources.count { it.readingStatus != "Finished" }} in progress", sources.firstOrNull()?.title ?: "Save articles, PDFs, books, videos, and notes with active reading prompts.", "Library", onClick = { onNavigate(FieldMindScreen.Library) }) }
        item { FieldSectionTitle("Patterns", "Simple offline analysis from your archive.") }
        item { ResearchCard("Top tag: ${tags.firstOrNull()?.name ?: "None yet"}", "Common tags, repeated subjects, pending questions, and project progress appear as your archive grows.", "Analysis", onClick = { onNavigate(FieldMindScreen.Analysis) }) }
        item { FieldSectionTitle("Recent notes") }
        val activity = buildList { observations.take(3).forEach { add(Triple(it.subject, "${it.category} • ${it.date} ${it.time}", "Observation")) }; questions.take(2).forEach { add(Triple(it.questionText, "${it.status} • ${it.priority}", "Question")) }; reports.take(1).forEach { add(Triple(it.title, it.type, "Report")) } }
        if (activity.isEmpty()) item { EmptyResearchState("No activity yet", "Start with one factual observation. Small notes become research when you can revisit them.") } else items(activity) { TimelineItem(it.first, it.second, it.third) }
    }
}

@Composable
fun ObserveScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, compactFieldMode: Boolean = false) {
    val observations by viewModel.observations.collectAsState()
    var showCapture by remember { mutableStateOf(compactFieldMode) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FieldMindTopBar(if (compactFieldMode) "Field mode" else "Observe", if (compactFieldMode) "Fast capture outside with minimal friction." else "Journal facts, evidence, confidence, location, and tags.") }
        item { Button(onClick = { showCapture = !showCapture }, modifier = Modifier.fillMaxWidth()) { Text(if (showCapture) "Hide capture form" else if (compactFieldMode) "One-tap field note" else "New observation") } }
        if (showCapture) item { ObservationCaptureCard(viewModel = viewModel, compact = compactFieldMode) { showCapture = compactFieldMode } }
        item { FieldSectionTitle("Observation journal", "${observations.size} saved entries") }
        if (observations.isEmpty()) item { EmptyResearchState("No observations yet", "Capture subject, facts, evidence, location, context, confidence, and tags.") }
        items(observations) { item -> ResearchCard(item.subject, "${item.category} • ${item.confidenceLevel} • ${item.date} ${item.time}\n${item.manualLocation.ifBlank { "No manual place" }}${if (item.latitude != null) " • GPS saved" else ""}\n${item.factsOnlyNotes.take(140)}", "Observation") { onOpenDetail("observation", item.id) } }
    }
}

@Composable
private fun ObservationCaptureCard(viewModel: FieldMindViewModel, compact: Boolean, onSaved: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    val defaultCategory by viewModel.fieldSettings.defaultCategory.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val mediaEnabled by viewModel.fieldSettings.mediaAttachmentsEnabled.collectAsState()
    val audioEnabled by viewModel.fieldSettings.audioRecordingEnabled.collectAsState()
    var subject by remember { mutableStateOf("") }
    var category by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    var facts by remember { mutableStateOf("") }
    var confidence by remember(defaultConfidence) { mutableStateOf(defaultConfidence) }
    var manualLocation by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }
    var tags by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var fieldContext by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createFieldMindFileUri(context, "photo", ".jpg")
            pendingPhotoUri = uri
            // launched by effect below to keep launcher stable
        } else scope.launch { snackbar.showSnackbar("Camera permission denied. Text notes and gallery/file attachments still work.") }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri
        if (saved && uri != null) attachments = attachments + DraftEvidenceAttachment("Photo", uri.toString(), "Camera photo")
        scope.launch { snackbar.showSnackbar(if (saved) "Photo attached." else "Camera capture cancelled.") }
        pendingPhotoUri = null
    }
    LaunchedEffect(pendingPhotoUri) { pendingPhotoUri?.let { takePicture.launch(it) } }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isEmpty()) scope.launch { snackbar.showSnackbar("Gallery selection cancelled.") }
        attachments = attachments + uris.map { DraftEvidenceAttachment("Gallery", it.toString(), "Selected media") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + DraftEvidenceAttachment("File", uri.toString(), "Reference file / PDF")
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        if (granted) {
            val captured = FieldLocationProvider(context).lastKnownLocation()
            capturedLocation = captured
            manualLocation = captured?.asDisplayText().orEmpty()
            scope.launch { snackbar.showSnackbar(captured?.let { "Location captured." } ?: "Permission granted, but no recent device location was available. Enter a place manually.") }
        } else scope.launch { snackbar.showSnackbar("Location denied. Manual place names remain available.") }
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

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.padding(padding).fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FieldSectionTitle(if (compact) "Quick field note" else "New observation", "Date/time are saved automatically. Separate facts from interpretation.")
                LabeledTextField(subject, { subject = it }, "Subject", supportingText = "Example: Crow on wire")
                if (!compact) ChoiceChips(observationCategories, category) { category = it }
                LabeledTextField(facts, { facts = it }, "Facts-only notes", minLines = if (compact) 3 else 6, supportingText = "Write only what you saw/heard/measured. Put guesses in a question or hypothesis.")
                if (!compact) {
                    ChoiceChips(confidenceOptions, confidence) { confidence = it }
                    LabeledTextField(fieldContext, { fieldContext = it }, "Mood / field context", supportingText = "Weather, light, surrounding activity, or constraints.")
                }
                FieldSectionTitle("Location", "GPS is optional; manual place names always work offline.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { manualLocation = ""; capturedLocation = null }, Modifier.weight(1f)) { Text("Manual") }
                    FilledTonalButton(onClick = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, Modifier.weight(1f)) { Text("Use GPS") }
                }
                LabeledTextField(manualLocation, { manualLocation = it }, "Place / GPS note")
                if (mediaEnabled) {
                    FieldSectionTitle("Evidence", "Attach real media or files before saving.")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val uri = createFieldMindFileUri(context, "photo", ".jpg"); pendingPhotoUri = uri } else cameraPermission.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f)) { Text("Camera") }
                        OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) { Text("Gallery") }
                        OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Text("File") }
                    }
                    if (audioEnabled) {
                        Button(onClick = {
                            if (recording) {
                                val file = audioFile
                                runCatching { recorder?.stop() }
                                recorder?.release(); recorder = null; recording = false
                                file?.let { attachments = attachments + DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4") }
                                scope.launch { snackbar.showSnackbar("Voice note attached.") }
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) audioPermission.launch(Manifest.permission.RECORD_AUDIO) else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (recording) "Stop voice note" else "Record voice note") }
                    }
                    AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                }
                LabeledTextField(evidence, { evidence = it }, "Evidence summary")
                LabeledTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated: birds, behavior, evening")
                if (projects.isNotEmpty()) ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
                Button(onClick = {
                    if (subject.isBlank() || facts.isBlank()) scope.launch { snackbar.showSnackbar("Subject and factual notes are required.") } else viewModel.addObservation(subject, category, facts, confidence, manualLocation, tags, evidence, fieldContext, projectId, capturedLocation?.latitude, capturedLocation?.longitude, attachments) {
                        subject = ""; facts = ""; manualLocation = ""; tags = ""; evidence = ""; fieldContext = ""; attachments = emptyList(); capturedLocation = null
                        scope.launch { snackbar.showSnackbar("Observation saved to your archive.") }
                        onSaved()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save observation") }
            }
        }
    }
}

@Composable
fun ProjectsScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, startTab: Int = 0) {
    val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val observations by viewModel.observations.collectAsState(); val sources by viewModel.sources.collectAsState(); val tags by viewModel.commonTags.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab) }
    val tabs = listOf("Projects", "Questions", "Hypotheses", "Data", "Analysis", "Reports")
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab) { tabs.forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) } }
        when (tab) {
            0 -> ProjectPanel(viewModel, projects, onOpenDetail)
            1 -> QuestionPanel(viewModel, questions, onOpenDetail)
            2 -> HypothesisPanel(viewModel, hypotheses, questions, onOpenDetail)
            3 -> DataToolPanel(viewModel, data, onOpenDetail)
            4 -> AnalysisPanel(observations, questions, hypotheses, projects, sources, data, reports, tags)
            5 -> ReportPanel(viewModel, reports, onOpenDetail)
        }
    }
}

@Composable fun ResearchScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) = ProjectsScreen(viewModel, onOpenDetail)
@Composable fun CaptureScreen(viewModel: FieldMindViewModel) = ObserveScreen(viewModel)

@Composable private fun ProjectPanel(viewModel: FieldMindViewModel, items: List<ProjectEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create project") } }; if (items.isEmpty()) item { EmptyResearchState("No projects yet", "A workspace ties questions, observations, sources, data, analysis, conclusions, and reports together.") }; items(items) { ResearchCard(it.title, "${it.status} • ${it.topicType}\n${it.objective.ifBlank { it.researchQuestion.ifBlank { "Open project workspace" } }}", "Project") { onOpenDetail("project", it.id) } } }; if (show) NewProjectDialog(viewModel) { show = false } }
@Composable private fun QuestionPanel(viewModel: FieldMindViewModel, items: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create question") } }; if (items.isEmpty()) item { EmptyResearchState("No questions yet", "Researchers collect questions before answers. Keep them testable and linked to evidence.") }; items(items) { ResearchCard(it.questionText, "${it.status} • ${it.priority} • ${it.sourceType}", "Question") { onOpenDetail("question", it.id) } } }; if (show) NewQuestionDialog(viewModel) { show = false } }
@Composable private fun HypothesisPanel(viewModel: FieldMindViewModel, items: List<HypothesisEntity>, questions: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create hypothesis") } }; if (items.isEmpty()) item { EmptyResearchState("No hypotheses yet", "State predictions, support/weaken criteria, and test method before looking for results.") }; items(items) { ResearchCard(it.prediction, "${it.resultStatus} • confidence ${it.confidencePercent}%\nEvidence: ${it.evidenceNeeded}", "Hypothesis") { onOpenDetail("hypothesis", it.id) } } }; if (show) NewHypothesisDialog(viewModel, questions) { show = false } }
@Composable private fun DataToolPanel(viewModel: FieldMindViewModel, items: List<DataRecordEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Open data collection tools") } }; item { ResearchCard("Tools", "Counter, measurement log, checklist, event log, weather log, site log, species tracker, comparison table.", "Offline") }; items(items) { ResearchCard(it.label, "${it.toolType} • ${it.value} ${it.unit}\n${it.location}\n${it.notes}", "Data") { onOpenDetail("data", it.id) } } }; if (show) NewDataRecordDialog(viewModel) { show = false } }
@Composable private fun ReportPanel(viewModel: FieldMindViewModel, items: List<ReportEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Build report") } }; if (items.isEmpty()) item { EmptyResearchState("No reports yet", "Write background, question, method, observations, results, interpretation, conclusion, limitations, and next steps.") }; items(items) { ResearchCard(it.title, "${it.type} • ${it.status}\n${it.conclusion.ifBlank { it.question }}", "Report") { onOpenDetail("report", it.id) } } }; if (show) NewReportDialog(viewModel) { show = false } }

@Composable
fun KnowledgeLibraryScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val sources by viewModel.sources.collectAsState(); val flashcards by viewModel.flashcards.collectAsState(); var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) { TabRow(selectedTabIndex = tab) { listOf("Sources", "Paper Reading", "Flashcards").forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) } }; when (tab) { 0 -> SourcePanel(viewModel, sources, onOpenDetail); 1 -> PaperReadingPanel(sources, onOpenDetail); 2 -> FlashcardPanel(viewModel, flashcards, onOpenDetail) } }
}
@Composable private fun SourcePanel(viewModel: FieldMindViewModel, items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Add source") } }; if (items.isEmpty()) item { EmptyResearchState("No sources yet", "Save articles, videos, PDFs, books, summaries, citations, and what each source taught you.") }; items(items) { ResearchCard(it.title, "${it.type} • ${it.author.ifBlank { "Unknown author" }} • reliability ${it.reliabilityScore}/5\n${it.whatThisSourceTaughtMe.ifBlank { it.personalSummary }}", "Source") { onOpenDetail("source", it.id) } } }; if (show) NewSourceDialog(viewModel) { show = false } }
@Composable private fun PaperReadingPanel(items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { FieldSectionTitle("Paper reading mode", "Prompts: topic, problem, method, result, unclear points, new question, next verification.") }; if (items.isEmpty()) item { EmptyResearchState("Add a source first", "Paper prompts are saved inside each source note.") }; items(items) { ResearchCard(it.title, it.paperNotes.ifBlank { "Open source detail and answer active-reading prompts." }, "Read") { onOpenDetail("source", it.id) } } } }
@Composable private fun FlashcardPanel(viewModel: FieldMindViewModel, items: List<FlashcardEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create flashcard") } }; if (items.isEmpty()) item { EmptyResearchState("No flashcards yet", "Turn terms, definitions, mistakes, source concepts, and questions into review cards.") }; items(items) { ResearchCard(it.front, it.back, it.type) { onOpenDetail("flashcard", it.id) } } }; if (show) NewFlashcardDialog(viewModel) { show = false } }

@Composable
fun LearnScreen(viewModel: FieldMindViewModel, onNavigate: (FieldMindScreen) -> Unit) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val sources by viewModel.sources.collectAsState(); val reports by viewModel.reports.collectAsState(); val projects by viewModel.projects.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FieldMindTopBar("Learn", "Skill tree, progress, revision, and research discipline.") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Entries", observations.size.toString(), Modifier.weight(1f)); StatPill("Questions", questions.size.toString(), Modifier.weight(1f)); StatPill("Sources", sources.size.toString(), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Projects", projects.size.toString(), Modifier.weight(1f)); StatPill("Reports", reports.size.toString(), Modifier.weight(1f)); StatPill("Sessions", observations.map { it.date }.distinct().size.toString(), Modifier.weight(1f)) } }
        item { ActionGrid(listOf("Flashcards" to FieldMindScreen.Library, "Reports" to FieldMindScreen.Reports, "Backup/export" to FieldMindScreen.BackupExport, "Progress" to FieldMindScreen.Progress)) { onNavigate(it) } }
        learningModules.forEach { (level, modules) -> item { FieldSectionTitle(level, "Tap modules as you practice them in your own projects.") }; items(modules) { module -> ResearchCard(module, learningModuleBody(module), level) } }
    }
}

@Composable
fun ArchiveScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val reports by viewModel.reports.collectAsState(); val flashcards by viewModel.flashcards.collectAsState(); var query by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldMindTopBar("Search archive", "Search forever by topic, date, place, source, project, status, and keyword.") }
        item { LabeledTextField(query, { query = it }, "Search") }
        item { Text("Filters supported: keyword, date range, project, category, source type, location, tag, status, and record type.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val q = query.trim().lowercase()
        fun matches(vararg parts: String) = q.isBlank() || parts.any { it.lowercase().contains(q) }
        items(observations.filter { matches(it.subject, it.category, it.factsOnlyNotes, it.manualLocation, it.tags) }) { ResearchCard(it.subject, it.factsOnlyNotes, "Observation") { onOpenDetail("observation", it.id) } }
        items(questions.filter { matches(it.questionText, it.category, it.status) }) { ResearchCard(it.questionText, it.status, "Question") { onOpenDetail("question", it.id) } }
        items(projects.filter { matches(it.title, it.topicType, it.objective, it.researchQuestion) }) { ResearchCard(it.title, it.objective, "Project") { onOpenDetail("project", it.id) } }
        items(sources.filter { matches(it.title, it.author, it.type, it.personalSummary) }) { ResearchCard(it.title, it.whatThisSourceTaughtMe, "Source") { onOpenDetail("source", it.id) } }
        items(reports.filter { matches(it.title, it.type, it.question, it.conclusion) }) { ResearchCard(it.title, it.conclusion, "Report") { onOpenDetail("report", it.id) } }
        items(flashcards.filter { matches(it.front, it.back, it.type) }) { ResearchCard(it.front, it.back, "Flashcard") { onOpenDetail("flashcard", it.id) } }
    }
}

@Composable
fun AnalysisScreen(viewModel: FieldMindViewModel) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val tags by viewModel.commonTags.collectAsState()
    AnalysisPanel(observations, questions, hypotheses, projects, sources, data, reports, tags)
}

@Composable
private fun AnalysisPanel(observations: List<ObservationEntity>, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>, projects: List<ProjectEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>, reports: List<ReportEntity>, tags: List<TagStatistic>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldMindTopBar("Analysis", "Offline pattern cards and simple progress summaries.") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Subjects", observations.map { it.subject }.distinct().size.toString(), Modifier.weight(1f)); StatPill("Open Q", questions.count { it.status != "Answered" }.toString(), Modifier.weight(1f)); StatPill("Sources", sources.size.toString(), Modifier.weight(1f)) } }
        item { ResearchCard("Repeated patterns", observations.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }.joinToString { "${it.key}: ${it.value}" }.ifBlank { "Add observations to discover repeated categories." }, "Trends") }
        item { ResearchCard("Common tags", tags.take(8).joinToString { "${it.name} (${it.observationCount})" }.ifBlank { "No tags yet." }, "Tags") }
        item { ResearchCard("Project progress", "${projects.count { it.status == "Active" }} active • ${reports.size} reports • ${data.size} data records • ${hypotheses.size} hypotheses", "Projects") }
        item { ResearchCard("Reading progress", "${sources.count { it.readingStatus == "Finished" }} finished • ${sources.count { it.readingStatus != "Finished" }} still active", "Library") }
    }
}

@Composable
fun BackupExportScreen(viewModel: FieldMindViewModel) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val snackbar = remember { SnackbarHostState() }
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val flashcards by viewModel.flashcards.collectAsState(); val attachmentMode by viewModel.fieldSettings.attachmentExportMode.collectAsState()
    var pendingText by remember { mutableStateOf("") }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("Export cancelled.") } else runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingText.toByteArray()) } }.onSuccess { scope.launch { snackbar.showSnackbar("Export written.") } }.onFailure { scope.launch { snackbar.showSnackbar("Export failed: ${it.localizedMessage}") } }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldMindTopBar("Backup & export", "Your research notes remain portable and owned by you.") }
        item { ResearchCard("Attachment mode", "$attachmentMode. Choose a different default in Settings.", "Media") }
        item { Button({ pendingText = FieldMindExport.observationsCsv(observations); createDoc.launch("fieldmind-observations.csv") }, Modifier.fillMaxWidth()) { Text("Export observations CSV") } }
        item { Button({ pendingText = FieldMindExport.dataCsv(data); createDoc.launch("fieldmind-data.csv") }, Modifier.fillMaxWidth()) { Text("Export data CSV") } }
        item { Button({ pendingText = FieldMindExport.archiveJson(observations, questions, hypotheses, projects, sources, data, reports, flashcards); createDoc.launch("fieldmind-archive.json") }, Modifier.fillMaxWidth()) { Text("Export archive JSON") } }
        item { Button({ pendingText = reports.joinToString("\n\n---\n\n") { FieldMindExport.buildMarkdownReport(it) }; createDoc.launch("fieldmind-reports.md") }, Modifier.fillMaxWidth()) { Text("Export reports Markdown") } }
    } }
}

@Composable
fun FieldMindSettingsScreen(viewModel: FieldMindViewModel? = null, onBack: () -> Unit, onResetOnboarding: () -> Unit) {
    val context = LocalContext.current
    val settings = viewModel?.fieldSettings ?: chromahub.rhythm.app.features.field.data.settings.FieldMindSettings.getInstance(context)
    val goal by settings.dailyObservationGoal.collectAsState(); val category by settings.defaultCategory.collectAsState(); val confidence by settings.defaultConfidence.collectAsState(); val locationMode by settings.locationMode.collectAsState(); val media by settings.mediaAttachmentsEnabled.collectAsState(); val audio by settings.audioRecordingEnabled.collectAsState(); val exportMode by settings.attachmentExportMode.collectAsState(); val ai by settings.geminiEnabled.collectAsState(); val key by settings.geminiApiKey.collectAsState(); val model by settings.geminiModel.collectAsState(); val confirm by settings.aiRequireConfirmBeforeSave.collectAsState(); val sendAttachments by settings.aiSendAttachments.collectAsState(); val reminders by settings.remindersEnabled.collectAsState(); val streaks by settings.streaksEnabled.collectAsState(); val exportFormat by settings.defaultExportFormat.collectAsState(); val privacy by settings.privacyLockEnabled.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldMindTopBar("FieldMind settings", "Durable controls for capture, AI, export, privacy, and Rhythm-style behavior.", "Back", onBack) }
        item { SettingsNumberRow("Daily observation goal", goal, { settings.setDailyObservationGoal(it) }, "Used by the Home dashboard and progress view.") }
        item { SettingChoice("Default category", observationCategories, category, settings::setDefaultCategory) }
        item { SettingChoice("Default confidence", confidenceOptions, confidence, settings::setDefaultConfidence) }
        item { SettingChoice("Location mode", listOf("Manual only", "Approximate", "Precise"), locationMode, settings::setLocationMode) }
        item { SettingsSwitchRow("Media attachments", media, settings::setMediaAttachmentsEnabled, "Enable camera, gallery, and file evidence tools.") }
        item { SettingsSwitchRow("Audio recording", audio, settings::setAudioRecordingEnabled, "Enable voice-note evidence capture.") }
        item { SettingChoice("Attachment export", listOf("Reference URIs", "Copy media later", "Skip media"), exportMode, settings::setAttachmentExportMode) }
        item { FieldSectionTitle("Gemini assistant", "Optional. Nothing is sent without user action.") }
        item { SettingsSwitchRow("Enable Gemini", ai, settings::setGeminiEnabled, "Requires API key and explicit confirmation before use.") }
        item { OutlinedTextField(value = key, onValueChange = settings::setGeminiApiKey, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), supportingText = { Text(if (key.isBlank()) "No key saved." else "Key saved locally in FieldMind settings.") }) }
        item { SettingChoice("Gemini model", listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"), model, settings::setGeminiModel) }
        item { SettingsSwitchRow("Confirm before saving AI output", confirm, settings::setAiRequireConfirmBeforeSave, "AI suggestions always stay as previews unless you apply them.") }
        item { SettingsSwitchRow("Allow attachment context for AI", sendAttachments, settings::setAiSendAttachments, "Off by default to protect field evidence privacy.") }
        item { FieldSectionTitle("Discipline and ownership") }
        item { SettingsSwitchRow("Reminders", reminders, settings::setRemindersEnabled, "Notification permission is only needed if reminders are enabled.") }
        item { SettingsSwitchRow("Streaks", streaks, settings::setStreaksEnabled, "Discipline made visible without replacing real work.") }
        item { SettingChoice("Default export", listOf("Markdown", "CSV", "JSON", "Plain text", "PDF later"), exportFormat, settings::setDefaultExportFormat) }
        item { SettingsSwitchRow("Privacy lock placeholder", privacy, settings::setPrivacyLockEnabled, "Persisted toggle; wire to app lock flow if the main app exposes one.") }
        item { ResearchCard("Theme", "FieldMind inherits Rhythm Material 3 light/dark/dynamic color controls and uses the same rounded settings rhythm.", "Appearance") }
        item { OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth()) { Text("Reset onboarding") } }
    }
}

@Composable
fun DetailScreen(kind: String, id: Long, viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val flashcards by viewModel.flashcards.collectAsState()
    val title = kind.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldMindTopBar(title, "Record detail workspace", "Back", onBack) }
        when (kind) {
            "observation" -> observations.firstOrNull { it.id == id }?.let { o -> item { ResearchCard(o.subject, "${o.date} ${o.time} • ${o.category} • ${o.confidenceLevel}\nLocation: ${o.manualLocation.ifBlank { "None" }}\nGPS: ${o.latitude?.let { "${o.latitude}, ${o.longitude}" } ?: "Not captured"}\nTags: ${o.tags}\n\n${o.factsOnlyNotes}\n\nEvidence: ${o.evidenceSummary}\nContext: ${o.moodOrContext}", "Observation") } }
            "question" -> questions.firstOrNull { it.id == id }?.let { q -> item { ResearchCard(q.questionText, "${q.category} • ${q.sourceType} • ${q.status} • ${q.priority}\nRelated project: ${q.relatedProjectId ?: "None"}", "Question") } }
            "hypothesis" -> hypotheses.firstOrNull { it.id == id }?.let { h -> item { ResearchCard(h.prediction, "Reasoning: ${h.reasoning}\nSupport: ${h.supportCriteria}\nWeaken: ${h.weakeningCriteria}\nTest: ${h.testMethod}\nResult: ${h.resultStatus}", "Hypothesis") } }
            "project" -> projects.firstOrNull { it.id == id }?.let { p -> item { ResearchCard(p.title, "Objective: ${p.objective}\nQuestion: ${p.researchQuestion}\nBackground: ${p.backgroundNotes}\nMethods: ${p.methods}\nData: ${p.dataSummary}\nAnalysis: ${p.analysis}\nConclusion: ${p.conclusion}\nFuture questions: ${p.futureQuestions}", "Project") } }
            "source" -> sources.firstOrNull { it.id == id }?.let { s -> item { ResearchCard(s.title, "${s.type} • ${s.author} • ${s.dateOrYear}\nLink: ${s.link}\nMain idea: ${s.personalSummary}\nKey findings: ${s.keyFindings}\nWhat this taught me: ${s.whatThisSourceTaughtMe}\nPaper prompts: ${s.paperNotes}\nQuestions: ${s.questionsGenerated}", "Source") } }
            "data" -> data.firstOrNull { it.id == id }?.let { d -> item { ResearchCard(d.label, "${d.toolType} • ${d.value} ${d.unit}\n${d.location}\n${d.notes}", "Data") } }
            "report" -> reports.firstOrNull { it.id == id }?.let { r -> item { ResearchCard(r.title, FieldMindExport.buildMarkdownReport(r), "Report") } }
            "flashcard" -> flashcards.firstOrNull { it.id == id }?.let { f -> item { ResearchCard(f.front, f.back, f.type) } }
        }
        item { AssistantPanel(viewModel) }
    }
}

@Composable private fun AssistantPanel(viewModel: FieldMindViewModel) { val enabled by viewModel.fieldSettings.geminiEnabled.collectAsState(); val key by viewModel.fieldSettings.geminiApiKey.collectAsState(); val assistant = remember(enabled, key) { GeminiResearchAssistant(enabled = enabled, apiKeyProvider = { key }) }; var suggestion by remember { mutableStateOf(assistant.researchMentorSuggestion()) }; ResearchCard(if (assistant.isAvailable()) "Gemini assistant ready" else "AI assistant disabled", suggestion.body, "AI preview"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Factuality" to assistant.observationFactualityReview(""), "Testability" to assistant.questionTestabilityCheck(""), "Writing" to assistant.writingImprovementPrompt()).forEach { (label, item) -> OutlinedButton({ suggestion = item }, Modifier.weight(1f)) { Text(label) } } } }

@Composable private fun AttachmentPreviewList(items: List<DraftEvidenceAttachment>, onCaptionChange: (Int, String) -> Unit, onRemove: (Int) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { items.forEachIndexed { index, item -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { EntityTypeBadge(item.type); TextButton({ onRemove(index) }) { Text("Remove") } }; Text(item.uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); LabeledTextField(item.caption, { onCaptionChange(index, it) }, "Caption") } } } } }
@Composable private fun FieldMindTopBar(title: String, subtitle: String, action: String? = null, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) } } }
@Composable private fun FieldProgressCard(title: String, body: String, complete: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick).animateContentSize(), colors = CardDefaults.cardColors(containerColor = if (complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(30.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { EntityTypeBadge(if (complete) "Complete" else "Today"); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(body, color = if (complete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ActionGrid(actions: List<Pair<String, FieldMindScreen>>, onNavigate: (FieldMindScreen) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { actions.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (label, screen) -> FilledTonalButton({ onNavigate(screen) }, Modifier.weight(1f)) { Text(label) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }
@Composable private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, body: String) { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, onCheckedChange) } } }
@Composable private fun SettingsNumberRow(title: String, value: Int, onValueChange: (Int) -> Unit, body: String) { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ onValueChange((value - 1).coerceAtLeast(0)) }) { Text("−") }; Text(value.toString(), style = MaterialTheme.typography.titleLarge); Button({ onValueChange(value + 1) }) { Text("+") } } } } }
@Composable private fun SettingChoice(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.SemiBold); ChoiceChips(options, selected, onSelected = onSelected) } } }

@Composable private fun NewQuestionDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var question by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Other") }; var source by remember { mutableStateOf("Observation") }; var status by remember { mutableStateOf("New") }; var priority by remember { mutableStateOf("Medium") }; FormDialog("New Question", onDismiss, { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); onDismiss() } }) { LabeledTextField(question, { question = it }, "Question", minLines = 2); ChoiceChips(observationCategories, category) { category = it }; ChoiceChips(sourceTypes, source) { source = it }; ChoiceChips(questionStatuses, status) { status = it }; ChoiceChips(listOf("Low", "Medium", "High"), priority) { priority = it }; Text("Testable questions name something you can observe, measure, compare, or verify.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun NewProjectDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var title by remember { mutableStateOf("") }; var topic by remember { mutableStateOf("Biology") }; var objective by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; FormDialog("New Project", onDismiss, { if (title.isNotBlank()) { viewModel.addProject(title, topic, objective, question); onDismiss() } }) { LabeledTextField(title, { title = it }, "Project title"); ChoiceChips(listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }; LabeledTextField(objective, { objective = it }, "Objective", minLines = 2); LabeledTextField(question, { question = it }, "Research question", minLines = 2) } }
@Composable private fun NewSourceDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("Article") }; var title by remember { mutableStateOf("") }; var author by remember { mutableStateOf("") }; var link by remember { mutableStateOf("") }; var summary by remember { mutableStateOf("") }; var taught by remember { mutableStateOf("") }; var findings by remember { mutableStateOf("") }; var questions by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var reliability by remember { mutableStateOf(3f) }; FormDialog("Add Source", onDismiss, { if (title.isNotBlank()) { viewModel.addSource(type, title, author, link, summary, taught, reliability.toInt(), findings, questions, notes); onDismiss() } }) { ChoiceChips(sourceLibraryTypes, type) { type = it }; LabeledTextField(title, { title = it }, "Title"); LabeledTextField(author, { author = it }, "Author / creator"); LabeledTextField(link, { link = it }, "Link / citation"); LabeledTextField(summary, { summary = it }, "Main idea", minLines = 2); LabeledTextField(findings, { findings = it }, "Key findings / definitions", minLines = 2); LabeledTextField(taught, { taught = it }, "What this source taught me", minLines = 2); LabeledTextField(questions, { questions = it }, "New questions", minLines = 2); LabeledTextField(notes, { notes = it }, "Paper reading prompts: topic, problem, method, result, unclear, new question, verify next", minLines = 4); Text("Credibility: ${reliability.toInt()}/5"); Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3) } }
@Composable private fun NewHypothesisDialog(viewModel: FieldMindViewModel, questions: List<QuestionEntity>, onDismiss: () -> Unit) { var prediction by remember { mutableStateOf("") }; var reasoning by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var support by remember { mutableStateOf("") }; var weaken by remember { mutableStateOf("") }; var test by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf(50f) }; val linked = questions.firstOrNull(); FormDialog("New Hypothesis", onDismiss, { if (prediction.isNotBlank()) { viewModel.addHypothesis(linked?.id, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test); onDismiss() } }) { linked?.let { Text("Linked question: ${it.questionText}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; LabeledTextField(prediction, { prediction = it }, "Prediction", minLines = 2); LabeledTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2); LabeledTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2); LabeledTextField(support, { support = it }, "Support criteria"); LabeledTextField(weaken, { weaken = it }, "Weakening criteria"); LabeledTextField(test, { test = it }, "Test method"); Text("Confidence: ${confidence.toInt()}%"); Slider(confidence, { confidence = it }, valueRange = 0f..100f) } }
@Composable private fun NewDataRecordDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var tool by remember { mutableStateOf("Counter") }; var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("0") }; var unit by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; FormDialog("Data Collection Tool", onDismiss, { if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, location); onDismiss() } }) { ChoiceChips(dataTools, tool) { tool = it }; LabeledTextField(label, { label = it }, "Label"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }; Text(value, style = MaterialTheme.typography.headlineSmall); Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }; TextButton({ value = "0" }) { Text("Reset") } }; LabeledTextField(value, { value = it }, "Value / checklist items / comparison samples"); LabeledTextField(unit, { unit = it }, "Unit", supportingText = "count, cm, °C, minutes"); LabeledTextField(location, { location = it }, "Location / site"); LabeledTextField(notes, { notes = it }, "Notes", minLines = 3) } }
@Composable private fun NewReportDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("Field Report") }; var title by remember { mutableStateOf("") }; var background by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var observations by remember { mutableStateOf("") }; var results by remember { mutableStateOf("") }; var interpretation by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var limitations by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; FormDialog("Report Builder", onDismiss, { if (title.isNotBlank()) { viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next); onDismiss() } }) { ChoiceChips(reportTypes, type) { type = it }; LabeledTextField(title, { title = it }, "Title"); LabeledTextField(background, { background = it }, "Background", minLines = 2); LabeledTextField(question, { question = it }, "Question", minLines = 2); LabeledTextField(methods, { methods = it }, "Methods", minLines = 2); LabeledTextField(observations, { observations = it }, "Observations", minLines = 2); LabeledTextField(results, { results = it }, "Data / results", minLines = 2); LabeledTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2); LabeledTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2); LabeledTextField(limitations, { limitations = it }, "Limitations", minLines = 2); LabeledTextField(next, { next = it }, "Next steps", minLines = 2); Text("Save generates a local Markdown draft for export.") } }
@Composable private fun NewFlashcardDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("concept") }; var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }; FormDialog("Create Flashcard", onDismiss, { if (front.isNotBlank() && back.isNotBlank()) { viewModel.addFlashcard(front, back, type); onDismiss() } }) { ChoiceChips(listOf("term", "definition", "concept", "question-answer", "mistake card"), type) { type = it }; LabeledTextField(front, { front = it }, "Front"); LabeledTextField(back, { back = it }, "Back", minLines = 3) } }
@Composable private fun FormDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }, confirmButton = { Button(onClick = onSave) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

private fun learningModuleBody(module: String): String = when (module) { "Observation" -> "Practice separating facts from guesses in the observation journal."; "Asking testable questions" -> "Convert curiosity into questions that can be observed, compared, or verified."; "Literature review" -> "Use sources, paper prompts, and what-this-taught-me summaries."; "Analysis" -> "Use tags, dates, projects, subjects, and data tools to find patterns."; else -> "Track this skill by applying it in notes, sources, projects, reports, and revision cards." }
private fun createFieldMindFile(context: Context, prefix: String, suffix: String): File { val dir = File(context.getExternalFilesDir(null), "fieldmind").apply { mkdirs() }; return File(dir, "$prefix-${System.currentTimeMillis()}$suffix") }
private fun createFieldMindFileUri(context: Context, prefix: String, suffix: String): Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", createFieldMindFile(context, prefix, suffix))
