package com.aistudio.dolarap.data.repository

import com.aistudio.dolarap.data.local.catalog.ComplementaryCurrencyCatalog
import com.aistudio.dolarap.data.local.dao.AvailablePairDao
import com.aistudio.dolarap.data.local.dao.CurrencyDao
import com.aistudio.dolarap.data.local.dao.SyncMetadataDao
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyEntity
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity
import com.aistudio.dolarap.data.mapper.QuoteEntityMapper
import com.aistudio.dolarap.data.mapper.RemoteErrorMapper
import com.aistudio.dolarap.data.remote.AwesomeApiConfig
import com.aistudio.dolarap.data.remote.AwesomeApiRemoteDataSource
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.service.TimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CatalogRepositoryImpl(
  private val availablePairDao: AvailablePairDao,
  private val currencyDao: CurrencyDao,
  private val syncMetadataDao: SyncMetadataDao,
  private val remoteDataSource: AwesomeApiRemoteDataSource,
  private val quoteEntityMapper: QuoteEntityMapper,
  private val remoteErrorMapper: RemoteErrorMapper,
  private val timeProvider: TimeProvider,
) : CatalogRepository {

  private val refreshMutex = Mutex()
  private var inFlightRefresh: CompletableDeferred<DomainResult<Int>>? = null

  override fun observeAvailableCurrencies(): Flow<List<AvailableCurrency>> =
    combine(
      availablePairDao.observeSupportedBrlPairs(),
      currencyDao.observeSelectedCurrencies(),
    ) { pairs, selected ->
      val selectedCodes = selected.mapTo(HashSet()) { it.code }
      pairs.map { pair -> quoteEntityMapper.toAvailableCurrency(pair, selectedCodes) }
    }

  override suspend fun refreshCatalog(): DomainResult<Int> {
    val existing = refreshMutex.withLock { inFlightRefresh }
    if (existing != null && existing.isActive) {
      return existing.await()
    }

    val created = CompletableDeferred<DomainResult<Int>>()
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
      val result = executeRefreshCatalog()
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

  private suspend fun executeRefreshCatalog(): DomainResult<Int> {
    val nowMillis = timeProvider.nowMillis()
    val previousMeta = syncMetadataDao.getSyncMetadata()

    return when (val remoteResult = remoteDataSource.syncAvailableBrlCatalog(updatedAtMillis = nowMillis)) {
      is RemoteResult.Success -> {
        syncMetadataDao.upsert(
          SyncMetadataEntity(
            id = SyncMetadataEntity.SINGLETON_ID,
            lastSuccessfulSyncAtMillis = previousMeta?.lastSuccessfulSyncAtMillis,
            lastAttemptAtMillis = nowMillis,
            lastErrorType = null,
            lastCatalogSyncAtMillis = nowMillis,
          )
        )
        DomainResult.Success(remoteResult.data.size)
      }
      is RemoteResult.Error -> {
        val domainError = remoteErrorMapper.map(remoteResult.error)
        syncMetadataDao.upsert(
          SyncMetadataEntity(
            id = SyncMetadataEntity.SINGLETON_ID,
            lastSuccessfulSyncAtMillis = previousMeta?.lastSuccessfulSyncAtMillis,
            lastAttemptAtMillis = nowMillis,
            lastErrorType = remoteErrorMapper.toErrorTag(domainError),
            lastCatalogSyncAtMillis = previousMeta?.lastCatalogSyncAtMillis,
          )
        )
        DomainResult.Error(domainError)
      }
    }
  }

  override suspend fun addCurrency(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    val pairCode = "$normalizedCode-${AwesomeApiConfig.TARGET_QUOTE_CODE}"

    if (!CODE_REGEX.matches(normalizedCode) || normalizedCode == CurrencyDefaults.REFERENCE_CURRENCY_CODE) {
      return DomainResult.Error(DomainError.UnsupportedPair(pairCode))
    }

    val availablePair = availablePairDao.getAvailablePair(pairCode)
    if (availablePair == null || !availablePair.isAvailable) {
      return DomainResult.Error(DomainError.UnsupportedPair(pairCode))
    }

    if (currencyDao.getCurrencyByCode(normalizedCode) != null) {
      return DomainResult.Error(DomainError.CurrencyAlreadySelected(normalizedCode))
    }

    val nextSortOrder = currencyDao.getMaxSortOrder() + 1
    val displayName =
      availablePair.friendlyName.ifBlank {
        ComplementaryCurrencyCatalog.resolveFriendlyName(normalizedCode, availablePair.apiDescription)
      }
    val entity =
      SelectedCurrencyEntity(
        code = normalizedCode,
        pairCode = pairCode,
        displayName = displayName,
        isDefault = CurrencyDefaults.isDefaultCurrency(normalizedCode),
        isFavorite = false,
        sortOrder = nextSortOrder,
        addedAtMillis = timeProvider.nowMillis(),
      )
    val rowId = currencyDao.insertIgnore(entity)
    return if (rowId == -1L) {
      DomainResult.Error(DomainError.CurrencyAlreadySelected(normalizedCode))
    } else {
      DomainResult.Success(Unit)
    }
  }

  private companion object {
    val CODE_REGEX = Regex("^[A-Z]{3}$")
  }
}
