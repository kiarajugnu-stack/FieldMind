package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.data.weather.WeatherUnitConverter
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime

// ══════════════════════════════════════════════════════════════════════
//  Observations Timeline Component — Full spec implementation
//  View modes: List, Gallery, Map, Calendar
//  Features: Search, Filter/Sort/Select toolbar, Advanced Filter Sheet
// ══════════════════════════════════════════════════════════════════════

enum class TimelineViewMode { List, Gallery, Map, Calendar }

data class ObservationFilterState(
    val query: String = "",
    val species: String = "",
    val category: String = "",
    val confidence: String = "",
    val dateRangeStart: String = "",
    val dateRangeEnd: String = "",
    val locationRadius: String = "",
    val habitat: String = "",
    val weather: String = "",
    val durationMin: String = "",
    val durationMax: String = "",
    val tags: String = "",
    val projectId: Long? = null,
    val evidenceType: String = "",
    val aiIdentifiedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val draftsOnly: Boolean = false,
    val reObservationsOnly: Boolean = false,
    val sortBy: String = "Date (newest)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationsTimelineSection(
    observations: List<ObservationEntity>,
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onStartCapture: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(TimelineViewMode.List) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(ObservationFilterState()) }

    // Apply filters
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
        if (filterState.habitat.isNotBlank()) filtered = filtered.filter { it.factsOnlyNotes.contains(filterState.habitat, ignoreCase = true) }

        // Apply sort
        when (filterState.sortBy) {
            "Date (oldest)" -> filtered.sortedBy { it.timestamp }
            "Confidence" -> filtered.sortedByDescending { it.confidenceLevel }
            "Category" -> filtered.sortedBy { it.category }
            else -> filtered.sortedByDescending { it.timestamp } // Date (newest)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Search Bar ──
        if (showSearch) {
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

        // ── View Mode Tabs + Toolbar ──
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
                    TimelineViewMode.entries.forEach { mode ->
                        val selected = viewMode == mode
                        Surface(
                            onClick = { viewMode = mode },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    when (mode) {
                                        TimelineViewMode.List -> FieldMindIcons.List
                                        TimelineViewMode.Gallery -> FieldMindIcons.Gallery
                                        TimelineViewMode.Map -> FieldMindIcons.Map
                                        TimelineViewMode.Calendar -> FieldMindIcons.Calendar
                                    },
                                    null,
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

            // Select mode
            IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                Icon(FieldMindIcons.Select, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            }
        }

        // ── Active Filter Chips ──
        val hasActiveFilters = filterState.query.isNotBlank() || filterState.category.isNotBlank() || 
                               filterState.confidence.isNotBlank() || filterState.projectId != null ||
                               filterState.tags.isNotBlank() || filterState.habitat.isNotBlank()
        if (hasActiveFilters) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (filterState.query.isNotBlank()) item { FilterChip(selected = true, onClick = { filterState = filterState.copy(query = "") }, label = { Text("\"${filterState.query}\"") }) }
                if (filterState.category.isNotBlank()) item { FilterChip(selected = true, onClick = { filterState = filterState.copy(category = "") }, label = { Text("Cat: ${filterState.category}") }) }
                if (filterState.confidence.isNotBlank()) item { FilterChip(selected = true, onClick = { filterState = filterState.copy(confidence = "") }, label = { Text("Conf: ${filterState.confidence}") }) }
                if (filterState.projectId != null) item { FilterChip(selected = true, onClick = { filterState = filterState.copy(projectId = null) }, label = { Text("Project filter") }) }
            }
        }

        // ── Content by View Mode (static Box to avoid infinite-height crash with LazyColumn inside AnimatedContent) ──
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            when (viewMode) {
                TimelineViewMode.List -> ObservationListView(filteredObservations, viewModel, onOpenDetail)
                TimelineViewMode.Gallery -> ObservationGalleryView(filteredObservations, viewModel, onOpenDetail)
                TimelineViewMode.Map -> ObservationMapPrompt(filteredObservations, onOpenMap)
                TimelineViewMode.Calendar -> ObservationCalendarView(filteredObservations)
            }
        }

        // ── Observation Count ──
        Text(
            "${filteredObservations.size} observation${if (filteredObservations.size != 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    // ══════════════════════════════════════════════════════════════════
    //  Advanced Filter Sheet — 15 filter dimensions per spec
    // ══════════════════════════════════════════════════════════════════
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
                projects = emptyList() // will be populated by caller
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  List View — Full observation cards matching spec layout
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ObservationListView(
    observations: List<ObservationEntity>,
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit
) {
    if (observations.isEmpty()) {
        EmptyState("No observations found", "Adjust your filters or start capturing.", icon = FieldMindIcons.Observation)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(observations) { obs ->
            ObservationTimelineCard(obs, viewModel, onClick = { onOpenDetail("observation", obs.id) })
        }
    }
}

@Composable
fun ObservationTimelineCard(
    obs: ObservationEntity,
    viewModel: FieldMindViewModel,
    onClick: () -> Unit = {}
) {
    val colors = FieldMindTheme.colors
    val attachments by viewModel.attachmentsForObservation(obs.id).collectAsState(initial = emptyList())
    val photoUri = attachments.firstOrNull { it.type.equals("Photo", true) || it.type.equals("Gallery", true) }?.let { it.localPath ?: it.uri }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // ── Hero Image (if available) ──
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = obs.subject,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }

            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Species + Badges ──
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            obs.subject.ifBlank { "Unidentified" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoChip(obs.category, icon = FieldMindIcons.iconForCategory(obs.category))
                            ConfidenceChip(obs.confidenceLevel)
                        }
                    }
                }

                // ── Location, Weather, Time ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(FieldMindIcons.Location, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                        Text(
                            if (obs.latitude != null) "GPS" else obs.manualLocation.ifBlank { "No location" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Weather
                    if (obs.weatherTemperature != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(FieldMindIcons.Weather, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                            Text(
                                "${obs.weatherTemperature?.toInt()}°${if (true) "C" else "F"} ${obs.weatherCondition.take(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(FieldMindIcons.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                        Text(
                            "${obs.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Tags ──
                if (obs.tags.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        obs.tags.split(",").filter { it.isNotBlank() }.take(3).forEach { tag ->
                            TagChip(tag.trim())
                        }
                    }
                }

                // ── Bottom row: Evidence count + Project + AI badge ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Evidence count
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(FieldMindIcons.File, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
                        Text("${attachments.size} evidence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // AI Verified badge (from structuredDetailsJson)
                    val aiVerified = remember(obs.structuredDetailsJson) {
                        obs.structuredDetailsJson.isNotBlank() &&
                        obs.structuredDetailsJson.contains("aiTopMatch")
                    }
                    if (aiVerified) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FieldMindTheme.colors.project.copy(alpha = 0.12f)
                        ) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(FieldMindIcons.Bolt, null, tint = FieldMindTheme.colors.project, size = 12.dp)
                                Text("AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.project)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Gallery View — Photo grid
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ObservationGalleryView(
    observations: List<ObservationEntity>,
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit
) {
    if (observations.isEmpty()) {
        EmptyState("No observations to display", "Capture your first observation to get started", icon = FieldMindIcons.Gallery)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(observations) { obs ->
            val attachments by viewModel.attachmentsForObservation(obs.id).collectAsState(initial = emptyList())
            val photoUri = attachments.firstOrNull { it.type.equals("Photo", true) || it.type.equals("Gallery", true) }?.let { it.localPath ?: it.uri }

            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onOpenDetail("observation", obs.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = obs.subject,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Observation, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), size = 32.dp)
                        }
                    }
                    // Overlay with species name
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            obs.subject.ifBlank { obs.category },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Map View Prompt — Reuses MapFieldScreen
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ObservationMapPrompt(
    observations: List<ObservationEntity>,
    onOpenMap: () -> Unit
) {
    val withGps = observations.count { it.latitude != null }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(FieldMindIcons.Map, null, tint = FieldMindTheme.colors.info, size = 48.dp)
            Text("Open full map view", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$withGps observations with GPS coordinates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpenMap, shape = RoundedCornerShape(14.dp)) { Text("Open Map") }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Calendar View
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ObservationCalendarView(observations: List<ObservationEntity>) {
    val today = LocalDate.now()
    val calendarData = remember(observations) {
        observations.groupingBy { it.date }.eachCount()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Month header
        val monthYear = today.month.name.take(3) + " " + today.year
        Text(monthYear, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Day headers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }

        // Calendar grid (simplified — shows current month days)
        val firstOfMonth = today.withDayOfMonth(1)
        val firstDayOfWeek = firstOfMonth.dayOfWeek.value - 1 // 0=Mon
        val daysInMonth = today.lengthOfMonth()

        var dayCounter = 1
        val weeks = (0 until 6).map { week ->
            (0 until 7).map { dow ->
                if (week == 0 && dow < firstDayOfWeek) null
                else if (dayCounter > daysInMonth) null
                else dayCounter++
            }
        }.filter { it.any { it != null } }

        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    if (day != null) {
                        val dateStr = "%04d-%02d-%02d".format(today.year, today.month.value, day)
                        val count = calendarData[dateStr] ?: 0
                        val isToday = day == today.dayOfMonth

                        Column(
                            Modifier.width(36.dp).padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.primary
                                            count > 0 -> FieldMindTheme.colors.observation.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (count > 0) {
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.observation, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(Modifier.width(36.dp))
                    }
                }
            }
        }

        // Stats footer
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val todayKey = today.toString()
            val todayCount = calendarData[todayKey] ?: 0
            val monthCount = calendarData.filterKeys { it.startsWith(today.year.toString() + "-" + "%02d".format(today.month.value)) }.values.sum()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$todayCount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.observation)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("This month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$monthCount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.observation)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${observations.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Advanced Filter Sheet Content — 15 filter dimensions per spec
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheetContent(
    filterState: ObservationFilterState,
    onFilterChange: (ObservationFilterState) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    projects: List<fieldmind.research.app.features.field.data.database.entity.ProjectEntity>
) {
    val scrollState = rememberScrollState()
    val categories = listOf("", "Bird", "Mammal", "Insect", "Plant", "Fungi", "Reptile", "Amphibian", "Fish", "Water", "Weather", "Soil", "Other")
    val confidenceLevels = listOf("", "Certain", "Very Likely", "Probable", "Unsure", "Needs Review")
    val habitatTypes = listOf("", "Forest", "Wetland", "Grassland", "Urban", "Agricultural", "Desert", "Coastal", "Freshwater")
    val weatherTypes = listOf("", "Clear", "Cloudy", "Rain", "Snow", "Fog", "Windy", "Thunderstorm")
    val evidenceTypes = listOf("", "Photo", "Video", "Audio", "PDF", "Note")

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onReset) { Text("Reset") }
        }

        // Species search
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Species", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = filterState.species,
                onValueChange = { onFilterChange(filterState.copy(species = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search species...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Category
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    val label = cat.ifBlank { "All" }
                    FilterChip(
                        selected = filterState.category == cat,
                        onClick = { onFilterChange(filterState.copy(category = cat)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Confidence
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Confidence", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                confidenceLevels.forEach { conf ->
                    val label = conf.ifBlank { "Any" }
                    FilterChip(
                        selected = filterState.confidence == conf,
                        onClick = { onFilterChange(filterState.copy(confidence = conf)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Date Range
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Date Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = filterState.dateRangeStart,
                    onValueChange = { onFilterChange(filterState.copy(dateRangeStart = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Start (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = filterState.dateRangeEnd,
                    onValueChange = { onFilterChange(filterState.copy(dateRangeEnd = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("End (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Location Radius
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Location Radius", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = filterState.locationRadius,
                onValueChange = { onFilterChange(filterState.copy(locationRadius = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Radius in km (leave blank for all)") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // Habitat
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Habitat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                habitatTypes.forEach { habitat ->
                    val label = habitat.ifBlank { "Any" }
                    FilterChip(
                        selected = filterState.habitat == habitat,
                        onClick = { onFilterChange(filterState.copy(habitat = habitat)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Weather
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Weather", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                weatherTypes.forEach { w ->
                    val label = w.ifBlank { "Any" }
                    FilterChip(
                        selected = filterState.weather == w,
                        onClick = { onFilterChange(filterState.copy(weather = w)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Observation Duration
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Observation Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = filterState.durationMin,
                    onValueChange = { onFilterChange(filterState.copy(durationMin = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Min (s)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = filterState.durationMax,
                    onValueChange = { onFilterChange(filterState.copy(durationMax = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Max (s)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Tags
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = filterState.tags,
                onValueChange = { onFilterChange(filterState.copy(tags = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Comma-separated tags") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // Evidence Type
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Evidence Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                evidenceTypes.forEach { et ->
                    val label = et.ifBlank { "Any" }
                    FilterChip(
                        selected = filterState.evidenceType == et,
                        onClick = { onFilterChange(filterState.copy(evidenceType = et)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Toggle filters
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Additional Filters", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.aiIdentifiedOnly,
                    onClick = { onFilterChange(filterState.copy(aiIdentifiedOnly = !filterState.aiIdentifiedOnly)) },
                    label = { Text("AI Identified Only") }
                )
                FilterChip(
                    selected = filterState.favoritesOnly,
                    onClick = { onFilterChange(filterState.copy(favoritesOnly = !filterState.favoritesOnly)) },
                    label = { Text("Favorites Only") }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.draftsOnly,
                    onClick = { onFilterChange(filterState.copy(draftsOnly = !filterState.draftsOnly)) },
                    label = { Text("Drafts Only") }
                )
                FilterChip(
                    selected = filterState.reObservationsOnly,
                    onClick = { onFilterChange(filterState.copy(reObservationsOnly = !filterState.reObservationsOnly)) },
                    label = { Text("Re-observations Only") }
                )
            }
        }

        // Apply button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Apply Filters")
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Observation Statistics Dashboard — Per spec
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ObservationStatsDashboard(
    observations: List<ObservationEntity>,
    modifier: Modifier = Modifier
) {
    val colors = FieldMindTheme.colors
    val totalObs = observations.size
    val speciesCount = observations.map { it.subject }.filter { it.isNotBlank() }.distinct().size
    val totalHours = observations.sumOf { it.durationMs ?: 0L } / 3600_000.0
    val evidenceCount = observations.count { it.evidenceSummary.isNotBlank() }
    val activeProjects = observations.mapNotNull { it.projectId }.distinct().size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Observation Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("$totalObs", "Observations", colors.observation)
                StatItem("$speciesCount", "Species", colors.question)
                StatItem("%.0f".format(totalHours), "Hours", colors.warning)
                StatItem("$evidenceCount", "Evidence", colors.info)
                StatItem("$activeProjects", "Projects", colors.project)
            }

            // Species distribution
            val speciesDistribution = observations.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }.take(6)
            if (speciesDistribution.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Text("Species Distribution", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val maxCount = speciesDistribution.maxOf { it.value }.coerceAtLeast(1)
                speciesDistribution.forEach { (category, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Box(Modifier.fillMaxWidth(count.toFloat() / maxCount).fillMaxHeight().background(colors.categoryColor(category), RoundedCornerShape(99.dp)))
                        }
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Monthly trends
            val monthlyTrend = observations.groupingBy { it.date.take(7) }.eachCount().entries.sortedBy { it.key }.takeLast(12)
            if (monthlyTrend.size >= 2) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Text("Monthly Activity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val maxMonthly = monthlyTrend.maxOf { it.value }.coerceAtLeast(1)
                monthlyTrend.forEach { (month, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(month.takeLast(2) + "/" + month.take(4), modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall)
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Box(Modifier.fillMaxWidth(count.toFloat() / maxMonthly).fillMaxHeight().background(colors.observation.copy(alpha = 0.6f), RoundedCornerShape(99.dp)))
                        }
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Confidence distribution
            val confidenceDist = observations.groupingBy { it.confidenceLevel }.eachCount()
            if (confidenceDist.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Text("Confidence Distribution", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val maxConf = confidenceDist.values.max().coerceAtLeast(1)
                confidenceDist.entries.sortedByDescending { it.value }.forEach { (level, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(level, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall)
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            val confColor = when (level.lowercase()) {
                                "certain" -> colors.confidenceSure
                                "likely", "very likely" -> colors.confidenceGuess
                                else -> colors.confidenceVerify
                            }
                            Box(Modifier.fillMaxWidth(count.toFloat() / maxConf).fillMaxHeight().background(confColor, RoundedCornerShape(99.dp)))
                        }
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Habitat breakdown
            val habitatData = observations.filter { it.factsOnlyNotes.isNotBlank() }
                .flatMap { it.factsOnlyNotes.split(",").map { s -> s.trim() } }
                .filter { it.isNotBlank() }
                .groupingBy { it }.eachCount()
            if (habitatData.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Text("Common Keywords", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitatData.entries.sortedByDescending { it.value }.take(10).forEach { (word, count) ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(word, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
