package com.aistudio.dolarap.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "selected_currencies",
  indices = [Index(value = ["pairCode"], unique = true)],
)
data class SelectedCurrencyEntity(
  @PrimaryKey val code: String,
  @ColumnInfo(name = "pairCode") val pairCode: String,
  @ColumnInfo(name = "displayName") val displayName: String,
  @ColumnInfo(name = "isDefault") val isDefault: Boolean,
  @ColumnInfo(name = "isFavorite") val isFavorite: Boolean = false,
  @ColumnInfo(name = "sortOrder") val sortOrder: Int,
  @ColumnInfo(name = "addedAtMillis") val addedAtMillis: Long,
)
