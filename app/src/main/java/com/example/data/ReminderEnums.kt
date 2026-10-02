package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CreativeColor
import com.example.ui.theme.ErrandsColor
import com.example.ui.theme.FinanceColor
import com.example.ui.theme.FitnessColor
import com.example.ui.theme.FutureColor
import com.example.ui.theme.GamingColor
import com.example.ui.theme.GeneralColor
import com.example.ui.theme.HabitColor
import com.example.ui.theme.HydrationColor
import com.example.ui.theme.MealColor
import com.example.ui.theme.MedColor
import com.example.ui.theme.PetsPlantsColor
import com.example.ui.theme.SchoolColor
import com.example.ui.theme.SleepColor
import com.example.ui.theme.SocialColor
import com.example.ui.theme.SpiritualColor
import com.example.ui.theme.TechColor
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
    HYDRATION("HYDRATION", "Hidratación", "💧", Icons.Default.WaterDrop, HydrationColor),
    HABIT("HABIT", "Hábito", "✨", Icons.Default.FitnessCenter, HabitColor),
    FITNESS("FITNESS", "Ejercicio", "🏃", Icons.Default.FitnessCenter, FitnessColor),
    SLEEP("SLEEP", "Dormir", "🌙", Icons.Default.Bedtime, SleepColor),
    WORK("WORK", "Trabajo", "💼", Icons.Default.Work, WorkColor),
    SCHOOL("SCHOOL", "Estudio", "📚", Icons.Default.School, SchoolColor),
    FINANCE("FINANCE", "Dinero", "💳", Icons.Default.Business, FinanceColor),
    ERRANDS("ERRANDS", "Tareas", "🧹", Icons.Default.Build, ErrandsColor),
    SOCIAL("SOCIAL", "Social", "📞", Icons.Default.PhoneInTalk, SocialColor),
    SPIRITUAL("SPIRITUAL", "Espiritual", "🧘", Icons.Default.AutoAwesome, SpiritualColor),
    CREATIVE("CREATIVE", "Creativo", "🎨", Icons.Default.Brush, CreativeColor),
    GAMING("GAMING", "Gaming", "🎮", Icons.Default.SportsEsports, GamingColor),
    TECH("TECH", "Tecnología", "🔋", Icons.Default.Devices, TechColor),
    PETS_PLANTS("PETS_PLANTS", "Mascotas y plantas", "🐾", Icons.Default.Pets, PetsPlantsColor),
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
    INTERVAL_HOURS("INTERVAL_HOURS", "Cada X horas", true),
    INTERVAL_MINUTES("INTERVAL_MINUTES", "Cada X minutos", true),
    GENERIC_INTERVAL("GENERIC_INTERVAL", "Cada X unidades", true);

    companion object {
        fun fromId(id: String): RecurrenceType {
            return entries.firstOrNull { it.id == id } ?: ONCE
        }
    }
}

enum class IntervalUnit(val id: String, val displayName: String) {
    SECOND("SECOND", "segundo(s)"),
    MINUTE("MINUTE", "minuto(s)"),
    HOUR("HOUR", "hora(s)"),
    DAY("DAY", "día(s)"),
    WEEK("WEEK", "semana(s)"),
    MONTH("MONTH", "mes(es)"),
    YEAR("YEAR", "año(s)"),
    CENTURY("CENTURY", "siglo(s)");

    companion object {
        fun fromId(id: String): IntervalUnit = entries.firstOrNull { it.id == id } ?: MINUTE
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
