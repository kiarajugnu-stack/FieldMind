package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon

// ─── Data models ──────────────────────────────────────────────────────

/**
 * Represents a broad settings category with an icon and label.
 */
private data class SettingsCategory(
    val id: String,
    val icon: MaterialSymbolIcon,
    val label: String,
    val description: String,
    val accentColor: Color
)

/**
 * Represents an individual setting item within a category.
 */
private data class SettingItem(
    val id: String,
    val icon: MaterialSymbolIcon,
    val title: String,
    val subtitle: String,
    val categoryId: String,
    val isToggle: Boolean = false,
    val toggleState: Boolean = false,
    val onToggle: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

// ─── Category definitions ─────────────────────────────────────────────

private val categories = listOf(
    SettingsCategory("profile", FieldMindIcons.User, "Profile & Account", "Your research identity and preferences", Color(0xFF4CAF50)),
    SettingsCategory("display", MaterialSymbolIcon("palette"), "Display & Theme", "Appearance, units, and map settings", Color(0xFF2196F3)),
    SettingsCategory("capture", FieldMindIcons.Camera, "Data & Capture", "Observation defaults, weather, species tools", Color(0xFFFF9800)),
    SettingsCategory("ai", MaterialSymbolIcon("smart_toy"), "AI & Intelligence", "AI assistant, local models, auto-generation", Color(0xFF9C27B0)),
    SettingsCategory("data", MaterialSymbolIcon("storage"), "Data & Storage", "Backup, export, and data integrity", Color(0xFF00BCD4)),
    SettingsCategory("security", FieldMindIcons.Lock, "Security & Privacy", "App lock, privacy controls, screen protection", Color(0xFFF44336)),
    SettingsCategory("advanced", FieldMindIcons.Settings, "Advanced", "Developer tools, debugging, about", Color(0xFF607D8B))
)

// ─── Setting items (flat list for search) ─────────────────────────────

private fun buildSettingItems(
    onOpenProfile: (() -> Unit)?,
    onOpenAppearance: (() -> Unit)?,
    onOpenUnits: (() -> Unit)?,
    onOpenCapture: (() -> Unit)?,
    onOpenWeather: (() -> Unit)?,
    onOpenSpeciesId: (() -> Unit)?,
    onOpenSpeciesPacks: (() -> Unit)?,
    onOpenAi: (() -> Unit)?,
    onOpenLocalModel: (() -> Unit)?,
    onOpenAutoGen: (() -> Unit)?,
    onOpenBackup: (() -> Unit)?,
    onOpenExport: (() -> Unit)?,
    onOpenDataIntegrity: (() -> Unit)?,
    onOpenSecurity: (() -> Unit)?,
    onOpenScreenVisibility: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    onOpenDeveloper: (() -> Unit)?,
    onOpenChangelog: (() -> Unit)?,
    onOpenAbout: (() -> Unit)?,
    onOpenScreenCaptureProtection: (() -> Unit)? = null,
    dynamicColorEnabled: Boolean = false,
    onDynamicColorToggle: ((Boolean) -> Unit)? = null,
    themeMode: String = "Dark",
    onThemeChange: ((String) -> Unit)? = null
): List<SettingItem> = buildList {
    // Profile & Account
    if (onOpenProfile != null) add(SettingItem("profile_main", FieldMindIcons.User, "Research Profile", "Name, role, expertise", "profile", onClick = onOpenProfile))

    // Display & Theme
    if (onOpenAppearance != null) add(SettingItem("appearance", MaterialSymbolIcon("palette"), "Appearance", "Theme, colors, layout", "display", onClick = onOpenAppearance))
    if (onOpenUnits != null) add(SettingItem("units", FieldMindIcons.Data, "Units & Format", "Distance, temperature, date/time", "display", onClick = onOpenUnits))
    if (onOpenMap != null) add(SettingItem("map", FieldMindIcons.Map, "Map Settings", "Map type, location display", "display", onClick = onOpenMap))

    // Data & Capture
    if (onOpenCapture != null) add(SettingItem("capture_defaults", FieldMindIcons.Camera, "Capture Defaults", "Category, confidence, media, GPS", "capture", onClick = onOpenCapture))
    if (onOpenWeather != null) add(SettingItem("weather", FieldMindIcons.Weather, "Weather", "Providers, display, units, auto-fetch", "capture", onClick = onOpenWeather))
    if (onOpenSpeciesId != null) add(SettingItem("species_id", MaterialSymbolIcon("pets"), "Species ID", "API keys, offline mode", "capture", onClick = onOpenSpeciesId))
    if (onOpenSpeciesPacks != null) add(SettingItem("species_packs", FieldMindIcons.Archive, "Species Packs", "Download & manage catalogs", "capture", onClick = onOpenSpeciesPacks))

    // AI & Intelligence
    if (onOpenAi != null) add(SettingItem("ai_assistant", MaterialSymbolIcon("smart_toy"), "AI Assistant", "Provider, model, API keys", "ai", onClick = onOpenAi))
    if (onOpenLocalModel != null) add(SettingItem("local_model", MaterialSymbolIcon("memory"), "Local Model", "On-device AI, downloads", "ai", onClick = onOpenLocalModel))
    if (onOpenAutoGen != null) add(SettingItem("auto_gen", MaterialSymbolIcon("auto_awesome"), "Auto Generation", "Patterns, flashcards, questions", "ai", onClick = onOpenAutoGen))

    // Data & Storage
    if (onOpenBackup != null) add(SettingItem("backup", FieldMindIcons.Archive, "Backup & Restore", "Auto-backup, export, import", "data", onClick = onOpenBackup))
    if (onOpenExport != null) add(SettingItem("export", FieldMindIcons.Export, "Export Settings", "Format, GPS privacy, media", "data", onClick = onOpenExport))
    if (onOpenDataIntegrity != null) add(SettingItem("data_integrity", MaterialSymbolIcon("verified"), "Data Integrity", "Validation, repair", "data", onClick = onOpenDataIntegrity))

    // Security & Privacy
    if (onOpenSecurity != null) add(SettingItem("security", FieldMindIcons.Lock, "App Security", "PIN lock, timeout, background lock", "security", onClick = onOpenSecurity))
    if (onOpenScreenVisibility != null) add(SettingItem("screen_visibility", FieldMindIcons.Visibility, "Screen Visibility", "Show/hide tabs and features", "security", onClick = onOpenScreenVisibility))
    if (onOpenScreenCaptureProtection != null) add(SettingItem("screen_capture", MaterialSymbolIcon("privacy_tip"), "Screen Protection", "Capture prevention, always-on", "security", onClick = onOpenScreenCaptureProtection))

    // Advanced
    if (onOpenDeveloper != null) add(SettingItem("developer", MaterialSymbolIcon("code"), "Developer", "Debug logging, testing tools", "advanced", onClick = onOpenDeveloper))
    if (onOpenChangelog != null) add(SettingItem("changelog", MaterialSymbolIcon("history"), "Changelog", "What's new in recent updates", "advanced", onClick = onOpenChangelog))
    if (onOpenAbout != null) add(SettingItem("about", FieldMindIcons.Info, "About", "Version, licenses, privacy", "advanced", onClick = onOpenAbout))
}

// ─── Main Settings Screen ─────────────────────────────────────────────

@Composable
fun FieldMindSettingsNewScreen(
    viewModel: FieldMindViewModel? = null,
    onBack: () -> Unit,
    onResetOnboarding: () -> Unit = {},
    onOpenExport: (() -> Unit)? = null,
    onOpenAbout: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenAppearance: (() -> Unit)? = null,
    onOpenCapture: (() -> Unit)? = null,
    onOpenAi: (() -> Unit)? = null,
    onOpenLocalModel: (() -> Unit)? = null,
    onOpenBackup: (() -> Unit)? = null,
    onOpenSecurity: (() -> Unit)? = null,
    onOpenChangelog: (() -> Unit)? = null,
    onOpenUnits: (() -> Unit)? = null,
    onOpenWeather: (() -> Unit)? = null,
    onOpenMap: (() -> Unit)? = null,
    onOpenDataIntegrity: (() -> Unit)? = null,
    onOpenDeveloper: (() -> Unit)? = null,
    onOpenSpeciesPacks: (() -> Unit)? = null,
    onOpenSpeciesId: (() -> Unit)? = null,
    onOpenAutoGen: (() -> Unit)? = null,
    onOpenScreenVisibility: (() -> Unit)? = null,
    onOpenScreenCaptureProtection: (() -> Unit)? = null
) {
    val colors = FieldMindTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Theme state from viewModel
    val settings = viewModel?.fieldSettings
    val dynamicColorEnabled by settings?.dynamicColorEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    val themeMode by settings?.themeMode?.collectAsState() ?: remember { mutableStateOf("Dark") }

    // Build all setting items
    val allItems = remember(
        onOpenProfile, onOpenAppearance, onOpenUnits, onOpenCapture, onOpenWeather,
        onOpenSpeciesId, onOpenSpeciesPacks, onOpenAi, onOpenLocalModel, onOpenAutoGen,
        onOpenBackup, onOpenExport, onOpenDataIntegrity, onOpenSecurity, onOpenScreenVisibility,
        onOpenMap, onOpenDeveloper, onOpenChangelog, onOpenAbout, onOpenScreenCaptureProtection
    ) {
        buildSettingItems(
            onOpenProfile = onOpenProfile,
            onOpenAppearance = onOpenAppearance,
            onOpenUnits = onOpenUnits,
            onOpenCapture = onOpenCapture,
            onOpenWeather = onOpenWeather,
            onOpenSpeciesId = onOpenSpeciesId,
            onOpenSpeciesPacks = onOpenSpeciesPacks,
            onOpenAi = onOpenAi,
            onOpenLocalModel = onOpenLocalModel,
            onOpenAutoGen = onOpenAutoGen,
            onOpenBackup = onOpenBackup,
            onOpenExport = onOpenExport,
            onOpenDataIntegrity = onOpenDataIntegrity,
            onOpenSecurity = onOpenSecurity,
            onOpenScreenVisibility = onOpenScreenVisibility,
            onOpenMap = onOpenMap,
            onOpenDeveloper = onOpenDeveloper,
            onOpenChangelog = onOpenChangelog,
            onOpenAbout = onOpenAbout,
            onOpenScreenCaptureProtection = onOpenScreenCaptureProtection,
            dynamicColorEnabled = dynamicColorEnabled,
            themeMode = themeMode
        )
    }

    // Filter items based on search or category
    val filteredItems = remember(searchQuery, selectedCategory, allItems) {
        allItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() || 
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.categoryId.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.categoryId == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Show categories when no search and no category selected
    val showCategories = searchQuery.isBlank() && selectedCategory == null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                StandardScreenHeader(
                    title = "Settings",
                    subtitle = "Customize your FieldMind experience",
                    icon = FieldMindIcons.Settings,
                    trailing = { BackButton(onClick = onBack) }
                )
            }

            // ── Search bar ──
            item {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it; selectedCategory = null },
                    isActive = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = "Search settings…"
                )
            }

            // ── Category pills (horizontal scroll) ──
            if (showCategories) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            Surface(
                                onClick = { selectedCategory = cat.id },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedCategory == cat.id) cat.accentColor.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selectedCategory == cat.id) androidx.compose.foundation.BorderStroke(1.5.dp, cat.accentColor) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(cat.icon, null, tint = cat.accentColor, size = 20.dp)
                                    Text(
                                        cat.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Category cards ──
                categories.forEach { cat ->
                    val catItems = allItems.filter { it.categoryId == cat.id }
                    if (catItems.isNotEmpty()) {
                        item {
                            CategoryCard(
                                category = cat,
                                items = catItems,
                                onItemClick = { it.onClick?.invoke() },
                                onCategoryClick = { selectedCategory = cat.id }
                            )
                        }
                    }
                }
            }

            // ── Filtered items view (when searching or category selected) ──
            if (!showCategories) {
                // Back to categories button
                if (selectedCategory != null && searchQuery.isBlank()) {
                    item {
                        Surface(
                            onClick = { selectedCategory = null },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(MaterialSymbolIcon("arrow_back"), null, size = 18.dp)
                                Text(
                                    categories.find { it.id == selectedCategory }?.label ?: "All Categories",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Show search results or category items
                if (filteredItems.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(FieldMindIcons.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 32.dp)
                                Text("No settings found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Try a different search term", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        val cat = categories.find { it.id == item.categoryId }
                        SettingItemCard(
                            item = item,
                            accentColor = cat?.accentColor ?: MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Reset onboarding (always visible at bottom) ──
            item {
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onResetOnboarding,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(FieldMindIcons.Refresh, null, tint = MaterialTheme.colorScheme.error, size = 20.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Reset onboarding", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Show welcome screens again", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                        }
                        Icon(MaterialSymbolIcon("chevron_right"), null, tint = MaterialTheme.colorScheme.onErrorContainer, size = 20.dp)
                    }
                }
            }
        }
    }
}

// ─── Search Bar ───────────────────────────────────────────────────────

@Composable
private fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    placeholder: String = "Search…"
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (isActive) 2.dp else 0.dp,
        shadowElevation = if (isActive) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onActiveChange(true) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(FieldMindIcons.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(FieldMindIcons.Close, "Clear", size = 18.dp)
                }
            }
        }
    }
}

// ─── Category Card ────────────────────────────────────────────────────

@Composable
private fun CategoryCard(
    category: SettingsCategory,
    items: List<SettingItem>,
    onItemClick: (SettingItem) -> Unit,
    onCategoryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Category header
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCategoryClick).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(category.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(category.icon, null, tint = category.accentColor, size = 20.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(category.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(MaterialSymbolIcon("chevron_right"), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), size = 20.dp)
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Items within the category
            items.forEach { item ->
                Surface(
                    onClick = { onItemClick(item) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(item.icon, null, tint = category.accentColor, size = 20.dp)
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(MaterialSymbolIcon("chevron_right"), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 18.dp)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ─── Setting Item Card (for search/category drill-down) ───────────────

@Composable
private fun SettingItemCard(
    item: SettingItem,
    accentColor: Color
) {
    val onClick = item.onClick
    Surface(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        enabled = !item.isToggle && onClick != null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = accentColor, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.isToggle && item.onToggle != null) {
                    Spacer(Modifier.height(6.dp))
                    Switch(
                        checked = item.toggleState,
                        onCheckedChange = { item.onToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            if (!item.isToggle && onClick != null) {
                Icon(MaterialSymbolIcon("chevron_right"), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 20.dp)
            }
        }
    }
}
