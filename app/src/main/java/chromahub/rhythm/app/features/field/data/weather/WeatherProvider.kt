package fieldmind.research.app.features.field.data.weather

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Abstract interface for weather data providers.
 * Each provider fetches current weather + daily forecasts for a lat/lon.
 */
interface WeatherProvider {
    /** Unique slug used for settings storage (e.g. "open-meteo", "openweathermap"). */
    val slug: String

    /** Human-readable name shown in settings UI (e.g. "OpenWeatherMap"). */
    val displayName: String

    /** Whether this provider requires an API key to function. */
    val requiresApiKey: Boolean

    /** Label for the API key text field in settings (e.g. "OpenWeatherMap API key"). */
    val apiKeyLabel: String

    /** Placeholder/supporting text for the API key field. */
    val apiKeyPlaceholder: String

    /** Fetch current weather + daily forecasts for the given coordinates. */
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String? = null
    ): WeatherSnapshot?
}

/**
 * Registry of available weather providers. The user selects one in settings.
 * Open-Meteo is the default (free, no API key needed).
 */
object WeatherProviders {
    val providers: List<WeatherProvider> = listOf(
        OpenMeteoProvider(),
        IndiaMeteorologicalDepartmentProvider(),
        MetNorwayProvider(),
        NationalWeatherServiceProvider(),
        OpenWeatherMapProvider(),
        WeatherApiDotComProvider()
    )

    private val bySlug: Map<String, WeatherProvider> = providers.associateBy { it.slug }

    fun fromSlug(slug: String): WeatherProvider = bySlug[slug] ?: OpenMeteoProvider()

    /** Returns the provider that best matches the given slug, defaulting to Open-Meteo. */
    fun getProvider(slug: String?): WeatherProvider = slug?.let { fromSlug(it) } ?: OpenMeteoProvider()

    fun selectedProviders(slugsCsv: String?): List<WeatherProvider> {
        val slugs = slugsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        return slugs.map { fromSlug(it) }.distinctBy { it.slug }.ifEmpty { listOf(OpenMeteoProvider()) }
    }

    suspend fun fetchMergedWeather(
        slugsCsv: String?,
        latitude: Double,
        longitude: Double,
        apiKey: String? = null
    ): WeatherSnapshot? = coroutineScope {
        val snapshots = selectedProviders(slugsCsv)
            .filter { !it.requiresApiKey || !apiKey.isNullOrBlank() }
            .map { provider -> async { provider.fetchWeather(latitude, longitude, apiKey) } }
            .mapNotNull { it.await() }
        mergeSnapshots(snapshots)
    }

    private fun mergeSnapshots(snapshots: List<WeatherSnapshot>): WeatherSnapshot? {
        if (snapshots.isEmpty()) return null
        fun avg(values: List<Double?>): Double? = values.filterNotNull().takeIf { it.isNotEmpty() }?.average()
        fun avgInt(values: List<Int?>): Int? = values.filterNotNull().takeIf { it.isNotEmpty() }?.average()?.toInt()
        val code = snapshots.map { it.weatherCode }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0
        val richestForecast = snapshots.maxByOrNull { it.dailyForecasts.size }?.dailyForecasts.orEmpty()
        return WeatherSnapshot(
            temperature = avg(snapshots.map { it.temperature }),
            weatherCode = code,
            weatherDescription = WeatherSnapshot.descriptionForCode(code),
            humidity = avgInt(snapshots.map { it.humidity }),
            windSpeed = avg(snapshots.map { it.windSpeed }),
            windDirection = avgInt(snapshots.map { it.windDirection }),
            cloudCover = avgInt(snapshots.map { it.cloudCover }),
            pressure = avg(snapshots.map { it.pressure }),
            sunrise = snapshots.firstNotNullOfOrNull { it.sunrise },
            sunset = snapshots.firstNotNullOfOrNull { it.sunset },
            dailyForecasts = richestForecast,
            fetchedAt = System.currentTimeMillis()
        )
    }
}
