package com.letsgo.randomcalendar.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_memos")
data class DayMemo(
    @PrimaryKey
    val date: String,       // "yyyy-MM-dd"
    val content: String = "",
    val photoPaths: String = ""  // comma-separated file paths
)
