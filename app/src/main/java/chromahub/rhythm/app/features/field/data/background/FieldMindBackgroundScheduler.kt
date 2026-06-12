package chromahub.rhythm.app.features.field.data.background

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
        val repeatDays = when (intervalLabel) {
            "Daily" -> 1L
            "Monthly" -> 30L
            else -> 7L
        }
        val request = PeriodicWorkRequestBuilder<FieldMindAutoBackupWorker>(repeatDays, TimeUnit.DAYS)
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
}
