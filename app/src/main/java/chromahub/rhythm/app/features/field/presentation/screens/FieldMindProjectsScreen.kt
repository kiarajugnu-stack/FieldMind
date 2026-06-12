package fieldmind.research.app.features.field.presentation.screens

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
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
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
    var title by remember { mutableStateOf("") }; var topic by remember { mutableStateOf("Biology") }; var objective by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var hypothesis by remember { mutableStateOf("") }; var dataPlan by remember { mutableStateOf("") }; var analysis by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var nextAction by remember { mutableStateOf("") }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton(if (show) "Cancel project form" else "Create project") { show = !show; if (!show) { title = ""; objective = ""; question = "" } } }
        if (show) item {
            InlineFormCard("New Project", onDismiss = { show = false; title = ""; objective = ""; question = "" }, onSave = {
                if (title.isNotBlank()) { viewModel.addProject(title, topic, objective, question, methods, nextAction, background, hypothesis, dataPlan, analysis, conclusion); show = false; title = "" }
            }, saveEnabled = title.isNotBlank()) {
                CaptureStep("Topic & title", "Name the project and choose the broad research area.", FieldMindIcons.Project) {
                    FieldTextField(title, { title = it }, "Project title")
                    ChoiceChips(listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
                }
                CaptureStep("Research purpose", "Write a concrete objective and a question that can guide observations.", FieldMindIcons.Question) {
                    FieldTextField(objective, { objective = it }, "Objective", minLines = 2)
                    FieldTextField(question, { question = it }, "Research question", minLines = 2)
                    FieldTextField(background, { background = it }, "Background / context", minLines = 2)
                }
                CaptureStep("Evidence plan", "Add the planned method, hypothesis, and category-specific data fields.", FieldMindIcons.Data) {
                    FieldTextField(methods, { methods = it }, "Method / data plan", minLines = 3)
                    FieldTextField(hypothesis, { hypothesis = it }, "Hypothesis summary", minLines = 2)
                    FieldTextField(dataPlan, { dataPlan = it }, "Data fields / units", supportingText = "Example: temperature °C, height cm, water clarity, count")
                }
                CaptureStep("Report direction", "Keep analysis, conclusion, and next action visible without cluttering the card.", FieldMindIcons.Report) {
                    FieldTextField(analysis, { analysis = it }, "Analysis plan", minLines = 2)
                    FieldTextField(conclusion, { conclusion = it }, "Early conclusion / expected output", minLines = 2)
                    FieldTextField(nextAction, { nextAction = it }, "Next action", supportingText = "Example: observe the same site at sunset for 3 days")
                }
            }
        }
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
}

@Composable
private fun QuestionPanel(viewModel: FieldMindViewModel, items: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Other") }; var source by remember { mutableStateOf("Observation") }; var status by remember { mutableStateOf("New") }; var priority by remember { mutableStateOf("Medium") }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton(if (show) "Cancel question" else "Create question") { show = !show; if (!show) question = "" } }
        if (show) item {
            InlineFormCard("New Question", onDismiss = { show = false; question = "" }, onSave = { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); show = false; question = "" } }, saveEnabled = question.isNotBlank()) {
                CaptureStep("Question", "Use one clear sentence and avoid assuming the answer.", FieldMindIcons.Question) {
                    FieldTextField(question, { question = it }, "What do you want to find out?", minLines = 3, supportingText = "Example: Do bird visits increase after rain at this site?")
                }
                CaptureStep("Classify", "Presets keep searching, charts, and reports cleaner.", FieldMindIcons.Category) {
                    ChoiceChips(observationCategories, category) { category = it }
                    Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(sourceTypes, source) { source = it }
                    Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(listOf("Low", "Medium", "High"), priority) { priority = it }
                    Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(questionStatuses, status) { status = it }
                }
            }
        }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 0, nextLabel = "Next: Hypotheses", onNext = onNext) }
            item { EmptyState("No questions yet", "Start with one question you can observe, compare, measure, or verify.", icon = FieldMindIcons.Question) }
        }
        items(items) { EntityCard(it.questionText, "question", meta = listOf(it.status, it.priority, it.sourceType)) { onOpenDetail("question", it.id) } }
    }
}

@Composable
private fun HypothesisPanel(viewModel: FieldMindViewModel, items: List<HypothesisEntity>, questions: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf("") }; var reasoning by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var support by remember { mutableStateOf("") }; var weaken by remember { mutableStateOf("") }; var test by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf(50f) }; var linkedId by remember { mutableStateOf(questions.firstOrNull()?.id) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton(if (show) "Cancel hypothesis" else "Create hypothesis") { show = !show; if (!show) { prediction = ""; reasoning = ""; evidence = "" } } }
        if (show) item {
            InlineFormCard("New Hypothesis", onDismiss = { show = false; prediction = "" }, onSave = { if (prediction.isNotBlank()) { viewModel.addHypothesis(linkedId, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test); show = false; prediction = "" } }, saveEnabled = prediction.isNotBlank()) {
                CaptureStep("Link & prediction", "Connect it to a question if one exists.", FieldMindIcons.Hypothesis) {
                    if (questions.isNotEmpty()) ChoiceChips(listOf("No question") + questions.take(8).map { it.questionText.take(28) }, questions.firstOrNull { it.id == linkedId }?.questionText?.take(28) ?: "No question") { picked -> linkedId = questions.firstOrNull { it.questionText.startsWith(picked) }?.id }
                    FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 3)
                    FieldTextField(reasoning, { reasoning = it }, "Why this might happen", minLines = 2)
                }
                CaptureStep("Evidence rules", "Decide success/failure before you bias yourself.", FieldMindIcons.Check) {
                    FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2)
                    FieldTextField(support, { support = it }, "Support criteria")
                    FieldTextField(weaken, { weaken = it }, "Weakening criteria")
                    FieldTextField(test, { test = it }, "Test method")
                    Text("Confidence: ${confidence.toInt()}%")
                    Slider(confidence, { confidence = it }, valueRange = 0f..100f)
                }
            }
        }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 1, nextLabel = "Next: Data", onNext = onNext) }
            item { EmptyState("No hypotheses yet", "Pick a question, predict what you expect, then define what evidence would support or weaken it.", icon = FieldMindIcons.Hypothesis) }
        }
        items(items) { EntityCard(it.prediction, "hypothesis", body = "Evidence: ${it.evidenceNeeded}", meta = listOf(it.resultStatus, "confidence ${it.confidencePercent}%")) { onOpenDetail("hypothesis", it.id) } }
    }
}

