package com.aistudio.dolarap.domain.model

data class AvailableCurrency(
  val pairCode: String,
  val code: String,
  val quoteCode: String,
  val friendlyName: String,
  val apiDescription: String,
  val symbol: String,
  val isDefault: Boolean,
  val isSelected: Boolean,
  val isAvailable: Boolean,
  val updatedAtMillis: Long,
)
