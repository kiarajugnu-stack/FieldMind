package fieldmind.research.app.features.field.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fieldmind.research.app.activities.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that keeps timers running in the background.
 * Supports two timer modes:
 * - READING: Tracks reading/focus time on the LearnReaderScreen
 * - RESEARCH_SESSION: Tracks active research session time
 *
 * The service shows an ongoing notification that the user can tap
 * to return to the active screen. The timer state is persisted to
 * SharedPreferences so it can be recovered after process death.
 */
class FieldMindTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "fieldmind_timer_service"
        const val NOTIFICATION_ID = 4109

        const val ACTION_START_READING = "fieldmind.action.START_READING"
        const val ACTION_STOP_READING = "fieldmind.action.STOP_READING"
        const val ACTION_START_SESSION = "fieldmind.action.START_SESSION"
        const val ACTION_STOP_SESSION = "fieldmind.action.STOP_SESSION"
        const val ACTION_UPDATE_TIMER = "fieldmind.action.UPDATE_TIMER"

        const val EXTRA_TIMER_TYPE = "timer_type"
        const val EXTRA_TIMER_MS = "timer_ms"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_OBSERVATION_COUNT = "observation_count"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"

        const val TYPE_READING = "reading"
        const val TYPE_SESSION = "session"

        // Persistence keys
        private const val PREFS_NAME = "fieldmind_timer_prefs"
        private const val KEY_ACTIVE_TYPE = "active_timer_type"
        private const val KEY_ELAPSED_MS = "elapsed_ms"
        private const val KEY_SESSION_NAME = "session_name"

        fun isTimerActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_ACTIVE_TYPE, null) != null
        }

        fun getSavedTimerState(context: Context): TimerState? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val type = prefs.getString(KEY_ACTIVE_TYPE, null) ?: return null
            val elapsedMs = prefs.getLong(KEY_ELAPSED_MS, 0L)
            val name = prefs.getString(KEY_SESSION_NAME, "")
            return TimerState(type, elapsedMs, name ?: "")
        }

        fun clearSavedTimerState(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_TYPE)
                .remove(KEY_ELAPSED_MS)
                .remove(KEY_SESSION_NAME)
                .apply()
        }

        private fun saveTimerState(context: Context, state: TimerState) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TYPE, state.type)
                .putLong(KEY_ELAPSED_MS, state.elapsedMs)
                .putString(KEY_SESSION_NAME, state.name)
                .apply()
        }
    }

    data class TimerState(
        val type: String,
        val elapsedMs: Long = 0L,
        val name: String = ""
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_READING -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reading"
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Focus timer running"
                elapsedMs = 0L
                startForeground(NOTIFICATION_ID, buildNotification(title, text))
                saveTimerState(this, TimerState(TYPE_READING, 0L, title))
                startTimerTick()
            }
            ACTION_STOP_READING -> {
                stopTimerTick()
                clearSavedTimerState(this)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_SESSION -> {
                val name = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Research Session"
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Field session in progress"
                elapsedMs = 0L
                startForeground(NOTIFICATION_ID, buildNotification(name, text))
                saveTimerState(this, TimerState(TYPE_SESSION, 0L, name))
                startTimerTick()
            }
            ACTION_STOP_SESSION -> {
                stopTimerTick()
                clearSavedTimerState(this)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_TIMER -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Timer"
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Running"
                elapsedMs = intent.getLongExtra(EXTRA_TIMER_MS, 0L)
                val type = intent.getStringExtra(EXTRA_TIMER_TYPE) ?: TYPE_READING
                updateNotification(title, text)

                // Persist elapsed time for recovery
                val currentState = getSavedTimerState(this)
                if (currentState != null) {
                    saveTimerState(this, currentState.copy(elapsedMs = elapsedMs))
                } else {
                    saveTimerState(this, TimerState(type, elapsedMs, title))
                }
            }
        }
        return START_STICKY
    }

    private var elapsedMs: Long = 0L
    private var currentTitle: String = "Timer"
    private var currentText: String = "Running"
    private var timerTickJob: kotlinx.coroutines.Job? = null

    override fun onDestroy() {
        stopTimerTick()
        super.onDestroy()
    }

    private fun startTimerTick() {
        stopTimerTick()
        timerTickJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()).launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                elapsedMs += 1000
                updateNotification(currentTitle, currentText)
            }
        }
    }

    private fun stopTimerTick() {
        timerTickJob?.cancel()
        timerTickJob = null
    }

    private fun updateNotification(title: String, text: String) {
        currentTitle = title
        currentText = text
        val displayText = "${FieldMindTimerManager.formatTime(elapsedMs)} • $text"
        val notification = buildNotification(title, displayText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FieldMind Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent timer for research sessions and reading focus"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }
}
