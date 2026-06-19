package fieldmind.research.app.features.field.data.analysis

import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A detected pattern found in the user's observation data.
 */
data class DetectedPattern(
    val type: String,
    val label: String,
    val description: String,
    val confidence: Float,
    val count: Int = 0,
    val relatedSubjects: List<String> = emptyList(),
    val relatedCategories: List<String> = emptyList(),
    val relatedLocations: List<String> = emptyList(),
    val insight: String = ""
)

/**
 * Engine that runs entirely offline to detect meaningful patterns in observations.
 *
 * Detects:
 * - **Repeated subjects** — same species/topic observed 3+ times
 * - **Site revisits** — same location visited 2+ times
 * - **Temporal clusters** — observations grouped by time of day
 * - **Category trends** — most/least observed categories
 * - **Observation gaps** — long gaps since last observation in a category
 * - **Weather correlations** — weather conditions when observing
 */
object PatternDetectionEngine {

    private const val SAME_LOCATION_RADIUS_M = 100.0
    private const val MIN_REPEATED_SUBJECTS = 3
    private const val MIN_SITE_REVISITS = 2
    private const val GAP_DAYS_THRESHOLD = 14

    /**
     * Run all detectors and return ranked patterns sorted by confidence descending.
     */
    fun detectAll(observations: List<ObservationEntity>): List<DetectedPattern> {
        if (observations.size < 2) return emptyList()

        val results = mutableListOf<DetectedPattern>()

        results.addAll(detectRepeatedSubjects(observations))
        results.addAll(detectSiteRevisits(observations))
        results.addAll(detectTemporalClusters(observations))
        results.addAll(detectCategoryTrends(observations))
        results.addAll(detectObservationGaps(observations))
        results.addAll(detectWeatherCorrelations(observations))

        return results.sortedByDescending { it.confidence }
    }

    // ── 1. Repeated subject detection ──

    private fun detectRepeatedSubjects(observations: List<ObservationEntity>): List<DetectedPattern> {
        val grouped = observations
            .filter { it.subject.isNotBlank() }
            .groupBy { it.subject.trim().lowercase() }

        return grouped.entries
            .filter { it.value.size >= MIN_REPEATED_SUBJECTS }
            .sortedByDescending { it.value.size }
            .take(5)
            .map { (_, obsList) ->
                val sample = obsList.first()
                val count = obsList.size
                val categories = obsList.map { it.category }.distinct().filterNot { it.isBlank() }
                val timesOfDay = obsList.mapNotNull { extractTimeOfDay(it.time) }
                val timeSummary = if (timesOfDay.isNotEmpty()) timesOfDay.groupingBy { it }.eachCount().entries.maxByOrNull { it.value }?.key else null
                DetectedPattern(
                    type = "repeated_subject",
                    label = "Repeated: ${sample.subject}",
                    description = "$count observations of \"${sample.subject}\" across ${categories.size} categor${if (categories.size == 1) "y" else "ies"}",
                    confidence = (count / 15f).coerceAtMost(1f),
                    count = count,
                    relatedSubjects = listOf(sample.subject),
                    relatedCategories = categories,
                    insight = buildString {
                        append("You've observed \"${sample.subject}\" $count times")
                        if (timeSummary != null) append(", often during $timeSummary")
                        append(". Consider whether this species or subject is worth tracking as a dedicated project.")
                    }
                )
            }
    }

    // ── 2. Site revisit detection ──

    private fun detectSiteRevisits(observations: List<ObservationEntity>): List<DetectedPattern> {
        val withLocation = observations.filter { it.latitude != null && it.longitude != null }
        if (withLocation.size < 2) return emptyList()

        val sites = mutableListOf<MutableList<ObservationEntity>>()

        for (obs in withLocation) {
            var added = false
            for (site in sites) {
                val first = site.first()
                if (distanceMeters(obs.latitude!!, obs.longitude!!, first.latitude!!, first.longitude!!) <= SAME_LOCATION_RADIUS_M) {
                    site.add(obs)
                    added = true
                    break
                }
            }
            if (!added) {
                sites.add(mutableListOf(obs))
            }
        }

        return sites
            .filter { it.size >= MIN_SITE_REVISITS }
            .sortedByDescending { it.size }
            .take(5)
            .map { obsList ->
                val count = obsList.size
                val locs = obsList.mapNotNull { it.manualLocation.ifBlank { null } }.distinct()
                val locationLabel = locs.firstOrNull() ?: "(${obsList.first().latitude}, ${obsList.first().longitude})"
                val subjects = obsList.map { it.subject }.distinct().filterNot { it.isBlank() }
                DetectedPattern(
                    type = "site_revisit",
                    label = "Site revisited: $locationLabel",
                    description = "$count observations at $locationLabel across ${obsList.map { it.date }.distinct().size} different days",
                    confidence = (count / 10f).coerceAtMost(1f),
                    count = count,
                    relatedSubjects = subjects.take(3),
                    relatedLocations = listOf(locationLabel),
                    insight = "You've returned to $locationLabel $count times. Consider creating a dedicated site-monitoring project to track changes over time."
                )
            }
    }

    // ── 3. Temporal cluster detection ──

