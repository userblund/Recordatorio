package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RecordatorioApp
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Rebuilds AlarmManager entries after Android restarts, time changes,
 * timezone changes, or an app update.
 *
 * The database remains the source of truth; this receiver only restores
 * future alarms and never opens an Activity.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? RecordatorioApp

        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = app.repository.activeReminders.first()
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
