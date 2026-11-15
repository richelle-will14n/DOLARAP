package com.aistudio.dolarap.domain.repository

import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.QuoteRate
import com.aistudio.dolarap.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

  fun observeDashboardQuotes(): Flow<DashboardQuotesSnapshot>

  fun observeFavoriteQuotes(): Flow<DashboardQuotesSnapshot>

  suspend fun getQuotesSnapshot(): Map<String, QuoteRate>

  suspend fun refreshQuotes(): DomainResult<SyncStatus>

  suspend fun refreshCurrencyQuote(code: String): DomainResult<Unit>

  suspend fun toggleFavorite(code: String): DomainResult<Unit>

  suspend fun removeSelectedCurrency(code: String): DomainResult<Unit>
}
