package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.shared.presentation.components.icons.Icon

// ──────────────────────────────────────────────────────────────────────
//  Evidence Hub Phase 6: Integrated into ProjectsScreen EvidenceTab
// ──────────────────────────────────────────────────────────────────────
//
//  This file contains supporting components for the Evidence Hub experience.
//  The main EvidenceTab implementation is in FieldMindProjectsScreen.kt
//  These are utility components used within the Evidence Hub workflow.

/**
 * EvidenceFilterState - Tracks active filters for evidence display
 */
data class EvidenceFilterState(
    val typeFilter: String = "All",
    val categoryFilter: String = "All",
    val dateRange: String = "Any time",
    val confidenceMin: Int = 0,
    val tags: Set<String> = emptySet(),
    val locationFilter: String = "",
    val hasMetadata: Boolean? = null,
    val missingEvidenceFilter: String = "None", // None, NoPhotos, NoGPS, NoWeather, NoFacts
    val viewMode: String = "list" // list, grid
)

/**
 * CompletenessIndicator - Shows how complete an observation's metadata is
 */
@Composable
fun CompletenessIndicator(observation: ObservationEntity) {
    val checks = listOf(
        "Subject" to observation.subject.isNotBlank(),
        "Facts" to observation.factsOnlyNotes.isNotBlank(),
        "Category" to observation.category.isNotBlank(),
        "Confidence" to observation.confidenceLevel.isNotBlank(),
        "Location" to observation.manualLocation.isNotBlank(),
        "GPS" to (observation.latitude != null && observation.longitude != null),
        "Weather" to observation.weatherCondition.isNotBlank(),
        "Time" to (observation.startedAt != null && observation.endedAt != null)
    )
    
    val completedCount = checks.count { it.second }
    val completenessPercent = (completedCount * 100) / checks.size

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Data completeness", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text("$completenessPercent%", style = MaterialTheme.typography.labelSmall, 
                    color = if (completenessPercent >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            LinearProgressIndicator(
                progress = { completenessPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(checks) { (label, completed) ->
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(
                                if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                            .padding(6.dp, 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * BulkSelectionToolbar - Appears when items are selected
 */
@Composable
fun BulkSelectionToolbar(
    selectedCount: Int,
    onTag: () -> Unit,
    onLink: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    AnimatedVisibility(visible = selectedCount > 0, enter = slideInVertically(), exit = slideOutVertically()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("$selectedCount selected", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    IconButton(onClick = onCancel) {
                        Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Triple("Add tags", onTag, FieldMindIcons.Tag),
                        Triple("Link", onLink, FieldMindIcons.Link),
                        Triple("Archive", onArchive, FieldMindIcons.Archive),
                        Triple("Delete", onDelete, FieldMindIcons.Delete)
                    ).forEach { (label, action, icon) ->
                        FilledTonalButton(
                            onClick = action,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(icon, null, size = 16.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * MissingEvidenceFilter - Filter chips for finding observations missing key data
 */
@Composable
fun MissingEvidenceFilter(
    current: String,
    onChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        "None" to "All",
        "NoPhotos" to "No photos",
        "NoGPS" to "No GPS",
        "NoWeather" to "No weather",
        "NoFacts" to "No facts"
    )
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { (value, label) ->
            FilterChip(
                selected = current == value,
                onClick = { onChanged(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (current == value) ({ Icon(FieldMindIcons.Filter, null, size = 14.dp) }) else null
            )
        }
    }
}

/**
 * ViewModeToggle - Switch between list and grid/gallery mode
 */
@Composable
fun ViewModeToggle(
    currentMode: String,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = { onToggle("list") }, modifier = Modifier.size(36.dp)) {
            Icon(
                icon = if (currentMode == "list") FieldMindIcons.List.copy(filled = true) else FieldMindIcons.List,
                contentDescription = "List view",
                tint = if (currentMode == "list") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
        }
        IconButton(onClick = { onToggle("grid") }, modifier = Modifier.size(36.dp)) {
            Icon(
                icon = if (currentMode == "grid") FieldMindIcons.Gallery.copy(filled = true) else FieldMindIcons.Gallery,
                contentDescription = "Grid view",
                tint = if (currentMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
        }
    }
}

/**
 * Applies the missing-evidence filter to a list of observations
 */
fun applyMissingEvidenceFilter(observations: List<ObservationEntity>, filter: String): List<ObservationEntity> {
    return when (filter) {
        "NoPhotos" -> observations.filter { obs ->
            obs.evidenceSummary.isBlank() || (!obs.evidenceSummary.contains("photo", true) && !obs.evidenceSummary.contains("image", true) && !obs.evidenceSummary.contains("gallery", true))
        }
        "NoGPS" -> observations.filter { obs -> obs.latitude == null || obs.longitude == null }
        "NoWeather" -> observations.filter { obs -> obs.weatherTemperature == null && obs.weatherCondition.isBlank() }
        "NoFacts" -> observations.filter { obs -> obs.factsOnlyNotes.isBlank() }
        else -> observations
    }
}

/**
 * EvidenceGridCard - Grid view card for evidence with selection
 */
@Composable
fun EvidenceGridCard(
    observation: ObservationEntity,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        ) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        observation.subject,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        observation.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        observation.confidenceLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
