package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.receiver.ReminderNotificationReceiver
import com.example.ui.AlarmRingingActivity

object ReminderScheduler {

    const val CHANNEL_ID = "recordatorio_alerts_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                setSound(alarmSound, audioAttributes)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val now = System.currentTimeMillis()
        val oneYearAhead = now + (365L * 24 * 60 * 60 * 1000)
        if (reminder.triggerTimeMillis > now && reminder.triggerTimeMillis <= oneYearAhead && !reminder.isCompleted) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return

                val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
                    action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
                    putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderNotificationReceiver.EXTRA_TITLE, reminder.title)
                    putExtra(
                        ReminderNotificationReceiver.EXTRA_MESSAGE,
                        if (reminder.description.isNotBlank()) reminder.description else "Es hora de tu recordatorio."
                    )
                    putExtra(ReminderNotificationReceiver.EXTRA_CATEGORY, reminder.category)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.triggerTimeMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            reminder.triggerTimeMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerTimeMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.w("ReminderScheduler", "Could not schedule exact alarm: ${e.message}")
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
                action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.w("ReminderScheduler", "Failed to cancel alarm: ${e.message}")
        }
    }

    fun triggerTestAlarm(context: Context) {
        val testTitle = "¡Alarma: Hora de comer o tomar remedio!"
        val testMessage = "Esta alarma sonará continuamente hasta que toques el botón APAGAR ALARMA."

        // 1. Play continuous sound & vibration
        AlarmPlayer.startAlarm(
            context,
            99999L,
            testTitle,
            testMessage,
            ReminderCategory.MEDICATION.id
        )

        // 2. Launch full-screen AlarmRingingActivity
        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, 99999L)
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, testTitle)
            putExtra(ReminderNotificationReceiver.EXTRA_MESSAGE, testMessage)
            putExtra(ReminderNotificationReceiver.EXTRA_CATEGORY, ReminderCategory.MEDICATION.id)
        }

        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error launching test alarm activity", e)
        }
    }
}
