package fieldmind.research.app.features.field.data.flashcard

import fieldmind.research.app.features.field.data.database.entity.FlashcardEntity
import fieldmind.research.app.features.field.data.database.entity.NoteEntity
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.data.database.entity.SourceEntity

/**
 * A suggested flashcard ready to be persisted.
 */
data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val type: String,
    val sourceId: Long? = null,
    val projectId: Long? = null,
    val dedupKey: String
)

/**
 * Generates flashcards from all user data without requiring any network calls.
 *
 * - From **observations**: subject → front, factsKey → back, uses category as tag
 * - From **notes**: title → front, body summary → back
 * - From **sources**: title → front, key findings → back
 * - Deduplicates against existing flashcards by normalised front+back hash
 * - Limits generation to prevent flooding
 */
object SmartFlashcardGenerator {

    private const val MAX_PER_SOURCE_TYPE = 8
    private const val MIN_OBSERVATION_LENGTH = 10
    private const val MIN_NOTE_LENGTH = 15
    private const val MIN_SOURCE_LENGTH = 10

    /**
     * Generate flashcards from observation data.
     */
    fun generateFromObservations(
        observations: List<ObservationEntity>,
        existing: List<FlashcardEntity>
    ): List<GeneratedFlashcard> {
        val existingKeys = existing.map { flashcardKey(it.front, it.back) }.toSet()
        val results = mutableListOf<GeneratedFlashcard>()

        for (obs in observations) {
            if (results.size >= MAX_PER_SOURCE_TYPE) break
            val subject = obs.subject.trim()
            val facts = obs.factsOnlyNotes.trim()

            // Skip empty or very short observations
            if (subject.length < 2) continue

            // Card 1: Subject → Facts (What did you observe?)
            val label1 = "Observed: $subject"
            val answer1 = facts.take(300).ifBlank { obs.evidenceSummary.take(200).ifBlank { obs.moodOrContext.take(200) } }
            if (answer1.length >= MIN_OBSERVATION_LENGTH) {
                val key = dedupKey("obs:what", label1, answer1)
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "What did you observe about $subject?",
                        back = answer1,
                        type = "observation",
                        sourceId = obs.id,
                        projectId = obs.projectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }

            // Card 2: Category → Subject (What category does this observation belong to?)
            if (obs.category.isNotBlank() && subject.length >= 3) {
                val label2 = "Category: $subject"
                val answer2 = obs.category
                val key = dedupKey("obs:cat", label2, answer2)
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "What category is \"$subject\" under?",
                        back = "$subject → ${obs.category}",
                        type = "observation",
                        sourceId = obs.id,
                        projectId = obs.projectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }

            // Card 3: Location → Subject (Where was this observed?)
            if (obs.manualLocation.isNotBlank() && subject.length >= 3) {
                val label3 = "Location: $subject"
                val answer3 = obs.manualLocation
                val key = dedupKey("obs:loc", label3, answer3)
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "Where did you observe \"$subject\"?",
                        back = "$subject was observed at ${obs.manualLocation}",
                        type = "observation",
                        sourceId = obs.id,
                        projectId = obs.projectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }
        }

        return results
    }

    /**
     * Generate flashcards from notes.
     */
    fun generateFromNotes(
        notes: List<NoteEntity>,
        existing: List<FlashcardEntity>
    ): List<GeneratedFlashcard> {
        val existingKeys = existing.map { flashcardKey(it.front, it.back) }.toSet()
        val results = mutableListOf<GeneratedFlashcard>()

        for (note in notes) {
            if (results.size >= MAX_PER_SOURCE_TYPE) break
            val title = note.title.trim()
            val body = note.body.trim()

            if (title.length < 3) continue

            // Card: Title → Body summary
            val bodySummary = body.take(300).ifBlank { note.tags.ifBlank { note.category } }
            if (bodySummary.length >= MIN_NOTE_LENGTH || body.isBlank()) {
                val key = dedupKey("note:body", title, bodySummary.take(100))
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "Notes: $title",
                        back = if (body.isNotBlank()) bodySummary else "See full note for \"$title\"",
                        type = "note",
                        sourceId = note.sourceId,
                        projectId = note.projectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }

            // Card: Category → Note title
            if (note.category.isNotBlank() && !note.category.equals("Other", ignoreCase = true)) {
                val key = dedupKey("note:cat", title, note.category)
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "Which note relates to \"${note.category}\"?",
                        back = "\"$title\" (${note.category})",
                        type = "note",
                        sourceId = note.sourceId,
                        projectId = note.projectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }
        }

        return results
    }

