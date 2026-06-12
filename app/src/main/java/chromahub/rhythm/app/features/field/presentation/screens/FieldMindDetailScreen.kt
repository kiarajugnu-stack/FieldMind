package chromahub.rhythm.app.features.field.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import chromahub.rhythm.app.features.field.data.ai.AiProvider
import chromahub.rhythm.app.features.field.data.ai.AssistantTask
import chromahub.rhythm.app.features.field.data.ai.GeminiResearchAssistant
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import chromahub.rhythm.app.features.field.data.export.FieldMindExport
import chromahub.rhythm.app.features.field.data.location.FieldLocationProvider
import java.util.Locale
import androidx.compose.foundation.lazy.LazyRow
// ══════════════════════════════════════════════════════════════════════
//  Detail + backlinks
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DetailScreen(kind: String, id: Long, viewModel: FieldMindViewModel, onBack: () -> Unit, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, onOpenReader: (String, String) -> Unit = { _, _ -> }) {
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldScreenHeader(title, "Record detail workspace", icon = FieldMindIcons.iconFor(kind), actionIcon = FieldMindIcons.Back, onAction = onBack) }
        if (editable && !showEdit) item { DetailActions(onEdit = { showEdit = true }, onDelete = { showDelete = true }) }
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
                    item { DetailBody(n.title, "note", listOf("Category" to n.category, "Tags" to n.tags, "Body" to n.body, "Updated" to recentRelativeTime(n.updatedAt))) }
                    item { NoteAttachmentsPanel(n.attachmentUris, onOpenReader) }
                    item { BacklinksPanel(buildList {
                        projects.firstOrNull { it.id == n.projectId }?.let { add(Triple("project", it.title, it.id)) }
                        sources.firstOrNull { it.id == n.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                    }, onOpenDetail) }
                }
                "observation" -> observations.firstOrNull { it.id == id }?.let { o ->
                    item {
                        ObservationReaderContent(
                            observation = o,
                            onAttachments = { ObservationAttachmentsPanel(viewModel, o.id, onOpenReader) },
                            onMap = {
                                if (o.latitude != null && o.longitude != null) {
                                    ObservationLocationCard(o.latitude, o.longitude, o.manualLocation)
                                }
                            }
                        )
                    }
                    item { BacklinksPanel(buildList {
                        projects.firstOrNull { it.id == o.projectId }?.let { add(Triple("project", it.title, it.id)) }
                        data.filter { it.observationId == o.id }.forEach { add(Triple("data", it.label, it.id)) }
                    }, onOpenDetail) }
                }
                "question" -> questions.firstOrNull { it.id == id }?.let { qn ->
                    item { DetailBody(qn.questionText, "question", listOf("Category" to qn.category, "Source" to qn.sourceType, "Status" to qn.status, "Priority" to qn.priority)) }
                    item { QuestionAnswerCard(qn) { ans -> viewModel.setQuestionAnswer(qn, ans) } }
                    item { BacklinksPanel(buildList {
                        projects.firstOrNull { it.id == qn.relatedProjectId }?.let { add(Triple("project", it.title, it.id)) }
                        hypotheses.filter { it.linkedQuestionId == qn.id }.forEach { add(Triple("hypothesis", it.prediction, it.id)) }
                    }, onOpenDetail) }
                }
                "hypothesis" -> hypotheses.firstOrNull { it.id == id }?.let { h ->
                    item { DetailBody(h.prediction, "hypothesis", listOf("Reasoning" to h.reasoning, "Support" to h.supportCriteria, "Weaken" to h.weakeningCriteria, "Test" to h.testMethod, "Result" to h.resultStatus, "Confidence" to "${h.confidencePercent}%")) }
                    item { BacklinksPanel(buildList {
                        questions.firstOrNull { it.id == h.linkedQuestionId }?.let { add(Triple("question", it.questionText, it.id)) }
                    }, onOpenDetail) }
                }
                "project" -> projects.firstOrNull { it.id == id }?.let { p ->
                    item { DetailBody(p.title, "project", listOf("Objective" to p.objective, "Question" to p.researchQuestion, "Background" to p.backgroundNotes, "Methods" to p.methods, "Data" to p.dataSummary, "Analysis" to p.analysis, "Conclusion" to p.conclusion, "Future" to p.futureQuestions)) }
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
                    item { DetailBody(d.label, "data", listOf("Tool" to d.toolType, "Value" to "${d.value} ${d.unit}".trim(), "Location" to d.location, "Notes" to d.notes)) }
                    item { BacklinksPanel(buildList {
                        projects.firstOrNull { it.id == d.projectId }?.let { add(Triple("project", it.title, it.id)) }
                        observations.firstOrNull { it.id == d.observationId }?.let { add(Triple("observation", it.subject, it.id)) }
                    }, onOpenDetail) }
                }
                "report" -> reports.firstOrNull { it.id == id }?.let { r ->
                    item { DetailBody(r.title, "report", listOf("Type" to r.type, "Status" to r.status, "Question" to r.question, "Conclusion" to r.conclusion)) }
                    item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Text(FieldMindExport.buildMarkdownReport(r), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) } }
                    item { BacklinksPanel(buildList { projects.firstOrNull { it.id == r.projectId }?.let { add(Triple("project", it.title, it.id)) } }, onOpenDetail) }
                }
                "flashcard" -> flashcards.firstOrNull { it.id == id }?.let { f ->
                    item { DetailBody(f.front, "flashcard", listOf("Type" to f.type, "Back" to f.back)) }
                    item { BacklinksPanel(buildList {
                        sources.firstOrNull { it.id == f.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                        projects.firstOrNull { it.id == f.projectId }?.let { add(Triple("project", it.title, it.id)) }
                    }, onOpenDetail) }
                }
            }
        }
    }
    if (showEdit && kind !in setOf("note", "observation")) EditEntityDialog(kind, id, viewModel) { showEdit = false }
    if (showDelete) ConfirmDeleteDialog(kind, onDismiss = { showDelete = false }) {
        deleteEntityByKind(kind, id, viewModel); showDelete = false; onBack()
    }
}

