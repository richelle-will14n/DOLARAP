package com.aistudio.dolarap.data.repository

import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.local.dao.CurrencyDao
import com.aistudio.dolarap.data.local.dao.QuoteDao
import com.aistudio.dolarap.data.local.dao.SyncMetadataDao
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity
import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.remote.AwesomeApiConfig
import com.aistudio.dolarap.data.remote.AwesomeApiRemoteDataSource
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.QuoteRepository
import com.aistudio.dolarap.domain.service.TimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuoteRepositoryImpl(
  private val currencyDao: CurrencyDao,
  private val quoteDao: QuoteDao,
  private val syncMetadataDao: SyncMetadataDao,
  private val remoteDataSource: AwesomeApiRemoteDataSource,
  private val quoteEntityMapper: QuoteEntityMapper,
  private val remoteErrorMapper: RemoteErrorMapper,
  private val timeProvider: TimeProvider,
) : QuoteRepository {

  private val refreshMutex = Mutex()
  private var inFlightRefresh: CompletableDeferred<DomainResult<SyncStatus>>? = null

  override fun observeDashboardQuotes(): Flow<DashboardQuotesSnapshot> =
    combine(
      currencyDao.observeSelectedCurrenciesWithQuotes(),
      syncMetadataDao.observeSyncMetadata(),
    ) { relations, metadata ->
      quoteEntityMapper.toDashboardSnapshot(
        relations = relations,
        metadata = metadata,
        nowMillis = timeProvider.nowMillis(),
      )
    }

  override fun observeFavoriteQuotes(): Flow<DashboardQuotesSnapshot> =
    combine(
      currencyDao.observeFavoriteCurrenciesWithQuotes(),
      syncMetadataDao.observeSyncMetadata(),
    ) { relations, metadata ->
      quoteEntityMapper.toDashboardSnapshot(
        relations = relations,
        metadata = metadata,
        nowMillis = timeProvider.nowMillis(),
      )
    }

  override suspend fun getQuotesSnapshot(): Map<String, QuoteRate> =
    quoteDao.getAllQuotesSnapshot()
      .mapNotNull { quoteEntityMapper.toQuoteRateOrNull(it) }
      .associateBy { it.code }

  override suspend fun refreshQuotes(): DomainResult<SyncStatus> {
    val existing = refreshMutex.withLock { inFlightRefresh }
    if (existing != null && existing.isActive) {
      return existing.await()
    }

    val created = CompletableDeferred<DomainResult<SyncStatus>>()
    val (activeDeferred, isLeader) =
      refreshMutex.withLock {
        val current = inFlightRefresh
        if (current != null && current.isActive) {
          current to false
        } else {
          inFlightRefresh = created
          created to true
        }
      }

    if (!isLeader) {
      return activeDeferred.await()
    }

    return try {
      val result = executeRefreshQuotes()
      created.complete(result)
      result
    } catch (t: Throwable) {
      created.completeExceptionally(t)
      throw t
    } finally {
      refreshMutex.withLock {
        if (inFlightRefresh === created) {
          inFlightRefresh = null
        }
      }
    }
  }

  private suspend fun executeRefreshQuotes(): DomainResult<SyncStatus> {
    val nowMillis = timeProvider.nowMillis()
    DolarapDatabase.ensureDefaultCurrenciesSeeded(currencyDao, nowMillis)

    val selected = currencyDao.getAllCurrenciesSnapshot()
    val pairCodes = selected.map { it.pairCode }.distinct()
    if (pairCodes.isEmpty()) {
      return DomainResult.Error(DomainError.CurrencyNotFound("ALL"))
    }

    val collectedQuotes = mutableListOf<QuoteEntity>()
    var firstError: DomainError? = null

    for (chunk in pairCodes.chunked(MAX_BATCH_SIZE)) {
      when (val remoteResult = remoteDataSource.fetchLastQuotes(chunk, fetchedAtMillis = nowMillis)) {
        is RemoteResult.Success -> collectedQuotes.addAll(remoteResult.data)
        is RemoteResult.Error -> {
          if (firstError == null) {
            firstError = remoteErrorMapper.map(remoteResult.error)
          }
        }
      }
    }

    if (collectedQuotes.isNotEmpty()) {
      quoteDao.upsertAll(collectedQuotes)
    }

    val previousMeta = syncMetadataDao.getSyncMetadata()

    return if (firstError == null) {
      val updatedMeta =
        SyncMetadataEntity(
          id = SyncMetadataEntity.SINGLETON_ID,
          lastSuccessfulSyncAtMillis = nowMillis,
          lastAttemptAtMillis = nowMillis,
          lastErrorType = null,
          lastCatalogSyncAtMillis = previousMeta?.lastCatalogSyncAtMillis,
        )
      syncMetadataDao.upsert(updatedMeta)
      DomainResult.Success(
        SyncStatus(
          lastSuccessfulSyncAtMillis = nowMillis,
          lastAttemptAtMillis = nowMillis,
          lastErrorType = null,
          lastCatalogSyncAtMillis = updatedMeta.lastCatalogSyncAtMillis,
          freshnessStatus = QuoteFreshnessStatus.FRESH,
        )
      )
    } else {
      val updatedMeta =
        SyncMetadataEntity(
          id = SyncMetadataEntity.SINGLETON_ID,
          lastSuccessfulSyncAtMillis = previousMeta?.lastSuccessfulSyncAtMillis,
          lastAttemptAtMillis = nowMillis,
          lastErrorType = remoteErrorMapper.toErrorTag(firstError),
          lastCatalogSyncAtMillis = previousMeta?.lastCatalogSyncAtMillis,
        )
      syncMetadataDao.upsert(updatedMeta)
      DomainResult.Error(firstError)
    }
  }

  override suspend fun refreshCurrencyQuote(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    val nowMillis = timeProvider.nowMillis()
    val pairCode = "$normalizedCode-${AwesomeApiConfig.TARGET_QUOTE_CODE}"

    return when (
      val remoteResult = remoteDataSource.fetchLastQuotes(listOf(pairCode), fetchedAtMillis = nowMillis)
    ) {
      is RemoteResult.Success -> {
        quoteDao.upsertAll(remoteResult.data)
        DomainResult.Success(Unit)
      }
      is RemoteResult.Error -> DomainResult.Error(remoteErrorMapper.map(remoteResult.error))
    }
  }

  override suspend fun toggleFavorite(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    val updatedRows = currencyDao.toggleFavorite(normalizedCode)
    return if (updatedRows > 0) {
      DomainResult.Success(Unit)
    } else {
      DomainResult.Error(DomainError.CurrencyNotFound(normalizedCode))
    }
  }

  override suspend fun removeSelectedCurrency(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    if (CurrencyDefaults.isDefaultCurrency(normalizedCode)) {
      return DomainResult.Error(DomainError.DefaultCurrencyProtected(normalizedCode))
    }

    val deletedRows = currencyDao.deleteNonDefaultByCode(normalizedCode)
    if (deletedRows > 0) {
      return DomainResult.Success(Unit)
    }

    val existing = currencyDao.getCurrencyByCode(normalizedCode)
    return if (existing?.isDefault == true) {
      DomainResult.Error(DomainError.DefaultCurrencyProtected(normalizedCode))
    } else {
      DomainResult.Error(DomainError.CurrencyNotFound(normalizedCode))
    }
  }

  private companion object {
    const val MAX_BATCH_SIZE = 25
  }
}
