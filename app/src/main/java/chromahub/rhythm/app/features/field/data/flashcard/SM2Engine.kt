package fieldmind.research.app.features.field.data.flashcard

/**
 * SM-2 Spaced Repetition Algorithm
 *
 * Based on the SuperMemo SM-2 algorithm by P.A. Wozniak.
 * Each card has: easeFactor (≥1.3), interval (days), repetition count, nextReviewAt timestamp.
 *
 * Rating: 0=Again, 1=Hard, 2=Good, 3=Easy
 */
object SM2Engine {

    data class SM2Result(
        val easeFactor: Double,
        val intervalDays: Int,
        val repetitionCount: Int,
        val nextReviewAt: Long,
        val lastReviewedAt: Long
    )

    /**
     * Calculate the next review schedule based on SM-2 algorithm.
     *
     * @param rating 0=Again, 1=Hard, 2=Good, 3=Easy
     * @param currentEaseFactor Current ease factor (default 2.5)
     * @param currentInterval Current interval in days (0 for new cards)
     * @param currentRepetition Current repetition count (0 for new cards)
     * @return SM2Result with updated scheduling
     */
    fun calculate(
        rating: Int,
        currentEaseFactor: Double = 2.5,
        currentInterval: Int = 0,
        currentRepetition: Int = 0
    ): SM2Result {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000

        // Map our 0-3 rating to SM-2's 0-5 scale (0,1,2,3 map to 0,2,3,4,5)
        val sm2Quality = when (rating) {
            0 -> 0    // Again → complete blackout
            1 -> 2    // Hard → incorrect, remembered after seeing answer
            2 -> 3    // Good → correct with hesitation
            3 -> 5    // Easy → perfect response
            else -> 3
        }

        var newEaseFactor = currentEaseFactor
        var newInterval = currentInterval
        var newRepetition = currentRepetition

        if (sm2Quality < 3) {
            // Failed: reset to beginning
            newRepetition = 0
            newInterval = when (rating) {
                0 -> 1   // Again: review in 1 minute (but we use 1 day minimum for daily scheduling)
                else -> 1
            }
        } else {
            // Passed: advance
            newRepetition = currentRepetition + 1
            newInterval = when (newRepetition) {
                1 -> 1
                2 -> 3
                else -> (currentInterval * currentEaseFactor).toInt().coerceAtLeast(1)
            }

            // Apply bonus for Easy rating
            if (rating == 3) {
                newInterval = (newInterval * 1.3).toInt().coerceAtLeast(newInterval + 1)
            }
        }

        // Update ease factor: EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        newEaseFactor = currentEaseFactor + (0.1 - (5 - sm2Quality) * (0.08 + (5 - sm2Quality) * 0.02))
        newEaseFactor = newEaseFactor.coerceAtLeast(1.3)

        val nextReviewAt = now + newInterval * dayMs

        return SM2Result(
            easeFactor = newEaseFactor,
            intervalDays = newInterval,
            repetitionCount = newRepetition,
            nextReviewAt = nextReviewAt,
            lastReviewedAt = now
        )
    }

    /**
     * Get a human-readable label for the next review interval.
     */
    fun nextReviewLabel(intervalDays: Int): String = when {
        intervalDays == 0 -> "Now"
        intervalDays == 1 -> "Tomorrow"
        intervalDays < 7 -> "In $intervalDays days"
        intervalDays < 30 -> "In ${intervalDays / 7} week${if (intervalDays / 7 > 1) "s" else ""}"
        intervalDays < 365 -> "In ${intervalDays / 30} month${if (intervalDays / 30 > 1) "s" else ""}"
        else -> "In ${intervalDays / 365} year${if (intervalDays / 365 > 1) "s" else ""}"
    }

    /**
     * Get a quality label for the ease factor.
     */
    fun easeLabel(easeFactor: Double): String = when {
        easeFactor < 1.5 -> "Difficult"
        easeFactor < 2.0 -> "Moderate"
        easeFactor < 2.5 -> "Normal"
        easeFactor < 3.0 -> "Easy"
        else -> "Very easy"
    }

    /**
     * Check if a card is due for review.
     */
    fun isDue(nextReviewAt: Long?): Boolean {
        if (nextReviewAt == null) return true // New card, always due
        return System.currentTimeMillis() >= nextReviewAt
    }

    /**
     * Get cards sorted by priority for review.
     * Overdue cards first, then new cards, then cards due later.
     */
    fun reviewPriority(nextReviewAt: Long?, easeFactor: Double): Long {
        if (nextReviewAt == null) return 0L // New cards first
        val overdue = System.currentTimeMillis() - nextReviewAt
        return if (overdue > 0) -overdue else nextReviewAt
    }
}
