package com.aistudio.dolarap.presentation.converter

import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.TimeProvider
import com.aistudio.dolarap.domain.usecase.ConvertCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.presentation.FakeQuoteRepository
import com.aistudio.dolarap.presentation.MainDispatcherRule
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConverterViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private fun createViewModel(
    quoteRepository: FakeQuoteRepository,
    nowMillis: Long = 1_700_000_100_000L,
  ): ConverterViewModel {
    val convertUseCase =
      ConvertCurrencyUseCase(
        quoteRepository = quoteRepository,
        currencyConverter = CurrencyConverter(),
        freshnessEvaluator = QuoteFreshnessEvaluator(),
        timeProvider = TimeProvider { nowMillis },
      )
    return ConverterViewModel(
      observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(quoteRepository),
      convertCurrencyUseCase = convertUseCase,
      refreshQuotesUseCase = RefreshQuotesUseCase(quoteRepository),
    )
  }

  @Test
  fun `includes BRL, 10 defaults, and custom currencies and calculates via ConvertCurrencyUseCase`() =
    runTest {
      val base = FakeQuoteRepository.defaultSnapshot()
      val custom =
        CurrencyQuote(
          code = "NOK",
          pairCode = "NOK-BRL",
          displayName = "Coroa Norueguesa",
          symbol = "kr",
          isDefault = false,
          isFavorite = false,
          sortOrder = 10,
          quote = FakeQuoteRepository.sampleQuoteRate("NOK", "0.5000"),
          freshnessStatus = QuoteFreshnessStatus.FRESH,
        )
      val repo =
        FakeQuoteRepository(DashboardQuotesSnapshot(items = base.items + custom, syncStatus = base.syncStatus))
      val viewModel = createViewModel(repo)
      val job = launch { viewModel.uiState.collect {} }

      viewModel.onAmountChange("100,00")
      val state = viewModel.uiState.first {
        it is ConverterUiState.Ready &&
          it.resultState is ConverterResultState.Calculated &&
          (it.resultState as ConverterResultState.Calculated).result.inputAmount.compareTo(BigDecimal("100.00")) == 0
      } as ConverterUiState.Ready

      // BRL + 10 padrões + 1 adicional = 12 opções
      assertEquals(12, state.options.size)
      assertEquals("BRL", state.options.first().code)
      assertTrue(state.options.any { it.code == "NOK" })

      val calculated = (state.resultState as ConverterResultState.Calculated).result
      // USD bid = 5.5000 -> 100 USD = 550.00 BRL
      assertEquals(0, BigDecimal("550.00").compareTo(calculated.convertedAmount))
      assertEquals(0, BigDecimal("5.5000").compareTo(calculated.effectiveRate))

      // Troca para moeda adicional NOK como destino (USD -> NOK via BRL = 5.5 / 0.5 = 11 -> 1100.00 NOK)
      viewModel.selectTargetCurrency("NOK")
      val crossState = viewModel.uiState.first {
        it is ConverterUiState.Ready &&
          it.targetCode == "NOK" &&
          it.resultState is ConverterResultState.Calculated
      } as ConverterUiState.Ready
      val crossResult = (crossState.resultState as ConverterResultState.Calculated).result
      assertEquals(0, BigDecimal("1100.00").compareTo(crossResult.convertedAmount))
      job.cancel()
    }

  @Test
  fun `swapCurrencies inverts source and target and recalculates`() = runTest {
    val repo = FakeQuoteRepository()
    val viewModel = createViewModel(repo)
    val job = launch { viewModel.uiState.collect {} }

    viewModel.onAmountChange("55")
    viewModel.swapCurrencies()

    val swapped = viewModel.uiState.first {
      it is ConverterUiState.Ready && it.sourceCode == "BRL" && it.targetCode == "USD"
    } as ConverterUiState.Ready

    val result = (swapped.resultState as ConverterResultState.Calculated).result
    // 55 BRL / 5.5000 = 10.00 USD
    assertEquals(0, BigDecimal("10.00").compareTo(result.convertedAmount))
    job.cancel()
  }

  @Test
  fun `handles blank input, invalid amount, unavailable quote, and stale quote warning`() = runTest {
    val fetchedAt = 1_700_000_000_000L
    // 20 minutos depois -> stale (> 15 min)
    val nowStale = fetchedAt + 20 * 60 * 1000L
    val repo = FakeQuoteRepository(FakeQuoteRepository.defaultSnapshot(fetchedAtMillis = fetchedAt))
    val viewModel = createViewModel(repo, nowMillis = nowStale)
    val job = launch { viewModel.uiState.collect {} }

    val staleState = viewModel.uiState.first {
      it is ConverterUiState.Ready && it.resultState is ConverterResultState.Calculated
    } as ConverterUiState.Ready
    assertTrue((staleState.resultState as ConverterResultState.Calculated).result.isStale)

    viewModel.onAmountChange("")
    val emptyState = viewModel.uiState.first {
      it is ConverterUiState.Ready && it.resultState is ConverterResultState.Empty
    } as ConverterUiState.Ready
    assertTrue(emptyState.resultState is ConverterResultState.Empty)

    viewModel.onAmountChange("-10")
    val invalidState = viewModel.uiState.first {
      it is ConverterUiState.Ready && it.resultState is ConverterResultState.InvalidInput
    } as ConverterUiState.Ready
    assertEquals(
      R.string.converter_invalid_amount_message,
      (invalidState.resultState as ConverterResultState.InvalidInput).message.resId,
    )

    // Sem cotação local disponível para USD
    repo.setSnapshot(FakeQuoteRepository.defaultSnapshot(withQuotes = false))
    viewModel.onAmountChange("10")
    val unavailableState = viewModel.uiState.first {
      it is ConverterUiState.Ready && it.resultState is ConverterResultState.QuoteUnavailable
    } as ConverterUiState.Ready
    assertEquals(
      "USD",
      (unavailableState.resultState as ConverterResultState.QuoteUnavailable).currencyCode,
    )
    job.cancel()
  }
}
