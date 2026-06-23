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
import kotlinx.coroutines.launch
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ── Taxonomic level model ──
private enum class TaxoLevel(val label: String, val plural: String) {
    Kingdom("Kingdom", "Kingdoms"),
    Phylum("Phylum", "Phyla"),
    Class("Class", "Classes"),
    Order("Order", "Orders"),
    Family("Family", "Families"),
    Genus("Genus", "Genera"),
    Species("Species", "Species")
}

// ── Breadcrumb item ──
private data class Breadcrumb(
    val level: TaxoLevel,
    val value: String
)

// ── Color helper ──
@Composable
private fun levelColor(level: TaxoLevel): androidx.compose.ui.graphics.Color = when (level) {
    TaxoLevel.Kingdom -> FieldMindTheme.colors.observation
    TaxoLevel.Phylum -> FieldMindTheme.colors.info
    TaxoLevel.Class -> FieldMindTheme.colors.project
    TaxoLevel.Order -> FieldMindTheme.colors.data
    TaxoLevel.Family -> FieldMindTheme.colors.hypothesis
    TaxoLevel.Genus -> FieldMindTheme.colors.report
    TaxoLevel.Species -> FieldMindTheme.colors.flashcard
}

// ══════════════════════════════════════════════════════════════════════
//  Taxonomic Browser — drill-down hierarchy (Kingdom → ... → Species)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TaxonomicBrowserScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }

    var breadcrumbs by rememberSaveable { mutableStateOf<List<Breadcrumb>>(emptyList()) }
    var currentLevel by rememberSaveable { mutableStateOf(TaxoLevel.Kingdom) }
    var siblings by remember { mutableStateOf<List<String>>(emptyList()) }
    var speciesList by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // ── Go up one level ──
    fun goUp() {
        if (breadcrumbs.isEmpty()) {
            onBack()
            return
        }
        val last = breadcrumbs.last()
        breadcrumbs = breadcrumbs.dropLast(1)
        val prevLevel = last.level
        currentLevel = prevLevel
    }

    BackHandler(enabled = true) { goUp() }

    // ── Load the current level's items ──
    fun loadLevel() {
        when (currentLevel) {
            TaxoLevel.Kingdom -> {
                scope.launch {
                    siblings = database.getKingdoms()
                    isLoading = false
                }
            }
            TaxoLevel.Phylum -> {
                val kingdom = breadcrumbs.firstOrNull { it.level == TaxoLevel.Kingdom }?.value ?: return
                scope.launch {
                    siblings = database.getPhyla(kingdom)
                    isLoading = false
                }
            }
            TaxoLevel.Class -> {
                val phylum = breadcrumbs.firstOrNull { it.level == TaxoLevel.Phylum }?.value ?: return
                scope.launch {
                    siblings = database.getClasses(phylum)
                    isLoading = false
                }
            }
            TaxoLevel.Order -> {
                val clss = breadcrumbs.firstOrNull { it.level == TaxoLevel.Class }?.value ?: return
                scope.launch {
                    siblings = database.getOrders(clss)
                    isLoading = false
                }
            }
            TaxoLevel.Family -> {
                val order = breadcrumbs.firstOrNull { it.level == TaxoLevel.Order }?.value ?: return
                scope.launch {
                    siblings = database.getFamilies(order)
                    isLoading = false
                }
            }
            TaxoLevel.Genus -> {
                val family = breadcrumbs.firstOrNull { it.level == TaxoLevel.Family }?.value ?: return
                scope.launch {
                    siblings = database.getGenera(family)
                    isLoading = false
                }
            }
            TaxoLevel.Species -> {
                val genus = breadcrumbs.firstOrNull { it.level == TaxoLevel.Genus }?.value ?: return
                val kingdom = breadcrumbs.firstOrNull { it.level == TaxoLevel.Kingdom }?.value
                val phylum = breadcrumbs.firstOrNull { it.level == TaxoLevel.Phylum }?.value
                val clss = breadcrumbs.firstOrNull { it.level == TaxoLevel.Class }?.value
                val order = breadcrumbs.firstOrNull { it.level == TaxoLevel.Order }?.value
                val family = breadcrumbs.firstOrNull { it.level == TaxoLevel.Family }?.value
                scope.launch {
                    speciesList = database.getSpeciesByTaxonomy(
                        kingdom = kingdom,
                        phylum = phylum,
                        category = clss,
                        order = order,
                        family = family,
                        genus = genus
                    )
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(currentLevel, breadcrumbs.size) {
        isLoading = true
        speciesList = emptyList()
        loadLevel()
    }

    // ── Drill into a sub-level ──
    fun drillDown(value: String) {
        breadcrumbs = breadcrumbs + Breadcrumb(currentLevel, value)
        val nextLevel = TaxoLevel.values()[currentLevel.ordinal + 1]
        currentLevel = nextLevel
    }

    // ── Jump back to a specific breadcrumb ──
    fun jumpTo(index: Int) {
        breadcrumbs = breadcrumbs.take(index)
        val targetLevel = if (index == 0) TaxoLevel.Kingdom else TaxoLevel.values()[index]
        currentLevel = targetLevel
    }

    val taxonListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Header row
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { goUp() },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (breadcrumbs.isEmpty()) FieldMindIcons.Back else FieldMindIcons.Up,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    size = 22.dp
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                currentLevel.plural,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (breadcrumbs.isEmpty()) "Browse by taxonomic hierarchy"
                                else "${breadcrumbs.joinToString(" › ") { it.value }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    // Breadcrumb trail
                    if (breadcrumbs.isNotEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            breadcrumbs.forEachIndexed { index, crumb ->
                                val accent = levelColor(crumb.level)
                                Surface(
                                    onClick = { jumpTo(index) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = accent.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            crumb.value,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accent
                                        )
                                        if (index < breadcrumbs.size - 1) {
                                            Icon(
                                                FieldMindIcons.Forward,
                                                null,
                                                tint = accent.copy(alpha = 0.5f),
                                                size = 12.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentLevel == TaxoLevel.Species) {
            // Show species list at the leaf level
            if (speciesList.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 48.dp)
                        Text("No species found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    state = taxonListState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${speciesList.size} species",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(speciesList, key = { it.id }) { record ->
                        SpeciesCard(
                            record = record,
                            onClick = { onOpenDetail(record.id) }
                        )
                    }
                }
            }
        } else {
            // Show list of taxon names at this level
            if (siblings.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 48.dp)
                        Text("No ${currentLevel.plural.lowercase()} found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    state = taxonListState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${siblings.size} ${currentLevel.plural.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(siblings, key = { it }) { name ->
                        TaxoLevelCard(
                            name = name,
                            level = currentLevel,
                            onClick = { drillDown(name) }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Taxon Level Card — shows a taxonomic group name with drill-down arrow
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TaxoLevelCard(
    name: String,
    level: TaxoLevel,
    onClick: () -> Unit
) {
    val accent = levelColor(level)

    Card(
        shape = RoundedCornerShape(18.dp),
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
            // Level icon badge
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    taxonIcon(level),
                    null,
                    tint = accent,
                    size = 24.dp
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    level.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Forward arrow
            Icon(
                FieldMindIcons.Forward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                size = 20.dp
            )
        }
    }
}

// ── Taxon level icon ──
@Composable
private fun taxonIcon(level: TaxoLevel): MaterialSymbolIcon = when (level) {
    TaxoLevel.Kingdom -> FieldMindIcons.Nature
    TaxoLevel.Phylum -> FieldMindIcons.Category
    TaxoLevel.Class -> FieldMindIcons.Category
    TaxoLevel.Order -> FieldMindIcons.List
    TaxoLevel.Family -> FieldMindIcons.Category
    TaxoLevel.Genus -> FieldMindIcons.Nature
    TaxoLevel.Species -> FieldMindIcons.Nature
}
