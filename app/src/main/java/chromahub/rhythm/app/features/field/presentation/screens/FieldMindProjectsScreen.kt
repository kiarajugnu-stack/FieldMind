package chromahub.rhythm.app.features.field.presentation.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.abs
// ══════════════════════════════════════════════════════════════════════
//  Projects workspace
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectsScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, startTab: Int = 0) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab.coerceIn(0, 4)) }
    val haptics = rememberFieldMindHaptics()
    val tabs = listOf("Projects", "Questions", "Hypotheses", "Data", "Reports")
    fun selectTab(next: Int) {
        val bounded = next.coerceIn(0, tabs.lastIndex)
        if (bounded != tab) {
            tab = bounded
            haptics.light()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader("Workspace", "Tie projects, questions, evidence, data, and reports together.", icon = FieldMindIcons.Projects)
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
                Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTab) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (abs(totalDrag) > 96f) {
                                    if (totalDrag < 0) selectTab(selectedTab + 1) else selectTab(selectedTab - 1)
                                }
                            }
                        )
                    }
            ) {
                when (selectedTab) {
                    0 -> ProjectPanel(viewModel, projects, questions, hypotheses, observations, sources, data, reports, onOpenDetail) { selectTab(1) }
                    1 -> QuestionPanel(viewModel, questions, onOpenDetail) { selectTab(2) }
                    2 -> HypothesisPanel(viewModel, hypotheses, questions, onOpenDetail) { selectTab(3) }
                    3 -> DataToolPanel(viewModel, data, onOpenDetail) { selectTab(4) }
                    4 -> ReportPanel(viewModel, reports, onOpenDetail)
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

@Composable
private fun ProjectPanel(
    viewModel: FieldMindViewModel,
    items: List<ProjectEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onNext: () -> Unit
) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create project") { show = true } }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 0, nextLabel = "Next: Questions", onNext = onNext) }
            item { EmptyState("No projects yet", "Create a focused workspace, then link questions, hypotheses, observations, sources, data, and reports without clutter.", icon = FieldMindIcons.Project) }
        } else {
            item { ProjectSummaryCard(items, questions, hypotheses, observations, sources, data, reports) }
            items(items) { project ->
                ProjectWorkspaceCard(project, questions, hypotheses, observations, sources, data, reports) { onOpenDetail("project", project.id) }
            }
        }
    }
    if (show) NewProjectDialog(viewModel) { show = false }
}

@Composable
private fun QuestionPanel(viewModel: FieldMindViewModel, items: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create question") { show = true } }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 0, nextLabel = "Next: Hypotheses", onNext = onNext) }
            item { EmptyState("No questions yet", "Start with one question you can observe, compare, measure, or verify.", icon = FieldMindIcons.Question) }
        }
        items(items) { EntityCard(it.questionText, "question", meta = listOf(it.status, it.priority, it.sourceType)) { onOpenDetail("question", it.id) } }
    }
    if (show) NewQuestionDialog(viewModel) { show = false }
}

@Composable
private fun HypothesisPanel(viewModel: FieldMindViewModel, items: List<HypothesisEntity>, questions: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create hypothesis") { show = true } }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 1, nextLabel = "Next: Data", onNext = onNext) }
            item { EmptyState("No hypotheses yet", "Pick a question, predict what you expect, then define what evidence would support or weaken it.", icon = FieldMindIcons.Hypothesis) }
        }
        items(items) { EntityCard(it.prediction, "hypothesis", body = "Evidence: ${it.evidenceNeeded}", meta = listOf(it.resultStatus, "confidence ${it.confidencePercent}%")) { onOpenDetail("hypothesis", it.id) } }
    }
    if (show) NewHypothesisDialog(viewModel, questions) { show = false }
}

@Composable
private fun DataToolPanel(viewModel: FieldMindViewModel, items: List<DataRecordEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    val grouped = items.groupBy { it.toolType.ifBlank { "Other" } }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Open data collection tools") { show = true } }
        if (items.isNotEmpty()) item { DataSummaryCard(items) }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 2, nextLabel = "Next: Reports", onNext = onNext) }
            item { EmptyState("Offline data tools", "Choose a tool for the category: weather uses temperature/conditions, measurements use cm/m, species tracking uses counts and traits.", icon = FieldMindIcons.Data) }
        } else {
            grouped.forEach { (tool, records) ->
                item { SectionHeader(tool, "${records.size} ${if (records.size == 1) "entry" else "entries"}") }
                items(records) { EntityCard(it.label, "data", body = it.notes.ifBlank { it.location }, meta = listOf("${it.value} ${it.unit}".trim())) { onOpenDetail("data", it.id) } }
            }
        }
    }
    if (show) NewDataRecordDialog(viewModel) { show = false }
}

