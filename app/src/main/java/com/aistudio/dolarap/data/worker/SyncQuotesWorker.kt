package com.aistudio.dolarap.data.worker

import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.SyncScheduler
import kotlinx.coroutines.CancellationException

class SyncQuotesWorker(
  appContext: Context,
  workerParams: WorkerParameters,
  private val quoteRepository: QuoteRepository,
  private val syncScheduler: SyncScheduler? = null,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val result =
      try {
        quoteRepository.refreshQuotes()
      } catch (e: CancellationException) {
        throw e
      } catch (e: SQLiteDatabaseLockedException) {
        return retryOrFail("SQLITE_LOCKED")
      } catch (e: SQLiteException) {
        val message = e.message.orEmpty().lowercase()
        return if (message.contains("locked") || message.contains("busy")) {
          retryOrFail("SQLITE_BUSY")
        } else {
          Result.failure(errorData("SQLITE_ERROR"))
        }
      } catch (e: Exception) {
        return Result.failure(errorData("UNEXPECTED_WORKER_ERROR"))
      }

    return when (result) {
      is DomainResult.Success -> {
        if (!tags.contains(WorkManagerSyncScheduler.TAG_CONNECTIVITY_RETRY)) {
          syncScheduler?.cancelConnectivityRetrySync()
        }
        Result.success(
          workDataOf(
            KEY_SYNC_TIMESTAMP to (result.data.lastSuccessfulSyncAtMillis ?: 0L),
          )
        )
      }

      is DomainResult.Error -> handleDomainError(result.error)
    }
  }

  private fun handleDomainError(error: DomainError): Result =
    when (error) {
      DomainError.NoConnection -> retryOrFail("NO_CONNECTION")
      DomainError.Timeout -> retryOrFail("TIMEOUT")

      is DomainError.RateLimited -> {
        val retryAfter = error.retryAfterSeconds
        if (retryAfter != null) {
          syncScheduler?.scheduleConnectivityRetrySync(delaySeconds = retryAfter)
          Result.failure(errorData("RATE_LIMITED"))
        } else {
          retryOrFail("RATE_LIMITED")
        }
      }

      is DomainError.ApiUnavailable -> {
        val code = error.httpCode
        if (code == null || code == 408 || code >= 500) {
          retryOrFail("API_UNAVAILABLE_${code ?: 0}")
        } else {
          Result.failure(errorData("API_CLIENT_ERROR_$code"))
        }
      }

      is DomainError.UnsupportedPair -> Result.failure(errorData("UNSUPPORTED_PAIR"))
      is DomainError.InvalidApiResponse -> Result.failure(errorData("INVALID_API_RESPONSE"))
      is DomainError.DefaultCurrencyProtected ->
        Result.failure(errorData("DEFAULT_CURRENCY_PROTECTED"))
      is DomainError.CurrencyAlreadySelected ->
        Result.failure(errorData("CURRENCY_ALREADY_SELECTED"))
      is DomainError.CurrencyNotFound -> Result.failure(errorData("CURRENCY_NOT_FOUND"))
      DomainError.InvalidAmount -> Result.failure(errorData("INVALID_AMOUNT"))
      is DomainError.QuoteUnavailable -> Result.failure(errorData("QUOTE_UNAVAILABLE"))
      is DomainError.Unexpected -> Result.failure(errorData("UNEXPECTED"))
    }

  private fun retryOrFail(errorCode: String): Result =
    if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
      Result.retry()
    } else {
      Result.failure(errorData(errorCode))
    }

  private fun errorData(errorCode: String): Data = workDataOf(KEY_ERROR_CODE to errorCode)

  companion object {
    const val MAX_RETRY_ATTEMPTS = 3
    const val KEY_ERROR_CODE = "error_code"
    const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
  }
}