    /**
     * Generate flashcards from sources (reading material).
     */
    fun generateFromSources(
        sources: List<SourceEntity>,
        existing: List<FlashcardEntity>
    ): List<GeneratedFlashcard> {
        val existingKeys = existing.map { flashcardKey(it.front, it.back) }.toSet()
        val results = mutableListOf<GeneratedFlashcard>()

        for (source in sources) {
            if (results.size >= MAX_PER_SOURCE_TYPE) break
            val title = source.title.trim()

            if (title.length < 2) continue

            // Card 1: Title → Personal summary
            val summary = source.personalSummary.take(300)
                .ifBlank { source.keyFindings.take(300) }
                .ifBlank { source.whatThisSourceTaughtMe.take(300) }
            if (summary.length >= MIN_SOURCE_LENGTH) {
                val key = dedupKey("src:summary", title, summary.take(100))
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "Key takeaway from \"$title\"",
                        back = summary,
                        type = "source",
                        sourceId = source.id,
                        projectId = source.relatedProjectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }

            // Card 2: Author → Title
            if (source.author.isNotBlank()) {
                val key = dedupKey("src:author", title, source.author)
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "Who wrote \"$title\"?",
                        back = source.author,
                        type = "source",
                        sourceId = source.id,
                        projectId = source.relatedProjectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }

            // Card 3: Key findings → Title
            val findings = source.keyFindings.take(300)
            if (findings.length >= MIN_SOURCE_LENGTH && findings != summary) {
                val key = dedupKey("src:findings", title, findings.take(100))
                if (key !in existingKeys) {
                    results += GeneratedFlashcard(
                        front = "What findings did \"$title\" present?",
                        back = findings,
                        type = "source",
                        sourceId = source.id,
                        projectId = source.relatedProjectId,
                        dedupKey = key
                    ))
                    existingKeys += key
                }
            }
        }

        return results
    }

    /**
     * Generate flashcards from all data sources in one call.
     */
    fun generateAll(
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        sources: List<SourceEntity>,
        existing: List<FlashcardEntity>
    ): List<GeneratedFlashcard> {
        val results = mutableListOf<GeneratedFlashcard>()
        results.addAll(generateFromObservations(observations, existing))
        results.addAll(generateFromNotes(notes, existing + results.map { it.toFlashcardEntity() }))
        results.addAll(generateFromSources(sources, existing + results.map { it.toFlashcardEntity() }))
        return results.take(20)
    }

    /**
     * Create a deterministic deduplication key so we never create the same card twice.
     */
    private fun dedupKey(prefix: String, front: String, back: String): String {
        val normalized = "$prefix:${front.lowercase().trim()}:${back.lowercase().trim()}"
        return normalized.hashCode().toLong().let {
            if (it == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(it)
        }.toString(36)
    }

    private fun flashcardKey(front: String, back: String): String {
        val normalized = "${front.lowercase().trim()}:${back.lowercase().trim()}"
        return normalized.hashCode().toLong().let {
            if (it == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(it)
        }.toString(36)
    }

    private fun GeneratedFlashcard.toFlashcardEntity() = FlashcardEntity(
        front = front,
        back = back,
        type = type,
        sourceId = sourceId,
        projectId = projectId,
        deckMode = "sm2",
        easeFactor = 2.5,
        intervalDays = 0,
        repetitionCount = 0
    )
}
