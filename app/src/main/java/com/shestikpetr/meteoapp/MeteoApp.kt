package com.shestikpetr.meteoapp

import android.app.Application
import org.osmdroid.config.Configuration

class MeteoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
    }
}
