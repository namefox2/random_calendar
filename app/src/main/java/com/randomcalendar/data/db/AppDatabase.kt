package com.randomcalendar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.randomcalendar.data.db.dao.*
import com.randomcalendar.data.db.entity.*

@Database(
    entities = [
        Category::class,
        RandomItem::class,
        TodoItem::class,
        MonthMemo::class,
        DayMemo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun randomItemDao(): RandomItemDao
    abstract fun todoItemDao(): TodoItemDao
    abstract fun monthMemoDao(): MonthMemoDao
    abstract fun dayMemoDao(): DayMemoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "random_calendar.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
