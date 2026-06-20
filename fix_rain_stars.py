#!/usr/bin/env nix-shell
#!nix-shell -p python3 -i python3
"""Fix rain visibility and add moon/stars/fireflies to rain/thunder scenes at night"""

import re

path = "app/src/main/java/chromahub/rhythm/app/features/field/presentation/components/AnimatedWeatherScene.kt"

with open(path, "r") as f:
    content = f.read()

fixes = []

# ─── Fix 1: Increase rain streak alpha and width for better visibility ───
# The rainColor alpha is too low. Increase it and make streaks wider.

# Find the rainColor definition
old_rain_color = """    val rainColor = Color(0xFFAAD4F0).copy(alpha = rainAlpha * intensity * 0.8f)"""
new_rain_color = """    val rainColor = Color(0xFFAAD4F0).copy(alpha = rainAlpha * (0.6f + intensity * 0.4f))"""

if old_rain_color in content:
    content = content.replace(old_rain_color, new_rain_color)
    fixes.append("Fix 1a: Increased rain streak alpha baseline (removed *0.8f multiplier)")
else:
    fixes.append("Fix 1a: WARNING - rainColor pattern not found")

# Find streak width and make it wider
old_streak_width = """        val streakWidth = 0.8f + depthFactor * 0.7f"""
new_streak_width = """        val streakWidth = 1.6f + depthFactor * 1.4f"""

if old_streak_width in content:
    content = content.replace(old_streak_width, new_streak_width)
    fixes.append("Fix 1b: Doubled rain streak width (0.8->1.6, 0.7->1.4)")
else:
    fixes.append("Fix 1b: WARNING - streakWidth pattern not found")

# Find streak count and increase it
old_streak_count = """    val streakCount = when {
        isHeavy -> 120
        isDrizzle -> 50
        else -> 80
    }"""
new_streak_count = """    val streakCount = when {
        isHeavy -> 200
        isDrizzle -> 100
        else -> 140
    }"""

if old_streak_count in content:
    content = content.replace(old_streak_count, new_streak_count)
    fixes.append("Fix 1c: Increased rain streak counts (120->200, 50->100, 80->140)")
else:
    fixes.append("Fix 1c: WARNING - streakCount pattern not found")

# Find rainAlpha and increase for drizzle
old_rain_alpha = """    val rainAlpha = if (isHeavy) 0.6f else 0.35f"""
new_rain_alpha = """    val rainAlpha = if (isHeavy) 0.7f else 0.55f"""

if old_rain_alpha in content:
    content = content.replace(old_rain_alpha, new_rain_alpha)
    fixes.append("Fix 1d: Increased rain alpha (0.6->0.7 heavy, 0.35->0.55 drizzle)")
else:
    fixes.append("Fix 1d: WARNING - rainAlpha pattern not found")

# ─── Fix 2: Add moon, stars, and fireflies to RainScene at night ───
# We need to find the section in RainScene's Canvas where the rain is drawn
# and add celestial objects BEFORE the clouds and rain.

# First, find the RainScene Canvas declaration and the first cloud drawing
# We need to add moon+stars at the very beginning of the Canvas, before clouds

# The RainScene Canvas starts with the background overlay
# Let's find the first drawing operations in RainScene's Canvas

# The Canvas starts after "Canvas(modifier = modifier.fillMaxSize()) {"
# The dark overlay and clouds come first, then rain streaks
# We need to add moon/stars/fireflies at the beginning of the Canvas

# Find the night section in RainScene - when timeOfDay is Night/Twilight
# Add after the Canvas opening brace

# Look for the pattern where Canvas begins in RainScene
old_canvas_begin = """    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Dark overlay for light mode (makes rain visible) ──"""

