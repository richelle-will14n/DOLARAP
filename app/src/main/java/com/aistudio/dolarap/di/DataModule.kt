package com.aistudio.dolarap.di

import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.repository.CatalogRepositoryImpl
import com.aistudio.dolarap.data.repository.QuoteRepositoryImpl
import com.aistudio.dolarap.data.time.SystemTimeProvider
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.TimeProvider
import org.koin.dsl.module

val dataModule = module {
  single<TimeProvider> { SystemTimeProvider }
  single { QuoteEntityMapper(freshnessEvaluator = get()) }
  single { RemoteErrorMapper() }

  single<QuoteRepository> {
    QuoteRepositoryImpl(
      currencyDao = get(),
      quoteDao = get(),
      syncMetadataDao = get(),
      remoteDataSource = get(),
      quoteEntityMapper = get(),
      remoteErrorMapper = get(),
      timeProvider = get(),
    )
  }

  single<CatalogRepository> {
    CatalogRepositoryImpl(
      availablePairDao = get(),
      currencyDao = get(),
      syncMetadataDao = get(),
      remoteDataSource = get(),
      quoteEntityMapper = get(),
      remoteErrorMapper = get(),
      timeProvider = get(),
    )
  }
}
