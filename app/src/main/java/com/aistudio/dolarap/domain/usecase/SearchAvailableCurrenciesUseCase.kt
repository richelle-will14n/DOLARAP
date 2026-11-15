package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.repository.CatalogRepository
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchAvailableCurrenciesUseCase(
  private val catalogRepository: CatalogRepository,
) {
  operator fun invoke(query: String): Flow<List<AvailableCurrency>> {
    val normalizedQuery = normalizeText(query)
    return catalogRepository.observeAvailableCurrencies().map { currencies ->
      if (normalizedQuery.isEmpty()) {
        currencies
      } else {
        currencies.filter { currency ->
          normalizeText(currency.code).contains(normalizedQuery) ||
            normalizeText(currency.friendlyName).contains(normalizedQuery) ||
            normalizeText(currency.apiDescription).contains(normalizedQuery)
        }
      }
    }
  }

  private fun normalizeText(input: String): String {
    val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
    return COMBINING_MARKS_REGEX.replace(decomposed, "").lowercase(Locale.ROOT)
  }

  private companion object {
    val COMBINING_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
  }
}
