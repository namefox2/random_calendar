package com.randomcalendar.ui.daydetail

import androidx.lifecycle.*
import com.randomcalendar.data.db.entity.TodoItem
import com.randomcalendar.data.repository.TodoItemRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayDetailViewModel(
    private val todoRepo: TodoItemRepository,
    date: LocalDate
) : ViewModel() {

    val dateStr: String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todos: LiveData<List<TodoItem>> = todoRepo.getByDate(dateStr)

    val summary: LiveData<DaySummary> = todos.map { list ->
        val total = list.size
        val done = list.count { it.isDone }
        val totalSec = list.sumOf { it.elapsedSeconds }
        val rate = if (total > 0) done * 100 / total else 0
        DaySummary(total, done, rate, totalSec)
    }

    fun addTodo(
        name: String,
        url: String = "",
        timerType: String = "NONE",
        timerGoalSeconds: Int? = null,
        setWorkSeconds: Int = 0,
        setRestSeconds: Int = 0,
        setCount: Int = 0
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = todoRepo.getTotalCount(dateStr)
            todoRepo.insert(
                TodoItem(
                    date = dateStr,
                    name = name.trim(),
                    url = url.trim(),
                    timerType = timerType,
                    timerGoalSeconds = timerGoalSeconds,
                    setWorkSeconds = setWorkSeconds,
                    setRestSeconds = setRestSeconds,
                    setCount = setCount,
                    order = order
                )
            )
        }
    }

    fun toggleDone(item: TodoItem) {
        viewModelScope.launch {
            todoRepo.updateIsDone(item.id, !item.isDone)
        }
    }

    fun delete(item: TodoItem) {
        viewModelScope.launch {
            todoRepo.delete(item)
        }
    }

    fun updateTimerType(item: TodoItem, timerType: String) {
        viewModelScope.launch {
            todoRepo.update(item.copy(timerType = timerType))
        }
    }

    fun updateElapsedSeconds(id: Long, seconds: Int) {
        viewModelScope.launch {
            todoRepo.updateElapsedSeconds(id, seconds)
        }
    }

    data class DaySummary(
        val total: Int,
        val done: Int,
        val achievementRate: Int,
        val totalElapsedSeconds: Int
    )
}
