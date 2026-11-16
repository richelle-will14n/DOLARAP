package com.aistudio.dolarap.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuoteDto(
  @param:Json(name = "code") val code: String? = null,
  @param:Json(name = "codein") val codein: String? = null,
  @param:Json(name = "name") val name: String? = null,
  @param:Json(name = "high") val high: String? = null,
  @param:Json(name = "low") val low: String? = null,
  @param:Json(name = "varBid") val varBid: String? = null,
  @param:Json(name = "pctChange") val pctChange: String? = null,
  @param:Json(name = "bid") val bid: String? = null,
  @param:Json(name = "ask") val ask: String? = null,
  @param:Json(name = "timestamp") val timestamp: String? = null,
  @param:Json(name = "create_date") val createDate: String? = null,
)