@Composable
private fun DetailActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

/** Inline answer editor on a question detail screen. */
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
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.fileUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }
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
                    haptics.confirm()
                    viewModel.linkSourceToProject(source, projects.firstOrNull { it.title == selected }?.id)
                    showProjects = false
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
private fun InlineEditNote(entity: NoteEntity, viewModel: FieldMindViewModel, onDone: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }; var body by remember { mutableStateOf(entity.body) }
    var category by remember { mutableStateOf(entity.category) }; var tags by remember { mutableStateOf(entity.tags) }
    var attachments by remember { mutableStateOf(entity.attachmentUris) }
    InlineFormCard("Edit Note", onDismiss = onDone, onSave = {
        if (title.isNotBlank() || body.isNotBlank()) { viewModel.updateNoteEntity(entity.copy(title = title.trim().ifBlank { body.take(36) }, body = body.trim(), category = category, tags = tags.trim(), attachmentUris = attachments.trim())); onDone() }
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
                FilledTonalButton(
                    onClick = { if (locationProvider.hasAnyLocationPermission()) startLocating()
                        else appContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    modifier = Modifier.weight(1f), enabled = !locating
                ) {
                    if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(FieldMindIcons.Location, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp)); Text(if (locating) "Locating…" else "Use GPS")
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

/** Evidence gallery for a saved observation: image thumbnails plus file/audio chips. */
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
                        Text(att.caption.ifBlank { att.type }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(displayUri, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun storedAttachmentRows(raw: String): List<DraftEvidenceAttachment> = raw.lines().mapNotNull { line ->
    val parts = line.split("|")
    when {
        parts.size >= 3 -> DraftEvidenceAttachment(parts[0], parts.drop(2).joinToString("|"), parts[1])
        line.isNotBlank() -> DraftEvidenceAttachment("File", line, "Attachment")
        else -> null
    }
}

@Composable
private fun NoteAttachmentsPanel(raw: String, onOpenReader: (String, String) -> Unit) {
    val attachments = remember(raw) { storedAttachmentRows(raw) }
    if (attachments.isEmpty()) return
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            attachments.forEach { att -> AttachmentOpenRow(att.uri, att.caption.ifBlank { att.type }, att.type, onOpenReader) }
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
                AsyncImage(
                    model = target,
                    contentDescription = source.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { onOpenReader(target, source.title) }
                )
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
internal fun AssistantPanel(viewModel: FieldMindViewModel, seedText: String = "") {
    val enabled by viewModel.fieldSettings.geminiEnabled.collectAsState()
    if (!enabled) return
    val providerName by viewModel.fieldSettings.aiProvider.collectAsState()
    val geminiKey by viewModel.fieldSettings.geminiApiKey.collectAsState()
    val geminiModel by viewModel.fieldSettings.geminiModel.collectAsState()
    val openAiKey by viewModel.fieldSettings.openAiApiKey.collectAsState()
    val openAiModel by viewModel.fieldSettings.openAiModel.collectAsState()
    val provider = if (providerName == "OpenAI") AiProvider.OPENAI else AiProvider.GEMINI
    val key = if (provider == AiProvider.OPENAI) openAiKey else geminiKey
    val model = if (provider == AiProvider.OPENAI) openAiModel else geminiModel
    val assistant = remember(enabled, provider, key, model) {
        GeminiResearchAssistant(enabled = enabled, provider = provider, apiKeyProvider = { key }, modelProvider = { model })
    }
    val available = assistant.isAvailable()
    val scope = rememberCoroutineScope()
    var input by remember(seedText) { mutableStateOf(seedText) }
    var selectedTask by remember { mutableStateOf(AssistantTask.PAPER_BOOK_SUGGESTIONS) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var assistantError by remember { mutableStateOf<String?>(null) }

    val tasks = listOf(
        AssistantTask.PAPER_BOOK_SUGGESTIONS to FieldMindIcons.Book,
        AssistantTask.FACTUALITY to FieldMindIcons.Check,
        AssistantTask.TESTABILITY to FieldMindIcons.Question,
        AssistantTask.HYPOTHESIS to FieldMindIcons.Hypothesis,
        AssistantTask.NEXT_STEPS to FieldMindIcons.Trend,
        AssistantTask.WRITING to FieldMindIcons.Edit
    )

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.Sparkle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 20.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("${assistant.providerLabel()} research assistant", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (available) "Helps you think — never invents evidence." else "Disabled — enable it in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ChoiceChips(tasks.map { it.first.title }, selectedTask.title) { title -> tasks.firstOrNull { it.first.title == title }?.let { selectedTask = it.first } }

            FieldTextField(
                input, { input = it },
                if (selectedTask == AssistantTask.PAPER_BOOK_SUGGESTIONS) "Topic for papers & books" else "Text to review",
                minLines = 2,
                supportingText = "Stays on device until you press Ask ${assistant.providerLabel()}."
            )

            Button(
                onClick = {
                    if (!available) return@Button
                    loading = true; result = null; assistantError = null
                    scope.launch {
                        runCatching { assistant.generateContent(selectedTask, input.trim()) }
                            .onSuccess { suggestion ->
                                if (suggestion.body.startsWith("AI request failed", ignoreCase = true)) assistantError = suggestion.body else result = suggestion.body
                            }
                            .onFailure { assistantError = it.localizedMessage ?: "The optional assistant could not complete this draft." }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                enabled = available && !loading && (selectedTask == AssistantTask.NEXT_STEPS || input.isNotBlank())
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp)); Text("Asking ${assistant.providerLabel()}…")
                } else {
                    Icon(icon = FieldMindIcons.Send, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Ask ${assistant.providerLabel()}")
                }
            }

            if (!available) {
                Text("Add an API key in Settings → AI assistant to enable live suggestions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            assistantError?.let { text ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Optional assistant unavailable", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Your FieldMind data remains local. You can keep working offline or retry later.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f))
                }
            }

            result?.let { text ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(selectedTask.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    Text("Draft only. Verify any titles, authors, or links before trusting them.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

