package com.aistudio.dolarap.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "available_pairs",
  indices = [Index(value = ["baseCode", "quoteCode"])],
)
data class AvailablePairEntity(
  @PrimaryKey val pairCode: String,
  @ColumnInfo(name = "baseCode") val baseCode: String,
  @ColumnInfo(name = "quoteCode") val quoteCode: String,
  @ColumnInfo(name = "apiDescription") val apiDescription: String,
  @ColumnInfo(name = "friendlyName") val friendlyName: String,
  @ColumnInfo(name = "isAvailable") val isAvailable: Boolean = true,
  @ColumnInfo(name = "updatedAtMillis") val updatedAtMillis: Long,
)
