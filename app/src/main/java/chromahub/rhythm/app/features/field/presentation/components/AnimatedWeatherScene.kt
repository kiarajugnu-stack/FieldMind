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
import kotlin.math.PI
import kotlin.random.Random
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme

/**
 * Animated weather scene that renders Canvas-drawn weather effects based on WMO weather codes.
 * Temperature drives the color palette; sunrise/sunset times determine day/night mode.
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
    compact: Boolean = false
) {
    val isDay = isDaytime(sunrise, sunset)
    val isDarkTheme = FieldMindTheme.colors.isDark
    val palette = weatherPalette(temperature, isDay, isDarkTheme)

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
            weatherCode == -1 -> DayCloudyScene(palette, compact, modifier)
            weatherCode == -2 -> NightSkyScene(palette, compact, modifier)
            weatherCode in 0..1 -> if (isDay) DayCloudyScene(palette, compact, modifier) else NightSkyScene(palette, compact, modifier)
            weatherCode in 2..3 -> CloudyScene(palette, compact, modifier)
            weatherCode in 45..48 -> FogScene(palette, compact, modifier)
            weatherCode in 51..67 || weatherCode in 80..82 -> RainScene(weatherCode, palette, compact, modifier)
            weatherCode in 71..77 || weatherCode in 85..86 -> SnowScene(weatherCode, palette, compact, modifier)
            weatherCode >= 95 -> ThunderstormScene(palette, compact, modifier)
            else -> ClearSkyScene(palette, isDay, compact, modifier)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Day/Night Detection
// ══════════════════════════════════════════════════════════════════════

private fun isDaytime(sunrise: String?, sunset: String?): Boolean {
    if (sunrise == null || sunset == null) return true // Default to day
    val now = System.currentTimeMillis()
    val sunriseMs = parseIsoTimeToMillis(sunrise)
    val sunsetMs = parseIsoTimeToMillis(sunset)
    if (sunriseMs == null || sunsetMs == null) return true
    return now in sunriseMs..sunsetMs
}

private fun parseIsoTimeToMillis(iso: String): Long? {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getDefault()
        sdf.parse(iso)?.time
    } catch (_: Exception) { null }
}

// ══════════════════════════════════════════════════════════════════════
//  Color Palette
// ══════════════════════════════════════════════════════════════════════

data class WeatherPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: List<Color>
)

private fun weatherPalette(temp: Double?, isDay: Boolean, isDarkTheme: Boolean): WeatherPalette {
    val tempC = temp ?: 20.0

    if (isDarkTheme) {
        // Dark theme: deep, rich, muted backgrounds — colors have substance on dark canvases
        val (top, bottom) = when {
            tempC < -5 -> Color(0xFF1A237E) to Color(0xFF303F9F)   // Freezing — deep indigo
            tempC < 0 -> Color(0xFF0D47A1) to Color(0xFF1565C0)    // Freezing — deep blue
            tempC < 10 -> Color(0xFF004D40) to Color(0xFF00695C)   // Cold — deep teal
            tempC < 20 -> Color(0xFF1B3A1B) to Color(0xFF2E5C2E)   // Cool — muted forest
            tempC < 30 -> Color(0xFF4E342E) to Color(0xFF5D4037)   // Warm — deep brown
            tempC < 38 -> Color(0xFF4E2A1A) to Color(0xFF6D3A1A)   // Hot — deep rust
            else -> Color(0xFF4A0000) to Color(0xFF6A1B1B)         // Extreme — deep crimson
        }
        val bgColors = if (isDay) listOf(top, bottom) else listOf(
            Color(0xFF0D0D2B),  // Very dark night sky top
            top.copy(alpha = 0.7f)
        )
        return WeatherPalette(
            primary = top,
            secondary = bottom,
            accent = Color(0xFFFFCC80),   // Warm amber accent visible on dark
            background = bgColors
        )
    } else {
        // Light theme: soft, muted, airy backgrounds — pastels with a subtle tint
        val (top, bottom) = when {
            tempC < -5 -> Color(0xFFE8EAF6) to Color(0xFFC5CAE9)   // Freezing — soft lavender
            tempC < 0 -> Color(0xFFE3F2FD) to Color(0xFFBBDEFB)    // Cold — soft sky blue
            tempC < 10 -> Color(0xFFE0F2F1) to Color(0xFFB2DFDB)   // Cool — soft teal
            tempC < 20 -> Color(0xFFF1F8E9) to Color(0xFFDCEDC8)   // Mild — soft green
            tempC < 30 -> Color(0xFFFFF8E1) to Color(0xFFFFECB3)   // Warm — soft amber
            tempC < 38 -> Color(0xFFFBE9E7) to Color(0xFFFFCCBC)   // Hot — soft coral
            else -> Color(0xFFFCE4EC) to Color(0xFFF8BBD0)         // Extreme — soft pink
        }
        val bgColors = if (isDay) listOf(top, bottom) else listOf(
            bottom.copy(red = bottom.red * 0.85f, green = bottom.green * 0.85f, blue = bottom.blue * 0.9f),
            top.copy(red = top.red * 0.7f, green = top.green * 0.7f, blue = top.blue * 0.8f, alpha = 0.8f)
        )
        return WeatherPalette(
            primary = top,
            secondary = bottom,
            accent = Color(0xFFFF8F00),   // Muted amber accent for light theme
            background = bgColors
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
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dayClouds")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate"
    )
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunGlow"
    )
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift2"
    )

    // Sky and cloud colors derived from palette
    val cloudColor = Color.White.copy(alpha = if (FieldMindTheme.colors.isDark) 0.30f else 0.25f)
    val cloudColorDark = palette.primary.copy(alpha = if (FieldMindTheme.colors.isDark) 0.20f else 0.15f)
    val sunBody = Color(0xFFFFF176).copy(alpha = 0.95f)
    val sunInner = Color(0xFFFFF9C4).copy(alpha = 0.85f)
    val rayColor = Color(0xFFFFF9C4).copy(alpha = 0.15f * sunGlow)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height * 0.35f
        val sunRadius = if (compact) size.minDimension * 0.12f else size.minDimension * 0.10f

        // Sun glow
        drawCircle(
            color = palette.accent.copy(alpha = sunGlow * 0.12f),
            radius = sunRadius * 2.5f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = sunGlow * 0.06f),
            radius = sunRadius * 3.5f,
            center = Offset(cx, cy)
        )

        // Sun rays — softer, fewer in dark mode
        val rayCount = if (compact) 6 else 10
        val rayLength = sunRadius * 2.0f
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
                strokeWidth = if (compact) 2f else 3f
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
            color = cloudColor.copy(alpha = 0.3f)
        )
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.55f,
            baseY = size.height * 0.12f,
            scale = size.width * 0.35f,
            color = cloudColor.copy(alpha = 0.25f)
        )

        // Front layer clouds
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.25f,
            baseY = size.height * 0.55f,
            scale = size.width * 0.5f,
            color = cloudColorDark.copy(alpha = 0.35f)
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.65f,
            baseY = size.height * 0.5f,
            scale = size.width * 0.4f,
            color = cloudColorDark.copy(alpha = 0.3f)
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.8f,
            baseY = size.height * 0.7f,
            scale = size.width * 0.3f,
            color = cloudColor.copy(alpha = 0.25f)
        )
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
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nightSky")
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "starTwinkle"
    )
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "nightClouds"
    )
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "moonGlow"
    )

    val starCount = if (compact) 25 else 60
    val stars = remember { rememberStarPositions(starCount) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height * 0.3f
        val moonRadius = if (compact) size.minDimension * 0.12f else size.minDimension * 0.10f

        val moonBody = Color(0xFFECEFF1).copy(alpha = if (FieldMindTheme.colors.isDark) 1f else 0.95f)
        val moonGlowColor = if (FieldMindTheme.colors.isDark) Color(0xFFECEFF1) else Color(0xFFFFF9C4)
        val starBright = Color(0xFFB3E5FC)  // Blue-white for bright stars
        val starWarm = Color(0xFFFFF9C4)    // Warm white
        val nightCloudColor = if (FieldMindTheme.colors.isDark) Color(0xFF37474F).copy(alpha = 0.18f) else Color(0xFF78909C).copy(alpha = 0.12f)

        // Moon glow ring
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.12f * moonGlow),
            radius = moonRadius * 3.0f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = moonGlowColor.copy(alpha = 0.04f * moonGlow),
            radius = moonRadius * 4.5f,
            center = Offset(cx, cy)
        )

        // Moon body
        drawCircle(
            color = moonBody,
            radius = moonRadius,
            center = Offset(cx, cy)
        )
        // Crescent shadow
        drawCircle(
            color = palette.background.last().copy(alpha = 0.65f),
            radius = moonRadius * 0.82f,
            center = Offset(cx + moonRadius * 0.2f, cy - moonRadius * 0.12f)
        )

        // Stars with bright twinkle
        stars.forEach { (x, y) ->
            val twinkle = (sin(starAlpha * 8f + x * 25f + y * 35f) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
            val starRadius = 1.0f + twinkle * 2.5f
            val starColor = when {
                twinkle > 0.85f -> starBright
                twinkle > 0.5f -> starWarm
                else -> Color.White
            }
            drawCircle(
                color = starColor.copy(alpha = twinkle * 0.95f),
                radius = starRadius,
                center = Offset(x * size.width, y * size.height)
            )
            // Cross glow on brightest stars
            if (twinkle > 0.8f && !compact) {
                drawLine(
                    color = starColor.copy(alpha = twinkle * 0.3f),
                    start = Offset(x * size.width - starRadius * 2, y * size.height),
                    end = Offset(x * size.width + starRadius * 2, y * size.height),
                    strokeWidth = 0.8f
                )
                drawLine(
                    color = starColor.copy(alpha = twinkle * 0.3f),
                    start = Offset(x * size.width, y * size.height - starRadius * 2),
                    end = Offset(x * size.width, y * size.height + starRadius * 2),
                    strokeWidth = 0.8f
                )
            }
        }

        // Drifting faint clouds at night
        drawCloud(
            offset = cloudOffset,
            baseX = size.width * 0.15f,
            baseY = size.height * 0.6f,
            scale = size.width * 0.4f,
            color = nightCloudColor
        )
        drawCloud(
            offset = cloudOffset,
            baseX = size.width * 0.55f,
            baseY = size.height * 0.75f,
            scale = size.width * 0.35f,
            color = nightCloudColor.copy(alpha = nightCloudColor.alpha * 0.7f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Clear Sky — Sun with rotating rays / Moon + drifting stars
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ClearSkyScene(
    palette: WeatherPalette,
    isDay: Boolean,
    compact: Boolean,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clearSky")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate"
    )
    // Sun pulse glow
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunGlow"
    )
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "starTwinkle"
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
    val particles = remember {
        val rng = Random(99)
        List(if (compact) 4 else 8) {
            Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 0.5f + 0.5f)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val sunRadius = if (compact) size.minDimension * 0.15f else size.minDimension * 0.12f

        if (isDay) {
            // Outer sun glow
            drawCircle(
                color = palette.accent.copy(alpha = sunGlow * 0.12f),
                radius = sunRadius * 2.2f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = sunGlow * 0.06f),
                radius = sunRadius * 3.0f,
                center = Offset(cx, cy)
            )

            // Sun rays
            val rayLength = sunRadius * 1.8f
            for (i in 0 until rayCount) {
                val angle = sunRotation + (360f / rayCount) * i
                val rad = (angle * PI.toFloat() / 180f)
                val x1 = cx + cos(rad) * sunRadius * 0.8f
                val y1 = cy + sin(rad) * sunRadius * 0.8f
                val x2 = cx + cos(rad) * rayLength
                val y2 = cy + sin(rad) * rayLength
                drawLine(
                    color = palette.accent.copy(alpha = 0.25f * sunGlow),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (compact) 2f else 3f
                )
            }

            // Sun body
            drawCircle(
                color = palette.accent,
                radius = sunRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = sunRadius * 0.6f,
                center = Offset(cx, cy)
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
        } else {
            val moonBody = Color(0xFFECEFF1).copy(alpha = if (isDark) 1f else 0.95f)
            val moonGlowColor = if (isDark) Color(0xFFECEFF1) else Color(0xFFFFF9C4)

            // Moon glow
            drawCircle(
                color = moonGlowColor.copy(alpha = 0.15f),
                radius = sunRadius * 2.5f,
                center = Offset(cx, cy)
            )

            // Moon
            drawCircle(
                color = moonBody,
                radius = sunRadius,
                center = Offset(cx, cy)
            )
            // Moon crescent shadow
            drawCircle(
                color = palette.background.last().copy(alpha = if (isDark) 0.75f else 0.7f),
                radius = sunRadius * 0.85f,
                center = Offset(cx + sunRadius * 0.2f, cy - sunRadius * 0.1f)
            )

            // Stars with twinkle
            stars.forEach { (x, y) ->
                val twinkle = (sin(starAlpha * 6f + x * 20f + y * 30f) * 0.5f + 0.5f).coerceIn(0.2f, 1f)
                val starColor = if (isDark) Color(0xFFB3E5FC).copy(alpha = twinkle * 0.95f) else Color.White.copy(alpha = twinkle * 0.85f)
                drawCircle(
                    color = starColor,
                    radius = 1.2f + twinkle * 1.8f,
                    center = Offset(x * size.width, y * size.height)
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
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift2"
    )
    val cloudOffset3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift3"
    )

    val isDark = FieldMindTheme.colors.isDark
    val cloudColor = if (isDark) Color(0xFF90A4AE).copy(alpha = 0.35f) else Color(0xFFB0BEC5).copy(alpha = 0.30f)
    val cloudColorDark = if (isDark) Color(0xFF78909C).copy(alpha = 0.25f) else Color(0xFF90A4AE).copy(alpha = 0.20f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudScale = if (compact) size.width * 0.4f else size.width * 0.5f
        val cloudScale2 = if (compact) size.width * 0.3f else size.width * 0.4f        // Back layer clouds (slow drift, seamless loop)
            val backDrift = cloudOffset1 % 1f
            drawCloud(
                offset = backDrift,
                baseX = size.width * 0.1f,
                baseY = size.height * 0.2f,
                scale = cloudScale,
                color = cloudColor.copy(alpha = 0.4f)
            )
            // Repeat to fill screen width for seamless loop
            drawCloud(
                offset = backDrift - 1f,
                baseX = size.width * 0.1f,
                baseY = size.height * 0.2f,
                scale = cloudScale,
                color = cloudColor.copy(alpha = 0.4f)
            )
            drawCloud(
                offset = backDrift,
                baseX = size.width * 0.5f,
                baseY = size.height * 0.15f,
                scale = cloudScale * 0.8f,
                color = cloudColor.copy(alpha = 0.3f)
            )
            drawCloud(
                offset = backDrift - 1f,
                baseX = size.width * 0.5f,
                baseY = size.height * 0.15f,
                scale = cloudScale * 0.8f,
                color = cloudColor.copy(alpha = 0.3f)
            )

            // Middle layer clouds (seamless loop)
            val midDrift = cloudOffset2 % 1f
            drawCloud(
                offset = midDrift,
                baseX = size.width * 0.3f,
                baseY = size.height * 0.35f,
                scale = cloudScale2,
                color = cloudColorDark.copy(alpha = 0.5f)
            )
            drawCloud(
                offset = midDrift - 1f,
                baseX = size.width * 0.3f,
                baseY = size.height * 0.35f,
                scale = cloudScale2,
                color = cloudColorDark.copy(alpha = 0.5f)
            )
            drawCloud(
                offset = midDrift,
                baseX = size.width * 0.7f,
                baseY = size.height * 0.3f,
                scale = cloudScale2 * 0.9f,
                color = cloudColorDark.copy(alpha = 0.4f)
            )
            drawCloud(
                offset = midDrift - 1f,
                baseX = size.width * 0.7f,
                baseY = size.height * 0.3f,
                scale = cloudScale2 * 0.9f,
                color = cloudColorDark.copy(alpha = 0.4f)
            )

            // Front layer clouds (fast drift, seamless loop)
            val frontDrift = cloudOffset3 % 1f
            drawCloud(
                offset = frontDrift,
                baseX = size.width * 0.15f,
                baseY = size.height * 0.7f,
                scale = cloudScale * 1.1f,
                color = cloudColor.copy(alpha = 0.6f)
            )
            drawCloud(
                offset = frontDrift - 1f,
                baseX = size.width * 0.15f,
                baseY = size.height * 0.7f,
                scale = cloudScale * 1.1f,
                color = cloudColor.copy(alpha = 0.6f)
            )
            drawCloud(
                offset = frontDrift,
                baseX = size.width * 0.6f,
                baseY = size.height * 0.65f,
                scale = cloudScale * 0.7f,
                color = cloudColor.copy(alpha = 0.5f)
            )
            drawCloud(
                offset = frontDrift - 1f,
                baseX = size.width * 0.6f,
                baseY = size.height * 0.65f,
                scale = cloudScale * 0.7f,
                color = cloudColor.copy(alpha = 0.5f)
            )
    }
}

private fun DrawScope.drawCloud(
    offset: Float,
    baseX: Float,
    baseY: Float,
    scale: Float,
    color: Color
) {
    val x = (baseX + offset * size.width) % (size.width + scale) - scale * 0.5f

    // Draw cloud as overlapping circles
    val circleRadius = scale * 0.15f
    drawCircle(color = color, radius = circleRadius, center = Offset(x, baseY))
    drawCircle(color = color, radius = circleRadius * 0.8f, center = Offset(x - circleRadius * 0.6f, baseY + circleRadius * 0.2f))
    drawCircle(color = color, radius = circleRadius * 0.7f, center = Offset(x + circleRadius * 0.7f, baseY - circleRadius * 0.1f))
    drawCircle(color = color, radius = circleRadius * 0.6f, center = Offset(x + circleRadius * 1.3f, baseY + circleRadius * 0.3f))
    drawCircle(color = color, radius = circleRadius * 0.5f, center = Offset(x - circleRadius * 1.1f, baseY + circleRadius * 0.1f))
}

// ══════════════════════════════════════════════════════════════════════
//  Fog — Translucent drifting fog bands
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FogScene(
    palette: WeatherPalette,
    compact: Boolean,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val fogOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "fogDrift"
    )
    val fogAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fogPulse"
    )

    val bandCount = if (compact) 3 else 5
    val fogColor = if (FieldMindTheme.colors.isDark)
        Color(0xFF90A4AE).copy(alpha = fogAlpha * 0.35f)
    else
        Color(0xFFB0BEC5).copy(alpha = fogAlpha * 0.25f)

    Canvas(modifier = modifier.fillMaxSize()) {

        for (i in 0 until bandCount) {
            val yBase = size.height * (0.2f + i * 0.15f)
            val xOffset = (fogOffset * size.width * 1.5f + i * size.width * 0.15f) % (size.width * 2f) - size.width * 0.5f
            val bandWidth = size.width * (1.2f + i * 0.1f)
            val bandHeight = size.height * (0.04f + i * 0.01f)

            // Fog band as a rounded rect
            drawRoundRect(
                color = fogColor.copy(alpha = fogAlpha * (0.3f + i * 0.1f)),
                topLeft = Offset(xOffset, yBase - bandHeight / 2),
                size = Size(bandWidth, bandHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bandHeight / 2)
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
    modifier: Modifier
) {
    val isHeavy = weatherCode >= 65 || weatherCode in 80..82
    val streakCount = if (compact) (if (isHeavy) 30 else 15) else (if (isHeavy) 70 else 40)
    val streakSpeed = if (isHeavy) 500f else 800f

    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(streakSpeed.toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "rainFall"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleAlpha"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleScale"
    )
    // Wind gust effect
    val windGust by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "windGust"
    )

    val rainColor = if (FieldMindTheme.colors.isDark)
        Color(0xFF81D4FA).copy(alpha = if (isHeavy) 0.45f else 0.30f)
    else
        Color(0xFFB3E5FC).copy(alpha = if (isHeavy) 0.40f else 0.25f)
    val streaks = rememberRainStreaks(streakCount)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Wind-blown rain streaks
        streaks.forEach { (x, speed, length) ->
            val windSway = windGust * 20f
            val y = (rainProgress * size.height + x * size.height * 0.3f * speed) % (size.height + length)
            val yEnd = y - length
            val xPos = x * size.width + windSway * (1f - y / size.height)
            drawLine(
                color = rainColor,
                start = Offset(xPos, y),
                end = Offset(xPos + windSway * 0.3f, yEnd),
                strokeWidth = if (isHeavy) 2.5f else 1.5f
            )
        }

        // Rain ripples on ground + splash particles
        if (!compact) {
            for (i in 0..5) {
                val rippleX = size.width * (0.1f + i * 0.16f)
                val rippleY = size.height * 0.95f
                // Expanding ring
                drawCircle(
                    color = rainColor.copy(alpha = rippleAlpha * 0.8f),
                    radius = 6f + rippleScale * 18f,
                    center = Offset(rippleX, rippleY),
                    style = Stroke(width = 1.5f)
                )
                // Splash dot at center
                drawCircle(
                    color = rainColor.copy(alpha = rippleAlpha * 0.5f),
                    radius = 2f,
                    center = Offset(rippleX, rippleY)
                )
                // Tiny splash particle
                val splashAngle = i * 60f + rainProgress * 360f
                val rad = splashAngle * PI.toFloat() / 180f
                val splashDist = 4f + rippleScale * 12f
                drawCircle(
                    color = rainColor.copy(alpha = rippleAlpha * 0.3f),
                    radius = 1.5f,
                    center = Offset(rippleX + cos(rad) * splashDist, rippleY - abs(sin(rad)) * splashDist * 0.5f)
                )
            }
        }
    }
}

private fun rememberRainStreaks(count: Int): List<Triple<Float, Float, Float>> {
    val seed = 123L
    val rng = Random(seed)
    return List(count) {
        Triple(rng.nextFloat(), 0.5f + rng.nextFloat(), 8f + rng.nextFloat() * 15f)
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
    modifier: Modifier
) {
    val isHeavy = weatherCode >= 75 || weatherCode == 86
    val flakeCount = if (compact) (if (isHeavy) 30 else 15) else (if (isHeavy) 60 else 35)

    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowFall"
    )
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowSway"
    )
    // Sparkle twinkle for snow
    val sparkleGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "sparkleGlow"
    )

    val flakes = rememberSnowflakes(flakeCount)
    val snowColor = if (FieldMindTheme.colors.isDark)
        Color.White.copy(alpha = if (isHeavy) 0.70f else 0.50f)
    else
        Color.White.copy(alpha = if (isHeavy) 0.60f else 0.40f)
    val sparkleColor = Color(0xFFE0F7FA)

    Canvas(modifier = modifier.fillMaxSize()) {
        flakes.forEach { (x, speed, flakeSize) ->
            val y = (snowProgress * size.height * speed + x * size.height * 0.15f) % (size.height + flakeSize * 3f)
            // Gentle figure-8 sway pattern
            val swayX = sin(swayOffset * 2f * Math.PI.toFloat() + x * size.width * 0.008f) * 20f
            val swayX2 = cos(swayOffset * 1.3f * Math.PI.toFloat() + x * size.width * 0.012f) * 8f
            val adjustedX = (x * size.width + swayX + swayX2).coerceIn(0f, size.width)

            // Draw snowflake (white circle)
            drawCircle(
                color = snowColor,
                radius = flakeSize,
                center = Offset(adjustedX, y)
            )

            // Sparkle highlight on larger flakes
            if (flakeSize > 3.5f) {
                val sparkleAlpha = (sin(sparkleGlow * 3f + x * 15f + y * 10f) * 0.5f + 0.5f) * 0.8f
                drawCircle(
                    color = sparkleColor.copy(alpha = sparkleAlpha * 0.6f),
                    radius = flakeSize * 0.4f,
                    center = Offset(adjustedX - 1.5f, y - 1.5f)
                )
            }
        }

        // Occasional sparkling particle floating
        if (!compact && isHeavy) {
            for (i in 0..3) {
                val sx = (sin(sparkleGlow * 2f + i * 1.7f) * 0.5f + 0.5f) * size.width
                val sy = (cos(sparkleGlow * 1.5f + i * 2.3f) * 0.5f + 0.5f) * size.height * 0.5f
                drawCircle(
                    color = sparkleColor.copy(alpha = sparkleGlow * 0.3f),
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
    modifier: Modifier
) {

    // Includes heavy rain effect
    RainScene(weatherCode = 65, palette = palette, compact = compact, modifier = modifier)

    // ── Lightning flash state (random timing, intensity, position) ──
    var flashAlpha by remember { mutableStateOf(0f) }
    val flashPosition = remember { mutableStateOf(Offset(0.3f, 0.1f)) }
    var flashIntensity by remember { mutableStateOf(0.3f) }
    
    // ── Lightning bolt paths computed from flash counter (stable for entire flash) ──
    // boltSeed increments each time a new flash starts directly in the main flash loop,
    // so the bolt path is fixed for the entire duration of a single flash — no frame-to-frame jitter.
    var boltSeed by remember { mutableStateOf(0) }

    // Seed the random generator once
    val rng = remember { Random(789) }

    // LaunchedEffect that triggers lightning flashes at random intervals
    LaunchedEffect(Unit) {
        while (true) {
            // Random wait between 2 and 6 seconds before next flash
            val nextFlashDelay = rng.nextLong(2000L, 6000L)
            delay(nextFlashDelay)

            // Randomize position — lightning can strike anywhere
            flashPosition.value = Offset(
                0.1f + rng.nextFloat() * 0.8f,  // x: 10%-90% across screen
                0.05f + rng.nextFloat() * 0.4f   // y: 5%-45% from top
            )

            // Randomize intensity — some flashes are bright, some subtle
            flashIntensity = 0.2f + rng.nextFloat() * 0.5f  // 0.2 to 0.7

            // Advance bolt seed before flash so Canvas draws stable path on first frame
            boltSeed++
            
            // Quick flash on (80-150ms)
            flashAlpha = 1f
            val flashOnDuration = rng.nextLong(80L, 150L)
            delay(flashOnDuration)

            // Quick fade out (100-200ms)
            val fadeSteps = rng.nextInt(3, 6)
            for (step in 1..fadeSteps) {
                flashAlpha = 1f - (step.toFloat() / fadeSteps)
                delay(rng.nextLong(20L, 40L))
            }
            flashAlpha = 0f

            // Sometimes do a double-flash (follow-up flash within 200-500ms)
            if (rng.nextFloat() < 0.35f) {
                delay(rng.nextLong(200L, 500L))
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

    // ── Glow afterglow state (follows flash with slower decay) ──
    var glowAlpha by remember { mutableStateOf(0f) }
    val glowPosition = remember { mutableStateOf(Offset(0.3f, 0.1f)) }

    LaunchedEffect(flashAlpha) {
        if (flashAlpha > 0.5f) {
            // Glow appears at the lightning strike position
            glowPosition.value = flashPosition.value
            glowAlpha = 0.4f
            delay(400)

            // Slow glow decay
            for (step in 1..8) {
                glowAlpha = 0.4f * (1f - step.toFloat() / 8f)
                delay(50)
            }
            glowAlpha = 0f
        }
    }

    // ── Lightning bolt path is seeded by boltSeed from above — stable for entire flash ──
    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Lightning bolt (jagged line from cloud to ground) ──
        if (flashAlpha > 0.3f) {
            val drawRng = Random(boltSeed)
            val boltX = flashPosition.value.x * size.width
            val startY = flashPosition.value.y * size.height
            val endY = size.height * (0.5f + drawRng.nextFloat() * 0.3f)

            // Primary bolt
            val boltPath = Path()
            boltPath.moveTo(boltX, 0f)
            var currentX = boltX
            var currentY = 0f
            val segments = 6 + drawRng.nextInt(4)
            for (i in 0 until segments) {
                val zigzagX = currentX + (drawRng.nextFloat() - 0.5f) * size.width * 0.08f
                val zigzagY = startY + (endY - startY) * ((i + 1f) / segments) + (drawRng.nextFloat() - 0.5f) * size.height * 0.03f
                boltPath.lineTo(zigzagX, zigzagY)
                currentX = zigzagX
                currentY = zigzagY
            }
            boltPath.lineTo(boltX + (drawRng.nextFloat() - 0.5f) * size.width * 0.05f, endY)

            drawPath(
                path = boltPath,
                color = Color.White.copy(alpha = flashAlpha * 0.9f),
                style = Stroke(width = 2f + flashIntensity * 4f)
            )

            // Secondary branches (smaller jagged offshoots)
            val branchCount = 2 + drawRng.nextInt(3)
            for (b in 0 until branchCount) {
                val branchPath = Path()
                val branchStart = startY + (endY - startY) * drawRng.nextFloat()
                val branchX = boltX + (drawRng.nextFloat() - 0.5f) * size.width * 0.15f
                branchPath.moveTo(boltX, branchStart)
                branchPath.lineTo(branchX, branchStart + (drawRng.nextFloat() - 0.5f) * size.height * 0.05f)
                branchPath.lineTo(branchX + (drawRng.nextFloat() - 0.5f) * size.width * 0.05f, branchStart + size.height * 0.06f)
                drawPath(
                    path = branchPath,
                    color = Color.White.copy(alpha = flashAlpha * 0.5f),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // ── Full-screen ambient flash ──
        if (flashAlpha > 0.3f) {
            drawRect(
                color = Color.White.copy(alpha = flashAlpha * 0.25f * flashIntensity),

                size = size
            )
        }


        // ── Screen-edge glow at lightning position ──
        if (glowAlpha > 0.01f) {
            val glowX = glowPosition.value.x * size.width
            val glowY = glowPosition.value.y * size.height
            val glowColor = if (FieldMindTheme.colors.isDark)
                Color(0xFFFFF9C4).copy(alpha = glowAlpha * 0.8f)
            else
                Color(0xFFFFF9C4).copy(alpha = glowAlpha)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        glowColor.copy(alpha = 0f),
                        Color.Transparent
                    ),
                    center = Offset(glowX, glowY),
                    radius = size.maxDimension * 0.6f
                ),
                size = size
            )

        }
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
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw background
        drawRect(
            brush = Brush.verticalGradient(palette.background),
            size = size
        )

        // Draw a static weather icon
        val cx = size.width / 2
        val cy = size.height / 2
        val r = size.minDimension * 0.15f

        when {
            weatherCode <= 1 -> {
                // Static sun
                drawCircle(color = palette.accent, radius = r, center = Offset(cx, cy))
            }
            weatherCode in 2..3 -> {
                // Static cloud
                val cloudColor = if (FieldMindTheme.colors.isDark) Color(0xFF90A4AE).copy(alpha = 0.45f) else Color(0xFFB0BEC5).copy(alpha = 0.40f)
                drawCloud(0f, cx - r, cy, r * 2f, cloudColor)
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
