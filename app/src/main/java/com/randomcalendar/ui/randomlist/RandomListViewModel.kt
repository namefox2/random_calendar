package com.randomcalendar.ui.randomlist

import androidx.lifecycle.*
import com.randomcalendar.data.db.entity.Category
import com.randomcalendar.data.db.entity.RandomItem
import com.randomcalendar.data.repository.CategoryRepository
import com.randomcalendar.data.repository.RandomItemRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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
                    val newId = categoryRepo.insert(Category(name = catJson.getString("name"), parentId = newParentId, level = level))
                    idMap[oldId] = newId
                }
            }
            var count = 0
            for (i in 0 until itemArray.length()) {
                val itemJson = itemArray.getJSONObject(i)
                val oldCatId = if (itemJson.isNull("categorySmallId")) null else itemJson.getLong("categorySmallId")
                itemRepo.insert(RandomItem(
                    name = itemJson.getString("name"),
                    categorySmallId = oldCatId?.let { idMap[it] },
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
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
