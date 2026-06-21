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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
    forceNight: Boolean? = null,
    showCloudAnimation: Boolean = true
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

    // In preview/inspection mode, show a static frame
    if (LocalInspectionMode.current) {
        StaticWeatherFrame(weatherCode, palette, modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background gradient — rich multi-stop with atmospheric warm band near horizon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bgColors = when {
                palette.background.size >= 3 -> palette.background
                palette.background.size == 2 -> listOf(palette.background[0], palette.tertiary, palette.background[1])
                else -> listOf(palette.primary, palette.tertiary, palette.secondary)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = bgColors,
                    startY = 0f,
                    endY = size.height
                ),
                size = size
            )
            // Subtle accent glow near horizon for added depth
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.75f),
                    radius = size.maxDimension * 0.5f
                ),
                size = size
            )
        }

        // Specific weather effects
        // weatherCode -1 = day cloudy, -2 = night sky (used by weather widget for enhanced display)
        // All scenes use the same animation style regardless of day/night — palette provides the colors
        // Cloudy conditions (code 2-3) now use DayCloudyScene/NightCloudyScene with high cloudIntensity
        // instead of the separate CloudyScene, so the time-of-day background, birds, aurora, ground
        // terrain, and atmospheric effects are preserved with clouds layered on top.
        val isDaytime = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight
        val isTransitionTime = timeOfDay == TimeOfDay.Dawn || timeOfDay == TimeOfDay.Twilight
        // Use EveningScene during Dawn and Twilight for clear/cloudy conditions
        // Weather-specific scenes (rain, snow, fog, thunder) still take priority
        val useEveningScene = isTransitionTime && (
            weatherCode == -1 || weatherCode == -2 || weatherCode in 0..3
        )
        when {
            useEveningScene -> EveningScene(palette, compact, timeOfDay, modifier)
            weatherCode == -1 || weatherCode in 0..1 -> {
                if (isDaytime) {
                    if (showCloudAnimation) DayCloudyScene(palette, compact, timeOfDay, modifier, cloudIntensity = 0.25f)
                    else ClearSkyScene(palette, timeOfDay, compact, modifier)
                } else {
                    ClearSkyScene(palette, timeOfDay, compact, modifier)
                }
            }
            weatherCode == -2 -> {
                if (showCloudAnimation) NightCloudyScene(palette, compact, timeOfDay, modifier, cloudIntensity = 0.25f)
                else NightSkyScene(palette, compact, timeOfDay, modifier)
            }
            weatherCode in 2..3 -> {
                if (showCloudAnimation) {
                    if (isDaytime) DayCloudyScene(palette, compact, timeOfDay, modifier, cloudIntensity = 0.85f)
                    else NightCloudyScene(palette, compact, timeOfDay, modifier, cloudIntensity = 0.85f)
                } else ClearSkyScene(palette, timeOfDay, compact, modifier)
            }
            weatherCode in 45..48 -> FogScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode in 51..67 || weatherCode in 80..82 -> RainScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode in 71..77 || weatherCode in 85..86 -> SnowScene(weatherCode, palette, compact, timeOfDay, modifier)
            weatherCode >= 95 -> ThunderstormScene(weatherCode, palette, compact, timeOfDay, modifier)
            else -> ClearSkyScene(palette, timeOfDay, compact, modifier)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Static preview frame
// ══════════════════════════════════════════════════════════════════════

/**
 * Static weather frame used in preview/inspection mode.
 * Renders a simplified weather scene without animations.
 */
@Composable
private fun StaticWeatherFrame(
    weatherCode: Int,
    palette: WeatherPalette,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(palette.background),
            size = size
        )
        drawGround(palette, weatherCode = weatherCode, isDay = true, isDark = isDark, compact = false)
        val cx = size.width / 2
        val cy = size.height * 0.35f
        val r = size.minDimension * 0.12f
        when {
            weatherCode <= 1 -> {
                drawCircle(color = palette.accent, radius = r, center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = r * 0.6f, center = Offset(cx, cy))
            }
            weatherCode in 2..3 -> {
                val cloudColor = if (isDark) Color(0xFF90A4AE).copy(alpha = 0.45f) else Color(0xFFB0BEC5).copy(alpha = 0.40f)
                drawCloud(0f, cx - r, cy, r * 2f, cloudColor)
            }
            weatherCode in 71..77 || weatherCode in 85..86 -> {
                for (i in 0..5) {
                    val sx = cx + sin(i * 1.05f) * r * 0.6f
                    val sy = cy + cos(i * 1.05f) * r * 0.6f
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = r * 0.15f, center = Offset(sx, sy))
                }
            }
            else -> {
                drawCircle(color = palette.accent.copy(alpha = 0.5f), radius = r, center = Offset(cx, cy))
            }
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
 * Rich, saturated palettes chosen for atmospheric depth at each time of day.
 */
private data class TimeOfDayColors(
    val skyTop: Color,          // Highest point of sky — darkest/saturated
    val skyMid: Color,          // Mid-sky — dominant hue
    val skyBottom: Color,       // Near horizon — warm/light
    val skyAccent: Color,       // Transition band color
    val sunCol: Color,          // Sun body
    val sunGlowCol: Color,      // Sun glow halo
    val sunLensFlare: Color,    // Lens flare tint
    val moonCol: Color,
    val moonGlowCol: Color,
    val cloudCol: Color,
    val hazeCol: Color,         // Atmospheric haze / dust
    val mountainFar: Color,     // Far mountain layer tint (atmospheric blue shift)
    val mountainMid: Color,     // Mid mountain layer tint
    val mountainNear: Color,    // Near mountain layer tint (warm/dark)
    val groundCol: Color,       // Ground terrain tint
    val groundDetail: Color     // Ground detail (trees, grass)
)

data class WeatherPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val background: List<Color>,
    val sunColor: Color = Color(0xFFFFF176),
    val sunGlowColor: Color = Color(0xFFFFF9C4),
    val sunFlareColor: Color = Color(0xFFFFAB91),
    val moonColor: Color = Color(0xFFECEFF1),
    val moonGlowColor: Color = Color(0xFFE3F2FD),
    val cloudBaseColor: Color = Color.White,
    val hazeColor: Color = Color.Transparent,
    val mountainFarColor: Color = Color(0xFF90A8C4),
    val mountainMidColor: Color = Color(0xFF6A8A9A),
    val mountainNearColor: Color = Color(0xFF4A6A5A),
    val groundColor: Color = Color(0xFF4A7A4A),
    val groundDetailColor: Color = Color(0xFF4A6A4A)
)

