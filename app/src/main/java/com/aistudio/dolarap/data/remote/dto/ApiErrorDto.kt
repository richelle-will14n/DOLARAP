package com.aistudio.dolarap.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
  @param:Json(name = "status") val status: Int? = null,
  @param:Json(name = "code") val code: String? = null,
  @param:Json(name = "message") val message: String? = null,
)
