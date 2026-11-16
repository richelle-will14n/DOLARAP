package com.aistudio.dolarap.presentation.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.service.BackgroundSyncInfo
import com.aistudio.dolarap.domain.service.BackgroundSyncState
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import com.aistudio.dolarap.presentation.format.DomainErrorMapper
import com.aistudio.dolarap.presentation.format.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class QuotesViewModel(
  private val observeDashboardQuotesUseCase: ObserveDashboardQuotesUseCase,
  private val refreshQuotesUseCase: RefreshQuotesUseCase,
  private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
  private val removeCurrencyUseCase: RemoveCurrencyUseCase,
  syncScheduler: SyncScheduler? = null,
  refreshOnStart: Boolean = true,
) : ViewModel() {

  private val favoritesOnly = MutableStateFlow(false)
  private val isRefreshing = MutableStateFlow(false)
  private val bannerError = MutableStateFlow<UiText?>(null)
  private val feedbackMessage = MutableStateFlow<UiText?>(null)

  private val backgroundSyncFlow =
    (syncScheduler?.observeSyncState() ?: flowOf(BackgroundSyncInfo()))
      .catch { emit(BackgroundSyncInfo()) }

  private val refreshStateStream =
    combine(isRefreshing, backgroundSyncFlow) { foregroundRefreshing, backgroundSync ->
      foregroundRefreshing to backgroundSync
    }

  private sealed interface SnapshotStream {
    data object Loading : SnapshotStream
    data class Data(val snapshot: DashboardQuotesSnapshot) : SnapshotStream
    data class Failed(val message: UiText) : SnapshotStream
  }

  private val snapshotStream =
    favoritesOnly
      .flatMapLatest { onlyFavorites ->
        observeDashboardQuotesUseCase(favoritesOnly = onlyFavorites)
          .map<DashboardQuotesSnapshot, SnapshotStream> { SnapshotStream.Data(it) }
          .catch { emit(SnapshotStream.Failed(UiText(R.string.error_unexpected))) }
      }

  val uiState: StateFlow<QuotesUiState> =
    combine(
      snapshotStream,
      favoritesOnly,
      refreshStateStream,
      bannerError,
      feedbackMessage,
    ) { stream, onlyFavorites, (foregroundRefreshing, backgroundSync), errorText, feedback ->
      val effectiveRefreshing =
        foregroundRefreshing || backgroundSync.state == BackgroundSyncState.RUNNING
      val retryScheduled =
        backgroundSync.state == BackgroundSyncState.ENQUEUED ||
          backgroundSync.state == BackgroundSyncState.BLOCKED

      when (stream) {
        SnapshotStream.Loading -> QuotesUiState.Loading
        is SnapshotStream.Failed ->
          QuotesUiState.Error(message = stream.message, isRefreshing = effectiveRefreshing)
        is SnapshotStream.Data -> {
          val snapshot = stream.snapshot
          val syncPhase =
            when {
              effectiveRefreshing -> QuotesSyncPhase.SYNCING
              snapshot.syncStatus.lastErrorType == "NO_CONNECTION" -> QuotesSyncPhase.OFFLINE
              snapshot.syncStatus.lastErrorType != null -> QuotesSyncPhase.FAILED
              snapshot.syncStatus.lastSuccessfulSyncAtMillis != null -> QuotesSyncPhase.COMPLETED
              else -> QuotesSyncPhase.WAITING_FIRST_SYNC
            }

          if (snapshot.items.isEmpty()) {
            QuotesUiState.Empty(
              favoritesOnly = onlyFavorites,
              syncStatus = snapshot.syncStatus,
              isRefreshing = effectiveRefreshing,
              bannerError = errorText,
              feedbackMessage = feedback,
              syncPhase = syncPhase,
              isBackgroundRetryScheduled = retryScheduled,
            )
          } else {
            QuotesUiState.Success(
              items = snapshot.items,
              syncStatus = snapshot.syncStatus,
              favoritesOnly = onlyFavorites,
              isRefreshing = effectiveRefreshing,
              bannerError = errorText,
              feedbackMessage = feedback,
              syncPhase = syncPhase,
              isBackgroundRetryScheduled = retryScheduled,
            )
          }
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = QuotesUiState.Loading,
    )

  init {
    if (refreshOnStart) {
      refreshQuotes()
    }
  }

  fun setFavoritesOnly(enabled: Boolean) {
    favoritesOnly.value = enabled
  }

  fun refreshQuotes() {
    if (isRefreshing.value) return
    viewModelScope.launch {
      isRefreshing.value = true
      bannerError.value = null
      when (val result = refreshQuotesUseCase()) {
        is DomainResult.Success -> Unit
        is DomainResult.Error -> bannerError.value = DomainErrorMapper.toUiText(result.error)
      }
      isRefreshing.value = false
    }
  }

  fun toggleFavorite(code: String) {
    viewModelScope.launch {
      when (val result = toggleFavoriteUseCase(code)) {
        is DomainResult.Success -> Unit
        is DomainResult.Error -> bannerError.value = DomainErrorMapper.toUiText(result.error)
      }
    }
  }

  fun removeCurrency(code: String) {
    viewModelScope.launch {
      when (val result = removeCurrencyUseCase(code)) {
        is DomainResult.Success -> {
          feedbackMessage.value = UiText(R.string.search_feedback_removed, listOf(code))
        }
        is DomainResult.Error -> {
          bannerError.value = DomainErrorMapper.toUiText(result.error)
        }
      }
    }
  }

  fun dismissBannerError() {
    bannerError.value = null
  }

  fun dismissFeedback() {
    feedbackMessage.value = null
  }
}
