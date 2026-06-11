package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.DataRecordEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity
import chromahub.rhythm.app.features.field.data.database.entity.ProjectEntity
import chromahub.rhythm.app.features.field.data.database.entity.QuestionEntity
import chromahub.rhythm.app.features.field.data.database.entity.SourceEntity
import chromahub.rhythm.app.features.field.presentation.components.ChoiceChips
import chromahub.rhythm.app.features.field.presentation.components.EmptyResearchState
import chromahub.rhythm.app.features.field.presentation.components.EntityTypeBadge
import chromahub.rhythm.app.features.field.presentation.components.FieldSectionTitle
import chromahub.rhythm.app.features.field.presentation.components.LabeledTextField
import chromahub.rhythm.app.features.field.presentation.components.ResearchCard
import chromahub.rhythm.app.features.field.presentation.components.StatPill
import chromahub.rhythm.app.features.field.presentation.components.TimelineItem
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import kotlinx.coroutines.launch

private val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Other")
private val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
private val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
private val questionStatuses = listOf("New", "Investigating", "Testing", "Solved", "Unresolved", "Abandoned")
private val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "Note")

@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val interests = listOf("Birds", "Plants", "Animals", "Insects", "Rocks", "Weather", "Water", "Human Behavior", "Other")
    var selectedInterest by remember { mutableStateOf("Birds") }
    val pages = listOf(
        "Welcome" to "FieldMind turns observations into questions, projects, sources, reports, and a searchable archive.",
        "Choose an interest" to "Start with a topic you can observe nearby. You can change this later.",
        "Research rhythm" to "Your core flow is Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive.",
        "Permissions later" to "Camera, audio, and location are requested only when you attach evidence or tag a place."
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(top = 48.dp)) {
                EntityTypeBadge(text = "FieldMind setup ${step + 1}/${pages.size}")
                Text(text = pages[step].first, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(text = pages[step].second, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedVisibility(visible = step == 1) {
                    ChoiceChips(options = interests, selected = selectedInterest, onSelected = { selectedInterest = it })
                }
                Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = "First goal: record one factual observation today. Write what you saw, how certain you are, and what evidence you have.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Skip") }
                Button(onClick = { if (step < pages.lastIndex) step++ else onFinish() }, modifier = Modifier.weight(1f)) {
                    Text(if (step < pages.lastIndex) "Next" else "Enter FieldMind")
                }
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
    val activeProject = projects.firstOrNull { it.status == "Active" } ?: projects.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("FieldMind", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Observe. Question. Discover.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenSettings) { Text("Settings") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Current Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(activeProject?.title ?: "Start a local research project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Today's Goal: Record 1 factual observation", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatPill("Streak", if (observations.isEmpty()) "0 days" else "1 day", Modifier.weight(1f))
                        StatPill("Last observation", observations.firstOrNull()?.subject ?: "None yet", Modifier.weight(1f))
                    }
                }
            }
        }
        item { FieldSectionTitle("Quick actions", "One tap. No menus for the important work.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onNavigate(FieldMindScreen.Capture) }, modifier = Modifier.weight(1f)) { Text("New Observation") }
                    FilledTonalButton(onClick = { onNavigate(FieldMindScreen.Research) }, modifier = Modifier.weight(1f)) { Text("New Question") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { onNavigate(FieldMindScreen.Library) }, modifier = Modifier.weight(1f)) { Text("Research Note") }
                    FilledTonalButton(onClick = { onNavigate(FieldMindScreen.Research) }, modifier = Modifier.weight(1f)) { Text("New Project") }
                }
            }
        }
        item { FieldSectionTitle("Research snapshot") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill("Observations", observations.size.toString(), Modifier.weight(1f))
                StatPill("Questions", questions.size.toString(), Modifier.weight(1f))
                StatPill("Sources", sources.size.toString(), Modifier.weight(1f))
            }
        }
        item { FieldSectionTitle("Recent activity", "Everything appears chronologically.") }
        val activity = buildList {
            observations.take(3).forEach { add(Triple("Observed ${it.subject}", "${it.category} • ${it.date} ${it.time}", "Observe")) }
            questions.take(2).forEach { add(Triple(it.questionText, "${it.status} • from ${it.sourceType}", "Question")) }
            sources.take(2).forEach { add(Triple(it.title, "${it.type} • reliability ${it.reliabilityScore}/5", "Source")) }
            reports.take(1).forEach { add(Triple(it.title, "Report draft", "Report")) }
        }
        if (activity.isEmpty()) {
            item { EmptyResearchState("No activity yet", "Start with Capture. One factual note is enough to begin the research chain.") }
        } else {
            items(activity) { item -> TimelineItem(title = item.first, subtitle = item.second, badge = item.third) }
        }
    }
}

