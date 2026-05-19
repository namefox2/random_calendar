package com.randomcalendar.ui.randomlist

import androidx.lifecycle.*
import com.randomcalendar.data.db.entity.Category
import com.randomcalendar.data.db.entity.RandomItem
import com.randomcalendar.data.repository.CategoryRepository
import com.randomcalendar.data.repository.RandomItemRepository
import kotlinx.coroutines.launch

class RandomListViewModel(
    private val categoryRepo: CategoryRepository,
    private val itemRepo: RandomItemRepository
) : ViewModel() {

    val allCategories: LiveData<List<Category>> = categoryRepo.all
    val allItems: LiveData<List<RandomItem>> = itemRepo.all

    // 분류 트리용 - 대/중/소 구분
    val topCategories: LiveData<List<Category>> = categoryRepo.allTopLevel

    fun getChildren(parentId: Long): LiveData<List<Category>> = categoryRepo.getChildren(parentId)

    fun addCategory(name: String, parentId: Long?, level: Int) {
        viewModelScope.launch {
            categoryRepo.insert(Category(name = name, parentId = parentId, level = level))
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            categoryRepo.update(category.copy(name = newName))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepo.delete(category)
        }
    }

    fun addItem(
        name: String,
        categorySmallId: Long?,
        url: String,
        memo: String,
        timerType: String,
        timerGoalSeconds: Int?,
        setWorkSeconds: Int,
        setRestSeconds: Int,
        setCount: Int
    ) {
        viewModelScope.launch {
            itemRepo.insert(
                RandomItem(
                    name = name,
                    categorySmallId = categorySmallId,
                    url = url,
                    memo = memo,
                    timerType = timerType,
                    timerGoalSeconds = timerGoalSeconds,
                    setWorkSeconds = setWorkSeconds,
                    setRestSeconds = setRestSeconds,
                    setCount = setCount
                )
            )
        }
    }

    // 소분류 추가 시 Category와 RandomItem을 동시에 생성
    fun addCategoryWithItem(
        name: String,
        parentMidId: Long,
        url: String,
        timerType: String,
        timerGoalSeconds: Int?,
        setWorkSeconds: Int,
        setRestSeconds: Int,
        setCount: Int
    ) {
        viewModelScope.launch {
            val categoryId = categoryRepo.insert(
                Category(name = name, parentId = parentMidId, level = 2)
            )
            itemRepo.insert(
                RandomItem(
                    name = name,
                    categorySmallId = categoryId,
                    url = url,
                    timerType = timerType,
                    timerGoalSeconds = timerGoalSeconds,
                    setWorkSeconds = setWorkSeconds,
                    setRestSeconds = setRestSeconds,
                    setCount = setCount
                )
            )
        }
    }

    fun deleteItem(item: RandomItem) {
        viewModelScope.launch {
            itemRepo.delete(item)
        }
    }

    fun updateItem(item: RandomItem) {
        viewModelScope.launch {
            itemRepo.update(item)
        }
    }
}
