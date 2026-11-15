package com.aistudio.dolarap.domain.service

import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteRate
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

  private val converter = CurrencyConverter()

  private val quotes =
    mapOf(
      "USD" to
        quoteRate(
          code = "USD",
          bid = "5.7276",
          ask = "5.7282",
        ),
      "EUR" to
        quoteRate(
          code = "EUR",
          bid = "6.2500",
          ask = "6.2550",
        ),
      "JPY" to
        quoteRate(
          code = "JPY",
          bid = "0.0375",
          ask = "0.0378",
        ),
      "BTC" to
        quoteRate(
          code = "BTC",
          bid = "500000.00",
          ask = "500500.00",
        ),
    )

  @Test
  fun `converts foreign currency to BRL using bid rate and keeps ask complementary`() {
    val result = converter.convert("100.00", "USD", "BRL", quotes)

    assertTrue(result is DomainResult.Success)
    val conversion = (result as DomainResult.Success).data
    assertEquals(BigDecimal("572.76"), conversion.convertedAmount)
    assertEquals(BigDecimal("5.7276"), conversion.effectiveRate)
    assertEquals(BigDecimal("5.7276"), conversion.sourceBidRate)
    assertEquals(BigDecimal("5.7282"), conversion.sourceAskRate)
    assertNull(conversion.targetBidRate)
    assertEquals(2, conversion.decimalPlaces)
  }

  @Test
  fun `converts BRL to foreign currency using bid rate with HALF_UP rounding`() {
    val result = converter.convert("1000.00", "BRL", "USD", quotes)

    assertTrue(result is DomainResult.Success)
    val conversion = (result as DomainResult.Success).data
    // 1000 / 5.7276 = 174.593197849... -> 174.59
    assertEquals(BigDecimal("174.59"), conversion.convertedAmount)
    assertEquals(BigDecimal("5.7276"), conversion.targetBidRate)
    assertEquals(BigDecimal("5.7282"), conversion.targetAskRate)
    assertNull(conversion.sourceBidRate)
    assertEquals(2, conversion.decimalPlaces)
  }

  @Test
  fun `converts between two foreign currencies via BRL using bid rates`() {
    val result = converter.convert("100", "USD", "EUR", quotes)

    assertTrue(result is DomainResult.Success)
    val conversion = (result as DomainResult.Success).data
    // (100 * 5.7276) / 6.2500 = 572.76 / 6.25 = 91.6416 -> 91.64
    assertEquals(BigDecimal("91.64"), conversion.convertedAmount)
    assertEquals(BigDecimal("5.7276"), conversion.sourceBidRate)
    assertEquals(BigDecimal("6.2500"), conversion.targetBidRate)
  }

  @Test
  fun `applies target currency decimal places for JPY 0 decimals and BTC 8 decimals`() {
    val jpyResult = converter.convert("100", "BRL", "JPY", quotes) as DomainResult.Success
    // 100 / 0.0375 = 2666.6666... -> 2667 (0 casas decimais para JPY)
    assertEquals(0, jpyResult.data.decimalPlaces)
    assertEquals(BigDecimal("2667"), jpyResult.data.convertedAmount)

    val btcResult = converter.convert("1000", "BRL", "BTC", quotes) as DomainResult.Success
    // 1000 / 500000 = 0.00200000 (8 casas decimais para BTC)
    assertEquals(8, btcResult.data.decimalPlaces)
    assertEquals(BigDecimal("0.00200000"), btcResult.data.convertedAmount)
  }

  @Test
  fun `handles same currency, zero input, and comma decimal input`() {
    val same = converter.convert("123,45", "USD", "USD", quotes) as DomainResult.Success
    assertEquals(BigDecimal("123.45"), same.data.convertedAmount)
    assertEquals(BigDecimal.ONE, same.data.effectiveRate)

    val zero = converter.convert("0", "USD", "BRL", quotes) as DomainResult.Success
    assertEquals(BigDecimal("0.00"), zero.data.convertedAmount)
  }

  @Test
  fun `rejects missing or non-positive quotes without inventing rates`() {
    val missingResult = converter.convert("10", "CHF", "BRL", quotes)
    assertTrue(missingResult is DomainResult.Error)
    assertEquals(
      DomainError.QuoteUnavailable("CHF"),
      (missingResult as DomainResult.Error).error,
    )

    val zeroBidQuotes = quotes + ("CHF" to quoteRate(code = "CHF", bid = "0", ask = "6.50"))
    val zeroBidResult = converter.convert("10", "BRL", "CHF", zeroBidQuotes)
    assertTrue(zeroBidResult is DomainResult.Error)
    assertEquals(
      DomainError.QuoteUnavailable("CHF"),
      (zeroBidResult as DomainResult.Error).error,
    )
  }

  @Test
  fun `rejects invalid, blank, and negative amounts`() {
    assertEquals(
      DomainError.InvalidAmount,
      (converter.convert("-50.00", "USD", "BRL", quotes) as DomainResult.Error).error,
    )
    assertEquals(
      DomainError.InvalidAmount,
      (converter.convert("abc", "USD", "BRL", quotes) as DomainResult.Error).error,
    )
    assertEquals(
      DomainError.InvalidAmount,
      (converter.convert("   ", "USD", "BRL", quotes) as DomainResult.Error).error,
    )
    assertEquals(
      DomainError.InvalidAmount,
      (converter.convert(BigDecimal("-0.01"), "USD", "BRL", quotes) as DomainResult.Error).error,
    )
  }

  private fun quoteRate(
    code: String,
    bid: String,
    ask: String,
  ): QuoteRate =
    QuoteRate(
      pairCode = "$code-BRL",
      code = code,
      codeIn = "BRL",
      apiName = "$code/Real Brasileiro",
      bid = BigDecimal(bid),
      ask = BigDecimal(ask),
      varBid = BigDecimal.ZERO,
      pctChange = BigDecimal.ZERO,
      high = BigDecimal(ask),
      low = BigDecimal(bid),
      apiTimestampSeconds = 1_700_000_000L,
      createDate = "2026-11-15 10:00:00",
      fetchedAtMillis = 1_700_000_000_000L,
    )
}
