package io.github.sourcem7.alfajralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.ui.AlfajrApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val graph: AppGraph by lazy { AppGraph.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AlfajrApp(graph) }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app is the recovery path after force-stop and OEM power management.
        graph.scope.launch { graph.scheduler.scheduleNext(ScheduleReason.AppOpened) }
    }
}
