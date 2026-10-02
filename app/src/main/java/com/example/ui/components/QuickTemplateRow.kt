package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickTemplateItem(
    val id: String,
    val label: String,
    val emoji: String,
    val category: QuickTemplateCategory
)

enum class QuickTemplateCategory(val label: String) {
    HEALTH("Salud"),
    DAILY("Día a día"),
    STUDY_WORK("Estudio y trabajo"),
    HOME("Casa"),
    GAMING("Gaming"),
    OTHER("Otros")
}

/* Curated quick library: common useful patterns without catalog bloat. */
val QUICK_TEMPLATES = listOf(
    QuickTemplateItem("MED_MORNING", "Medicamento mañana", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("MED_LUNCH", "Medicamento mediodía", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("MED_NIGHT", "Medicamento noche", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("MED_INTERVAL_4", "Medicamento cada 4 h", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("MED_INTERVAL_8", "Medicamento cada 8 h", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("MED_INTERVAL_12", "Medicamento cada 12 h", "💊", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("WATER", "Agua cada 2 h", "💧", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("WATER_30M", "Agua cada 30 min", "💧", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("GAMING_WATER", "Agua durante gaming", "🎮💧", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("EYE_BREAK", "Descanso de ojos", "👀", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("STRETCH", "Estiramiento", "🧘", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("EXERCISE", "Ejercicio", "🏃", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("SLEEP", "Prepararse para dormir", "🌙", QuickTemplateCategory.HEALTH),
    QuickTemplateItem("WAKE", "Despertar", "☀️", QuickTemplateCategory.HEALTH),

    QuickTemplateItem("BREAKFAST", "Desayuno", "🥐", QuickTemplateCategory.DAILY),
    QuickTemplateItem("LUNCH", "Almuerzo", "🥗", QuickTemplateCategory.DAILY),
    QuickTemplateItem("DINNER", "Cena", "🍲", QuickTemplateCategory.DAILY),
    QuickTemplateItem("SHOWER", "Ducha", "🚿", QuickTemplateCategory.DAILY),
    QuickTemplateItem("BRUSH_TEETH", "Cepillarse", "🪥", QuickTemplateCategory.DAILY),
    QuickTemplateItem("CALL_FAMILY", "Llamar familia", "📞", QuickTemplateCategory.DAILY),
    QuickTemplateItem("APPOINTMENT", "Cita", "📅", QuickTemplateCategory.DAILY),

    QuickTemplateItem("STUDY", "Estudiar", "📚", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("HOMEWORK", "Tarea", "📝", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("READ", "Leer", "📖", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("EXAM_PREP", "Repaso de examen", "📋", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("WORK_START", "Empezar trabajo", "💼", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("WORK_BREAK", "Pausa de trabajo", "☕", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("MEETING", "Reunión", "👥", QuickTemplateCategory.STUDY_WORK),
    QuickTemplateItem("DEADLINE", "Fecha límite", "⏳", QuickTemplateCategory.STUDY_WORK),

    QuickTemplateItem("CLEAN_ROOM", "Ordenar habitación", "🧹", QuickTemplateCategory.HOME),
    QuickTemplateItem("LAUNDRY", "Lavar ropa", "👕", QuickTemplateCategory.HOME),
    QuickTemplateItem("DISHES", "Lavar platos", "🍽️", QuickTemplateCategory.HOME),
    QuickTemplateItem("TRASH", "Sacar basura", "🗑️", QuickTemplateCategory.HOME),
    QuickTemplateItem("SHOPPING", "Compras", "🛒", QuickTemplateCategory.HOME),
    QuickTemplateItem("PET_FOOD", "Alimentar mascota", "🐾", QuickTemplateCategory.HOME),
    QuickTemplateItem("PLANTS", "Regar plantas", "🪴", QuickTemplateCategory.HOME),

    QuickTemplateItem("GAMING_BREAK", "Pausa gaming", "🎮⏸️", QuickTemplateCategory.GAMING),
    QuickTemplateItem("GAMING_EYES", "Descanso ojos gaming", "🎮👀", QuickTemplateCategory.GAMING),
    QuickTemplateItem("GAMING_POSTURE", "Postura gaming", "🪑🎮", QuickTemplateCategory.GAMING),
    QuickTemplateItem("GAME_DAILY_QUEST", "Misiones diarias", "🎯", QuickTemplateCategory.GAMING),
    QuickTemplateItem("GAME_EVENT_LIVE", "Evento del juego", "🎉", QuickTemplateCategory.GAMING),
    QuickTemplateItem("AFK_30S", "Temporizador 30 s", "⏱️", QuickTemplateCategory.GAMING),
    QuickTemplateItem("AFK_1M", "Temporizador 1 min", "⏱️", QuickTemplateCategory.GAMING),
    QuickTemplateItem("QUICK_BREAK_2M", "Pausa 2 min", "⚡", QuickTemplateCategory.GAMING),

    QuickTemplateItem("BACKUP", "Copia de seguridad", "💾", QuickTemplateCategory.OTHER),
    QuickTemplateItem("CHARGE_PHONE", "Cargar teléfono", "🔋", QuickTemplateCategory.OTHER),
    QuickTemplateItem("BILL", "Pagar cuenta", "💳", QuickTemplateCategory.OTHER),
    QuickTemplateItem("BIRTHDAY", "Cumpleaños", "🎂", QuickTemplateCategory.OTHER),
    QuickTemplateItem("TRAVEL", "Preparar viaje", "🧳", QuickTemplateCategory.OTHER),
    QuickTemplateItem("CAPSULE_2500", "Cápsula 2500", "🚀", QuickTemplateCategory.OTHER),
    QuickTemplateItem("YEAR_10000", "Meta año 10000", "🌌", QuickTemplateCategory.OTHER)
)

private val MAIN_QUICK_TEMPLATE_IDS = listOf(
    "MED_MORNING", "WATER", "BREAKFAST", "LUNCH",
    "SLEEP", "STUDY", "WORK_START", "GAMING_WATER"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTemplateRow(
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAll by remember { mutableStateOf(false) }
    val mainTemplates = remember {
        MAIN_QUICK_TEMPLATE_IDS.mapNotNull { id ->
            QUICK_TEMPLATES.firstOrNull { it.id == id }
        }
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        mainTemplates.forEach { item ->
            AssistChip(
                onClick = { onSelectTemplate(item.id) },
                label = {
                    Text(
                        text = "${item.emoji} ${item.label}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                shape = RoundedCornerShape(10.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp),
                modifier = Modifier.testTag("quick_template_${item.id}")
            )
        }

        AssistChip(
            onClick = { showAll = true },
            label = { Text("Ver biblioteca", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("quick_templates_all")
        )
    }

    if (showAll) {
        QuickTemplateLibraryDialog(
            onDismiss = { showAll = false },
            onSelectTemplate = {
                showAll = false
                onSelectTemplate(it)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickTemplateLibraryDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<QuickTemplateCategory?>(null) }

    val filtered = remember(query, selectedCategory) {
        val normalized = query.trim().lowercase()
        QUICK_TEMPLATES.filter { item ->
            (selectedCategory == null || item.category == selectedCategory) &&
                (normalized.isEmpty() ||
                    item.label.lowercase().contains(normalized) ||
                    item.category.label.lowercase().contains(normalized))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Biblioteca de plantillas") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Buscar plantilla") }
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = { selectedCategory = null },
                        label = { Text("Todas") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    QuickTemplateCategory.entries.forEach { category ->
                        AssistChip(
                            onClick = {
                                selectedCategory =
                                    if (selectedCategory == category) null else category
                            },
                            label = { Text(category.label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        count = filtered.size,
                        key = { index -> filtered[index].id }
                    ) { index ->
                        val item = filtered[index]
                        TextButton(
                            onClick = { onSelectTemplate(item.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${item.emoji} ${item.label}",
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
