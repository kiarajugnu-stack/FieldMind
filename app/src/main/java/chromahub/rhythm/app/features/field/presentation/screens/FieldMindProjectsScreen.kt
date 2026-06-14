package fieldmind.research.app.features.field.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.export.FieldReportTemplates
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import kotlin.math.abs

// ══════════════════════════════════════════════════════════════════════
//  Workspace — Redesigned: 3 tabs (Projects, Evidence, Analysis)
//  - No guided research flow
//  - Simplified project creation (Name + Question → Create)
//  - Project attachments support
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectsScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    startTab: Int = 0,
    onStartSession: (() -> Unit)? = null
) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notes by viewModel.notes.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab.coerceIn(0, 2)) }
    val haptics = rememberFieldMindHaptics()
    val tabs = listOf("Projects", "Evidence", "Analysis")

    fun selectTab(next: Int) {
        val bounded = next.coerceIn(0, tabs.lastIndex)
        if (bounded != tab) { tab = bounded; haptics.light() }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader(
                "Workspace",
                "Projects, evidence, and analysis — all connected.",
                icon = FieldMindIcons.Projects
            )
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 20.dp, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, label -> Tab(tab == i, { selectTab(i) }, text = { Text(label) }) }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(210)) { direction * it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(210)) { -direction * it / 4 } + fadeOut(tween(180)))
            },
            label = "workspacePage"
        ) { selectedTab ->
            Box(
                Modifier.fillMaxSize().pointerInput(selectedTab) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                        onDragEnd = { if (abs(totalDrag) > 96f) { if (totalDrag < 0) selectTab(selectedTab + 1) else selectTab(selectedTab - 1) } }
                    )
                }
            ) {
                when (selectedTab) {
                    0 -> ProjectsTab(viewModel, projects, questions, hypotheses, observations, sources, data, reports, onOpenDetail, onStartSession)
                    1 -> EvidenceTab(viewModel, observations, notes, questions, sources, onOpenDetail)
                    2 -> AnalysisTab(viewModel, hypotheses, questions, data, reports, observations, onOpenDetail)
                }
            }
        }
    }
}

@Composable
internal fun AddButton(label: String, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Button(onClick = { haptics.light(); onClick() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 20.dp); Spacer(Modifier.size(8.dp)); Text(label)
    }
}

internal fun panelPadding() = PaddingValues(20.dp, 4.dp, 20.dp, 96.dp)

