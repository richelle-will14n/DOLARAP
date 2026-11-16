package com.aistudio.dolarap.data.worker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.BackgroundSyncInfo
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.presentation.FakeQuoteRepository
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SyncQuotesWorkerTest {

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @SuppressLint("RestrictedApi")
  private fun createWorkerParams(
    runAttemptCount: Int = 0,
    tags: Set<String> = setOf(WorkManagerSyncScheduler.TAG_PERIODIC_SYNC),
  ): WorkerParameters {
    val directExecutor = Executor { runnable -> runnable.run() }
    val taskExecutor = WorkManagerTaskExecutor(directExecutor)
    return WorkerParameters(
      UUID.randomUUID(),
      Data.EMPTY,
      tags,
      WorkerParameters.RuntimeExtras(),
      runAttemptCount,
      0,
      directExecutor,
      kotlinx.coroutines.Dispatchers.Unconfined,
      taskExecutor,
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
  fun `worker executes refreshQuotes and returns success with timestamp and cancels retry when periodic`() =
    runBlocking {
      val repo = FakeQuoteRepository()
      val scheduler = RecordingSyncScheduler()
      val expectedStatus =
        SyncStatus(
          lastSuccessfulSyncAtMillis = 1_700_000_555_000L,
          lastAttemptAtMillis = 1_700_000_555_000L,
          lastErrorType = null,
          lastCatalogSyncAtMillis = 1_700_000_555_000L,
          freshnessStatus = QuoteFreshnessStatus.FRESH,
        )
      repo.refreshQuotesResult = DomainResult.Success(expectedStatus)

      val worker =
        SyncQuotesWorker(
          appContext = context,
          workerParams =
            createWorkerParams(
              runAttemptCount = 0,
              tags = setOf(WorkManagerSyncScheduler.TAG_PERIODIC_SYNC),
            ),
          quoteRepository = repo,
          syncScheduler = scheduler,
        )

      val result = worker.doWork()

      assertTrue(result is ListenableWorker.Result.Success)
      assertEquals(1, repo.refreshCallCount)
      assertEquals(
        1_700_000_555_000L,
        (result as ListenableWorker.Result.Success)
          .outputData
          .getLong(SyncQuotesWorker.KEY_SYNC_TIMESTAMP, 0L),
      )
      assertEquals(1, scheduler.cancelRetryCalls)
    }

  @Test
  fun `worker returns retry on temporary failures up to MAX_RETRY_ATTEMPTS then failure`() =
    runBlocking {
      val temporaryErrors =
        listOf(
          DomainError.NoConnection,
          DomainError.Timeout,
          DomainError.ApiUnavailable(httpCode = 503, message = "Service Unavailable"),
        )

      for (error in temporaryErrors) {
        val repo = FakeQuoteRepository().apply { refreshQuotesResult = DomainResult.Error(error) }
        val scheduler = RecordingSyncScheduler()

        val retryWorker =
          SyncQuotesWorker(
            appContext = context,
            workerParams = createWorkerParams(runAttemptCount = 0),
            quoteRepository = repo,
            syncScheduler = scheduler,
          )
        assertEquals(ListenableWorker.Result.retry(), retryWorker.doWork())

        val exhaustedWorker =
          SyncQuotesWorker(
            appContext = context,
            workerParams = createWorkerParams(runAttemptCount = SyncQuotesWorker.MAX_RETRY_ATTEMPTS),
            quoteRepository = repo,
            syncScheduler = scheduler,
          )
        assertTrue(exhaustedWorker.doWork() is ListenableWorker.Result.Failure)
      }
    }

  @Test
  fun `worker returns immediate failure on permanent errors without retrying`() = runBlocking {
    val permanentErrors =
      listOf(
        DomainError.ApiUnavailable(httpCode = 403, message = "Forbidden"),
        DomainError.UnsupportedPair("XYZ-BRL"),
        DomainError.InvalidApiResponse("Malformed JSON"),
      )

    for (error in permanentErrors) {
      val repo = FakeQuoteRepository().apply { refreshQuotesResult = DomainResult.Error(error) }
      val worker =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(runAttemptCount = 0),
          quoteRepository = repo,
          syncScheduler = RecordingSyncScheduler(),
        )
      val result = worker.doWork()
      assertTrue("Expected failure for $error", result is ListenableWorker.Result.Failure)
    }
  }

  @Test
  fun `worker respects HTTP 429 Retry-After by scheduling delayed retry and falls back to bounded retry without header`() =
    runBlocking {
      val scheduler = RecordingSyncScheduler()
      val repoWithHeader =
        FakeQuoteRepository().apply {
          refreshQuotesResult =
            DomainResult.Error(DomainError.RateLimited(httpCode = 429, retryAfterSeconds = 120L))
        }

      val workerWithHeader =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(runAttemptCount = 0),
          quoteRepository = repoWithHeader,
          syncScheduler = scheduler,
        )
      val headerResult = workerWithHeader.doWork()

      assertTrue(headerResult is ListenableWorker.Result.Failure)
      assertEquals(listOf(120L), scheduler.scheduledRetryDelays)

      val repoNoHeader =
        FakeQuoteRepository().apply {
          refreshQuotesResult =
            DomainResult.Error(DomainError.RateLimited(httpCode = 429, retryAfterSeconds = null))
        }
      val workerNoHeader =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(runAttemptCount = 0),
          quoteRepository = repoNoHeader,
          syncScheduler = scheduler,
        )
      assertEquals(ListenableWorker.Result.retry(), workerNoHeader.doWork())
    }

  @Test
  fun `worker handles transient SQLite lock with bounded retry and non-transient SQLite error with failure`() =
    runBlocking {
      val lockedRepo = ThrowingQuoteRepository(SQLiteDatabaseLockedException("database is locked"))
      val lockedWorker =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(runAttemptCount = 0),
          quoteRepository = lockedRepo,
          syncScheduler = RecordingSyncScheduler(),
        )
      assertEquals(ListenableWorker.Result.retry(), lockedWorker.doWork())

      val fatalRepo = ThrowingQuoteRepository(SQLiteException("no such table: quotes"))
      val fatalWorker =
        SyncQuotesWorker(
          appContext = context,
          workerParams = createWorkerParams(runAttemptCount = 0),
          quoteRepository = fatalRepo,
          syncScheduler = RecordingSyncScheduler(),
        )
      assertTrue(fatalWorker.doWork() is ListenableWorker.Result.Failure)
    }

  private class RecordingSyncScheduler : SyncScheduler {
    val scheduledRetryDelays = mutableListOf<Long?>()
    var cancelRetryCalls = 0

    override fun schedulePeriodicSync() = Unit

    override fun scheduleConnectivityRetrySync(delaySeconds: Long?) {
      scheduledRetryDelays.add(delaySeconds)
    }

    override fun cancelConnectivityRetrySync() {
      cancelRetryCalls++
    }

    override fun observeSyncState(): Flow<BackgroundSyncInfo> = MutableStateFlow(BackgroundSyncInfo())
  }

  private class ThrowingQuoteRepository(
    private val throwable: Throwable,
  ) : QuoteRepository {
    override fun observeDashboardQuotes(): Flow<DashboardQuotesSnapshot> =
      throw UnsupportedOperationException()

    override fun observeFavoriteQuotes(): Flow<DashboardQuotesSnapshot> =
      throw UnsupportedOperationException()

    override suspend fun getQuotesSnapshot(): Map<String, QuoteRate> = emptyMap()

    override suspend fun refreshQuotes(): DomainResult<SyncStatus> = throw throwable

    override suspend fun refreshCurrencyQuote(code: String): DomainResult<Unit> =
      throw UnsupportedOperationException()

    override suspend fun toggleFavorite(code: String): DomainResult<Unit> =
      throw UnsupportedOperationException()

    override suspend fun removeSelectedCurrency(code: String): DomainResult<Unit> =
      throw UnsupportedOperationException()
  }
}
