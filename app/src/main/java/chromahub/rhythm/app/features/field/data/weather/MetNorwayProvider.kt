package fieldmind.research.app.features.field.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** MET Norway Locationforecast provider. Free/no key; requires an identifying User-Agent. */
class MetNorwayProvider : WeatherProvider {
    override val slug = "met-norway"
    override val displayName = "MET Norway"
    override val requiresApiKey = false
    override val apiKeyLabel = "N/A"
    override val apiKeyPlaceholder = "MET Norway is free and requires no API key"

    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val gson = Gson()

    override suspend fun fetchWeather(latitude: Double, longitude: Double, apiKey: String?): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$latitude&lon=$longitude"
            val req = Request.Builder().url(url).header("User-Agent", "FieldMind/1.0 https://github.com/FieldMind").get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val data = gson.fromJson(resp.body?.string() ?: return@withContext null, MetResponse::class.java)
            val instant = data.properties?.timeseries?.firstOrNull()?.data?.instant?.details ?: return@withContext null
            val next = data.properties.timeseries.firstOrNull()?.data?.next1Hours?.summary ?: data.properties.timeseries.firstOrNull()?.data?.next6Hours?.summary
            val code = symbolToWmo(next?.symbolCode)
            WeatherSnapshot(
                temperature = instant.airTemperature,
                weatherCode = code,
                weatherDescription = WeatherSnapshot.descriptionForCode(code),
                humidity = instant.relativeHumidity?.toInt(),
                windSpeed = instant.windSpeed?.let { it * 3.6 },
                windDirection = instant.windFromDirection?.toInt(),
                cloudCover = instant.cloudAreaFraction?.toInt(),
                pressure = instant.airPressureAtSeaLevel
            )
        } catch (_: Exception) { null }
    }

    private fun symbolToWmo(symbol: String?): Int = when {
        symbol == null -> 0
        "thunder" in symbol -> 95
        "snow" in symbol || "sleet" in symbol -> 71
        "rain" in symbol -> 61
        "fog" in symbol -> 45
        "cloudy" in symbol && "partly" !in symbol -> 3
        "partlycloudy" in symbol -> 2
        else -> 0
    }
}

private data class MetResponse(val properties: MetProperties?)
private data class MetProperties(val timeseries: List<MetTimeStep>?)
private data class MetTimeStep(val data: MetData?)
private data class MetData(val instant: MetInstant?, @SerializedName("next_1_hours") val next1Hours: MetNext?, @SerializedName("next_6_hours") val next6Hours: MetNext?)
private data class MetInstant(val details: MetDetails?)
private data class MetNext(val summary: MetSummary?)
private data class MetSummary(@SerializedName("symbol_code") val symbolCode: String?)
private data class MetDetails(
    @SerializedName("air_temperature") val airTemperature: Double?,
    @SerializedName("relative_humidity") val relativeHumidity: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("wind_from_direction") val windFromDirection: Double?,
    @SerializedName("cloud_area_fraction") val cloudAreaFraction: Double?,
    @SerializedName("air_pressure_at_sea_level") val airPressureAtSeaLevel: Double?
)
