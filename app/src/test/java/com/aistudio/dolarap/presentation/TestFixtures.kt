package com.aistudio.dolarap.presentation

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

class FakeQuoteRepository(
  initialSnapshot: DashboardQuotesSnapshot = defaultSnapshot(),
) : QuoteRepository {

  private val snapshotFlow = MutableStateFlow(initialSnapshot)
  var failObservingFlow: Boolean = false
  var refreshQuotesResult: DomainResult<SyncStatus> =
    DomainResult.Success(initialSnapshot.syncStatus)
  var refreshCurrencyResult: DomainResult<Unit> = DomainResult.Success(Unit)
  var refreshCallCount: Int = 0
  var lastToggledCode: String? = null
  var lastRemovedCode: String? = null

  fun setSnapshot(snapshot: DashboardQuotesSnapshot) {
    snapshotFlow.value = snapshot
  }

  fun addCustomItem(item: CurrencyQuote) {
    val current = snapshotFlow.value
    snapshotFlow.value = current.copy(items = current.items + item)
  }

  override fun observeDashboardQuotes(): Flow<DashboardQuotesSnapshot> =
    if (failObservingFlow) {
      flow { throw IllegalStateException("Stream failure") }
    } else {
      snapshotFlow
    }

  override fun observeFavoriteQuotes(): Flow<DashboardQuotesSnapshot> =
    if (failObservingFlow) {
      flow { throw IllegalStateException("Stream failure") }
    } else {
      snapshotFlow.map { snapshot ->
        snapshot.copy(items = snapshot.items.filter { it.isFavorite })
      }
    }

  override suspend fun getQuotesSnapshot(): Map<String, QuoteRate> =
    snapshotFlow.value.items.mapNotNull { item -> item.quote?.let { item.code to it } }.toMap()

  override suspend fun refreshQuotes(): DomainResult<SyncStatus> {
    refreshCallCount++
    if (refreshQuotesResult is DomainResult.Success) {
      val status = (refreshQuotesResult as DomainResult.Success).data
      snapshotFlow.value = snapshotFlow.value.copy(syncStatus = status)
    }
    return refreshQuotesResult
  }

  override suspend fun refreshCurrencyQuote(code: String): DomainResult<Unit> = refreshCurrencyResult

  override suspend fun toggleFavorite(code: String): DomainResult<Unit> {
    val normalized = CurrencyDefaults.normalizeCode(code)
    lastToggledCode = normalized
    val current = snapshotFlow.value
    val found = current.items.any { it.code == normalized }
    if (!found) return DomainResult.Error(DomainError.CurrencyNotFound(normalized))
    snapshotFlow.value =
      current.copy(
        items =
          current.items.map { item ->
            if (item.code == normalized) item.copy(isFavorite = !item.isFavorite) else item
          }
      )
    return DomainResult.Success(Unit)
  }

  override suspend fun removeSelectedCurrency(code: String): DomainResult<Unit> {
    val normalized = CurrencyDefaults.normalizeCode(code)
    lastRemovedCode = normalized
    if (CurrencyDefaults.isDefaultCurrency(normalized)) {
      return DomainResult.Error(DomainError.DefaultCurrencyProtected(normalized))
    }
    val current = snapshotFlow.value
    val exists = current.items.any { it.code == normalized }
    if (!exists) return DomainResult.Error(DomainError.CurrencyNotFound(normalized))
    snapshotFlow.value =
      current.copy(items = current.items.filterNot { it.code == normalized })
    return DomainResult.Success(Unit)
  }

  companion object {
    fun sampleQuoteRate(
      code: String,
      bid: String,
      ask: String = bid,
      pctChange: String = "0.50",
      varBid: String = "0.0250",
      fetchedAtMillis: Long = 1_700_000_000_000L,
    ): QuoteRate =
      QuoteRate(
        pairCode = "$code-BRL",
        code = code,
        codeIn = "BRL",
        apiName = "$code/Real Brasileiro",
        bid = BigDecimal(bid),
        ask = BigDecimal(ask),
        varBid = BigDecimal(varBid),
        pctChange = BigDecimal(pctChange),
        high = BigDecimal(ask),
        low = BigDecimal(bid),
        apiTimestampSeconds = fetchedAtMillis / 1000L,
        createDate = "2026-11-15 12:00:00",
        fetchedAtMillis = fetchedAtMillis,
      )

    fun defaultSnapshot(
      withQuotes: Boolean = true,
      freshnessStatus: QuoteFreshnessStatus =
        if (withQuotes) QuoteFreshnessStatus.FRESH else QuoteFreshnessStatus.UNAVAILABLE,
      fetchedAtMillis: Long = 1_700_000_000_000L,
    ): DashboardQuotesSnapshot {
      val sampleBids =
        mapOf(
          "USD" to ("5.5000" to "5.5020"),
          "EUR" to ("6.0000" to "6.0040"),
          "GBP" to ("7.0000" to "7.0050"),
          "CAD" to ("4.0000" to "4.0020"),
          "AUD" to ("3.6000" to "3.6030"),
          "CHF" to ("6.2000" to "6.2050"),
          "JPY" to ("0.0360" to "0.0362"),
          "ARS" to ("0.0055" to "0.0056"),
          "CNY" to ("0.7600" to "0.7615"),
          "MXN" to ("0.2800" to "0.2810"),
        )
      val items =
        CurrencyDefaults.DEFAULT_CURRENCY_CODES.mapIndexed { index, code ->
          val rate =
            if (withQuotes) {
              val (b, a) = sampleBids.getValue(code)
              sampleQuoteRate(code = code, bid = b, ask = a, fetchedAtMillis = fetchedAtMillis)
            } else {
              null
            }
          CurrencyQuote(
            code = code,
            pairCode = "$code-BRL",
            displayName =
              when (code) {
                "USD" -> "Dólar Americano"
                "EUR" -> "Euro"
                "CHF" -> "Franco Suíço"
                else -> code
              },
            symbol = if (code == "USD") "US$" else code,
            isDefault = true,
            isFavorite = false,
            sortOrder = index,
            quote = rate,
            freshnessStatus = freshnessStatus,
          )
        }
      return DashboardQuotesSnapshot(
        items = items,
        syncStatus =
          SyncStatus(
            lastSuccessfulSyncAtMillis = if (withQuotes) fetchedAtMillis else null,
            lastAttemptAtMillis = fetchedAtMillis,
            lastErrorType = null,
            lastCatalogSyncAtMillis = fetchedAtMillis,
            freshnessStatus = freshnessStatus,
          ),
      )
    }
  }
}