@Composable
fun CaptureScreen(viewModel: FieldMindViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bird") }
    var facts by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf("Sure") }
    var location by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { FieldSectionTitle("Capture", "Write what you observed before it disappears.") }
            item { LabeledTextField(subject, { subject = it }, "What are you observing?", supportingText = "Example: House Crow") }
            item { FieldSectionTitle("Category") }
            item { ChoiceChips(observationCategories, category, { category = it }) }
            item {
                LabeledTextField(
                    facts,
                    { facts = it },
                    "Facts only",
                    minLines = 6,
                    supportingText = "Write what you observed, not what you think happened. Good: Crow landed on wire. Bad: Crow was looking for food."
                )
            }
            item { FieldSectionTitle("How certain are you?", "Every observation should say what you saw, how certain you are, and what evidence you have.") }
            item { ChoiceChips(confidenceOptions, confidence, { confidence = it }) }
            item { LabeledTextField(evidence, { evidence = it }, "Evidence summary", supportingText = "Photos, audio, video, or visible clues. Attachments can be added in a later media pass.") }
            item { LabeledTextField(location, { location = it }, "Location", supportingText = "Manual location for now. GPS can be enabled contextually later.") }
            item { LabeledTextField(tags, { tags = it }, "Tags", supportingText = "Example: crow, evening, behavior") }
            item { LabeledTextField(context, { context = it }, "Mood / context", supportingText = "Weather, light, surroundings, or field conditions.") }
            item {
                Button(
                    onClick = {
                        if (subject.isBlank() || facts.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Subject and factual notes are required.") }
                        } else {
                            viewModel.addObservation(subject, category, facts, confidence, location, tags, evidence, context)
                            subject = ""; facts = ""; location = ""; tags = ""; evidence = ""; context = ""
                            scope.launch { snackbarHostState.showSnackbar("Observation saved to your archive.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Observation") }
            }
        }
    }
}

@Composable
fun ResearchScreen(viewModel: FieldMindViewModel) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val dataRecords by viewModel.dataRecords.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Questions", "Hypotheses", "Projects", "Data")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) { tabs.forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }) } }
        when (tab) {
            0 -> QuestionPanel(viewModel, questions)
            1 -> HypothesisPanel(viewModel, questions)
            2 -> ProjectPanel(viewModel, projects)
            else -> DataPanel(viewModel, dataRecords)
        }
    }
}

@Composable
private fun QuestionPanel(viewModel: FieldMindViewModel, questions: List<QuestionEntity>) {
    var showDialog by remember { mutableStateOf(false) }
    ResearchListScaffold("Question bank", "Researchers collect questions, not just answers.", "New Question", { showDialog = true }) {
        if (questions.isEmpty()) item { EmptyResearchState("No questions yet", "Turn an observation, reading, video, thought, or discussion into a testable question.") }
        items(questions) { ResearchCard(it.questionText, "${it.status} • ${it.sourceType} • Priority ${it.priority}", label = "Question") }
    }
    if (showDialog) NewQuestionDialog(viewModel, onDismiss = { showDialog = false })
}

@Composable
private fun HypothesisPanel(viewModel: FieldMindViewModel, questions: List<QuestionEntity>) {
    val hypotheses by viewModel.hypotheses.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    ResearchListScaffold("Hypotheses", "Turn curiosity into testable thinking.", "New Hypothesis", { showDialog = true }) {
        if (hypotheses.isEmpty()) item { EmptyResearchState("No hypotheses yet", "Link a prediction to a question, define evidence, then update support after data collection.") }
        items(hypotheses) { ResearchCard(it.prediction, "Confidence ${it.confidencePercent}% • ${it.resultStatus}\nEvidence needed: ${it.evidenceNeeded.ifBlank { "Not defined" }}", label = "Hypothesis") }
    }
    if (showDialog) NewHypothesisDialog(viewModel, questions, onDismiss = { showDialog = false })
}

