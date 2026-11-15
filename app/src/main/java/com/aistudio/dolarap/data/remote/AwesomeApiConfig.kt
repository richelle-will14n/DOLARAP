package com.aistudio.dolarap.data.remote

object AwesomeApiConfig {
  const val BASE_URL = "https://economia.awesomeapi.com.br/"
  const val ENDPOINT_LAST_QUOTES = "json/last/{pairs}"
  const val ENDPOINT_AVAILABLE_JSON = "json/available"
  const val ENDPOINT_AVAILABLE_XML = "xml/available"

  const val CONNECT_TIMEOUT_SECONDS = 15L
  const val READ_TIMEOUT_SECONDS = 15L
  const val WRITE_TIMEOUT_SECONDS = 15L

  const val ERROR_CODE_COIN_NOT_EXISTS = "CoinNotExists"
  const val TARGET_QUOTE_CODE = "BRL"
}
