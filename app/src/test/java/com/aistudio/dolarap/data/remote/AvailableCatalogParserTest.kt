package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.data.remote.model.RemoteError
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailableCatalogParserTest {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val parser = AvailableCatalogParser(moshi)

  @Test
  fun `parseJsonToBrlPairs filters strictly BRL pairs and enriches friendly name`() {
    val json =
      """
      {
        "USD-BRL": "Dólar Americano/Real Brasileiro",
        "CHF-BRL": "Franco Suíço/Real Brasileiro",
        "EUR-USD": "Euro/Dólar Americano",
        "USD-BRLT": "Dólar Americano/Real Brasileiro Turismo",
        "USD-BRLPTAX": "Dólar Americano/Real Brasileiro PTAX",
        "INVALID": "Ignorar"
      }
      """
        .trimIndent()

    val result = parser.parseJsonToBrlPairs(json, updatedAtMillis = 5000L)

    assertTrue(result is RemoteResult.Success)
    val pairs = (result as RemoteResult.Success).data
    assertEquals(listOf("CHF-BRL", "USD-BRL"), pairs.map { it.pairCode })
    assertEquals("Franco Suíço", pairs.first { it.baseCode == "CHF" }.friendlyName)
    assertEquals(5000L, pairs.first().updatedAtMillis)
  }

  @Test
  fun `parseJsonToBrlPairs rejects XML, blank, or malformed JSON`() {
    val xmlPayload = "<xml><USD-BRL>Dólar Americano/Real Brasileiro</USD-BRL></xml>"
    val malformed = """{"USD-BRL": "Dólar"""

    val xmlResult = parser.parseJsonToBrlPairs(xmlPayload, 1000L)
    val emptyResult = parser.parseJsonToBrlPairs("   ", 1000L)
    val malformedResult = parser.parseJsonToBrlPairs(malformed, 1000L)

    assertTrue((xmlResult as RemoteResult.Error).error is RemoteError.InvalidResponse)
    assertTrue((emptyResult as RemoteResult.Error).error is RemoteError.EmptyOrIncompleteResponse)
    assertTrue((malformedResult as RemoteResult.Error).error is RemoteError.InvalidResponse)
  }

  @Test
  fun `parseXmlToBrlPairs parses standard AwesomeAPI tag-per-pair XML and filters BRL`() {
    val xml =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <xml>
        <USD-BRL>Dólar Americano/Real Brasileiro</USD-BRL>
        <CLP-BRL>Peso Chileno/Real Brasileiro</CLP-BRL>
        <BTC-USD>Bitcoin/Dólar Americano</BTC-USD>
        <USD-BRLT>Dólar Americano/Real Brasileiro Turismo</USD-BRLT>
      </xml>
      """
        .trimIndent()

    val result = parser.parseXmlToBrlPairs(xml, updatedAtMillis = 9000L)

    assertTrue(result is RemoteResult.Success)
    val pairs = (result as RemoteResult.Success).data
    assertEquals(listOf("CLP-BRL", "USD-BRL"), pairs.map { it.pairCode })
    assertEquals("Peso Chileno", pairs.first { it.baseCode == "CLP" }.friendlyName)
  }

  @Test
  fun `parseXmlToBrlPairs also supports item element structure and rejects invalid XML`() {
    val itemXml =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <xml>
        <item>
          <code>UYU</code>
          <codein>BRL</codein>
          <name>Peso Uruguaio/Real Brasileiro</name>
        </item>
        <item>
          <code>EUR</code>
          <codein>USD</codein>
          <name>Euro/Dólar Americano</name>
        </item>
      </xml>
      """
        .trimIndent()

    val success = parser.parseXmlToBrlPairs(itemXml, 1000L)
    assertTrue(success is RemoteResult.Success)
    assertEquals(listOf("UYU-BRL"), (success as RemoteResult.Success).data.map { it.pairCode })

    val broken = parser.parseXmlToBrlPairs("<xml><USD-BRL>", 1000L)
    assertTrue((broken as RemoteResult.Error).error is RemoteError.InvalidResponse)
  }
}
