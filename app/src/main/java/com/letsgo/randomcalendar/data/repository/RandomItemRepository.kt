package com.letsgo.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.letsgo.randomcalendar.data.db.dao.RandomItemDao
import com.letsgo.randomcalendar.data.db.entity.RandomItem

class RandomItemRepository(private val dao: RandomItemDao) {

    val all: LiveData<List<RandomItem>> = dao.getAll()

    fun getByCategory(categoryId: Long): LiveData<List<RandomItem>> = dao.getByCategory(categoryId)

    suspend fun getByCategoryIds(categoryIds: List<Long>): List<RandomItem> =
        dao.getByCategoryIds(categoryIds)

    suspend fun getById(id: Long): RandomItem? = dao.getById(id)

    suspend fun insert(item: RandomItem): Long = dao.insert(item)

    suspend fun update(item: RandomItem) = dao.update(item)

    suspend fun delete(item: RandomItem) = dao.delete(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun pickRandom(items: List<RandomItem>, count: Int): List<RandomItem> =
        items.shuffled().take(count)

    suspend fun getAllOnce(): List<RandomItem> = dao.getAllOnce()

    suspend fun findByNameAndCategory(name: String, categorySmallId: Long?): RandomItem? =
        dao.findByNameAndCategory(name, categorySmallId)

    suspend fun getByCategoryIdOnce(categoryId: Long): RandomItem? =
        dao.getByCategoryIdOnce(categoryId)

    suspend fun deleteByCategoryId(categoryId: Long) = dao.deleteByCategoryId(categoryId)

    suspend fun deleteOrphaned() = dao.deleteOrphaned()
}
