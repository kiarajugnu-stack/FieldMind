package fieldmind.research.app.features.field.presentation.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.database.entity.DataRecordEntity
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Data Tools Hub — Main hub showing all 8 data tools as clickable cards
// ══════════════════════════════════════════════════════════════════════

private data class DataComparisonRow(val label: String, val items: List<String>)

private data class ToolCardInfo(
    val name: String,
    val description: String,
    val icon: MaterialSymbolIcon,
    val accentColor: androidx.compose.ui.graphics.Color,
    val screen: FieldMindScreen
)

@Composable
fun DataToolsHubScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val accentColor = FieldMindTheme.colors.data
    val tools = remember {
        listOf(
            ToolCardInfo("Counter", "Tally with live count", FieldMindIcons.Add, accentColor, FieldMindScreen.CounterTool),
            ToolCardInfo("Measurement", "Log with units", FieldMindIcons.Graph, accentColor, FieldMindScreen.MeasurementTool),
            ToolCardInfo("Weather Log", "Conditions record", FieldMindIcons.Weather, accentColor, FieldMindScreen.WeatherLogTool),
            ToolCardInfo("Species", "Quick observation", FieldMindIcons.Nature, accentColor, FieldMindScreen.SpeciesTool),
            ToolCardInfo("Checklist", "Track items", FieldMindIcons.Check, accentColor, FieldMindScreen.ChecklistTool),
            ToolCardInfo("Event Log", "Record events", FieldMindIcons.List, accentColor, FieldMindScreen.EventLogTool),
            ToolCardInfo("Site Log", "Visit conditions", FieldMindIcons.Map, accentColor, FieldMindScreen.SiteLogTool),
            ToolCardInfo("Comparison", "Species/samples", FieldMindIcons.Data, accentColor, FieldMindScreen.ComparisonTable)
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FieldScreenHeader(
                    "Data tools",
                    "Interactive tools for field data collection.",
                    icon = FieldMindIcons.Data,
                    actionIcon = FieldMindIcons.Back,
                    onAction = onBack
                )
            }

            // Tools in a 2-column grid
            tools.chunked(2).forEach { rowTools ->
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowTools.forEach { tool ->
                            ToolCardItem(
                                tool = tool,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(tool.screen) }
                            )
                        }
                        // Fill remaining space if odd number
                        if (rowTools.size < 2) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Summary of saved data records
            item {
                val dataRecords by viewModel.dataRecords.collectAsState()
                val counterCount = dataRecords.count { it.toolType == "Counter" }
                val measurementCount = dataRecords.count { it.toolType == "Measurement Log" }
                val weatherCount = dataRecords.count { it.toolType == "Weather Log" }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Saved records", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            RecordStat("Counter", counterCount, FieldMindIcons.Add)
                            RecordStat("Measure", measurementCount, FieldMindIcons.Graph)
                            RecordStat("Weather", weatherCount, FieldMindIcons.Weather)
                        }
                        Text(
                            "Total: ${dataRecords.size} records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCardItem(tool: ToolCardInfo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = tool.accentColor, size = 22.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tool.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecordStat(label: String, count: Int, icon: MaterialSymbolIcon) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = FieldMindTheme.colors.data, size = 18.dp)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Group 1: Interactive Data Tools — Dedicated mini-tool UIs
//  Each tool has a full-screen layout with its own save-to-datastore flow.
// ══════════════════════════════════════════════════════════════════════

// ══════════════════════════════════════════════════════════════════════
//  1. Counter Tool — Local tally with explicit save (no auto-save per tap)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun CounterToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val dataRecords by viewModel.dataRecords.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var count by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val counterRecords = remember(dataRecords) {
        dataRecords.filter { it.toolType == "Counter" }.sortedByDescending { it.timestamp }
    }

    // Pulse animation on the count
    val transition = rememberInfiniteTransition(label = "counterPulse" )
    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "counterScale"
    )

    fun saveCurrentCount() {
        if (count <= 0) return
        haptics.confirm()
        val labelToUse = label.ifBlank { "Counter tally" }
        viewModel.addCounter(labelToUse, count, "Manual save at $count") { success ->
            showFastSnackbar(snackbar, scope, if (success) "Saved count: $count" else "Failed to save — try again")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {}
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader(
                        "Counter",
                        "Tap +/− to tally. Tap Save to persist your count.",
                        icon = FieldMindIcons.Add,
                        actionIcon = FieldMindIcons.Back,
                        onAction = onBack
                    )
                }

                // ── Big counter display ──
                item {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Current count",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 72.sp
                                ),
                                color = FieldMindTheme.colors.data,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (count > 0) pulseScale else 1f
                                    scaleY = if (count > 0) pulseScale else 1f
                                }
                            )

                            // Stepper buttons — local only, no save
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (count > 0) {
                                            haptics.light()
                                            count--
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("−", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        haptics.confirm()
                                        count++
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("+", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Reset
                            TextButton(
                                onClick = {
                                    haptics.light()
                                    count = 0
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    "Reset to zero",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // ── Label + Save row ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Counter label",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FieldTextField(
                                value = label,
                                onValueChange = { label = it },
                                label = "What are you counting?",
                                supportingText = "e.g. Birds seen, Trees sampled, Steps taken"
                            )

                            // Save button — only persists when tapped
                            Button(
                                onClick = ::saveCurrentCount,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = count > 0
                            ) {
                                Icon(FieldMindIcons.Check, null, size = 18.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Save current count ($count)")
                            }

                            Text(
                                "Count is local until you tap Save. Each save creates one data record with the final count.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── History toggle ──
                if (counterRecords.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .clickable { showHistory = !showHistory }
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(FieldMindIcons.Data, null, tint = FieldMindTheme.colors.data, size = 20.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Saved tallies",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${counterRecords.size} saves",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        if (showHistory) FieldMindIcons.Up else FieldMindIcons.Down,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 20.dp
                                    )
                                }
                                if (showHistory) {
                                    counterRecords.take(20).forEach { record ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                record.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                record.value,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = FieldMindTheme.colors.data
                                            )
                                            Text(
                                                SimpleDateFormat("HH:mm", Locale.getDefault())
                                                    .format(Date(record.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        // Top snackbar overlay
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  2. Measurement Tool — Structured entry form with unit selector
// ══════════════════════════════════════════════════════════════════════

@Composable
fun MeasurementToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("cm") }
    var notes by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    val commonUnits = listOf("cm", "m", "mm", "km", "g", "kg", "°C", "°F", "%", "mL", "L", "ppm", "mg/L", "lux", "dB")
    var showUnitPicker by remember { mutableStateOf(false) }

    val canSave = label.isNotBlank() && value.isNotBlank()

    fun saveMeasurement() {
        if (!canSave) return
        haptics.confirm()
        val savedLabel = label.trim()
        val savedValue = value.trim()
        val savedUnit = unit.trim()
        viewModel.addDataRecord(
            toolType = "Measurement Log",
            label = savedLabel,
            value = savedValue,
            unit = savedUnit,
            notes = notes.trim(),
            location = location.trim(),
            datasetKind = "Measurements",
            chartPreference = "Line",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Measurement saved: $savedLabel = $savedValue $savedUnit" else "Failed to save measurement — try again")
            }
        )
        label = ""
        value = ""
        notes = ""
        location = ""
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {}
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader(
                        "Measurement",
                        "Log a structured measurement with units and notes.",
                        icon = FieldMindIcons.Graph,
                        actionIcon = FieldMindIcons.Back,
                        onAction = onBack
                    )
                }

                // ── Value + Unit entry ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Enter measurement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Large value display
                            Text(
                                if (value.isNotBlank()) "$value $unit" else "—",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                color = FieldMindTheme.colors.data,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Number pad style entry
                                NumberField(
                                    value = value,
                                    onValueChange = { value = it },
                                    label = "Value",
                                    modifier = Modifier.weight(1f),
                                    decimalPlaces = 2
                                )

                                // Unit selector
                                Box {
                                    OutlinedButton(
                                        onClick = { showUnitPicker = !showUnitPicker },
                                        modifier = Modifier.height(56.dp),
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Text(unit, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.size(4.dp))
                                        Icon(FieldMindIcons.Down, null, size = 16.dp)
                                    }
                                    DropdownMenu(
                                        expanded = showUnitPicker,
                                        onDismissRequest = { showUnitPicker = false }
                                    ) {
                                        commonUnits.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text(u) },
                                                onClick = { unit = u; showUnitPicker = false }
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick unit presets
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("cm", "m", "mm", "g", "kg", "°C").forEach { preset ->
                                    FilterChip(
                                        selected = unit == preset,
                                        onClick = { unit = preset },
                                        label = { Text(preset, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Button(
                                onClick = ::saveMeasurement,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = canSave
                            ) {
                                Icon(FieldMindIcons.Check, null, size = 18.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Save measurement")
                            }
                        }
                    }
                }

                // ── Details form ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FieldTextField(label, { label = it }, "What are you measuring?", supportingText = "e.g. Tree height, Water pH, Bird wing length")
                            FieldTextField(notes, { notes = it }, "Notes", minLines = 2, supportingText = "Method, instrument, conditions")
                            FieldTextField(location, { location = it }, "Location")
                        }
                    }
                }
            }
        }
        // Top snackbar overlay
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  3. Weather Log Tool — Quick weather conditions log
// ══════════════════════════════════════════════════════════════════════

@Composable
fun WeatherLogToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationProvider = remember { fieldmind.research.app.features.field.data.location.FieldLocationProvider(context) }

    var temperature by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Clear") }
    var humidity by remember { mutableStateOf("") }
    var windSpeed by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }

    val conditions = listOf("Clear", "Partly cloudy", "Overcast", "Foggy", "Drizzle", "Rain", "Snow", "Thunderstorm")

    fun saveWeatherLog() {
        haptics.confirm()
        val value = buildString {
            temperature.takeIf { it.isNotBlank() }?.let { append("${it}°C ") }
            append(condition)
            humidity.takeIf { it.isNotBlank() }?.let { append(" | ${it}% humidity") }
            windSpeed.takeIf { it.isNotBlank() }?.let { append(" | ${it} km/h wind") }
        }
        val savedCondition = condition
        viewModel.addDataRecord(
            toolType = "Weather Log",
            label = "Weather: $condition",
            value = value.trim(),
            unit = "",
            notes = notes.trim(),
            location = location.trim(),
            datasetKind = "Weather logs",
            chartPreference = "Line",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Weather log saved: $savedCondition" else "Failed to save weather log — try again")
            }
        )
    }

    fun fetchLocation() {
        locating = true
        locationProvider.requestCurrentLocation { captured ->
            locating = false
            if (captured != null) {
                location = captured.asDisplayText()
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                    if (!place.isNullOrBlank()) location = captured.copy(placeName = place).asDisplayText()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {}
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader(
                        "Weather log",
                        "Record current conditions at your location.",
                        icon = FieldMindIcons.Weather,
                        actionIcon = FieldMindIcons.Back,
                        onAction = onBack
                    )
                }

                // ── Condition picker ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Weather condition",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            OptionPickerField(label = "Condition", selected = condition, options = conditions, onSelected = { condition = it }, icon = FieldMindIcons.Info)

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NumberField(
                                    value = temperature,
                                    onValueChange = { temperature = it },
                                    label = "Temp (°C)",
                                    modifier = Modifier.weight(1f),
                                    decimalPlaces = 1,
                                    suffix = "°C"
                                )
                                NumberField(
                                    value = humidity,
                                    onValueChange = { humidity = it },
                                    label = "Humidity %",
                                    modifier = Modifier.weight(1f),
                                    decimalPlaces = 0,
                                    suffix = "%"
                                )
                            }

                            NumberField(
                                value = windSpeed,
                                onValueChange = { windSpeed = it },
                                label = "Wind speed (km/h)",
                                decimalPlaces = 1,
                                suffix = "km/h"
                            )
                        }
                    }
                }

                // ── Location & notes ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Location & notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldTextField(location, { location = it }, "Location", modifier = Modifier.weight(1f))
                                FilledTonalButton(
                                    onClick = { fetchLocation() },
                                    modifier = Modifier.height(56.dp),
                                    enabled = !locating
                                ) {
                                    if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Icon(FieldMindIcons.Location, null, size = 18.dp)
                                }
                            }
                            FieldTextField(notes, { notes = it }, "Notes", minLines = 2, supportingText = "Sky conditions, precipitation, visibility")
                        }
                    }
                }

                // ── Save ──
                item {
                    Button(
                        onClick = ::saveWeatherLog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Save weather log")
                    }
                }
            }
        }
        // Top snackbar overlay
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  4. Species Tool — Quick species observation log
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit = {},
    onOpenTaxonomicBrowser: () -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var speciesName by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("1") }
    var behavior by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf("Likely") }
    var habitat by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val behaviors = listOf("Feeding", "Flying", "Calling", "Resting", "Moving", "Nesting", "Interacting", "Hiding")
    val confidenceOptions = listOf("Certain", "Likely", "Unsure")

    val canSave = speciesName.isNotBlank()

    fun saveSpeciesObservation() {
        if (!canSave) return
        haptics.confirm()
        viewModel.addObservation(
            subject = speciesName.trim(),
            category = when {
                speciesName.contains(Regex("[Bb]ird|[Aa]vian|[Rr]aven|[Ss]parrow|[Cc]row|[Ee]agle|[Ff]alcon|[Hh]awk|[Oo]wl|[Mm]ockingbird|[Cc]ardinal")) -> "Bird"
                speciesName.contains(Regex("[Mm]ammal|[Dd]eer|[Ff]ox|[Bb]ear|[Rr]accoon|[Ss]quirrel|[Mm]ouse|[Rr]abbit|[Cc]oyote|[Ww]olf|[Bb]at")) -> "Mammal"
                speciesName.contains(Regex("[Ii]nsect|[Bb]ee|[Bb]utterfly|[Aa]nt|[Bb]eetle|[Mm]oth|[Dd]ragonfly|[Gg]rasshopper|[Cc]aterpillar|[Ll]arva")) -> "Insect"
                speciesName.contains(Regex("[Pp]lant|[Tt]ree|[Ff]lower|[Ll]eaf|[Ff]ern|[Mm]oss|[Gg]rass|[Ss]hrub|[Vv]ine")) -> "Plant"
                else -> "Other"
            },
            facts = buildString {
                append("Species: $speciesName")
                count.takeIf { it.isNotBlank() && it != "1" }?.let { append(", Count: $it") }
                behavior.takeIf { it.isNotBlank() }?.let { append(", Behavior: $it") }
                habitat.takeIf { it.isNotBlank() }?.let { append(", Habitat: $it") }
                notes.takeIf { it.isNotBlank() }?.let { append(", Notes: $it") }
            },
            confidence = confidence,
            manualLocation = "",
            tags = "species-tracking",
            evidence = "",
            context = "Species quick-capture"
        ) {
            scope.launch {
                showFastSnackbar(snackbar, scope, "$speciesName logged")
            }
            speciesName = ""
            count = "1"
            behavior = ""
            habitat = ""
            notes = ""
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {}
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader(
                        "Species log",
                        "Quick-capture a species observation with facts.",
                        icon = FieldMindIcons.Nature,
                        actionIcon = FieldMindIcons.Back,
                        onAction = onBack
                    )
                }

                // ── Species name + count ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Species identity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            FieldTextField(speciesName, { speciesName = it }, "Species name", supportingText = "e.g. American Robin, Red Oak, Monarch butterfly", required = true)

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberField(
                                    value = count,
                                    onValueChange = { count = it },
                                    label = "Count",
                                    modifier = Modifier.weight(1f),
                                    decimalPlaces = 0
                                )
                                // Stepper for quick count adjustment
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val c = (count.toIntOrNull() ?: 1) - 1
                                            if (c >= 0) count = c.toString()
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            val c = (count.toIntOrNull() ?: 0) + 1
                                            count = c.toString()
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                "Species confidence",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OptionPickerField(label = "Confidence", selected = confidence, options = confidenceOptions, onSelected = { confidence = it }, icon = FieldMindIcons.Check)
                        }
                    }
                }

                // ── Behavior & habitat ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Behavior & habitat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OptionPickerField(label = "Behavior", selected = behavior, options = behaviors, onSelected = { behavior = it }, icon = FieldMindIcons.Trend)
                            FieldTextField(habitat, { habitat = it }, "Habitat / substrate", supportingText = "e.g. Woodland edge, Garden pond, Decaying log")
                            FieldTextField(notes, { notes = it }, "Additional notes", minLines = 2, supportingText = "Plumage, markings, size, sounds, or other ID clues")
                        }
                    }
                }

                // ── Browse species catalog ──
                item {
                    OutlinedButton(
                        onClick = onOpenBrowser,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(FieldMindIcons.Nature, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Browse species catalog")
                    }
                }

                // ── Browse by taxonomic hierarchy ──
                item {
                    OutlinedButton(
                        onClick = onOpenTaxonomicBrowser,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(FieldMindIcons.Category, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Browse by taxonomy (Kingdom → Species)")
                    }
                }

                item {
                    Button(
                        onClick = ::saveSpeciesObservation,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canSave
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Save species observation")
                    }
                }
            }
        }
        // Top snackbar overlay
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  5. Checklist Tool — Simple checklist with add/check/delete
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ChecklistToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var items by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var newItemText by remember { mutableStateOf("") }

    fun addItem() {
        val text = newItemText.trim()
        if (text.isBlank()) return
        haptics.light()
        items = items + (text to false)
        newItemText = ""
    }

    fun toggleItem(index: Int) {
        haptics.light()
        items = items.toMutableList().also { it[index] = it[index].first to !it[index].second }
    }

    fun removeItem(index: Int) {
        haptics.confirm()
        items = items.toMutableList().also { it.removeAt(index) }
    }

    fun saveChecklist() {
        if (items.isEmpty()) return
        haptics.confirm()
        val checkedCount = items.count { it.second }
        val itemCount = items.size
        val summary = items.joinToString("; ") { (name, checked) -> "${if (checked) "✓" else "○"} $name" }
        viewModel.addDataRecord(
            toolType = "Checklist",
            label = "Checklist (${checkedCount}/${itemCount} checked)",
            value = summary,
            unit = "",
            notes = "",
            datasetKind = "Checklists",
            chartPreference = "Bar",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Checklist saved (${checkedCount}/${itemCount})" else "Failed to save checklist — try again")
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {}
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader(
                        "Checklist",
                        "Add items, check them off, and save as a data record.",
                        icon = FieldMindIcons.Check,
                        actionIcon = FieldMindIcons.Back,
                        onAction = onBack
                    )
                }

                // ── Add item input ──
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add item", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldTextField(
                                    value = newItemText,
                                    onValueChange = { newItemText = it },
                                    label = "Item name",
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = ::addItem,
                                    modifier = Modifier.height(56.dp),
                                    enabled = newItemText.isNotBlank(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── Checklist items ──
                if (items.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Items (${items.count { it.second }}/${items.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                items.forEachIndexed { index, (text, checked) ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = { toggleItem(index) })
                                        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                                        IconButton(onClick = { removeItem(index) }, modifier = Modifier.size(36.dp)) {
                                            Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = ::saveChecklist,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(FieldMindIcons.Check, null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Save checklist (${items.count { it.second }}/${items.size})")
                        }
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Add items above to build your checklist.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        FieldMindSnackbarOverlay(hostState = snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════
//  6. Event Log Tool — Record notable events with date/category/notes
// ══════════════════════════════════════════════════════════════════════

@Composable
fun EventLogToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sighting") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val categories = listOf("Sighting", "Discovery", "Change", "Visit", "Weather Event", "Other")

    fun saveEvent() {
        if (title.isBlank()) return
        haptics.confirm()
        val savedTitle = title.trim()
        viewModel.addDataRecord(
            toolType = "Event Log",
            label = savedTitle,
            value = "$category | $date",
            unit = "",
            notes = "$description\nDate: $date",
            datasetKind = "Events",
            chartPreference = "Bar",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Event saved: $savedTitle" else "Failed to save event — try again")
            }
        )
        title = ""
        description = ""
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = {}) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader("Event log", "Record a notable event with category and notes.", icon = FieldMindIcons.List, actionIcon = FieldMindIcons.Back, onAction = onBack)
                }
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Event details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            FieldTextField(title, { title = it }, "Event title", required = true, supportingText = "e.g. First monarch sighting of season")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldTextField(date, { date = it }, "Date", modifier = Modifier.weight(1f))
                                Box {
                                    OutlinedButton(onClick = { showCategoryPicker = true }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(18.dp)) {
                                        Text(category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.size(4.dp))
                                        Icon(FieldMindIcons.Down, null, size = 14.dp)
                                    }
                                    DropdownMenu(expanded = showCategoryPicker, onDismissRequest = { showCategoryPicker = false }) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; showCategoryPicker = false })
                                        }
                                    }
                                }
                            }
                            FieldTextField(description, { description = it }, "Description", minLines = 3, supportingText = "What happened, where, and any notable details")
                            Button(onClick = ::saveEvent, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = title.isNotBlank()) {
                                Icon(FieldMindIcons.Check, null, size = 18.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Save event")
                            }
                        }
                    }
                }
            }
        }
        FieldMindSnackbarOverlay(hostState = snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════
//  7. Site Log Tool — Log site visits with conditions and purpose
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SiteLogToolScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var siteName by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Survey") }
    var conditions by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    val purposes = listOf("Survey", "Monitoring", "Collection", "Observation", "Maintenance", "Exploration")

    fun saveSiteLog() {
        if (siteName.isBlank()) return
        haptics.confirm()
        val savedName = siteName.trim()
        viewModel.addDataRecord(
            toolType = "Site Log",
            label = savedName,
            value = "$purpose | Duration: ${duration.ifBlank { "N/A" }}",
            unit = "",
            notes = "Conditions: $conditions\nFindings: $findings",
            datasetKind = "Site visits",
            chartPreference = "Bar",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Site log saved: $savedName" else "Failed to save site log — try again")
            }
        )
        siteName = ""
        conditions = ""
        findings = ""
        duration = ""
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = {}) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader("Site log", "Record a site visit with purpose, conditions, and findings.", icon = FieldMindIcons.Map, actionIcon = FieldMindIcons.Back, onAction = onBack)
                }
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Site visit details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            FieldTextField(siteName, { siteName = it }, "Site name", required = true, supportingText = "e.g. North Meadow, Creek Bend")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldTextField(duration, { duration = it }, "Duration", modifier = Modifier.weight(1f), supportingText = "e.g. 2 hours")
                                OptionPickerField(label = "Purpose", selected = purpose, options = purposes, onSelected = { purpose = it }, icon = FieldMindIcons.Info, modifier = Modifier.weight(1f))
                            }
                            FieldTextField(conditions, { conditions = it }, "Conditions", supportingText = "Weather, terrain, accessibility notes")
                            FieldTextField(findings, { findings = it }, "Key findings", minLines = 2, supportingText = "What did you observe or collect?")
                            Button(onClick = ::saveSiteLog, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = siteName.isNotBlank()) {
                                Icon(FieldMindIcons.Check, null, size = 18.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Save site log")
                            }
                        }
                    }
                }
            }
        }
        FieldMindSnackbarOverlay(hostState = snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════
//  8. Comparison Table — Compare multiple items side by side
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ComparisonTableScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var tableName by remember { mutableStateOf("") }
    var columnCount by remember { mutableIntStateOf(2) }
    var rows by remember { mutableStateOf(listOf<DataComparisonRow>()) }
    var newRowLabel by remember { mutableStateOf("") }
    var newRowValues by remember { mutableStateOf(List(2) { "" }) }

    fun addRow() {
        val label = newRowLabel.trim()
        if (label.isBlank()) return
        haptics.light()
        val values = newRowValues.map { it.ifBlank { "—" } }
        rows = rows + DataComparisonRow(label, values)
        newRowLabel = ""
        newRowValues = List(columnCount) { "" }
    }

    fun saveComparison() {
        if (rows.isEmpty() || tableName.isBlank()) return
        haptics.confirm()
        val savedName = tableName.trim()
        val summary = rows.joinToString("; ") { row ->
            "${row.label}: ${row.items.joinToString(" vs ")}"
        }
        viewModel.addDataRecord(
            toolType = "Comparison Table",
            label = savedName,
            value = summary,
            unit = "",
            notes = "$columnCount columns, ${rows.size} rows",
            datasetKind = "Comparisons",
            chartPreference = "Bar",
            onResult = { success ->
                showFastSnackbar(snackbar, scope, if (success) "Table saved: $savedName" else "Failed to save table — try again")
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = {}) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FieldScreenHeader("Comparison table", "Compare species, samples, or sites side by side.", icon = FieldMindIcons.Data, actionIcon = FieldMindIcons.Back, onAction = onBack)
                }
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Table setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            FieldTextField(tableName, { tableName = it }, "Table name", required = true, supportingText = "e.g. Bird species comparison")
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Columns:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                FilledTonalIconButton(onClick = { if (columnCount > 1) { columnCount-- } }, modifier = Modifier.size(36.dp)) {
                                    Text("−", fontWeight = FontWeight.Bold)
                                }
                                Text("$columnCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                FilledTonalIconButton(onClick = { if (columnCount < 5) { columnCount++ } }, modifier = Modifier.size(36.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── Add row ──
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add row", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            FieldTextField(newRowLabel, { newRowLabel = it }, "Row label (e.g. species name)")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (0 until columnCount).forEach { col ->
                                    OutlinedTextField(
                                        value = if (col < newRowValues.size) newRowValues[col] else "",
                                        onValueChange = { v ->
                                            newRowValues = newRowValues.toMutableList().also { while (it.size <= col) it.add(""); it[col] = v }
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        placeholder = { Text("Item ${col + 1}", style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }
                            FilledTonalButton(onClick = ::addRow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = newRowLabel.isNotBlank()) {
                                Text("Add row", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Table preview ──
                if (rows.isNotEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Table (${rows.size} rows)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                rows.forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(row.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        row.items.forEach { item ->
                                            Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                                Spacer(Modifier.size(8.dp))
                                Button(onClick = ::saveComparison, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = tableName.isNotBlank() && rows.isNotEmpty()) {
                                    Icon(FieldMindIcons.Check, null, size = 18.dp)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Save comparison table")
                                }
                            }
                        }
                    }
                }
            }
        }
        FieldMindSnackbarOverlay(hostState = snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp))
    }
}
