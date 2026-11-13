package com.igi2.mobile

import android.app.Application
import com.igi2.mobile.data.LauncherRepository

class Igi2Application : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create the single repository up front so the shared preferences are read once. The
        // network probe is not started here; MainActivity warms it up once the UI is on screen.
        LauncherRepository.get(this)
    }
}
