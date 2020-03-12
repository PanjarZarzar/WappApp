package com.canay.updatewhatsapp2019

import android.app.Application
import com.canay.updatewhatsapp2019.di.initDI
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        //init logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        initDI()
    }
}