@Composable
private fun ProjectPanel(viewModel: FieldMindViewModel, projects: List<ProjectEntity>) {
    var showDialog by remember { mutableStateOf(false) }
    ResearchListScaffold("Projects", "Move from curiosity to structured field work.", "New Project", { showDialog = true }) {
        if (projects.isEmpty()) item { EmptyResearchState("No projects yet", "Create a project like Bird Activity Near Home or Local Plant Changes Across Seasons.") }
        items(projects) { ResearchCard(it.title, "${it.topicType} • ${it.status}\nGoal: ${it.objective.ifBlank { "Define a goal" }}", label = "Project") }
    }
    if (showDialog) NewProjectDialog(viewModel, onDismiss = { showDialog = false })
}

@Composable
private fun DataPanel(viewModel: FieldMindViewModel, records: List<DataRecordEntity>) {
    var showDialog by remember { mutableStateOf(false) }
    ResearchListScaffold("Data tools", "Start simple: counts, measurements, checklists, events, weather, and site logs.", "Counter", { showDialog = true }) {
        if (records.isEmpty()) item { EmptyResearchState("No data yet", "Use the counter to begin a measurable trail. More data tools can share this same record model.") }
        items(records) { ResearchCard(it.label, "${it.toolType}: ${it.value} ${it.unit}\n${it.notes}", label = "Data") }
    }
    if (showDialog) NewCounterDialog(viewModel, onDismiss = { showDialog = false })
}

@Composable
private fun ResearchListScaffold(title: String, subtitle: String, action: String, onAction: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FieldSectionTitle(title, subtitle, Modifier.weight(1f))
                Button(onClick = onAction) { Text(action) }
            }
        }
        content()
    }
}

@Composable
fun KnowledgeLibraryScreen(viewModel: FieldMindViewModel) {
    val sources by viewModel.sources.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FieldSectionTitle("Library", "Articles, papers, books, videos, websites, and notes.", Modifier.weight(1f))
                Button(onClick = { showDialog = true }) { Text("Add Source") }
            }
        }
        item { ChoiceChips(sourceLibraryTypes, "Paper", { _ -> }) }
        item {
            ResearchCard(
                title = "Paper reading mode",
                subtitle = "What was studied? Why? How? What was found? What confused you? What new questions did this create?",
                label = "Guided workflow"
            )
        }
        if (sources.isEmpty()) item { EmptyResearchState("No sources yet", "Save a paper, article, video, book, website, or note. Add what it taught you so reading becomes research.") }
        items(sources) { source ->
            ResearchCard(source.title, "${source.type} • ${source.author.ifBlank { "Unknown author" }} • reliability ${source.reliabilityScore}/5\nTaught me: ${source.whatThisSourceTaughtMe.ifBlank { "Add a reflection" }}", label = source.type)
        }
    }
    if (showDialog) NewSourceDialog(viewModel, onDismiss = { showDialog = false })
}

@Composable
fun ArchiveScreen(viewModel: FieldMindViewModel) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val filteredObservations = observations.filter { q.isBlank() || it.subject.lowercase().contains(q) || it.factsOnlyNotes.lowercase().contains(q) || it.tags.lowercase().contains(q) }
    val filteredQuestions = questions.filter { q.isBlank() || it.questionText.lowercase().contains(q) || it.status.lowercase().contains(q) }
    val filteredProjects = projects.filter { q.isBlank() || it.title.lowercase().contains(q) || it.objective.lowercase().contains(q) }
    val filteredSources = sources.filter { q.isBlank() || it.title.lowercase().contains(q) || it.personalSummary.lowercase().contains(q) }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item { FieldSectionTitle("Archive", "Search everything you have ever recorded.") }
        item { LabeledTextField(query, { query = it }, "Search observations, questions, projects, sources, tags") }
        item { Text("Filters: keyword • date • project • category • source • location • tag • status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (filteredObservations.isEmpty() && filteredQuestions.isEmpty() && filteredProjects.isEmpty() && filteredSources.isEmpty()) {
            item { EmptyResearchState("No matches", "Nothing is lost. Try a different keyword or create your first research record.") }
        }
        items(filteredObservations) { ResearchCard(it.subject, "${it.category} • ${it.date}\n${it.factsOnlyNotes}", label = "Observation") }
        items(filteredQuestions) { ResearchCard(it.questionText, "${it.status} • ${it.sourceType}", label = "Question") }
        items(filteredProjects) { ResearchCard(it.title, it.objective.ifBlank { "Project workspace" }, label = "Project") }
        items(filteredSources) { ResearchCard(it.title, it.personalSummary.ifBlank { "Saved source" }, label = it.type) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldMindSettingsScreen(onBack: () -> Unit, onResetOnboarding: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("FieldMind Settings") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FieldSectionTitle("Research defaults", "Observation category, confidence, location, and capture defaults belong here.")
            ResearchCard("Observation defaults", "Default category: Bird • Default confidence: Sure", label = "Capture")
            ResearchCard("Location tagging", "Ask contextually when saving an observation. Manual location remains available offline.", label = "Privacy")
            ResearchCard("Media attachments", "Photos, audio, and video evidence are planned without deleting or scanning user media.", label = "Evidence")
            ResearchCard("Gemini assistant", "Optional AI for factuality review, question quality, hypothesis suggestions, paper summaries, and writing improvement.", label = "AI")
            ResearchCard("Backup & export", "Markdown, JSON, CSV, text, and future PDF exports keep research data portable.", label = "Data ownership")
            OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth()) { Text("Show onboarding again") }
        }
    }
}

