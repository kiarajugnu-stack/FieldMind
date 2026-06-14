package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun WeatherDatabaseScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val colors = FieldMindTheme.colors

    // ── Weather display preferences ──
    val showTemp by viewModel.fieldSettings.weatherShowTemperature.collectAsState()
    val showCondition by viewModel.fieldSettings.weatherShowCondition.collectAsState()
    val showHumidity by viewModel.fieldSettings.weatherShowHumidity.collectAsState()
    val showWind by viewModel.fieldSettings.weatherShowWind.collectAsState()
    val showCloudPref by viewModel.fieldSettings.weatherShowCloudCover.collectAsState()
    val showPressure by viewModel.fieldSettings.weatherShowPressure.collectAsState()

    // ── Live current weather with auto-refresh ──
    var currentWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherError by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var dashboardPlaceName by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    // Refresh on screen open and every 30 minutes
    LaunchedEffect(Unit) {
        while (true) {
            isRefreshing = true
            val snapshot = viewModel.refreshWeatherFromLocation()
            currentWeather = snapshot
            weatherError = snapshot == null
            isRefreshing = false
            
            // Resolve place name for display
            if (snapshot != null) {
                val locProvider = runCatching { FieldLocationProvider(ctx) }.getOrNull()
                if (locProvider != null && locProvider.hasAnyLocationPermission()) {
                    locProvider.lastKnownLocation()?.let { loc ->
                        locProvider.resolvePlaceName(loc.latitude, loc.longitude) { place ->
                            dashboardPlaceName = place
                        }
                    }
                }
            }
            delay(30 * 60 * 1000L) // 30 minutes
        }
    }

    // ── Daily analysis ──
    val weatherObs = remember(observations) {
        observations.filter { it.weatherTemperature != null }
            .sortedByDescending { it.timestamp }
    }
    
    // Group by date for day switching
    val dailyGroups = remember(weatherObs) {
        weatherObs.groupBy { it.date }.entries.sortedByDescending { it.key }
    }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    
    // Get the stats for the currently selected day
    val dayStats = remember(weatherObs, selectedDayIndex, dailyGroups) {
        val dayObs = if (dailyGroups.isNotEmpty() && selectedDayIndex < dailyGroups.size) {
            dailyGroups[selectedDayIndex].value
        } else {
            weatherObs
        }
        val temps = dayObs.mapNotNull<ObservationEntity, Double> { it.weatherTemperature }
        val humidities = dayObs.mapNotNull<ObservationEntity, Double> { it.weatherHumidity?.toDouble() }
        val windSpeeds = dayObs.mapNotNull<ObservationEntity, Double> { it.weatherWindSpeed }
        DayStats(
            dayObs.size,
            temps.takeIf { it.isNotEmpty() }?.average(),
            temps.minOrNull(),
            temps.maxOrNull(),
            humidities.takeIf { it.isNotEmpty() }?.average(),
            windSpeeds.takeIf { it.isNotEmpty() }?.average()
        )
    }
    
    // Filter observations for selected day
    val filteredObs = remember(weatherObs, selectedDayIndex, dailyGroups) {
        if (dailyGroups.isNotEmpty() && selectedDayIndex < dailyGroups.size) {
            dailyGroups[selectedDayIndex].value
        } else weatherObs
    }

    // ── Day selector header ──
    val selectedDate = remember(selectedDayIndex, dailyGroups) {
        if (dailyGroups.isNotEmpty() && selectedDayIndex < dailyGroups.size) {
            dailyGroups[selectedDayIndex].key
        } else null
    }
    val formattedDate = remember(selectedDate) {
        selectedDate?.let {
            try {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it) ?: Date()
                )
            } catch (_: Exception) { it }
        } ?: "All time"
    }

    // Overall stats (all days)
    val allStats = remember(weatherObs) {
        val temps = weatherObs.mapNotNull<ObservationEntity, Double> { it.weatherTemperature }
        val humidities = weatherObs.mapNotNull<ObservationEntity, Double> { it.weatherHumidity?.toDouble() }
        mapOf<String, Any?>(
            "avg_temp" to temps.takeIf { it.isNotEmpty() }?.average(),
            "min_temp" to temps.minOrNull(),
            "max_temp" to temps.maxOrNull(),
            "avg_humidity" to humidities.takeIf { it.isNotEmpty() }?.average(),
            "total_records" to weatherObs.size
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldScreenHeader(
                "Weather Database",
                "${weatherObs.size} weather records collected offline",
                icon = FieldMindIcons.Weather,
                actionIcon = FieldMindIcons.Back,
                onAction = onBack
            )
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                Modifier.fillMaxWidth().widthIn(max = 600.dp),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // ── Live Current Weather Card ──
            item {
                LiveCurrentWeatherCard(
                    weather = currentWeather,
                    hasError = weatherError,
                    isRefreshing = isRefreshing,
                    placeName = dashboardPlaceName,
                    showTemp = showTemp,
                    showCondition = showCondition,
                    showHumidity = showHumidity,
                    showWind = showWind,
                    showCloudCover = showCloudPref,
                    showPressure = showPressure,
                    onOpenSettings = onOpenSettings
                )
            }

            // ── Day selector row ──
            if (dailyGroups.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FieldMindIcons.Calendar,
                            null,
                            tint = colors.info,
                            size = 18.dp
                        )
                        Text(
                            formattedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        // Previous / Next day arrows
                        IconButton(
                            onClick = { if (selectedDayIndex < dailyGroups.size - 1) selectedDayIndex++ },
                            enabled = selectedDayIndex < dailyGroups.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                FieldMindIcons.Forward,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp
                            )
                        }
                        IconButton(
                            onClick = { if (selectedDayIndex > 0) selectedDayIndex-- },
                            enabled = selectedDayIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                FieldMindIcons.Back,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp
                            )
                        }
                    }
                }
            }

            // Day stats summary
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        "Avg Temp",
                        "${dayStats.avgTemp?.let { "%.1f°".format(it) } ?: "--"}",
                        colors.info,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Range",
                        "${dayStats.minTemp?.let { "%.0f".format(it) } ?: "--"}° - ${dayStats.maxTemp?.let { "%.0f".format(it) } ?: "--"}°",
                        colors.observation,
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        "Avg Humidity",
                        "${dayStats.avgHumidity?.let { "%.0f".format(it) } ?: "--"}%",
                        colors.data,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Avg Wind",
                        "${dayStats.avgWind?.let { "%.1f km/h".format(it) } ?: "--"}",
                        colors.warning,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Records",
                        "${dayStats.recordCount}",
                        colors.project,
                        Modifier.weight(1f)
                    )
                }
            }

            // Weather records header
            if (weatherObs.isNotEmpty()) {
                item {
                    Text(
                        "Observations with weather data ($formattedDate)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Weather records
            if (weatherObs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(FieldMindIcons.Weather, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 48.dp)
                            Text("No weather data yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Enable GPS and auto-weather in settings to start collecting data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredObs) { weather ->
                    WeatherRecordCard(
                        observation = weather,
                        colors = colors,
                        onOpenDetail = onOpenDetail
                    )
                }
            }
        }
    }
}
}

