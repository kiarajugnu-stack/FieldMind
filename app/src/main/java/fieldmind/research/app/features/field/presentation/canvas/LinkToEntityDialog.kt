package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.*

/**
 * Entity type tabs for the link-to-entity picker.
 */
private enum class EntityTab(
    val label: String,
    val entityType: String,
    val icon: MaterialSymbolIcon,
    val accent: Color
) {
    Observations("Observations", "observation", MaterialSymbolIcon("visibility"), Color(0xFF5F7F52)),
    Notes("Notes", "note", MaterialSymbolIcon("note"), Color(0xFF7F6B52)),
    Questions("Questions", "question", MaterialSymbolIcon("help"), Color(0xFF527F7F)),
    Sources("Sources", "source", MaterialSymbolIcon("book"), Color(0xFF7F527F)),
    Projects("Projects", "project", MaterialSymbolIcon("folder"), Color(0xFF527F5F))
}

/**
 * A searchable entity picker dialog that shows Observations, Notes, Questions,
 * Sources, and Projects in tabbed categories. The user can search, browse, and
 * tap an entity to link it to a canvas block.
 *
 * @param blockId the canvas block to link
 * @param viewModel the app-level ViewModel providing entity lists
 * @param canvasViewModel the canvas ViewModel for performing the link
 * @param onLinked optional callback fired after a successful link with (entityType, entityId, entityName)
 * @param onDismiss called to close the dialog
 */
