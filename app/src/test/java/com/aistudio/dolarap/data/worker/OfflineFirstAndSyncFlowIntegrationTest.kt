package com.aistudio.dolarap.data.worker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.remote.AwesomeApiRemoteDataSource
import com.aistudio.dolarap.data.remote.AwesomeApiService
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.aistudio.dolarap.data.repository.QuoteRepositoryImpl
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.service.BackgroundSyncInfo
import com.aistudio.dolarap.domain.service.BackgroundSyncState
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.domain.service.TimeProvider
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import com.aistudio.dolarap.presentation.MainDispatcherRule
import com.aistudio.dolarap.presentation.quotes.QuotesSyncPhase
import com.aistudio.dolarap.presentation.quotes.QuotesUiState
import com.aistudio.dolarap.presentation.quotes.QuotesViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class OfflineFirstAndSyncFlowIntegrationTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var mockWebServer: MockWebServer
  private lateinit var database: DolarapDatabase
  private lateinit var quoteRepository: QuoteRepositoryImpl
  private var fakeNowMillis = 1_700_000_000_000L

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, DolarapDatabase::class.java)
        .addCallback(DolarapDatabase.seedCallback())
        .allowMainThreadQueries()
        .build()

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    val service =
      Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AwesomeApiService::class.java)

    val remoteDataSource =
      AwesomeApiRemoteDataSource(
        service = service,
        moshi = moshi,
        quoteRemoteMapper = QuoteRemoteMapper(),
        availableCatalogParser = AvailableCatalogParser(moshi),
        availablePairDao = database.availablePairDao(),
      )

    val entityMapper = QuoteEntityMapper(QuoteFreshnessEvaluator())
    quoteRepository =
      QuoteRepositoryImpl(
        currencyDao = database.currencyDao(),
        quoteDao = database.quoteDao(),
        syncMetadataDao = database.syncMetadataDao(),
        remoteDataSource = remoteDataSource,
        quoteEntityMapper = entityMapper,
        remoteErrorMapper = RemoteErrorMapper(),
        timeProvider = TimeProvider { fakeNowMillis },
      )
  }

  @After
  fun tearDown() {
    database.close()
    mockWebServer.shutdown()
  }

  @SuppressLint("RestrictedApi")
  private fun createWorkerParams(): WorkerParameters {
    val directExecutor = Executor { it.run() }
    return WorkerParameters(
      UUID.randomUUID(),
      Data.EMPTY,
      setOf(WorkManagerSyncScheduler.TAG_PERIODIC_SYNC),
      WorkerParameters.RuntimeExtras(),
      0,
      0,
      directExecutor,
      kotlinx.coroutines.Dispatchers.Unconfined,
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
  }

  @Test
  fun `offline first serves Room immediately, preserves cache on failure, and updates UI Flow when worker syncs`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val scheduler = FakeSyncScheduler()
      val viewModel =
        QuotesViewModel(
          observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(quoteRepository),
          refreshQuotesUseCase = RefreshQuotesUseCase(quoteRepository, scheduler),
          toggleFavoriteUseCase = ToggleFavoriteUseCase(quoteRepository),
          removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepository),
          syncScheduler = scheduler,
          refreshOnStart = false,
        )

      val collectorJob = launch { viewModel.uiState.collect {} }

      // 1. Estado inicial sem internet: exibe as 10 padrão aguardando primeira sincronização
      val initialState = viewModel.uiState.first { it is QuotesUiState.Success } as QuotesUiState.Success
      assertEquals(10, initialState.items.size)
      assertEquals(QuotesSyncPhase.WAITING_FIRST_SYNC, initialState.syncPhase)

      // 2. Worker em background sincroniza cotações novas e grava no Room -> UI Flow atualiza automaticamente
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(
            """
            {
              "USDBRL": {
                "code": "USD",
                "codein": "BRL",
                "name": "Dólar Americano/Real Brasileiro",
                "high": "5.8200",
                "low": "5.7900",
                "varBid": "0.0200",
                "pctChange": "0.35",
                "bid": "5.8123",
                "ask": "5.8140",
                "timestamp": "1700000000",
                "create_date": "2026-11-16 10:00:00"
              }
            }
            """
              .trimIndent()
          )
      )

      scheduler.flow.value = BackgroundSyncInfo(periodicScheduled = true, state = BackgroundSyncState.RUNNING)
      val syncingState =
        viewModel.uiState.first {
          it is QuotesUiState.Success && it.syncPhase == QuotesSyncPhase.SYNCING
        } as QuotesUiState.Success
      assertTrue(syncingState.isRefreshing)

      val worker =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(),
          quoteRepository = quoteRepository,
          syncScheduler = scheduler,
        )
      val workResult = worker.doWork()
      assertTrue(workResult is ListenableWorker.Result.Success)
      scheduler.flow.value = BackgroundSyncInfo(periodicScheduled = true, state = BackgroundSyncState.SUCCEEDED)

      val completedState =
        viewModel.uiState.first {
          it is QuotesUiState.Success &&
            it.syncPhase == QuotesSyncPhase.COMPLETED &&
            it.items.first { item -> item.code == "USD" }.quote?.bid == BigDecimal("5.8123")
        } as QuotesUiState.Success
      assertFalse(completedState.isRefreshing)
      assertFalse(completedState.syncStatus.isStale)
      assertEquals(QuoteFreshnessStatus.FRESH, completedState.syncStatus.freshnessStatus)

      // 3. Avança > 15 minutos e simula falha de rede -> cache local mantido, marca STALE e agenda retry
      fakeNowMillis += 16L * 60L * 1000L
      mockWebServer.shutdown()

      viewModel.refreshQuotes()
      scheduler.flow.value = BackgroundSyncInfo(periodicScheduled = true, state = BackgroundSyncState.ENQUEUED)

      val offlineStaleState =
        viewModel.uiState.first {
          it is QuotesUiState.Success &&
            it.syncPhase == QuotesSyncPhase.OFFLINE &&
            it.syncStatus.isStale
        } as QuotesUiState.Success

      assertEquals(1, scheduler.retryScheduledCalls)
      assertTrue(offlineStaleState.isBackgroundRetryScheduled)
      assertEquals(
        BigDecimal("5.8123"),
        offlineStaleState.items.first { it.code == "USD" }.quote?.bid,
      )
      assertEquals(QuoteFreshnessStatus.STALE, offlineStaleState.syncStatus.freshnessStatus)

      collectorJob.cancel()
    }

  private class FakeSyncScheduler : SyncScheduler {
    val flow = MutableStateFlow(BackgroundSyncInfo())
    var retryScheduledCalls = 0

    override fun schedulePeriodicSync() = Unit

    override fun scheduleConnectivityRetrySync(delaySeconds: Long?) {
      retryScheduledCalls++
    }

    override fun cancelConnectivityRetrySync() = Unit

    override fun observeSyncState(): Flow<BackgroundSyncInfo> = flow
  }
}
