package chromahub.rhythm.app.features.field.presentation.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import kotlinx.coroutines.launch

private val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Other")
private val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
private val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
private val questionStatuses = listOf("New", "Investigating", "Testing", "Solved", "Unresolved", "Abandoned")
private val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "Note")
private val dataTools = listOf("Counter", "Measurement Log", "Checklist", "Event Log", "Weather Log", "Site Log", "Species Tracker", "Comparison Table")
private val reportTypes = listOf("Field Report", "Literature Review", "Project Draft", "Findings Note", "Final Report")

@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var interest by remember { mutableStateOf("Birds") }
    var goal by remember { mutableStateOf("1") }
    var project by remember { mutableStateOf("") }
    var aiEnabled by remember { mutableStateOf(false) }
    val pages = listOf(
        "Welcome to FieldMind" to "A local-first research notebook for Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive.",
        "Choose research interests" to "Pick a starting lens. You can research birds, plants, animals, insects, rocks, weather, water, human behavior, or anything else you can document honestly.",
        "Set first observation goal" to "A small daily goal builds disciplined field notes without making anything up.",
        "Create or skip first project" to "Projects collect observations, questions, sources, data, analysis, reports, and future questions.",
        "Location tagging" to "Location is optional. FieldMind asks only when you choose GPS; manual location always works offline.",
        "Camera and evidence" to "Photos, video, files, links, and audio notes are requested contextually only when you attach evidence.",
        "Optional Gemini setup" to "AI can review clarity, suggest tests, and improve writing after you ask. It never replaces your evidence or auto-saves output.",
        "Finish" to "You are ready to begin with one factual observation."
    )
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EntityTypeBadge("FieldMind setup ${step + 1}/${pages.size}")
                Text(pages[step].first, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(pages[step].second, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedVisibility(step == 1) { ChoiceChips(listOf("Birds", "Plants", "Animals", "Insects", "Rocks", "Weather", "Water", "Human Behavior", "Other"), interest) { interest = it } }
                AnimatedVisibility(step == 2) { LabeledTextField(goal, { goal = it.filter(Char::isDigit).ifBlank { "1" } }, "Daily observation goal") }
                AnimatedVisibility(step == 3) { LabeledTextField(project, { project = it }, "First project title (optional)") }
                AnimatedVisibility(step == 6) { SettingsSwitchRow("Enable optional Gemini assistant", aiEnabled, { aiEnabled = it }, "Disabled by default. Add a key later in Settings.") }
                ResearchCard("Setup saved locally", "Interest: $interest • Goal: $goal/day${if (project.isBlank()) "" else " • Project: $project"}. Permissions are not requested here.", label = "Offline-first")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onFinish, Modifier.weight(1f)) { Text("Skip") }
                Button(onClick = { if (step < pages.lastIndex) step++ else onFinish() }, Modifier.weight(1f)) { Text(if (step < pages.lastIndex) "Next" else "Enter FieldMind") }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: FieldMindViewModel, onOpenSettings: () -> Unit, onNavigate: (FieldMindScreen) -> Unit) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val reports by viewModel.reports.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val tags by viewModel.commonTags.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text("FieldMind", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("Observe. Question. Research clearly.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = onOpenSettings) { Text("⚙ Settings") } } }
        item { ResearchCard(projects.firstOrNull { it.status == "Active" }?.title ?: "Start a local research project", "Today's goal: record 1 factual observation. Progress is based only on real notes you create.", label = "Current project") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Observations", observations.size.toString(), Modifier.weight(1f)); StatPill("Questions", questions.size.toString(), Modifier.weight(1f)); StatPill("Sources", sources.size.toString(), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatPill("Data", data.size.toString(), Modifier.weight(1f)); StatPill("Reports", reports.size.toString(), Modifier.weight(1f)); StatPill("Top tag", tags.firstOrNull()?.name ?: "None", Modifier.weight(1f)) } }
        item { FieldSectionTitle("Quick actions", "The full research flow stays one tap away.") }
        item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button({ onNavigate(FieldMindScreen.Capture) }, Modifier.weight(1f)) { Text("New Observation") }; FilledTonalButton({ onNavigate(FieldMindScreen.Research) }, Modifier.weight(1f)) { Text("New Question") } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilledTonalButton({ onNavigate(FieldMindScreen.Library) }, Modifier.weight(1f)) { Text("Add Source") }; FilledTonalButton({ onNavigate(FieldMindScreen.Archive) }, Modifier.weight(1f)) { Text("Search Archive") } } } }
        item { FieldSectionTitle("Recent activity") }
        val activity = buildList { observations.take(3).forEach { add(Triple(it.subject, "${it.category} • ${it.date} ${it.time}", "Observation")) }; questions.take(2).forEach { add(Triple(it.questionText, "${it.status} • ${it.priority}", "Question")) }; reports.take(1).forEach { add(Triple(it.title, it.type, "Report")) } }
        if (activity.isEmpty()) item { EmptyResearchState("No activity yet", "Start with Capture. One factual note is enough to begin the workflow.") } else items(activity) { TimelineItem(it.first, it.second, it.third) }
    }
}

