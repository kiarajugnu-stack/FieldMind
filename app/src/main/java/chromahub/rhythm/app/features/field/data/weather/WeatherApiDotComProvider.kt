package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * WeatherAPI.com provider (free tier: 1M calls/month, requires API key).
 * Fetches current weather + 3-day daily forecast.
 *
 * Sign up: https://www.weatherapi.com/signup.aspx
 * API docs: https://www.weatherapi.com/docs/
 */
class WeatherApiDotComProvider : WeatherProvider {
    override val slug: String = "weatherapi"
    override val displayName: String = "WeatherAPI.com"
    override val requiresApiKey: Boolean = true
    override val apiKeyLabel: String = "WeatherAPI.com API key"
    override val apiKeyPlaceholder: String = "Get one free at weatherapi.com"

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
        val key = apiKey ?: return@withContext null
        try {
            val url = "https://api.weatherapi.com/v1/forecast.json" +
                "?key=$key" +
                "&q=$latitude,$longitude" +
                "&days=4" +
                "&aqi=no&alerts=no"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val data = gson.fromJson(body, WapiResponse::class.java)

            val current = data.current ?: return@withContext null

            val weatherCode = wapiCodeToWmo(current.condition?.code ?: 1000)
            val description = current.condition?.text ?: "Clear"

            val forecasts = data.forecast?.forecastday
                ?.filter { it.date.isNotBlank() }
                ?.map { day ->
                    DailyForecast(
                        date = day.date,
                        temperatureMax = day.day?.maxtemp_c ?: 0.0,
                        temperatureMin = day.day?.mintemp_c ?: 0.0,
                        weatherCode = wapiCodeToWmo(day.day?.condition?.code ?: 1000),
                        weatherDescription = day.day?.condition?.text ?: "",
                        precipitationSum = day.day?.totalprecip_mm,
                        windSpeedMax = day.day?.maxwind_kph,
                        humidityMax = day.day?.avghumidity
                    )
                } ?: emptyList()

            WeatherSnapshot(
                temperature = current.temp_c,
                weatherCode = weatherCode,
                weatherDescription = description,
                humidity = current.humidity,
                windSpeed = current.wind_kph,
                windDirection = current.wind_degree,
                cloudCover = current.cloud,
                pressure = current.pressure_mb?.toDouble(),
                sunrise = data.forecast?.forecastday?.firstOrNull()?.astro?.sunrise?.let { normalizeTime12to24(it) },
                sunset = data.forecast?.forecastday?.firstOrNull()?.astro?.sunset?.let { normalizeTime12to24(it) },
                dailyForecasts = forecasts
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Convert WeatherAPI 12-hour time (e.g. "06:30 AM") to 24-hour format ("06:30"). */
        private fun normalizeTime12to24(time12: String): String {
            val trimmed = time12.trim()
            try {
                val parts = trimmed.split(" ")
                if (parts.size != 2) return trimmed.take(5)
                val timeParts = parts[0].split(":")
                if (timeParts.size != 2) return trimmed.take(5)
                var hour = timeParts[0].toInt()
                val minute = timeParts[1]
                val isPM = parts[1].equals("PM", ignoreCase = true)
                val isAM = parts[1].equals("AM", ignoreCase = true)
                if (isPM && hour != 12) hour += 12
                if (isAM && hour == 12) hour = 0
                return "%02d:%s".format(hour, minute)
            } catch (_: Exception) {
                return trimmed.take(5)
            }
        }

        /** Map WeatherAPI.com condition codes to WMO codes. */
        private fun wapiCodeToWmo(code: Int): Int = when (code) {
            1000 -> 0           // Clear/Sunny
            1003 -> 2           // Partly cloudy
            1006, 1009 -> 3     // Cloudy/Overcast
            1030, 1135, 1147 -> 45 // Mist/Fog
            1063, 1150, 1153, 1168, 1171, 1180, 1183, 1186, 1189,
            1192, 1195, 1198, 1201, 1240, 1243, 1246 -> 61 // Rain
            1066, 1114, 1117, 1204, 1207, 1210, 1213, 1216,
            1219, 1222, 1225, 1255, 1258 -> 71 // Snow
            1069, 1072, 1147, 1204, 1207, 1249, 1252, 1261, 1264 -> 51 // Sleet/drizzle
            1087, 1273, 1276, 1279, 1282 -> 95 // Thunderstorm
            else -> 0
        }
    }
}

// ── Response models ──

private data class WapiResponse(
    val location: WapiLocation? = null,
    val current: WapiCurrent? = null,
    val forecast: WapiForecast? = null
)

private data class WapiLocation(
    val name: String? = null,
    val region: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerializedName("localtime") val localtime: String? = null
)

private data class WapiCurrent(
    @SerializedName("temp_c") val temp_c: Double? = null,
    val condition: WapiCondition? = null,
    @SerializedName("wind_kph") val wind_kph: Double? = null,
    @SerializedName("wind_degree") val wind_degree: Int? = null,
    @SerializedName("wind_dir") val wind_dir: String? = null,
    @SerializedName("pressure_mb") val pressure_mb: Double? = null,
    @SerializedName("humidity") val humidity: Int? = null,
    @SerializedName("cloud") val cloud: Int? = null,
    @SerializedName("feelslike_c") val feelslike_c: Double? = null,
    @SerializedName("vis_km") val vis_km: Double? = null,
    @SerializedName("uv") val uv: Double? = null
)

private data class WapiCondition(
    val text: String? = null,
    val icon: String? = null,
    val code: Int? = null
)

private data class WapiForecast(
    @SerializedName("forecastday") val forecastday: List<WapiForecastDay>? = null
)

private data class WapiForecastDay(
    val date: String = "",
    val day: WapiDay? = null,
    val astro: WapiAstro? = null
)

private data class WapiDay(
    @SerializedName("maxtemp_c") val maxtemp_c: Double? = null,
    @SerializedName("mintemp_c") val mintemp_c: Double? = null,
    @SerializedName("avgtemp_c") val avgtemp_c: Double? = null,
    @SerializedName("maxwind_kph") val maxwind_kph: Double? = null,
    @SerializedName("totalprecip_mm") val totalprecip_mm: Double? = null,
    @SerializedName("avghumidity") val avghumidity: Int? = null,
    val condition: WapiCondition? = null,
    val uv: Double? = null
)

private data class WapiAstro(
    val sunrise: String? = null,
    val sunset: String? = null,
    val moonrise: String? = null,
    val moonset: String? = null,
    @SerializedName("moon_phase") val moon_phase: String? = null
)
