package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.ui.theme.RhythmColors

/**
 * Phase 9: Insights Dashboard Redesign
 */

/**
 * Research Health Issues - actionable insights
 */
data class ResearchHealthIssue(
    val id: String,
    val title: String,
    val severity: String,  // Critical, Warning, Info
    val suggestion: String,
    val affectedCount: Int
)

val sampleHealthIssues = listOf(
    ResearchHealthIssue("low_evidence", "Add more evidence", "Warning", "25 observations without photos or audio", 25),
    ResearchHealthIssue("unlinked", "Link questions to hypotheses", "Warning", "3 hypotheses not linked to observations", 3),
    ResearchHealthIssue("stale_data", "Data collection slowing", "Info", "No observations added this week", 0),
    ResearchHealthIssue("incomplete", "Complete metadata", "Warning", "42 observations missing location data", 42)
)

/**
 * Research Health Summary Card
 */
@Composable
fun ResearchHealthCard(
    issues: List<ResearchHealthIssue> = sampleHealthIssues,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠️", style = MaterialTheme.typography.headlineSmall)
                Text("Research Health", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${issues.size} issues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.height(24.dp)) {
                    Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (expanded && issues.isNotEmpty()) {
                Divider()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(issues) { issue ->
                        HealthIssueBadge(issue)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthIssueBadge(issue: ResearchHealthIssue) {
    val severityColor = when (issue.severity) {
        "Critical" -> MaterialTheme.colorScheme.error
        "Warning" -> RhythmColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(severityColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(severityColor, RoundedCornerShape(4.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(issue.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = severityColor)
            Text(issue.suggestion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (issue.affectedCount > 0) {
            Badge(
                containerColor = severityColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Text(issue.affectedCount.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Confidence Summary Card
 */
@Composable
fun ConfidenceSummaryCard(
    confidencePercent: Int,
    modifier: Modifier = Modifier
) {
    val confidenceLabel = when {
        confidencePercent >= 80 -> "Strong Evidence"
        confidencePercent >= 60 -> "Moderate Evidence"
        confidencePercent >= 40 -> "Developing"
        else -> "Weak Evidence"
    }
    val confidenceColor = when {
        confidencePercent >= 80 -> MaterialTheme.colorScheme.primary
        confidencePercent >= 60 -> FieldMindTheme.colors.info
        confidencePercent >= 40 -> FieldMindTheme.colors.warning
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = confidenceColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Research Confidence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = confidenceColor)
            Text("$confidencePercent%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = confidenceColor)
            LinearProgressIndicator(
                progress = { confidencePercent / 100f },
                color = confidenceColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Text(confidenceLabel, style = MaterialTheme.typography.labelSmall, color = confidenceColor)
        }
    }
}

/**
 * Insights Category Ranking
 */
@Composable
fun InsightsCategoryRanking(
    items: List<Pair<String, Int>>,  // category to count
    title: String = "Category Distribution",
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val maxCount = items.maxOfOrNull { it.second } ?: 1

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (category, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(0.3f)) {
                            Text(category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(0.5f)) {
                            LinearProgressIndicator(
                                progress = { count.toFloat() / maxCount },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }
                        Column(Modifier.weight(0.2f), horizontalAlignment = Alignment.End) {
                            Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Open Questions Summary
 */
@Composable
fun OpenQuestionsCard(
    questions: List<Triple<String, Int, String>>,  // question, obs_count, status
    modifier: Modifier = Modifier,
    onViewAll: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Open Questions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onViewAll, modifier = Modifier.height(24.dp)) {
                    Text("View all", style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                questions.take(3).forEach { (question, obsCount, status) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(question.take(40), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("$obsCount observations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Trend Indicator
 */
@Composable
fun TrendIndicator(
    label: String,
    value: Int,
    trend: Int,  // positive = up, negative = down, 0 = neutral
    modifier: Modifier = Modifier
) {
    val trendColor = when {
        trend > 0 -> MaterialTheme.colorScheme.primary
        trend < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val trendSymbol = when {
        trend > 0 -> "↑"
        trend < 0 -> "↓"
        else -> "−"
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(trendSymbol, style = MaterialTheme.typography.labelSmall, color = trendColor, fontWeight = FontWeight.Bold)
        }
    }
}
