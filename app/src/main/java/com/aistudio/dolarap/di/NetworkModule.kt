package com.aistudio.dolarap.di

import com.aistudio.dolarap.BuildConfig
import com.aistudio.dolarap.data.remote.AwesomeApiAuthInterceptor
import com.aistudio.dolarap.data.remote.AwesomeApiConfig
import com.aistudio.dolarap.data.remote.AwesomeApiRemoteDataSource
import com.aistudio.dolarap.data.remote.AwesomeApiService
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
  single {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  single {
    val logging =
      HttpLoggingInterceptor().apply {
        redactHeader(AwesomeApiConfig.HEADER_API_KEY)
        level =
          if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
          else HttpLoggingInterceptor.Level.NONE
      }

    val authInterceptor = AwesomeApiAuthInterceptor { BuildConfig.AWESOME_API_KEY }

    OkHttpClient.Builder()
      .connectTimeout(AwesomeApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(AwesomeApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(AwesomeApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .addInterceptor(authInterceptor)
      .addInterceptor(logging)
      .build()
  }

  single {
    Retrofit.Builder()
      .baseUrl(AwesomeApiConfig.BASE_URL)
      .client(get())
      .addConverterFactory(MoshiConverterFactory.create(get()))
      .build()
  }

  single { get<Retrofit>().create(AwesomeApiService::class.java) }
  single { QuoteRemoteMapper() }
  single { AvailableCatalogParser(get()) }
  single {
    AwesomeApiRemoteDataSource(
      service = get(),
      moshi = get(),
      quoteRemoteMapper = get(),
      availableCatalogParser = get(),
      availablePairDao = get(),
    )
  }
}
