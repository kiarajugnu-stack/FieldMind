package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.delay

// ── Category accent colors ──
@Composable
private fun categoryColor(category: String): androidx.compose.ui.graphics.Color {
    val colors = FieldMindTheme.colors
    return when {
        category.equals("Bird", ignoreCase = true) -> colors.info
        category.equals("Mammal", ignoreCase = true) -> colors.warning
        category.equals("Plant", ignoreCase = true) -> colors.info
        category.equals("Insect", ignoreCase = true) -> colors.data
        category.equals("Fungi", ignoreCase = true) -> colors.report
        category.equals("Reptile", ignoreCase = true) -> colors.hypothesis
        category.equals("Amphibian", ignoreCase = true) -> colors.project
        category.equals("Fish", ignoreCase = true) -> colors.info
        category.equals("Arachnid", ignoreCase = true) -> colors.warning
        category.equals("Crustacean", ignoreCase = true) -> colors.data
        category.equals("Mollusk", ignoreCase = true) -> colors.report
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun categoryIcon(category: String): MaterialSymbolIcon = when {
    category.equals("Bird", ignoreCase = true) -> FieldMindIcons.Bird
    category.equals("Mammal", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Plant", ignoreCase = true) -> FieldMindIcons.Plant
    category.equals("Insect", ignoreCase = true) -> FieldMindIcons.Insect
    category.equals("Fungi", ignoreCase = true) -> FieldMindIcons.Plant
    category.equals("Reptile", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Amphibian", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Fish", ignoreCase = true) -> FieldMindIcons.Water
    category.equals("Arachnid", ignoreCase = true) -> FieldMindIcons.Insect
    category.equals("Crustacean", ignoreCase = true) -> FieldMindIcons.Water
    category.equals("Mollusk", ignoreCase = true) -> FieldMindIcons.Water
    else -> FieldMindIcons.Nature
}

// ══════════════════════════════════════════════════════════════════════
//  Species Browser Screen — search, category filter, scrollable list
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesBrowserScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    BackHandler(enabled = true) { onBack() }
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedContinent by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var continents by remember { mutableStateOf<List<String>>(emptyList()) }
    var species by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }

    // ── Sorting state ──
    val sortOptions = listOf(
        "Common name (A-Z)",
        "Common name (Z-A)",
        "Scientific name (A-Z)",
        "Scientific name (Z-A)",
        "Genus (A-Z)",
        "Family (A-Z)",
        "Order (A-Z)",
        "Kingdom (A-Z)",
        "Category (A-Z)"
    )
    val speciesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var selectedSort by rememberSaveable { mutableStateOf(sortOptions[0]) }
    var showSortDropdown by remember { mutableStateOf(false) }

    // Load categories, continents, and initial species
    LaunchedEffect(Unit) {
        categories = database.getCategories()
        continents = database.getContinents()
        totalCount = database.getTotalSpeciesCount()
        species = database.search("", limit = 200)
        isLoading = false
    }

    // ── Sort helper ──
    fun List<SpeciesRecord>.sortedByOption(option: String): List<SpeciesRecord> {
        return when (option) {
            "Common name (A-Z)" -> sortedBy { it.commonName.lowercase() }
            "Common name (Z-A)" -> sortedByDescending { it.commonName.lowercase() }
            "Scientific name (A-Z)" -> sortedBy { it.scientificName.lowercase() }
            "Scientific name (Z-A)" -> sortedByDescending { it.scientificName.lowercase() }
            "Genus (A-Z)" -> sortedBy { it.genus.lowercase() }
            "Family (A-Z)" -> sortedBy { it.family.lowercase() }
            "Order (A-Z)" -> sortedBy { it.order.lowercase() }
            "Kingdom (A-Z)" -> sortedBy { it.kingdom.lowercase() }
            "Category (A-Z)" -> sortedBy { it.category.lowercase() }
            else -> this
        }
    }

    // Debounced search + sort + continent filter
    LaunchedEffect(searchQuery, selectedCategory, selectedContinent, selectedSort) {
        delay(200) // 200ms debounce
        isLoading = true

        // Get base results (by category or full search)
        val base = if (selectedCategory != null) {
            val byCategory = database.getByCategory(selectedCategory!!)
            if (searchQuery.isBlank()) byCategory
            else byCategory.filter {
                val q = searchQuery.lowercase()
                it.commonName.lowercase().contains(q) ||
                it.scientificName.lowercase().contains(q) ||
                it.genus.lowercase().contains(q) ||
                it.family.lowercase().contains(q) ||
                it.order.lowercase().contains(q) ||
                it.phylum.lowercase().contains(q) ||
                it.kingdom.lowercase().contains(q) ||
                it.tags.any { t -> t.lowercase().contains(q) } ||
                it.habitat.lowercase().contains(q)
            }
        } else {
            database.search(searchQuery, limit = 200)
        }

        // Apply continent filter
        val filtered = if (selectedContinent != null) {
            base.filter { it.continents.any { c -> c.equals(selectedContinent, ignoreCase = true) } }
        } else {
            base
        }

        species = filtered.sortedByOption(selectedSort)
        isLoading = false
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // ── Species Browser Header (expanded) ──
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BackButton(onClick = onBack)
                        
                        // Icon badge
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FieldMindTheme.colors.info.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Nature, null, tint = FieldMindTheme.colors.info, size = 28.dp)
                        }
                        
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Species Browser",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (totalCount > 0) {
                                Text(
                                    "$totalCount species in catalog",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, scientific name, genus, family, order…") },
                        leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(FieldMindIcons.Close, null, size = 18.dp)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Category filter chips
                    if (categories.isNotEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "All" chip
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All ($totalCount)", fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(20.dp),
                                leadingIcon = if (selectedCategory == null) {{ Icon(FieldMindIcons.Check, null, size = 16.dp) }} else null
                            )
                            categories.forEach { (cat, count) ->
                                val accent = categoryColor(cat)
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                    label = { Text("$cat ($count)", fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(20.dp),
                                    leadingIcon = if (selectedCategory == cat) {{ Icon(FieldMindIcons.Check, null, size = 16.dp, tint = accent) }} else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent.copy(alpha = 0.12f),
                                        selectedLabelColor = accent
                                    )
                                )
                            }
                        }
                    }

                    // Continent filter chips
                    if (continents.isNotEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "All regions" chip
                            FilterChip(
                                selected = selectedContinent == null,
                                onClick = { selectedContinent = null },
                                label = { Text("All regions", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = if (selectedContinent == null) {{ Icon(FieldMindIcons.Check, null, size = 14.dp) }} else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            continents.forEach { c ->
                                val isSelected = selectedContinent == c
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedContinent = if (isSelected) null else c },
                                    label = { Text(c, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = if (isSelected) {{ Icon(FieldMindIcons.Check, null, size = 14.dp) }} else null
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && species.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (species.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 64.dp)
                    Text(
                        if (searchQuery.isNotBlank()) "No species match \"$searchQuery\""
                        else if (selectedCategory != null) "No species in this category"
                        else "No species found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Try a different search or clear filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = speciesListState,
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Results count + sort
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${species.size} species",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Sort button
                        Box {
                            Surface(
                                onClick = { showSortDropdown = !showSortDropdown },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        FieldMindIcons.Category,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 14.dp
                                    )
                                    Text(
                                        selectedSort.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        if (showSortDropdown) FieldMindIcons.Up else FieldMindIcons.Down,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 14.dp
                                    )
                                }
                            }

                            // Sort dropdown
                            DropdownMenu(
                                expanded = showSortDropdown,
                                onDismissRequest = { showSortDropdown = false },
                                modifier = Modifier.width(200.dp)
                            ) {
                                sortOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    option,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (option == selectedSort) {
                                                    Icon(
                                                        FieldMindIcons.Check,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        size = 16.dp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedSort = option
                                            showSortDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                items(species, key = { it.id }) { record ->
                    SpeciesCard(
                        record = record,
                        onClick = { onOpenDetail(record.id) }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Card — compact row for the list
// ══════════════════════════════════════════════════════════════════════

@Composable
internal fun SpeciesCard(
    record: SpeciesRecord,
    onClick: () -> Unit
) {
    val accent = categoryColor(record.category)
    val colors = FieldMindTheme.colors

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon badge
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon(record.category),
                    null,
                    tint = accent,
                    size = 26.dp
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Common name
                Text(
                    record.commonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Scientific name (italic style)
                record.scientificName.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.1f)
                    ) {
                        Text(
                            record.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    // Conservation status if notable
                    record.conservationStatus.takeIf { it.isNotBlank() && !it.contains("Least Concern", ignoreCase = true) && !it.contains("Not Evaluated", ignoreCase = true) }?.let { status ->
                        val statusColor = when {
                            status.contains("Endangered", ignoreCase = true) || status.contains("Critically", ignoreCase = true) -> MaterialTheme.colorScheme.error
                            status.contains("Vulnerable", ignoreCase = true) || status.contains("Near", ignoreCase = true) -> colors.warning
                            status.contains("Extinct", ignoreCase = true) -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Forward arrow
            Icon(
                FieldMindIcons.Forward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                size = 18.dp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Detail Screen — full species information
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesDetailScreen(
    speciesId: String,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }

    var species by remember { mutableStateOf<SpeciesRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var similarSpecies by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }

    LaunchedEffect(speciesId) {
        val record = database.getById(speciesId)
        species = record
        if (record != null && record.similarSpecies.isNotEmpty()) {
            // Load similar species data
            similarSpecies = record.similarSpecies.mapNotNull { name ->
                // Look up by common name (fuzzy match)
                database.search(name, limit = 1).firstOrNull { it.commonName.equals(name, ignoreCase = true) }
                    ?: database.search(name, limit = 1).firstOrNull { it.commonName.contains(name, ignoreCase = true) }
                    ?: database.search(name, limit = 1).firstOrNull { it.scientificName.contains(name, ignoreCase = true) }
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (species == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 48.dp)
                    Text("Species not found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onBack) { Text("Go back") }
                }
            }
        } else {
            val record = species!!
            val accent = categoryColor(record.category)

            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Hero header with back button ──
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(accent.copy(alpha = 0.06f))
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            // Back button row
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(FieldMindIcons.Back, null, tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
                                    }
                                }
                            }

                            // Species identity
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Category badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = accent.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(categoryIcon(record.category), null, tint = accent, size = 18.dp)
                                        Text(
                                            record.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    record.commonName,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                if (record.scientificName.isNotBlank()) {
                                    Text(
                                        record.scientificName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                // Conservation status
                                if (record.conservationStatus.isNotBlank()) {
                                    val statusColor = when {
                                        record.conservationStatus.contains("Least Concern", ignoreCase = true) || record.conservationStatus.contains("Not Evaluated", ignoreCase = true) -> MaterialTheme.colorScheme.onSurfaceVariant
                                        record.conservationStatus.contains("Endangered", ignoreCase = true) || record.conservationStatus.contains("Critically", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                        record.conservationStatus.contains("Vulnerable", ignoreCase = true) || record.conservationStatus.contains("Near", ignoreCase = true) -> FieldMindTheme.colors.warning
                                        record.conservationStatus.contains("Extinct", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            FieldMindIcons.Info,
                                            null,
                                            tint = statusColor,
                                            size = 16.dp
                                        )
                                        Text(
                                            record.conservationStatus,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Content sections ──
                item { Spacer(Modifier.height(12.dp)) }

                // Description
                if (record.description.isNotBlank()) {
                    item {
                        DetailSection(
                            icon = FieldMindIcons.Info,
                            title = "Description",
                            accent = accent
                        ) {
                            Text(
                                record.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Habitat & Diet (side-by-side)
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (record.habitat.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(FieldMindIcons.Location, null, tint = accent, size = 22.dp)
                                    Text(
                                        "Habitat",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        record.habitat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (record.diet.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(FieldMindIcons.Water, null, tint = accent, size = 22.dp)
                                    Text(
                                        "Diet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        record.diet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }

                // Key Features
                if (record.keyFeatures.isNotEmpty()) {
                    item {
                        DetailSection(
                            icon = FieldMindIcons.Visibility,
                            title = "Identification features",
                            accent = accent
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                record.keyFeatures.forEach { feature ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = accent.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(FieldMindIcons.Visibility, null, tint = accent, size = 14.dp)
                                            Text(
                                                feature,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = accent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Tags
                if (record.tags.isNotEmpty()) {
                    item {
                        DetailSection(
                            icon = FieldMindIcons.Category,
                            title = "Tags",
                            accent = accent
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                record.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Similar Species
                if (similarSpecies.isNotEmpty()) {
                    item {
                        DetailSection(
                            icon = FieldMindIcons.Nature,
                            title = "Similar species",
                            subtitle = "Species you might confuse this with",
                            accent = accent
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                similarSpecies.forEach { similar ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(categoryColor(similar.category).copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    categoryIcon(similar.category),
                                                    null,
                                                    tint = categoryColor(similar.category),
                                                    size = 20.dp
                                                )
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    similar.commonName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (similar.scientificName.isNotBlank()) {
                                                    Text(
                                                        similar.scientificName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            // Tap to navigate to that species
                                            Surface(
                                                onClick = { /* would navigate to similar species detail */ },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        FieldMindIcons.Forward,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        size = 16.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Bottom spacer
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared detail section component
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun DetailSection(
    icon: MaterialSymbolIcon,
    title: String,
    subtitle: String = "",
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, size = 20.dp)
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}
