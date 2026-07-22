package com.letsgo.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.letsgo.randomcalendar.data.db.dao.CategoryDao
import com.letsgo.randomcalendar.data.db.entity.Category

class CategoryRepository(private val dao: CategoryDao) {

    val all: LiveData<List<Category>> = dao.getAll()

    suspend fun getChildrenOnce(parentId: Long): List<Category> = dao.getChildrenOnce(parentId)

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    suspend fun getAllOnce(): List<Category> = dao.getAllOnce()

    suspend fun findByNameAndParent(name: String, parentId: Long?, level: Int): Category? =
        dao.findByNameAndParent(name, parentId, level)
}
