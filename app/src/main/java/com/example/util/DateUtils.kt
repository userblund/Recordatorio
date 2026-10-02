package com.example.util

import com.example.data.RecurrenceType
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private val MONTH_NAMES_ES = arrayOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    fun computeTriggerMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0
    ): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, second.coerceIn(0, 59))
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatTime(hour: Int, minute: Int, second: Int = 0): String {
        return if (second > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }
    }

    fun formatDate(year: Int, month: Int, day: Int): String {
        val monthIdx = (month - 1).coerceIn(0, 11)
        val monthName = MONTH_NAMES_ES[monthIdx]
        return if (year > 2100) {
            String.format(Locale.getDefault(), "%02d %s, Año %d", day, monthName, year)
        } else {
            String.format(Locale.getDefault(), "%02d %s %d", day, monthName, year)
        }
    }

    fun formatRelativeCountdown(triggerMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = triggerMillis - now

        if (diff <= 0) {
            val pastDiff = -diff
            val pastMinutes = pastDiff / (1000 * 60)
            return if (pastMinutes < 60) {
                if (pastMinutes <= 1) "¡Es ahora!" else "Hace $pastMinutes min"
            } else {
                val pastHours = pastMinutes / 60
                if (pastHours < 24) "Hace $pastHours h" else "Pendiente"
            }
        }

        val totalSeconds = diff / 1000
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        val totalDays = totalHours / 24
        val totalYears = (totalDays / 365.25).toLong()

        return when {
            totalSeconds < 60 -> "En $totalSeconds s"
            totalMinutes < 1 -> "En menos de 1 min"
            totalMinutes < 60 -> "En $totalMinutes min"
            totalHours < 24 -> {
                val remMinutes = totalMinutes % 60
                if (remMinutes == 0L) "En $totalHours h" else "En $totalHours h $remMinutes min"
            }
            totalDays == 1L -> "Mañana"
            totalDays < 30 -> "En $totalDays días"
            totalDays < 365 -> {
                val months = totalDays / 30
                "En ~$months ${if (months == 1L) "mes" else "meses"}"
            }
            totalYears in 1..2 -> "En ~$totalYears ${if (totalYears == 1L) "año" else "años"}"
            else -> {
                val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
                val targetYear = cal.get(Calendar.YEAR)
                "En $totalYears años (Año $targetYear)"
            }
        }
    }

    fun isToday(year: Int, month: Int, day: Int, now: Long = System.currentTimeMillis()): Boolean {
        val today = Calendar.getInstance().apply { timeInMillis = now }
        return today.get(Calendar.YEAR) == year &&
                (today.get(Calendar.MONTH) + 1) == month &&
                today.get(Calendar.DAY_OF_MONTH) == day
    }

    fun isFarFuture(year: Int): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return year >= currentYear + 2 || year >= 2030
    }

    fun computeNextOccurrence(
        currentYear: Int,
        currentMonth: Int,
        currentDay: Int,
        hour: Int,
        minute: Int,
        recurrenceType: RecurrenceType,
        intervalHours: Int = 8,
        intervalMinutes: Int = 0,
        second: Int = 0
    ): NextScheduledDate {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, currentDay)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, second)
        cal.set(Calendar.MILLISECOND, 0)

        when (recurrenceType) {
            RecurrenceType.ONCE -> {
                // No change
            }
            RecurrenceType.DAILY -> {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            RecurrenceType.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                } while (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
            }
            RecurrenceType.WEEKLY -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
            RecurrenceType.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
            }
            RecurrenceType.YEARLY -> {
                cal.add(Calendar.YEAR, 1)
            }
            RecurrenceType.INTERVAL_HOURS -> {
                cal.add(Calendar.HOUR_OF_DAY, intervalHours.coerceAtLeast(1))
            }
            RecurrenceType.INTERVAL_MINUTES -> {
                cal.add(Calendar.MINUTE, intervalMinutes.coerceAtLeast(1))
            }
        }

        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        val newDay = cal.get(Calendar.DAY_OF_MONTH)
        val newHour = cal.get(Calendar.HOUR_OF_DAY)
        val newMinute = cal.get(Calendar.MINUTE)
        val newSecond = cal.get(Calendar.SECOND)
        val newMillis = cal.timeInMillis

        return NextScheduledDate(
            year = newYear,
            month = newMonth,
            day = newDay,
            hour = newHour,
            minute = newMinute,
            second = newSecond,
            triggerMillis = newMillis
        )
    }

    data class NextScheduledDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int = 0,
        val triggerMillis: Long
    )
}
