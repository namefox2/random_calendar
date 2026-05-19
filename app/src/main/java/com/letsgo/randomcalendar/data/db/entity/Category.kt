package com.letsgo.randomcalendar.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // null = 대분류, 대분류id = 중분류, 중분류id = 소분류
    val parentId: Long? = null,
    // 0 = 대분류, 1 = 중분류, 2 = 소분류
    val level: Int = 0
)
