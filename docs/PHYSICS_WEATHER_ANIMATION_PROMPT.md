# Physics-Based Weather Animation System — Implementation Prompt

## Overview

Replace the current `AnimatedWeatherScene.kt` cloud and weather animations with a **true physics-based particle system** that simulates atmospheric fluid dynamics for cloud formation, dissipation, collision, merging, and thunderstorm electrification. Every visual element must obey physical laws — no hard-coded loops, no repetitive `infiniteRepeatable` animations.

---

## 1. Core Philosophy: Physics Over Animation

### ❌ Current Anti-Patterns to Eliminate

| Anti-pattern | Example | Why it's wrong |
|---|---|---|
| `infiniteRepeatable(tween(...), RepeatMode.Restart)` | Cloud drift every 18s | Clouds restart from the same position — looks like a GIF loop |
| `RepeatMode.Reverse` for morphing | `cloudMorph 0→6→0` | Shape morphs ping-pong — organic clouds drift and evolve directionally |
| Fixed cloud count | `val extraClouds = (cloudIntensity * 3f).toInt()` | Clouds don't spawn/dissipate based on physical conditions |
| `easing = LinearEasing` for drift | All cloud motion | Real clouds accelerate with wind gusts, decelerate, and change direction |
| Global `cloudOffset` for all clouds | Single drift value | Each cloud should have independent velocity, direction, and altitude |

### ✅ Physics-First Design Principles

1. **Every cloud is a persistent particle** with position (x, y, z), velocity (vx, vy), mass, water content, and charge state
2. **No animation loops** — use a fluid simulation step that advances each frame with delta-time integration
3. **Clouds emerge from humidity** — spawn conditions based on simulated atmospheric water vapor
4. **Clouds dissipate through evaporation** — shrink and fade when humidity drops or wind shear exceeds threshold
5. **Collisions are real** — overlapping cloud particles merge mass and trigger precipitation/electrification
6. **Wind is a force field** — velocity field with gusts, shear layers, and turbulence, not a constant scroll

---

## 2. Fluid Simulation Architecture

### 2.1 Cloud Particle System

Each cloud is represented as a **`CloudParticle`** with the following state:

```kotlin
data class CloudParticle(
    val id: Long,
    // Position — 3D world space (z = altitude)
    var x: Float,           // 0..1 normalized screen X
    var y: Float,           // 0..1 normalized screen Y  
    var z: Float,           // 0.0 (ground) to 1.0 (top of sky)
    
    // Velocity — physics state
    var vx: Float,          // horizontal velocity (m/s scaled)
    var vy: Float,          // vertical velocity (thermal updraft/downdraft)
    
    // Physical properties
    var mass: Float,        // water mass — determines size and opacity
    var density: Float,     // 0..1 compactness (fluffy vs wispy)
    var temperature: Float, // internal temperature (affects buoyancy)
    var charge: Float,      // electrical charge — 0 = neutral, positive/negative for lightning
    
    // Visual state
    var scale: Float,       // current visual scale
    var alpha: Float,       // current opacity (fades during dissipation)
    var cloudType: CloudType, // Cumulus, Stratus, Cirrus, Cumulonimbus
    var ageMs: Long,        // how long this cloud has existed
    var lifetimeMs: Long    // natural lifetime before complete dissipation
)
```

### 2.2 Environmental State

```kotlin
data class AtmosphericState(
    // Humidity field — horizontal layers
    var surfaceHumidity: Float,    // 0..1, drives cumulus formation
    var midHumidity: Float,        // 0..1, drives stratus formation  
    var highHumidity: Float,       // 0..1, drives cirrus formation
    
    // Wind field — multi-layer with gusts
    val surfaceWind: WindLayer,
    val midWind: WindLayer,
    val highWind: WindLayer,
    
    // Thermal activity
    var thermalStrength: Float,    // 0..1, updraft intensity
    var thermalDistribution: Float,// spatial variation of thermals
    
    // Electrical field (for thunderstorms)
    var electricFieldStrength: Float, // 0..1, builds during storms
    var breakdownThreshold: Float,    // when exceeded, lightning strikes
)

data class WindLayer(
    var baseSpeed: Float,       // m/s
    var direction: Float,       // degrees
    var gustFactor: Float,      // 0..1, turbulence intensity
    var shearVertical: Float    // vertical wind shear
)
```

### 2.3 Simulation Step (called every frame)

```kotlin
fun simulateAtmosphere(
    particles: MutableList<CloudParticle>,
    atmosphere: AtmosphericState,
    deltaMs: Long,
    screenWidth: Float,
    screenHeight: Float
) {
    val deltaSec = deltaMs / 1000f
    
    // 1. Update wind field — evolve gusts and shear
    updateWindField(atmosphere, deltaSec)
    
    // 2. Spawn new clouds from humidity
    spawnClouds(particles, atmosphere, deltaSec)
    
    // 3. Physics integration for each particle
    for (cloud in particles) {
        // Advection — wind pushes clouds
        val wind = windAtAltitude(atmosphere, cloud.z)
        cloud.vx += (wind.baseSpeed * cos(wind.direction) + 
                     windGust(wind) - cloud.vx) * 0.1f
        cloud.vy += buoyancyForce(cloud) + thermalForce(atmosphere, cloud)
        
        // Integrate position
        cloud.x += cloud.vx * deltaSec * 0.001f
        cloud.y += cloud.vy * deltaSec * 0.001f
        
        // Wrap horizontally (clouds are infinite) — but not all at the same edge
        if (cloud.x > 1.3f) { cloud.x = -0.3f; cloud.x += Random.nextFloat() * 0.1f }
        if (cloud.x < -0.3f) { cloud.x = 1.3f; cloud.x -= Random.nextFloat() * 0.1f }
        
        // 4. Phase transitions — growth, dissipation, evaporation
        updateCloudPhase(cloud, atmosphere, deltaSec)
        
        // 5. Collision detection — merge clouds that overlap
        checkCloudCollisions(particles, cloud)
        
        // 6. Electrification (cumulonimbus only)
        if (cloud.cloudType == CloudType.Cumulonimbus) {
            updateCharge(cloud, atmosphere, deltaSec)
        }
    }
    
    // 7. Remove fully dissipated clouds
    particles.removeAll { it.alpha <= 0.01f || it.mass <= 0f }
}
```

