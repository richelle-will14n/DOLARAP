package com.aistudio.dolarap.presentation.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatador monetário e temporal em Português Brasileiro (`pt-BR`) usando exclusivamente [BigDecimal]
 * (nunca convertendo valores monetários para `Double` ou `Float`).
 */
object BrazilianFormatter {

  private val ptBrLocale = Locale.forLanguageTag("pt-BR")
  private val ptBrSymbols =
    DecimalFormatSymbols(ptBrLocale).apply {
      groupingSeparator = '.'
      decimalSeparator = ','
    }

  fun formatBrlQuoteRate(value: BigDecimal): String {
    val scale = maxOf(2, minOf(4, value.stripTrailingZeros().scale().coerceAtLeast(2)))
    return "R$ ${formatDecimal(value, minFractionDigits = scale, maxFractionDigits = scale)}"
  }

  fun formatCurrencyAmount(
    amount: BigDecimal,
    currencyCode: String,
    symbol: String,
    decimalPlaces: Int,
  ): String {
    val safeDecimals = decimalPlaces.coerceIn(0, 8)
    val rounded = amount.setScale(safeDecimals, RoundingMode.HALF_UP)
    val formattedNumber =
      formatDecimal(
        value = rounded,
        minFractionDigits = safeDecimals,
        maxFractionDigits = safeDecimals,
      )
    val prefix = symbol.ifBlank { currencyCode }
    return "$prefix $formattedNumber"
  }

  fun formatRateValue(rate: BigDecimal): String {
    val normalizedScale = rate.stripTrailingZeros().scale().coerceIn(2, 6)
    return formatDecimal(
      value = rate,
      minFractionDigits = normalizedScale,
      maxFractionDigits = normalizedScale,
    )
  }

  fun formatPercentageChange(pctChange: BigDecimal): String {
    val sign = if (pctChange > BigDecimal.ZERO) "+" else ""
    val formatted = formatDecimal(pctChange, minFractionDigits = 2, maxFractionDigits = 2)
    return "$sign$formatted%"
  }

  fun formatVariationBid(varBid: BigDecimal): String {
    val sign = if (varBid > BigDecimal.ZERO) "+" else ""
    val formatted = formatDecimal(varBid, minFractionDigits = 4, maxFractionDigits = 4)
    return "$sign$formatted"
  }

  fun formatDateTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBrLocale)
    return formatter.format(Date(epochMillis))
  }

  private fun formatDecimal(
    value: BigDecimal,
    minFractionDigits: Int,
    maxFractionDigits: Int,
  ): String {
    val formatter =
      DecimalFormat("#,##0", ptBrSymbols).apply {
        isGroupingUsed = true
        minimumFractionDigits = minFractionDigits
        maximumFractionDigits = maxFractionDigits
        roundingMode = RoundingMode.HALF_UP
      }
    return formatter.format(value)
  }
}
