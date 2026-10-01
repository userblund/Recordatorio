package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF002B75),
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF003732),
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = AmberTertiaryDark,
    onTertiary = Color(0xFF451E00),
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = SlateBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = SlateBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateOutlineLight
)

@Composable
fun RecordatorioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
