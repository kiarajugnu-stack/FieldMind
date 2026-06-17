package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.pow
import kotlin.random.Random
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme

/**
 * Time-of-day phase for granular color palettes.
 * Used to shift sky colors throughout the day from warm sunrise
 * through bright midday to deep night.
 */
enum class TimeOfDay {
    Dawn,       // Pre-sunrise — soft purple-pink
    Sunrise,    // Golden orange glow
    Morning,    // Soft warm blue
    Midday,     // Bright crisp blue
    Afternoon,  // Slightly warmer blue
    Sunset,     // Orange-red-purple
    Twilight,   // Deep indigo-purple
    Night       // Dark blue-black
}

/**
 * Animated weather scene that renders Canvas-drawn weather effects based on WMO weather codes.
 * Temperature drives the color palette; sunrise/sunset times determine time-of-day colors.
 *
 * Usage:
 *   AnimatedWeatherScene(
 *       weatherCode = observation.weatherCode,
 *       temperature = weather.temperature,
 *       sunrise = weather.sunrise,
 *       sunset = weather.sunset
 *   )
 */
@Composable
fun AnimatedWeatherScene(
    weatherCode: Int,
    temperature: Double?,
    sunrise: String? = null,
    sunset: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    forceNight: Boolean? = null
) {
    val computedTimeOfDay = computeTimeOfDay(sunrise, sunset)
    val forcedTimeOfDay = when (forceNight) {
        true -> TimeOfDay.Night
        false -> TimeOfDay.Midday  // Force day
        null -> computedTimeOfDay
    }
    val timeOfDay = forcedTimeOfDay
    val isDarkTheme = FieldMindTheme.colors.isDark
    val palette = weatherPalette(temperature, timeOfDay, isDarkTheme)
    val isDay = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight

    // In preview/inspection mode, show a static frame
    if (LocalInspectionMode.current) {
        StaticWeatherFrame(weatherCode, palette, modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(palette.background),
                size = size
            )
        }

        // Specific weather effects
        // weatherCode -1 = day cloudy, -2 = night sky (used by weather widget for enhanced display)
        when {
            weatherCode == -1 -> DayCloudyScene(palette, compact, timeOfDay, modifier)
            weatherCode == -2 -> NightSkyScene(palette, compact, timeOfDay, modifier)
            weatherCode in 0..1 -> if (isDay) DayCloudyScene(palette, compact, timeOfDay, modifier) else NightSkyScene(palette, compact, timeOfDay, modifier)
            weatherCode in 2..3 -> CloudyScene(palette, compact, timeOfDay, modifier)
            weatherCode in 45..48 -> FogScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode in 51..67 || weatherCode in 80..82 -> RainScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode in 71..77 || weatherCode in 85..86 -> SnowScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode >= 95 -> ThunderstormScene(palette, compact, timeOfDay, modifier)
            else -> ClearSkyScene(palette, timeOfDay, compact, modifier)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Time-of-day computation
// ══════════════════════════════════════════════════════════════════════

/**
 * Compute the current time-of-day phase based on the device's local time.
 * Uses the current hour to determine whether it's dawn, sunrise, morning,
 * midday, afternoon, sunset, twilight, or night.
 *
 * This always uses device local time for consistency — avoids flickering
 * that can occur when switching between API-based sunrise/sunset parsing
 * and the local hour fallback, especially during data refresh cycles.
 */
private fun computeTimeOfDay(sunrise: String?, sunset: String?): TimeOfDay {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..5 -> TimeOfDay.Dawn
        in 6..7 -> TimeOfDay.Sunrise
        in 8..10 -> TimeOfDay.Morning
        in 11..14 -> TimeOfDay.Midday
        in 15..16 -> TimeOfDay.Afternoon
        in 17..18 -> TimeOfDay.Sunset
        in 19..20 -> TimeOfDay.Twilight
        else -> TimeOfDay.Night
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Color Palette
// ══════════════════════════════════════════════════════════════════════

/**
 * Raw time-of-day color components before temperature modulation and theme processing.
 * Named fields avoid Kotlin's 5-component List destructuring limit.
 */
private data class TimeOfDayColors(
    val skyTop: Color,
    val skyBottom: Color,
    val skyAccent: Color,
    val sunCol: Color,
    val sunGlowCol: Color,
    val moonCol: Color,
    val moonGlowCol: Color,
    val cloudCol: Color,
    val hazeCol: Color
)

data class WeatherPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: List<Color>,
    val sunColor: Color = Color(0xFFFFF176),
    val sunGlowColor: Color = Color(0xFFFFF9C4),
    val moonColor: Color = Color(0xFFECEFF1),
    val moonGlowColor: Color = Color(0xFFE3F2FD),
    val cloudBaseColor: Color = Color.White,
    val hazeColor: Color = Color.Transparent
)

private fun weatherPalette(temp: Double?, timeOfDay: TimeOfDay, isDarkTheme: Boolean): WeatherPalette {
    val tempC = temp ?: 20.0

    // ── Time-of-day sky colors (override temperature base) ──
    val timeColors = when (timeOfDay) {
        TimeOfDay.Dawn -> TimeOfDayColors(
            skyTop = Color(0xFF8B6FC0),
            skyBottom = Color(0xFFF5C6A0),
            skyAccent = Color(0xFFE8A0B4),
            sunCol = Color(0xFFFFD4A8),
            sunGlowCol = Color(0xFFFFE0C0),
            moonCol = Color(0xFFC8B8D8),
            moonGlowCol = Color(0xFFD0C0E0),
            cloudCol = Color(0xFFF0E0D0),
            hazeCol = Color(0xFFFFD4B0)
        )
        TimeOfDay.Sunrise -> TimeOfDayColors(
            skyTop = Color(0xFFFF8A50),
            skyBottom = Color(0xFFFFD54F),
            skyAccent = Color(0xFFFFAB40),
            sunCol = Color(0xFFFFF176),
            sunGlowCol = Color(0xFFFFF9C4),
            moonCol = Color(0xFFE0C8A0),
            moonGlowCol = Color(0xFFFFE0B0),
            cloudCol = Color(0xFFFFE0C0),
            hazeCol = Color(0xFFFFD4A0)
        )
        TimeOfDay.Morning -> TimeOfDayColors(
            skyTop = Color(0xFF87CEEB),
            skyBottom = Color(0xFFB0E0E6),
            skyAccent = Color(0xFF64B5F6),
            sunCol = Color(0xFFFFF176),
            sunGlowCol = Color(0xFFFFF9C4),
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color(0xFFFFF8E1),
            hazeCol = Color(0xFFFFF9C4)
        )
        TimeOfDay.Midday -> TimeOfDayColors(
            skyTop = Color(0xFF4A90D9),
            skyBottom = Color(0xFF87CEEB),
            skyAccent = Color(0xFF42A5F5),
            sunCol = Color(0xFFFFF176),
            sunGlowCol = Color(0xFFFFF9C4),
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color.White,
            hazeCol = Color(0xFFE3F2FD)
        )
        TimeOfDay.Afternoon -> TimeOfDayColors(
            skyTop = Color(0xFF5BA3D9),
            skyBottom = Color(0xFF9AC8E0),
            skyAccent = Color(0xFF64B5F6),
            sunCol = Color(0xFFFFF176),
            sunGlowCol = Color(0xFFFFF9C4),
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color(0xFFFFF8E1),
            hazeCol = Color(0xFFFFF3E0)
        )
        TimeOfDay.Sunset -> TimeOfDayColors(
            skyTop = Color(0xFFFF6B35),
            skyBottom = Color(0xFF9C27B0),
            skyAccent = Color(0xFFFF5252),
            sunCol = Color(0xFFFF8A65),
            sunGlowCol = Color(0xFFFF6B35),
            moonCol = Color(0xFFC8A0C0),
            moonGlowCol = Color(0xFFE0B0D0),
            cloudCol = Color(0xFFF0D0C0),
            hazeCol = Color(0xFFFFAB91)
        )
        TimeOfDay.Twilight -> TimeOfDayColors(
            skyTop = Color(0xFF283593),
            skyBottom = Color(0xFF4A148C),
            skyAccent = Color(0xFF7C4DFF),
            sunCol = Color(0xFFFFCC80),
            sunGlowCol = Color(0xFFFFAB91),
            moonCol = Color(0xFFB0BEC5),
            moonGlowCol = Color(0xFF90A4AE),
            cloudCol = Color(0xFF546E7A),
            hazeCol = Color(0xFF7E57C2)
        )
        TimeOfDay.Night -> TimeOfDayColors(
            skyTop = Color(0xFF0D0D2B),
            skyBottom = Color(0xFF1A1A3E),
            skyAccent = Color(0xFF5C6BC0),
            sunCol = Color(0xFFECEFF1),
            sunGlowCol = Color(0xFFB0BEC5),
            moonCol = Color(0xFFECEFF1),
            moonGlowCol = Color(0xFFB3E5FC),
            cloudCol = Color(0xFF37474F),
            hazeCol = Color(0xFF1A237E)
        )
    }
    val skyTop = timeColors.skyTop
    val skyBottom = timeColors.skyBottom
    val skyAccent = timeColors.skyAccent
    val sunCol = timeColors.sunCol
    val sunGlowCol = timeColors.sunGlowCol
    val moonCol = timeColors.moonCol
    val moonGlowCol = timeColors.moonGlowCol
    val cloudCol = timeColors.cloudCol
    val hazeCol = timeColors.hazeCol

    // ── Temperature modulation: blend toward warm (hot) or cool (cold) ──
    val tempBlend = when {
        tempC < -10 -> 0.2f   // Very cold: blue-shift 20%
        tempC < 0 -> 0.1f
        tempC > 35 -> 0.15f   // Very hot: warm-shift 15%
        tempC > 28 -> 0.08f
        else -> 0f
    }
    val warmColor = Color(0xFFFF8A65)
    val coolColor = Color(0xFF64B5F6)

    val modulatedTop = if (tempBlend > 0f && tempC > 28) {
        skyTop.let { Color(
            (it.red + (warmColor.red - it.red) * tempBlend).coerceIn(0f, 1f),
            (it.green + (warmColor.green - it.green) * tempBlend).coerceIn(0f, 1f),
            (it.blue + (warmColor.blue - it.blue) * tempBlend).coerceIn(0f, 1f),
            it.alpha
        )}
    } else if (tempBlend > 0f && tempC < 0) {
        skyTop.let { Color(
            (it.red + (coolColor.red - it.red) * tempBlend).coerceIn(0f, 1f),
            (it.green + (coolColor.green - it.green) * tempBlend).coerceIn(0f, 1f),
            (it.blue + (coolColor.blue - it.blue) * tempBlend).coerceIn(0f, 1f),
            it.alpha
        )}
    } else skyTop

    if (isDarkTheme) {
        // Dark mode: deeper, more saturated versions
        // Use different multipliers for day vs night scenes
        val isNighttime = timeOfDay == TimeOfDay.Night || timeOfDay == TimeOfDay.Twilight
        
        val dayTopMul = 0.42f  // Brighter for daytime (was 0.35f)
        val dayTopMulG = 0.38f  // (was 0.30f)
        val dayTopMulB = 0.50f  // (was 0.40f)
        val dayBotMul = 0.35f   // (was 0.25f)
        val dayBotMulG = 0.28f  // (was 0.20f)
        val dayBotMulB = 0.40f  // (was 0.30f)
        
        val nightTopMul = 0.28f  // Keep night dark (was 0.35f)
        val nightTopMulG = 0.25f
        val nightTopMulB = 0.35f
        val nightBotMul = 0.18f
        val nightBotMulG = 0.15f
        val nightBotMulB = 0.22f
        
        val topMulR = if (isNighttime) nightTopMul else dayTopMul
        val topMulG = if (isNighttime) nightTopMulG else dayTopMulG
        val topMulB = if (isNighttime) nightTopMulB else dayTopMulB
        val botMulR = if (isNighttime) nightBotMul else dayBotMul
        val botMulG = if (isNighttime) nightBotMulG else dayBotMulG
        val botMulB = if (isNighttime) nightBotMulB else dayBotMulB
        
        val darkTop = modulatedTop.copy(
            red = (modulatedTop.red * topMulR).coerceAtMost(0.55f),
            green = (modulatedTop.green * topMulG).coerceAtMost(0.45f),
            blue = (modulatedTop.blue * topMulB).coerceAtMost(0.65f)
        )
        val darkBottom = skyBottom.copy(
            red = (skyBottom.red * botMulR).coerceAtMost(0.40f),
            green = (skyBottom.green * botMulG).coerceAtMost(0.35f),
            blue = (skyBottom.blue * botMulB).coerceAtMost(0.50f)
        )
        val bgColors = when (timeOfDay) {
            TimeOfDay.Night, TimeOfDay.Twilight -> listOf(
                Color(0xFF050510),  // Near-black
                Color(0xFF0A0A20),  // Very dark indigo
                darkBottom.copy(alpha = 0.85f)
            )
            TimeOfDay.Sunrise, TimeOfDay.Morning, TimeOfDay.Midday, TimeOfDay.Afternoon -> listOf(
                darkTop.copy(alpha = 0.92f),
                darkBottom.copy(alpha = 0.88f)
            )
            TimeOfDay.Sunset, TimeOfDay.Dawn -> listOf(
                darkTop,
                darkBottom.copy(red = darkBottom.red * 1.2f, green = darkBottom.green * 0.8f, blue = darkBottom.blue * 1.3f)
            )
        }
        
        // Higher cloud alpha for daytime dark mode so clouds are visible
        val cloudAlpha = if (isNighttime) 0.18f else 0.35f
        
        return WeatherPalette(
            primary = darkTop,
            secondary = darkBottom,
            accent = skyAccent.copy(alpha = if (isNighttime) 0.5f else 0.8f),
            background = bgColors,
            sunColor = sunCol.copy(alpha = if (isNighttime) 0.6f else 0.9f),
            sunGlowColor = sunGlowCol.copy(alpha = if (isNighttime) 0.2f else 0.4f),
            moonColor = moonCol,
            moonGlowColor = moonGlowCol.copy(alpha = if (isNighttime) 0.5f else 0.3f),
            cloudBaseColor = cloudCol.copy(alpha = cloudAlpha),
            hazeColor = hazeCol.copy(alpha = if (isNighttime) 0.04f else 0.08f)
        )
    } else {
        // Light mode: preserve more color saturation — only slightly lighten
        // Stormy weather (rain, thunder) gets a darker, moodier sky
        val isStormy = tempC < 15.0  // Lower temps tend to be stormy/rainy
        val lightTop = modulatedTop.copy(
            red = (modulatedTop.red * 0.88f + 0.12f * if (isStormy) 0.3f else 1.0f).coerceAtMost(1f),
            green = (modulatedTop.green * 0.88f + 0.12f * if (isStormy) 0.3f else 1.0f).coerceAtMost(1f),
            blue = (modulatedTop.blue * 0.90f + 0.10f).coerceAtMost(1f)
        )
        val lightBottom = skyBottom.copy(
            red = (skyBottom.red * 0.80f + 0.20f * if (isStormy) 0.3f else 1.0f).coerceAtMost(1f),
            green = (skyBottom.green * 0.80f + 0.20f * if (isStormy) 0.3f else 1.0f).coerceAtMost(1f),
            blue = (skyBottom.blue * 0.82f + 0.18f).coerceAtMost(1f)
        )
        val bgColors = when (timeOfDay) {
            TimeOfDay.Night, TimeOfDay.Twilight -> listOf(
                lightTop.copy(alpha = 0.7f),
                lightBottom.copy(alpha = 0.6f)
            )
            TimeOfDay.Sunset, TimeOfDay.Dawn -> listOf(
                lightTop.copy(red = lightTop.red * 1.1f, green = lightTop.green * 0.85f, blue = lightTop.blue * 0.9f),
                lightBottom.copy(red = lightBottom.red * 1.2f, green = lightBottom.green * 0.8f, blue = lightBottom.blue * 0.85f)
            )
            else -> listOf(lightTop, lightBottom)
        }
        return WeatherPalette(
            primary = lightTop,
            secondary = lightBottom,
            accent = skyAccent.copy(alpha = 0.8f),
            background = bgColors,
            sunColor = sunCol,
            sunGlowColor = sunGlowCol.copy(alpha = 0.6f),
            moonColor = moonCol.copy(alpha = 0.8f),
            moonGlowColor = moonGlowCol.copy(alpha = 0.3f),
            cloudBaseColor = cloudCol.copy(alpha = if (isStormy) 0.55f else 0.50f),
            hazeColor = hazeCol.copy(alpha = if (isStormy) 0.15f else 0.08f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Enhanced Day Scene — Sun + drifting clouds + weather condition
// ══════════════════════════════════════════════════════════════════════

/**
 * Day scene with sun, drifting clouds, and subtle weather condition overlay.
 * Used by the weather widget as the default day background.
 */
@Composable
private fun DayCloudyScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "dayClouds")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate"
    )
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunGlow"
    )
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift2"
    )
    // Cloud morph — slow shape evolution for lifelike drift
    val cloudMorph by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudMorph"
    )

    // Sky and cloud colors derived from palette and time of day
    val cloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.30f else 0.42f)
    val cloudColorDark = palette.primary.copy(alpha = if (isDark) 0.20f else 0.28f)
    val sunBody = palette.sunColor.copy(alpha = 0.95f)
    val sunInner = palette.sunGlowColor.copy(alpha = 0.85f)
    val rayColor = palette.sunGlowColor.copy(alpha = 0.15f * sunGlow)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.85f  // Top-right corner
        val cy = size.height * 0.12f
        val sunRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f

        // Sun glow — smaller, more subtle
        drawCircle(
            color = palette.accent.copy(alpha = sunGlow * 0.10f),
            radius = sunRadius * 1.8f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = sunGlow * 0.05f),
            radius = sunRadius * 2.5f,
            center = Offset(cx, cy)
        )

        // Sun rays — shorter, fewer
        val rayCount = if (compact) 5 else 8
        val rayLength = sunRadius * 1.5f
        for (i in 0 until rayCount) {
            val angle = sunRotation + (360f / rayCount) * i
            val rad = (angle * PI.toFloat() / 180f)
            val x1 = cx + cos(rad) * sunRadius * 0.9f
            val y1 = cy + sin(rad) * sunRadius * 0.9f
            val x2 = cx + cos(rad) * rayLength
            val y2 = cy + sin(rad) * rayLength
            drawLine(
                color = rayColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = if (compact) 1.5f else 2.5f
            )
        }

        // Sun body
        drawCircle(
            color = sunBody,
            radius = sunRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = sunInner,
            radius = sunRadius * 0.55f,
            center = Offset(cx, cy)
        )

        // Back layer clouds (slow drift, behind sun)
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.1f,
            baseY = size.height * 0.15f,
            scale = size.width * 0.45f,
            color = cloudColor.copy(alpha = 0.3f),
            morph = cloudMorph
        )
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.55f,
            baseY = size.height * 0.12f,
            scale = size.width * 0.35f,
            color = cloudColor.copy(alpha = 0.25f),
            morph = cloudMorph + 2f
        )

        // Front layer clouds
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.25f,
            baseY = size.height * 0.55f,
            scale = size.width * 0.5f,
            color = cloudColorDark.copy(alpha = 0.35f),
            morph = cloudMorph + 4f
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.65f,
            baseY = size.height * 0.5f,
            scale = size.width * 0.4f,
            color = cloudColorDark.copy(alpha = 0.3f),
            morph = cloudMorph + 1f
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.8f,
            baseY = size.height * 0.7f,
            scale = size.width * 0.3f,
            color = cloudColor.copy(alpha = 0.25f),
            morph = cloudMorph + 3f
        )

        // Tree line silhouette (static — only sky elements should animate)
        drawTreeLine(isDark = isDark)

        // Atmospheric haze colored by time of day
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = isDark, hazeAlpha = sunGlow * 0.6f)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Enhanced Night Scene — Moon + bright stars + weather condition
// ══════════════════════════════════════════════════════════════════════

