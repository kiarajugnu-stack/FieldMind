package chromahub.rhythm.app.features.field.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Supported remote AI providers for FieldMind's optional research assistant. */
enum class AiProvider(val label: String) { GEMINI("Gemini"), OPENAI("OpenAI") }

/**
 * Optional research assistant for FieldMind.
 *
 * Persistent instruction: help the user think clearly; do not replace the user's thinking;
 * do not invent observations, citations, evidence, locations, or conclusions; if evidence is
 * missing, say what evidence is needed. Output is a draft preview until the user explicitly saves it.
 */
class GeminiResearchAssistant(
    private val enabled: Boolean,
    private val provider: AiProvider = AiProvider.GEMINI,
    private val apiKeyProvider: () -> String?,
    private val modelProvider: () -> String = { "gemini-1.5-flash" }
) {
    fun isAvailable(): Boolean = enabled && !apiKeyProvider().isNullOrBlank()
    fun providerLabel(): String = provider.label

    suspend fun generateContent(task: AssistantTask, userText: String): AssistantSuggestion = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext disabledSuggestion(task)
        when (provider) {
            AiProvider.GEMINI -> generateGemini(task, userText)
            AiProvider.OPENAI -> generateOpenAi(task, userText)
        }
    }

    private fun generateGemini(task: AssistantTask, userText: String): AssistantSuggestion {
        val apiKey = apiKeyProvider().orEmpty()
        val model = modelProvider().ifBlank { "gemini-1.5-flash" }
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val prompt = buildPrompt(task, userText)
        val request = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.3).put("topP", 0.8))
        val body = postJson(endpoint, request) { connection ->
            connection.setRequestProperty("Content-Type", "application/json")
        }
        if (body.first !in 200..299) return AssistantSuggestion(task.title, "Gemini request failed (${body.first}). ${body.second}", true, true)
        val text = JSONObject(body.second)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
        return AssistantSuggestion(task.title, text.ifBlank { "Gemini returned an empty draft." }, true, true)
    }

    private fun generateOpenAi(task: AssistantTask, userText: String): AssistantSuggestion {
        val apiKey = apiKeyProvider().orEmpty()
        val model = modelProvider().ifBlank { "gpt-4.1-mini" }
        val endpoint = URL("https://api.openai.com/v1/responses")
        val request = JSONObject()
            .put("model", model)
            .put("temperature", 0.3)
            .put("input", buildPrompt(task, userText))
        val body = postJson(endpoint, request) { connection ->
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        if (body.first !in 200..299) return AssistantSuggestion(task.title, "OpenAI request failed (${body.first}). ${body.second}", true, true)
        val json = JSONObject(body.second)
        val outputText = json.optString("output_text").ifBlank {
            json.optJSONArray("output")
                ?.optJSONObject(0)
                ?.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
        }
        return AssistantSuggestion(task.title, outputText.ifBlank { "OpenAI returned an empty draft." }, true, true)
    }

    private fun postJson(endpoint: URL, request: JSONObject, configure: (HttpURLConnection) -> Unit): Pair<Int, String> {
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        configure(connection)
        connection.doOutput = true
        connection.outputStream.use { it.write(request.toString().toByteArray()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return code to body
    }

    fun observationFactualityReview(notes: String): AssistantSuggestion = promptPreview(AssistantTask.FACTUALITY, notes)
    fun questionTestabilityCheck(question: String): AssistantSuggestion = promptPreview(AssistantTask.TESTABILITY, question)
    fun hypothesisSuggestions(question: String): AssistantSuggestion = promptPreview(AssistantTask.HYPOTHESIS, question)
    fun paperSummaryPrompt(): AssistantSuggestion = promptPreview(AssistantTask.SOURCE_SUMMARY, "Paste source text before sending.")
    fun keywordExtractionPrompt(): AssistantSuggestion = promptPreview(AssistantTask.KEYWORDS, "")
    fun flashcardGenerationPrompt(): AssistantSuggestion = promptPreview(AssistantTask.FLASHCARDS, "")
    fun researchMentorSuggestion(): AssistantSuggestion = promptPreview(AssistantTask.NEXT_STEPS, "")
    fun writingImprovementPrompt(): AssistantSuggestion = promptPreview(AssistantTask.WRITING, "")

    private fun promptPreview(task: AssistantTask, userText: String) = AssistantSuggestion(
        title = task.title,
        body = "Ready to ask ${provider.label}: ${task.instructions}\n\nUser text stays local until you press an explicit AI action.${if (userText.isBlank()) "" else "\nPreview input: ${userText.take(240)}"}",
        requiresConfirmation = true,
        maySendDataOnlyAfterUserAction = true
    )

    private fun disabledSuggestion(task: AssistantTask) = AssistantSuggestion(
        title = task.title,
        body = "${provider.label} is disabled or missing an API key. Enable it in FieldMind Settings before sending research text.",
        requiresConfirmation = true,
        maySendDataOnlyAfterUserAction = true
    )

    private fun buildPrompt(task: AssistantTask, userText: String): String = """
        You are FieldMind's research assistant. Assist thinking; do not replace it.
        Never invent observations, evidence, citations, locations, measurements, species names, or conclusions.
        If information is missing, say exactly what evidence is needed.
        Preserve uncertainty and separate facts from interpretation.
        Prefer primary, free-access, verifiable sources such as Wikipedia overview pages, government science agencies, universities, OpenStax, NCBI Bookshelf, USGS, NOAA, NASA, EPA, Cornell Lab, Crossref/OpenAlex metadata, or open-access papers.

        Task: ${task.instructions}

        User-provided material:
        $userText
    """.trimIndent()
}

enum class AssistantTask(val title: String, val instructions: String) {
    FACTUALITY("Factuality review", "Mark directly observed facts, interpretations, missing evidence, and safer rewrite suggestions."),
    TESTABILITY("Question testability", "Check whether the question can be observed, measured, compared, or researched without assuming the answer."),
    HYPOTHESIS("Hypothesis suggestions", "Suggest several testable predictions with support evidence, weakening evidence, and simple test methods."),
    SOURCE_SUMMARY("Paper/source summary", "Summarize only supplied source text: topic, problem, method, result, unclear points, and new questions."),
    KEYWORDS("Keyword extraction", "Extract tags only from supplied text; never infer locations, species, or evidence not present."),
    FLASHCARDS("Flashcard drafts", "Draft term, concept, Q/A, and mistake cards from confirmed source notes only."),
    NEXT_STEPS("Research mentor", "Suggest next observations, controls, sources, or data tools based on existing user-provided records only."),
    WRITING("Writing improvement", "Improve clarity while preserving uncertainty and separating observations, analysis, and conclusions."),
    PAPER_BOOK_SUGGESTIONS(
        "Papers & books",
        "Suggest 4-6 real, free-access or easy-to-verify primary resources relevant to the topic. Prefer Wikipedia overview pages, government/university pages, OpenStax/NCBI/USGS/NOAA/NASA/EPA/Cornell resources, open metadata, and open-access papers. Never fabricate titles, authors, DOIs, or links. If unsure, suggest a search query instead."
    )
}

data class AssistantSuggestion(
    val title: String,
    val body: String,
    val requiresConfirmation: Boolean,
    val maySendDataOnlyAfterUserAction: Boolean
)
