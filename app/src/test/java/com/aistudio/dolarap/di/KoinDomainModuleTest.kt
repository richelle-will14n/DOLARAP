package com.aistudio.dolarap.di

import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.TimeProvider
import com.aistudio.dolarap.domain.usecase.AddCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ConvertCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ObserveAvailableCurrenciesUseCase
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshCatalogUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinDomainModuleTest {

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `Koin resolves repositories, domain services, and all UseCases without circular dependencies`() {
    val koin = GlobalContext.get()

    val quoteRepo1 = koin.get<QuoteRepository>()
    val quoteRepo2 = koin.get<QuoteRepository>()
    val catalogRepo = koin.get<CatalogRepository>()
    val timeProvider = koin.get<TimeProvider>()
    val converter = koin.get<CurrencyConverter>()
    val evaluator = koin.get<QuoteFreshnessEvaluator>()

    assertSame(quoteRepo1, quoteRepo2)
    assertNotNull(catalogRepo)
    assertNotNull(timeProvider)
    assertNotNull(converter)
    assertNotNull(evaluator)

    assertNotNull(koin.get<ObserveDashboardQuotesUseCase>())
    assertNotNull(koin.get<RefreshQuotesUseCase>())
    assertNotNull(koin.get<ObserveAvailableCurrenciesUseCase>())
    assertNotNull(koin.get<SearchAvailableCurrenciesUseCase>())
    assertNotNull(koin.get<RefreshCatalogUseCase>())
    assertNotNull(koin.get<AddCurrencyUseCase>())
    assertNotNull(koin.get<RemoveCurrencyUseCase>())
    assertNotNull(koin.get<ToggleFavoriteUseCase>())
    assertNotNull(koin.get<ConvertCurrencyUseCase>())
  }
}
