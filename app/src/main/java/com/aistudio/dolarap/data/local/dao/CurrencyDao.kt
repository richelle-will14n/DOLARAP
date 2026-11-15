package com.aistudio.dolarap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyEntity
import com.aistudio.dolarap.data.local.entity.SelectedCurrencyWithQuote
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

  @Query("SELECT * FROM selected_currencies ORDER BY sortOrder ASC, addedAtMillis ASC")
  fun observeSelectedCurrencies(): Flow<List<SelectedCurrencyEntity>>

  @Transaction
  @Query("SELECT * FROM selected_currencies ORDER BY sortOrder ASC, addedAtMillis ASC")
  fun observeSelectedCurrenciesWithQuotes(): Flow<List<SelectedCurrencyWithQuote>>

  @Query(
    "SELECT * FROM selected_currencies WHERE isFavorite = 1 ORDER BY sortOrder ASC, addedAtMillis ASC"
  )
  fun observeFavoriteCurrencies(): Flow<List<SelectedCurrencyEntity>>

  @Transaction
  @Query(
    "SELECT * FROM selected_currencies WHERE isFavorite = 1 ORDER BY sortOrder ASC, addedAtMillis ASC"
  )
  fun observeFavoriteCurrenciesWithQuotes(): Flow<List<SelectedCurrencyWithQuote>>

  @Query("SELECT * FROM selected_currencies WHERE code = :code LIMIT 1")
  suspend fun getCurrencyByCode(code: String): SelectedCurrencyEntity?

  @Query("SELECT * FROM selected_currencies ORDER BY sortOrder ASC, addedAtMillis ASC")
  suspend fun getAllCurrenciesSnapshot(): List<SelectedCurrencyEntity>

  @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM selected_currencies")
  suspend fun getMaxSortOrder(): Int

  @Query("SELECT COUNT(*) FROM selected_currencies")
  suspend fun countCurrencies(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIgnoreAll(currencies: List<SelectedCurrencyEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIgnore(currency: SelectedCurrencyEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCurrency(currency: SelectedCurrencyEntity)

  @Query("UPDATE selected_currencies SET isFavorite = :isFavorite WHERE code = :code")
  suspend fun updateFavorite(code: String, isFavorite: Boolean): Int

  @Query("UPDATE selected_currencies SET isFavorite = NOT isFavorite WHERE code = :code")
  suspend fun toggleFavorite(code: String): Int

  @Query("DELETE FROM selected_currencies WHERE code = :code AND isDefault = 0")
  suspend fun deleteNonDefaultByCode(code: String): Int
}