/**
 * Night scene with crescent moon, bright twinkling stars, and drifting clouds.
 * Used by the weather widget as the default night background.
 */
@Composable
private fun NightSkyScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "nightSky")
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "nightClouds"
    )
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "moonGlow"
    )
    val cloudMorph by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(15000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nightCloudMorph"
    )
    // Shooting stars — multiple random positions, steeper angle, less frequent
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, delayMillis = 5000, easing = LinearEasing), RepeatMode.Restart),
        label = "shootingStar"
    )

    val starCount = if (compact) 25 else 60
    val stars = remember { rememberStarPositions(starCount) }
    // Pre-compute star twinkle phases for independent twinkling
    val starPhases = remember {
        val rng = Random(999)
        List(starCount) {
            StarPhase(
                speed = 1.5f + rng.nextFloat() * 3.5f,
                phase = rng.nextFloat() * 6.28f,
                brightness = 0.3f + rng.nextFloat() * 0.7f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.85f  // Top-right corner
        val cy = size.height * 0.18f
        val moonRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f

        val moonBody = palette.moonColor.copy(alpha = 1f)
        val moonGlowColor = palette.moonGlowColor
        val starBright = if (isDark) Color(0xFFB3E5FC) else Color(0xFFB3E5FC).copy(alpha = 0.8f)  // Blue-white for bright stars
        val starWarm = when (timeOfDay) {
            TimeOfDay.Twilight, TimeOfDay.Dawn -> Color(0xFFFFF9C4)
            TimeOfDay.Sunset -> Color(0xFFFFCC80)
            else -> Color(0xFFE0E8F0)
        }
        val nightCloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.18f else 0.12f)

        // Moon glow ring — tighter and subtler so it doesn't cover everything
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.08f * moonGlow),
            radius = moonRadius * 2.0f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.03f * moonGlow),
            radius = moonRadius * 3.0f,
            center = Offset(cx, cy)
        )

        // Moon body with phase-aware shape
        val moonPhase = getMoonPhaseValue()
        drawMoonPhase(
            phaseValue = moonPhase,
            cx = cx,
            cy = cy,
            radius = moonRadius,
            litColor = moonBody,
            shadowColor = palette.background.last().copy(alpha = if (isDark) 0.75f else 0.65f),
            glowColor = moonGlowColor
        )

        // Stars with independent twinkle — each star has its own speed and phase
        stars.forEachIndexed { index, (x, y) ->
            val phase = starPhases[index]
            val twinkle = (sin(moonGlow * phase.speed + phase.phase) * 0.5f + 0.5f)
                .coerceIn(0.15f, 1f) * phase.brightness
            val starRadius = 0.8f + twinkle * 2.5f
            val starColor = when {
                twinkle > 0.7f -> starBright
                twinkle > 0.4f -> starWarm
                else -> Color.White
            }
            val drawAlpha = twinkle.coerceIn(0.2f, 0.95f)
            drawCircle(
                color = starColor.copy(alpha = drawAlpha),
                radius = starRadius,
                center = Offset(x * size.width, y * size.height)
            )
            // Cross glow on brightest stars
            if (twinkle > 0.75f && !compact) {
                drawLine(
                    color = starColor.copy(alpha = drawAlpha * 0.3f),
                    start = Offset(x * size.width - starRadius * 2, y * size.height),
                    end = Offset(x * size.width + starRadius * 2, y * size.height),
                    strokeWidth = 0.8f
                )
                drawLine(
                    color = starColor.copy(alpha = drawAlpha * 0.3f),
                    start = Offset(x * size.width, y * size.height - starRadius * 2),
                    end = Offset(x * size.width, y * size.height + starRadius * 2),
                    strokeWidth = 0.8f
                )
            }
        }

        // Shooting stars — 3 random positions, 50° angle, visible for only 35% of cycle
        val nightStarPositions = listOf(
            Offset(size.width * 0.5f, size.height * 0.08f),
            Offset(size.width * 0.75f, size.height * 0.15f),
            Offset(size.width * 0.6f, size.height * 0.05f)
        )
        if (shootingStarProgress < 0.35f) {
            val ssProgress = shootingStarProgress / 0.35f
            val starIndex = (shootingStarProgress * 6f).toInt().coerceIn(0, 2)
            val starPos = nightStarPositions[starIndex]
            drawShootingStar(
                progress = ssProgress,
                startX = starPos.x,
                startY = starPos.y,
                angleDeg = -50f
            )
        }

        // Ground terrain
        drawGround(weatherCode = -2, isDay = false, isDark = isDark, compact = compact)

        // Drifting faint clouds at night
        drawCloud(
            offset = cloudOffset,
            baseX = size.width * 0.15f,
            baseY = size.height * 0.55f,
            scale = size.width * 0.4f,
            color = nightCloudColor,
            morph = cloudMorph
        )
        drawCloud(
            offset = cloudOffset,
            baseX = size.width * 0.55f,
            baseY = size.height * 0.7f,
            scale = size.width * 0.35f,
            color = nightCloudColor.copy(alpha = nightCloudColor.alpha * 0.7f),
            morph = cloudMorph + 2f
        )

        // Atmospheric haze
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = true, hazeAlpha = moonGlow)
    }
}

