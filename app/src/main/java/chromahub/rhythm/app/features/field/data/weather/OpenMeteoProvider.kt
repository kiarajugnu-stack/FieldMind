package fieldmind.research.app.features.field.data.weather

import android.util.Log
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
 * Uses a single robust response model.
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
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m" +
                "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum,wind_speed_10m_max,apparent_temperature_max,apparent_temperature_min,time" +
                "&timezone=auto"

            val request = Request.Builder()
                .url(url)
                // Open-Meteo accepts the user-agent but doesn't require one
                .header("User-Agent", "FieldMind/1.0 (field-research-app; open-meteo-provider)")
                .get()
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w("OpenMeteo", "HTTP ${response.code} for $latitude,$longitude — body: ${response.body?.string()?.take(200)}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, OpenMeteoFullResponse::class.java)

            val curr = parsed.current
            val daily = parsed.daily

            if (curr == null) {
                Log.w("OpenMeteo", "No 'current' object in API response for $latitude,$longitude")
                // Try legacy format
                val legacy = gson.fromJson(body, OpenMeteoLegacyResponse::class.java)
                val lw = legacy.currentWeather ?: return@withContext null
                return@withContext WeatherSnapshot(
                    temperature = lw.temperature,
                    weatherCode = lw.weatherCode ?: 0,
                    weatherDescription = WeatherSnapshot.descriptionForCode(lw.weatherCode ?: 0),
                    windSpeed = lw.windSpeed,
                    windDirection = lw.windDirection
                )
            }

            val temp = curr.temperature
            val code = curr.weatherCode ?: 0
            val humidity = curr.humidity
            val windSpeed = curr.windSpeed
            val windDir = curr.windDirection
            val cloudCover = curr.cloudCover
            val pressure = curr.pressure
            val sunrise = daily?.sunrise?.firstOrNull { it.isNotBlank() }
            val sunset = daily?.sunset?.firstOrNull { it.isNotBlank() }

            // Parse daily forecasts
            val forecasts = if (daily?.time != null && daily.tempMax != null && daily.tempMin != null) {
                daily.time.zip(daily.tempMax.zip(daily.tempMin)).mapIndexed { index, (date, temps) ->
                    val (tMax, tMin) = temps
                    val wCode = daily.weatherCode?.getOrNull(index) ?: 0
                    val precip = daily.precipitationSum?.getOrNull(index)
                    val windMax = daily.windSpeedMax?.getOrNull(index)
                    val apparentMax = daily.apparentTempMax?.getOrNull(index)
                    val apparentMin = daily.apparentTempMin?.getOrNull(index)
                    val apparentAvg = if (apparentMax != null && apparentMin != null) (apparentMax + apparentMin) / 2.0 else null
                    DailyForecast(
                        date = date,
                        temperatureMax = tMax,
                        temperatureMin = tMin,
                        weatherCode = wCode,
                        weatherDescription = WeatherSnapshot.descriptionForCode(wCode),
                        precipitationSum = precip,
                        windSpeedMax = windMax,
                        humidityMax = null,
                        apparentTemperature = apparentAvg
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
            Log.e("OpenMeteo", "fetchWeather failed for $latitude,$longitude", e)
            null
        }
    }
}

// ── Response models ──

/** New API format: current + daily at top level. */
internal data class OpenMeteoFullResponse(
    @SerializedName("current") val current: OpenMeteoCurrent? = null,
    @SerializedName("daily") val daily: OpenMeteoDaily? = null
)

/** Legacy format: current_weather flat object. */
internal data class OpenMeteoLegacyResponse(
    @SerializedName("current_weather") val currentWeather: OpenMeteoLegacyCurrent? = null
)

internal data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
    @SerializedName("surface_pressure") val pressure: Double? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null
)

internal data class OpenMeteoLegacyCurrent(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("weathercode") val weatherCode: Int? = null,
    @SerializedName("windspeed") val windSpeed: Double? = null,
    @SerializedName("winddirection") val windDirection: Int? = null
)

internal data class OpenMeteoDaily(
    @SerializedName("time") val time: List<String>? = null,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>? = null,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int>? = null,
    @SerializedName("precipitation_sum") val precipitationSum: List<Double>? = null,
    @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double>? = null,
    @SerializedName("apparent_temperature_max") val apparentTempMax: List<Double>? = null,
    @SerializedName("apparent_temperature_min") val apparentTempMin: List<Double>? = null,
    @SerializedName("sunrise") val sunrise: List<String>? = null,
    @SerializedName("sunset") val sunset: List<String>? = null
)