    private fun detectTemporalClusters(observations: List<ObservationEntity>): List<DetectedPattern> {
        val timeSlots = observations
            .mapNotNull { extractTimeOfDay(it.time) }
            .groupingBy { it }
            .eachCount()

        if (timeSlots.isEmpty()) return emptyList()

        val peak = timeSlots.maxByOrNull { it.value } ?: return emptyList()
        val total = timeSlots.values.sum().toFloat()
        val ratio = peak.value / total

        if (peak.value < 3 || ratio < 0.25f) return emptyList()

        return listOf(
            DetectedPattern(
                type = "temporal_cluster",
                label = "Peak observation time: ${peak.key}",
                description = "${peak.value} of $total observations (${(ratio * 100).roundToInt()}%) happen during ${peak.key}",
                confidence = ratio.coerceAtMost(1f),
                count = peak.value,
                insight = "You tend to observe most during ${peak.key}. Try scheduling observations at different times to capture a fuller picture."
            )
        )
    }

    // ── 4. Category trend detection ──

    private fun detectCategoryTrends(observations: List<ObservationEntity>): List<DetectedPattern> {
        val grouped = observations
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .mapValues { it.value.size }

        if (grouped.size < 2) return emptyList()

        val top = grouped.maxByOrNull { it.value } ?: return emptyList()
        val bottom = grouped.minByOrNull { it.value } ?: return emptyList()

        val results = mutableListOf<DetectedPattern>()

        // Top category
        results.add(
            DetectedPattern(
                type = "category_trend",
                label = "Most observed: ${top.key}",
                description = "${top.value} observations in \"${top.key}\" — your primary focus area",
                confidence = (top.value / grouped.values.sum().toFloat()).coerceAtMost(1f),
                count = top.value,
                relatedCategories = listOf(top.key),
                insight = "\"${top.key}\" is your most-observed category. You might deepen this into a focused project."
            )
        )

        // Least observed (if there's a significant gap)
        if (bottom.value <= 2 && bottom.value < top.value / 3f) {
            results.add(
                DetectedPattern(
                    type = "category_trend",
                    label = "Underexplored: ${bottom.key}",
                    description = "Only ${bottom.value} observation${if (bottom.value == 1) "" else "s"} in \"${bottom.key}\"",
                    confidence = 0.5f,
                    count = bottom.value,
                    relatedCategories = listOf(bottom.key),
                    insight = "\"${bottom.key}\" has only ${bottom.value} observation${if (bottom.value == 1) "" else "s"}. Consider exploring this category more."
                )
            )
        }

        return results
    }

    // ── 5. Observation gap detection ──

    private fun detectObservationGaps(observations: List<ObservationEntity>): List<DetectedPattern> {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val byCategory = observations
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }

        return byCategory.entries
            .mapNotNull { (cat, obsList) ->
                val latest = obsList.maxOfOrNull { it.timestamp } ?: return@mapNotNull null
                val daysSince = (now - latest) / dayMs
                if (daysSince < GAP_DAYS_THRESHOLD || obsList.size < 3) return@mapNotNull null
                DetectedPattern(
                    type = "observation_gap",
                    label = "Gap in \"$cat\"",
                    description = "No \"$cat\" observation in $daysSince days (${obsList.size} total)",
                    confidence = (daysSince / 60f).coerceAtMost(1f),
                    count = obsList.size,
                    relatedCategories = listOf(cat),
                    insight = "It's been $daysSince days since your last \"$cat\" observation. You previously logged ${obsList.size} observations in this category."
                )
            }
            .sortedByDescending { it.confidence }
            .take(3)
    }

    // ── 6. Weather correlation detection ──

    private fun detectWeatherCorrelations(observations: List<ObservationEntity>): List<DetectedPattern> {
        val withWeather = observations.filter { it.weatherCondition.isNotBlank() }
        if (withWeather.size < 5) return emptyList()

        val conditions = withWeather
            .map { normalizeWeatherCondition(it.weatherCondition) }
            .groupingBy { it }
            .eachCount()

        val dominant = conditions.maxByOrNull { it.value } ?: return emptyList()
        val total = withWeather.size
        val ratio = dominant.value / total.toFloat()

        if (dominant.value < 3 || ratio < 0.3f) return emptyList()

        return listOf(
            DetectedPattern(
                type = "weather_correlation",
                label = "Mostly ${dominant.key} weather",
                description = "${dominant.value} of $total observations (${(ratio * 100).roundToInt()}%) were in ${dominant.key} conditions",
                confidence = ratio.coerceAtMost(1f),
                count = dominant.value,
                insight = "You observe most in ${dominant.key} weather. This could affect what species or phenomena you see. Try observing in different weather to compare."
            )
        )
    }

    // ── Utility helpers ──

    private fun extractTimeOfDay(time: String): String? {
        if (time.isBlank()) return null
        val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: return null
        return when (hour) {
            in 5..8 -> "Dawn / Early morning"
            in 9..11 -> "Late morning"
            in 12..14 -> "Midday"
            in 15..17 -> "Afternoon"
            in 18..20 -> "Dusk / Evening"
            in 21..23 -> "Night"
            in 0..4 -> "Late night"
            else -> null
        }
    }

    private fun normalizeWeatherCondition(condition: String): String {
        val c = condition.lowercase()
        return when {
            "clear" in c || "sunny" in c -> "Clear / Sunny"
            "cloud" in c || "overcast" in c -> "Cloudy"
            "rain" in c || "drizzle" in c || "shower" in c -> "Rain"
            "snow" in c || "sleet" in c || "blizzard" in c -> "Snow"
            "fog" in c || "mist" in c || "haze" in c -> "Fog / Mist"
            "thunder" in c || "storm" in c -> "Thunderstorm"
            "wind" in c -> "Windy"
            else -> "Other"
        }
    }

    /**
     * Haversine distance in meters between two lat/lng points.
     */
    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2).let { it * it }
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}
