package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.data.location.FieldLocationProvider
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.data.weather.WeatherUnitConverter
import fieldmind.research.app.features.field.presentation.components.AnimatedWeatherScene
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val tempUnit by viewModel.fieldSettings.tempUnit.collectAsState()
    val windSpeedUnit by viewModel.fieldSettings.windSpeedUnit.collectAsState()

    // Weather Database always shows all data (display prefs only affect the home widget)

    // ── Live current weather with auto-refresh ──
    var currentWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherError by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var dashboardPlaceName by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    // ── Track last refresh time for "Updated just now" fix ──
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var refreshTimestampText by remember { mutableStateOf("Initializing…") }
    
    // Refresh on screen open (respects cooldown — won't re-fetch if within interval)
    LaunchedEffect(Unit) {
        // Use cached data if available to avoid unnecessary loading states
        val cached = viewModel.lastWeatherSnapshot
        if (cached != null) {
            currentWeather = cached
            weatherError = false
            lastRefreshTime = System.currentTimeMillis()
        }
        
        // This call respects the ViewModel cooldown — returns cached data if within interval
        val snapshot = viewModel.refreshWeatherFromLocation()
        if (snapshot != null) {
            currentWeather = snapshot
            weatherError = false
            lastRefreshTime = System.currentTimeMillis()
        } else if (currentWeather == null) {
            weatherError = true
        }
        
        // Resolve place name for display
        val locProvider = runCatching { FieldLocationProvider(ctx) }.getOrNull()
        if (locProvider != null && locProvider.hasAnyLocationPermission()) {
            locProvider.lastKnownLocation()?.let { loc ->
                locProvider.resolvePlaceName(loc.latitude, loc.longitude) { place ->
                    dashboardPlaceName = place
                }
            }
        }
    }
    
    // Update the "last updated" text every 30 seconds
    LaunchedEffect(lastRefreshTime) {
        while (true) {
            val elapsed = System.currentTimeMillis() - lastRefreshTime
            refreshTimestampText = when {
                elapsed < 60_000 -> "Updated just now"
                elapsed < 120_000 -> "Updated 1 min ago"
                elapsed < 3600_000 -> "Updated ${elapsed / 60_000} min ago"
                else -> "Updated ${elapsed / 3600_000}h ago"
            }
            delay(30_000L) // Update every 30 seconds
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
                    refreshTimestampText = refreshTimestampText,
                    onOpenSettings = onOpenSettings,
                    tempUnit = tempUnit,
                    windSpeedUnit = windSpeedUnit
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
                        // ← Previous (older) — dailyGroups is sorted newest-first, so index++ goes to older
                        IconButton(
                            onClick = { if (selectedDayIndex < dailyGroups.size - 1) selectedDayIndex++ },
                            enabled = selectedDayIndex < dailyGroups.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                FieldMindIcons.Back,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp
                            )
                        }
                        // → Next (newer) — index-- goes to newer (index 0 = newest)
                        IconButton(
                            onClick = { if (selectedDayIndex > 0) selectedDayIndex-- },
                            enabled = selectedDayIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                FieldMindIcons.Forward,
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
                        dayStats.avgTemp?.let { WeatherUnitConverter.formatTemp(it, tempUnit) } ?: "--",
                        colors.info,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "Range",
                        "${dayStats.minTemp?.let { WeatherUnitConverter.formatTemp(it, tempUnit) } ?: "--"} - ${dayStats.maxTemp?.let { WeatherUnitConverter.formatTemp(it, tempUnit) } ?: "--"}",
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
                        dayStats.avgWind?.let { WeatherUnitConverter.formatWind(it, windSpeedUnit) } ?: "--",
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
    onOpenSettings: () -> Unit = {},
    refreshTimestampText: String = "Updated just now",
    tempUnit: String = "Celsius",
    windSpeedUnit: String = "km/h"
) {
    val colors = FieldMindTheme.colors
    val isDarkTheme = colors.isDark
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
    // Text color that adapts to the animated weather scene background
    // White works on dark/night scenes; dark text is needed on light/day pastel scenes
    val textOnScene = when {
        isDarkTheme -> Color.White               // Dark mode: always dark scene bg
        isNight -> Color.White                    // Night scene even in light mode: dark bg
        else -> Color(0xFF1A1A3E)                 // Light mode + day scene: pastel bg, dark text
    }
    
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
                            color = if (weather != null) textOnScene else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            timeGreeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (weather != null) textOnScene.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Settings
                        Surface(
                            onClick = onOpenSettings,
                            shape = CircleShape,
                            color = if (weather != null) textOnScene.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    FieldMindIcons.Settings,
                                    null,
                                    tint = if (weather != null) textOnScene else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 22.dp
                                )
                            }
                        }
                        // Refresh spinner
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (weather != null) textOnScene else colors.info
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
                        color = textOnScene.copy(alpha = 0.9f)
                    )
                    }

                    // Glass-morphism detailed metrics card
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme || isNight) Color.White.copy(alpha = 0.12f) else Color(0xFF1A1A3E).copy(alpha = 0.06f)),
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
                                color = textOnScene
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
                            ExpandInfoChip(FieldMindIcons.Sunrise, "Sunrise ${formatTimeFromIso(it)}", Modifier.weight(1f), textOnScene)
                        }
                        w.sunset?.let {
                            ExpandInfoChip(FieldMindIcons.Sunset, "Sunset ${formatTimeFromIso(it)}", Modifier.weight(1f), textOnScene)
                        }
                    }

                    // ── 7-Day Forecast ──
                    if (w.dailyForecasts.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        ForecastDashboard(
                            forecasts = w.dailyForecasts.take(7),
                            textOnScene = textOnScene,
                            colors = colors
                        )
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
                            color = if (weather != null) textOnScene.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Auto-refreshes every 30 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (weather != null) textOnScene.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val fontSize = remember(value) {
        when {
            value.length > 8 -> 14.sp
            value.length > 5 -> 18.sp
            else -> 24.sp
        }
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpandMetric(value: String, label: String, icon: MaterialSymbolIcon, color: Color, textColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = color, size = 24.dp)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
    }
}