---

## 3. Cloud Behavior Physics

### 3.1 Spawning (Emergence)

Clouds must **emerge gradually** from invisible humidity, not pop into existence:

```kotlin
fun spawnClouds(particles: MutableList<CloudParticle>, atmosphere: AtmosphericState, deltaSec: Float) {
    val spawnChance = atmosphere.surfaceHumidity * deltaSec * 0.5f
    
    if (Random.nextFloat() < spawnChance && particles.size < MAX_CLOUDS) {
        val z = when {
            Random.nextFloat() < atmosphere.highHumidity -> 0.15f + Random.nextFloat() * 0.15f // cirrus high
            Random.nextFloat() < atmosphere.midHumidity -> 0.30f + Random.nextFloat() * 0.20f // stratus mid
            else -> 0.45f + Random.nextFloat() * 0.30f // cumulus low-mid
        }
        
        val cloudType = cloudTypeForAltitude(z, atmosphere)
        val initialMass = 0.1f + Random.nextFloat() * 0.3f  // Start small
        
        particles.add(CloudParticle(
            id = nextId++,
            x = -0.1f - Random.nextFloat() * 0.2f, // spawn off-screen left
            y = 0.3f + Random.nextFloat() * 0.5f,
            z = z,
            vx = 0f, vy = 0f,
            mass = initialMass,
            density = 0.3f + Random.nextFloat() * 0.5f,
            temperature = atmosphere.surfaceHumidity * 30f,
            charge = 0f,
            scale = initialMass * 3f,
            alpha = 0f,  // Start invisible
            cloudType = cloudType,
            ageMs = 0,
            lifetimeMs = (15000 + Random.nextFloat() * 45000).toLong() // 15-60 seconds
        ))
    }
    
    // Fade in newborn clouds
    for (cloud in particles) {
        if (cloud.ageMs < 2000 && cloud.alpha < 1f) {
            cloud.alpha = (cloud.ageMs / 2000f).coerceIn(0f, 1f)
        }
    }
}
```

### 3.2 Growth & Dissipation (Phase Transitions)

```kotlin
fun updateCloudPhase(cloud: CloudParticle, atmosphere: AtmosphericState, deltaSec: Float) {
    cloud.ageMs += (deltaSec * 1000).toLong()
    
    // Vertical movement — thermals push cumulus upward
    if (cloud.cloudType == CloudType.Cumulus) {
        val thermalUp = atmosphere.thermalStrength * 0.3f
        val gravity = -0.02f * cloud.density
        cloud.vy += (thermalUp + gravity) * deltaSec * 2f
    }
    
    // Water accumulation — cloud grows when humidity is high
    val localHumidity = humidityAtAltitude(atmosphere, cloud.z)
    if (localHumidity > cloud.density * 0.6f && cloud.mass < 5f) {
        cloud.mass += (localHumidity - cloud.density * 0.5f) * deltaSec * 0.5f
        cloud.scale = cloud.mass * 3f
        cloud.density = (cloud.density + localHumidity * deltaSec * 0.2f).coerceAtMost(1f)
    }
    
    // Evaporation — cloud shrinks when humidity is low or wind shear is high
    if (localHumidity < 0.3f || windShearAtAltitude(atmosphere, cloud.z) > 0.7f) {
        cloud.mass -= (0.3f - localHumidity) * deltaSec * 0.8f
        cloud.scale = (cloud.mass * 3f).coerceAtLeast(0.1f)
        cloud.density -= deltaSec * 0.1f
    }
    
    // Dissipation — cloud fades when mass drops below threshold
    if (cloud.mass < 0.3f || cloud.ageMs > cloud.lifetimeMs * 0.9f) {
        cloud.alpha -= deltaSec * 0.2f
    }
    
    // Type transition — cumulus can grow into cumulonimbus (thundercloud)
    if (cloud.cloudType == CloudType.Cumulus && cloud.mass > 3.5f && cloud.z < 0.5f) {
        cloud.cloudType = CloudType.Cumulonimbus
        cloud.density = 0.9f
        // Trigger lightning readiness
    }
}
```

### 3.3 Collision & Merging

