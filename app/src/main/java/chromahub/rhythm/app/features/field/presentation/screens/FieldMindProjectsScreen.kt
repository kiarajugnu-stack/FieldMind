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
    val haptics = rememberFieldMindHaptics()

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

        if (show) item {
            InlineFormCard("New Project", onDismiss = { show = false; title = ""; question = "" }, onSave = {
                if (title.isNotBlank()) {
                    viewModel.addProject(title, "Other", "", question)
                    show = false; title = ""; question = ""
                }
            }, saveEnabled = title.isNotBlank()) {
                FieldTextField(title, { title = it }, "Project title", supportingText = "Give your investigation a name")
                FieldTextField(question, { question = it }, "Research question", minLines = 2, supportingText = "What do you want to find out?")
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
    // Merge all evidence into chronological list
    val evidence = remember(observations, notes, questions, sources) {
        buildList {
            observations.forEach { add(EvidenceItem("observation", it.id, it.timestamp, it.subject.ifBlank { "Observation" }, "${it.category} • ${it.date}", it.category)) }
            notes.forEach { add(EvidenceItem("note", it.id, it.updatedAt, it.title.ifBlank { "Untitled note" }, it.body.take(80), it.category)) }
            questions.forEach { add(EvidenceItem("question", it.id, it.updatedAt, it.questionText, "${it.status} • ${it.priority}", it.status)) }
            sources.forEach { add(EvidenceItem("source", it.id, it.updatedAt, it.title, "${it.type} • ${it.readingStatus}", it.type)) }
        }.sortedByDescending { it.timestamp }
    }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("All evidence", "${evidence.size} items across observations, notes, questions, and sources") }
        if (evidence.isEmpty()) {
            item {
                EmptyState(
                    "No evidence yet",
                    "Your observations, notes, questions, and sources will appear here — all linked together.",
                    icon = FieldMindIcons.Observation,
                    actionLabel = "Start observing"
                ) { onOpenDetail("observe", 0) }
            }
        }
        items(evidence) { item ->
            EntityCard(
                item.title, item.kind,
                body = item.subtitle,
                meta = listOf(item.group),
                onClick = { onOpenDetail(item.kind, item.id) }
            )
        }
    }
}

private data class EvidenceItem(val kind: String, val id: Long, val timestamp: Long, val title: String, val subtitle: String, val group: String)

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
            var prediction by remember { mutableStateOf("") }
            var reasoning by remember { mutableStateOf("") }
            var evidence by remember { mutableStateOf("") }
            var linkedId by remember { mutableStateOf(questions.firstOrNull()?.id) }
            item {
                InlineFormCard("New Hypothesis", onDismiss = { showHypothesis = false; prediction = "" }, onSave = {
                    if (prediction.isNotBlank()) { viewModel.addHypothesis(linkedId, prediction, evidence, 50, reasoning); showHypothesis = false; prediction = "" }
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
        }
        if (hypotheses.isEmpty()) item { EmptyState("No hypotheses yet", "Predict what evidence would show, then test it.", icon = FieldMindIcons.Hypothesis) }
        items(hypotheses) { h -> EntityCard(h.prediction, "hypothesis", body = "Confidence: ${h.confidencePercent}% • ${h.resultStatus}", onClick = { onOpenDetail("hypothesis", h.id) }) }

        // Data tools
        item { Divider12() }
        item { SectionHeader("Data tools", "${data.size} records") }
        item { AddButton(if (showData) "Cancel" else "Add data record") { showData = !showData } }
        if (showData) {
            var tool by remember { mutableStateOf("Counter") }
            var label by remember { mutableStateOf("") }
            var value by remember { mutableStateOf("0") }
            var unit by remember { mutableStateOf("count") }
            var notes by remember { mutableStateOf("") }
            item {
                InlineFormCard("Data Record", onDismiss = { showData = false; label = "" }, onSave = {
                    if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes); showData = false; label = "" }
                }, saveEnabled = label.isNotBlank()) {
                    ChoiceChips(dataTools, tool) { tool = it }
                    FieldTextField(label, { label = it }, "Label")
                    FieldTextField(value, { value = it }, "Value")
                    FieldTextField(unit, { unit = it }, "Unit")
                    FieldTextField(notes, { notes = it }, "Notes", minLines = 2)
                }
            }
        }
        if (data.isEmpty()) item { EmptyState("No data records yet", "Measure, count, compare, or log with offline tools.", icon = FieldMindIcons.Data) }
        items(data) { d -> EntityCard(d.label, "data", body = "${d.value} ${d.unit}", meta = listOf(d.toolType), onClick = { onOpenDetail("data", d.id) }) }

        // Reports
        item { Divider12() }
        item { SectionHeader("Reports", "${reports.size} reports") }
        item { AddButton(if (showReport) "Cancel" else "Build report") { showReport = !showReport } }
        if (showReport) {
            var type by remember { mutableStateOf("Field Report") }
            var title by remember { mutableStateOf("") }
            var conclusion by remember { mutableStateOf("") }
            var question by remember { mutableStateOf("") }
            item {
                InlineFormCard("Report", onDismiss = { showReport = false; title = "" }, onSave = {
                    if (title.isNotBlank()) { viewModel.addReport(type, title, question = question, conclusion = conclusion); showReport = false; title = "" }
                }, saveEnabled = title.isNotBlank()) {
                    ChoiceChips(reportTypes, type) { type = it }
                    FieldTextField(title, { title = it }, "Title")
                    FieldTextField(question, { question = it }, "Question", minLines = 2)
                    FieldTextField(conclusion, { conclusion = it }, "Conclusion / findings", minLines = 3)
                }
            }
        }
        if (reports.isEmpty()) item { EmptyState("No reports yet", "Write up your findings with background, methods, results, and conclusions.", icon = FieldMindIcons.Report) }
        items(reports) { r -> EntityCard(r.title, "report", body = r.conclusion.ifBlank { r.question }, meta = listOf(r.type, r.status), onClick = { onOpenDetail("report", r.id) }) }
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

@Composable
private fun ProjectWorkspaceCard(
    project: ProjectEntity, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>, sources: List<SourceEntity>, data: List<DataRecordEntity>,
    reports: List<ReportEntity>, onClick: () -> Unit
) {
    val relatedQuestions = questions.count { it.relatedProjectId == project.id }
    val relatedObservations = observations.count { it.projectId == project.id }
    val relatedData = data.count { it.projectId == project.id }
    val bars = listOf(
        "Q" to relatedQuestions.toFloat(),
        "Obs" to relatedObservations.toFloat(),
        "Data" to relatedData.toFloat()
    )
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
                    Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InfoChip(project.status)
            }
            Text(project.objective.ifBlank { project.researchQuestion.ifBlank { "Open project workspace" } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            BarChart(bars, barColors = listOf(FieldMindTheme.colors.question, FieldMindTheme.colors.observation, FieldMindTheme.colors.data))
        }
    }
}