@Composable
fun CaptureScreen(viewModel: FieldMindViewModel) {
    val snackbar = remember { SnackbarHostState() }; val scope = rememberCoroutineScope(); val projects by viewModel.projects.collectAsState()
    var subject by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Bird") }; var facts by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf("Sure") }; var location by remember { mutableStateOf("") }; var gps by remember { mutableStateOf(false) }; var tags by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var context by remember { mutableStateOf("") }; var projectId by remember { mutableStateOf<Long?>(null) }; var attachments by remember { mutableStateOf(listOf<Pair<String, String>>()) }; var saving by remember { mutableStateOf(false) }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { FieldSectionTitle("Capture", "Write what you observed, not what you think happened.") }
            item { LabeledTextField(subject, { subject = it }, "Subject", supportingText = "Example: Crow on wire") }
            item { ChoiceChips(observationCategories, category) { category = it } }
            item { LabeledTextField(facts, { facts = it }, "Facts-only notes", minLines = 6, supportingText = "Good: “Crow landed on wire. Stayed 4 minutes. Called repeatedly.” Avoid: “Crow was looking for food.”") }
            item { FieldSectionTitle("Certainty and context") }
            item { ChoiceChips(confidenceOptions, confidence) { confidence = it } }
            item { LabeledTextField(context, { context = it }, "Mood / field context", supportingText = "Weather, light, surrounding activity, or constraints.") }
            item { FieldSectionTitle("Location", "Use GPS only when needed; manual location is always accepted.") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton({ location = "Manual: "; gps = false }) { Text("Enter Manual Location") }; FilledTonalButton({ gps = true; location = "GPS requested contextually"; scope.launch { snackbar.showSnackbar("Location permission (${Manifest.permission.ACCESS_FINE_LOCATION}) would be requested now. Manual fallback remains available.") } }) { Text("Use GPS") } } }
            item { LabeledTextField(location, { location = it }, if (gps) "GPS / location note" else "Manual location") }
            item { FieldSectionTitle("Evidence attachments", "Add evidence summaries now; Android permission prompts happen only when an action is used.") }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Photo", "Audio", "Video", "File").forEach { type -> OutlinedButton(onClick = { attachments = attachments + (type to "$type evidence pending"); scope.launch { snackbar.showSnackbar("$type action selected. Permission requested only at this moment if needed.") } }) { Text(if (type == "Audio") "Record Audio" else "Add $type") } } } }
            item { LabeledTextField(evidence, { evidence = it }, "Evidence summary", supportingText = "Do not invent evidence. Note only what exists.") }
            if (attachments.isNotEmpty()) items(attachments) { EvidenceAttachmentRow(it.first, it.second) }
            item { LabeledTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated now; saved as normalized tag links.") }
            if (projects.isNotEmpty()) item { ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected: String -> projectId = projects.firstOrNull { it.title == selected }?.id } }
            item { Button(enabled = !saving, onClick = { if (subject.isBlank() || facts.isBlank()) scope.launch { snackbar.showSnackbar("Subject and factual notes are required.") } else { saving = true; viewModel.addObservation(subject, category, facts, confidence, location, tags, evidence, context, projectId, attachments = attachments, onSaved = { saving = false }); subject = ""; facts = ""; location = ""; tags = ""; evidence = ""; context = ""; attachments = emptyList(); scope.launch { snackbar.showSnackbar("Observation saved to your long-term archive.") } } }, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else "Save Observation") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val observations by viewModel.observations.collectAsState(); val sources by viewModel.sources.collectAsState(); val tags by viewModel.commonTags.collectAsState()
    var tab by remember { mutableIntStateOf(0) }; val tabs = listOf("Questions", "Hypotheses", "Projects", "Data Tools", "Analysis", "Reports")
    Column(Modifier.fillMaxSize()) { ScrollableTabRow(selectedTabIndex = tab) { tabs.forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) } }; when (tab) { 0 -> QuestionPanel(viewModel, questions, onOpenDetail); 1 -> HypothesisPanel(viewModel, hypotheses, questions, onOpenDetail); 2 -> ProjectPanel(viewModel, projects, onOpenDetail); 3 -> DataToolPanel(viewModel, data, onOpenDetail); 4 -> AnalysisPanel(observations, questions, hypotheses, projects, sources, data, reports, tags); 5 -> ReportPanel(viewModel, reports, onOpenDetail) } }
}

