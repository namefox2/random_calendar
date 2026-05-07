package com.randomcalendar

import android.app.Application
import com.randomcalendar.data.db.AppDatabase
import com.randomcalendar.data.repository.*

class RandomCalendarApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val randomItemRepository by lazy { RandomItemRepository(database.randomItemDao()) }
    val todoItemRepository by lazy { TodoItemRepository(database.todoItemDao()) }
    val monthMemoRepository by lazy { MonthMemoRepository(database.monthMemoDao()) }
}
