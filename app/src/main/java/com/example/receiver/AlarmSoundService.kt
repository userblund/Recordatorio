package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.example.R
import com.example.util.AlarmPlayer
import com.example.util.ReminderScheduler

/**
 * Keeps an active alarm sound/vibration alive after the AlarmManager
 * BroadcastReceiver has returned. It never starts an Activity, so the
 * current game/app remains in the foreground.
 */
class AlarmSoundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, 0L) ?: 0L
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Recordatorio"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Es hora de tu recordatorio."
        val category = intent?.getStringExtra(EXTRA_CATEGORY) ?: "PERSONAL"

        if (reminderId <= 0L) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        createServiceChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarma activa")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                ServiceCompat.startForeground(
                    this,
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    0
                )
            }
        } catch (_: Exception) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        AlarmPlayer.startAlarm(this, reminderId, title, message, category)
        return START_STICKY
    }

    override fun onDestroy() {
        AlarmPlayer.stopAlarm(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Servicio de alarma",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Mantiene activa la alarma mientras espera una respuesta."
                        setSound(null, null)
                        enableVibration(false)
                    }
                )
            }
        }
    }

    companion object {
        const val ACTION_STOP_ONE = "com.example.ACTION_STOP_ONE_ALARM"
        const val EXTRA_REMINDER_ID = "extra_alarm_service_reminder_id"
        const val EXTRA_TITLE = "extra_alarm_service_title"
        const val EXTRA_MESSAGE = "extra_alarm_service_message"
        const val EXTRA_CATEGORY = "extra_alarm_service_category"

        const val CHANNEL_ID = "recordatorio_alarm_service_channel"
        const val SERVICE_NOTIFICATION_ID = 0x524543

        fun start(context: android.content.Context, reminderId: Long, title: String, message: String, category: String) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_CATEGORY, category)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
