package com.aistudio.dolarap.data.remote.mapper

import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.remote.dto.QuoteDto
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
    val code = dto.code?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return null
    val codeIn = dto.codein?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return null
    val bid = normalizeDecimalString(dto.bid) ?: return null
    val ask = normalizeDecimalString(dto.ask) ?: return null

    val varBid = normalizeDecimalString(dto.varBid) ?: "0"
    val pctChange = normalizeDecimalString(dto.pctChange) ?: "0"
    val high = normalizeDecimalString(dto.high) ?: bid
    val low = normalizeDecimalString(dto.low) ?: bid
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

  private fun normalizeDecimalString(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // Valida sintaxe decimal exata sem converter para Double/Float
    return if (trimmed.toBigDecimalOrNull() != null) trimmed else null
  }
}
