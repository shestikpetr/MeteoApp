package com.shestikpetr.meteoapp

import android.app.Application
import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.util.TokenStore
import org.osmdroid.config.Configuration

class MeteoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        RetrofitClient.init(TokenStore(this))
    }
}
