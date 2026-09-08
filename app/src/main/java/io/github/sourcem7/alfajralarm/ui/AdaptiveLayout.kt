package io.github.sourcem7.alfajralarm.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/** High-level layout decisions shared by the app's presentation surfaces. */
data class AppWindowLayout(
    val expandedWidth: Boolean,
    val compactHeight: Boolean,
) {
    val showTwoPanes: Boolean get() = expandedWidth && !compactHeight
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun currentAppWindowLayout(): AppWindowLayout {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return AppWindowLayout(
        expandedWidth = sizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
        ),
        compactHeight = !sizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
        ),
    )
}
