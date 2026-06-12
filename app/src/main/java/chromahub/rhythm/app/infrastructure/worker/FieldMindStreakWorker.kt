package chromahub.rhythm.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import chromahub.rhythm.app.R
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.features.field.data.database.FieldMindDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Daily worker that checks FieldMind observation streaks and sends reminder notifications.
 * Uses SharedPreferences to persist streak state across app restarts.
 */
class FieldMindStreakWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "FieldMindStreakWorker"
        const val WORK_NAME = "fieldmind_streak_work"
        private const val CHANNEL_ID = "fieldmind_streak"
        private const val NOTIFICATION_ID = 7701
        private const val PREF_NAME = "fieldmind_streak"
        private const val KEY_LAST_STREAK_DATE = "last_streak_date"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""

            // Check if user made observations today
            val dao = FieldMindDatabase.getInstance(applicationContext).fieldMindDao()
            val observations = dao.observeObservations().let { flow ->
                var result = emptyList<chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity>()
                flow.collect { result = it }; result
            }
            val todayCount = observations.count { it.date == today }

            if (todayCount > 0) {
                // User has observations today — update streak
                val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
                val newStreak = if (lastDate == today) currentStreak else currentStreak + 1
                val bestStreak = maxOf(newStreak, prefs.getInt(KEY_BEST_STREAK, 0))
                prefs.edit()
                    .putString(KEY_LAST_STREAK_DATE, today)
                    .putInt(KEY_CURRENT_STREAK, newStreak)
                    .putInt(KEY_BEST_STREAK, bestStreak)
                    .apply()
                Log.d(TAG, "Streak updated: $newStreak days (best: $bestStreak)")
            } else if (lastDate != today) {
                // No observations today and streak was active — send reminder
                val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
                if (currentStreak > 0) {
                    sendReminderNotification(currentStreak)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Streak check failed", e)
            Result.retry()
        }
    }

    private fun sendReminderNotification(streakDays: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FieldMind Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily research streak reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 6..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Quick reminder"
        }

        val message = "$greeting! You have a $streakDays-day research streak going. Capture one observation today to keep it alive."

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 303, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FieldMind research streak")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Streak reminder sent ($streakDays days)")
    }
}
