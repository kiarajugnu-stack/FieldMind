package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.*

/** India Meteorological Department public city forecast provider. Free/no key; best inside India. */
class IndiaMeteorologicalDepartmentProvider : WeatherProvider {
    override val slug = "imd-india"
    override val displayName = "IMD India"
    override val requiresApiKey = false
    override val apiKeyLabel = "N/A"
    override val apiKeyPlaceholder = "IMD public city forecast is free and requires no API key"

    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val gson = Gson()

    override suspend fun fetchWeather(latitude: Double, longitude: Double, apiKey: String?): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            if (latitude !in 6.0..38.8 || longitude !in 68.0..98.5) return@withContext null
            val request = Request.Builder()
                .url("https://api.imd.gov.in/api/v1/cityforecastloc")
                .header("User-Agent", "FieldMind/1.0 field research app")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val root = gson.fromJson(response.body?.string() ?: return@withContext null, JsonElement::class.java)
            val rows = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject.get("data").isJsonArray -> root.asJsonObject.getAsJsonArray("data")
                root.isJsonObject && root.asJsonObject.has("records") && root.asJsonObject.get("records").isJsonArray -> root.asJsonObject.getAsJsonArray("records")
                else -> JsonArray()
            }
            val nearest = rows.mapNotNull { it.asJsonObjectOrNull() }
                .mapNotNull { obj -> obj.toImdForecast(latitude, longitude) }
                .minByOrNull { it.distanceKm } ?: return@withContext null
            WeatherSnapshot(
                temperature = nearest.temperature,
                weatherCode = textToWmo(nearest.condition),
                weatherDescription = nearest.condition.ifBlank { WeatherSnapshot.descriptionForCode(textToWmo(nearest.condition)) },
                humidity = nearest.humidity,
                windSpeed = nearest.windKph,
                dailyForecasts = nearest.dailyForecasts
            )
        } catch (_: Exception) { null }
    }

    private fun JsonObject.toImdForecast(lat: Double, lon: Double): ImdForecast? {
        val stationLat = doubleAny("Lat", "Latitude", "lat", "latitude") ?: return null
        val stationLon = doubleAny("Lon", "Long", "Longitude", "lon", "longitude") ?: return null
        val distance = haversineKm(lat, lon, stationLat, stationLon)
        val max = doubleAny("Today_Max_temp", "Max_temp", "MaxTemp", "Day1_Max_Temp")
        val min = doubleAny("Today_Min_temp", "Min_temp", "MinTemp", "Day1_Min_Temp")
        val temp = listOfNotNull(max, min).takeIf { it.isNotEmpty() }?.average()
        val condition = stringAny("Weather", "Weather1", "Day1_Forecast", "Forecast", "Weather_Description")
        val humidity = intAny("RH", "Humidity", "Relative_Humidity")
        val wind = doubleAny("Wind_Speed", "WindSpeed", "Wind_speed_kmph")
        val date = stringAny("Date", "Issue_Date").ifBlank { "Today" }
        val daily = if (max != null && min != null) listOf(DailyForecast(date, max, min, textToWmo(condition), condition.ifBlank { WeatherSnapshot.descriptionForCode(textToWmo(condition)) })) else emptyList()
        return ImdForecast(distance, temp, condition, humidity, wind, daily)
    }

    private fun JsonObject.stringAny(vararg keys: String): String = keys.firstNotNullOfOrNull { key -> get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() && it != "--" } }.orEmpty()
    private fun JsonObject.doubleAny(vararg keys: String): Double? = stringAny(*keys).replace("°C", "", true).replace("kmph", "", true).trim().toDoubleOrNull()
    private fun JsonObject.intAny(vararg keys: String): Int? = doubleAny(*keys)?.roundToInt()
    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null

    private fun textToWmo(text: String): Int = when {
        text.contains("thunder", true) -> 95
        text.contains("snow", true) || text.contains("sleet", true) -> 71
        text.contains("rain", true) || text.contains("shower", true) -> 61
        text.contains("fog", true) || text.contains("mist", true) || text.contains("haze", true) -> 45
        text.contains("cloud", true) || text.contains("overcast", true) -> 3
        else -> 0
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private data class ImdForecast(val distanceKm: Double, val temperature: Double?, val condition: String, val humidity: Int?, val windKph: Double?, val dailyForecasts: List<DailyForecast>)
}
