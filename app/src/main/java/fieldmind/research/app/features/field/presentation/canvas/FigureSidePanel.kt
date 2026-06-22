package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.FigureMetaEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/**
 * Slide-in side panel for analyzing figure/image blocks on the canvas.
 *
 * Shows when a block of type "image", "figure", or "pdf" is selected.
 * Provides four tabs:
 * 1. **Notes** — free-text user notes auto-saved to [FigureMetaEntity.userNotes]
 * 2. **Interpretation** — AI-generated or manual analysis, saved to [FigureMetaEntity.interpretation]
 * 3. **Related Ideas** — linked entities with a link-to-entity button
 * 4. **Questions** — inline question form and quick-suggest, saved to [FigureMetaEntity.questionsGenerated]
 *
 * @param block the selected figure/image block
 * @param figureMeta the current figure metadata (null before first save)
 * @param canvasViewModel for persisting figure meta changes
 * @param onDismiss called to close the panel
 * @param onLinkToEntity called to open the entity picker for linking
 */
@Composable
fun FigureSidePanel(
    block: CanvasBlockEntity,
    figureMeta: FigureMetaEntity?,
    canvasViewModel: CanvasViewModel,
    onDismiss: () -> Unit,
    onLinkToEntity: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var notesText by remember(figureMeta?.userNotes) { mutableStateOf(figureMeta?.userNotes ?: "") }
    var interpretationText by remember(figureMeta?.interpretation) { mutableStateOf(figureMeta?.interpretation ?: "") }
    var newQuestionText by remember { mutableStateOf("") }

    // Debounced notes auto-save
    LaunchedEffect(notesText) {
        if (notesText != (figureMeta?.userNotes ?: "")) {
            delay(600)
            canvasViewModel.updateFigureNotes(block.id, notesText)
        }
    }

    // Debounced interpretation auto-save
    LaunchedEffect(interpretationText) {
        if (interpretationText != (figureMeta?.interpretation ?: "")) {
            delay(600)
            canvasViewModel.updateFigureInterpretation(block.id, interpretationText)
        }
    }

    // Parse related ideas and questions from JSON
    val relatedIdeas = remember(figureMeta?.relatedIdeas) {
        parseRelatedIdeas(figureMeta?.relatedIdeas ?: "")
    }
    val questions = remember(figureMeta?.questionsGenerated) {
        parseQuestions(figureMeta?.questionsGenerated ?: "")
    }

    Surface(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    // Title row with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    MaterialSymbolIcon("image"),
                                    null,
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    "Figure Analysis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Block #${block.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                MaterialSymbolIcon("close"),
                                "Close panel",
                                size = 20.dp
                            )
                        }
                    }
                }
            }

            // ── Figure info card ──
            FigureInfoCard(block = block, figureMeta = figureMeta)

            // ── Tab row ──
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                val tabs = listOf("Notes", "Interpretation", "Related", "Questions")
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // ── Tab content ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                when (selectedTab) {
                    0 -> NotesTab(
                        notesText = notesText,
                        onNotesChange = { notesText = it },
                        figureMeta = figureMeta
                    )
                    1 -> InterpretationTab(
                        interpretationText = interpretationText,
                        onInterpretationChange = { interpretationText = it },
                        canvasViewModel = canvasViewModel,
                        block = block,
                        figureMeta = figureMeta
                    )
                    2 -> RelatedIdeasTab(
                        relatedIdeas = relatedIdeas,
                        onLinkToEntity = onLinkToEntity
                    )
                    3 -> QuestionsTab(
                        questions = questions,
                        newQuestionText = newQuestionText,
                        onNewQuestionChange = { newQuestionText = it },
                        onAddQuestion = {
                            if (newQuestionText.isNotBlank()) {
                                val updated = questions + QuestionItem(
                                    id = System.currentTimeMillis(),
                                    text = newQuestionText,
                                    category = "General",
                                    isAiGenerated = false
                                )
                                canvasViewModel.updateFigureQuestions(
                                    block.id,
                                    questionsToJson(updated)
                                )
                                newQuestionText = ""
                            }
                        },
                        canvasViewModel = canvasViewModel,
                        block = block
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Figure Info Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FigureInfoCard(
    block: CanvasBlockEntity,
    figureMeta: FigureMetaEntity?
) {
    // Extract image URI from contentJson
    val imageUri = remember(block.contentJson) {
        if (block.contentJson.isNotBlank()) {
            try {
                JSONObject(block.contentJson).optString("uri", "")
            } catch (_: Exception) { "" }
        } else ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (imageUri.isNotBlank())
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MaterialSymbolIcon(if (imageUri.isNotBlank()) "image" else "broken_image"),
                    null,
                    size = 22.dp,
                    tint = if (imageUri.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            // Info text
            Column(Modifier.weight(1f)) {
                Text(
                    block.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${block.width.toInt()} × ${block.height.toInt()}px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (figureMeta != null) {
                    Text(
                        "Figure ${figureMeta.figureNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Type badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    block.type,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Notes Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun NotesTab(
    notesText: String,
    onNotesChange: (String) -> Unit,
    figureMeta: FigureMetaEntity?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                MaterialSymbolIcon("edit_note"),
                null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Your notes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedTextField(
            value = notesText,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxSize(),
            placeholder = {
                Text(
                    "Record observations, thoughts, or questions about this figure…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Interpretation Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun InterpretationTab(
    interpretationText: String,
    onInterpretationChange: (String) -> Unit,
    canvasViewModel: CanvasViewModel,
    block: CanvasBlockEntity,
    figureMeta: FigureMetaEntity?
) {
    var generating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                MaterialSymbolIcon("psychology"),
                null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "AI Interpretation",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            "Use AI to analyze this figure, or write your own interpretation below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Generate button
        Button(
            onClick = {
                generating = true
                // AI generation would call GeminiResearchAssistant here.
                // For now, insert a placeholder interpretation.
                onInterpretationChange(
                    buildString {
                        appendLine("AI-generated interpretation will appear here.")
                        appendLine()
                        appendLine("The figure shows a ${block.type} block positioned at (${block.positionX.toInt()}, ${block.positionY.toInt()}) on the canvas.")
                        appendLine("Dimensions: ${block.width.toInt()}px × ${block.height.toInt()}px")
                        if (figureMeta?.caption?.isNotBlank() == true) {
                            appendLine("Caption: ${figureMeta.caption}")
                        }
                        generating = false
                    }
                )
            },
            enabled = !generating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (generating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Generating…", style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(
                    MaterialSymbolIcon("auto_awesome"),
                    null,
                    size = 16.dp
                )
                Spacer(Modifier.width(6.dp))
                Text("Generate AI Interpretation", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Interpretation text field
        OutlinedTextField(
            value = interpretationText,
            onValueChange = onInterpretationChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    "Interpretation will appear here…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            textStyle = MaterialTheme.typography.bodySmall
        )

        if (interpretationText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("info"),
                        null,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "Edits are auto-saved after you stop typing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Related Ideas Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RelatedIdeasTab(
    relatedIdeas: List<RelatedIdeaItem>,
    onLinkToEntity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                MaterialSymbolIcon("hub"),
                null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Linked entities",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            "Link this figure to observations, questions, sources, or projects to connect your research.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Link button
        OutlinedButton(
            onClick = onLinkToEntity,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(MaterialSymbolIcon("add_link"), null, size = 16.dp)
            Spacer(Modifier.width(6.dp))
            Text("Link to entity", style = MaterialTheme.typography.labelMedium)
        }

        if (relatedIdeas.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            Text(
                "Existing links (${relatedIdeas.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            relatedIdeas.forEach { idea ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ideaIcon = when (idea.type) {
                            "observation" -> MaterialSymbolIcon("visibility")
                            "note" -> MaterialSymbolIcon("note")
                            "question" -> MaterialSymbolIcon("help")
                            "source" -> MaterialSymbolIcon("book")
                            "project" -> MaterialSymbolIcon("folder")
                            else -> MaterialSymbolIcon("link")
                        }
                        Icon(ideaIcon, null, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(
                                idea.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                idea.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("link_off"),
                        null,
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        "No links yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Questions Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuestionsTab(
    questions: List<QuestionItem>,
    newQuestionText: String,
    onNewQuestionChange: (String) -> Unit,
    onAddQuestion: () -> Unit,
    canvasViewModel: CanvasViewModel,
    block: CanvasBlockEntity
) {
    var suggested by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                MaterialSymbolIcon("help"),
                null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Questions about this figure",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // New question input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = newQuestionText,
                onValueChange = onNewQuestionChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask something…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Surface(
                onClick = onAddQuestion,
                shape = RoundedCornerShape(12.dp),
                color = if (newQuestionText.isNotBlank())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        MaterialSymbolIcon("add"),
                        "Add question",
                        size = 20.dp,
                        tint = if (newQuestionText.isNotBlank())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // AI suggest button
        if (suggested.isEmpty()) {
            OutlinedButton(
                onClick = {
                    suggested = listOf(
                        "What patterns are visible in this figure?",
                        "How does this relate to the research question?",
                        "What further data would strengthen this analysis?"
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(MaterialSymbolIcon("auto_awesome"), null, size = 14.dp)
                Spacer(Modifier.width(6.dp))
                Text("Suggest questions", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            // Show suggested questions
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("auto_awesome"),
                            null,
                            size = 14.dp,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            "Suggested questions",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    suggested.forEach { suggestion ->
                        Surface(
                            onClick = {
                                val updated = questions + QuestionItem(
                                    id = System.currentTimeMillis(),
                                    text = suggestion,
                                    category = "AI Suggested",
                                    isAiGenerated = true
                                )
                                canvasViewModel.updateFigureQuestions(
                                    block.id,
                                    questionsToJson(updated)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    MaterialSymbolIcon("add"),
                                    null,
                                    size = 14.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { suggested = emptyList() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Existing questions list
        if (questions.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            Text(
                "${questions.size} question(s)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            questions.forEach { question ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (question.isAiGenerated) MaterialSymbolIcon("auto_awesome")
                            else MaterialSymbolIcon("help"),
                            null,
                            size = 16.dp,
                            tint = if (question.isAiGenerated)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                question.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                question.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Data models
// ══════════════════════════════════════════════════════════════════════

private data class RelatedIdeaItem(
    val id: Long,
    val type: String,
    val label: String
)

private data class QuestionItem(
    val id: Long,
    val text: String,
    val category: String,
    val isAiGenerated: Boolean = false
)

// ══════════════════════════════════════════════════════════════════════
//  JSON parsing helpers
// ══════════════════════════════════════════════════════════════════════

private fun parseRelatedIdeas(json: String): List<RelatedIdeaItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RelatedIdeaItem(
                id = obj.optLong("id", 0),
                type = obj.optString("type", ""),
                label = obj.optString("label", "")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun parseQuestions(json: String): List<QuestionItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            QuestionItem(
                id = obj.optLong("id", 0),
                text = obj.optString("text", ""),
                category = obj.optString("category", "General"),
                isAiGenerated = obj.optBoolean("isAiGenerated", false)
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun questionsToJson(items: List<QuestionItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("category", item.category)
                put("isAiGenerated", item.isAiGenerated)
            }
        )
    }
    return arr.toString()
}