```kotlin
fun checkCloudCollisions(particles: MutableList<CloudParticle>, cloud: CloudParticle) {
    for (other in particles) {
        if (other.id == cloud.id) continue
        
        val dx = cloud.x - other.x
        val dy = (cloud.y - other.y) * 0.6f  // squash Y for aspect ratio
        val dist = sqrt(dx * dx + dy * dy)
        val overlapDist = (cloud.scale + other.scale) * 0.08f  // collision radius
        
        if (dist < overlapDist && dist > 0.001f) {
            // Elastic collision response — clouds push each other
            val overlap = overlapDist - dist
            val nx = dx / dist
            val ny = dy / dist
            
            cloud.x += nx * overlap * 0.5f
            cloud.y += ny * overlap * 0.5f
            other.x -= nx * overlap * 0.5f
            other.y -= ny * overlap * 0.5f
            
            // Mass transfer — larger cloud absorbs smaller one's mass
            if (cloud.mass > other.mass * 1.5f && dist < overlapDist * 0.5f) {
                cloud.mass += other.mass * 0.3f
                cloud.scale = cloud.mass * 3f
                other.mass *= 0.7f
                // Merging visual effect: brief bright flash, shape distortion
                triggerMergeEffect(cloud, other)
            }
            
            // Electrification — friction between clouds builds charge
            if (cloud.cloudType == CloudType.Cumulonimbus || other.cloudType == CloudType.Cumulonimbus) {
                cloud.charge += overlap * 0.01f
                other.charge -= overlap * 0.005f
            }
        }
    }
}
```

### 3.4 Organic Shape Evolution

Replace the current `drawCumulus()` static circle clusters with a **shape that evolves organically** based on physical state:

```
drawCloud() algorithm:
  1. Base shape is defined by a set of connected control points (not fixed circles)
  2. Control points move slowly based on:
     - Wind shear: stretches the cloud horizontally
     - Thermal activity: pushes top of cloud upward (cauliflower top)
     - Turbulence: adds wobbly deformation to edges
     - Mass: larger clouds have more undulations
  3. The cloud's bottom remains flat (evaporation base) while top billows
  4. Cumulonimbus anvil shape forms when cloud hits the tropopause (z threshold)
```

```kotlin
fun drawOrganicCloud(
    drawScope: DrawScope,
    cloud: CloudParticle,
    palette: WeatherPalette,
    timeMs: Long
) {
    val cx = cloud.x * drawScope.size.width
    val cy = cloud.y * drawScope.size.height
    val baseScale = cloud.scale * drawScope.size.width * 0.08f
    
    // Number of billows = f(mass, density) — larger clouds have more complexity
    val billowCount = (4 + cloud.mass * 2).toInt().coerceAtMost(12)
    val billowAngles = (0 until billowCount).map { i ->
        val baseAngle = (i.toFloat() / billowCount) * 2f * PI.toFloat()
        // Add organic jitter that evolves over time
        val jitter = sin(timeMs * 0.0003f + i * 1.7f) * 0.2f +
                     sin(timeMs * 0.0007f + i * 3.1f) * 0.1f
        baseAngle + jitter
    }
    
    // Billow radii vary with cloud properties and evolve over time
    val billowRadii = billowAngles.mapIndexed { i, angle ->
        val baseR = baseScale * (0.5f + cloud.mass * 0.15f)
        val verticalStretch = if (cos(angle) < 0f) 1.2f else 1.0f  // top billows stretch up
        val timeWobble = sin(timeMs * 0.0005f + i * 2.3f) * baseScale * 0.15f
        val shearStretch = 1f + cloud.vx * 0.5f  // wind stretches cloud
        (baseR + timeWobble) * verticalStretch * shearStretch
    }
    
    // Bottom flatness — cumulus have flat bottoms at evaporation level
    val flatBottom = cloud.cloudType == CloudType.Cumulus || cloud.cloudType == CloudType.Cumulonimbus
    
    // Draw billows from bottom to top for layering
    val sortedIndices = billowAngles.indices.sortedBy { 
        sin(billowAngles[it])  // bottom billows first
    }
    
    for (i in sortedIndices) {
        val angle = billowAngles[i]
        val radius = billowRadii[i]
        val bx = cx + cos(angle) * radius * 0.6f
        val by = cy + sin(angle) * radius * 0.5f + baseScale * 0.3f  // offset center upward
        
        // Skip drawing bottom billows for cumulus (flat base)
        if (flatBottom && sin(angle) > 0.3f) continue
        
        // Color based on depth — back billows are darker, front are brighter
        val depth = cos(angle - cloud.vx * 0.3f) * 0.5f + 0.5f
        val billowColor = palette.cloudBaseColor.copy(
            alpha = cloud.alpha * (0.5f + depth * 0.5f)
        )
        
        drawScope.drawCircle(
            color = billowColor,
            radius = radius,
            center = Offset(bx, by)
        )
        
        // Highlight rim on upper-right
        if (sin(angle) < -0.2f) {
            drawScope.drawCircle(
                color = Color.White.copy(alpha = cloud.alpha * 0.15f),
                radius = radius * 0.4f,
                center = Offset(bx + radius * 0.2f, by - radius * 0.2f)
            )
        }
    }
    
    // Cumulonimbus anvil (flat top from tropopause)
    if (cloud.cloudType == CloudType.Cumulonimbus && cloud.mass > 3f) {
        val anvilWidth = baseScale * 2.5f
        val anvilY = cy - baseScale * 1.2f
        drawScope.drawRect(
            color = palette.cloudBaseColor.copy(alpha = cloud.alpha * 0.4f),
            topLeft = Offset(cx - anvilWidth * 0.5f, anvilY - baseScale * 0.1f),
            size = Size(anvilWidth, baseScale * 0.2f),
            style = drawScope.DrawStyle.Fill
        )
        // Anvil ice crystal texture — wispy horizontal streaks
        for (s in 0..8) {
            val sx = cx - anvilWidth * 0.4f + s * anvilWidth * 0.1f
            drawScope.drawLine(
                color = Color.White.copy(alpha = cloud.alpha * 0.1f),
                start = Offset(sx, anvilY - baseScale * 0.05f),
                end = Offset(sx + anvilWidth * 0.08f, anvilY - baseScale * 0.08f),
                strokeWidth = 1.5f
            )
        }
    }
}
```

