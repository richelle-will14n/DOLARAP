package com.aistudio.dolarap.di

import com.aistudio.dolarap.presentation.converter.ConverterViewModel
import com.aistudio.dolarap.presentation.quotes.QuotesViewModel
import com.aistudio.dolarap.presentation.search.CurrencySearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
  viewModel {
    QuotesViewModel(
      observeDashboardQuotesUseCase = get(),
      refreshQuotesUseCase = get(),
      toggleFavoriteUseCase = get(),
      removeCurrencyUseCase = get(),
      syncScheduler = getOrNull(),
    )
  }

  viewModel {
    CurrencySearchViewModel(
      searchAvailableCurrenciesUseCase = get(),
      refreshCatalogUseCase = get(),
      addCurrencyUseCase = get(),
      removeCurrencyUseCase = get(),
    )
  }

  viewModel {
    ConverterViewModel(
      observeDashboardQuotesUseCase = get(),
      convertCurrencyUseCase = get(),
      refreshQuotesUseCase = get(),
    )
  }
}
