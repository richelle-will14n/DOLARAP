package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.data.local.dao.AvailablePairDao
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.remote.dto.ApiErrorDto
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.model.RemoteError
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import okhttp3.ResponseBody
import retrofit2.Response

class AwesomeApiRemoteDataSource(
  private val service: AwesomeApiService,
  private val moshi: Moshi,
  private val quoteRemoteMapper: QuoteRemoteMapper,
  private val availableCatalogParser: AvailableCatalogParser,
  private val availablePairDao: AvailablePairDao,
) {

  private val apiErrorAdapter by lazy { moshi.adapter(ApiErrorDto::class.java) }
  private val sessionInvalidPairs = ConcurrentHashMap.newKeySet<String>()

  suspend fun fetchLastQuotes(
    pairCodes: List<String>,
    fetchedAtMillis: Long = System.currentTimeMillis(),
  ): RemoteResult<List<QuoteEntity>> {
    val normalizedPairs =
      pairCodes
        .map { it.trim().uppercase(Locale.ROOT) }
        .filter { it.matches(PAIR_REGEX) }
        .distinct()

    if (normalizedPairs.isEmpty()) {
      return RemoteResult.Error(RemoteError.InvalidRequest("Nenhum par de moedas válido informado"))
    }

    val hasCachedCatalog = availablePairDao.countSupportedBrlPairs() > 0
    val eligiblePairs =
      normalizedPairs.filter { pair ->
        if (sessionInvalidPairs.contains(pair)) {
          false
        } else if (hasCachedCatalog) {
          availablePairDao.isPairSupported(pair)
        } else {
          true
        }
      }

    if (eligiblePairs.isEmpty()) {
      return RemoteResult.Error(
        RemoteError.CoinNotExists(
          pairCode = normalizedPairs.first(),
          apiMessage = "Par(es) indisponível(is) ou não suportado(s) no catálogo local",
        )
      )
    }

    return try {
      val response = service.getLastQuotes(eligiblePairs.joinToString(","))
      if (response.isSuccessful) {
        val dtoMap = response.body()
        if (dtoMap.isNullOrEmpty()) {
          return RemoteResult.Error(
            RemoteError.EmptyOrIncompleteResponse("Resposta de cotações vazia")
          )
        }
        val mapped = quoteRemoteMapper.mapToEntities(dtoMap, fetchedAtMillis)
        if (mapped.isEmpty()) {
          RemoteResult.Error(
            RemoteError.EmptyOrIncompleteResponse(
              "Nenhum item de cotação possuía campos obrigatórios válidos"
            )
          )
        } else {
          RemoteResult.Success(mapped)
        }
      } else {
        handleHttpError(response.code(), response.errorBody(), eligiblePairs)
      }
    } catch (e: Exception) {
      mapException(e)
    }
  }

  /**
   * Sincroniza o catálogo de pares disponíveis na AwesomeAPI para BRL.
   *
   * `/xml/available` é a fonte oficialmente documentada pela AwesomeAPI. `/json/available` é
   * tratado somente como tentativa experimental; se falhar por erro de endpoint ou formato
   * inválido/incompatível, aciona o fallback oficial `/xml/available`. Não executa segunda
   * requisição em HTTP 429 (rate limit), HTTP 5xx (indisponibilidade do servidor), timeout ou
   * ausência de conexão, preservando o cache existente no Room em caso de falha.
   */
  suspend fun syncAvailableBrlCatalog(
    updatedAtMillis: Long = System.currentTimeMillis(),
  ): RemoteResult<List<AvailablePairEntity>> {
    val jsonAttempt = fetchAndParseCatalogJson(updatedAtMillis)
    if (jsonAttempt is RemoteResult.Success) {
      availablePairDao.upsertAll(jsonAttempt.data)
      sessionInvalidPairs.removeAll(jsonAttempt.data.map { it.pairCode }.toSet())
      return jsonAttempt
    }

    val jsonError = (jsonAttempt as RemoteResult.Error).error
    if (!shouldFallbackToXml(jsonError)) {
      return jsonAttempt
    }

    val xmlAttempt = fetchAndParseCatalogXml(updatedAtMillis)
    if (xmlAttempt is RemoteResult.Success) {
      availablePairDao.upsertAll(xmlAttempt.data)
      sessionInvalidPairs.removeAll(xmlAttempt.data.map { it.pairCode }.toSet())
    }
    return xmlAttempt
  }

  private fun shouldFallbackToXml(error: RemoteError): Boolean =
    when (error) {
      RemoteError.NoConnection,
      RemoteError.Timeout,
      is RemoteError.ServerError -> false
      is RemoteError.HttpError -> error.httpCode != 429
      is RemoteError.InvalidResponse,
      is RemoteError.EmptyOrIncompleteResponse,
      is RemoteError.Unexpected -> true
      is RemoteError.CoinNotExists,
      is RemoteError.InvalidRequest -> false
    }

  private suspend fun fetchAndParseCatalogJson(
    updatedAtMillis: Long,
  ): RemoteResult<List<AvailablePairEntity>> =
    try {
      val response = service.getAvailablePairsJson()
      if (!response.isSuccessful) {
        mapCatalogHttpError(response)
      } else {
        val rawBody = response.body()?.string().orEmpty()
        availableCatalogParser.parseJsonToBrlPairs(rawBody, updatedAtMillis)
      }
    } catch (e: Exception) {
      mapException(e)
    }

  private suspend fun fetchAndParseCatalogXml(
    updatedAtMillis: Long,
  ): RemoteResult<List<AvailablePairEntity>> =
    try {
      val response = service.getAvailablePairsXml()
      if (!response.isSuccessful) {
        mapCatalogHttpError(response)
      } else {
        val rawBody = response.body()?.string().orEmpty()
        availableCatalogParser.parseXmlToBrlPairs(rawBody, updatedAtMillis)
      }
    } catch (e: Exception) {
      mapException(e)
    }

  private suspend fun handleHttpError(
    httpCode: Int,
    errorBody: ResponseBody?,
    requestedPairs: List<String>,
  ): RemoteResult.Error {
    val parsedError = parseApiError(errorBody)
    if (
      httpCode == 404 &&
        parsedError?.code.equals(AwesomeApiConfig.ERROR_CODE_COIN_NOT_EXISTS, ignoreCase = true)
    ) {
      val missingPair =
        extractMissingPairFromMessage(parsedError?.message, requestedPairs)
          ?: requestedPairs.singleOrNull()
      if (missingPair != null) {
        sessionInvalidPairs.add(missingPair)
        availablePairDao.setPairAvailability(missingPair, false)
      }
      return RemoteResult.Error(
        RemoteError.CoinNotExists(pairCode = missingPair, apiMessage = parsedError?.message)
      )
    }

    return if (httpCode in 400..499) {
      RemoteResult.Error(
        RemoteError.HttpError(
          httpCode = httpCode,
          apiCode = parsedError?.code,
          apiMessage = parsedError?.message,
        )
      )
    } else {
      RemoteResult.Error(
        RemoteError.ServerError(httpCode = httpCode, apiMessage = parsedError?.message)
      )
    }
  }

  private fun mapCatalogHttpError(response: Response<ResponseBody>): RemoteResult.Error {
    val httpCode = response.code()
    val parsedError = parseApiError(response.errorBody())
    return if (httpCode in 400..499) {
      RemoteResult.Error(
        RemoteError.HttpError(
          httpCode = httpCode,
          apiCode = parsedError?.code,
          apiMessage = parsedError?.message,
        )
      )
    } else {
      RemoteResult.Error(
        RemoteError.ServerError(httpCode = httpCode, apiMessage = parsedError?.message)
      )
    }
  }

  private fun parseApiError(errorBody: ResponseBody?): ApiErrorDto? {
    val raw = runCatching { errorBody?.string() }.getOrNull()?.trim() ?: return null
    if (!raw.startsWith("{")) return null
    return runCatching { apiErrorAdapter.fromJson(raw) }.getOrNull()
  }

  private fun extractMissingPairFromMessage(
    message: String?,
    requestedPairs: List<String>,
  ): String? {
    if (message.isNullOrBlank()) return null
    val upperMessage = message.uppercase(Locale.ROOT)
    val requestedSet = requestedPairs.toSet()

    val hyphenatedMatch =
      PAIR_FIND_REGEX.findAll(upperMessage)
        .map { it.value }
        .firstOrNull { it in requestedSet }
    if (hyphenatedMatch != null) return hyphenatedMatch

    return PAIR_COMPACT_FIND_REGEX.findAll(upperMessage)
      .map { "${it.groupValues[1]}-${AwesomeApiConfig.TARGET_QUOTE_CODE}" }
      .firstOrNull { it in requestedSet }
  }

  private fun mapException(exception: Exception): RemoteResult.Error =
    when (exception) {
      is SocketTimeoutException -> RemoteResult.Error(RemoteError.Timeout)
      is EOFException,
      is JsonDataException,
      is JsonEncodingException ->
        RemoteResult.Error(
          RemoteError.InvalidResponse("Falha de desserialização JSON: ${exception.message}")
        )
      is UnknownHostException,
      is ConnectException,
      is NoRouteToHostException,
      is IOException -> RemoteResult.Error(RemoteError.NoConnection)
      else -> RemoteResult.Error(RemoteError.Unexpected(exception.message))
    }

  private companion object {
    val PAIR_REGEX = Regex("^[A-Z]{3}-BRL$")
    val PAIR_FIND_REGEX = Regex("\\b[A-Z]{3}-BRL\\b")
    val PAIR_COMPACT_FIND_REGEX = Regex("\\b([A-Z]{3})BRL\\b")
  }
}
