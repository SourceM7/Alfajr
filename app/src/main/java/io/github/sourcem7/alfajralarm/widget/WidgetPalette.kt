package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.github.sourcem7.alfajralarm.R

/**
 * One colour that may differ between the light and dark system themes. Only
 * the daytime and resting skies follow the theme; evening, night, and first
 * light are dark whatever the theme, because that is what the sky is.
 */
internal data class Tone(val day: Color, val night: Color = day) {
    val provider: ColorProvider get() = ColorProvider(day = day, night = night)

    fun resolve(context: Context): Color {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (mode == Configuration.UI_MODE_NIGHT_YES) night else day
    }
}

internal data class SkyStyle(
    @param:DrawableRes val background: Int,
    val text: Tone,
    val secondary: Tone,
    val accent: Tone,
    val track: Tone,
    val stars: Boolean,
)

/*
 * Drawn from the app's Dawn palette (ui/theme/Color.kt): the greens of the
 * primary roles and the teal of the tertiary roles, set against navy for the
 * night. Text tones are chosen per sky for contrast, not per theme role.
 */
private val NightText = Tone(Color(0xFFE4EAE3))
private val NightSecondary = Tone(Color(0xFF9FB2A6))

internal fun skyStyle(phase: SkyPhase, lockScreen: Boolean = false): SkyStyle = when {
    phase == SkyPhase.FIRST_LIGHT -> SkyStyle(
        background = R.drawable.widget_sky_first_light,
        text = Tone(Color.White),
        secondary = Tone(Color(0xFFD2F0E0)),
        accent = Tone(Color(0xFFA6F3BE)),
        track = Tone(Color(0x33FFFFFF)),
        stars = false,
    )
    lockScreen -> SkyStyle(
        background = R.drawable.widget_sky_lock,
        text = Tone(Color(0xFFCFD6CE)),
        secondary = Tone(Color(0xFF7F8A82)),
        accent = Tone(Color(0xFF8BD6A3)),
        track = Tone(Color(0x22FFFFFF)),
        stars = phase == SkyPhase.NIGHT,
    )
    phase == SkyPhase.NIGHT -> SkyStyle(
        background = R.drawable.widget_sky_night,
        text = NightText,
        secondary = NightSecondary,
        accent = Tone(Color(0xFF8BD6A3)),
        track = Tone(Color(0x26FFFFFF)),
        stars = true,
    )
    phase == SkyPhase.EVENING -> SkyStyle(
        background = R.drawable.widget_sky_evening,
        text = Tone(Color(0xFFEEF2EB)),
        secondary = Tone(Color(0xFFB9CDD0)),
        accent = Tone(Color(0xFFA2CEDD)),
        track = Tone(Color(0x26FFFFFF)),
        stars = true,
    )
    phase == SkyPhase.DAY -> SkyStyle(
        background = R.drawable.widget_sky_day,
        text = Tone(day = Color(0xFF0D1F13), night = Color(0xFFDFE4DD)),
        secondary = Tone(day = Color(0xFF3F5245), night = Color(0xFFA9B8AB)),
        accent = Tone(day = Color(0xFF15653C), night = Color(0xFF8BD6A3)),
        track = Tone(day = Color(0x2615653C), night = Color(0x268BD6A3)),
        stars = false,
    )
    else -> SkyStyle(
        background = R.drawable.widget_sky_resting,
        text = Tone(day = Color(0xFF414942), night = Color(0xFFC1C9BF)),
        secondary = Tone(day = Color(0xFF6F786F), night = Color(0xFF8B938A)),
        accent = Tone(Color(0xFF8B938A)),
        track = Tone(day = Color(0x22414942), night = Color(0x22C1C9BF)),
        stars = false,
    )
}
