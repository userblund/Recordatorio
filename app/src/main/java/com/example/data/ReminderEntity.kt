package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = ReminderCategory.PERSONAL.id,
    val priority: String = ReminderPriority.NORMAL.id,
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..31
    val hour: Int,  // 0..23
    val minute: Int, // 0..59
    val second: Int = 0, // 0..59 — granular for game events / AFK timers
    val triggerTimeMillis: Long,
    val recurrenceType: String = RecurrenceType.ONCE.id,
    val recurrenceIntervalHours: Int = 8,
    val recurrenceIntervalMinutes: Int = 0, // used when recurrenceType == INTERVAL_MINUTES
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
