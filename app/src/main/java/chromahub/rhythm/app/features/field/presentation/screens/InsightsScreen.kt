package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

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
    val tags by viewModel.commonTags.collectAsState()
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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { FieldScreenHeader("Insights", "Offline analysis of your own archive.", icon = FieldMindIcons.Insights, actionIcon = FieldMindIcons.Search, onAction = { onNavigate(FieldMindScreen.Search) }) }
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
        item { InsightCard("Field map", FieldMindIcons.Location) { MiniMap(mapPoints, pointColor = colors.observation) } }
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
private fun InsightCard(title: String, icon: chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon, content: @Composable () -> Unit) {
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
