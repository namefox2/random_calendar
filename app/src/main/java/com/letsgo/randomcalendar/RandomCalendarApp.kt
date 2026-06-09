package com.letsgo.randomcalendar

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.letsgo.randomcalendar.data.db.AppDatabase
import com.letsgo.randomcalendar.data.repository.*

class RandomCalendarApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf("EDB1FC15E44D699A918000A49655C8BE"))
                .build()
        )
        MobileAds.initialize(this)
    }

    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val randomItemRepository by lazy { RandomItemRepository(database.randomItemDao()) }
    val todoItemRepository by lazy { TodoItemRepository(database.todoItemDao()) }
    val monthMemoRepository by lazy { MonthMemoRepository(database.monthMemoDao()) }
    val dayMemoRepository by lazy { DayMemoRepository(database.dayMemoDao()) }
}
