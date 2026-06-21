package fieldmind.research.app.features.field.presentation.components

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Physics-based cloud system with infinite seamless scrolling and no sudden disappears
 * Clouds drift based on wind with individual mass/density affecting speed
 * Uses wrap-around positioning for infinite looping
 */
data class PhysicsCloud(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val mass: Float = 1f,  // Affects wind resistance
    val opacity: Float = 0.8f,
    val type: CloudType = CloudType.CUMULUS,
    var vx: Float = 0f,  // Velocity x
    var driftOffset: Float = 0f,  // For perlin drift animation
    val depth: Float = 0.5f  // 0 = far (slower), 1 = close (faster parallax)
) {
    enum class CloudType { CIRRUS, CUMULUS, STRATUS, CUMULONIMBUS }
}

class CloudPhysicsSystem(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val maxClouds: Int = 8,
    private val windForce: Float = 0.02f,
    private val dragCoefficient: Float = 0.01f
) {
    private val clouds = mutableListOf<PhysicsCloud>()
    private var windDirection = 1f
    private var windIntensity = 0.5f
    private var time = 0f
    private val random = Random(999)

    init {
        // Initialize clouds with varied positions and types
        repeat(maxClouds) {
            val cloudType = listOf(
                PhysicsCloud.CloudType.CIRRUS,
                PhysicsCloud.CloudType.CUMULUS,
                PhysicsCloud.CloudType.STRATUS,
                PhysicsCloud.CloudType.CUMULONIMBUS
            ).random(random)

            val (width, height, mass) = when (cloudType) {
                PhysicsCloud.CloudType.CIRRUS -> Triple(180f, 40f, 0.3f)      // Light, high
                PhysicsCloud.CloudType.CUMULUS -> Triple(160f, 120f, 0.7f)    // Medium, fluffy
                PhysicsCloud.CloudType.STRATUS -> Triple(280f, 60f, 0.9f)     // Dense, low
                PhysicsCloud.CloudType.CUMULONIMBUS -> Triple(220f, 180f, 1.2f) // Heavy, dark
            }

            val depth = when (cloudType) {
                PhysicsCloud.CloudType.CIRRUS -> 0.1f        // Very far
                PhysicsCloud.CloudType.CUMULUS -> 0.4f       // Medium
                PhysicsCloud.CloudType.STRATUS -> 0.6f       // Closer
                PhysicsCloud.CloudType.CUMULONIMBUS -> 0.8f  // Very close
            }

            clouds.add(
                PhysicsCloud(
                    x = random.nextFloat() * canvasWidth,
                    y = random.nextFloat() * canvasHeight * 0.4f,  // Keep in upper half
                    width = width,
                    height = height,
                    mass = mass,
                    opacity = when (cloudType) {
                        PhysicsCloud.CloudType.CIRRUS -> 0.3f
                        PhysicsCloud.CloudType.CUMULUS -> 0.7f
                        PhysicsCloud.CloudType.STRATUS -> 0.85f
                        PhysicsCloud.CloudType.CUMULONIMBUS -> 0.95f
                    },
                    type = cloudType,
                    depth = depth
                )
            )
        }
    }

    fun update(deltaTime: Float = 0.016f, windGust: Float = 0f) {
        time += deltaTime

        // Update wind with gusts
        windIntensity = 0.5f + 0.3f * sin(time * 0.5f) + windGust * 0.3f
        windIntensity = windIntensity.coerceIn(0f, 1f)

        clouds.forEach { cloud ->
            // Apply wind force scaled by depth (clouds further away move slower)
            val depthWind = windForce * windIntensity * cloud.depth / cloud.mass
            cloud.vx += windDirection * depthWind

            // Apply gentle drag
            cloud.vx *= (1f - dragCoefficient)

            // Perlin-based drift for natural sway
            cloud.driftOffset = perlinNoise(cloud.x * 0.001f, time * 0.3f, 0f) * 20f

            // Update position with wrap-around for seamless looping
            cloud.x += cloud.vx
            val actualX = cloud.x + cloud.driftOffset

            // Seamless wrap: no sudden disappears
            when {
                actualX < -cloud.width -> cloud.x = canvasWidth + cloud.width
                actualX > canvasWidth + cloud.width -> cloud.x = -cloud.width
            }
        }
    }

    fun setWindDirection(direction: Float) {
        windDirection = if (direction < 0) -1f else 1f
    }

    fun getActiveClouds(): List<PhysicsCloud> = clouds

    fun clear() {
        clouds.clear()
    }
}

