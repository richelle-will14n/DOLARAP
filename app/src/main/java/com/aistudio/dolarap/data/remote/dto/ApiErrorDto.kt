package com.aistudio.dolarap.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
  @Json(name = "status") val status: Int? = null,
  @Json(name = "code") val code: String? = null,
  @Json(name = "message") val message: String? = null,
)