// Star twinkle data: each star gets its own speed, phase offset, and base brightness
private data class StarPhase(
    val speed: Float,
    val phase: Float,
    val brightness: Float
)

// ══════════════════════════════════════════════════════════════════════
//  Clear Sky — Sun with rotating rays / Moon + drifting stars
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ClearSkyScene(
    palette: WeatherPalette,
    timeOfDay: TimeOfDay,
    compact: Boolean,
    modifier: Modifier
) {
    val isDay = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight
    val infiniteTransition = rememberInfiniteTransition(label = "clearSky")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate"
    )
    // Sun pulse glow — slower
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunGlow"
    )
    // Shooting stars for night mode — less frequent
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, delayMillis = 4000, easing = LinearEasing), RepeatMode.Restart),
        label = "shootingStar"
    )
    // Drifting particles (dust motes / fireflies)
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "particleDrift"
    )

    val rayCount = if (compact) 6 else 12
    val starCount = if (compact) 10 else 30
    val isDark = FieldMindTheme.colors.isDark

    // Pre-generate star positions and particle positions
    val stars = remember { rememberStarPositions(starCount) }
    val starPhases = remember {
        val rng = Random(888)
        List(starCount) {
            StarPhase(
                speed = 1.5f + rng.nextFloat() * 3.5f,
                phase = rng.nextFloat() * 6.28f,
                brightness = 0.3f + rng.nextFloat() * 0.7f
            )
        }
    }
    val particles = remember {
        val rng = Random(99)
        List(if (compact) 4 else 8) {
            Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 0.5f + 0.5f)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val sunCx = size.width * 0.85f  // Top-right corner
        val sunCy = size.height * 0.12f
        val sunRadius = if (compact) size.minDimension * 0.10f else size.minDimension * 0.08f

        if (isDay) {
            // Outer sun glow — smaller, subtler
            drawCircle(
                color = palette.accent.copy(alpha = sunGlow * 0.10f),
                radius = sunRadius * 1.8f,
                center = Offset(sunCx, sunCy)
            )
            drawCircle(
                color = Color.White.copy(alpha = sunGlow * 0.05f),
                radius = sunRadius * 2.5f,
                center = Offset(sunCx, sunCy)
            )

            // Sun rays — shorter
            val rayLength = sunRadius * 1.3f
            for (i in 0 until rayCount) {
                val angle = sunRotation + (360f / rayCount) * i
                val rad = (angle * PI.toFloat() / 180f)
                val x1 = sunCx + cos(rad) * sunRadius * 0.8f
                val y1 = sunCy + sin(rad) * sunRadius * 0.8f
                val x2 = sunCx + cos(rad) * rayLength
                val y2 = sunCy + sin(rad) * rayLength
                drawLine(
                    color = palette.accent.copy(alpha = 0.20f * sunGlow),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (compact) 1.5f else 2.5f
                )
            }

            // Sun body
            drawCircle(
                color = palette.accent,
                radius = sunRadius,
                center = Offset(sunCx, sunCy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = sunRadius * 0.6f,
                center = Offset(sunCx, sunCy)
            )

            // Drifting dust motes (tiny circles)
            particles.forEach { (x, y, speed) ->
                val px = (x * size.width + particleDrift * size.width * 0.3f * speed) % (size.width + 20f) - 10f
                val py = (y * size.height + particleDrift * size.height * 0.1f * speed) % size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f * sunGlow),
                    radius = 1.5f,
                    center = Offset(px, py)
                )
            }

            // Ground terrain
            drawGround(weatherCode = 0, isDay = true, isDark = isDark, compact = compact)

            // Atmospheric haze
            drawAtmosphericHaze(isDark = false, hazeAlpha = sunGlow * 0.5f, hazeColor = palette.hazeColor)
        } else {
            val moonBody = palette.moonColor.copy(alpha = 1f)
            val moonGlowColor = palette.moonGlowColor
            val moonCx = size.width * 0.85f  // Top-right corner
            val moonCy = size.height * 0.18f
            val moonR = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f

            // Moon glow — tighter so it doesn't cover everything
            drawCircle(
                color = moonGlowColor.copy(alpha = 0.10f),
                radius = moonR * 2.0f,
                center = Offset(moonCx, moonCy)
            )
            drawCircle(
                color = moonGlowColor.copy(alpha = 0.04f),
                radius = moonR * 3.0f,
                center = Offset(moonCx, moonCy)
            )

            // Moon body with phase-aware shape
            val moonPhase = getMoonPhaseValue()
            drawMoonPhase(
                phaseValue = moonPhase,
                cx = moonCx,
                cy = moonCy,
                radius = moonR,
                litColor = moonBody,
                shadowColor = palette.background.last().copy(alpha = if (isDark) 0.75f else 0.7f),
                glowColor = moonGlowColor
            )

            // Stars with independent twinkle
            stars.forEachIndexed { index, (x, y) ->
                val phase = starPhases[index]
                val twinkle = (sin(sunGlow * phase.speed + phase.phase) * 0.5f + 0.5f)
                    .coerceIn(0.15f, 1f) * phase.brightness
                val starColor = if (isDark) Color(0xFFB3E5FC).copy(alpha = twinkle.coerceIn(0.2f, 0.95f)) else Color.White.copy(alpha = twinkle.coerceIn(0.2f, 0.85f))
                val starRadius = 0.8f + twinkle * 2.0f
                drawCircle(
                    color = starColor,
                    radius = starRadius,
                    center = Offset(x * size.width, y * size.height)
                )
            }

            // Shooting stars — 3 random positions, 55° angle, visible for 30% of cycle
            val clearStarPositions = listOf(
                Offset(size.width * 0.55f, size.height * 0.06f),
                Offset(size.width * 0.78f, size.height * 0.12f),
                Offset(size.width * 0.65f, size.height * 0.03f)
            )
            if (shootingStarProgress < 0.30f) {
                val ssProgress = shootingStarProgress / 0.30f
                val starIndex = (shootingStarProgress * 7f).toInt().coerceIn(0, 2)
                val starPos = clearStarPositions[starIndex]
                drawShootingStar(
                    progress = ssProgress,
                    startX = starPos.x,
                    startY = starPos.y,
                    angleDeg = -55f
                )
            }

            // Fireflies / floating particles at night
            particles.forEach { (x, y, speed) ->
                val px = (x * size.width + particleDrift * size.width * 0.2f * speed) % (size.width + 20f) - 10f
                val py = (y * size.height + particleDrift * size.height * 0.08f * speed) % size.height
                val glow = (sin(particleDrift * 4f + x * 10f) * 0.5f + 0.5f) * 0.6f
                val fireflyColor = if (isDark) Color(0xFFFFF9C4).copy(alpha = glow * 0.8f) else Color(0xFFFFF9C4).copy(alpha = glow * 0.5f)
                drawCircle(
                    color = fireflyColor,
                    radius = 2f + glow * 2f,
                    center = Offset(px, py)
                )
            }

            // Ground terrain
            drawGround(weatherCode = -2, isDay = false, isDark = isDark, compact = compact)

            // Atmospheric haze
            drawAtmosphericHaze(isDark = true, hazeAlpha = 0.6f, hazeColor = palette.hazeColor)
        }
    }
}

