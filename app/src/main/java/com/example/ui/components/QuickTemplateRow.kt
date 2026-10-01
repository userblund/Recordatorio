package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class QuickTemplateItem(
    val id: String,
    val label: String,
    val emoji: String
)

val QUICK_TEMPLATES = listOf(
    QuickTemplateItem("WATER", "Agua", "💧"),
    QuickTemplateItem("BREAKFAST", "Desayuno", "🥐"),
    QuickTemplateItem("LUNCH", "Almuerzo", "🥗"),
    QuickTemplateItem("DINNER", "Cena", "🍲"),
    QuickTemplateItem("MED_MORNING", "Medicamento mañana", "💊"),
    QuickTemplateItem("MED_NIGHT", "Medicamento noche", "💊"),
    QuickTemplateItem("EYE_BREAK", "Descanso de ojos", "👀"),
    QuickTemplateItem("STRETCH", "Estiramiento", "🧘"),
    QuickTemplateItem("WALK", "Caminar", "🚶"),
    QuickTemplateItem("EXERCISE", "Ejercicio", "🏃"),
    QuickTemplateItem("SLEEP", "Prepararse para dormir", "🌙"),
    QuickTemplateItem("WAKE", "Despertar", "☀️"),
    QuickTemplateItem("SHOWER", "Ducha", "🚿"),
    QuickTemplateItem("BRUSH_TEETH", "Cepillarse", "🪥"),
    QuickTemplateItem("SKINCARE", "Cuidado personal", "🧴"),
    QuickTemplateItem("STUDY", "Estudiar", "📚"),
    QuickTemplateItem("HOMEWORK", "Tarea", "📝"),
    QuickTemplateItem("READ", "Leer", "📖"),
    QuickTemplateItem("BREAK_STUDY", "Pausa de estudio", "⏸️"),
    QuickTemplateItem("WORK_START", "Empezar trabajo", "💼"),
    QuickTemplateItem("WORK_BREAK", "Pausa de trabajo", "☕"),
    QuickTemplateItem("MEETING", "Reunión", "👥"),
    QuickTemplateItem("DEADLINE", "Fecha límite", "⏳"),
    QuickTemplateItem("BACKUP", "Copia de seguridad", "💾"),
    QuickTemplateItem("UPDATE", "Buscar actualizaciones", "🔄"),
    QuickTemplateItem("CHARGE_PHONE", "Cargar teléfono", "🔋"),
    QuickTemplateItem("CLEAN_ROOM", "Ordenar habitación", "🧹"),
    QuickTemplateItem("LAUNDRY", "Lavar ropa", "👕"),
    QuickTemplateItem("DISHES", "Lavar platos", "🍽️"),
    QuickTemplateItem("TRASH", "Sacar basura", "🗑️"),
    QuickTemplateItem("SHOPPING", "Compras", "🛒"),
    QuickTemplateItem("BILL", "Pagar cuenta", "💳"),
    QuickTemplateItem("SAVE_MONEY", "Revisar ahorro", "💰"),
    QuickTemplateItem("COOK", "Cocinar", "👨‍🍳"),
    QuickTemplateItem("PET", "Cuidar mascota", "🐾"),
    QuickTemplateItem("PLANTS", "Regar plantas", "🪴"),
    QuickTemplateItem("CALL_FAMILY", "Llamar familia", "📞"),
    QuickTemplateItem("MESSAGE", "Enviar mensaje", "💬"),
    QuickTemplateItem("APPOINTMENT", "Cita", "📅"),
    QuickTemplateItem("TRAVEL", "Preparar viaje", "🧳"),
    QuickTemplateItem("KEYS", "Revisar llaves", "🔑"),
    QuickTemplateItem("GAMING_WATER", "Agua mientras juego", "🎮💧"),
    QuickTemplateItem("GAMING_BREAK", "Pausa de videojuego", "🎮⏸️"),
    QuickTemplateItem("GAMING_EYES", "Descanso de ojos gaming", "🎮👀"),
    QuickTemplateItem("SAVE_GAME", "Guardar partida", "🎮💾"),
    QuickTemplateItem("SERVER_CHECK", "Revisar servidor", "🖥️"),
    QuickTemplateItem("STREAM_START", "Empezar stream", "📺"),
    QuickTemplateItem("STREAM_BREAK", "Pausa del stream", "🎥"),
    QuickTemplateItem("CONTENT", "Crear contenido", "🎬"),
    QuickTemplateItem("MUSIC", "Práctica de música", "🎵"),
    QuickTemplateItem("CREATIVE", "Proyecto creativo", "🎨"),
    QuickTemplateItem("MEDITATE", "Pausa tranquila", "🧘‍♂️"),
    QuickTemplateItem("BREATHING", "Respirar y descansar", "🌿"),
    QuickTemplateItem("POSTURE", "Revisar postura", "🪑"),
    QuickTemplateItem("SUNLIGHT", "Tomar aire/luz", "🌤️"),
    QuickTemplateItem("CAPSULE_2500", "Cápsula 2500", "🚀"),
    QuickTemplateItem("YEAR_10000", "Meta año 10000", "🌌")
)

@Composable
fun QuickTemplateRow(
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QUICK_TEMPLATES.forEach { item ->
            AssistChip(
                onClick = { onSelectTemplate(item.id) },
                label = { Text("${item.emoji} ${item.label}") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp),
                modifier = Modifier.testTag("quick_template_${item.id}")
            )
        }
    }
}
