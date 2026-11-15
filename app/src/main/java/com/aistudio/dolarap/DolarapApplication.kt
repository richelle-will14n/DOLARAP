package com.aistudio.dolarap

import android.app.Application
import com.aistudio.dolarap.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DolarapApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    if (GlobalContext.getOrNull() == null) {
      startKoin {
        androidContext(this@DolarapApplication)
        modules(appModules)
      }
    }
  }
}