private fun rememberStarPositions(count: Int): List<Pair<Float, Float>> {
    val seed = 42L // Fixed seed for reproducibility
    val rng = Random(seed)
    return List(count) { rng.nextFloat() to rng.nextFloat() }
}

// ══════════════════════════════════════════════════════════════════════
//  Cloudy — Layered parallax clouds
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun CloudyScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val isDaytime = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift2"
    )
    val cloudOffset3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift3"
    )
    val cloudMorph by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudMorph"
    )
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudSunGlow"
    )

    val cloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.35f else 0.52f)
    val cloudColorDark = palette.primary.copy(alpha = if (isDark) 0.25f else 0.35f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Partially visible sun behind clouds (daytime only)
        if (isDaytime) {
            val sunCx = size.width * 0.82f
            val sunCy = size.height * 0.10f
            val sunRadius = if (compact) size.minDimension * 0.08f else size.minDimension * 0.06f

            // Soft sun glow behind clouds
            drawCircle(
                color = palette.accent.copy(alpha = sunGlow * 0.08f),
                radius = sunRadius * 2.5f,
                center = Offset(sunCx, sunCy)
            )
            drawCircle(
                color = Color.White.copy(alpha = sunGlow * 0.04f),
                radius = sunRadius * 3.5f,
                center = Offset(sunCx, sunCy)
            )

            // Sun body — partially visible through clouds
            drawCircle(
                color = palette.sunColor.copy(alpha = sunGlow * 0.35f),
                radius = sunRadius,
                center = Offset(sunCx, sunCy)
            )
            drawCircle(
                color = palette.sunGlowColor.copy(alpha = sunGlow * 0.20f),
                radius = sunRadius * 0.55f,
                center = Offset(sunCx, sunCy)
            )
        }

        val cloudScale = if (compact) size.width * 0.4f else size.width * 0.5f
        val cloudScale2 = if (compact) size.width * 0.3f else size.width * 0.4f

        // Back layer clouds (slow drift, seamless loop)
        val backDrift = cloudOffset1 % 1f
        drawCloud(offset = backDrift, baseX = size.width * 0.1f, baseY = size.height * 0.2f, scale = cloudScale, color = cloudColor.copy(alpha = 0.4f), morph = cloudMorph)
        drawCloud(offset = backDrift - 1f, baseX = size.width * 0.1f, baseY = size.height * 0.2f, scale = cloudScale, color = cloudColor.copy(alpha = 0.4f), morph = cloudMorph)
        drawCloud(offset = backDrift, baseX = size.width * 0.5f, baseY = size.height * 0.15f, scale = cloudScale * 0.8f, color = cloudColor.copy(alpha = 0.3f), morph = cloudMorph + 1f)
        drawCloud(offset = backDrift - 1f, baseX = size.width * 0.5f, baseY = size.height * 0.15f, scale = cloudScale * 0.8f, color = cloudColor.copy(alpha = 0.3f), morph = cloudMorph + 1f)

        // Middle layer clouds (seamless loop)
        val midDrift = cloudOffset2 % 1f
        drawCloud(offset = midDrift, baseX = size.width * 0.3f, baseY = size.height * 0.35f, scale = cloudScale2, color = cloudColorDark.copy(alpha = 0.5f), morph = cloudMorph + 2f)
        drawCloud(offset = midDrift - 1f, baseX = size.width * 0.3f, baseY = size.height * 0.35f, scale = cloudScale2, color = cloudColorDark.copy(alpha = 0.5f), morph = cloudMorph + 2f)
        drawCloud(offset = midDrift, baseX = size.width * 0.7f, baseY = size.height * 0.3f, scale = cloudScale2 * 0.9f, color = cloudColorDark.copy(alpha = 0.4f), morph = cloudMorph + 3f)
        drawCloud(offset = midDrift - 1f, baseX = size.width * 0.7f, baseY = size.height * 0.3f, scale = cloudScale2 * 0.9f, color = cloudColorDark.copy(alpha = 0.4f), morph = cloudMorph + 3f)

        // Front layer clouds (fast drift, seamless loop)
        val frontDrift = cloudOffset3 % 1f
        drawCloud(offset = frontDrift, baseX = size.width * 0.15f, baseY = size.height * 0.6f, scale = cloudScale * 1.1f, color = cloudColor.copy(alpha = 0.6f), morph = cloudMorph + 4f)
        drawCloud(offset = frontDrift - 1f, baseX = size.width * 0.15f, baseY = size.height * 0.6f, scale = cloudScale * 1.1f, color = cloudColor.copy(alpha = 0.6f), morph = cloudMorph + 4f)
        drawCloud(offset = frontDrift, baseX = size.width * 0.6f, baseY = size.height * 0.55f, scale = cloudScale * 0.7f, color = cloudColor.copy(alpha = 0.5f), morph = cloudMorph + 5f)
        drawCloud(offset = frontDrift - 1f, baseX = size.width * 0.6f, baseY = size.height * 0.55f, scale = cloudScale * 0.7f, color = cloudColor.copy(alpha = 0.5f), morph = cloudMorph + 5f)

        // Ground terrain with mountains
        drawGround(weatherCode = 3, isDay = isDaytime, isDark = isDark, compact = compact)
    }
}

private fun DrawScope.drawCloud(
    offset: Float,
    baseX: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    morph: Float = 0f  // subtle shape morphing for lifelike drift
) {
    val x = (baseX + offset * size.width) % (size.width + scale) - scale * 0.5f

    val cr = scale * 0.14f
    // Billowy cloud clusters — more circles in natural arrangement, with morph wiggle
    drawCircle(color = color, radius = cr * 1.3f, center = Offset(x, baseY))
    drawCircle(color = color, radius = cr * 1.0f, center = Offset(x - cr * 0.8f + morph * 3f, baseY + cr * 0.3f))
    drawCircle(color = color, radius = cr * 0.9f, center = Offset(x + cr * 0.9f - morph * 2f, baseY - cr * 0.15f))
    drawCircle(color = color, radius = cr * 0.8f, center = Offset(x + cr * 1.6f + morph * 2f, baseY + cr * 0.4f))
    drawCircle(color = color, radius = cr * 0.7f, center = Offset(x - cr * 1.4f - morph * 3f, baseY + cr * 0.15f))
    drawCircle(color = color, radius = cr * 0.6f, center = Offset(x + cr * 2.2f + morph * 1f, baseY - cr * 0.05f))
    drawCircle(color = color, radius = cr * 0.5f, center = Offset(x - cr * 2.0f + morph * 2f, baseY + cr * 0.3f))
    drawCircle(color = color, radius = cr * 0.5f, center = Offset(x + cr * 0.3f - morph * 1f, baseY - cr * 0.6f))
    drawCircle(color = color, radius = cr * 0.4f, center = Offset(x - cr * 0.3f, baseY - cr * 0.45f))
}

/**
 * Draw a landscape treeline silhouette with varied tree heights, properly anchored
 * to the ground curve. Trees get smaller toward the edges for a natural look.
 * The baseY aligns with the ground drawn by drawGround/drawMountainRange.
 */
private fun DrawScope.drawTreeLine(morph: Float = 0f, isDark: Boolean = false) {
    val baseColor = if (isDark) Color(0xFF07120B).copy(alpha = 0.72f) else Color(0xFF315331).copy(alpha = 0.30f)
    val detailColor = if (isDark) Color(0xFF020704).copy(alpha = 0.66f) else Color(0xFF173219).copy(alpha = 0.24f)
    val groundY = size.height * 0.88f

    // Compute ground curve that trees sit on (matches drawGround's hill formula)
    fun groundHeight(t: Float): Float {
        val hills = sin(t * 4f) * 0.06f + sin(t * 9f + 1.2f) * 0.04f + sin(t * 18f + 3.7f) * 0.02f
        return groundY - hills * size.height
    }

    // Undulating treeline silhouette (fills gaps between trees)
    val path = Path()
    path.moveTo(-10f, size.height + 10f)
    path.lineTo(-10f, groundY)

    val segments = 40
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val px = t * (size.width + 20f) - 10f
        val noise = sin(t * 8f + morph * 0.5f) * 0.3f +
            sin(t * 15f + morph * 0.3f + 1.3f) * 0.2f +
            sin(t * 25f + morph * 0.7f + 2.7f) * 0.12f +
            sin(t * 40f + morph * 1.1f + 0.5f) * 0.08f
        val treeHeight = noise * size.height * 0.08f + size.height * 0.04f
        val py = groundHeight(t) - treeHeight
        path.lineTo(px, py)
    }
    path.lineTo(size.width + 10f, size.height + 10f)
    path.close()
    drawPath(path = path, color = baseColor, style = Fill)

    // Individual trees — properly anchored to ground curve with size variation.
    // Trees cluster more densely in the center, taper off at edges.
    val treePositions = 24
    for (i in 0 until treePositions) {
        val t = (i.toFloat() + 0.5f) / treePositions
        val edgeWeight = sin(t * PI.toFloat()).coerceIn(0.3f, 1f) // smaller at edges
        val peakNoise = sin(t * 12f + morph * 0.3f + i * 0.7f) * 0.35f + 0.5f
        if (peakNoise > 0.45f) {
            val px = t * size.width
            // Ground height at this x position — same formula as drawGround uses
            val gh = groundHeight(t)
            
            // Tree size varies with position — larger toward center, smaller at edges
            val sizeFactor = edgeWeight * (0.5f + peakNoise * 0.5f)
            val treeH = size.height * (0.025f + sizeFactor * 0.05f)
            val treeW = treeH * (0.18f + sizeFactor * 0.12f)
            
            // Tree base sits on the ground curve
            val treeBaseY = gh
            
            // Trunk
            val trunkH = treeH * 0.25f
            drawRoundRect(
                color = detailColor.copy(alpha = detailColor.alpha * 0.65f),
                topLeft = Offset(px - treeW * 0.2f, treeBaseY - trunkH),
                size = Size(treeW * 0.4f, trunkH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(treeW * 0.15f, treeW * 0.15f)
            )
            
            // Canopy layers (3-4 triangular tiers)
            val canopyLayers = 3 + (i % 2)
            for (layer in 0 until canopyLayers) {
                val layerT = layer.toFloat() / canopyLayers
                val cy = treeBaseY - trunkH - treeH * (0.15f + layerT * 0.45f)
                val halfW = treeW * (0.5f + (canopyLayers - layer) * (0.15f + sizeFactor * 0.1f))
                val canopyH = treeH * (0.18f + (1f - layerT) * 0.12f)
                val canopyPath = Path().apply {
                    moveTo(px - halfW, cy + canopyH * 0.3f)
                    quadraticTo(px, cy - canopyH * 0.6f, px + halfW, cy + canopyH * 0.3f)
                    close()
                }
                drawPath(canopyPath, detailColor, style = Fill)
            }
            
            // Snow on treetop for cold scenes (always subtle white cap)
            if (!isDark && morph < 3f) {
                val snowPath = Path().apply {
                    moveTo(px - treeW * 0.25f, treeBaseY - trunkH - treeH * 0.55f)
                    quadraticTo(px, treeBaseY - trunkH - treeH * 0.65f, px + treeW * 0.25f, treeBaseY - trunkH - treeH * 0.55f)
                    close()
                }
                drawPath(snowPath, Color.White.copy(alpha = 0.12f), style = Fill)
            }
        }
    }
    

}