private fun weatherPalette(temp: Double?, timeOfDay: TimeOfDay, isDarkTheme: Boolean): WeatherPalette {
    val tempC = temp ?: 20.0

    // ── Time-of-day sky colors — rich multi-stop gradients for atmospheric depth ──
    // Each palette is designed with: dark/saturated top → mid hue → warm light near horizon
    // Mountain and ground colors shift with atmospheric perspective (cool far → warm near)
    val timeColors = when (timeOfDay) {
        TimeOfDay.Dawn -> TimeOfDayColors(
            skyTop = Color(0xFF5B3E8A),          // Deep lavender
            skyMid = Color(0xFFD48BAA),           // Soft pink-mauve
            skyBottom = Color(0xFFFFD6A8),        // Warm peach-gold horizon
            skyAccent = Color(0xFFE8A0B4),        // Rose accent
            sunCol = Color(0xFFFFDAB0),           // Warm peach sun
            sunGlowCol = Color(0xFFFFE8C0),       // Soft gold glow
            sunLensFlare = Color(0xFFFFAA88),     // Pink-orange flare
            moonCol = Color(0xFFC8B8D8),
            moonGlowCol = Color(0xFFD0C0E0),
            cloudCol = Color(0xFFF0E0D0),
            hazeCol = Color(0xFFFFE0C0),
            mountainFar = Color(0xFFA090C8),      // Cool violet-blue (far, hazy)
            mountainMid = Color(0xFF8A7098),       // Muted mauve (mid)
            mountainNear = Color(0xFF5A4A5A),      // Warm dark (near)
            groundCol = Color(0xFF6A5A4A),         // Warm earth
            groundDetail = Color(0xFF4A3A3A)       // Dark detail
        )
        TimeOfDay.Sunrise -> TimeOfDayColors(
            skyTop = Color(0xFFFF6A30),           // Deep orange-red
            skyMid = Color(0xFFFFAA50),            // Golden orange
            skyBottom = Color(0xFFFFDD70),         // Bright gold horizon
            skyAccent = Color(0xFFFF8A60),         // Coral accent
            sunCol = Color(0xFFFFF8C8),            // Warm bright sun
            sunGlowCol = Color(0xFFFFE888),        // Golden glow
            sunLensFlare = Color(0xFFFF8833),      // Orange flare
            moonCol = Color(0xFFE0C8A0),
            moonGlowCol = Color(0xFFFFE0B0),
            cloudCol = Color(0xFFFFE0C0),
            hazeCol = Color(0xFFFFE0A0),
            mountainFar = Color(0xFFD0A0A0),       // Pink-washed far (atmospheric scattering)
            mountainMid = Color(0xFFA07070),        // Warm brown mid
            mountainNear = Color(0xFF5A3A2A),       // Dark warm near
            groundCol = Color(0xFF6A4A2A),
            groundDetail = Color(0xFF4A2A1A)
        )
        TimeOfDay.Morning -> TimeOfDayColors(
            skyTop = Color(0xFF6AB0E8),           // Rich cerulean
            skyMid = Color(0xFF8AC8EE),            // Soft sky blue
            skyBottom = Color(0xFFC0E8F0),         // Pale blue horizon
            skyAccent = Color(0xFF64B5F6),         // Blue accent
            sunCol = Color(0xFFFFF9C4),            // Pale warm sun
            sunGlowCol = Color(0xFFFFFDE8),        // White-gold glow
            sunLensFlare = Color(0xFFFFDD88),      // Warm lens flare
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color(0xFFFFF8E1),
            hazeCol = Color(0xFFE8F4FA),
            mountainFar = Color(0xFFA0C0E0),       // Blue-grey far (strong atmospheric scattering)
            mountainMid = Color(0xFF6A8A9A),        // Muted teal mid
            mountainNear = Color(0xFF2A4A3A),       // Dark green near
            groundCol = Color(0xFF3A6A3A),
            groundDetail = Color(0xFF1A3A1A)
        )
        TimeOfDay.Midday -> TimeOfDayColors(
            skyTop = Color(0xFF1A6AC8),            // Deep vibrant blue
            skyMid = Color(0xFF4A9AEA),             // Bright mid-blue
            skyBottom = Color(0xFF8AC8F0),         // Pale sky horizon
            skyAccent = Color(0xFF42A5F5),         // Blue accent
            sunCol = Color(0xFFFFF9C4),            // Bright white-gold
            sunGlowCol = Color(0xFFFFFDE8),        // White glow
            sunLensFlare = Color(0xFFFFDD88),      // Warm flare
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color.White,
            hazeCol = Color(0xFFD6ECFA),
            mountainFar = Color(0xFF8AB0D8),       // Strong blue shift (Rayleigh scattering)
            mountainMid = Color(0xFF4A7A8A),        // Muted blue-green mid
            mountainNear = Color(0xFF1A3A2A),       // Dark green near
            groundCol = Color(0xFF2A5A2A),
            groundDetail = Color(0xFF0A2A1A)
        )
        TimeOfDay.Afternoon -> TimeOfDayColors(
            skyTop = Color(0xFF3A80C8),            // Warm blue
            skyMid = Color(0xFF6AAAD8),             // Soft warm blue
            skyBottom = Color(0xFFD6D8B0),         // Golden-pale horizon
            skyAccent = Color(0xFFFFB74D),          // Warm accent
            sunCol = Color(0xFFFFF9C4),
            sunGlowCol = Color(0xFFFFFDE8),
            sunLensFlare = Color(0xFFFFDDA0),
            moonCol = Color(0xFFD0D8E0),
            moonGlowCol = Color(0xFFE0E8F0),
            cloudCol = Color(0xFFFFF0D0),
            hazeCol = Color(0xFFF0E8D0),
            mountainFar = Color(0xFF90A8B8),       // Blue-grey with warm tint
            mountainMid = Color(0xFF5A7A7A),        // Warm grey-green mid
            mountainNear = Color(0xFF2A4A3A),       // Dark green near
            groundCol = Color(0xFF3A5A3A),
            groundDetail = Color(0xFF1A3A1A)
        )
        TimeOfDay.Sunset -> TimeOfDayColors(
            skyTop = Color(0xFFFF4A20),            // Deep fiery orange
            skyMid = Color(0xFFE83888),             // Magenta-pink
            skyBottom = Color(0xFF8822AA),          // Purple horizon
            skyAccent = Color(0xFFFF5252),          // Red accent
            sunCol = Color(0xFFFFE880),             // Warm golden sun
            sunGlowCol = Color(0xFFFFAA44),         // Orange glow
            sunLensFlare = Color(0xFFFF4400),       // Red-orange flare
            moonCol = Color(0xFFC8A0C0),
            moonGlowCol = Color(0xFFE0B0D0),
            cloudCol = Color(0xFFE0A090),
            hazeCol = Color(0xFFE08070),
            mountainFar = Color(0xFFC08090),        // Pink-purple far (atmosphere reflects sunset)
            mountainMid = Color(0xFF8A5060),        // Muted crimson mid
            mountainNear = Color(0xFF3A1A2A),       // Deep purple-black near
            groundCol = Color(0xFF4A2A2A),
            groundDetail = Color(0xFF2A1A1A)
        )
        TimeOfDay.Twilight -> TimeOfDayColors(
            skyTop = Color(0xFF0D0D3A),            // Deep midnight purple
            skyMid = Color(0xFF2A1878),             // Rich indigo
            skyBottom = Color(0xFF6A1A8E),          // Deep purple horizon glow
            skyAccent = Color(0xFF7C4DFF),          // Violet accent
            sunCol = Color(0xFFFFCC88),             // Fading warm sun
            sunGlowCol = Color(0xFFFF8855),         // Ember glow
            sunLensFlare = Color(0xFFCC5500),       // Deep orange
            moonCol = Color(0xFFC8D0E0),
            moonGlowCol = Color(0xFF90A0C0),
            cloudCol = Color(0xFF4A5878),
            hazeCol = Color(0xFF3A2860),
            mountainFar = Color(0xFF283868),       // Deep blue (almost silhouette)
            mountainMid = Color(0xFF1A2848),        // Very dark blue
            mountainNear = Color(0xFF0A0A1A),       // Near-black
            groundCol = Color(0xFF0A1018),
            groundDetail = Color(0xFF05080C)
        )
        TimeOfDay.Night -> TimeOfDayColors(
            skyTop = Color(0xFF040418),            // Near-black blue
            skyMid = Color(0xFF0A0A30),             // Deep midnight blue
            skyBottom = Color(0xFF1A1A48),          // Slightly lighter at horizon
            skyAccent = Color(0xFF4A5AC0),          // Subtle blue accent
            sunCol = Color(0xFFD8DCE0),             // No sun - very faint
            sunGlowCol = Color(0xFFA0A8B0),         // Subtle warm grey
            sunLensFlare = Color(0xFF606870),       // Very subtle
            moonCol = Color(0xFFE8ECF0),
            moonGlowCol = Color(0xFFA0B8E8),
            cloudCol = Color(0xFF283048),
            hazeCol = Color(0xFF0A0A28),
            mountainFar = Color(0xFF1A1A38),       // Very dark blue silhouette
            mountainMid = Color(0xFF0E0E24),        // Near-black
            mountainNear = Color(0xFF06060E),       // Almost pure silhouette
            groundCol = Color(0xFF04080C),
            groundDetail = Color(0xFF020406)
        )
    }
    val skyTop = timeColors.skyTop
    val skyMid = timeColors.skyMid
    val skyBottom = timeColors.skyBottom
    val skyAccent = timeColors.skyAccent
    val sunCol = timeColors.sunCol
    val sunGlowCol = timeColors.sunGlowCol
    val sunLensFlare = timeColors.sunLensFlare
    val moonCol = timeColors.moonCol
    val moonGlowCol = timeColors.moonGlowCol
    val cloudCol = timeColors.cloudCol
    val hazeCol = timeColors.hazeCol
    val mountainFar = timeColors.mountainFar
    val mountainMid = timeColors.mountainMid
    val mountainNear = timeColors.mountainNear
    val groundCol = timeColors.groundCol
    val groundDetail = timeColors.groundDetail

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

    val isNighttime = timeOfDay == TimeOfDay.Night || timeOfDay == TimeOfDay.Twilight

    if (isDarkTheme) {
        // Dark mode: rich, vibrant dark colors — preserve the same beautiful look as light mode
        // but with deeper, more saturated tones instead of heavy desaturation
        val dayTopMul = 0.72f
        val dayTopMulG = 0.68f
        val dayTopMulB = 0.78f
        val dayBotMul = 0.65f
        val dayBotMulG = 0.58f
        val dayBotMulB = 0.70f

        val nightTopMul = 0.52f
        val nightTopMulG = 0.48f
        val nightTopMulB = 0.58f
        val nightBotMul = 0.42f
        val nightBotMulG = 0.38f
        val nightBotMulB = 0.48f

        val topMulR = if (isNighttime) nightTopMul else dayTopMul
        val topMulG = if (isNighttime) nightTopMulG else dayTopMulG
        val topMulB = if (isNighttime) nightTopMulB else dayTopMulB
        val botMulR = if (isNighttime) nightBotMul else dayBotMul
        val botMulG = if (isNighttime) nightBotMulG else dayBotMulG
        val botMulB = if (isNighttime) nightBotMulB else dayBotMulB

        val darkTop = modulatedTop.copy(
            red = (modulatedTop.red * topMulR).coerceAtMost(0.75f),
            green = (modulatedTop.green * topMulG).coerceAtMost(0.70f),
            blue = (modulatedTop.blue * topMulB).coerceAtMost(0.80f)
        )
        val darkBottom = skyBottom.copy(
            red = (skyBottom.red * botMulR).coerceAtMost(0.65f),
            green = (skyBottom.green * botMulG).coerceAtMost(0.60f),
            blue = (skyBottom.blue * botMulB).coerceAtMost(0.70f)
        )
        val bgColors = when (timeOfDay) {
            TimeOfDay.Night, TimeOfDay.Twilight -> listOf(
                Color(0xFF080818),
                Color(0xFF121235),
                darkBottom.copy(alpha = 0.88f)
            )
            TimeOfDay.Sunrise, TimeOfDay.Morning, TimeOfDay.Midday, TimeOfDay.Afternoon -> listOf(
                darkTop.copy(alpha = 0.92f),
                darkBottom.copy(alpha = 0.88f)
            )
            TimeOfDay.Sunset, TimeOfDay.Dawn -> listOf(
                darkTop,
                darkBottom.copy(red = darkBottom.red * 1.15f, green = darkBottom.green * 0.85f, blue = darkBottom.blue * 1.2f)
            )
        }

        val cloudAlpha = if (isNighttime) 0.28f else 0.48f

        return WeatherPalette(
            primary = darkTop,
            secondary = darkBottom,
            tertiary = skyMid.copy(alpha = 0.6f),
            accent = skyAccent.copy(alpha = if (isNighttime) 0.7f else 0.9f),
            background = bgColors,
            sunColor = sunCol.copy(alpha = if (isNighttime) 0.7f else 0.95f),
            sunGlowColor = sunGlowCol.copy(alpha = if (isNighttime) 0.35f else 0.55f),
            sunFlareColor = sunLensFlare.copy(alpha = if (isNighttime) 0.2f else 0.4f),
            moonColor = moonCol,
            moonGlowColor = moonGlowCol.copy(alpha = if (isNighttime) 0.6f else 0.4f),
            cloudBaseColor = cloudCol.copy(alpha = cloudAlpha),
            hazeColor = hazeCol.copy(alpha = if (isNighttime) 0.06f else 0.12f),
            mountainFarColor = mountainFar.copy(alpha = 0.18f),
            mountainMidColor = mountainMid.copy(alpha = 0.32f),
            mountainNearColor = mountainNear.copy(alpha = 0.45f),
            groundColor = groundCol.copy(alpha = if (isNighttime) 0.3f else 0.35f),
            groundDetailColor = groundDetail.copy(alpha = if (isNighttime) 0.25f else 0.28f)
        )
    } else {
        // Light mode: use true theme-respecting colors
        // Night: use same dark colors for true night sky regardless of theme
        // Day: preserve saturation with slight lightening
        val isStormy = tempC < 15.0

        if (isNighttime) {
            val nightBg = listOf(
                Color(0xFF04041A),
                skyMid,
                skyBottom.copy(alpha = 0.7f)
            )
            return WeatherPalette(
                primary = Color(0xFF080820),
                secondary = Color(0xFF1A1A3E),
                tertiary = skyMid.copy(alpha = 0.5f),
                accent = skyAccent.copy(alpha = 0.6f),
                background = nightBg,
                sunColor = sunCol.copy(alpha = 0.5f),
                sunGlowColor = sunGlowCol.copy(alpha = 0.2f),
                sunFlareColor = sunLensFlare.copy(alpha = 0.15f),
                moonColor = Color(0xFFECEFF1),
                moonGlowColor = Color(0xFFB3E5FC).copy(alpha = 0.5f),
                cloudBaseColor = cloudCol.copy(alpha = 0.22f),
                hazeColor = hazeCol.copy(alpha = 0.04f),
                mountainFarColor = mountainFar.copy(alpha = 0.12f),
                mountainMidColor = mountainMid.copy(alpha = 0.25f),
                mountainNearColor = mountainNear.copy(alpha = 0.38f),
                groundColor = groundCol.copy(alpha = 0.25f),
                groundDetailColor = groundDetail.copy(alpha = 0.2f)
            )
        }

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
        val bgColors = listOf(
            lightTop,
            skyMid.copy(
                red = (skyMid.red * 0.9f + 0.1f).coerceAtMost(1f),
                green = (skyMid.green * 0.9f + 0.1f).coerceAtMost(1f),
                blue = (skyMid.blue * 0.9f + 0.1f).coerceAtMost(1f),
                alpha = 0.95f
            ),
            lightBottom
        )
        return WeatherPalette(
            primary = lightTop,
            secondary = lightBottom,
            tertiary = skyMid.copy(alpha = 0.85f),
            accent = skyAccent.copy(alpha = 0.8f),
            background = bgColors,
            sunColor = sunCol,
            sunGlowColor = sunGlowCol.copy(alpha = 0.6f),
            sunFlareColor = sunLensFlare.copy(alpha = 0.35f),
            moonColor = moonCol.copy(alpha = 0.8f),
            moonGlowColor = moonGlowCol.copy(alpha = 0.3f),
            cloudBaseColor = cloudCol.copy(alpha = if (isStormy) 0.55f else 0.50f),
            hazeColor = hazeCol.copy(alpha = if (isStormy) 0.15f else 0.08f),
            mountainFarColor = mountainFar.copy(alpha = 0.15f),
            mountainMidColor = mountainMid.copy(alpha = 0.25f),
            mountainNearColor = mountainNear.copy(alpha = 0.35f),
            groundColor = groundCol.copy(alpha = 0.25f),
            groundDetailColor = groundDetail.copy(alpha = 0.2f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Enhanced Day Scene — Sun + drifting clouds + weather condition
// ══════════════════════════════════════════════════════════════════════


/**
 * Computes the vertical Y position (in pixels) of the sun based on time of day.
 * Sun rises from behind mountains at Sunrise (~0.58h), climbs to zenith at Midday (0.12h),
 * and sets behind mountains at Sunset (~0.58h). At Night the sun is off-screen.
 */
private fun sunVerticalY(timeOfDay: TimeOfDay, height: Float): Float {
    return height * when (timeOfDay) {
        TimeOfDay.Dawn -> 0.72f          // Below horizon, soft pre-sunrise glow
        TimeOfDay.Sunrise -> 0.55f        // Peeking over horizon
        TimeOfDay.Morning -> 0.32f        // Climbing toward zenith
        TimeOfDay.Midday -> 0.10f         // At zenith — highest point in sky
        TimeOfDay.Afternoon -> 0.32f      // Descending from zenith
        TimeOfDay.Sunset -> 0.55f         // Sinking below horizon
        TimeOfDay.Twilight -> 0.72f       // Below horizon, fading afterglow
        TimeOfDay.Night -> 1.20f          // Off-screen
    }
}

/**
 * Computes the vertical Y position (in pixels) of the moon based on time of day.
 * Moon is high in the night sky (0.15h), lower during twilight (0.42h),
 * and off-screen during daylight hours.
 */
private fun moonVerticalY(timeOfDay: TimeOfDay, height: Float): Float {
    return height * when (timeOfDay) {
        TimeOfDay.Dawn -> 0.78f
        TimeOfDay.Sunrise -> 0.65f  // Fixed: was 1.20f (off-screen), now visible in upper-right
        TimeOfDay.Morning -> 0.60f  // Fixed: was 1.20f (off-screen), now visible in upper-right
        TimeOfDay.Midday -> 0.55f   // Fixed: was 1.20f (off-screen), moon visible but faint during day
        TimeOfDay.Afternoon -> 0.50f // Fixed: was 1.20f (off-screen), now visible in upper-right
        TimeOfDay.Sunset -> 0.72f
        TimeOfDay.Twilight -> 0.42f
        TimeOfDay.Night -> 0.15f
    }
}
/**
 * Day scene with sun, drifting clouds, and subtle weather condition overlay.
 * Used by the weather widget as the default day background.
 */
@Composable
private fun DayCloudyScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier,
    cloudIntensity: Float = 0.5f  // 0.0 = clear, 0.5 = partly cloudy, 1.0 = overcast
) {
    val isDark = FieldMindTheme.colors.isDark
    
    // Physics-based cloud system for seamless infinite scrolling
    val cloudSystem = remember(cloudIntensity) {
        CloudPhysicsSystem(
            canvasWidth = 1f,
            canvasHeight = 1f,
            maxClouds = if (compact) 4 else 8,
            windForce = 0.01f + cloudIntensity * 0.01f
        )
    }
    
    // Thunder system for realistic lightning with ground effects
    val thunderSystem = remember {
        ThunderPhysicsSystem()
    }
    
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
    val windGust by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "windGust"
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

    // Bird flock animation — visible during morning/evening
    val birdProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "birdFlight"
    )

    // Tree sway animation — gentle wind
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "treeSway"
    )

    // Sky and cloud colors derived from palette, time of day, and cloud intensity
    // cloudIntensity scales cloud opacity and darkens the sun
    val cloudAlphaMul = 0.2f + 0.8f * cloudIntensity  // 0.2 at clear, 1.0 at overcast
    val sunDim = 1f - cloudIntensity * 0.55f  // 1.0 at clear, 0.45 at overcast (dim but visible)
    val cloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.30f else 0.42f * cloudAlphaMul)
    val cloudColorDark = palette.primary.copy(alpha = if (isDark) 0.20f else 0.28f * cloudAlphaMul)
    val sunBody = palette.sunColor.copy(alpha = 0.95f * sunDim)
    val sunInner = palette.sunGlowColor.copy(alpha = 0.85f * sunDim)
    val rayColor = palette.sunGlowColor.copy(alpha = 0.15f * sunGlow * sunDim)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.85f  // Top-right corner
        val cy = sunVerticalY(timeOfDay, size.height)
        val sunRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f

        // Update physics systems
        cloudSystem.update(deltaTime = 0.016f, windGust = windGust)
        thunderSystem.update(deltaTime = 0.016f)
        
        // Trigger occasional lightning during overcast conditions
        if (cloudIntensity > 0.7f && (cloudOffset1 * 100f).toInt() % 300 == 0) {
            thunderSystem.triggerLightning(
                cx + (cloudOffset1 * 400f - 200f),
                cy - size.height * 0.3f,
                size.height * 0.9f
            )
        }

        // Draw the sun
        drawSun(palette, timeOfDay, sunRotation, sunGlow, compact)

        // Render physics-based clouds with infinite scrolling
        val activeClouds = cloudSystem.getActiveClouds()
        activeClouds.forEach { cloud ->
            val cloudDrawColor = if (cloud.type == PhysicsCloud.CloudType.CUMULONIMBUS) {
                cloudColorDark.copy(alpha = cloud.opacity * cloudAlphaMul)
            } else {
                cloudColor.copy(alpha = cloud.opacity * cloudAlphaMul * 0.7f)
            }
            drawCloud(
                offset = (cloud.x + cloud.driftOffset) / size.width,
                baseX = cloud.x,
                baseY = cloud.y,
                scale = cloud.width,
                color = cloudDrawColor,
                morph = cloudMorph + cloud.depth * 2f
            )
        }

        // Draw lightning bolts
        val activeBolts = thunderSystem.getActiveBolts()
        activeBolts.forEach { bolt ->
            drawLightningBolt(bolt, size.width, size.height)
        }

        // Add illumination flash during lightning
        val illumination = thunderSystem.getIlluminationIntensity()
        if (illumination > 0f) {
            drawRect(
                color = Color.White.copy(alpha = illumination * 0.3f),
                size = Size(size.width, size.height)
            )
        }

        // Back layer clouds (slow drift, behind sun) - scales with cloudIntensity
        // At low intensity (0.25): 1 faint cloud. At high intensity (0.85): 3 thick clouds
        val extraClouds = (cloudIntensity * 3f).toInt().coerceIn(1, 3)  // 1-3 additional cloud pairs
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.1f,
            baseY = size.height * 0.15f,
            scale = size.width * 0.45f,
            color = cloudColor.copy(alpha = 0.12f + cloudIntensity * 0.15f),
            morph = cloudMorph
        )

        // Extra back layer clouds at higher intensity
        if (extraClouds >= 2) {
            drawCloud(
                offset = cloudOffset1 * 0.7f,
                baseX = size.width * 0.5f,
                baseY = size.height * 0.1f,
                scale = size.width * 0.4f,
                color = cloudColor.copy(alpha = 0.08f + cloudIntensity * 0.12f),
                morph = cloudMorph + 0.5f
            )
        }
        if (extraClouds >= 3) {
            drawCloud(
                offset = cloudOffset1 * 1.3f,
                baseX = size.width * 0.2f,
                baseY = size.height * 0.25f,
                scale = size.width * 0.35f,
                color = cloudColor.copy(alpha = 0.06f + cloudIntensity * 0.10f),
                morph = cloudMorph + 1.5f
            )
        }

        // Front layer clouds - scales with cloudIntensity
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.65f,
            baseY = size.height * 0.5f,
            scale = size.width * 0.4f,
            color = cloudColorDark.copy(alpha = 0.12f + cloudIntensity * 0.20f),
            morph = cloudMorph + 1f
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.8f,
            baseY = size.height * 0.7f,
            scale = size.width * 0.3f,
            color = cloudColor.copy(alpha = 0.10f + cloudIntensity * 0.15f),
            morph = cloudMorph + 3f
        )

        // Additional front clouds for overcast conditions
        if (extraClouds >= 2) {
            drawCloud(
                offset = cloudOffset2 * 0.8f,
                baseX = size.width * 0.3f,
                baseY = size.height * 0.55f,
                scale = size.width * 0.35f,
                color = cloudColorDark.copy(alpha = 0.08f + cloudIntensity * 0.18f),
                morph = cloudMorph + 2f
            )
        }
        if (extraClouds >= 3) {
            drawCloud(
                offset = cloudOffset2 * 1.2f,
                baseX = size.width * 0.1f,
                baseY = size.height * 0.6f,
                scale = size.width * 0.38f,
                color = cloudColor.copy(alpha = 0.06f + cloudIntensity * 0.14f),
                morph = cloudMorph + 4f
            )
        }

        // Ground terrain with mountains, trees, and rolling hills
        drawGround(palette, weatherCode = 0, isDay = true, isDark = isDark, compact = compact, treeMorph = treeSway)

        // Flying birds during morning/evening
        drawBirds(birdProgress, timeOfDay, isDark)

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
    // Aurora borealis animation
    val auroraProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "auroraDrift"
    )
    val auroraBrightness by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "auroraPulse"
    )
    // Tree sway at night (gentle breeze)
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nightTreeSway"
    )
    // Bird animation (dawn/twilight only)
    val birdProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "nightBirdFlight"
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
        val cy = moonVerticalY(timeOfDay, size.height)
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

        drawMoon(palette, timeOfDay, moonGlow, compact)

        // Shooting stars — 3 random positions, 50° angle, visible for only 35% of cycle
        val nightStarPositions = listOf(
            Offset(size.width * 0.5f, size.height * 0.08f),
            Offset(size.width * 0.75f, size.height * 0.15f),
            Offset(size.width * 0.6f, size.height * 0.05f)
        )
        if (shootingStarProgress < 0.50f) {
            val ssProgress = shootingStarProgress / 0.50f
            val starIndex = (shootingStarProgress * 6f).toInt().coerceIn(0, 2)
            val starPos = nightStarPositions[starIndex]
            drawShootingStar(
                progress = ssProgress,
                startX = starPos.x,
                startY = starPos.y,
                angleDeg = -50f
            )
        }

        // Aurora borealis — beautiful glowing bands across the upper night sky
        drawAurora(auroraProgress, auroraBrightness, isDark)

        // Ground terrain (with wind-affected trees)
        drawGround(palette, weatherCode = -2, isDay = false, isDark = isDark, compact = compact, treeMorph = treeSway)

        // Flying birds during twilight/dawn
        drawBirds(birdProgress, timeOfDay, isDark)

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

        // Firefly particles near ground
        drawFireflies(progress = moonGlow, isDarkTheme = true, compact = compact)

        // Atmospheric haze
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = true, hazeAlpha = moonGlow)
    }
}

/**
 * Cloud shape types for visual variety.
 * - Cumulus: fluffy billowy clusters (classic fair-weather clouds)
 * - Stratus: flat horizontal sheets (overcast, grey)
 * - Cirrus: wispy thin streaks (high-altitude, icy)
 */
enum class CloudType { Cumulus, Stratus, Cirrus }

// Star twinkle data: each star gets its own speed, phase offset, and base brightness
private data class StarPhase(
    val speed: Float,
    val phase: Float,
    val brightness: Float
)

// ══════════════════════════════════════════════════════════════════════
//  Night Cloudy Scene — Moon + stars visible through clouds
// ══════════════════════════════════════════════════════════════════════

/**
 * Night scene with full cloud cover — moon and stars remain visible
 * through semi-transparent cloud layers. Combines NightSkyScene elements
 * with layered cloud rendering from CloudyScene.
 */
