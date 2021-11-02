package com.example.githubissues

import android.app.Application
import androidx.work.Configuration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            //inject Android context
            androidContext(this@MyApp)
            androidLogger()
            modules(koinModule)
        }

    }

    // Instance of AppContainer that will be used by all the Activities or Fragments of the app
    //val appContainer = AppContainer()
    //val context: Context = this.applicationContext

    //val appContainer = AppContainer(this)

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}