/**
 * Draw subtle atmospheric haze — a very faint gradient overlay that adds depth.
 * Uses the time-of-day haze color from the palette when provided.
 */
private fun DrawScope.drawAtmosphericHaze(
    isDark: Boolean,
    hazeAlpha: Float = 1f,
    hazeColor: Color = Color.Transparent
) {
    val adjustedHaze = if (hazeColor != Color.Transparent) {
        hazeColor.copy(alpha = if (isDark) 0.04f * hazeAlpha else 0.06f * hazeAlpha)
    } else {
        if (isDark) Color(0xFFB3E5FC).copy(alpha = 0.03f * hazeAlpha) else Color(0xFFFFF9C4).copy(alpha = 0.02f * hazeAlpha)
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(adjustedHaze, Color.Transparent),
            center = Offset(size.width * 0.3f, size.height * 0.2f),
            radius = size.maxDimension * 0.7f
        ),
        size = size
    )
}

/**
 * Draw weather-adaptive ground terrain — rolling hills, beach, snow, or mud.
 * This grounds the scene in a real landscape and makes the weather feel immersive.
 */
private fun DrawScope.drawGround(
    weatherCode: Int,
    isDay: Boolean,
    isDark: Boolean,
    compact: Boolean
) {
    if (compact) return  // No ground in compact mode

    val groundY = size.height * 0.82f
    val isRain = weatherCode in 51..67 || weatherCode in 80..82
    val isSnow = weatherCode in 71..77 || weatherCode in 85..86
    val isThunder = weatherCode >= 95

    // Choose ground colors based on weather and theme
    val (groundTop, groundBottom, detailColor) = when {
        isSnow -> Triple(
            Color(0xFFE8EAF0).copy(alpha = 0.7f),
            Color(0xFFD5D8E0).copy(alpha = 0.5f),
            Color(0xFFD0D4DC).copy(alpha = 0.3f)
        )
        isRain || isThunder -> Triple(
            if (isDark) Color(0xFF1A2A1A).copy(alpha = 0.5f) else Color(0xFF3D5A3D).copy(alpha = 0.3f),
            if (isDark) Color(0xFF0D1A0D).copy(alpha = 0.4f) else Color(0xFF2D4A2D).copy(alpha = 0.25f),
            if (isDark) Color(0xFF152515).copy(alpha = 0.35f) else Color(0xFF354A35).copy(alpha = 0.2f)
        )
        !isDay -> Triple(
            Color(0xFF0A0A1A).copy(alpha = 0.4f),
            Color(0xFF050510).copy(alpha = 0.35f),
            Color(0xFF080818).copy(alpha = 0.3f)
        )
        else -> Triple(
            if (isDark) Color(0xFF1B3A1B).copy(alpha = 0.4f) else Color(0xFF4A7A4A).copy(alpha = 0.2f),
            if (isDark) Color(0xFF0D2A0D).copy(alpha = 0.3f) else Color(0xFF3A6A3A).copy(alpha = 0.15f),
            if (isDark) Color(0xFF152A15).copy(alpha = 0.25f) else Color(0xFF4A6A4A).copy(alpha = 0.12f)
        )
    }

    // Main rolling ground silhouette
    drawMountainRange(isDark = isDark, isSnow = isSnow, isDay = isDay)

    // Main rolling ground silhouette
    val groundPath = Path()
    groundPath.moveTo(-10f, size.height + 10f)
    groundPath.lineTo(-10f, groundY)

    val segments = 50
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val px = t * (size.width + 20f) - 10f
        val hills = sin(t * 4f) * 0.06f + sin(t * 9f + 1.2f) * 0.04f + sin(t * 18f + 3.7f) * 0.02f
        val py = groundY - hills * size.height
        groundPath.lineTo(px, py)
    }
    groundPath.lineTo(size.width + 10f, size.height + 10f)
    groundPath.close()
    drawPath(path = groundPath, color = groundTop, style = Fill)

    // Second ground layer (foreground hills)
    val fgPath = Path()
    fgPath.moveTo(-10f, size.height + 10f)
    fgPath.lineTo(-10f, size.height * 0.90f)
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val px = t * (size.width + 20f) - 10f
        val hills = sin(t * 5f + 0.8f) * 0.04f + sin(t * 12f + 2.1f) * 0.025f
        val py = size.height * 0.90f - hills * size.height
        fgPath.lineTo(px, py)
    }
    fgPath.lineTo(size.width + 10f, size.height + 10f)
    fgPath.close()
    drawPath(path = fgPath, color = groundBottom, style = Fill)

    // Grass/vegetation detail for non-snow scenes
    if (!isSnow) {
        val grassPath = Path()
        for (i in 0..20) {
            val t = (i.toFloat() / 20f) * size.width + 10f
            val bladeHeight = (sin(t * 0.05f + i * 0.7f) * 0.5f + 0.5f) * size.height * 0.015f
            if (bladeHeight > 2f) {
                val bx = t + 10f
                val by = groundY - (sin(t * 0.02f + i * 0.5f) * 0.5f + 0.3f) * size.height * 0.06f
                grassPath.moveTo(bx, by)
                grassPath.lineTo(bx + 3f, by - bladeHeight)
                grassPath.lineTo(bx + 6f, by)
            }
        }
        drawPath(path = grassPath, color = detailColor, style = Fill)
    }

    // Puddles on rainy ground
    if (isRain || isThunder) {
        for (i in 0..4) {
            val px = size.width * (0.1f + i * 0.2f) + sin(i * 2.3f) * size.width * 0.05f
            val py = groundY + size.height * 0.02f + sin(i * 1.7f) * size.height * 0.01f
            val puddleW = size.width * (0.06f + sin(i * 0.5f) * 0.02f)
            val puddleH = size.height * 0.008f
            val puddleColor = if (isDark)
                Color(0xFF4A6A8A).copy(alpha = 0.25f)
            else
                Color(0xFF6A9ABA).copy(alpha = 0.15f)
            drawOval(
                color = puddleColor,
                topLeft = Offset(px - puddleW / 2, py - puddleH / 2),
                size = Size(puddleW, puddleH)
            )
            // Puddle highlight
            drawOval(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = Offset(px - puddleW * 0.3f, py - puddleH * 0.3f),
                size = Size(puddleW * 0.6f, puddleH * 0.4f)
            )
        }
    }
}

/**
 * Draw a layered mountain range with atmospheric perspective, smooth ridgelines,
 * and natural-looking snow caps. Three layers create depth: far (blueish/hazy),
 * mid (warmer), near (darkest). Uses bezier curves for ridgelines instead of
 * sharp peak-to-valley lines for a more organic appearance.
 */