---

## 4. Thunderstorm Physics

### 4.1 Lightning — True Electrical Breakdown Model

Lightning must obey **dielectric breakdown physics**, not random timing:

```kotlin
data class LightningBolt(
    var segments: List<Offset>,   // fractal path segments
    var progress: Float,          // 0..1, how far the leader has propagated
    var brightness: Float,        // decays after initial strike
    var type: LightningType,      // Cloud-to-Cloud, Cloud-to-Ground, Intra-Cloud
    var branchSegments: List<List<Offset>>, // forked branches
    var thunderDelayMs: Long,     // ms until thunder sound should play
    var alpha: Float              // 1.0 → 0.0 as bolt fades
)

enum class LightningType { CloudToGround, CloudToCloud, IntraCloud }
```

**Stepped Leader Physics:**

```kotlin
fun propagateLightningBolt(bolt: LightningBolt, deltaSec: Float, screenSize: Size) {
    if (bolt.progress >= 1f) {
        // Fade phase
        bolt.alpha -= deltaSec * 3f
        bolt.brightness -= deltaSec * 2f
        return
    }
    
    // Stepped leader propagation: 10-20 meter steps at ~10^5 m/s
    val stepLength = screenSize.maxDimension * 0.02f
    val stepsPerFrame = 3 + (Random.nextFloat() * 3).toInt()
    
    for (s in 0 until stepsPerFrame) {
        val lastPos = bolt.segments.last()
        
        // Direction: generally downward (CG) or lateral (CC/IC)
        val baseDirection = when (bolt.type) {
            LightningType.CloudToGround -> 90f  // straight down
            LightningType.CloudToCloud -> 0f    // horizontal
            LightningType.IntraCloud -> -30f + Random.nextFloat() * 60f
        }
        
        // Forking: random deviation from base direction
        val deviation = (Random.nextFloat() - 0.5f) * 60f
        val angleRad = (baseDirection + deviation) * PI.toFloat() / 180f
        
        // Leader step with zizag
        val newX = lastPos.x + cos(angleRad) * stepLength * (0.5f + Random.nextFloat())
        val newY = lastPos.y + sin(angleRad) * stepLength * (0.5f + Random.nextFloat())
        
        bolt.segments.add(Offset(newX, newY))
        
        // Branching: 10% chance per step to spawn a branch
        if (Random.nextFloat() < 0.1f && bolt.branchSegments.size < 3) {
            val branchAngle = angleRad + (Random.nextFloat() - 0.5f) * 80f
            val branch = mutableListOf(Offset(newX, newY))
            // Branch propagates 3-5 steps
            for (b in 0..(3 + Random.nextFloat() * 2).toInt()) {
                val bPos = branch.last()
                val bAngle = branchAngle + (Random.nextFloat() - 0.5f) * 40f
                branch.add(Offset(
                    bPos.x + cos(bAngle) * stepLength * 0.7f,
                    bPos.y + sin(bAngle) * stepLength * 0.7f
                ))
            }
            bolt.branchSegments.add(branch)
        }
        
        bolt.progress += 1f / 40f  // ~40 steps total
        
        // CG lightning stops when reaching ground
        if (bolt.type == LightningType.CloudToGround && newY > screenSize.height * 0.82f) {
            bolt.progress = 1f
            bolt.thunderDelayMs = 3000 + (Random.nextFloat() * 2000).toInt() // thunder 3-5s later
            break
        }
    }
}
```

### 4.2 Lightning Trigger Conditions

```kotlin
fun checkLightningTrigger(
    particles: List<CloudParticle>,
    atmosphere: AtmosphericState,
    deltaSec: Float
): LightningBolt? {
    // Find cumulonimbus clouds with sufficient charge
    val stormClouds = particles.filter { 
        it.cloudType == CloudType.Cumulonimbus && abs(it.charge) > 2f 
    }
    if (stormClouds.isEmpty()) return null
    
    // Build electric field between cloud and ground (or between clouds)
    atmosphere.electricFieldStrength += deltaSec * 0.02f * stormClouds.sumOf { abs(it.charge) }
    
    // Dielectric breakdown of air: ~3MV/m
    // When field exceeds threshold, lightning strikes
    if (atmosphere.electricFieldStrength > atmosphere.breakdownThreshold) {
        val sourceCloud = stormClouds.maxByOrNull { abs(it.charge) } ?: return null
        
        atmosphere.electricFieldStrength = 0f
        
        val isCG = Random.nextFloat() < 0.6f  // 60% cloud-to-ground
        return LightningBolt(
            segments = mutableListOf(Offset(
                sourceCloud.x * screenWidth,
                sourceCloud.y * screenHeight
            )),
            progress = 0f,
            brightness = 1f,
            type = if (isCG) LightningType.CloudToGround else LightningType.CloudToCloud,
            branchSegments = mutableListOf(),
            thunderDelayMs = 0,
            alpha = 1f
        )
    }
    
    return null
}
```

### 4.3 Lightning Flash Rendering

