package com.letsgo.randomcalendar.ui.main

import androidx.lifecycle.*
import com.letsgo.randomcalendar.data.db.entity.MonthMemo
import com.letsgo.randomcalendar.data.db.entity.TodoItem
import com.letsgo.randomcalendar.data.repository.MonthMemoRepository
import com.letsgo.randomcalendar.data.repository.TodoItemRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainViewModel(
    private val todoRepo: TodoItemRepository,
    private val memoRepo: MonthMemoRepository
) : ViewModel() {

    private val _currentYearMonth = MutableLiveData(YearMonth.now())
    val currentYearMonth: LiveData<YearMonth> = _currentYearMonth

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    val todosForSelectedDate: LiveData<List<TodoItem>> = _selectedDate.switchMap { date ->
        todoRepo.getByDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    val currentMonthMemo: LiveData<MonthMemo?> = _currentYearMonth.switchMap { ym ->
        memoRepo.getByYearMonth(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }

    private val _monthDayData = MutableLiveData<Map<String, DayData>>()
    val monthDayData: LiveData<Map<String, DayData>> = _monthDayData

    private var achievementThreshold: Int = 80

    fun setAchievementThreshold(threshold: Int) {
        achievementThreshold = threshold
        refreshMonthData()
    }

    fun goToPreviousMonth() {
        _currentYearMonth.value = _currentYearMonth.value?.minusMonths(1)
        refreshMonthData()
    }

    fun goToNextMonth() {
        _currentYearMonth.value = _currentYearMonth.value?.plusMonths(1)
        refreshMonthData()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun refreshMonthData() {
        val ym = _currentYearMonth.value ?: return
        val yearMonth = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            val items = todoRepo.getAllInMonth(yearMonth)
            val grouped = items.groupBy { it.date }
            val dayDataMap = grouped.mapValues { (_, dayItems) ->
                val total = dayItems.size
                val done = dayItems.count { it.isDone }
                val elapsedSeconds = dayItems.filter { it.isDone }.sumOf { it.elapsedSeconds }
                val achievementRate = if (total > 0) done.toFloat() / total * 100f else 0f
                DayData(
                    totalCount = total,
                    doneCount = done,
                    achievementRate = achievementRate,
                    totalElapsedSeconds = elapsedSeconds,
                    isGreen = total > 0 && achievementRate >= achievementThreshold,
                    isRed = total > 0 && achievementRate < achievementThreshold
                )
            }
            _monthDayData.postValue(dayDataMap)
        }
    }

    fun addTodo(date: LocalDate, name: String, url: String = "") {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingCount = todoRepo.getTotalCount(dateStr)
            val item = TodoItem(
                date = dateStr,
                name = name,
                url = url,
                order = existingCount
            )
            todoRepo.insert(item)
            refreshMonthData()
        }
    }

    fun toggleTodoDone(item: TodoItem) {
        viewModelScope.launch {
            todoRepo.updateIsDone(item.id, !item.isDone)
            refreshMonthData()
        }
    }

    fun deleteTodo(item: TodoItem) {
        viewModelScope.launch {
            todoRepo.delete(item)
            refreshMonthData()
        }
    }

    fun updateTodo(item: TodoItem) {
        viewModelScope.launch {
            todoRepo.update(item)
            refreshMonthData()
        }
    }

    fun saveMemo(content: String) {
        val ym = _currentYearMonth.value ?: return
        val yearMonth = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            memoRepo.save(yearMonth, content)
        }
    }

    data class DayData(
        val totalCount: Int,
        val doneCount: Int,
        val achievementRate: Float,
        val totalElapsedSeconds: Int,
        val isGreen: Boolean,
        val isRed: Boolean
    )
}
