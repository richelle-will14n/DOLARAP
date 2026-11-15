package com.aistudio.dolarap.data.remote.model

sealed interface RemoteResult<out T> {
  data class Success<T>(val data: T) : RemoteResult<T>

  data class Error(val error: RemoteError) : RemoteResult<Nothing>
}

sealed interface RemoteError {
  data object NoConnection : RemoteError

  data object Timeout : RemoteError

  data class CoinNotExists(
    val pairCode: String?,
    val apiMessage: String?,
  ) : RemoteError

  data class HttpError(
    val httpCode: Int,
    val apiCode: String? = null,
    val apiMessage: String? = null,
  ) : RemoteError

  data class ServerError(
    val httpCode: Int,
    val apiMessage: String? = null,
  ) : RemoteError

  data class InvalidResponse(val reason: String) : RemoteError

  data class EmptyOrIncompleteResponse(val reason: String) : RemoteError

  data class InvalidRequest(val reason: String) : RemoteError

  data class Unexpected(val message: String?) : RemoteError
}
