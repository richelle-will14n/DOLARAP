package com.aistudio.dolarap.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.usecase.AddCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.RefreshCatalogUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.aistudio.dolarap.presentation.format.DomainErrorMapper
import com.aistudio.dolarap.presentation.format.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencySearchViewModel(
  private val searchAvailableCurrenciesUseCase: SearchAvailableCurrenciesUseCase,
  private val refreshCatalogUseCase: RefreshCatalogUseCase,
  private val addCurrencyUseCase: AddCurrencyUseCase,
  private val removeCurrencyUseCase: RemoveCurrencyUseCase,
  syncIfEmptyOnStart: Boolean = true,
) : ViewModel() {

  private val query = MutableStateFlow("")
  private val isRefreshing = MutableStateFlow(false)
  private val pendingCodes = MutableStateFlow<Set<String>>(emptySet())
  private val errorMessage = MutableStateFlow<UiText?>(null)
  private val feedbackMessage = MutableStateFlow<UiText?>(null)

  private sealed interface CatalogStream {
    data object Loading : CatalogStream
    data class Data(val items: List<AvailableCurrency>) : CatalogStream
    data class Failed(val message: UiText) : CatalogStream
  }

  private val catalogStream =
    query.flatMapLatest { searchQuery ->
      searchAvailableCurrenciesUseCase(searchQuery)
        .map<List<AvailableCurrency>, CatalogStream> { CatalogStream.Data(it) }
        .catch { emit(CatalogStream.Failed(UiText(R.string.error_unexpected))) }
    }

  val uiState: StateFlow<CurrencySearchUiState> =
    combine(
      query,
      catalogStream,
      pendingCodes,
      isRefreshing,
      combine(errorMessage, feedbackMessage) { err, ok -> err to ok },
    ) { currentQuery, stream, pending, refreshing, messages ->
      val (err, feedback) = messages
      when (stream) {
        CatalogStream.Loading -> CurrencySearchUiState.Loading(currentQuery)
        is CatalogStream.Failed ->
          CurrencySearchUiState.Error(
            query = currentQuery,
            message = stream.message,
            isRefreshing = refreshing,
          )
        is CatalogStream.Data -> {
          val items = stream.items
          when {
            items.isEmpty() && currentQuery.isBlank() && err != null ->
              CurrencySearchUiState.Error(
                query = currentQuery,
                message = err,
                isRefreshing = refreshing,
              )
            items.isEmpty() ->
              CurrencySearchUiState.Empty(
                query = currentQuery,
                isCatalogEmpty = currentQuery.isBlank(),
                isRefreshing = refreshing,
                errorMessage = err,
                feedbackMessage = feedback,
              )
            else ->
              CurrencySearchUiState.Success(
                query = currentQuery,
                currencies = items,
                pendingCodes = pending,
                isRefreshing = refreshing,
                errorMessage = err,
                feedbackMessage = feedback,
              )
          }
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = CurrencySearchUiState.Loading(),
    )

  init {
    if (syncIfEmptyOnStart) {
      viewModelScope.launch {
        val current = searchAvailableCurrenciesUseCase("").firstOrNull()
        if (current.isNullOrEmpty()) {
          refreshCatalog()
        }
      }
    }
  }

  fun onQueryChange(newQuery: String) {
    query.value = newQuery
  }

  fun clearQuery() {
    query.value = ""
  }

  fun refreshCatalog() {
    if (isRefreshing.value) return
    viewModelScope.launch {
      isRefreshing.value = true
      errorMessage.value = null
      when (val result = refreshCatalogUseCase()) {
        is DomainResult.Success -> {
          feedbackMessage.value =
            UiText(R.string.search_feedback_catalog_synced, listOf(result.data))
        }
        is DomainResult.Error -> {
          errorMessage.value = DomainErrorMapper.toUiText(result.error)
        }
      }
      isRefreshing.value = false
    }
  }

  fun addCurrency(code: String) {
    val normalized = CurrencyDefaults.normalizeCode(code)
    if (pendingCodes.value.contains(normalized)) return

    viewModelScope.launch {
      pendingCodes.update { it + normalized }
      errorMessage.value = null
      when (val result = addCurrencyUseCase(normalized)) {
        is DomainResult.Success -> {
          feedbackMessage.value = UiText(R.string.search_feedback_added, listOf(normalized))
        }
        is DomainResult.Error -> {
          errorMessage.value = DomainErrorMapper.toUiText(result.error)
        }
      }
      pendingCodes.update { it - normalized }
    }
  }

  fun removeCurrency(code: String) {
    val normalized = CurrencyDefaults.normalizeCode(code)
    if (pendingCodes.value.contains(normalized)) return

    viewModelScope.launch {
      pendingCodes.update { it + normalized }
      errorMessage.value = null
      when (val result = removeCurrencyUseCase(normalized)) {
        is DomainResult.Success -> {
          feedbackMessage.value = UiText(R.string.search_feedback_removed, listOf(normalized))
        }
        is DomainResult.Error -> {
          errorMessage.value = DomainErrorMapper.toUiText(result.error)
        }
      }
      pendingCodes.update { it - normalized }
    }
  }

  fun dismissMessages() {
    errorMessage.value = null
    feedbackMessage.value = null
  }
}
