package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.RecordatorioApp
import com.example.ui.AlarmRingingActivity
import com.example.util.AlarmPlayer
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == ACTION_DISMISS_ALARM) {
            AlarmPlayer.stopAlarm(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1)
            notificationManager.cancel(notifId)

            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
            if (reminderId > 0) {
                val app = context.applicationContext as? RecordatorioApp
                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        val reminder = it.repository.getReminderById(reminderId)
                        if (reminder != null) {
                            it.repository.toggleCompleted(reminder)
                        }
                    }
                }
            }
            return
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "¡Alarma de Recordatorio!"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Es momento de atender tu recordatorio."
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "PERSONAL"

        // 1. Start continuous audio and vibration
        AlarmPlayer.startAlarm(context, reminderId, title, message, category)

        // 2. Intent to open full-screen AlarmRingingActivity
        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CATEGORY, category)
        }

        val pendingAlarmIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            this.action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_NOTIFICATION_ID, reminderId.toInt().coerceAtLeast(1))
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build notification with full-screen intent and action button
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingAlarmIntent)
            .setFullScreenIntent(pendingAlarmIntent, true)
            .addAction(
                android.R.drawable.ic_delete,
                "APAGAR ALARMA",
                dismissPendingIntent
            )
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt().coerceAtLeast(1), notification)

        // Also launch activity directly
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            // Background start restricted on some devices, fullScreenIntent handles it
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.ACTION_SHOW_REMINDER"
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
