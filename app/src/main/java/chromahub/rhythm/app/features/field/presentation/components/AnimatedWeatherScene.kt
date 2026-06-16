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
    val isDark = FieldMindTheme.colors.isDark
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

    // Sky and cloud colors derived from palette
    val cloudColor = Color.White.copy(alpha = if (isDark) 0.30f else 0.25f)
    val cloudColorDark = palette.primary.copy(alpha = if (isDark) 0.20f else 0.15f)
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

        // Tree line silhouette gently swaying
        drawTreeLine(morph = cloudMorph * 2f, isDark = isDark)

        // Atmospheric haze
        drawAtmosphericHaze(isDark, hazeAlpha = sunGlow * 0.6f)
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
    // Shooting star
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, delayMillis = 3000, easing = LinearEasing), RepeatMode.Restart),
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
        val cx = size.width / 2
        val cy = size.height * 0.3f
        val moonRadius = if (compact) size.minDimension * 0.12f else size.minDimension * 0.10f

        val moonBody = Color(0xFFECEFF1).copy(alpha = 1f)
        val moonGlowColor = if (isDark) Color(0xFFECEFF1) else Color(0xFFFFF9C4)
        val starBright = Color(0xFFB3E5FC)  // Blue-white for bright stars
        val starWarm = Color(0xFFFFF9C4)    // Warm white
        val nightCloudColor = if (isDark) Color(0xFF37474F).copy(alpha = 0.18f) else Color(0xFF78909C).copy(alpha = 0.12f)

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

        // Shooting star (visible during first 30% of animation, then reset)
        if (shootingStarProgress < 0.7f) {
            val ssProgress = shootingStarProgress / 0.7f
            drawShootingStar(
                progress = ssProgress,
                startX = size.width * 0.7f,
                startY = size.height * 0.1f,
                angleDeg = -25f + sin(moonGlow * 0.5f) * 5f
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
        drawAtmosphericHaze(isDark = true, hazeAlpha = moonGlow)
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
    // Shooting star for night mode
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, delayMillis = 2000, easing = LinearEasing), RepeatMode.Restart),
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

            // Ground terrain
            drawGround(weatherCode = 0, isDay = true, isDark = isDark, compact = compact)

            // Atmospheric haze
            drawAtmosphericHaze(isDark = false, hazeAlpha = sunGlow * 0.5f)
        } else {
            val moonBody = Color(0xFFECEFF1).copy(alpha = 1f)
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

            // Shooting star
            if (shootingStarProgress < 0.65f) {
                val ssProgress = shootingStarProgress / 0.65f
                drawShootingStar(
                    progress = ssProgress,
                    startX = size.width * 0.8f,
                    startY = size.height * 0.08f,
                    angleDeg = -20f
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
            drawAtmosphericHaze(isDark = true, hazeAlpha = 0.6f)
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
    val isDark = FieldMindTheme.colors.isDark
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

    val cloudColor = if (isDark) Color(0xFF90A4AE).copy(alpha = 0.35f) else Color(0xFFB0BEC5).copy(alpha = 0.30f)
    val cloudColorDark = if (isDark) Color(0xFF78909C).copy(alpha = 0.25f) else Color(0xFF90A4AE).copy(alpha = 0.20f)

    Canvas(modifier = modifier.fillMaxSize()) {
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

        // Ground terrain
        drawGround(weatherCode = 3, isDay = true, isDark = isDark, compact = compact)
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
 * Draw a gentle tree/bush silhouette along the bottom of the scene.
 * This grounds the scene and makes the weather feel anchored in a real landscape.
 */
private fun DrawScope.drawTreeLine(morph: Float = 0f, isDark: Boolean = false) {
    val baseColor = if (isDark) Color(0xFF0A0A1A).copy(alpha = 0.6f) else Color(0xFF2D3A1E).copy(alpha = 0.25f)
    val detailColor = if (isDark) Color(0xFF05050E).copy(alpha = 0.5f) else Color(0xFF1A2812).copy(alpha = 0.18f)
    val groundY = size.height * 0.88f

    // Main treeline — undulating silhouette
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
        val py = groundY - treeHeight
        path.lineTo(px, py)
    }
    path.lineTo(size.width + 10f, size.height + 10f)
    path.close()

    drawPath(path = path, color = baseColor, style = Fill)

    // Detail layer — scattered individual tree peaks
    val detailPath = Path()
    for (i in 0..12) {
        val t = (i.toFloat() / 12f) * size.width
        val peakNoise = sin(t * 0.01f + morph * 0.4f + i.toFloat()) * 0.5f + 0.5f
        if (peakNoise > 0.5f) {
            val px = t + 10f
            val treeBaseY = groundY - (0.3f + peakNoise * 0.5f) * size.height * 0.05f
            val treeTipY = treeBaseY - peakNoise * size.height * 0.03f
            detailPath.moveTo(px - 6f, treeBaseY)
            detailPath.lineTo(px, treeTipY)
            detailPath.lineTo(px + 6f, treeBaseY)
        }
    }
    drawPath(path = detailPath, color = detailColor, style = Fill)
}

/**
 * Draw subtle atmospheric haze — a very faint gradient overlay that adds depth.
 */
private fun DrawScope.drawAtmosphericHaze(isDark: Boolean, hazeAlpha: Float = 1f) {
    val hazeColor = if (isDark) Color(0xFFB3E5FC).copy(alpha = 0.03f * hazeAlpha) else Color(0xFFFFF9C4).copy(alpha = 0.02f * hazeAlpha)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(hazeColor, Color.Transparent),
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

    // Trail (fading line)
    val trailPath = Path()
    trailPath.moveTo(x, y)
    trailPath.lineTo(
        x - cos(rad) * trailLength * (1f - progress),
        y - sin(rad) * trailLength * (1f - progress)
    )
    drawPath(
        path = trailPath,
        color = Color.White.copy(alpha = (1f - progress) * 0.8f),
        style = Stroke(width = 2f * (1f - progress) + 0.5f)
    )

    // Bright head
    drawCircle(
        color = Color.White.copy(alpha = (1f - progress) * 0.95f),
        radius = 2.5f * (1f - progress) + 0.5f,
        center = Offset(x, y)
    )
    // Head glow
    drawCircle(
        color = Color(0xFFB3E5FC).copy(alpha = (1f - progress) * 0.3f),
        radius = 5f * (1f - progress) + 1f,
        center = Offset(x, y)
    )
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

    val fogBaseColor = if (isDark) Color(0xFF90A4AE) else Color(0xFFE0E6ED)
    val fogDarkColor = if (isDark) Color(0xFF607D8B) else Color(0xFFB0BEC5)

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
    val isDark = FieldMindTheme.colors.isDark
    val isHeavy = weatherCode >= 65 || weatherCode in 80..82
    val streakCount = if (compact) (if (isHeavy) 30 else 15) else (if (isHeavy) 80 else 50)
    val baseSpeed = if (isHeavy) 400f else 700f

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
    val windGust by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "windGust"
    )
    // Rain intensity pulsing for living feel
    val rainIntensity by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000 + Random.nextInt(2000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rainIntensity"
    )

    val rainColor = if (isDark)
        Color(0xFF81D4FA).copy(alpha = if (isHeavy) 0.50f else 0.35f)
    else
        Color(0xFFB3E5FC).copy(alpha = if (isHeavy) 0.45f else 0.30f)
    val streaks = rememberRainStreaks(streakCount)

    Canvas(modifier = modifier.fillMaxSize()) {
        val intensityAlpha = rainIntensity.coerceIn(0.4f, 1f)

        // Wind-blown rain streaks with varied intensity
        streaks.forEach { (x, speed, length) ->
            val windSway = windGust * 25f * intensityAlpha
            val y = (rainProgress * size.height * speed * intensityAlpha + x * size.height * 0.2f) % (size.height + length)
            val yEnd = y - length * intensityAlpha.coerceIn(0.7f, 1.2f)
            val xPos = x * size.width + windSway * (1f - y / size.height) * 0.5f
            val streakAlpha = (0.5f + intensityAlpha * 0.5f) * (0.7f + speed * 0.3f)
            drawLine(
                color = rainColor.copy(alpha = streakAlpha.coerceAtMost(1f)),
                start = Offset(xPos, y),
                end = Offset(xPos + windSway * 0.3f, yEnd),
                strokeWidth = if (isHeavy) 2.5f * intensityAlpha else 1.5f * intensityAlpha
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

        // Ground terrain with puddles (handled by drawGround, but we need to render it)
        drawGround(weatherCode = weatherCode, isDay = true, isDark = isDark, compact = compact)

        // Foreground rain layer (closer, more visible)
        val fgStreakCount = if (compact) 5 else 12
        for (i in 0 until fgStreakCount) {
            val fx = (rainProgress * size.width + i * size.width * 0.08f) % size.width
            val fy = (rainProgress * size.height * 1.5f + i * size.height * 0.02f) % (size.height + 30f)
            drawLine(
                color = rainColor.copy(alpha = 0.6f * intensityAlpha),
                start = Offset(fx, fy),
                end = Offset(fx + windGust * 5f, (fy - 30f * intensityAlpha)),
                strokeWidth = 3f * intensityAlpha
            )
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
    modifier: Modifier
) {
    val isDark = FieldMindTheme.colors.isDark

    // ── Thunder rumble visual state ──
    var rumbleAlpha by remember { mutableStateOf(0f) }
    var rumblePosition by remember { mutableStateOf(0f) }

    // Includes heavy rain effect
    RainScene(weatherCode = 65, palette = palette, compact = compact, modifier = modifier)

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
