package io.github.sourcem7.alfajralarm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* M3 shape tokens: smooth rounded corners across components.
 * chips/snackbars 8dp, fields 12dp, standard cards 16dp,
 * hero cards/sheets 24dp, dialogs/bottom sheets 28dp. */
internal val AlfajrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
