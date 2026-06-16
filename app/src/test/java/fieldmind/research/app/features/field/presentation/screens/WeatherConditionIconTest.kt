package fieldmind.research.app.features.field.presentation.screens

import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [weatherConditionIcon] pure function that maps
 * WMO weather codes to Material Symbol icon references.
 *
 * Tests use the actual [FieldMindIcons] objects so the mapping is verified
 * against production icon names (e.g. "partly_cloudy_day", "rainy").
 */
class WeatherConditionIconTest {

    @Test
    fun clearSky_returnsPartlyCloudyDay() {
        assertEquals(FieldMindIcons.Weather, weatherConditionIcon(0))
        assertEquals(FieldMindIcons.Weather, weatherConditionIcon(1))
    }

    @Test
    fun partlyCloudy_returnsCloud() {
        assertEquals(FieldMindIcons.Cloud, weatherConditionIcon(2))
    }

    @Test
    fun overcast_returnsCloud() {
        assertEquals(FieldMindIcons.Cloud, weatherConditionIcon(3))
    }

    @Test
    fun fogAndRimeFog_returnsFoggy() {
        assertEquals(FieldMindIcons.Foggy, weatherConditionIcon(45))
        assertEquals(FieldMindIcons.Foggy, weatherConditionIcon(48))
    }

    @Test
    fun drizzle_returnsRainy() {
        val drizzleCodes = listOf(51, 53, 55, 56, 57)
        drizzleCodes.forEach { code ->
            assertEquals("Code $code should map to Rainy", FieldMindIcons.Rainy, weatherConditionIcon(code))
        }
    }

    @Test
    fun rain_returnsRainy() {
        val rainCodes = listOf(61, 63, 65, 66, 67, 80, 81, 82)
        rainCodes.forEach { code ->
            assertEquals("Code $code should map to Rainy", FieldMindIcons.Rainy, weatherConditionIcon(code))
        }
    }

    @Test
    fun snow_returnsSnowy() {
        val snowCodes = listOf(71, 73, 75, 77, 85, 86)
        snowCodes.forEach { code ->
            assertEquals("Code $code should map to Snowy", FieldMindIcons.Snowy, weatherConditionIcon(code))
        }
    }

    @Test
    fun thunderstorm_returnsThunderstorm() {
        val stormCodes = listOf(95, 96, 99)
        stormCodes.forEach { code ->
            assertEquals("Code $code should map to Thunderstorm", FieldMindIcons.Thunderstorm, weatherConditionIcon(code))
        }
    }

    @Test
    fun unknownCode_fallsBackToWeather() {
        assertEquals(FieldMindIcons.Weather, weatherConditionIcon(-1))
        assertEquals(FieldMindIcons.Weather, weatherConditionIcon(999))
        assertEquals(FieldMindIcons.Weather, weatherConditionIcon(100))
    }

    @Test
    fun allKnownWmoCodesProduceNonNullIcons() {
        val allKnownCodes = listOf(
            0, 1, 2, 3,
            45, 48,
            51, 53, 55, 56, 57,
            61, 63, 65, 66, 67,
            71, 73, 75, 77,
            80, 81, 82,
            85, 86,
            95, 96, 99
        )
        allKnownCodes.forEach { code ->
            val icon = weatherConditionIcon(code)
            // MaterialSymbolIcon is a data class — verify it has a non-blank name
            assert(icon.name.isNotBlank()) { "Code $code produced icon with blank name" }
        }
    }

    @Test
    fun mappingIsConsistentWithDataClassEquality() {
        // MaterialSymbolIcon is a data class, so equals compares by name/filled/defaultWeight
        val clearIcon = weatherConditionIcon(0)
        val alsoClear = weatherConditionIcon(1)
        assertEquals("Same weather type should return equal icon objects", clearIcon, alsoClear)

        val rainIcon = weatherConditionIcon(61)
        val alsoRain = weatherConditionIcon(80)
        assertEquals("Same icon type should return equal icon objects", rainIcon, alsoRain)
    }
}