```kotlin
fun drawLightningFlash(
    drawScope: DrawScope,
    bolt: LightningBolt,
    palette: WeatherPalette
) {
    if (bolt.alpha <= 0f) return
    
    // Main bolt — bright white core
    val coreColor = Color.White.copy(alpha = bolt.alpha * bolt.brightness)
    for (i in 0 until bolt.segments.size - 1) {
        drawScope.drawLine(
            color = coreColor,
            start = bolt.segments[i],
            end = bolt.segments[i + 1],
            strokeWidth = 3f * bolt.alpha + 1f
        )
    }
    
    // Glow around main bolt — purple-blue corona
    val glowColor = Color(0xFF7B68EE).copy(alpha = bolt.alpha * bolt.brightness * 0.4f)
    for (i in 0 until bolt.segments.size - 1) {
        drawScope.drawLine(
            color = glowColor,
            start = bolt.segments[i],
            end = bolt.segments[i + 1],
            strokeWidth = 8f * bolt.alpha * bolt.brightness
        )
    }
    
    // Branches — thinner, slightly dimmer
    for (branch in bolt.branchSegments) {
        for (i in 0 until branch.size - 1) {
            drawScope.drawLine(
                color = coreColor.copy(alpha = bolt.alpha * 0.7f),
                start = branch[i],
                end = branch[i + 1],
                strokeWidth = 2f * bolt.alpha
            )
        }
    }
    
    // Screen flash — brief full-screen illumination
    if (bolt.progress < 0.3f) {
        val flashAlpha = (1f - bolt.progress / 0.3f) * 0.15f
        drawScope.drawRect(
            color = Color.White.copy(alpha = flashAlpha),
            size = drawScope.size
        )
    }
}
```

### 4.4 Thunder Sound Timing

```kotlin
// Thunder delay = distance from strike / speed of sound
// Visual: lightning appears instantly, thunder arrives later
// Delay is calculated from cloud altitude:
fun calculateThunderDelay(bolt: LightningBolt): Long {
    // Average CG strike length = 5km, speed of sound = 343 m/s
    // => ~14.6 seconds for thunder to reach observer
    // Scale to visual time: compress to 2-4 seconds for mobile
    val approximateDistanceKm = 3f + Random.nextFloat() * 7f
    return (approximateDistanceKm * 500).toLong()  // compressed: 1.5-5 seconds
}
```

---

## 5. Cloud Type-Specific Physics

### 5.1 Cumulus (Fair-Weather)

- **Formation**: Surface heating → thermal updrafts → adiabatic cooling → condensation
- **Shape**: Flat bottom (condensation level), rounded billowy top, distinct separation
- **Behavior**: Rises during warm thermals, evaporates in dry air, moves with wind
- **Lifespan**: 5-40 minutes (compressed to 20-60 seconds in animation)
- **Rendering**: Multiple overlapping hemispheres with bright tops, darker bases

### 5.2 Stratus (Overcast)

- **Formation**: Large-scale uniform lifting of moist air (fronts)
- **Shape**: Flat, layered, wide sheets covering most of sky
- **Behavior**: Minimal vertical motion, slow horizontal drift, uniform density
- **Rendering**: Wide horizontal ovals that tile across screen, soft edges, no billowing

### 5.3 Cirrus (High-Altitude)

- **Formation**: Ice crystals at high altitude (>6km), often ahead of warm fronts
- **Shape**: Wispy, feathery streaks that curve with upper-level winds
- **Behavior**: Moves fast with jet stream, slow evaporation (ice sublimation)
- **Rendering**: Thin curved lines with feathered edges, slight glow from ice refraction

### 5.4 Cumulonimbus (Thunderstorm)

- **Formation**: Extreme cumulus growth when atmosphere is unstable and humid
- **Shape**: Towering vertical with flat anvil top (tropopause), dark flat base
- **Behavior**: Strong internal updrafts, precipitation drag, electrical charge separation
- **Transitions**: Cumulus → Cumulonimbus when mass > threshold AND humidity > 0.8 AND z < 0.5
- **Rendering**: Dark base (gray/purple), billowing tower, ice crystal anvil, precipitation curtains

---

## 6. Wind Field Dynamics

Replace single `cloudOffset` with a true wind simulation:

```kotlin
fun updateWindField(atmosphere: AtmosphericState, deltaSec: Float) {
    // Gusts evolve with Perlin-like smooth noise
    for (layer in listOf(atmosphere.surfaceWind, atmosphere.midWind, atmosphere.highWind)) {
        // Wind speed oscillates with turbulence
        layer.gustFactor = 0.3f + sin(timeMs * 0.0007f + layer.hashCode()) * 0.3f
        layer.baseSpeed += (layer.gustFactor * 2f - layer.baseSpeed) * deltaSec * 0.1f
        
        // Wind direction shifts gradually (weather patterns rotating)
        layer.direction += sin(timeMs * 0.0003f + layer.hashCode() * 1.3f) * deltaSec * 5f
        
        // Vertical shear (different speeds at different altitudes)
        layer.shearVertical = sin(timeMs * 0.0005f + layer.hashCode() * 0.7f) * 0.5f + 0.5f
    }
}
```

**Wind visualization**: Add subtle indicators for wind:
- Tree sway amplitude tied to surface wind speed
- Grass bending direction matches wind direction
- Dust/leaf particles blowing in the wind (during dry conditions)
- Snow/rain angle matches wind at precipitation altitude

---

## 7. Precipitation Physics

### 7.1 Rain — Particle System

