package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val dataRecords by viewModel.dataRecords.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val tags by viewModel.commonTags.collectAsState()
    val profileName by viewModel.fieldSettings.profileName.collectAsState()
    val profileRole by viewModel.fieldSettings.profileRole.collectAsState()
    val profileFocus by viewModel.fieldSettings.profileFocus.collectAsState()
    val localModel by viewModel.fieldSettings.localModelOption.collectAsState()
    val localReady by viewModel.fieldSettings.localModelDownloaded.collectAsState()
    val backupInterval by viewModel.fieldSettings.autoBackupInterval.collectAsState()
    val colors = FieldMindTheme.colors

    val categoryCounts = observations.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }.take(6).map { it.key to it.value.toFloat() }
    val trend = observations.groupingBy { it.date }.eachCount().toSortedMap().values.toList().takeLast(10).map { it.toFloat() }
    val confidenceParts = listOf(
        Triple("Sure", observations.count { it.confidenceLevel == "Sure" }.toFloat(), colors.confidenceSure),
        Triple("Guess", observations.count { it.confidenceLevel == "Guess" }.toFloat(), colors.confidenceGuess),
        Triple("Verify", observations.count { it.confidenceLevel == "Needs Verification" }.toFloat(), colors.confidenceVerify)
    ).filter { it.second > 0f }
    val mapPoints = observations.mapNotNull { o -> o.latitude?.let { lat -> o.longitude?.let { lon -> lat to lon } } }

    val graphNodes = remember(observations, questions, hypotheses, projects, sources) {
        val nodes = mutableListOf<GraphNode>()
        val index = HashMap<String, Int>()
        val edges = mutableListOf<Pair<Int, Int>>()
        fun add(key: String, label: String, color: androidx.compose.ui.graphics.Color, emphasis: Boolean): Int =
            index.getOrPut(key) { nodes.add(GraphNode(label, color, emphasis)); nodes.size - 1 }
        projects.take(8).forEach { p -> add("project:${p.id}", p.title, colors.project, true) }
        questions.filter { it.relatedProjectId != null }.take(12).forEach { q ->
            index["project:${q.relatedProjectId}"]?.let { p -> edges.add(add("question:${q.id}", q.questionText, colors.question, false) to p) }
        }
        observations.filter { it.projectId != null }.take(16).forEach { o ->
            index["project:${o.projectId}"]?.let { p -> edges.add(add("obs:${o.id}", o.subject, colors.observation, false) to p) }
        }
        sources.filter { it.relatedProjectId != null }.take(12).forEach { s ->
            index["project:${s.relatedProjectId}"]?.let { p -> edges.add(add("source:${s.id}", s.title, colors.source, false) to p) }
        }
        hypotheses.filter { it.linkedQuestionId != null }.take(12).forEach { h ->
            index["question:${h.linkedQuestionId}"]?.let { q -> edges.add(add("hyp:${h.id}", h.prediction, colors.hypothesis, false) to q) }
        }
        nodes.toList() to edges.toList()
    }
    val achievements = remember(observations, questions, projects, sources, reports, notes, dataRecords, flashcards) {
        listOf(
            Achievement("First Observation", "Save one facts-only field observation.", FieldMindIcons.Observation, colors.observation, observations.size, 1),
            Achievement("7-Day Observer", "Log observations across seven different dates.", FieldMindIcons.Streak, colors.warning, observations.map { it.date }.distinct().size, 7),
            Achievement("Evidence Collector", "Attach or summarize evidence in 10 observations.", FieldMindIcons.Camera, colors.observation, observations.count { it.evidenceSummary.isNotBlank() }, 10),
            Achievement("Source Curator", "Save five sources with citation metadata.", FieldMindIcons.Source, colors.source, sources.size, 5),
            Achievement("Question Builder", "Write five researchable questions.", FieldMindIcons.Question, colors.question, questions.size, 5),
            Achievement("Project Starter", "Create your first research project.", FieldMindIcons.Project, colors.project, projects.size, 1),
            Achievement("Data Logger", "Record ten data entries.", FieldMindIcons.Data, colors.data, dataRecords.size, 10),
            Achievement("Report Writer", "Draft a research report.", FieldMindIcons.Report, colors.report, reports.size, 1),
            Achievement("Note Maker", "Save ten free-form notes.", FieldMindIcons.Note, colors.source, notes.size, 10),
            Achievement("Review Builder", "Create five flashcards from sources or findings.", FieldMindIcons.Flashcard, colors.flashcard, flashcards.size, 5)
        )
    }
    val context = LocalContext.current
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unlockedTitles = achievements.filter { it.unlocked }.joinToString("|") { it.title }
    LaunchedEffect(unlockedTitles) {
        if (unlockedTitles.isNotBlank()) {
            val prefs = context.getSharedPreferences("fieldmind_achievements", 0)
            achievements.filter { it.unlocked && !prefs.getBoolean(it.title, false) }.forEach { achievement ->
                scope.launch { snackbarState.showSnackbar("🎉 ${achievement.title} unlocked!") }
                prefs.edit().putBoolean(achievement.title, true).apply()
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarState) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { FieldScreenHeader("Insights", "Offline analysis of your own archive.", icon = FieldMindIcons.Insights, actionIcon = FieldMindIcons.Search, onAction = { onNavigate(FieldMindScreen.Search) }) }
        item { ResearchProfileInsightCard(profileName, profileRole, profileFocus, localModel, localReady, backupInterval) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Observations", observations.size.toString(), FieldMindIcons.Observation, Modifier.weight(1f), colors.observation)
                MetricTile("Hypotheses", hypotheses.size.toString(), FieldMindIcons.Hypothesis, Modifier.weight(1f), colors.hypothesis)
                MetricTile("Reports", reports.size.toString(), FieldMindIcons.Report, Modifier.weight(1f), colors.report)
            }
        }
        if (observations.isEmpty()) {
            item { EmptyState("No data yet", "Insights, charts, and your offline map appear as you log observations.", icon = FieldMindIcons.Insights, actionLabel = "Capture one") { onNavigate(FieldMindScreen.Observe) } }
        }
        if (categoryCounts.isNotEmpty()) {
            item { InsightCard("Observations by category", FieldMindIcons.Data) { BarChart(categoryCounts, barColors = categoryCounts.map { colors.categoryColor(it.first) }) } }
        }
        if (trend.size >= 2) {
            item { InsightCard("Daily capture trend", FieldMindIcons.Trend) { LineChart(trend, lineColor = colors.positive) } }
        }
        if (confidenceParts.isNotEmpty()) {
            item { InsightCard("Confidence balance", FieldMindIcons.Check) { BreakdownBar(confidenceParts) } }
        }
        item {
            InsightCard("Field map", FieldMindIcons.Location) {
                OsmMap(points = mapPoints, markerColor = colors.observation)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onNavigate(FieldMindScreen.MapScreen) }) {
                        Text("Open full map")
                        Spacer(Modifier.size(4.dp))
                        Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 18.dp)
                    }
                }
            }
        }
        item {
            InsightCard("Knowledge graph", FieldMindIcons.Graph) {
                ConnectionGraph(graphNodes.first, graphNodes.second)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphLegend("Projects", colors.project)
                    GraphLegend("Questions", colors.question)
                    GraphLegend("Observations", colors.observation)
                    GraphLegend("Sources", colors.source)
                    GraphLegend("Hypotheses", colors.hypothesis)
                }
            }
        }
        item { SectionHeader("Top tags", if (tags.isEmpty()) "Tags appear as you capture" else null) }
        if (tags.isEmpty()) {
            item { EmptyState("No tags yet", "Add comma-separated tags when capturing to surface repeated subjects.", icon = FieldMindIcons.Tag) }
        } else {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tags.take(8).forEach { tag ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TagChip(tag.name)
                                Text("${tag.observationCount}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Open questions", "${questions.count { it.status != "Answered" }} unanswered") }
        items(questions.filter { it.status != "Answered" }.take(5)) { q ->
            EntityCard(q.questionText, "question", meta = listOf(q.status, q.priority)) { onOpenDetail("question", q.id) }
        }
        item { SectionHeader("Active projects", "${projects.count { it.status == "Active" }} active • ${sources.size} sources") }
        items(projects.take(4)) { p ->
            EntityCard(p.title, "project", body = p.objective.ifBlank { p.researchQuestion }, meta = listOf(p.status)) { onOpenDetail("project", p.id) }
        }
        item { CollapsibleAchievements(achievements) }
    }
    }
}


