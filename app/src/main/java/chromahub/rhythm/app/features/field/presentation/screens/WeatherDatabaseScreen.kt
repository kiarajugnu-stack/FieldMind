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
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun WeatherDatabaseScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val observations by viewModel.observations.collectAsState()
    val colors = FieldMindTheme.colors

    // ── Live current weather with auto-refresh ──
    var currentWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherError by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Refresh on screen open and every 30 minutes
    LaunchedEffect(Unit) {
        while (true) {
            isRefreshing = true
            val snapshot = viewModel.refreshWeatherFromLocation()
            currentWeather = snapshot
            weatherError = snapshot == null
            isRefreshing = false
            delay(30 * 60 * 1000L) // 30 minutes
        }
    }

    val weatherObs = remember(observations) {
        observations.filter { it.weatherTemperature != null }
            .sortedByDescending { it.timestamp }
    }

    val stats = remember(weatherObs) {
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Live Current Weather Card ──
            item { LiveCurrentWeatherCard(currentWeather, weatherError, isRefreshing) }

            // Stats summary
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        "Avg Temp",
                        "${stats["avg_temp"]?.let { "%.1f°".format(it) } ?: "--"}",
                        colors.info,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Range",
                        "${stats["min_temp"]?.let { "%.0f".format(it) } ?: "--"}° - ${stats["max_temp"]?.let { "%.0f".format(it) } ?: "--"}°",
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
                        "${stats["avg_humidity"]?.let { "%.0f".format(it) } ?: "--"}%",
                        colors.data,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Total Records",
                        stats["total_records"].toString(),
                        colors.project,
                        Modifier.weight(1f)
                    )
                }
            }

            // Weather records header
            if (weatherObs.isNotEmpty()) {
                item {
                    Text(
                        "Weather history",
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
                items(weatherObs) { weather ->
                    WeatherRecordCard(weather, colors)
                }
            }
        }
    }
}

@Composable
private fun LiveCurrentWeatherCard(
    weather: WeatherSnapshot?,
    hasError: Boolean,
    isRefreshing: Boolean
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
                        "Current conditions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${weather.temperature?.let { "%.0f°".format(it) } ?: "--"}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.info
                        )
                        Text("Temperature", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Condition
                    if (weather.weatherDescription.isNotBlank()) {
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

                // Extra details row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
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

private fun weatherIconForCode(code: Int): fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon {
    return when (code) {
        0, 1 -> FieldMindIcons.Weather // Clear / mainly clear
        2, 3 -> FieldMindIcons.Cloud // Partly cloudy / overcast
        45, 48 -> FieldMindIcons.Weather // Fog
        51, 53, 55, 56, 57 -> FieldMindIcons.Rainy // Drizzle
        61, 63, 65, 66, 67 -> FieldMindIcons.Rainy // Rain
        71, 73, 75, 77 -> FieldMindIcons.Weather // Snow
        80, 81, 82 -> FieldMindIcons.Rainy // Rain showers
        85, 86 -> FieldMindIcons.Weather // Snow showers
        95, 96, 99 -> FieldMindIcons.Alert // Thunderstorm
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
    colors: fieldmind.research.app.features.field.presentation.theme.FieldMindColors
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).fillMaxWidth(0.15f)
                    .let { it.align(Alignment.Top) },
                contentAlignment = Alignment.Center
            ) {
                Icon(FieldMindIcons.Weather, null, tint = colors.info, size = 28.dp)
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (observation.weatherHumidity != null) {
                        Text("${observation.weatherHumidity}% humidity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (observation.weatherWindSpeed != null) {
                        Text("${observation.weatherWindSpeed} m/s wind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(observation.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (observation.manualLocation.isNotBlank()) {
                    Text(
                        observation.manualLocation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
