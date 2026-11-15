package com.aistudio.dolarap.data.remote

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.local.dao.AvailablePairDao
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.model.RemoteError
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AwesomeApiRemoteDataSourceTest {

  private lateinit var mockWebServer: MockWebServer
  private lateinit var database: DolarapDatabase
  private lateinit var availablePairDao: AvailablePairDao
  private lateinit var moshi: Moshi
  private lateinit var dataSource: AwesomeApiRemoteDataSource

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, DolarapDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    availablePairDao = database.availablePairDao()
    moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    dataSource = buildDataSource(readTimeoutMillis = 2_000L)
  }

  @After
  fun tearDown() {
    database.close()
    mockWebServer.shutdown()
  }

  private fun buildDataSource(readTimeoutMillis: Long): AwesomeApiRemoteDataSource {
    val client =
      OkHttpClient.Builder()
        .connectTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        .writeTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    val retrofit =
      Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service = retrofit.create(AwesomeApiService::class.java)
    return AwesomeApiRemoteDataSource(
      service = service,
      moshi = moshi,
      quoteRemoteMapper = QuoteRemoteMapper(),
      availableCatalogParser = AvailableCatalogParser(moshi),
      availablePairDao = availablePairDao,
    )
  }

  @Test
  fun `fetchLastQuotes returns mapped QuoteEntities for multiple pairs`() = runBlocking {
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
              "high": "5.7340",
              "low": "5.7279",
              "varBid": "-0.0054",
              "pctChange": "-0.09",
              "bid": "5.7276",
              "ask": "5.7282",
              "timestamp": "1618315045",
              "create_date": "2021-04-13 08:57:27"
            },
            "EURBRL": {
              "code": "EUR",
              "codein": "BRL",
              "name": "Euro/Real Brasileiro",
              "high": "6.851",
              "low": "6.825",
              "varBid": "0.012",
              "pctChange": "0.18",
              "bid": "6.839",
              "ask": "6.842",
              "timestamp": "1618315048",
              "create_date": "2021-04-13 08:57:28"
            }
          }
          """
            .trimIndent()
        )
    )

    val result = dataSource.fetchLastQuotes(listOf("usd-brl", "EUR-BRL"), fetchedAtMillis = 12345L)

    val recorded = mockWebServer.takeRequest()
    assertEquals("/json/last/USD-BRL,EUR-BRL", recorded.path)
    assertTrue(result is RemoteResult.Success)
    val entities = (result as RemoteResult.Success).data
    assertEquals(2, entities.size)
    assertEquals("5.7276", entities.first { it.pairCode == "USD-BRL" }.bid)
    assertEquals("6.839", entities.first { it.pairCode == "EUR-BRL" }.bid)
  }

  @Test
  fun `fetchLastQuotes handles 404 CoinNotExists and blocks repeated requests for invalid pair`() =
    runBlocking {
      availablePairDao.upsertAll(
        listOf(
          AvailablePairEntity(
            pairCode = "XYZ-BRL",
            baseCode = "XYZ",
            quoteCode = "BRL",
            apiDescription = "Teste",
            friendlyName = "Teste",
            isAvailable = true,
            updatedAtMillis = 100L,
          )
        )
      )

      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(404)
          .setBody(
            """{"status":404,"code":"CoinNotExists","message":"moeda nao encontrada XYZ-BRL"}"""
          )
      )

      val firstAttempt = dataSource.fetchLastQuotes(listOf("XYZ-BRL"))
      assertTrue(firstAttempt is RemoteResult.Error)
      val coinError = (firstAttempt as RemoteResult.Error).error as RemoteError.CoinNotExists
      assertEquals("XYZ-BRL", coinError.pairCode)
      assertFalse(availablePairDao.isPairSupported("XYZ-BRL"))

      // Segunda tentativa deve ser bloqueada localmente sem enviar nova requisição HTTP
      val secondAttempt = dataSource.fetchLastQuotes(listOf("XYZ-BRL"))
      assertTrue((secondAttempt as RemoteResult.Error).error is RemoteError.CoinNotExists)
      assertEquals(1, mockWebServer.requestCount)
    }

  @Test
  fun `fetchLastQuotes handles HTTP 429, 500, empty JSON, and malformed JSON safely`() =
    runBlocking {
      mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"status":429}"""))
      val rateLimit = dataSource.fetchLastQuotes(listOf("USD-BRL"))
      assertEquals(429, ((rateLimit as RemoteResult.Error).error as RemoteError.HttpError).httpCode)

      mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
      val serverErr = dataSource.fetchLastQuotes(listOf("USD-BRL"))
      assertEquals(500, ((serverErr as RemoteResult.Error).error as RemoteError.ServerError).httpCode)

      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
      val emptyRes = dataSource.fetchLastQuotes(listOf("USD-BRL"))
      assertTrue((emptyRes as RemoteResult.Error).error is RemoteError.EmptyOrIncompleteResponse)

      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{invalid-json"))
      val invalidRes = dataSource.fetchLastQuotes(listOf("USD-BRL"))
      assertTrue((invalidRes as RemoteResult.Error).error is RemoteError.InvalidResponse)
    }

  @Test
  fun `fetchLastQuotes maps socket timeout and connection refusal without crashing`() =
    runBlocking {
      val shortTimeoutSource = buildDataSource(readTimeoutMillis = 200L)
      mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

      val timeoutResult = shortTimeoutSource.fetchLastQuotes(listOf("USD-BRL"))
      assertTrue((timeoutResult as RemoteResult.Error).error is RemoteError.Timeout)

      mockWebServer.shutdown()
      val offlineResult = shortTimeoutSource.fetchLastQuotes(listOf("USD-BRL"))
      assertTrue((offlineResult as RemoteResult.Error).error is RemoteError.NoConnection)
    }

  @Test
  fun `syncAvailableBrlCatalog persists JSON catalog into AvailablePairDao`() = runBlocking {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
          {
            "USD-BRL": "Dólar Americano/Real Brasileiro",
            "CHF-BRL": "Franco Suíço/Real Brasileiro",
            "EUR-USD": "Euro/Dólar Americano"
          }
          """
            .trimIndent()
        )
    )

    val result = dataSource.syncAvailableBrlCatalog(updatedAtMillis = 777L)

    assertTrue(result is RemoteResult.Success)
    assertEquals("/json/available", mockWebServer.takeRequest().path)
    val stored = availablePairDao.observeSupportedBrlPairs().first()
    assertEquals(listOf("CHF-BRL", "USD-BRL"), stored.map { it.pairCode })
    assertEquals("Franco Suíço", stored.first { it.baseCode == "CHF" }.friendlyName)
  }

  @Test
  fun `syncAvailableBrlCatalog falls back to official xml available when json available fails or returns XML`() =
    runBlocking {
      // Simula /json/available retornando 404 (ou não confiável) seguido de /xml/available oficial
      mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <xml>
              <USD-BRL>Dólar Americano/Real Brasileiro</USD-BRL>
              <CAD-BRL>Dólar Canadense/Real Brasileiro</CAD-BRL>
              <EUR-USD>Euro/Dólar Americano</EUR-USD>
            </xml>
            """
              .trimIndent()
          )
      )

      val result = dataSource.syncAvailableBrlCatalog(updatedAtMillis = 888L)

      assertEquals("/json/available", mockWebServer.takeRequest().path)
      assertEquals("/xml/available", mockWebServer.takeRequest().path)
      assertTrue(result is RemoteResult.Success)
      val stored = availablePairDao.observeSupportedBrlPairs().first()
      assertEquals(listOf("CAD-BRL", "USD-BRL"), stored.map { it.pairCode })
    }

  @Test
  fun `syncAvailableBrlCatalog preserves existing cached pairs when both endpoints fail`() =
    runBlocking {
      availablePairDao.upsertAll(
        listOf(
          AvailablePairEntity(
            pairCode = "USD-BRL",
            baseCode = "USD",
            quoteCode = "BRL",
            apiDescription = "Dólar Americano/Real Brasileiro",
            friendlyName = "Dólar Americano",
            isAvailable = true,
            updatedAtMillis = 100L,
          )
        )
      )

      mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Error"))
      mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Error"))

      val result = dataSource.syncAvailableBrlCatalog()
      assertTrue(result is RemoteResult.Error)
      assertTrue((result as RemoteResult.Error).error is RemoteError.ServerError)
      assertEquals(1, availablePairDao.countSupportedBrlPairs())
    }
}
