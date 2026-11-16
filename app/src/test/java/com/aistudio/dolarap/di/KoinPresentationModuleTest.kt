package com.aistudio.dolarap.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.worker.SyncQuotesWorker
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.presentation.MainDispatcherRule
import com.aistudio.dolarap.presentation.converter.ConverterViewModel
import com.aistudio.dolarap.presentation.quotes.QuotesViewModel
import com.aistudio.dolarap.presentation.search.CurrencySearchViewModel
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinPresentationModuleTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `Koin resolves all presentation ViewModels using existing domain and data modules`() {
    val koin = GlobalContext.get()

    val quotesViewModel = koin.get<QuotesViewModel>()
    val searchViewModel = koin.get<CurrencySearchViewModel>()
    val converterViewModel = koin.get<ConverterViewModel>()

    assertNotNull(quotesViewModel)
    assertNotNull(searchViewModel)
    assertNotNull(converterViewModel)
  }

  @SuppressLint("RestrictedApi")
  @Test
  fun `Koin resolves SyncScheduler and KoinWorkerFactory instantiates SyncQuotesWorker`() {
    val koin = GlobalContext.get()
    val scheduler = koin.get<SyncScheduler>()
    assertNotNull(scheduler)

    val directExecutor = Executor { it.run() }
    val params =
      WorkerParameters(
        UUID.randomUUID(),
        Data.EMPTY,
        emptySet(),
        WorkerParameters.RuntimeExtras(),
        0,
        0,
        directExecutor,
        Dispatchers.Unconfined,
        WorkManagerTaskExecutor(directExecutor),
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ): ListenableWorker? = null
        },
        ProgressUpdater { _, _, _ -> throw UnsupportedOperationException() },
        ForegroundUpdater { _, _, _ -> throw UnsupportedOperationException() },
      )

    val worker =
      KoinWorkerFactory().createWorker(
        ApplicationProvider.getApplicationContext(),
        SyncQuotesWorker::class.java.name,
        params,
      )

    assertTrue(worker is SyncQuotesWorker)
  }
}
