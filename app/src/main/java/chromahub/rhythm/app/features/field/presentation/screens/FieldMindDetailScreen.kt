package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.data.ai.AiProvider
import fieldmind.research.app.features.field.data.ai.AssistantTask
import fieldmind.research.app.features.field.data.ai.GeminiResearchAssistant
import fieldmind.research.app.features.field.data.database.entity.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Detail Screen — Entity-specific rich layouts
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DetailScreen(
    kind: String,
    id: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenReader: (String, String) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val title = kind.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    var showEdit by remember(kind, id) { mutableStateOf(false) }
    var showDelete by remember(kind, id) { mutableStateOf(false) }
    val editable = kind in setOf("observation", "note", "question", "hypothesis", "project", "source", "data", "report", "flashcard")
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val exportText = remember(kind, id, observations, projects, data, reports) {
        when (kind) {
            "observation" -> observations.firstOrNull { it.id == id }?.let(FieldMindExport::singleObservationMarkdown)
            "project" -> projects.firstOrNull { it.id == id }?.let(FieldMindExport::singleProjectMarkdown)
            "data" -> data.firstOrNull { it.id == id }?.let(FieldMindExport::singleDataRecordMarkdown)
            "report" -> reports.firstOrNull { it.id == id }?.let(FieldMindExport::singleReportMarkdown)
            else -> null
        }.orEmpty()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Back header ──
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(44.dp)
                    ) { Box(contentAlignment = Alignment.Center) { Icon(FieldMindIcons.Back, null, size = 22.dp) } }
                    EntityBadge(kind)
                    Spacer(Modifier.weight(1f))
                    if (editable && exportText.isNotBlank()) {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(exportText)) }, modifier = Modifier.size(36.dp)) {
                            Icon(FieldMindIcons.Article, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                        }
                        IconButton(onClick = { sharePlainText(context, exportText) }, modifier = Modifier.size(36.dp)) {
                            Icon(FieldMindIcons.Export, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                        }
                    }
                }
            }

            if (editable && !showEdit) {
                item { DetailActionBar(onEdit = { showEdit = true }, onDelete = { showDelete = true }) }
            }

            if (showEdit) {
                when (kind) {
                    "note" -> notes.firstOrNull { it.id == id }?.let { n ->
                        item { InlineEditNote(n, viewModel, { showEdit = false; onBack() }) }
                    }
                    "observation" -> observations.firstOrNull { it.id == id }?.let { o ->
                        item { InlineEditObservation(o, viewModel, { showEdit = false; onBack() }) }
                    }
                }
            } else {
                when (kind) {
                    "note" -> notes.firstOrNull { it.id == id }?.let { n ->
                        item { NoteDetailContent(n, onOpenDetail) }
                        item { BacklinksPanel(buildList {
                            projects.firstOrNull { it.id == n.projectId }?.let { add(Triple("project", it.title, it.id)) }
                            sources.firstOrNull { it.id == n.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                        }, onOpenDetail) }
                    }
                    "observation" -> observations.firstOrNull { it.id == id }?.let { o ->
                        item { ObservationDetailContent(o, viewModel, onOpenReader) }
                        item { BacklinksPanel(buildList {
                            projects.firstOrNull { it.id == o.projectId }?.let { add(Triple("project", it.title, it.id)) }
                            data.filter { it.observationId == o.id }.forEach { add(Triple("data", it.label, it.id)) }
                        }, onOpenDetail) }
                    }
                    "question" -> questions.firstOrNull { it.id == id }?.let { qn ->
                        item { QuestionDetailContent(qn, hypotheses) { ans -> viewModel.setQuestionAnswer(qn, ans) } }
                        item { BacklinksPanel(buildList {
                            projects.firstOrNull { it.id == qn.relatedProjectId }?.let { add(Triple("project", it.title, it.id)) }
                            hypotheses.filter { it.linkedQuestionId == qn.id }.forEach { add(Triple("hypothesis", it.prediction, it.id)) }
                        }, onOpenDetail) }
                    }
                    "hypothesis" -> hypotheses.firstOrNull { it.id == id }?.let { h ->
                        item { HypothesisDetailContent(h) }
                        item { BacklinksPanel(buildList {
                            questions.firstOrNull { it.id == h.linkedQuestionId }?.let { add(Triple("question", it.questionText, it.id)) }
                        }, onOpenDetail) }
                    }
                    "project" -> projects.firstOrNull { it.id == id }?.let { p ->
                        item { ProjectDetailContent(p, observations, questions, sources, data, reports) }
                        item { BacklinksPanel(buildList {
                            observations.filter { it.projectId == p.id }.forEach { add(Triple("observation", it.subject, it.id)) }
                            questions.filter { it.relatedProjectId == p.id }.forEach { add(Triple("question", it.questionText, it.id)) }
                            sources.filter { it.relatedProjectId == p.id }.forEach { add(Triple("source", it.title, it.id)) }
                            data.filter { it.projectId == p.id }.forEach { add(Triple("data", it.label, it.id)) }
                            reports.filter { it.projectId == p.id }.forEach { add(Triple("report", it.title, it.id)) }
                        }, onOpenDetail) }
                    }
                    "source" -> sources.firstOrNull { it.id == id }?.let { s ->
                        item { DetailBody(s.title, "source", listOf("Type" to s.type, "Author" to s.author, "Year" to s.dateOrYear, "DOI / ISBN" to s.doiOrIsbn, "Publisher / journal" to s.publisherOrJournal, "Link" to s.link, "Access date" to s.accessDate, "Citation note" to s.citationStyleNote, "Importance" to s.importance, "Reading status" to s.readingStatus, "Project" to (projects.firstOrNull { it.id == s.relatedProjectId }?.title ?: "None"), "Main idea" to s.personalSummary, "Key findings" to s.keyFindings, "Taught me" to s.whatThisSourceTaughtMe, "Paper prompts" to s.paperNotes, "Questions" to s.questionsGenerated)) }
                        item { SourcePreviewPanel(s, onOpenReader) }
                        item { SourceActionPanel(s, projects, viewModel, onOpenDetail) }
                        item { BacklinksPanel(buildList {
                            projects.firstOrNull { it.id == s.relatedProjectId }?.let { add(Triple("project", it.title, it.id)) }
                            flashcards.filter { it.sourceId == s.id }.forEach { add(Triple("flashcard", it.front, it.id)) }
                        }, onOpenDetail) }
                    }
                    "data" -> data.firstOrNull { it.id == id }?.let { d ->
                        item { DataRecordDetailContent(d) }
                        item { BacklinksPanel(buildList {
                            projects.firstOrNull { it.id == d.projectId }?.let { add(Triple("project", it.title, it.id)) }
                            observations.firstOrNull { it.id == d.observationId }?.let { add(Triple("observation", it.subject, it.id)) }
                        }, onOpenDetail) }
                    }
                    "report" -> reports.firstOrNull { it.id == id }?.let { r ->
                        item { ReportDetailContent(r) }
                        item { BacklinksPanel(buildList { projects.firstOrNull { it.id == r.projectId }?.let { add(Triple("project", it.title, it.id)) } }, onOpenDetail) }
                    }
                    "flashcard" -> flashcards.firstOrNull { it.id == id }?.let { f ->
                        item { FlashcardDetailContent(f) }
                        item { BacklinksPanel(buildList {
                            sources.firstOrNull { it.id == f.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                            projects.firstOrNull { it.id == f.projectId }?.let { add(Triple("project", it.title, it.id)) }
                        }, onOpenDetail) }
                    }
                }
            }
        }
    }
    if (showEdit && kind !in setOf("note", "observation")) EditEntityDialog(kind, id, viewModel) { showEdit = false }
    if (showDelete) ConfirmDeleteDialog(kind, onDismiss = { showDelete = false }) {
        deleteEntityByKind(kind, id, viewModel); showDelete = false; onBack()
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity-Specific Detail Content
// ══════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ObservationDetailContent(
    o: ObservationEntity,
    viewModel: FieldMindViewModel,
    onOpenReader: (String, String) -> Unit
) {
    val colors = FieldMindTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── 1. Hero carousel (swipeable media) ──
            ObservationHeroCarousel(viewModel, o.id, onOpenReader)

            // ── 2. Header with subject and badges ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.observation.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Observation, null, tint = colors.observation, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(o.subject.ifBlank { "Observation" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(o.category, icon = FieldMindIcons.Category)
                        ConfidenceChip(o.confidenceLevel)
                    }
                }
            }

            // ── 3. Stats bar ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ObsStatItem("${o.date} ${o.time}", "Date", FieldMindIcons.Calendar)
                if (o.latitude != null && o.longitude != null) {
                    ObsStatItem("GPS", "Location", FieldMindIcons.Location)
                }
                if (o.evidenceSummary.isNotBlank()) {
                    ObsStatItem("Has evidence", "Attachments", FieldMindIcons.Gallery)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ── 4. Facts-only notes (prominent) ──
            if (o.factsOnlyNotes.isNotBlank()) {
                Text("Facts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.observation)
                Text(o.factsOnlyNotes, style = MaterialTheme.typography.bodyLarge)
            }

            // ── 5. Context ──
            if (o.moodOrContext.isNotBlank()) {
                Text("Context", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(o.moodOrContext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── 6. Evidence summary ──
            if (o.evidenceSummary.isNotBlank()) {
                Text("Evidence", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.info)
                Text(o.evidenceSummary, style = MaterialTheme.typography.bodyMedium)
            }

            // ── 7. Tags ──
            if (o.tags.isNotBlank()) {
                FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    o.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        TagChip(tag.trim())
                    }
                }
            }

            // ── 8. Weather chip row (compact) ──
            if (o.weatherTemperature != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.info.copy(alpha = 0.08f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(FieldMindIcons.Weather, null, tint = colors.info, size = 18.dp)
                    WeatherChip("${o.weatherTemperature?.let { "%.1f°".format(it) } ?: "--"}", colors.info)
                    if (o.weatherCondition.isNotBlank()) WeatherChip(o.weatherCondition.take(12), colors.info)
                    if (o.weatherHumidity != null) WeatherChip("${o.weatherHumidity}% RH", colors.data)
                    if (o.weatherWindSpeed != null) WeatherChip("%.1f km/h".format(o.weatherWindSpeed), colors.warning)
                    if (o.weatherCloudCover != null) WeatherChip("☁ ${o.weatherCloudCover}%", MaterialTheme.colorScheme.onSurfaceVariant)
                    if (o.weatherPressure != null) WeatherChip("${o.weatherPressure?.toInt()} hPa", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── 9. Provenance collapsible section ──
            var showProvenance by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showProvenance = !showProvenance },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                            Text("Provenance & metadata", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(if (showProvenance) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                    }
                    if (showProvenance) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProvenanceRow("Date & time", "${o.date} ${o.time}")
                            o.startedAt?.let { ProvenanceRow("Session start", formatTimestamp(it)) }
                            o.endedAt?.let { ProvenanceRow("Session end", formatTimestamp(it)) }
                            o.durationMs?.let { ProvenanceRow("Duration", formatDuration(it)) }
                            o.latitude?.let { lat ->
                                o.longitude?.let { lng -> ProvenanceRow("Coordinates", "%.6f, %.6f".format(lat, lng)) }
                            }
                            if (o.manualLocation.isNotBlank()) ProvenanceRow("Place", o.manualLocation)
                            o.weatherSnapshotAt?.let { ProvenanceRow("Weather snapshot", formatTimestamp(it)) }
                            ProvenanceRow("Record ID", "#${o.id}")
                            ProvenanceRow("Created", formatTimestamp(o.createdAt))
                            ProvenanceRow("Updated", formatTimestamp(o.updatedAt))
                        }
                    }
                }
            }

            // ── 10. Map card ──
            if (o.latitude != null && o.longitude != null) {
                ObservationLocationCard(o.latitude, o.longitude, o.manualLocation)
            }

            // ── 11. Attachments gallery ──
            ObservationAttachmentsPanel(viewModel, o.id, onOpenReader)
        }
    }
}

