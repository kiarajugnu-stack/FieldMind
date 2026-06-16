package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
//  Research Hub — Full redesign per spec
//  Features: Start Session, New Project, Templates (18 types + 17 templates),
//  Emoji picker, Priority/Dates/Tags, 5 tabs (Overview, Observations,
//  Hypotheses, Data, Reports)
// ══════════════════════════════════════════════════════════════════════

// ── Project Types (18 from spec) ──
internal val researchProjectTypes = listOf(
    "Species Survey", "Population Census", "Habitat Assessment",
    "Wildlife Monitoring", "Behavioral Study", "Migration Study",
    "Biodiversity Inventory", "Vegetation Survey", "Conservation Project",
    "Ecological Research", "Camera Trap Study", "Acoustic Monitoring",
    "Pollinator Survey", "Water Quality Study", "Citizen Science Project",
    "Environmental Impact Study", "Long Term Monitoring", "Species Discovery"
)

// ── Project Templates (17 from spec) ──
internal data class ProjectTemplateDef(
    val name: String,
    val type: String,
    val icon: String,
    val description: String,
    val defaultMethods: Set<String>
)

internal val projectTemplates = listOf(
    ProjectTemplateDef("Bird Survey", "Species Survey", "🐦", "Systematic bird count and species identification with habitat notes.", setOf("Photo documentation", "Species counting", "Audio recording")),
    ProjectTemplateDef("Mammal Survey", "Population Census", "🐾", "Track signs, scat, camera trap data and direct observations.", setOf("Photo documentation", "Species counting", "GPS tracking")),
    ProjectTemplateDef("Butterfly Survey", "Pollinator Survey", "🦋", "Transect-based butterfly counts with host plant records.", setOf("Species counting", "Photo documentation", "Weather logging")),
    ProjectTemplateDef("Plant Documentation", "Vegetation Survey", "🌿", "Quadrat-based vegetation sampling with growth stage records.", setOf("Measurement logging", "Photo documentation", "Species counting")),
    ProjectTemplateDef("Nest Monitoring", "Wildlife Monitoring", "🏠", "Track active nests with periodic checks and success/failure records.", setOf("Daily observations", "Photo documentation", "Behavior logging")),
    ProjectTemplateDef("Camera Trap Research", "Camera Trap Study", "📷", "Deploy camera traps with bait stations, check SD cards, classify captures.", setOf("Photo documentation", "Species counting", "Weekly observations")),
    ProjectTemplateDef("Amphibian Survey", "Species Survey", "🐸", "Wetland/pond surveys for frogs, toads, and salamanders.", setOf("Audio recording", "Photo documentation", "Weather logging")),
    ProjectTemplateDef("Reptile Survey", "Species Survey", "🦎", "Herp searches with cover object checks and basking observations.", setOf("Photo documentation", "Measurement logging", "GPS tracking")),
    ProjectTemplateDef("Pollinator Study", "Pollinator Survey", "🐝", "Flower visitor observations with timed counts per plant species.", setOf("Daily observations", "Behavior logging", "Photo documentation")),
    ProjectTemplateDef("Wetland Assessment", "Habitat Assessment", "💧", "Hydrology, vegetation, and water quality assessment of wetland sites.", setOf("Water testing", "Measurement logging", "Photo documentation")),
    ProjectTemplateDef("Forest Biodiversity", "Biodiversity Inventory", "🌲", "Inventory all species in a forest plot with taxonomy records.", setOf("Species counting", "Photo documentation", "Measurement logging")),
    ProjectTemplateDef("Urban Wildlife Survey", "Citizen Science Project", "🏙️", "Document urban wildlife with community participation.", setOf("Daily observations", "Photo documentation", "GPS tracking")),
    ProjectTemplateDef("Species Count Project", "Population Census", "📊", "Repeated counts at fixed points for population estimation.", setOf("Species counting", "Daily observations", "Weather logging")),
    ProjectTemplateDef("Migration Tracking", "Migration Study", "🗺️", "Track migration timing, routes, and stopover sites.", setOf("GPS tracking", "Daily observations", "Photo documentation")),
    ProjectTemplateDef("Acoustic Survey", "Acoustic Monitoring", "🎧", "Deploy audio recorders for call-based species detection.", setOf("Audio recording", "Species counting", "Photo documentation")),
    ProjectTemplateDef("Habitat Assessment", "Habitat Assessment", "🌍", "Full habitat structure assessment with vegetation and soil measurements.", setOf("Measurement logging", "Photo documentation", "GPS tracking")),
    ProjectTemplateDef("Custom Blank Template", "Custom Research Project", "📋", "Start from scratch — define your own research from the ground up.", emptySet())
)

