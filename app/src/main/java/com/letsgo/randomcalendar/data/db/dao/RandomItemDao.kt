package com.letsgo.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.letsgo.randomcalendar.data.db.entity.RandomItem

@Dao
interface RandomItemDao {

    @Query("SELECT * FROM random_items ORDER BY name ASC")
    fun getAll(): LiveData<List<RandomItem>>

    @Query("SELECT * FROM random_items ORDER BY name ASC")
    suspend fun getAllOnce(): List<RandomItem>

    @Query("SELECT * FROM random_items WHERE name = :name AND categorySmallId IS :categorySmallId LIMIT 1")
    suspend fun findByNameAndCategory(name: String, categorySmallId: Long?): RandomItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RandomItem): Long

    @Update
    suspend fun update(item: RandomItem)

    @Delete
    suspend fun delete(item: RandomItem)

    @Query("DELETE FROM random_items WHERE categorySmallId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long): Int

    @Query("DELETE FROM random_items WHERE categorySmallId IS NULL OR categorySmallId NOT IN (SELECT id FROM categories)")
    suspend fun deleteOrphaned(): Int
}
