package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** U.S. National Weather Service provider. Free/no key; only returns data for U.S. points. */
class NationalWeatherServiceProvider : WeatherProvider {
    override val slug = "nws"
    override val displayName = "NWS (United States)"
    override val requiresApiKey = false
    override val apiKeyLabel = "N/A"
    override val apiKeyPlaceholder = "Weather.gov is free and requires no API key"

    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val gson = Gson()

    override suspend fun fetchWeather(latitude: Double, longitude: Double, apiKey: String?): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            val headers = { b: Request.Builder -> b.header("User-Agent", "FieldMind/1.0 contact@fieldmind.app").header("Accept", "application/geo+json") }
            val pointReq = headers(Request.Builder().url("https://api.weather.gov/points/$latitude,$longitude")).get().build()
            val pointResp = client.newCall(pointReq).execute()
            if (!pointResp.isSuccessful) return@withContext null
            val point = gson.fromJson(pointResp.body?.string() ?: return@withContext null, NwsPoint::class.java)
            val forecastUrl = point.properties?.forecast ?: return@withContext null
            val forecastResp = client.newCall(headers(Request.Builder().url(forecastUrl)).get().build()).execute()
            if (!forecastResp.isSuccessful) return@withContext null
            val period = gson.fromJson(forecastResp.body?.string() ?: return@withContext null, NwsForecast::class.java).properties?.periods?.firstOrNull() ?: return@withContext null
            val tempC = if (period.temperatureUnit.equals("F", true)) (period.temperature - 32.0) * 5.0 / 9.0 else period.temperature
            val code = textToWmo(period.shortForecast)
            WeatherSnapshot(temperature = tempC, weatherCode = code, weatherDescription = period.shortForecast.ifBlank { WeatherSnapshot.descriptionForCode(code) }, windSpeed = parseWindKph(period.windSpeed), windDirection = directionToDegrees(period.windDirection))
        } catch (_: Exception) { null }
    }

    private fun textToWmo(text: String): Int = when {
        text.contains("Thunder", true) -> 95
        text.contains("Snow", true) -> 71
        text.contains("Rain", true) || text.contains("Showers", true) -> 61
        text.contains("Fog", true) -> 45
        text.contains("Overcast", true) || text.contains("Cloudy", true) -> 3
        text.contains("Partly", true) || text.contains("Mostly Sunny", true) -> 2
        else -> 0
    }
    private fun parseWindKph(wind: String?): Double? = wind?.split(" ")?.firstOrNull()?.toDoubleOrNull()?.let { it * 1.60934 }
    private fun directionToDegrees(dir: String?): Int? = listOf("N","NE","E","SE","S","SW","W","NW").indexOf(dir).takeIf { it >= 0 }?.let { it * 45 }
}
private data class NwsPoint(val properties: NwsPointProps?)
private data class NwsPointProps(val forecast: String?)
private data class NwsForecast(val properties: NwsForecastProps?)
private data class NwsForecastProps(val periods: List<NwsPeriod>?)
private data class NwsPeriod(val temperature: Double, val temperatureUnit: String?, val shortForecast: String, val windSpeed: String?, val windDirection: String?)
