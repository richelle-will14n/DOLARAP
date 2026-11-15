package com.aistudio.dolarap.domain.service

import com.aistudio.dolarap.domain.model.ConversionResult
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteRate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

/**
 * Calculadora monetária pura baseada exclusivamente em [BigDecimal].
 *
 * Política de precisão e arredondamento:
 * - Moeda de referência: `BRL`.
 * - Taxa padrão de conversão: `bid` (mantendo `ask` apenas como informação complementar).
 * - Operações intermediárias: [MathContext.DECIMAL128] (34 dígitos de precisão).
 * - Resultado convertido: arredondado para a quantidade de casas decimais da moeda de destino
 *   (obtida via ISO 4217 em [java.util.Currency.getDefaultFractionDigits], com fallback explícito
 *   para criptoativos conhecidos) utilizando [RoundingMode.HALF_UP].
 * - Taxa efetiva derivada (`BRL -> Y` e `X -> Y`): escala de 8 casas decimais com [RoundingMode.HALF_UP].
 */
class CurrencyConverter {

  fun convert(
    rawAmount: String,
    sourceCode: String,
    targetCode: String,
    quotesByCode: Map<String, QuoteRate>,
    isStale: Boolean = false,
  ): DomainResult<ConversionResult> {
    val parsedAmount = parseValidAmount(rawAmount)
      ?: return DomainResult.Error(DomainError.InvalidAmount)

    return convert(
      amount = parsedAmount,
      sourceCode = sourceCode,
      targetCode = targetCode,
      quotesByCode = quotesByCode,
      isStale = isStale,
    )
  }

  fun convert(
    amount: BigDecimal,
    sourceCode: String,
    targetCode: String,
    quotesByCode: Map<String, QuoteRate>,
    isStale: Boolean = false,
  ): DomainResult<ConversionResult> {
    if (amount < BigDecimal.ZERO) {
      return DomainResult.Error(DomainError.InvalidAmount)
    }

    val normalizedSource = CurrencyDefaults.normalizeCode(sourceCode)
    val normalizedTarget = CurrencyDefaults.normalizeCode(targetCode)
    if (!CODE_REGEX.matches(normalizedSource) || !CODE_REGEX.matches(normalizedTarget)) {
      return DomainResult.Error(DomainError.UnsupportedPair("$normalizedSource-$normalizedTarget"))
    }

    val targetDecimals = resolveDecimalPlaces(normalizedTarget)

    if (normalizedSource == normalizedTarget) {
      val sameCurrencyQuote =
        if (normalizedSource == CurrencyDefaults.REFERENCE_CURRENCY_CODE) null
        else quotesByCode[normalizedSource]
      val roundedInput = amount.setScale(targetDecimals, RESULT_ROUNDING_MODE)
      return DomainResult.Success(
        ConversionResult(
          sourceCode = normalizedSource,
          targetCode = normalizedTarget,
          inputAmount = amount,
          convertedAmount = roundedInput,
          effectiveRate = BigDecimal.ONE,
          sourceBidRate = sameCurrencyQuote?.bid,
          sourceAskRate = sameCurrencyQuote?.ask,
          targetBidRate = sameCurrencyQuote?.bid,
          targetAskRate = sameCurrencyQuote?.ask,
          decimalPlaces = targetDecimals,
          isStale = isStale,
        )
      )
    }

    val brlCode = CurrencyDefaults.REFERENCE_CURRENCY_CODE

    return when {
      // X -> BRL: valor * bid(X)
      normalizedTarget == brlCode -> {
        val sourceQuote =
          validQuoteOrNull(quotesByCode[normalizedSource])
            ?: return DomainResult.Error(DomainError.QuoteUnavailable(normalizedSource))

        val converted =
          amount
            .multiply(sourceQuote.bid, INTERMEDIATE_MATH_CONTEXT)
            .setScale(targetDecimals, RESULT_ROUNDING_MODE)

        DomainResult.Success(
          ConversionResult(
            sourceCode = normalizedSource,
            targetCode = normalizedTarget,
            inputAmount = amount,
            convertedAmount = converted,
            effectiveRate = sourceQuote.bid,
            sourceBidRate = sourceQuote.bid,
            sourceAskRate = sourceQuote.ask,
            targetBidRate = null,
            targetAskRate = null,
            decimalPlaces = targetDecimals,
            isStale = isStale,
          )
        )
      }

      // BRL -> Y: valor / bid(Y)
      normalizedSource == brlCode -> {
        val targetQuote =
          validQuoteOrNull(quotesByCode[normalizedTarget])
            ?: return DomainResult.Error(DomainError.QuoteUnavailable(normalizedTarget))

        val converted =
          amount
            .divide(targetQuote.bid, INTERMEDIATE_MATH_CONTEXT)
            .setScale(targetDecimals, RESULT_ROUNDING_MODE)

        val effectiveRate =
          BigDecimal.ONE.divide(targetQuote.bid, EFFECTIVE_RATE_SCALE, RESULT_ROUNDING_MODE)

        DomainResult.Success(
          ConversionResult(
            sourceCode = normalizedSource,
            targetCode = normalizedTarget,
            inputAmount = amount,
            convertedAmount = converted,
            effectiveRate = effectiveRate,
            sourceBidRate = null,
            sourceAskRate = null,
            targetBidRate = targetQuote.bid,
            targetAskRate = targetQuote.ask,
            decimalPlaces = targetDecimals,
            isStale = isStale,
          )
        )
      }

      // X -> Y via BRL: (valor * bid(X)) / bid(Y)
      else -> {
        val sourceQuote =
          validQuoteOrNull(quotesByCode[normalizedSource])
            ?: return DomainResult.Error(DomainError.QuoteUnavailable(normalizedSource))
        val targetQuote =
          validQuoteOrNull(quotesByCode[normalizedTarget])
            ?: return DomainResult.Error(DomainError.QuoteUnavailable(normalizedTarget))

        val amountInBrl = amount.multiply(sourceQuote.bid, INTERMEDIATE_MATH_CONTEXT)
        val converted =
          amountInBrl
            .divide(targetQuote.bid, INTERMEDIATE_MATH_CONTEXT)
            .setScale(targetDecimals, RESULT_ROUNDING_MODE)

        val effectiveRate =
          sourceQuote.bid.divide(targetQuote.bid, EFFECTIVE_RATE_SCALE, RESULT_ROUNDING_MODE)

        DomainResult.Success(
          ConversionResult(
            sourceCode = normalizedSource,
            targetCode = normalizedTarget,
            inputAmount = amount,
            convertedAmount = converted,
            effectiveRate = effectiveRate,
            sourceBidRate = sourceQuote.bid,
            sourceAskRate = sourceQuote.ask,
            targetBidRate = targetQuote.bid,
            targetAskRate = targetQuote.ask,
            decimalPlaces = targetDecimals,
            isStale = isStale,
          )
        )
      }
    }
  }

