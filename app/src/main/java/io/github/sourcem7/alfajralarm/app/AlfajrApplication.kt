package io.github.sourcem7.alfajralarm.app

import android.app.Application
import io.github.sourcem7.alfajralarm.alarm.NotificationChannels

class AlfajrApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        // Channels must exist before capability health can report on them.
        NotificationChannels.ensure(this)
    }
}
