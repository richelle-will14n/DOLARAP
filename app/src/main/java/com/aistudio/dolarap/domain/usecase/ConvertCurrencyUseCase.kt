package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.ConversionResult
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.TimeProvider
import java.math.BigDecimal

class ConvertCurrencyUseCase(
  private val quoteRepository: QuoteRepository,
  private val currencyConverter: CurrencyConverter,
  private val freshnessEvaluator: QuoteFreshnessEvaluator,
  private val timeProvider: TimeProvider,
) {

  suspend operator fun invoke(
    rawAmount: String,
    sourceCode: String,
    targetCode: String,
  ): DomainResult<ConversionResult> {
    val quotesByCode = quoteRepository.getQuotesSnapshot()
    return convertFromSnapshot(
      rawAmount = rawAmount,
      sourceCode = sourceCode,
      targetCode = targetCode,
      quotesByCode = quotesByCode,
    )
  }

  suspend operator fun invoke(
    amount: BigDecimal,
    sourceCode: String,
    targetCode: String,
  ): DomainResult<ConversionResult> {
    val quotesByCode = quoteRepository.getQuotesSnapshot()
    val isStale = isConversionStale(sourceCode, targetCode, quotesByCode)
    return currencyConverter.convert(
      amount = amount,
      sourceCode = sourceCode,
      targetCode = targetCode,
      quotesByCode = quotesByCode,
      isStale = isStale,
    )
  }

  fun convertFromSnapshot(
    rawAmount: String,
    sourceCode: String,
    targetCode: String,
    quotesByCode: Map<String, QuoteRate>,
  ): DomainResult<ConversionResult> {
    val isStale = isConversionStale(sourceCode, targetCode, quotesByCode)
    return currencyConverter.convert(
      rawAmount = rawAmount,
      sourceCode = sourceCode,
      targetCode = targetCode,
      quotesByCode = quotesByCode,
      isStale = isStale,
    )
  }

  private fun isConversionStale(
    sourceCode: String,
    targetCode: String,
    quotesByCode: Map<String, QuoteRate>,
  ): Boolean {
    val nowMillis = timeProvider.nowMillis()
    val normalizedSource = CurrencyDefaults.normalizeCode(sourceCode)
    val normalizedTarget = CurrencyDefaults.normalizeCode(targetCode)
    val brl = CurrencyDefaults.REFERENCE_CURRENCY_CODE

    val sourceStale =
      normalizedSource != brl &&
        quotesByCode[normalizedSource]?.let {
          freshnessEvaluator.isStale(it.fetchedAtMillis, nowMillis)
        } == true

    val targetStale =
      normalizedTarget != brl &&
        quotesByCode[normalizedTarget]?.let {
          freshnessEvaluator.isStale(it.fetchedAtMillis, nowMillis)
        } == true

    return sourceStale || targetStale
  }
}
