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
  fun `fetchLastQuotes rejects non-three-letter or non-BRL pairs locally without HTTP call`() =
    runBlocking {
      val result =
        dataSource.fetchLastQuotes(listOf("USDT-BRL", "US-BRL", "123-BRL", "EUR-USD", "USD-BRLT"))

      assertTrue(result is RemoteResult.Error)
      assertTrue((result as RemoteResult.Error).error is RemoteError.InvalidRequest)
      assertEquals(0, mockWebServer.requestCount)
    }

  @Test
  fun `fetchLastQuotes handles 404 CoinNotExists in multi-pair batch, preserves valid pairs, and skips invalid pair on next request`() =
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
          ),
          AvailablePairEntity(
            pairCode = "XYZ-BRL",
            baseCode = "XYZ",
            quoteCode = "BRL",
            apiDescription = "Teste",
            friendlyName = "Teste",
            isAvailable = true,
            updatedAtMillis = 100L,
          ),
          AvailablePairEntity(
            pairCode = "EUR-BRL",
            baseCode = "EUR",
            quoteCode = "BRL",
            apiDescription = "Euro/Real Brasileiro",
            friendlyName = "Euro",
            isAvailable = true,
            updatedAtMillis = 100L,
          ),
        )
      )

      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(404)
          .setBody(
            """{"status":404,"code":"CoinNotExists","message":"moeda nao encontrada XYZ-BRL"}"""
          )
      )

      val firstAttempt = dataSource.fetchLastQuotes(listOf("USD-BRL", "XYZ-BRL", "EUR-BRL"))
      assertTrue(firstAttempt is RemoteResult.Error)
      val coinError = (firstAttempt as RemoteResult.Error).error as RemoteError.CoinNotExists
      assertEquals("XYZ-BRL", coinError.pairCode)
      assertFalse(availablePairDao.isPairSupported("XYZ-BRL"))
      assertTrue(availablePairDao.isPairSupported("USD-BRL"))
      assertTrue(availablePairDao.isPairSupported("EUR-BRL"))
      assertEquals("/json/last/USD-BRL,XYZ-BRL,EUR-BRL", mockWebServer.takeRequest().path)

      // Próxima consulta em lote preserva os pares válidos e não repete XYZ-BRL
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
                "high": "5.73",
                "low": "5.72",
                "varBid": "0.01",
                "pctChange": "0.17",
                "bid": "5.7250",
                "ask": "5.7260",
                "timestamp": "1618315045",
                "create_date": "2021-04-13 08:57:27"
              },
              "EURBRL": {
                "code": "EUR",
                "codein": "BRL",
                "name": "Euro/Real Brasileiro",
                "high": "6.85",
                "low": "6.82",
                "varBid": "0.01",
                "pctChange": "0.15",
                "bid": "6.8390",
                "ask": "6.8420",
                "timestamp": "1618315048",
                "create_date": "2021-04-13 08:57:28"
              }
            }
            """
              .trimIndent()
          )
      )

      val secondAttempt =
        dataSource.fetchLastQuotes(listOf("USD-BRL", "XYZ-BRL", "EUR-BRL"), fetchedAtMillis = 999L)
      assertEquals("/json/last/USD-BRL,EUR-BRL", mockWebServer.takeRequest().path)
      assertTrue(secondAttempt is RemoteResult.Success)
      assertEquals(
        listOf("USD-BRL", "EUR-BRL"),
        (secondAttempt as RemoteResult.Success).data.map { it.pairCode },
      )
      assertEquals(2, mockWebServer.requestCount)

      val blockedSingle = dataSource.fetchLastQuotes(listOf("XYZ-BRL"))
      assertTrue((blockedSingle as RemoteResult.Error).error is RemoteError.CoinNotExists)
      assertEquals(2, mockWebServer.requestCount)
    }

  @Test
  fun `fetchLastQuotes does not mark any pair invalid when 404 CoinNotExists batch message omits pair code`() =
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
          ),
          AvailablePairEntity(
            pairCode = "EUR-BRL",
            baseCode = "EUR",
            quoteCode = "BRL",
            apiDescription = "Euro/Real Brasileiro",
            friendlyName = "Euro",
            isAvailable = true,
            updatedAtMillis = 100L,
          ),
        )
      )

      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(404)
          .setBody("""{"status":404,"code":"CoinNotExists","message":"moeda nao encontrada"}""")
      )

      val result = dataSource.fetchLastQuotes(listOf("USD-BRL", "EUR-BRL"))
      assertTrue(result is RemoteResult.Error)
      val coinError = (result as RemoteResult.Error).error as RemoteError.CoinNotExists
      assertEquals(null, coinError.pairCode)
      assertTrue(availablePairDao.isPairSupported("USD-BRL"))
      assertTrue(availablePairDao.isPairSupported("EUR-BRL"))
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
  fun `syncAvailableBrlCatalog persists experimental JSON catalog when valid`() = runBlocking {
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
  fun `syncAvailableBrlCatalog falls back to official xml available when json is invalid and persists in Room`() =
    runBlocking {
      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{invalid-json"))
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
              <USDT-BRL>Tether/Real Brasileiro</USDT-BRL>
            </xml>
            """
              .trimIndent()
          )
      )

      val result = dataSource.syncAvailableBrlCatalog(updatedAtMillis = 888L)

      assertEquals("/json/available", mockWebServer.takeRequest().path)
      assertEquals("/xml/available", mockWebServer.takeRequest().path)
      assertEquals(2, mockWebServer.requestCount)
      assertTrue(result is RemoteResult.Success)
      val stored = availablePairDao.observeSupportedBrlPairs().first()
      assertEquals(listOf("CAD-BRL", "USD-BRL"), stored.map { it.pairCode })
    }

  @Test
  fun `syncAvailableBrlCatalog does NOT trigger XML fallback on HTTP 429 or HTTP 500 and preserves Room cache`() =
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

      mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"status":429}"""))
      val rateLimitResult = dataSource.syncAvailableBrlCatalog()
      assertEquals(429, ((rateLimitResult as RemoteResult.Error).error as RemoteError.HttpError).httpCode)
      assertEquals(1, mockWebServer.requestCount)
      assertEquals(1, availablePairDao.countSupportedBrlPairs())

      mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
      val serverErrorResult = dataSource.syncAvailableBrlCatalog()
      assertEquals(
        500,
        ((serverErrorResult as RemoteResult.Error).error as RemoteError.ServerError).httpCode,
      )
      assertEquals(2, mockWebServer.requestCount)
      assertEquals(1, availablePairDao.countSupportedBrlPairs())
    }

  @Test
  fun `syncAvailableBrlCatalog preserves existing cached pairs when both JSON and XML payloads are invalid`() =
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

      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{invalid-json"))
      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("<xml><broken>"))

      val result = dataSource.syncAvailableBrlCatalog()
      assertTrue(result is RemoteResult.Error)
      assertTrue((result as RemoteResult.Error).error is RemoteError.InvalidResponse)
      assertEquals(2, mockWebServer.requestCount)
      assertEquals(1, availablePairDao.countSupportedBrlPairs())
      assertTrue(availablePairDao.isPairSupported("USD-BRL"))
    }
}
