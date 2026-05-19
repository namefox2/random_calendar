package com.letsgo.randomcalendar.ui.daydetail

import androidx.lifecycle.*
import com.letsgo.randomcalendar.data.db.entity.DayMemo
import com.letsgo.randomcalendar.data.db.entity.TodoItem
import com.letsgo.randomcalendar.data.repository.DayMemoRepository
import com.letsgo.randomcalendar.data.repository.TodoItemRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayDetailViewModel(
    private val todoRepo: TodoItemRepository,
    private val dayMemoRepo: DayMemoRepository,
    initialDate: LocalDate
) : ViewModel() {

    private val _currentDate = MutableLiveData(initialDate)
    val currentDate: LiveData<LocalDate> = _currentDate

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val dateStr: String get() = _currentDate.value!!.format(fmt)

    fun setDate(date: LocalDate) {
        _currentDate.value = date
    }

    val todos: LiveData<List<TodoItem>> = _currentDate.switchMap { date ->
        todoRepo.getByDate(date.format(fmt))
    }

    val summary: LiveData<DaySummary> = todos.map { list ->
        val total = list.size
        val done = list.count { it.isDone }
        val totalSec = list.filter { it.isDone }.sumOf { it.elapsedSeconds }
        val rate = if (total > 0) done * 100 / total else 0
        DaySummary(total, done, rate, totalSec)
    }

    val dayMemo: LiveData<DayMemo?> = _currentDate.switchMap { date ->
        dayMemoRepo.getByDate(date.format(fmt))
    }

    fun saveMemoContent(content: String, photoPaths: String) {
        viewModelScope.launch {
            dayMemoRepo.save(dateStr, content, photoPaths)
        }
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
    ) {
        val elapsedText: String get() {
            val h = totalElapsedSeconds / 3600
            val m = (totalElapsedSeconds % 3600) / 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                m > 0 -> "${m}m"
                else -> ""
            }
        }
    }
}