```kotlin
data class Raindrop(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,  // velocity (wind-affected)
    var length: Float,              // streak length (persistence of vision)
    var alpha: Float,               // opacity (fades near ground in heavy rain)
    var splashProgress: Float       // 0..1, splash animation on ground impact
)

fun simulateRain(
    drops: MutableList<Raindrop>,
    intensity: Float,          // 0..1 rain rate
    wind: WindLayer,
    deltaSec: Float
) {
    val spawnRate = (intensity * 500 * deltaSec).toInt()
    for (i in 0 until spawnRate) {
        drops.add(Raindrop(
            x = Random.nextFloat(),
            y = -0.05f,  // start above screen
            vx = wind.baseSpeed * 0.3f * cos(wind.direction),
            vy = 10f + intensity * 5f,  // terminal velocity
            length = 15f + intensity * 10f,
            alpha = 0.3f + intensity * 0.7f,
            splashProgress = 0f
        ))
    }
    
    for (drop in drops) {
        drop.x += drop.vx * deltaSec * 15f
        drop.y += drop.vy * deltaSec * 15f
        
        // Wind shear — upper drops pushed more
        val altitudeFactor = 1f - drop.y  // top of screen = 1
        drop.vx += wind.gustFactor * altitudeFactor * deltaSec * 2f
        
        // Ground splash
        if (drop.y > 0.82f) {
            drop.splashProgress += deltaSec * 3f
            if (drop.splashProgress > 1f) {
                drop.alpha = 0f  // remove
            }
        }
        
        // Remove off-screen
        if (drop.y > 1f || drop.x < -0.1f || drop.x > 1.1f) {
            drop.alpha = 0f
        }
    }
    
    drops.removeAll { it.alpha <= 0f }
    // Cap max drops
    while (drops.size > 800) drops.removeAt(0)
}
```

### 7.2 Snow — Flutter Physics

Snowflakes have **mass, drag, and flutter** — they don't fall straight down:

```kotlin
data class Snowflake(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var flutterPhase: Float,   // horizontal oscillation
    var flutterSpeed: Float,   // oscillation frequency (varies by flake size)
    var size: Float,           // visual radius
    var rotation: Float,       // rotation angle
    var alpha: Float,          // opacity
    var meltProgress: Float    // 0..1, melts when temperature > 0
)

fun simulateSnowflake(flake: Snowflake, tempC: Float, wind: WindLayer, deltaSec: Float) {
    // Flutter — horizontal oscillation as flake falls
    flake.flutterPhase += deltaSec * flake.flutterSpeed
    flake.vx = wind.baseSpeed * 0.1f + sin(flake.flutterPhase) * 0.5f
    
    // Terminal velocity (snow is slow)
    flake.vy = 0.5f + flake.size * 0.3f  // larger flakes fall faster
    
    // Rotation — slow tumble
    flake.rotation += deltaSec * (20f + Random.nextFloat() * 40f)
    
    // Melt
    if (tempC > 0f) {
        flake.meltProgress += deltaSec * 0.3f * tempC
        if (flake.meltProgress > 1f) {
            // Snowflake becomes raindrop
            flake.alpha = 0f  // transition to rain
        }
    }
}
```

---

## 8. Scene Integration

### 8.1 Animation Pipeline (replacing all separate scenes)

Replace `DayCloudyScene`, `NightCloudyScene`, `CloudyScene`, `RainScene`, `SnowScene`, `ThunderstormScene` with a **unified simulation-driven renderer**:

```kotlin
@Composable
fun PhysicsWeatherScene(
    weatherCode: Int,
    temperature: Double?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    timeOfDay: TimeOfDay
) {
    // 1. Initialize atmospheric state from weather code
    val atmosphere = remember(weatherCode) { createAtmosphereFromCode(weatherCode, temperature) }
    
    // 2. Cloud particle system
    val clouds = remember { mutableStateListOf<CloudParticle>() }
    
    // 3. Precipitation systems
    val raindrops = remember { mutableStateListOf<Raindrop>() }
    val snowflakes = remember { mutableStateListOf<Snowflake>() }
    
    // 4. Lightning state
    var activeLightning by remember { mutableStateOf<LightningBolt?>(null) }
    
    // 5. Simulation loop — runs every frame via LaunchedEffect with frame callback
    LaunchedEffect(Unit) {
        while (true) {
            val frameStart = System.nanoTime()
            
            // Physics step
            simulateAtmosphere(clouds, atmosphere, frameIntervalMs)
            simulatePrecipitation(raindrops, snowflakes, atmosphere, weatherCode, frameIntervalMs)
            activeLightning = checkLightningTrigger(clouds, atmosphere, frameIntervalMs) 
                ?: activeLightning?.also { propagateLightningBolt(it, frameIntervalMs / 1000f, size) }
            
            // Clean up expired lightning
            if (activeLightning?.alpha != null && activeLightning!!.alpha <= 0f) {
                activeLightning = null
            }
            
            frameIntervalMs = (System.nanoTime() - frameStart) / 1_000_000
            delay(16) // ~60fps target
        }
    }
    
    // 6. Render everything
    Canvas(modifier = modifier.fillMaxSize()) {
        // Background gradient (existing)
        drawSkyGradient(palette, size)
        
        // Cloud layers (back to front by z-order)
        val sortedClouds = clouds.sortedBy { it.z }
        for (cloud in sortedClouds) {
            drawOrganicCloud(this, cloud, palette, System.currentTimeMillis())
        }
        
        // Lightning
        activeLightning?.let { drawLightningFlash(this, it, palette) }
        
        // Precipitation
        for (drop in raindrops) drawRaindrop(this, drop)
        for (flake in snowflakes) drawSnowflake(this, flake, palette)
        
        // Ground (existing)
        drawGround(palette, weatherCode, ...)
    }
}
```

### 8.2 Weather Code → Atmosphere Mapping