private fun DrawScope.drawMountainRange(isDark: Boolean, isSnow: Boolean, isDay: Boolean) {
    val ridgeBase = size.height * 0.76f
    
    // Three mountain layers with atmospheric perspective
    data class MountainLayer(val baseY: Float, val heightFactor: Float, val color: Color, val snowColor: Color, val peakCount: Int)
    
    val layers = listOf(
        // Far layer — blueish, hazy, lowest contrast, most peaks
        MountainLayer(
            baseY = 0.64f,
            heightFactor = 0.09f,
            color = if (isDark) Color(0xFF1A2A50).copy(alpha = 0.30f) else Color(0xFF90A8C4).copy(alpha = 0.15f),
            snowColor = if (isDark) Color(0xFFC8D8E8).copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f),
            peakCount = 9
        ),
        // Mid layer — richer, more contrast
        MountainLayer(
            baseY = 0.70f,
            heightFactor = 0.11f,
            color = if (isDark) Color(0xFF0F1A3A).copy(alpha = 0.45f) else Color(0xFF6A8A9A).copy(alpha = 0.22f),
            snowColor = if (isDark) Color(0xFFB0C8E0).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.18f),
            peakCount = 7
        ),
        // Near layer — darkest, highest contrast
        MountainLayer(
            baseY = 0.74f,
            heightFactor = 0.13f,
            color = if (isDark) Color(0xFF081428).copy(alpha = 0.58f) else Color(0xFF4A6A5A).copy(alpha = 0.28f),
            snowColor = if (isDark) Color(0xFF90B0C8).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f),
            peakCount = 6
        )
    )
    
    val seedOffset = 137
    
    layers.forEachIndexed { layerIdx, layer ->
        val path = Path()
        path.moveTo(-20f, ridgeBase + size.height * 0.02f)
        
        // Generate organic peak positions and heights using multiple sine waves
        val peakPositions = (0 until layer.peakCount).map { i ->
            val baseX = (i.toFloat() + 0.5f) / layer.peakCount
            // Jitter the positions for natural look - use multiple sine harmonics
            val jitter = sin(i * 2.7f + layerIdx * 1.3f + seedOffset) * 0.04f +
                         sin(i * 5.1f + layerIdx * 3.7f + 42L) * 0.02f
            val px = (baseX + jitter).coerceIn(0.02f, 1.02f)
            // Height with gradual center prominence (mountains tend to be taller in the middle)
            val centerBias = sin(px * PI.toFloat())
            val heightVar = 0.3f + 0.7f * (0.5f + 0.5f * sin(i * 3.1f + layerIdx * 2.7f + seedOffset).pow(2)) * (0.6f + 0.4f * centerBias)
            val py = size.height * (layer.baseY - layer.heightFactor * heightVar)
            px to py
        }
        
        // Build mountain profile using smooth bezier curves between peaks
        var prevX = -20f
        var prevPeakX = -20f
        var prevPeakY = ridgeBase
        
        for ((px, py) in peakPositions) {
            val x = px * size.width
            // Valley point between peaks — lower than peaks but smoothly connected
            val midX = (prevPeakX + x) / 2f
            val valleyDepth = 0.5f + 0.5f * sin(px * 5f + layerIdx * 3.7f + seedOffset).pow(2)
            val valleyY = ridgeBase - size.height * (0.01f + 0.015f * (1f - valleyDepth))
            
            // Use cubic bezier for smooth ridgeline
            val ctrl1x = prevPeakX + (midX - prevPeakX) * 0.4f
            val ctrl2x = midX + (x - midX) * 0.4f
            
            path.cubicTo(ctrl1x, prevPeakY * 0.7f + valleyY * 0.3f, ctrl2x, valleyY * 0.5f + py * 0.5f, midX, valleyY)
            path.cubicTo(midX + (x - midX) * 0.3f, valleyY * 0.5f + py * 0.5f, x - (x - prevPeakX) * 0.1f, py * 0.7f + valleyY * 0.3f, x, py)
            
            prevPeakX = x
            prevPeakY = py
        }
        
        path.lineTo(size.width + 20f, ridgeBase + size.height * 0.02f)
        path.lineTo(size.width + 20f, size.height)
        path.lineTo(-20f, size.height)
        path.close()
        
        // Draw mountain body
        drawPath(path, layer.color, style = Fill)
        
        // Draw snow caps on peaks (only when isSnow or isDay)
        if (isSnow || isDay) {
            for ((px, py) in peakPositions) {
                val x = px * size.width
                val peakHeight = (ridgeBase - py) / size.height
                // Only cap peaks that are tall enough
                if (peakHeight > 0.035f) {
                    val capSize = size.minDimension * (0.01f + peakHeight * 0.07f)
                    
                    // Natural snow cap — follows the peak contour
                    val snowPath = Path()
                    snowPath.moveTo(x - capSize, py + capSize * 0.4f)
                    // Descend from left to peak with slight jitter
                    snowPath.lineTo(x - capSize * 0.6f, py + capSize * 0.1f * sin(px * 11f))
                    snowPath.lineTo(x - capSize * 0.3f, py - capSize * 0.15f)
                    // Peak cap
                    snowPath.lineTo(x, py - capSize * 0.45f)
                    snowPath.lineTo(x + capSize * 0.3f, py - capSize * 0.1f)
                    snowPath.lineTo(x + capSize * 0.6f, py + capSize * 0.2f * cos(px * 9f))
                    snowPath.lineTo(x + capSize, py + capSize * 0.35f)
                    // Snow streaks running down the mountain
                    snowPath.lineTo(x + capSize * 0.65f, py + capSize * 0.7f)
                    snowPath.lineTo(x + capSize * 0.3f, py + capSize * 0.75f)
                    snowPath.lineTo(x, py + capSize * 0.65f)
                    snowPath.lineTo(x - capSize * 0.3f, py + capSize * 0.7f)
                    snowPath.lineTo(x - capSize * 0.65f, py + capSize * 0.6f)
                    snowPath.close()
                    drawPath(snowPath, layer.snowColor, style = Fill)
                    
                    // Snow highlight on peak tip
                    drawCircle(
                        color = layer.snowColor.copy(alpha = layer.snowColor.alpha * 0.5f),
                        radius = capSize * 0.15f,
                        center = Offset(x, py - capSize * 0.3f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Moon Phase Calculation & Drawing
// ══════════════════════════════════════════════════════════════════════

/**
 * Compute the current moon phase as a value from 0.0 to 1.0.
 * 0.0 = New, 0.25 = First Quarter, 0.5 = Full, 0.75 = Last Quarter, 1.0 = New.
 * Uses the same reference new moon epoch (2000-01-06 18:14 UTC).
 */
private fun getMoonPhaseValue(): Float {
    val knownNewMoon = LocalDate.of(2000, 1, 6)
    val today = LocalDate.now()
    val daysSince = ChronoUnit.DAYS.between(knownNewMoon, today).toDouble()
    val lunations = daysSince / 29.53058770576
    return (lunations - floor(lunations)).toFloat()
}

/**
 * Draw the moon with proper phase shape.
 *
 * Phase ranges:
 *   0.0       → New moon (completely dark)
 *   0.125     → Waxing crescent (thin right edge lit)
 *   0.25      → First quarter (right half lit)
 *   0.375     → Waxing gibbous (mostly right lit)
 *   0.5       → Full moon (fully lit)
 *   0.625     → Waning gibbous (mostly left lit)
 *   0.75      → Last quarter (left half lit)
 *   0.875     → Waning crescent (thin left edge lit)
 *   1.0       → New moon
 *
 * Uses two overlaid circles: the moon body (lit side) plus a darker shadow
 * circle offset to create the correct phase shape. For quarter phases a
 * straight cut path is used for a clean half-moon appearance.
 */
private fun DrawScope.drawMoonPhase(
    phaseValue: Float,        // 0.0–1.0 moon phase
    cx: Float,                // center x
    cy: Float,                // center y
    radius: Float,            // moon radius
    litColor: Color,          // illuminated moon color
    shadowColor: Color,       // dark/shadowed portion color
    glowColor: Color,         // subtle inner glow for dark-side detail
) {
    // Step 1: Draw the lit moon body
    drawCircle(color = litColor, radius = radius, center = Offset(cx, cy))

    // Full moon — no shadow overlay
    if (phaseValue in 0.48f..0.52f) return

    // New moon — complete shadow
    if (phaseValue < 0.03f || phaseValue > 0.97f) {
        drawCircle(color = shadowColor, radius = radius, center = Offset(cx, cy))
        // Add a subtle rim glow for the new moon (barely visible edge)
        drawCircle(
            color = glowColor.copy(alpha = 0.15f),
            radius = radius * 0.12f,
            center = Offset(cx, cy + radius * 0.5f)
        )
        return
    }

    val waxing = phaseValue < 0.5f  // lit on the right
    val direction = if (waxing) -1f else 1f
    val darkness = abs(phaseValue - 0.5f) * 2f  // 0 = full, 1 = new

    // At quarter phase (darkness ≈ 0.5), use a straight cut for clean half-moon
    if (darkness in 0.38f..0.62f) {
        val cutX = cx + (0.5f - darkness) * radius * 2.5f * direction
        val shadowPath = Path().apply {
            if (waxing) {
                // Waxing: shadow on LEFT (lit on right)
                // Path covers from left edge (cx - radius) to cutX
                moveTo(cx - radius, cy - radius)
                lineTo(cutX, cy - radius)
                lineTo(cutX, cy + radius)
                lineTo(cx - radius, cy + radius)
            } else {
                // Waning: shadow on RIGHT (lit on left)
                // Path covers from cutX to right edge (cx + radius)
                moveTo(cx + radius, cy - radius)
                lineTo(cutX, cy - radius)
                lineTo(cutX, cy + radius)
                lineTo(cx + radius, cy + radius)
            }
            close()
        }
        drawPath(shadowPath, color = shadowColor, style = Fill)
        return
    }

    // For crescent phases (darkness > 0.62): large shadow with small offset
    // For gibbous phases (darkness < 0.38): small shadow with large offset
    val offsetFactor = when {
        darkness > 0.62f -> (1f - darkness) * 3.2f       // crescent: offset from 0.38 to 1.2
        else -> (1f - darkness * 1.4f) * 1.3f              // gibbous: offset from 0.6 to 1.3
    }
    val shadowOffset = offsetFactor.coerceIn(0.05f, 1.25f) * radius * direction

    drawCircle(
        color = shadowColor,
        radius = radius * 1.03f,
        center = Offset(cx + shadowOffset, cy)
    )

    // For gibbous phases, add a subtle earthshine glow on the dark portion
    if (darkness < 0.38f && darkness > 0.05f) {
        val earthshineX = cx - shadowOffset * 0.5f
        val earthshineAlpha = (1f - darkness / 0.38f) * 0.06f
        drawCircle(
            color = glowColor.copy(alpha = earthshineAlpha),
            radius = radius * 0.7f,
            center = Offset(earthshineX, cy)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shooting Star
// ══════════════════════════════════════════════════════════════════════

/**
 * Draw a shooting star — bright head + fading tail.
 */
private fun DrawScope.drawShootingStar(
    progress: Float,
    startX: Float,
    startY: Float,
    angleDeg: Float = -30f
) {
    val rad = angleDeg * PI.toFloat() / 180f
    val trailLength = size.width * 0.2f
    val speed = 0.8f

    val x = startX + cos(rad) * progress * trailLength * 3f * speed
    val y = startY + sin(rad) * progress * trailLength * 3f * speed

    if (x < -50f || x > size.width + 50f || y < -50f || y > size.height + 50f) return

    // Gradual fade-in (first 25% of flight) + fade-out (toward end)
    val fadeIn = (progress / 0.25f).coerceIn(0f, 1f)
    val alpha = fadeIn * (1f - progress)

    // Trail (fading line)
    val trailPath = Path()
    trailPath.moveTo(x, y)
    trailPath.lineTo(
        x - cos(rad) * trailLength * (1f - progress),
        y - sin(rad) * trailLength * (1f - progress)
    )
    drawPath(
        path = trailPath,
        color = Color.White.copy(alpha = alpha * 0.8f),
        style = Stroke(width = 2f * (1f - progress) + 0.5f)
    )

    // Bright head
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.95f),
        radius = 2.5f * (1f - progress) + 0.5f,
        center = Offset(x, y)
    )
    // Head glow
    drawCircle(
        color = Color(0xFFB3E5FC).copy(alpha = alpha * 0.3f),
        radius = 5f * (1f - progress) + 1f,
        center = Offset(x, y)
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Fog — Translucent drifting fog bands
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FogScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val fogOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "fogDrift"
    )
    val fogAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fogPulse"
    )
    val iceSparkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "iceSparkle"
    )

    val fogBaseColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.5f else 0.65f)
    val fogDarkColor = palette.primary.copy(alpha = if (isDark) 0.4f else 0.55f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Wispy organic fog layers — made of overlapping soft ellipses that roll and drift
        val layerCount = if (compact) 4 else 7

        for (layer in 0 until layerCount) {
            val layerAlpha = fogAlpha * (0.15f + layer * 0.12f).coerceAtMost(0.7f)
            val layerColor = if (layer % 2 == 0) fogBaseColor.copy(alpha = layerAlpha) else fogDarkColor.copy(alpha = layerAlpha * 0.8f)
            val yBase = size.height * (0.1f + layer * 0.13f)

            // Each layer has multiple wispy blobs that drift at slightly different speeds
            val blobCount = if (compact) 2 else 3
            for (blob in 0 until blobCount) {
                val blobSpeed = 1.0f + blob * 0.3f + layer * 0.05f
                val xCenter = (fogOffset * size.width * blobSpeed + layer * size.width * 0.1f + blob * size.width * 0.3f) % (size.width * 1.5f) - size.width * 0.25f
                val blobWidth = size.width * (0.4f + sin(layer * 0.7f + blob * 1.3f) * 0.2f)
                val blobHeight = size.height * (0.04f + sin(layer * 0.5f + blob * 0.9f) * 0.015f).coerceAtLeast(0.02f)

                // Draw soft elliptical fog blob
                drawOval(
                    color = layerColor.copy(alpha = layerAlpha * (0.6f + sin(fogOffset * 2f + layer + blob) * 0.3f).coerceIn(0.3f, 0.9f)),
                    topLeft = Offset(xCenter - blobWidth / 2, yBase - blobHeight / 2),
                    size = Size(blobWidth, blobHeight)
                )

                // Secondary smaller wisp within each blob
                val wispWidth = blobWidth * 0.6f
                val wispHeight = blobHeight * 0.5f
                drawOval(
                    color = layerColor.copy(alpha = layerAlpha * 0.4f),
                    topLeft = Offset(xCenter + blobWidth * 0.1f - wispWidth / 2, yBase + blobHeight * 0.1f - wispHeight / 2),
                    size = Size(wispWidth, wispHeight)
                )
            }

            // Rolling fog ground layer
            if (layer == layerCount - 1) {
                val groundFogColor = fogBaseColor.copy(alpha = fogAlpha * 0.5f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, groundFogColor),
                        startY = size.height * 0.6f,
                        endY = size.height
                    ),
                    size = size
                )
            }
        }

        // Ground terrain with mountains (drawn behind fog for depth)
        drawGround(weatherCode = 45, isDay = true, isDark = isDark, compact = compact)

        // Rime fog (code 48): add ice crystal sparkles
        if (weatherCode == 48) {
            val sparkleCount = if (compact) 8 else 20
            for (i in 0 until sparkleCount) {
                val sx = (sin(iceSparkle * 2.5f + i * 1.7f + fogOffset * 3f) * 0.5f + 0.5f) * size.width
                val sy = (cos(iceSparkle * 1.8f + i * 2.3f + fogOffset * 2f) * 0.5f + 0.5f) * size.height * 0.7f
                val sparkleAlpha = (sin(iceSparkle * 3f + i * 1.3f) * 0.5f + 0.5f) * 0.6f
                val sparkleSize = 1f + sparkleAlpha * 2f
                // Ice crystal sparkle
                drawCircle(
                    color = Color(0xFFE0F7FA).copy(alpha = sparkleAlpha),
                    radius = sparkleSize,
                    center = Offset(sx, sy)
                )
                // Cross glow for larger sparkles
                if (sparkleAlpha > 0.4f) {
                    drawLine(
                        color = Color(0xFFE0F7FA).copy(alpha = sparkleAlpha * 0.3f),
                        start = Offset(sx - sparkleSize * 2f, sy),
                        end = Offset(sx + sparkleSize * 2f, sy),
                        strokeWidth = 0.6f
                    )
                    drawLine(
                        color = Color(0xFFE0F7FA).copy(alpha = sparkleAlpha * 0.3f),
                        start = Offset(sx, sy - sparkleSize * 2f),
                        end = Offset(sx, sy + sparkleSize * 2f),
                        strokeWidth = 0.6f
                    )
                }
            }

            // Extra white ground fog for rime
            val rimeGroundFog = Color(0xFFE8ECF0).copy(alpha = fogAlpha * 0.25f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, rimeGroundFog),
                    startY = size.height * 0.5f,
                    endY = size.height
                ),
                size = size
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Rain — Falling streaks + ground ripples
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RainScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val isHeavy = weatherCode >= 65 || weatherCode in 80..82
    val isDrizzle = weatherCode in 51..57
    // Drizzle: more but smaller drops; heavy: big fat streaks
    val streakCount = when {
        compact -> if (isHeavy) 30 else if (isDrizzle) 25 else 18
        else -> if (isHeavy) 75 else if (isDrizzle) 55 else 45
    }
    val baseSpeed = if (isHeavy) 600f else if (isDrizzle) 1200f else 1000f
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(baseSpeed.toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "rainFall"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleAlpha"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleScale"
    )
    // Wind gust effect — now with intensity variation
    // Subtle wind — reduced tilt for more natural vertical fall
    val windGust by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "windGust"
    )
    // Rain intensity pulsing for living feel
    val rainIntensity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000 + Random.nextInt(2000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rainIntensity"
    )

    val rainColor = if (isDark)
        Color(0xFF81D4FA).copy(alpha = if (isHeavy) 0.55f else if (isDrizzle) 0.45f else 0.40f)
    else
        Color(0xFF4A90D9).copy(alpha = if (isHeavy) 0.60f else if (isDrizzle) 0.50f else 0.45f)
    val streaks = rememberRainStreaks(streakCount, isDrizzle)

    Canvas(modifier = modifier.fillMaxSize()) {
        val intensityAlpha = rainIntensity.coerceIn(0.4f, 1f)

        // Rain streaks — each drop falls individually with random phase offset (no synchronized lines)
        streaks.forEach { streak ->
            val x = streak.x
            val speed = streak.speed
            val length = streak.length
            val delay = streak.delay
            // Subtle wind sway — minimal tilt for natural near-vertical fall
            val windSway = windGust * 10f * intensityAlpha
            // Per-drop phase offset — each drop has unique timing, no two fall together
            val fallProgress = ((rainProgress + delay) % 1f).let { if (it < 0f) it + 1f else it }
            val y = (fallProgress * size.height * speed * intensityAlpha) % (size.height + length)
            val yEnd = y - length * intensityAlpha.coerceIn(0.8f, 1.2f)
            val xPos = x * size.width + windSway * (1f - y / size.height) * 0.3f
            val streakAlpha = (0.4f + intensityAlpha * 0.5f) * (0.5f + speed * 0.4f)
            drawLine(
                color = rainColor.copy(alpha = streakAlpha.coerceAtMost(1f)),
                start = Offset(xPos, y),
                end = Offset(xPos + windSway * 0.2f, yEnd),
                strokeWidth = if (isHeavy) 2f * intensityAlpha else if (isDrizzle) 0.8f * intensityAlpha else 1.4f * intensityAlpha
            )
        }

        // Rain ripples on ground + splash particles
        if (!compact) {
            val rippleCount = if (isHeavy) 8 else 5
            for (i in 0 until rippleCount) {
                val rippleX = size.width * (0.05f + i.toFloat() / rippleCount * 0.9f) + sin(i * 1.3f) * size.width * 0.04f
                val rippleY = size.height * 0.92f + sin(i * 2.1f) * size.height * 0.02f
                // Expanding ring
                drawCircle(
                    color = rainColor.copy(alpha = rippleAlpha * 0.8f),
                    radius = 4f + rippleScale * 20f,
                    center = Offset(rippleX, rippleY),
                    style = Stroke(width = 1.2f + rippleScale * 0.8f)
                )
                // Splash crown — multiple droplets ejecting outward
                val crownCount = 5
                for (c in 0 until crownCount) {
                    val angle = c * 72f + rainProgress * 180f
                    val rad = angle * PI.toFloat() / 180f
                    val dist = 3f + rippleScale * 15f * (0.5f + c * 0.1f)
                    val crownAlpha = rippleAlpha * (0.6f - c * 0.1f)
                    drawCircle(
                        color = rainColor.copy(alpha = crownAlpha.coerceIn(0.1f, 0.6f)),
                        radius = 1f + rippleScale * 1.5f * (1f - c * 0.1f),
                        center = Offset(
                            rippleX + cos(rad) * dist,
                            rippleY - abs(sin(rad)) * dist * 0.4f
                        )
                    )
                }
            }
        }

        // Ground terrain
        drawGround(weatherCode = weatherCode, isDay = true, isDark = isDark, compact = compact)
    }
}

private data class RainStreak(val x: Float, val speed: Float, val length: Float, val delay: Float)

private fun rememberRainStreaks(count: Int, isDrizzle: Boolean = false): List<RainStreak> {
    val seed = 123L
    val rng = Random(seed)
    return List(count) {
        RainStreak(
            x = rng.nextFloat(),
            speed = if (isDrizzle) 0.3f + rng.nextFloat() * 0.5f else 0.5f + rng.nextFloat(),
            length = if (isDrizzle) 4f + rng.nextFloat() * 8f else 8f + rng.nextFloat() * 15f,
            delay = rng.nextFloat() * 0.9f  // Random phase offset for continuous random drops
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Snow — Drifting snowflakes with gentle sway
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SnowScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val isHeavy = weatherCode >= 75 || weatherCode == 86
    val flakeCount = if (compact) (if (isHeavy) 30 else 15) else (if (isHeavy) 70 else 40)

    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowFall"
    )
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "snowSway"
    )
    val sparkleGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "sparkleGlow"
    )
    // Flake size oscillation for tumbling effect
    val sizeOscillation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "flakeTumble"
    )

    val flakes = rememberSnowflakes(flakeCount)
    val snowColor = Color.White.copy(alpha = if (isHeavy) 0.75f else 0.55f)
    val sparkleColor = Color(0xFFE0F7FA)

    Canvas(modifier = modifier.fillMaxSize()) {
        flakes.forEach { (x, speed, baseSize) ->
            val y = (snowProgress * size.height * speed + x * size.height * 0.1f) % (size.height + baseSize * 4f)
            // Gentle figure-8 sway with more natural randomness
            val swayX = sin(swayOffset * 2.5f * PI.toFloat() + x * size.width * 0.006f) * 18f * speed
            val swayX2 = cos(swayOffset * 1.7f * PI.toFloat() + x * size.width * 0.01f) * 7f * speed
            val adjustedX = (x * size.width + swayX + swayX2).coerceIn(0f, size.width)

            // Size oscillation — flakes appear to tumble and rotate
            val sizeMod = 1.0f + sin(sizeOscillation * 3f * PI.toFloat() + x * 20f + y * 0.01f) * 0.3f
            val adjustedSize = baseSize * sizeMod.coerceIn(0.7f, 1.3f)

            // Draw snowflake with slight transparency tumbling effect
            val flakeAlpha = (0.7f + sin(sizeOscillation * 2f + x * 10f) * 0.3f).coerceIn(0.4f, 1f)
            drawCircle(
                color = snowColor.copy(alpha = flakeAlpha),
                radius = adjustedSize,
                center = Offset(adjustedX, y)
            )

            // Sparkle highlight on larger flakes
            if (baseSize > 3f) {
                val sparkleAlpha = (sin(sparkleGlow * 3.5f + x * 15f + y * 10f) * 0.5f + 0.5f) * 0.8f
                // Sparkle slightly offset (like a light catching the flake edge)
                drawCircle(
                    color = sparkleColor.copy(alpha = sparkleAlpha * 0.5f),
                    radius = adjustedSize * 0.35f,
                    center = Offset(adjustedX - adjustedSize * 0.3f, y - adjustedSize * 0.3f)
                )
            }
        }

        // Ground terrain with snow cover
        drawGround(weatherCode = weatherCode, isDay = true, isDark = isDark, compact = compact)

        // Occasional sparkling particle floating in the air
        if (!compact && isHeavy) {
            for (i in 0..3) {
                val sx = (sin(sparkleGlow * 2f + i * 1.7f + snowProgress * 3f) * 0.5f + 0.5f) * size.width
                val sy = (cos(sparkleGlow * 1.5f + i * 2.3f + snowProgress * 2f) * 0.5f + 0.5f) * size.height * 0.5f
                drawCircle(
                    color = sparkleColor.copy(alpha = sparkleGlow * 0.25f),
                    radius = 1f + sparkleGlow * 2f,
                    center = Offset(sx, sy)
                )
            }
        }
    }
}

