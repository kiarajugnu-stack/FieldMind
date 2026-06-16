#!/usr/bin/env python3
import os

os.chdir('/workspaces/FieldMind')

with open('app/src/main/java/chromahub/rhythm/app/features/field/presentation/screens/WeatherDatabaseScreen.kt', 'r') as f:
    content = f.read()

# Replace the LiveCurrentWeatherCard function with the beautiful expanded overlay design
old_fn_start = "@Composable\nprivate fun LiveCurrentWeatherCard("
new_fn_end_marker = "@Composable\nprivate fun StatCard("

idx_start = content.find(old_fn_start)
idx_end = content.find(new_fn_end_marker, idx_start)

if idx_start >= 0 and idx_end >= 0:
    print(f"Found LiveCurrentWeatherCard at {idx_start}, StatCard at {idx_end}")
    
    new_card = """@Composable
private fun LiveCurrentWeatherCard(
    weather: WeatherSnapshot?,
    hasError: Boolean,
    isRefreshing: Boolean,
    placeName: String? = null,
    onOpenSettings: () -> Unit = {},
    refreshTimestampText: String = "Updated just now",
    tempUnit: String = "Celsius",
    windSpeedUnit: String = "km/h"
) {
    val colors = FieldMindTheme.colors
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val timeOfDay = when (currentHour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }
    val timeGreeting = when (timeOfDay) {
        "morning" -> "Good morning"
        "afternoon" -> "Good afternoon"
        "evening" -> "Good evening"
        else -> "Good night"
    }
    val isNight = timeOfDay == "night"
    
    // Temperature gradient
    val tempDisplay = weather?.temperature ?: 20.0
    val displayColors = when {
        tempDisplay < 0 -> listOf(Color(0xFF1A237E), Color(0xFF42A5F5))
        tempDisplay < 10 -> listOf(Color(0xFF1565C0), Color(0xFF64B5F6))
        tempDisplay < 20 -> listOf(Color(0xFF0D47A1), Color(0xFF66BB6A))
        tempDisplay < 30 -> listOf(Color(0xFFE65100), Color(0xFFFFB74D))
        else -> listOf(Color(0xFFBF360C), Color(0xFFE57373))
    }
    val weatherGradient = Brush.horizontalGradient(displayColors)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Animated weather scene background
            if (weather != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    AnimatedWeatherScene(
                        weatherCode = weather.weatherCode,
                        temperature = weather.temperature,
                        sunrise = weather.sunrise,
                        sunset = weather.sunset,
                        compact = false
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top row: greeting + settings + refresh
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            placeName ?: "Weather Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (weather != null) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            timeGreeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (weather != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Settings
                        Surface(
                            onClick = onOpenSettings,
                            shape = CircleShape,
                            color = if (weather != null) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    FieldMindIcons.Settings,
                                    null,
                                    tint = if (weather != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 22.dp
                                )
                            }
                        }
                        // Refresh spinner
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (weather != null) Color.White else colors.info
                            )
                        }
                    }
                }

                if (weather != null) {
                    val w = weather

                    // Large gradient temperature
                    Text(
                        WeatherUnitConverter.formatTemp(w.temperature, tempUnit),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            brush = weatherGradient
                        )
                    )

                    // Condition row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherConditionImage(
                            code = w.weatherCode,
                            isNight = isNight,
                            size = 56.dp
                        )
                        Text(
                            w.weatherDescription,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Glass-morphism detailed metrics card
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Detailed metrics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                w.humidity?.let {
                                    ExpandMetric("$it%", "Humidity", FieldMindIcons.Water, colors.data)
                                }
                                w.windSpeed?.let { wind ->
                                    ExpandMetric(
                                        WeatherUnitConverter.formatWind(wind, windSpeedUnit),
                                        "Wind",
                                        FieldMindIcons.windIconForSpeed(wind),
                                        colors.warning
                                    )
                                }
                                w.cloudCover?.let {
                                    ExpandMetric("$it%", "Clouds", FieldMindIcons.Cloud, colors.hypothesis)
                                }
                                w.pressure?.let {
                                    ExpandMetric("%.0f".format(it), "hPa", FieldMindIcons.Compress, colors.project)
                                }
                            }
                        }
                    }

                    // Sunrise / Sunset
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        w.sunrise?.let {
                            ExpandInfoChip(FieldMindIcons.Sunrise, "Sunrise ${formatTimeFromIso(it)}", Modifier.weight(1f))
                        }
                        w.sunset?.let {
                            ExpandInfoChip(FieldMindIcons.Sunset, "Sunset ${formatTimeFromIso(it)}", Modifier.weight(1f))
                        }
                    }

                    // Update timestamp
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            refreshTimestampText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (weather != null) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Auto-refreshes every 30 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (weather != null) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else if (hasError) {
                    // Empty state
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Weather,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            size = 48.dp
                        )
                        Text(
                            "Enable GPS for live weather",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            refreshTimestampText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Loading
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = colors.info
                        )
                    }
                }
            }
        }
    }
}"""
    
    content = content[:idx_start] + new_card + content[idx_end:]
    
    with open('app/src/main/java/chromahub/rhythm/app/features/field/presentation/screens/WeatherDatabaseScreen.kt', 'w') as f:
        f.write(content)
    print("Successfully replaced LiveCurrentWeatherCard with the beautiful expanded overlay design")
else:
    print(f"Could not find boundaries. idx_start={idx_start}, idx_end={idx_end}")