```kotlin
fun createAtmosphereFromCode(code: Int, tempC: Double?): AtmosphericState {
    return when {
        code in 0..1 -> AtmosphericState(          // Clear / mainly clear
            surfaceHumidity = 0.2f, midHumidity = 0.3f, highHumidity = 0.1f,
            surfaceWind = WindLayer(2f, 270f, 0.2f, 0f),
            midWind = WindLayer(5f, 280f, 0.3f, 0.1f),
            highWind = WindLayer(15f, 290f, 0.4f, 0.2f),
            thermalStrength = 0.6f, electricFieldStrength = 0f,
            breakdownThreshold = 10f
        )
        code in 2..3 -> AtmosphericState(          // Partly cloudy / overcast
            surfaceHumidity = 0.6f, midHumidity = 0.7f, highHumidity = 0.4f,
            surfaceWind = WindLayer(4f, 250f, 0.4f, 0.2f),
            midWind = WindLayer(8f, 260f, 0.5f, 0.3f),
            highWind = WindLayer(20f, 270f, 0.6f, 0.4f),
            thermalStrength = 0.3f, electricFieldStrength = 0f,
            breakdownThreshold = 10f
        )
        code in 45..48 -> AtmosphericState(        // Fog
            surfaceHumidity = 0.95f, midHumidity = 0.8f, highHumidity = 0.3f,
            surfaceWind = WindLayer(1f, 180f, 0.1f, 0f),
            midWind = WindLayer(3f, 200f, 0.2f, 0.1f),
            highWind = WindLayer(10f, 220f, 0.3f, 0.2f),
            thermalStrength = 0.05f, electricFieldStrength = 0f,
            breakdownThreshold = 10f
        )
        code in 95..99 -> AtmosphericState(        // Thunderstorm
            surfaceHumidity = 0.9f, midHumidity = 0.95f, highHumidity = 0.7f,
            surfaceWind = WindLayer(8f, 220f, 0.8f, 0.6f),
            midWind = WindLayer(15f, 230f, 0.9f, 0.8f),
            highWind = WindLayer(30f, 240f, 1.0f, 0.9f),
            thermalStrength = 0.9f, electricFieldStrength = 0f,
            breakdownThreshold = 3f  // Low threshold = frequent lightning
        )
        // ... other codes
        else -> AtmosphericState(...)
    }
}
```

---

## 9. Visual Polish (Fixing "Bland")

### 9.1 Cloud Volume Rendering

Current clouds look flat. Add **pseudo-volume** with these techniques:

1. **Depth layering**: Sort clouds by z-order, draw back clouds first. Each layer has:
   - Back layer: low contrast, hazy, shifted toward sky color (atmospheric perspective)
   - Mid layer: moderate contrast, distinct shapes
   - Front layer: high contrast, crisp edges, warm highlights

2. **Solar illumination**: Compute angle from sun position:
   ```kotlin
   val sunAngle = angleBetween(sunPosition, cloudPosition)
   val illumination = cos(sunAngle).coerceIn(0.2f, 1f)
   // Top of cloud facing sun gets bright rim
   ```

3. **Shadow casting**: Large clouds cast shadows on ground and lower clouds:
   - Draw a dark semi-transparent ellipse on the ground below each cumulonimbus
   - Shadow moves as cloud drifts

4. **Edge glow**: Backlit clouds (sun behind) get a bright rim on their edge:
   ```kotlin
   // When cloud is between viewer and sun
   val backlitIntensity = (1f - illumination).pow(2f) * sunBrightness
   // Draw bright rim on cloud edge
   ```

### 9.2 Horizon Detail

Add richness to the horizon:
- **Heat shimmer** (visible on hot days, code 0 with temp > 30°C):
  ```kotlin
  // Animated distortion at horizon line using sine displacement
  val shimmer = sin(timeMs * 0.002f + x * 0.002f) * 2f * temperatureFactor
  ```
- **Distant rain shafts** (visible under distant cumulonimbus):
  ```kotlin
  // Subtle diagonal grey streaks below distant storm clouds
  ```
- **Birds** (keep existing, but make their flight paths random, not looped)

### 9.3 Fog Rendering (code 45-48)

Replace the current fog implementation with **volumetric fog**:
```kotlin
// Fog is a density field, not a colored overlay
// Dense near ground, thinning with altitude
fun drawVolumetricFog(drawScope: DrawScope, fogDensity: Float, palette: WeatherPalette) {
    val fogColor = palette.hazeColor.copy(alpha = 0.3f * fogDensity)
    
    // Ground-hugging layer
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(fogColor, Color.Transparent),
            startY = drawScope.size.height * 0.75f,
            endY = drawScope.size.height * 0.5f
        ),
        size = drawScope.size
    )
    
    // Swirling density variation
    val swirlCount = 5
    for (i in 0 until swirlCount) {
        val swirlX = sin(timeMs * 0.0003f + i * 1.7f) * drawScope.size.width * 0.3f + drawScope.size.width * 0.5f
        val swirlY = drawScope.size.height * (0.65f + sin(timeMs * 0.0005f + i * 2.3f) * 0.05f)
        drawScope.drawCircle(
            color = fogColor.copy(alpha = 0.15f * fogDensity),
            radius = drawScope.size.width * (0.1f + sin(timeMs * 0.0004f + i) * 0.05f),
            center = Offset(swirlX, swirlY)
        )
    }
}
```

### 9.4 Hail (code specific)

For severe thunder codes (>=96), add hail:
```kotlin
data class Hailstone(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float,        // 3-10x larger than raindrop
    var rotation: Float,
    var alpha: Float
)
// Hail bounces off ground, doesn't splash
```

