package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.BuildConfig
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.squareup.moshi.Moshi
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveAwesomeApiDiagnosticTest {

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `diagnostic single request to AwesomeAPI with existing auth interceptor`() = runBlocking {
    val koin = GlobalContext.get()
    val baseClient = koin.get<OkHttpClient>()
    val moshi = koin.get<Moshi>()

    val expectedKey = BuildConfig.AWESOME_API_KEY.trim()
    var observedUrl: String? = null
    var headerPresent = false
    var headerMatchesBuildConfig = false
    var headerLength = 0
    var rawHttpCode: Int? = null
    var rawHttpMessage: String? = null
    val safeHeaders = linkedMapOf<String, String>()
    var rawBodyPreview: String? = null
    var networkError: String? = null

    val diagnosticClient =
      baseClient.newBuilder()
        .addInterceptor { chain ->
          val request = chain.request()
          observedUrl = request.url.toString()
          val sentHeader = request.header(AwesomeApiConfig.HEADER_API_KEY)
          headerPresent = sentHeader != null
          headerLength = sentHeader?.length ?: 0
          headerMatchesBuildConfig = sentHeader != null && sentHeader == expectedKey

          val response = chain.proceed(request)
          rawHttpCode = response.code
          rawHttpMessage = response.message

          response.headers.names().forEach { name ->
            val lower = name.lowercase()
            if (!lower.contains("key") && !lower.contains("auth") && !lower.contains("token")) {
              val value = response.headers.values(name).joinToString("; ")
              safeHeaders[name] =
                if (expectedKey.isNotEmpty()) value.replace(expectedKey, "[REDACTED]") else value
            }
          }

          val rawBytes = response.body?.bytes() ?: ByteArray(0)
          val contentType = response.body?.contentType()
          val rawText = rawBytes.toString(Charsets.UTF_8)
          val sanitizedText =
            if (expectedKey.isNotEmpty()) rawText.replace(expectedKey, "[REDACTED]") else rawText
          rawBodyPreview = sanitizedText.take(600)

          response.newBuilder()
            .body(rawBytes.toResponseBody(contentType))
            .build()
        }
        .build()

    val service =
      Retrofit.Builder()
        .baseUrl(AwesomeApiConfig.BASE_URL)
        .client(diagnosticClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AwesomeApiService::class.java)

    val retrofitResultSummary =
      try {
        val response = service.getLastQuotes("USD-BRL")
        val bid = response.body()?.get("USDBRL")?.bid
        val ask = response.body()?.get("USDBRL")?.ask
        "isSuccessful=${response.isSuccessful}, code=${response.code()}, bid=$bid, ask=$ask"
      } catch (e: IOException) {
        networkError = "${e::class.java.simpleName}: ${e.message}"
        "IOException: $networkError"
      } catch (e: Exception) {
        networkError = "${e::class.java.simpleName}: ${e.message}"
        "Exception: $networkError"
      }

    println("AWESOME_DIAG_START")
    println("url=$observedUrl")
    println("buildConfigKeyLength=${expectedKey.length}")
    println("buildConfigIsPlaceholder=${expectedKey == AwesomeApiConfig.PLACEHOLDER_API_KEY}")
    println("xApiKeyHeaderPresent=$headerPresent")
    println("xApiKeyHeaderMatchesBuildConfig=$headerMatchesBuildConfig")
    println("xApiKeyHeaderLength=$headerLength")
    println("rawHttpCode=$rawHttpCode")
    println("rawHttpMessage=$rawHttpMessage")
    println("safeHeaders=$safeHeaders")
    println("rawBodyPreview=$rawBodyPreview")
    println("retrofitResult=$retrofitResultSummary")
    println("networkError=$networkError")
    println("AWESOME_DIAG_END")

    assertTrue("Expected x-api-key header to be attached", headerPresent && headerMatchesBuildConfig)
  }
}
