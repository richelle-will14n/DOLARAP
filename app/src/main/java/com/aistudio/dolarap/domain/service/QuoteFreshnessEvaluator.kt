package com.aistudio.dolarap.domain.service

import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus

/**
 * Avalia se uma cotação local está atualizada, desatualizada ou indisponível.
 *
 * Política:
 * - Sem timestamp local (`fetchedAtMillis == null`): [QuoteFreshnessStatus.UNAVAILABLE].
 * - Timestamp inválido (`<= 0`), relógio atual inválido (`<= 0`) ou timestamp futuro
 *   (`fetchedAtMillis > nowMillis`): tratado defensivamente como [QuoteFreshnessStatus.STALE],
 *   nunca considerando a cotação atualizada por engano.
 * - Idade até 15 minutos (`<= 900_000 ms`): [QuoteFreshnessStatus.FRESH].
 * - Idade acima de 15 minutos (`> 900_000 ms`): [QuoteFreshnessStatus.STALE].
 */
class QuoteFreshnessEvaluator {

  fun evaluate(
    fetchedAtMillis: Long?,
    nowMillis: Long,
  ): QuoteFreshnessStatus {
    if (fetchedAtMillis == null) return QuoteFreshnessStatus.UNAVAILABLE
    if (fetchedAtMillis <= 0L || nowMillis <= 0L) return QuoteFreshnessStatus.STALE
    if (fetchedAtMillis > nowMillis) return QuoteFreshnessStatus.STALE

    val elapsedMillis = nowMillis - fetchedAtMillis
    return if (elapsedMillis <= STALE_THRESHOLD_MILLIS) {
      QuoteFreshnessStatus.FRESH
    } else {
      QuoteFreshnessStatus.STALE
    }
  }

  fun isStale(
    fetchedAtMillis: Long?,
    nowMillis: Long,
  ): Boolean = evaluate(fetchedAtMillis, nowMillis) == QuoteFreshnessStatus.STALE

  companion object {
    const val STALE_THRESHOLD_MINUTES = 15L
    const val STALE_THRESHOLD_MILLIS = STALE_THRESHOLD_MINUTES * 60L * 1000L
  }
}