// ──────────────────────────────────────────────────────────────────────
//  Tab 1: Projects — Simplified creation (Name + Question → Create)
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectsTab(
    viewModel: FieldMindViewModel,
    items: List<ProjectEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onStartSession: (() -> Unit)? = null
) {
    var show by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Observation") }
    var investigationPlan by remember { mutableStateOf(setOf("Photo documentation", "Daily observations")) }
    var selectedTemplate by remember { mutableStateOf<Int?>(-1) }
    var showTemplates by remember { mutableStateOf(false) }
    val haptics = rememberFieldMindHaptics()

    // Project templates
    val projectTemplates = listOf(
        ProjectTemplate("Species Survey", "Observation", "What species occur here?", "Recommended data: Species Tracker. Evidence: photos, audio, location. Charts: bar + timeline. Duration: 1 day to 1 month.", setOf("Species counting", "Photo documentation", "Audio recording")),
        ProjectTemplate("Behavior Study", "Investigation", "What behavior is happening, when, and under what conditions?", "Recommended data: Event Log. Evidence: video, time notes, repeated observations. Charts: timeline + by-hour.", setOf("Video documentation", "Daily observations", "Behavior logging")),
        ProjectTemplate("Site Survey", "Survey", "What are the main conditions and evidence at this site?", "Recommended data: Site Log. Evidence: photos, GPS, weather, measurements. Duration: one visit to seasonal monitoring.", setOf("Photo documentation", "Measurement logging", "Weather logging")),
        ProjectTemplate("Experiment", "Experiment", "What changes when one condition varies?", "Recommended data: Comparison Table. Evidence: measurements, controls, notes. Charts: comparison + line.", setOf("Measurement logging", "Comparison table", "Weekly observations")),
        ProjectTemplate("Monitoring", "Monitoring", "How does this place or subject change over time?", "Recommended data: Time Series. Evidence: repeated photos, weather, count. Charts: trend + timeline.", setOf("Daily observations", "Photo documentation", "Weather logging"))
    )

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Start Research Session button
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { haptics.light(); onStartSession?.invoke() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.project.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Bolt, null, tint = FieldMindTheme.colors.project, size = 26.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Research Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Timer-based multi-observation capture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                }
            }
        }

        item { AddButton(if (show) "Cancel" else "New project") { show = !show; if (!show) { title = ""; question = "" } } }

        // Template selector toggle
        item {
            OutlinedButton(
                onClick = { showTemplates = !showTemplates; selectedTemplate = -1 },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(FieldMindIcons.Project, null, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text(if (showTemplates) "Hide templates" else "Start from template")
            }
        }

        if (showTemplates) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Project templates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Choose a template to pre-fill your project setup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        projectTemplates.forEachIndexed { i, template ->
                            val isSelected = selectedTemplate == i
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedTemplate = i
                                    title = template.name
                                    question = template.question
                                    projectType = template.type
                                    investigationPlan = template.methods
                                    show = true
                                    showTemplates = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                            .background(FieldMindTheme.colors.project.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 20.dp) }
                                    Column(Modifier.weight(1f)) {
                                        Text(template.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(template.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(FieldMindIcons.Add, null, tint = FieldMindTheme.colors.project, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (show) item {
            InlineFormCard("New Project", onDismiss = { show = false; title = ""; question = "" }, onSave = {
                if (title.isNotBlank()) {
                    val methods = investigationPlan.joinToString(" • ")
                    viewModel.addProject(title, projectType, "Project type: $projectType", question, methods)
                    show = false; title = ""; question = ""; projectType = "Observation"; investigationPlan = setOf("Photo documentation", "Daily observations")
                }
            }, saveEnabled = title.isNotBlank()) {
                FieldTextField(title, { title = it }, "Project title", supportingText = "Give your investigation a name")
                Text("Project type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceChips(listOf("Observation", "Investigation", "Survey", "Experiment", "Monitoring"), projectType) { projectType = it }
                FieldTextField(question, { question = it }, "Research question", minLines = 2, supportingText = "What do you want to find out?")
                ResearchMethodBuilder(investigationPlan) { investigationPlan = it }
                ResearchPreviewCard(projectType, investigationPlan)
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    "No projects yet",
                    "Start with a name and a question. Add methods, data, and reports as your research grows.",
                    icon = FieldMindIcons.Project,
                    actionLabel = "Create project"
                ) { show = true }
            }
        } else {
            item { ProjectSummaryCard(items, questions, hypotheses, observations, sources, data, reports) }
            items(items) { project ->
                ProjectWorkspaceCard(project, questions, hypotheses, observations, sources, data, reports) { onOpenDetail("project", project.id) }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Tab 2: Evidence — Unified view of observations, notes, questions, sources
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun EvidenceTab(
    viewModel: FieldMindViewModel,
    observations: List<ObservationEntity>,
    notes: List<NoteEntity>,
    questions: List<QuestionEntity>,
    sources: List<SourceEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var filterKind by remember { mutableStateOf("All") }
    var filterGroup by remember { mutableStateOf("All") }
    var bulkMode by remember { mutableStateOf(false) }
    // Merge all evidence into chronological list
    val evidence = remember(observations, notes, questions, sources) {
        buildList {
            observations.forEach { add(EvidenceItem("observation", it.id, it.timestamp, it.subject.ifBlank { "Observation" }, "${it.category} • ${it.date}", it.category)) }
            notes.forEach { add(EvidenceItem("note", it.id, it.updatedAt, it.title.ifBlank { "Untitled note" }, it.body.take(80), it.category)) }
            questions.forEach { add(EvidenceItem("question", it.id, it.updatedAt, it.questionText, "${it.status} • ${it.priority}", it.status)) }
            sources.forEach { add(EvidenceItem("source", it.id, it.updatedAt, it.title, "${it.type} • ${it.readingStatus}", it.type)) }
        }.sortedByDescending { it.timestamp }
    }
    val groups = remember(evidence) { listOf("All") + evidence.map { it.group }.distinct().sorted().take(16) }
    val filteredEvidence = remember(evidence, filterKind, filterGroup) { evidence.filter { (filterKind == "All" || it.kind == filterKind.lowercase()) && (filterGroup == "All" || it.group == filterGroup) } }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Evidence hub", "${filteredEvidence.size} of ${evidence.size} items • filter, review, bulk-manage") }
        item { EvidenceFilterBar(filterKind, { filterKind = it }, filterGroup, { filterGroup = it }, groups, bulkMode, { bulkMode = !bulkMode }) }
        if (filteredEvidence.isEmpty()) {
            item {
                EmptyState(
                    "No evidence yet",
                    "Your observations, notes, questions, and sources will appear here — all linked together.",
                    icon = FieldMindIcons.Observation,
                    actionLabel = "Start observing"
                ) { onOpenDetail("observe", 0) }
            }
        }
        items(filteredEvidence) { item ->
            EntityCard(
                item.title, item.kind,
                body = item.subtitle,
                meta = listOf(item.group),
                onClick = { onOpenDetail(item.kind, item.id) }
            )
        }
    }
}

private data class ProjectTemplate(val name: String, val type: String, val question: String, val summary: String, val methods: Set<String>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchMethodBuilder(selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    val options = listOf("Daily observations", "Weekly observations", "Photo documentation", "Audio recording", "Video documentation", "Measurement logging", "Species counting", "Weather logging", "Behavior logging", "Comparison table")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Research method builder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option in selected, onClick = { onSelected(if (option in selected) selected - option else selected + option) }, label = { Text(option) }, leadingIcon = if (option in selected) ({ Icon(FieldMindIcons.Check, null, size = 16.dp) }) else null)
            }
        }
    }
}

@Composable
private fun ResearchPreviewCard(projectType: String, methods: Set<String>) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Auto workspace preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Type: $projectType", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Recommended data: ${recommendedDatasetFor(methods)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Recommended evidence: ${recommendedEvidenceFor(methods)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun recommendedDatasetFor(methods: Set<String>): String = when {
    methods.any { "Species" in it } -> "Species Tracker"
    methods.any { "Measurement" in it } -> "Measurement Log"
    methods.any { "Weather" in it } -> "Weather Log"
    methods.any { "Comparison" in it } -> "Comparison Table"
    else -> "Observation Timeline"
}

private fun recommendedEvidenceFor(methods: Set<String>): String = listOfNotNull(
    "photos".takeIf { methods.any { "Photo" in it } },
    "audio".takeIf { methods.any { "Audio" in it } },
    "video".takeIf { methods.any { "Video" in it } },
    "measurements".takeIf { methods.any { "Measurement" in it } },
    "GPS + weather".takeIf { methods.any { "Weather" in it || "Daily" in it } }
).ifEmpty { listOf("notes", "observations") }.joinToString(", ")

@Composable
private fun EvidenceFilterBar(kind: String, onKind: (String) -> Unit, group: String, onGroup: (String) -> Unit, groups: List<String>, bulkMode: Boolean, onBulk: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceChips(listOf("All", "Observation", "Note", "Question", "Source"), kind) { onKind(it) }
            ChoiceChips(groups, group) { onGroup(it) }
            OutlinedButton(onClick = onBulk, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(if (bulkMode) "Done bulk selection" else "Bulk select / manage") }
        }
    }
}

