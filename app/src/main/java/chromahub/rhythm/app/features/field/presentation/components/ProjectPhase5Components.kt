package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.ui.theme.RhythmColors

/**
 * Project Types for Phase 5 classification
 */
enum class ProjectType(val displayName: String, val description: String) {
    OBSERVATION("Observation", "Record single observations"),
    INVESTIGATION("Investigation", "Systematic observation study"),
    SURVEY("Survey", "Wide-scale data collection"),
    EXPERIMENT("Experiment", "Test hypothesis with control"),
    MONITORING("Monitoring", "Long-term repeated observations")
}

/**
 * Research Methods for Method Builder
 */
data class ResearchMethod(
    val id: String,
    val name: String,
    val category: String,
    val description: String
)

val researchMethods = listOf(
    ResearchMethod("daily_obs", "Daily observations", "Observation", "Regular observation cycles"),
    ResearchMethod("weekly_obs", "Weekly observations", "Observation", "Once per week documentation"),
    ResearchMethod("photo_doc", "Photo documentation", "Evidence", "Visual record keeping"),
    ResearchMethod("audio_rec", "Audio recording", "Evidence", "Sound/audio capture"),
    ResearchMethod("video_doc", "Video documentation", "Evidence", "Video evidence collection"),
    ResearchMethod("measure_log", "Measurement logging", "Data", "Quantitative measurements"),
    ResearchMethod("species_count", "Species counting", "Data", "Enumerate species/items"),
    ResearchMethod("weather_log", "Weather logging", "Data", "Record weather conditions"),
    ResearchMethod("behavior_log", "Behavior logging", "Data", "Record observed behaviors"),
    ResearchMethod("comparison", "Comparison table", "Data", "Side-by-side data comparison")
)

/**
 * Project Type Badge - displays project type with color coding
 */
@Composable
fun ProjectTypeBadge(
    projectType: String,
    modifier: Modifier = Modifier
) {
    val type = ProjectType.values().firstOrNull { it.name == projectType.uppercase() } ?: ProjectType.OBSERVATION
    val backgroundColor = when (type) {
        ProjectType.OBSERVATION -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ProjectType.INVESTIGATION -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        ProjectType.SURVEY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        ProjectType.EXPERIMENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        ProjectType.MONITORING -> RhythmColors.warning.copy(alpha = 0.15f)
    }
    val textColor = when (type) {
        ProjectType.OBSERVATION -> MaterialTheme.colorScheme.primary
        ProjectType.INVESTIGATION -> MaterialTheme.colorScheme.secondary
        ProjectType.SURVEY -> MaterialTheme.colorScheme.tertiary
        ProjectType.EXPERIMENT -> MaterialTheme.colorScheme.error
        ProjectType.MONITORING -> RhythmColors.warning
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Project Type Selector - for creation/editing
 */
@Composable
fun ProjectTypeSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Project Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(ProjectType.values()) { type ->
                FilterChip(
                    selected = selected == type.name,
                    onClick = { onSelected(type.name) },
                    label = { Text(type.displayName) },
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

/**
 * Research Method Builder - multi-select for selected methods
 */
@Composable
fun ResearchMethodBuilder(
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategory by remember { mutableStateOf("") }
    
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Research Methods", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        
        val categories = researchMethods.map { it.category }.distinct()
        categories.forEach { category ->
            val methodsInCategory = researchMethods.filter { it.category == category }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedCategory = if (expandedCategory == category) "" else category }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (expandedCategory == category) "▼" else "▶",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (expandedCategory == category) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        methodsInCategory.forEach { method ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = selected.toMutableSet()
                                        if (method.id in selected) newSet.remove(method.id) else newSet.add(method.id)
                                        onSelectionChange(newSet)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = method.id in selected,
                                    onCheckedChange = {
                                        val newSet = selected.toMutableSet()
                                        if (it) newSet.add(method.id) else newSet.remove(method.id)
                                        onSelectionChange(newSet)
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(method.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    Text(method.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Project metrics card showing observation, evidence, data counts
 */
@Composable
fun ProjectMetricsCard(
    observationCount: Int,
    evidenceCount: Int,
    dataCount: Int,
    hypothesesCount: Int,
    reportsCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Project Assets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSmall("Obs", observationCount, Modifier.weight(1f))
                MetricSmall("Evidence", evidenceCount, Modifier.weight(1f))
                MetricSmall("Data", dataCount, Modifier.weight(1f))
                MetricSmall("Ideas", hypothesesCount, Modifier.weight(1f))
                MetricSmall("Reports", reportsCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricSmall(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Project question/research question display card
 */
@Composable
fun ProjectQuestionCard(
    question: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (question.isBlank()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("❓", style = MaterialTheme.typography.labelMedium)
                Text("Research Question", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

/**
 * Project status display
 */
@Composable
fun ProjectStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when {
        status.equals("Active", ignoreCase = true) -> 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
        status.equals("Paused", ignoreCase = true) -> 
            RhythmColors.warning.copy(alpha = 0.15f) to RhythmColors.warning
        status.equals("Completed", ignoreCase = true) -> 
            RhythmColors.success.copy(alpha = 0.15f) to RhythmColors.success
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
