package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CustomTemplateEntity
import com.example.data.IntervalUnit
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
    GAMING("Gaming", "🎮"),
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

    val customTemplates: StateFlow<List<CustomTemplateEntity>> = repository.allCustomTemplates
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
                !it.isCompleted && (it.category == ReminderCategory.MEAL.id ||
                        it.category == ReminderCategory.HYDRATION.id)
            }
            ReminderTab.MEDS -> list.filter {
                !it.isCompleted && it.category == ReminderCategory.MEDICATION.id
            }
            ReminderTab.GAMING -> list.filter {
                !it.isCompleted && it.category == ReminderCategory.GAMING.id
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
        second: Int = 0,
        recurrenceType: RecurrenceType,
        recurrenceIntervalHours: Int = 8,
        recurrenceIntervalMinutes: Int = 0,
        recurrenceIntervalValue: Long = 1L,
        recurrenceIntervalUnit: IntervalUnit = IntervalUnit.MINUTE
    ) {
        viewModelScope.launch {
            val triggerMillis = DateUtils.computeTriggerMillis(year, month, day, hour, minute, second)
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
                second = second,
                triggerTimeMillis = triggerMillis,
                recurrenceType = recurrenceType.id,
                recurrenceIntervalHours = recurrenceIntervalHours,
                recurrenceIntervalMinutes = recurrenceIntervalMinutes,
                recurrenceIntervalValue = recurrenceIntervalValue.coerceAtLeast(1L),
                recurrenceIntervalUnit = recurrenceIntervalUnit.id,
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

    // ---------------------------------------------------------------------------
    // Built-in quick templates. The "when" block is intentionally long because
    // the user explicitly requested the maximum amount of useful templates,
    // including game-specific ones (Minecraft Bedrock, Adopt Me, Free Fire,
    // Roblox general, plus generic gaming and life templates).
    // ---------------------------------------------------------------------------
    fun createFromQuickTemplate(templateType: String) {
        val now = Calendar.getInstance()

        fun nextClock(hour: Int, minute: Int, second: Int = 0): Calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
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
            second: Int = 0,
            recurrence: RecurrenceType = RecurrenceType.DAILY
        ) {
            val c = nextClock(hour, minute, second)
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
                second = c.get(Calendar.SECOND),
                recurrenceType = recurrence
            )
        }

        fun addIntervalHours(
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
                second = c.get(Calendar.SECOND),
                recurrenceType = RecurrenceType.INTERVAL_HOURS,
                recurrenceIntervalHours = hours
            )
        }

        fun addIntervalMinutes(
            title: String,
            description: String,
            category: ReminderCategory,
            minutes: Int,
            priority: ReminderPriority = ReminderPriority.NORMAL
        ) {
            val c = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }
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
                second = c.get(Calendar.SECOND),
                recurrenceType = RecurrenceType.INTERVAL_MINUTES,
                recurrenceIntervalMinutes = minutes
            )
        }

        fun addIntervalSeconds(
            title: String,
            description: String,
            category: ReminderCategory,
            seconds: Int,
            priority: ReminderPriority = ReminderPriority.NORMAL
        ) {
            // For sub-minute intervals, store as INTERVAL_MINUTES=1 and let the
            // trigger time carry the precise second. This is useful for very
            // short AFK/game-event timers (e.g. "30s AFK check").
            val c = Calendar.getInstance().apply { add(Calendar.SECOND, seconds) }
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
                second = c.get(Calendar.SECOND),
                recurrenceType = RecurrenceType.GENERIC_INTERVAL,
                recurrenceIntervalValue = seconds.toLong().coerceAtLeast(1L),
                recurrenceIntervalUnit = IntervalUnit.SECOND
            )
        }

        when (templateType) {
            // ---------- HEALTH / MEDS ----------
            "MED_MORNING" -> addClock("Medicamento de la mañana 💊", "Tomar según la indicación correspondiente.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 8, 0)
            "MED_NIGHT" -> addClock("Medicamento de la noche 💊", "Tomar según la indicación correspondiente.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 22, 0)
            "MED_LUNCH" -> addClock("Medicamento del mediodía 💊", "Tomar con comida.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 13, 0)
            "MED_INTERVAL_4" -> addIntervalHours("Medicamento cada 4h 💊", "Dosis programada cada 4 horas.", ReminderCategory.MEDICATION, 4, ReminderPriority.HIGH)
            "MED_INTERVAL_6" -> addIntervalHours("Medicamento cada 6h 💊", "Dosis programada cada 6 horas.", ReminderCategory.MEDICATION, 6, ReminderPriority.HIGH)
            "MED_INTERVAL_8" -> addIntervalHours("Medicamento cada 8h 💊", "Dosis programada cada 8 horas.", ReminderCategory.MEDICATION, 8, ReminderPriority.HIGH)
            "MED_INTERVAL_12" -> addIntervalHours("Medicamento cada 12h 💊", "Dosis programada cada 12 horas.", ReminderCategory.MEDICATION, 12, ReminderPriority.HIGH)
            "MED_INTERVAL_24" -> addClock("Medicamento diario 💊", "Toma única diaria.", ReminderCategory.MEDICATION, ReminderPriority.HIGH, 9, 0)

            // ---------- HYDRATION / MEALS ----------
            "WATER" -> addIntervalHours("Beber Agua 💧", "Toma agua y responde desde la notificación: OK → ¿ya tomaste?", ReminderCategory.HYDRATION, 2)
            "WATER_30M" -> addIntervalMinutes("Beber Agua (cada 30m) 💧", "Recordatorio corto y frecuente.", ReminderCategory.HYDRATION, 30)
            "WATER_GAMING" -> addIntervalMinutes("Agua mientras juegas 🎮💧", "Aparece sin abrir la app. Compatible con Minecraft / Free Fire / Roblox.", ReminderCategory.HYDRATION, 45)
            "BREAKFAST" -> addClock("Desayuno 🥐", "Momento para desayunar.", ReminderCategory.MEAL, hour = 8, minute = 30)
            "LUNCH" -> addClock("Almuerzo 🥗", "Momento para almorzar.", ReminderCategory.MEAL, hour = 13, minute = 30)
            "DINNER" -> addClock("Cena 🍲", "Momento para cenar.", ReminderCategory.MEAL, hour = 20, minute = 30)
            "SNACK" -> addClock("Snack saludable 🍎", "Pequeño refrigerio entre comidas.", ReminderCategory.MEAL, hour = 17, minute = 0)
            "COFFEE" -> addClock("Pausa de café ☕", "Tu pausa de café.", ReminderCategory.MEAL, hour = 10, minute = 30)
            "COOK" -> addClock("Cocinar 👨‍🍳", "Hora de preparar comida.", ReminderCategory.MEAL, hour = 12, minute = 0)

            // ---------- HABITS / FITNESS ----------
            "EYE_BREAK" -> addIntervalHours("Descanso de ojos 👀", "Mira lejos de la pantalla y descansa la vista.", ReminderCategory.HABIT, 1)
            "STRETCH" -> addIntervalHours("Estiramiento 🧘", "Haz una breve pausa para moverte y estirarte.", ReminderCategory.HABIT, 2)
            "WALK" -> addClock("Caminar 🚶", "Momento para caminar un poco.", ReminderCategory.FITNESS, hour = 18, minute = 0)
            "EXERCISE" -> addClock("Ejercicio 🏃", "Sesión de actividad física programada.", ReminderCategory.FITNESS, hour = 17, minute = 30)
            "WORKOUT_30" -> addIntervalMinutes("Micro-entrenamiento 💪", "30 seg de sentadillas/flexiones.", ReminderCategory.FITNESS, 30)
            "POSTURE" -> addIntervalMinutes("Revisar postura 🪑", "Comprueba que estás cómodo y cambia de posición.", ReminderCategory.HABIT, 20)
            "BREATHING" -> addIntervalMinutes("Respirar y descansar 🌿", "Pausa breve para relajarte.", ReminderCategory.SPIRITUAL, 90)
            "MEDITATE" -> addClock("Pausa tranquila 🧘‍♂️", "Tómate unos minutos para desconectar.", ReminderCategory.SPIRITUAL, hour = 20, minute = 30)
            "SUNLIGHT" -> addClock("Tomar aire/luz 🌤️", "Sal un momento si puedes y haz una pausa.", ReminderCategory.HABIT, hour = 10, minute = 0)

            // ---------- SLEEP ----------
            "SLEEP" -> addClock("Prepararse para dormir 🌙", "Empieza tu rutina de descanso.", ReminderCategory.SLEEP, hour = 22, minute = 30)
            "WAKE" -> addClock("Despertar ☀️", "Inicio del día.", ReminderCategory.SLEEP, hour = 7, minute = 0)
            "BEDTIME_PHONE" -> addClock("Alejar el teléfono 📵", "Modo no molestar y dejar el teléfono lejos de la cama.", ReminderCategory.SLEEP, hour = 22, minute = 45)

            // ---------- PERSONAL / SELF-CARE ----------
            "SHOWER" -> addClock("Ducha 🚿", "Momento de tu rutina personal.", ReminderCategory.PERSONAL, hour = 7, minute = 30)
            "BRUSH_TEETH" -> addClock("Cepillarse 🪥", "Momento de tu rutina de higiene.", ReminderCategory.PERSONAL, hour = 7, minute = 45)
            "BRUSH_TEETH_NIGHT" -> addClock("Cepillarse noche 🪥", "Higiene antes de dormir.", ReminderCategory.PERSONAL, hour = 22, minute = 15)
            "SKINCARE" -> addClock("Cuidado personal 🧴", "Rutina personal.", ReminderCategory.PERSONAL, hour = 21, minute = 0)

            // ---------- SCHOOL / STUDY ----------
            "STUDY" -> addClock("Estudiar 📚", "Comienza tu sesión de estudio.", ReminderCategory.SCHOOL, hour = 16, minute = 0)
            "HOMEWORK" -> addClock("Tarea 📝", "Revisa y completa tus tareas pendientes.", ReminderCategory.SCHOOL, hour = 18, minute = 30)
            "READ" -> addClock("Leer 📖", "Tiempo reservado para leer.", ReminderCategory.SCHOOL, hour = 21, minute = 30)
            "BREAK_STUDY" -> addIntervalMinutes("Pausa de estudio ⏸️", "Descansa antes de continuar.", ReminderCategory.SCHOOL, 15)
            "EXAM_PREP" -> addClock("Repaso de examen 📋", "Repasa los temas importantes.", ReminderCategory.SCHOOL, hour = 19, minute = 0)

            // ---------- WORK ----------
            "WORK_START" -> addClock("Empezar trabajo 💼", "Comienza tu bloque de trabajo.", ReminderCategory.WORK, hour = 9, minute = 0)
            "WORK_BREAK" -> addIntervalHours("Pausa de trabajo ☕", "Haz una pausa breve.", ReminderCategory.WORK, 2)
            "MEETING" -> addClock("Reunión 👥", "Revisa la reunión programada.", ReminderCategory.WORK, hour = 10, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "DEADLINE" -> addClock("Fecha límite ⏳", "Revisa esta tarea antes de la fecha límite.", ReminderCategory.WORK, hour = 17, minute = 0, recurrence = RecurrenceType.ONCE)

            // ---------- TECH ----------
            "BACKUP" -> addClock("Copia de seguridad 💾", "Revisa o realiza una copia de seguridad.", ReminderCategory.TECH, hour = 20, minute = 0)
            "UPDATE" -> addClock("Buscar actualizaciones 🔄", "Revisa las actualizaciones importantes.", ReminderCategory.TECH, hour = 19, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "CHARGE_PHONE" -> addClock("Cargar teléfono 🔋", "Conecta el teléfono para cargarlo.", ReminderCategory.TECH, hour = 23, minute = 0)
            "CLEAN_STORAGE" -> addClock("Limpiar almacenamiento 🗃️", "Revisa archivos y libera espacio.", ReminderCategory.TECH, hour = 11, minute = 0, recurrence = RecurrenceType.WEEKLY)

            // ---------- ERRANDS / HOME ----------
            "CLEAN_ROOM" -> addClock("Ordenar habitación 🧹", "Ordena un poco tu espacio.", ReminderCategory.ERRANDS, hour = 11, minute = 0)
            "LAUNDRY" -> addClock("Lavar ropa 👕", "Revisa la ropa pendiente.", ReminderCategory.ERRANDS, hour = 10, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "DISHES" -> addIntervalHours("Lavar platos 🍽️", "Revisa los platos pendientes.", ReminderCategory.ERRANDS, 2)
            "TRASH" -> addClock("Sacar basura 🗑️", "Revisa si corresponde sacar la basura.", ReminderCategory.ERRANDS, hour = 20, minute = 30, recurrence = RecurrenceType.WEEKLY)
            "SHOPPING" -> addClock("Compras 🛒", "Revisa tu lista de compras.", ReminderCategory.ERRANDS, hour = 18, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "COOK_PREP" -> addClock("Preparar ingredientes 🥄", "Saca y pica los ingredientes antes de cocinar.", ReminderCategory.MEAL, hour = 11, minute = 30)

            // ---------- FINANCE ----------
            "BILL" -> addClock("Pagar cuenta 💳", "Revisa una cuenta o pago pendiente.", ReminderCategory.FINANCE, ReminderPriority.HIGH, 12, 0, recurrence = RecurrenceType.MONTHLY)
            "SAVE_MONEY" -> addClock("Revisar ahorro 💰", "Revisa tus gastos y objetivos de ahorro.", ReminderCategory.FINANCE, hour = 20, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "SUBSCRIPTION" -> addClock("Revisar suscripciones 📊", "¿Sigues usando esta suscripción?", ReminderCategory.FINANCE, hour = 10, minute = 0, recurrence = RecurrenceType.MONTHLY)
            "BUDGET_CHECK" -> addClock("Revisar presupuesto 📈", "Compara gastos vs ingresos del mes.", ReminderCategory.FINANCE, hour = 19, minute = 0, recurrence = RecurrenceType.WEEKLY)

            // ---------- PETS / PLANTS ----------
            "PET_FOOD" -> addClock("Alimentar mascota 🐾", "Revisa comida y agua.", ReminderCategory.PETS_PLANTS, hour = 8, minute = 0)
            "PET_WALK" -> addClock("Pasear mascota 🦮", "Tiempo de salir con tu mascota.", ReminderCategory.PETS_PLANTS, hour = 18, minute = 0, recurrence = RecurrenceType.DAILY)
            "PET_MED" -> addClock("Medicamento mascota 💊", "Dosis programada.", ReminderCategory.PETS_PLANTS, ReminderPriority.HIGH, 9, 0)
            "PLANTS" -> addClock("Regar plantas 🪴", "Revisa si tus plantas necesitan agua.", ReminderCategory.PETS_PLANTS, hour = 9, minute = 30, recurrence = RecurrenceType.WEEKLY)

            // ---------- SOCIAL ----------
            "CALL_FAMILY" -> addClock("Llamar familia 📞", "Recuerda hacer esa llamada.", ReminderCategory.SOCIAL, hour = 19, minute = 30, recurrence = RecurrenceType.WEEKLY)
            "MESSAGE" -> addClock("Enviar mensaje 💬", "Revisa a quién querías responder.", ReminderCategory.SOCIAL, hour = 18, minute = 30)
            "APPOINTMENT" -> addClock("Cita 📅", "Revisa los detalles de tu cita.", ReminderCategory.SOCIAL, ReminderPriority.HIGH, 9, 0, recurrence = RecurrenceType.ONCE)
            "BIRTHDAY" -> addClock("Cumpleaños 🎂", "Saluda a esa persona especial.", ReminderCategory.SOCIAL, ReminderPriority.MEDIUM, 0, 0, recurrence = RecurrenceType.YEARLY)

            // ---------- CREATIVE ----------
            "CONTENT" -> addClock("Crear contenido 🎬", "Bloque de tiempo para crear contenido.", ReminderCategory.CREATIVE, hour = 16, minute = 0)
            "MUSIC" -> addClock("Práctica de música 🎵", "Tiempo reservado para practicar.", ReminderCategory.CREATIVE, hour = 18, minute = 0)
            "CREATIVE" -> addClock("Proyecto creativo 🎨", "Tiempo reservado para tu proyecto.", ReminderCategory.CREATIVE, hour = 17, minute = 0)
            "STREAM_START" -> addClock("Empezar stream 📺", "Prepara el directo.", ReminderCategory.CREATIVE, hour = 20, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "STREAM_BREAK" -> addIntervalHours("Pausa del stream 🎥", "Haz una pausa sin cerrar tu flujo de trabajo.", ReminderCategory.CREATIVE, 2)

            // ---------- TRAVEL ----------
            "TRAVEL" -> addClock("Preparar viaje 🧳", "Revisa documentos, equipaje y lo necesario.", ReminderCategory.PERSONAL, hour = 20, minute = 0, recurrence = RecurrenceType.ONCE)
            "KEYS" -> addIntervalHours("Revisar llaves 🔑", "Antes de salir, comprueba que llevas tus llaves.", ReminderCategory.ERRANDS, 1)
            "PACK_BAG" -> addClock("Preparar mochila 🎒", "Revisa lo que necesitas llevar mañana.", ReminderCategory.ERRANDS, hour = 21, minute = 0)

            // ===================================================================
            // GAMING TEMPLATES (USER'S PRIMARY USE CASE)
            // These appear as heads-up notifications WITHOUT opening the app,
            // so games like Minecraft Bedrock single-player, Roblox Adopt Me
            // or Free Fire are never minimized when the reminder fires.
            // ===================================================================

            // ---- Generic gaming (works for any game) ----
            "GAMING_WATER" -> addIntervalMinutes("Agua mientras juegas 🎮💧", "Recordatorio que NO abre la app: aparece sobre el juego.", ReminderCategory.GAMING, 45)
            "GAMING_BREAK" -> addIntervalMinutes("Pausa de videojuego 🎮⏸️", "Pausa breve; la notificación no abre la aplicación.", ReminderCategory.GAMING, 60)
            "GAMING_EYES" -> addIntervalMinutes("Descanso de ojos gaming 🎮👀", "Mira lejos unos 20 segundos para descansar la vista.", ReminderCategory.GAMING, 30)
            "SAVE_GAME" -> addIntervalMinutes("Guardar partida 🎮💾", "Recuerda guardar tu progreso.", ReminderCategory.GAMING, 30)
            "SERVER_CHECK" -> addIntervalMinutes("Revisar servidor 🖥️", "Comprueba el estado de tu servidor.", ReminderCategory.GAMING, 90)
            "AFK_WARNING" -> addIntervalMinutes("Revisar AFK ⚠️", "¿Sigues jugando o te fuiste AFK? Si no, toca OK.", ReminderCategory.GAMING, 15)
            "GAMING_POSTURE" -> addIntervalMinutes("Postura gaming 🪑🎮", "Ajusta cómo estás sentado.", ReminderCategory.GAMING, 45)
            "GAME_DAILY_QUEST" -> addClock("Misiones diarias del juego 🎯", "Revisa/completa tus misiones diarias antes del reset.", ReminderCategory.GAMING, hour = 19, minute = 0)
            "GAME_WEEKLY_RESET" -> addClock("Reset semanal del juego 🗓️", "Misiones/semanales se reinician hoy.", ReminderCategory.GAMING, hour = 4, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "GAME_EVENT_LIVE" -> addIntervalHours("Evento activo del juego 🎉", "El evento sigue disponible: ¡no lo pierdas!", ReminderCategory.GAMING, 6)
            "GAME_LOGIN_BONUS" -> addClock("Bonus de login diario 🎁", "Reclama tu bonus por iniciar sesión.", ReminderCategory.GAMING, hour = 12, minute = 0)
            "GAME_BATTLE_PASS" -> addClock("Revisar battle pass 🎟️", "Avanza en tu battle pass antes de que termine.", ReminderCategory.GAMING, hour = 20, minute = 0, recurrence = RecurrenceType.DAILY)
            "GAME_PVP_RANKED" -> addClock("Ranked del juego 🏆", "Sesión de ranked antes del reset.", ReminderCategory.GAMING, hour = 21, minute = 0, recurrence = RecurrenceType.DAILY)
            "GAME_SCREENSHOT" -> addIntervalHours("Captura de pantalla 📸", "Guarda un momento clave.", ReminderCategory.GAMING, 2)
            "GAME_FRIEND_LIST" -> addClock("Lista de amigos 👥", "Revisa si tus amigos están conectados.", ReminderCategory.GAMING, hour = 19, minute = 30)

            // ---- Minecraft Bedrock (single player focus) ----
            "MC_HUNGER" -> addIntervalMinutes("Revisar hambre en Minecraft 🍗", "¿Tu barra de hambre está baja? Come algo.", ReminderCategory.GAMING, 20)
            "MC_DAY_NIGHT" -> addIntervalMinutes("Ciclo día/noche Minecraft 🌗", "En Bedrock el día dura ~10 min, la noche ~10 min. ¿Es hora de cama?", ReminderCategory.GAMING, 10)
            "MC_SLEEP_SKIP_NIGHT" -> addClock("Dormir para saltar noche 🛏️", "Recuerda usar la cama para saltar la noche.", ReminderCategory.GAMING, hour = 22, minute = 30)
            "MC_SAVE_BEDROCK" -> addIntervalMinutes("Guardar mundo Bedrock 💾⛏️", "Sale y guarda el mundo para no perder progreso.", ReminderCategory.GAMING, 30)
            "MC_BUILD_CHECKPOINT" -> addIntervalMinutes("Checkpoint de construcción 🏗️", "Detente un momento: ¿estás construyendo bien? Guarda.", ReminderCategory.GAMING, 45)
            "MC_END_PREP" -> addClock("Preparar viaje al End 🐉", "Revisa: armadura, comida, perlas, arco.", ReminderCategory.GAMING, hour = 15, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "MC_NETHER_PREP" -> addClock("Preparar viaje al Nether 🔥", "Lleva obsidiana, encantamientos y comida.", ReminderCategory.GAMING, hour = 16, minute = 0, recurrence = RecurrenceType.WEEKLY)
            "MC_INVENTORY_CHECK" -> addIntervalMinutes("Ordenar inventario 🎒⛏️", "Revisa el inventario y organiza.", ReminderCategory.GAMING, 60)
            "MC_VILLAGER_TRADE" -> addClock("Comerciar con aldeanos 🤝 villagers", "Hora de revisar las ofertas.", ReminderCategory.GAMING, hour = 12, minute = 0, recurrence = RecurrenceType.DAILY)

            // ---- Adopt Me (Roblox) ----
            "ADOPT_FEED_PETS" -> addIntervalMinutes("Alimentar mascotas Adopt Me 🐶", "Revisa el hambre de tus mascotas en Adopt Me.", ReminderCategory.GAMING, 30)
            "ADOPT_PET_SLEEP" -> addIntervalHours("Dormir mascotas Adopt Me 😴", "Las mascotas necesitan descansar.", ReminderCategory.GAMING, 4)
            "ADOPT_PET_FUN" -> addIntervalMinutes("Jugar con mascotas Adopt Me 🎾", "Sube la barra de diversión de tus mascotas.", ReminderCategory.GAMING, 45)
            "ADOPT_PET_DRINK" -> addIntervalMinutes("Bebida para mascotas Adopt Me 💧", "Revisa la barra de sed.", ReminderCategory.GAMING, 30)
            "ADOPT_PET_SCHOOL" -> addIntervalHours("Llevar mascotas a la escuela 🏫", "Clases para subir stats.", ReminderCategory.GAMING, 6)
            "ADOPT_DAILY" -> addClock("Recompensa diaria Adopt Me 🎁", "Reclama tu recompensa diaria en Adopt Me.", ReminderCategory.GAMING, hour = 12, minute = 0)
            "ADOPT_TRADE_POST" -> addClock("Revisar Trade Post 🔄", "Mira si hay ofertas interesantes.", ReminderCategory.GAMING, hour = 18, minute = 0)
            "ADOPT_AGE_UP" -> addIntervalHours("Chequear age up mascotas ⏰", "¿Es hora de que tu mascota crezca?", ReminderCategory.GAMING, 6)
            "ADOPT_NEON_PREP" -> addClock("Preparar mascota Neon ✨", "Junta 4 mascotas Full Grown para hacer Neon.", ReminderCategory.GAMING, hour = 14, minute = 0, recurrence = RecurrenceType.WEEKLY)

            // ---- Free Fire ----
            "FF_LOGIN_DAILY" -> addClock("Login diario Free Fire 🔥", "Reclama tu recompensa diaria.", ReminderCategory.GAMING, hour = 12, minute = 0)
            "FF_DAILY_MISSIONS" -> addClock("Misiones diarias Free Fire 📋", "Completa tus misiones antes del reset.", ReminderCategory.GAMING, hour = 22, minute = 0)
            "FF_RANKED_RESET" -> addClock("Ranked reset Free Fire 🏆", "El ranked se reinicia pronto: ¡juega tus partidas!", ReminderCategory.GAMING, hour = 21, minute = 0)
            "FF_DIAMOND_SPIN" -> addClock("Diamond Spin Free Fire 💎", "Reclama tu giro diario de diamonds.", ReminderCategory.GAMING, hour = 13, minute = 0)
            "FF_BATTLE_PASS" -> addClock("Battle Pass Free Fire 🎟️", "Avanza en las misiones del battle pass.", ReminderCategory.GAMING, hour = 19, minute = 0)
            "FF_BOOYAH_DAY" -> addClock("Booyah Day Free Fire 🎯", "Revisa el evento Booyah Day.", ReminderCategory.GAMING, hour = 18, minute = 0)
            "FF_SQUAD_UP" -> addClock("Juntar squad Free Fire 👥", "Llama a tu squad para jugar.", ReminderCategory.GAMING, hour = 21, minute = 30)
            "FF_CLAIM_REWARDS" -> addClock("Reclamar recompensas Free Fire 🎁", "Revisa el buzón y reclama lo pendiente.", ReminderCategory.GAMING, hour = 14, minute = 0)

            // ---- Roblox general ----
            "ROBLOX_DAILY" -> addClock("Recompensa diaria Roblox 🟢", "Inicia sesión y reclama tu daily.", ReminderCategory.GAMING, hour = 12, minute = 0)
            "ROBLOX_EVENT" -> addClock("Evento de Roblox 🎉", "Revisa el evento actual.", ReminderCategory.GAMING, hour = 18, minute = 0, recurrence = RecurrenceType.DAILY)
            "ROBLOX_LIMITED_TIME" -> addIntervalHours("Item limitado Roblox ⏳", "El item sale del shop pronto.", ReminderCategory.GAMING, 3)

            // ---- Short AFK / 30-second timers ----
            "AFK_30S" -> addIntervalSeconds("AFK 30 segundos ⏱️", "¿Sigues ahí? Si no, toca OK.", ReminderCategory.GAMING, 30)
            "AFK_1M" -> addIntervalMinutes("AFK 1 minuto ⏱️", "Chequeo rápido AFK.", ReminderCategory.GAMING, 1)
            "QUICK_BREAK_2M" -> addIntervalMinutes("Pausa rápida 2 min ⚡", "Estírate un segundo.", ReminderCategory.GAMING, 2)

            // ---------- FAR FUTURE / INFINITE ----------
            "CAPSULE_2500" -> saveReminder(
                title = "Cápsula del Futuro Año 2500 🚀",
                description = "Recordatorio guardado para el futuro distante.",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.MEDIUM,
                year = 2500, month = 1, day = 1, hour = 12, minute = 0, second = 0,
                recurrenceType = RecurrenceType.ONCE
            )
            "YEAR_10000" -> saveReminder(
                title = "Mensaje a la Humanidad: Año 10000 🌌",
                description = "Recordatorio ultra-futurista guardado en la base de datos.",
                category = ReminderCategory.FAR_FUTURE,
                priority = ReminderPriority.HIGH,
                year = 10000, month = 1, day = 1, hour = 0, minute = 0, second = 0,
                recurrenceType = RecurrenceType.ONCE
            )
            "ANNUAL_ANNIVERSARY" -> saveReminder(
                title = "Aniversario anual 🎉",
                description = "Recuerda este día cada año (infinito).",
                category = ReminderCategory.PERSONAL,
                priority = ReminderPriority.MEDIUM,
                year = now.get(Calendar.YEAR) + 1,
                month = now.get(Calendar.MONTH) + 1,
                day = now.get(Calendar.DAY_OF_MONTH),
                hour = 12, minute = 0, second = 0,
                recurrenceType = RecurrenceType.YEARLY
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Custom templates (user-saved)
    // ---------------------------------------------------------------------------

    /**
     * Saves the current form state as a custom template. The user can later
     * tap this template in the "Plantillas" tab to recreate the reminder.
     *
     * If [intervalOffsetMinutes] > 0 the template stores a relative offset
     * (great for "fire 30 minutes from now" templates for game events).
     * Otherwise the absolute date/time is stored as-is.
     */
    fun saveCurrentAsCustomTemplate(
        name: String,
        emoji: String,
        title: String,
        description: String,
        category: ReminderCategory,
        priority: ReminderPriority,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        recurrenceType: RecurrenceType,
        recurrenceIntervalHours: Int,
        recurrenceIntervalMinutes: Int,
        intervalOffsetMinutes: Long = 0L,
        recurrenceIntervalValue: Long = 1L,
        recurrenceIntervalUnit: IntervalUnit = IntervalUnit.MINUTE
    ) {
        viewModelScope.launch {
            val template = CustomTemplateEntity(
                name = name.trim().ifEmpty { title.trim() },
                emoji = emoji.ifBlank { "📌" },
                title = title.trim(),
                description = description.trim(),
                category = category.id,
                priority = priority.id,
                intervalOffsetMinutes = intervalOffsetMinutes,
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                second = second,
                recurrenceType = recurrenceType.id,
                recurrenceIntervalHours = recurrenceIntervalHours,
                recurrenceIntervalMinutes = recurrenceIntervalMinutes,
                recurrenceIntervalValue = recurrenceIntervalValue,
                recurrenceIntervalUnit = recurrenceIntervalUnit.id
            )
            repository.saveCustomTemplate(template)
        }
    }

    /**
     * Applies a custom template: builds a real reminder from the stored
     * values. If the template has a non-zero [intervalOffsetMinutes], the
     * reminder fires that many minutes from now — otherwise the absolute
     * stored date/time is used.
     */
    fun applyCustomTemplate(template: CustomTemplateEntity) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            if (template.intervalOffsetMinutes > 0L) {
                now.add(Calendar.MINUTE, template.intervalOffsetMinutes.toInt())
                saveReminder(
                    title = template.title,
                    description = template.description,
                    category = ReminderCategory.fromId(template.category),
                    priority = ReminderPriority.fromId(template.priority),
                    year = now.get(Calendar.YEAR),
                    month = now.get(Calendar.MONTH) + 1,
                    day = now.get(Calendar.DAY_OF_MONTH),
                    hour = now.get(Calendar.HOUR_OF_DAY),
                    minute = now.get(Calendar.MINUTE),
                    second = now.get(Calendar.SECOND),
                    recurrenceType = RecurrenceType.fromId(template.recurrenceType),
                    recurrenceIntervalHours = template.recurrenceIntervalHours,
                    recurrenceIntervalMinutes = template.recurrenceIntervalMinutes,
                    recurrenceIntervalValue = template.recurrenceIntervalValue,
                    recurrenceIntervalUnit = IntervalUnit.fromId(template.recurrenceIntervalUnit)
                )
            } else {
                saveReminder(
                    title = template.title,
                    description = template.description,
                    category = ReminderCategory.fromId(template.category),
                    priority = ReminderPriority.fromId(template.priority),
                    year = template.year,
                    month = template.month,
                    day = template.day,
                    hour = template.hour,
                    minute = template.minute,
                    second = template.second,
                    recurrenceType = RecurrenceType.fromId(template.recurrenceType),
                    recurrenceIntervalHours = template.recurrenceIntervalHours,
                    recurrenceIntervalMinutes = template.recurrenceIntervalMinutes,
                    recurrenceIntervalValue = template.recurrenceIntervalValue,
                    recurrenceIntervalUnit = IntervalUnit.fromId(template.recurrenceIntervalUnit)
                )
            }
        }
    }

    fun deleteCustomTemplate(template: CustomTemplateEntity) {
        viewModelScope.launch {
            repository.deleteCustomTemplate(template)
        }
    }

    fun triggerTestAlarm() {
        ReminderScheduler.triggerTestAlarm(appContext)
    }

    fun dismissActiveAlarm() {
        val alarm = currentAlarm.value
        if (alarm != null && alarm.reminderId > 0 && alarm.reminderId != 99999L) {
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