@Composable
private fun NightCloudyScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier,
    cloudIntensity: Float = 0.5f  // 0.0 = clear, 0.5 = partly cloudy, 1.0 = overcast
) {
    val isDark = FieldMindTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "nightCloudy")
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "ncCloudDrift1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "ncCloudDrift2"
    )
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ncMoonGlow"
    )
    val cloudMorph by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(15000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ncCloudMorph"
    )
    // Aurora borealis for cloudy nights
    val auroraProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart),
        label = "ncAuroraDrift"
    )
    val auroraBrightness by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(10000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ncAuroraPulse"
    )
    // Tree animation
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ncTreeSway"
    )
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, delayMillis = 5000, easing = LinearEasing), RepeatMode.Restart),
        label = "ncShootingStar"
    )
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ncSunGlow"
    )

    val starCount = if (compact) 20 else 50
    val stars = remember {
        val rng = Random(555)
        List(starCount) { rng.nextFloat() to rng.nextFloat() }
    }
    val starPhases = remember {
        val rng = Random(777)
        List(starCount) {
            StarPhase(
                speed = 1.5f + rng.nextFloat() * 3.5f,
                phase = rng.nextFloat() * 6.28f,
                brightness = 0.2f + rng.nextFloat() * 0.6f
            )
        }
    }
    // Use mix of cloud types for variety
    val cloudTypes = remember {
        val rng = Random(555)
        List(10) { CloudType.entries[rng.nextInt(CloudType.entries.size)] }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.85f
        val cy = moonVerticalY(timeOfDay, size.height)
        val moonRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f

        val moonBody = palette.moonColor.copy(alpha = 1f)
        val moonGlowColor = palette.moonGlowColor
        val nightCloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.22f else 0.18f)
        val nightCloudDark = palette.primary.copy(alpha = if (isDark) 0.18f else 0.14f)

        // Aurora borealis — visible even through clouds
        drawAurora(auroraProgress, auroraBrightness, isDark)

        // Moon glow — enhanced so it's clearly visible through clouds
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.10f * moonGlow),
            radius = moonRadius * 2.5f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.04f * moonGlow),
            radius = moonRadius * 4.0f,
            center = Offset(cx, cy)
        )

        // Moon body with phase
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

        // Stars with independent twinkle
        val starBright = Color(0xFFB3E5FC)
        val starWarm = Color(0xFFFFF9C4)
        stars.forEachIndexed { index, (x, y) ->
            val phase = starPhases[index]
            val twinkle = (sin(moonGlow * phase.speed + phase.phase) * 0.5f + 0.5f)
                .coerceIn(0.15f, 1f) * phase.brightness
            val starRadius = 0.6f + twinkle * 2.0f
            val starColor = if (twinkle > 0.6f) starBright else starWarm
            drawCircle(
                color = starColor.copy(alpha = twinkle.coerceIn(0.15f, 0.85f)),
                radius = starRadius,
                center = Offset(x * size.width, y * size.height)
            )
        }

        // Shooting stars
        val starPositions = listOf(
            Offset(size.width * 0.5f, size.height * 0.08f),
            Offset(size.width * 0.75f, size.height * 0.15f),
            Offset(size.width * 0.6f, size.height * 0.05f)
        )
        if (shootingStarProgress < 0.50f) {
            val ssProgress = shootingStarProgress / 0.50f
            val starIndex = (shootingStarProgress * 7f).toInt().coerceIn(0, 2)
            drawShootingStar(
                progress = ssProgress,
                startX = starPositions[starIndex].x,
                startY = starPositions[starIndex].y,
                angleDeg = -50f
            )
        }

        // Draw moon
        drawMoon(palette, timeOfDay, moonGlow, compact)

        // Cloud opacity derived from cloudIntensity
        val extraLayers = (cloudIntensity * 2f).toInt().coerceIn(0, 2)
        val nightMidAlpha = (0.25f + cloudIntensity * 0.35f).coerceAtMost(0.6f)
        val nightFrontAlpha = (0.20f + cloudIntensity * 0.30f).coerceAtMost(0.5f)
        val midDrift = (cloudOffset2 * 0.8f) % 1f

        // Mid layer clouds
        drawCloud(cloudOffset2, size.width * 0.15f, size.height * 0.3f, size.width * 0.4f,
            nightCloudDark.copy(alpha = nightMidAlpha), cloudMorph + 2f, cloudTypes[3 % cloudTypes.size])
        drawCloud(midDrift - 1f, size.width * 0.3f, size.height * 0.4f, size.width * 0.45f,
            nightCloudDark.copy(alpha = nightMidAlpha), cloudMorph + 2f, cloudTypes[4 % cloudTypes.size])
        drawCloud(midDrift, size.width * 0.7f, size.height * 0.35f, size.width * 0.35f,
            nightCloudColor.copy(alpha = nightMidAlpha * 0.8f), cloudMorph + 3f, cloudTypes[5 % cloudTypes.size])
        if (extraLayers >= 1) {
            drawCloud(midDrift * 0.6f, size.width * 0.1f, size.height * 0.42f, size.width * 0.3f,
                nightCloudColor.copy(alpha = nightMidAlpha * 0.6f), cloudMorph + 6f, cloudTypes[9 % cloudTypes.size])
        }

        // Front layer clouds (darker, more opaque but still see-through)
        val frontDrift = (cloudOffset1 * 0.6f) % 1f
        drawCloud(frontDrift, size.width * 0.15f, size.height * 0.6f, size.width * 0.5f,
            nightCloudDark.copy(alpha = nightFrontAlpha), cloudMorph + 4f, cloudTypes[6 % cloudTypes.size])
        drawCloud(frontDrift - 1f, size.width * 0.15f, size.height * 0.6f, size.width * 0.5f,
            nightCloudDark.copy(alpha = nightFrontAlpha), cloudMorph + 4f, cloudTypes[7 % cloudTypes.size])
        drawCloud(frontDrift, size.width * 0.6f, size.height * 0.55f, size.width * 0.4f,
            nightCloudColor.copy(alpha = nightFrontAlpha * 0.85f), cloudMorph + 5f, cloudTypes[8 % cloudTypes.size])
        if (extraLayers >= 2) {
            drawCloud(frontDrift * 0.5f, size.width * 0.5f, size.height * 0.65f, size.width * 0.35f,
                nightCloudDark.copy(alpha = nightFrontAlpha * 0.7f), cloudMorph + 7f, cloudTypes[10 % cloudTypes.size])
        }

        // Ground terrain with wind-affected trees
        drawGround(palette, weatherCode = 3, isDay = false, isDark = isDark, compact = compact, treeMorph = treeSway)

        // Firefly particles near ground
        drawFireflies(progress = moonGlow, isDarkTheme = true, compact = compact)

        // Atmospheric haze
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = true, hazeAlpha = moonGlow * 0.5f)
    }
}

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
    // Tree sway and birds for clear sky
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "clearTreeSway"
    )
    val birdProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "clearBirdFlight"
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
        val sunCy = sunVerticalY(timeOfDay, size.height)
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

            // Flying birds during daytime
            drawBirds(birdProgress, timeOfDay, isDark)

            // Rainbow — soft atmospheric arc behind the mountains, visible during sunny times
            drawRainbow(palette, compact, timeOfDay)

            // Ground terrain (with wind-affected trees)
            drawGround(palette, weatherCode = 0, isDay = true, isDark = isDark, compact = compact, treeMorph = treeSway)

            // Atmospheric haze
            drawAtmosphericHaze(isDark = false, hazeAlpha = sunGlow * 0.5f, hazeColor = palette.hazeColor)
        } else {
            val moonBody = palette.moonColor.copy(alpha = 1f)
            val moonGlowColor = palette.moonGlowColor
            val moonCx = size.width * 0.85f  // Top-right corner
            val moonCy = moonVerticalY(timeOfDay, size.height)
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
                val starRadius = 1.0f + twinkle * 2.0f
                val starColor = if (twinkle > 0.6f) Color(0xFFB3E5FC) else Color(0xFFFFF9C4)
                drawCircle(
                    color = starColor.copy(alpha = twinkle.coerceIn(0.1f, 0.8f)),
                    radius = starRadius,
                    center = Offset(x * size.width, y * size.height)
                )
            }

            drawSun(palette, timeOfDay, sunRotation, sunGlow, compact)

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

            // Flying birds during twilight/morning
            drawBirds(birdProgress, timeOfDay, isDark)

            // Ground terrain (with wind-affected trees)
            drawGround(palette, weatherCode = -2, isDay = false, isDark = isDark, compact = compact, treeMorph = treeSway)

            // Firefly particles near ground
            drawFireflies(progress = sunGlow, isDarkTheme = true, compact = compact)

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
//  Evening Scene — Dawn & Twilight transition atmosphere
// ══════════════════════════════════════════════════════════════════════

/**
 * A dedicated scene for Dawn (pre-sunrise) and Twilight (post-sunset) transitions.
 * Features a warm horizon glow where the sun recently set or is about to rise,
 * with stars gradually emerging in the darker upper sky and fireflies near the ground.
 * The scene uses palette-driven colors to render the appropriate atmospheric mood.
 */
@Composable
private fun EveningScene(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDawn = timeOfDay == TimeOfDay.Dawn
    val isDark = FieldMindTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "evening")
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eveningSunGlow"
    )
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eveningMoonGlow"
    )
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "eveningStarTwinkle"
    )
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "eveningWave"
    )
    val fireflyProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "eveningFirefly"
    )
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eveningTreeSway"
    )
    val birdProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "eveningBirdFlight"
    )

    val starCount = if (compact) 20 else 50
    val stars = remember { rememberStarPositions(starCount) }
    val starPhases = remember {
        val rng = Random(333)
        List(starCount) {
            StarPhase(
                speed = 1.2f + rng.nextFloat() * 3.0f,
                phase = rng.nextFloat() * 6.28f,
                brightness = 0.2f + rng.nextFloat() * 0.6f
            )
        }
    }

    // Shooting stars in the darker upper sky
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, delayMillis = 6000, easing = LinearEasing), RepeatMode.Restart),
        label = "eveningShootingStar"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Celestial rendering ──
        if (isDawn) {
            // Dawn: sun rising below horizon — warm glow band
            drawSun(palette, timeOfDay, 0f, sunGlow, compact)
        } else {
            // Twilight: moon and fading sunset glow
            drawMoon(palette, timeOfDay, moonGlow, compact)
        }

        // ── Stars in the darker upper sky ──
        val starVisibility = if (isDawn) 0.3f else 0.7f  // More stars at twilight
        val starBrightColor = Color(0xFFB3E5FC)
        val starWarmColor = Color(0xFFFFF9C4)
        stars.forEachIndexed { index, (x, y) ->
            // Stars only visible in the upper portion (y < 0.4) where sky is dark
            if (y < 0.4f) {
                val phase = starPhases[index]
                val twinkle = (sin(starTwinkle * phase.speed + phase.phase) * 0.5f + 0.5f)
                    .coerceIn(0.1f, 1f) * phase.brightness * starVisibility
                val starColor = if (twinkle > 0.5f) starBrightColor else starWarmColor
                val starR = 0.5f + twinkle * 1.8f
                drawCircle(
                    color = starColor.copy(alpha = twinkle.coerceIn(0.05f, 0.7f)),
                    radius = starR,
                    center = Offset(x * size.width, y * size.height)
                )
            }
        }

        // ── Shooting stars ──
        val shootingStarPositions = listOf(
            Offset(size.width * 0.6f, size.height * 0.06f),
            Offset(size.width * 0.8f, size.height * 0.12f),
            Offset(size.width * 0.3f, size.height * 0.04f)
        )
        if (shootingStarProgress < 0.50f) {
            val ssProgress = shootingStarProgress / 0.50f
            val starIndex = (shootingStarProgress * 7f).toInt().coerceIn(0, 2)
            drawShootingStar(
                progress = ssProgress,
                startX = shootingStarPositions[starIndex].x,
                startY = shootingStarPositions[starIndex].y,
                angleDeg = -45f
            )
        }

        // ── Rainbow — soft atmospheric arc behind the mountains (dawn after rain) ──
        drawRainbow(palette, compact, timeOfDay)

        // ── Evening sea view — animated ocean with reflections ──
        drawEveningSea(palette, isDawn, waveProgress)

        // ── Ground terrain with rolling hills ──
        drawGround(
            palette = palette,
            weatherCode = 0,
            isDay = isDawn,
            isDark = isDark,
            compact = compact,
            treeMorph = treeSway
        )

        // ── Flying birds during dawn (migrating) ──
        if (isDawn) {
            drawBirds(birdProgress, timeOfDay, isDark)
        }

        // ── Fireflies near ground ──
        drawFireflies(progress = fireflyProgress, isDarkTheme = true, compact = compact)

        // ── Atmospheric haze ──
        val hazeAlpha = if (isDawn) sunGlow * 0.5f else moonGlow * 0.4f
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = isDark, hazeAlpha = hazeAlpha)
    }
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
    val infiniteTransition = rememberInfiniteTransition(label = "cloudy")
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudSunGlow"
    )
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift"
    )
    val cloudMorph by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(15000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudMorph"
    )
    // Tree sway in wind for cloudy scenes
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloudTreeSway"
    )
    val birdProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudBirdFlight"
    )

    val cloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.35f else 0.52f)
    val cloudColorDark = palette.primary.copy(alpha = if (isDark) 0.25f else 0.35f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudScale = size.width * 0.5f
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

            drawSun(palette, timeOfDay, 0f, sunGlow, compact, cloudDim = 0.6f)
        }

        // Front layer clouds (fast drift, seamless loop)
        val frontDrift = cloudOffset % 1f
        drawCloud(offset = frontDrift, baseX = size.width * 0.15f, baseY = size.height * 0.6f, scale = cloudScale * 1.1f, color = cloudColor.copy(alpha = 0.6f), morph = cloudMorph + 4f)
        drawCloud(offset = frontDrift - 1f, baseX = size.width * 0.15f, baseY = size.height * 0.6f, scale = cloudScale * 1.1f, color = cloudColor.copy(alpha = 0.6f), morph = cloudMorph + 4f)
        drawCloud(offset = frontDrift, baseX = size.width * 0.6f, baseY = size.height * 0.55f, scale = cloudScale * 0.7f, color = cloudColor.copy(alpha = 0.5f), morph = cloudMorph + 5f)
        drawCloud(offset = frontDrift - 1f, baseX = size.width * 0.6f, baseY = size.height * 0.55f, scale = cloudScale * 0.7f, color = cloudColor.copy(alpha = 0.5f), morph = cloudMorph + 5f)

        // Night mode — moon visible through clouds
        if (!isDaytime) {
            val cx = size.width * 0.85f
            val cy = moonVerticalY(timeOfDay, size.height)
            val moonRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f
            drawMoon(palette, timeOfDay, 0.5f, compact)
        }

        drawGround(palette, weatherCode = 3, isDay = isDaytime, isDark = isDark, compact = compact, treeMorph = treeSway)
    }
}

private fun DrawScope.drawCloud(
    offset: Float,
    baseX: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    morph: Float = 0f,
    cloudType: CloudType = CloudType.Cumulus
) {
    val x = (baseX + offset * size.width) % (size.width + scale) - scale * 0.5f

    // Fade-in/out at screen edges for smooth transitions — no sudden popping
    val fadeMargin = scale * 0.8f
    val fadeAlpha = when {
        x < -fadeMargin -> 0f
        x < 0f -> ((x + fadeMargin) / fadeMargin).coerceIn(0f, 1f)
        x > size.width -> 0f
        x > size.width - fadeMargin -> ((size.width - x) / fadeMargin).coerceIn(0f, 1f)
        else -> 1f
    }
    val fadedColor = color.copy(alpha = color.alpha * fadeAlpha)

    when (cloudType) {
        CloudType.Cumulus -> drawCumulus(x, baseY, scale, fadedColor, morph)
        CloudType.Stratus -> drawStratus(x, baseY, scale, fadedColor, morph)
        CloudType.Cirrus -> drawCirrus(x, baseY, scale, fadedColor, morph)
    }
}

/** Billowy fluffy cumulus clouds — classic rounded clusters */
/** Billowy towering cumulonimbus — proper 3D fluffy thundercloud with internal depth */
private fun DrawScope.drawCumulus(
    x: Float, baseY: Float, scale: Float, color: Color, morph: Float
) {
    val cr = scale * 0.14f
    
    // Draw from back to front (3D depth layering)
    // Back layer — dark base of the cloud (anvil)
    val darkColor = color.copy(alpha = color.alpha * 0.6f)
    val midColor = color.copy(alpha = color.alpha * 0.8f)
    val brightColor = color.copy(alpha = color.alpha * 1.0f)
    
    // Bottom flat anvil base — wide, dark, horizontal
    drawCircle(color = darkColor, radius = cr * 1.4f, center = Offset(x, baseY + cr * 0.2f))
    drawCircle(color = darkColor, radius = cr * 1.2f, center = Offset(x - cr * 1.0f + morph * 2f, baseY + cr * 0.4f))
    drawCircle(color = darkColor, radius = cr * 1.1f, center = Offset(x + cr * 1.1f - morph * 1.5f, baseY + cr * 0.35f))
    
    // Middle body — fluffy rounded puffs with organic overlap
    drawCircle(color = midColor, radius = cr * 1.1f, center = Offset(x, baseY))
    drawCircle(color = midColor, radius = cr * 0.9f, center = Offset(x - cr * 0.7f + morph * 2.5f, baseY + cr * 0.2f))
    drawCircle(color = midColor, radius = cr * 0.85f, center = Offset(x + cr * 0.8f - morph * 2f, baseY + cr * 0.15f))
    drawCircle(color = midColor, radius = cr * 0.7f, center = Offset(x - cr * 1.3f - morph * 3f, baseY + cr * 0.1f))
    drawCircle(color = midColor, radius = cr * 0.7f, center = Offset(x + cr * 1.5f + morph * 2.5f, baseY + cr * 0.05f))
    
    // Top — bright, sun-lit cauliflower tops (towering cumulus)
    drawCircle(color = brightColor, radius = cr * 0.8f, center = Offset(x + cr * 0.2f - morph * 1f, baseY - cr * 0.3f))
    drawCircle(color = brightColor, radius = cr * 0.7f, center = Offset(x - cr * 0.5f + morph * 1.5f, baseY - cr * 0.5f))
    drawCircle(color = brightColor, radius = cr * 0.6f, center = Offset(x + cr * 0.6f - morph * 1f, baseY - cr * 0.4f))
    drawCircle(color = brightColor, radius = cr * 0.5f, center = Offset(x + cr * 0.1f, baseY - cr * 0.7f))
    drawCircle(color = brightColor.copy(alpha = brightColor.alpha * 1.2f), radius = cr * 0.4f, center = Offset(x - cr * 0.2f - morph * 0.5f, baseY - cr * 0.85f))
    
    // Highlight rim on upper-right (sunlight hitting the top)
    val rimHighlight = Color.White.copy(alpha = color.alpha * 0.15f)
    drawCircle(color = rimHighlight, radius = cr * 0.3f, center = Offset(x + cr * 0.4f, baseY - cr * 0.6f))
    drawCircle(color = rimHighlight, radius = cr * 0.25f, center = Offset(x + cr * 0.6f, baseY - cr * 0.3f))
    
    // Bottom shadow rim (for ground-relative depth)
    val shadowColor = Color(0xFF0A0A15).copy(alpha = color.alpha * 0.15f)
    drawCircle(color = shadowColor, radius = cr * 0.5f, center = Offset(x - cr * 0.3f, baseY + cr * 0.5f))
    drawCircle(color = shadowColor, radius = cr * 0.4f, center = Offset(x + cr * 0.4f, baseY + cr * 0.55f))
}
private fun DrawScope.drawStratus(
    x: Float, baseY: Float, scale: Float, color: Color, morph: Float
) {
    val sw = scale * 0.55f
    val sh = scale * 0.07f
    // Wide flat oval with subtle vertical undulation
    drawOval(
        color = color,
        topLeft = Offset(x - sw * 0.5f, baseY - sh * 0.5f),
        size = Size(sw, sh)
    )
    // Secondary layer slightly offset for depth
    drawOval(
        color = color.copy(alpha = color.alpha * 0.7f),
        topLeft = Offset(x - sw * 0.35f + morph * 2f, baseY - sh * 0.3f),
        size = Size(sw * 0.7f, sh * 0.6f)
    )
}

