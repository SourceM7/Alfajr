package io.github.sourcem7.alfajralarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sourcem7.alfajralarm.domain.AppearancePreferencesStore
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingingSessionObserver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Read-only presentation boundary for the full-screen ringing activity. */
class RingingViewModel(
    observer: RingingSessionObserver,
    appearance: AppearancePreferencesStore,
) : ViewModel() {
    val session: StateFlow<RingingSession?> = observer.session
    val dynamicColor: StateFlow<Boolean> = appearance.dynamicColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    /**
     * Set while a session is starting but has not begun. The activity renders a
     * starting state for it rather than an empty window.
     */
    val startingSessionId: StateFlow<String?> = observer.startingSessionId
}
