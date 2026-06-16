package fieldmind.research.app.features.field.data.weather

/**
 * Snapshot of current weather conditions + daily forecasts for a location.
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
    val sunrise: String? = null,
    val sunset: String? = null,
    val dailyForecasts: List<DailyForecast> = emptyList(),
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

/**
 * Daily forecast data for a single day.
 * @param apparentTemperature Average feels-like temperature for the day (null if unavailable).
 */
data class DailyForecast(
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val weatherCode: Int,
    val weatherDescription: String,
    val precipitationSum: Double? = null,
    val windSpeedMax: Double? = null,
    val humidityMax: Int? = null,
    val apparentTemperature: Double? = null
)
