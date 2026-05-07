package com.randomcalendar.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "month_memos")
data class MonthMemo(
    @PrimaryKey
    // "yyyy-MM"
    val yearMonth: String,
    val content: String = ""
)
