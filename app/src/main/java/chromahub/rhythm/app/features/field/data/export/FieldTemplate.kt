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

data class ReportTemplate(
    val id: String,
    val label: String,
    val helperPrompt: String,
    val sections: List<Pair<String, String>>,
    val presets: List<String> = listOf("School report", "Peer share", "Personal log", "Project final")
)

object FieldReportTemplates {
    val defaults = listOf(
        ReportTemplate("field_report", "Field Report", "Turn observations into a clear site-based field narrative.", listOf("Background" to "Why this site or subject matters.", "Question" to "The research question guiding the trip.", "Methods" to "Route, timing, tools, and sampling choices.", "Observations" to "Facts, measurements, photos, and attachments.", "Results" to "Patterns or counts found.", "Interpretation" to "What the results may mean.", "Limitations" to "Biases, missing data, weather, or access limits.", "Next steps" to "What to test or revisit.")),
        ReportTemplate("lab_experiment", "Lab/Experiment Report", "Structure variables, method, trials, and results.", listOf("Background" to "Concepts and prior evidence.", "Question" to "Testable question.", "Methods" to "Materials, variables, procedure, controls.", "Results" to "Tables, charts, trial outcomes.", "Conclusion" to "Answer supported by evidence.", "Limitations" to "Uncertainty and errors.", "Next steps" to "Improved trials.")),
        ReportTemplate("literature_review", "Literature Review", "Synthesize sources into themes and gaps.", listOf("Background" to "Topic and scope.", "Methods" to "Search terms and inclusion choices.", "Observations" to "Source summaries.", "Interpretation" to "Themes, agreements, and conflicts.", "Conclusion" to "Current best understanding.", "Next steps" to "Open questions.")),
        ReportTemplate("species_observation", "Species Observation Report", "Summarize repeated sightings and evidence.", listOf("Background" to "Species/context.", "Question" to "Behavior or distribution question.", "Methods" to "Observation schedule and locations.", "Observations" to "Sightings, photos, counts.", "Results" to "Patterns.", "Next steps" to "Follow-up checks.")),
        ReportTemplate("site_survey", "Site Survey", "Document a place with connected evidence.", listOf("Background" to "Site description.", "Methods" to "Survey route and tools.", "Observations" to "Habitats, species, geology, human factors.", "Results" to "Key findings.", "Limitations" to "Coverage constraints.", "Next steps" to "Monitoring plan.")),
        ReportTemplate("project_summary", "Project Summary", "Create a readable status page for a project.", listOf("Question" to "Main question.", "Methods" to "Work completed.", "Results" to "Evidence collected.", "Interpretation" to "Current meaning.", "Next steps" to "Open tasks.")),
        ReportTemplate("final_findings", "Final Findings", "Package final evidence, analysis, and recommendations.", listOf("Background" to "Why the project matters.", "Methods" to "How evidence was collected.", "Results" to "Findings.", "Interpretation" to "Evidence-backed explanation.", "Conclusion" to "Final answer.", "Limitations" to "What remains uncertain.", "Next steps" to "Recommendations."))
    )
}