// ── Emoji icon options for projects ──
internal val projectEmojis = listOf("🐦", "🌿", "🦋", "🌳", "🦎", "🐸", "🐝", "🦉", "🦊", "🐟", "🌺", "🍄", "🪨", "💧", "🌊", "☀️", "🌙", "⭐", "📷", "🎧", "🗺️", "📊", "📋", "🧪")

// ── Research Categories ──
internal val researchCategories = listOf(
    "Ornithology", "Mammalogy", "Herpetology", "Entomology",
    "Botany", "Ecology", "Conservation", "Climate Science",
    "Hydrology", "Geology", "Oceanography", "Citizen Science",
    "Behavioral Ecology", "Evolution", "Taxonomy", "Other"
)

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
        // ── Research Hub Header ──
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(FieldMindTheme.colors.project.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Projects, null, tint = FieldMindTheme.colors.project, size = 28.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Research Hub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Manage projects, templates, and research workflow", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
                    0 -> ResearchHubOverviewTab(viewModel, projects, observations, questions, hypotheses, sources, data, reports, researchSessions, onOpenDetail, onStartSession)
                    1 -> ObservationsTab(viewModel, observations, projects, onOpenDetail)
                    2 -> HypothesesTab(viewModel, hypotheses, questions, observations, onOpenDetail)
                    3 -> DataTab(viewModel, data, reports, observations, projects, onOpenDetail)
                    4 -> ReportsTab(viewModel, reports, projects, onOpenDetail)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Research Hub Overview Tab — Full redesign with templates, emoji, dates
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchHubOverviewTab(
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
    var showTemplates by remember { mutableStateOf(false) }
    var showTypes by remember { mutableStateOf(false) }

    // Project creation state (full spec)
    var projTitle by remember { mutableStateOf("") }
    var projType by remember { mutableStateOf(researchProjectTypes[0]) }
    var projTemplate by remember { mutableStateOf(projectTemplates.last().name) }
    var projEmoji by remember { mutableStateOf("🐦") }
    var projDesc by remember { mutableStateOf("") }
    var projCategory by remember { mutableStateOf(researchCategories[0]) }
    var projPriority by remember { mutableStateOf("Medium") }
    var projStatus by remember { mutableStateOf("Planning") }
    var projStartDate by remember { mutableStateOf("") }
    var projEndDate by remember { mutableStateOf("") }
    var projTeam by remember { mutableStateOf("") }
    var projTags by remember { mutableStateOf("") }
    var projQuestion by remember { mutableStateOf("") }
    var projMethods by remember { mutableStateOf(setOf("Photo documentation", "Daily observations")) }
    var projHypothesis by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val haptics = rememberFieldMindHaptics()

    // Computed metrics
    val totalProjects = projects.size
    val totalObs = observations.size
    val obsThisWeek = observations.count { it.timestamp > System.currentTimeMillis() - 7 * 24 * 3600_000L }
    val obsThisMonth = observations.count { it.timestamp > System.currentTimeMillis() - 30 * 24 * 3600_000L }
    val uniqueSites = observations.mapNotNull { it.manualLocation.ifBlank { if (it.latitude != null && it.longitude != null) "${it.latitude},${it.longitude}" else null } }.distinct().size
    val openQuestions = questions.count { it.answer.isBlank() }
    val supportedHypotheses = hypotheses.count { it.resultStatus.equals("Supported", true) }
    val refutedHypotheses = hypotheses.count { it.resultStatus.equals("Refuted", true) }
    val untestedHypotheses = hypotheses.size - supportedHypotheses - refutedHypotheses
    val totalFieldHours = researchSessions.sumOf { it.totalDurationMs } / 3600_000.0
    val totalSessions = researchSessions.size
    val totalObsInSessions = researchSessions.sumOf { it.observationCount }

    LazyColumn(contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── 1. Start Research Session ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { haptics.light(); onStartSession?.invoke() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.project.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Bolt, null, tint = FieldMindTheme.colors.project, size = 28.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Start Research Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Timer-based multi-observation capture with GPS tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                }
            }
        }

        // ── 2. Dashboard Metrics ──
        item { ResearchHubDashboard(totalObs, obsThisWeek, obsThisMonth, uniqueSites, openQuestions, supportedHypotheses, refutedHypotheses, untestedHypotheses, totalFieldHours, totalSessions, totalObsInSessions) }

        // ── 3. New Project Button + Templates + Types ──
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showNewProject = !showNewProject; if (!showNewProject) { projTitle = ""; projDesc = ""; projQuestion = "" } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(FieldMindIcons.Add, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showNewProject) "Cancel" else "New Project")
                }
                OutlinedButton(
                    onClick = { showTemplates = !showTemplates; if (showTemplates) showTypes = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(FieldMindIcons.Project, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showTemplates) "Hide templates" else "Templates")
                }
                OutlinedButton(
                    onClick = { showTypes = !showTypes; if (showTypes) showTemplates = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(FieldMindIcons.Category, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showTypes) "Hide types" else "Types")
                }
            }
        }

        // ── 4. Project Types Grid ──
        if (showTypes) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Project Types (${researchProjectTypes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Select a type to pre-configure your research project.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(researchProjectTypes.chunked(6)) { chunk ->
                                Column(Modifier.width(160.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    chunk.forEach { type ->
                                        Surface(
                                            onClick = { projType = type; projTitle = ""; showTypes = false; showNewProject = true },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (projType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(FieldMindIcons.Project, null, tint = if (projType == type) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                                                Text(type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 5. Templates Grid ──
        if (showTemplates) {
            item { TemplatesGrid(projectTemplates) { template ->
                projTemplate = template.name
                projType = template.type
                projTitle = template.name
                projDesc = template.description
                projMethods = template.defaultMethods
                showTemplates = false
                showNewProject = true
            } }
        }

        // ── 6. Project Creation Form (full spec) ──
        if (showNewProject) item {
            ProjectCreationForm(
                title = projTitle, onTitleChange = { projTitle = it },
                projectType = projType, onProjectTypeChange = { projType = it },
                template = projTemplate, onTemplateChange = { projTemplate = it },
                emoji = projEmoji, onEmojiChange = { projEmoji = it },
                description = projDesc, onDescChange = { projDesc = it },
                category = projCategory, onCategoryChange = { projCategory = it },
                priority = projPriority, onPriorityChange = { projPriority = it },
                status = projStatus, onStatusChange = { projStatus = it },
                startDate = projStartDate, onStartDateChange = { projStartDate = it },
                endDate = projEndDate, onEndDateChange = { projEndDate = it },
                team = projTeam, onTeamChange = { projTeam = it },
                tags = projTags, onTagsChange = { projTags = it },
                question = projQuestion, onQuestionChange = { projQuestion = it },
                methods = projMethods, onMethodsChange = { projMethods = it },
                hypothesis = projHypothesis, onHypothesisChange = { projHypothesis = it },
                showEmojiPicker = showEmojiPicker, onToggleEmojiPicker = { showEmojiPicker = !showEmojiPicker },
                onSave = {
                    if (projTitle.isNotBlank()) {
                        val methodsStr = projMethods.joinToString(" • ")
                        val fullDesc = buildString {
                            append(projDesc)
                            if (projCategory.isNotBlank()) append("\nCategory: $projCategory")
                            if (projHypothesis.isNotBlank()) append("\nHypothesis: $projHypothesis")
                            if (projTags.isNotBlank()) append("\nTags: $projTags")
                            if (projTeam.isNotBlank()) append("\nTeam: $projTeam")
                            if (projStartDate.isNotBlank()) append("\nStart: $projStartDate")
                            if (projEndDate.isNotBlank()) append("\nEnd: $projEndDate")
                        }
                        viewModel.addProject(
                            title = "$projEmoji $projTitle",
                            topicType = projType,
                            objective = fullDesc.trim(),
                            researchQuestion = projQuestion,
                            methods = methodsStr,
                            backgroundNotes = "Priority: $projPriority • Status: $projStatus • Template: $projTemplate"
                        )
                        showNewProject = false; projTitle = ""; projDesc = ""; projQuestion = ""; projHypothesis = ""
                    }
                },
                onDismiss = { showNewProject = false; projTitle = ""; projDesc = ""; projQuestion = "" }
            )
        }

        // ── 7. Projects List ──
        if (totalProjects == 0) {
            item {
                EmptyState("No projects yet", "Start with a name and a question. Use templates or pick a project type to get started.", icon = FieldMindIcons.Project, actionLabel = "Create project") { showNewProject = true }
            }
        } else {
            item { SectionHeader("Projects ($totalProjects)", "Tap any project to open the workspace") }
            items(projects) { project -> ProjectDashboardCardCompact(project, observations, questions, hypotheses, sources, data, reports, researchSessions) { onOpenDetail("project", project.id) } }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Research Hub Dashboard
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchHubDashboard(
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric(totalObs.toString(), "Total obs", FieldMindTheme.colors.observation)
                DashboardMetric(obsThisWeek.toString(), "This week", MaterialTheme.colorScheme.primary)
                DashboardMetric(obsThisMonth.toString(), "This month", MaterialTheme.colorScheme.secondary)
                DashboardMetric(uniqueSites.toString(), "Sites", FieldMindTheme.colors.info)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric(openQuestions.toString(), "Open Qs", FieldMindTheme.colors.question)
                DashboardMetric(supportedHyp.toString(), "Supported", FieldMindTheme.colors.positive)
                DashboardMetric(refutedHyp.toString(), "Refuted", MaterialTheme.colorScheme.error)
                DashboardMetric(untestedHyp.toString(), "Untested", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric("%.1f".format(totalFieldHours), "Field hours", FieldMindTheme.colors.warning)
                DashboardMetric(totalSessions.toString(), "Sessions", FieldMindTheme.colors.data)
                DashboardMetric(totalObsInSessions.toString(), "Session obs", FieldMindTheme.colors.observation)
            }
        }
    }
}

@Composable
private fun DashboardMetric(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Templates Grid
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TemplatesGrid(templates: List<ProjectTemplateDef>, onSelect: (ProjectTemplateDef) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Templates (${templates.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Choose a template to pre-fill your project", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates) { template ->
                    Card(
                        modifier = Modifier.width(200.dp).clickable { onSelect(template) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(template.icon, fontSize = 28.sp)
                            Text(template.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                template.defaultMethods.take(3).forEach { method ->
                                    Text(method.take(12), style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.project, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Project Creation Form — Full spec with all fields
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCreationForm(
    title: String, onTitleChange: (String) -> Unit,
    projectType: String, onProjectTypeChange: (String) -> Unit,
    template: String, onTemplateChange: (String) -> Unit,
    emoji: String, onEmojiChange: (String) -> Unit,
    description: String, onDescChange: (String) -> Unit,
    category: String, onCategoryChange: (String) -> Unit,
    priority: String, onPriorityChange: (String) -> Unit,
    status: String, onStatusChange: (String) -> Unit,
    startDate: String, onStartDateChange: (String) -> Unit,
    endDate: String, onEndDateChange: (String) -> Unit,
    team: String, onTeamChange: (String) -> Unit,
    tags: String, onTagsChange: (String) -> Unit,
    question: String, onQuestionChange: (String) -> Unit,
    methods: Set<String>, onMethodsChange: (Set<String>) -> Unit,
    hypothesis: String, onHypothesisChange: (String) -> Unit,
    showEmojiPicker: Boolean, onToggleEmojiPicker: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    val scrollState = rememberScrollState()

    InlineFormCard("New Research Project", onDismiss = onDismiss, onSave = onSave, saveEnabled = title.isNotBlank()) {
        Column(Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Project Title
            FieldTextField(title, onTitleChange, "Project Title", supportingText = "Give your investigation a name")

            // Project Type
            Text("Project Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(researchProjectTypes) { type ->
                    FilterChip(
                        selected = projectType == type,
                        onClick = { onProjectTypeChange(type) },
                        label = { Text(type, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Template
            Text("Template", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(projectTemplates.map { it.name }, template) { onTemplateChange(it) }

            // Emoji Icon Picker
            Text("Project Icon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 32.sp)
                OutlinedButton(onClick = onToggleEmojiPicker, shape = RoundedCornerShape(12.dp)) { Text("Choose icon") }
            }
            AnimatedVisibility(showEmojiPicker) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    projectEmojis.forEach { e ->
                        Surface(
                            onClick = { onEmojiChange(e); onToggleEmojiPicker() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (emoji == e) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(e, modifier = Modifier.padding(6.dp), fontSize = 20.sp)
                        }
                    }
                }
            }

            // Description
            FieldTextField(description, onDescChange, "Description", minLines = 3, supportingText = "Describe your project objectives and scope")

            // Research Category
            Text("Research Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(researchCategories, category) { onCategoryChange(it) }

            // Priority
            Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low", "Medium", "High").forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { onPriorityChange(p) },
                        label = { Text(p) },
                        leadingIcon = if (priority == p) ({ Icon(FieldMindIcons.Check, null, size = 16.dp) }) else null
                    )
                }
            }

            // Status
            Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(listOf("Planning", "Active", "On Hold", "Completed", "Archived"), status) { onStatusChange(it) }

            // Dates
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(startDate, onStartDateChange, "Start Date", modifier = Modifier.weight(1f), supportingText = "YYYY-MM-DD")
                FieldTextField(endDate, onEndDateChange, "End Date", modifier = Modifier.weight(1f), supportingText = "YYYY-MM-DD")
            }

            // Team Members
            FieldTextField(team, onTeamChange, "Team Members", supportingText = "Comma-separated names")

            // Tags
            FieldTextField(tags, onTagsChange, "Tags", supportingText = "Comma-separated keywords")

            // Research Question
            Text("Research Question Builder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = FieldMindTheme.colors.question)
            FieldTextField(question, onQuestionChange, "Research Question", minLines = 2, supportingText = "What do you want to find out?")

            // Hypothesis
            Text("Hypothesis", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FieldTextField(hypothesis, onHypothesisChange, "Hypothesis Statement", minLines = 2, supportingText = "What do you predict?")

            // Research Methods
            Text("Research Methods", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ResearchMethodBuilder(methods, onMethodsChange)

            // Preview
            ResearchPreviewCard(projectType, methods)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchMethodBuilder(selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    val options = listOf("Daily observations", "Weekly observations", "Photo documentation", "Audio recording",
        "Video documentation", "Measurement logging", "Species counting", "Weather logging",
        "Behavior logging", "Comparison table", "GPS tracking", "Water testing", "Camera trap")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onSelected(if (option in selected) selected - option else selected + option) },
                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (option in selected) ({ Icon(FieldMindIcons.Check, null, size = 16.dp) }) else null
            )
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
    methods.any { "Camera" in it } -> "Camera Trap Log"
    else -> "Observation Timeline"
}

private fun recommendedEvidenceFor(methods: Set<String>): String = listOfNotNull(
    "photos".takeIf { methods.any { "Photo" in it } },
    "audio".takeIf { methods.any { "Audio" in it } },
    "video".takeIf { methods.any { "Video" in it } },
    "measurements".takeIf { methods.any { "Measurement" in it } },
    "GPS + weather".takeIf { methods.any { "Weather" in it || "Daily" in it || "GPS" in it } },
    "water data".takeIf { methods.any { "Water" in it } },
    "camera trap".takeIf { methods.any { "Camera" in it } }
).ifEmpty { listOf("notes", "observations") }.joinToString(", ")

// ══════════════════════════════════════════════════════════════════════
//  Project Card (preserved from original with expanded metrics)
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectDashboardCardCompact(
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
                if (projectFieldHours >= 0.5) ProjectMetricChip(ceil(projectFieldHours).toInt(), "hrs", FieldMindTheme.colors.project)
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

// ── Remaining tabs (Observations, Hypotheses, Data, Reports) preserved from original ──

@Composable
internal fun AddButton(label: String, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Button(onClick = { haptics.light(); onClick() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 20.dp); Spacer(Modifier.size(8.dp)); Text(label)
    }
}

internal fun panelPadding() = PaddingValues(20.dp, 4.dp, 20.dp, 96.dp)

// ── Tab 1: Observations ──

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

// ── Hypothesis Form ──

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
        FieldTextField(prediction, { prediction = it }, "Hypothesis", minLines = 2, supportingText = "If... then... because...")
        FieldTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2)
        FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2)
    }
}

// ── Data Record Form ──

@Composable
private fun DataRecordForm(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var tool by remember { mutableStateOf("Counter") }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf("count") }
    var notes by remember { mutableStateOf("") }
    var datasetKind by remember { mutableStateOf("Measurements") }
    var chartMode by remember { mutableStateOf("Line") }
    InlineFormCard("Data Entry", onDismiss = { onDismiss(); label = "" }, onSave = {
        if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, datasetKind = datasetKind, chartPreference = chartMode); onDismiss(); label = "" }
    }, saveEnabled = label.isNotBlank()) {
        ChoiceChips(listOf("Counters", "Measurements", "Event logs", "Weather logs", "Species tracking", "Comparison table", "Time series"), datasetKind) { datasetKind = it }
        ChoiceChips(listOf("Bar", "Line", "Donut/Pie", "Breakdown", "Timeline"), chartMode) { chartMode = it }
        ChoiceChips(dataTools, tool) { tool = it }
        FieldTextField(label, { label = it }, "Label")
        FieldTextField(value, { value = it }, "Value")
        FieldTextField(unit, { unit = it }, "Unit")
        FieldTextField(notes, { notes = it }, "Notes", minLines = 2)
    }
}

// ── Report Form ──

@Composable
private fun ReportForm(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
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
    InlineFormCard("Report Builder", onDismiss = { onDismiss(); title = "" }, onSave = {
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

// ── Tab 2: Hypotheses ──

@Composable
private fun HypothesesTab(
    viewModel: FieldMindViewModel,
    hypotheses: List<HypothesisEntity>,
    questions: List<QuestionEntity>,
    observations: List<ObservationEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
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
            EntityCard(h.prediction, "hypothesis", body = h.reasoning.take(120), meta = listOf("${h.confidencePercent}%", h.resultStatus), onClick = { onOpenDetail("hypothesis", h.id) })
        }
    }
}

// ── Tab 3: Data ──

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

// ── Tab 4: Reports ──

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

// ── Tracking Flow Cards ──

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
