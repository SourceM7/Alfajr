package io.github.sourcem7.alfajralarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * The Material 3 type scale, retuned for this app rather than replaced. Two
 * rules drive every change, and both are about Arabic:
 *
 *  - Line heights are generous (roughly 1.45x the size, more for small text).
 *    Arabic sets taller than Latin once diacritics and descenders are counted,
 *    and the stock scale clips them at large system font sizes.
 *  - Letter spacing is zero everywhere except the display styles. Positive
 *    tracking buys a Latin reader very little and visibly damages Arabic, whose
 *    letters join.
 *
 * Sizes stay close to the stock scale, except that the smallest body and label
 * steps are raised: they carry the supporting text on nearly every screen, and
 * at 11-12sp that text was the least readable thing in the app.
 *
 * No font files are bundled. The platform font is what renders both scripts
 * correctly on every device, and shipping one would mean shipping two.
 */
private val Default = Typography()

internal val AlfajrTypography = Typography(
    // Display is for the alarm time and nothing else. Negative tracking is
    // applied at that call site, where the glyphs are always digits.
    displayLarge = Default.displayLarge.copy(lineHeight = 68.sp),
    displayMedium = Default.displayMedium.copy(lineHeight = 56.sp),
    displaySmall = Default.displaySmall.copy(lineHeight = 48.sp),

    headlineLarge = Default.headlineLarge.tuned(size = 32, height = 44),
    headlineMedium = Default.headlineMedium.tuned(size = 28, height = 40),
    headlineSmall = Default.headlineSmall.tuned(size = 24, height = 34),

    titleLarge = Default.titleLarge.tuned(size = 22, height = 30),
    titleMedium = Default.titleMedium.tuned(size = 16, height = 24, weight = FontWeight.Medium),
    titleSmall = Default.titleSmall.tuned(size = 14, height = 20, weight = FontWeight.Medium),

    bodyLarge = Default.bodyLarge.tuned(size = 16, height = 26),
    bodyMedium = Default.bodyMedium.tuned(size = 15, height = 24),
    bodySmall = Default.bodySmall.tuned(size = 13, height = 20),

    labelLarge = Default.labelLarge.tuned(size = 14, height = 20, weight = FontWeight.Medium),
    labelMedium = Default.labelMedium.tuned(size = 13, height = 18, weight = FontWeight.Medium),
    labelSmall = Default.labelSmall.tuned(size = 12, height = 16, weight = FontWeight.Medium),
)

private fun TextStyle.tuned(size: Int, height: Int, weight: FontWeight? = null): TextStyle = copy(
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp,
    fontWeight = weight ?: fontWeight,
)
