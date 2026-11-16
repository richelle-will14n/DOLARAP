package com.aistudio.dolarap.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AwesomeApiAuthInterceptorTest {

  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `adds x-api-key header with configured credential and preserves existing headers`() {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
    val client =
      OkHttpClient.Builder()
        .addInterceptor(AwesomeApiAuthInterceptor { "  sk_test_fake_credential_123  " })
        .build()

    val request =
      Request.Builder()
        .url(mockWebServer.url("/json/last/USD-BRL"))
        .header("Accept", "application/json")
        .header("X-Custom-Header", "dolarap-client")
        .build()

    client.newCall(request).execute().use { response ->
      assertEquals(200, response.code)
    }

    val recorded = mockWebServer.takeRequest()
    assertEquals("sk_test_fake_credential_123", recorded.getHeader("x-api-key"))
    assertEquals("application/json", recorded.getHeader("Accept"))
    assertEquals("dolarap-client", recorded.getHeader("X-Custom-Header"))
  }

  @Test
  fun `omits x-api-key header when key is null, blank, or placeholder while preserving other headers`() {
    val unconfiguredKeys =
      listOf(null, "", "   ", AwesomeApiConfig.PLACEHOLDER_API_KEY)

    unconfiguredKeys.forEach { candidateKey ->
      mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
      val client =
        OkHttpClient.Builder()
          .addInterceptor(AwesomeApiAuthInterceptor { candidateKey })
          .build()

      val request =
        Request.Builder()
          .url(mockWebServer.url("/json/last/USD-BRL"))
          .header("Accept", "application/json")
          .build()

      client.newCall(request).execute().use { response ->
        assertEquals(200, response.code)
      }

      val recorded = mockWebServer.takeRequest()
      assertNull(recorded.getHeader("x-api-key"))
      assertEquals("application/json", recorded.getHeader("Accept"))
    }
  }

  @Test
  fun `Retrofit AwesomeApiService sends x-api-key via OkHttp interceptor and redacts header in logs`() =
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
                "high": "5.7340",
                "low": "5.7279",
                "varBid": "-0.0054",
                "pctChange": "-0.09",
                "bid": "5.7276",
                "ask": "5.7282",
                "timestamp": "1618315045",
                "create_date": "2021-04-13 08:57:27"
              }
            }
            """
              .trimIndent()
          )
      )
      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody("""<?xml version="1.0"?><xml><USD-BRL>Dólar Americano/Real Brasileiro</USD-BRL></xml>""")
      )

      val logLines = mutableListOf<String>()
      val logging =
        HttpLoggingInterceptor { message -> logLines.add(message) }.apply {
          redactHeader(AwesomeApiConfig.HEADER_API_KEY)
          level = HttpLoggingInterceptor.Level.HEADERS
        }

      val client =
        OkHttpClient.Builder()
          .addInterceptor(AwesomeApiAuthInterceptor { "sk_test_retrofit_secret" })
          .addInterceptor(logging)
          .build()

      val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
      val service =
        Retrofit.Builder()
          .baseUrl(mockWebServer.url("/"))
          .client(client)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(AwesomeApiService::class.java)

      val quotesResponse = service.getLastQuotes("USD-BRL")
      assertTrue(quotesResponse.isSuccessful)
      assertEquals("5.7276", quotesResponse.body()?.get("USDBRL")?.bid)

      val xmlResponse = service.getAvailablePairsXml()
      assertTrue(xmlResponse.isSuccessful)

      val firstRequest = mockWebServer.takeRequest()
      assertEquals("/json/last/USD-BRL", firstRequest.path)
      assertEquals("sk_test_retrofit_secret", firstRequest.getHeader("x-api-key"))

      val secondRequest = mockWebServer.takeRequest()
      assertEquals("/xml/available", secondRequest.path)
      assertEquals("sk_test_retrofit_secret", secondRequest.getHeader("x-api-key"))

      assertTrue(logLines.any { it.contains("x-api-key: ██", ignoreCase = true) })
      assertFalse(logLines.any { it.contains("sk_test_retrofit_secret") })
    }
}
