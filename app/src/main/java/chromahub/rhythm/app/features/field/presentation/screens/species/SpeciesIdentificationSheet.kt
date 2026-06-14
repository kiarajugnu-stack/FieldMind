package fieldmind.research.app.features.field.presentation.screens.species

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.data.vision.SpeciesClassifier
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesMatch
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch

/**
 * Bottom sheet for species identification results.
 *
 * Shows:
 * - Top-5 matches from the image classifier with confidence bars
 * - Manual search bar for offline lookup
 * - Category filter chips
 * - Species detail card on selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesIdentificationSheet(
    imageUri: String?,
    classifier: SpeciesClassifier,
    database: SpeciesDatabase,
    onSelectSpecies: (SpeciesMatch) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var matches by remember { mutableStateOf<List<SpeciesMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }
    var searchMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var selectedMatch by remember { mutableStateOf<SpeciesMatch?>(null) }
    var showDetail by remember { mutableStateOf(false) }

    // Trigger identification on load
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            loading = true
            error = null
            try {
                val speciesMatches = classifier.identifyFromImage(imageUri)
                matches = speciesMatches
                if (speciesMatches.isEmpty()) {
                    error = "No species identified. Try a different photo or search manually."
                }
            } catch (e: Exception) {
                error = e.message ?: "Identification failed"
            }
            loading = false
        }
    }

    // Load categories
    LaunchedEffect(Unit) {
        categories = database.getCategories()
    }

    // Search when query changes
    LaunchedEffect(query) {
        if (query.length >= 2) {
            searchResults = database.search(query)
        } else {
            searchResults = emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // ── Drag handle ──
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(40.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }

                // ── Header ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Nature, null, tint = FieldMindTheme.colors.observation, size = 22.dp)
                        }
                        Column {
                            Text(
                                "Species Identification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (imageUri != null) "From photo" else "Search species",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Toggle: Camera results vs Manual search ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !searchMode,
                        onClick = { searchMode = false },
                        label = { Text("From photo") },
                        leadingIcon = { Icon(FieldMindIcons.Camera, null, size = 16.dp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = searchMode,
                        onClick = { searchMode = true },
                        label = { Text("Search manual") },
                        leadingIcon = { Icon(FieldMindIcons.Search, null, size = 16.dp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (searchMode) {
                    // ── Manual search mode ──
                    SearchModeContent(
                        query = query,
                        onQueryChange = { query = it },
                        searchResults = searchResults,
                        selectedCategory = selectedCategory,
                        categories = categories,
                        onCategorySelect = { selectedCategory = it },
                        onSelectRecord = { record ->
                            haptics.confirm()
                            onSelectSpecies(
                                SpeciesMatch(
                                    commonName = record.commonName,
                                    scientificName = record.scientificName,
                                    confidence = 1.0f,
                                    category = record.category,
                                    description = record.description
                                )
                            )
                            onDismiss()
                        },
                        onDismiss = onDismiss
                    )
                } else {
                    // ── Identification results mode ──
                    when {
                        loading -> LoadingContent()
                        error != null -> ErrorContent(error!!) { onDismiss() }
                        matches.isEmpty() -> {
                            // Empty state — encourage manual search
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    FieldMindIcons.Nature,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    size = 64.dp
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No matches found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Try the manual search or use a clearer photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { searchMode = true },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(FieldMindIcons.Search, null, size = 18.dp)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Search manually")
                                }
                            }
                        }
                        else -> {
                            // ── Results list ──
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                            ) {
                                // Info header
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = FieldMindTheme.colors.info.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                FieldMindIcons.Info,
                                                null,
                                                tint = FieldMindTheme.colors.info,
                                                size = 16.dp
                                            )
                                            Text(
                                                "Top ${matches.size} suggestions — tap to select and add to your observation",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FieldMindTheme.colors.info
                                            )
                                        }
                                    }
                                }

                                // Match cards
                                items(matches, key = { it.scientificName }) { match ->
                                    SpeciesMatchCard(
                                        match = match,
                                        isSelected = selectedMatch?.scientificName == match.scientificName,
                                        onSelect = {
                                            haptics.light()
                                            selectedMatch = if (selectedMatch?.scientificName == match.scientificName) null else match
                                        },
                                        onConfirm = {
                                            haptics.confirm()
                                            onSelectSpecies(match)
                                            onDismiss()
                                        }
                                    )
                                }

                                // Not found option
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        onClick = { searchMode = true },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                MaterialSymbolIcon("search"),
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 18.dp
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text(
                                                "Not listed? Search by name",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Match Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SpeciesMatchCard(
    match: SpeciesMatch,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = FieldMindTheme.colors
    val confidenceColor = when {
        match.confidence >= 0.8f -> colors.positive
        match.confidence >= 0.5f -> colors.warning
        else -> MaterialTheme.colorScheme.error
    }
    val categoryColor = colors.accentFor(match.category.lowercase())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon / placeholder for species image
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FieldMindIcons.iconForCategory(match.category),
                        null,
                        tint = categoryColor,
                        size = 28.dp
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        match.commonName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        match.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoChip(match.category, icon = FieldMindIcons.iconForCategory(match.category))
                        InfoChip(
                            "${(match.confidence * 100).toInt()}%",
                            icon = FieldMindIcons.Check,
                            color = confidenceColor
                        )
                    }
                }

                // Confidence ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { match.confidence },
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 4.dp,
                        color = confidenceColor,
                        trackColor = confidenceColor.copy(alpha = 0.12f)
                    )
                    Text(
                        "${(match.confidence * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Confidence bar
            LinearProgressIndicator(
                progress = { match.confidence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = confidenceColor,
                trackColor = confidenceColor.copy(alpha = 0.12f)
            )

            // Description (when selected)
            if (isSelected && match.description.isNotBlank()) {
                Text(
                    match.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Confirm button (when selected)
            if (isSelected) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = categoryColor
                    )
                ) {
                    Icon(FieldMindIcons.Check, null, size = 18.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Add ${match.commonName} to observation")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Search Mode Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SearchModeContent(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<SpeciesRecord>,
    selectedCategory: String?,
    categories: List<Pair<String, Int>>,
    onCategorySelect: (String?) -> Unit,
    onSelectRecord: (SpeciesRecord) -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by common or scientific name...") },
                leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Category filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text("All") }
                    )
                }
                items(categories) { (category, count) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(if (selectedCategory == category) null else category) },
                        label = { Text("$category ($count)") }
                    )
                }
            }
        }

        // Results
        val filtered = if (selectedCategory != null) {
            searchResults.filter { it.category == selectedCategory }
        } else searchResults

        if (query.length < 2) {
            // Show category browse when no search query
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            MaterialSymbolIcon("search_hands_free"),
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            size = 48.dp
                        )
                        Text(
                            "Type at least 2 characters to search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Or browse by category above",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No results for \"$query\"", style = MaterialTheme.typography.bodyMedium)
                        Text("Try a different spelling or browse by category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { record ->
                SpeciesSearchResultCard(
                    record = record,
                    onSelect = { onSelectRecord(record) }
                )
            }
        }
    }
}

@Composable
private fun SpeciesSearchResultCard(
    record: SpeciesRecord,
    onSelect: () -> Unit
) {
    val color = FieldMindTheme.colors.accentFor(record.category.lowercase())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    FieldMindIcons.iconForCategory(record.category),
                    null,
                    tint = color,
                    size = 24.dp
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.commonName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    record.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (record.description.isNotBlank()) {
                    Text(
                        record.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            InfoChip(record.category, icon = FieldMindIcons.iconForCategory(record.category))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Loading & Error States
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingContent() {
    val transition = rememberInfiniteTransition(label = "identifySpin")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "spin"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 4.dp
        )
        Text(
            "Identifying species...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Analyzing photo against species database",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            MaterialSymbolIcon("error_outline", filled = true),
            null,
            tint = MaterialTheme.colorScheme.error,
            size = 56.dp
        )
        Text(
            "Couldn't identify species",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text("Close")
            }
            Button(onClick = { /* retry with different image */ }, shape = RoundedCornerShape(14.dp)) {
                Text("Try again")
            }
        }
    }
}
