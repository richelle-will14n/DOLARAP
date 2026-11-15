package com.aistudio.dolarap.presentation.quotes

import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import com.aistudio.dolarap.presentation.FakeQuoteRepository
import com.aistudio.dolarap.presentation.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuotesViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private fun createViewModel(
    quoteRepository: FakeQuoteRepository,
    refreshOnStart: Boolean = false,
  ): QuotesViewModel =
    QuotesViewModel(
      observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(quoteRepository),
      refreshQuotesUseCase = RefreshQuotesUseCase(quoteRepository),
      toggleFavoriteUseCase = ToggleFavoriteUseCase(quoteRepository),
      removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepository),
      refreshOnStart = refreshOnStart,
    )

  @Test
  fun `loads initial 10 default currencies in Success state`() = runTest {
    val repo = FakeQuoteRepository()
    val viewModel = createViewModel(repo, refreshOnStart = false)

    val job = launch { viewModel.uiState.collect {} }
    val state = viewModel.uiState.first { it is QuotesUiState.Success } as QuotesUiState.Success

    assertEquals(10, state.items.size)
    assertFalse(state.favoritesOnly)
    assertEquals(QuoteFreshnessStatus.FRESH, state.syncStatus.freshnessStatus)
    job.cancel()
  }

  @Test
  fun `emits Empty state when favorites filter is active without favorites`() = runTest {
    val repo = FakeQuoteRepository()
    val viewModel = createViewModel(repo, refreshOnStart = false)

    val job = launch { viewModel.uiState.collect {} }
    viewModel.setFavoritesOnly(true)

    val state = viewModel.uiState.first { it is QuotesUiState.Empty } as QuotesUiState.Empty
    assertTrue(state.favoritesOnly)
    job.cancel()
  }

  @Test
  fun `emits Error state when observing quotes fails`() = runTest {
    val repo = FakeQuoteRepository().apply { failObservingFlow = true }
    val viewModel = createViewModel(repo, refreshOnStart = false)

    val job = launch { viewModel.uiState.collect {} }
    val state = viewModel.uiState.first { it is QuotesUiState.Error } as QuotesUiState.Error
    assertEquals(R.string.error_unexpected, state.message.resId)
    job.cancel()
  }

  @Test
  fun `manual refresh preserves existing data and exposes non-destructive banner error on failure`() =
    runTest {
      val repo = FakeQuoteRepository()
      repo.refreshQuotesResult = DomainResult.Error(DomainError.NoConnection)
      val viewModel = createViewModel(repo, refreshOnStart = false)

      val job = launch { viewModel.uiState.collect {} }
      viewModel.uiState.first { it is QuotesUiState.Success }

      viewModel.refreshQuotes()

      val updated = viewModel.uiState.first {
        it is QuotesUiState.Success && it.bannerError != null
      } as QuotesUiState.Success
      assertEquals(10, updated.items.size)
      assertEquals(R.string.error_no_connection, updated.bannerError?.resId)
      assertEquals(1, repo.refreshCallCount)

      viewModel.dismissBannerError()
      val cleared = viewModel.uiState.first {
        it is QuotesUiState.Success && it.bannerError == null
      } as QuotesUiState.Success
      assertNull(cleared.bannerError)
      job.cancel()
    }

  @Test
  fun `toggleFavorite updates currency favorite state and populates favorites filter`() = runTest {
    val repo = FakeQuoteRepository()
    val viewModel = createViewModel(repo, refreshOnStart = false)

    val job = launch { viewModel.uiState.collect {} }
    viewModel.toggleFavorite("USD")
    viewModel.setFavoritesOnly(true)

    val favState = viewModel.uiState.first {
      it is QuotesUiState.Success && it.favoritesOnly
    } as QuotesUiState.Success

    assertEquals(1, favState.items.size)
    assertEquals("USD", favState.items.first().code)
    assertTrue(favState.items.first().isFavorite)
    job.cancel()
  }

  @Test
  fun `removeCurrency protects default currencies and removes custom currency`() = runTest {
    val initial = FakeQuoteRepository.defaultSnapshot()
    val customItem =
      CurrencyQuote(
        code = "NOK",
        pairCode = "NOK-BRL",
        displayName = "Coroa Norueguesa",
        symbol = "kr",
        isDefault = false,
        isFavorite = false,
        sortOrder = 10,
        quote = null,
        freshnessStatus = QuoteFreshnessStatus.UNAVAILABLE,
      )
    val repo =
      FakeQuoteRepository(
        DashboardQuotesSnapshot(items = initial.items + customItem, syncStatus = initial.syncStatus)
      )
    val viewModel = createViewModel(repo, refreshOnStart = false)
    val job = launch { viewModel.uiState.collect {} }

    viewModel.removeCurrency("USD")
    val protectedState = viewModel.uiState.first {
      it is QuotesUiState.Success && it.bannerError != null
    } as QuotesUiState.Success
    assertEquals(R.string.error_default_currency_protected, protectedState.bannerError?.resId)
    assertEquals(11, protectedState.items.size)

    viewModel.removeCurrency("NOK")
    val afterRemove = viewModel.uiState.first {
      it is QuotesUiState.Success && it.items.size == 10
    } as QuotesUiState.Success
    assertNotNull(afterRemove.feedbackMessage)
    assertEquals(R.string.search_feedback_removed, afterRemove.feedbackMessage?.resId)
    job.cancel()
  }
}
