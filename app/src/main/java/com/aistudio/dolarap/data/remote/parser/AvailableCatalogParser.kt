package com.aistudio.dolarap.data.remote.parser

import com.aistudio.dolarap.data.local.catalog.ComplementaryCurrencyCatalog
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.remote.AwesomeApiConfig
import com.aistudio.dolarap.data.remote.model.RemoteError
import com.aistudio.dolarap.data.remote.model.RemoteResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.EOFException
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

class AvailableCatalogParser(
  private val moshi: Moshi,
) {

  private val mapAdapter: JsonAdapter<Map<String, String?>> by lazy {
    val mapType =
      Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    moshi.adapter(mapType)
  }

  fun parseJsonToBrlPairs(
    rawJson: String,
    updatedAtMillis: Long,
  ): RemoteResult<List<AvailablePairEntity>> {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) {
      return RemoteResult.Error(RemoteError.EmptyOrIncompleteResponse("Catálogo JSON vazio"))
    }
    if (!trimmed.startsWith("{")) {
      return RemoteResult.Error(
        RemoteError.InvalidResponse("Resposta de /json/available não é um objeto JSON")
      )
    }

    return try {
      val parsedMap = mapAdapter.fromJson(trimmed)
      if (parsedMap.isNullOrEmpty()) {
        return RemoteResult.Error(RemoteError.EmptyOrIncompleteResponse("Catálogo JSON sem pares"))
      }

      val brlPairs =
        parsedMap.entries
          .mapNotNull { (pairKey, description) ->
            buildBrlPairEntity(pairKey, description, updatedAtMillis)
          }
          .distinctBy { it.pairCode }
          .sortedBy { it.baseCode }

      if (brlPairs.isEmpty()) {
        RemoteResult.Error(
          RemoteError.EmptyOrIncompleteResponse("Nenhum par *-BRL encontrado no catálogo JSON")
        )
      } else {
        RemoteResult.Success(brlPairs)
      }
    } catch (e: JsonDataException) {
      RemoteResult.Error(RemoteError.InvalidResponse("JSON de catálogo inválido: ${e.message}"))
    } catch (e: JsonEncodingException) {
      RemoteResult.Error(RemoteError.InvalidResponse("JSON de catálogo malformado: ${e.message}"))
    } catch (e: EOFException) {
      RemoteResult.Error(RemoteError.InvalidResponse("JSON de catálogo truncado: ${e.message}"))
    } catch (e: Exception) {
      RemoteResult.Error(RemoteError.Unexpected(e.message))
    }
  }

  fun parseXmlToBrlPairs(
    rawXml: String,
    updatedAtMillis: Long,
  ): RemoteResult<List<AvailablePairEntity>> {
    val trimmed = rawXml.trim()
    if (trimmed.isEmpty()) {
      return RemoteResult.Error(RemoteError.EmptyOrIncompleteResponse("Catálogo XML vazio"))
    }

    return try {
      val factory =
        DocumentBuilderFactory.newInstance().apply {
          isNamespaceAware = false
          runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
          runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
          runCatching {
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
          }
        }
      val document = factory.newDocumentBuilder().parse(InputSource(StringReader(trimmed)))
      val root =
        document.documentElement
          ?: return RemoteResult.Error(RemoteError.InvalidResponse("XML sem elemento raiz"))

      val parsedPairs = mutableListOf<AvailablePairEntity>()
      val childNodes = root.childNodes

      for (i in 0 until childNodes.length) {
        val node = childNodes.item(i)
        if (node.nodeType != Node.ELEMENT_NODE) continue
        val element = node as Element
        val tagName = element.tagName.trim()

        if (tagName.equals("item", ignoreCase = true)) {
          parseItemElement(element, updatedAtMillis)?.let { parsedPairs.add(it) }
        } else if (tagName.contains("-")) {
          buildBrlPairEntity(tagName, element.textContent, updatedAtMillis)?.let {
            parsedPairs.add(it)
          }
        }
      }

      val distinctSorted = parsedPairs.distinctBy { it.pairCode }.sortedBy { it.baseCode }
      if (distinctSorted.isEmpty()) {
        RemoteResult.Error(
          RemoteError.EmptyOrIncompleteResponse("Nenhum par *-BRL encontrado no catálogo XML")
        )
      } else {
        RemoteResult.Success(distinctSorted)
      }
    } catch (e: Exception) {
      RemoteResult.Error(RemoteError.InvalidResponse("XML de catálogo inválido: ${e.message}"))
    }
  }

  private fun parseItemElement(
    itemElement: Element,
    updatedAtMillis: Long,
  ): AvailablePairEntity? {
    val coinTag = findChildText(itemElement, "coin")
    if (!coinTag.isNullOrBlank()) {
      val name = findChildText(itemElement, "name")
      return buildBrlPairEntity(coinTag, name, updatedAtMillis)
    }

    val code = findChildText(itemElement, "code")?.uppercase(Locale.ROOT)
    val codeIn = findChildText(itemElement, "codein")?.uppercase(Locale.ROOT)
    if (!code.isNullOrBlank() && codeIn == AwesomeApiConfig.TARGET_QUOTE_CODE) {
      val name = findChildText(itemElement, "name")
      return buildBrlPairEntity("$code-$codeIn", name, updatedAtMillis)
    }
    return null
  }

  private fun findChildText(parent: Element, tagName: String): String? {
    val list = parent.getElementsByTagName(tagName)
    if (list.length == 0) return null
    return list.item(0)?.textContent?.trim()
  }

  private fun buildBrlPairEntity(
    rawPairKey: String,
    rawDescription: String?,
    updatedAtMillis: Long,
  ): AvailablePairEntity? {
    val normalizedPair = rawPairKey.trim().uppercase(Locale.ROOT)
    if (!normalizedPair.matches(VALID_BRL_PAIR_REGEX)) return null

    val cleanDescription = rawDescription?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val baseCode = normalizedPair.substringBefore("-")
    val quoteCode = AwesomeApiConfig.TARGET_QUOTE_CODE
    val friendlyName = ComplementaryCurrencyCatalog.resolveFriendlyName(baseCode, cleanDescription)

    return AvailablePairEntity(
      pairCode = normalizedPair,
      baseCode = baseCode,
      quoteCode = quoteCode,
      apiDescription = cleanDescription,
      friendlyName = friendlyName,
      isAvailable = true,
      updatedAtMillis = updatedAtMillis,
    )
  }

  private companion object {
    val VALID_BRL_PAIR_REGEX = Regex("^[A-Z]{3}-BRL$")
  }
}
