package chromahub.rhythm.app.features.field.data.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.features.field.data.database.FieldMindDatabase
import chromahub.rhythm.app.features.field.data.repository.FieldMindRepository
import chromahub.rhythm.app.features.field.data.settings.FieldMindSettings
import chromahub.rhythm.app.features.field.data.stats.FieldMindStreaks
import chromahub.rhythm.app.R
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Daily research prompt. Skips notifications if the user already captured an observation today. */
class FieldMindReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = FieldMindSettings.getInstance(applicationContext)
        if (!settings.remindersEnabled.value) return@runCatching Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return@runCatching Result.success()
        }

        val observations = FieldMindRepository(FieldMindDatabase.getInstance(applicationContext).fieldMindDao()).observations.first()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (observations.any { it.date == today }) return@runCatching Result.success()

        showReminder(FieldMindStreaks.currentStreakDays(observations.map { it.date }))
        Result.success()
    }.getOrElse { Result.retry() }

    @SuppressLint("MissingPermission")
    private fun showReminder(streak: Int) {
        val channelId = "fieldmind_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, "FieldMind reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily prompts to capture a factual observation"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = if (streak > 0) "Keep your $streak-day research streak alive with one factual observation." else "Capture one factual observation to start today's field record."
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FieldMind observation reminder")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(4102, notification)
    }
}
