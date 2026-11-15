package com.aistudio.dolarap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.dolarap.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

  @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
  fun observeSyncMetadata(): Flow<SyncMetadataEntity?>

  @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
  suspend fun getSyncMetadata(): SyncMetadataEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(metadata: SyncMetadataEntity)

  @Query("DELETE FROM sync_metadata")
  suspend fun clear()
}
