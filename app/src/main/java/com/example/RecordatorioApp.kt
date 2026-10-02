package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RecordatorioApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            customTemplateDao = database.customTemplateDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)

        // Seed the starter reminders only once. An empty database after that
        // is a valid user state (the user may have deleted every reminder).
        // The flag is stored separately from Room so closing/reopening the app
        // never recreates reminders the user intentionally deleted.
        applicationScope.launch {
            val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            if (preferences.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) {
                return@launch
            }

            val existing = repository.allReminders.firstOrNull()
            if (existing.isNullOrEmpty()) {
                repository.populateDefaultsIfEmpty()
            }

            // Mark initialization complete whether defaults were inserted or
            // the database already contained user data.
            preferences.edit()
                .putBoolean(KEY_DEFAULTS_INITIALIZED, true)
                .apply()

            // Rebuild AlarmManager entries whenever the application process starts.
            // This is important on devices that reclaim the app process in the
            // background: Room remains the source of truth and the alarms are
            // restored without requiring the user to open a screen manually.
            repository.activeReminders.firstOrNull()?.forEach { reminder ->
                ReminderScheduler.scheduleReminder(this@RecordatorioApp, reminder)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "recordatorio_app_state"
        private const val KEY_DEFAULTS_INITIALIZED = "default_reminders_initialized"
    }
}
