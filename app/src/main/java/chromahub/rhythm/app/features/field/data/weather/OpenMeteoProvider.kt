package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo provider (free, no API key required, 10,000 requests/day).
 * Uses the Open-Meteo API to fetch current weather and daily forecasts.
 *
 * This is the default provider for FieldMind.
 */
class OpenMeteoProvider : WeatherProvider {
    override val slug: String = "open-meteo"
    override val displayName: String = "Open-Meteo"
    override val requiresApiKey: Boolean = false
    override val apiKeyLabel: String = "N/A"
    override val apiKeyPlaceholder: String = "Open-Meteo is free and requires no API key"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String?
    ): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m" +
                "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum,wind_speed_10m_max,relative_humidity_2m_max,time" +
                "&timezone=auto"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, OpenMeteoResponse::class.java)
            val parsedFull = try { gson.fromJson(body, OpenMeteoResponseFull::class.java) } catch (_: Exception) { null }

            // Try new API format first, fall back to legacy
            val current = parsed.current ?: parsedFull?.current
            val legacy = parsed.currentWeather
            val daily = parsedFull?.daily

            val temp = current?.temperature ?: legacy?.temperature
            val code = current?.weatherCode ?: legacy?.weatherCode ?: 0
            val humidity = current?.humidity
            val windSpeed = current?.windSpeed ?: legacy?.windSpeed
            val windDir = current?.windDirection ?: legacy?.windDirection
            val cloudCover = current?.cloudCover
            val pressure = current?.pressure
            val sunrise = daily?.sunrise?.firstOrNull { it.isNotBlank() }
            val sunset = daily?.sunset?.firstOrNull { it.isNotBlank() }

            // Parse daily forecasts
            val forecasts = if (daily?.time != null && daily.tempMax != null && daily.tempMin != null) {
                daily.time.zip(daily.tempMax.zip(daily.tempMin)).mapIndexed { index, (date, temps) ->
                    val (tMax, tMin) = temps
                    val wCode = daily.weatherCode?.getOrNull(index) ?: 0
                    val precip = daily.precipitationSum?.getOrNull(index)
                    val windMax = daily.windSpeedMax?.getOrNull(index)
                    val humMax = daily.humidityMax?.getOrNull(index)
                    DailyForecast(
                        date = date,
                        temperatureMax = tMax,
                        temperatureMin = tMin,
                        weatherCode = wCode,
                        weatherDescription = WeatherSnapshot.descriptionForCode(wCode),
                        precipitationSum = precip,
                        windSpeedMax = windMax,
                        humidityMax = humMax
                    )
                }
            } else emptyList()

            WeatherSnapshot(
                temperature = temp,
                weatherCode = code,
                weatherDescription = WeatherSnapshot.descriptionForCode(code),
                humidity = humidity,
                windSpeed = windSpeed,
                windDirection = windDir,
                cloudCover = cloudCover,
                pressure = pressure,
                sunrise = sunrise,
                sunset = sunset,
                dailyForecasts = forecasts
            )
        } catch (e: Exception) {
            null // Silent failure — weather is optional
        }
    }
}

// Open-Meteo API response models
internal data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("current") val current: Current?
)

internal data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("weathercode") val weatherCode: Int? = null,
    @SerializedName("windspeed") val windSpeed: Double? = null,
    @SerializedName("winddirection") val windDirection: Int? = null
)

internal data class Current(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
    @SerializedName("surface_pressure") val pressure: Double? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null
)

internal data class DailyData(
    @SerializedName("sunrise") val sunrise: List<String>? = null,
    @SerializedName("sunset") val sunset: List<String>? = null,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>? = null,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int>? = null,
    @SerializedName("precipitation_sum") val precipitationSum: List<Double>? = null,
    @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double>? = null,
    @SerializedName("relative_humidity_2m_max") val humidityMax: List<Int>? = null,
    @SerializedName("time") val time: List<String>? = null
)

internal data class OpenMeteoResponseFull(
    @SerializedName("current") val current: Current? = null,
    @SerializedName("daily") val daily: DailyData? = null
)