private fun rememberSnowflakes(count: Int): List<Triple<Float, Float, Float>> {
    val seed = 456L
    val rng = Random(seed)
    return List(count) {
        Triple(rng.nextFloat(), 0.4f + rng.nextFloat() * 0.8f, 2f + rng.nextFloat() * 5f)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Thunderstorm — Rain + random lightning flashes + screen-edge glow
//  Uses unpredictable timing (2-6s apart) and random bolt positions
//  to avoid the constant-flashing headache of a fixed animation.
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ThunderstormScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark

    // ── Thunder rumble visual state ──
    var rumbleAlpha by remember { mutableStateOf(0f) }
    var rumblePosition by remember { mutableStateOf(0f) }

    // Includes heavy rain effect
    RainScene(weatherCode = 65, palette = palette, compact = compact, timeOfDay = timeOfDay, modifier = modifier)

    // ── Lightning flash state (random timing, intensity, position) ──
    var flashAlpha by remember { mutableStateOf(0f) }
    val flashPosition = remember { mutableStateOf(Offset(0.3f, 0.1f)) }
    var flashIntensity by remember { mutableStateOf(0.3f) }
    
    var boltSeed by remember { mutableStateOf(0) }

    val rng = remember { Random(789) }

    // LaunchedEffect that triggers lightning flashes at random intervals
    LaunchedEffect(Unit) {
        while (true) {
            val nextFlashDelay = rng.nextLong(2000L, 6000L)
            delay(nextFlashDelay)

            flashPosition.value = Offset(
                0.1f + rng.nextFloat() * 0.8f,
                0.05f + rng.nextFloat() * 0.4f
            )
            flashIntensity = 0.3f + rng.nextFloat() * 0.6f
            boltSeed++

            // Thunder rumble precedes flash
            rumblePosition = flashPosition.value.x
            rumbleAlpha = 0.15f
            delay(50)
            for (step in 1..4) {
                rumbleAlpha = 0.15f * (1f - step.toFloat() / 4f)
                delay(40)
            }
            rumbleAlpha = 0f
            
            // Quick flash on
            flashAlpha = 1f
            val flashOnDuration = rng.nextLong(80L, 150L)
            delay(flashOnDuration)

            // Quick fade out
            val fadeSteps = rng.nextInt(3, 6)
            for (step in 1..fadeSteps) {
                flashAlpha = 1f - (step.toFloat() / fadeSteps)
                delay(rng.nextLong(20L, 40L))
            }
            flashAlpha = 0f

            // After-flash glow (warm lingering light)
            if (flashIntensity > 0.5f) {
                // Glow already handled in Canvas
            }

            // Sometimes do a double-flash
            if (rng.nextFloat() < 0.35f) {
                delay(rng.nextLong(200L, 500L))
                flashPosition.value = Offset(
                    flashPosition.value.x + (rng.nextFloat() - 0.5f) * 0.2f,
                    flashPosition.value.y
                )
                boltSeed++
                flashAlpha = 0.7f + rng.nextFloat() * 0.3f
                delay(rng.nextLong(60L, 120L))

                val fadeSteps2 = rng.nextInt(2, 4)
                for (step in 1..fadeSteps2) {
                    flashAlpha = 0.7f + rng.nextFloat() * 0.3f - (step.toFloat() / fadeSteps2)
                    delay(rng.nextLong(20L, 40L))
                }
                flashAlpha = 0f
            }
        }
    }

    // ── Glow afterglow state ──
    var glowAlpha by remember { mutableStateOf(0f) }
    val glowPosition = remember { mutableStateOf(Offset(0.3f, 0.1f)) }

    LaunchedEffect(flashAlpha) {
        if (flashAlpha > 0.5f) {
            glowPosition.value = flashPosition.value
            glowAlpha = 0.4f
            delay(400)
            for (step in 1..8) {
                glowAlpha = 0.4f * (1f - step.toFloat() / 8f)
                delay(50)
            }
            glowAlpha = 0f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Lightning bolt ──
        if (flashAlpha > 0.3f) {
            val drawRng = Random(boltSeed)
            val boltX = flashPosition.value.x * size.width
            val startY = flashPosition.value.y * size.height
            val endY = size.height * 0.55f + drawRng.nextFloat() * size.height * 0.25f

            // Glow aura around bolt
            drawPath(
                path = Path().apply {
                    moveTo(boltX, 0f)
                    val segments = 6 + drawRng.nextInt(4)
                    var cx = boltX
                    for (i in 0 until segments) {
                        val zx = cx + (drawRng.nextFloat() - 0.5f) * size.width * 0.08f
                        val zy = startY + (endY - startY) * ((i + 1f) / segments) + (drawRng.nextFloat() - 0.5f) * size.height * 0.03f
                        lineTo(zx, zy)
                        cx = zx
                    }
                    lineTo(boltX + (drawRng.nextFloat() - 0.5f) * size.width * 0.05f, endY)
                },
                color = Color(0xFFB3E5FC).copy(alpha = flashAlpha * 0.3f * flashIntensity),
                style = Stroke(width = 8f + flashIntensity * 10f)
            )

            // Primary bolt (thin bright core)
            val boltPath = Path()
            boltPath.moveTo(boltX, 0f)
            var currentX = boltX
            val segments = 7 + drawRng.nextInt(4)
            for (i in 0 until segments) {
                val zigzagX = currentX + (drawRng.nextFloat() - 0.5f) * size.width * 0.08f
                val zigzagY = startY + (endY - startY) * ((i + 1f) / segments) + (drawRng.nextFloat() - 0.5f) * size.height * 0.03f
                boltPath.lineTo(zigzagX, zigzagY)
                currentX = zigzagX
            }
            boltPath.lineTo(boltX + (drawRng.nextFloat() - 0.5f) * size.width * 0.05f, endY)

            drawPath(
                path = boltPath,
                color = Color.White.copy(alpha = flashAlpha * 0.95f),
                style = Stroke(width = 1.5f + flashIntensity * 4f)
            )
            // Bolt glow core
            drawPath(
                path = boltPath,
                color = Color.White.copy(alpha = flashAlpha * 0.4f),
                style = Stroke(width = 4f + flashIntensity * 6f)
            )

            // Secondary branches
            val branchCount = 2 + drawRng.nextInt(3)
            for (b in 0 until branchCount) {
                val branchPath = Path()
                val branchStart = startY + (endY - startY) * drawRng.nextFloat()
                val branchX = boltX + (drawRng.nextFloat() - 0.5f) * size.width * 0.15f
                branchPath.moveTo(boltX, branchStart)
                branchPath.lineTo(branchX, branchStart + drawRng.nextFloat() * size.height * 0.04f)
                branchPath.lineTo(branchX + (drawRng.nextFloat() - 0.5f) * size.width * 0.04f, branchStart + size.height * 0.05f)
                drawPath(
                    path = branchPath,
                    color = Color.White.copy(alpha = flashAlpha * 0.5f),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // ── Thunder rumble (horizontal screen shake lines at flash position) ──
        if (rumbleAlpha > 0.01f) {
            val rx = rumblePosition * size.width
            for (i in 0..2) {
                val ry = size.height * (0.1f + i * 0.3f) + (sin(rumbleAlpha * 20f + i) * 5f)
                drawLine(
                    color = Color.White.copy(alpha = rumbleAlpha * 0.3f),
                    start = Offset(rx - size.width * 0.1f, ry),
                    end = Offset(rx + size.width * 0.1f, ry),
                    strokeWidth = 1.5f
                )
            }
        }

        // ── Full-screen ambient flash ──
        if (flashAlpha > 0.3f) {
            drawRect(
                color = Color.White.copy(alpha = flashAlpha * 0.30f * flashIntensity),
                size = size
            )
        }

        // ── Storm darkness overlay (light mode) ──
        // In light mode, the sky is too bright for a thunderstorm.
        // This overlay darkens the scene to create a stormy atmosphere.
        if (!isDark) {
            drawRect(
                color = Color(0xFF0A1620).copy(alpha = 0.25f),
                size = size
            )
        }

        // ── Screen-edge glow at lightning position ──
        if (glowAlpha > 0.01f) {
            val glowX = glowPosition.value.x * size.width
            val glowY = glowPosition.value.y * size.height
            val glowColor = if (isDark) Color(0xFFFFF9C4).copy(alpha = glowAlpha * 0.8f) else Color(0xFFFFF9C4).copy(alpha = glowAlpha)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, glowColor.copy(alpha = 0f), Color.Transparent),
                    center = Offset(glowX, glowY),
                    radius = size.maxDimension * 0.7f
                ),
                size = size
            )
        }

        // Ground terrain already rendered by RainScene composable above
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Utility: weather code to description mapping for detail screen
// ══════════════════════════════════════════════════════════════════════

/**
 * Resolve a WMO weather code to its description.
 * Used by CompactWeatherIcon and detail screen components.
 */
fun weatherCodeToDescription(code: Int): String = WeatherSnapshot.descriptionForCode(code)

// ══════════════════════════════════════════════════════════════════════
//  Static frame for previews/exports
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StaticWeatherFrame(
    weatherCode: Int,
    palette: WeatherPalette,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw background
        drawRect(
            brush = Brush.verticalGradient(palette.background),
            size = size
        )

        // Draw ground
        drawGround(weatherCode = weatherCode, isDay = true, isDark = isDark, compact = false)

        // Draw a static weather icon
        val cx = size.width / 2
        val cy = size.height * 0.35f
        val r = size.minDimension * 0.12f

        when {
            weatherCode <= 1 -> {
                // Static sun
                drawCircle(color = palette.accent, radius = r, center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = r * 0.6f, center = Offset(cx, cy))
            }
            weatherCode in 2..3 -> {
                // Static cloud
                val cloudColor = if (isDark) Color(0xFF90A4AE).copy(alpha = 0.45f) else Color(0xFFB0BEC5).copy(alpha = 0.40f)
                drawCloud(0f, cx - r, cy, r * 2f, cloudColor)
            }
            weatherCode in 71..77 || weatherCode in 85..86 -> {
                // Snow icon
                for (i in 0..5) {
                    val sx = cx + sin(i * 1.05f) * r * 0.6f
                    val sy = cy + cos(i * 1.05f) * r * 0.6f
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = r * 0.15f, center = Offset(sx, sy))
                }
            }
            else -> {
                // Generic weather icon
                drawCircle(color = palette.accent.copy(alpha = 0.5f), radius = r, center = Offset(cx, cy))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Compact animated weather icon for detail screen chips
// ══════════════════════════════════════════════════════════════════════

@Composable
fun CompactWeatherIcon(
    weatherCode: Int,
    temperature: Double?,
    modifier: Modifier = Modifier
) {
    AnimatedWeatherScene(
        weatherCode = weatherCode,
        temperature = temperature,
        modifier = modifier,
        compact = true
    )
}
