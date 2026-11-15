package com.aistudio.dolarap.di

import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.presentation.MainDispatcherRule
import com.aistudio.dolarap.presentation.converter.ConverterViewModel
import com.aistudio.dolarap.presentation.quotes.QuotesViewModel
import com.aistudio.dolarap.presentation.search.CurrencySearchViewModel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinPresentationModuleTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `Koin resolves all presentation ViewModels using existing domain and data modules`() {
    val koin = GlobalContext.get()

    val quotesViewModel = koin.get<QuotesViewModel>()
    val searchViewModel = koin.get<CurrencySearchViewModel>()
    val converterViewModel = koin.get<ConverterViewModel>()

    assertNotNull(quotesViewModel)
    assertNotNull(searchViewModel)
    assertNotNull(converterViewModel)
  }
}