---

## 10. Performance Budget

| Particle Type | Max Count | Update Rate | Draw Cost |
|---|---|---|---|
| Cloud particles | 25 | Every frame | Medium (organic shapes) |
| Raindrops | 800 | Every frame | Low (lines) |
| Snowflakes | 300 | Every frame | Low (circles) |
| Lightning bolts | 3 | as needed | Medium (lines + glow) |
| Dust motes | 40 | Every 2 frames | Very low (dots) |

**Optimization techniques**:
- Cull particles outside screen bounds
- Reduce draw calls by batching same-type particles
- Use LOD (level of detail): fewer particles when compact=true
- Pool particle objects to reduce GC pressure

---

## 11. Implementation Checklist

### Phase 1: Core Physics Engine
- [ ] Define `CloudParticle` data class with all physical properties
- [ ] Define `AtmosphericState` with humidity, wind, thermal, and electrical fields
- [ ] Implement `WindLayer` with gust evolution and vertical shear
- [ ] Implement `simulateAtmosphere()` — the main physics loop
- [ ] Implement cloud spawning (emergence from humidity)
- [ ] Implement cloud growth/evaporation (water mass transfer)
- [ ] Implement cloud dissipation (fade and shrink)
- [ ] Implement cloud collision detection and mass merging
- [ ] Implement horizontal wrapping with edge fade

### Phase 2: Cloud Rendering
- [ ] Implement `drawOrganicCloud()` with evolving control points
- [ ] Implement cumulus flat-bottom rendering
- [ ] Implement cirrus wispy streak rendering
- [ ] Implement stratus flat-layer rendering
- [ ] Implement cumulonimbus anvil top rendering
- [ ] Add solar illumination (bright rims, dark bases)
- [ ] Add shadow casting for large clouds
- [ ] Add backlight edge glow
- [ ] Add depth layering by z-order

### Phase 3: Lightning & Thunderstorms
- [ ] Implement stepped leader physics (fractal path)
- [ ] Implement branching forks
- [ ] Implement charge accumulation in cumulonimbus
- [ ] Implement dielectric breakdown model
- [ ] Implement lightning flash rendering (core + glow + screen flash)
- [ ] Implement thunder delay timing
- [ ] Implement cloud-to-cloud and intra-cloud lightning
- [ ] Add screen flash effect

### Phase 4: Precipitation
- [ ] Implement raindrop particle system
- [ ] Implement wind-affected rain angle
- [ ] Implement ground splash
- [ ] Implement snowflake flutter physics
- [ ] Implement snow melt transition
- [ ] Implement hail for severe storms
- [ ] Add distant rain shafts under storm clouds

### Phase 5: Visual Richness
- [ ] Add heat shimmer at horizon
- [ ] Implement volumetric fog (density field, swirl)
- [ ] Add wind indicators (grass bend, trees, dust)
- [ ] Enhance mountain atmospheric perspective
- [ ] Add distant rain shaft streaks
- [ ] Add cloud iridescence (thin cloud edges showing rainbow colors)

### Phase 6: Integration & Cleanup
- [ ] Replace all scene composables with unified `PhysicsWeatherScene`
- [ ] Map all WMO weather codes to atmospheric states
- [ ] Remove old `infiniteRepeatable` animation code
- [ ] Add performance instrumentation
- [ ] Test on low-end devices
- [ ] Verify dark mode and light mode
- [ ] Remove `DayCloudyScene`, `NightCloudyScene`, `CloudyScene`, `RainScene`, `SnowScene`, `ThunderstormScene` functions

---

## 12. Comparison: Current vs Physics-Based

| Aspect | Current Implementation | Physics-Based Implementation |
|---|---|---|
| **Cloud motion** | `infiniteRepeatable(tween(18000), Restart)` loops | Continuous drift with wind field + gusts |
| **Cloud shape** | Fixed circle clusters | Organic control points evolving with physics |
| **Cloud count** | Fixed by `cloudIntensity * 3` | Emerges from humidity and thermal conditions |
| **Cloud types** | Cumulus only (with enum unused) | Cumulus, Stratus, Cirrus, Cumulonimbus all active |
| **Transitions** | Instant between scenes | Gradual emergence and dissipation |
| **Lightning** | No physics, likely timed | Dielectric breakdown model |
| **Thunder** | None | Delay = distance / speed of sound |
| **Rain** | Simple line loops | Particle system with wind, splash, intensity variation |
| **Wind** | Single `cloudOffset` float | Multi-layer wind field with gusts and shear |
| **Performance** | Low (few particles) | Moderate (needs culling and pooling) |
| **Visual richness** | Minimal — "bland" as noted | 3D volume, shadows, backlight, heat shimmer, iridescence |

---

## 13. Testing Criteria

After implementation, verify:

1. **Clouds never repeat the same pattern** — watch for 5 minutes, no visible loop
2. **Clouds merge realistically** — two clouds passing close visually blend
3. **Thunder always follows lightning** — visual-to-audio delay is proportional
4. **Storm buildup feels gradual** — cumulus → cumulonimbus transition looks natural
5. **Fog hugs the ground** — dense at surface, thin above
6. **FPS stays above 30** — even on mid-range devices with max particles
7. **No visual popping** — all appearance/disappearance uses alpha fade
8. **Wind direction changes logically** — all particles respond consistently
9. **Rain angle matches wind** — visible correlation
10. **Scene feels alive** — always something changing somewhere in the viewport
