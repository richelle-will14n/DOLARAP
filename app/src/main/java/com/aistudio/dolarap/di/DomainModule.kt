package com.aistudio.dolarap.di

import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.usecase.AddCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ConvertCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ObserveAvailableCurrenciesUseCase
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshCatalogUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import org.koin.dsl.module

val domainModule = module {
  single { QuoteFreshnessEvaluator() }
  single { CurrencyConverter() }

  factory { ObserveDashboardQuotesUseCase(quoteRepository = get()) }
  factory { RefreshQuotesUseCase(quoteRepository = get(), syncScheduler = getOrNull()) }
  factory { ObserveAvailableCurrenciesUseCase(catalogRepository = get()) }
  factory { SearchAvailableCurrenciesUseCase(catalogRepository = get()) }
  factory { RefreshCatalogUseCase(catalogRepository = get()) }
  factory { AddCurrencyUseCase(catalogRepository = get(), quoteRepository = get()) }
  factory { RemoveCurrencyUseCase(quoteRepository = get()) }
  factory { ToggleFavoriteUseCase(quoteRepository = get()) }
  factory {
    ConvertCurrencyUseCase(
      quoteRepository = get(),
      currencyConverter = get(),
      freshnessEvaluator = get(),
      timeProvider = get(),
    )
  }
}