/** Wispy cirrus streaks — thin, high-altitude, feathery */
private fun DrawScope.drawCirrus(
    x: Float, baseY: Float, scale: Float, color: Color, morph: Float
) {
    val streakLen = scale * 0.6f
    val streakW = scale * 0.02f
    // Multiple thin angled streaks with slight curve
    for (i in 0..3) {
        val offsetY = i * scale * 0.04f - scale * 0.06f
        val curveX = sin(morph * 0.5f + i * 1.2f) * streakLen * 0.15f
        val alpha = color.alpha * (0.4f + i * 0.2f)
        val path = Path().apply {
            moveTo(x - streakLen * 0.5f + curveX, baseY + offsetY)
            quadraticTo(
                x + curveX * 0.3f, baseY + offsetY - streakW * 2f,
                x + streakLen * 0.5f - curveX, baseY + offsetY
            )
        }
        drawPath(path, color.copy(alpha = alpha), style = Stroke(width = streakW * (1f + i * 0.5f)))
    }
}

/**
 * Draw a layered tree line with natural depth and smooth transitions.
 *
 * Three elevation levels create forest perspective:
 *   Far trees   → Small, faint silhouettes high on the mountain slopes
 *   Mid trees   → Medium size, moderate detail on rolling hills
 *   Near trees  → Largest, most detailed in the foreground
 *
 * All trees use smooth alpha fade at screen edges — no sudden popping.
 * Wind (morph) sways trunks and canopies gently.
 * Snow caps appear as white crescents on canopy tops when isSnow=true.
 */
private fun DrawScope.drawTreeLine(morph: Float = 0f, isDark: Boolean = false, isSnow: Boolean = false) {
    val trunkColor = if (isDark) Color(0xFF020704).copy(alpha = 0.66f) else Color(0xFF173219).copy(alpha = 0.24f)
    val canopyColor = if (isDark) Color(0xFF07120B).copy(alpha = 0.72f) else Color(0xFF2A4A2A).copy(alpha = 0.30f)
    val snowCapColor = if (isDark) Color(0xFFC8D8E8).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.35f)

    // Ground reference curve (matching drawGround's mid terrain)
    fun groundY(t: Float): Float {
        val baseY = size.height * 0.82f
        val hills = sin(t * 3.5f) * 0.05f +
            sin(t * 8.0f + 1.1f) * 0.035f +
            sin(t * 16.0f + 3.3f) * 0.018f
        return baseY - hills * size.height
    }

    data class TreeLayer(val elevation: Float, val size: Float, val alpha: Float, val count: Int)
    val layers = listOf(
        TreeLayer(0.72f, 0.35f, 0.35f, 14),
        TreeLayer(0.79f, 0.60f, 0.60f, 20),
        TreeLayer(0.86f, 1.00f, 1.00f, 28)
    )

    // Edge fade margin — trees within this distance from screen edges fade smoothly
    val edgeFadeMargin = 0.08f * size.width

    layers.forEach { layer ->
        val seed = (layer.elevation * 100).toInt()
        for (i in 0 until layer.count) {
            val t = (i.toFloat() + 0.5f) / layer.count
            val px = t * size.width

            // Smooth edge fade — no popping
            val edgeFade = when {
                px < edgeFadeMargin -> (px / edgeFadeMargin).coerceIn(0f, 1f)
                px > size.width - edgeFadeMargin -> ((size.width - px) / edgeFadeMargin).coerceIn(0f, 1f)
                else -> 1f
            }
            // Extra fade for outermost trees for natural thinning
            val edgeWeight = sin(t * PI.toFloat()).pow(0.6f).coerceIn(0.15f, 1f)
            val totalFade = edgeFade * edgeWeight
            if (totalFade < 0.02f) continue

            // Height + width variation using noise
            val noise = sin(t * 17.0f + seed + morph * 0.3f) * 0.3f + 0.5f +
                sin(t * 31.0f + seed * 2 + morph * 0.5f) * 0.15f
            val sizeFactor = (0.2f + noise * 0.8f).coerceAtLeast(0.05f)

            val treeH = size.height * (0.005f + sizeFactor * 0.028f) * layer.size
            val treeW = treeH * (0.15f + sizeFactor * 0.12f) * layer.size
            val treeBaseY = groundY(t)

            // Wind sway — trunks bend slightly
            val sway = morph * 0.15f * (0.5f + noise * 0.5f)

            // Trunk
            val trunkH = treeH * 0.22f
            val trunkW = treeW * 0.3f
            val trunkAlpha = trunkColor.alpha * layer.alpha * totalFade
            drawRoundRect(
                color = trunkColor.copy(alpha = trunkAlpha * 0.6f),
                topLeft = Offset(px - trunkW * 0.5f + sway * 2f, treeBaseY - trunkH),
                size = Size(trunkW, trunkH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trunkW * 0.3f, trunkW * 0.3f)
            )

            // Canopy — 3 rounded tiers stacked pyramid-style
            val canopyTiers = if (layer.size < 0.5f) 2 else 3
            for (tier in 0 until canopyTiers) {
                val tierRatio = tier.toFloat() / canopyTiers
                val cy = treeBaseY - trunkH - treeH * (0.12f + tierRatio * 0.50f)
                val tierW = treeW * (0.55f + (canopyTiers - tier) * 0.20f)
                val tierH = treeH * (0.15f + (1f - tierRatio) * 0.12f)
                val tierAlpha = canopyColor.alpha * layer.alpha * totalFade * (1f - tier * 0.08f)

                // Pine/rounded canopy shape
                val canopy = Path().apply {
                    moveTo(px - tierW * 0.5f + sway, cy + tierH * 0.2f)
                    quadraticTo(px + sway, cy - tierH * 0.5f, px + tierW * 0.5f + sway, cy + tierH * 0.2f)
                    close()
                }
                drawPath(canopy, canopyColor.copy(alpha = tierAlpha), style = Fill)

                // Snow cap
                if (isSnow) {
                    val snowW = tierW * 0.55f
                    val snowH = tierH * 0.18f
                    val snowPath = Path().apply {
                        moveTo(px - snowW + sway, cy + tierH * 0.05f)
                        quadraticTo(px + sway, cy - tierH * 0.4f - snowH * 0.3f, px + snowW + sway, cy + tierH * 0.05f)
                        close()
                    }
                    drawPath(snowPath, snowCapColor.copy(alpha = snowCapColor.alpha * layer.alpha * totalFade), style = Fill)
                }
            }
        }
    }

    // ── Base forest silhouette (fills gaps between trees with a continuous undulating treeline) ──
    val baseAlpha = if (isDark) 0.65f else 0.25f
    val basePath = Path().apply {
        moveTo(-10f, size.height + 10f)
        lineTo(-10f, groundY(0f))
        val segs = 60
        for (i in 0..segs) {
            val t = i.toFloat() / segs
            val px = t * (size.width + 20f) - 10f
            val noise = sin(t * 10f + morph * 0.4f) * 0.25f +
                sin(t * 22f + morph * 0.3f + 1.5f) * 0.15f +
                sin(t * 40f + morph * 0.7f + 2.7f) * 0.1f
            val treeH = noise * size.height * 0.07f + size.height * 0.035f
            val py = groundY(t) - treeH
            lineTo(px, py)
        }
        lineTo(size.width + 10f, size.height + 10f)
        close()
    }
    drawPath(basePath, canopyColor.copy(alpha = baseAlpha), style = Fill)
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
 * Draw weather-adaptive ground terrain with multi-layered depth.
 * Three terrain layers create a sense of receding landscape:
 *   Far terrain (behind mountains) → blends into atmospheric haze
 *   Mid terrain (rolling hills) → primary ground silhouette
 *   Near terrain (foreground) → highest contrast, warmest tones
 *
 * Terrain colors are driven by the time-of-day palette, with weather-specific
 * overrides for snow (white/grey), rain (dark/muted), and storms (moody).
 */
private fun DrawScope.drawGround(
    palette: WeatherPalette,
    weatherCode: Int,
    isDay: Boolean,
    isDark: Boolean,
    compact: Boolean,
    treeMorph: Float = 0f
) {
    if (compact) return

    val groundY = size.height * 0.82f
    val isRain = weatherCode in 51..67 || weatherCode in 80..82
    val isSnow = weatherCode in 71..77 || weatherCode in 85..86
    val isThunder = weatherCode >= 95

    // Weather-modulated ground colors — palette provides base, weather tweaks alpha/shade
    val baseGround = palette.groundColor
    val baseDetail = palette.groundDetailColor

    val groundMul = when {
        isSnow -> 1.5f
        isRain || isThunder -> 0.7f
        !isDay -> 0.5f
        else -> 1.0f
    }

    // Draw mountain range behind everything (palette-driven atmospheric perspective)
    drawMountainRange(palette, isDark = isDark, isSnow = isSnow, isDay = isDay, isThunder = isThunder)

    // ── Sea/ocean at the horizon during fair weather ──
    val isSeaVisible = isDay && !isSnow && !isThunder && !isRain
    if (isSeaVisible) {
        val seaColor = palette.tertiary.copy(
            red = palette.tertiary.red * 0.6f,
            green = palette.tertiary.green * 0.7f + 0.15f,
            blue = palette.tertiary.blue * 0.8f + 0.3f,  // More blue for deeper water
            alpha = 0.18f  // Higher alpha for richer color
        )
        val seaDeep = palette.primary.copy(
            red = palette.primary.red * 0.3f,
            green = palette.primary.green * 0.4f,
            blue = palette.primary.blue * 0.7f + 0.2f,  // Darker, more blue
            alpha = 0.25f  // More visible
        )
        val seaBorder = Color.White.copy(alpha = 0.35f)  // Thin white horizon line
        drawLine(
            color = seaBorder,
            start = Offset(0f, groundY - size.height * 0.02f),
            end = Offset(size.width, groundY - size.height * 0.02f),
            strokeWidth = 1.5f
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(seaColor, seaDeep),
                startY = groundY - size.height * 0.02f,
                endY = size.height
            ),
            topLeft = Offset(0f, groundY - size.height * 0.02f),
            size = Size(size.width, size.height - groundY + size.height * 0.02f)
        )
        // Subtle sun reflection on water
        if (isDay) {
            val reflectColor = palette.accent.copy(alpha = 0.08f)
            for (i in 0..4) {
                val rx = size.width * (0.35f + i * 0.08f)
                val ry = groundY + size.height * (0.01f + i * 0.015f)
                val rw = size.width * 0.03f
                val reflectAlpha = (1f - i * 0.15f).coerceAtLeast(0.2f)
                drawLine(
                    color = reflectColor.copy(alpha = reflectAlpha * 0.15f),
                    start = Offset(rx, ry),
                    end = Offset(rx + rw, ry),
                    strokeWidth = 1.5f
                )
            }
        }
    }

    // ── Mid-distance terrain (primary rolling hills) ──
    val midColor = baseGround.copy(
        red = (baseGround.red * groundMul).coerceAtMost(1f),
        green = (baseGround.green * groundMul).coerceAtMost(1f),
        blue = (baseGround.blue * groundMul).coerceAtMost(1f),
        alpha = (baseGround.alpha * (if (isSnow) 1.4f else 1.0f)).coerceAtMost(0.45f)
    )

    val midPath = Path()
    midPath.moveTo(-10f, size.height + 10f)
    midPath.lineTo(-10f, groundY)

    val segments = 60
    var prevX = -10f
    var prevY = groundY
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val px = t * (size.width + 20f) - 10f
        val hills = sin(t * 3.5f) * 0.05f +
            sin(t * 8.0f + 1.1f) * 0.035f +
            sin(t * 16.0f + 3.3f) * 0.018f +
            sin(t * 30.0f + 0.7f) * 0.008f
        val py = groundY - hills * size.height
        // Smooth bezier interpolation between points
        val midX = (prevX + px) / 2f
        val midY = (prevY + py) / 2f - size.height * 0.003f
        midPath.cubicTo(
            prevX + (midX - prevX) * 0.5f, prevY,
            midX, midY,
            px, py
        )
        prevX = px
        prevY = py
    }
    midPath.lineTo(size.width + 10f, size.height + 10f)
    midPath.close()
    drawPath(path = midPath, color = midColor, style = Fill)

    // ── Foreground terrain (closer, darker, more detailed) ──
    val nearColor = baseDetail.copy(
        alpha = (baseDetail.alpha * (if (isSnow) 1.5f else 1.0f)).coerceAtMost(0.4f)
    )

    val nearPath = Path()
    nearPath.moveTo(-10f, size.height + 10f)
    nearPath.lineTo(-10f, size.height * 0.88f)

    prevX = -10f
    prevY = size.height * 0.88f
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val px = t * (size.width + 20f) - 10f
        val hills = sin(t * 5.0f + 0.7f) * 0.035f +
            sin(t * 11.0f + 2.1f) * 0.022f +
            sin(t * 22.0f + 4.5f) * 0.012f
        val py = size.height * 0.88f - hills * size.height
        val midX = (prevX + px) / 2f
        val midY = (prevY + py) / 2f - size.height * 0.002f
        nearPath.cubicTo(
            prevX + (midX - prevX) * 0.5f, prevY,
            midX, midY,
            px, py
        )
        prevX = px
        prevY = py
    }
    nearPath.lineTo(size.width + 10f, size.height + 10f)
    nearPath.close()
    drawPath(path = nearPath, color = nearColor, style = Fill)

    // ── Tree line (animated with wind — drawn between mid and near terrain) ──
    drawTreeLine(morph = treeMorph, isDark = isDark, isSnow = isSnow)

    // ── Grass detail on near terrain (non-snow) ──
    if (!isSnow) {
        val grassColor = baseDetail.copy(alpha = baseDetail.alpha * 0.6f)
        for (i in 0..24) {
            val t = (i.toFloat() / 24f)
            val gh = groundY - (sin(t * 5.0f + 0.3f) * 0.05f + sin(t * 12f + 2.0f) * 0.03f) * size.height
            val bladeH = (sin(t * 7.0f + i * 0.5f) * 0.4f + 0.6f) * size.height * 0.012f
            if (bladeH < 2f) continue
            val bx = t * (size.width + 20f) - 10f
            val grassPath = Path().apply {
                moveTo(bx, gh)
                quadraticTo(bx + 2f + treeMorph * 1.5f, gh - bladeH * 0.7f, bx + 4f + treeMorph * 2f, gh - bladeH)
            }
            drawPath(grassPath, grassColor, style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        }
    }

    // ── Puddles on rainy/wet ground ──
    if (isRain || isThunder) {
        for (i in 0..5) {
            val px = size.width * (0.08f + i * 0.17f) + sin(i * 2.3f) * size.width * 0.04f
            val py = groundY + size.height * 0.025f + sin(i * 1.7f) * size.height * 0.015f
            val pw = size.width * (0.05f + sin(i * 0.5f) * 0.015f)
            val ph = size.height * 0.006f
            val puddleC = if (isDark) Color(0xFF3A5A7A).copy(alpha = 0.2f) else Color(0xFF5A8AAA).copy(alpha = 0.12f)
            drawOval(color = puddleC, topLeft = Offset(px - pw / 2, py - ph / 2), size = Size(pw, ph))
            drawOval(color = Color.White.copy(alpha = 0.04f), topLeft = Offset(px - pw * 0.25f, py - ph * 0.2f), size = Size(pw * 0.5f, ph * 0.4f))
        }
    }
}