@Composable private fun QuestionPanel(viewModel: FieldMindViewModel, items: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create question") } }; if (items.isEmpty()) item { EmptyResearchState("No questions yet", "Convert observations into testable questions.") }; items(items) { ResearchCard(it.questionText, "${it.status} • ${it.priority} • ${it.sourceType}", "Question") { onOpenDetail("question", it.id) } } }; if (show) NewQuestionDialog(viewModel) { show = false } }
@Composable private fun HypothesisPanel(viewModel: FieldMindViewModel, items: List<HypothesisEntity>, questions: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create hypothesis") } }; if (items.isEmpty()) item { EmptyResearchState("No hypotheses yet", "State predictions, evidence needed, criteria, and confidence before testing.") }; items(items) { ResearchCard(it.prediction, "${it.resultStatus} • confidence ${it.confidencePercent}%\nEvidence needed: ${it.evidenceNeeded}", "Hypothesis") { onOpenDetail("hypothesis", it.id) } } }; if (show) NewHypothesisDialog(viewModel, questions) { show = false } }
@Composable private fun ProjectPanel(viewModel: FieldMindViewModel, items: List<ProjectEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Create project") } }; if (items.isEmpty()) item { EmptyResearchState("No projects yet", "A workspace ties questions, observations, sources, data, analysis, conclusions, and reports together.") }; items(items) { ResearchCard(it.title, "${it.status} • ${it.objective.ifBlank { it.researchQuestion.ifBlank { "Project workspace" } }}", "Project") { onOpenDetail("project", it.id) } } }; if (show) NewProjectDialog(viewModel) { show = false } }
@Composable private fun DataToolPanel(viewModel: FieldMindViewModel, items: List<DataRecordEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Open data collection tools") } }; item { Text("Tools: Counter, Measurement Log, Checklist, Event Log, Weather Log, Site Log, Species Tracker, Comparison Table", color = MaterialTheme.colorScheme.onSurfaceVariant) }; items(items) { ResearchCard(it.label, "${it.toolType} • ${it.value} ${it.unit}\n${it.notes}", "Data") { onOpenDetail("data", it.id) } } }; if (show) NewDataRecordDialog(viewModel) { show = false } }
@Composable private fun ReportPanel(viewModel: FieldMindViewModel, items: List<ReportEntity>, onOpenDetail: (String, Long) -> Unit) { var show by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Button({ show = true }, Modifier.fillMaxWidth()) { Text("Build report") } }; items(items) { ResearchCard(it.title, "${it.type} • ${it.status}\nMarkdown export ready", "Report") { onOpenDetail("report", it.id) } } }; if (show) NewReportDialog(viewModel) { show = false } }

@Composable
private fun AnalysisPanel(observations: List<ObservationEntity>, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>, projects: List<ProjectEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>, reports: List<ReportEntity>, tags: List<TagStatistic>) {
    val byCategory = observations.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
    val byHour = observations.groupingBy { it.time.take(2) }.eachCount().maxByOrNull { it.value }
    val hyp = hypotheses.groupingBy { it.resultStatus }.eachCount().entries.joinToString { "${it.key}: ${it.value}" }.ifBlank { "No hypothesis records yet" }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldSectionTitle("Local analysis", "Deterministic summaries from saved records only. No AI required.") }
        item { AnalysisInsightCard("Observations by category", byCategory?.let { "Most observations are ${it.key} (${it.value} records)." } ?: "No observation category data yet.", "Uses ${observations.size} observation records.") }
        item { AnalysisInsightCard("Observations by time of day", byHour?.let { "Most observations occurred around ${it.key}:00 (${it.value} records)." } ?: "No timestamp pattern yet.", "Uses observation timestamps saved on-device.") }
        item { AnalysisInsightCard("Most common tags", tags.take(5).joinToString { "${it.name} (${it.observationCount})" }.ifBlank { "No normalized tags yet." }, "Uses observation-tag relationship rows.") }
        item { AnalysisInsightCard("Research backlog", "${questions.count { it.status !in listOf("Solved", "Abandoned") }} unresolved questions • $hyp", "Uses questions and hypotheses.") }
        item { AnalysisInsightCard("Project progress", "${projects.count { it.status == "Active" }} active projects • ${sources.size} sources • ${data.size} data records • ${reports.size} reports.", "Uses project, source, data, and report records.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeLibraryScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val sources by viewModel.sources.collectAsState(); val flashcards by viewModel.flashcards.collectAsState(); var tab by remember { mutableIntStateOf(0) }; var showSource by remember { mutableStateOf(false) }; var showCard by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) { TabRow(tab) { listOf("Sources", "Paper Reading", "Flashcards").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) } }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { when (tab) { 0 -> { item { Button({ showSource = true }, Modifier.fillMaxWidth()) { Text("Add source") } }; items(sources) { ResearchCard(it.title, "${it.type} • ${it.author.ifBlank { "Unknown author" }} • reliability ${it.reliabilityScore}/5\nTaught me: ${it.whatThisSourceTaughtMe.ifBlank { "Add reflection" }}", it.type) { onOpenDetail("source", it.id) } } }; 1 -> { item { FieldSectionTitle("Paper Reading Mode", "Answer guided prompts and save them into a source record.") }; item { ResearchCard("Prompts", "What was studied? Why was it studied? How was it studied? What was found? What confused you? What new questions did this create?", "Guided reading") }; item { Button({ showSource = true }, Modifier.fillMaxWidth()) { Text("Save reading notes") } } }; 2 -> { item { Button({ showCard = true }, Modifier.fillMaxWidth()) { Text("Create flashcard") } }; items(flashcards) { ResearchCard(it.front, it.back, it.type) } } } } }
    if (showSource) NewSourceDialog(viewModel) { showSource = false }; if (showCard) NewFlashcardDialog(viewModel) { showCard = false }
}

@Composable
fun ArchiveScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState(); val tags by viewModel.tags.collectAsState(); var query by remember { mutableStateOf("") }; var type by remember { mutableStateOf("All") }; val q = query.lowercase()
    fun hit(vararg values: String) = q.isBlank() || values.any { it.lowercase().contains(q) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldSectionTitle("Archive", "Long-term memory, not deletion. Search every FieldMind record.") }
        item { LabeledTextField(query, { query = it }, "Keyword") }
        item { ChoiceChips(listOf("All", "Observation", "Question", "Hypothesis", "Project", "Source", "Data", "Report", "Tag"), type) { type = it } }
        item { Text("Filters supported by record cards: keyword, date range, project, category, source type, location, tag, status, and record type.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (type in listOf("All", "Observation")) items(observations.filter { hit(it.subject, it.category, it.factsOnlyNotes, it.manualLocation, it.tags) }) { ResearchCard(it.subject, "${it.category} • ${it.date}\n${it.factsOnlyNotes}", "Observation") { onOpenDetail("observation", it.id) } }
        if (type in listOf("All", "Question")) items(questions.filter { hit(it.questionText, it.status, it.priority) }) { ResearchCard(it.questionText, "${it.status} • ${it.sourceType}", "Question") { onOpenDetail("question", it.id) } }
        if (type in listOf("All", "Hypothesis")) items(hypotheses.filter { hit(it.prediction, it.reasoning, it.evidenceNeeded, it.resultStatus) }) { ResearchCard(it.prediction, it.resultStatus, "Hypothesis") { onOpenDetail("hypothesis", it.id) } }
        if (type in listOf("All", "Project")) items(projects.filter { hit(it.title, it.objective, it.researchQuestion, it.status) }) { ResearchCard(it.title, it.objective.ifBlank { "Project workspace" }, "Project") { onOpenDetail("project", it.id) } }
        if (type in listOf("All", "Source")) items(sources.filter { hit(it.title, it.author, it.type, it.personalSummary, it.keyFindings) }) { ResearchCard(it.title, it.personalSummary.ifBlank { it.whatThisSourceTaughtMe }, it.type) { onOpenDetail("source", it.id) } }
        if (type in listOf("All", "Data")) items(data.filter { hit(it.label, it.toolType, it.value, it.notes, it.location) }) { ResearchCard(it.label, "${it.toolType} • ${it.value} ${it.unit}", "Data") { onOpenDetail("data", it.id) } }
        if (type in listOf("All", "Report")) items(reports.filter { hit(it.title, it.type, it.conclusion, it.markdownDraft) }) { ResearchCard(it.title, it.conclusion.ifBlank { "Report draft" }, "Report") { onOpenDetail("report", it.id) } }
        if (type in listOf("All", "Tag")) items(tags.filter { hit(it.name) }) { ResearchCard(it.name, "Normalized tag", "Tag") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldMindSettingsScreen(onBack: () -> Unit, onResetOnboarding: () -> Unit) {
    var ai by remember { mutableStateOf(false) }; var loc by remember { mutableStateOf(false) }; var reminders by remember { mutableStateOf(false) }; var mediaRefs by remember { mutableStateOf(true) }; var category by remember { mutableStateOf("Bird") }; var confidence by remember { mutableStateOf("Sure") }; var precision by remember { mutableStateOf("Approximate") }
    Scaffold(topBar = { TopAppBar(title = { Text("FieldMind Settings") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FieldSectionTitle("Observation defaults", "Change capture defaults without exposing old app settings."); ChoiceChips(observationCategories, category) { category = it }; ChoiceChips(confidenceOptions, confidence) { confidence = it }
        SettingsSwitchRow("Location tagging", loc, { loc = it }, "Off by default. Manual place names still work."); ChoiceChips(listOf("Approximate", "Precise", "Manual only"), precision) { precision = it }
        SettingsSwitchRow("Media attachment references", mediaRefs, { mediaRefs = it }, "Exports reference attachment URIs; bundling can be added later.")
        ResearchCard("Backup & export", "Markdown, JSON, CSV, plain text, and database backup options preserve user ownership.", "Data")
        SettingsSwitchRow("Gemini assistant", ai, { ai = it }, "Optional. Requires user action before anything leaves the device. AI output is never auto-saved.")
        ResearchCard("Archive/search preferences", "Default: search all record types and treat Archive as long-term memory.", "Archive")
        SettingsSwitchRow("Reminders and streaks", reminders, { reminders = it }, "Notification permission is requested only if reminders are enabled.")
        ResearchCard("Privacy/data ownership", "Offline-first database, contextual permissions, no invented observations, evidence, locations, citations, or conclusions.", "Privacy")
        ResearchCard("Theme", "Uses the existing global Material 3 light/dark/dynamic color theme controls.", "Appearance")
        OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth()) { Text("Reset onboarding") }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(kind: String, id: Long, viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val observations by viewModel.observations.collectAsState(); val questions by viewModel.questions.collectAsState(); val hypotheses by viewModel.hypotheses.collectAsState(); val projects by viewModel.projects.collectAsState(); val sources by viewModel.sources.collectAsState(); val data by viewModel.dataRecords.collectAsState(); val reports by viewModel.reports.collectAsState()
    val title = kind.replaceFirstChar { it.uppercase() }
    Scaffold(topBar = { TopAppBar(title = { Text("$title Detail") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when (kind) {
            "observation" -> observations.firstOrNull { it.id == id }?.let { item { ObservationDetailContent(it, viewModel) } }
            "question" -> questions.firstOrNull { it.id == id }?.let { item { QuestionDetailContent(it, observations, sources, hypotheses) } }
            "hypothesis" -> hypotheses.firstOrNull { it.id == id }?.let { item { HypothesisDetailContent(it) } }
            "project" -> projects.firstOrNull { it.id == id }?.let { item { ProjectDetailContent(it, questions, observations, sources, data, reports) } }
            "source" -> sources.firstOrNull { it.id == id }?.let { item { SourceDetailContent(it) } }
            "report" -> reports.firstOrNull { it.id == id }?.let { item { ReportDetailContent(it) } }
            "data" -> data.firstOrNull { it.id == id }?.let { item { ResearchCard(it.label, "${it.toolType} • ${it.value} ${it.unit}\n${it.location}\n${it.notes}", "Data record") } }
        }
        item { ResearchCard("Available actions", "Edit, link existing records, create related records, add evidence/data, archive, export Markdown, and continue the research chain. Destructive deletion is intentionally hidden until soft-delete flows are explicit.", "Actions") }
    } }
}

@Composable private fun ObservationDetailContent(o: ObservationEntity, viewModel: FieldMindViewModel) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ResearchCard(o.subject, "${o.category} • ${o.date} ${o.time} • ${o.confidenceLevel}\n${o.factsOnlyNotes}\nLocation: ${o.manualLocation.ifBlank { "Not recorded" }}\nEvidence: ${o.evidenceSummary.ifBlank { "No summary" }}\nTags: ${o.tags}", "Observation"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ viewModel.addQuestion("What explains ${o.subject}?", o.category, "Observation", "New", "Medium", observationId = o.id) }) { Text("Create question") }; OutlinedButton({ viewModel.addDataRecord("Event Log", "Follow-up for ${o.subject}", "1", notes = o.factsOnlyNotes, observationId = o.id) }) { Text("Add data") }; OutlinedButton({ viewModel.archiveObservation(o.id) }) { Text("Archive") } } } }
@Composable private fun QuestionDetailContent(q: QuestionEntity, observations: List<ObservationEntity>, sources: List<SourceEntity>, hypotheses: List<HypothesisEntity>) { ResearchCard(q.questionText, "Status: ${q.status} • Priority: ${q.priority}\nRelated observations: ${observations.size}\nRelated sources: ${sources.size}\nLinked hypotheses: ${hypotheses.count { it.linkedQuestionId == q.id }}", "Question") }
@Composable private fun HypothesisDetailContent(h: HypothesisEntity) { ResearchCard(h.prediction, "Result: ${h.resultStatus} • Confidence: ${h.confidencePercent}%\nReasoning: ${h.reasoning}\nEvidence needed: ${h.evidenceNeeded}\nSupport: ${h.supportCriteria}\nWeakening: ${h.weakeningCriteria}\nTest: ${h.testMethod}", "Hypothesis") }
@Composable private fun ProjectDetailContent(p: ProjectEntity, questions: List<QuestionEntity>, observations: List<ObservationEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>, reports: List<ReportEntity>) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ResearchCard(p.title, "Goal: ${p.objective}\nQuestion: ${p.researchQuestion}\nStatus: ${p.status}", "Project"); listOf("Overview" to p.backgroundNotes, "Questions" to "${questions.count { it.relatedProjectId == p.id }} linked", "Observations" to "${observations.count { it.projectId == p.id }} linked", "Sources" to "${sources.count { it.relatedProjectId == p.id }} linked", "Data" to "${data.count { it.projectId == p.id }} records", "Analysis" to p.analysis, "Conclusions" to p.conclusion, "Future Questions" to p.futureQuestions, "Reports" to "${reports.count { it.projectId == p.id }} drafts").forEach { ResearchCard(it.first, it.second.ifBlank { "Not written yet" }, "Section") } } }
@Composable private fun SourceDetailContent(s: SourceEntity) { ResearchCard(s.title, "${s.type} • ${s.author} • ${s.dateOrYear}\nLink: ${s.link}\nSummary: ${s.personalSummary}\nKey findings: ${s.keyFindings}\nTaught me: ${s.whatThisSourceTaughtMe}\nQuestions: ${s.questionsGenerated}\nReliability: ${s.reliabilityScore}/5\nReading: ${s.readingStatus}\nPaper notes: ${s.paperNotes}", "Source") }
@Composable private fun ReportDetailContent(r: ReportEntity) { ResearchCard(r.title, "${r.type} • ${r.status}\n${r.markdownDraft.ifBlank { "Markdown draft is generated when report sections are saved." }}", "Report") }

@Composable private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, body: String) { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, onCheckedChange) } } }
@Composable private fun EvidenceAttachmentRow(type: String, caption: String) { ResearchCard(type, caption, "Evidence") }
@Composable private fun AnalysisInsightCard(title: String, insight: String, basis: String) { ResearchCard(title, "$insight\nBasis: $basis", "Analysis") }

@Composable private fun NewQuestionDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var question by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Other") }; var source by remember { mutableStateOf("Observation") }; var status by remember { mutableStateOf("New") }; var priority by remember { mutableStateOf("Medium") }; FormDialog("New Question", onDismiss, { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); onDismiss() } }) { LabeledTextField(question, { question = it }, "Question", minLines = 2); ChoiceChips(observationCategories, category) { category = it }; ChoiceChips(sourceTypes, source) { source = it }; ChoiceChips(questionStatuses, status) { status = it }; ChoiceChips(listOf("Low", "Medium", "High"), priority) { priority = it } } }
@Composable private fun NewProjectDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var title by remember { mutableStateOf("") }; var topic by remember { mutableStateOf("") }; var objective by remember { mutableStateOf("") }; var rq by remember { mutableStateOf("") }; FormDialog("New Project", onDismiss, { if (title.isNotBlank()) { viewModel.addProject(title, topic, objective, rq); onDismiss() } }) { LabeledTextField(title, { title = it }, "Project title"); LabeledTextField(topic, { topic = it }, "Topic type"); LabeledTextField(objective, { objective = it }, "Goal", minLines = 2); LabeledTextField(rq, { rq = it }, "Research question", minLines = 2) } }
@Composable private fun NewSourceDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("Paper") }; var title by remember { mutableStateOf("") }; var author by remember { mutableStateOf("") }; var link by remember { mutableStateOf("") }; var summary by remember { mutableStateOf("") }; var findings by remember { mutableStateOf("") }; var taught by remember { mutableStateOf("") }; var questions by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var reliability by remember { mutableStateOf(3f) }; FormDialog("Add Source / Reading Notes", onDismiss, { if (title.isNotBlank()) { viewModel.addSource(type, title, author, link, summary, taught, reliability.toInt(), findings, questions, notes); onDismiss() } }) { ChoiceChips(sourceLibraryTypes, type) { type = it }; LabeledTextField(title, { title = it }, "Title"); LabeledTextField(author, { author = it }, "Author"); LabeledTextField(link, { link = it }, "Link"); LabeledTextField(summary, { summary = it }, "Personal summary", minLines = 2); LabeledTextField(findings, { findings = it }, "Key findings", minLines = 2); LabeledTextField(taught, { taught = it }, "What this source taught me", minLines = 2); LabeledTextField(questions, { questions = it }, "Questions generated", minLines = 2); LabeledTextField(notes, { notes = it }, "Paper reading prompts: studied, why, how, found, confusing, new questions", minLines = 4); Text("Reliability: ${reliability.toInt()}/5"); Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3) } }
@Composable private fun NewHypothesisDialog(viewModel: FieldMindViewModel, questions: List<QuestionEntity>, onDismiss: () -> Unit) { var prediction by remember { mutableStateOf("") }; var reasoning by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var support by remember { mutableStateOf("") }; var weaken by remember { mutableStateOf("") }; var test by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf(50f) }; val linked = questions.firstOrNull(); FormDialog("New Hypothesis", onDismiss, { if (prediction.isNotBlank()) { viewModel.addHypothesis(linked?.id, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test); onDismiss() } }) { linked?.let { Text("Linked question: ${it.questionText}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; LabeledTextField(prediction, { prediction = it }, "Prediction", minLines = 2); LabeledTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2); LabeledTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2); LabeledTextField(support, { support = it }, "Support criteria"); LabeledTextField(weaken, { weaken = it }, "Weakening criteria"); LabeledTextField(test, { test = it }, "Test method"); Text("Confidence: ${confidence.toInt()}%"); Slider(confidence, { confidence = it }, valueRange = 0f..100f) } }
@Composable private fun NewDataRecordDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var tool by remember { mutableStateOf("Counter") }; var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("0") }; var unit by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; FormDialog("Data Collection Tool", onDismiss, { if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, location); onDismiss() } }) { ChoiceChips(dataTools, tool) { tool = it }; LabeledTextField(label, { label = it }, "Label"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }; Text(value, style = MaterialTheme.typography.headlineSmall); Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }; TextButton({ value = "0" }) { Text("Reset") } }; LabeledTextField(value, { value = it }, "Value / checklist items / comparison samples"); LabeledTextField(unit, { unit = it }, "Unit", supportingText = "Free-form; examples: count, cm, °C, minutes"); LabeledTextField(location, { location = it }, "Location"); LabeledTextField(notes, { notes = it }, "Notes", minLines = 3) } }
@Composable private fun NewReportDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("Field Report") }; var title by remember { mutableStateOf("") }; var background by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var observations by remember { mutableStateOf("") }; var results by remember { mutableStateOf("") }; var interpretation by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var limitations by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; FormDialog("Report Builder", onDismiss, { if (title.isNotBlank()) { viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next); onDismiss() } }) { ChoiceChips(reportTypes, type) { type = it }; listOf("Title" to title).forEach { LabeledTextField(title, { title = it }, "Title") }; LabeledTextField(background, { background = it }, "Background", minLines = 2); LabeledTextField(question, { question = it }, "Question", minLines = 2); LabeledTextField(methods, { methods = it }, "Methods", minLines = 2); LabeledTextField(observations, { observations = it }, "Linked observations summary", minLines = 2); LabeledTextField(results, { results = it }, "Data / results", minLines = 2); LabeledTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2); LabeledTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2); LabeledTextField(limitations, { limitations = it }, "Limitations", minLines = 2); LabeledTextField(next, { next = it }, "Next steps", minLines = 2); Text("Save generates a local Markdown draft for export.") } }
@Composable private fun NewFlashcardDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) { var type by remember { mutableStateOf("concept") }; var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }; FormDialog("Create Flashcard", onDismiss, { if (front.isNotBlank() && back.isNotBlank()) { viewModel.addFlashcard(front, back, type); onDismiss() } }) { ChoiceChips(listOf("term", "definition", "concept", "question-answer", "mistake card"), type) { type = it }; LabeledTextField(front, { front = it }, "Front"); LabeledTextField(back, { back = it }, "Back", minLines = 3) } }
@Composable private fun FormDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }, confirmButton = { Button(onClick = onSave) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
