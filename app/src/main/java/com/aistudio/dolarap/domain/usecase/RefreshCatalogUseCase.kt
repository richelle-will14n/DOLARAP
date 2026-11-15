package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.repository.CatalogRepository

class RefreshCatalogUseCase(
  private val catalogRepository: CatalogRepository,
) {
  suspend operator fun invoke(): DomainResult<Int> = catalogRepository.refreshCatalog()
}
