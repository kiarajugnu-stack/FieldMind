package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.database.entity.HypothesisEntity
import fieldmind.research.app.features.field.data.database.entity.QuestionEntity
import fieldmind.research.app.features.field.data.question.QuestionGenerator
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Unified Questions & Hypotheses Screen — Hypothesis Testing Workflow
// ══════════════════════════════════════════════════════════════════════

@Composable
fun QuestionsScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val haptics = rememberFieldMindHaptics()

    // ── Local tracking of linked observations per hypothesis ──
    // hypothesisId -> list of observation ids and whether they are supporting (true) or refuting (false)
    val hypothesisEvidence = remember { mutableStateMapOf<Long, MutableList<Pair<Long, Boolean>>>() }

    // ── Load persisted evidence from database on screen open ──
    LaunchedEffect(Unit) {
        viewModel.hypothesisEvidenceCrossRefs.collect { crossRefs ->
            hypothesisEvidence.clear()
            crossRefs.forEach { ref ->
                val list = hypothesisEvidence.getOrPut(ref.hypothesisId) { mutableListOf() }
                list.add(ref.observationId to true) // default to supporting (cross-ref has no isSupporting column)
                hypothesisEvidence[ref.hypothesisId] = list
            }
        }
    }

    // ── Dialogs state ──
    var showQuestionBuilder by remember { mutableStateOf(false) }
    var createHypothesisFor by remember { mutableStateOf<QuestionEntity?>(null) }
    var addObservationFor by remember { mutableStateOf<HypothesisEntity?>(null) }
    var markTestedFor by remember { mutableStateOf<HypothesisEntity?>(null) }
    var showConcludeDialog by remember { mutableStateOf<HypothesisEntity?>(null) }

    // Auto-builder states
    var autoQuestion by remember { mutableStateOf("") }
    var autoContext by remember { mutableStateOf("") }
    var suggestedCauses by remember { mutableStateOf(listOf<String>()) }
    var suggestedPredictions by remember { mutableStateOf(listOf<String>()) }
    var suggestedCategory by remember { mutableStateOf("Other") }
    var suggestedSourceType by remember { mutableStateOf("Thought") }
    var suggestedPriority by remember { mutableStateOf("Medium") }
    var sourceNotes by remember { mutableStateOf("") }

    // Generated questions from observations
    val suggestedQuestions = remember(observations, sources, questions) {
        if (observations.isNotEmpty()) {
            QuestionGenerator.generateAll(observations, sources, questions).filter { generated ->
                !questions.any { q -> q.questionText.lowercase().trim() == generated.questionText.lowercase().trim() }
            }
        } else emptyList()
    }

    // ── Helper: count observations matching a question's category ──
    fun evidenceCount(question: QuestionEntity): Int =
        observations.count { it.category == question.category }

    // ── Helper: compute evidence stats for a hypothesis ──
    fun hypothesisSupportCount(h: HypothesisEntity): Int =
        hypothesisEvidence[h.id]?.count { it.second } ?: 0

    fun hypothesisRefuteCount(h: HypothesisEntity): Int =
        hypothesisEvidence[h.id]?.count { !it.second } ?: 0

    fun hypothesisConfidence(h: HypothesisEntity): Int {
        val supporting = hypothesisSupportCount(h)
        val refuting = hypothesisRefuteCount(h)
        val total = supporting + refuting
        if (total == 0) return h.confidencePercent
        val ratio = supporting.toFloat() / total
        return (ratio * 100).toInt().coerceIn(5, 100)
    }

    // Filter
    var filterStatus by remember { mutableStateOf("All") }
    var filterCategory by remember { mutableStateOf("All") }
    val categories = remember(questions) { listOf("All") + questions.map { it.category }.distinct().sorted() }
    val statuses = listOf("All", "Open", "Answered", "Investigating", "Archived")

    val filtered = remember(questions, filterStatus, filterCategory) {
        questions.filter { q ->
            (filterStatus == "All" || q.status == filterStatus) &&
            (filterCategory == "All" || q.category == filterCategory)
        }
    }

    // Stats
    val totalCount = questions.size
    val answeredCount = questions.count { it.status == "Answered" }
    val openCount = questions.count { it.status == "Open" || it.status == "Investigating" }
    val hypothesisCount = hypotheses.size
    val testedCount = hypotheses.count { it.resultStatus != "Unknown" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Screen Header ──
        item {
            StandardScreenHeader(
                title = "Questions & Hypotheses",
                subtitle = "All research questions, hypothesis testing workflow, and evidence tracking in one place.",
                icon = FieldMindIcons.Question,
                heroColor = FieldMindTheme.colors.question
            )
        }

        // ── Stats row ──
        if (totalCount > 0 || hypothesisCount > 0) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("${totalCount}", "Questions", FieldMindIcons.Question, FieldMindTheme.colors.question, Modifier.weight(1f))
                    StatCard("${hypothesisCount}", "Hypotheses", FieldMindIcons.Hypothesis, FieldMindTheme.colors.observation, Modifier.weight(1f))
                    StatCard("${testedCount}", "Tested", FieldMindIcons.Check, FieldMindTheme.colors.positive, Modifier.weight(1f))
                }
            }
        }

        // ════════════════════════════════════════════════════════
        //  AUTO-BUILDER — Always visible (not collapsible)
        // ════════════════════════════════════════════════════════
        item {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // ── Header row (non-clickable, always visible) ──
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(FieldMindIcons.Sparkle, null, tint = FieldMindTheme.colors.observation, size = 22.dp) }
                        Column(Modifier.weight(1f)) {
                            Text("Auto-builder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Generate questions from your observations and data gaps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // ── Content (always visible) ──
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Suggested questions from observations
                        if (suggestedQuestions.isNotEmpty()) {
                            Text("Suggested from observations:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            suggestedQuestions.take(6).forEach { sq ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        autoQuestion = sq.questionText
                                        autoContext = sq.context
                                        suggestedCategory = sq.category
                                        suggestedSourceType = sq.sourceType
                                        suggestedPriority = sq.priority
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(sq.questionText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                InfoChip(sq.category, icon = FieldMindIcons.Category)
                                                InfoChip(sq.sourceType)
                                                InfoChip(sq.priority, icon = FieldMindIcons.Alert)
                                            }
                                        }
                                        Icon(FieldMindIcons.Add, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                        }

                        // ── Or enter a custom topic ──
                        Text("Or write your own:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FieldTextField(autoQuestion, { autoQuestion = it }, "What are you curious about? (e.g., \"bird activity at dawn\")", minLines = 2)

                        FieldTextField(autoContext, { autoContext = it }, "Context / observations that led to this question", minLines = 2)
                        FieldTextField(sourceNotes, { sourceNotes = it }, "Testable prediction or hypothesis", minLines = 1)

                        // More parameters
                        val allCategories = remember(questions) {
                            listOf("Other", "Bird", "Mammal", "Insect", "Plant", "Fungi", "Amphibian", "Reptile", "Fish", "Marine", "Weather", "Geology", "Astronomy", "Ecology", "Behavior", "Phenology", "General") +
                                questions.map { it.category }.distinct().filterNot { it.isBlank() }.sorted()
                        }
                        Text("Question parameters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChips(allCategories, suggestedCategory.ifBlank { "Other" }) { suggestedCategory = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChips(listOf("Thought", "Observation", "Reading", "Data gap", "Discussion", "Method", "Prediction", "Comparison"), suggestedSourceType.ifBlank { "Thought" }) { suggestedSourceType = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChips(listOf("Low", "Medium", "High"), suggestedPriority.ifBlank { "Medium" }) { suggestedPriority = it }
                        }

                        Button(
                            onClick = {
                                haptics.confirm()
                                val finalQuestion = autoQuestion.ifBlank { "Investigate: ${autoContext.take(80)}" }
                                viewModel.addQuestion(
                                    question = finalQuestion,
                                    category = suggestedCategory.ifBlank { "Other" },
                                    sourceType = suggestedSourceType.ifBlank { "Thought" },
                                    status = "Open",
                                    priority = suggestedPriority.ifBlank { "Medium" }
                                )
                                autoQuestion = ""
                                autoContext = ""
                                sourceNotes = ""
                                suggestedCategory = ""
                                suggestedSourceType = ""
                                suggestedPriority = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = autoQuestion.isNotBlank() || autoContext.isNotBlank()
                        ) { Text("Create research question") }
                    }
                }
            }
        }

        // ── Quick add question ──
        item {
            AddButton("Add question") {
                showQuestionBuilder = true
            }
        }

        // ── Filters ──
        if (categories.size > 1) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(FieldMindIcons.Filter, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Text("Filters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChipsRow(statuses, filterStatus) { filterStatus = it }
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChipsRow(categories, filterCategory) { filterCategory = it }
                    }
                }
            }
        }

        // ── Empty state ──
        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    if (questions.isEmpty()) "No questions yet" else "No matching questions",
                    if (questions.isEmpty()) "Questions drive your research. Build questions from observations, data gaps, and curiosity. Use the auto-builder above to generate structured questions. Then tap [Test →] to create hypotheses and track evidence." else "Try changing the filters.",
                    icon = FieldMindIcons.Question
                )
            }
        }

        // ════════════════════════════════════════════════════════
        //  QUESTION CARDS with nested hypotheses
        // ════════════════════════════════════════════════════════
        items(filtered, key = { it.id }) { q ->
            val questionHypotheses = remember(hypotheses, q.id) {
                hypotheses.filter { it.linkedQuestionId == q.id }
            }

            QuestionCardWithHypotheses(
                question = q,
                evidenceCount = evidenceCount(q),
                hypotheses = questionHypotheses,
                hypothesisSupportCount = { hypothesisSupportCount(it) },
                hypothesisRefuteCount = { hypothesisRefuteCount(it) },
                hypothesisConfidence = { hypothesisConfidence(it) },
                onTest = { createHypothesisFor = q },
                onAddObservation = { addObservationFor = it },
                onMarkTested = { markTestedFor = it },
                onToggleEvidence = { h, obsId, isSupp ->
                    val list = hypothesisEvidence.getOrPut(h.id) { mutableListOf() }
                    val idx = list.indexOfFirst { it.first == obsId }
                    if (idx >= 0) list[idx] = obsId to isSupp
                },
                onDeleteHypothesis = { viewModel.deleteHypothesis(it.id) },
                viewModel = viewModel,
                onOpenDetail = onOpenDetail
            )
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DIALOGS
    // ════════════════════════════════════════════════════════════

    // Question builder screen
    if (showQuestionBuilder) {
        NewQuestionScreen(viewModel = viewModel, onBack = { showQuestionBuilder = false })
    }

    // Hypothesis creation dialog
    createHypothesisFor?.let { question ->
        HypothesisCreationDialog(
            question = question,
            onDismiss = { createHypothesisFor = null },
            onCreate = { prediction, reasoning, evidenceNeeded, supportCriteria, weakeningCriteria, confidence ->
                viewModel.addHypothesis(
                    questionId = question.id,
                    prediction = prediction,
                    reasoning = reasoning,
                    evidenceNeeded = evidenceNeeded,
                    supportCriteria = supportCriteria,
                    weakeningCriteria = weakeningCriteria,
                    confidence = confidence
                )
                createHypothesisFor = null
            }
        )
    }

    // Quick-capture dialog for adding observation to hypothesis
    addObservationFor?.let { hypothesis ->
        QuickCaptureDialog(
            hypothesis = hypothesis,
            onDismiss = { addObservationFor = null },
            onSave = { subject, facts, category, isSupporting ->
                viewModel.addObservation(
                    subject = subject.ifBlank { "Evidence: ${hypothesis.prediction.take(40)}" },
                    category = category,
                    facts = facts.ifBlank { "Observation linked to hypothesis #${hypothesis.id}" },
                    confidence = "Likely",
                    manualLocation = "",
                    tags = "hypothesis:${hypothesis.id}",
                    evidence = "",
                    context = ""
                ) { observationId ->
                    // Link observation to hypothesis in VM
                    viewModel.linkHypothesisEvidence(hypothesis.id, observationId)
                    // Track locally for badge display
                    val list = hypothesisEvidence.getOrPut(hypothesis.id) { mutableListOf() }
                    list.add(observationId to isSupporting)
                    hypothesisEvidence[hypothesis.id] = list
                }
                addObservationFor = null
            }
        )
    }

    // Mark as tested dialog
    markTestedFor?.let { hypothesis ->
        MarkAsTestedDialog(
            hypothesis = hypothesis,
            onDismiss = { markTestedFor = null },
            onConclude = { conclusion ->
                viewModel.updateHypothesisEntity(
                    hypothesis.copy(
                        resultStatus = conclusion,
                        confidencePercent = hypothesisConfidence(hypothesis)
                    )
                )
                markTestedFor = null
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  QUESTION CARD WITH NESTED HYPOTHESES
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuestionCardWithHypotheses(
    question: QuestionEntity,
    evidenceCount: Int,
    hypotheses: List<HypothesisEntity>,
    hypothesisSupportCount: (HypothesisEntity) -> Int,
    hypothesisRefuteCount: (HypothesisEntity) -> Int,
    hypothesisConfidence: (HypothesisEntity) -> Int,
    onTest: () -> Unit,
    onAddObservation: (HypothesisEntity) -> Unit,
    onMarkTested: (HypothesisEntity) -> Unit,
    onToggleEvidence: (HypothesisEntity, Long, Boolean) -> Unit,
    onDeleteHypothesis: (HypothesisEntity) -> Unit,
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit
) {
    var expanded by remember(question.id) { mutableStateOf(false) }
    val accent = when (question.status) {
        "Answered" -> FieldMindTheme.colors.positive
        "Open" -> FieldMindTheme.colors.flashcard
        "Investigating" -> FieldMindTheme.colors.data
        "Archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── Question header row ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Question, null, tint = accent, size = 20.dp) }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        question.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(question.status, icon = FieldMindIcons.Check)
                        InfoChip(question.priority, icon = FieldMindIcons.Alert)
                        InfoChip(question.category)
                    }

                    // Evidence count badge
                    if (evidenceCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 13.dp)
                            Text(
                                "$evidenceCount related observation${if (evidenceCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FieldMindTheme.colors.observation
                            )
                        }
                    }

                    // Hypothesis count
                    if (hypotheses.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(FieldMindIcons.Hypothesis, null, tint = FieldMindTheme.colors.observation, size = 13.dp)
                            Text(
                                "${hypotheses.size} test${if (hypotheses.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FieldMindTheme.colors.observation
                            )
                        }
                    }
                }

                Icon(if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            }

            // ── Expanded actions ──
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Source: ${question.sourceType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Created: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(question.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (question.answer.isNotBlank()) {
                        Text(
                            "Answer: ${question.answer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ── Action buttons ──
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [Test →] button
                        FilledTonalButton(
                            onClick = onTest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FieldMindTheme.colors.observation.copy(alpha = 0.14f),
                                contentColor = FieldMindTheme.colors.observation
                            )
                        ) {
                            Icon(FieldMindIcons.Hypothesis, null, size = 16.dp)
                            Spacer(Modifier.size(4.dp))
                            Text("Test →", fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.updateQuestionEntity(question.copy(
                                    status = if (question.status == "Answered") "Open" else "Answered",
                                    answeredAt = if (question.status == "Answered") null else System.currentTimeMillis()
                                ))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (question.status == "Answered") "Reopen" else "Mark answered")
                        }

                        OutlinedButton(
                            onClick = { onOpenDetail("question", question.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Detail")
                        }
                    }

                    // ════════════════════════════════════════════════════
                    //  HYPOTHESIS CARDS nested under this question
                    // ════════════════════════════════════════════════════
                    if (hypotheses.isNotEmpty()) {
                        HorizontalDivider(color = FieldMindTheme.colors.observation.copy(alpha = 0.12f))

                        Text(
                            "Hypotheses (${hypotheses.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = FieldMindTheme.colors.observation
                        )

                        hypotheses.forEach { hypothesis ->
                            HypothesisCard(
                                hypothesis = hypothesis,
                                supportCount = hypothesisSupportCount(hypothesis),
                                refuteCount = hypothesisRefuteCount(hypothesis),
                                confidence = hypothesisConfidence(hypothesis),
                                onAddObservation = { onAddObservation(hypothesis) },
                                onMarkTested = { onMarkTested(hypothesis) },
                                onDelete = { onDeleteHypothesis(hypothesis) },
                                onToggleEvidenceSupport = { obsId, isSupp ->
                                    onToggleEvidence(hypothesis, obsId, isSupp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  HYPOTHESIS CARD
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HypothesisCard(
    hypothesis: HypothesisEntity,
    supportCount: Int,
    refuteCount: Int,
    confidence: Int,
    onAddObservation: () -> Unit,
    onMarkTested: () -> Unit,
    onDelete: () -> Unit,
    onToggleEvidenceSupport: (Long, Boolean) -> Unit
) {
    val isTested = hypothesis.resultStatus != "Unknown"
    val statusColor = when (hypothesis.resultStatus) {
        "Supported" -> FieldMindTheme.colors.positive
        "Refuted" -> MaterialTheme.colorScheme.error
        "Inconclusive" -> FieldMindTheme.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val totalEvidence = supportCount + refuteCount

    // Animate confidence bar
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence / 100f,
        animationSpec = tween(600),
        label = "confidenceBar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── Status badge + prediction ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        if (isTested) hypothesis.resultStatus else "Untested",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    hypothesis.prediction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), size = 16.dp)
                }
            }

            // Reasoning
            if (hypothesis.reasoning.isNotBlank()) {
                Text(
                    hypothesis.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Confidence progress bar ──
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confidence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${confidence}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
                LinearProgressIndicator(
                    progress = { animatedConfidence },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (confidence >= 60) FieldMindTheme.colors.positive
                            else if (confidence >= 30) FieldMindTheme.colors.warning
                            else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            // ── Evidence counts: supporting (green) vs refuting (red) ──
            if (totalEvidence > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Supporting count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(FieldMindTheme.colors.positive)
                        )
                        Text(
                            "$supportCount supporting",
                            style = MaterialTheme.typography.labelSmall,
                            color = FieldMindTheme.colors.positive,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Refuting count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Text(
                            "$refuteCount refuting",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Evidence rules
            if (hypothesis.evidenceNeeded.isNotBlank()) {
                Text(
                    "Evidence needed: ${hypothesis.evidenceNeeded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Action buttons ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddObservation,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(FieldMindIcons.Add, null, size = 14.dp)
                    Spacer(Modifier.size(4.dp))
                    Text("Add observation", fontSize = 10.sp)
                }

                if (!isTested) {
                    FilledTonalButton(
                        onClick = onMarkTested,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 14.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Mark as tested", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  HYPOTHESIS CREATION DIALOG
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HypothesisCreationDialog(
    question: QuestionEntity,
    onDismiss: () -> Unit,
    onCreate: (prediction: String, reasoning: String, evidenceNeeded: String, supportCriteria: String, weakeningCriteria: String, confidence: Int) -> Unit
) {
    var prediction by remember { mutableStateOf("") }
    var reasoning by remember { mutableStateOf("") }
    var evidenceNeeded by remember { mutableStateOf("") }
    var supportCriteria by remember { mutableStateOf("") }
    var weakeningCriteria by remember { mutableStateOf("") }
    var confidence by remember { mutableIntStateOf(50) }
    val canCreate = prediction.isNotBlank()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Hypothesis, null, tint = FieldMindTheme.colors.observation, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Test this question", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(question.questionText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Prediction
                FieldTextField(prediction, { prediction = it }, "What do you predict will happen?", minLines = 2, required = true, supportingText = "A testable prediction based on your question")

                // Reasoning
                FieldTextField(reasoning, { reasoning = it }, "Why do you think this?", minLines = 2, supportingText = "Your reasoning behind this prediction")

                // Evidence needed
                FieldTextField(evidenceNeeded, { evidenceNeeded = it }, "What evidence would confirm or refute this?", minLines = 2, supportingText = "Describe what you would observe if this prediction is correct vs incorrect")

                // Support/refute criteria
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldTextField(supportCriteria, { supportCriteria = it }, "Supporting criteria", modifier = Modifier.weight(1f), supportingText = "What counts as supporting evidence?")
                    FieldTextField(weakeningCriteria, { weakeningCriteria = it }, "Weakening criteria", modifier = Modifier.weight(1f), supportingText = "What would weaken this hypothesis?")
                }

                // Initial confidence slider
                Text("Initial confidence: ${confidence}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = confidence.toFloat(),
                    onValueChange = { confidence = it.toInt() },
                    valueRange = 5f..95f,
                    steps = 17,
                    modifier = Modifier.fillMaxWidth()
                )

                // Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onCreate(prediction, reasoning, evidenceNeeded, supportCriteria, weakeningCriteria, confidence) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = canCreate
                    ) {
                        Text("Create hypothesis")
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  QUICK-CAPTURE DIALOG — Add observation linked to hypothesis
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickCaptureDialog(
    hypothesis: HypothesisEntity,
    onDismiss: () -> Unit,
    onSave: (subject: String, facts: String, category: String, isSupporting: Boolean) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var facts by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var isSupporting by remember { mutableStateOf(true) }
    val canSave = subject.isNotBlank() || facts.isNotBlank()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Add, null, tint = FieldMindTheme.colors.observation, size = 22.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Add evidence observation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("For hypothesis: ${hypothesis.prediction.take(60)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FieldTextField(subject, { subject = it }, "What did you observe?", supportingText = "e.g. Crow was feeding at dawn")
                FieldTextField(facts, { facts = it }, "Details", minLines = 2, supportingText = "Describe what you saw, heard, or measured")

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Category:", style = MaterialTheme.typography.labelMedium)
                    ChoiceChips(
                        listOf("Other", "Behavior", "Environment", "Ecology", "Social", "Phenology"),
                        category
                    ) { category = it }
                }

                // Supporting vs refuting toggle
                Surface(
                    onClick = { isSupporting = !isSupporting },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSupporting) FieldMindTheme.colors.positive.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    border = if (isSupporting) null
                             else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(12.dp).clip(CircleShape)
                                .background(if (isSupporting) FieldMindTheme.colors.positive else MaterialTheme.colorScheme.error)
                        )
                        Text(
                            if (isSupporting) "This observation SUPPORTS the hypothesis" else "This observation REFUTES the hypothesis",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSupporting) FieldMindTheme.colors.positive else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(subject, facts, category, isSupporting) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = canSave
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Save observation")
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  MARK AS TESTED DIALOG
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MarkAsTestedDialog(
    hypothesis: HypothesisEntity,
    onDismiss: () -> Unit,
    onConclude: (conclusion: String) -> Unit
) {
    val conclusions = listOf("Supported", "Refuted", "Inconclusive")
    var selectedConclusion by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Check, null, tint = FieldMindTheme.colors.observation, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Mark hypothesis as tested", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(hypothesis.prediction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Text("Based on the evidence collected, what is your conclusion?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Conclusion options
                conclusions.forEach { conclusion ->
                    val color = when (conclusion) {
                        "Supported" -> FieldMindTheme.colors.positive
                        "Refuted" -> MaterialTheme.colorScheme.error
                        else -> FieldMindTheme.colors.warning
                    }
                    val icon = when (conclusion) {
                        "Supported" -> FieldMindIcons.Check
                        "Refuted" -> FieldMindIcons.Close
                        else -> FieldMindIcons.Question
                    }

                    Surface(
                        onClick = { selectedConclusion = conclusion },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedConclusion == conclusion) color.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selectedConclusion == conclusion) androidx.compose.foundation.BorderStroke(1.5.dp, color)
                                 else null,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedConclusion == conclusion) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = if (selectedConclusion == conclusion) color else MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(conclusion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    color = if (selectedConclusion == conclusion) color else MaterialTheme.colorScheme.onSurface)
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
                            if (selectedConclusion == conclusion) {
                                Icon(FieldMindIcons.Check, null, tint = color, size = 20.dp)
                            }
                        }
                    }
                }

                // Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConclude(selectedConclusion) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = selectedConclusion.isNotBlank()
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  HELPERS (reused from original)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StatCard(value: String, label: String, icon: MaterialSymbolIcon, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, size = 16.dp)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilterChipsRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { item ->
            FilterChip(selected = selected == item, onClick = { onSelect(item) }, label = { Text(item, fontSize = 10.sp) })
        }
    }
}
