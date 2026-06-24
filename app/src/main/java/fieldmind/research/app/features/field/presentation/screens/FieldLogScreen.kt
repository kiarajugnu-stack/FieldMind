package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

private enum class TimelineViewMode { List, Gallery }

/**
 * Full-screen dedicated Field Log — a chronological field journal of all observations.
 *
 * Features:
 * - List view with full observation cards
 * - Gallery view with photo grid
 * - Search bar with real-time filtering
 * - Advanced filter sheet with 15+ dimensions
 * - Sort by date (newest/oldest), confidence, category
 * - Quick stats header
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldLogScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenExport: () -> Unit = {}
) {
    val observations by viewModel.observations.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val sessionCrossRefs by viewModel.sessionObservationCrossRefs.collectAsState()

    var viewMode by remember { mutableStateOf(TimelineViewMode.List) }
    var showSearch by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterState by rememberSaveable(stateSaver = ObservationFilterStateSaver) { mutableStateOf(ObservationFilterState()) }
    var showSessionGroups by remember { mutableStateOf(true) }
    var expandedSessions by remember { mutableStateOf(setOf<Long>()) }

    // Build session → observation mapping
    val sessionObsMap = remember(observations, sessionCrossRefs, researchSessions) {
        val obsIdToObs = observations.associateBy { it.id }
        val sessionIdToName = researchSessions.associate { it.id to it.name.ifBlank { "Session #${it.id}" } }
        val map = mutableMapOf<Long, MutableList<ObservationEntity>>()
        sessionCrossRefs.forEach { ref ->
            obsIdToObs[ref.observationId]?.let { obs ->
                map.getOrPut(ref.sessionId) { mutableListOf() }.add(obs)
            }
        }
        // Sort each session's observations newest first
        map.forEach { (_, obsList) -> obsList.sortByDescending { it.timestamp } }
        map
    }

    // Track which observations belong to a session
    val sessionObsIds = remember(sessionCrossRefs) {
        sessionCrossRefs.mapTo(hashSetOf()) { it.observationId }
    }

    // Filter & sort
    val filteredObservations = remember(observations, filterState) {
        var filtered = observations
        if (filterState.query.isNotBlank()) {
            val q = filterState.query.lowercase()
            filtered = filtered.filter {
                it.subject.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.factsOnlyNotes.lowercase().contains(q) ||
                it.manualLocation.lowercase().contains(q) ||
                it.tags.lowercase().contains(q)
            }
        }
        if (filterState.category.isNotBlank()) filtered = filtered.filter { it.category == filterState.category }
        if (filterState.confidence.isNotBlank()) filtered = filtered.filter { it.confidenceLevel == filterState.confidence }
        if (filterState.tags.isNotBlank()) {
            val tagSet = filterState.tags.split(",").map { it.trim().lowercase() }.toSet()
            filtered = filtered.filter { it.tags.split(",").any { t -> t.trim().lowercase() in tagSet } }
        }
        if (filterState.projectId != null) filtered = filtered.filter { it.projectId == filterState.projectId }
        if (filterState.draftsOnly) filtered = filtered.filter { it.status == "Draft" }

        when (filterState.sortBy) {
            "Date (oldest)" -> filtered.sortedBy { it.timestamp }
            "Confidence" -> filtered.sortedByDescending { it.confidenceLevel }
            "Category" -> filtered.sortedBy { it.category }
            else -> filtered.sortedByDescending { it.timestamp }
        }
    }

    // Build session-grouped display items
    data class DisplayItem(val type: String, val sessionId: Long = 0L, val sessionName: String = "", val obsCount: Int = 0, val observation: ObservationEntity? = null)

    val displayItems = remember(filteredObservations, sessionObsMap, sessionObsIds, showSessionGroups, expandedSessions, researchSessions) {
        if (!showSessionGroups) {
            filteredObservations.map { DisplayItem("obs", observation = it) }
        } else {
            val sessionIdToName = researchSessions.associate { it.id to it.name.ifBlank { "Session #${it.id}" } }
            val items = mutableListOf<DisplayItem>()
            val usedObsIds = mutableSetOf<Long>()

            // Sort sessions by most recent observation
            val sortedSessions = sessionObsMap.entries
                .filter { (_, obsList) -> obsList.any { it.id in filteredObservations.map { f -> f.id }.toSet() } }
                .sortedByDescending { (_, obsList) -> obsList.maxOfOrNull { it.timestamp } ?: 0L }

            for ((sessionId, obsList) in sortedSessions) {
                val sessionObs = obsList.filter { it.id in filteredObservations.map { f -> f.id }.toSet() }
                if (sessionObs.isEmpty()) continue
                val name = sessionIdToName[sessionId] ?: "Session #$sessionId"
                items.add(DisplayItem("sessionHeader", sessionId = sessionId, sessionName = name, obsCount = sessionObs.size))
                if (expandedSessions.contains(sessionId)) {
                    sessionObs.forEach { obs ->
                        items.add(DisplayItem("obs", observation = obs))
                        usedObsIds.add(obs.id)
                    }
                } else {
                    // Show just the first observation as preview
                    items.add(DisplayItem("obs", observation = sessionObs.first()))
                    usedObsIds.add(sessionObs.first().id)
                    if (sessionObs.size > 1) {
                        items.add(DisplayItem("sessionExpand", sessionId = sessionId, obsCount = sessionObs.size - 1))
                    }
                }
            }

            // Ungrouped observations
            val ungrouped = filteredObservations.filter { it.id !in usedObsIds && it.id !in sessionObsIds }
            if (ungrouped.isNotEmpty()) {
                items.add(DisplayItem("ungroupedHeader", obsCount = ungrouped.size))
                ungrouped.forEach { items.add(DisplayItem("obs", observation = it)) }
            }

            items
        }
    }

    val refreshScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.List, null, tint = FieldMindTheme.colors.observation, size = 18.dp)
                        }
                        Text("Field Log", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FieldMindIcons.Back, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                refreshScope.launch {
                    // Simulate refresh — in production this would re-fetch data
                    kotlinx.coroutines.delay(600)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // ── Quick stats ──
            item { ObservationStatsDashboard(observations) }

            // ── Past research sessions ──
            val completedSessions = researchSessions.filter { it.status == "Completed" }.sortedByDescending { it.endedAt }
            if (completedSessions.isNotEmpty()) {
                item {
                    SectionHeader("Past research sessions", "${completedSessions.size} completed")
                }
                items(completedSessions.take(5), key = { it.id }) { session ->
                    ClickableCard(
                        onClick = { onOpenDetail("research_session", session.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                    .background(FieldMindTheme.colors.positive.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Bolt, null, tint = FieldMindTheme.colors.positive, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    session.name.ifBlank { "Research Session" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val elapsedStr = if (session.totalDurationMs > 0) {
                                    val totalSec = session.totalDurationMs / 1000
                                    val hours = totalSec / 3600
                                    val minutes = (totalSec % 3600) / 60
                                    val seconds = totalSec % 60
                                    if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
                                    else "%d:%02d".format(minutes, seconds)
                                } else ""
                                val obsStr = "${session.observationCount} obs"
                                val dateStr = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(session.startedAt))
                                Text(
                                    listOfNotNull(elapsedStr.takeIf { it.isNotBlank() }, obsStr, dateStr).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                FieldMindIcons.Forward, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        }
                    }
                }
            }

            // ── Search bar ──
            item {
                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = filterState.query,
                        onValueChange = { filterState = filterState.copy(query = it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search observations...") },
                        leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                        trailingIcon = {
                            if (filterState.query.isNotBlank()) {
                                IconButton(onClick = { filterState = filterState.copy(query = "") }) {
                                    Icon(FieldMindIcons.Close, null, size = 20.dp)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }
            }

            // ── Toolbar: View mode tabs + action buttons ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View mode segmented control
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(
                                TimelineViewMode.List to FieldMindIcons.List,
                                TimelineViewMode.Gallery to FieldMindIcons.Gallery
                            ).forEach { (mode, icon) ->
                                val selected = viewMode == mode
                                Surface(
                                    onClick = { viewMode = mode },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            icon, null,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 16.dp
                                        )
                                        Text(
                                            mode.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Search toggle
                    IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(32.dp)) {
                        Icon(FieldMindIcons.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }

                    // Filter button (with active indicator)
                    val hasActiveFilters = filterState != ObservationFilterState()
                    BadgedBox(badge = {
                        if (hasActiveFilters) {
                            Badge(containerColor = FieldMindTheme.colors.observation)
                        }
                    }) {
                        IconButton(onClick = { showFilterSheet = true }, modifier = Modifier.size(32.dp)) {
                            Icon(FieldMindIcons.Filter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                        }
                    }

                    // Sort button
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(FieldMindIcons.Sort, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            listOf("Date (newest)", "Date (oldest)", "Confidence", "Category").forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort) },
                                    onClick = { filterState = filterState.copy(sortBy = sort); showSortMenu = false },
                                    leadingIcon = if (filterState.sortBy == sort) ({ Icon(FieldMindIcons.Check, null, size = 18.dp) }) else null
                                )
                            }
                        }
                    }

                    // Session group toggle
                    IconButton(
                        onClick = { showSessionGroups = !showSessionGroups },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Session,
                            null,
                            tint = if (showSessionGroups) FieldMindTheme.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                    }

                    // Export button
                    IconButton(
                        onClick = onOpenExport,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Export,
                            null,
                            tint = FieldMindTheme.colors.observation,
                            size = 20.dp
                        )
                    }
                }
            }

            // ── Active filter chips ──
            val hasActiveFilters = filterState.category.isNotBlank() || filterState.confidence.isNotBlank() ||
                    filterState.projectId != null || filterState.tags.isNotBlank() || filterState.draftsOnly
            if (hasActiveFilters) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (filterState.category.isNotBlank()) item {
                            FilterChip(selected = true, onClick = { filterState = filterState.copy(category = "") },
                                label = { Text("Cat: ${filterState.category}") })
                        }
                        if (filterState.confidence.isNotBlank()) item {
                            FilterChip(selected = true, onClick = { filterState = filterState.copy(confidence = "") },
                                label = { Text("Conf: ${filterState.confidence}") })
                        }
                        if (filterState.projectId != null) item {
                            FilterChip(selected = true, onClick = { filterState = filterState.copy(projectId = null) },
                                label = { Text("Project filter") })
                        }
                        if (filterState.draftsOnly) item {
                            FilterChip(selected = true, onClick = { filterState = filterState.copy(draftsOnly = false) },
                                label = { Text("Drafts") })
                        }
                    }
                }
            }

            // ── Content by view mode ──
            when (viewMode) {
                TimelineViewMode.List -> {
                    if (filteredObservations.isEmpty()) {
                        item {
                            EmptyState("No observations found", "Adjust your filters or capture something new.",
                                icon = FieldMindIcons.Observation)
                        }
                    } else if (showSessionGroups && displayItems.isNotEmpty()) {
                        // Session-grouped list with headers
                        items(displayItems) { item ->
                            when (item.type) {
                                "sessionHeader" -> {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedSessions = if (expandedSessions.contains(item.sessionId))
                                                    expandedSessions - item.sessionId
                                                else
                                                    expandedSessions + item.sessionId
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            Modifier.size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(FieldMindIcons.Session, null, tint = FieldMindTheme.colors.observation, size = 18.dp)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(item.sessionName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            Text("${item.obsCount} observation${if (item.obsCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(
                                            if (expandedSessions.contains(item.sessionId)) FieldMindIcons.Up else FieldMindIcons.Down,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 18.dp
                                        )
                                    }
                                }
                                "sessionExpand" -> {
                                    TextButton(
                                        onClick = {
                                            expandedSessions = expandedSessions + item.sessionId
                                        },
                                        modifier = Modifier.padding(start = 40.dp)
                                    ) {
                                        Text("+${item.obsCount} more from this session", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                "ungroupedHeader" -> {
                                    Row(
                                        Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(Modifier.weight(1f))
                                        Text("Other observations (${item.obsCount})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        HorizontalDivider(Modifier.weight(1f))
                                    }
                                }
                                "obs" -> {
                                    item.observation?.let { obs ->
                                        EntityCard(
                                            title = obs.subject.ifBlank { "Observation" },
                                            kind = "observation",
                                            body = "${obs.category} • ${obs.date}",
                                            meta = listOf(obs.confidenceLevel),
                                            onClick = { onOpenDetail("observation", obs.id) },
                                            animate = true
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Flat list without session grouping
                        itemsIndexed(filteredObservations) { i, obs ->
                            EntityCard(
                                title = obs.subject.ifBlank { "Observation" },
                                kind = "observation",
                                body = "${obs.category} • ${obs.date}",
                                meta = listOf(obs.confidenceLevel),
                                onClick = { onOpenDetail("observation", obs.id) },
                                index = i,
                                animate = true
                            )
                        }
                    }
                }
                TimelineViewMode.Gallery -> {
                    item {
                        Box(Modifier.fillMaxWidth().heightIn(max = 800.dp)) {
                            ObservationGalleryView(filteredObservations, viewModel, onOpenDetail)
                        }
                    }
                }
                else -> {} // Map & Calendar not shown in Field Log
            }

            // ── Count footer ──
            item {
                Text(
                    "${filteredObservations.size} observation${if (filteredObservations.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
        } // end PullToRefreshBox
    }

    // ── Advanced Filter Sheet ──
    FluidBottomSheet(
        visible = showFilterSheet,
        onDismiss = { showFilterSheet = false }
    ) {
        AdvancedFilterSheetContent(
            filterState = filterState,
            onFilterChange = { filterState = it },
            onDismiss = { showFilterSheet = false },
            onReset = { filterState = ObservationFilterState() },
            projects = emptyList()
        )
    }

/** Filter state for the Field Log screen. */
data class ObservationFilterState(
    val query: String = "",
    val category: String = "",
    val confidence: String = "",
    val tags: String = "",
    val projectId: Long? = null,
    val draftsOnly: Boolean = false,
    val sortBy: String = "Date (newest)"
)

/** Saver for ObservationFilterState to survive configuration changes. */
val ObservationFilterStateSaver = androidx.compose.runtime.saveable.Saver<ObservationFilterState, List<String?>>(
    save = { state ->
        listOf(
            state.query,
            state.category,
            state.confidence,
            state.tags,
            state.projectId?.toString(),
            state.draftsOnly.toString(),
            state.sortBy
        )
    },
    restore = { list ->
        ObservationFilterState(
            query = list.getOrElse(0) { "" } ?: "",
            category = list.getOrElse(1) { "" } ?: "",
            confidence = list.getOrElse(2) { "" } ?: "",
            tags = list.getOrElse(3) { "" } ?: "",
            projectId = list.getOrElse(4) { null }?.toLongOrNull(),
            draftsOnly = list.getOrElse(5) { "false" }?.toBoolean() ?: false,
            sortBy = list.getOrElse(6) { "Date (newest)" } ?: "Date (newest)"
        )
    }
)
}
