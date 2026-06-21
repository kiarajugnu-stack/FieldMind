package fieldmind.research.app.features.field.presentation.components

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Physics-based particle system for weather effects.
 * Models gravity, wind drag, and velocity-based rendering.
 */
data class PhysicsParticle(
    var x: Float,
    var y: Float,
    var vx: Float,  // velocity x
    var vy: Float,  // velocity y
    var ax: Float = 0f,  // acceleration x
    var ay: Float = 0f,  // acceleration y (gravity)
    val mass: Float = 1f,
    val size: Float = 1f,
    val id: Int = 0,
    var alive: Boolean = true
)

class PhysicsParticleSystem(
    val canvasWidth: Float,
    val canvasHeight: Float,
    private val maxParticles: Int = 120,
    private val gravity: Float = 0.15f,
    private val windForce: Float = 0.05f,
    private val dragCoefficient: Float = 0.02f
) {
    private val particles = mutableListOf<PhysicsParticle>()
    private var windDirection = 1f  // 1 for right, -1 for left
    private var windIntensity = 0.5f
    private var time = 0f
    
    // Seeded random for deterministic behavior across recompositions
    private val random = Random(42)

    init {
        particles.clear()
    }

    /**
     * Emit particles at the top of the canvas (for rain/snow)
     */
    fun emitAtTop(count: Int, vx: Float, vy: Float, size: Float, massRange: Pair<Float, Float> = 0.8f to 1.2f) {
        repeat(count) { i ->
            if (particles.size < maxParticles) {
                val x = random.nextFloat() * canvasWidth
                val mass = random.nextFloat() * (massRange.second - massRange.first) + massRange.first
                particles.add(
                    PhysicsParticle(
                        x = x,
                        y = -size,
                        vx = vx + random.nextFloat() * 0.2f - 0.1f,  // Add slight randomness
                        vy = vy,
                        mass = mass,
                        size = size * mass,
                        id = particles.size
                    )
                )
            }
        }
    }

    /**
     * Update all particles with physics simulation
     */
    fun update(deltaTime: Float = 0.016f, windGust: Float = 0f) {
        time += deltaTime

        // Update wind properties based on time (sine wave oscillation)
        windIntensity = 0.5f + 0.3f * kotlin.math.sin(time * 2f)
        if (windGust > 0) windIntensity += windGust

        particles.forEach { particle ->
            if (particle.alive) {
                // Apply gravity
                particle.ay = gravity / particle.mass

                // Apply wind force (direction can change)
                val windForceScaled = windForce * windIntensity
                particle.ax = windDirection * windForceScaled / particle.mass

                // Apply air drag (proportional to velocity squared)
                val speed = sqrt(particle.vx * particle.vx + particle.vy * particle.vy)
                if (speed > 0.01f) {
                    val dragX = -dragCoefficient * particle.vx * speed
                    val dragY = -dragCoefficient * particle.vy * speed
                    particle.ax += dragX
                    particle.ay += dragY
                }

                // Update velocity (v = v + a * dt)
                particle.vx += particle.ax * deltaTime
                particle.vy += particle.ay * deltaTime

                // Terminal velocity cap (prevents particles from accelerating infinitely)
                val maxTerminalVelocity = 8f
                val currentSpeed = sqrt(particle.vx * particle.vx + particle.vy * particle.vy)
                if (currentSpeed > maxTerminalVelocity) {
                    val scale = maxTerminalVelocity / currentSpeed
                    particle.vx *= scale
                    particle.vy *= scale
                }

                // Update position (x = x + v * dt)
                particle.x += particle.vx * deltaTime
                particle.y += particle.vy * deltaTime

                // Wrap horizontally for seamless looping
                when {
                    particle.x < -particle.size -> particle.x = canvasWidth + particle.size
                    particle.x > canvasWidth + particle.size -> particle.x = -particle.size
                }

                // Deactivate if below canvas
                if (particle.y > canvasHeight + particle.size) {
                    particle.alive = false
                }
            }
        }

        // Remove dead particles and recycle slots
        particles.removeAll { !it.alive }
    }

    /**
     * Get list of active particles
     */
    fun getActiveParticles(): List<PhysicsParticle> = particles.filter { it.alive }

    /**
     * Set wind direction (-1 = left, 1 = right)
     */
    fun setWindDirection(direction: Float) {
        windDirection = if (direction < 0) -1f else 1f
    }

    /**
     * Apply a wind gust (temporary increase in wind intensity)
     */
    fun applyWindGust(intensity: Float) {
        windIntensity = minOf(1f, windIntensity + intensity)
    }

    /**
     * Clear all particles
     */
    fun clear() {
        particles.clear()
    }

    /**
     * Get count of active particles
     */
    fun getParticleCount(): Int = particles.count { it.alive }
}

/**
 * Perlin-like noise function for smooth, natural motion
 * Using layered sine waves to approximate Perlin noise behavior
 */
fun perlinNoise(x: Float, y: Float, time: Float): Float {
    val value1 = kotlin.math.sin(x * 0.5f + time * 0.3f) * kotlin.math.cos(y * 0.7f)
    val value2 = kotlin.math.sin(x * 0.3f - time * 0.2f) * kotlin.math.cos(y * 0.5f + time * 0.1f)
    val value3 = kotlin.math.sin((x + time * 0.15f) * 0.2f) * kotlin.math.cos((y + time * 0.1f) * 0.3f)
    return (value1 + value2 + value3) / 3f
}

/**
 * Helper to calculate velocity angle based on physics
 */
fun getVelocityAngle(vx: Float, vy: Float): Float {
    return kotlin.math.atan2(vy, vx)
}

/**
 * Helper to calculate velocity magnitude
 */
fun getVelocityMagnitude(vx: Float, vy: Float): Float {
    return sqrt(vx * vx + vy * vy)
}
