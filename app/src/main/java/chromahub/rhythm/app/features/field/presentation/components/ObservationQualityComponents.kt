package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.shared.presentation.components.icons.Icon

/**
 * Calculates observation quality score based on completeness.
 * Returns 0-100 integer representing quality percentage.
 */
fun calculateObservationQuality(
    hasSubject: Boolean,
    hasEvidence: Int,  // number of evidence items
    hasLocation: Boolean,
    hasWeather: Boolean,
    hasMeasurements: Boolean,
    hasNotes: Boolean,
    hasDuration: Boolean,
    hasConfidence: Boolean
): Int {
    var score = 0
    var maxPoints = 0

    // Subject (25 points)
    if (hasSubject) score += 25 else maxPoints += 25
    
    // Evidence (25 points)
    if (hasEvidence > 0) score += 25 else maxPoints += 25
    
    // Location (15 points)
    if (hasLocation) score += 15 else maxPoints += 15
    
    // Weather (10 points)
    if (hasWeather) score += 10 else maxPoints += 10
    
    // Measurements (10 points)
    if (hasMeasurements) score += 10 else maxPoints += 10
    
    // Notes (5 points)
    if (hasNotes) score += 5 else maxPoints += 5
    
    // Duration (5 points)
    if (hasDuration) score += 5 else maxPoints += 5
    
    // Confidence (5 points) - already included in form
    if (hasConfidence) score += 5 else maxPoints += 5
    
    return (score.toFloat() / 100 * 100).toInt().coerceIn(0, 100)
}

@Composable
fun QualityScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val animatedScore by animateFloatAsState(score.toFloat(), label = "score")
    val qualityLabel = when {
        score >= 80 -> "Excellent"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        else -> "Needs work"
    }
    val qualityColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF2196F3)
        score >= 40 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Observation Quality", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${animatedScore.toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = qualityColor)
            }
            LinearProgressIndicator(progress = { animatedScore / 100 }, color = qualityColor, trackColor = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(4.dp))
            Text(qualityLabel, style = MaterialTheme.typography.labelSmall, color = qualityColor)
        }
    }
}

@Composable
fun MissingFieldsChecklist(
    hasSubject: Boolean,
    hasEvidence: Boolean,
    hasLocation: Boolean,
    hasWeather: Boolean,
    hasMeasurements: Boolean,
    hasNotes: Boolean,
    hasDuration: Boolean,
    hasConfidence: Boolean,
    modifier: Modifier = Modifier
) {
    val fields = listOf(
        "Subject" to hasSubject,
        "Evidence" to hasEvidence,
        "Location" to hasLocation,
        "Weather" to hasWeather,
        "Measurements" to hasMeasurements,
        "Notes" to hasNotes,
        "Duration" to hasDuration,
        "Confidence" to hasConfidence
    )
    
    val missing = fields.filter { !it.second }

    if (missing.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("To improve quality, add:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(missing) { (field, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                        Text(field, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeciesConfidenceSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Certain", "Likely", "Unsure")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Species Confidence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DistanceSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("2m", "10m", "50m", "100m+")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Distance from Observer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ObservationChecklistPicker(
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Seen", "Heard", "Smelled", "Touched", "Measured")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Observation Methods", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().wrapContentHeight(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = {
                        val newSet = selected.toMutableSet()
                        if (option in selected) newSet.remove(option) else newSet.add(option)
                        onSelectionChange(newSet)
                    },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MeasurementsInputSection(
    measurements: Map<String, String>,
    onMeasurementChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val measurementFields = listOf(
        "Height" to "cm",
        "Width" to "cm",
        "Length" to "cm",
        "Diameter" to "cm",
        "Weight" to "g"
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Measurements", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        measurementFields.forEach { (field, unit) ->
            OutlinedTextField(
                value = measurements[field] ?: "",
                onValueChange = { onMeasurementChange(field, it) },
                label = { Text("$field ($unit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun FollowUpScheduler(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("None", "Tomorrow", "3 days", "1 week", "Custom")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Schedule Follow-up", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = selected == option,
                        onClick = { onSelected(option) }
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
