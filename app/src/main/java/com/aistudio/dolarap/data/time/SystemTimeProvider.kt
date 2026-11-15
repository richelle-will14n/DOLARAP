package com.aistudio.dolarap.data.time

import com.aistudio.dolarap.domain.service.TimeProvider

object SystemTimeProvider : TimeProvider {
  override fun nowMillis(): Long = System.currentTimeMillis()
}