@Composable
fun LinkToEntityDialog(
    blockId: Long,
    viewModel: FieldMindViewModel,
    canvasViewModel: CanvasViewModel,
    onLinked: ((entityType: String, entityId: Long, entityName: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // ── State ──
    var selectedTab by remember { mutableStateOf(EntityTab.Observations) }
    var searchQuery by remember { mutableStateOf("") }
    var linkedEntityName by remember { mutableStateOf<String?>(null) }

    // Collect entity lists from FieldMindViewModel
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()

    // ── Filter entities by search query ──
    val filteredObservations = remember(observations, searchQuery) {
        if (searchQuery.isBlank()) observations
        else observations.filter { it.subject.contains(searchQuery, ignoreCase = true) }
    }
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    val filteredQuestions = remember(questions, searchQuery) {
        if (searchQuery.isBlank()) questions
        else questions.filter { it.questionText.contains(searchQuery, ignoreCase = true) }
    }
    val filteredSources = remember(sources, searchQuery) {
        if (searchQuery.isBlank()) sources
        else sources.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // Determine the current list
    val currentList: List<Any> = when (selectedTab) {
        EntityTab.Observations -> filteredObservations
        EntityTab.Notes -> filteredNotes
        EntityTab.Questions -> filteredQuestions
        EntityTab.Sources -> filteredSources
        EntityTab.Projects -> filteredProjects
    }

    // ── Date formatter for display ──
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // ── Success snackbar ──
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(linkedEntityName) {
        linkedEntityName?.let { name ->
            snackbarHostState.showSnackbar(
                message = "Linked to \"$name\"",
                duration = SnackbarDuration.Short
            )
            linkedEntityName = null
        }
    }

    // ── UI ──
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.fillMaxSize()) {
                    // ── Header ──
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp)
                        ) {
                            // Title + close button row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                MaterialSymbolIcon("close"),
                                                "Close",
                                                size = 20.dp
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Link to Entity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        selectedTab.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    selectedTab.icon,
                                    null,
                                    size = 24.dp,
                                    tint = selectedTab.accent
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // ── Search bar ──
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                placeholder = {
                                    Text(
                                        "Search ${selectedTab.label.lowercase()}…",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        MaterialSymbolIcon("search"),
                                        "Search",
                                        size = 20.dp,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                MaterialSymbolIcon("close"),
                                                "Clear",
                                                size = 18.dp
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            // ── Category tabs ──
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EntityTab.entries.forEach { tab ->
                                    val isSelected = tab == selectedTab
                                    Surface(
                                        onClick = { selectedTab = tab; searchQuery = "" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) tab.accent.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                tab.icon,
                                                null,
                                                size = 14.dp,
                                                tint = if (isSelected) tab.accent
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                tab.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) tab.accent
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Entity list ──
                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    selectedTab.icon,
                                    null,
                                    size = 48.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Text(
                                    if (searchQuery.isNotBlank()) "No matching ${selectedTab.label.lowercase()}"
                                    else "No ${selectedTab.label.lowercase()} yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                if (searchQuery.isBlank()) {
                                    Text(
                                        "Create one to link it here",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = currentList,
                                key = { entity ->
                                    when (entity) {
                                        is ObservationEntity -> "obs_${entity.id}"
                                        is NoteEntity -> "note_${entity.id}"
                                        is QuestionEntity -> "q_${entity.id}"
                                        is SourceEntity -> "src_${entity.id}"
                                        is ProjectEntity -> "proj_${entity.id}"
                                        else -> entity.hashCode().toString()
                                    }
                                }
                            ) { entity ->
                                EntityListItem(
                                    entity = entity,
                                    tab = selectedTab,
                                    dateFormat = dateFormat,                                        onLink = {
                                        val (eType, eId, eName) = when (entity) {
                                            is ObservationEntity -> {
                                                canvasViewModel.linkBlockToEntity(blockId, "observation", entity.id)
                                                Triple("observation", entity.id, entity.subject)
                                            }
                                            is NoteEntity -> {
                                                canvasViewModel.linkBlockToEntity(blockId, "note", entity.id)
                                                Triple("note", entity.id, entity.title)
                                            }
                                            is QuestionEntity -> {
                                                canvasViewModel.linkBlockToEntity(blockId, "question", entity.id)
                                                Triple("question", entity.id, entity.questionText)
                                            }
                                            is SourceEntity -> {
                                                canvasViewModel.linkBlockToEntity(blockId, "source", entity.id)
                                                Triple("source", entity.id, entity.title)
                                            }
                                            is ProjectEntity -> {
                                                canvasViewModel.linkBlockToEntity(blockId, "project", entity.id)
                                                Triple("project", entity.id, entity.title)
                                            }
                                            else -> Triple("", 0L, "")
                                        }
                                        linkedEntityName = eName
                                        onLinked?.invoke(eType, eId, eName)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity List Item
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun EntityListItem(
    entity: Any,
    tab: EntityTab,
    dateFormat: SimpleDateFormat,
    onLink: () -> Unit
) {
    val (primaryText, secondaryText, accentColor, icon) = when (entity) {
        is ObservationEntity -> Triple(
            entity.subject,
            buildString {
                append(entity.category)
                append(" · ")
                append(dateFormat.format(Date(entity.timestamp)))
            },
            tab.accent,
            MaterialSymbolIcon("visibility")
        )
        is NoteEntity -> Triple(
            entity.title.ifBlank { "Untitled note" },
            entity.category,
            tab.accent,
            MaterialSymbolIcon("note")
        )
        is QuestionEntity -> Triple(
            entity.questionText,
            buildString {
                append(entity.category)
                append(" · ")
                append(entity.status)
            },
            tab.accent,
            MaterialSymbolIcon("help")
        )
        is SourceEntity -> Triple(
            entity.title,
            buildString {
                append(entity.type)
                if (entity.author.isNotBlank()) {
                    append(" · ")
                    append(entity.author)
                }
            },
            tab.accent,
            MaterialSymbolIcon("book")
        )
        is ProjectEntity -> Triple(
            entity.title,
            entity.topicType,
            tab.accent,
            MaterialSymbolIcon("folder")
        )
        else -> Triple("Unknown", "", tab.accent, MaterialSymbolIcon("help"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLink),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    size = 20.dp,
                    tint = accentColor
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    primaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (secondaryText.isNotBlank()) {
                    Text(
                        secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Link button
            Surface(
                onClick = onLink,
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("link"),
                        null,
                        size = 14.dp,
                        tint = accentColor
                    )
                    Text(
                        "Link",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
