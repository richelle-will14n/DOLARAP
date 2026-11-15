package com.aistudio.dolarap.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuoteDto(
  @Json(name = "code") val code: String? = null,
  @Json(name = "codein") val codein: String? = null,
  @Json(name = "name") val name: String? = null,
  @Json(name = "high") val high: String? = null,
  @Json(name = "low") val low: String? = null,
  @Json(name = "varBid") val varBid: String? = null,
  @Json(name = "pctChange") val pctChange: String? = null,
  @Json(name = "bid") val bid: String? = null,
  @Json(name = "ask") val ask: String? = null,
  @Json(name = "timestamp") val timestamp: String? = null,
  @Json(name = "create_date") val createDate: String? = null,
)
