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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val palette = weatherPalette(temperature, isDay)

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
        when {
            weatherCode in 0..1 -> ClearSkyScene(palette, isDay, compact, modifier)
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

private fun weatherPalette(temp: Double?, isDay: Boolean): WeatherPalette {
    val tempC = temp ?: 20.0
    val (top, bottom) = when {
        tempC < -5 -> Color(0xFF0D0D2B) to Color(0xFF1A237E) // Deep freezing
        tempC < 0 -> Color(0xFF1A237E) to Color(0xFF42A5F5)  // Freezing
        tempC < 10 -> Color(0xFF1565C0) to Color(0xFF90CAF9) // Cold
        tempC < 20 -> Color(0xFF0D47A1) to Color(0xFF66BB6A) // Cool
        tempC < 30 -> Color(0xFFE65100) to Color(0xFFFFCC80) // Warm
        tempC < 38 -> Color(0xFFBF360C) to Color(0xFFFFAB91) // Hot
        else -> Color(0xFFB71C1C) to Color(0xFFE57373)      // Extreme heat
    }

    val bgColors = if (isDay) listOf(top, bottom) else listOf(top.copy(alpha = 0.7f), bottom.copy(red = bottom.red * 0.5f, green = bottom.green * 0.5f, blue = bottom.blue * 0.6f))
    return WeatherPalette(
        primary = top,
        secondary = bottom,
        accent = if (isDay) Color(0xFFFFD54F) else Color(0xFFB0BEC5),
        background = bgColors
    )
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
            // Moon glow
            drawCircle(
                color = Color(0xFFECEFF1).copy(alpha = 0.15f),
                radius = sunRadius * 2.5f,
                center = Offset(cx, cy)
            )

            // Moon
            drawCircle(
                color = Color(0xFFECEFF1),
                radius = sunRadius,
                center = Offset(cx, cy)
            )
            // Moon crescent shadow
            drawCircle(
                color = palette.background.last().copy(alpha = 0.7f),
                radius = sunRadius * 0.85f,
                center = Offset(cx + sunRadius * 0.2f, cy - sunRadius * 0.1f)
            )

            // Stars with twinkle
            stars.forEach { (x, y) ->
                val twinkle = (sin(starAlpha * 6f + x * 20f + y * 30f) * 0.5f + 0.5f).coerceIn(0.2f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = twinkle * 0.9f),
                    radius = 1.2f + twinkle * 1.8f,
                    center = Offset(x * size.width, y * size.height)
                )
            }

            // Fireflies / floating particles at night
            particles.forEach { (x, y, speed) ->
                val px = (x * size.width + particleDrift * size.width * 0.2f * speed) % (size.width + 20f) - 10f
                val py = (y * size.height + particleDrift * size.height * 0.08f * speed) % size.height
                val glow = (sin(particleDrift * 4f + x * 10f) * 0.5f + 0.5f) * 0.6f
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = glow),
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

    val cloudColor = Color(0xFFB0BEC5)
    val cloudColorDark = Color(0xFF78909C)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudScale = if (compact) size.width * 0.4f else size.width * 0.5f
        val cloudScale2 = if (compact) size.width * 0.3f else size.width * 0.4f

        // Back layer clouds (slow drift)
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.1f,
            baseY = size.height * 0.2f,
            scale = cloudScale,
            color = cloudColor.copy(alpha = 0.4f)
        )
        drawCloud(
            offset = cloudOffset1,
            baseX = size.width * 0.5f,
            baseY = size.height * 0.15f,
            scale = cloudScale * 0.8f,
            color = cloudColor.copy(alpha = 0.3f)
        )

        // Middle layer clouds
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.3f,
            baseY = size.height * 0.35f,
            scale = cloudScale2,
            color = cloudColorDark.copy(alpha = 0.5f)
        )
        drawCloud(
            offset = cloudOffset2,
            baseX = size.width * 0.7f,
            baseY = size.height * 0.3f,
            scale = cloudScale2 * 0.9f,
            color = cloudColorDark.copy(alpha = 0.4f)
        )

        // Front layer clouds (fast drift)
        drawCloud(
            offset = cloudOffset3,
            baseX = size.width * 0.15f,
            baseY = size.height * 0.7f,
            scale = cloudScale * 1.1f,
            color = cloudColor.copy(alpha = 0.6f)
        )
        drawCloud(
            offset = cloudOffset3,
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

    Canvas(modifier = modifier.fillMaxSize()) {
        val fogColor = Color(0xFFB0BEC5).copy(alpha = fogAlpha * 0.5f)

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

    val rainColor = Color(0xFFB3E5FC).copy(alpha = if (isHeavy) 0.6f else 0.4f)
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
    val snowColor = Color.White.copy(alpha = if (isHeavy) 0.85f else 0.6f)
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
//  Thunderstorm — Rain + periodic lightning flash + screen-edge glow
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ThunderstormScene(
    palette: WeatherPalette,
    compact: Boolean,
    modifier: Modifier
) {
    // Includes rain effect
    RainScene(weatherCode = 65, palette = palette, compact = compact, modifier = modifier)

    val randomDelay = remember { Random(789).nextInt(3000) }
    val infiniteTransition = rememberInfiniteTransition(label = "thunder")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset(randomDelay)
        ),
        label = "lightningFlash"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset(randomDelay + 500)
        ),
        label = "glowFlash"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Lightning flash (brief full-screen white flash)
        if (flashAlpha > 0.5f) {
            drawRect(
                color = Color.White.copy(alpha = flashAlpha * 0.3f),
                size = size
            )
        }

        // Screen-edge glow
        val glowColor = Color(0xFFFFF9C4).copy(alpha = glowAlpha)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor,
                    glowColor.copy(alpha = 0f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.1f),
                radius = size.maxDimension * 0.6f
            ),
            size = size
        )
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
                drawCloud(0f, cx - r, cy, r * 2f, Color(0xFFB0BEC5).copy(alpha = 0.6f))
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
