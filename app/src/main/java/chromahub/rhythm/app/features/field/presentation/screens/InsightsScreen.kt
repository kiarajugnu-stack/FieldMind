package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

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
            item { InsightCard("Observations by category", FieldMindIcons.Data) { BarChart(categoryCounts, barColor = colors.observation) } }
        }
        if (trend.size >= 2) {
            item { InsightCard("Daily capture trend", FieldMindIcons.Trend) { LineChart(trend, lineColor = colors.positive) } }
        }
        if (confidenceParts.isNotEmpty()) {
            item { InsightCard("Confidence balance", FieldMindIcons.Check) { BreakdownBar(confidenceParts) } }
        }
        item { InsightCard("Field map", FieldMindIcons.Location) { MiniMap(mapPoints, pointColor = colors.observation) } }
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
