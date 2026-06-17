package fieldmind.research.app.features.field.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure-Kotlin image analysis engine for offline species identification.
 *
 * Uses three complementary techniques — all implemented without any ML/AI dependency:
 *
 * 1. **Color Histogram Analysis** — Extracts dominant colors and full RGB/HSV histograms,
 *    then compares against species-level reference profiles. Birds tend to have specific
 *    color palettes (e.g. red + black for Northern Cardinal, blue + white for Blue Jay).
 *
 * 2. **Edge Density / Texture Analysis** — Measures fine-detail ratio, average edge
 *    magnitude, and directional uniformity. Feathers vs. fur vs. leaves produce distinct
 *    texture signatures.
 *
 * 3. **Perceptual Hashing (pHash)** — Generates a 64-bit fingerprint of the image and
 *    compares hamming distance against reference hashes. Useful for matching against
 *    previously identified observations.
 *
 * Each method produces a confidence score; scores are fused into a final ranking.
 */
class SpeciesImageAnalyzer(
    private val context: Context
) {
    companion object {
        private const val THUMB_SIZE = 256        // px — analysis resolution
        private const val HISTOGRAM_BINS = 32      // bins per channel (32³ = 32K)
        private const val EDGE_THRESHOLD = 30      // Sobel magnitude threshold
        private const val PHASH_SIZE = 8           // 8×8 DCT for perceptual hash

        // ── Species color profiles [commonName → list of dominant HSV ranges] ──
        // Each entry: (hueCenter, hueSpread, satMin, satMax, valMin, valMax, weight)
        // These are heuristic profiles — refined over time as users confirm IDs.
        private val SPECIES_COLOR_PROFILES: Map<String, List<ColorProfile>> = mapOf(
            "Northern Cardinal" to listOf(
                ColorProfile(0f, 15f, 0.50f, 1.0f, 0.40f, 0.90f, 3.0f),   // Red body
                ColorProfile(0f, 10f, 0f, 0.15f, 0.05f, 0.30f, 1.5f),      // Black face mask
                ColorProfile(20f, 30f, 0.30f, 0.70f, 0.50f, 0.90f, 1.0f)   // Orange beak
            ),
            "Blue Jay" to listOf(
                ColorProfile(210f, 25f, 0.40f, 0.90f, 0.40f, 0.80f, 3.0f),  // Blue back/wings
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 2.0f),       // White belly
                ColorProfile(0f, 10f, 0f, 0.10f, 0.05f, 0.25f, 1.5f)        // Black markings
            ),
            "American Robin" to listOf(
                ColorProfile(10f, 20f, 0.40f, 0.85f, 0.40f, 0.80f, 3.0f),   // Red-orange breast
                ColorProfile(0f, 360f, 0f, 0.15f, 0.60f, 0.95f, 2.0f),      // Gray-brown back
                ColorProfile(20f, 40f, 0.30f, 0.60f, 0.50f, 0.80f, 1.5f)    // Yellow-orange beak
            ),
            "American Goldfinch" to listOf(
                ColorProfile(50f, 70f, 0.50f, 1.0f, 0.50f, 0.95f, 3.0f),    // Yellow body
                ColorProfile(0f, 10f, 0f, 0.10f, 0.05f, 0.20f, 2.0f),       // Black wings
                ColorProfile(0f, 10f, 0f, 0.10f, 0.80f, 1.0f, 1.0f)         // White wing bars
            ),
            "Eastern Bluebird" to listOf(
                ColorProfile(210f, 25f, 0.35f, 0.85f, 0.35f, 0.75f, 3.0f),  // Blue back
                ColorProfile(10f, 25f, 0.40f, 0.80f, 0.40f, 0.80f, 2.5f),   // Rusty breast
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 1.5f)        // White belly
            ),
            "Monarch Butterfly" to listOf(
                ColorProfile(25f, 35f, 0.60f, 1.0f, 0.40f, 0.90f, 3.0f),    // Orange wings
                ColorProfile(0f, 10f, 0f, 0.15f, 0.05f, 0.30f, 2.5f),       // Black veins/borders
                ColorProfile(0f, 360f, 0f, 0.10f, 0.80f, 1.0f, 2.0f)        // White spots
            ),
            "Ruby-throated Hummingbird" to listOf(
                ColorProfile(120f, 30f, 0.20f, 0.50f, 0.30f, 0.60f, 2.5f),  // Green body
                ColorProfile(340f, 20f, 0.50f, 1.0f, 0.30f, 0.70f, 3.0f),   // Ruby throat
                ColorProfile(0f, 360f, 0f, 0.15f, 0.60f, 0.90f, 1.5f)       // White/light belly
            ),
            "Common Dandelion" to listOf(
                ColorProfile(50f, 15f, 0.60f, 1.0f, 0.70f, 1.0f, 3.0f),     // Yellow flower
                ColorProfile(100f, 30f, 0.30f, 0.80f, 0.20f, 0.60f, 2.0f),  // Green leaves
                ColorProfile(0f, 360f, 0f, 0.15f, 0.80f, 1.0f, 1.0f)        // White puffball
            ),
            "Eastern Gray Squirrel" to listOf(
                ColorProfile(0f, 360f, 0f, 0.25f, 0.30f, 0.70f, 3.0f),      // Gray fur
                ColorProfile(30f, 20f, 0.20f, 0.50f, 0.40f, 0.80f, 1.5f),   // Brownish hints
                ColorProfile(0f, 360f, 0f, 0.10f, 0.70f, 1.0f, 1.5f)        // White belly
            ),
            "White-tailed Deer" to listOf(
                ColorProfile(30f, 20f, 0.15f, 0.40f, 0.40f, 0.75f, 3.0f),   // Brown body
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 2.0f),       // White tail/belly
                ColorProfile(20f, 15f, 0.20f, 0.50f, 0.20f, 0.50f, 1.0f)    // Darker legs/face
            ),
            "Red Fox" to listOf(
                ColorProfile(15f, 20f, 0.40f, 0.85f, 0.40f, 0.80f, 3.0f),   // Red-orange fur
                ColorProfile(0f, 360f, 0f, 0.15f, 0.80f, 1.0f, 2.0f),       // White tail tip/chest
                ColorProfile(0f, 10f, 0f, 0.10f, 0.05f, 0.25f, 2.0f)        // Black ears/legs
            ),
            "Monarch Butterfly" to listOf(
                ColorProfile(25f, 35f, 0.60f, 1.0f, 0.40f, 0.90f, 3.0f),
                ColorProfile(0f, 10f, 0f, 0.15f, 0.05f, 0.30f, 2.5f),
                ColorProfile(0f, 360f, 0f, 0.10f, 0.80f, 1.0f, 2.0f)
            ),
            "Honey Bee" to listOf(
                ColorProfile(40f, 15f, 0.50f, 0.90f, 0.50f, 0.85f, 3.0f),   // Yellow bands
                ColorProfile(0f, 10f, 0f, 0.15f, 0.05f, 0.25f, 3.0f),       // Black bands
                ColorProfile(0f, 360f, 0f, 0.10f, 0.60f, 0.90f, 1.0f)       // Brown/tan
            ),
            "Red Maple" to listOf(
                ColorProfile(0f, 15f, 0.40f, 0.90f, 0.30f, 0.80f, 3.0f),    // Red leaves/flowers
                ColorProfile(90f, 20f, 0.20f, 0.60f, 0.20f, 0.60f, 2.0f),   // Green leaves
                ColorProfile(30f, 15f, 0.10f, 0.35f, 0.20f, 0.50f, 1.5f)    // Brown bark
            ),
            "Eastern Cottontail" to listOf(
                ColorProfile(30f, 20f, 0.15f, 0.40f, 0.40f, 0.75f, 2.5f),   // Brown body
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 3.0f),       // White cotton tail
                ColorProfile(0f, 360f, 0f, 0.10f, 0.70f, 0.95f, 1.5f)       // White belly
            ),
            "Raccoon" to listOf(
                ColorProfile(0f, 360f, 0f, 0.20f, 0.30f, 0.60f, 3.0f),      // Gray fur
                ColorProfile(0f, 10f, 0f, 0.10f, 0.05f, 0.20f, 3.0f),       // Black mask
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 2.0f)        // White face
            ),
            "Peregrine Falcon" to listOf(
                ColorProfile(0f, 360f, 0f, 0.15f, 0.35f, 0.65f, 3.0f),     // Gray back
                ColorProfile(0f, 360f, 0f, 0.15f, 0.70f, 1.0f, 2.5f),      // White chest
                ColorProfile(0f, 10f, 0f, 0.10f, 0.10f, 0.35f, 2.0f)       // Dark markings
            ),
            "Great Horned Owl" to listOf(
                ColorProfile(30f, 25f, 0.15f, 0.40f, 0.25f, 0.55f, 3.0f),  // Brown body
                ColorProfile(40f, 20f, 0.10f, 0.30f, 0.40f, 0.70f, 2.0f),  // Tawny face
                ColorProfile(50f, 15f, 0.30f, 0.60f, 0.50f, 0.80f, 2.5f)   // Yellow eyes
            ),
            "Bald Eagle" to listOf(
                ColorProfile(0f, 360f, 0f, 0.10f, 0.70f, 1.0f, 3.0f),      // White head/tail
                ColorProfile(30f, 20f, 0.10f, 0.30f, 0.15f, 0.40f, 3.0f),  // Brown body
                ColorProfile(50f, 15f, 0.40f, 0.80f, 0.50f, 0.90f, 2.5f)   // Yellow beak
            ),
            "Mallard" to listOf(
                ColorProfile(110f, 20f, 0.30f, 0.70f, 0.20f, 0.50f, 3.0f), // Green head (male)
                ColorProfile(30f, 20f, 0.15f, 0.40f, 0.35f, 0.70f, 2.5f),  // Brown body
                ColorProfile(0f, 360f, 0f, 0.10f, 0.70f, 1.0f, 2.0f)       // White ring
            ),
            "Wild Turkey" to listOf(
                ColorProfile(30f, 20f, 0.10f, 0.30f, 0.20f, 0.50f, 3.0f),  // Brown body
                ColorProfile(0f, 10f, 0f, 0.10f, 0.05f, 0.20f, 2.5f),      // Black tips
                ColorProfile(0f, 10f, 0.40f, 0.80f, 0.30f, 0.60f, 1.0f)    // Red wattles
            )
        )

        // ── Texture profiles ──
        // Average edge magnitude ranges for broad categories
        private val TEXTURE_PROFILES: Map<String, TextureProfile> = mapOf(
            "Bird" to TextureProfile(0.08f, 0.25f, 3.0f),      // Feathers: moderate detail
            "Mammal" to TextureProfile(0.05f, 0.18f, 2.5f),    // Fur: lower detail
            "Insect" to TextureProfile(0.12f, 0.35f, 3.5f),    // Exoskeleton: high detail
            "Arachnid" to TextureProfile(0.10f, 0.30f, 3.0f),  // Similar to insect
            "Plant" to TextureProfile(0.06f, 0.22f, 2.5f),     // Leaves: moderate
            "Fungi" to TextureProfile(0.04f, 0.15f, 2.0f),     // Smooth surfaces
            "Reptile" to TextureProfile(0.10f, 0.30f, 3.0f),   // Scales: high detail
            "Amphibian" to TextureProfile(0.06f, 0.20f, 2.5f), // Smooth skin
            "Fish" to TextureProfile(0.08f, 0.25f, 2.5f),      // Scales: moderate
            "Mollusk" to TextureProfile(0.04f, 0.15f, 2.0f)    // Shells: smooth
        )
    }

    data class ColorProfile(
        val hueCenter: Float,       // 0-360
        val hueSpread: Float,       // +/- degrees
        val satMin: Float,          // 0-1
        val satMax: Float,          // 0-1
        val valMin: Float,          // 0-1
        val valMax: Float,          // 0-1
        val weight: Float           // importance of this zone
    )

    data class TextureProfile(
        val edgeDensityMin: Float,  // fraction of edge pixels (Sobel above threshold)
        val edgeDensityMax: Float,
        val weight: Float
    )

    /**
     * Extracted feature vector from an image.
     */
    data class ImageFeatures(
        val histogram: FloatArray,          // RGB histogram (3 × HISTOGRAM_BINS)
        val dominantColors: List<FloatArray>, // Top 5 HSV colors (hue, sat, val, fraction)
        val edgeDensity: Float,             // fraction of pixels with significant gradient
        val avgEdgeMagnitude: Float,        // average Sobel magnitude (0-1 normalized)
        val perceptualHash: Long,           // 64-bit pHash fingerprint
        val textureUniformity: Float        // how uniform the texture direction is (0-1)
    )

    /**
     * Analyze an image from a URI and return extracted features.
     */
    suspend fun analyzeImage(imageUri: String): ImageFeatures? = withContext(Dispatchers.Default) {
        val bitmap = loadBitmap(imageUri) ?: return@withContext null
        val thumb = Bitmap.createScaledBitmap(bitmap, THUMB_SIZE, THUMB_SIZE, true)

        val pixels = IntArray(THUMB_SIZE * THUMB_SIZE)
        thumb.getPixels(pixels, 0, THUMB_SIZE, 0, 0, THUMB_SIZE, THUMB_SIZE)

        val histogram = computeHistogram(pixels)
        val dominantColors = computeDominantColors(pixels)
        val edgeDensity = computeEdgeDensity(pixels)
        val avgEdgeMagnitude = computeAvgEdgeMagnitude(pixels)
        val perceptualHash = computePerceptualHash(pixels)
        val textureUniformity = computeTextureUniformity(pixels)

        thumb.recycle()
        bitmap.recycle()

        ImageFeatures(
            histogram = histogram,
            dominantColors = dominantColors,
            edgeDensity = edgeDensity,
            avgEdgeMagnitude = avgEdgeMagnitude,
            perceptualHash = perceptualHash,
            textureUniformity = textureUniformity
        )
    }

    /**
     * Compare image features against species profiles and return ranked matches.
     *
     * @param features The extracted image features.
     * @param candidates Species to compare against.
     * @param topK Maximum number of results.
     * @param phashBoosts Optional map of speciesName → (boost amount, isStrictMatch)
     *        from [PhashDatabase.computeHashBoosts]. Stored fingerprint matches are
     *        added on top of the color/texture analysis scores.
     */
    suspend fun scoreAgainstSpecies(
        features: ImageFeatures,
        candidates: List<SpeciesRecord>,
        topK: Int = 10,
        phashBoosts: Map<String, Pair<Float, Boolean>> = emptyMap()
    ): List<SpeciesMatch> = withContext(Dispatchers.Default) {
        val scored = candidates.map { species ->
            val colorScore = colorProfileScore(species.commonName, features.dominantColors)
            val textureScore = textureCategoryScore(species.category, features.edgeDensity)

            // Base score from color + texture analysis
            var confidence = (
                colorScore * 0.50f +
                textureScore * 0.20f +
                0.05f  // base score from fallback
            ).coerceIn(0f, 0.95f)

            // ── pHash boost from stored user-confirmed fingerprints ──
            val hashBoost = phashBoosts[species.commonName]
            if (hashBoost != null) {
                val (boost, isStrict) = hashBoost
                // Progressive boost: each matching confirmation adds diminishing returns
                confidence = (confidence + boost * (1f - confidence)).coerceIn(0f, 0.99f)
            }

            SpeciesMatch(
                commonName = species.commonName,
                scientificName = species.scientificName,
                confidence = confidence,
                category = species.category,
                description = species.description
            )
        }

        scored
            .filter { it.confidence >= 0.05f }
            .sortedByDescending { it.confidence }
            .take(topK)
    }

    /**
     * Quick category prediction based on edge/texture analysis alone.
     * Returns (category, confidence) pairs.
     */
    suspend fun predictCategory(features: ImageFeatures): List<Pair<String, Float>> =
        withContext(Dispatchers.Default) {
            TEXTURE_PROFILES.map { (category, profile) ->
                val score = if (features.edgeDensity in profile.edgeDensityMin..profile.edgeDensityMax) {
                    // Higher score when density is close to center of range
                    val range = profile.edgeDensityMax - profile.edgeDensityMin
                    val center = (profile.edgeDensityMax + profile.edgeDensityMin) / 2f
                    val distance = abs(features.edgeDensity - center) / (range / 2f)
                    (1f - distance.coerceIn(0f, 1f)) * 0.6f
                } else {
                    // Some score even outside range (fuzzy boundary)
                    val closest = when {
                        features.edgeDensity < profile.edgeDensityMin -> profile.edgeDensityMin - features.edgeDensity
                        else -> features.edgeDensity - profile.edgeDensityMax
                    }
                    (0.3f - closest * 3f).coerceIn(0f, 0.3f)
                }
                category to score
            }.sortedByDescending { it.second }.take(3)
        }

    // ── Private helpers ──

    private fun loadBitmap(imageUri: String): Bitmap? {
        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Cannot open $imageUri")
            BitmapFactory.decodeStream(inputStream).also { inputStream.close() }
        } catch (_: Exception) {
            // Try as file path
            try {
                BitmapFactory.decodeFile(imageUri)
            } catch (_: Exception) { null }
        }
    }

    /**
     * Compute a 3-channel RGB histogram (HISTOGRAM_BINS per channel).
     * Normalized so all bins sum to 1.0.
     */
    private fun computeHistogram(pixels: IntArray): FloatArray {
        val hist = FloatArray(HISTOGRAM_BINS * 3) { 0f }
        val binWidth = 256f / HISTOGRAM_BINS
        val total = pixels.size.toFloat()

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val rBin = (r / binWidth).toInt().coerceIn(0, HISTOGRAM_BINS - 1)
            val gBin = (g / binWidth).toInt().coerceIn(0, HISTOGRAM_BINS - 1)
            val bBin = (b / binWidth).toInt().coerceIn(0, HISTOGRAM_BINS - 1)

            hist[rBin] += 1f
            hist[HISTOGRAM_BINS + gBin] += 1f
            hist[2 * HISTOGRAM_BINS + bBin] += 1f
        }

        // Normalize
        for (i in hist.indices) hist[i] /= total
        return hist
    }

    /**
     * Extract up to 5 dominant colors using a simple clustering approach
     * on HSV values. Returns (hue, sat, val, fraction) for each cluster.
     */
    private fun computeDominantColors(pixels: IntArray, maxColors: Int = 5): List<FloatArray> {
        // Simplified k-means with k=5 on pixel HSV values
        val hsvValues = pixels.map { pixel ->
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6)
                max == g -> 60f * (((b - r) / delta) + 2)
                else -> 60f * (((r - g) / delta) + 4)
            }.let { if (it < 0) it + 360 else it }

            val sat = if (max == 0f) 0f else delta / max
            floatArrayOf(hue, sat, max)
        }

        // Simple color binning: quantize to coarse grid and pick top bins
        val colorBins = mutableMapOf<Int, MutableList<FloatArray>>()
        for (hsv in hsvValues) {
            val hBin = (hsv[0] / 30f).toInt().coerceIn(0, 11)  // 12 hue bins
            val sBin = (hsv[1] * 2f).toInt().coerceIn(0, 2)     // 3 saturation bins
            val vBin = (hsv[2] * 2f).toInt().coerceIn(0, 2)     // 3 value bins
            val key = (hBin * 9) + (sBin * 3) + vBin
            colorBins.getOrPut(key) { mutableListOf() }.add(hsv)
        }

        val totalPixels = hsvValues.size
        colorBins.entries
            .sortedByDescending { it.value.size }
            .take(maxColors)
            .map { (_, pixelsInBin) ->
                val avgHue = pixelsInBin.map { it[0] }.average().toFloat()
                val avgSat = pixelsInBin.map { it[1] }.average().toFloat()
                val avgVal = pixelsInBin.map { it[2] }.average().toFloat()
                val fraction = pixelsInBin.size.toFloat() / totalPixels
                floatArrayOf(avgHue, avgSat, avgVal, fraction)
            }
    }

    /**
     * Compute edge density using simple Sobel-like gradient approximation.
     * Returns fraction of pixels with gradient magnitude above threshold.
     */
    private fun computeEdgeDensity(pixels: IntArray): Float {
        val width = THUMB_SIZE
        val height = THUMB_SIZE
        val gray = IntArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        var edgeCount = 0
        var totalMagnitude = 0f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // Simple horizontal + vertical gradient (Sobel-like with 3x3)
                val gx = (
                    -gray[(y - 1) * width + (x - 1)] + gray[(y - 1) * width + (x + 1)]
                    -2 * gray[y * width + (x - 1)] + 2 * gray[y * width + (x + 1)]
                    -gray[(y + 1) * width + (x - 1)] + gray[(y + 1) * width + (x + 1)]
                )
                val gy = (
                    -gray[(y - 1) * width + (x - 1)] - 2 * gray[(y - 1) * width + x] - gray[(y - 1) * width + (x + 1)]
                    + gray[(y + 1) * width + (x - 1)] + 2 * gray[(y + 1) * width + x] + gray[(y + 1) * width + (x + 1)]
                )
                val mag = sqrt((gx * gx + gy * gy).toFloat()) / 4f // normalized to ~0-255
                totalMagnitude += mag
                if (mag > EDGE_THRESHOLD) edgeCount++
            }
        }

        val totalPixels = (height - 2) * (width - 2)
        return edgeCount.toFloat() / totalPixels
    }

    /**
     * Compute average edge magnitude across the image (0-1 normalized).
     */
    private fun computeAvgEdgeMagnitude(pixels: IntArray): Float {
        val width = THUMB_SIZE
        val height = THUMB_SIZE
        val gray = IntArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        var totalMag = 0f
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = (
                    -gray[(y - 1) * width + (x - 1)] + gray[(y - 1) * width + (x + 1)]
                    -2 * gray[y * width + (x - 1)] + 2 * gray[y * width + (x + 1)]
                    -gray[(y + 1) * width + (x - 1)] + gray[(y + 1) * width + (x + 1)]
                )
                val gy = (
                    -gray[(y - 1) * width + (x - 1)] - 2 * gray[(y - 1) * width + x] - gray[(y - 1) * width + (x + 1)]
                    + gray[(y + 1) * width + (x - 1)] + 2 * gray[(y + 1) * width + x] + gray[(y + 1) * width + (x + 1)]
                )
                totalMag += sqrt((gx * gx + gy * gy).toFloat())
                count++
            }
        }

        return (totalMag / count) / 255f  // normalize to 0-1
    }

    /**
     * Compute perceptual hash (pHash) — a 64-bit fingerprint.
     * Uses DCT-based approach: 8x8 DCT, compare against median.
     */
    private fun computePerceptualHash(pixels: IntArray): Long {
        val size = PHASH_SIZE
        val gray = IntArray(THUMB_SIZE * THUMB_SIZE) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        // Downsample to 8x8 by averaging blocks
        val blockSize = THUMB_SIZE / size
        val dctInput = FloatArray(size * size) { 0f }
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0
                var count = 0
                for (dy in 0 until blockSize) {
                    for (dx in 0 until blockSize) {
                        val py = y * blockSize + dy
                        val px = x * blockSize + dx
                        if (py < THUMB_SIZE && px < THUMB_SIZE) {
                            sum += gray[py * THUMB_SIZE + px]
                            count++
                        }
                    }
                }
                dctInput[y * size + x] = (sum.toFloat() / count) / 255f
            }
        }

        // Compute 8x8 DCT (simplified — only low frequencies matter for hashing)
        val dct = FloatArray(size * size) { 0f }
        for (u in 0 until size) {
            for (v in 0 until size) {
                var sum = 0f
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        val cu = if (u == 0) 1f / sqrt(2f) else 1f
                        val cv = if (v == 0) 1f / sqrt(2f) else 1f
                        val cosU = kotlin.math.cos(((2 * x + 1) * u * Math.PI) / (2 * size)).toFloat()
                        val cosV = kotlin.math.cos(((2 * y + 1) * v * Math.PI) / (2 * size)).toFloat()
                        sum += cu * cv * dctInput[y * size + x] * cosU * cosV
                    }
                }
                dct[v * size + u] = sum * 2f / size
            }
        }

        // Keep only lowest 8x8 (skip DC component at 0,0)
        val hashValues = mutableListOf<Float>()
        for (i in 1 until size * size) {
            hashValues.add(dct[i])
        }

        val median = hashValues.sorted().let { sorted ->
            if (sorted.size % 2 == 0) (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2f
            else sorted[sorted.size / 2]
        }

        // Generate 64-bit hash
        var hash = 0L
        for (i in hashValues.indices) {
            if (hashValues[i] > median) {
                hash = hash or (1L shl i)
            }
        }
        return hash
    }

    /**
     * Compute texture uniformity (directional coherence).
     * 0 = completely random/noise, 1 = completely uniform direction.
     */
    private fun computeTextureUniformity(pixels: IntArray): Float {
        val width = THUMB_SIZE
        val height = THUMB_SIZE
        val gray = IntArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        // Collect gradient orientations
        val angleHistogram = FloatArray(36) { 0f }  // 10-degree bins
        var totalEdges = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = (
                    -gray[(y - 1) * width + (x - 1)] + gray[(y - 1) * width + (x + 1)]
                    -2 * gray[y * width + (x - 1)] + 2 * gray[y * width + (x + 1)]
                    -gray[(y + 1) * width + (x - 1)] + gray[(y + 1) * width + (x + 1)]
                ).toFloat()
                val gy = (
                    -gray[(y - 1) * width + (x - 1)] - 2 * gray[(y - 1) * width + x] - gray[(y - 1) * width + (x + 1)]
                    + gray[(y + 1) * width + (x - 1)] + 2 * gray[(y + 1) * width + x] + gray[(y + 1) * width + (x + 1)]
                ).toFloat()
                val mag = sqrt(gx * gx + gy * gy)
                if (mag > EDGE_THRESHOLD) {
                    val angle = Math.atan2(gy.toDouble(), gx.toDouble()).toFloat()
                    val bin = ((angle + Math.PI) / (2 * Math.PI) * 36).toInt().coerceIn(0, 35)
                    angleHistogram[bin] += mag
                    totalEdges++
                }
            }
        }

        if (totalEdges == 0) return 0.5f

        // Normalize and compute entropy-based uniformity
        for (i in angleHistogram.indices) angleHistogram[i] /= totalEdges

        val maxBin = angleHistogram.maxOrNull() ?: 0f
        val entropy = angleHistogram.sumOf { p ->
            if (p > 0f) -p * kotlin.math.log(p.toDouble(), 2.0) else 0.0
        }.toFloat()

        // Normalize: max entropy for 36 bins = log2(36) ≈ 5.17
        val maxEntropy = kotlin.math.log(36.0, 2.0).toFloat()
        return 1f - (entropy / maxEntropy)  // 1 = very uniform, 0 = completely random
    }

    /**
     * Score how well the image's dominant colors match a species profile.
     */
    private fun colorProfileScore(
        speciesName: String,
        dominantColors: List<FloatArray>
    ): Float {
        val profiles = SPECIES_COLOR_PROFILES[speciesName] ?: return 0f

        if (dominantColors.isEmpty() || profiles.isEmpty()) return 0f

        var totalScore = 0f
        var totalWeight = 0f

        for (profile in profiles) {
            var bestMatch = 0f
            for (color in dominantColors) {
                val hue = color[0]
                val sat = color[1]
                val `val` = color[2]

                // Compute hue distance (circular)
                val hueDiff = abs(hue - profile.hueCenter)
                val hueDist = min(hueDiff, 360f - hueDiff)

                if (hueDist <= profile.hueSpread && 
                    sat in profile.satMin..profile.satMax &&
                    `val` in profile.valMin..profile.valMax) {
                    val hueScore = (1f - hueDist / profile.hueSpread).coerceIn(0f, 1f)
                    val satScore = (1f - abs(sat - (profile.satMin + profile.satMax) / 2f) / (profile.satMax - profile.satMin + 0.01f)).coerceIn(0f, 1f)
                    val valScore = (1f - abs(`val` - (profile.valMin + profile.valMax) / 2f) / (profile.valMax - profile.valMin + 0.01f)).coerceIn(0f, 1f)
                    val match = (hueScore * 0.5f + satScore * 0.25f + valScore * 0.25f) * color[3] * 3f
                    bestMatch = maxOf(bestMatch, match)
                }
            }
            totalScore += bestMatch * profile.weight
            totalWeight += profile.weight
        }

        return if (totalWeight > 0f) (totalScore / totalWeight).coerceIn(0f, 1f) else 0f
    }

    /**
     * Score how well the image's texture matches a category profile.
     */
    private fun textureCategoryScore(category: String, edgeDensity: Float): Float {
        val profile = TEXTURE_PROFILES[category] ?: return 0f
        if (edgeDensity in profile.edgeDensityMin..profile.edgeDensityMax) {
            val range = profile.edgeDensityMax - profile.edgeDensityMin
            val center = (profile.edgeDensityMax + profile.edgeDensityMin) / 2f
            val distance = abs(edgeDensity - center) / (range / 2f).coerceAtLeast(0.01f)
            return (1f - distance.coerceIn(0f, 1f)) * 0.4f
        }
        return 0.1f
    }
}
