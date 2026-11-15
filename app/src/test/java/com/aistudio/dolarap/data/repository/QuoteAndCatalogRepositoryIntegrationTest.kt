package com.aistudio.dolarap.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.remote.AwesomeApiRemoteDataSource
import com.aistudio.dolarap.data.remote.AwesomeApiService
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.TimeProvider
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class QuoteAndCatalogRepositoryIntegrationTest {

  private lateinit var mockWebServer: MockWebServer
  private lateinit var database: DolarapDatabase
  private lateinit var quoteRepository: QuoteRepositoryImpl
  private lateinit var catalogRepository: CatalogRepositoryImpl

  private var fakeNowMillis = 1_700_000_000_000L
  private val timeProvider = TimeProvider { fakeNowMillis }

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, DolarapDatabase::class.java)
        .addCallback(DolarapDatabase.seedCallback())
        .allowMainThreadQueries()
        .build()

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    val service =
      Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AwesomeApiService::class.java)

    val remoteDataSource =
      AwesomeApiRemoteDataSource(
        service = service,
        moshi = moshi,
        quoteRemoteMapper = QuoteRemoteMapper(),
        availableCatalogParser = AvailableCatalogParser(moshi),
        availablePairDao = database.availablePairDao(),
      )

    val freshnessEvaluator = QuoteFreshnessEvaluator()
    val entityMapper = QuoteEntityMapper(freshnessEvaluator)
    val errorMapper = RemoteErrorMapper()

    quoteRepository =
      QuoteRepositoryImpl(
        currencyDao = database.currencyDao(),
        quoteDao = database.quoteDao(),
        syncMetadataDao = database.syncMetadataDao(),
        remoteDataSource = remoteDataSource,
        quoteEntityMapper = entityMapper,
        remoteErrorMapper = errorMapper,
        timeProvider = timeProvider,
      )

    catalogRepository =
      CatalogRepositoryImpl(
        availablePairDao = database.availablePairDao(),
        currencyDao = database.currencyDao(),
        syncMetadataDao = database.syncMetadataDao(),
        remoteDataSource = remoteDataSource,
        quoteEntityMapper = entityMapper,
        remoteErrorMapper = errorMapper,
        timeProvider = timeProvider,
      )
  }

  @After
  fun tearDown() {
    database.close()
    mockWebServer.shutdown()
  }

  @Test
  fun `observes 10 default currencies via Room Flow when no local quotes exist yet`() = runBlocking {
    val snapshot = quoteRepository.observeDashboardQuotes().first()

    assertEquals(10, snapshot.items.size)
    assertTrue(snapshot.items.all { it.quote == null })
    assertTrue(snapshot.items.all { it.freshnessStatus == QuoteFreshnessStatus.UNAVAILABLE })
    assertTrue(snapshot.syncStatus.hasNeverSynced)
  }

  @Test
  fun `refreshQuotes persists API response to Room, emits via Flow, and preserves cache on network failure`() =
    runBlocking {
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(
            """
            {
              "USDBRL": {
                "code": "USD",
                "codein": "BRL",
                "name": "Dólar Americano/Real Brasileiro",
                "high": "5.7400",
                "low": "5.7100",
                "varBid": "-0.0054",
                "pctChange": "-0.09",
                "bid": "5.7276",
                "ask": "5.7282",
                "timestamp": "1700000000",
                "create_date": "2026-11-15 10:00:00"
              }
            }
            """
              .trimIndent()
          )
      )

      val refreshSuccess = quoteRepository.refreshQuotes()
      assertTrue(refreshSuccess is DomainResult.Success)

      val freshSnapshot = quoteRepository.observeDashboardQuotes().first()
      val usdItem = freshSnapshot.items.first { it.code == "USD" }
      assertNotNull(usdItem.quote)
      assertEquals(BigDecimal("5.7276"), usdItem.quote?.bid)
      assertEquals(BigDecimal("5.7282"), usdItem.quote?.ask)
      assertEquals(QuoteFreshnessStatus.FRESH, usdItem.freshnessStatus)
      assertEquals(fakeNowMillis, freshSnapshot.syncStatus.lastSuccessfulSyncAtMillis)
      assertNull(freshSnapshot.syncStatus.lastErrorType)

      // Avança 16 minutos e simula falha de rede (HTTP 500)
      fakeNowMillis += 16L * 60L * 1000L
      mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

      val refreshFailure = quoteRepository.refreshQuotes()
      assertTrue(refreshFailure is DomainResult.Error)
      assertTrue((refreshFailure as DomainResult.Error).error is DomainError.ApiUnavailable)

      // Cache do Room continua preservado e agora marcado como STALE
      val staleSnapshot = quoteRepository.observeDashboardQuotes().first()
      val cachedUsd = staleSnapshot.items.first { it.code == "USD" }
      assertEquals(BigDecimal("5.7276"), cachedUsd.quote?.bid)
      assertEquals(QuoteFreshnessStatus.STALE, cachedUsd.freshnessStatus)
      assertTrue(staleSnapshot.syncStatus.isStale)
      assertNotNull(staleSnapshot.syncStatus.lastErrorType)
    }

  @Test
  fun `coalesces concurrent refreshQuotes calls into a single HTTP request`() = runBlocking {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBodyDelay(150, TimeUnit.MILLISECONDS)
        .setBody(
          """
          {
            "USDBRL": {
              "code": "USD",
              "codein": "BRL",
              "name": "Dólar Americano/Real Brasileiro",
              "high": "5.74",
              "low": "5.71",
              "varBid": "0.01",
              "pctChange": "0.10",
              "bid": "5.73",
              "ask": "5.74",
              "timestamp": "1700000000",
              "create_date": "2026-11-15 10:00:00"
            }
          }
          """
            .trimIndent()
        )
    )

    val results =
      listOf(
        async { quoteRepository.refreshQuotes() },
        async { quoteRepository.refreshQuotes() },
        async { quoteRepository.refreshQuotes() },
      )
        .awaitAll()

    assertTrue(results.all { it is DomainResult.Success })
    assertEquals(1, mockWebServer.requestCount)
  }

  @Test
  fun `syncs catalog, filters via search, validates supported pair addition, and persists favorites`() =
    runBlocking {
      mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <xml>
              <USD-BRL>Dólar Americano/Real Brasileiro</USD-BRL>
              <CLP-BRL>Peso Chileno/Real Brasileiro</CLP-BRL>
            </xml>
            """
              .trimIndent()
          )
      )

      val syncCatalog = catalogRepository.refreshCatalog()
      assertEquals(2, (syncCatalog as DomainResult.Success).data)

      val searchResults = SearchAvailableCurrenciesUseCase(catalogRepository)("chileno").first()
      assertEquals(listOf("CLP"), searchResults.map { it.code })

      // Rejeita par não suportado no catálogo
      val unsupportedAdd = catalogRepository.addCurrency("ZZZ")
      assertEquals(DomainError.UnsupportedPair("ZZZ-BRL"), (unsupportedAdd as DomainResult.Error).error)

      // Adiciona par validado CLP
      val validAdd = catalogRepository.addCurrency("CLP")
      assertTrue(validAdd is DomainResult.Success)
      assertTrue(quoteRepository.observeDashboardQuotes().first().items.any { it.code == "CLP" })

      // Favoritar e observar filtro de favoritos
      assertTrue(quoteRepository.toggleFavorite("USD") is DomainResult.Success)
      assertEquals(listOf("USD"), quoteRepository.observeFavoriteQuotes().first().items.map { it.code })

      // Protege moeda padrão e permite remover moeda adicional
      assertTrue(
        (quoteRepository.removeSelectedCurrency("USD") as DomainResult.Error).error
          is DomainError.DefaultCurrencyProtected
      )
      assertTrue(quoteRepository.removeSelectedCurrency("CLP") is DomainResult.Success)
      assertTrue(quoteRepository.observeDashboardQuotes().first().items.none { it.code == "CLP" })
    }
}
