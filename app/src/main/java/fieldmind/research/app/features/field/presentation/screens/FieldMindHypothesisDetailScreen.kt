package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  HYPOTHESIS DETAIL SCREEN
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HypothesisDetailScreen(
    hypothesisId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val hypotheses by viewModel.hypotheses.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val hypothesisEvidenceCrossRefs by viewModel.hypothesisEvidenceCrossRefs.collectAsState()
    val haptics = rememberFieldMindHaptics()

    val hypothesis = hypotheses.firstOrNull { it.id == hypothesisId }

    if (hypothesis == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Hypothesis, null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text("Hypothesis not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // ── Linked question ──
    val linkedQuestion = remember(questions, hypothesis.linkedQuestionId) {
        hypothesis.linkedQuestionId?.let { id -> questions.firstOrNull { it.id == id } }
    }

    // ── Linked observations (from hypothesisEvidenceCrossRefs) ──
    val linkedObservations = remember(observations, hypothesisEvidenceCrossRefs, hypothesis.id) {
        val obsIds = hypothesisEvidenceCrossRefs
            .filter { it.hypothesisId == hypothesis.id }
            .map { it.observationId }
            .toSet()
        observations.filter { it.id in obsIds }
    }

    // ── Result status colors ──
    val resultColor = when (hypothesis.resultStatus) {
        "Supported" -> FieldMindTheme.colors.positive
        "Refuted" -> MaterialTheme.colorScheme.error
        "Inconclusive" -> FieldMindTheme.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val resultIcon = when (hypothesis.resultStatus) {
        "Supported" -> MaterialSymbolIcon("check_circle")
        "Refuted" -> MaterialSymbolIcon("cancel")
        "Inconclusive" -> MaterialSymbolIcon("help")
        else -> MaterialSymbolIcon("help_outline")
    }

    // ── Animated confidence bar ──
    val animatedConfidence by animateFloatAsState(
        targetValue = hypothesis.confidencePercent / 100f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "hypothesisConfidence"
    )

    // ── Overflow menu ──
    var showOverflow by remember { mutableStateOf(false) }
    var showMarkTestedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ════════════════════════════════════════════════════════════
        //  Header: Back + title + overflow
        // ════════════════════════════════════════════════════════════
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(MaterialSymbolIcon("arrow_back"), "Back", size = 22.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Hypothesis",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Box {
                        IconButton(onClick = { showOverflow = true }, modifier = Modifier.size(40.dp)) {
                            Icon(MaterialSymbolIcon("more_vert"), "Menu", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit hypothesis") },
                                onClick = { showOverflow = false },
                                leadingIcon = { Icon(MaterialSymbolIcon("edit"), null, size = 18.dp) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as tested") },
                                onClick = {
                                    showOverflow = false
                                    showMarkTestedDialog = true
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("science"), null, size = 18.dp) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflow = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("delete"), null, size = 18.dp, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Prediction + Status badge
        // ════════════════════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Prediction ──
                Text(
                    hypothesis.prediction,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // ── Badge row: result status + confidence ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Result status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = resultColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                resultIcon,
                                null,
                                size = 12.dp,
                                tint = resultColor
                            )
                            Text(
                                hypothesis.resultStatus.ifBlank { "Untested" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = resultColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Confidence badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FieldMindTheme.colors.hypothesis.copy(alpha = 0.12f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(MaterialSymbolIcon("psychology"), null, size = 12.dp, tint = FieldMindTheme.colors.hypothesis)
                            Text(
                                "${hypothesis.confidencePercent}% confidence",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FieldMindTheme.colors.hypothesis,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Confidence Progress Bar
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("psychology"),
                title = "Confidence",
                iconTint = FieldMindTheme.colors.hypothesis
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confidence Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${hypothesis.confidencePercent}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = resultColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedConfidence },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = resultColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Linked Question
        // ════════════════════════════════════════════════════════════
        if (linkedQuestion != null) {
            item {
                SectionCard(
                    icon = FieldMindIcons.Question,
                    title = "Linked Question",
                    iconTint = FieldMindTheme.colors.question
                ) {
                    Surface(
                        onClick = { onOpenDetail("question", linkedQuestion.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    .background(FieldMindTheme.colors.question.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Question, null, tint = FieldMindTheme.colors.question, size = 16.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    linkedQuestion.questionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(linkedQuestion.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(linkedQuestion.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Reasoning
        // ════════════════════════════════════════════════════════════
        if (hypothesis.reasoning.isNotBlank()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("psychology"),
                    title = "Reasoning",
                    iconTint = FieldMindTheme.colors.hypothesis
                ) {
                    Text(
                        hypothesis.reasoning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Evidence Rules
        // ════════════════════════════════════════════════════════════
        if (hypothesis.evidenceNeeded.isNotBlank() || 
            hypothesis.supportCriteria.isNotBlank() || 
            hypothesis.weakeningCriteria.isNotBlank()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("science"),
                    title = "Evidence Rules",
                    iconTint = FieldMindTheme.colors.hypothesis
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (hypothesis.evidenceNeeded.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Evidence needed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    hypothesis.evidenceNeeded,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        if (hypothesis.supportCriteria.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FieldMindTheme.colors.positive.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Support criteria",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FieldMindTheme.colors.positive
                                    )
                                    Text(
                                        hypothesis.supportCriteria,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (hypothesis.weakeningCriteria.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Weakening criteria",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        hypothesis.weakeningCriteria,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Test Method
        // ════════════════════════════════════════════════════════════
        if (hypothesis.testMethod.isNotBlank()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("biotech"),
                    title = "Test Method",
                    iconTint = FieldMindTheme.colors.info
                ) {
                    Text(
                        hypothesis.testMethod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Linked Observations
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = FieldMindIcons.Observation,
                title = "Linked Observations",
                iconTint = FieldMindTheme.colors.observation,
                badge = if (linkedObservations.isNotEmpty()) "${linkedObservations.size}" else null
            ) {
                if (linkedObservations.isEmpty()) {
                    Text(
                        "Link observations from the field to support or refute this hypothesis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        linkedObservations.forEach { obs ->
                            Surface(
                                onClick = { onOpenDetail("observation", obs.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 16.dp)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            obs.subject.ifBlank { "Observation #${obs.id}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(obs.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(obs.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Activity Log
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("history"),
                title = "Activity Log",
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityLogRow(
                        icon = MaterialSymbolIcon("add_circle"),
                        text = "Created",
                        date = hypothesis.createdAt
                    )
                    if (hypothesis.updatedAt > hypothesis.createdAt) {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("edit"),
                            text = "Last edited",
                            date = hypothesis.updatedAt
                        )
                    }
                    if (hypothesis.resultStatus != "Unknown" && hypothesis.resultStatus.isNotBlank()) {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("science"),
                            text = "Tested — ${hypothesis.resultStatus}",
                            date = hypothesis.updatedAt
                        )
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Mark as Tested Dialog
    // ════════════════════════════════════════════════════════════════
    if (showMarkTestedDialog) {
        MarkTestedDialog(
            currentStatus = hypothesis.resultStatus,
            onDismiss = { showMarkTestedDialog = false },
            onConfirm = { newStatus ->
                haptics.confirm()
                viewModel.updateHypothesisEntity(hypothesis.copy(resultStatus = newStatus))
                showMarkTestedDialog = false
            }
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Delete Confirmation Dialog
    // ════════════════════════════════════════════════════════════════
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(MaterialSymbolIcon("delete"), null, size = 28.dp, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete hypothesis?") },
            text = {
                Text("This hypothesis will be archived and hidden from lists. You can restore it from the archive.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.warning()
                        viewModel.deleteHypothesis(hypothesis.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Mark as Tested Dialog
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MarkTestedDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    val conclusions = listOf("Supported", "Refuted", "Inconclusive")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(MaterialSymbolIcon("science"), null, size = 28.dp, tint = FieldMindTheme.colors.hypothesis) },
        title = { Text("Mark as tested") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Based on the evidence collected, what is your conclusion?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                conclusions.forEach { conclusion ->
                    val color = when (conclusion) {
                        "Supported" -> FieldMindTheme.colors.positive
                        "Refuted" -> MaterialTheme.colorScheme.error
                        else -> FieldMindTheme.colors.warning
                    }
                    val icon = when (conclusion) {
                        "Supported" -> MaterialSymbolIcon("check_circle")
                        "Refuted" -> MaterialSymbolIcon("cancel")
                        else -> MaterialSymbolIcon("help")
                    }
                    Surface(
                        onClick = { selectedStatus = conclusion },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedStatus == conclusion) color.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selectedStatus == conclusion) androidx.compose.foundation.BorderStroke(1.5.dp, color)
                                 else null,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedStatus == conclusion) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = if (selectedStatus == conclusion) color else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(conclusion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    color = if (selectedStatus == conclusion) color else MaterialTheme.colorScheme.onSurface)
                                Text(
                                    when (conclusion) {
                                        "Supported" -> "Evidence supports your prediction"
                                        "Refuted" -> "Evidence contradicts your prediction"
                                        else -> "Evidence is ambiguous or insufficient"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedStatus == conclusion) {
                                Icon(MaterialSymbolIcon("check_circle", filled = true), null, tint = color, size = 20.dp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus) },
                shape = RoundedCornerShape(14.dp),
                enabled = selectedStatus.isNotBlank()
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Reusable Components
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    icon: MaterialSymbolIcon,
    title: String,
    iconTint: Color = FieldMindTheme.colors.flashcard,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, size = 18.dp, tint = iconTint)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iconTint.copy(alpha = 0.12f)
                    ) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun ActivityLogRow(icon: MaterialSymbolIcon, text: String, date: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            formatTimestamp(date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════

private fun formatTimestamp(ts: Long): String {
    return try {
        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
        fmt.format(Date(ts))
    } catch (_: Exception) { "" }
}
