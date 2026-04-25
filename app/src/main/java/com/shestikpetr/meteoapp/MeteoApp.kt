package com.shestikpetr.meteoapp

import android.app.Application
import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.util.TokenManager
import org.osmdroid.config.Configuration

class MeteoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        RetrofitClient.init(TokenManager(this))
    }
}
