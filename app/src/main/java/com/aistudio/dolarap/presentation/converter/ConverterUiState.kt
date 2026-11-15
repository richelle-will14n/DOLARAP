package com.aistudio.dolarap.presentation.converter

import com.aistudio.dolarap.domain.model.ConversionResult
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.presentation.format.UiText

data class ConverterCurrencyOption(
  val code: String,
  val displayName: String,
  val symbol: String,
  val isDefault: Boolean,
  val hasLocalQuote: Boolean,
)

sealed interface ConverterResultState {
  data object Empty : ConverterResultState

  data class InvalidInput(val message: UiText) : ConverterResultState

  data class QuoteUnavailable(
    val currencyCode: String,
    val message: UiText,
  ) : ConverterResultState

  data class Calculated(val result: ConversionResult) : ConverterResultState
}

sealed interface ConverterUiState {
  data object Loading : ConverterUiState

  data class Error(
    val message: UiText,
    val isRefreshing: Boolean = false,
  ) : ConverterUiState

  data class Ready(
    val rawAmount: String,
    val sourceCode: String,
    val targetCode: String,
    val options: List<ConverterCurrencyOption>,
    val syncStatus: SyncStatus,
    val referenceTimestampMillis: Long?,
    val resultState: ConverterResultState,
    val isRefreshing: Boolean = false,
    val refreshError: UiText? = null,
  ) : ConverterUiState
}
