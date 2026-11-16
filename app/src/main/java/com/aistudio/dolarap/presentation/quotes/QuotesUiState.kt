package com.aistudio.dolarap.presentation.quotes

import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.presentation.format.UiText

enum class QuotesSyncPhase {
  WAITING_FIRST_SYNC,
  SYNCING,
  COMPLETED,
  OFFLINE,
  FAILED,
}

sealed interface QuotesUiState {
  data object Loading : QuotesUiState

  data class Empty(
    val favoritesOnly: Boolean,
    val syncStatus: SyncStatus,
    val isRefreshing: Boolean,
    val bannerError: UiText? = null,
    val feedbackMessage: UiText? = null,
    val syncPhase: QuotesSyncPhase = QuotesSyncPhase.WAITING_FIRST_SYNC,
    val isBackgroundRetryScheduled: Boolean = false,
  ) : QuotesUiState

  data class Success(
    val items: List<CurrencyQuote>,
    val syncStatus: SyncStatus,
    val favoritesOnly: Boolean,
    val isRefreshing: Boolean,
    val bannerError: UiText? = null,
    val feedbackMessage: UiText? = null,
    val syncPhase: QuotesSyncPhase = QuotesSyncPhase.WAITING_FIRST_SYNC,
    val isBackgroundRetryScheduled: Boolean = false,
  ) : QuotesUiState

  data class Error(
    val message: UiText,
    val isRefreshing: Boolean = false,
  ) : QuotesUiState
}
