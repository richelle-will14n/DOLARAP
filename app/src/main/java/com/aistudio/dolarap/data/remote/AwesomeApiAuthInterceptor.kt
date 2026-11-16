package com.aistudio.dolarap.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AwesomeApiAuthInterceptor(
  private val apiKeyProvider: () -> String?,
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val normalizedKey = apiKeyProvider()?.trim().orEmpty()
    val isConfigured =
      normalizedKey.isNotEmpty() && normalizedKey != AwesomeApiConfig.PLACEHOLDER_API_KEY

    val request =
      if (isConfigured && originalRequest.header(AwesomeApiConfig.HEADER_API_KEY) == null) {
        originalRequest.newBuilder()
          .header(AwesomeApiConfig.HEADER_API_KEY, normalizedKey)
          .build()
      } else {
        originalRequest
      }

    return chain.proceed(request)
  }
}
