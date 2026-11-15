package com.aistudio.dolarap.domain.repository

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.model.DomainResult
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {

  fun observeAvailableCurrencies(): Flow<List<AvailableCurrency>>

  suspend fun refreshCatalog(): DomainResult<Int>

  suspend fun addCurrency(code: String): DomainResult<Unit>
}
