package com.aistudio.dolarap.domain.service

import kotlinx.coroutines.flow.Flow

enum class BackgroundSyncState {
  IDLE,
  ENQUEUED,
  RUNNING,
  SUCCEEDED,
  FAILED,
  BLOCKED,
}

data class BackgroundSyncInfo(
  val periodicScheduled: Boolean = false,
  val state: BackgroundSyncState = BackgroundSyncState.IDLE,
  val requiresConnectivity: Boolean = true,
  val nextRetryAtMillis: Long? = null,
  val lastErrorCode: String? = null,
)

interface SyncScheduler {
  fun schedulePeriodicSync()

  fun scheduleConnectivityRetrySync(delaySeconds: Long? = null)

  fun cancelConnectivityRetrySync()

  fun observeSyncState(): Flow<BackgroundSyncInfo>
}
