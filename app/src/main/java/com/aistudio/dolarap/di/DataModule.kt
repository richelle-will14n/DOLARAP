package com.aistudio.dolarap.di

import androidx.work.WorkerParameters
import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.repository.CatalogRepositoryImpl
import com.aistudio.dolarap.data.repository.QuoteRepositoryImpl
import com.aistudio.dolarap.data.time.SystemTimeProvider
import com.aistudio.dolarap.data.worker.SyncQuotesWorker
import com.aistudio.dolarap.data.worker.WorkManagerSyncScheduler
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.domain.service.TimeProvider
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val dataModule = module {
  single<TimeProvider> { SystemTimeProvider }
  single { QuoteEntityMapper(freshnessEvaluator = get()) }
  single { RemoteErrorMapper() }
  single<SyncScheduler> { WorkManagerSyncScheduler(context = androidContext()) }

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

  worker { (params: WorkerParameters) ->
    SyncQuotesWorker(
      appContext = androidContext(),
      workerParams = params,
      quoteRepository = get(),
      syncScheduler = getOrNull(),
    )
  }
}
