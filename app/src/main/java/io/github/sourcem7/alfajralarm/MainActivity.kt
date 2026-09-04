package io.github.sourcem7.alfajralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.ui.AlfajrApp
import io.github.sourcem7.alfajralarm.ui.AlfajrViewModel

class MainActivity : ComponentActivity() {
    private val graph: AppGraph by lazy { AppGraph.from(this) }
    private val viewModel: AlfajrViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = graph.createAlfajrViewModel() as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AlfajrApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app is the recovery path after force-stop and OEM power management.
        viewModel.onAppResumed()
    }
}