private data class EvidenceItem(val kind: String, val id: Long, val timestamp: Long, val title: String, val subtitle: String, val group: String)

// ──────────────────────────────────────────────────────────────────────
//  Extracted form composables (to avoid @Composable-in-scope issues)
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun HypothesisForm(
    questions: List<QuestionEntity>,
    viewModel: FieldMindViewModel,
    onDismiss: () -> Unit
) {
    var prediction by remember { mutableStateOf("") }
    var reasoning by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var linkedId by remember { mutableStateOf(questions.firstOrNull()?.id) }
    InlineFormCard("New Hypothesis", onDismiss = { onDismiss(); prediction = "" }, onSave = {
        if (prediction.isNotBlank()) { viewModel.addHypothesis(linkedId, prediction, evidence, 50, reasoning); onDismiss(); prediction = "" }
    }, saveEnabled = prediction.isNotBlank()) {
        if (questions.isNotEmpty()) {
            ChoiceChips(listOf("No question") + questions.take(6).map { it.questionText.take(28) }, questions.firstOrNull { it.id == linkedId }?.questionText?.take(28) ?: "No question") { picked ->
                linkedId = questions.firstOrNull { it.questionText.startsWith(picked) }?.id
            }
        }
        FieldTextField(prediction, { prediction = it }, "If...", minLines = 2, supportingText = "What do you predict will happen?")
        FieldTextField(reasoning, { reasoning = it }, "Because...", minLines = 2)
        FieldTextField(evidence, { evidence = it }, "Evidence needed to support or weaken", minLines = 2)
    }
}

