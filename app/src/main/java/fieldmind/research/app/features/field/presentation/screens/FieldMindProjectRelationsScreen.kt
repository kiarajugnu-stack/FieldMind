package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// ══════════════════════════════════════════════════════════════════════
//  DATA MODELS for the relations tree
// ══════════════════════════════════════════════════════════════════════

private data class LinkedGroup(
    val kind: String,
    val icon: MaterialSymbolIcon,
    val color: Color,
    val count: Int,
    val items: List<Pair<Long, String>> // id, label
)

// ══════════════════════════════════════════════════════════════════════
//  PROJECT RELATIONS SCREEN — Tree view of observation-linked entities
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectRelationsScreen(
    projectId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val species by viewModel.speciesRegistry.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val hypothesisCrossRefs by viewModel.hypothesisEvidenceCrossRefs.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val sessionCrossRefs by viewModel.sessionObservationCrossRefs.collectAsState()
    val haptics = rememberFieldMindHaptics()

    // ── Filter entities to this project ──
    val projectObs = remember(observations, projectId) {
        observations.filter { it.projectId == projectId && it.deletedAt == null }
    }
    val projectQs = remember(questions, projectId) {
        questions.filter { it.relatedProjectId == projectId && it.deletedAt == null }
    }
    val projectNotes = remember(notes, projectId) {
        notes.filter { it.projectId == projectId && it.deletedAt == null }
    }
    val projectSources = remember(sources, projectId) {
        sources.filter { it.relatedProjectId == projectId && it.deletedAt == null }
    }
    val projectTasks = remember(tasks, projectId) {
        tasks.filter { it.projectId == projectId && it.deletedAt == null }
    }
    val projectSpecies = remember(species, projectId) {
        species.filter { it.projectId == projectId && it.deletedAt == null }
    }

    // ── Compute linked groups for each observation ──
    data class ObservationRelations(
        val observation: ObservationEntity,
        val groups: List<LinkedGroup>
    )

    val relations = remember(projectObs, projectQs, projectNotes, projectSources, projectTasks, projectSpecies, hypotheses, hypothesisCrossRefs, researchSessions, sessionCrossRefs) {
        projectObs.map { obs ->
            val groups = mutableListOf<LinkedGroup>()

            // 1. Linked Notes (same project)
            val linkedNotes = projectNotes.filter { it.projectId == obs.projectId }
            if (linkedNotes.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Note",
                    icon = MaterialSymbolIcon("edit_note"),
                    color = FieldMindTheme.colors.project,
                    count = linkedNotes.size,
                    items = linkedNotes.map { it.id to (it.title.ifBlank { "Untitled note" }) }
                ))
            }

            // 2. Linked Questions (via relatedObservationIds or cross-ref)
            val linkedQs = projectQs.filter { q ->
                q.relatedObservationIds.split(",").any { it.trim().toLongOrNull() == obs.id }
            }
            if (linkedQs.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Question",
                    icon = FieldMindIcons.Question,
                    color = FieldMindTheme.colors.question,
                    count = linkedQs.size,
                    items = linkedQs.map { it.id to it.questionText.take(80) }
                ))
            }

            // 3. Linked Sources (same project)
            if (projectSources.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Source",
                    icon = FieldMindIcons.Source,
                    color = FieldMindTheme.colors.source,
                    count = projectSources.size,
                    items = projectSources.map { it.id to (it.title.ifBlank { "Unnamed source" }) }
                ))
            }

            // 4. Linked Tasks (via linkedObservationId or same project)
            val linkedTasks = projectTasks.filter { it.linkedObservationId == obs.id || it.projectId == obs.projectId }
            if (linkedTasks.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Task",
                    icon = MaterialSymbolIcon("checklist"),
                    color = FieldMindTheme.colors.flashcard,
                    count = linkedTasks.size,
                    items = linkedTasks.map { it.id to (it.title.ifBlank { "Unnamed task" }) }
                ))
            }

            // 5. Linked Species (same project)
            if (projectSpecies.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Species",
                    icon = FieldMindIcons.Nature,
                    color = FieldMindTheme.colors.positive,
                    count = projectSpecies.size,
                    items = projectSpecies.map { it.id to (it.commonName.ifBlank { it.scientificName.ifBlank { "Unknown species" } }) }
                ))
            }

            // 6. Linked Hypotheses (via evidence cross-refs)
            val linkedHypothesisIds = hypothesisCrossRefs.filter { it.observationId == obs.id }.map { it.hypothesisId }.toSet()
            val linkedHypotheses = hypotheses.filter { it.id in linkedHypothesisIds }
            if (linkedHypotheses.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Hypothesis",
                    icon = FieldMindIcons.Hypothesis,
                    color = FieldMindTheme.colors.hypothesis,
                    count = linkedHypotheses.size,
                    items = linkedHypotheses.map { it.id to it.prediction.take(80) }
                ))
            }

            // 7. Linked Research Sessions (via session cross-refs)
            val linkedSessionIds = sessionCrossRefs.filter { it.observationId == obs.id }.map { it.sessionId }.toSet()
            val linkedSessions = researchSessions.filter { it.id in linkedSessionIds }
            if (linkedSessions.isNotEmpty()) {
                groups.add(LinkedGroup(
                    kind = "Session",
                    icon = FieldMindIcons.Bolt,
                    color = FieldMindTheme.colors.data,
                    count = linkedSessions.size,
                    items = linkedSessions.map { it.id to (it.name.ifBlank { "Session #${it.id}" }) }
                ))
            }

            ObservationRelations(obs, groups)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ════════════════════════════════════════════════════════════
        //  Header
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
                Text(
                    "Relations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(40.dp)) // Balance the row
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Summary card
        // ════════════════════════════════════════════════════════════
        item {
            val totalLinked = relations.sumOf { rel -> rel.groups.sumOf { it.count } }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FieldMindTheme.colors.project.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(MaterialSymbolIcon("hub"), null, tint = FieldMindTheme.colors.project, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${projectObs.size} observations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$totalLinked linked entities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Empty state
        // ════════════════════════════════════════════════════════════
        if (projectObs.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("hub"),
                            null,
                            size = 48.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No observations yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Add observations to this project to see their relations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Observation cards with tree
        // ════════════════════════════════════════════════════════════
        items(relations, key = { it.observation.id }) { rel ->
            val obs = rel.observation
            var expanded by remember(obs.id) { mutableStateOf(rel.groups.isNotEmpty()) }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ── Observation header (always visible, tappable to expand) ──
                    Surface(
                        onClick = {
                            haptics.light()
                            expanded = !expanded
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Observation icon
                            Box(
                                Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 18.dp)
                            }

                            Column(Modifier.weight(1f)) {
                                Text(
                                    obs.subject.ifBlank { "Observation #${obs.id}" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${obs.category} • ${obs.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Total linked count badge
                            val totalCount = rel.groups.sumOf { it.count }
                            if (totalCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FieldMindTheme.colors.project.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "$totalCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FieldMindTheme.colors.project,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Expand/collapse icon
                            Icon(
                                if (expanded) MaterialSymbolIcon("expand_less") else MaterialSymbolIcon("expand_more"),
                                null,
                                size = 18.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // ── Tree branches (expandable) ──
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (rel.groups.isEmpty()) {
                                Text(
                                    "No linked entities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            } else {
                                rel.groups.forEach { group ->
                                    RelationGroupCard(
                                        group = group,
                                        onOpenDetail = onOpenDetail
                                    )
                                }
                            }

                            // ── View full observation detail ──
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { onOpenDetail("observation", obs.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(FieldMindIcons.Open, null, size = 14.dp)
                                Spacer(Modifier.size(6.dp))
                                Text("View observation detail", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Relation Group Card — One entity type branch in the tree
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RelationGroupCard(
    group: LinkedGroup,
    onOpenDetail: (String, Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Group header ──
            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tree branch connector
                    Box(
                        Modifier.size(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(group.color.copy(alpha = 0.5f))
                    )

                    // Kind icon
                    Box(
                        Modifier.size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(group.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(group.icon, null, tint = group.color, size = 14.dp)
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Linked ${group.kind}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = group.color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${group.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = group.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }

                    Icon(
                        if (expanded) MaterialSymbolIcon("expand_less") else MaterialSymbolIcon("expand_more"),
                        null,
                        size = 16.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Items list (expandable) ──
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    group.items.forEach { (id, label) ->
                        Surface(
                            onClick = { onOpenDetail(group.kind.lowercase(), id) },
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dot connector
                                Box(
                                    Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                                        .background(group.color.copy(alpha = 0.3f))
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    MaterialSymbolIcon("chevron_right"),
                                    null,
                                    size = 14.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
