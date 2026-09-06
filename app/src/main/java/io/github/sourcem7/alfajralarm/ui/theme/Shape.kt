package io.github.sourcem7.alfajralarm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* M3 shape tokens: chips/snackbars 4dp, fields 8dp, cards 12dp,
 * sheets/drawer edge 16dp, dialogs 28dp. */
internal val AlfajrShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
