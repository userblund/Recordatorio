package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ReminderEntity
import com.example.ui.AlarmRingingScreen
import com.example.ui.ReminderDialog
import com.example.ui.ReminderTab
import com.example.ui.ReminderViewModel
import com.example.ui.components.OverviewHeader
import com.example.ui.components.QuickTemplateRow
import com.example.ui.components.ReminderCard
import com.example.ui.theme.RecordatorioTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RecordatorioApp
        val repository = app.repository

        setContent {
            RecordatorioTheme {
                val viewModel: ReminderViewModel = viewModel(
                    factory = ReminderViewModel.provideFactory(repository, applicationContext)
                )

                RecordatorioMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RecordatorioMainScreen(viewModel: ReminderViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val displayedReminders by viewModel.displayedReminders.collectAsStateWithLifecycle()
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsStateWithLifecycle()
    val currentAlarm by viewModel.currentAlarm.collectAsStateWithLifecycle()

    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Notificaciones desactivadas. Actívalas para recibir alertas.")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    reminderToEdit = null
                    showReminderDialog = true
                },
                modifier = Modifier.testTag("fab_new_reminder"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo Recordatorio", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header / Stats Overview
                item(key = "header_overview") {
                    OverviewHeader(
                        stats = todayStats,
                        onTestAlarm = {
                            viewModel.triggerTestAlarm()
                            scope.launch {
                                snackbarHostState.showSnackbar("🔔 Alarma sonora iniciada. Toca APAGAR ALARMA para detenerla.")
                            }
                        }
                    )
                }

                // Quick Templates Row
                item(key = "quick_templates") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Plantillas Rápidas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        QuickTemplateRow(
                            onSelectTemplate = { templateId ->
                                viewModel.createFromQuickTemplate(templateId)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Recordatorio añadido desde plantilla")
                                }
                            }
                        )
                    }
                }

                // Search Bar
                item(key = "search_bar") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = { Text("Buscar en recordatorios...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateSearchQuery("") },
                                    modifier = Modifier.testTag("clear_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Category & Filter Tabs
                item(key = "tab_row") {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_tab_row"),
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        ReminderTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                modifier = Modifier.testTag("tab_${tab.name}"),
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${tab.iconText} ${tab.label}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (tab == ReminderTab.TODAY && todayStats.pendingTodayCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "${todayStats.pendingTodayCount}",
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Reminder list items or empty state
                if (displayedReminders.isEmpty()) {
                    item(key = "empty_state") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("empty_state_view"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "Sin resultados para \"$searchQuery\""
                                    } else {
                                        "No hay recordatorios en esta sección"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Toca el botón + para programar uno nuevo o usa las plantillas rápidas de arriba.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = displayedReminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleComplete = {
                                viewModel.toggleCompleted(reminder)
                                scope.launch {
                                    val msg = if (!reminder.isCompleted) {
                                        "¡Completado! Registrado con éxito."
                                    } else {
                                        "Recordatorio reactivado."
                                    }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onEdit = {
                                reminderToEdit = reminder
                                showReminderDialog = true
                            },
                            onDelete = {
                                reminderToDelete = reminder
                            },
                            onSnooze = { minutes ->
                                viewModel.snooze(reminder, minutes)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Pospuesto por $minutes minutos.")
                                }
                            }
                        )
                    }
                }

                // Bottom spacer for FAB clearance
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Reminder Create/Edit Dialog
    if (showReminderDialog) {
        ReminderDialog(
            initialReminder = reminderToEdit,
            onDismiss = {
                showReminderDialog = false
                reminderToEdit = null
            },
            onSave = { title, description, category, priority, year, month, day, hour, minute, recurrenceType, recurrenceIntervalHours ->
                viewModel.saveReminder(
                    id = reminderToEdit?.id ?: 0L,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    year = year,
                    month = month,
                    day = day,
                    hour = hour,
                    minute = minute,
                    recurrenceType = recurrenceType,
                    recurrenceIntervalHours = recurrenceIntervalHours
                )
                showReminderDialog = false
                reminderToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Recordatorio guardado correctamente.")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    reminderToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Se eliminará \"${toDelete.title}\". Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(toDelete)
                        reminderToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Recordatorio eliminado.")
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { reminderToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Active Alarm Ringing Overlay (Full screen alarm experience)
    if (isAlarmPlaying) {
        val alarm = currentAlarm
        AlarmRingingScreen(
            title = alarm?.title ?: "¡Hora de tu recordatorio!",
            message = alarm?.message ?: "Toca el botón APAGAR ALARMA para detener el sonido.",
            categoryStr = alarm?.category ?: "PERSONAL",
            onDismiss = {
                viewModel.dismissActiveAlarm()
                scope.launch {
                    snackbarHostState.showSnackbar("Alarma apagada.")
                }
            },
            onSnooze = {
                viewModel.snoozeActiveAlarm(10)
                scope.launch {
                    snackbarHostState.showSnackbar("Alarma pospuesta por 10 minutos.")
                }
            }
        )
    }
}
