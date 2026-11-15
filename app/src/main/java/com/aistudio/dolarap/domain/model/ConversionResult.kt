package com.aistudio.dolarap.domain.model

import java.math.BigDecimal

data class ConversionResult(
  val sourceCode: String,
  val targetCode: String,
  val inputAmount: BigDecimal,
  val convertedAmount: BigDecimal,
  val effectiveRate: BigDecimal,
  val sourceBidRate: BigDecimal?,
  val sourceAskRate: BigDecimal?,
  val targetBidRate: BigDecimal?,
  val targetAskRate: BigDecimal?,
  val decimalPlaces: Int,
  val isStale: Boolean,
)
