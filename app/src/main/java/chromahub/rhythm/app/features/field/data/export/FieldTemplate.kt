package fieldmind.research.app.features.field.data.export

import fieldmind.research.app.features.field.data.database.entity.*

/**
 * Observation template for quick pre-filled observations.
 * Templates are stored in-memory and can be saved/loaded from Settings.
 */
data class ObservationTemplate(
    val name: String,
    val description: String = "",
    val category: String = "Bird",
    val defaultConfidence: String = "Sure",
    val defaultSubject: String = "",
    val defaultFactsPrompt: String = "",
    val tags: String = "",
    val evidenceSummary: String = "",
    val context: String = ""
) {
    /** Pre-fill an observation from this template. */
    fun applyTo(subject: String = defaultSubject): String = buildString {
        if (defaultFactsPrompt.isNotBlank()) {
            appendLine("Prompt: $defaultFactsPrompt")
        }
        if (tags.isNotBlank()) appendLine("Tags: $tags")
        if (context.isNotBlank()) appendLine("Context: $context")
    }
}

/** A collection of built-in templates. */
object FieldTemplates {
    val defaults: List<ObservationTemplate> = listOf(
        ObservationTemplate(
            name = "Quick bird sighting",
            description = "Log a bird species with location and behavior",
            category = "Bird",
            defaultFactsPrompt = "Species, plumage, behavior, call, direction of flight",
            tags = "bird, sighting",
            context = "Clear daylight"
        ),
        ObservationTemplate(
            name = "Plant observation",
            description = "Record a plant species with habitat details",
            category = "Plant",
            defaultFactsPrompt = "Species, height, flower/fruit, leaf shape, habitat",
            tags = "plant, flora",
            context = "Growing season"
        ),
        ObservationTemplate(
            name = "Weather note",
            description = "Log current weather conditions",
            category = "Weather",
            defaultFactsPrompt = "Temperature, cloud cover, wind, precipitation, visibility",
            tags = "weather, conditions",
            context = ""
        ),
        ObservationTemplate(
            name = "Insect observation",
            description = "Record an insect sighting with behavior",
            category = "Insect",
            defaultFactsPrompt = "Species, size, behavior, host plant, location",
            tags = "insect, invertebrate",
            context = ""
        ),
        ObservationTemplate(
            name = "Reading insight",
            description = "Log a key insight from reading",
            category = "Reading Insight",
            defaultFactsPrompt = "Source, key claim, evidence, page reference",
            tags = "reading, insight",
            context = ""
        )
    )
}
