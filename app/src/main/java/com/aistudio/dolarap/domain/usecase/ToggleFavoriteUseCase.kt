package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.QuoteRepository

class ToggleFavoriteUseCase(
  private val quoteRepository: QuoteRepository,
) {
  suspend operator fun invoke(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    return quoteRepository.toggleFavorite(normalizedCode)
  }
}
