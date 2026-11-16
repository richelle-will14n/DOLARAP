package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.BackgroundSyncInfo
import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.SyncScheduler
import com.aistudio.dolarap.domain.service.TimeProvider
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainUseCasesTest {

  private val fakeNowMillis = 1_700_000_000_000L
  private val timeProvider = TimeProvider { fakeNowMillis }
  private val freshnessEvaluator = QuoteFreshnessEvaluator()

  @Test
  fun `RemoveCurrencyUseCase protects all 10 default currencies and allows removing additional currencies`() =
    runBlocking {
      val quoteRepo = FakeQuoteRepository()
      val useCase = RemoveCurrencyUseCase(quoteRepo)

      CurrencyDefaults.DEFAULT_CURRENCY_CODES.forEach { defaultCode ->
        val result = useCase(defaultCode.lowercase())
        assertTrue(result is DomainResult.Error)
        assertEquals(
          DomainError.DefaultCurrencyProtected(defaultCode),
          (result as DomainResult.Error).error,
        )
      }
      assertEquals(0, quoteRepo.removedCodes.size)

      val customResult = useCase("clp")
      assertTrue(customResult is DomainResult.Success)
      assertEquals(listOf("CLP"), quoteRepo.removedCodes)
    }

  @Test
  fun `AddCurrencyUseCase validates pair, adds supported currency, and keeps addition even if quote refresh fails`() =
    runBlocking {
      val quoteRepo = FakeQuoteRepository(refreshSingleResult = DomainResult.Error(DomainError.NoConnection))
      val catalogRepo = FakeCatalogRepository(supportedCodes = setOf("CLP"))
      val useCase = AddCurrencyUseCase(catalogRepo, quoteRepo)

      val invalidCode = useCase("BRL")
      assertTrue((invalidCode as DomainResult.Error).error is DomainError.UnsupportedPair)

      val unsupported = useCase("ZZZ")
      assertEquals(DomainError.UnsupportedPair("ZZZ-BRL"), (unsupported as DomainResult.Error).error)

      val supported = useCase("clp")
      assertTrue(supported is DomainResult.Success)
      assertEquals(listOf("CLP"), catalogRepo.addedCodes)
      assertEquals(listOf("CLP"), quoteRepo.refreshedSingleCodes)
    }

  @Test
  fun `SearchAvailableCurrenciesUseCase filters case-insensitively and accent-insensitively`() =
    runBlocking {
      val catalogRepo =
        FakeCatalogRepository(
          initialCatalog =
            listOf(
              availableCurrency("USD", "Dólar Americano"),
              availableCurrency("CHF", "Franco Suíço"),
              availableCurrency("CLP", "Peso Chileno"),
            )
        )
      val useCase = SearchAvailableCurrenciesUseCase(catalogRepo)

      assertEquals(listOf("CHF"), useCase("suico").first().map { it.code })
      assertEquals(listOf("USD"), useCase("usd").first().map { it.code })
      assertEquals(listOf("USD", "CHF", "CLP"), useCase("   ").first().map { it.code })
    }

  @Test
  fun `ConvertCurrencyUseCase marks stale conversions without blocking and fails when quote missing`() =
    runBlocking {
      val staleTimestamp = fakeNowMillis - (16L * 60L * 1000L)
      val quoteRepo =
        FakeQuoteRepository(
          snapshot =
            mapOf(
              "USD" to
                QuoteRate(
                  pairCode = "USD-BRL",
                  code = "USD",
                  codeIn = "BRL",
                  apiName = "Dólar Americano/Real Brasileiro",
                  bid = BigDecimal("5.50"),
                  ask = BigDecimal("5.51"),
                  varBid = BigDecimal.ZERO,
                  pctChange = BigDecimal.ZERO,
                  high = BigDecimal("5.52"),
                  low = BigDecimal("5.48"),
                  apiTimestampSeconds = 1_700_000_000L,
                  createDate = "2026-11-15 10:00:00",
                  fetchedAtMillis = staleTimestamp,
                )
            )
        )
      val useCase =
        ConvertCurrencyUseCase(quoteRepo, CurrencyConverter(), freshnessEvaluator, timeProvider)

      val staleConversion = useCase("10", "USD", "BRL") as DomainResult.Success
      assertEquals(BigDecimal("55.00"), staleConversion.data.convertedAmount)
      assertTrue(staleConversion.data.isStale)

      val missingQuote = useCase("10", "EUR", "BRL") as DomainResult.Error
      assertEquals(DomainError.QuoteUnavailable("EUR"), missingQuote.error)
    }

  @Test
  fun `ObserveDashboardQuotesUseCase switches between all currencies and favorites`() = runBlocking {
    val quoteRepo = FakeQuoteRepository()
    val useCase = ObserveDashboardQuotesUseCase(quoteRepo)

    assertEquals(2, useCase(favoritesOnly = false).first().items.size)
    assertEquals(listOf("USD"), useCase(favoritesOnly = true).first().items.map { it.code })
  }

  @Test
  fun `RefreshQuotesUseCase schedules connectivity retry on transient failures and cancels on success`() =
    runBlocking {
      val quoteRepo = FakeQuoteRepository()
      val scheduler = RecordingSyncScheduler()
      val useCase = RefreshQuotesUseCase(quoteRepo, scheduler)

      quoteRepo.refreshAllResult = DomainResult.Error(DomainError.NoConnection)
      useCase()
      assertEquals(listOf<Long?>(null), scheduler.scheduledRetryDelays)

      quoteRepo.refreshAllResult = DomainResult.Error(DomainError.RateLimited(retryAfterSeconds = 75L))
      useCase()
      assertEquals(listOf(null, 75L), scheduler.scheduledRetryDelays)

      quoteRepo.refreshAllResult = DomainResult.Error(DomainError.UnsupportedPair("XYZ-BRL"))
      useCase()
      assertEquals(2, scheduler.scheduledRetryDelays.size)

      quoteRepo.refreshAllResult = DomainResult.Success(quoteRepo.emptyStatus)
      useCase()
      assertEquals(1, scheduler.cancelRetryCount)
    }

  private fun availableCurrency(
    code: String,
    friendlyName: String,
  ): AvailableCurrency =
    AvailableCurrency(
      pairCode = "$code-BRL",
      code = code,
      quoteCode = "BRL",
      friendlyName = friendlyName,
      apiDescription = "$friendlyName/Real Brasileiro",
      symbol = code,
      isDefault = CurrencyDefaults.isDefaultCurrency(code),
      isSelected = CurrencyDefaults.isDefaultCurrency(code),
      isAvailable = true,
      updatedAtMillis = fakeNowMillis,
    )

  private class RecordingSyncScheduler : SyncScheduler {
    val scheduledRetryDelays = mutableListOf<Long?>()
    var cancelRetryCount = 0

    override fun schedulePeriodicSync() = Unit

    override fun scheduleConnectivityRetrySync(delaySeconds: Long?) {
      scheduledRetryDelays.add(delaySeconds)
    }

    override fun cancelConnectivityRetrySync() {
      cancelRetryCount++
    }

    override fun observeSyncState(): Flow<BackgroundSyncInfo> = MutableStateFlow(BackgroundSyncInfo())
  }

  private class FakeQuoteRepository(
    private val snapshot: Map<String, QuoteRate> = emptyMap(),
    private val refreshSingleResult: DomainResult<Unit> = DomainResult.Success(Unit),
  ) : QuoteRepository {
    val removedCodes = mutableListOf<String>()
    val refreshedSingleCodes = mutableListOf<String>()

    private val itemsFlow =
      MutableStateFlow(
        listOf(
          com.aistudio.dolarap.domain.model.CurrencyQuote(
            code = "USD",
            pairCode = "USD-BRL",
            displayName = "Dólar Americano",
            symbol = "$",
            isDefault = true,
            isFavorite = true,
            sortOrder = 0,
            quote = null,
            freshnessStatus = QuoteFreshnessStatus.UNAVAILABLE,
          ),
          com.aistudio.dolarap.domain.model.CurrencyQuote(
            code = "EUR",
            pairCode = "EUR-BRL",
            displayName = "Euro",
            symbol = "€",
            isDefault = true,
            isFavorite = false,
            sortOrder = 1,
            quote = null,
            freshnessStatus = QuoteFreshnessStatus.UNAVAILABLE,
          ),
        )
      )

    val emptyStatus =
      SyncStatus(
        lastSuccessfulSyncAtMillis = null,
        lastAttemptAtMillis = null,
        lastErrorType = null,
        lastCatalogSyncAtMillis = null,
        freshnessStatus = QuoteFreshnessStatus.UNAVAILABLE,
      )

    var refreshAllResult: DomainResult<SyncStatus> = DomainResult.Success(emptyStatus)

    override fun observeDashboardQuotes(): Flow<DashboardQuotesSnapshot> =
      itemsFlow.map { DashboardQuotesSnapshot(it, emptyStatus) }

    override fun observeFavoriteQuotes(): Flow<DashboardQuotesSnapshot> =
      itemsFlow.map { list -> DashboardQuotesSnapshot(list.filter { it.isFavorite }, emptyStatus) }

    override suspend fun getQuotesSnapshot(): Map<String, QuoteRate> = snapshot

    override suspend fun refreshQuotes(): DomainResult<SyncStatus> = refreshAllResult

    override suspend fun refreshCurrencyQuote(code: String): DomainResult<Unit> {
      refreshedSingleCodes.add(code)
      return refreshSingleResult
    }

    override suspend fun toggleFavorite(code: String): DomainResult<Unit> = DomainResult.Success(Unit)

    override suspend fun removeSelectedCurrency(code: String): DomainResult<Unit> {
      removedCodes.add(code)
      return DomainResult.Success(Unit)
    }
  }

  private class FakeCatalogRepository(
    private val supportedCodes: Set<String> = emptySet(),
    initialCatalog: List<AvailableCurrency> = emptyList(),
  ) : CatalogRepository {
    val addedCodes = mutableListOf<String>()
    private val catalogFlow = MutableStateFlow(initialCatalog)

    override fun observeAvailableCurrencies(): Flow<List<AvailableCurrency>> = catalogFlow

    override suspend fun refreshCatalog(): DomainResult<Int> = DomainResult.Success(0)

    override suspend fun addCurrency(code: String): DomainResult<Unit> {
      return if (supportedCodes.contains(code)) {
        addedCodes.add(code)
        DomainResult.Success(Unit)
      } else {
        DomainResult.Error(DomainError.UnsupportedPair("$code-BRL"))
      }
    }
  }
}
