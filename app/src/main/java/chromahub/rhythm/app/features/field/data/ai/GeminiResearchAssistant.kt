package chromahub.rhythm.app.features.field.data.ai

/**
 * Optional Gemini research assistant contract for FieldMind.
 *
 * Persistent instruction: help the user think clearly; do not replace the user's thinking;
 * do not invent observations, citations, evidence, locations, or conclusions; if evidence is
 * missing, say what evidence is needed. Implementations must never auto-save AI output and must
 * require explicit user confirmation before saving any suggestion.
 */
class GeminiResearchAssistant(
    private val enabled: Boolean,
    private val apiKeyProvider: () -> String?
) {
    fun isAvailable(): Boolean = enabled && !apiKeyProvider().isNullOrBlank()

    fun observationFactualityReview(notes: String): AssistantSuggestion = localOnlySuggestion(
        "Factuality review",
        "Check whether each sentence describes directly observed facts. Mark interpretations as questions or hypotheses. Notes reviewed locally until the user chooses to send them."
    )

    fun questionTestabilityCheck(question: String): AssistantSuggestion = localOnlySuggestion(
        "Question testability",
        "A testable question should name what could be observed, measured, compared, or researched without assuming the answer."
    )

    fun hypothesisSuggestions(question: String): AssistantSuggestion = localOnlySuggestion(
        "Hypothesis suggestions",
        "Draft multiple possible predictions and list the evidence needed to support or weaken each one."
    )

    fun paperSummaryPrompt(): AssistantSuggestion = localOnlySuggestion(
        "Paper/source summary",
        "Summarize only the supplied source text. If a citation or result is missing, say it is missing."
    )

    fun keywordExtractionPrompt(): AssistantSuggestion = localOnlySuggestion(
        "Keyword extraction",
        "Extract tags from user-provided text only; never infer locations, species, or evidence not present."
    )

    fun flashcardGenerationPrompt(): AssistantSuggestion = localOnlySuggestion(
        "Flashcard generation",
        "Generate draft cards from confirmed source notes and ask for user confirmation before saving."
    )

    fun researchMentorSuggestion(): AssistantSuggestion = localOnlySuggestion(
        "Research mentor",
        "Suggest next observations, controls, sources, or data tools based on existing user records only."
    )

    fun writingImprovementPrompt(): AssistantSuggestion = localOnlySuggestion(
        "Writing improvement",
        "Improve clarity while preserving uncertainty and separating observations, analysis, and conclusions."
    )

    private fun localOnlySuggestion(title: String, body: String) = AssistantSuggestion(
        title = title,
        body = body,
        requiresConfirmation = true,
        maySendDataOnlyAfterUserAction = true
    )
}

data class AssistantSuggestion(
    val title: String,
    val body: String,
    val requiresConfirmation: Boolean,
    val maySendDataOnlyAfterUserAction: Boolean
)
