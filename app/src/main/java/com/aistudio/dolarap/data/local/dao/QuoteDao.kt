package com.aistudio.dolarap.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aistudio.dolarap.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

  @Query("SELECT * FROM quotes ORDER BY code ASC")
  fun observeAllQuotes(): Flow<List<QuoteEntity>>

  @Query("SELECT * FROM quotes WHERE pairCode = :pairCode LIMIT 1")
  fun observeQuoteByPair(pairCode: String): Flow<QuoteEntity?>

  @Query("SELECT * FROM quotes WHERE pairCode = :pairCode LIMIT 1")
  suspend fun getQuoteByPair(pairCode: String): QuoteEntity?

  @Query("SELECT * FROM quotes ORDER BY code ASC")
  suspend fun getAllQuotesSnapshot(): List<QuoteEntity>

  @Upsert
  suspend fun upsert(quote: QuoteEntity)

  @Upsert
  suspend fun upsertAll(quotes: List<QuoteEntity>)

  @Query("DELETE FROM quotes WHERE pairCode = :pairCode")
  suspend fun deleteByPair(pairCode: String): Int
}
