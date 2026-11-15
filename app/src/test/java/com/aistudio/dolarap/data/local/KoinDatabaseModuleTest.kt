package com.aistudio.dolarap.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinDatabaseModuleTest {

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `Koin provides DolarapDatabase and all four DAOs as singletons with seeded default currencies`() =
    runBlocking {
      val koin = GlobalContext.get()

      val db1 = koin.get<DolarapDatabase>()
      val db2 = koin.get<DolarapDatabase>()
      assertSame(db1, db2)

      val currencyDao = koin.get<com.aistudio.dolarap.data.local.dao.CurrencyDao>()
      val quoteDao = koin.get<com.aistudio.dolarap.data.local.dao.QuoteDao>()
      val availablePairDao = koin.get<com.aistudio.dolarap.data.local.dao.AvailablePairDao>()
      val syncMetadataDao = koin.get<com.aistudio.dolarap.data.local.dao.SyncMetadataDao>()

      assertSame(currencyDao, koin.get<com.aistudio.dolarap.data.local.dao.CurrencyDao>())
      assertSame(quoteDao, koin.get<com.aistudio.dolarap.data.local.dao.QuoteDao>())
      assertSame(availablePairDao, koin.get<com.aistudio.dolarap.data.local.dao.AvailablePairDao>())
      assertSame(syncMetadataDao, koin.get<com.aistudio.dolarap.data.local.dao.SyncMetadataDao>())

      val seededCurrencies = currencyDao.observeSelectedCurrencies().first()
      assertEquals(10, seededCurrencies.size)
    }
}
