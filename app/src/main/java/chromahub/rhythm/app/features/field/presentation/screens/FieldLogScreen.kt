package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon

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
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()

    var viewMode by remember { mutableStateOf(TimelineViewMode.List) }
    var showSearch by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(ObservationFilterState()) }

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
                    } else {
                        item {
                            Box(Modifier.fillMaxWidth().heightIn(max = 800.dp)) {
                                ObservationListView(filteredObservations, viewModel, onOpenDetail)
                            }
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
    }

    // ── Advanced Filter Sheet ──
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            AdvancedFilterSheetContent(
                filterState = filterState,
                onFilterChange = { filterState = it },
                onDismiss = { showFilterSheet = false },
                onReset = { filterState = ObservationFilterState() },
                projects = emptyList()
            )
        }
    }
}
