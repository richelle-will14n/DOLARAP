package com.aistudio.dolarap.domain.model

import java.math.BigDecimal
import java.util.Locale

object CurrencyDefaults {
  const val REFERENCE_CURRENCY_CODE = "BRL"
  const val REFERENCE_CURRENCY_NAME = "Real Brasileiro"
  const val REFERENCE_CURRENCY_SYMBOL = "R$"

  val DEFAULT_CURRENCY_CODES: List<String> =
    listOf("USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY", "ARS", "CNY", "MXN")

  private val defaultSet: Set<String> = DEFAULT_CURRENCY_CODES.toSet()

  fun isDefaultCurrency(code: String): Boolean =
    defaultSet.contains(code.trim().uppercase(Locale.ROOT))

  fun normalizeCode(code: String): String = code.trim().uppercase(Locale.ROOT)
}

enum class QuoteFreshnessStatus {
  FRESH,
  STALE,
  UNAVAILABLE,
}

data class QuoteRate(
  val pairCode: String,
  val code: String,
  val codeIn: String,
  val apiName: String,
  val bid: BigDecimal,
  val ask: BigDecimal,
  val varBid: BigDecimal,
  val pctChange: BigDecimal,
  val high: BigDecimal,
  val low: BigDecimal,
  val apiTimestampSeconds: Long,
  val createDate: String,
  val fetchedAtMillis: Long,
)

data class CurrencyQuote(
  val code: String,
  val pairCode: String,
  val displayName: String,
  val symbol: String,
  val isDefault: Boolean,
  val isFavorite: Boolean,
  val sortOrder: Int,
  val quote: QuoteRate?,
  val freshnessStatus: QuoteFreshnessStatus,
)

data class SyncStatus(
  val lastSuccessfulSyncAtMillis: Long?,
  val lastAttemptAtMillis: Long?,
  val lastErrorType: String?,
  val lastCatalogSyncAtMillis: Long?,
  val freshnessStatus: QuoteFreshnessStatus,
) {
  val isStale: Boolean
    get() = freshnessStatus == QuoteFreshnessStatus.STALE

  val hasNeverSynced: Boolean
    get() = lastSuccessfulSyncAtMillis == null
}

data class DashboardQuotesSnapshot(
  val items: List<CurrencyQuote>,
  val syncStatus: SyncStatus,
)
