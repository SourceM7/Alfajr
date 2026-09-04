package io.github.sourcem7.alfajralarm.ui

import androidx.lifecycle.ViewModel
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingingSessionObserver
import kotlinx.coroutines.flow.StateFlow

/** Read-only presentation boundary for the full-screen ringing activity. */
class RingingViewModel(observer: RingingSessionObserver) : ViewModel() {
    val session: StateFlow<RingingSession?> = observer.session
}