@Composable
private fun ExpandInfoChip(icon: MaterialSymbolIcon, text: String, modifier: Modifier = Modifier, textColor: Color = Color.White) {
    val chipBg = textColor.copy(alpha = 0.12f)
    Surface(shape = RoundedCornerShape(14.dp), color = chipBg, modifier = modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = textColor.copy(alpha = 0.8f), size = 16.dp)
            Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ForecastDashboard(
    forecasts: List<fieldmind.research.app.features.field.data.weather.DailyForecast>,
    textOnScene: Color,
    colors: fieldmind.research.app.features.field.presentation.theme.FieldMindColors
) {
    var expandedIdx by remember { mutableIntStateOf(-1) }
    val scrollState = rememberLazyListState()
    val allTemps = forecasts.map { it.temperatureMax }
    val globalMax = allTemps.maxOrNull() ?: 30.0
    val globalMin = allTemps.minOrNull() ?: 0.0
    val tempRange = (globalMax - globalMin).coerceAtLeast(10.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textOnScene
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(textOnScene.copy(alpha = 0.3f)))
                Text(
                    "Swipe for more days →",
                    style = MaterialTheme.typography.labelSmall,
                    color = textOnScene.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(forecasts.take(7), key = { it.date }) { day ->
                val isExpanded = expandedIdx == forecasts.indexOf(day)
                val dayTemp = day.temperatureMax
                val normalizedPos = ((dayTemp - globalMin) / tempRange).coerceIn(0.0, 1.0)

                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { expandedIdx = if (isExpanded) -1 else forecasts.indexOf(day) }
                        .background(textOnScene.copy(alpha = 0.08f))
                        .padding(10.dp)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Day name
                    val dayName = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val date = sdf.parse(day.date) ?: java.util.Date()
                        java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date)
                    } catch (_: Exception) { day.date.take(3) }
                    Text(dayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textOnScene.copy(alpha = 0.8f))

                    // Weather icon
                    val icon = when {
                        day.weatherCode <= 1 -> FieldMindIcons.Weather
                        day.weatherCode in 2..3 -> FieldMindIcons.Cloud
                        day.weatherCode in 45..48 -> FieldMindIcons.Foggy
                        day.weatherCode in 51..67 || day.weatherCode in 80..82 -> FieldMindIcons.Rainy
                        day.weatherCode in 71..77 || day.weatherCode in 85..86 -> FieldMindIcons.Snowy
                        day.weatherCode >= 95 -> FieldMindIcons.Thunderstorm
                        else -> FieldMindIcons.Weather
                    }
                    Icon(icon, null, tint = textOnScene.copy(alpha = 0.8f), size = 20.dp)

                    // Temperature bar
                    Box(
                        Modifier.width(28.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.info.copy(alpha = 0.4f),
                                        colors.warning.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )

                    // High temp
                    Text(
                        "${day.temperatureMax.roundToInt()}°",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnScene
                    )
                    // Low temp
                    Text(
                        "${day.temperatureMin.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = textOnScene.copy(alpha = 0.6f)
                    )

                    // Expanded details
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            Modifier.width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HorizontalDivider(color = textOnScene.copy(alpha = 0.15f))
                            Spacer(Modifier.height(2.dp))

                            day.precipitationSum?.let { precip ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(FieldMindIcons.Water, null, tint = colors.data, size = 14.dp)
                                    Text("$precip%", style = MaterialTheme.typography.labelSmall, color = colors.data, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            day.windSpeedMax?.let { ws ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(FieldMindIcons.windIconForSpeed(ws), null, tint = colors.warning, size = 14.dp)
                                    Text("${ws.roundToInt()} km/h", style = MaterialTheme.typography.labelSmall, color = colors.warning, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            day.humidityMax?.let { hum ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(FieldMindIcons.Water, null, tint = colors.data, size = 14.dp)
                                    Text("$hum%", style = MaterialTheme.typography.labelSmall, color = colors.data, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            day.apparentTemperature?.let { feels ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(FieldMindIcons.Info, null, tint = textOnScene.copy(alpha = 0.6f), size = 14.dp)
                                    Text("Feels ${feels.roundToInt()}°", style = MaterialTheme.typography.labelSmall, color = textOnScene.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
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
                        WeatherUnitConverter.formatTemp(observation.weatherTemperature, "Celsius"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.info
                    )
                    observation.weatherCondition.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // All weather data in a compact row — explicitly left-aligned
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    observation.weatherHumidity?.let {
                        Text("Humidity: $it% ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                    }
                    observation.weatherWindSpeed?.let { ws ->
                        Text("Wind: ${WeatherUnitConverter.formatWind(ws, "km/h")} ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                    }
                    observation.weatherCloudCover?.let {
                        Text("Cloud: $it%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Location + time — always left-aligned, location takes precedence
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (observation.manualLocation.isNotBlank()) {
                        Icon(
                            FieldMindIcons.Location,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 12.dp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            observation.manualLocation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(observation.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f, fill = false)
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
        c.contains("snow") || c.contains("snow grains") || c.contains("sleet") || c.contains("ice pellets") -> FieldMindIcons.Snowy
        c.contains("drizzle") || c.contains("rain") || c.contains("shower") -> FieldMindIcons.Rainy
        c.contains("thunder") || c.contains("hail") -> FieldMindIcons.Thunderstorm
        else -> FieldMindIcons.Weather
    }
}
