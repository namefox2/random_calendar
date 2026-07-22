package com.letsgo.randomcalendar.ui.randomlist

import androidx.lifecycle.*
import com.letsgo.randomcalendar.data.db.entity.Category
import com.letsgo.randomcalendar.data.db.entity.RandomItem
import com.letsgo.randomcalendar.data.repository.CategoryRepository
import com.letsgo.randomcalendar.data.repository.RandomItemRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class RandomListViewModel(
    private val categoryRepo: CategoryRepository,
    private val itemRepo: RandomItemRepository
) : ViewModel() {

    val allCategories: LiveData<List<Category>> = categoryRepo.all
    val allItems: LiveData<List<RandomItem>> = itemRepo.all

    init {
        viewModelScope.launch { itemRepo.deleteOrphaned() }
    }

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
            deleteCategoryRecursive(category)
        }
    }

    private suspend fun deleteCategoryRecursive(category: Category) {
        if (category.level == 2) {
            itemRepo.deleteByCategoryId(category.id)
        } else {
            categoryRepo.getChildrenOnce(category.id).forEach { deleteCategoryRecursive(it) }
        }
        categoryRepo.delete(category)
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

    fun deleteItem(item: RandomItem) {
        viewModelScope.launch {
            itemRepo.delete(item)
        }
    }

    fun moveToCategory(category: Category, newParentId: Long) {
        viewModelScope.launch {
            categoryRepo.update(category.copy(parentId = newParentId))
        }
    }

    fun updateItem(item: RandomItem) {
        viewModelScope.launch {
            itemRepo.update(item)
        }
    }

    suspend fun exportToJson(): String {
        val categories = categoryRepo.getAllOnce()
        val items = itemRepo.getAllOnce()
        val root = JSONObject()
        root.put("version", 1)
        val catArray = JSONArray()
        categories.forEach { cat ->
            catArray.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("parentId", if (cat.parentId != null) cat.parentId else JSONObject.NULL)
                put("level", cat.level)
            })
        }
        root.put("categories", catArray)
        val itemArray = JSONArray()
        items.forEach { item ->
            itemArray.put(JSONObject().apply {
                put("name", item.name)
                put("categorySmallId", if (item.categorySmallId != null) item.categorySmallId else JSONObject.NULL)
                put("url", item.url)
                put("memo", item.memo)
                put("timerType", item.timerType)
                put("timerGoalSeconds", if (item.timerGoalSeconds != null) item.timerGoalSeconds else JSONObject.NULL)
                put("setWorkSeconds", item.setWorkSeconds)
                put("setRestSeconds", item.setRestSeconds)
                put("setCount", item.setCount)
            })
        }
        root.put("items", itemArray)
        return root.toString(2)
    }

    suspend fun importFromJson(json: String): Result<Int> {
        return try {
            val root = JSONObject(json)
            val catArray = root.getJSONArray("categories")
            val itemArray = root.getJSONArray("items")
            val idMap = mutableMapOf<Long, Long>()
            val catList = (0 until catArray.length()).map { catArray.getJSONObject(it) }
            for (level in 0..2) {
                catList.filter { it.getInt("level") == level }.forEach { catJson ->
                    val oldId = catJson.getLong("id")
                    val oldParentId = if (catJson.isNull("parentId")) null else catJson.getLong("parentId")
                    val newParentId = oldParentId?.let { idMap[it] }
                    val name = catJson.getString("name")
                    val existing = categoryRepo.findByNameAndParent(name, newParentId, level)
                    val newId = existing?.id ?: categoryRepo.insert(
                        Category(name = name, parentId = newParentId, level = level)
                    )
                    idMap[oldId] = newId
                }
            }
            var count = 0
            for (i in 0 until itemArray.length()) {
                val itemJson = itemArray.getJSONObject(i)
                val oldCatId = if (itemJson.isNull("categorySmallId")) null else itemJson.getLong("categorySmallId")
                val newCatId = oldCatId?.let { idMap[it] }
                val itemName = itemJson.getString("name")
                val alreadyExists = itemRepo.findByNameAndCategory(itemName, newCatId) != null
                if (!alreadyExists) {
                    itemRepo.insert(RandomItem(
                        name = itemName,
                        categorySmallId = newCatId,
                        url = itemJson.optString("url", ""),
                        memo = itemJson.optString("memo", ""),
                        timerType = itemJson.optString("timerType", "NONE"),
                        timerGoalSeconds = if (itemJson.isNull("timerGoalSeconds")) null else itemJson.getInt("timerGoalSeconds"),
                        setWorkSeconds = itemJson.optInt("setWorkSeconds", 0),
                        setRestSeconds = itemJson.optInt("setRestSeconds", 0),
                        setCount = itemJson.optInt("setCount", 0)
                    ))
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
