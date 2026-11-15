package com.aistudio.dolarap.data.local

import com.aistudio.dolarap.data.local.catalog.ComplementaryCurrencyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplementaryCurrencyCatalogTest {

  @Test
  fun `defaultCurrencies contains the exact 10 required currencies in order`() {
    val codes = ComplementaryCurrencyCatalog.defaultCurrencies.map { it.code }
    assertEquals(
      listOf("USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY", "ARS", "CNY", "MXN"),
      codes,
    )
    assertEquals((0..9).toList(), ComplementaryCurrencyCatalog.defaultCurrencies.map { it.sortOrder })
    assertTrue(
      ComplementaryCurrencyCatalog.defaultCurrencies.all { it.pairCode == "${it.code}-BRL" }
    )
  }

  @Test
  fun `isDefaultCurrency identifies default codes case-insensitively and rejects others`() {
    assertTrue(ComplementaryCurrencyCatalog.isDefaultCurrency("usd"))
    assertTrue(ComplementaryCurrencyCatalog.isDefaultCurrency("CHF"))
    assertFalse(ComplementaryCurrencyCatalog.isDefaultCurrency("CLP"))
    assertFalse(ComplementaryCurrencyCatalog.isDefaultCurrency("BRL"))
  }

  @Test
  fun `resolveFriendlyName prioritizes curated PT-BR name and falls back cleanly`() {
    assertEquals(
      "Franco Suíço",
      ComplementaryCurrencyCatalog.resolveFriendlyName("chf", "Ignorado/Real Brasileiro"),
    )
    assertEquals(
      "Moeda Personalizada",
      ComplementaryCurrencyCatalog.resolveFriendlyName("XYZ", "Moeda Personalizada/Real Brasileiro"),
    )
    assertEquals(
      "ZZZ",
      ComplementaryCurrencyCatalog.resolveFriendlyName("zzz", null),
    )
  }

  @Test
  fun `normalizeForSearch removes diacritics and lowercases`() {
    assertEquals(
      "franco suico",
      ComplementaryCurrencyCatalog.normalizeForSearch("  Franco Suíço "),
    )
    assertEquals(
      "dolar americano",
      ComplementaryCurrencyCatalog.normalizeForSearch("Dólar Americano"),
    )
  }
}
