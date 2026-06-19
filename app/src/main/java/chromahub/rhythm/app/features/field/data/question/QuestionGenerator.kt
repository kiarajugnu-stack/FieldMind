package fieldmind.research.app.features.field.data.question

import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.data.database.entity.QuestionEntity
import fieldmind.research.app.features.field.data.database.entity.SourceEntity

/**
 * A suggested question ready to be persisted.
 */
data class GeneratedQuestion(
    val questionText: String,
    val category: String,
    val sourceType: String,
    val priority: String,
    val context: String,
    val observationId: Long? = null,
    val relatedCategories: List<String> = emptyList()
)

/**
 * Offline question generator that creates structured, contextual questions
 * from observations and data gaps.
 *
 * Question types:
 * - **Observation** — direct questions from what was observed
 * - **Comparison** — compare across categories, locations, or times
 * - **Pattern** — questions about detected patterns or changes
 * - **Cause** — questions about why something was observed
 * - **Gap** — questions about categories/locations not yet explored
 * - **Prediction** — questions predicting future observations
 * - **Method** — questions about how to observe more effectively
 */
object QuestionGenerator {

    private const val MAX_QUESTIONS = 12

    /**
     * Generate questions from all observation data.
     */
    fun generateAll(
        observations: List<ObservationEntity>,
        sources: List<SourceEntity>,
        existing: List<QuestionEntity>
    ): List<GeneratedQuestion> {
        if (observations.isEmpty()) return emptyList()

        val existingTexts = existing.map { it.questionText.lowercase().trim() }.toSet()
        val results = mutableListOf<GeneratedQuestion>()

        results.addAll(generateObservationQuestions(observations, existingTexts))
        results.addAll(generateComparisonQuestions(observations, existingTexts))
        results.addAll(generateCauseQuestions(observations, existingTexts))
        results.addAll(generatePatternQuestions(observations, existingTexts))
        results.addAll(generateGapQuestions(observations, existingTexts))
        results.addAll(generatePredictionQuestions(observations, existingTexts))
        results.addAll(generateMethodQuestions(observations, existingTexts))

        return results.distinctBy { it.questionText.lowercase().trim() }.take(MAX_QUESTIONS)
    }

    // ── 1. Observation questions — direct from what was seen ──

