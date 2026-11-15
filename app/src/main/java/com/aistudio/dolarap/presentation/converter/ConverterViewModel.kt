package com.aistudio.dolarap.presentation.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.usecase.ConvertCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.presentation.format.DomainErrorMapper
import com.aistudio.dolarap.presentation.format.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConverterViewModel(
  observeDashboardQuotesUseCase: ObserveDashboardQuotesUseCase,
  private val convertCurrencyUseCase: ConvertCurrencyUseCase,
  private val refreshQuotesUseCase: RefreshQuotesUseCase,
) : ViewModel() {

  private val rawAmount = MutableStateFlow(DEFAULT_AMOUNT)
  private val sourceCode = MutableStateFlow(DEFAULT_SOURCE_CODE)
  private val targetCode = MutableStateFlow(CurrencyDefaults.REFERENCE_CURRENCY_CODE)
  private val isRefreshing = MutableStateFlow(false)
  private val refreshError = MutableStateFlow<UiText?>(null)

  private sealed interface QuotesStream {
    data object Loading : QuotesStream
    data class Data(val snapshot: DashboardQuotesSnapshot) : QuotesStream
    data class Failed(val message: UiText) : QuotesStream
  }

  private val quotesStream =
    observeDashboardQuotesUseCase(favoritesOnly = false)
      .map<DashboardQuotesSnapshot, QuotesStream> { QuotesStream.Data(it) }
      .catch { emit(QuotesStream.Failed(UiText(R.string.error_unexpected))) }

  val uiState: StateFlow<ConverterUiState> =
    combine(
      quotesStream,
      rawAmount,
      combine(sourceCode, targetCode) { src, tgt -> src to tgt },
      isRefreshing,
      refreshError,
    ) { stream, amountText, pair, refreshing, syncErr ->
      when (stream) {
        QuotesStream.Loading -> ConverterUiState.Loading
        is QuotesStream.Failed ->
          ConverterUiState.Error(message = stream.message, isRefreshing = refreshing)
        is QuotesStream.Data -> {
          val snapshot = stream.snapshot
          val brlOption =
            ConverterCurrencyOption(
              code = CurrencyDefaults.REFERENCE_CURRENCY_CODE,
              displayName = CurrencyDefaults.REFERENCE_CURRENCY_NAME,
              symbol = CurrencyDefaults.REFERENCE_CURRENCY_SYMBOL,
              isDefault = true,
              hasLocalQuote = true,
            )
          val options =
            listOf(brlOption) +
              snapshot.items.map { item ->
                ConverterCurrencyOption(
                  code = item.code,
                  displayName = item.displayName,
                  symbol = item.symbol,
                  isDefault = item.isDefault,
                  hasLocalQuote = item.quote != null,
                )
              }

          val validCodes = options.mapTo(HashSet()) { it.code }
          val resolvedSource =
            if (validCodes.contains(pair.first)) pair.first else DEFAULT_SOURCE_CODE
          val resolvedTarget =
            if (validCodes.contains(pair.second)) pair.second
            else CurrencyDefaults.REFERENCE_CURRENCY_CODE

          val quotesByCode =
            snapshot.items
              .mapNotNull { item -> item.quote?.let { rate -> item.code to rate } }
              .toMap()

          val resultState =
            if (amountText.isBlank()) {
              ConverterResultState.Empty
            } else {
              when (
                val conversion =
                  convertCurrencyUseCase.convertFromSnapshot(
                    rawAmount = amountText,
                    sourceCode = resolvedSource,
                    targetCode = resolvedTarget,
                    quotesByCode = quotesByCode,
                  )
              ) {
                is DomainResult.Success -> ConverterResultState.Calculated(conversion.data)
                is DomainResult.Error ->
                  when (val err = conversion.error) {
                    DomainError.InvalidAmount ->
                      ConverterResultState.InvalidInput(DomainErrorMapper.toUiText(err))
                    is DomainError.QuoteUnavailable ->
                      ConverterResultState.QuoteUnavailable(
                        currencyCode = err.currencyCode,
                        message = DomainErrorMapper.toUiText(err),
                      )
                    else -> ConverterResultState.InvalidInput(DomainErrorMapper.toUiText(err))
                  }
              }
            }

          val relevantTimestamps =
            listOfNotNull(
              quotesByCode[resolvedSource]?.fetchedAtMillis,
              quotesByCode[resolvedTarget]?.fetchedAtMillis,
            )
          val referenceTimestamp =
            relevantTimestamps.minOrNull() ?: snapshot.syncStatus.lastSuccessfulSyncAtMillis

          ConverterUiState.Ready(
            rawAmount = amountText,
            sourceCode = resolvedSource,
            targetCode = resolvedTarget,
            options = options,
            syncStatus = snapshot.syncStatus,
            referenceTimestampMillis = referenceTimestamp,
            resultState = resultState,
            isRefreshing = refreshing,
            refreshError = syncErr,
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = ConverterUiState.Loading,
    )

  fun onAmountChange(newAmount: String) {
    rawAmount.value = newAmount
  }

  fun selectSourceCurrency(code: String) {
    sourceCode.value = CurrencyDefaults.normalizeCode(code)
  }

  fun selectTargetCurrency(code: String) {
    targetCode.value = CurrencyDefaults.normalizeCode(code)
  }

  fun swapCurrencies() {
    val currentSource = sourceCode.value
    val currentTarget = targetCode.value
    sourceCode.value = currentTarget
    targetCode.value = currentSource
  }

  fun refreshQuotes() {
    if (isRefreshing.value) return
    viewModelScope.launch {
      isRefreshing.value = true
      refreshError.value = null
      when (val result = refreshQuotesUseCase()) {
        is DomainResult.Success -> Unit
        is DomainResult.Error -> refreshError.value = DomainErrorMapper.toUiText(result.error)
      }
      isRefreshing.value = false
    }
  }

  fun dismissRefreshError() {
    refreshError.value = null
  }

  private companion object {
    const val DEFAULT_AMOUNT = "1"
    const val DEFAULT_SOURCE_CODE = "USD"
  }
}
