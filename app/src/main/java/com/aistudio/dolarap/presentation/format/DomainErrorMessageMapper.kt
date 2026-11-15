package com.aistudio.dolarap.presentation.format

import androidx.annotation.StringRes
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.DomainError

data class UiText(
  @param:StringRes val resId: Int,
  val formatArgs: List<Any> = emptyList(),
)

object DomainErrorMapper {

  fun toUiText(error: DomainError): UiText =
    when (error) {
      DomainError.NoConnection -> UiText(R.string.error_no_connection)
      DomainError.Timeout -> UiText(R.string.error_timeout)
      is DomainError.UnsupportedPair ->
        UiText(R.string.error_unsupported_pair, listOf(error.pairCode ?: "-"))
      is DomainError.RateLimited -> UiText(R.string.error_rate_limited)
      is DomainError.ApiUnavailable -> UiText(R.string.error_api_unavailable)
      is DomainError.InvalidApiResponse -> UiText(R.string.error_invalid_api_response)
      is DomainError.DefaultCurrencyProtected ->
        UiText(R.string.error_default_currency_protected, listOf(error.code))
      is DomainError.CurrencyAlreadySelected ->
        UiText(R.string.error_currency_already_selected, listOf(error.code))
      is DomainError.CurrencyNotFound ->
        UiText(R.string.error_currency_not_found, listOf(error.code))
      DomainError.InvalidAmount -> UiText(R.string.converter_invalid_amount_message)
      is DomainError.QuoteUnavailable ->
        UiText(R.string.converter_unavailable_quote_message, listOf(error.currencyCode))
      is DomainError.Unexpected -> UiText(R.string.error_unexpected)
    }
}
