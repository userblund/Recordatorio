package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.R
import com.example.RecordatorioApp
import com.example.data.RecurrenceType
import com.example.util.AlarmPlayer
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_STOP_TEST_ALARM -> {
                stopAlarmForReminder(context, reminderId)
                manager.cancel(notificationId(reminderId))
                return
            }

            ACTION_ACKNOWLEDGE -> {
                stopAlarmForReminder(context, reminderId)
                manager.cancel(notificationId(reminderId))
                showConfirmation(context, reminderId, intent)
                return
            }

            ACTION_COMPLETED_YES -> {
                stopAlarmForReminder(context, reminderId)
                manager.cancel(notificationId(reminderId))
                finishReminder(context, reminderId)
                return
            }

            ACTION_COMPLETED_NO -> {
                stopAlarmForReminder(context, reminderId)
                manager.cancel(notificationId(reminderId))
                showDelayOptions(context, reminderId, intent)
                return
            }

            ACTION_REPLY -> {
                stopAlarmForReminder(context, reminderId)
                val results = RemoteInput.getResultsFromIntent(intent)
                val answer = results?.getCharSequence(REPLY_KEY)?.toString()?.trim()?.lowercase() ?: ""
                manager.cancel(notificationId(reminderId))
                val normalized = answer
                    .replace("á", "a")
                    .replace("é", "e")
                    .replace("í", "i")
                    .replace("ó", "o")
                    .replace("ú", "u")
                    .trim()

                when {
                    normalized == "si" ||
                            normalized == "s" ||
                            normalized == "yes" ||
                            normalized == "y" ||
                            normalized == "hecho" ||
                            normalized == "listo" ||
                            normalized == "completado" ||
                            normalized == "ya" -> {
                        finishReminder(context, reminderId)
                    }
                    normalized == "no" ||
                            normalized == "n" ||
                            normalized == "todavia no" ||
                            normalized == "aun no" -> {
                        showDelayOptions(context, reminderId, intent)
                    }
                    else -> {
                        showConfirmation(context, reminderId, intent, "Responde sí o no.")
                    }
                }
                return
            }

            ACTION_DELAY -> {
                stopAlarmForReminder(context, reminderId)
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).coerceIn(1, 10080)
                manager.cancel(notificationId(reminderId))
                if (minutes > 0) {
                    delayReminder(context, reminderId, minutes)
                }
                return
            }

            ACTION_DELAY_CUSTOM -> {
                stopAlarmForReminder(context, reminderId)
                val results: Bundle? = RemoteInput.getResultsFromIntent(intent)
                val raw = results?.getCharSequence(REMOTE_INPUT_KEY)?.toString()?.trim()
                val minutes = raw?.toIntOrNull()?.coerceIn(1, 10080)
                manager.cancel(notificationId(reminderId))
                if (minutes != null) {
                    delayReminder(context, reminderId, minutes)
                } else {
                    showDelayOptions(context, reminderId, intent, "Escribe un número de minutos válido.")
                }
                return
            }

            ACTION_DISMISS_ALARM -> {
                stopAlarmForReminder(context, reminderId)
                manager.cancel(notificationId(reminderId))
                finishReminder(context, reminderId)
                return
            }
        }

        // Normal reminder delivery: notification only. We deliberately do NOT
        // launch AlarmRingingActivity, so a game such as Minecraft stays in
        // the foreground while the alert appears as a heads-up notification.
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Recordatorio"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Es momento de atender tu recordatorio."
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "PERSONAL"
        val isTestAlarm = intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false)

        try {
            AlarmSoundService.start(context, reminderId, title, message, category)
        } catch (_: Exception) {
            // The notification still provides the alert if the foreground
            // service cannot be started on this device.
        }
        showInitialNotification(context, reminderId, title, message, category)
    }

    private fun stopAlarmForReminder(context: Context, reminderId: Long) {
        AlarmPlayer.stopAlarm(context, reminderId)
        if (!AlarmPlayer.isAlarmPlaying.value) {
            context.stopService(Intent(context, AlarmSoundService::class.java))
        }
    }

    private fun showInitialNotification(
        context: Context,
        reminderId: Long,
        title: String,
        message: String,
        category: String
    ) {
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                if (isTestAlarm) {
                    action(context, reminderId, ACTION_STOP_TEST_ALARM, "DETENER", title, message, category)
                } else {
                    action(context, reminderId, ACTION_ACKNOWLEDGE, "OK", title, message, category, 10)
                }
            )
            .addAction(action(context, reminderId, ACTION_DELAY, "5 min", title, message, category, 5))
            .addAction(action(context, reminderId, ACTION_DELAY, "30 min", title, message, category, 30))
            .build()

        notify(context, reminderId, notification)
    }

    private fun showConfirmation(
        context: Context,
        reminderId: Long,
        original: Intent,
        error: String? = null
    ) {
        val title = original.getStringExtra(EXTRA_TITLE) ?: "Recordatorio"
        val category = original.getStringExtra(EXTRA_CATEGORY) ?: "PERSONAL"

        val yes = action(context, reminderId, ACTION_COMPLETED_YES, "SÍ", title, "", category)
        val no = action(context, reminderId, ACTION_COMPLETED_NO, "NO", title, "", category)
        val later = action(context, reminderId, ACTION_DELAY, "AHORA NO", title, "", category, 10)

        val replyInput = RemoteInput.Builder(REPLY_KEY)
            .setLabel("Escribe o dicta: sí / no")
            .build()
        val replyIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CATEGORY, category)
        }
        // IMPORTANT (Android 12+ / API 31+): PendingIntents that carry a
        // RemoteInput action MUST be FLAG_MUTABLE so the system can attach
        // the user's reply text into the Intent's extras before dispatching.
        // Using FLAG_IMMUTABLE here throws:
        //   IllegalArgumentException: PendingIntents attached to actions with
        //   remote inputs must be mutable
        // and crashes ReminderNotificationReceiver.showConfirmation.
        val replyPending = PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, 9000),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val question = if (error != null) "$error ¿Completaste «$title»?" else "¿Completaste «$title»?"
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Ya lo hiciste?")
            .setContentText(question)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(yes)
            .addAction(no)
            .addAction(later)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Responder",
                    replyPending
                ).addRemoteInput(replyInput).build()
            )
            .build()

        notify(context, reminderId, notification)
    }

    private fun showDelayOptions(
        context: Context,
        reminderId: Long,
        original: Intent,
        error: String? = null
    ) {
        val title = original.getStringExtra(EXTRA_TITLE) ?: "Recordatorio"
        val category = original.getStringExtra(EXTRA_CATEGORY) ?: "PERSONAL"

        val customInput = RemoteInput.Builder(REMOTE_INPUT_KEY)
            .setLabel("Minutos hasta el próximo recordatorio")
            .build()

        val customIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_DELAY_CUSTOM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CATEGORY, category)
        }
        // IMPORTANT (Android 12+ / API 31+): This PendingIntent carries a
        // RemoteInput ("Minutos hasta el próximo recordatorio"), so it MUST
        // be FLAG_MUTABLE or the system will reject the notification with:
        //   IllegalArgumentException: PendingIntents attached to actions with
        //   remote inputs must be mutable
        val customPending = PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, 7000),
            customIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Cuándo vuelvo a recordártelo?")
            .setContentText(error ?: "Elige un intervalo o escribe tus propios minutos.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(action(context, reminderId, ACTION_DELAY, "5 min", title, "", category, 5))
            .addAction(action(context, reminderId, ACTION_DELAY, "15 min", title, "", category, 15))
            .addAction(action(context, reminderId, ACTION_DELAY, "30 min", title, "", category, 30))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Minutos",
                    customPending
                ).addRemoteInput(customInput).build()
            )

        notify(context, reminderId, notificationBuilder.build())
    }

    private fun finishReminder(context: Context, reminderId: Long) {
        if (reminderId <= 0L) return
        val app = context.applicationContext as? RecordatorioApp ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = app.repository.getReminderById(reminderId) ?: return@launch
            app.repository.toggleCompleted(reminder)

            val updated = app.repository.getReminderById(reminderId)
            if (updated != null && !updated.isCompleted &&
                RecurrenceType.fromId(updated.recurrenceType).isInfinite
            ) {
                ReminderScheduler.scheduleReminder(context, updated)
            } else {
                ReminderScheduler.cancelReminder(context, reminderId)
            }
        }
    }

    private fun delayReminder(context: Context, reminderId: Long, minutes: Int) {
        if (reminderId <= 0L) return
        val app = context.applicationContext as? RecordatorioApp ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = app.repository.getReminderById(reminderId) ?: return@launch
            app.repository.snooze(reminder, minutes)
            val updated = app.repository.getReminderById(reminderId)
            if (updated != null) {
                ReminderScheduler.scheduleReminder(context, updated)
            }
        }
    }

    private fun action(
        context: Context,
        reminderId: Long,
        action: String,
        label: String,
        title: String,
        message: String,
        category: String,
        minutes: Int? = null
    ): NotificationCompat.Action {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CATEGORY, category)
            if (minutes != null) putExtra(EXTRA_MINUTES, minutes)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, action.hashCode() + (minutes ?: 0)),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            label,
            pending
        ).build()
    }

    private fun notify(context: Context, reminderId: Long, notification: android.app.Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId(reminderId), notification)
    }

    private fun notificationId(reminderId: Long): Int =
        (reminderId xor (reminderId ushr 32)).toInt() and 0x7fffffff

    private fun requestCode(reminderId: Long, salt: Int): Int =
        ((reminderId xor (reminderId ushr 32)).toInt() * 31 + salt) and 0x7fffffff

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.ACTION_SHOW_REMINDER"
        const val ACTION_STOP_TEST_ALARM = "com.example.ACTION_STOP_TEST_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"
        const val ACTION_ACKNOWLEDGE = "com.example.ACTION_ACKNOWLEDGE"
        const val ACTION_COMPLETED_YES = "com.example.ACTION_COMPLETED_YES"
        const val ACTION_COMPLETED_NO = "com.example.ACTION_COMPLETED_NO"
        const val ACTION_REPLY = "com.example.ACTION_REPLY"
        const val ACTION_DELAY = "com.example.ACTION_DELAY"
        const val ACTION_DELAY_CUSTOM = "com.example.ACTION_DELAY_CUSTOM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_MINUTES = "extra_minutes"
        const val REMOTE_INPUT_KEY = "recordatorio_minutes_input"
        const val REPLY_KEY = "recordatorio_reply"
    }
}
