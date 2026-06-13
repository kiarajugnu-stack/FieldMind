package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Weather data fetched from Open-Meteo API (free, no API key required).
 * Attaches weather snapshots to observations automatically.
 */
data class WeatherSnapshot(
    val temperature: Double? = null,
    val weatherCode: Int = 0,
    val weatherDescription: String = "",
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val windDirection: Int? = null,
    val cloudCover: Int? = null,
    val pressure: Double? = null,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun asDisplayText(): String = buildString {
        temperature?.let { append("%.1f°C".format(it)) }
        if (weatherDescription.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(weatherDescription)
        }
        humidity?.let { append(" • ${it}% humidity") }
        windSpeed?.let { append(" • %.1f km/h wind".format(it)) }
    }

    companion object {
        private val WMO_CODES = mapOf(
            0 to "Clear sky", 1 to "Mainly clear", 2 to "Partly cloudy", 3 to "Overcast",
            45 to "Foggy", 48 to "Rime fog", 51 to "Light drizzle", 53 to "Moderate drizzle",
            55 to "Dense drizzle", 56 to "Freezing drizzle", 57 to "Dense freezing drizzle",
            61 to "Slight rain", 63 to "Moderate rain", 65 to "Heavy rain",
            66 to "Freezing rain", 67 to "Heavy freezing rain",
            71 to "Slight snow", 73 to "Moderate snow", 75 to "Heavy snow",
            77 to "Snow grains", 80 to "Slight rain showers", 81 to "Moderate rain showers",
            82 to "Violent rain showers", 85 to "Slight snow showers", 86 to "Heavy snow showers",
            95 to "Thunderstorm", 96 to "Thunderstorm with hail", 99 to "Heavy thunderstorm with hail"
        )
        fun descriptionForCode(code: Int): String = WMO_CODES[code] ?: "Unknown ($code)"
    }
}

// Open-Meteo API response models
private data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("current") val current: Current?
)

private data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("weathercode") val weatherCode: Int? = null,
    @SerializedName("windspeed") val windSpeed: Double? = null,
    @SerializedName("winddirection") val windDirection: Int? = null
)

private data class Current(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
    @SerializedName("surface_pressure") val pressure: Double? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null
)

/**
 * Fetches weather from Open-Meteo using latitude/longitude.
 * Free, no API key needed. Rate limit: 10,000 requests/day.
 */
class WeatherApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetch current weather for the given coordinates.
     * Returns null if the request fails (offline, error, etc.)
     */
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m" +
                "&timezone=auto"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, OpenMeteoResponse::class.java)

            // Try new API format first, fall back to legacy
            val current = parsed.current
            val legacy = parsed.currentWeather

            val temp = current?.temperature ?: legacy?.temperature
            val code = current?.weatherCode ?: legacy?.weatherCode ?: 0
            val humidity = current?.humidity
            val windSpeed = current?.windSpeed ?: legacy?.windSpeed
            val windDir = current?.windDirection ?: legacy?.windDirection
            val cloudCover = current?.cloudCover
            val pressure = current?.pressure

            WeatherSnapshot(
                temperature = temp,
                weatherCode = code,
                weatherDescription = WeatherSnapshot.descriptionForCode(code),
                humidity = humidity,
                windSpeed = windSpeed,
                windDirection = windDir,
                cloudCover = cloudCover,
                pressure = pressure
            )
        } catch (e: Exception) {
            null // Silent failure — weather is optional
        }
    }
}
