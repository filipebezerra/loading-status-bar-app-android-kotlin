package com.udacity

import android.app.Application
import timber.log.Timber

class LoadingStatusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BuildConfig.DEBUG.takeIf { it }?.let { Timber.plant(Timber.DebugTree()) }
    }
}