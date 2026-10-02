package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Custom templates the user saves from the reminder dialog so they can
 * quickly create the same reminder again later (e.g. "Daily Minecraft
 * save", "Free Fire ranked reset", "Feed Adopt Me pets", etc.).
 *
 * A custom template stores every field needed to recreate a reminder,
 * but it is NOT itself a reminder: it does not trigger any alarm, and
 * selecting it inside the "Plantillas" tab builds a fresh reminder from
 * the saved values.
 *
 * The trigger time is recomputed relative to "now + intervalOffsetMinutes"
 * when the user picks the template, so a template saved as "in 30 minutes"
 * keeps meaning "30 minutes from now" every time the user applies it.
 */
@Entity(tableName = "custom_templates")
data class CustomTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "📌",
    val title: String,
    val description: String = "",
    val category: String = ReminderCategory.PERSONAL.id,
    val priority: String = ReminderPriority.NORMAL.id,
    /**
     * Relative offset in minutes from "now" when this template is applied.
     * 0 means "use the saved absolute year/month/day/hour/minute/second".
     * > 0 means "fire N minutes from now" (great for game events / AFK).
     */
    val intervalOffsetMinutes: Long = 0L,
    /**
     * When intervalOffsetMinutes == 0, these absolute fields are used to
     * build the reminder. When > 0, they are ignored and the time is
     * computed from now + offset.
     */
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val recurrenceType: String = RecurrenceType.ONCE.id,
    val recurrenceIntervalHours: Int = 8,
    val recurrenceIntervalMinutes: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)