@Composable
private fun ResearchProfileInsightCard(name: String, role: String, focus: String, localModel: String, localReady: Boolean, backupInterval: String) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 28.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (name.isBlank()) "Your research profile" else name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("$role • $focus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                Text("Offline setup: ${if (localReady) localModel else "local model not downloaded"} • Backup: $backupInterval", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
            }
        }
    }
}

private data class Achievement(
    val title: String,
    val description: String,
    val icon: fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon,
    val accent: androidx.compose.ui.graphics.Color,
    val progress: Int,
    val target: Int
) {
    val unlocked: Boolean get() = progress >= target
    val fraction: Float get() = (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

@Composable
private fun CollapsibleAchievements(items: List<Achievement>) {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.clickable { expanded = !expanded }.animateContentSize()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Streak, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${items.count { it.unlocked }} unlocked • tap to ${if (expanded) "collapse" else "expand"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
                    items.forEach { achievement -> AchievementCard(achievement, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(item: Achievement, modifier: Modifier = Modifier) {
    Card(modifier = modifier.animateContentSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(item.accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = item.accent, size = 21.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (item.unlocked) "Unlocked" else "${item.progress}/${item.target}", style = MaterialTheme.typography.labelMedium, color = item.accent, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = item.fraction, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)), color = item.accent, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

@Composable
private fun GraphLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightCard(title: String, icon: fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
