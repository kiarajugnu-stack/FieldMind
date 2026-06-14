package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.HypothesisEntity
import fieldmind.research.app.ui.theme.RhythmColors

/**
 * Phase 8: Hypotheses Redesign
 */

enum class HypothesisStatus(val displayName: String, val color: Color?) {
    SUPPORTED("Supported", null),
    CONTRADICTED("Contradicted", null),
    INCONCLUSIVE("Inconclusive", null),
    UNTESTED("Untested", null)
}

/**
 * Hypothesis Card - displays prediction with confidence, status, evidence count
 */
@Composable
fun HypothesisCard(
    hypothesis: HypothesisEntity,
    evidenceCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusEnum = HypothesisStatus.values().firstOrNull { it.displayName == hypothesis.status } ?: HypothesisStatus.UNTESTED
    val statusColor = when (statusEnum) {
        HypothesisStatus.SUPPORTED -> MaterialTheme.colorScheme.primary
        HypothesisStatus.CONTRADICTED -> MaterialTheme.colorScheme.error
        HypothesisStatus.INCONCLUSIVE -> RhythmColors.warning
        HypothesisStatus.UNTESTED -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != {}) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("If ${hypothesis.prediction}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(hypothesis.reasoning.ifBlank { "No reasoning provided" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Confidence bar
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Confidence", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { hypothesis.confidence / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Text("${hypothesis.confidence}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusEnum.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Evidence count
            if (evidenceCount > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text("$evidenceCount evidence items") }
                )
            }
        }
    }
}

/**
 * Hypothesis Status Badge
 */
@Composable
fun HypothesisStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val statusEnum = HypothesisStatus.values().firstOrNull { it.displayName == status } ?: HypothesisStatus.UNTESTED
    val backgroundColor = when (statusEnum) {
        HypothesisStatus.SUPPORTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        HypothesisStatus.CONTRADICTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        HypothesisStatus.INCONCLUSIVE -> (MaterialTheme.colorScheme.warning ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
        HypothesisStatus.UNTESTED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (statusEnum) {
        HypothesisStatus.SUPPORTED -> MaterialTheme.colorScheme.primary
        HypothesisStatus.CONTRADICTED -> MaterialTheme.colorScheme.error
        HypothesisStatus.INCONCLUSIVE -> MaterialTheme.colorScheme.warning ?: MaterialTheme.colorScheme.primary
        HypothesisStatus.UNTESTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = statusEnum.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Hypothesis Update Form
 */
@Composable
fun HypothesisUpdateForm(
    currentStatus: String,
    currentConfidence: Int,
    currentSupport: String,
    onStatusChange: (String) -> Unit,
    onConfidenceChange: (Int) -> Unit,
    onSupportChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Status selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                HypothesisStatus.values().forEach { status ->
                    FilterChip(
                        selected = currentStatus == status.displayName,
                        onClick = { onStatusChange(status.displayName) },
                        label = { Text(status.displayName) }
                    )
                }
            }
        }

        Divider()

        // Confidence slider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confidence: $currentConfidence%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = currentConfidence.toFloat(),
                onValueChange = { onConfidenceChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider()

        // Evidence for/against
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Evidence needed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = currentSupport,
                onValueChange = onSupportChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                label = { Text("What evidence would support or refute this?") }
            )
        }
    }
}
