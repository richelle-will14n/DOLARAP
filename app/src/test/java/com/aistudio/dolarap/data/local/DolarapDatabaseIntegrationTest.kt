package com.aistudio.dolarap.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dolarap.data.local.dao.AvailablePairDao
import com.aistudio.dolarap.data.local.dao.CurrencyDao
import com.aistudio.dolarap.data.local.dao.QuoteDao
import com.aistudio.dolarap.data.local.dao.SyncMetadataDao
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyEntity
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class DolarapDatabaseIntegrationTest {

  private lateinit var context: Context
  private lateinit var database: DolarapDatabase
  private lateinit var currencyDao: CurrencyDao
  private lateinit var quoteDao: QuoteDao
  private lateinit var availablePairDao: AvailablePairDao
  private lateinit var syncMetadataDao: SyncMetadataDao

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, DolarapDatabase::class.java)
        .allowMainThreadQueries()
        .addCallback(DolarapDatabase.seedCallback())
        .build()
    currencyDao = database.currencyDao()
    quoteDao = database.quoteDao()
    availablePairDao = database.availablePairDao()
    syncMetadataDao = database.syncMetadataDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `onCreate seeds the 10 default currencies automatically`() = runBlocking {
    val currencies = currencyDao.observeSelectedCurrencies().first()

    assertEquals(10, currencies.size)
    assertEquals(
      listOf("USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY", "ARS", "CNY", "MXN"),
      currencies.map { it.code },
    )
    assertTrue(currencies.all { it.isDefault })
    assertTrue(currencies.none { it.isFavorite })
  }

  @Test
  fun `ensureDefaultCurrenciesSeeded is idempotent and preserves favorites and custom currencies`() =
    runBlocking {
      // Dispara abertura e seed inicial
      assertEquals(10, currencyDao.countCurrencies())

      // Marca USD como favorita e adiciona moeda personalizada CLP
      currencyDao.updateFavorite("USD", true)
      currencyDao.upsertCurrency(
        SelectedCurrencyEntity(
          code = "CLP",
          pairCode = "CLP-BRL",
          displayName = "Peso Chileno",
          isDefault = false,
          isFavorite = true,
          sortOrder = 10,
          addedAtMillis = 1_700_000_000_000L,
        )
      )

      // Executa seed novamente
      DolarapDatabase.ensureDefaultCurrenciesSeeded(currencyDao)

      val currencies = currencyDao.getAllCurrenciesSnapshot()
      assertEquals(11, currencies.size)
      assertTrue(currencies.first { it.code == "USD" }.isFavorite)
      assertTrue(currencies.first { it.code == "CLP" }.isFavorite)
    }

  @Test
  fun `deleteNonDefaultByCode protects default currencies and deletes user-added currencies`() =
    runBlocking {
      currencyDao.upsertCurrency(
        SelectedCurrencyEntity(
          code = "PYG",
          pairCode = "PYG-BRL",
          displayName = "Guarani Paraguaio",
          isDefault = false,
          isFavorite = false,
          sortOrder = 10,
          addedAtMillis = 1_700_000_000_000L,
        )
      )
      assertEquals(11, currencyDao.countCurrencies())

      val deletedDefaultCount = currencyDao.deleteNonDefaultByCode("USD")
      assertEquals(0, deletedDefaultCount)
      assertNotNull(currencyDao.getCurrencyByCode("USD"))

      val deletedCustomCount = currencyDao.deleteNonDefaultByCode("PYG")
      assertEquals(1, deletedCustomCount)
      assertNull(currencyDao.getCurrencyByCode("PYG"))
      assertEquals(10, currencyDao.countCurrencies())
    }

  @Test
  fun `reactive Flow emits updated SelectedCurrencyWithQuote and favorites when quotes or favorites change`() =
    runBlocking {
      val emissions =
        Channel<List<com.aistudio.dolarap.data.local.entity.SelectedCurrencyWithQuote>>(
          Channel.UNLIMITED
        )
      val job =
        launch(Dispatchers.IO) {
          currencyDao.observeSelectedCurrenciesWithQuotes().collect { emissions.send(it) }
        }

      try {
        // Emissão 1: estado inicial sem cotações
        val initial = withTimeout(5_000) { emissions.receive() }
        assertNull(initial.first { it.currency.code == "USD" }.quote)

        // Emissão 2: inserção de cotação USD-BRL
        quoteDao.upsert(
          QuoteEntity(
            pairCode = "USD-BRL",
            code = "USD",
            codeIn = "BRL",
            apiName = "Dólar Americano/Real Brasileiro",
            bid = "5.7276",
            ask = "5.7282",
            varBid = "-0.0054",
            pctChange = "-0.09",
            high = "5.7340",
            low = "5.7279",
            apiTimestampSeconds = 1_700_000_000L,
            createDate = "2026-11-15 12:00:00",
            fetchedAtMillis = 1_700_000_100_000L,
          )
        )
        val afterInsert = withTimeout(5_000) { emissions.receive() }
        assertEquals("5.7276", afterInsert.first { it.currency.code == "USD" }.quote?.bid)

        // Emissão 3: atualização de cotação USD-BRL
        quoteDao.upsert(
          QuoteEntity(
            pairCode = "USD-BRL",
            code = "USD",
            codeIn = "BRL",
            apiName = "Dólar Americano/Real Brasileiro",
            bid = "5.8000",
            ask = "5.8050",
            varBid = "0.0724",
            pctChange = "1.26",
            high = "5.8100",
            low = "5.7200",
            apiTimestampSeconds = 1_700_000_500L,
            createDate = "2026-11-15 12:10:00",
            fetchedAtMillis = 1_700_000_500_000L,
          )
        )
        val afterUpdate = withTimeout(5_000) { emissions.receive() }
        assertEquals("5.8000", afterUpdate.first { it.currency.code == "USD" }.quote?.bid)

        // Verifica fluxo reativo de favoritos com cotação associada
        assertTrue(currencyDao.observeFavoriteCurrenciesWithQuotes().first().isEmpty())
        currencyDao.toggleFavorite("USD")
        val favorites = currencyDao.observeFavoriteCurrenciesWithQuotes().first()
        assertEquals(1, favorites.size)
        assertEquals("USD", favorites.first().currency.code)
        assertEquals("5.8000", favorites.first().quote?.bid)
      } finally {
        job.cancel()
        emissions.close()
      }
    }

  @Test
  fun `favorites and selected custom currencies persist across database reopen`() = runBlocking {
    val dbFile = File(context.cacheDir, "dolarap_reopen_test.db")
    dbFile.delete()

    val firstDb =
      Room.databaseBuilder(context, DolarapDatabase::class.java, dbFile.absolutePath)
        .allowMainThreadQueries()
        .addCallback(DolarapDatabase.seedCallback())
        .build()

    firstDb.currencyDao().updateFavorite("EUR", true)
    firstDb.currencyDao().upsertCurrency(
      SelectedCurrencyEntity(
        code = "UYU",
        pairCode = "UYU-BRL",
        displayName = "Peso Uruguaio",
        isDefault = false,
        isFavorite = true,
        sortOrder = 10,
        addedAtMillis = 1_700_000_000_000L,
      )
    )
    firstDb.close()

    val reopenedDb =
      Room.databaseBuilder(context, DolarapDatabase::class.java, dbFile.absolutePath)
        .allowMainThreadQueries()
        .addCallback(DolarapDatabase.seedCallback())
        .build()

    try {
      val all = reopenedDb.currencyDao().observeSelectedCurrencies().first()
      val favorites = reopenedDb.currencyDao().observeFavoriteCurrencies().first()

      assertEquals(11, all.size)
      assertEquals(listOf("EUR", "UYU"), favorites.map { it.code })
      assertFalse(all.first { it.code == "UYU" }.isDefault)
    } finally {
      reopenedDb.close()
      dbFile.delete()
    }
  }

  @Test
  fun `AvailablePairDao filters BRL pairs, searches by code or name, and toggles availability`() =
    runBlocking {
      availablePairDao.upsertAll(
        listOf(
          AvailablePairEntity(
            pairCode = "USD-BRL",
            baseCode = "USD",
            quoteCode = "BRL",
            apiDescription = "Dólar Americano/Real Brasileiro",
            friendlyName = "Dólar Americano",
            isAvailable = true,
            updatedAtMillis = 1000L,
          ),
          AvailablePairEntity(
            pairCode = "CHF-BRL",
            baseCode = "CHF",
            quoteCode = "BRL",
            apiDescription = "Franco Suíço/Real Brasileiro",
            friendlyName = "Franco Suíço",
            isAvailable = true,
            updatedAtMillis = 1000L,
          ),
          AvailablePairEntity(
            pairCode = "EUR-USD",
            baseCode = "EUR",
            quoteCode = "USD",
            apiDescription = "Euro/Dólar Americano",
            friendlyName = "Euro",
            isAvailable = true,
            updatedAtMillis = 1000L,
          ),
        )
      )

      val brlPairs = availablePairDao.observeSupportedBrlPairs().first()
      assertEquals(listOf("CHF-BRL", "USD-BRL"), brlPairs.map { it.pairCode })
      assertTrue(availablePairDao.isPairSupported("CHF-BRL"))
      assertFalse(availablePairDao.isPairSupported("XYZ-BRL"))

      val byName = availablePairDao.searchSupportedBrlPairs("franco").first()
      assertEquals(listOf("CHF-BRL"), byName.map { it.pairCode })

      val byCode = availablePairDao.searchSupportedBrlPairs("usd").first()
      assertEquals(listOf("USD-BRL"), byCode.map { it.pairCode })

      availablePairDao.setPairAvailability("CHF-BRL", false)
      assertFalse(availablePairDao.isPairSupported("CHF-BRL"))
      assertEquals(1, availablePairDao.countSupportedBrlPairs())
    }

  @Test
  fun `SyncMetadataDao inserts, updates, observes, and clears sync metadata`() = runBlocking {
    assertNull(syncMetadataDao.observeSyncMetadata().first())

    val metadata =
      SyncMetadataEntity(
        lastSuccessfulSyncAtMillis = 1_700_000_000_000L,
        lastAttemptAtMillis = 1_700_000_000_000L,
        lastErrorType = null,
        lastCatalogSyncAtMillis = 1_699_999_000_000L,
      )
    syncMetadataDao.upsert(metadata)

    val saved = syncMetadataDao.observeSyncMetadata().first()
    assertEquals(metadata, saved)

    syncMetadataDao.clear()
    assertNull(syncMetadataDao.getSyncMetadata())
  }
}
