package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.SyncScheduler

class RefreshQuotesUseCase(
  private val quoteRepository: QuoteRepository,
  private val syncScheduler: SyncScheduler? = null,
) {
  suspend operator fun invoke(): DomainResult<SyncStatus> {
    val result = quoteRepository.refreshQuotes()
    when (result) {
      is DomainResult.Success -> syncScheduler?.cancelConnectivityRetrySync()
      is DomainResult.Error ->
        when (val error = result.error) {
          DomainError.NoConnection,
          DomainError.Timeout -> syncScheduler?.scheduleConnectivityRetrySync()
          is DomainError.RateLimited ->
            syncScheduler?.scheduleConnectivityRetrySync(delaySeconds = error.retryAfterSeconds)
          is DomainError.ApiUnavailable ->
            if ((error.httpCode ?: 0) >= 500) {
              syncScheduler?.scheduleConnectivityRetrySync()
            }
          else -> Unit
        }
    }
    return result
  }
}
