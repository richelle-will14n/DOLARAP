package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.domain.model.DomainError
import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.CatalogRepository
import com.aistudio.dolarap.domain.repository.QuoteRepository

class AddCurrencyUseCase(
  private val catalogRepository: CatalogRepository,
  private val quoteRepository: QuoteRepository,
) {
  suspend operator fun invoke(code: String): DomainResult<Unit> {
    val normalizedCode = CurrencyDefaults.normalizeCode(code)
    if (!CODE_REGEX.matches(normalizedCode) || normalizedCode == CurrencyDefaults.REFERENCE_CURRENCY_CODE) {
      return DomainResult.Error(
        DomainError.UnsupportedPair("$normalizedCode-${CurrencyDefaults.REFERENCE_CURRENCY_CODE}")
      )
    }

    val addResult = catalogRepository.addCurrency(normalizedCode)
    if (addResult is DomainResult.Success) {
      // Melhor esforço para obter cotação imediata; falha de rede não desfaz a adição no Room
      quoteRepository.refreshCurrencyQuote(normalizedCode)
    }
    return addResult
  }

  private companion object {
    val CODE_REGEX = Regex("^[A-Z]{3}$")
  }
}