/**
 * Draw a layered mountain range with true atmospheric perspective.
 *
 * Three layers create depth through atmospheric scattering:
 *   Far  → Strongly blue-shifted (Rayleigh scattering), low contrast, hazy, compressed values
 *   Mid  → Slightly blue-shifted, moderate contrast, defined rolling shapes
 *   Near → Warm tones, high contrast, crisp edges, most detailed
 *
 * Color shifts follow the time-of-day palette from WeatherPalette.
 * Snow caps are largest on near mountains and fade into the haze on far mountains.
 */
private fun DrawScope.drawMountainRange(
    palette: WeatherPalette,
    isDark: Boolean,
    isSnow: Boolean,
    isDay: Boolean,
    isThunder: Boolean = false
) {
    val ridgeBase = size.height * 0.76f
    val thunderScale = if (isThunder) 1.6f else 1.0f
    val thunderBaseShift = if (isThunder) -0.06f else 0f

    data class MountainLayer(
        val baseY: Float,
        val heightFactor: Float,
        val color: Color,
        val snowColor: Color,
        val undulationFreq: Float,
        val edgeSoftness: Float  // 0.0=crisp, 1.0=very soft
    )

    // Use the palette's mountain colors with proper atmospheric perspective
    val farPalette = palette.mountainFarColor
    val midPalette = palette.mountainMidColor
    val nearPalette = palette.mountainNearColor
    val snowBase = if (isDark) Color(0xFFC8D8E8) else Color.White

    val layers = listOf(
        // Far layer — strongly blue-shifted, hazy, low contrast, gentle rolling hills
        MountainLayer(
            baseY = 0.60f + thunderBaseShift * 0.3f,
            heightFactor = 0.07f * thunderScale,
            color = farPalette.copy(alpha = farPalette.alpha * 0.7f),
            snowColor = snowBase.copy(alpha = if (isSnow || isDay) 0.06f else 0f),
            undulationFreq = 2.0f,
            edgeSoftness = 1.0f
        ),
        // Mid layer — moderate blue shift, more defined
        MountainLayer(
            baseY = 0.66f + thunderBaseShift * 0.5f,
            heightFactor = 0.09f * thunderScale,
            color = midPalette.copy(alpha = midPalette.alpha * 0.85f),
            snowColor = snowBase.copy(alpha = if (isSnow) 0.10f else 0.03f),
            undulationFreq = 3.0f,
            edgeSoftness = 0.5f
        ),
        // Near layer — warm/dark, high contrast, crisp
        MountainLayer(
            baseY = 0.72f + thunderBaseShift,
            heightFactor = 0.13f * thunderScale,
            color = nearPalette.copy(alpha = nearPalette.alpha),
            snowColor = snowBase.copy(alpha = if (isSnow) 0.18f else 0.05f),
            undulationFreq = 4.5f,
            edgeSoftness = 0.0f
        )
    )

    val seedOffset = 137

    // ── Opaque base silhouette — prevents rainbow/background from showing through
    // the semi-transparent atmospheric mountain layers. Uses the near layer's
    // shape parameters for the widest silhouette.
    val baseSilhouette = Path()
    baseSilhouette.moveTo(-20f, ridgeBase + size.height * 0.02f)
    var basePrevX = -20f
    var basePrevY = ridgeBase
    val baseSegs = 80
    for (i in 0..baseSegs) {
        val t = i.toFloat() / baseSegs
        val px = t * (size.width + 40f) - 20f
        val undulation =
            sin(t * 4.5f * PI.toFloat() + 274f) * 0.6f +
            sin(t * 4.5f * 1.8f + 274f * 1.3f) * 0.25f +
            sin(t * 4.5f * 3.5f + 274f * 0.7f) * 0.1f +
            sin(t * 4.5f * 7f + 274f * 2.1f) * 0.05f
        val centerBias = sin(t * PI.toFloat()).pow(0.8f)
        val height = 0.13f * thunderScale *
            (0.4f + 0.6f * (undulation * 0.5f + 0.5f)) *
            (0.7f + 0.3f * centerBias)
        val py = size.height * (0.60f + thunderBaseShift - height)
        val midX = (basePrevX + px) / 2f
        val midY = (basePrevY + py) / 2f - size.height * 0.005f
        baseSilhouette.cubicTo(
            basePrevX + (midX - basePrevX) * 0.7f, basePrevY,
            midX, midY,
            px, py
        )
        basePrevX = px
        basePrevY = py
    }
    baseSilhouette.lineTo(size.width + 20f, ridgeBase + size.height * 0.02f)
    baseSilhouette.lineTo(size.width + 20f, size.height)
    baseSilhouette.lineTo(-20f, size.height)
    baseSilhouette.close()
    drawPath(baseSilhouette, nearPalette.copy(alpha = 1f), style = Fill)

    layers.forEachIndexed { layerIdx, layer ->
        val path = Path()
        path.moveTo(-20f, ridgeBase + size.height * 0.02f)

        val segments = 80
        var prevX = -20f
        var prevY = ridgeBase

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val px = t * (size.width + 40f) - 20f

            // Smooth undulating terrain with multiple sine harmonics
            val layerOffset = layerIdx * 1.7f + seedOffset
            val undulation =
                sin(t * layer.undulationFreq * PI.toFloat() + layerOffset) * 0.6f +
                sin(t * layer.undulationFreq * 1.8f + layerOffset * 1.3f) * 0.25f +
                sin(t * layer.undulationFreq * 3.5f + layerOffset * 0.7f) * 0.1f +
                sin(t * layer.undulationFreq * 7f + layerOffset * 2.1f) * 0.05f

            // Center bias — terrain naturally higher in the middle
            val centerBias = sin(t * PI.toFloat()).pow(0.8f)

            // Value compression: far layers have less height variation (compressed values)
            val valueRange = 1f - layerIdx * 0.15f  // 1.0, 0.85, 0.70
            val height = layer.heightFactor *
                (0.4f + 0.6f * (undulation * 0.5f + 0.5f)) *
                (0.7f + 0.3f * centerBias) *
                valueRange

            val py = size.height * (layer.baseY - height)

            // Edge softening: far layers use smoother curves (less sharp transitions)
            val smoothness = 0.5f + layer.edgeSoftness * 0.4f
            val midX = (prevX + px) / 2f
            val midY = (prevY + py) / 2f - size.height * 0.005f * (1f - layerIdx * 0.2f)
            path.cubicTo(
                prevX + (midX - prevX) * (1f - smoothness * 0.3f), prevY,
                midX, midY + layer.edgeSoftness * size.height * 0.005f,
                px, py
            )

            prevX = px
            prevY = py
        }

        path.lineTo(size.width + 20f, ridgeBase + size.height * 0.02f)
        path.lineTo(size.width + 20f, size.height)
        path.lineTo(-20f, size.height)
        path.close()

        // Draw mountain body
        drawPath(path, layer.color, style = Fill)

        // Atmospheric haze gradient at the base of far/mid layers (blends into ground)
        if (layerIdx < 2) {
            val hazeHeight = size.height * (0.04f - layerIdx * 0.01f)
            val hazeColor = if (isDay) Color(0xFFFFF9C4).copy(alpha = 0.03f * (1f - layerIdx * 0.3f))
                else Color(0xFFB3E5FC).copy(alpha = 0.02f * (1f - layerIdx * 0.3f))
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, hazeColor),
                    startY = ridgeBase - hazeHeight,
                    endY = ridgeBase + size.height * 0.02f
                ),
                topLeft = Offset(-20f, ridgeBase - hazeHeight - 5f),
                size = Size(size.width + 40f, hazeHeight + 5f)
            )
        }

        // Draw snow caps on high peaks
        if (isSnow || (isDay && layerIdx == 2)) {
            val capSegments = 50
            for (i in 0 until capSegments) {
                val t = (i.toFloat() + 0.5f) / capSegments
                val lo = layerIdx * 1.7f + seedOffset
                val und =
                    sin(t * layer.undulationFreq * PI.toFloat() + lo) * 0.6f +
                    sin(t * layer.undulationFreq * 1.8f + lo * 1.3f) * 0.25f +
                    sin(t * layer.undulationFreq * 3.5f + lo * 0.7f) * 0.1f
                val cb = sin(t * PI.toFloat()).pow(0.8f)
                val h = layer.heightFactor * (0.4f + 0.6f * (und * 0.5f + 0.5f)) * (0.7f + 0.3f * cb)
                val py = size.height * (layer.baseY - h)
                val peakH = (ridgeBase - py) / size.height

                // Far layer needs taller peaks to snow-cap, near layer snow-caps more easily
                val snowThreshold = 0.06f - layerIdx * 0.015f
                if (peakH > snowThreshold && und > 0.1f) {
                    val px = t * (size.width + 40f) - 20f
                    val capW = size.minDimension * (0.012f + peakH * 0.06f) * (0.6f + 0.4f * cb)
                    val capH = capW * 0.25f * (1f + layerIdx * 0.1f)

                    val snowPath = Path().apply {
                        moveTo(px - capW * 0.5f, py + capH * 0.3f)
                        cubicTo(
                            px - capW * 0.3f, py + capH * 0.1f,
                            px - capW * 0.15f, py - capH * 0.3f,
                            px, py - capH * 0.5f
                        )
                        cubicTo(
                            px + capW * 0.15f, py - capH * 0.3f,
                            px + capW * 0.3f, py + capH * 0.1f,
                            px + capW * 0.5f, py + capH * 0.3f
                        )
                        lineTo(px + capW * 0.35f, py + capH * 0.6f)
                        lineTo(px + capW * 0.15f, py + capH * 0.55f)
                        lineTo(px, py + capH * 0.5f)
                        lineTo(px - capW * 0.15f, py + capH * 0.55f)
                        lineTo(px - capW * 0.35f, py + capH * 0.6f)
                        close()
                    }
                    drawPath(snowPath, layer.snowColor, style = Fill)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Moon Phase Calculation & Drawing

// ══════════════════════════════════════════════════════════════════════
//  Unified Celestial Rendering — Sun & Moon with depth and atmosphere
// ══════════════════════════════════════════════════════════════════════

/**
 * Draw the sun with multi-ring glow, rotating rays, lens flare artifacts,
 * and a horizontal horizon glow band during sunrise/sunset.
 * cloudDim controls how visible the sun is through cloud cover (0-1).
 */
private fun DrawScope.drawSun(
    palette: WeatherPalette,
    timeOfDay: TimeOfDay,
    sunRotation: Float,
    sunGlow: Float,
    compact: Boolean,
    cloudDim: Float = 1f
) {
    val cx = size.width * 0.85f
    val cy = sunVerticalY(timeOfDay, size.height)
    val sunRadius = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f
    val isLowSun = timeOfDay == TimeOfDay.Sunrise || timeOfDay == TimeOfDay.Sunset || timeOfDay == TimeOfDay.Dawn

    // Outer atmosphere glow (three rings of decreasing intensity)
    drawCircle(color = palette.sunGlowColor.copy(alpha = 0.12f * sunGlow * cloudDim),
        radius = sunRadius * 1.8f, center = Offset(cx, cy))
    drawCircle(color = palette.sunFlareColor.copy(alpha = 0.06f * sunGlow * cloudDim),
        radius = sunRadius * 2.8f, center = Offset(cx, cy))
    drawCircle(color = Color.White.copy(alpha = 0.03f * sunGlow * cloudDim),
        radius = sunRadius * 4.0f, center = Offset(cx, cy))

    // Horizontal horizon glow band (sunrise/sunset only)
    if (isLowSun && !compact) {
        val bandColor = palette.sunGlowColor.copy(alpha = 0.04f * sunGlow * cloudDim)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(bandColor, Color.Transparent),
                center = Offset(cx, cy + sunRadius),
                radius = size.width * 0.6f
            ),
            topLeft = Offset(cx - size.width * 0.5f, cy - size.height * 0.03f),
            size = Size(size.width, size.height * 0.06f)
        )
    }

    // Sun rays (rotating, fade with clouds)
    val rayCount = if (compact) 5 else 8
    val rayLen = sunRadius * 1.3f
    val rayCol = palette.sunGlowColor.copy(alpha = 0.12f * sunGlow * cloudDim)
    for (i in 0 until rayCount) {
        val angle = sunRotation + (360f / rayCount) * i
        val rad = (angle * PI.toFloat() / 180f)
        val x1 = cx + cos(rad) * sunRadius * 0.9f
        val y1 = cy + sin(rad) * sunRadius * 0.9f
        val x2 = cx + cos(rad) * rayLen
        val y2 = cy + sin(rad) * rayLen
        drawLine(color = rayCol, start = Offset(x1, y1), end = Offset(x2, y2),
            strokeWidth = if (compact) 1.2f else 2f, cap = StrokeCap.Round)
    }

    // Sun body (bright core + warm outer)
    drawCircle(color = palette.sunColor.copy(alpha = 0.95f * cloudDim),
        radius = sunRadius, center = Offset(cx, cy))
    drawCircle(color = Color.White.copy(alpha = 0.5f * cloudDim),
        radius = sunRadius * 0.55f, center = Offset(cx, cy))

    // Lens flare artifacts (low sun only)
    if (isLowSun && !compact && cloudDim > 0.3f) {
        val flareCol = palette.sunFlareColor.copy(alpha = 0.08f * sunGlow * cloudDim)
        for (i in 0..2) {
            val fx = cx - sunRadius * (2.2f + i * 1.3f)
            val fy = cy + sin(i * 2.1f) * sunRadius * 0.4f
            val fs = (1.5f - i * 0.3f).coerceAtLeast(0.5f)
            drawCircle(color = flareCol, radius = sunRadius * 0.12f * fs, center = Offset(fx, fy))
            drawCircle(color = flareCol.copy(alpha = flareCol.alpha * 0.5f),
                radius = sunRadius * 0.25f * fs, center = Offset(fx, fy))
        }
        drawLine(color = flareCol.copy(alpha = 0.3f),
            start = Offset(cx - sunRadius * 6f, cy + sunRadius * 0.1f),
            end = Offset(cx - sunRadius * 1.5f, cy - sunRadius * 0.1f),
            strokeWidth = 0.5f, cap = StrokeCap.Round)
    }

    // Sun reflection on water (low sun only)
    if (isLowSun && !compact) {
        val waterY = size.height * 0.82f
        val reflectCol = palette.sunColor.copy(alpha = 0.08f * sunGlow * cloudDim)
        for (i in 0..4) {
            val rw = sunRadius * (0.5f + i * 0.2f)
            val ry = waterY + i * (size.height * 0.006f)
            drawLine(color = reflectCol.copy(alpha = (0.15f - i * 0.03f).coerceAtLeast(0f)),
                start = Offset(cx - rw, ry), end = Offset(cx + rw, ry), strokeWidth = 1.2f - i * 0.2f)
        }
    }
}

/**
 * Draw the moon with multi-ring glow, phase-aware shape, and atmospheric tint.
 * Glow expands during crescent phases and tightens during full moon.
 */
private fun DrawScope.drawMoon(
    palette: WeatherPalette,
    timeOfDay: TimeOfDay,
    moonGlow: Float,
    compact: Boolean
) {
    val cx = size.width * 0.85f
    val cy = moonVerticalY(timeOfDay, size.height)
    val moonR = if (compact) size.minDimension * 0.09f else size.minDimension * 0.07f
    val phase = getMoonPhaseValue()
    val isFull = phase in 0.48f..0.52f
    val gm = if (isFull) 0.8f else 1.3f  // Tighter glow at full moon

    drawCircle(color = palette.moonGlowColor.copy(alpha = 0.08f * moonGlow),
        radius = moonR * 2.0f * gm, center = Offset(cx, cy))
    drawCircle(color = palette.moonGlowColor.copy(alpha = 0.03f * moonGlow),
        radius = moonR * 3.5f * gm, center = Offset(cx, cy))
    drawCircle(color = Color.White.copy(alpha = 0.01f * moonGlow),
        radius = moonR * 5.0f * gm, center = Offset(cx, cy))

    drawMoonPhase(
        phaseValue = phase, cx = cx, cy = cy, radius = moonR,
        litColor = palette.moonColor.copy(alpha = 1f),
        shadowColor = Color(0xFF0A0A1A).copy(alpha = 0.7f),
        glowColor = palette.moonGlowColor
    )
}

// ═══════════════════════════════════════════════════════════════��══���═══

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
    phaseValue: Float,        // 0.0–1.0 moon phase (0=new, 0.25=waxing quarter, 0.5=full, 0.75=waning quarter)
    cx: Float,                // center x
    cy: Float,                // center y
    radius: Float,            // moon radius
    litColor: Color,          // illuminated moon color
    shadowColor: Color,       // dark/shadowed portion color
    glowColor: Color,         // subtle inner glow for dark-side detail
) {
    // Draw full lit circle first
    drawCircle(color = litColor, radius = radius, center = Offset(cx, cy))

    // Full moon — no shadow needed
    if (phaseValue in 0.48f..0.52f) return

    // New moon — completely shadow
    if (phaseValue < 0.03f || phaseValue > 0.97f) {
        drawCircle(color = shadowColor, radius = radius, center = Offset(cx, cy))
        // Add subtle rim glow for edge visibility
        drawCircle(
            color = glowColor.copy(alpha = 0.1f),
            radius = radius * 0.95f,
            center = Offset(cx, cy)
        )
        return
    }

    // Determine waxing (0-0.5) vs waning (0.5-1)
    val isWaxing = phaseValue < 0.5f
    // Normalize phase to 0-1 range within current half
    val normalizedPhase = if (isWaxing) phaseValue * 2f else (phaseValue - 0.5f) * 2f
    
    // Calculate the shadow circle offset and radius for proper moon phases
    // Shadow offset moves from left (waxing) or right (waning) toward center as moon gets fuller
    val shadowOffsetX = if (isWaxing) {
        // Waxing: shadow starts on LEFT (2*radius), moves RIGHT toward center
        radius * 2f * (1f - normalizedPhase)
    } else {
        // Waning: shadow starts on RIGHT (-2*radius), moves LEFT toward center
        -radius * 2f * (1f - normalizedPhase)
    }

    // Draw the shadow circle with offset
    // The shadow circle needs to be slightly larger to smoothly hide the lit portion
    drawCircle(
        color = shadowColor,
        radius = radius * 1.02f,
        center = Offset(cx + shadowOffsetX, cy)
    )

    // Add earthshine (subtle glow on dark side) for intermediate phases
    if (normalizedPhase in 0.1f..0.4f || normalizedPhase in 0.6f..0.9f) {
        val earthshineAlpha = when {
            normalizedPhase < 0.5f -> (0.4f - normalizedPhase) * 0.15f  // Waxing crescent to gibbous
            else -> (normalizedPhase - 0.6f) * 0.15f                     // Waning gibbous to crescent
        }
        
        val earthshineX = if (isWaxing) cx - radius * 0.4f else cx + radius * 0.4f
        drawCircle(
            color = glowColor.copy(alpha = earthshineAlpha.coerceIn(0f, 0.1f)),
            radius = radius * 0.8f,
            center = Offset(earthshineX, cy)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Birds — Small silhouettes flying in morning/evening sky
// ═══════════════════════════════════════════════════════════���══════════

/**
 * Draw small flying birds as V-shaped silhouettes drifting across the sky.
 * Birds appear in flocks during morning/evening with natural random spacing.
 */
private fun DrawScope.drawBirds(
    progress: Float,        // 0..1 animation cycle
    timeOfDay: TimeOfDay,
    isDarkTheme: Boolean
) {
    // Only during Dawn, Sunrise, Morning, Afternoon, Sunset — active hours
    val birdActive = timeOfDay in listOf(TimeOfDay.Dawn, TimeOfDay.Sunrise, TimeOfDay.Morning, TimeOfDay.Afternoon, TimeOfDay.Sunset)
    if (!birdActive) return

    val birdColor = if (isDarkTheme) Color(0xFF2A2A3A).copy(alpha = 0.50f) else Color(0xFF2A3A2A).copy(alpha = 0.28f)
    // Slightly darker birds during sunset for silhouette effect
    val silhouetteColor = if (timeOfDay == TimeOfDay.Sunset || timeOfDay == TimeOfDay.Dawn)
        Color(0xFF1A1A2A).copy(alpha = 0.55f)
    else
        birdColor

    // Flock size based on time of day
    val flockCount = when (timeOfDay) {
        TimeOfDay.Dawn, TimeOfDay.Sunset -> 5  // More birds at dawn/dusk
        TimeOfDay.Morning, TimeOfDay.Afternoon -> 3
        else -> 2
    }

    for (i in 0 until flockCount) {
        // Each bird has unique position, size, and speed
        val phaseOffset = i.toFloat() / flockCount
        val birdX = ((progress * 1.2f + phaseOffset * 0.7f) % 1.4f - 0.2f) * size.width
        val birdY = size.height * (0.12f + phaseOffset * 0.06f + sin(progress * 2f + i * 1.3f) * 0.02f)
        val birdSize = (6f + phaseOffset * 4f) * (0.7f + sin(progress * 1.5f + i) * 0.3f)
        val wingFlap = sin(progress * 8f + i * 2.1f) * 0.3f + 0.5f  // 0..1 wing cycle

        // Bird body is a V-shape
        val wingExtent = birdSize * (0.5f + wingFlap * 0.3f)
        val bodyLength = birdSize * 0.6f

        // Left wing
        drawLine(
            color = silhouetteColor,
            start = Offset(birdX - bodyLength * 0.5f, birdY + wingExtent * 0.3f),
            end = Offset(birdX, birdY),
            strokeWidth = 1.2f
        )
        // Right wing
        drawLine(
            color = silhouetteColor,
            start = Offset(birdX, birdY),
            end = Offset(birdX + bodyLength * 0.5f, birdY + wingExtent * 0.3f),
            strokeWidth = 1.2f
        )
        // Tail
        drawLine(
            color = silhouetteColor.copy(alpha = silhouetteColor.alpha * 0.7f),
            start = Offset(birdX, birdY),
            end = Offset(birdX, birdY + birdSize * 0.2f),
            strokeWidth = 0.8f
        )
    }
}

// ══════════��═══════════════════════════════════════════════════════════
//  Aurora Borealis — Wavy glowing bands across the night sky
// ══════════════════════════════════════════════════════════════════════

/**
 * Draw gorgeous aurora borealis effects — translucent wavy bands of green,
 * purple, and teal that drift and pulse across the upper sky.
 */
/**
 * Draw vibrant aurora borealis with layered ribbons, vertical streaks,
 * and atmospheric depth. Uses 5 color bands with independent wave motion
 * for a rich, immersive northern-lights effect.
 */
private fun DrawScope.drawAurora(auroraProgress: Float, auroraBrightness: Float, isDarkTheme: Boolean) {
    if (!isDarkTheme) return

    val baseAlpha = 0.25f * auroraBrightness

    // Aurora color bands — vibrant green core with teal/purple edges
    val auroraColors = listOf(
        Color(0xFF66FFAA).copy(alpha = baseAlpha * 0.6f),  // Bright green
        Color(0xFF44DDAA).copy(alpha = baseAlpha * 1.0f),  // Deep green
        Color(0xFF22CC88).copy(alpha = baseAlpha * 1.4f),  // Core green
        Color(0xFF44DDFF).copy(alpha = baseAlpha * 0.8f),  // Teal
        Color(0xFF8844FF).copy(alpha = baseAlpha * 0.4f)   // Purple
    )

    // Three ribbon bands at different heights with independent wave motion
    for (band in 0 until 3) {
        val bandHeight = 0.08f + band * 0.06f + sin(auroraProgress * 2f + band * 1.5f) * 0.015f
        val topY = size.height * (0.05f + bandHeight)
        val bottomY = size.height * (0.05f + bandHeight + 0.045f + cos(auroraProgress * 1.5f + band * 2.0f) * 0.02f)

        val path = Path()
        val segs = 60
        val waveFreq = 3.5f + band * 1.2f
        val waveOffset = auroraProgress * 4f + band * 2.0f

        // Top edge of ribbon
        path.moveTo(-20f, topY)
        for (i in 0..segs) {
            val t = i.toFloat() / segs
            val px = t * (size.width + 40f) - 20f
            val wave = sin(t * waveFreq * PI.toFloat() + waveOffset) * size.height * 0.025f +
                sin(t * 7f + waveOffset * 1.5f) * size.height * 0.012f +
                cos(t * 12f + waveOffset * 0.7f) * size.height * 0.008f
            path.lineTo(px, topY + wave)
        }
        // Bottom edge of ribbon (reverse)
        for (i in segs downTo 0) {
            val t = i.toFloat() / segs
            val px = t * (size.width + 40f) - 20f
            val wave = sin(t * waveFreq * PI.toFloat() + waveOffset) * size.height * 0.025f +
                sin(t * 7f + waveOffset * 1.5f) * size.height * 0.012f +
                cos(t * 12f + waveOffset * 0.7f) * size.height * 0.008f
            path.lineTo(px, bottomY + wave * 0.6f)
        }
        path.close()

        // Draw ribbon with gradient for atmospheric fade at edges
        drawPath(path, auroraColors[band], style = Fill)

        // Draw bright core line through ribbon center
        val corePath = Path()
        for (i in 0..segs) {
            val t = i.toFloat() / segs
            val px = t * (size.width + 40f) - 20f
            val wave = sin(t * waveFreq * PI.toFloat() + waveOffset) * size.height * 0.025f +
                sin(t * 7f + waveOffset * 1.5f) * size.height * 0.012f
            val cy = (topY + bottomY) / 2f + wave * 0.8f
            if (i == 0) corePath.moveTo(px, cy) else corePath.lineTo(px, cy)
        }
        drawPath(corePath, Color.White.copy(alpha = baseAlpha * 0.3f), style = Stroke(width = 1.5f))
    }

    // Vertical aurora pillars (shimmering light rays)
    for (i in 0 until 8) {
        val px = size.width * (0.08f + i * 0.12f) + sin(auroraProgress * 2f + i * 2.5f) * size.width * 0.04f
        val pillarHeight = (0.5f + sin(auroraProgress * 1.5f + i * 1.2f) * 0.5f) * size.height * 0.15f
        val pillarAlpha = (0.3f + sin(auroraProgress * 2.5f + i * 3.0f) * 0.7f) * baseAlpha
        val pillarColor = auroraColors[i % auroraColors.size].copy(alpha = pillarAlpha.coerceIn(0f, 0.3f))

        drawRect(
            color = pillarColor,
            topLeft = Offset(px - 2f, size.height * 0.04f),
            size = Size(4f, pillarHeight)
        )
    }

    // Subtle horizontal glow band connecting the aurora
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                auroraColors[0].copy(alpha = baseAlpha * 0.08f),
                Color.Transparent
            ),
            startY = size.height * 0.06f,
            endY = size.height * 0.22f
        ),
        size = size
    )
}
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

