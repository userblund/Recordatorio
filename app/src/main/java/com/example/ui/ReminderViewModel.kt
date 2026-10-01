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
            // The repository is the source of truth because a recurring reminder
            // may remain active while its canonical occurrence advances.
            repository.toggleCompleted(reminder)

            val updated = repository.getReminderById(reminder.id)
            if (updated == null) {
                ReminderScheduler.cancelReminder(appContext, reminder.id)
                AlarmPlayer.stopAlarm(appContext, reminder.id)
                return@launch
            }

            if (updated.isCompleted) {
                ReminderScheduler.cancelReminder(appContext, updated.id)
                AlarmPlayer.stopAlarm(appContext, updated.id)
            } else {
                // Recurring reminders are advanced to their next occurrence by
                // the repository. Schedule that new occurrence rather than
                // cancelling it after the database update.
                ReminderScheduler.scheduleReminder(appContext, updated)
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
        val now = Calendar.getInstance()

        fun nextClock(hour: Int, minute: Int): Calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }

        fun addClock(
            title: String,
            description: String,
            category: ReminderCategory,
            priority: ReminderPriority = ReminderPriority.NORMAL,
            hour: Int,
            minute: Int,
            recurrence: RecurrenceType = RecurrenceType.DAILY
        ) {
            val c = nextClock(hour, minute)
            saveReminder(
                title = title,
                description = description,
                category = category,
                priority = priority,
                year = c.get(Calendar.YEAR),
                month = c.get(Calendar.MONTH) + 1,
                day = c.get(Calendar.DAY_OF_MONTH),
                hour = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                recurrenceType = recurrence
            )
        }

        fun addInterval(
            title: String,
            description: String,
            category: ReminderCategory,
            hours: Int,
            priority: ReminderPriority = ReminderPriority.NORMAL
        ) {
            val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, hours) }
            saveReminder(
                title = title,
                description = description,
                category = category,
                priority = priority,
                year = c.get(Calendar.YEAR),
                month = c.get(Calendar.MONTH) + 1,
                day = c.get(Calendar.DAY_OF_MONTH),
                hour = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                recurrenceType = RecurrenceType.INTERVAL_HOURS,
                recurrenceIntervalHours = hours
            )
        }

        when (templateType) {
            "WATER" -> addInterval("Beber Agua 💧", "Toma agua y responde desde la notificación: OK → ¿ya tomaste?", ReminderCategory.HABIT, 2)
            "BREAKFAST" -> addClock("Desayuno 🥐", "Momento para desayunar.", ReminderCategory.MEAL, hour = 8, minute = 30)
            "LUNCH" -> addClock("Almuerzo 🥗", "Momento para almorzar.", ReminderCategory.MEAL, hour = 13, minute = 30)
            "DINNER" -> addClock("Cena 🍲", "Momento para cenar.", ReminderCategory.MEAL, hour = 20, minute = 30)
            "MED_MORNING" -> addClock("Medicamento de la mañana 💊", "Tomar según la indicación correspondiente.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 8, 0)
            "MED_NIGHT" -> addClock("Medicamento de la noche 💊", "Tomar según la indicación correspondiente.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 22, 0)

            "EYE_BREAK" -> addInterval("Descanso de ojos 👀", "Mira lejos de la pantalla y descansa la vista.", ReminderCategory.HABIT, 1)
            "STRETCH" -> addInterval("Estiramiento 🧘", "Haz una breve pausa para moverte y estirarte.", ReminderCategory.HABIT, 2)
            "WALK" -> addClock("Caminar 🚶", "Momento para caminar un poco.", ReminderCategory.HABIT, hour = 18, minute = 0)
            "EXERCISE" -> addClock("Ejercicio 🏃", "Sesión de actividad física programada.", ReminderCategory.HABIT, hour = 17, minute = 30)
            "SLEEP" -> addClock("Prepararse para dormir 🌙", "Empieza tu rutina de descanso.", ReminderCategory.HABIT, hour = 22, minute = 30)
            "WAKE" -> addClock("Despertar ☀️", "Inicio del día.", ReminderCategory.HABIT, hour = 7, minute = 0)
            "SHOWER" -> addClock("Ducha 🚿", "Momento de tu rutina personal.", ReminderCategory.PERSONAL, hour = 7, minute = 30)
            "BRUSH_TEETH" -> addClock("Cepillarse 🪥", "Momento de tu rutina de higiene.", ReminderCategory.HABIT, hour = 7, minute = 45)
            "SKINCARE" -> addClock("Cuidado personal 🧴", "Rutina personal.", ReminderCategory.PERSONAL, hour = 21, minute = 0)

            "STUDY" -> addClock("Estudiar 📚", "Comienza tu sesión de estudio.", ReminderCategory.WORK, hour = 16, minute = 0)
            "HOMEWORK" -> addClock("Tarea 📝", "Revisa y completa tus tareas pendientes.", ReminderCategory.WORK, hour = 18, minute = 30)
            "READ" -> addClock("Leer 📖", "Tiempo reservado para leer.", ReminderCategory.PERSONAL, hour = 21, minute = 30)
            "BREAK_STUDY" -> addInterval("Pausa de estudio ⏸️", "Descansa antes de continuar.", ReminderCategory.WORK, 1)
            "WORK_START" -> addClock("Empezar trabajo 💼", "Comienza tu bloque de trabajo.", ReminderCategory.WORK, hour = 9, minute = 0)
            "WORK_BREAK" -> addInterval("Pausa de trabajo ☕", "Haz una pausa breve.", ReminderCategory.WORK, 2)
            "MEETING" -> addClock("Reunión 👥", "Revisa la reunión programada.", ReminderCategory.WORK, hour = 10, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "DEADLINE" -> addClock("Fecha límite ⏳", "Revisa esta tarea antes de la fecha límite.", ReminderCategory.WORK, hour = 17, minute = 0, recurrence = RecurrenceType.ONCE)

            "BACKUP" -> addClock("Copia de seguridad 💾", "Revisa o realiza una copia de seguridad.", ReminderCategory.PERSONAL, hour = 20, minute = 0)
            "UPDATE" -> addClock("Buscar actualizaciones 🔄", "Revisa las actualizaciones importantes.", ReminderCategory.PERSONAL, hour = 19, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "CHARGE_PHONE" -> addClock("Cargar teléfono 🔋", "Conecta el teléfono para cargarlo.", ReminderCategory.PERSONAL, hour = 23, minute = 0)
            "CLEAN_ROOM" -> addClock("Ordenar habitación 🧹", "Ordena un poco tu espacio.", ReminderCategory.PERSONAL, hour = 11, minute = 0)
            "LAUNDRY" -> addClock("Lavar ropa 👕", "Revisa la ropa pendiente.", ReminderCategory.PERSONAL, hour = 10, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "DISHES" -> addInterval("Lavar platos 🍽️", "Revisa los platos pendientes.", ReminderCategory.PERSONAL, 2)
            "TRASH" -> addClock("Sacar basura 🗑️", "Revisa si corresponde sacar la basura.", ReminderCategory.PERSONAL, hour = 20, minute = 30, recurrence = RecurrenceType.WEEKLY)
            "SHOPPING" -> addClock("Compras 🛒", "Revisa tu lista de compras.", ReminderCategory.PERSONAL, hour = 18, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "BILL" -> addClock("Pagar cuenta 💳", "Revisa una cuenta o pago pendiente.", ReminderCategory.PERSONAL, ReminderPriority.HIGH, 12, 0, RecurrenceType.MONTHLY)
            "SAVE_MONEY" -> addClock("Revisar ahorro 💰", "Revisa tus gastos y objetivos de ahorro.", ReminderCategory.PERSONAL, hour = 20, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "COOK" -> addClock("Cocinar 👨‍🍳", "Hora de preparar comida.", ReminderCategory.MEAL, hour = 12, minute = 0)
            "PET" -> addClock("Cuidar mascota 🐾", "Revisa comida, agua y cuidados pendientes.", ReminderCategory.PERSONAL, hour = 8, minute = 0)
            "PLANTS" -> addClock("Regar plantas 🪴", "Revisa si tus plantas necesitan agua.", ReminderCategory.PERSONAL, hour = 9, minute = 30, recurrence = RecurrenceType.WEEKLY)

            "CALL_FAMILY" -> addClock("Llamar familia 📞", "Recuerda hacer esa llamada.", ReminderCategory.PERSONAL, hour = 19, minute = 30, recurrence = RecurrenceType.WEEKLY)
            "MESSAGE" -> addClock("Enviar mensaje 💬", "Revisa a quién querías responder.", ReminderCategory.PERSONAL, hour = 18, minute = 30)
            "APPOINTMENT" -> addClock("Cita 📅", "Revisa los detalles de tu cita.", ReminderCategory.PERSONAL, ReminderPriority.HIGH, 9, 0, RecurrenceType.ONCE)
            "TRAVEL" -> addClock("Preparar viaje 🧳", "Revisa documentos, equipaje y lo necesario.", ReminderCategory.PERSONAL, hour = 20, minute = 0, recurrence = RecurrenceType.ONCE)
            "KEYS" -> addInterval("Revisar llaves 🔑", "Antes de salir, comprueba que llevas tus llaves.", ReminderCategory.PERSONAL, 1)

            "GAMING_WATER" -> addInterval("Agua mientras juegas 🎮💧", "Recordatorio pensado para aparecer sobre el juego sin abrir Recordatorio.", ReminderCategory.HABIT, 2)
            "GAMING_BREAK" -> addInterval("Pausa de videojuego 🎮⏸️", "Haz una pausa breve; la notificación no abre la aplicación.", ReminderCategory.HABIT, 1)
            "GAMING_EYES" -> addInterval("Descanso de ojos gaming 🎮👀", "Descansa la vista unos minutos.", ReminderCategory.HABIT, 1)
            "SAVE_GAME" -> addInterval("Guardar partida 🎮💾", "Recuerda guardar tu progreso.", ReminderCategory.PERSONAL, 2)
            "SERVER_CHECK" -> addInterval("Revisar servidor 🖥️", "Comprueba el estado de tu servidor cuando corresponda.", ReminderCategory.PERSONAL, 2)
            "STREAM_START" -> addClock("Empezar stream 📺", "Prepara el directo.", ReminderCategory.PERSONAL, hour = 20, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "STREAM_BREAK" -> addInterval("Pausa del stream 🎥", "Haz una pausa sin cerrar tu flujo de trabajo.", ReminderCategory.HABIT, 2)
            "CONTENT" -> addClock("Crear contenido 🎬", "Bloque de tiempo para crear contenido.", ReminderCategory.PERSONAL, hour = 16, minute = 0)
            "MUSIC" -> addClock("Práctica de música 🎵", "Tiempo reservado para practicar.", ReminderCategory.PERSONAL, hour = 18, minute = 0)
            "CREATIVE" -> addClock("Proyecto creativo 🎨", "Tiempo reservado para tu proyecto.", ReminderCategory.PERSONAL, hour = 17, minute = 0)

            "MEDITATE" -> addClock("Pausa tranquila 🧘‍♂️", "Tómate unos minutos para desconectar.", ReminderCategory.HABIT, hour = 20, minute = 30)
            "BREATHING" -> addInterval("Respirar y descansar 🌿", "Pausa breve para relajarte.", ReminderCategory.HABIT, 2)
            "POSTURE" -> addInterval("Revisar postura 🪑", "Comprueba que estás cómodo y cambia de posición.", ReminderCategory.HABIT, 1)
            "SUNLIGHT" -> addClock("Tomar aire/luz 🌤️", "Sal un momento si puedes y haz una pausa.", ReminderCategory.HABIT, hour = 10, minute = 0)

            "CAPSULE_2500" -> saveReminder(
                title = "Cápsula del Futuro Año 2500 🚀",
                description = "Recordatorio guardado para el futuro distante.",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.MEDIUM,
                year = 2500, month = 1, day = 1, hour = 12, minute = 0,
                recurrenceType = RecurrenceType.ONCE
            )
            "YEAR_10000" -> saveReminder(
                title = "Mensaje a la Humanidad: Año 10000 🌌",
                description = "Recordatorio ultra-futurista guardado en la base de datos.",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.HIGH,
                year = 10000, month = 1, day = 1, hour = 0, minute = 0,
                recurrenceType = RecurrenceType.ONCE
            )
        }
    }

    fun triggerTestAlarm() {
        ReminderScheduler.triggerTestAlarm(appContext)
    }

    fun dismissActiveAlarm() {
        val alarm = currentAlarm.value
        if (alarm != null && alarm.reminderId > 0 && alarm.reminderId != 99999L) {
            // Stop only the alarm the user is currently acting on. Another
            // reminder may be ringing at the same time.
            AlarmPlayer.stopAlarm(appContext, alarm.reminderId)
            viewModelScope.launch {
                val item = repository.getReminderById(alarm.reminderId)
                if (item != null) {
                    repository.toggleCompleted(item)
                    val updated = repository.getReminderById(item.id)
                    if (updated != null && !updated.isCompleted) {
                        ReminderScheduler.scheduleReminder(appContext, updated)
                    } else {
                        ReminderScheduler.cancelReminder(appContext, item.id)
                    }
                }
            }
        }
    }

    fun snoozeActiveAlarm(minutes: Int = 10) {
        val alarm = currentAlarm.value
        if (alarm != null && alarm.reminderId > 0 && alarm.reminderId != 99999L) {
            AlarmPlayer.stopAlarm(appContext, alarm.reminderId)
            viewModelScope.launch {
                val item = repository.getReminderById(alarm.reminderId)
                if (item != null) {
                    repository.snooze(item, minutes)
                    val updated = repository.getReminderById(item.id)
                    if (updated != null) {
                        ReminderScheduler.scheduleReminder(appContext, updated)
                    }
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
