package com.aistudio.dolarap.presentation.quotes

import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.presentation.format.UiText

sealed interface QuotesUiState {
  data object Loading : QuotesUiState

  data class Empty(
    val favoritesOnly: Boolean,
    val syncStatus: SyncStatus,
    val isRefreshing: Boolean,
    val bannerError: UiText? = null,
    val feedbackMessage: UiText? = null,
  ) : QuotesUiState

  data class Success(
    val items: List<CurrencyQuote>,
    val syncStatus: SyncStatus,
    val favoritesOnly: Boolean,
    val isRefreshing: Boolean,
    val bannerError: UiText? = null,
    val feedbackMessage: UiText? = null,
  ) : QuotesUiState

  data class Error(
    val message: UiText,
    val isRefreshing: Boolean = false,
  ) : QuotesUiState
}