private fun DrawScope.drawRainbow(
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay = TimeOfDay.Midday
) {
    if (compact) return
    // Rainbows don't appear at night or twilight
    if (timeOfDay == TimeOfDay.Night || timeOfDay == TimeOfDay.Twilight) return

    val horizonY = size.height * 0.82f
    val rainbowCx = size.width * 0.50f
    val maxRadius = size.width * 0.28f
    val bandWidth = maxRadius * 0.09f

    val rainbowColors = listOf(
        Color(0xFFFF0000),  // Red
        Color(0xFFFF7F00),  // Orange
        Color(0xFFFFFF00),  // Yellow
        Color(0xFF00EE44),  // Green
        Color(0xFF00AAFF),  // Cyan
        Color(0xFF4040FF),  // Blue
        Color(0xFF8000C0)   // Violet
    )

    // Draw overlapping bands with decreasing alpha toward the ends,
    // creating a soft atmospheric rainbow that blends into the sky
    for (i in rainbowColors.indices) {
        val bandRadius = maxRadius - i * bandWidth
        val outerR = bandRadius + bandWidth * 0.5f
        val innerR = bandRadius - bandWidth * 0.5f

        val path = Path()
        val startAngle = -PI.toFloat() * 0.05f
        val endAngle = PI.toFloat() * 1.05f
        val segs = 80

        // Outer arc
        path.moveTo(
            rainbowCx + cos(startAngle) * outerR,
            horizonY - sin(startAngle).coerceAtLeast(0f) * outerR
        )
        for (s in 0..segs) {
            val theta = startAngle + (endAngle - startAngle) * s / segs
            path.lineTo(
                rainbowCx + cos(theta) * outerR,
                horizonY - sin(theta).coerceAtLeast(0f) * outerR
            )
        }
        // Inner arc (reverse)
        for (s in segs downTo 0) {
            val theta = startAngle + (endAngle - startAngle) * s / segs
            path.lineTo(
                rainbowCx + cos(theta) * innerR,
                horizonY - sin(theta).coerceAtLeast(0f) * innerR
            )
        }
        path.close()

        // Alpha fades for outer bands — soft atmospheric falloff
        val bandAlpha = 0.40f - i * 0.040f
        val color = rainbowColors[i].copy(alpha = bandAlpha.coerceIn(0.06f, 0.40f))
        drawPath(path, color, style = Fill)
    }

    // Soft outer glow blending rainbow into sky
    drawCircle(
        color = Color.White.copy(alpha = 0.025f),
        radius = maxRadius + bandWidth * 3f,
        center = Offset(rainbowCx, horizonY)
    )
}


/**
 * Draws subtle glowing firefly/bioluminescence particles near the ground.
 * Particles float upward with slow drift and pulse with a warm yellow-green glow.
 */
/**
 * Draws subtle glowing firefly/bioluminescence particles near the ground.
 * Particles float upward with slow drift and pulse with a warm yellow-green glow.
 * Three layers of fireflies create depth: far (small, dim) / mid / near (large, bright).
 */
/**
 * Draw a beautiful animated sea/ocean view with gentle waves,
 * sunset/dawn light reflections on the water, and a soft horizon glow.
 * Designed specifically for the EveningScene transition times.
 */
