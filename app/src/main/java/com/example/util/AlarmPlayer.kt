package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlarmData(
    val reminderId: Long,
    val title: String,
    val message: String,
    val category: String,
    val startedAt: Long = System.currentTimeMillis()
)

object AlarmPlayer {

    private const val TAG = "AlarmPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val _isAlarmPlaying = MutableStateFlow(false)
    val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()

    private val _currentAlarm = MutableStateFlow<ActiveAlarmData?>(null)
    val currentAlarm: StateFlow<ActiveAlarmData?> = _currentAlarm.asStateFlow()

    @Synchronized
    fun startAlarm(
        context: Context,
        reminderId: Long,
        title: String,
        message: String,
        category: String
    ) {
        stopAlarm(context) // Ensure any previous alarm is stopped

        _currentAlarm.value = ActiveAlarmData(
            reminderId = reminderId,
            title = title,
            message = message,
            category = category
        )
        _isAlarmPlaying.value = true

        // 1. Play continuous alarm sound
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context.applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media player for alarm", e)
        }

        // 2. Start repeating vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibrator for alarm", e)
        }
    }

    @Synchronized
    fun stopAlarm(context: Context) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping mediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibrator", e)
        } finally {
            vibrator = null
        }

        _isAlarmPlaying.value = false
        _currentAlarm.value = null
    }
}
