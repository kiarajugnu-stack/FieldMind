package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherDatabaseScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val observations by viewModel.observations.collectAsState()
    val colors = FieldMindTheme.colors
    val weatherObs = remember(observations) {
        observations.filter { it.weatherTemperature != null }
            .sortedByDescending { it.timestamp }
    }

    val stats = remember(weatherObs) {
        val temps = weatherObs.mapNotNull { it.weatherTemperature }
        val humidities = weatherObs.mapNotNull { it.weatherHumidity?.toDoubleOrNull() }
        mapOf(
            "avg_temp" to if (temps.isNotEmpty()) temps.average() else null,
            "min_temp" to temps.minOrNull(),
            "max_temp" to temps.maxOrNull(),
            "avg_humidity" to if (humidities.isNotEmpty()) humidities.average() else null,
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
                    if (observation.weatherWind != null) {
                        Text("${observation.weatherWind} wind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
