package com.randomcalendar.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "random_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categorySmallId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categorySmallId")]
)
data class RandomItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categorySmallId: Long? = null,
    val url: String = "",
    val memo: String = "",
    // "NONE", "NORMAL", "SET"
    val timerType: String = "NONE",
    // 일반 타이머 목표 시간 (초), null = 무제한
    val timerGoalSeconds: Int? = null,
    // 세트 타이머
    val setWorkSeconds: Int = 0,
    val setRestSeconds: Int = 0,
    val setCount: Int = 0
)
