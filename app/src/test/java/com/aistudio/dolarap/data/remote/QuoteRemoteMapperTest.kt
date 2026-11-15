package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.data.remote.dto.ApiErrorDto
import com.aistudio.dolarap.data.remote.dto.QuoteDto
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteRemoteMapperTest {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val mapper = QuoteRemoteMapper()

  @Test
  fun `deserializes AwesomeAPI multi-pair JSON and preserves exact decimal strings`() {
    val json =
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
          "bid": "5.727600",
          "ask": "5.728200",
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

    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, QuoteDto::class.java)
    val adapter = moshi.adapter<Map<String, QuoteDto>>(mapType)
    val dtoMap = adapter.fromJson(json)!!

    val entities = mapper.mapToEntities(dtoMap, fetchedAtMillis = 1_700_000_000_000L)

    assertEquals(2, entities.size)
    val usd = entities.first { it.pairCode == "USD-BRL" }
    assertEquals("USD", usd.code)
    assertEquals("BRL", usd.codeIn)
    assertEquals("5.727600", usd.bid)
    assertEquals("5.728200", usd.ask)
    assertEquals("-0.0054", usd.varBid)
    assertEquals("-0.09", usd.pctChange)
    assertEquals("5.7340", usd.high)
    assertEquals("5.7279", usd.low)
    assertEquals(1618315045L, usd.apiTimestampSeconds)
    assertEquals("2021-04-13 08:57:27", usd.createDate)
    assertEquals(1_700_000_000_000L, usd.fetchedAtMillis)
  }

  @Test
  fun `rejects incomplete or non-numeric quote DTOs safely`() {
    val missingBid = QuoteDto(code = "USD", codein = "BRL", bid = null, ask = "5.70")
    val invalidBid = QuoteDto(code = "USD", codein = "BRL", bid = "N/A", ask = "5.70")
    val missingCode = QuoteDto(code = "", codein = "BRL", bid = "5.69", ask = "5.70")

    assertNull(mapper.mapSingleOrNull(missingBid, 1000L))
    assertNull(mapper.mapSingleOrNull(invalidBid, 1000L))
    assertNull(mapper.mapSingleOrNull(missingCode, 1000L))
  }

  @Test
  fun `deserializes CoinNotExists ApiErrorDto`() {
    val errorJson =
      """{"status":404,"code":"CoinNotExists","message":"moeda nao encontrada XYZ-BRL"}"""
    val errorDto = moshi.adapter(ApiErrorDto::class.java).fromJson(errorJson)!!

    assertEquals(404, errorDto.status)
    assertEquals("CoinNotExists", errorDto.code)
    assertEquals("moeda nao encontrada XYZ-BRL", errorDto.message)
  }
}
