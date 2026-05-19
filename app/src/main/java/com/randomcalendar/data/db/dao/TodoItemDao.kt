package com.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.randomcalendar.data.db.entity.TodoItem

@Dao
interface TodoItemDao {

    @Query("SELECT * FROM todo_items WHERE date = :date ORDER BY `order` ASC, id ASC")
    fun getByDate(date: String): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE date = :date ORDER BY `order` ASC, id ASC")
    suspend fun getByDateOnce(date: String): List<TodoItem>

    @Query("SELECT DISTINCT date FROM todo_items WHERE date LIKE :yearMonth || '%'")
    suspend fun getDatesInMonth(yearMonth: String): List<String>

    @Query("SELECT * FROM todo_items WHERE date LIKE :yearMonth || '%'")
    suspend fun getAllInMonth(yearMonth: String): List<TodoItem>

    @Query("SELECT SUM(elapsedSeconds) FROM todo_items WHERE date = :date")
    suspend fun getTotalElapsedSeconds(date: String): Int?

    @Query("SELECT COUNT(*) FROM todo_items WHERE date = :date")
    suspend fun getTotalCount(date: String): Int

    @Query("SELECT COUNT(*) FROM todo_items WHERE date = :date AND isDone = 1")
    suspend fun getDoneCount(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TodoItem>)

    @Update
    suspend fun update(item: TodoItem)

    @Query("UPDATE todo_items SET isDone = :isDone WHERE id = :id")
    suspend fun updateIsDone(id: Long, isDone: Boolean): Int

    @Query("UPDATE todo_items SET elapsedSeconds = :seconds WHERE id = :id")
    suspend fun updateElapsedSeconds(id: Long, seconds: Int): Int

    @Delete
    suspend fun delete(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
