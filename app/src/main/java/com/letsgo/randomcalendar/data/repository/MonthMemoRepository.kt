package com.letsgo.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.letsgo.randomcalendar.data.db.dao.MonthMemoDao
import com.letsgo.randomcalendar.data.db.entity.MonthMemo

class MonthMemoRepository(private val dao: MonthMemoDao) {

    fun getByYearMonth(yearMonth: String): LiveData<MonthMemo?> = dao.getByYearMonth(yearMonth)

    suspend fun save(yearMonth: String, content: String) {
        dao.insert(MonthMemo(yearMonth = yearMonth, content = content))
    }
}
