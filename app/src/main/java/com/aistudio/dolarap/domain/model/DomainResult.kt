package com.aistudio.dolarap.domain.model

sealed interface DomainResult<out T> {
  data class Success<T>(val data: T) : DomainResult<T>

  data class Error(val error: DomainError) : DomainResult<Nothing>
}

sealed interface DomainError {
  data object NoConnection : DomainError

  data object Timeout : DomainError

  data class UnsupportedPair(val pairCode: String?) : DomainError

  data class RateLimited(val httpCode: Int = 429) : DomainError

  data class ApiUnavailable(val httpCode: Int? = null, val message: String? = null) : DomainError

  data class InvalidApiResponse(val reason: String) : DomainError

  data class DefaultCurrencyProtected(val code: String) : DomainError

  data class CurrencyAlreadySelected(val code: String) : DomainError

  data class CurrencyNotFound(val code: String) : DomainError

  data object InvalidAmount : DomainError

  data class QuoteUnavailable(val currencyCode: String) : DomainError

  data class Unexpected(val message: String?) : DomainError
}