new_canvas_begin = """    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Celestial objects for night rain (moon + stars visible through clouds) ──
        val isNightRain = timeOfDay == TimeOfDay.Night || timeOfDay == TimeOfDay.Twilight
        if (isNightRain) {
            drawMoon(palette, timeOfDay, 0.6f + moonGlow * 0.4f, compact)
            // Pre-compute star positions
            val starBright = Color(0xFFB3E5FC)
            val starWarm = Color(0xFFFFF9C4)
            val nightStars = remember { rememberStarPositions(30) }
            nightStars.forEachIndexed { index, (x, y) ->
                val twinkle = (sin(moonGlow * (1.5f + index % 3) + index * 1.3f) * 0.5f + 0.5f)
                    .coerceIn(0.1f, 0.6f)
                val starColor = if (twinkle > 0.4f) starBright else starWarm
                drawCircle(
                    color = starColor.copy(alpha = twinkle * 0.5f),
                    radius = 0.8f + twinkle * 1.5f,
                    center = Offset(x * size.width, y * size.height * 0.4f)
                )
            }
        }

        // ── Overhead clouds for drizzle/rain ──"""

# Actually this is getting complex. Let me try a different approach.
# The mistmatch might be because the Canvas opening changed in our earlier fix.
# Let me check what's actually there now.

# Look for the section in RainScene's Canvas where the first thing is drawn
# From earlier, we know the Canvas starts, then clouds are drawn, then dark overlay
# Let me find the actual Cloud drawing section in RainScene

# Actually, let me take a step back. The issue is:
# 1. RainScene doesn't know about timeOfDay - it doesn't take that as a parameter!
# Let me check... Looking at the function signature from earlier analysis:
# private fun RainScene(weatherCode: Int, palette: WeatherPalette, compact: Boolean, timeOfDay: TimeOfDay, modifier: Modifier)
# Yes! It DOES take timeOfDay.

# So the fix is simpler - just add celestial bodies at the start of the Canvas.

# Let me find the exact text at the beginning of RainScene's Canvas
# After our Fix 5 from the previous script, the beginning of RainScene's Canvas is:
"""
    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Overhead clouds for drizzle/rain ──
"""

# We need to add moon+stars BEFORE these clouds when it's night
old_rain_canvas = """    Canvas(modifier = modifier.fillMaxSize()) {
        // ── Overhead clouds for drizzle/rain ──
        // Draw clouds so rain scenes have visible cloud cover overhead"""

new_rain_canvas = """    Canvas(modifier = modifier.fillMaxSize()) {
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
        // Draw clouds so rain scenes have visible cloud cover overhead"""

if old_rain_canvas in content:
    content = content.replace(old_rain_canvas, new_rain_canvas)
    fixes.append("Fix 2: Added moon + stars to RainScene's Canvas at night")
else:
    fixes.append("Fix 2: WARNING - RainScene Canvas pattern not found")
    # Debug: find what's actually there
    idx = content.find("Canvas(modifier = modifier.fillMaxSize())")
    idx2 = content.find("Canvas(modifier = modifier.fillMaxSize())", idx + 50)
    # Find the RainScene Canvas (second occurrence may be it)
    # Let's search for nearby text
    for i in range(3):
        idx = content.find("Canvas(modifier = modifier.fillMaxSize())", idx + (50 if idx > 0 else 0))
        snippet = content[idx:idx+400]
        fixes.append(f"  Canvas #{i+1} at {idx}: {snippet[:150]}...")

# ─── Fix 3: Add fireflies near ground in RainScene at night ───
# Find the drawGround + drawAtmosphericHaze section at end of RainScene's Canvas

# The user also wants fireflies at night in rain/thunder scenes

# Let me also add to the ThunderstormScene
# The ThunderstormScene already draws clouds and lightning, but at night
# there should be moon and stars visible through breaks
# Let me check if ThunderstormScene already has celestial objects

old_thunder_canvas = """    Canvas(modifier = modifier.fillMaxSize()) {
        val drift = (cloudDrift * 0.8f) % 1f"""

# Check if this pattern exists (likely changed after our earlier fix)
idx_thunder = content.find("Canvas(modifier = modifier.fillMaxSize())")
all_canvases = []
while idx_thunder >= 0:
    snippet = content[idx_thunder:idx_thunder+200]
    all_canvases.append(snippet)
    idx_thunder = content.find("Canvas(modifier = modifier.fillMaxSize())", idx_thunder + 50)

for i, s in enumerate(all_canvases):
    fixes.append(f"  Canvas #{i+1}: {s[:120]}...")

# Write back
with open(path, "w") as f:
    f.write(content)

print("Applied fixes:")
for fix in fixes:
    print(f"  - {fix}")
