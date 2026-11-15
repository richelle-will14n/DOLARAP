package com.aistudio.dolarap.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SelectedCurrencyWithQuote(
  @Embedded val currency: SelectedCurrencyEntity,
  @Relation(
    parentColumn = "pairCode",
    entityColumn = "pairCode",
  )
  val quote: QuoteEntity?,
)
