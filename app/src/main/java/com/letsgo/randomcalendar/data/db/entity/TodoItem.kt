package com.letsgo.randomcalendar.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // "yyyy-MM-dd"
    val date: String,
    val name: String,
    val url: String = "",
    // "NONE", "NORMAL", "SET"
    val timerType: String = "NONE",
    val timerGoalSeconds: Int? = null,
    val setWorkSeconds: Int = 0,
    val setRestSeconds: Int = 0,
    val setCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val isDone: Boolean = false,
    val order: Int = 0
)
