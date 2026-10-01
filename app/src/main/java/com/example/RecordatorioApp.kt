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
    val repository by lazy { ReminderRepository(database.reminderDao()) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)

        // Seed initial helpful organizer reminders (meals, meds, infinite capsule) if empty
        applicationScope.launch {
            val existing = repository.allReminders.firstOrNull()
            if (existing.isNullOrEmpty()) {
                repository.populateDefaultsIfEmpty()
            }
        }
    }
}
