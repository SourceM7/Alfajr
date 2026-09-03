package io.github.sourcem7.alfajralarm.ui

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

/* A restrained pre-dawn palette: ink, blue hour, and the first warm horizon. */
private val DawnDark = darkColorScheme(
    primary = Color(0xFFFFC68A),
    onPrimary = Color(0xFF402000),
    primaryContainer = Color(0xFF613D15),
    onPrimaryContainer = Color(0xFFFFDDB9),
    secondary = Color(0xFFAEC8E8),
    background = Color(0xFF10151D),
    surface = Color(0xFF171E28),
    surfaceVariant = Color(0xFF283340),
    onBackground = Color(0xFFE1E8F2),
    onSurface = Color(0xFFE1E8F2),
    error = Color(0xFFFFB4AB),
)

private val DawnLight = lightColorScheme(
    primary = Color(0xFF825300),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB9),
    onPrimaryContainer = Color(0xFF291800),
    secondary = Color(0xFF365F87),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF),
    surfaceVariant = Color(0xFFDFE7F2),
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
)

@Composable
fun AlfajrTheme(dynamicColor: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        }
        dark -> DawnDark
        else -> DawnLight
    }
    MaterialTheme(colorScheme = colors, content = content)
}
