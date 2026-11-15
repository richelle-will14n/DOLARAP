package com.aistudio.dolarap.data.local.catalog

import java.text.Normalizer
import java.util.Currency
import java.util.Locale

data class CurrencyCatalogMetadata(
  val code: String,
  val friendlyNamePtBr: String,
  val symbol: String,
  val searchAliases: List<String> = emptyList(),
)

data class DefaultCurrencySpec(
  val code: String,
  val pairCode: String,
  val displayName: String,
  val sortOrder: Int,
)

/**
 * Catálogo complementar local para enriquecimento de nomes amigáveis em PT-BR e símbolos.
 *
 * Regra de segurança: este catálogo NUNCA autoriza a adição ou consulta de uma moeda por conta
 * própria. A disponibilidade real de um par continua condicionada ao catálogo retornado pela API.
 */
object ComplementaryCurrencyCatalog {

  val defaultCurrencies: List<DefaultCurrencySpec> =
    listOf(
      DefaultCurrencySpec("USD", "USD-BRL", "Dólar Americano", 0),
      DefaultCurrencySpec("EUR", "EUR-BRL", "Euro", 1),
      DefaultCurrencySpec("GBP", "GBP-BRL", "Libra Esterlina", 2),
      DefaultCurrencySpec("CAD", "CAD-BRL", "Dólar Canadense", 3),
      DefaultCurrencySpec("AUD", "AUD-BRL", "Dólar Australiano", 4),
      DefaultCurrencySpec("CHF", "CHF-BRL", "Franco Suíço", 5),
      DefaultCurrencySpec("JPY", "JPY-BRL", "Iene Japonês", 6),
      DefaultCurrencySpec("ARS", "ARS-BRL", "Peso Argentino", 7),
      DefaultCurrencySpec("CNY", "CNY-BRL", "Yuan Chinês", 8),
      DefaultCurrencySpec("MXN", "MXN-BRL", "Peso Mexicano", 9),
    )

  private val defaultCodesSet: Set<String> = defaultCurrencies.mapTo(HashSet()) { it.code }

  private val curatedMetadata: Map<String, CurrencyCatalogMetadata> =
    listOf(
        CurrencyCatalogMetadata("USD", "Dólar Americano", "$", listOf("estados unidos", "eua")),
        CurrencyCatalogMetadata("EUR", "Euro", "€", listOf("uniao europeia", "europa")),
        CurrencyCatalogMetadata("GBP", "Libra Esterlina", "£", listOf("reino unido", "uk")),
        CurrencyCatalogMetadata("CAD", "Dólar Canadense", "CA$", listOf("canada")),
        CurrencyCatalogMetadata("AUD", "Dólar Australiano", "A$", listOf("australia")),
        CurrencyCatalogMetadata("CHF", "Franco Suíço", "CHF", listOf("suica")),
        CurrencyCatalogMetadata("JPY", "Iene Japonês", "¥", listOf("japao")),
        CurrencyCatalogMetadata("ARS", "Peso Argentino", "AR$", listOf("argentina")),
        CurrencyCatalogMetadata("CNY", "Yuan Chinês", "CN¥", listOf("china", "renminbi")),
        CurrencyCatalogMetadata("MXN", "Peso Mexicano", "MX$", listOf("mexico")),
        CurrencyCatalogMetadata("BRL", "Real Brasileiro", "R$", listOf("brasil")),
        CurrencyCatalogMetadata("PYG", "Guarani Paraguaio", "₲", listOf("paraguai")),
        CurrencyCatalogMetadata("UYU", "Peso Uruguaio", "UY$", listOf("uruguai")),
        CurrencyCatalogMetadata("CLP", "Peso Chileno", "CL$", listOf("chile")),
        CurrencyCatalogMetadata("COP", "Peso Colombiano", "CO$", listOf("colombia")),
        CurrencyCatalogMetadata("PEN", "Sol Peruano", "S/", listOf("peru")),
        CurrencyCatalogMetadata("BOB", "Boliviano", "Bs", listOf("bolivia")),
        CurrencyCatalogMetadata("NZD", "Dólar Neozelandês", "NZ$", listOf("nova zelandia")),
        CurrencyCatalogMetadata("SEK", "Coroa Sueca", "kr", listOf("suecia")),
        CurrencyCatalogMetadata("NOK", "Coroa Norueguesa", "kr", listOf("noruega")),
        CurrencyCatalogMetadata("DKK", "Coroa Dinamarquesa", "kr", listOf("dinamarca")),
        CurrencyCatalogMetadata("PLN", "Zloty Polonês", "zł", listOf("polonia")),
        CurrencyCatalogMetadata("TRY", "Lira Turca", "₺", listOf("turquia")),
        CurrencyCatalogMetadata("ZAR", "Rand Sul-Africano", "R", listOf("africa do sul")),
        CurrencyCatalogMetadata("INR", "Rúpia Indiana", "₹", listOf("india")),
        CurrencyCatalogMetadata("KRW", "Won Sul-Coreano", "₩", listOf("coreia do sul")),
        CurrencyCatalogMetadata("SGD", "Dólar de Singapura", "S$", listOf("singapura")),
        CurrencyCatalogMetadata("HKD", "Dólar de Hong Kong", "HK$", listOf("hong kong")),
        CurrencyCatalogMetadata("ILS", "Novo Shekel Israelense", "₪", listOf("israel")),
        CurrencyCatalogMetadata("AED", "Dirham dos Emirados", "AED", listOf("emirados arabes", "dubai")),
        CurrencyCatalogMetadata("SAR", "Riyal Saudita", "SAR", listOf("arabia saudita")),
        CurrencyCatalogMetadata("RUB", "Rublo Russo", "₽", listOf("russia")),
        CurrencyCatalogMetadata("BTC", "Bitcoin", "₿", listOf("cripto")),
        CurrencyCatalogMetadata("ETH", "Ethereum", "ETH", listOf("cripto")),
      )
      .associateBy { it.code }

  fun isDefaultCurrency(code: String): Boolean = defaultCodesSet.contains(code.uppercase(Locale.ROOT))

  fun findMetadata(code: String): CurrencyCatalogMetadata? =
    curatedMetadata[code.uppercase(Locale.ROOT)]

  private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

  fun resolveFriendlyName(code: String, apiDescription: String? = null): String {
    val normalizedCode = code.uppercase(Locale.ROOT).trim()
    curatedMetadata[normalizedCode]?.let {
      return it.friendlyNamePtBr
    }

    val fromApi =
      apiDescription
        ?.substringBefore('/')
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals(normalizedCode, ignoreCase = true) }
    if (fromApi != null) return fromApi

    val javaCurrencyName =
      runCatching {
          Currency.getInstance(normalizedCode).getDisplayName(ptBrLocale)
        }
        .getOrNull()
        ?.takeIf { it.isNotBlank() && !it.equals(normalizedCode, ignoreCase = true) }
    if (javaCurrencyName != null) {
      return javaCurrencyName.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(ptBrLocale) else ch.toString()
      }
    }

    return normalizedCode
  }

  fun resolveSymbol(code: String): String {
    val normalizedCode = code.uppercase(Locale.ROOT).trim()
    curatedMetadata[normalizedCode]?.let {
      return it.symbol
    }
    return runCatching { Currency.getInstance(normalizedCode).getSymbol(ptBrLocale) }
      .getOrDefault(normalizedCode)
  }

  fun normalizeForSearch(input: String): String {
    val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase(Locale.ROOT)
  }
}
