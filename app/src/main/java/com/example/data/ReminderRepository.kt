package com.example.data

import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReminderRepository(private val reminderDao: ReminderDao) {

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()
    val completedReminders: Flow<List<ReminderEntity>> = reminderDao.getCompletedReminders()

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun insert(reminder: ReminderEntity): Long = reminderDao.insert(reminder)

    suspend fun update(reminder: ReminderEntity) = reminderDao.update(reminder)

    suspend fun delete(reminder: ReminderEntity) = reminderDao.delete(reminder)

    suspend fun deleteById(id: Long) = reminderDao.deleteById(id)

    suspend fun toggleCompleted(reminder: ReminderEntity) {
        val now = System.currentTimeMillis()
        val recurrence = RecurrenceType.fromId(reminder.recurrenceType)

        if (!reminder.isCompleted) {
            // Marking as completed
            if (recurrence.isInfinite) {
                // For infinite / recurring reminders: advance to next occurrence and keep active!
                var next = DateUtils.computeNextOccurrence(
                    currentYear = reminder.year,
                    currentMonth = reminder.month,
                    currentDay = reminder.day,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    recurrenceType = recurrence,
                    intervalHours = reminder.recurrenceIntervalHours
                )

                // If the phone was off, the app was stopped, or the user
                // responds long after the scheduled occurrence, skip missed
                // recurring occurrences instead of immediately firing a chain
                // of overdue alarms. The next stored occurrence must be in the
                // future relative to the moment the user completed this one.
                var guard = 0
                while (next.triggerMillis <= now && guard < 100_000) {
                    next = DateUtils.computeNextOccurrence(
                        currentYear = next.year,
                        currentMonth = next.month,
                        currentDay = next.day,
                        hour = next.hour,
                        minute = next.minute,
                        recurrenceType = recurrence,
                        intervalHours = reminder.recurrenceIntervalHours
                    )
                    guard++
                }

                val updated = reminder.copy(
                    year = next.year,
                    month = next.month,
                    day = next.day,
                    hour = next.hour,
                    minute = next.minute,
                    triggerTimeMillis = next.triggerMillis,
                    isCompleted = false,
                    completedAtMillis = now
                )
                reminderDao.update(updated)
            } else {
                reminderDao.setCompletedStatus(reminder.id, completed = true, completedAt = now)
            }
        } else {
            // Reactivate
            reminderDao.setCompletedStatus(reminder.id, completed = false, completedAt = null)
        }
    }

    suspend fun snooze(reminder: ReminderEntity, additionalMinutes: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, additionalMinutes)
        val newMillis = cal.timeInMillis

        // Snooze changes only this occurrence. Keep the reminder's
        // canonical calendar slot untouched so recurring reminders return
        // to their normal schedule after the snoozed occurrence.
        val updated = reminder.copy(
            triggerTimeMillis = newMillis,
            isCompleted = false
        )
        reminderDao.update(updated)
    }

    suspend fun populateDefaultsIfEmpty() {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curDay = cal.get(Calendar.DAY_OF_MONTH)

        // Starter reminders demonstrating daily meals, meds, and the infinite far-future feature requested by user
        val defaultItems = listOf(
            ReminderEntity(
                title = "Tomar remedio / Medicamento 💊",
                description = "Tomar con medio vaso de agua después del desayuno.",
                category = ReminderCategory.MEDICATION.id,
                priority = ReminderPriority.HIGH.id,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 9,
                minute = 0,
                triggerTimeMillis = DateUtils.computeTriggerMillis(curYear, curMonth, curDay, 9, 0),
                recurrenceType = RecurrenceType.DAILY.id,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            ReminderEntity(
                title = "Almuerzo saludable 🥗",
                description = "Hora de comer: plato balanceado y pausa de trabajo.",
                category = ReminderCategory.MEAL.id,
                priority = ReminderPriority.NORMAL.id,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 13,
                minute = 30,
                triggerTimeMillis = DateUtils.computeTriggerMillis(curYear, curMonth, curDay, 13, 30),
                recurrenceType = RecurrenceType.DAILY.id,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            ReminderEntity(
                title = "Remedio nocturno / Suplemento 💊",
                description = "1 comprimido antes de dormir.",
                category = ReminderCategory.MEDICATION.id,
                priority = ReminderPriority.HIGH.id,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 22,
                minute = 0,
                triggerTimeMillis = DateUtils.computeTriggerMillis(curYear, curMonth, curDay, 22, 0),
                recurrenceType = RecurrenceType.DAILY.id,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            ReminderEntity(
                title = "Cápsula del Tiempo Año 2500 🚀",
                description = "Recordatorio guardado para el futuro distante: ¡El compromiso infinito sigue vigente!",
                category = ReminderCategory.FAR_FUTURE.id,
                priority = ReminderPriority.MEDIUM.id,
                year = 2500,
                month = 1,
                day = 1,
                hour = 12,
                minute = 0,
                triggerTimeMillis = DateUtils.computeTriggerMillis(2500, 1, 1, 12, 0),
                recurrenceType = RecurrenceType.ONCE.id,
                soundEnabled = true,
                vibrationEnabled = true
            )
        )

        for (item in defaultItems) {
            reminderDao.insert(item)
        }
    }
}
