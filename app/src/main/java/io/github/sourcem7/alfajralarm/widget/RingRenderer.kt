package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import io.github.sourcem7.alfajralarm.R
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The ring widget's whole face as one round bitmap: a disc of the current sky,
 * the countdown ring around it, a faint mark at each hour of the window, and a
 * glowing head at the present. Widgets cannot draw arcs, Glance's circular
 * indicator has no determinate form, and launchers force their own corner
 * radius on a widget background, so a true circle has to be painted.
 */
internal fun renderRing(context: Context, sizePx: Int, progress: Float, style: SkyStyle): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val track = style.track.resolve(context).toArgb()
    val fill = style.accent.resolve(context).toArgb()

    val disc = Path().apply { addCircle(center, center, center, Path.Direction.CW) }
    canvas.withClip(disc) {
        ContextCompat.getDrawable(context, style.background)?.apply {
            setBounds(0, 0, sizePx, sizePx)
            draw(this@withClip)
        }
        if (style.stars) {
            // The star field is wider than tall; crop it to the disc rather than squash it.
            ContextCompat.getDrawable(context, R.drawable.widget_stars)?.apply {
                val width = (sizePx * intrinsicWidth.toFloat() / intrinsicHeight).roundToInt()
                val left = (sizePx - width) / 2
                setBounds(left, 0, left + width, sizePx)
                draw(this@withClip)
            }
        }
    }

    val stroke = sizePx * 0.04f
    val inset = stroke * 2.4f
    val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)
    val radius = bounds.width() / 2

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = track
    }
    canvas.drawOval(bounds, paint)

    val hours = DawnTimeline.RING_WINDOW.inWholeHours.toInt()
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.FILL
        color = track
    }
    repeat(hours) { hour ->
        val angle = Math.toRadians(-90.0 + 360.0 * hour / hours)
        val markRadius = radius - stroke * 1.9f
        canvas.drawCircle(
            center + markRadius * cos(angle).toFloat(),
            center + markRadius * sin(angle).toFloat(),
            stroke * 0.24f,
            dot,
        )
    }

    val filled = progress.coerceIn(0f, 1f)
    if (filled > 0f) {
        paint.color = fill
        canvas.drawArc(bounds, -90f, 360f * filled, false, paint)

        val headAngle = Math.toRadians(-90.0 + 360.0 * filled)
        val headX = center + radius * cos(headAngle).toFloat()
        val headY = center + radius * sin(headAngle).toFloat()
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = fill
            alpha = 0x40
        }
        canvas.drawCircle(headX, headY, stroke * 1.6f, glow)
        glow.alpha = 0xFF
        canvas.drawCircle(headX, headY, stroke * 0.85f, glow)
    }
    return bitmap
}