    private fun generateObservationQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        return observations
            .filter { it.subject.isNotBlank() && it.factsOnlyNotes.isNotBlank() }
            .sortedByDescending { it.timestamp }
            .take(5)
            .mapNotNull { obs ->
                val q = "What explains the behavior or appearance of \"${obs.subject}\" observed on ${obs.date}?"
                if (q.lowercase() in existing) return@mapNotNull null
                GeneratedQuestion(
                    questionText = q,
                    category = obs.category.ifBlank { "Other" },
                    sourceType = "Observation",
                    priority = "Medium",
                    context = "Based on observation: ${obs.factsOnlyNotes.take(150)}",
                    observationId = obs.id,
                    relatedCategories = listOf(obs.category)
                )
            }
    }

    // ── 2. Comparison questions — compare across categories, locations, or times ──

    private fun generateComparisonQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        // Compare two most-observed categories
        val categories = observations
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(2)

        if (categories.size == 2) {
            val (c1, c2) = categories[0].key to categories[1].key
            val q = "How do observations in \"$c1\" compare with those in \"$c2\"?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "Comparison",
                    sourceType = "Observation",
                    priority = "Medium",
                    context = "You have ${categories[0].value} observations in \"$c1\" and ${categories[1].value} in \"$c2\".",
                    relatedCategories = listOf(c1, c2)
                ))
            }
        }

        // Compare two locations if available
        val locations = observations
            .filter { it.manualLocation.isNotBlank() }
            .groupBy { it.manualLocation }
            .mapValues { it.value.size }
            .entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(2)

        if (locations.size == 2) {
            val (l1, l2) = locations[0].key to locations[1].key
            val q = "How does \"$l1\" differ from \"$l2\" in terms of species or observations?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "Comparison",
                    sourceType = "Observation",
                    priority = "Low",
                    context = "You have ${locations[0].value} observations at \"$l1\" and ${locations[1].value} at \"$l2\".",
                    relatedCategories = emptyList()
                ))
            }
        }

        return results
    }

    // ── 3. Cause questions — why was something observed ──

    private fun generateCauseQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        // Look for weather-attached observations to ask about causality
        val withWeather = observations.filter { it.weatherCondition.isNotBlank() && it.subject.isNotBlank() }
        if (withWeather.isNotEmpty()) {
            val obs = withWeather.first()
            val q = "Does the weather condition \"${obs.weatherCondition}\" influence the presence or behavior of \"${obs.subject}\"?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = obs.category.ifBlank { "Other" },
                    sourceType = "Observation",
                    priority = "High",
                    context = "${obs.subject} was observed during ${obs.weatherCondition}. Weather data was automatically attached.",
                    observationId = obs.id,
                    relatedCategories = listOf(obs.category)
                ))
            }
        }

        // Time-based cause
        val timeObservations = observations.filter { it.time.isNotBlank() && it.subject.isNotBlank() }
        if (timeObservations.isNotEmpty()) {
            val obs = timeObservations.first()
            val hour = obs.time.split(":").firstOrNull()?.toIntOrNull()
            if (hour != null) {
                val timeDesc = when (hour) {
                    in 5..8 -> "early morning"
                    in 9..11 -> "late morning"
                    in 12..14 -> "midday"
                    in 15..17 -> "afternoon"
                    in 18..20 -> "evening"
                    else -> "night"
                }
                val q = "Why is \"${obs.subject}\" most commonly observed during $timeDesc?"
                if (q.lowercase() !in existing) {
                    results.add(GeneratedQuestion(
                        questionText = q,
                        category = obs.category.ifBlank { "Other" },
                        sourceType = "Observation",
                        priority = "Medium",
                        context = "Based on timing of observations for \"${obs.subject}\".",
                        observationId = obs.id,
                        relatedCategories = listOf(obs.category)
                    ))
                }
            }
        }

        return results
    }

    // ── 4. Pattern questions — ask about detected patterns ──

    private fun generatePatternQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        // Repeated subjects
        val repeatedSubjects = observations
            .filter { it.subject.isNotBlank() }
            .groupBy { it.subject.trim().lowercase() }
            .filter { it.value.size >= 3 }
            .entries
            .sortedByDescending { it.value.size }
            .take(2)

        for ((subject, obsList) in repeatedSubjects) {
            val displayName = obsList.first().subject
            val q = "What recurring patterns emerge from the repeated observations of \"$displayName\" over time?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = obsList.first().category.ifBlank { "Other" },
                    sourceType = "Observation",
                    priority = "High",
                    context = "\"$displayName\" has been observed ${obsList.size} times. Each observation captures different conditions and details.",
                    observationId = obsList.first().id,
                    relatedCategories = obsList.map { it.category }.distinct()
                ))
            }
        }

        // Site revisit patterns
        if (observations.count { it.latitude != null && it.longitude != null } >= 4) {
            val q = "Are there consistent patterns in the observations at frequently revisited sites?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "General",
                    sourceType = "Observation",
                    priority = "Medium",
                    context = "Multiple observations were made at the same locations, suggesting site fidelity or recurring phenomena."
                ))
            }
        }

        return results
    }

    // ── 5. Gap questions — what haven't we explored ──

    private fun generateGapQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        val categories = observations
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }

        // Find underexplored categories
        val underexplored = categories.filter { it.value.size == 1 }
        for ((cat, obsList) in underexplored.take(2)) {
            val q = "What more can we learn about \"$cat\" — currently only ${obsList.size} observation?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = cat,
                    sourceType = "Data gap",
                    priority = "Low",
                    context = "Only ${obsList.size} observation in \"$cat\". Exploring more could reveal new insights.",
                    observationId = obsList.first().id,
                    relatedCategories = listOf(cat)
                ))
            }
        }

        // Investigate if a location was visited only once
        val singleVisitLocations = observations
            .filter { it.manualLocation.isNotBlank() }
            .groupBy { it.manualLocation }
            .filter { it.value.size == 1 }
            .take(1)

        for ((loc, _) in singleVisitLocations) {
            val q = "Should \"$loc\" be revisited for follow-up observations?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "General",
                    sourceType = "Data gap",
                    priority = "Low",
                    context = "\"$loc\" was visited only once. Revisiting could reveal seasonal or temporal differences."
                ))
            }
        }

        return results
    }

    // ── 6. Prediction questions — what might happen next ──

    private fun generatePredictionQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        // Ask about a highly-observed subject
        val topSubject = observations
            .filter { it.subject.isNotBlank() }
            .groupBy { it.subject.trim().lowercase() }
            .maxByOrNull { it.value.size }

        if (topSubject != null && topSubject.value.size >= 2) {
            val displayName = topSubject.value.first().subject
            val q = "Will \"$displayName\" continue to be observed at the same frequency in the coming weeks?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "Prediction",
                    sourceType = "Observation",
                    priority = "Medium",
                    context = "Based on ${topSubject.value.size} past observations of \"$displayName\".",
                    observationId = topSubject.value.first().id,
                    relatedCategories = topSubject.value.map { it.category }.distinct()
                ))
            }
        }

        // Weather-based prediction (if weather was attached)
        val withWeather = observations.filter { it.weatherCondition.isNotBlank() }
        if (withWeather.size >= 3) {
            val q = "How will changing weather conditions affect future observations?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "Prediction",
                    sourceType = "Observation",
                    priority = "Low",
                    context = "Weather data has been logged for ${withWeather.size} observations."
                ))
            }
        }

        return results
    }

    // ── 7. Method questions — how to improve observations ──

    private fun generateMethodQuestions(
        observations: List<ObservationEntity>,
        existing: Set<String>
    ): List<GeneratedQuestion> {
        val results = mutableListOf<GeneratedQuestion>()

        // If attachments are mentioned, ask about documentation methods
        val withEvidence = observations.filter { it.evidenceSummary.isNotBlank() }
        if (withEvidence.isNotEmpty()) {
            val obs = withEvidence.first()
            val q = "What is the most effective way to document evidence for \"${obs.subject}\" observations?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = obs.category.ifBlank { "Other" },
                    sourceType = "Method",
                    priority = "Low",
                    context = "Evidence was noted for \"${obs.subject}\" but documentation methods could be improved.",
                    observationId = obs.id,
                    relatedCategories = listOf(obs.category)
                ))
            }
        }

        // If observations have confidence levels, ask about consistency
        val confidenceLevels = observations.map { it.confidenceLevel }.distinct()
        if (confidenceLevels.size > 1) {
            val q = "How can observation confidence levels be made more consistent across entries?"
            if (q.lowercase() !in existing) {
                results.add(GeneratedQuestion(
                    questionText = q,
                    category = "General",
                    sourceType = "Method",
                    priority = "Low",
                    context = "Observations use various confidence levels: ${confidenceLevels.joinToString(", ")}."
                ))
            }
        }

        return results
    }
}
