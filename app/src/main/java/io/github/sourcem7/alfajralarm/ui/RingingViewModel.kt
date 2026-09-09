package io.github.sourcem7.alfajralarm.ui

import androidx.lifecycle.ViewModel
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingingSessionObserver
import kotlinx.coroutines.flow.StateFlow

/** Read-only presentation boundary for the full-screen ringing activity. */
class RingingViewModel(observer: RingingSessionObserver) : ViewModel() {
    val session: StateFlow<RingingSession?> = observer.session

    /**
     * Set while a session is starting but has not begun. The activity renders a
     * starting state for it rather than an empty window.
     */
    val startingSessionId: StateFlow<String?> = observer.startingSessionId
}
