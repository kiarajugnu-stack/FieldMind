package fieldmind.research.app.features.field.data.weather

import kotlin.math.roundToInt

/**
 * Converts raw weather API values (Open-Meteo returns Celsius, km/h, km)
 * to the user's preferred display units.
 *
 * Usage: call `converter.temp(23.5)` inside a composable or ViewModel
 * that observes [fieldmind.research.app.features.field.data.settings.FieldMindSettings.tempUnit],
 * [windSpeedUnit], and [distanceUnit].
 */
object WeatherUnitConverter {

    // ── Temperature ──

    /** Convert Celsius → user's preferred temp unit symbol (°C or °F). */
    fun tempLabel(unit: String): String = when (unit) {
        "Fahrenheit" -> "°F"
        else -> "°C"
    }

    /** Convert Celsius value to the display unit. */
    fun temp(celsius: Double?, targetUnit: String): Double? = when (targetUnit) {
        "Fahrenheit" -> celsius?.let { it * 9.0 / 5.0 + 32.0 }
        else -> celsius
    }

    /** Format a temperature value for display (e.g. "23°C" or "73°F"). */
    fun formatTemp(celsius: Double?, targetUnit: String): String {
        val converted = temp(celsius, targetUnit) ?: return "--"
        return "${converted.roundToInt()}${tempLabel(targetUnit)}"
    }

    /** Format with one decimal (e.g. "23.5°C"). */
    fun formatTempPrecise(celsius: Double?, targetUnit: String): String {
        val converted = temp(celsius, targetUnit) ?: return "--"
        return "%.1f%s".format(converted, tempLabel(targetUnit))
    }

    // ── Wind speed ──

    /** Convert km/h → user's preferred speed unit symbol. */
    fun windLabel(unit: String): String = when (unit) {
        "mph" -> "mph"
        "knots" -> "kn"
        else -> "km/h"
    }

    /** Convert km/h value to the display unit. */
    fun windSpeed(kmh: Double?, targetUnit: String): Double? = when (targetUnit) {
        "mph" -> kmh?.times(0.621371)
        "knots" -> kmh?.times(0.539957)
        else -> kmh
    }

    /** Format a wind speed value (e.g. "15 km/h" or "9 mph"). */
    fun formatWind(kmh: Double?, targetUnit: String): String {
        val converted = windSpeed(kmh, targetUnit) ?: return "--"
        return "%.1f %s".format(converted, windLabel(targetUnit))
    }

    /** Format with no decimal (whole number). */
    fun formatWindInt(kmh: Double?, targetUnit: String): String {
        val converted = windSpeed(kmh, targetUnit) ?: return "--"
        return "${converted.roundToInt()} ${windLabel(targetUnit)}"
    }

    // ── Distance ──

    /** Convert km → user's preferred distance unit symbol. */
    fun distanceLabel(unit: String): String = when (unit) {
        "miles" -> "mi"
        else -> "km"
    }

    /** Convert km value to the display unit. */
    fun distance(km: Double?, targetUnit: String): Double? = when (targetUnit) {
        "miles" -> km?.times(0.621371)
        else -> km
    }

    /** Format a distance value. */
    fun formatDistance(km: Double?, targetUnit: String): String {
        val converted = distance(km, targetUnit) ?: return "--"
        return "%.1f %s".format(converted, distanceLabel(targetUnit))
    }
}
