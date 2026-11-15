package com.aistudio.dolarap.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.dolarap.data.local.catalog.ComplementaryCurrencyCatalog
import com.aistudio.dolarap.data.local.dao.AvailablePairDao
import com.aistudio.dolarap.data.local.dao.CurrencyDao
import com.aistudio.dolarap.data.local.dao.QuoteDao
import com.aistudio.dolarap.data.local.dao.SyncMetadataDao
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyEntity
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity

@Database(
  entities = [
    SelectedCurrencyEntity::class,
    QuoteEntity::class,
    AvailablePairEntity::class,
    SyncMetadataEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class DolarapDatabase : RoomDatabase() {

  abstract fun currencyDao(): CurrencyDao

  abstract fun quoteDao(): QuoteDao

  abstract fun availablePairDao(): AvailablePairDao

  abstract fun syncMetadataDao(): SyncMetadataDao

  companion object {
    const val DATABASE_NAME = "dolarap.db"

    fun build(context: Context): DolarapDatabase =
      Room.databaseBuilder(
          context.applicationContext,
          DolarapDatabase::class.java,
          DATABASE_NAME,
        )
        .addCallback(seedCallback())
        .build()

    fun seedCallback(): Callback =
      object : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
          super.onCreate(db)
          seedDefaultCurrencies(db)
        }
      }

    fun seedDefaultCurrencies(
      db: SupportSQLiteDatabase,
      nowMillis: Long = System.currentTimeMillis(),
    ) {
      db.beginTransaction()
      try {
        ComplementaryCurrencyCatalog.defaultCurrencies.forEach { spec ->
          val values =
            ContentValues().apply {
              put("code", spec.code)
              put("pairCode", spec.pairCode)
              put("displayName", spec.displayName)
              put("isDefault", 1)
              put("isFavorite", 0)
              put("sortOrder", spec.sortOrder)
              put("addedAtMillis", nowMillis)
            }
          db.insert("selected_currencies", SQLiteDatabase.CONFLICT_IGNORE, values)
        }
        db.setTransactionSuccessful()
      } finally {
        db.endTransaction()
      }
    }

    suspend fun ensureDefaultCurrenciesSeeded(
      currencyDao: CurrencyDao,
      nowMillis: Long = System.currentTimeMillis(),
    ) {
      val entities =
        ComplementaryCurrencyCatalog.defaultCurrencies.map { spec ->
          SelectedCurrencyEntity(
            code = spec.code,
            pairCode = spec.pairCode,
            displayName = spec.displayName,
            isDefault = true,
            isFavorite = false,
            sortOrder = spec.sortOrder,
            addedAtMillis = nowMillis,
          )
        }
      currencyDao.insertIgnoreAll(entities)
    }
  }
}
