package fieldmind.research.app.features.field.data.weather

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
        OpenWeatherMapProvider(),
        WeatherApiDotComProvider()
    )

    private val bySlug: Map<String, WeatherProvider> = providers.associateBy { it.slug }

    fun fromSlug(slug: String): WeatherProvider = bySlug[slug] ?: OpenMeteoProvider()

    /** Returns the provider that best matches the given slug, defaulting to Open-Meteo. */
    fun getProvider(slug: String?): WeatherProvider = slug?.let { fromSlug(it) } ?: OpenMeteoProvider()
}
