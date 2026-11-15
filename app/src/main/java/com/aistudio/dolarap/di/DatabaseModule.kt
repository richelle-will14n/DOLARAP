package com.aistudio.dolarap.di

import com.aistudio.dolarap.data.local.DolarapDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
  single { DolarapDatabase.build(androidContext()) }
  single { get<DolarapDatabase>().currencyDao() }
  single { get<DolarapDatabase>().quoteDao() }
  single { get<DolarapDatabase>().availablePairDao() }
  single { get<DolarapDatabase>().syncMetadataDao() }
}