@Composable
private fun LiveCurrentWeatherCard(
    weather: WeatherSnapshot?,
    hasError: Boolean,
    isRefreshing: Boolean,
    placeName: String? = null,
    showTemp: Boolean = true,
    showCondition: Boolean = true,
    showHumidity: Boolean = true,
    showWind: Boolean = true,
    showCloudCover: Boolean = true,
    showPressure: Boolean = true,
    onOpenSettings: () -> Unit = {}
) {
    val colors = FieldMindTheme.colors

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (weather != null) colors.info.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.info.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Weather, null, tint = colors.info, size = 26.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        placeName ?: "Current conditions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isRefreshing) "Refreshing..."
                        else if (weather != null) "Updated just now"
                        else if (hasError) "Enable location for live weather"
                        else "No weather data available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Settings gear icon — opens Capture defaults where weather dashboard metrics are configured
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        FieldMindIcons.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.info
                    )
                }
            }

            // Weather data row
            if (weather != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Temperature
                    if (showTemp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${weather.temperature?.let { "%.0f°".format(it) } ?: "--"}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.info
                            )
                            Text("Temperature", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Condition
                    if (showCondition && weather.weatherDescription.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                weatherIconForCode(weather.weatherCode),
                                null,
                                tint = colors.observation,
                                size = 32.dp
                            )
                            Text(
                                weather.weatherDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Humidity
                    if (showHumidity) {
                        weather.humidity?.let { hum ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$hum%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.data
                                )
                                Text("Humidity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Extra details row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (showWind) {
                        weather.windSpeed?.let { wind ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%.1f km/h".format(wind),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.warning
                                )
                                Text("Wind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (showCloudCover) {
                        weather.cloudCover?.let { cloud ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$cloud%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.hypothesis
                                )
                                Text("Cloud cover", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (showPressure) {
                        weather.pressure?.let { press ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%.0f hPa".format(press),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.project
                                )
                                Text("Pressure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Auto-refresh indicator
                Text(
                    "Auto-refreshes every 30 minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private data class DayStats(
    val recordCount: Int = 0,
    val avgTemp: Double? = null,
    val minTemp: Double? = null,
    val maxTemp: Double? = null,
    val avgHumidity: Double? = null,
    val avgWind: Double? = null
)

private fun weatherIconForCode(code: Int): fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon {
    return when (code) {
        0, 1 -> FieldMindIcons.Weather         // Clear / mainly clear
        2, 3 -> FieldMindIcons.Cloud            // Partly cloudy / overcast
        45, 48 -> FieldMindIcons.Foggy          // Fog
        51, 53, 55, 56, 57 -> FieldMindIcons.Rainy  // Drizzle
        61, 63, 65, 66, 67 -> FieldMindIcons.Rainy  // Rain
        71, 73, 75, 77 -> FieldMindIcons.Snowy      // Snow
        80, 81, 82 -> FieldMindIcons.Rainy          // Rain showers
        85, 86 -> FieldMindIcons.Snowy              // Snow showers
        95, 96, 99 -> FieldMindIcons.Thunderstorm   // Thunderstorm
        else -> FieldMindIcons.Weather
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeatherRecordCard(
    observation: ObservationEntity,
    colors: fieldmind.research.app.features.field.presentation.theme.FieldMindColors,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail("observation", observation.id) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconForWeatherCondition(observation.weatherCondition),
                    null,
                    tint = colors.info,
                    size = 28.dp
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Observation subject (linked)
                Text(
                    observation.subject.ifBlank { "Observation" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Temperature and condition
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${observation.weatherTemperature?.let { "%.1f°".format(it) } ?: "--"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.info
                    )
                    observation.weatherCondition.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // All weather data in a compact row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    observation.weatherHumidity?.let {
                        Text("Humidity: $it%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    observation.weatherWindSpeed?.let {
                        Text("Wind: %.1f km/h".format(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    observation.weatherCloudCover?.let {
                        Text("Cloud: $it%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Location + time
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (observation.manualLocation.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                FieldMindIcons.Location,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 12.dp
                            )
                            Text(
                                observation.manualLocation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(observation.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Forward arrow indicating tap target
            Icon(
                FieldMindIcons.Forward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                size = 18.dp
            )
        }
    }
}

private fun iconForWeatherCondition(condition: String): fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon {
    val c = condition.lowercase()
    return when {
        c.contains("clear") || c.contains("mainly") || c.isBlank() -> FieldMindIcons.Weather
        c.contains("cloudy") || c.contains("overcast") || c.contains("partly") -> FieldMindIcons.Cloud
        c.contains("fog") || c.contains("rime") -> FieldMindIcons.Foggy
        c.contains("drizzle") || c.contains("rain") || c.contains("shower") -> FieldMindIcons.Rainy
        c.contains("snow") || c.contains("snow grains") -> FieldMindIcons.Snowy
        c.contains("thunder") || c.contains("hail") -> FieldMindIcons.Thunderstorm
        else -> FieldMindIcons.Weather
    }
}
