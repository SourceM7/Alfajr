package io.github.sourcem7.alfajralarm.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.sourcem7.alfajralarm.ui.theme.AlfajrShapes
import io.github.sourcem7.alfajralarm.ui.theme.AlfajrTypography
import io.github.sourcem7.alfajralarm.ui.theme.DawnDark
import io.github.sourcem7.alfajralarm.ui.theme.DawnLight

/* A restrained pre-dawn palette: ink, blue hour, and the first warm horizon.
 * Full color roles live in ui.theme.Color; shapes and spacing are M3 tokens. */
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
    MaterialTheme(
        colorScheme = colors,
        shapes = AlfajrShapes,
        typography = AlfajrTypography,
        content = content,
    )
}
