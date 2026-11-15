package com.aistudio.dolarap.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
  @PrimaryKey val pairCode: String,
  @ColumnInfo(name = "code") val code: String,
  @ColumnInfo(name = "codeIn") val codeIn: String,
  @ColumnInfo(name = "apiName") val apiName: String,
  @ColumnInfo(name = "bid") val bid: String,
  @ColumnInfo(name = "ask") val ask: String,
  @ColumnInfo(name = "varBid") val varBid: String,
  @ColumnInfo(name = "pctChange") val pctChange: String,
  @ColumnInfo(name = "high") val high: String,
  @ColumnInfo(name = "low") val low: String,
  @ColumnInfo(name = "apiTimestampSeconds") val apiTimestampSeconds: Long,
  @ColumnInfo(name = "createDate") val createDate: String,
  @ColumnInfo(name = "fetchedAtMillis") val fetchedAtMillis: Long,
)
