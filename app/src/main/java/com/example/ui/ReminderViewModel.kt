package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RecurrenceType
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderPriority
import com.example.data.ReminderRepository
import com.example.util.ActiveAlarmData
import com.example.util.AlarmPlayer
import com.example.util.DateUtils
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReminderTab(val label: String, val iconText: String) {
    TODAY("Hoy", "⏰"),
    MEALS("Comidas", "🍽️"),
    MEDS("Remedios", "💊"),
    ALL("Todos", "📋"),
    FAR_FUTURE("Infinito", "🚀"),
    COMPLETED("Historial", "✅")
}

data class TodayStats(
    val pendingTodayCount: Int = 0,
    val completedTodayCount: Int = 0,
    val nextReminder: ReminderEntity? = null
)

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val appContext: Context
) : ViewModel() {

    val isAlarmPlaying: StateFlow<Boolean> = AlarmPlayer.isAlarmPlaying
    val currentAlarm: StateFlow<ActiveAlarmData?> = AlarmPlayer.currentAlarm

    private val _selectedTab = MutableStateFlow(ReminderTab.TODAY)
    val selectedTab: StateFlow<ReminderTab> = _selectedTab

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayStats: StateFlow<TodayStats> = allReminders.combine(_selectedTab) { list, _ ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curDay = cal.get(Calendar.DAY_OF_MONTH)

        val todayReminders = list.filter {
            it.year == curYear && it.month == curMonth && it.day == curDay ||
                    (RecurrenceType.fromId(it.recurrenceType).isInfinite && !it.isCompleted)
        }

        val pending = todayReminders.filter { !it.isCompleted }
        val completed = list.filter {
            it.isCompleted && it.completedAtMillis != null && DateUtils.isToday(
                curYear,
                curMonth,
                curDay,
                it.completedAtMillis
            )
        }

        val nextUpcoming = list
            .filter { !it.isCompleted && it.triggerTimeMillis >= now }
            .minByOrNull { it.triggerTimeMillis }

        TodayStats(
            pendingTodayCount = pending.size,
            completedTodayCount = completed.size,
            nextReminder = nextUpcoming
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayStats()
    )

    val displayedReminders: StateFlow<List<ReminderEntity>> = combine(
        allReminders,
        _selectedTab,
        _searchQuery
    ) { list, tab, query ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curDay = cal.get(Calendar.DAY_OF_MONTH)

        val filteredByTab = when (tab) {
            ReminderTab.TODAY -> list.filter {
                !it.isCompleted && (
                        (it.year == curYear && it.month == curMonth && it.day == curDay) ||
                                (RecurrenceType.fromId(it.recurrenceType).isInfinite) ||
                                (it.triggerTimeMillis in (now - 12 * 3600 * 1000)..(now + 24 * 3600 * 1000))
                        )
            }
            ReminderTab.MEALS -> list.filter {
                !it.isCompleted && it.category == ReminderCategory.MEAL.id
            }
            ReminderTab.MEDS -> list.filter {
                !it.isCompleted && it.category == ReminderCategory.MEDICATION.id
            }
            ReminderTab.ALL -> list.filter { !it.isCompleted }
            ReminderTab.FAR_FUTURE -> list.filter {
                !it.isCompleted && (it.year > curYear + 1 || it.category == ReminderCategory.FAR_FUTURE.id || it.year >= 2030)
            }
            ReminderTab.COMPLETED -> list.filter { it.isCompleted }
        }

        if (query.isBlank()) {
            filteredByTab
        } else {
            val q = query.trim().lowercase()
            filteredByTab.filter {
                it.title.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.year.toString().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: ReminderTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.toggleCompleted(reminder)
            if (!reminder.isCompleted) {
                // If it was active and now completed, cancel alarm
                ReminderScheduler.cancelReminder(appContext, reminder.id)
            } else {
                // Reactivated
                ReminderScheduler.scheduleReminder(appContext, reminder)
            }
        }
    }

    fun snooze(reminder: ReminderEntity, minutes: Int) {
        viewModelScope.launch {
            repository.snooze(reminder, minutes)
            val updated = repository.getReminderById(reminder.id)
            if (updated != null) {
                ReminderScheduler.scheduleReminder(appContext, updated)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(appContext, reminder.id)
            repository.delete(reminder)
        }
    }

    fun saveReminder(
        id: Long = 0,
        title: String,
        description: String,
        category: ReminderCategory,
        priority: ReminderPriority,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        recurrenceType: RecurrenceType,
        recurrenceIntervalHours: Int = 8
    ) {
        viewModelScope.launch {
            val triggerMillis = DateUtils.computeTriggerMillis(year, month, day, hour, minute)
            val entity = ReminderEntity(
                id = id,
                title = title.trim(),
                description = description.trim(),
                category = category.id,
                priority = priority.id,
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                triggerTimeMillis = triggerMillis,
                recurrenceType = recurrenceType.id,
                recurrenceIntervalHours = recurrenceIntervalHours,
                isCompleted = false
            )

            if (id == 0L) {
                val newId = repository.insert(entity)
                val inserted = entity.copy(id = newId)
                ReminderScheduler.scheduleReminder(appContext, inserted)
            } else {
                repository.update(entity)
                ReminderScheduler.scheduleReminder(appContext, entity)
            }
        }
    }

    fun createFromQuickTemplate(templateType: String) {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curDay = cal.get(Calendar.DAY_OF_MONTH)

        when (templateType) {
            "BREAKFAST" -> saveReminder(
                title = "Desayuno Energético 🥐",
                description = "Hora de desayunar con calma e hidratarse.",
                category = ReminderCategory.MEAL,
                priority = ReminderPriority.NORMAL,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 8,
                minute = 30,
                recurrenceType = RecurrenceType.DAILY
            )
            "LUNCH" -> saveReminder(
                title = "Almuerzo Saludable 🥗",
                description = "Comer pausadamente y desconectar unos minutos.",
                category = ReminderCategory.MEAL,
                priority = ReminderPriority.NORMAL,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 13,
                minute = 30,
                recurrenceType = RecurrenceType.DAILY
            )
            "DINNER" -> saveReminder(
                title = "Cena Ligera 🍲",
                description = "Cena saludable al menos 2 horas antes de dormir.",
                category = ReminderCategory.MEAL,
                priority = ReminderPriority.NORMAL,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 20,
                minute = 30,
                recurrenceType = RecurrenceType.DAILY
            )
            "WATER" -> saveReminder(
                title = "Beber Agua 💧",
                description = "Mantén tu cuerpo hidratado (1 vaso de agua).",
                category = ReminderCategory.HABIT,
                priority = ReminderPriority.NORMAL,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = cal.get(Calendar.HOUR_OF_DAY) + 1,
                minute = 0,
                recurrenceType = RecurrenceType.INTERVAL_HOURS,
                recurrenceIntervalHours = 2
            )
            "MED_MORNING" -> saveReminder(
                title = "Remedio de la Mañana 💊",
                description = "Tomar la dosis indicada por el médico.",
                category = ReminderCategory.MEDICATION,
                priority = ReminderPriority.HIGH,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 8,
                minute = 0,
                recurrenceType = RecurrenceType.DAILY
            )
            "MED_NIGHT" -> saveReminder(
                title = "Remedio de la Noche 💊",
                description = "Tomar el medicamento o suplemento antes de acostarse.",
                category = ReminderCategory.MEDICATION,
                priority = ReminderPriority.HIGH,
                year = curYear,
                month = curMonth,
                day = curDay,
                hour = 22,
                minute = 0,
                recurrenceType = RecurrenceType.DAILY
            )
            "YEAR_2500" -> saveReminder(
                title = "Cápsula del Futuro Año 2500 🚀",
                description = "¡Recordatorio infinito programado para dentro de casi 500 años!",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.MEDIUM,
                year = 2500,
                month = 1,
                day = 1,
                hour = 12,
                minute = 0,
                recurrenceType = RecurrenceType.ONCE
            )
            "YEAR_10000" -> saveReminder(
                title = "Mensaje a la Humanidad: Año 10000 🌌",
                description = "Recordatorio ultra-futurista: persistido en la base de datos sin límites de tiempo.",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.HIGH,
                year = 10000,
                month = 1,
                day = 1,
                hour = 0,
                minute = 0,
                recurrenceType = RecurrenceType.ONCE
            )
        }
    }

    fun triggerTestAlarm() {
        ReminderScheduler.triggerTestAlarm(appContext)
    }

    fun dismissActiveAlarm() {
        val alarm = currentAlarm.value
        AlarmPlayer.stopAlarm(appContext)
        if (alarm != null && alarm.reminderId > 0 && alarm.reminderId != 99999L) {
            viewModelScope.launch {
                val item = repository.getReminderById(alarm.reminderId)
                if (item != null) {
                    repository.toggleCompleted(item)
                }
            }
        }
    }

    fun snoozeActiveAlarm(minutes: Int = 10) {
        val alarm = currentAlarm.value
        AlarmPlayer.stopAlarm(appContext)
        if (alarm != null && alarm.reminderId > 0 && alarm.reminderId != 99999L) {
            viewModelScope.launch {
                val item = repository.getReminderById(alarm.reminderId)
                if (item != null) {
                    repository.snooze(item, minutes)
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ReminderRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReminderViewModel(repository, context.applicationContext) as T
            }
        }
    }
}
