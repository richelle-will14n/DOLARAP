package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.data.remote.dto.QuoteDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApiService {

  @GET(AwesomeApiConfig.ENDPOINT_LAST_QUOTES)
  suspend fun getLastQuotes(
    @Path("pairs", encoded = true) pairs: String,
  ): Response<Map<String, QuoteDto>>

  @GET(AwesomeApiConfig.ENDPOINT_AVAILABLE_JSON)
  suspend fun getAvailablePairsJson(): Response<ResponseBody>

  @GET(AwesomeApiConfig.ENDPOINT_AVAILABLE_XML)
  suspend fun getAvailablePairsXml(): Response<ResponseBody>
}
