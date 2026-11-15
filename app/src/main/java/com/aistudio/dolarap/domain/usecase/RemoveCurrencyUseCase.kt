package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.QuoteRepository

class RemoveCurrencyUseCase(
  private val quoteRepository: QuoteRepository,
) {
  suspend operator fun invoke(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    if (CurrencyDefaults.isDefaultCurrency(normalizedCode)) {
      return DomainResult.Error(DomainError.DefaultCurrencyProtected(normalizedCode))
    }
    return quoteRepository.removeSelectedCurrency(normalizedCode)
  }
}
