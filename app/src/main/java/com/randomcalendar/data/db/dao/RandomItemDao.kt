package com.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.randomcalendar.data.db.entity.RandomItem

@Dao
interface RandomItemDao {

    @Query("SELECT * FROM random_items ORDER BY name ASC")
    fun getAll(): LiveData<List<RandomItem>>

    @Query("SELECT * FROM random_items ORDER BY name ASC")
    suspend fun getAllOnce(): List<RandomItem>

    @Query("SELECT * FROM random_items WHERE categorySmallId = :categoryId ORDER BY name ASC")
    fun getByCategory(categoryId: Long): LiveData<List<RandomItem>>

    @Query("SELECT * FROM random_items WHERE categorySmallId IN (:categoryIds) ORDER BY name ASC")
    suspend fun getByCategoryIds(categoryIds: List<Long>): List<RandomItem>

    @Query("SELECT * FROM random_items WHERE id = :id")
    suspend fun getById(id: Long): RandomItem?

    @Query("SELECT * FROM random_items WHERE name = :name AND categorySmallId IS :categorySmallId LIMIT 1")
    suspend fun findByNameAndCategory(name: String, categorySmallId: Long?): RandomItem?

    @Query("SELECT * FROM random_items WHERE categorySmallId = :categoryId LIMIT 1")
    suspend fun getByCategoryIdOnce(categoryId: Long): RandomItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RandomItem): Long

    @Update
    suspend fun update(item: RandomItem)

    @Delete
    suspend fun delete(item: RandomItem)

    @Query("DELETE FROM random_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
