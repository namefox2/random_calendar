package com.letsgo.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.letsgo.randomcalendar.data.db.entity.Category

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY name ASC")
    suspend fun getChildrenOnce(parentId: Long): List<Category>

    @Query("SELECT * FROM categories ORDER BY level ASC, name ASC")
    fun getAll(): LiveData<List<Category>>

    @Query("SELECT * FROM categories ORDER BY level ASC, name ASC")
    suspend fun getAllOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name AND parentId IS :parentId AND level = :level LIMIT 1")
    suspend fun findByNameAndParent(name: String, parentId: Long?, level: Int): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

}
