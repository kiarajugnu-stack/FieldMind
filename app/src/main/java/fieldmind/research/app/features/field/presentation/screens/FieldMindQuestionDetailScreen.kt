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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  QUESTION DETAIL SCREEN
// ══════════════════════════════════════════════════════════════════════

@Composable
fun QuestionDetailScreen(
    questionId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val haptics = rememberFieldMindHaptics()

    val question = questions.firstOrNull { it.id == questionId }

    if (question == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Question, null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text("Question not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // ── Linked hypotheses ──
    val linkedHypotheses = remember(hypotheses, question.id) {
        hypotheses.filter { it.linkedQuestionId == question.id }
    }

    // ── Linked observations (from relatedObservationIds) ──
    val linkedObservations = remember(observations, question.relatedObservationIds) {
        if (question.relatedObservationIds.isNotBlank()) {
            val ids = question.relatedObservationIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            observations.filter { it.id in ids }
        } else emptyList()
    }

    // ── Linked sources (from relatedSourceIds) ──
    val linkedSources = remember(sources, question.relatedSourceIds) {
        if (question.relatedSourceIds.isNotBlank()) {
            val ids = question.relatedSourceIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            sources.filter { it.id in ids }
        } else emptyList()
    }

    // ── Local notes state (initialized from persisted data, auto-saved with debounce) ──
    var notesText by remember(question.notes) { mutableStateOf(question.notes) }
    LaunchedEffect(notesText) {
        if (notesText != question.notes) {
            kotlinx.coroutines.delay(1500)
            viewModel.updateQuestionEntity(question.copy(notes = notesText))
        }
    }

    // ── Answer state ──
    var showAnswerEditor by remember { mutableStateOf(false) }
    var answerText by remember(question.answer) { mutableStateOf(question.answer) }
    var confidenceLevel by remember(question.confidence) { mutableIntStateOf(question.confidence) }

    // ── Priority colors ──
    val priorityColor = when (question.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> FieldMindTheme.colors.warning
        else -> FieldMindTheme.colors.positive
    }

    // ── Status colors ──
    val statusColor = when (question.status) {
        "Answered" -> FieldMindTheme.colors.positive
        "Open" -> FieldMindTheme.colors.flashcard
        "Investigating" -> FieldMindTheme.colors.data
        "Archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    // ── Picker dialogs state ──
    var showObservationPicker by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var obsPickerSearch by remember { mutableStateOf("") }
    var srcPickerSearch by remember { mutableStateOf("") }

    // ── Overflow menu ──
    var showOverflow by remember { mutableStateOf(false) }

    // ── Animated confidence bar ──
    val animatedConfidence by animateFloatAsState(
        targetValue = confidenceLevel / 100f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "confidence"
    )

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
                        "Question",
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
                                text = { Text("Edit question") },
                                onClick = { showOverflow = false },
                                leadingIcon = { Icon(MaterialSymbolIcon("edit"), null, size = 18.dp) }
                            )
                            DropdownMenuItem(
                                text = { Text("Create hypothesis") },
                                onClick = { showOverflow = false },
                                leadingIcon = { Icon(MaterialSymbolIcon("psychology"), null, size = 18.dp) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (question.status == "Answered") "Reopen" else "Mark answered") },
                                onClick = {
                                    showOverflow = false
                                    haptics.confirm()
                                    viewModel.updateQuestionEntity(question.copy(
                                        status = if (question.status == "Answered") "Open" else "Answered",
                                        answeredAt = if (question.status == "Answered") null else System.currentTimeMillis()
                                    ))
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("check_circle"), null, size = 18.dp) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Archive", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflow = false
                                    haptics.confirm()
                                    viewModel.updateQuestionEntity(question.copy(archivedAt = System.currentTimeMillis()))
                                    onBack()
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("archive"), null, size = 18.dp, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Question text + Status + Priority badges
        // ════════════════════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Question text ──
                Text(
                    question.questionText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // ── Badge row: status + priority + category ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                when (question.status) {
                                    "Answered" -> MaterialSymbolIcon("check_circle")
                                    "Open" -> MaterialSymbolIcon("help")
                                    "Investigating" -> MaterialSymbolIcon("search")
                                    else -> MaterialSymbolIcon("help_outline")
                                },
                                null,
                                size = 12.dp,
                                tint = statusColor
                            )
                            Text(
                                question.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Priority badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${question.priority} Priority",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp
                        )
                    }

                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            question.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Hypotheses section
        // ════════════════════════════════════════════════════════════
        if (linkedHypotheses.isNotEmpty()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("psychology"),
                    title = "Hypotheses",
                    iconTint = FieldMindTheme.colors.hypothesis,
                    badge = "${linkedHypotheses.size}"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        linkedHypotheses.forEach { h ->
                            HypothesisMiniCard(
                                hypothesis = h,
                                onClick = { onOpenDetail("hypothesis", h.id) }
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Evidence & Linked Observations (observations linked to this question)
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("science"),
                title = "Evidence & Observations",
                iconTint = FieldMindTheme.colors.observation,
                badge = if (linkedObservations.isNotEmpty()) "${linkedObservations.size}" else null
            ) {
                if (linkedObservations.isEmpty()) {
                    Text(
                        "Link observations from the field to provide evidence for your hypotheses.",
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
                                    // Unlink button
                                    IconButton(
                                        onClick = { viewModel.unlinkQuestionObservation(question.id, obs.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(MaterialSymbolIcon("link_off"), "Unlink", size = 14.dp, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                    Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
                // Link button
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = { showObservationPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(MaterialSymbolIcon("add_link"), null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Link observation")
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Linked Sources
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("menu_book"),
                title = "Linked Sources",
                iconTint = FieldMindTheme.colors.source
            ) {
                if (linkedSources.isEmpty()) {
                    Text(
                        "No sources linked to this question.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        linkedSources.forEach { src ->
                            Surface(
                                onClick = { onOpenDetail("source", src.id) },
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
                                            .background(FieldMindTheme.colors.source.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(FieldMindIcons.Category, null, tint = FieldMindTheme.colors.source, size = 16.dp)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            src.title.ifBlank { "Source #${src.id}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${src.type} • ${src.readingStatus}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Unlink button
                                    IconButton(
                                        onClick = { viewModel.unlinkQuestionSource(question.id, src.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(MaterialSymbolIcon("link_off"), "Unlink", size = 14.dp, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                    Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
                // Link button
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = { showSourcePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(MaterialSymbolIcon("add_link"), null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Link source")
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Notes
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("edit_note"),
                title = "Notes",
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Add notes about this question...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Answer section
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("lightbulb"),
                title = "Answer",
                iconTint = FieldMindTheme.colors.positive
            ) {
                if (question.answer.isNotBlank() && !showAnswerEditor) {
                    // Display answer
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            question.answer,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // ── Confidence badge ──
                        val displayConfidence = question.confidence
                        val confColor = when {
                            displayConfidence >= 80 -> FieldMindTheme.colors.positive
                            displayConfidence >= 50 -> FieldMindTheme.colors.warning
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = confColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(MaterialSymbolIcon("psychology"), null, size = 12.dp, tint = confColor)
                                    Text(
                                        "${displayConfidence}% confidence",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = confColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (question.answeredAt != null) {
                                Text(
                                    "Answered ${formatTimestamp(question.answeredAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Edit button
                        OutlinedButton(
                            onClick = {
                                answerText = question.answer
                                confidenceLevel = question.confidence
                                showAnswerEditor = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(MaterialSymbolIcon("edit"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Edit answer")
                        }
                    }
                } else {
                    // Answer editor
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = { answerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "What is the answer to this question?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        )

                        // ── Confidence Level ──
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Confidence Level",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${confidenceLevel}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        confidenceLevel >= 80 -> FieldMindTheme.colors.positive
                                        confidenceLevel >= 50 -> FieldMindTheme.colors.warning
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            Slider(
                                value = confidenceLevel.toFloat(),
                                onValueChange = { confidenceLevel = it.toInt() },
                                valueRange = 0f..100f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = when {
                                        confidenceLevel >= 80 -> FieldMindTheme.colors.positive
                                        confidenceLevel >= 50 -> FieldMindTheme.colors.warning
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    activeTrackColor = when {
                                        confidenceLevel >= 80 -> FieldMindTheme.colors.positive
                                        confidenceLevel >= 50 -> FieldMindTheme.colors.warning
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            )
                        }

                        // Confidence visual indicator
                        LinearProgressIndicator(
                            progress = { animatedConfidence },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = when {
                                confidenceLevel >= 80 -> FieldMindTheme.colors.positive
                                confidenceLevel >= 50 -> FieldMindTheme.colors.warning
                                else -> MaterialTheme.colorScheme.error
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )

                        // Save button
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (question.answer.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        showAnswerEditor = false
                                        answerText = question.answer
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Cancel")
                                }
                            }
                            Button(
                                onClick = {
                                    haptics.confirm()
                                    viewModel.setQuestionAnswer(question, answerText, confidenceLevel)
                                    showAnswerEditor = false
                                },
                                modifier = if (question.answer.isNotBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                enabled = answerText.isNotBlank()
                            ) {
                                Icon(MaterialSymbolIcon("save"), null, size = 16.dp)
                                Spacer(Modifier.size(6.dp))
                                Text("Save answer")
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
                        date = question.createdAt
                    )
                    if (question.updatedAt > question.createdAt) {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("edit"),
                            text = "Last edited",
                            date = question.updatedAt
                        )
                    }
                    if (question.answeredAt != null) {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("check_circle"),
                            text = "Answered",
                            date = question.answeredAt
                        )
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Observation Picker Dialog
    // ════════════════════════════════════════════════════════════════
    if (showObservationPicker) {
        EntityPickerDialog(
            title = "Link Observation",
            searchQuery = obsPickerSearch,
            onSearchChange = { obsPickerSearch = it },
            onDismiss = {
                showObservationPicker = false
                obsPickerSearch = ""
            },
            items = observations.filter { obs ->
                obs.deletedAt == null && obs.id !in linkedObservations.map { it.id } &&
                (obsPickerSearch.isBlank() ||
                 obs.subject.contains(obsPickerSearch, ignoreCase = true) ||
                 obs.category.contains(obsPickerSearch, ignoreCase = true))
            },
            itemIcon = { Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 16.dp) },
            itemPrimaryText = { it.subject.ifBlank { "Observation #${it.id}" } },
            itemSecondaryText = { "${it.category} • ${it.date}" },
            onSelect = { obs ->
                haptics.confirm()
                viewModel.linkQuestionObservation(question.id, obs.id)
                showObservationPicker = false
                obsPickerSearch = ""
            }
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Source Picker Dialog
    // ════════════════════════════════════════════════════════════════
    if (showSourcePicker) {
        EntityPickerDialog(
            title = "Link Source",
            searchQuery = srcPickerSearch,
            onSearchChange = { srcPickerSearch = it },
            onDismiss = {
                showSourcePicker = false
                srcPickerSearch = ""
            },
            items = sources.filter { src ->
                src.deletedAt == null && src.id !in linkedSources.map { it.id } &&
                (srcPickerSearch.isBlank() ||
                 src.title.contains(srcPickerSearch, ignoreCase = true) ||
                 src.type.contains(srcPickerSearch, ignoreCase = true))
            },
            itemIcon = { Icon(FieldMindIcons.Category, null, tint = FieldMindTheme.colors.source, size = 16.dp) },
            itemPrimaryText = { it.title.ifBlank { "Source #${it.id}" } },
            itemSecondaryText = { "${it.type} • ${it.readingStatus}" },
            onSelect = { src ->
                haptics.confirm()
                viewModel.linkQuestionSource(question.id, src.id)
                showSourcePicker = false
                srcPickerSearch = ""
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity Picker Dialog (reusable searchable list)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun <T> EntityPickerDialog(
    title: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    items: List<T>,
    itemIcon: @Composable (T) -> Unit,
    itemPrimaryText: @Composable (T) -> String,
    itemSecondaryText: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(MaterialSymbolIcon("search"), null, size = 18.dp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )

                if (items.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No items found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(items) { item ->
                            Surface(
                                onClick = { onSelect(item) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        itemIcon(item)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            itemPrimaryText(item),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            itemSecondaryText(item),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Hypothesis Mini Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HypothesisMiniCard(
    hypothesis: HypothesisEntity,
    onClick: () -> Unit
) {
    val resultColor = when (hypothesis.resultStatus) {
        "Supported" -> FieldMindTheme.colors.positive
        "Refuted" -> MaterialTheme.colorScheme.error
        "Inconclusive" -> FieldMindTheme.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status dot
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background(resultColor.copy(alpha = if (hypothesis.resultStatus == "Unknown") 0.3f else 0.8f))
            )

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    hypothesis.prediction,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        hypothesis.resultStatus.ifBlank { "Untested" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = resultColor
                    )
                    Text(
                        "${hypothesis.confidencePercent}% confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
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
                        color = FieldMindTheme.colors.hypothesis.copy(alpha = 0.12f)
                    ) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FieldMindTheme.colors.hypothesis,
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
