package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
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
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity

/**
 * Phase 6: Evidence Hub - Filter and Bulk Management
 */

/**
 * Evidence filter state for advanced filtering
 */
data class EvidenceFilterState(
    val evidenceType: String = "All",  // All, Photo, Audio, Video, Document
    val dateRange: String = "All time",  // All time, Today, This week, This month
    val tags: Set<String> = emptySet(),
    val locations: Set<String> = emptySet(),
    val projects: Set<String> = emptySet(),
    val minConfidence: String = "Any",  // Any, Certain, Likely, Unsure
    val completeness: String = "Any"  // Any, Complete, Needs review, Missing metadata
)

/**
 * Collapsible Evidence Filter Bar
 */
@Composable
fun AdvancedEvidenceFilterBar(
    filterState: EvidenceFilterState,
    onFilterChange: (EvidenceFilterState) -> Unit,
    availableTags: List<String>,
    availableLocations: List<String>,
    availableProjects: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Primary filter chips (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterState.evidenceType != "All",
                onClick = { expanded = !expanded },
                label = { Text(if (expanded) "Hide filters" else "Show filters") }
            )
            if (filterState.tags.isNotEmpty()) {
                AssistChip(
                    onClick = { onFilterChange(filterState.copy(tags = emptySet())) },
                    label = { Text("${filterState.tags.size} tags") },
                    trailingIcon = { Text("✕") }
                )
            }
        }

        // Advanced filters (collapsible)
        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Evidence type
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Evidence Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("All", "Photo", "Audio", "Video", "Document").forEach { type ->
                                FilterChip(
                                    selected = filterState.evidenceType == type,
                                    onClick = { onFilterChange(filterState.copy(evidenceType = type)) },
                                    label = { Text(type) }
                                )
                            }
                        }
                    }

                    Divider()

                    // Date range
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Date Range", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("All time", "Today", "This week", "This month").forEach { range ->
                                FilterChip(
                                    selected = filterState.dateRange == range,
                                    onClick = { onFilterChange(filterState.copy(dateRange = range)) },
                                    label = { Text(range) }
                                )
                            }
                        }
                    }

                    Divider()

                    // Confidence
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Confidence", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Any", "Certain", "Likely", "Unsure").forEach { conf ->
                                FilterChip(
                                    selected = filterState.minConfidence == conf,
                                    onClick = { onFilterChange(filterState.copy(minConfidence = conf)) },
                                    label = { Text(conf) }
                                )
                            }
                        }
                    }

                    Divider()

                    // Tags (multi-select)
                    if (availableTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tags", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(availableTags) { tag ->
                                    FilterChip(
                                        selected = tag in filterState.tags,
                                        onClick = {
                                            val updated = filterState.tags.toMutableSet()
                                            if (tag in updated) updated.remove(tag) else updated.add(tag)
                                            onFilterChange(filterState.copy(tags = updated))
                                        },
                                        label = { Text(tag) }
                                    )
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
 * Bulk Selection Mode Toolbar
 */
@Composable
fun BulkSelectionToolbar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onAddTag: () -> Unit,
    onLinkProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$selectedCount selected",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onSelectAll) { Text("All") }
                    TextButton(onClick = onDeselectAll) { Text("None") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onAddTag,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Tag", maxLines = 1) }
                OutlinedButton(
                    onClick = onLinkProject,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Link", maxLines = 1) }
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Archive", maxLines = 1) }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", maxLines = 1) }
            }
        }
    }
}

/**
 * Evidence Completeness Badge
 */
@Composable
fun CompletenessIndicator(
    observation: ObservationEntity,
    modifier: Modifier = Modifier
) {
    val hasEvidence = observation.evidenceSummary.isNotBlank()
    val hasLocation = observation.latitude != null && observation.longitude != null
    val hasWeather = observation.weatherTemperature != null
    val hasNotes = observation.factsOnlyNotes.isNotBlank()
    val hasContext = observation.moodOrContext.isNotBlank()

    val completeness = listOfNotNull(
        hasEvidence,
        hasLocation,
        hasWeather,
        hasNotes,
        hasContext
    ).size

    val completenessPercent = (completeness * 20)
    val status = when {
        completenessPercent >= 80 -> "Complete"
        completenessPercent >= 60 -> "Good"
        completenessPercent >= 40 -> "Fair"
        else -> "Incomplete"
    }
    val statusColor = when {
        completenessPercent >= 80 -> MaterialTheme.colorScheme.primary
        completenessPercent >= 60 -> MaterialTheme.colorScheme.info ?: MaterialTheme.colorScheme.primary
        completenessPercent >= 40 -> MaterialTheme.colorScheme.warning ?: MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { completenessPercent / 100f }, color = statusColor, trackColor = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier
            .fillMaxWidth()
            .height(4.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Evidence Card for display in grid
 */
@Composable
fun EvidenceGridCard(
    observation: ObservationEntity,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) {
            androidx.compose.foundation.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
        } else null
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = isSelected, onCheckedChange = { onSelect() }, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(observation.subject, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(observation.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CompletenessIndicator(observation)
        }
    }
}
