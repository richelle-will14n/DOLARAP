package com.aistudio.dolarap.domain.service

fun interface TimeProvider {
  fun nowMillis(): Long
}
