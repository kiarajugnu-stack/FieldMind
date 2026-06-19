package fieldmind.research.app.features.field.background

import android.content.Context
import android.content.Intent

/**
 * Manages the lifecycle of the [FieldMindTimerService] foreground timer.
 *
 * Provides helper functions for starting/stopping the service and
 * checking if timers are currently active. Timer state is persisted
 * via SharedPreferences in the service for crash/reboot recovery.
 */
object FieldMindTimerManager {

    /** Start a reading/focus timer. */
    fun startReadingTimer(context: Context) {
        val intent = Intent(context, FieldMindTimerService::class.java).apply {
            action = FieldMindTimerService.ACTION_START_READING
            putExtra(FieldMindTimerService.EXTRA_TITLE, "Reading")
            putExtra(FieldMindTimerService.EXTRA_TEXT, "Focus timer running")
        }
        context.startForegroundService(intent)
    }

    /** Stop the reading/focus timer. */
    fun stopReadingTimer(context: Context) {
        val intent = Intent(context, FieldMindTimerService::class.java).apply {
            action = FieldMindTimerService.ACTION_STOP_READING
        }
        context.startService(intent)
    }

    /** Start a research session timer. */
    fun startSessionTimer(context: Context, sessionName: String) {
        val intent = Intent(context, FieldMindTimerService::class.java).apply {
            action = FieldMindTimerService.ACTION_START_SESSION
            putExtra(FieldMindTimerService.EXTRA_SESSION_NAME, sessionName)
            putExtra(FieldMindTimerService.EXTRA_TEXT, "Research session running")
        }
        context.startForegroundService(intent)
    }

    /** Stop the research session timer. */
    fun stopSessionTimer(context: Context) {
        val intent = Intent(context, FieldMindTimerService::class.java).apply {
            action = FieldMindTimerService.ACTION_STOP_SESSION
        }
        context.startService(intent)
    }

    /** Update the notification with current timer info. */
    fun updateTimerNotification(
        context: Context,
        title: String,
        text: String,
        elapsedMs: Long,
        timerType: String
    ) {
        val intent = Intent(context, FieldMindTimerService::class.java).apply {
            action = FieldMindTimerService.ACTION_UPDATE_TIMER
            putExtra(FieldMindTimerService.EXTRA_TITLE, title)
            putExtra(FieldMindTimerService.EXTRA_TEXT, text)
            putExtra(FieldMindTimerService.EXTRA_TIMER_MS, elapsedMs)
            putExtra(FieldMindTimerService.EXTRA_TIMER_TYPE, timerType)
        }
        context.startService(intent)
    }

    /** Check if any timer is currently active (from persisted state). */
    fun isAnyTimerActive(context: Context): Boolean =
        FieldMindTimerService.isTimerActive(context)

    /** Get the saved timer state for recovery. */
    fun getSavedTimerState(context: Context): FieldMindTimerService.TimerState? =
        FieldMindTimerService.getSavedTimerState(context)

    /** Clear saved timer state (after recovery or discard). */
    fun clearSavedState(context: Context) {
        FieldMindTimerService.clearSavedTimerState(context)
    }

    /** Format milliseconds to HH:MM:SS or MM:SS. */
    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }
}
