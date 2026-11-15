package com.aistudio.dolarap.presentation.search

import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.usecase.AddCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.RefreshCatalogUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.aistudio.dolarap.presentation.FakeCatalogRepository
import com.aistudio.dolarap.presentation.FakeQuoteRepository
import com.aistudio.dolarap.presentation.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CurrencySearchViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private fun createViewModel(
    quoteRepository: FakeQuoteRepository = FakeQuoteRepository(),
    catalogRepository: FakeCatalogRepository = FakeCatalogRepository(quoteRepository = quoteRepository),
    syncIfEmptyOnStart: Boolean = false,
  ): CurrencySearchViewModel =
    CurrencySearchViewModel(
      searchAvailableCurrenciesUseCase = SearchAvailableCurrenciesUseCase(catalogRepository),
      refreshCatalogUseCase = RefreshCatalogUseCase(catalogRepository),
      addCurrencyUseCase = AddCurrencyUseCase(catalogRepository, quoteRepository),
      removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepository),
      syncIfEmptyOnStart = syncIfEmptyOnStart,
    )

  @Test
  fun `search filters currencies ignoring case and accents`() = runTest {
    val viewModel = createViewModel()
    val job = launch { viewModel.uiState.collect {} }

    viewModel.onQueryChange("suico")
    val filtered = viewModel.uiState.first {
      it is CurrencySearchUiState.Success && it.currencies.size == 1
    } as CurrencySearchUiState.Success

    assertEquals("CHF", filtered.currencies.first().code)

    viewModel.onQueryChange("inexistente")
    val emptyQuery = viewModel.uiState.first {
      it is CurrencySearchUiState.Empty
    } as CurrencySearchUiState.Empty
    assertFalse(emptyQuery.isCatalogEmpty)

    viewModel.clearQuery()
    val restored = viewModel.uiState.first {
      it is CurrencySearchUiState.Success && it.currencies.size == 4
    } as CurrencySearchUiState.Success
    assertEquals("", restored.query)
    job.cancel()
  }

  @Test
  fun `addCurrency and removeCurrency update selection and protect default currencies`() = runTest {
    val quoteRepo = FakeQuoteRepository()
    val catalogRepo = FakeCatalogRepository(quoteRepository = quoteRepo)
    val viewModel = createViewModel(quoteRepo, catalogRepo)
    val job = launch { viewModel.uiState.collect {} }

    viewModel.addCurrency("NOK")
    val addedState = viewModel.uiState.first {
      it is CurrencySearchUiState.Success &&
        it.currencies.first { c -> c.code == "NOK" }.isSelected
    } as CurrencySearchUiState.Success
    assertEquals(R.string.search_feedback_added, addedState.feedbackMessage?.resId)

    // Tentar adicionar novamente retorna proteção contra duplicidade
    viewModel.addCurrency("NOK")
    val duplicateErrorState = viewModel.uiState.first {
      it is CurrencySearchUiState.Success && it.errorMessage != null
    } as CurrencySearchUiState.Success
    assertEquals(R.string.error_currency_already_selected, duplicateErrorState.errorMessage?.resId)

    // Moeda padrão não pode ser removida
    viewModel.removeCurrency("USD")
    val protectedState = viewModel.uiState.first {
      it is CurrencySearchUiState.Success &&
        it.errorMessage?.resId == R.string.error_default_currency_protected
    } as CurrencySearchUiState.Success
    assertEquals(R.string.error_default_currency_protected, protectedState.errorMessage?.resId)

    // Moeda adicional pode ser removida
    catalogRepo.markUnselected("NOK")
    viewModel.removeCurrency("NOK")
    val removedState = viewModel.uiState.first {
      it is CurrencySearchUiState.Success &&
        it.feedbackMessage?.resId == R.string.search_feedback_removed
    } as CurrencySearchUiState.Success
    assertEquals(R.string.search_feedback_removed, removedState.feedbackMessage?.resId)
    job.cancel()
  }

  @Test
  fun `empty catalog shows Empty state offline or Error state after failed refresh`() = runTest {
    val catalogRepo = FakeCatalogRepository(initialCatalog = emptyList())
    catalogRepo.refreshCatalogResult = DomainResult.Error(DomainError.NoConnection)

    val viewModel = createViewModel(catalogRepository = catalogRepo, syncIfEmptyOnStart = false)
    val job = launch { viewModel.uiState.collect {} }

    val initialEmpty = viewModel.uiState.first {
      it is CurrencySearchUiState.Empty
    } as CurrencySearchUiState.Empty
    assertTrue(initialEmpty.isCatalogEmpty)

    viewModel.refreshCatalog()
    val errorState = viewModel.uiState.first {
      it is CurrencySearchUiState.Error
    } as CurrencySearchUiState.Error
    assertEquals(R.string.error_no_connection, errorState.message.resId)
    job.cancel()
  }
}