@Composable
private fun ReportPanel(viewModel: FieldMindViewModel, items: List<ReportEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Build report") { show = true } }
        if (items.isNotEmpty()) item { ReportSummaryCard(items) }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 3, nextLabel = "Research flow ready", onNext = null) }
            item { EmptyState("No reports yet", "When evidence is ready, write background, question, method, results, interpretation, conclusion, limits, and next steps.", icon = FieldMindIcons.Report) }
        }
        items(items) { EntityCard(it.title, "report", body = it.conclusion.ifBlank { it.question }, meta = listOf(it.type, it.status)) { onOpenDetail("report", it.id) } }
    }
    if (show) NewReportDialog(viewModel) { show = false }
}


@Composable
private fun ResearchFlowGuide(activeStep: Int, nextLabel: String, onNext: (() -> Unit)?) {
    val steps = listOf(
        Triple("Questions", "Ask something observable.", FieldMindIcons.Question),
        Triple("Hypotheses", "Predict what evidence would show.", FieldMindIcons.Hypothesis),
        Triple("Data", "Measure, count, compare, or log.", FieldMindIcons.Data),
        Triple("Reports", "Explain claim, evidence, limits.", FieldMindIcons.Report)
    )
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Guided research flow", "Follow the sequence one small step at a time.")
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val active = index == activeStep
                    Box(Modifier.size(38.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                        Icon(icon = step.third, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(step.first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(step.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (active) AssistChip(onClick = { onNext?.invoke() }, enabled = onNext != null, label = { Text(nextLabel) })
                }
            }
        }
    }
}

@Composable
private fun ProjectSummaryCard(
    projects: List<ProjectEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>
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
            SectionHeader("Project portfolio", "${projects.size} active workspaces with evidence coverage")
            BreakdownBar(parts)
        }
    }
}

@Composable
private fun ProjectWorkspaceCard(
    project: ProjectEntity,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    onClick: () -> Unit
) {
    val relatedQuestions = questions.count { it.relatedProjectId == project.id }
    val relatedHypotheses = hypotheses.count { h -> h.linkedQuestionId != null && questions.any { it.id == h.linkedQuestionId && it.relatedProjectId == project.id } }
    val relatedObservations = observations.count { it.projectId == project.id }
    val relatedSources = sources.count { it.relatedProjectId == project.id }
    val relatedData = data.count { it.projectId == project.id }
    val relatedReports = reports.count { it.projectId == project.id }
    val bars = listOf(
        "Q" to relatedQuestions.toFloat(),
        "H" to relatedHypotheses.toFloat(),
        "Obs" to relatedObservations.toFloat(),
        "Src" to relatedSources.toFloat(),
        "Data" to relatedData.toFloat(),
        "Rpt" to relatedReports.toFloat()
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
            BarChart(bars, barColors = listOf(FieldMindTheme.colors.question, FieldMindTheme.colors.hypothesis, FieldMindTheme.colors.observation, FieldMindTheme.colors.source, FieldMindTheme.colors.data, FieldMindTheme.colors.report))
        }
    }
}

@Composable
private fun DataSummaryCard(items: List<DataRecordEntity>) {
    val colors = FieldMindTheme.colors
    val byTool = items.groupingBy { it.toolType.ifBlank { "Other" } }.eachCount().map { it.key to it.value.toFloat() }.sortedByDescending { it.second }
    val byUnit = items.groupingBy { it.unit.ifBlank { "unitless" } }.eachCount().map { it.key to it.value.toFloat() }.sortedByDescending { it.second }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Data overview", "${items.size} entries • ${byTool.size} tools • ${byUnit.size} unit groups")
            DonutChart(byTool.take(6).map { Triple(it.first, it.second, colors.categoryColor(it.first)) })
            BarChart(byUnit.take(6), barColors = byUnit.take(6).map { colors.categoryColor(it.first) }, height = 112.dp)
        }
    }
}

@Composable
private fun ReportSummaryCard(items: List<ReportEntity>) {
    val colors = FieldMindTheme.colors
    val statusParts = items.groupingBy { it.status.ifBlank { "Draft" } }.eachCount().map { Triple(it.key, it.value.toFloat(), colors.categoryColor(it.key)) }
    val typeParts = items.groupingBy { it.type.ifBlank { "Report" } }.eachCount().map { it.key to it.value.toFloat() }.sortedByDescending { it.second }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Report pipeline", "Drafts, final reports, writing progress, and output types")
            DonutChart(statusParts)
            BarChart(typeParts.take(6), barColors = typeParts.take(6).map { colors.categoryColor(it.first) }, height = 112.dp)
        }
    }
}