@Composable
private fun WeatherChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProvenanceRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ObservationHeroCarousel(viewModel: FieldMindViewModel, observationId: Long, onOpenReader: (String, String) -> Unit) {
    val attachments by viewModel.attachmentsForObservation(observationId).collectAsState(initial = emptyList())
    val media = attachments.filter { it.type.equals("Photo", true) || it.type.equals("Gallery", true) || uriLooksImage(it.uri) || uriLooksImage(it.localPath.orEmpty()) }
    if (media.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { media.size })

    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = media[page]
            val displayUri = item.localPath ?: item.uri
            AsyncImage(
                model = displayUri,
                contentDescription = item.caption.ifBlank { "Observation media" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onOpenReader(displayUri, item.caption.ifBlank { "Observation media" }) }
            )
        }

        // Page indicator dots
        if (media.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(media.size) { index ->
                    Box(
                        Modifier
                            .size(if (pagerState.currentPage == index) 10.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .run { if (pagerState.currentPage == index) background(MaterialTheme.colorScheme.primary) else background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) }
                    )
                }
            }
        }

        // Media counter badge
        if (media.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${media.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ObsStatItem(value: String, label: String, icon: MaterialSymbolIcon) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun NoteDetailContent(
    n: NoteEntity,
    onOpenDetail: (String, Long) -> Unit
) {
    val colors = FieldMindTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.source.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Note, null, tint = colors.source, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(n.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(n.category.ifBlank { "Uncategorized" }, icon = FieldMindIcons.Category)
                        InfoChip(recentRelativeTime(n.updatedAt), icon = FieldMindIcons.Calendar)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Body
            if (n.body.isNotBlank()) {
                Text(n.body, style = MaterialTheme.typography.bodyLarge)
            }

            // Tags
            if (n.tags.isNotBlank()) {
                FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    n.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        TagChip(tag.trim())
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity-Specific Detail Composables
// ══════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuestionDetailContent(
    qn: QuestionEntity,
    hypotheses: List<HypothesisEntity>,
    onSaveAnswer: (String) -> Unit
) {
    val colors = FieldMindTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header with icon and badges
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.question.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Question, null, tint = colors.question, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(qn.questionText.ifBlank { "Question" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(qn.category, icon = FieldMindIcons.Category)
                        StatusChip(qn.status, colors.question)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Details
            DetailRow("Source type", qn.sourceType)
            DetailRow("Priority", qn.priority)
            if (qn.relatedProjectId != null) {
                DetailRow("Project", "Linked to project")
            }

            // Answer section
            QuestionAnswerCard(qn, onSaveAnswer)

            // Linked hypotheses
            val linked = hypotheses.filter { it.linkedQuestionId == qn.id }
            if (linked.isNotEmpty()) {
                Text("Hypotheses (${linked.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.hypothesis)
                linked.forEach { h ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(colors.hypothesis.copy(alpha = 0.08f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(FieldMindIcons.Hypothesis, null, tint = colors.hypothesis, size = 18.dp)
                        Column(Modifier.weight(1f)) {
                            Text(h.prediction, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Confidence: ${h.confidencePercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HypothesisDetailContent(
    h: HypothesisEntity
) {
    val colors = FieldMindTheme.colors
    val resultColor = when ((h.resultStatus ?: "").lowercase()) {
        "supported" -> colors.positive
        "refuted" -> MaterialTheme.colorScheme.error
        "inconclusive" -> colors.warning
        else -> colors.hypothesis
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.hypothesis.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Hypothesis, null, tint = colors.hypothesis, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(h.prediction.ifBlank { "Hypothesis" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    StatusChip(h.resultStatus.ifBlank { "Untested" }, resultColor)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Reasoning
            if (h.reasoning.isNotBlank()) {
                Text("Reasoning", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.hypothesis)
                Text(h.reasoning, style = MaterialTheme.typography.bodyMedium)
            }

            // Support criteria
            if (h.supportCriteria.isNotBlank()) {
                Text("Support criteria", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.positive)
                Text(h.supportCriteria, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Weakening criteria
            if (h.weakeningCriteria.isNotBlank()) {
                Text("Weakening criteria", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                Text(h.weakeningCriteria, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Test method
            if (h.testMethod.isNotBlank()) {
                Text("Test method", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.info)
                Text(h.testMethod, style = MaterialTheme.typography.bodyMedium)
            }

            // Confidence meter
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Confidence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${h.confidencePercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = resultColor)
            }
            LinearProgressIndicator(
                progress = { (h.confidencePercent).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = resultColor,
                trackColor = resultColor.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProjectDetailContent(
    p: ProjectEntity,
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    sources: List<SourceEntity>,
    dataRecords: List<DataRecordEntity>,
    reports: List<ReportEntity>
) {
    val colors = FieldMindTheme.colors
    val obsCount = observations.count { it.projectId == p.id }
    val qCount = questions.count { it.relatedProjectId == p.id }
    val srcCount = sources.count { it.relatedProjectId == p.id }
    val dataCount = dataRecords.count { it.projectId == p.id }
    val repCount = reports.count { it.projectId == p.id }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.project.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Project, null, tint = colors.project, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(p.title.ifBlank { "Project" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    StatusChip(p.status.ifBlank { "Active" }, colors.project)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Research question
            if (p.researchQuestion.isNotBlank()) {
                Text("Research question", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.project)
                Text(p.researchQuestion, style = MaterialTheme.typography.bodyMedium)
            }

            // Objective
            if (p.objective.isNotBlank()) {
                Text("Objective", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.objective, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Background notes
            if (p.backgroundNotes.isNotBlank()) {
                Text("Background", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.backgroundNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Methods
            if (p.methods.isNotBlank()) {
                Text("Methods", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.info)
                Text(p.methods, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Data summary
            if (p.dataSummary.isNotBlank()) {
                Text("Data summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.data)
                Text(p.dataSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Analysis
            if (p.analysis.isNotBlank()) {
                Text("Analysis", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.hypothesis)
                Text(p.analysis, style = MaterialTheme.typography.bodySmall)
            }

            // Conclusion
            if (p.conclusion.isNotBlank()) {
                Text("Conclusion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.positive)
                Text(p.conclusion, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Linked entity counts
            Text("Connected records", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetCountChip("Observations", obsCount, FieldMindIcons.Observation, colors.observation)
                AssetCountChip("Questions", qCount, FieldMindIcons.Question, colors.question)
                AssetCountChip("Sources", srcCount, FieldMindIcons.Source, colors.source)
                AssetCountChip("Data", dataCount, FieldMindIcons.Data, colors.data)
                AssetCountChip("Reports", repCount, FieldMindIcons.Report, colors.report)
            }

            // Future questions
            if (p.futureQuestions.isNotBlank()) {
                Text("Future questions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.question)
                Text(p.futureQuestions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DataRecordDetailContent(
    d: DataRecordEntity
) {
    val colors = FieldMindTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.data.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Data, null, tint = colors.data, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(d.label.ifBlank { "Data record" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    InfoChip(d.toolType, icon = FieldMindIcons.Data)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Value display (prominent)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${d.value} ${d.unit}".trim(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.data
                    )
                    Text(d.toolType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Location
            if (d.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FieldMindIcons.Location, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                    Text(d.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Notes
            if (d.notes.isNotBlank()) {
                Text("Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(d.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReportDetailContent(
    r: ReportEntity
) {
    val colors = FieldMindTheme.colors
    val statusColor = when (r.status.lowercase()) {
        "published" -> colors.positive
        "draft" -> colors.warning
        "archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> colors.report
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.report.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Report, null, tint = colors.report, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text(r.title.ifBlank { "Report" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(r.type, icon = FieldMindIcons.Report)
                        StatusChip(r.status, statusColor)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Research question
            if (r.question.isNotBlank()) {
                Text("Research question", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.question)
                Text(r.question, style = MaterialTheme.typography.bodyMedium)
            }

            // Conclusion
            if (r.conclusion.isNotBlank()) {
                Text("Conclusion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.positive)
                Text(r.conclusion, style = MaterialTheme.typography.bodyMedium)
            }

            // Full report preview
            Text("Full report", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    FieldMindExport.buildMarkdownReport(r),
                    Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FlashcardDetailContent(
    f: FlashcardEntity
) {
    val colors = FieldMindTheme.colors

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.flashcard.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Flashcard, null, tint = colors.flashcard, size = 26.dp) }
                Column(Modifier.weight(1f)) {
                    Text("Flashcard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    InfoChip(f.type, icon = FieldMindIcons.Flashcard)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Front (question)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.flashcard.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Front", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.flashcard)
                    Text(f.front.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }

            // Back (answer)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.info.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Back", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.info)
                    Text(f.back.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
    }
}

@Composable
private fun AssetCountChip(label: String, count: Int, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, size = 14.dp)
        Text("$count $label", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusChip(status: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(4.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Action Bar
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun DetailActionBar(onEdit: () -> Unit, onDelete: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(onClick = { haptics.light(); onEdit() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
            Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Edit")
        }
        OutlinedButton(
            onClick = { haptics.light(); onDelete() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(icon = FieldMindIcons.Delete, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Delete")
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared composables (preserved from original)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuestionAnswerCard(question: QuestionEntity, onSave: (String) -> Unit) {
    var editing by remember(question.id, question.answer) { mutableStateOf(question.answer.isBlank()) }
    var draft by remember(question.id) { mutableStateOf(question.answer) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Answer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (editing) {
                FieldTextField(draft, { draft = it }, "What did you conclude?", minLines = 3, supportingText = "Saving marks this question as Answered.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(draft); editing = false }, shape = RoundedCornerShape(14.dp), enabled = draft.isNotBlank()) { Text("Save answer") }
                    if (question.answer.isNotBlank()) TextButton(onClick = { draft = question.answer; editing = false }) { Text("Cancel") }
                }
            } else {
                Text(question.answer, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { editing = true }, contentPadding = PaddingValues(0.dp)) {
                    Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 16.dp); Spacer(Modifier.size(6.dp)); Text("Edit answer")
                }
            }
        }
    }
}

private fun sharePlainText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share FieldMind record"))
}

@Composable
private fun ConfirmDeleteDialog(kind: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon = FieldMindIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete $kind?") },
        text = { Text("This removes the $kind from your active records. This can't be undone from the app.") },
        confirmButton = { Button(onClick = { haptics.confirm(); onConfirm() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun deleteEntityByKind(kind: String, id: Long, viewModel: FieldMindViewModel) {
    when (kind) {
        "observation" -> viewModel.deleteObservation(id)
        "note" -> viewModel.deleteNote(id)
        "question" -> viewModel.deleteQuestion(id)
        "hypothesis" -> viewModel.deleteHypothesis(id)
        "project" -> viewModel.deleteProject(id)
        "source" -> viewModel.deleteSource(id)
        "data" -> viewModel.deleteDataRecord(id)
        "report" -> viewModel.deleteReport(id)
        "flashcard" -> viewModel.deleteFlashcard(id)
    }
}

@Composable
private fun DetailBody(title: String, kind: String, fields: List<Pair<String, String>>) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EntityBadge(kind)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            fields.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SourceActionPanel(source: SourceEntity, projects: List<ProjectEntity>, viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = rememberFieldMindHaptics()
    var showProjects by remember { mutableStateOf(false) }
    val citation = remember(source) { viewModel.buildSourceCitation(source) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Source actions", "Open, cite, prioritize, and turn reading into review cards.")
            SourcePreviewCard(source.link, source.fileUri)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { haptics.light(); runCatching { uriHandler.openUri(source.link) } }, enabled = source.link.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.OpenLink, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open link")
                }
                FilledTonalButton(onClick = {
                    haptics.light()
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.fileUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                }, enabled = source.fileUri.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.File, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open file")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.light(); clipboard.setText(AnnotatedString(citation)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Article, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Copy citation")
                }
                OutlinedButton(onClick = { haptics.confirm(); viewModel.toggleSourceImportant(source) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Favorite, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text(if (source.importance == "Normal") "Important" else "Normal")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.confirm(); viewModel.markSourceRead(source) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), enabled = source.readingStatus != "Read") {
                    Icon(FieldMindIcons.Done, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Mark read")
                }
                OutlinedButton(onClick = {
                    haptics.confirm()
                    if (source.title.isNotBlank() && source.whatThisSourceTaughtMe.isNotBlank()) viewModel.addFlashcard(source.title, source.whatThisSourceTaughtMe, "source-takeaway", source.id, source.relatedProjectId)
                    if (source.keyFindings.isNotBlank()) viewModel.addFlashcard("Key finding from ${source.title}", source.keyFindings, "source-finding", source.id, source.relatedProjectId)
                    if (source.questionsGenerated.isNotBlank()) viewModel.addFlashcard("Question from ${source.title}", source.questionsGenerated, "source-question", source.id, source.relatedProjectId)
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Flashcard, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Smart cards")
                }
            }
            TextButton(onClick = { haptics.light(); showProjects = !showProjects }) {
                Icon(FieldMindIcons.Project, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Link to project")
            }
            AnimatedVisibility(showProjects) {
                ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == source.relatedProjectId }?.title ?: "No project") { selected ->
                    haptics.confirm(); viewModel.linkSourceToProject(source, projects.firstOrNull { it.title == selected }?.id); showProjects = false
                }
            }
            if (source.relatedProjectId != null) {
                projects.firstOrNull { it.id == source.relatedProjectId }?.let { project ->
                    EntityCard(project.title, "project", body = project.objective.ifBlank { project.researchQuestion }, meta = listOf("Linked project")) { onOpenDetail("project", project.id) }
                }
            }
        }
    }
}

@Composable
private fun ObservationAttachmentsPanel(viewModel: FieldMindViewModel, observationId: Long, onOpenReader: (String, String) -> Unit = { _, _ -> }) {
    val attachments by viewModel.attachmentsForObservation(observationId).collectAsState(initial = emptyList())
    if (attachments.isEmpty()) return
    val images = attachments.filter { it.type.equals("Photo", true) || it.type.equals("Gallery", true) || it.uri.contains(Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)", RegexOption.IGNORE_CASE)) }
    val others = attachments - images.toSet()
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Gallery, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Evidence (${attachments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(images) { img ->
                        val displayUri = img.localPath ?: img.uri
                        Column(Modifier.width(140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AsyncImage(
                                model = displayUri,
                                contentDescription = img.caption.ifBlank { "Observation photo" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { onOpenReader(displayUri, img.caption.ifBlank { "Observation image" }) }
                            )
                            if (img.caption.isNotBlank()) Text(img.caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            others.forEach { att ->
                val displayUri = att.localPath ?: att.uri
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenReader(displayUri, att.caption.ifBlank { att.type }) }.padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                        Icon(icon = if (att.type.equals("Audio", true)) FieldMindIcons.Mic else FieldMindIcons.File, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(att.caption.ifBlank { if (att.type.equals("Audio", true)) "Audio evidence" else "Attached evidence" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(att.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePreviewPanel(source: SourceEntity, onOpenReader: (String, String) -> Unit) {
    val target = source.fileUri.ifBlank { source.link }
    if (target.isBlank()) return
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Source preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (source.type.equals("Image", true) || uriLooksImage(target)) {
                AsyncImage(model = target, contentDescription = source.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { onOpenReader(target, source.title) })
            }
            AttachmentOpenRow(target, source.title, source.type, onOpenReader)
        }
    }
}

@Composable
private fun AttachmentOpenRow(uri: String, title: String, type: String, onOpenReader: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenReader(uri, title) }.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon = if (type.equals("Image", true) || type.equals("Gallery", true) || type.equals("Photo", true) || uriLooksImage(uri)) FieldMindIcons.Gallery else FieldMindIcons.File, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(icon = FieldMindIcons.OpenLink, contentDescription = null, size = 18.dp)
    }
}

@Composable
private fun BacklinksPanel(links: List<Triple<String, String, Long>>, onOpenDetail: (String, Long) -> Unit) {
    if (links.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon = FieldMindIcons.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            Text("Linked records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${links.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        links.forEach { (lkKind, lkTitle, lkId) ->
            EntityCard(lkTitle, lkKind, onClick = { onOpenDetail(lkKind, lkId) })
        }
    }
}

@Composable
private fun InlineEditNote(entity: NoteEntity, viewModel: FieldMindViewModel, onDone: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }
    var body by remember { mutableStateOf(entity.body) }
    var category by remember { mutableStateOf(entity.category) }
    var tags by remember { mutableStateOf(entity.tags) }
    var attachments by remember { mutableStateOf(entity.attachmentUris) }
    InlineFormCard("Edit Note", onDismiss = onDone, onSave = {
        if (title.isNotBlank() || body.isNotBlank()) {
            viewModel.updateNoteEntity(entity.copy(title = title.trim().ifBlank { body.take(36) }, body = body.trim(), category = category, tags = tags.trim(), attachmentUris = attachments.trim())); onDone()
        }
    }, saveEnabled = title.isNotBlank() || body.isNotBlank()) {
        FormChoice("Category", observationCategories, category) { category = it }
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(body, { body = it }, "Body", minLines = 5)
        FieldTextField(tags, { tags = it }, "Tags")
        FieldTextField(attachments, { attachments = it }, "Attachments", minLines = 2, supportingText = "One stored attachment per line: type|caption|uri")
    }
}

@Composable
private fun InlineEditObservation(entity: ObservationEntity, viewModel: FieldMindViewModel, onDone: () -> Unit) {
    val appContext = LocalContext.current
    var subject by remember { mutableStateOf(entity.subject) }
    var category by remember { mutableStateOf(entity.category) }
    var facts by remember { mutableStateOf(entity.factsOnlyNotes) }
    var confidence by remember { mutableStateOf(entity.confidenceLevel) }
    var location by remember { mutableStateOf(entity.manualLocation) }
    var latitude by remember { mutableStateOf(entity.latitude?.toString().orEmpty()) }
    var longitude by remember { mutableStateOf(entity.longitude?.toString().orEmpty()) }
    var tags by remember { mutableStateOf(entity.tags) }
    var evidence by remember { mutableStateOf(entity.evidenceSummary) }
    var fieldContext by remember { mutableStateOf(entity.moodOrContext) }
    var locating by remember { mutableStateOf(false) }
    val locationProvider = remember { FieldLocationProvider(appContext) }
    fun startLocating() {
        locating = true
        locationProvider.requestCurrentLocation { captured ->
            locating = false
            if (captured != null) {
                latitude = captured.latitude.toString(); longitude = captured.longitude.toString()
                location = captured.asDisplayText()
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place -> if (!place.isNullOrBlank()) location = captured.copy(placeName = place).asDisplayText() }
            }
        }
    }
    InlineFormCard("Edit Observation", onDismiss = onDone, onSave = {
        if (subject.isNotBlank()) {
            viewModel.updateObservation(entity.copy(subject = subject.trim(), category = category, factsOnlyNotes = facts.trim(), confidenceLevel = confidence, manualLocation = location.trim(), latitude = latitude.toDoubleOrNull(), longitude = longitude.toDoubleOrNull(), evidenceSummary = evidence.trim(), moodOrContext = fieldContext.trim()), tags)
            onDone()
        }
    }, saveEnabled = subject.isNotBlank()) {
        CaptureStep("Identity", "Keep the subject and category easy to scan later.", FieldMindIcons.Observation) {
            FieldTextField(subject, { subject = it }, "Subject")
            ChoiceChips(observationCategories, category) { category = it }
            ChoiceChips(confidenceOptions, confidence) { confidence = it }
        }
        CaptureStep("Facts & context", "Separate facts from mood, context, or field conditions.", FieldMindIcons.Edit) {
            FieldTextField(facts, { facts = it }, "Facts only", minLines = 3)
            ChoiceChips(contextPresets, fieldContext) { fieldContext = if (fieldContext.isBlank()) it else "$fieldContext, $it" }
            FieldTextField(fieldContext, { fieldContext = it }, "Context / mood", minLines = 2)
        }
        CaptureStep("GPS", "Use automatic GPS or repair coordinates manually.", FieldMindIcons.Location) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { if (locationProvider.hasAnyLocationPermission()) startLocating() else appContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }, modifier = Modifier.weight(1f), enabled = !locating) {
                    if (locating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.size(6.dp)); Text("Locating…") } else { Icon(FieldMindIcons.Location, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Use GPS") }
                }
                OutlinedButton(onClick = { latitude = ""; longitude = ""; location = "" }, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
            FieldTextField(location, { location = it }, "Location")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(latitude, { latitude = it }, "Latitude", modifier = Modifier.weight(1f))
                FieldTextField(longitude, { longitude = it }, "Longitude", modifier = Modifier.weight(1f))
            }
        }
        FieldTextField(tags, { tags = it }, "Tags (comma separated)")
        FieldTextField(evidence, { evidence = it }, "Evidence summary", minLines = 2)
    }
}