private fun DrawScope.drawEveningSea(
    palette: WeatherPalette,
    isDawn: Boolean,
    waveProgress: Float
) {
    val horizonY = size.height * 0.82f
    val seaTop = horizonY - size.height * 0.01f

    // Sea base colors derived from the evening palette
    val shallowColor = palette.tertiary.copy(
        red = (palette.tertiary.red * 0.3f + 0.1f),
        green = (palette.tertiary.green * 0.4f + 0.15f),
        blue = (palette.tertiary.blue * 0.6f + 0.3f),
        alpha = 0.35f
    )
    val deepColor = palette.primary.copy(
        red = palette.primary.red * 0.25f,
        green = palette.primary.green * 0.30f,
        blue = palette.primary.blue * 0.40f + 0.15f,
        alpha = 0.45f
    )
    val horizonColor = if (isDawn) {
        Color(0xFFFFCC88).copy(alpha = 0.15f)  // Warm dawn glow
    } else {
        Color(0xFF8855AA).copy(alpha = 0.12f)   // Purple twilight glow
    }

    // ── Main sea body (deep gradient from horizon to foreground) ──
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                horizonColor,
                shallowColor,
                deepColor
            ),
            startY = seaTop,
            endY = size.height
        ),
        topLeft = Offset(0f, seaTop),
        size = Size(size.width, size.height - seaTop)
    )

    // ── Gentle wave animations (3 layers of sine waves) ──
    val waveColors = listOf(
        Color.White.copy(alpha = 0.04f),
        shallowColor.copy(alpha = 0.08f),
        deepColor.copy(alpha = 0.06f)
    )

    for (layer in 0..2) {
        val wavePath = Path()
        val amplitude = 2f + layer * 3f
        val frequency = 3f + layer * 1.5f
        val speed = waveProgress * (0.5f + layer * 0.3f)
        val yOffset = seaTop + size.height * (0.02f + layer * 0.06f)

        wavePath.moveTo(-5f, yOffset)

        for (i in 0..60) {
            val t = i.toFloat() / 60f
            val px = t * (size.width + 10f) - 5f
            val wave = sin(t * frequency * PI.toFloat() * 2f + speed * 4f) * amplitude +
                sin(t * frequency * 4f + speed * 2.5f) * amplitude * 0.4f
            wavePath.lineTo(px, yOffset + wave)
        }
        wavePath.lineTo(size.width + 5f, yOffset + amplitude * 2f)
        wavePath.lineTo(size.width + 5f, size.height + 5f)
        wavePath.lineTo(-5f, size.height + 5f)
        wavePath.close()

        drawPath(wavePath, waveColors[layer], style = Fill)
    }

    // ── Sun/moon reflection path on the water ──
    val reflectColor = if (isDawn) {
        palette.sunColor.copy(alpha = 0.10f)
    } else {
        palette.moonColor.copy(alpha = 0.06f)
    }
    val reflectCx = size.width * 0.85f

    // Vertical reflection streak (sun/moon glint on water)
    for (i in 0..8) {
        val ry = seaTop + size.height * (0.02f + i * 0.03f)
        val rw = size.width * (0.04f - i * 0.004f).coerceAtLeast(0.01f)
        val rAlpha = (0.12f - i * 0.012f).coerceAtLeast(0.02f)
        val reflectWave = sin(waveProgress * 3f + i * 1.5f) * rw * 0.3f

        drawLine(
            color = reflectColor.copy(alpha = rAlpha),
            start = Offset(reflectCx - rw + reflectWave, ry),
            end = Offset(reflectCx + rw + reflectWave, ry),
            strokeWidth = 1.5f - i * 0.15f,
            cap = StrokeCap.Round
        )
    }

    // ── Horizon glow band (blends sky into sea) ──
    val glowColor = if (isDawn) {
        palette.sunGlowColor.copy(alpha = 0.08f)
    } else {
        palette.moonGlowColor.copy(alpha = 0.05f)
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(glowColor, Color.Transparent),
            startY = seaTop - size.height * 0.02f,
            endY = seaTop + size.height * 0.04f
        ),
        topLeft = Offset(0f, seaTop - size.height * 0.02f),
        size = Size(size.width, size.height * 0.06f)
    )

    // ── Subtle sparkle/glint dots on wave crests ──
    for (i in 0..12) {
        val seed = i * 1.234f
        val sx = (seed * 0.618f % 1f) * size.width
        val sy = seaTop + size.height * (0.03f + (seed * 0.382f % 1f) * 0.10f)
        val glint = sin(waveProgress * 5f + seed * 7f) * 0.5f + 0.5f
        val glintAlpha = glint * 0.06f

        drawCircle(
            color = Color.White.copy(alpha = glintAlpha),
            radius = 1f + glint * 2f,
            center = Offset(sx, sy)
        )
    }
}
private fun DrawScope.drawFireflies(
    progress: Float,
    isDarkTheme: Boolean,
    compact: Boolean
) {
    if (!isDarkTheme) return

    val count = if (compact) 12 else 24
    val fireflyColors = listOf(
        Color(0xFFFFF9C4),  // Warm yellow
        Color(0xFFD4F0A0),  // Yellow-green
        Color(0xFFA8E6A0),  // Soft green
        Color(0xFF80D0C0),  // Cyan-tinted
        Color(0xFFFFE0A0)   // Orange-yellow
    )

    // Three layers: far (0.55-0.65h), mid (0.65-0.78h), near (0.78-0.95h)
    for (i in 0 until count) {
        val seed = i * 1.618f
        val layer = (i % 3).toFloat() / 3f  // 0, 0.33, 0.67

        val baseX = ((i * 0.618f) % 1f) * size.width
        val baseY = size.height * (0.55f + layer * 0.38f)

        // Horizontal drift — each firefly has a unique path
        val driftX = sin(progress * 2f + seed) * size.width * 0.04f +
            sin(progress * 5f + seed * 1.5f) * size.width * 0.02f

        // Vertical floating — gentle upward then slow drift
        val floatY = sin(progress * 3f + seed) * size.height * 0.015f +
            cos(progress * 1.7f + seed * 0.7f) * size.height * 0.01f

        // Pulsing glow — random phase per firefly
        val pulse = (sin(progress * 8f + seed * 6.28f) * 0.5f + 0.5f).pow(1.5f)
        val glowIntensity = 0.15f + pulse * 0.85f

        val px = baseX + driftX
        val py = baseY + floatY

        // Layer-based sizing and alpha
        val layerScale = 0.5f + layer * 0.5f
        val layerAlpha = 0.3f + layer * 0.5f

        val color = fireflyColors[i % fireflyColors.size]
        val alpha = glowIntensity * layerAlpha * 0.8f

        // Outer glow — large, diffuse
        drawCircle(
            color = color.copy(alpha = alpha * 0.15f),
            radius = (4f + glowIntensity * 6f) * layerScale,
            center = Offset(px, py)
        )
        // Inner glow — medium
        drawCircle(
            color = color.copy(alpha = alpha * 0.4f),
            radius = (2f + glowIntensity * 3f) * layerScale,
            center = Offset(px, py)
        )
        // Bright core — small, intense
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.6f),
            radius = (0.8f + glowIntensity * 1.5f) * layerScale,
            center = Offset(px, py)
        )
    }

    // Subtle ambient glow near the ground from the collective fireflies
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                fireflyColors[0].copy(alpha = 0.02f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.9f),
            radius = size.maxDimension * 0.35f
        ),
        size = size
    )
}
/**
 * Dense volumetric fog scene with multiple depth layers.
 * Fog billows drift at independent speeds for a cinematic immersive effect.
 * Weather code 48 (rime fog) adds ice crystal sparkles.
 */
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
    val fogDrift1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "fogDrift1"
    )
    val fogDrift2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "fogDrift2"
    )
    val fogDrift3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "fogDrift3"
    )
    val fogPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fogPulse"
    )
    val iceGlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "iceGlow"
    )
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fogTreeSway"
    )
    val isRimeFog = weatherCode == 48

    val fogBaseColor = if (isDark) Color(0xFF3A3A4A) else Color(0xFFD8D8E0)
    val fogMidColor = if (isDark) Color(0xFF484858) else Color(0xFFE0E0E8)
    val fogLightColor = if (isDark) Color(0xFF505060) else Color(0xFFE8E8F0)

    val fogWispColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.20f else 0.15f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Volumetric background fog gradient ──
        val fogAlpha = 0.3f + 0.5f * fogPulse
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    fogBaseColor.copy(alpha = 0.6f * fogAlpha),
                    fogMidColor.copy(alpha = 0.5f * fogAlpha),
                    fogLightColor.copy(alpha = 0.4f * fogAlpha)
                ),
                startY = 0f, endY = size.height
            ),
            size = size
        )

        // ── Far fog wisps (large, slow, very faint) ──
        val farAlpha = 0.08f * fogPulse
        for (i in 0..3) {
            val x = ((fogDrift1 * 0.3f + i * 0.4f) % 1f) * (size.width * 1.3f) - size.width * 0.15f
            val y = size.height * (0.2f + i * 0.18f)
            val w = size.width * 0.6f
            val h = size.height * 0.04f + sin(fogDrift1 * 2f + i) * size.height * 0.01f
            drawOval(
                color = fogWispColor.copy(alpha = farAlpha),
                topLeft = Offset(x - w / 2, y - h / 2),
                size = Size(w, h)
            )
        }

        // ── Mid fog wisps (medium, varied drift) ──
        val midAlpha = 0.14f * fogPulse
        for (i in 0..5) {
            val x = ((fogDrift2 * 0.5f + i * 0.35f) % 1f) * (size.width * 1.4f) - size.width * 0.2f
            val y = size.height * (0.35f + i * 0.12f)
            val w = size.width * (0.3f + sin(i * 2.5f) * 0.1f)
            val h = size.height * 0.025f + cos(fogDrift2 * 3f + i * 1.5f) * size.height * 0.008f
            drawOval(
                color = fogWispColor.copy(alpha = midAlpha),
                topLeft = Offset(x - w / 2, y - h / 2),
                size = Size(w, h)
            )
        }

        // ── Near fog wisps (small, fast, higher contrast) ──
        val nearAlpha = 0.20f * fogPulse
        for (i in 0..4) {
            val x = ((fogDrift3 * 0.8f + i * 0.45f) % 1f) * (size.width * 1.5f) - size.width * 0.25f
            val y = size.height * (0.55f + i * 0.10f)
            val w = size.width * (0.2f + cos(i * 1.3f) * 0.08f)
            val h = size.height * 0.015f + sin(fogDrift3 * 4f + i * 2.0f) * size.height * 0.006f
            drawOval(
                color = fogWispColor.copy(alpha = nearAlpha),
                topLeft = Offset(x - w / 2, y - h / 2),
                size = Size(w, h)
            )
        }

        // ── Deep ground fog layer (blankets the bottom) ──
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    fogLightColor.copy(alpha = 0.35f * fogAlpha),
                    fogMidColor.copy(alpha = 0.50f * fogAlpha)
                ),
                startY = size.height * 0.70f,
                endY = size.height
            ),
            size = size
        )

        // ── Ground terrain (barely visible through fog) ──
        drawGround(
            palette = palette,
            weatherCode = weatherCode,
            isDay = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight,
            isDark = isDark,
            compact = compact,
            treeMorph = treeSway
        )

        // ── Rime fog ice crystals (weather code 48) ──
        if (isRimeFog && !compact) {
            for (i in 0..30) {
                val seed = i * 1.234f
                val px = ((seed * 0.618f) % 1f) * size.width
                val py = ((seed * 0.382f) % 1f) * size.height
                val sparkle = sin(iceGlow * 2f + seed * 4f) * 0.5f + 0.5f
                val crystalSize = 1f + sparkle * 3f
                val crystalAlpha = 0.05f + sparkle * 0.25f

                val crystalColor = Color.White.copy(alpha = crystalAlpha)
                // Hexagonal crystal glow
                drawCircle(color = crystalColor, radius = crystalSize * 0.5f, center = Offset(px, py))
                // Cross glow for rime sparkle
                drawLine(color = crystalColor.copy(alpha = crystalAlpha * 0.5f),
                    start = Offset(px - crystalSize, py), end = Offset(px + crystalSize, py),
                    strokeWidth = 0.5f)
                drawLine(color = crystalColor.copy(alpha = crystalAlpha * 0.5f),
                    start = Offset(px, py - crystalSize), end = Offset(px, py + crystalSize),
                    strokeWidth = 0.5f)
            }
            // Extra ground fog glow for rime
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.85f),
                    radius = size.maxDimension * 0.4f
                ),
                size = size
            )
        }

        // ── Atmospheric haze ──
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = isDark, hazeAlpha = 0.3f)
    }
}
/**
 * Rain scene with 3D perspective rain streaks, dynamic wind gusts,
 * rain intensity variation, ground splash ripples, and wet puddle reflections.
 * Uses the time-of-day palette for atmospheric color integration.
 */
@Composable
private fun RainScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark

    // Rain parameters derived from WMO weather code
    val isHeavy = weatherCode in 62..67 || weatherCode in 82..82
    val isDrizzle = weatherCode in 51..55 || weatherCode in 80..81
    val streakCount = when {
        compact -> if (isHeavy) 60 else if (isDrizzle) 20 else 40
        else -> if (isHeavy) 120 else if (isDrizzle) 40 else 80
    }
    val rainSpeed = if (isHeavy) 1.3f else 0.8f
    val rainAlpha = if (isHeavy) 0.7f else 0.55f

    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween((2000 / rainSpeed).toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "rainFall"
    )
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "ripples"
    )
    val windGust by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "windGust"
    )
    val intensity by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "intensity"
    )
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rainTreeSway"
    )

    // Rain streak data — pre-computed positions and phases
    data class RainStreak(val x: Float, val phase: Float, val length: Float, val windAffinity: Float)
    val streaks = remember {
        val rng = Random(42)
        List(streakCount) {
            RainStreak(
                x = rng.nextFloat(),
                phase = rng.nextFloat() * 10f,
                length = 0.4f + rng.nextFloat() * 0.6f,
                windAffinity = 0.3f + rng.nextFloat() * 0.7f
            )
        }
    }

    // Physics-based particle system for enhanced rain dynamics
    val particleSystem = remember(streakCount) {
        PhysicsParticleSystem(
            canvasWidth = 1f,
            canvasHeight = 1f,
            maxParticles = streakCount,
            gravity = if (isHeavy) 0.25f else 0.15f,
            windForce = if (isHeavy) 0.08f else 0.05f,
            dragCoefficient = if (isHeavy) 0.015f else 0.02f
        )
    }

    // Rain color — blue-grey that blends with palette
    val rainColor = if (isDark) Color(0xFF8ABADA).copy(alpha = 0.55f * (0.6f + rainAlpha * 0.4f) * intensity)
        else Color(0xFF8ABADA).copy(alpha = 0.50f * (0.6f + rainAlpha * 0.4f) * intensity)

    // Light mode: darken background so rain is visible
    val overlayAlpha = if (!isDark) 0.12f else 0f

    // Night rain: moon glow animation
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rainNightMoonGlow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Moon and stars visible through rain clouds at night ──
        val isNightRain = timeOfDay == TimeOfDay.Night || timeOfDay == TimeOfDay.Twilight
        if (isNightRain) {
            drawMoon(palette, timeOfDay, moonGlow, compact)
            for (i in 0..24) {
                val x = (i * 0.039f + 0.021f * (i % 7)) % 1f
                val y = (i * 0.027f + 0.013f * (i % 5)) % 0.5f
                val twinkle = (sin(moonGlow * (1.8f + i * 0.7f) + i * 1.7f) * 0.5f + 0.5f).coerceIn(0.08f, 0.55f)
                val starColor = if (twinkle > 0.35f) Color(0xFFB3E5FC) else Color(0xFFFFF9C4)
                drawCircle(
                    color = starColor.copy(alpha = twinkle * 0.6f),
                    radius = 0.7f + twinkle * 1.2f,
                    center = Offset(x * size.width, y * size.height * 0.55f)
                )
            }
        }

        // ── Overhead clouds for drizzle/rain ──
        // Draw clouds so rain scenes have visible cloud cover overhead
        val cloudDrift = (rainProgress * 0.2f) % 1f
        val cloudScale = size.width * 0.5f
        val cloudColor = palette.cloudBaseColor.copy(alpha = if (isDark) 0.25f else 0.18f)
        val cloudDark = Color(0xFF4A5A6A).copy(alpha = if (isDark) 0.30f else 0.15f)
        // Back layer — high stratus clouds
        drawCloud(cloudDrift, size.width * 0.1f, size.height * 0.05f, cloudScale * 0.45f,
            cloudColor.copy(alpha = cloudColor.alpha * 0.6f), cloudDrift * 3f, CloudType.Stratus)
        drawCloud(cloudDrift * 0.7f, size.width * 0.4f, size.height * 0.08f, cloudScale * 0.4f,
            cloudColor.copy(alpha = cloudColor.alpha * 0.7f), cloudDrift * 4f, CloudType.Stratus)
        drawCloud(cloudDrift * 1.2f, size.width * 0.7f, size.height * 0.06f, cloudScale * 0.35f,
            cloudColor.copy(alpha = cloudColor.alpha * 0.5f), cloudDrift * 5f, CloudType.Stratus)
        // Mid layer — darker cumulus clouds
        drawCloud(cloudDrift * 0.5f, size.width * 0.2f, size.height * 0.2f, cloudScale * 0.3f,
            cloudDark.copy(alpha = cloudDark.alpha * 0.5f), cloudDrift * 2f, CloudType.Cumulus)
        drawCloud(cloudDrift * 0.9f - 1f, size.width * 0.5f, size.height * 0.22f, cloudScale * 0.28f,
            cloudDark.copy(alpha = cloudDark.alpha * 0.4f), cloudDrift * 3f, CloudType.Cumulus)

        // ── Dark overlay for light mode (makes rain visible) ──
        if (overlayAlpha > 0f) {
            drawRect(color = Color(0xFF1A1A2E).copy(alpha = overlayAlpha * intensity), size = size)
        }

        // ── 3D perspective rain streaks with physics-based dynamics ──
        val effectiveWind = windGust * 0.15f
        val fallSpeed = size.height * 0.9f * rainSpeed

        // Update physics system with current wind and intensity
        particleSystem.setWindDirection(effectiveWind)
        particleSystem.update(deltaTime = 0.016f, windGust = windGust * 0.3f)

        // Emit new rain particles continuously
        if (particleSystem.getParticleCount() < streakCount * 0.8f) {
            particleSystem.emitAtTop(
                count = 2,
                vx = effectiveWind * 0.3f,
                vy = rainSpeed * 3f,
                size = 0.008f,
                massRange = if (isHeavy) 1f to 1.4f else 0.8f to 1.2f
            )
        }

        // Render physics-based rain particles
        val activeParticles = particleSystem.getActiveParticles()
        for (particle in activeParticles) {
            val px = particle.x * size.width
            val py = particle.y * size.height
            val velocity = getVelocityMagnitude(particle.vx, particle.vy)

            // Perspective: particles near bottom are slightly wider
            val depthFactor = 0.6f + (py / size.height).coerceIn(0f, 1f) * 0.4f

            // Velocity-based streak length (faster particles = longer streaks)
            val streakLen = (4f + velocity * 8f) * depthFactor
            val streakWidth = 1.2f + depthFactor * 1.2f

            // Alpha modulated by intensity and particle visibility
            val alpha = rainColor.alpha * (0.5f + intensity * 0.5f) * depthFactor

            // Calculate streak direction from velocity
            val angle = getVelocityAngle(particle.vx, particle.vy)
            val dx = kotlin.math.cos(angle) * streakLen
            val dy = kotlin.math.sin(angle) * streakLen

            // Draw rain streak
            drawLine(
                color = rainColor.copy(alpha = alpha),
                start = Offset(px, py),
                end = Offset(px + dx, py + dy),
                strokeWidth = streakWidth,
                cap = StrokeCap.Round
            )
        }

        // ── Ground splash ripples (not in compact mode) ──
        if (!compact) {
            val groundY = size.height * 0.82f
            for (i in 0..8) {
                val ripplePhase = (rippleProgress + i * 0.12f) % 1f
                val rippleX = size.width * (0.1f + i * 0.11f) + sin(i * 3.7f) * size.width * 0.03f
                val rippleY = groundY + size.height * 0.03f + cos(i * 2.1f) * size.height * 0.015f
                val rippleRadius = ripplePhase * size.width * 0.06f
                val rippleAlpha = (1f - ripplePhase) * 0.15f * intensity

                if (rippleRadius > 1f && rippleAlpha > 0.01f) {
                    drawCircle(
                        color = Color.White.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = Offset(rippleX, rippleY),
                        style = Stroke(width = 0.8f)
                    )
                }

                // Splash crown droplets
                if (ripplePhase < 0.15f) {
                    val splashProgress = ripplePhase / 0.15f
                    for (d in 0..3) {
                        val angle = d * PI.toFloat() / 2f + i * 0.5f
                        val dist = splashProgress * size.width * 0.025f
                        val sx = rippleX + cos(angle) * dist
                        val sy = rippleY - splashProgress * size.height * 0.015f
                        val splashAlpha = (1f - splashProgress) * 0.3f * intensity
                        drawCircle(
                            color = Color.White.copy(alpha = splashAlpha),
                            radius = 0.8f,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }

        // ── Ground terrain ──
        drawGround(
            palette = palette,
            weatherCode = weatherCode,
            isDay = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight,
            isDark = isDark,
            compact = compact,
            treeMorph = treeSway
        )

        // ── Atmospheric haze ──
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = isDark, hazeAlpha = 0.4f)
    }
}

/**
 * Pre-computed rain streak data for deterministic random distribution.
 */
/**
 * Snow scene with layered snowfall, gentle drifting, sparkling crystals,
 * and depth-based snowflake sizing. Heavy snow (code 71-77, 85-86) adds
 * accumulation tint and denser flake coverage.
 */
@Composable
private fun SnowScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val isHeavy = weatherCode in 71..77 || weatherCode in 85..86
    val flakeCount = if (compact) (if (isHeavy) 50 else 30) else (if (isHeavy) 120 else 70)

    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowFall"
    )
    val windDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "windDrift"
    )
    val sparkleGlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkleGlow"
    )
    val sizeOscillation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sizeOsc"
    )
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "snowTreeSway"
    )

    data class Snowflake(val x: Float, val speed: Float, val baseSize: Float, val swayAmount: Float, val rotationSpeed: Float)
    val snowflakes = remember {
        val rng = Random(123)
        List(flakeCount) {
            Snowflake(
                x = rng.nextFloat(),
                speed = 0.4f + rng.nextFloat() * 0.6f,
                baseSize = 0.3f + rng.nextFloat() * 0.7f,
                swayAmount = 0.2f + rng.nextFloat() * 0.8f,
                rotationSpeed = 0.5f + rng.nextFloat() * 1.5f  // Per-flake rotation
            )
        }
    }

    // Physics-based snow particle system with Perlin noise drift
    val particleSystem = remember(flakeCount) {
        PhysicsParticleSystem(
            canvasWidth = 1f,
            canvasHeight = 1f,
            maxParticles = flakeCount,
            gravity = if (isHeavy) 0.08f else 0.05f,  // Snow falls slower than rain
            windForce = if (isHeavy) 0.12f else 0.08f,
            dragCoefficient = if (isHeavy) 0.035f else 0.04f  // Snow has more air resistance
        )
    }

    val flakeColor = if (isDark) Color(0xFFC8D8E8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update physics system with wind influence
        particleSystem.update(deltaTime = 0.016f, windGust = windDrift * 0.2f)

        // Emit new snow particles continuously
        if (particleSystem.getParticleCount() < flakeCount * 0.8f) {
            particleSystem.emitAtTop(
                count = 3,
                vx = windDrift * 0.1f,
                vy = if (isHeavy) 0.8f else 0.5f,
                size = 0.012f,
                massRange = if (isHeavy) 0.6f to 1.0f else 0.7f to 1.1f
            )
        }

        // ── Snowfall with physics-based Perlin noise drift and per-flake rotation ──
        for ((index, flake) in snowflakes.withIndex()) {
            val t = (snowProgress * flake.speed * 2f) % 1f
            val py = (t * (size.height + 30f)) - 15f

            // Perlin noise-based drift for more natural, organic motion
            val perlinDrift = perlinNoise(flake.x * 5f, t * 3f, windDrift * 2f)
            val driftX = perlinDrift * size.width * 0.08f * flake.swayAmount +
                sin(windDrift * 2.5f + flake.x * 10f) * size.width * 0.03f * flake.swayAmount
            val px = (flake.x * size.width + driftX + t * size.width * 0.01f) % (size.width + 20f) - 10f

            // Depth-based sizing — larger flakes appear closer, size affects speed
            val depth = 0.3f + flake.baseSize * 0.7f
            val flakeSize = (1f + flake.baseSize * 3f) * (0.7f + sizeOscillation * 0.3f)

            // Per-flake rotation for spinning effect
            val rotation = (snowProgress * flake.rotationSpeed * 2f + flake.x * 6.28f) % 6.28f

            // Alpha variation for natural look
            val alpha = flakeColor.alpha * (0.5f + depth * 0.5f) * (0.6f + 0.4f * sin(t * 10f))

            // Draw flake with rotation visualization (by drawing rotated 6-pointed star)
            drawCircle(
                color = flakeColor.copy(alpha = alpha.coerceIn(0f, 0.9f)),
                radius = flakeSize,
                center = Offset(px, py)
            )

            // Draw rotation arms (creates appearance of spinning snowflake)
            if (flakeSize > 1f && !compact) {
                repeat(6) { arm ->
                    val armAngle = (rotation + (arm * 1.047f)) // 60 degrees apart
                    val armLen = flakeSize * 1.2f
                    val armX = cos(armAngle) * armLen
                    val armY = sin(armAngle) * armLen
                    drawLine(
                        color = flakeColor.copy(alpha = alpha * 0.4f),
                        start = Offset(px, py),
                        end = Offset(px + armX, py + armY),
                        strokeWidth = flakeSize * 0.3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Sparkle highlight on larger flakes
            if (flake.baseSize > 0.5f && !compact) {
                val sparkle = sin(sparkleGlow * 2f + flake.x * 20f + rotation) * 0.5f + 0.5f
                val highlightAlpha = sparkle * 0.3f * alpha
                if (highlightAlpha > 0.05f) {
                    drawCircle(
                        color = Color.White.copy(alpha = highlightAlpha),
                        radius = flakeSize * 0.4f,
                        center = Offset(px - flakeSize * 0.3f, py - flakeSize * 0.3f)
                    )
                }
            }
        }

        // ── Floating sparkle particles (heavy snow) ──
        if (isHeavy && !compact) {
            for (i in 0..10) {
                val seed = i * 2.345f
                val px = ((seed * 0.618f) % 1f) * size.width
                val py = ((seed * 0.382f) % 1f) * size.height
                val sparkle = sin(sparkleGlow * 1.5f + seed * 3f) * 0.5f + 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = sparkle * 0.08f),
                    radius = 1f + sparkle * 2f,
                    center = Offset(px, py)
                )
            }
        }

        // ── Ground terrain with snow tint ──
        drawGround(
            palette = palette,
            weatherCode = weatherCode,
            isDay = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight,
            isDark = isDark,
            compact = compact,
            treeMorph = treeSway
        )

        // ── Foreground snow layer (larger, faster flakes in front) ──
        val frontCount = flakeCount / 4
        for (i in 0 until frontCount) {
            val seed = i * 3.141f
            val t = (snowProgress * 1.5f + seed) % 1f
            val py = (t * (size.height + 40f)) - 20f
            val px = ((seed * 0.382f + windDrift * 0.1f) % 1f) * size.width
            val frontSize = 3f + sin(seed + sizeOscillation * 5f) * 2f
            val frontAlpha = 0.15f * (0.5f + 0.5f * sin(t * 8f))
            drawCircle(
                color = flakeColor.copy(alpha = frontAlpha),
                radius = frontSize,
                center = Offset(px, py)
            )
        }

        // ── Atmospheric haze ──
        drawAtmosphericHaze(hazeColor = palette.hazeColor, isDark = isDark, hazeAlpha = 0.3f)
    }
}

