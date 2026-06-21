package fieldmind.research.app.features.field.data.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Central WorkManager wiring for FieldMind background features exposed in Settings. */
object FieldMindBackgroundScheduler {
    private const val AUTO_BACKUP_WORK = "fieldmind_auto_backup"
    private const val DAILY_REMINDER_WORK = "fieldmind_daily_reminder"

    fun syncAll(context: Context, autoBackupEnabled: Boolean, autoBackupInterval: String, remindersEnabled: Boolean) {
        scheduleAutoBackup(context, autoBackupEnabled, autoBackupInterval)
        scheduleDailyReminder(context, remindersEnabled)
    }

    fun scheduleAutoBackup(context: Context, enabled: Boolean, intervalLabel: String) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK)
            return
        }
        val (duration, unit) = when (intervalLabel) {
            "Every 6 hours" -> 6L to TimeUnit.HOURS
            "Every 12 hours" -> 12L to TimeUnit.HOURS
            "Daily" -> 1L to TimeUnit.DAYS
            "Monthly" -> 30L to TimeUnit.DAYS
            else -> 7L to TimeUnit.DAYS // Weekly fallback
        }
        val request = PeriodicWorkRequestBuilder<FieldMindAutoBackupWorker>(duration, unit)
            .addTag(AUTO_BACKUP_WORK)
            .build()
        workManager.enqueueUniquePeriodicWork(AUTO_BACKUP_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleDailyReminder(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(DAILY_REMINDER_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<FieldMindReminderWorker>(1, TimeUnit.DAYS)
            .addTag(DAILY_REMINDER_WORK)
            .build()
        workManager.enqueueUniquePeriodicWork(DAILY_REMINDER_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /**
     * Parse an interval label into milliseconds for countdown display.
     */
    fun intervalToMillis(intervalLabel: String): Long = when (intervalLabel) {
        "Every 6 hours" -> 6L * 60 * 60 * 1000
        "Every 12 hours" -> 12L * 60 * 60 * 1000
        "Daily" -> 24L * 60 * 60 * 1000
        "Weekly" -> 7L * 24 * 60 * 60 * 1000
        "Monthly" -> 30L * 24 * 60 * 60 * 1000
        else -> 7L * 24 * 60 * 60 * 1000
    }
}
