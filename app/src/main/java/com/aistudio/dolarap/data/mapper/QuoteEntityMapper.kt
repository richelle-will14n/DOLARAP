package com.aistudio.dolarap.data.mapper

import com.aistudio.dolarap.data.local.catalog.ComplementaryCurrencyCatalog
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyWithQuote
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity
import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import java.math.BigDecimal

class QuoteEntityMapper(
  private val freshnessEvaluator: QuoteFreshnessEvaluator,
) {

  fun toQuoteRateOrNull(entity: QuoteEntity): QuoteRate? {
    val bid = entity.bid.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val ask = entity.ask.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val varBid = entity.varBid.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val pctChange = entity.pctChange.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val high = entity.high.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: bid
    val low = entity.low.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: bid

    return QuoteRate(
      pairCode = entity.pairCode,
      code = entity.code,
      codeIn = entity.codeIn,
      apiName = entity.apiName,
      bid = bid,
      ask = ask,
      varBid = varBid,
      pctChange = pctChange,
      high = high,
      low = low,
      apiTimestampSeconds = entity.apiTimestampSeconds,
      createDate = entity.createDate,
      fetchedAtMillis = entity.fetchedAtMillis,
    )
  }

  fun toCurrencyQuote(
    relation: SelectedCurrencyWithQuote,
    nowMillis: Long,
  ): CurrencyQuote {
    val quoteRate = relation.quote?.let(::toQuoteRateOrNull)
    val freshness = freshnessEvaluator.evaluate(quoteRate?.fetchedAtMillis, nowMillis)
    val currency = relation.currency

    return CurrencyQuote(
      code = currency.code,
      pairCode = currency.pairCode,
      displayName = currency.displayName,
      symbol = ComplementaryCurrencyCatalog.resolveSymbol(currency.code),
      isDefault = currency.isDefault,
      isFavorite = currency.isFavorite,
      sortOrder = currency.sortOrder,
      quote = quoteRate,
      freshnessStatus = freshness,
    )
  }

  fun toSyncStatus(
    metadata: SyncMetadataEntity?,
    items: List<CurrencyQuote>,
    nowMillis: Long,
  ): SyncStatus {
    val referenceTimestamp =
      metadata?.lastSuccessfulSyncAtMillis ?: items.mapNotNull { it.quote?.fetchedAtMillis }.maxOrNull()
    val freshness = freshnessEvaluator.evaluate(referenceTimestamp, nowMillis)

    return SyncStatus(
      lastSuccessfulSyncAtMillis = metadata?.lastSuccessfulSyncAtMillis,
      lastAttemptAtMillis = metadata?.lastAttemptAtMillis,
      lastErrorType = metadata?.lastErrorType,
      lastCatalogSyncAtMillis = metadata?.lastCatalogSyncAtMillis,
      freshnessStatus = freshness,
    )
  }

  fun toDashboardSnapshot(
    relations: List<SelectedCurrencyWithQuote>,
    metadata: SyncMetadataEntity?,
    nowMillis: Long,
  ): DashboardQuotesSnapshot {
    val items = relations.map { toCurrencyQuote(it, nowMillis) }
    val syncStatus = toSyncStatus(metadata, items, nowMillis)
    return DashboardQuotesSnapshot(items = items, syncStatus = syncStatus)
  }

  fun toAvailableCurrency(
    entity: AvailablePairEntity,
    selectedCodes: Set<String>,
  ): AvailableCurrency =
    AvailableCurrency(
      pairCode = entity.pairCode,
      code = entity.baseCode,
      quoteCode = entity.quoteCode,
      friendlyName = entity.friendlyName,
      apiDescription = entity.apiDescription,
      symbol = ComplementaryCurrencyCatalog.resolveSymbol(entity.baseCode),
      isDefault = CurrencyDefaults.isDefaultCurrency(entity.baseCode),
      isSelected = selectedCodes.contains(entity.baseCode),
      isAvailable = entity.isAvailable,
      updatedAtMillis = entity.updatedAtMillis,
    )
}