@Composable
private fun NewQuestionDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var source by remember { mutableStateOf("Observation") }
    var status by remember { mutableStateOf("New") }
    var priority by remember { mutableStateOf("Medium") }
    FormDialog("New Question", onDismiss, onSave = { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); onDismiss() } }) {
        LabeledTextField(question, { question = it }, "Question", minLines = 2)
        ChoiceChips(sourceTypes, source, { source = it })
        ChoiceChips(questionStatuses, status, { status = it })
        ChoiceChips(listOf("Low", "Medium", "High"), priority, { priority = it })
    }
}

@Composable
private fun NewProjectDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var researchQuestion by remember { mutableStateOf("") }
    FormDialog("New Project", onDismiss, onSave = { if (title.isNotBlank()) { viewModel.addProject(title, topic, objective, researchQuestion); onDismiss() } }) {
        LabeledTextField(title, { title = it }, "Project title")
        LabeledTextField(topic, { topic = it }, "Topic type", supportingText = "Biology, geology, weather, behavior...")
        LabeledTextField(objective, { objective = it }, "Project goal", minLines = 2)
        LabeledTextField(researchQuestion, { researchQuestion = it }, "Research question", minLines = 2)
    }
}

@Composable
private fun NewSourceDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("Paper") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var taught by remember { mutableStateOf("") }
    var reliability by remember { mutableStateOf(3f) }
    FormDialog("Add Source", onDismiss, onSave = { if (title.isNotBlank()) { viewModel.addSource(type, title, author, link, summary, taught, reliability.toInt()); onDismiss() } }) {
        ChoiceChips(sourceLibraryTypes, type, { type = it })
        LabeledTextField(title, { title = it }, "Title")
        LabeledTextField(author, { author = it }, "Author")
        LabeledTextField(link, { link = it }, "Link")
        LabeledTextField(summary, { summary = it }, "Personal summary", minLines = 3)
        LabeledTextField(taught, { taught = it }, "What this source taught me", minLines = 2)
        Text("Reliability: ${reliability.toInt()}/5")
        Slider(value = reliability, onValueChange = { reliability = it }, valueRange = 1f..5f, steps = 3)
    }
}

@Composable
private fun NewHypothesisDialog(viewModel: FieldMindViewModel, questions: List<QuestionEntity>, onDismiss: () -> Unit) {
    var prediction by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(50f) }
    val linkedQuestion = questions.firstOrNull()
    FormDialog("New Hypothesis", onDismiss, onSave = { if (prediction.isNotBlank()) { viewModel.addHypothesis(linkedQuestion?.id, prediction, evidence, confidence.toInt()); onDismiss() } }) {
        if (linkedQuestion != null) Text("Linked question: ${linkedQuestion.questionText}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LabeledTextField(prediction, { prediction = it }, "Prediction", minLines = 2)
        LabeledTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2)
        Text("Confidence: ${confidence.toInt()}%")
        Slider(value = confidence, onValueChange = { confidence = it }, valueRange = 0f..100f)
    }
}

@Composable
private fun NewCounterDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    FormDialog("Counter", onDismiss, onSave = { if (label.isNotBlank()) { viewModel.addCounter(label, count, notes); onDismiss() } }) {
        LabeledTextField(label, { label = it }, "What are you counting?", supportingText = "Example: Crows")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (count > 0) count-- }) { Text("−1") }
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { count++ }) { Text("+1") }
        }
        LabeledTextField(notes, { notes = it }, "Notes", minLines = 2)
    }
}

@Composable
private fun FormDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
