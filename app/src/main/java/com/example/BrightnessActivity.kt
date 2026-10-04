package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class BrightnessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrightnessScreen(
                        canWrite = Settings.System.canWrite(this),
                        current = readBrightness(),
                        onRequestPermission = { requestWritePermission() },
                        onApply = { value -> writeBrightness(value) }
                    )
                }
            }
        }
    }

    private fun readBrightness(): Int {
        return runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(38).coerceIn(0, 255)
    }

    private fun writeBrightness(value: Int) {
        val v = value.coerceIn(0, 255)
        if (!Settings.System.canWrite(this)) {
            requestWritePermission()
            return
        }
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
    }

    private fun requestWritePermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }
}

@androidx.compose.runtime.Composable
private fun BrightnessScreen(
    canWrite: Boolean,
    current: Int,
    onRequestPermission: () -> Unit,
    onApply: (Int) -> Unit
) {
    var value by remember(current) { mutableIntStateOf(current) }
    val percent = (value * 100f / 255f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Control de brillo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Escala real del sistema: 0–255")
        Spacer(Modifier.height(24.dp))

        Text("$percent%  •  $value / 255", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = { value = it.roundToInt().coerceIn(0, 255) },
            valueRange = 0f..255f,
            steps = 254,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { value = 38 }) {
                Text("15% → 38")
            }
            Button(onClick = { value = 255 }) {
                Text("100%")
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onApply(value) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aplicar $value / 255")
        }

        Spacer(Modifier.height(12.dp))

        if (!canWrite) {
            Text(
                "Android necesita permiso especial para modificar el brillo del sistema.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conceder permiso de brillo")
            }
        } else {
            Text("Permiso de modificación del sistema: concedido")
        }
    }
}