@Composable
private fun DataRecordForm(
    viewModel: FieldMindViewModel,
    onDismiss: () -> Unit
) {
    var tool by remember { mutableStateOf("Counter") }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf("count") }
    var notes by remember { mutableStateOf("") }
    var datasetKind by remember { mutableStateOf("Measurements") }
    var chartMode by remember { mutableStateOf("Line") }
    InlineFormCard("Data Workspace Entry", onDismiss = { onDismiss(); label = "" }, onSave = {
        if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, datasetKind = datasetKind, chartPreference = chartMode); onDismiss(); label = "" }
    }, saveEnabled = label.isNotBlank()) {
        ChoiceChips(listOf("Counters", "Measurements", "Event logs", "Weather logs", "Species tracking", "Comparison table", "Time series"), datasetKind) { datasetKind = it }
        ChoiceChips(listOf("Bar", "Line", "Donut/Pie", "Breakdown", "Timeline"), chartMode) { chartMode = it }
        ChoiceChips(dataTools, tool) { tool = it }
        FieldTextField(label, { label = it }, "Dataset / row label")
        FieldTextField(value, { value = it }, "Value")
        FieldTextField(unit, { unit = it }, "Unit")
        FieldTextField(notes, { notes = it }, "Notes", minLines = 2)
    }
}

