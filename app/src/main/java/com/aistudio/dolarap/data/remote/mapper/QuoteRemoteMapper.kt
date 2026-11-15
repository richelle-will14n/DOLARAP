package com.aistudio.dolarap.data.remote.mapper

import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.remote.AwesomeApiConfig
import com.aistudio.dolarap.data.remote.dto.QuoteDto
import java.math.BigDecimal
import java.util.Locale

class QuoteRemoteMapper {

  fun mapToEntities(
    dtoMap: Map<String, QuoteDto>,
    fetchedAtMillis: Long,
  ): List<QuoteEntity> =
    dtoMap.values.mapNotNull { dto -> mapSingleOrNull(dto, fetchedAtMillis) }

  fun mapSingleOrNull(
    dto: QuoteDto,
    fetchedAtMillis: Long,
  ): QuoteEntity? {
    val code =
      dto.code?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.matches(THREE_LETTER_CODE_REGEX) }
        ?: return null
    val codeIn =
      dto.codein?.trim()?.uppercase(Locale.ROOT)?.takeIf { it == AwesomeApiConfig.TARGET_QUOTE_CODE }
        ?: return null
    val bid = normalizePositiveDecimalString(dto.bid) ?: return null
    val ask = normalizePositiveDecimalString(dto.ask) ?: return null

    val varBid = normalizeDecimalString(dto.varBid) ?: "0"
    val pctChange = normalizeDecimalString(dto.pctChange) ?: "0"
    val high = normalizePositiveDecimalString(dto.high) ?: bid
    val low = normalizePositiveDecimalString(dto.low) ?: bid
    val timestampSeconds = dto.timestamp?.trim()?.toLongOrNull() ?: 0L

    return QuoteEntity(
      pairCode = "$code-$codeIn",
      code = code,
      codeIn = codeIn,
      apiName = dto.name?.trim().orEmpty().ifEmpty { "$code/$codeIn" },
      bid = bid,
      ask = ask,
      varBid = varBid,
      pctChange = pctChange,
      high = high,
      low = low,
      apiTimestampSeconds = timestampSeconds,
      createDate = dto.createDate?.trim().orEmpty(),
      fetchedAtMillis = fetchedAtMillis,
    )
  }

  private fun normalizePositiveDecimalString(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val parsed = trimmed.toBigDecimalOrNull() ?: return null
    return if (parsed > BigDecimal.ZERO) trimmed else null
  }

  private fun normalizeDecimalString(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // Valida sintaxe decimal exata sem converter para Double/Float
    return if (trimmed.toBigDecimalOrNull() != null) trimmed else null
  }

  private companion object {
    val THREE_LETTER_CODE_REGEX = Regex("^[A-Z]{3}$")
  }
}
