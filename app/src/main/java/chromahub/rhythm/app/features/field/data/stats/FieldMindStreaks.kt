package chromahub.rhythm.app.features.field.data.stats

import java.time.LocalDate
import java.time.ZoneId

/** Small, deterministic streak helpers shared by dashboard, insights, and reminders. */
object FieldMindStreaks {
    fun currentStreakDays(dateStrings: Iterable<String>, today: LocalDate = LocalDate.now()): Int {
        val dates = dateStrings.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        if (dates.isEmpty()) return 0

        var cursor = today
        if (cursor !in dates) cursor = cursor.minusDays(1)

        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun currentStreakDays(timestamps: Iterable<Long>, zoneId: ZoneId = ZoneId.systemDefault()): Int = currentStreakDays(
        timestamps.map { millis -> java.time.Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toString() }
    )
}
