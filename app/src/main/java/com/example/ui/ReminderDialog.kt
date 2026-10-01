package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RecurrenceType
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderPriority
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    initialReminder: ReminderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
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
        recurrenceIntervalHours: Int
    ) -> Unit
) {
    val cal = remember {
        Calendar.getInstance().apply {
            if (initialReminder != null) {
                timeInMillis = initialReminder.triggerTimeMillis
            }
        }
    }

    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialReminder != null) ReminderCategory.fromId(initialReminder.category)
            else ReminderCategory.MEDICATION
        )
    }
    var selectedPriority by remember {
        mutableStateOf(
            if (initialReminder != null) ReminderPriority.fromId(initialReminder.priority)
            else ReminderPriority.NORMAL
        )
    }

    var year by remember {
        mutableIntStateOf(initialReminder?.year ?: cal.get(Calendar.YEAR))
    }
    var month by remember {
        mutableIntStateOf(initialReminder?.month ?: (cal.get(Calendar.MONTH) + 1))
    }
    var day by remember {
        mutableIntStateOf(initialReminder?.day ?: cal.get(Calendar.DAY_OF_MONTH))
    }
    var hour by remember {
        mutableIntStateOf(initialReminder?.hour ?: cal.get(Calendar.HOUR_OF_DAY))
    }
    var minute by remember {
        mutableIntStateOf(initialReminder?.minute ?: (cal.get(Calendar.MINUTE) / 5 * 5))
    }

    var selectedRecurrence by remember {
        mutableStateOf(
            if (initialReminder != null) RecurrenceType.fromId(initialReminder.recurrenceType)
            else RecurrenceType.DAILY
        )
    }
    var intervalHours by remember {
        mutableIntStateOf(initialReminder?.recurrenceIntervalHours ?: 8)
    }

    var titleError by remember { mutableStateOf(false) }

    val months = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    val previewTriggerMillis by remember {
        derivedStateOf {
            DateUtils.computeTriggerMillis(year, month, day, hour, minute)
        }
    }

    val previewCountdown by remember {
        derivedStateOf {
            DateUtils.formatRelativeCountdown(previewTriggerMillis)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("reminder_form_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (initialReminder == null) "Nuevo Recordatorio" else "Editar Recordatorio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configuración infinita y personalizada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Título del recordatorio *") },
                        placeholder = { Text("Ej: Tomar antibiótico, Almuerzo, Meta año 2500...") },
                        isError = titleError,
                        supportingText = {
                            if (titleError) Text("El título no puede estar vacío")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description / Instructions
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción o instrucciones") },
                        placeholder = { Text("Ej: 500mg con abundante agua tras comer...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_desc_input"),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ReminderCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${cat.iconName} ${cat.displayName}") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                        selectedLabelColor = cat.color
                                    ),
                                    modifier = Modifier.testTag("chip_cat_${cat.id}")
                                )
                            }
                        }
                    }

                    // Priority Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Prioridad",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReminderPriority.entries.forEach { prio ->
                                FilterChip(
                                    selected = selectedPriority == prio,
                                    onClick = { selectedPriority = prio },
                                    label = { Text(prio.displayName) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("chip_prio_${prio.id}")
                                )
                            }
                        }
                    }

                    // TIME PICKER SECTION
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Hora: ${DateUtils.formatTime(hour, minute)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Stepper controls for Hour and Minute
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { hour = if (hour == 0) 23 else hour - 1 },
                                        modifier = Modifier.testTag("hour_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menos hora")
                                    }
                                    Text(
                                        text = String.format("%02d h", hour),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { hour = (hour + 1) % 24 },
                                        modifier = Modifier.testTag("hour_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más hora")
                                    }
                                }

                                Text(text = ":", style = MaterialTheme.typography.titleLarge)

                                // Minute
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { minute = if (minute < 5) 55 else minute - 5 },
                                        modifier = Modifier.testTag("minute_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menos min")
                                    }
                                    Text(
                                        text = String.format("%02d m", minute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { minute = (minute + 5) % 60 },
                                        modifier = Modifier.testTag("minute_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más min")
                                    }
                                }
                            }

                            // Quick time presets for meals & meds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "08:00 (Mañana)" to (8 to 0),
                                    "13:30 (Almuerzo)" to (13 to 30),
                                    "17:00 (Tarde)" to (17 to 0),
                                    "20:30 (Cena)" to (20 to 30),
                                    "22:00 (Noche)" to (22 to 0)
                                ).forEach { (label, time) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable {
                                                hour = time.first
                                                minute = time.second
                                            }
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // INFINITE DATE & YEAR SELECTOR (USER SPECIAL REQUIREMENT)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Fecha: ${DateUtils.formatDate(year, month, day)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AllInclusive,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Infinito",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Direct Year Editing + Steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = year.toString(),
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }
                                        if (filtered.isNotBlank()) {
                                            val parsed = filtered.toIntOrNull()
                                            if (parsed != null && parsed in 1..999999) {
                                                year = parsed
                                            }
                                        }
                                    },
                                    label = { Text("Año (ej. 2026, 2500, 10000)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("year_direct_input"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Fast jump buttons
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = { year += 1 },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("+1a", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { year += 10 },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("+10a", fontSize = 11.sp)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = { year = 2500 },
                                            modifier = Modifier.height(34.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF8B5CF6)
                                            )
                                        ) {
                                            Text("2500", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { year = 10000 },
                                            modifier = Modifier.height(34.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFEC4899)
                                            )
                                        ) {
                                            Text("10000", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Month Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                months.forEachIndexed { index, mName ->
                                    val mNum = index + 1
                                    val isSelected = month == mNum
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable { month = mNum }
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = mName,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Day Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Día del mes: $day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { day = if (day <= 1) 31 else day - 1 },
                                        modifier = Modifier.testTag("day_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menos día")
                                    }
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { day = if (day >= 31) 1 else day + 1 },
                                        modifier = Modifier.testTag("day_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más día")
                                    }
                                }
                            }

                            // Quick Date Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val nowCal = Calendar.getInstance()
                                val curY = nowCal.get(Calendar.YEAR)
                                val curM = nowCal.get(Calendar.MONTH) + 1
                                val curD = nowCal.get(Calendar.DAY_OF_MONTH)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clickable {
                                            year = curY
                                            month = curM
                                            day = curD
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Text("Hoy", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clickable {
                                            val tCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                                            year = tCal.get(Calendar.YEAR)
                                            month = tCal.get(Calendar.MONTH) + 1
                                            day = tCal.get(Calendar.DAY_OF_MONTH)
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Text("Mañana", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clickable {
                                            val tCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
                                            year = tCal.get(Calendar.YEAR)
                                            month = tCal.get(Calendar.MONTH) + 1
                                            day = tCal.get(Calendar.DAY_OF_MONTH)
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Text("En 1 semana", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // RECURRENCE SECTION (INFINITE REPETITION)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Frecuencia / Repetición Infinita",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RecurrenceType.entries.forEach { rec ->
                                FilterChip(
                                    selected = selectedRecurrence == rec,
                                    onClick = { selectedRecurrence = rec },
                                    label = { Text(rec.displayName) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("chip_rec_${rec.id}")
                                )
                            }
                        }

                        // If INTERVAL_HOURS chosen (for meds)
                        AnimatedVisibility(visible = selectedRecurrence == RecurrenceType.INTERVAL_HOURS) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tomar cada:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                listOf(4, 6, 8, 12).forEach { hrs ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (intervalHours == hrs) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { intervalHours = hrs }
                                    ) {
                                        Text(
                                            text = "$hrs horas",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (intervalHours == hrs) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // LIVE PREVIEW BOX
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Primer aviso: ${DateUtils.formatDate(year, month, day)} a las ${DateUtils.formatTime(hour, minute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$previewCountdown • ${selectedRecurrence.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                onSave(
                                    title,
                                    description,
                                    selectedCategory,
                                    selectedPriority,
                                    year,
                                    month,
                                    day,
                                    hour,
                                    minute,
                                    selectedRecurrence,
                                    intervalHours
                                )
                            }
                        },
                        modifier = Modifier.testTag("dialog_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (initialReminder == null) "Crear Recordatorio" else "Guardar Cambios",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
