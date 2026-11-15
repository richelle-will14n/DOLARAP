package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveAvailableCurrenciesUseCase(
  private val catalogRepository: CatalogRepository,
) {
  operator fun invoke(): Flow<List<AvailableCurrency>> =
    catalogRepository.observeAvailableCurrencies()
}
