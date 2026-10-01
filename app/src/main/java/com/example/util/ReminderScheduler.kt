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
import com.example.R
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.receiver.ReminderNotificationReceiver

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
        // AlarmManager accepts RTC timestamps well beyond one year. Do not
        // artificially discard long-future reminders; the database remains
        // the source of truth for them.
        if (reminder.triggerTimeMillis > now && !reminder.isCompleted) {
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
                    alarmRequestCode(reminder.id),
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
        val testTitle = "Prueba de recordatorio"
        val testMessage = "Esta prueba usa la misma notificación que un recordatorio real y no abre una pantalla encima de otras apps."

        // Test the production-safe path: broadcast -> notification receiver.
        // This deliberately never launches an Activity, so testing cannot
        // accidentally minimize or background the game the user is playing.
        val testIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, 99999L)
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, testTitle)
            putExtra(ReminderNotificationReceiver.EXTRA_MESSAGE, testMessage)
            putExtra(
                ReminderNotificationReceiver.EXTRA_CATEGORY,
                ReminderCategory.PERSONAL.id
            )
        }

        try {
            context.sendBroadcast(testIntent)
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error sending test reminder", e)
        }
    }

    private fun alarmRequestCode(reminderId: Long): Int =
        (reminderId xor (reminderId ushr 32)).toInt() and 0x7fffffff
}
