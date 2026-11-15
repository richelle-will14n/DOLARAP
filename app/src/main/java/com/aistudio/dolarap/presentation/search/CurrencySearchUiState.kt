package com.aistudio.dolarap.presentation.search

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.presentation.format.UiText

sealed interface CurrencySearchUiState {
  val query: String

  data class Loading(
    override val query: String = "",
  ) : CurrencySearchUiState

  data class Empty(
    override val query: String,
    val isCatalogEmpty: Boolean,
    val isRefreshing: Boolean,
    val errorMessage: UiText? = null,
    val feedbackMessage: UiText? = null,
  ) : CurrencySearchUiState

  data class Error(
    override val query: String,
    val message: UiText,
    val isRefreshing: Boolean = false,
  ) : CurrencySearchUiState

  data class Success(
    override val query: String,
    val currencies: List<AvailableCurrency>,
    val pendingCodes: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    val errorMessage: UiText? = null,
    val feedbackMessage: UiText? = null,
  ) : CurrencySearchUiState
}
