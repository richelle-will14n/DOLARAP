package com.aistudio.dolarap.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aistudio.dolarap.data.local.entity.AvailablePairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailablePairDao {

  @Query(
    "SELECT * FROM available_pairs WHERE quoteCode = 'BRL' AND isAvailable = 1 ORDER BY baseCode ASC"
  )
  fun observeSupportedBrlPairs(): Flow<List<AvailablePairEntity>>

  @Query(
    """
    SELECT * FROM available_pairs
    WHERE quoteCode = 'BRL'
      AND isAvailable = 1
      AND (
        UPPER(baseCode) LIKE '%' || UPPER(:query) || '%'
        OR UPPER(friendlyName) LIKE '%' || UPPER(:query) || '%'
        OR UPPER(apiDescription) LIKE '%' || UPPER(:query) || '%'
      )
    ORDER BY baseCode ASC
    """
  )
  fun searchSupportedBrlPairs(query: String): Flow<List<AvailablePairEntity>>

  @Query("SELECT * FROM available_pairs WHERE pairCode = :pairCode AND isAvailable = 1 LIMIT 1")
  suspend fun getAvailablePair(pairCode: String): AvailablePairEntity?

  @Query(
    "SELECT EXISTS(SELECT 1 FROM available_pairs WHERE pairCode = :pairCode AND isAvailable = 1)"
  )
  suspend fun isPairSupported(pairCode: String): Boolean

  @Query("SELECT COUNT(*) FROM available_pairs WHERE quoteCode = 'BRL' AND isAvailable = 1")
  suspend fun countSupportedBrlPairs(): Int

  @Upsert
  suspend fun upsertAll(pairs: List<AvailablePairEntity>)

  @Query("UPDATE available_pairs SET isAvailable = :isAvailable WHERE pairCode = :pairCode")
  suspend fun setPairAvailability(pairCode: String, isAvailable: Boolean): Int
}
