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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import fieldmind.research.app.features.field.data.export.FieldReportTemplates
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.abs
import kotlin.math.ceil

// ══════════════════════════════════════════════════════════════════════
//  Workspace — Redesigned: 5 in-screen tabs (Overview, Observations, Hypotheses, Data, Reports)
//  - Project dashboard with metrics, sampling-effort tracking
//  - Consolidated from 4 duplicate routes into proper tabs
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
    val researchSessions by viewModel.researchSessions.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab.coerceIn(0, 4)) }
    val haptics = rememberFieldMindHaptics()
    val tabs = listOf("Overview", "Observations", "Hypotheses", "Data", "Reports")

    fun selectTab(next: Int) {
        val bounded = next.coerceIn(0, tabs.lastIndex)
        if (bounded != tab) { tab = bounded; haptics.light() }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader(
                "Workspace",
                "Research overview, evidence, and analysis — all connected.",
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
                    0 -> OverviewTab(viewModel, projects, observations, questions, hypotheses, sources, data, reports, researchSessions, onOpenDetail, onStartSession)
                    1 -> ObservationsTab(viewModel, observations, projects, onOpenDetail)
                    2 -> HypothesesTab(viewModel, hypotheses, questions, observations, onOpenDetail)
                    3 -> DataTab(viewModel, data, reports, observations, projects, onOpenDetail)
                    4 -> ReportsTab(viewModel, reports, projects, onOpenDetail)
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
//  Tab 0: Overview — Project dashboard with metrics, sampling-effort, recent activity
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    viewModel: FieldMindViewModel,
    projects: List<ProjectEntity>,
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    researchSessions: List<ResearchSessionEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onStartSession: (() -> Unit)? = null
) {
    var showNewProject by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Observation") }
    var investigationPlan by remember { mutableStateOf(setOf("Photo documentation", "Daily observations")) }
    var selectedTemplate by remember { mutableStateOf<Int?>(-1) }
    var showTemplates by remember { mutableStateOf(false) }
    val haptics = rememberFieldMindHaptics()

    // Project templates
    val projectTemplates = listOf(
        ProjectTemplate("Species Survey", "Observation", "What species occur here?", "Recommended data: Species Tracker. Evidence: photos, audio, location. Charts: bar + timeline.", setOf("Species counting", "Photo documentation", "Audio recording")),
        ProjectTemplate("Behavior Study", "Investigation", "What behavior is happening, when, and under what conditions?", "Recommended data: Event Log. Evidence: video, time notes, repeated observations. Charts: timeline + by-hour.", setOf("Video documentation", "Daily observations", "Behavior logging")),
        ProjectTemplate("Site Survey", "Survey", "What are the main conditions and evidence at this site?", "Recommended data: Site Log. Evidence: photos, GPS, weather, measurements.", setOf("Photo documentation", "Measurement logging", "Weather logging")),
        ProjectTemplate("Experiment", "Experiment", "What changes when one condition varies?", "Recommended data: Comparison Table. Evidence: measurements, controls, notes. Charts: comparison + line.", setOf("Measurement logging", "Comparison table", "Weekly observations")),
        ProjectTemplate("Monitoring", "Monitoring", "How does this place or subject change over time?", "Recommended data: Time Series. Evidence: repeated photos, weather, count.", setOf("Daily observations", "Photo documentation", "Weather logging"))
    )

    // ── Dashboard metrics ──
    val totalProjects = projects.size
    val totalObs = observations.size
    val obsThisWeek = observations.count { it.timestamp > System.currentTimeMillis() - 7 * 24 * 3600_000L }
    val obsThisMonth = observations.count { it.timestamp > System.currentTimeMillis() - 30 * 24 * 3600_000L }
    val uniqueSites = observations.mapNotNull { it.manualLocation.ifBlank { if (it.latitude != null && it.longitude != null) "${it.latitude},${it.longitude}" else null } }.distinct().size
    val openQuestions = questions.count { it.answer.isBlank() }
    val supportedHypotheses = hypotheses.count { it.resultStatus.equals("Supported", true) }
    val refutedHypotheses = hypotheses.count { it.resultStatus.equals("Refuted", true) }
    val untestedHypotheses = hypotheses.size - supportedHypotheses - refutedHypotheses

    // ── Sampling-effort tracking ──
    val totalFieldHours = researchSessions.sumOf { it.totalDurationMs } / 3600_000.0
    val totalSessions = researchSessions.size
    val totalObsInSessions = researchSessions.sumOf { it.observationCount }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Research Session quick-start
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

        // ── Dashboard hero card ──
        item { ProjectDashboardCard(totalObs, obsThisWeek, obsThisMonth, uniqueSites, openQuestions, supportedHypotheses, refutedHypotheses, untestedHypotheses, totalFieldHours, totalSessions, totalObsInSessions) }

        // New project button
        item { AddButton(if (showNewProject) "Cancel" else "New project") { showNewProject = !showNewProject; if (!showNewProject) { title = ""; question = "" } } }

        // Template selector
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
            item { TemplateSelectionCard(projectTemplates, selectedTemplate) { i, name, q, type, methods ->
                selectedTemplate = i; title = name; question = q; projectType = type; investigationPlan = methods; showNewProject = true; showTemplates = false
            } }
        }

        if (showNewProject) item {
            InlineFormCard("New Project", onDismiss = { showNewProject = false; title = ""; question = "" }, onSave = {
                if (title.isNotBlank()) {
                    viewModel.addProject(title, projectType, "Project type: $projectType", question, investigationPlan.joinToString(" • "))
                    showNewProject = false; title = ""; question = ""; projectType = "Observation"; investigationPlan = setOf("Photo documentation", "Daily observations")
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

        if (totalProjects == 0) {
            item {
                EmptyState("No projects yet", "Start with a name and a question. Add methods, data, and reports as your research grows.", icon = FieldMindIcons.Project, actionLabel = "Create project") { showNewProject = true }
            }
        } else {
            item { SectionHeader("Projects ($totalProjects)", "Tap any project to open workspace") }
            items(projects) { project -> ProjectDashboardCardCompact(project, observations, questions, hypotheses, sources, data, reports, researchSessions) { onOpenDetail("project", project.id) } }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProjectDashboardCard(
    totalObs: Int, obsThisWeek: Int, obsThisMonth: Int, uniqueSites: Int,
    openQuestions: Int, supportedHyp: Int, refutedHyp: Int, untestedHyp: Int,
    totalFieldHours: Double, totalSessions: Int, totalObsInSessions: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Text("Research Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Row 1: Observation metrics
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric("Total obs", totalObs.toString(), FieldMindTheme.colors.observation)
                DashboardMetric("This week", obsThisWeek.toString(), MaterialTheme.colorScheme.primary)
                DashboardMetric("This month", obsThisMonth.toString(), MaterialTheme.colorScheme.secondary)
                DashboardMetric("Sites", uniqueSites.toString(), FieldMindTheme.colors.info)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Row 2: Knowledge state
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric("Open Qs", openQuestions.toString(), FieldMindTheme.colors.question)
                DashboardMetric("Supported ✓", supportedHyp.toString(), FieldMindTheme.colors.positive)
                DashboardMetric("Refuted ✗", refutedHyp.toString(), MaterialTheme.colorScheme.error)
                DashboardMetric("Untested", untestedHyp.toString(), MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Row 3: Sampling effort
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric("Field hours", "%.1f".format(totalFieldHours), FieldMindTheme.colors.warning)
                DashboardMetric("Sessions", totalSessions.toString(), FieldMindTheme.colors.data)
                DashboardMetric("Session obs", totalObsInSessions.toString(), FieldMindTheme.colors.observation)
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TemplateSelectionCard(
    templates: List<ProjectTemplate>,
    selectedTemplate: Int?,
    onSelect: (Int, String, String, String, Set<String>) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Project templates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Choose a template to pre-fill your project setup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            templates.forEachIndexed { i, template ->
                val isSelected = selectedTemplate == i
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(i, template.name, template.question, template.type, template.methods) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 20.dp)
                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDashboardCardCompact(
    project: ProjectEntity,
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    researchSessions: List<ResearchSessionEntity>,
    onClick: () -> Unit
) {
    val relatedObs = observations.count { it.projectId == project.id }
    val relatedQs = questions.count { it.relatedProjectId == project.id }
    val relatedSources = sources.count { it.relatedProjectId == project.id }
    val relatedData = data.count { it.projectId == project.id }
    val relatedReports = reports.count { it.projectId == project.id }
    val relatedHypotheses = hypotheses.count { h -> questions.any { it.id == h.linkedQuestionId && it.relatedProjectId == project.id } }
    val projectSessions = researchSessions.count { it.projectId == project.id }
    val projectFieldHours = researchSessions.filter { it.projectId == project.id }.sumOf { it.totalDurationMs } / 3600_000.0
    val relatedObsRecent = observations.count { it.projectId == project.id && it.timestamp > System.currentTimeMillis() - 7 * 24 * 3600_000L }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 24.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$relatedObs obs", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.observation)
                        if (relatedObsRecent > 0) {
                            Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$relatedObsRecent new", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.positive)
                        }
                    }
                }
                InfoChip(project.status)
            }
            Text(project.objective.ifBlank { project.researchQuestion.ifBlank { "Open project workspace" } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProjectMetricChip(relatedObs, "obs", FieldMindTheme.colors.observation)
                ProjectMetricChip(relatedQs, "Qs", FieldMindTheme.colors.question)
                ProjectMetricChip(relatedSources, "src", FieldMindTheme.colors.source)
                ProjectMetricChip(relatedData, "data", FieldMindTheme.colors.data)
                ProjectMetricChip(relatedReports, "reports", FieldMindTheme.colors.report)
                ProjectMetricChip(projectSessions, "sessions", FieldMindTheme.colors.warning)
                    if (projectFieldHours >= 0.5) ProjectMetricChip(kotlin.math.ceil(projectFieldHours).toInt(), "hrs", FieldMindTheme.colors.project)
            }
        }
    }
}

@Composable
private fun ProjectMetricChip(value: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("$value $label", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Tab 1: Observations — All observations with project filter
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ObservationsTab(
    viewModel: FieldMindViewModel,
    observations: List<ObservationEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    val projectOptions = remember(projects) { listOf(null to "All projects") + projects.map { it.id to it.title } }
    val filtered = remember(observations, selectedProjectId) {
        if (selectedProjectId == null) observations else observations.filter { it.projectId == selectedProjectId }
    }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Observations", "${filtered.size} of ${observations.size} total") }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filter by project", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projectOptions.take(12)) { (id, label) ->
                            FilterChip(selected = selectedProjectId == id, onClick = { selectedProjectId = id }, label = { Text(label, maxLines = 1) })
                        }
                    }
                }
            }
        }
        items(filtered.sortedByDescending { it.timestamp }) { obs ->
            EntityCard(
                obs.subject.ifBlank { "Observation" },
                "observation",
                body = "${obs.category} • ${obs.date}",
                meta = listOfNotNull(obs.weatherCondition.takeIf { it.isNotBlank() }, obs.confidenceLevel),
                onClick = { onOpenDetail("observation", obs.id) }
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyState("No observations", "Observations will appear here, filtered by project.", icon = FieldMindIcons.Observation, actionLabel = "Start observing") { onOpenDetail("observe", 0) } }
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

// ──────────────────────────────────────────────────────────────────────
//  Extracted form composables
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
//  Tab 2: Hypotheses — All hypotheses with project filter
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun HypothesesTab(
    viewModel: FieldMindViewModel,
    hypotheses: List<HypothesisEntity>,
    questions: List<QuestionEntity>,
    observations: List<ObservationEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    val projectQuestions = remember(questions) { questions.filter { it.relatedProjectId != null } }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Hypotheses", "${hypotheses.size} predictions • ${hypotheses.count { it.resultStatus == "Supported" }} supported, ${hypotheses.count { it.resultStatus == "Refuted" }} refuted") }
        item { AddButton(if (showForm) "Cancel" else "New hypothesis") { showForm = !showForm } }
        if (showForm) item { HypothesisForm(questions, viewModel, onDismiss = { showForm = false }) }
        if (hypotheses.isEmpty()) item { EmptyState("No hypotheses yet", "Predict what evidence would show, then test it.", icon = FieldMindIcons.Hypothesis) }
        items(hypotheses.sortedByDescending { it.createdAt }) { h ->
            val supportColor = when (h.resultStatus.lowercase()) {
                "supported" -> FieldMindTheme.colors.positive
                "refuted" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            EntityCard(
                h.prediction, "hypothesis",
                body = "${h.reasoning.take(120)}",
                meta = listOf("${h.confidencePercent}%", h.resultStatus),
                accentColor = supportColor,
                onClick = { onOpenDetail("hypothesis", h.id) }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Tab 3: Data — All data records with project filter
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun DataTab(
    viewModel: FieldMindViewModel,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    observations: List<ObservationEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Data Records", "${data.size} records across ${data.map { it.datasetKind }.distinct().size} datasets") }
        item { TrackingFlowCards() }
        item { AddButton(if (showForm) "Cancel" else "Add data record") { showForm = !showForm } }
        if (showForm) item { DataRecordForm(viewModel, onDismiss = { showForm = false }) }
        if (data.isEmpty()) item { EmptyState("No data records yet", "Measure, count, compare, or log with offline tools.", icon = FieldMindIcons.Data) }
        items(data.sortedByDescending { it.timestamp }) { d -> EntityCard(d.label, "data", body = "${d.value} ${d.unit}", meta = listOf(d.toolType, d.datasetKind), onClick = { onOpenDetail("data", d.id) }) }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Tab 4: Reports — All reports with project filter
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ReportsTab(
    viewModel: FieldMindViewModel,
    reports: List<ReportEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Reports", "${reports.size} reports • ${reports.count { it.status == "Published" }} published") }
        item { AddButton(if (showForm) "Cancel" else "Build report") { showForm = !showForm } }
        if (showForm) item { ReportForm(viewModel, onDismiss = { showForm = false }) }
        if (reports.isEmpty()) item { EmptyState("No reports yet", "Write up your findings with background, methods, results, and conclusions.", icon = FieldMindIcons.Report) }
        items(reports.sortedByDescending { it.createdAt }) { r -> EntityCard(r.title, "report", body = r.conclusion.ifBlank { r.question }, meta = listOf(r.type, r.status), onClick = { onOpenDetail("report", r.id) }) }
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


// ──────────────────────────────────────────────────────────────────────
//  Summary cards
// ──────────────────────────────────────────────────────────────────────