/**
 * Pre-computed snowflake data for consistent visual distribution.
 */

private fun rememberSnowflakes(count: Int): List<FloatArray> {
    val rng = Random(42)
    return List(count) { floatArrayOf(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) }
}

/**
 * Thunderstorm scene with towering 3D cumulonimbus clouds, lightning
 * bolts that originate from inside the clouds, cloud interior flash/glow,
 * and thunder rumble expanding rings. Procedural branching bolts with
 * realistic timing and double-flash capability.
 */
@Composable
private fun ThunderstormScene(
    weatherCode: Int,
    palette: WeatherPalette,
    compact: Boolean,
    timeOfDay: TimeOfDay,
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark
    val isDaytime = timeOfDay != TimeOfDay.Night && timeOfDay != TimeOfDay.Twilight

    val infiniteTransition = rememberInfiniteTransition(label = "thunder")
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "stormDrift"
    )
    val treeSway by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stormTree"
    )

    // Lightning state
    var flashIntensity by remember { mutableStateOf(0f) }
    var boltSeed by remember { mutableStateOf(0f) }
    var boltCloudIndex by remember { mutableStateOf(0) }
    var hasDoubleFlash by remember { mutableStateOf(false) }
    var afterglow by remember { mutableStateOf(0f) }
    var cloudGlowIntensity by remember { mutableStateOf(0f) }
    var cloudGlowIndex by remember { mutableStateOf(0) }
    var thunderRingProgress by remember { mutableStateOf(0f) }

    // Pre-computed cloud positions (towering cumulonimbus)
    val cloudPositions = remember {
        listOf(
            0.20f to 0.15f,  // Left cloud
            0.50f to 0.10f,  // Center cloud (main)
            0.75f to 0.12f   // Right cloud
        )
    }

    // Storm cloud colors
    val cloudBaseColor = if (isDark) Color(0xFF2A2A3A).copy(alpha = 0.55f) else Color(0xFF3A3A4A).copy(alpha = 0.50f)
    val cloudDarkColor = if (isDark) Color(0xFF1A1A28).copy(alpha = 0.45f) else Color(0xFF2A2A38).copy(alpha = 0.40f)
    val cloudMidColor = if (isDark) Color(0xFF222236).copy(alpha = 0.50f) else Color(0xFF353548).copy(alpha = 0.45f)

    // Lightning timing
    LaunchedEffect(Unit) {
        while (true) {
            val delayMs = if (compact) 5000L + (8000..16000).random() else 2000L + (1000..5000).random()
            delay(delayMs)

            boltSeed = Random.nextFloat() * 100f
            boltCloudIndex = Random.nextInt(cloudPositions.size)
            hasDoubleFlash = Random.nextFloat() > 0.55f
            cloudGlowIndex = boltCloudIndex

            // Thunder rumble starts BEFORE the flash (deep rumbling from clouds)
            thunderRingProgress = 0.01f

            // Main flash
            cloudGlowIntensity = 1f
            flashIntensity = 1f
            delay(100)
            flashIntensity = 0.2f
            cloudGlowIntensity = 0.5f
            delay(50)
            flashIntensity = 0.7f
            cloudGlowIntensity = 0.8f
            delay(40)
            flashIntensity = 0f
            cloudGlowIntensity = 0.2f

            // Expanding thunder ring from cloud center
            for (ri in 0..20) { thunderRingProgress = 0.05f + ri * 0.045f; delay(60) }; thunderRingProgress = 0f

            // Double flash
            if (hasDoubleFlash) {
                delay(150 + Random.nextLong(250))
                flashIntensity = 0.5f
                cloudGlowIntensity = 0.7f
                delay(60)
                flashIntensity = 0f
                cloudGlowIntensity = 0.1f
            }

            afterglow = 1f
        }
    }

    // Afterglow fade
    LaunchedEffect(afterglow) {
        if (afterglow > 0f) {
            delay(1800)
            afterglow = 0f
            cloudGlowIntensity = 0f
            thunderRingProgress = 0f
        }
    }

    // Rain base layer
    RainScene(weatherCode = if (isDaytime) 65 else 63, palette = palette, compact = compact, timeOfDay = timeOfDay, modifier = modifier)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudScale = if (compact) size.width * 0.25f else size.width * 0.40f
        val drift = (cloudDrift * 0.3f) % 1f

        // ── Towering cumulonimbus clouds (rear layer) ──
        for (entry in cloudPositions.withIndex()) {
            val i = entry.index
            val (cloudCx, cy) = entry.value
            val baseX = ((cloudCx + drift * 0.8f + i * 0.15f) % 1f) * size.width
            val scaleMul = 0.8f + i * 0.15f
            val isLit = cloudGlowIndex == i && cloudGlowIntensity > 0f

            // Base cloud layer
            drawCumulus(
                baseX, size.height * cy, cloudScale * scaleMul,
                if (isLit) cloudMidColor.copy(
                    red = (cloudMidColor.red + 0.3f * cloudGlowIntensity).coerceAtMost(1f),
                    green = (cloudMidColor.green + 0.3f * cloudGlowIntensity).coerceAtMost(1f),
                    blue = (cloudMidColor.blue + 0.4f * cloudGlowIntensity).coerceAtMost(1f),
                    alpha = (cloudMidColor.alpha + 0.2f * cloudGlowIntensity).coerceAtMost(0.7f)
                ) else cloudMidColor,
                cloudDrift * 3f + i * 2f
            )

            // Darker anvil base
            drawCumulus(
                baseX - size.width * 0.04f, size.height * (cy + 0.04f), cloudScale * scaleMul * 1.1f,
                cloudDarkColor,
                cloudDrift * 2f + i * 1.5f
            )

            // Bright towering top (sunlight hitting upper turrets)
            val topColor = if (isDark) Color(0xFF3A3A50).copy(alpha = 0.35f) else Color(0xFF6A6A80).copy(alpha = 0.30f)
            drawCumulus(
                baseX + size.width * 0.02f, size.height * (cy - 0.05f), cloudScale * scaleMul * 0.7f,
                topColor,
                cloudDrift * 4f + i * 3f
            )

            // ── Cloud interior glow (lightning illuminating from within) ──
            if (isLit && cloudGlowIntensity > 0.05f) {
                val glowRadius = size.width * (0.08f + cloudGlowIntensity * 0.10f)
                drawCircle(
                    color = Color(0xFFAADDFF).copy(alpha = cloudGlowIntensity * 0.12f),
                    radius = glowRadius,
                    center = Offset(baseX, size.height * (cy + 0.02f))
                )
                drawCircle(
                    color = Color.White.copy(alpha = cloudGlowIntensity * 0.06f),
                    radius = glowRadius * 1.5f,
                    center = Offset(baseX, size.height * (cy + 0.02f))
                )
            }
        }

        // ── Storm darkness overlay ──
        val stormDark = Color(0xFF0A0A18).copy(alpha = (0.22f + afterglow * 0.12f) * (1f - flashIntensity * 0.4f))
        drawRect(color = stormDark, size = size)

        // ── Full screen flash ──
        if (flashIntensity > 0.01f) {
            drawRect(color = Color.White.copy(alpha = flashIntensity * 0.22f), size = size)
        }

        // ── Thunder expanding ring from cloud origin ──
        if (thunderRingProgress > 0.01f) {
            val (tcx, tcy) = cloudPositions[boltCloudIndex]
            val ringCenter = Offset(tcx * size.width, tcy * size.height)
            val ringRadius = size.maxDimension * thunderRingProgress * 0.25f
            val ringAlpha = (1f - thunderRingProgress) * 0.06f
            drawCircle(
                color = Color(0xFFCCDDFF).copy(alpha = ringAlpha),
                radius = ringRadius,
                center = ringCenter,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha * 0.5f),
                radius = ringRadius * 0.8f,
                center = ringCenter,
                style = Stroke(width = 1.5f)
            )
        }

        // ── Lightning bolt (originates from inside the cloud) ──
        if (flashIntensity > 0f || afterglow > 0f) {
            val (cloudBaseX, cloudBaseY) = cloudPositions[boltCloudIndex]
            val startX = cloudBaseX * size.width + (Random(boltSeed.toInt()).nextFloat() - 0.5f) * size.width * 0.04f
            val startY = cloudBaseY * size.height + size.height * 0.04f

            val boltAlpha = (flashIntensity + afterglow * 0.25f).coerceIn(0f, 1f)
            val boltColor = Color.White.copy(alpha = boltAlpha)
            val glowColor = Color(0xFFAADDFF).copy(alpha = boltAlpha * 0.35f)

            val boltPath = Path()
            boltPath.moveTo(startX, startY)

            var bx = startX
            var by = startY
            for (segment in 0..14) {
                val nextX = bx + (Random(boltSeed.toInt() + segment).nextFloat() - 0.5f) * size.width * 0.05f
                val nextY = by + size.height * (0.05f + Random(boltSeed.toInt() + segment * 2).nextFloat() * 0.04f)
                boltPath.lineTo(nextX, nextY)
                bx = nextX
                by = nextY
            }

            // Glow aura
            drawPath(boltPath, glowColor, style = Stroke(width = 10f * boltAlpha, cap = StrokeCap.Round))
            drawPath(boltPath, glowColor.copy(alpha = glowColor.alpha * 0.5f), style = Stroke(width = 20f * boltAlpha, cap = StrokeCap.Round))
            // Bright core
            drawPath(boltPath, boltColor, style = Stroke(width = 2.5f * boltAlpha, cap = StrokeCap.Round))

            // Branch forks
            for (branch in 0..4) {
                val forkPoint = branch * 3 + 1
                if (forkPoint > 14) continue
                val branchPath = Path()
                var fbx = startX
                var fby = startY
                for (seg in 0 until forkPoint) {
                    val nextX = fbx + (Random(boltSeed.toInt() + seg * 3).nextFloat() - 0.5f) * size.width * 0.05f
                    val nextY = fby + size.height * (0.05f + Random(boltSeed.toInt() + seg * 5).nextFloat() * 0.04f)
                    fbx = nextX
                    fby = nextY
                }
                branchPath.moveTo(fbx, fby)
                for (seg in 0..2) {
                    val nextX = fbx + (Random(boltSeed.toInt() + branch * 10 + seg).nextFloat() - 0.6f) * size.width * 0.04f
                    val nextY = fby + size.height * (0.02f + Random(boltSeed.toInt() + branch * 10 + seg * 2).nextFloat() * 0.03f)
                    branchPath.lineTo(nextX, nextY)
                    fbx = nextX
                    fby = nextY
                }
                drawPath(branchPath, glowColor.copy(alpha = glowColor.alpha * 0.4f), style = Stroke(width = 5f * boltAlpha))
                drawPath(branchPath, boltColor.copy(alpha = boltAlpha * 0.5f), style = Stroke(width = 1.2f * boltAlpha))
            }

            // Reflection on ground
            if (boltAlpha > 0.1f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = boltAlpha * 0.06f), Color.Transparent),
                        center = Offset(startX, size.height * 0.84f),
                        radius = size.width * 0.12f
                    ),
                    topLeft = Offset(startX - size.width * 0.12f, size.height * 0.83f),
                    size = Size(size.width * 0.24f, size.height * 0.15f)
                )
            }
        }

        // ── Edge vignette ──
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF0A0A1E).copy(alpha = 0.12f)),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.maxDimension * 0.35f
            ),
            size = size
        )
    }
}

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
        compact = true,
        showCloudAnimation = true
    )
}

/**
 * Draw realistic fractal lightning bolt with branching
 */
private fun DrawScope.drawLightningBolt(
    bolt: LightningBolt,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val baseColor = Color(0xFFFFFFFF).copy(alpha = 0.9f * bolt.intensity)
    val glowColor = Color(0xFF87CEEB).copy(alpha = 0.4f * bolt.intensity)  // Sky blue glow
    
    // Draw main bolt with glow effect
    drawLightningPath(bolt.startX * canvasWidth, bolt.startY * canvasHeight, 
                     bolt.endX * canvasWidth, bolt.endY * canvasHeight,
                     width = 8f, color = glowColor)
    
    // Draw bright core
    drawLightningPath(bolt.startX * canvasWidth, bolt.startY * canvasHeight,
                     bolt.endX * canvasWidth, bolt.endY * canvasHeight,
                     width = 3f, color = baseColor)
    
    // Draw branches recursively
    bolt.branches.forEach { branch ->
        drawLightningBolt(branch, canvasWidth, canvasHeight)
    }
}

/**
 * Helper to draw a lightning segment with anti-aliasing
 */
private fun DrawScope.drawLightningPath(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    width: Float,
    color: Color
) {
    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}