@Composable
private fun ReportForm(
    viewModel: FieldMindViewModel,
    onDismiss: () -> Unit
) {
    val templates = FieldReportTemplates.defaults
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var preset by remember { mutableStateOf(selectedTemplate.presets.first()) }
    var title by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var results by remember { mutableStateOf("") }
    var interpretation by remember { mutableStateOf("") }
    var conclusion by remember { mutableStateOf("") }
    var limitations by remember { mutableStateOf("") }
    var nextSteps by remember { mutableStateOf("") }
    InlineFormCard("Report builder", onDismiss = { onDismiss(); title = "" }, onSave = {
        if (title.isNotBlank()) { viewModel.addReport(selectedTemplate.label, title, background, question, methods, observations, results, interpretation, conclusion, limitations, nextSteps); onDismiss(); title = "" }
    }, saveEnabled = title.isNotBlank()) {
        ChoiceChips(templates.map { it.label }, selectedTemplate.label) { label -> selectedTemplate = templates.first { it.label == label } }
        ChoiceChips(selectedTemplate.presets, preset) { preset = it }
        Text(selectedTemplate.helperPrompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(background, { background = it }, "Background", minLines = 2)
        FieldTextField(question, { question = it }, "Research question", minLines = 2)
        FieldTextField(methods, { methods = it }, "Methods", minLines = 3)
        FieldTextField(observations, { observations = it }, "Observations / evidence", minLines = 3)
        FieldTextField(results, { results = it }, "Results", minLines = 2)
        FieldTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2)
        FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
        FieldTextField(limitations, { limitations = it }, "Limitations", minLines = 2)
        FieldTextField(nextSteps, { nextSteps = it }, "Next steps", minLines = 2)
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Tab 3: Analysis — Hypotheses, data tools, reports
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisTab(
    viewModel: FieldMindViewModel,
    hypotheses: List<HypothesisEntity>,
    questions: List<QuestionEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    observations: List<ObservationEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var showHypothesis by remember { mutableStateOf(false) }
    var showData by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hypotheses
        item { SectionHeader("Hypotheses", "${hypotheses.size} predictions to test") }
        item { AddButton(if (showHypothesis) "Cancel" else "New hypothesis") { showHypothesis = !showHypothesis } }
        if (showHypothesis) {
            item {
                HypothesisForm(questions, viewModel, onDismiss = { showHypothesis = false })
            }
        }
        if (hypotheses.isEmpty()) item { EmptyState("No hypotheses yet", "Predict what evidence would show, then test it.", icon = FieldMindIcons.Hypothesis) }
        items(hypotheses) { h -> EntityCard(h.prediction, "hypothesis", body = "Confidence: ${h.confidencePercent}% • ${h.resultStatus}", onClick = { onOpenDetail("hypothesis", h.id) }) }

        // Data tools
        item { Divider12() }
        item { SectionHeader("Live Data Workspace", "${data.size} records • choose what you are tracking") }
        item { TrackingFlowCards() }
        item { AddButton(if (showData) "Cancel" else "Add data record") { showData = !showData } }
        if (showData) {
            item {
                DataRecordForm(viewModel, onDismiss = { showData = false })
            }
        }
        if (data.isEmpty()) item { EmptyState("No data records yet", "Measure, count, compare, or log with offline tools.", icon = FieldMindIcons.Data) }
        items(data) { d -> EntityCard(d.label, "data", body = "${d.value} ${d.unit}", meta = listOf(d.toolType), onClick = { onOpenDetail("data", d.id) }) }

        // Reports
        item { Divider12() }
        item { SectionHeader("Reports", "${reports.size} reports") }
        item { AddButton(if (showReport) "Cancel" else "Build report") { showReport = !showReport } }
        if (showReport) {
            item {
                ReportForm(viewModel, onDismiss = { showReport = false })
            }
        }
        if (reports.isEmpty()) item { EmptyState("No reports yet", "Write up your findings with background, methods, results, and conclusions.", icon = FieldMindIcons.Report) }
        items(reports) { r -> EntityCard(r.title, "report", body = r.conclusion.ifBlank { r.question }, meta = listOf(r.type, r.status), onClick = { onOpenDetail("report", r.id) }) }
    }
}


@Composable
private fun TrackingFlowCards() {
    val modes = listOf(
        "Count things" to "Creates a Counter dataset with bar charts.",
        "Measure something" to "Creates a Measurement Log with trend charts.",
        "Compare locations" to "Creates a Comparison Table with grouped charts.",
        "Track changes over time" to "Creates a Time Series with timeline preview.",
        "Record weather" to "Creates a Weather Log linked to observations.",
        "Track species" to "Creates a Species Tracker with evidence requirements."
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(modes) { (title, body) ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.width(180.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DatasetModeCards() {
    val modes = listOf("Counters", "Measurements", "Event logs", "Weather logs", "Species tracking", "Comparison tables", "Time series")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(modes) { mode ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.width(148.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                    Text(mode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Filter, chart, and link to projects or sessions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Summary cards
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectSummaryCard(
    projects: List<ProjectEntity>, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>, reports: List<ReportEntity>
) {
    val parts = listOf(
        Triple("Questions", questions.size.toFloat(), FieldMindTheme.colors.question),
        Triple("Hypotheses", hypotheses.size.toFloat(), FieldMindTheme.colors.hypothesis),
        Triple("Observations", observations.size.toFloat(), FieldMindTheme.colors.observation),
        Triple("Sources", sources.size.toFloat(), FieldMindTheme.colors.source),
        Triple("Data", data.size.toFloat(), FieldMindTheme.colors.data),
        Triple("Reports", reports.size.toFloat(), FieldMindTheme.colors.report)
    )
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Project portfolio", "${projects.size} active with evidence coverage")
            BreakdownBar(parts)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchRelationshipStrip(observations: Int, sources: Int, hypotheses: Int, data: Int, reports: Int) {
    val metrics = listOf(
        "Observations" to observations,
        "Sources" to sources,
        "Hypotheses" to hypotheses,
        "Data" to data,
        "Reports" to reports
    )
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Connected research assets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { (label, count) -> AssistChip(onClick = {}, label = { Text("$count $label") }) }
            }
        }
    }
}

@Composable
private fun ProjectWorkspaceCard(
    project: ProjectEntity, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>,
    reports: List<ReportEntity>, onClick: () -> Unit
) {
    val relatedQuestions = questions.count { it.relatedProjectId == project.id }
    val relatedObservations = observations.count { it.projectId == project.id }
    val relatedHypotheses = hypotheses.count { hypothesis -> questions.any { it.id == hypothesis.linkedQuestionId && it.relatedProjectId == project.id } }
    val relatedSources = sources.count { it.relatedProjectId == project.id }
    val relatedData = data.count { it.projectId == project.id }
    val relatedReports = reports.count { it.projectId == project.id }
    val progressMetrics = listOf(
        "Questions" to relatedQuestions.toFloat(),
        "Observations" to relatedObservations.toFloat(),
        "Data" to relatedData.toFloat(),
        "Reports" to relatedReports.toFloat()
    )
    val maxMetric = progressMetrics.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.Project, contentDescription = null, tint = FieldMindTheme.colors.project, size = 24.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${relatedObservations} obs", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.observation)
                    }
                }
                InfoChip(project.status)
            }
            Text(project.objective.ifBlank { project.researchQuestion.ifBlank { "Open project workspace" } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ResearchRelationshipStrip(relatedObservations, relatedSources, relatedHypotheses, relatedData, relatedReports)
            
            // Mini progress bars for each metric
            progressMetrics.forEach { (label, count) ->
                val fraction = (count / maxMetric).coerceIn(0f, 1f)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                    Row(
                        Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.fillMaxWidth(fraction).fillMaxHeight()
                                .background(FieldMindTheme.colors.project.copy(alpha = 0.4f + fraction * 0.6f), RoundedCornerShape(3.dp))
                        )
                    }
                    Text("${count.toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
