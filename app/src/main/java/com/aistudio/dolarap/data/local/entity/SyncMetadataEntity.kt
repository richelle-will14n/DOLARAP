package com.aistudio.dolarap.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
  @PrimaryKey val id: Int = SINGLETON_ID,
  @ColumnInfo(name = "lastSuccessfulSyncAtMillis") val lastSuccessfulSyncAtMillis: Long? = null,
  @ColumnInfo(name = "lastAttemptAtMillis") val lastAttemptAtMillis: Long? = null,
  @ColumnInfo(name = "lastErrorType") val lastErrorType: String? = null,
  @ColumnInfo(name = "lastCatalogSyncAtMillis") val lastCatalogSyncAtMillis: Long? = null,
) {
  companion object {
    const val SINGLETON_ID = 1
  }
}
