package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate"
    )
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "starTwinkle"
    )

    val rayCount = if (compact) 6 else 12
    val starCount = if (compact) 8 else 20

    // Pre-generate star positions
    val stars = if (!isDay && !compact) {
        rememberStarPositions(starCount)
    } else null

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val sunRadius = if (compact) size.minDimension * 0.15f else size.minDimension * 0.12f

        if (isDay) {
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
                    color = palette.accent.copy(alpha = 0.3f),
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
                color = Color.White.copy(alpha = 0.3f),
                radius = sunRadius * 0.6f,
                center = Offset(cx, cy)
            )
        } else {
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

            // Stars
            stars?.forEach { (x, y) ->
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha * (0.5f + Random.nextFloat() * 0.5f)),
                    radius = 1.5f + Random.nextFloat() * 1.5f,
                    center = Offset(x * size.width, y * size.height)
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
    val streakCount = if (compact) (if (isHeavy) 30 else 15) else (if (isHeavy) 60 else 30)
    val streakSpeed = if (isHeavy) 600f else 1000f

    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(streakSpeed.toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "rainFall"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleAlpha"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleScale"
    )

    val rainColor = Color(0xFFB3E5FC).copy(alpha = if (isHeavy) 0.6f else 0.4f)
    val streaks = rememberRainStreaks(streakCount)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Rain streaks
        streaks.forEach { (x, speed, length) ->
            val y = (rainProgress * size.height + x * size.height * 0.3f * speed) % (size.height + length)
            val yEnd = y - length
            drawLine(
                color = rainColor,
                start = Offset(x * size.width, y),
                end = Offset(x * size.width, yEnd),
                strokeWidth = if (isHeavy) 2f else 1.5f
            )
        }

        // Rain ripples on ground
        if (!compact) {
            for (i in 0..3) {
                val rippleX = size.width * (0.15f + i * 0.22f)
                val rippleY = size.height * 0.95f
                drawCircle(
                    color = rainColor.copy(alpha = rippleAlpha),
                    radius = 8f + rippleScale * 20f,
                    center = Offset(rippleX, rippleY),
                    style = Stroke(width = 1.5f)
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
    val flakeCount = if (compact) (if (isHeavy) 25 else 12) else (if (isHeavy) 50 else 25)

    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowFall"
    )
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "snowSway"
    )

    val flakes = rememberSnowflakes(flakeCount)
    val snowColor = Color.White.copy(alpha = if (isHeavy) 0.8f else 0.6f)

    Canvas(modifier = modifier.fillMaxSize()) {
        flakes.forEach { (x, speed, flakeSize) ->
            val y = (snowProgress * size.height * speed + x * size.height * 0.2f) % (size.height + flakeSize)
            val swayX = sin(swayOffset * 2f * Math.PI.toFloat() + x * size.width * 0.01f) * 15f
            val adjustedX = (x * size.width + swayX).coerceIn(0f, size.width)

            drawCircle(
                color = snowColor,
                radius = flakeSize,
                center = Offset(adjustedX, y)
            )
        }
    }
}

private fun rememberSnowflakes(count: Int): List<Triple<Float, Float, Float>> {
    val seed = 456L
    val rng = Random(seed)
    return List(count) {
        Triple(rng.nextFloat(), 0.5f + rng.nextFloat() * 1.0f, 2f + rng.nextFloat() * 4f)
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

    // ── Lightning bolt paths computed from flash counter (stable for entire flash) ──
    // boltSeed increments each time a new flash starts directly in the main flash loop,
    // so the bolt path is fixed for the entire duration of a single flash — no frame-to-frame jitter.
    var boltSeed by remember { mutableStateOf(0) }
    
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
            val glowColor = Color(0xFFFFF9C4).copy(alpha = glowAlpha)
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