  fun parseValidAmount(rawAmount: String): BigDecimal? {
    val trimmed = rawAmount.trim()
    if (trimmed.isEmpty()) return null
    val normalized = normalizeInputString(trimmed)
    val parsed = normalized.toBigDecimalOrNull() ?: return null
    return if (parsed >= BigDecimal.ZERO) parsed else null
  }

  fun resolveDecimalPlaces(currencyCode: String): Int {
    val normalized = currencyCode.trim().uppercase(Locale.ROOT)
    CUSTOM_CRYPTO_DECIMALS[normalized]?.let { return it }
    val isoDigits =
      runCatching { Currency.getInstance(normalized).defaultFractionDigits }.getOrNull()
    return if (isoDigits != null && isoDigits >= 0) isoDigits else DEFAULT_DECIMAL_PLACES
  }

  private fun validQuoteOrNull(quote: QuoteRate?): QuoteRate? =
    quote?.takeIf { it.bid > BigDecimal.ZERO }

  private fun normalizeInputString(raw: String): String {
    // Suporta entrada simples com vírgula decimal ("100,50") desde que não misture separadores ambíguos
    return if (raw.contains(',') && !raw.contains('.')) {
      raw.replace(',', '.')
    } else {
      raw
    }
  }

  companion object {
    const val DEFAULT_DECIMAL_PLACES = 2
    const val EFFECTIVE_RATE_SCALE = 8
    val RESULT_ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP
    val INTERMEDIATE_MATH_CONTEXT: MathContext = MathContext.DECIMAL128

    private val CODE_REGEX = Regex("^[A-Z]{3}$")
    private val CUSTOM_CRYPTO_DECIMALS = mapOf("BTC" to 8, "ETH" to 8, "LTC" to 8, "XRP" to 6)
  }
}
