package com.aistudio.dolarap.presentation.format

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class BrazilianFormatterTest {

  @Test
  fun `formats BRL quote rate with comma decimal separator and grouping`() {
    assertEquals("R$ 5,7276", BrazilianFormatter.formatBrlQuoteRate(BigDecimal("5.7276")))
    assertEquals("R$ 1.250,50", BrazilianFormatter.formatBrlQuoteRate(BigDecimal("1250.50")))
  }

  @Test
  fun `formats currency amounts respecting target decimal places including JPY 0 decimals`() {
    assertEquals(
      "R$ 550,00",
      BrazilianFormatter.formatCurrencyAmount(
        amount = BigDecimal("550.00"),
        currencyCode = "BRL",
        symbol = "R$",
        decimalPlaces = 2,
      ),
    )
    assertEquals(
      "¥ 15.200",
      BrazilianFormatter.formatCurrencyAmount(
        amount = BigDecimal("15200"),
        currencyCode = "JPY",
        symbol = "¥",
        decimalPlaces = 0,
      ),
    )
  }

  @Test
  fun `formats positive, negative, and zero percentage changes`() {
    assertEquals("+1,25%", BrazilianFormatter.formatPercentageChange(BigDecimal("1.25")))
    assertEquals("-0,09%", BrazilianFormatter.formatPercentageChange(BigDecimal("-0.09")))
    assertEquals("0,00%", BrazilianFormatter.formatPercentageChange(BigDecimal.ZERO))
  }
}
