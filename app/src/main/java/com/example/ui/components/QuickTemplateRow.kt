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
    QuickTemplateItem("MED_MORNING", "Remedio Mañana", "💊"),
    QuickTemplateItem("LUNCH", "Almuerzo", "🥗"),
    QuickTemplateItem("BREAKFAST", "Desayuno", "🥐"),
    QuickTemplateItem("DINNER", "Cena", "🍲"),
    QuickTemplateItem("MED_NIGHT", "Remedio Noche", "💊"),
    QuickTemplateItem("WATER", "Beber Agua", "💧"),
    QuickTemplateItem("YEAR_2500", "Cápsula 2500", "🚀"),
    QuickTemplateItem("YEAR_10000", "Meta Año 10000", "🌌")
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
