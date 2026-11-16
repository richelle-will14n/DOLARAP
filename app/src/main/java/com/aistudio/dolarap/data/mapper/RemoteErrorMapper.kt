package com.aistudio.dolarap.data.mapper

import com.aistudio.dolarap.data.remote.model.RemoteError
import com.aistudio.dolarap.domain.model.DomainError

class RemoteErrorMapper {

  fun map(remoteError: RemoteError): DomainError =
    when (remoteError) {
      RemoteError.NoConnection -> DomainError.NoConnection
      RemoteError.Timeout -> DomainError.Timeout
      is RemoteError.CoinNotExists -> DomainError.UnsupportedPair(remoteError.pairCode)
      is RemoteError.HttpError ->
        if (remoteError.httpCode == 429) {
          DomainError.RateLimited(
            httpCode = 429,
            retryAfterSeconds = remoteError.retryAfterSeconds,
          )
        } else {
          DomainError.ApiUnavailable(
            httpCode = remoteError.httpCode,
            message = remoteError.apiMessage,
          )
        }
      is RemoteError.ServerError ->
        DomainError.ApiUnavailable(
          httpCode = remoteError.httpCode,
          message = remoteError.apiMessage,
        )
      is RemoteError.InvalidResponse -> DomainError.InvalidApiResponse(remoteError.reason)
      is RemoteError.EmptyOrIncompleteResponse -> DomainError.InvalidApiResponse(remoteError.reason)
      is RemoteError.InvalidRequest -> DomainError.UnsupportedPair(remoteError.reason)
      is RemoteError.Unexpected -> DomainError.Unexpected(remoteError.message)
    }

  fun toErrorTag(error: DomainError): String =
    when (error) {
      DomainError.NoConnection -> "NO_CONNECTION"
      DomainError.Timeout -> "TIMEOUT"
      is DomainError.UnsupportedPair -> "UNSUPPORTED_PAIR"
      is DomainError.RateLimited -> "RATE_LIMITED"
      is DomainError.ApiUnavailable -> "API_UNAVAILABLE_${error.httpCode ?: 0}"
      is DomainError.InvalidApiResponse -> "INVALID_API_RESPONSE"
      is DomainError.DefaultCurrencyProtected -> "DEFAULT_CURRENCY_PROTECTED"
      is DomainError.CurrencyAlreadySelected -> "CURRENCY_ALREADY_SELECTED"
      is DomainError.CurrencyNotFound -> "CURRENCY_NOT_FOUND"
      DomainError.InvalidAmount -> "INVALID_AMOUNT"
      is DomainError.QuoteUnavailable -> "QUOTE_UNAVAILABLE"
      is DomainError.Unexpected -> "UNEXPECTED"
    }
}
