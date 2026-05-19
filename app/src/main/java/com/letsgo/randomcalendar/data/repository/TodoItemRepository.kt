package com.letsgo.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.letsgo.randomcalendar.data.db.dao.TodoItemDao
import com.letsgo.randomcalendar.data.db.entity.TodoItem

class TodoItemRepository(private val dao: TodoItemDao) {

    fun getByDate(date: String): LiveData<List<TodoItem>> = dao.getByDate(date)

    suspend fun getByDateOnce(date: String): List<TodoItem> = dao.getByDateOnce(date)

    suspend fun getDatesInMonth(yearMonth: String): List<String> = dao.getDatesInMonth(yearMonth)

    suspend fun getAllInMonth(yearMonth: String): List<TodoItem> = dao.getAllInMonth(yearMonth)

    suspend fun getTotalElapsedSeconds(date: String): Int =
        dao.getTotalElapsedSeconds(date) ?: 0

    suspend fun getTotalCount(date: String): Int = dao.getTotalCount(date)

    suspend fun getDoneCount(date: String): Int = dao.getDoneCount(date)

    suspend fun getAchievementRate(date: String): Float {
        val total = getTotalCount(date)
        if (total == 0) return 0f
        return getDoneCount(date).toFloat() / total * 100f
    }

    suspend fun insert(item: TodoItem): Long = dao.insert(item)

    suspend fun insertAll(items: List<TodoItem>) = dao.insertAll(items)

    suspend fun update(item: TodoItem) = dao.update(item)

    suspend fun updateIsDone(id: Long, isDone: Boolean) = dao.updateIsDone(id, isDone)

    suspend fun updateElapsedSeconds(id: Long, seconds: Int) = dao.updateElapsedSeconds(id, seconds)

    suspend fun delete(item: TodoItem) = dao.delete(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun formatElapsedTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> ""
        }
    }
}