@Composable
private fun DataToolPanel(viewModel: FieldMindViewModel, items: List<DataRecordEntity>, onOpenDetail: (String, Long) -> Unit, onNext: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf("Counter") }; var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("0") }; var unit by remember { mutableStateOf("count") }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    val grouped = items.groupBy { it.toolType.ifBlank { "Other" } }
    fun defaultUnitForTool(t: String): String = when (t) {
        "Weather Log" -> "°C"; "Measurement Log" -> "cm"; "Counter", "Species Tracker" -> "count"; "Event Log" -> "event"; "Site Log" -> "site"; "Checklist" -> "done/total"; "Comparison Table" -> "score"; else -> ""
    }
    fun defaultLabelForTool(t: String): String = when (t) {
        "Weather Log" -> "Air temperature"; "Measurement Log" -> "Measured length"; "Species Tracker" -> "Species count"; "Checklist" -> "Checklist item"; "Event Log" -> "Observed event"; "Site Log" -> "Site condition"; "Comparison Table" -> "Comparison variable"; else -> ""
    }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton(if (show) "Cancel form" else "Open data collection tools") { show = !show; if (!show) { label = ""; value = "0" } } }
        if (show) item {
            InlineFormCard("Data Collection Tool", onDismiss = { show = false; label = "" }, onSave = { if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, location); show = false; label = "" } }, saveEnabled = label.isNotBlank()) {
                CaptureStep("Preset", "Each tool adapts labels and units for the category.", FieldMindIcons.Data) {
                    ChoiceChips(dataTools, tool) { tool = it; unit = defaultUnitForTool(it); label = defaultLabelForTool(it) }
                    FieldTextField(label, { label = it }, "Label")
                    if (tool == "Counter" || tool == "Species Tracker") Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }; Text(value, style = MaterialTheme.typography.headlineSmall); Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }; TextButton({ value = "0" }) { Text("Reset") } }
                }
                CaptureStep("Measurement", "Use the suggested unit or type a better one.", FieldMindIcons.Graph) {
                    FieldTextField(value, { value = it }, "Value")
                    FieldTextField(unit, { unit = it }, "Unit")
                    FieldTextField(location, { location = it }, "Location / site")
                }
                CaptureStep("Context", "Add conditions, instrument notes, mood, or quality flags.", FieldMindIcons.Note) {
                    ChoiceChips(contextPresets, notes) { notes = if (notes.isBlank()) it else "$notes, $it" }
                    FieldTextField(notes, { notes = it }, "Notes", minLines = 3)
                }
            }
        }
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
}

@Composable
private fun ReportPanel(viewModel: FieldMindViewModel, items: List<ReportEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("Field Report") }; var title by remember { mutableStateOf("") }; var background by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var observations by remember { mutableStateOf("") }; var results by remember { mutableStateOf("") }; var interpretation by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var limitations by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton(if (show) "Cancel report" else "Build report") { show = !show; if (!show) { title = ""; conclusion = "" } } }
        if (show) item {
            InlineFormCard("Report Builder", onDismiss = { show = false; title = "" }, onSave = { if (title.isNotBlank()) { viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next); show = false; title = "" } }, saveEnabled = title.isNotBlank()) {
                CaptureStep("Type & title", "Pick a report preset for export organization.", FieldMindIcons.Report) {
                    ChoiceChips(reportTypes, type) { type = it }
                    FieldTextField(title, { title = it }, "Title")
                }
                CaptureStep("Setup", "Frame the research before results.", FieldMindIcons.Question) {
                    FieldTextField(background, { background = it }, "Background", minLines = 2)
                    FieldTextField(question, { question = it }, "Question", minLines = 2)
                    FieldTextField(methods, { methods = it }, "Methods", minLines = 2)
                }
                CaptureStep("Evidence", "Summarize observations and data clearly.", FieldMindIcons.Data) {
                    FieldTextField(observations, { observations = it }, "Observations", minLines = 2)
                    FieldTextField(results, { results = it }, "Data / results", minLines = 2)
                    FieldTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2)
                }
                CaptureStep("Conclusion", "Be honest about uncertainty and what comes next.", FieldMindIcons.Check) {
                    FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
                    FieldTextField(limitations, { limitations = it }, "Limitations", minLines = 2)
                    FieldTextField(next, { next = it }, "Next steps", minLines = 2)
                }
            }
        }
        if (items.isNotEmpty()) item { ReportSummaryCard(items) }
        if (items.isEmpty()) {
            item { ResearchFlowGuide(activeStep = 3, nextLabel = "Research flow ready", onNext = null) }
            item { EmptyState("No reports yet", "When evidence is ready, write background, question, method, results, interpretation, conclusion, limits, and next steps.", icon = FieldMindIcons.Report) }
        }
        items(items) { EntityCard(it.title, "report", body = it.conclusion.ifBlank { it.question }, meta = listOf(it.type, it.status)) { onOpenDetail("report", it.id) } }
    }
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