fun perlinNoise(x: Float, y: Float, z: Float): Float {
    val sin1 = sin(x * 0.5f + y * 0.3f) * cos(z)
    val sin2 = sin(x * 0.3f - y * 0.2f) * cos(z + 1f)
    val sin3 = sin((x + y * 0.15f) * 0.2f) * cos(z + 2f)
    return (sin1 + sin2 + sin3) / 2.5f
}

/**
 * Physics-based real thunder system with branching lightning and ground effects
 */
data class LightningBolt(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val branches: List<LightningBolt> = emptyList(),
    val intensity: Float = 1f,
    val creationTime: Float = 0f,
    val duration: Float = 0.15f
)

class ThunderPhysicsSystem {
    private var activeBolts = mutableListOf<LightningBolt>()
    private var time = 0f
    private val random = Random(777)

    fun update(deltaTime: Float = 0.016f) {
        time += deltaTime
        // Remove expired bolts
        activeBolts.removeAll { time - it.creationTime > it.duration }
    }

    /**
     * Generate realistic fractal lightning with branching
     */
    fun generateLightning(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        depth: Int = 0,
        maxDepth: Int = 4,
        intensity: Float = 1f
    ): LightningBolt {
        val branches = mutableListOf<LightningBolt>()

        if (depth < maxDepth) {
            val dx = endX - startX
            val dy = endY - startY
            val midX = startX + dx * 0.5f + (random.nextFloat() - 0.5f) * dx * 0.3f
            val midY = startY + dy * 0.5f + (random.nextFloat() - 0.5f) * dy * 0.3f

            // Generate branches at midpoint
            if (random.nextFloat() > 0.4f) {
                val branchX = midX + (random.nextFloat() - 0.5f) * 200f
                val branchY = midY + (random.nextFloat() - 0.5f) * 300f
                branches.add(
                    generateLightning(
                        midX, midY, branchX, branchY,
                        depth + 1, maxDepth,
                        intensity * 0.7f
                    )
                )
            }

            // Split into two main paths
            branches.add(
                generateLightning(
                    startX, startY, midX, midY,
                    depth + 1, maxDepth, intensity * 0.9f
                )
            )
            branches.add(
                generateLightning(
                    midX, midY, endX, endY,
                    depth + 1, maxDepth, intensity * 0.9f
                )
            )
        }

        return LightningBolt(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            branches = branches,
            intensity = intensity,
            creationTime = time
        )
    }

    /**
     * Trigger a lightning strike at specified coordinates
     */
    fun triggerLightning(cloudX: Float, cloudY: Float, groundY: Float) {
        val bolt = generateLightning(
            cloudX + 50f, cloudY + 100f,
            cloudX + (random.nextFloat() - 0.5f) * 300f, groundY,
            intensity = 0.9f + random.nextFloat() * 0.1f
        )
        activeBolts.add(bolt)
    }

    fun getActiveBolts(): List<LightningBolt> = activeBolts

    fun clear() {
        activeBolts.clear()
    }

    /**
     * Get illumination factor for lightning flash effect (0-1)
     */
    fun getIlluminationIntensity(): Float {
        if (activeBolts.isEmpty()) return 0f
        return activeBolts.map { bolt ->
            val elapsed = time - bolt.creationTime
            val progress = (elapsed / bolt.duration).coerceIn(0f, 1f)
            // Intensity peaks at 0.3 of duration, then fades
            (1f - (progress - 0.3f).coerceAtLeast(0f) * 1.5f).coerceIn(0f, 1f) * bolt.intensity
        }.maxOrNull() ?: 0f
    }

    /**
     * Get thunder sound delay based on lightning distance
     */
    fun getThunderDelay(lightningX: Float, lightningY: Float, observerX: Float, observerY: Float): Long {
        val distance = sqrt((lightningX - observerX) * (lightningX - observerX) + (lightningY - observerY) * (lightningY - observerY))
        // Sound travels ~343 m/s; approximate pixels to meters
        val delaySeconds = (distance / 343f).coerceIn(0f, 10f)
        return (delaySeconds * 1000).toLong()
    }
}
