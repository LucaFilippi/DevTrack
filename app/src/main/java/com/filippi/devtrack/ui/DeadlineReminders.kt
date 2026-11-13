package com.filippi.devtrack.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.filippi.devtrack.MainActivity
import com.filippi.devtrack.R
import com.filippi.devtrack.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal object DeadlineReminders {
    const val ACTION_CHECK = "com.filippi.devtrack.CHECK_DEADLINES"
    private const val CHANNEL_ID = "deadline_reminders"
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { refresh(app) } }
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        alarm(app).cancel(pendingIntent(app))
        app.getSystemService(NotificationManager::class.java).cancelAll()
    }

    suspend fun refresh(context: Context) = lock.withLock {
        val app = context.applicationContext
        if (!AppPreferences(app).notificationsEnabled) {
            alarm(app).cancel(pendingIntent(app))
            return@withLock
        }
        scheduleNext(app)
        if (!canNotify(app)) return@withLock

        val database = Room.databaseBuilder(app, AppDatabase::class.java, "devtrack.db")
            .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3)
            .build()
        try {
            val today = LocalDate.now()
            val projects = database.projectDao().getAll()
            val activeProjects = projects.filter { it.status != "completed" && it.status != "archived" }
            val activeIds = activeProjects.mapTo(hashSetOf()) { it.id }
            val goals = database.goalDao().getAll()
            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, app.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT
            ))
            val sent = app.getSharedPreferences("deadline_reminders", Context.MODE_PRIVATE)
            val contentIntent = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            fun notify(kind: String, id: String, name: String, deadlineAt: Long) {
                val days = reminderOffset(deadlineAt, today) ?: return
                val message = when (days) {
                    7 -> R.string.reminder_week
                    5 -> R.string.reminder_five_days
                    1 -> R.string.reminder_tomorrow
                    else -> return
                }
                val key = "$kind:$id:$deadlineAt:$days"
                if (sent.getBoolean(key, false)) return
                val title = if (kind == "project") R.string.reminder_project_title else R.string.reminder_goal_title
                val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                    .setSmallIcon(R.drawable.launcher_clipboard)
                    .setContentTitle(app.getString(title))
                    .setContentText(app.getString(message, name))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(app.getString(message, name)))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build()
                manager.notify(key.hashCode(), notification)
                sent.edit().putBoolean(key, true).apply()
            }

            activeProjects.forEach { project ->
                project.deadlineAt?.let { notify("project", project.id, project.name, it) }
            }
            goals.filter { !it.completed && it.projectId in activeIds }.forEach { goal ->
                notify("goal", goal.id, goal.name, goal.deadlineAt)
            }
        } finally {
            database.close()
        }
    }

    private fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun scheduleNext(context: Context) {
        val now = java.time.ZonedDateTime.now(ZoneId.systemDefault())
        var next = now.toLocalDate().atTime(LocalTime.of(9, 0)).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarm(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(), pendingIntent(context))
    }

    private fun alarm(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, DeadlineReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class DeadlineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(DeadlineReminders.ACTION_CHECK, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { runCatching { DeadlineReminders.refresh(context) } } finally { pending.finish() }
        }
    }
}
