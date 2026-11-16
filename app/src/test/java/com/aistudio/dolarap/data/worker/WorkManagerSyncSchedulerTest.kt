package com.aistudio.dolarap.data.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.aistudio.dolarap.domain.service.BackgroundSyncState
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class WorkManagerSyncSchedulerTest {

  private lateinit var context: Context
  private lateinit var workManager: WorkManager
  private lateinit var scheduler: WorkManagerSyncScheduler

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val noOpFactory =
      object : WorkerFactory() {
        override fun createWorker(
          appContext: Context,
          workerClassName: String,
          workerParameters: WorkerParameters,
        ): ListenableWorker? = null
      }
    if (!WorkManager.isInitialized()) {
      WorkManager.initialize(
        context,
        Configuration.Builder()
          .setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
          .setTaskExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
          .setWorkerFactory(noOpFactory)
          .build(),
      )
    }
    workManager = WorkManager.getInstance(context)
    workManager.cancelAllWork().result.get()
    workManager.pruneWork().result.get()
    scheduler = WorkManagerSyncScheduler(context)
  }

  @After
  fun tearDown() {
    if (::workManager.isInitialized) {
      workManager.cancelAllWork().result.get()
      workManager.pruneWork().result.get()
    }
  }

  @Test
  fun `schedulePeriodicSync enqueues unique 6-hour periodic work with NetworkType CONNECTED without duplication`() =
    runBlocking {
      scheduler.schedulePeriodicSync()
      scheduler.schedulePeriodicSync()

      val infos =
        workManager
          .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_PERIODIC_SYNC_WORK)
          .get()
      assertEquals(1, infos.size)
      val info = infos.first()
      assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
      assertEquals(
        TimeUnit.HOURS.toMillis(WorkManagerSyncScheduler.PERIODIC_INTERVAL_HOURS),
        info.periodicityInfo?.repeatIntervalMillis,
      )
      assertTrue(info.tags.contains(WorkManagerSyncScheduler.TAG_PERIODIC_SYNC))

      val syncState = scheduler.observeSyncState().first()
      assertTrue(syncState.periodicScheduled)
      assertTrue(syncState.requiresConnectivity)
    }

  @Test
  fun `scheduleConnectivityRetrySync enqueues unique OneTimeWork with NetworkType CONNECTED and respects Retry-After delay`() =
    runBlocking {
      scheduler.scheduleConnectivityRetrySync()
      val firstInfos =
        workManager
          .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_CONNECTIVITY_RETRY_WORK)
          .get()
      assertEquals(1, firstInfos.size)
      val firstId = firstInfos.first().id
      assertEquals(NetworkType.CONNECTED, firstInfos.first().constraints.requiredNetworkType)
      assertEquals(0L, firstInfos.first().initialDelayMillis)

      // KEEP evita duplicar trabalho pendente quando não há delay explícito
      scheduler.scheduleConnectivityRetrySync()
      val keptInfos =
        workManager
          .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_CONNECTIVITY_RETRY_WORK)
          .get()
      assertEquals(1, keptInfos.size)
      assertEquals(firstId, keptInfos.first().id)

      // Delay explícito vindo de Retry-After substitui com initialDelay configurado e teto seguro
      scheduler.scheduleConnectivityRetrySync(delaySeconds = 180L)
      val delayedInfos =
        workManager
          .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_CONNECTIVITY_RETRY_WORK)
          .get()
      assertEquals(1, delayedInfos.size)
      assertEquals(TimeUnit.SECONDS.toMillis(180L), delayedInfos.first().initialDelayMillis)
      assertEquals(NetworkType.CONNECTED, delayedInfos.first().constraints.requiredNetworkType)

      scheduler.cancelConnectivityRetrySync()
      val cancelledInfos =
        workManager
          .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_CONNECTIVITY_RETRY_WORK)
          .get()
      assertEquals(WorkInfo.State.CANCELLED, cancelledInfos.firstOrNull()?.state)
    }

  @Test
  fun `mapWorkInfosToSyncInfo distinguishes IDLE, ENQUEUED, RUNNING, SUCCEEDED, FAILED, and BLOCKED states`() {
    val periodicEnqueued =
      WorkInfo(
        id = java.util.UUID.randomUUID(),
        state = WorkInfo.State.ENQUEUED,
        tags = setOf(WorkManagerSyncScheduler.TAG_PERIODIC_SYNC),
      )
    val retryRunning =
      WorkInfo(
        id = java.util.UUID.randomUUID(),
        state = WorkInfo.State.RUNNING,
        tags = setOf(WorkManagerSyncScheduler.TAG_CONNECTIVITY_RETRY),
      )
    val retryFailed =
      WorkInfo(
        id = java.util.UUID.randomUUID(),
        state = WorkInfo.State.FAILED,
        tags = setOf(WorkManagerSyncScheduler.TAG_CONNECTIVITY_RETRY),
        outputData =
          androidx.work.workDataOf(SyncQuotesWorker.KEY_ERROR_CODE to "NO_CONNECTION"),
      )
    val retryBlocked =
      WorkInfo(
        id = java.util.UUID.randomUUID(),
        state = WorkInfo.State.BLOCKED,
        tags = setOf(WorkManagerSyncScheduler.TAG_CONNECTIVITY_RETRY),
      )
    val retrySucceeded =
      WorkInfo(
        id = java.util.UUID.randomUUID(),
        state = WorkInfo.State.SUCCEEDED,
        tags = setOf(WorkManagerSyncScheduler.TAG_CONNECTIVITY_RETRY),
      )

    assertEquals(
      BackgroundSyncState.IDLE,
      scheduler.mapWorkInfosToSyncInfo(periodicInfo = null, retryInfo = null).state,
    )
    assertFalse(
      scheduler.mapWorkInfosToSyncInfo(periodicInfo = null, retryInfo = null).periodicScheduled
    )
    assertEquals(
      BackgroundSyncState.ENQUEUED,
      scheduler.mapWorkInfosToSyncInfo(periodicInfo = periodicEnqueued, retryInfo = null).state,
    )
    assertEquals(
      BackgroundSyncState.RUNNING,
      scheduler
        .mapWorkInfosToSyncInfo(periodicInfo = periodicEnqueued, retryInfo = retryRunning)
        .state,
    )
    assertEquals(
      BackgroundSyncState.BLOCKED,
      scheduler
        .mapWorkInfosToSyncInfo(periodicInfo = periodicEnqueued, retryInfo = retryBlocked)
        .state,
    )
    assertEquals(
      BackgroundSyncState.SUCCEEDED,
      scheduler
        .mapWorkInfosToSyncInfo(periodicInfo = periodicEnqueued, retryInfo = retrySucceeded)
        .state,
    )
    val failedMapped =
      scheduler.mapWorkInfosToSyncInfo(periodicInfo = periodicEnqueued, retryInfo = retryFailed)
    assertEquals(BackgroundSyncState.FAILED, failedMapped.state)
    assertNotNull(failedMapped.lastErrorCode)
    assertEquals("NO_CONNECTION", failedMapped.lastErrorCode)
  }
}
