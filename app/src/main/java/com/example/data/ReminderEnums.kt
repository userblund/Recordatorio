package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.FutureColor
import com.example.ui.theme.GeneralColor
import com.example.ui.theme.HabitColor
import com.example.ui.theme.MealColor
import com.example.ui.theme.MedColor
import com.example.ui.theme.WorkColor

enum class ReminderCategory(
    val id: String,
    val displayName: String,
    val iconName: String,
    val icon: ImageVector,
    val color: Color
) {
    MEDICATION("MEDICATION", "Medicamento", "💊", Icons.Default.Medication, MedColor),
    MEAL("MEAL", "Comida", "🍽️", Icons.Default.Fastfood, MealColor),
    HABIT("HABIT", "Hábito", "✨", Icons.Default.FitnessCenter, HabitColor),
    WORK("WORK", "Trabajo", "💼", Icons.Default.Work, WorkColor),
    FAR_FUTURE("FAR_FUTURE", "Infinito / Futuro", "🚀", Icons.Default.RocketLaunch, FutureColor),
    PERSONAL("PERSONAL", "Personal", "📝", Icons.Default.Notifications, GeneralColor);

    companion object {
        fun fromId(id: String): ReminderCategory {
            return entries.firstOrNull { it.id == id } ?: PERSONAL
        }
    }
}

enum class RecurrenceType(
    val id: String,
    val displayName: String,
    val isInfinite: Boolean
) {
    ONCE("ONCE", "Una sola vez", false),
    DAILY("DAILY", "Todos los días (Infinito)", true),
    WEEKDAYS("WEEKDAYS", "Lunes a Viernes", true),
    WEEKLY("WEEKLY", "Semanal", true),
    MONTHLY("MONTHLY", "Mensual", true),
    YEARLY("YEARLY", "Anual", true),
    INTERVAL_HOURS("INTERVAL_HOURS", "Cada X horas", true);

    companion object {
        fun fromId(id: String): RecurrenceType {
            return entries.firstOrNull { it.id == id } ?: ONCE
        }
    }
}

enum class ReminderPriority(
    val id: String,
    val displayName: String,
    val colorHex: Long
) {
    NORMAL("NORMAL", "Normal", 0xFF64748B),
    MEDIUM("MEDIUM", "Importante", 0xFFF59E0B),
    HIGH("HIGH", "Urgente", 0xFFEF4444);

    companion object {
        fun fromId(id: String): ReminderPriority {
            return entries.firstOrNull { it.id == id } ?: NORMAL
        }
    }
}
