package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.DashboardQuotesSnapshot
import com.aistudio.dolarap.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveDashboardQuotesUseCase(
  private val quoteRepository: QuoteRepository,
) {
  operator fun invoke(favoritesOnly: Boolean = false): Flow<DashboardQuotesSnapshot> =
    if (favoritesOnly) {
      quoteRepository.observeFavoriteQuotes()
    } else {
      quoteRepository.observeDashboardQuotes()
    }
}
