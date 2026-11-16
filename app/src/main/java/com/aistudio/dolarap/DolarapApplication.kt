package com.aistudio.dolarap

import android.app.Application
import androidx.work.Configuration
import com.aistudio.dolarap.di.appModules
import com.aistudio.dolarap.domain.service.SyncScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DolarapApplication : Application(), Configuration.Provider {

  override val workManagerConfiguration: Configuration
    get() =
      Configuration.Builder()
        .setWorkerFactory(KoinWorkerFactory())
        .build()

  override fun onCreate() {
    super.onCreate()
    if (GlobalContext.getOrNull() == null) {
      startKoin {
        androidContext(this@DolarapApplication)
        modules(appModules)
      }
    }
    GlobalContext.getOrNull()?.getOrNull<SyncScheduler>()?.schedulePeriodicSync()
  }
}
