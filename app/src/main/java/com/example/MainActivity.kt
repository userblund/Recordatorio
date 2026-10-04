package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ReminderEntity
import com.example.data.CustomTemplateEntity
import com.example.data.IntervalUnit
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
            val themePreferences = remember {
                getSharedPreferences("recordatorio_ui", MODE_PRIVATE)
            }
            var darkTheme by remember {
                mutableStateOf(themePreferences.getBoolean("dark_theme", false))
            }

            RecordatorioTheme(darkTheme = darkTheme, dynamicColor = false) {
                val viewModel: ReminderViewModel = viewModel(
                    factory = ReminderViewModel.provideFactory(repository, applicationContext)
                )

                RecordatorioMainScreen(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        val next = !darkTheme
                        darkTheme = next
                        themePreferences.edit().putBoolean("dark_theme", next).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun RecordatorioMainScreen(
    viewModel: ReminderViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val displayedReminders by viewModel.displayedReminders.collectAsStateWithLifecycle()
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsStateWithLifecycle()
    val currentAlarm by viewModel.currentAlarm.collectAsStateWithLifecycle()
    val customTemplates by viewModel.customTemplates.collectAsStateWithLifecycle()

    var exactAlarmAllowed by remember { mutableStateOf(true) }

    fun refreshExactAlarmPermission() {
        exactAlarmAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<ReminderEntity?>(null) }
    var customTemplateToDelete by remember { mutableStateOf<CustomTemplateEntity?>(null) }
    var showBrightnessPanel by remember { mutableStateOf(false) }
    var brightnessValue by remember { mutableStateOf(readSystemBrightness(context)) }
    var brightnessPresets by remember { mutableStateOf(loadBrightnessPresets(context)) }
    var showBrightnessPresetDialog by remember { mutableStateOf(false) }
    var brightnessPresetName by remember { mutableStateOf("") }

    fun applyBrightness(value: Int) {
        val v = value.coerceIn(0, 255)
        if (!Settings.System.canWrite(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
        } else {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
            brightnessValue = v
            scope.launch { snackbarHostState.showSnackbar("Brillo aplicado: $v / 255") }
        }
    }

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
        refreshExactAlarmPermission()

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshExactAlarmPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onTestAlarm = {
                            viewModel.triggerTestAlarm()
                            scope.launch {
                                snackbarHostState.showSnackbar("🔔 Alarma sonora iniciada. Toca APAGAR ALARMA para detenerla.")
                            }
                        }
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmAllowed) {
                    item(key = "exact_alarm_permission") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Alarmas exactas desactivadas",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Android puede retrasar tus recordatorios si este permiso está desactivado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(
                                                Intent(
                                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                    Uri.parse("package:" + context.packageName)
                                                )
                                            )
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                                        }
                                    }
                                ) {
                                    Text("Activar alarmas exactas")
                                }
                            }
                        }
                    }
                }

                // Quick Templates Row
                item(key = "brightness_control") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Brillo de pantalla", fontWeight = FontWeight.Bold)
                            Text("Valor actual: $brightnessValue / 255  •  ${(brightnessValue * 100f / 255f).roundToInt()}%")
                            Slider(
                                value = brightnessValue.toFloat(),
                                onValueChange = { brightnessValue = it.roundToInt().coerceIn(0, 255) },
                                valueRange = 0f..255f,
                                steps = 254
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { brightnessValue = 38 }) { Text("15% → 38") }
                                TextButton(onClick = { applyBrightness(brightnessValue) }) { Text("Aplicar") }
                                TextButton(onClick = { showBrightnessPresetDialog = true }) { Text("⭐ Guardar preset") }
                            }
                            if (brightnessPresets.isNotEmpty()) {
                                Text("Presets de brillo", fontWeight = FontWeight.Bold)
                                brightnessPresets.forEach { preset ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(onClick = { brightnessValue = preset.second; applyBrightness(preset.second) }) { Text("⭐ " + preset.first + ": " + preset.second + "/255") }
                                        TextButton(onClick = { brightnessPresets = brightnessPresets.filterNot { it.first == preset.first }; saveBrightnessPresets(context, brightnessPresets) }) { Text("Borrar") }
                                    }
                                }
                            }
                        }
                    }
                }

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

                // Custom Templates (user-saved) — appear only when user has saved at least one
                if (customTemplates.isNotEmpty()) {
                    item(key = "custom_templates") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📌 Mis Plantillas Custom",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${customTemplates.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                customTemplates.forEach { template ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_template_${template.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${template.emoji} ${template.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = template.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (template.intervalOffsetMinutes > 0L) {
                                                    Text(
                                                        text = "⏱️ dentro de ${template.intervalOffsetMinutes} min",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.applyCustomTemplate(template)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Recordatorio creado desde tu plantilla: ${template.name}")
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("apply_custom_${template.id}")
                                                ) { Text("+ Añadir") }
                                                IconButton(
                                                    onClick = { customTemplateToDelete = template },
                                                    modifier = Modifier.testTag("delete_custom_${template.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Borrar plantilla",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
            onSave = { title, description, category, priority, year, month, day, hour, minute, second, recurrenceType, recurrenceIntervalHours, recurrenceIntervalMinutes, recurrenceIntervalValue, recurrenceIntervalUnit ->
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
                    second = second,
                    recurrenceType = recurrenceType,
                    recurrenceIntervalHours = recurrenceIntervalHours,
                    recurrenceIntervalMinutes = recurrenceIntervalMinutes,
                    recurrenceIntervalValue = recurrenceIntervalValue,
                    recurrenceIntervalUnit = recurrenceIntervalUnit
                )
                showReminderDialog = false
                reminderToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Recordatorio guardado correctamente.")
                }
            },
            onSaveAsTemplate = { title, description, category, priority, year, month, day, hour, minute, second, recurrenceType, recurrenceIntervalHours, recurrenceIntervalMinutes, intervalOffsetMinutes, recurrenceIntervalValue, recurrenceIntervalUnit ->
                viewModel.saveCurrentAsCustomTemplate(
                    name = title,
                    emoji = "📌",
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    year = year,
                    month = month,
                    day = day,
                    hour = hour,
                    minute = minute,
                    second = second,
                    recurrenceType = recurrenceType,
                    recurrenceIntervalHours = recurrenceIntervalHours,
                    recurrenceIntervalMinutes = recurrenceIntervalMinutes,
                    intervalOffsetMinutes = intervalOffsetMinutes,
                    recurrenceIntervalValue = recurrenceIntervalValue,
                    recurrenceIntervalUnit = recurrenceIntervalUnit
                )
                scope.launch {
                    snackbarHostState.showSnackbar("Plantilla guardada arriba en 'Mis Plantillas Custom'.")
                }
            }
        )
    }

    if (showBrightnessPresetDialog) {
        AlertDialog(
            onDismissRequest = { showBrightnessPresetDialog = false },
            title = { Text("Crear preset de brillo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brightnessPresetName,
                        onValueChange = { brightnessPresetName = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Se guardará " + brightnessValue + " / 255 (" + (brightnessValue * 100f / 255f).roundToInt() + "%).")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = brightnessPresetName.trim()
                    if (name.isNotEmpty()) {
                        brightnessPresets = brightnessPresets.filterNot { it.first == name } + (name to brightnessValue)
                        saveBrightnessPresets(context, brightnessPresets)
                        brightnessPresetName = ""
                        showBrightnessPresetDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showBrightnessPresetDialog = false }) { Text("Cancelar") } }
        )
    }

    // Delete Confirmation Dialog (reminder)
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

    // Delete Confirmation Dialog (custom template)
    customTemplateToDelete?.let { toDeleteTemplate ->
        AlertDialog(
            onDismissRequest = { customTemplateToDelete = null },
            title = { Text("¿Borrar plantilla custom?") },
            text = { Text("Se eliminará la plantilla \"${toDeleteTemplate.name}\". No afecta a los recordatorios ya creados a partir de ella.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomTemplate(toDeleteTemplate)
                        customTemplateToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Plantilla custom eliminada.")
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_template_button")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { customTemplateToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_template_button")
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


private fun readSystemBrightness(context: android.content.Context): Int = runCatching {
    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
}.getOrDefault(38).coerceIn(0, 255)

private fun loadBrightnessPresets(context: android.content.Context): List<Pair<String, Int>> =
    context.getSharedPreferences("brightness_presets", android.content.Context.MODE_PRIVATE)
        .getStringSet("items", emptySet())
        ?.mapNotNull {
            val p = it.split("|", limit = 2)
            if (p.size == 2) p[0] to p[1].toIntOrNull()?.coerceIn(0, 255) else null
        } ?: emptyList()

private fun saveBrightnessPresets(context: android.content.Context, items: List<Pair<String, Int>>) {
    context.getSharedPreferences("brightness_presets", android.content.Context.MODE_PRIVATE)
        .edit().putStringSet("items", items.map { it.first + "|" + it.second }.toSet()).apply()
}
