package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextOverflow

data class QuickTemplateItem(
    val id: String,
    val label: String,
    val emoji: String
)

/**
 * Built-in quick templates. The user explicitly requested the MAXIMUM amount
 * of useful templates, so this list is intentionally large and covers:
 *   - Health & meds
 *   - Hydration & meals
 *   - Habits & fitness
 *   - Sleep
 *   - Personal & self-care
 *   - School / study
 *   - Work
 *   - Tech
 *   - Errands / home
 *   - Finance
 *   - Pets & plants
 *   - Social
 *   - Creative
 *   - Travel
 *   - GAMING (the user's primary use case): generic + Minecraft Bedrock +
 *     Adopt Me (Roblox) + Free Fire + Roblox general + AFK short timers
 *   - Far future / infinite (year 2500 / 10000)
 *
 * The IDs here MUST match the `when` branches in
 * `ReminderViewModel.createFromQuickTemplate`.
 */
val QUICK_TEMPLATES = listOf(
    // Health / Meds
    QuickTemplateItem("MED_MORNING", "Medicamento mañana", "💊"),
    QuickTemplateItem("MED_LUNCH", "Medicamento mediodía", "💊"),
    QuickTemplateItem("MED_NIGHT", "Medicamento noche", "💊"),
    QuickTemplateItem("MED_INTERVAL_4", "Medicamento cada 4h", "💊"),
    QuickTemplateItem("MED_INTERVAL_6", "Medicamento cada 6h", "💊"),
    QuickTemplateItem("MED_INTERVAL_8", "Medicamento cada 8h", "💊"),
    QuickTemplateItem("MED_INTERVAL_12", "Medicamento cada 12h", "💊"),
    QuickTemplateItem("MED_INTERVAL_24", "Medicamento diario", "💊"),

    // Hydration & Meals
    QuickTemplateItem("WATER", "Agua cada 2h", "💧"),
    QuickTemplateItem("WATER_30M", "Agua cada 30m", "💧"),
    QuickTemplateItem("WATER_GAMING", "Agua gaming", "💧"),
    QuickTemplateItem("BREAKFAST", "Desayuno", "🥐"),
    QuickTemplateItem("LUNCH", "Almuerzo", "🥗"),
    QuickTemplateItem("DINNER", "Cena", "🍲"),
    QuickTemplateItem("SNACK", "Snack", "🍎"),
    QuickTemplateItem("COFFEE", "Pausa de café", "☕"),
    QuickTemplateItem("COOK", "Cocinar", "👨‍🍳"),
    QuickTemplateItem("COOK_PREP", "Preparar ingredientes", "🥄"),

    // Habits / Fitness
    QuickTemplateItem("EYE_BREAK", "Descanso de ojos", "👀"),
    QuickTemplateItem("STRETCH", "Estiramiento", "🧘"),
    QuickTemplateItem("WALK", "Caminar", "🚶"),
    QuickTemplateItem("EXERCISE", "Ejercicio", "🏃"),
    QuickTemplateItem("WORKOUT_30", "Micro-entrenamiento", "💪"),
    QuickTemplateItem("POSTURE", "Revisar postura", "🪑"),
    QuickTemplateItem("BREATHING", "Respirar", "🌿"),
    QuickTemplateItem("MEDITATE", "Pausa tranquila", "🧘‍♂️"),
    QuickTemplateItem("SUNLIGHT", "Tomar aire/luz", "🌤️"),

    // Sleep
    QuickTemplateItem("SLEEP", "Prepararse para dormir", "🌙"),
    QuickTemplateItem("WAKE", "Despertar", "☀️"),
    QuickTemplateItem("BEDTIME_PHONE", "Alejar el teléfono", "📵"),

    // Personal
    QuickTemplateItem("SHOWER", "Ducha", "🚿"),
    QuickTemplateItem("BRUSH_TEETH", "Cepillarse", "🪥"),
    QuickTemplateItem("BRUSH_TEETH_NIGHT", "Cepillarse noche", "🪥"),
    QuickTemplateItem("SKINCARE", "Cuidado personal", "🧴"),

    // School / Study
    QuickTemplateItem("STUDY", "Estudiar", "📚"),
    QuickTemplateItem("HOMEWORK", "Tarea", "📝"),
    QuickTemplateItem("READ", "Leer", "📖"),
    QuickTemplateItem("BREAK_STUDY", "Pausa de estudio", "⏸️"),
    QuickTemplateItem("EXAM_PREP", "Repaso de examen", "📋"),

    // Work
    QuickTemplateItem("WORK_START", "Empezar trabajo", "💼"),
    QuickTemplateItem("WORK_BREAK", "Pausa de trabajo", "☕"),
    QuickTemplateItem("MEETING", "Reunión", "👥"),
    QuickTemplateItem("DEADLINE", "Fecha límite", "⏳"),

    // Tech
    QuickTemplateItem("BACKUP", "Copia de seguridad", "💾"),
    QuickTemplateItem("UPDATE", "Buscar actualizaciones", "🔄"),
    QuickTemplateItem("CHARGE_PHONE", "Cargar teléfono", "🔋"),
    QuickTemplateItem("CLEAN_STORAGE", "Limpiar almacenamiento", "🗃️"),

    // Errands / Home
    QuickTemplateItem("CLEAN_ROOM", "Ordenar habitación", "🧹"),
    QuickTemplateItem("LAUNDRY", "Lavar ropa", "👕"),
    QuickTemplateItem("DISHES", "Lavar platos", "🍽️"),
    QuickTemplateItem("TRASH", "Sacar basura", "🗑️"),
    QuickTemplateItem("SHOPPING", "Compras", "🛒"),
    QuickTemplateItem("KEYS", "Revisar llaves", "🔑"),
    QuickTemplateItem("PACK_BAG", "Preparar mochila", "🎒"),

    // Finance
    QuickTemplateItem("BILL", "Pagar cuenta", "💳"),
    QuickTemplateItem("SAVE_MONEY", "Revisar ahorro", "💰"),
    QuickTemplateItem("SUBSCRIPTION", "Revisar suscripciones", "📊"),
    QuickTemplateItem("BUDGET_CHECK", "Revisar presupuesto", "📈"),

    // Pets & Plants
    QuickTemplateItem("PET_FOOD", "Alimentar mascota", "🐾"),
    QuickTemplateItem("PET_WALK", "Pasear mascota", "🦮"),
    QuickTemplateItem("PET_MED", "Medicamento mascota", "💊"),
    QuickTemplateItem("PLANTS", "Regar plantas", "🪴"),

    // Social
    QuickTemplateItem("CALL_FAMILY", "Llamar familia", "📞"),
    QuickTemplateItem("MESSAGE", "Enviar mensaje", "💬"),
    QuickTemplateItem("APPOINTMENT", "Cita", "📅"),
    QuickTemplateItem("BIRTHDAY", "Cumpleaños", "🎂"),

    // Creative
    QuickTemplateItem("CONTENT", "Crear contenido", "🎬"),
    QuickTemplateItem("MUSIC", "Práctica de música", "🎵"),
    QuickTemplateItem("CREATIVE", "Proyecto creativo", "🎨"),
    QuickTemplateItem("STREAM_START", "Empezar stream", "📺"),
    QuickTemplateItem("STREAM_BREAK", "Pausa del stream", "🎥"),

    // Travel
    QuickTemplateItem("TRAVEL", "Preparar viaje", "🧳"),

    // ----- GAMING (the user's primary use case) -----
    QuickTemplateItem("GAMING_WATER", "Agua gaming", "🎮💧"),
    QuickTemplateItem("GAMING_BREAK", "Pausa gaming", "🎮⏸️"),
    QuickTemplateItem("GAMING_EYES", "Descanso ojos gaming", "🎮👀"),
    QuickTemplateItem("SAVE_GAME", "Guardar partida", "🎮💾"),
    QuickTemplateItem("SERVER_CHECK", "Revisar servidor", "🖥️"),
    QuickTemplateItem("AFK_WARNING", "Revisar AFK 15m", "⚠️"),
    QuickTemplateItem("GAMING_POSTURE", "Postura gaming", "🪑🎮"),
    QuickTemplateItem("GAME_DAILY_QUEST", "Misiones diarias", "🎯"),
    QuickTemplateItem("GAME_WEEKLY_RESET", "Reset semanal", "🗓️"),
    QuickTemplateItem("GAME_EVENT_LIVE", "Evento activo", "🎉"),
    QuickTemplateItem("GAME_LOGIN_BONUS", "Bonus de login", "🎁"),
    QuickTemplateItem("GAME_BATTLE_PASS", "Battle pass", "🎟️"),
    QuickTemplateItem("GAME_PVP_RANKED", "Ranked del juego", "🏆"),
    QuickTemplateItem("GAME_SCREENSHOT", "Captura de pantalla", "📸"),
    QuickTemplateItem("GAME_FRIEND_LIST", "Lista de amigos", "👥"),

    // Minecraft Bedrock (single player focus)
    QuickTemplateItem("MC_HUNGER", "Revisar hambre MC", "🍗"),
    QuickTemplateItem("MC_DAY_NIGHT", "Ciclo día/noche MC", "🌗"),
    QuickTemplateItem("MC_SLEEP_SKIP_NIGHT", "Dormir saltar noche", "🛏️"),
    QuickTemplateItem("MC_SAVE_BEDROCK", "Guardar mundo Bedrock", "💾⛏️"),
    QuickTemplateItem("MC_BUILD_CHECKPOINT", "Checkpoint construcción", "🏗️"),
    QuickTemplateItem("MC_END_PREP", "Prep viaje al End", "🐉"),
    QuickTemplateItem("MC_NETHER_PREP", "Prep viaje al Nether", "🔥"),
    QuickTemplateItem("MC_INVENTORY_CHECK", "Ordenar inventario", "🎒⛏️"),
    QuickTemplateItem("MC_VILLAGER_TRADE", "Comerciar con aldeanos", "🤝"),

    // Adopt Me (Roblox)
    QuickTemplateItem("ADOPT_FEED_PETS", "Alimentar mascotas Adopt Me", "🐶"),
    QuickTemplateItem("ADOPT_PET_SLEEP", "Dormir mascotas Adopt Me", "😴"),
    QuickTemplateItem("ADOPT_PET_FUN", "Jugar mascotas Adopt Me", "🎾"),
    QuickTemplateItem("ADOPT_PET_DRINK", "Bebida mascotas Adopt Me", "💧"),
    QuickTemplateItem("ADOPT_PET_SCHOOL", "Mascotas a la escuela", "🏫"),
    QuickTemplateItem("ADOPT_DAILY", "Recompensa diaria Adopt Me", "🎁"),
    QuickTemplateItem("ADOPT_TRADE_POST", "Trade Post Adopt Me", "🔄"),
    QuickTemplateItem("ADOPT_AGE_UP", "Age up mascotas", "⏰"),
    QuickTemplateItem("ADOPT_NEON_PREP", "Preparar mascota Neon", "✨"),

    // Free Fire
    QuickTemplateItem("FF_LOGIN_DAILY", "Login diario FF", "🔥"),
    QuickTemplateItem("FF_DAILY_MISSIONS", "Misiones FF", "📋"),
    QuickTemplateItem("FF_RANKED_RESET", "Ranked reset FF", "🏆"),
    QuickTemplateItem("FF_DIAMOND_SPIN", "Diamond Spin FF", "💎"),
    QuickTemplateItem("FF_BATTLE_PASS", "Battle Pass FF", "🎟️"),
    QuickTemplateItem("FF_BOOYAH_DAY", "Booyah Day FF", "🎯"),
    QuickTemplateItem("FF_SQUAD_UP", "Squad FF", "👥"),
    QuickTemplateItem("FF_CLAIM_REWARDS", "Reclamar recompensas FF", "🎁"),

    // Roblox general
    QuickTemplateItem("ROBLOX_DAILY", "Daily Roblox", "🟢"),
    QuickTemplateItem("ROBLOX_EVENT", "Evento Roblox", "🎉"),
    QuickTemplateItem("ROBLOX_LIMITED_TIME", "Item limitado Roblox", "⏳"),

    // Short AFK / game-event timers (sub-minute / few-minute)
    QuickTemplateItem("AFK_30S", "AFK 30 segundos", "⏱️"),
    QuickTemplateItem("AFK_1M", "AFK 1 minuto", "⏱️"),
    QuickTemplateItem("QUICK_BREAK_2M", "Pausa 2 min", "⚡"),

    // Far Future / Infinite
    QuickTemplateItem("CAPSULE_2500", "Cápsula 2500", "🚀"),
    QuickTemplateItem("YEAR_10000", "Meta año 10000", "🌌"),
    QuickTemplateItem("ANNUAL_ANNIVERSARY", "Aniversario anual", "🎉")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTemplateRow(
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAll by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QUICK_TEMPLATES.take(8).forEach { item ->
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
            label = { Text("Ver todas", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("quick_templates_all")
        )
    }

    if (showAll) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            title = { Text("Todas las plantillas") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(QUICK_TEMPLATES.size) { index ->
                        val item = QUICK_TEMPLATES[index]
                        TextButton(
                            onClick = {
                                showAll = false
                                onSelectTemplate(item.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_template_dialog_${item.id}")
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
            },
            confirmButton = {
                TextButton(onClick = { showAll = false }) { Text("Cerrar") }
            }
        )
    }
}