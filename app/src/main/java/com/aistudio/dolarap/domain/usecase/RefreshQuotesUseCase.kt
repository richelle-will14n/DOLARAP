package com.aistudio.dolarap.domain.usecase

import com.aistudio.dolarap.domain.model.DomainResult
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.domain.repository.QuoteRepository

class RefreshQuotesUseCase(
  private val quoteRepository: QuoteRepository,
) {
  suspend operator fun invoke(): DomainResult<SyncStatus> = quoteRepository.refreshQuotes()
}
