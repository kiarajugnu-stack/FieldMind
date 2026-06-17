package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * OpenWeatherMap provider (free tier: 1,000 calls/day, requires API key).
 * Fetches current weather + 5-day/3-hour forecast.
 *
 * Sign up: https://home.openweathermap.org/users/sign_up
 * API docs: https://openweathermap.org/current
 */
class OpenWeatherMapProvider : WeatherProvider {
    override val slug: String = "openweathermap"
    override val displayName: String = "OpenWeatherMap"
    override val requiresApiKey: Boolean = true
    override val apiKeyLabel: String = "OpenWeatherMap API key"
    override val apiKeyPlaceholder: String = "Get one free at openweathermap.org"

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
            // Current weather
            val currentUrl = "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=$latitude&lon=$longitude&appid=$key&units=metric"

            val currentReq = Request.Builder().url(currentUrl).get().build()
            val currentResp = client.newCall(currentReq).execute()
            if (!currentResp.isSuccessful) return@withContext null
            val currentBody = currentResp.body?.string() ?: return@withContext null
            val current = gson.fromJson(currentBody, OwmCurrentResponse::class.java)

            // 5-day forecast
            val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast" +
                "?lat=$latitude&lon=$longitude&appid=$key&units=metric"

            val forecastReq = Request.Builder().url(forecastUrl).get().build()
            val forecastResp = client.newCall(forecastReq).execute()
            val forecastBody = if (forecastResp.isSuccessful) forecastResp.body?.string() else null
            val forecast = forecastBody?.let { gson.fromJson(it, OwmForecastResponse::class.java) }

            val weatherCode = owmIdToWmo(current.weather?.firstOrNull()?.id ?: 800)
            val description = current.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Clear sky"

            // Build daily forecasts from 3-hourly data
            val dailyForecasts = forecast?.let { buildDailyForecasts(it) } ?: emptyList()

            WeatherSnapshot(
                temperature = current.main?.temp,
                weatherCode = weatherCode,
                weatherDescription = description,
                humidity = current.main?.humidity,
                windSpeed = current.wind?.speed?.let { it * 3.6 }, // m/s → km/h
                windDirection = current.wind?.deg,
                cloudCover = current.clouds?.all,
                pressure = current.main?.pressure?.toDouble(),
                sunrise = current.sys?.sunrise?.let { epochToIso(it) },
                sunset = current.sys?.sunset?.let { epochToIso(it) },
                dailyForecasts = dailyForecasts
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun buildDailyForecasts(forecast: OwmForecastResponse): List<DailyForecast> {
        // Group 3-hour intervals by date, compute daily min/max
        val dayBuckets = forecast.list?.groupBy { item ->
            item.dt_txt?.take(10) ?: ""
        }?.filter { it.key.isNotBlank() }?.entries?.take(7) ?: return emptyList()

        return dayBuckets.mapIndexed { index, (date, items) ->
            val temps = items.mapNotNull { it.main?.temp }
            val weatherItems = items.mapNotNull { it.weather?.firstOrNull() }
            val dominantWeather = weatherItems.groupBy { it.id }.maxByOrNull { it.value.size }?.key ?: 800
            val tMax = temps.maxOrNull() ?: 0.0
            val tMin = temps.minOrNull() ?: 0.0
            val wCode = owmIdToWmo(dominantWeather)
            val desc = weatherItems.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
                    val feelsLikeTemps = items.mapNotNull { it.main?.feelsLike }
                    val apparentAvg = if (feelsLikeTemps.isNotEmpty()) feelsLikeTemps.average() else null
                    DailyForecast(
                        date = date,
                        temperatureMax = tMax,
                        temperatureMin = tMin,
                        weatherCode = wCode,
                        weatherDescription = desc,
                        precipitationSum = items.sumOf { it.rain?.the3H ?: 0.0 }.takeIf { it > 0 },
                        windSpeedMax = items.maxOfOrNull { it.wind?.speed?.let { s -> s * 3.6 } ?: 0.0 },
                        humidityMax = items.maxOfOrNull { it.main?.humidity ?: 0 },
                        apparentTemperature = apparentAvg
                    )
        }
    }

    companion object {
        /** Map OpenWeatherMap condition codes to WMO codes used across FieldMind. */
        private fun owmIdToWmo(owmId: Int): Int = when (owmId) {
            in 200..232 -> 95  // Thunderstorm
            in 300..321 -> 51  // Drizzle
            in 500..531 -> 61  // Rain
            in 600..622 -> 71  // Snow
            in 700..781 -> 45  // Fog/mist/haze
            800 -> 0           // Clear
            801 -> 2           // Few clouds → partly cloudy
            802 -> 2           // Scattered clouds → partly cloudy
            803, 804 -> 3      // Broken/overcast → overcast
            else -> 0
        }

        private fun epochToIso(epoch: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
            return sdf.format(java.util.Date(epoch * 1000))
        }
    }
}

// ── Response models ──

private data class OwmCurrentResponse(
    val main: OwmMain? = null,
    val weather: List<OwmWeather>? = null,
    val wind: OwmWind? = null,
    val clouds: OwmClouds? = null,
    val sys: OwmSys? = null,
    val visibility: Int? = null
)

private data class OwmMain(
    val temp: Double? = null,
    @SerializedName("feels_like") val feelsLike: Double? = null,
    @SerializedName("temp_min") val tempMin: Double? = null,
    @SerializedName("temp_max") val tempMax: Double? = null,
    val pressure: Int? = null,
    val humidity: Int? = null
)

private data class OwmWeather(
    val id: Int = 800,
    val main: String? = null,
    val description: String? = null,
    val icon: String? = null
)

private data class OwmWind(
    val speed: Double? = null,
    val deg: Int? = null,
    val gust: Double? = null
)

private data class OwmClouds(
    val all: Int? = null
)

private data class OwmSys(
    val sunrise: Long? = null,
    val sunset: Long? = null
)

private data class OwmForecastResponse(
    val list: List<OwmForecastItem>? = null
)

private data class OwmForecastItem(
    val dt: Long? = null,
    @SerializedName("dt_txt") val dt_txt: String? = null,
    val main: OwmMain? = null,
    val weather: List<OwmWeather>? = null,
    val wind: OwmWind? = null,
    val clouds: OwmClouds? = null,
    val visibility: Int? = null,
    @SerializedName("pop") val pop: Double? = null,
    val rain: OwmRain? = null
)

private data class OwmRain(
    @SerializedName("3h") val the3H: Double? = null
)