class FakeCatalogRepository(
  initialCatalog: List<AvailableCurrency> = defaultCatalog(),
  private val quoteRepository: FakeQuoteRepository? = null,
) : CatalogRepository {

  private val catalogFlow = MutableStateFlow(initialCatalog)
  var failObservingFlow: Boolean = false
  var refreshCatalogResult: DomainResult<Int> = DomainResult.Success(initialCatalog.size)
  var refreshCount: Int = 0

  fun setCatalog(items: List<AvailableCurrency>) {
    catalogFlow.value = items
  }

  override fun observeAvailableCurrencies(): Flow<List<AvailableCurrency>> =
    if (failObservingFlow) {
      flow { throw IllegalStateException("Catalog flow failure") }
    } else {
      catalogFlow
    }

  override suspend fun refreshCatalog(): DomainResult<Int> {
    refreshCount++
    return refreshCatalogResult
  }

  override suspend fun addCurrency(code: String): DomainResult<Unit> {
    val normalized = CurrencyDefaults.normalizeCode(code)
    val item =
      catalogFlow.value.firstOrNull { it.code == normalized && it.isAvailable }
        ?: return DomainResult.Error(DomainError.UnsupportedPair("$normalized-BRL"))
    if (item.isSelected) {
      return DomainResult.Error(DomainError.CurrencyAlreadySelected(normalized))
    }
    catalogFlow.value =
      catalogFlow.value.map { current ->
        if (current.code == normalized) current.copy(isSelected = true) else current
      }
    quoteRepository?.addCustomItem(
      CurrencyQuote(
        code = normalized,
        pairCode = "$normalized-BRL",
        displayName = item.friendlyName,
        symbol = item.symbol,
        isDefault = item.isDefault,
        isFavorite = false,
        sortOrder = 99,
        quote = FakeQuoteRepository.sampleQuoteRate(normalized, "5.2000"),
        freshnessStatus = QuoteFreshnessStatus.FRESH,
      )
    )
    return DomainResult.Success(Unit)
  }

  fun markUnselected(code: String) {
    val normalized = CurrencyDefaults.normalizeCode(code)
    catalogFlow.value =
      catalogFlow.value.map { current ->
        if (current.code == normalized) current.copy(isSelected = false) else current
      }
  }

  companion object {
    fun defaultCatalog(): List<AvailableCurrency> =
      listOf(
        AvailableCurrency(
          pairCode = "USD-BRL",
          code = "USD",
          quoteCode = "BRL",
          friendlyName = "Dólar Americano",
          apiDescription = "Dólar Americano/Real Brasileiro",
          symbol = "US$",
          isDefault = true,
          isSelected = true,
          isAvailable = true,
          updatedAtMillis = 1_700_000_000_000L,
        ),
        AvailableCurrency(
          pairCode = "CHF-BRL",
          code = "CHF",
          quoteCode = "BRL",
          friendlyName = "Franco Suíço",
          apiDescription = "Franco Suíço/Real Brasileiro",
          symbol = "CHF",
          isDefault = true,
          isSelected = true,
          isAvailable = true,
          updatedAtMillis = 1_700_000_000_000L,
        ),
        AvailableCurrency(
          pairCode = "NOK-BRL",
          code = "NOK",
          quoteCode = "BRL",
          friendlyName = "Coroa Norueguesa",
          apiDescription = "Coroa Norueguesa/Real Brasileiro",
          symbol = "kr",
          isDefault = false,
          isSelected = false,
          isAvailable = true,
          updatedAtMillis = 1_700_000_000_000L,
        ),
        AvailableCurrency(
          pairCode = "ZAR-BRL",
          code = "ZAR",
          quoteCode = "BRL",
          friendlyName = "Rand Sul-Africano",
          apiDescription = "Rand Sul-Africano/Real Brasileiro",
          symbol = "R",
          isDefault = false,
          isSelected = false,
          isAvailable = true,
          updatedAtMillis = 1_700_000_000_000L,
        ),
      )
  }
}
