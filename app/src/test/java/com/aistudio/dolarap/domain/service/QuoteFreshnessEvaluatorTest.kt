package com.aistudio.dolarap.domain.service

import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteFreshnessEvaluatorTest {

  private val evaluator = QuoteFreshnessEvaluator()
  private val baseNowMillis = 1_700_000_000_000L
  private val fifteenMinutesMillis = 15L * 60L * 1000L

  @Test
  fun `returns UNAVAILABLE when there is no local fetchedAtMillis`() {
    assertEquals(
      QuoteFreshnessStatus.UNAVAILABLE,
      evaluator.evaluate(fetchedAtMillis = null, nowMillis = baseNowMillis),
    )
    assertFalse(evaluator.isStale(fetchedAtMillis = null, nowMillis = baseNowMillis))
  }

  @Test
  fun `returns FRESH up to and including exact 15 minutes boundary`() {
    assertEquals(
      QuoteFreshnessStatus.FRESH,
      evaluator.evaluate(fetchedAtMillis = baseNowMillis, nowMillis = baseNowMillis),
    )
    assertEquals(
      QuoteFreshnessStatus.FRESH,
      evaluator.evaluate(
        fetchedAtMillis = baseNowMillis - (fifteenMinutesMillis - 1L),
        nowMillis = baseNowMillis,
      ),
    )
    assertEquals(
      QuoteFreshnessStatus.FRESH,
      evaluator.evaluate(
        fetchedAtMillis = baseNowMillis - fifteenMinutesMillis,
        nowMillis = baseNowMillis,
      ),
    )
  }

  @Test
  fun `returns STALE immediately after 15 minutes boundary`() {
    val fetched = baseNowMillis - (fifteenMinutesMillis + 1L)
    assertEquals(
      QuoteFreshnessStatus.STALE,
      evaluator.evaluate(fetchedAtMillis = fetched, nowMillis = baseNowMillis),
    )
    assertTrue(evaluator.isStale(fetchedAtMillis = fetched, nowMillis = baseNowMillis))
  }

  @Test
  fun `treats invalid clock or future fetchedAtMillis defensively as STALE`() {
    assertEquals(
      QuoteFreshnessStatus.STALE,
      evaluator.evaluate(fetchedAtMillis = baseNowMillis + 60_000L, nowMillis = baseNowMillis),
    )
    assertEquals(
      QuoteFreshnessStatus.STALE,
      evaluator.evaluate(fetchedAtMillis = 0L, nowMillis = baseNowMillis),
    )
    assertEquals(
      QuoteFreshnessStatus.STALE,
      evaluator.evaluate(fetchedAtMillis = -1000L, nowMillis = baseNowMillis),
    )
    assertEquals(
      QuoteFreshnessStatus.STALE,
      evaluator.evaluate(fetchedAtMillis = baseNowMillis, nowMillis = 0L),
    )
  }
}
