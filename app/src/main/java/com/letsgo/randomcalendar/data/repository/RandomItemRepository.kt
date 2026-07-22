package com.letsgo.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.letsgo.randomcalendar.data.db.dao.RandomItemDao
import com.letsgo.randomcalendar.data.db.entity.RandomItem

class RandomItemRepository(private val dao: RandomItemDao) {

    val all: LiveData<List<RandomItem>> = dao.getAll()

    suspend fun insert(item: RandomItem): Long = dao.insert(item)

    suspend fun update(item: RandomItem) = dao.update(item)

    suspend fun delete(item: RandomItem) = dao.delete(item)

    suspend fun getAllOnce(): List<RandomItem> = dao.getAllOnce()

    suspend fun findByNameAndCategory(name: String, categorySmallId: Long?): RandomItem? =
        dao.findByNameAndCategory(name, categorySmallId)

    suspend fun deleteByCategoryId(categoryId: Long) = dao.deleteByCategoryId(categoryId)

    suspend fun deleteOrphaned() = dao.deleteOrphaned()
}
