package com.randomcalendar

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.randomcalendar.data.db.AppDatabase
import com.randomcalendar.data.repository.*

class RandomCalendarApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }

    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val randomItemRepository by lazy { RandomItemRepository(database.randomItemDao()) }
    val todoItemRepository by lazy { TodoItemRepository(database.todoItemDao()) }
    val monthMemoRepository by lazy { MonthMemoRepository(database.monthMemoDao()) }
    val dayMemoRepository by lazy { DayMemoRepository(database.dayMemoDao()) }
}
