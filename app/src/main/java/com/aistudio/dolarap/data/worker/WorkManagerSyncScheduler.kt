package com.aistudio.dolarap.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aistudio.dolarap.domain.service.BackgroundSyncInfo
import com.aistudio.dolarap.domain.service.BackgroundSyncState
import com.aistudio.dolarap.domain.service.SyncScheduler
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WorkManagerSyncScheduler(
  private val context: Context,
) : SyncScheduler {

  private val workManager: WorkManager
    get() = WorkManager.getInstance(context.applicationContext)

  override fun schedulePeriodicSync() {
    val request =
      PeriodicWorkRequestBuilder<SyncQuotesWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
        .setConstraints(connectedConstraints())
        .setBackoffCriteria(
          BackoffPolicy.EXPONENTIAL,
          INITIAL_BACKOFF_SECONDS,
          TimeUnit.SECONDS,
        )
        .addTag(TAG_PERIODIC_SYNC)
        .build()

    workManager.enqueueUniquePeriodicWork(
      UNIQUE_PERIODIC_SYNC_WORK,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  override fun scheduleConnectivityRetrySync(delaySeconds: Long?) {
    val clampedDelaySeconds = delaySeconds?.coerceIn(1L, MAX_RETRY_AFTER_SECONDS)

    val builder =
      OneTimeWorkRequestBuilder<SyncQuotesWorker>()
        .setConstraints(connectedConstraints())
        .setBackoffCriteria(
          BackoffPolicy.EXPONENTIAL,
          INITIAL_BACKOFF_SECONDS,
          TimeUnit.SECONDS,
        )
        .addTag(TAG_CONNECTIVITY_RETRY)

    if (clampedDelaySeconds != null) {
      builder.setInitialDelay(clampedDelaySeconds, TimeUnit.SECONDS)
    }

    val policy =
      if (clampedDelaySeconds != null) {
        ExistingWorkPolicy.REPLACE
      } else {
        ExistingWorkPolicy.KEEP
      }

    workManager.enqueueUniqueWork(
      UNIQUE_CONNECTIVITY_RETRY_WORK,
      policy,
      builder.build(),
    )
  }

  override fun cancelConnectivityRetrySync() {
    workManager.cancelUniqueWork(UNIQUE_CONNECTIVITY_RETRY_WORK)
  }

  override fun observeSyncState(): Flow<BackgroundSyncInfo> =
    combine(
      workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_SYNC_WORK),
      workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_CONNECTIVITY_RETRY_WORK),
    ) { periodicInfos, retryInfos ->
      mapWorkInfosToSyncInfo(periodicInfos.firstOrNull(), retryInfos.firstOrNull())
    }

  private fun connectedConstraints(): Constraints =
    Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

  internal fun mapWorkInfosToSyncInfo(
    periodicInfo: WorkInfo?,
    retryInfo: WorkInfo?,
  ): BackgroundSyncInfo {
    val periodicScheduled = periodicInfo != null && periodicInfo.state != WorkInfo.State.CANCELLED

    val resolvedState =
      when {
        retryInfo?.state == WorkInfo.State.RUNNING ||
          periodicInfo?.state == WorkInfo.State.RUNNING -> BackgroundSyncState.RUNNING
        retryInfo?.state == WorkInfo.State.ENQUEUED -> BackgroundSyncState.ENQUEUED
        retryInfo?.state == WorkInfo.State.BLOCKED ||
          periodicInfo?.state == WorkInfo.State.BLOCKED -> BackgroundSyncState.BLOCKED
        retryInfo?.state == WorkInfo.State.FAILED -> BackgroundSyncState.FAILED
        retryInfo?.state == WorkInfo.State.SUCCEEDED -> BackgroundSyncState.SUCCEEDED
        periodicInfo?.state == WorkInfo.State.ENQUEUED -> BackgroundSyncState.ENQUEUED
        periodicInfo?.state == WorkInfo.State.SUCCEEDED -> BackgroundSyncState.SUCCEEDED
        else -> BackgroundSyncState.IDLE
      }

    val nextRetryAt =
      retryInfo
        ?.takeIf {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
        }
        ?.nextScheduleTimeMillis
        ?.takeIf { it > 0L && it != Long.MAX_VALUE }

    val errorCode =
      retryInfo?.outputData?.getString(SyncQuotesWorker.KEY_ERROR_CODE)
        ?: periodicInfo?.outputData?.getString(SyncQuotesWorker.KEY_ERROR_CODE)

    return BackgroundSyncInfo(
      periodicScheduled = periodicScheduled,
      state = resolvedState,
      requiresConnectivity = true,
      nextRetryAtMillis = nextRetryAt,
      lastErrorCode = errorCode,
    )
  }

  companion object {
    const val UNIQUE_PERIODIC_SYNC_WORK = "dolarap_periodic_quotes_sync"
    const val UNIQUE_CONNECTIVITY_RETRY_WORK = "dolarap_connectivity_retry_quotes_sync"
    const val TAG_PERIODIC_SYNC = "tag_dolarap_periodic_sync"
    const val TAG_CONNECTIVITY_RETRY = "tag_dolarap_connectivity_retry"
    const val PERIODIC_INTERVAL_HOURS = 6L
    const val INITIAL_BACKOFF_SECONDS = 30L
    const val MAX_RETRY_AFTER_SECONDS = 3600L
  }
}
