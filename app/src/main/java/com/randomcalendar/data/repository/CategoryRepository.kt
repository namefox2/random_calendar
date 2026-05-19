package com.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.randomcalendar.data.db.dao.CategoryDao
import com.randomcalendar.data.db.entity.Category

class CategoryRepository(private val dao: CategoryDao) {

    val allTopLevel: LiveData<List<Category>> = dao.getAllTopLevel()
    val allSmallCategories: LiveData<List<Category>> = dao.getAllSmallCategories()
    val all: LiveData<List<Category>> = dao.getAll()

    fun getChildren(parentId: Long): LiveData<List<Category>> = dao.getChildren(parentId)

    suspend fun getChildrenOnce(parentId: Long): List<Category> = dao.getChildrenOnce(parentId)

    suspend fun getById(id: Long): Category? = dao.getById(id)

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getAllOnce(): List<Category> = dao.getAllOnce()
}
