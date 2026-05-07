package com.randomcalendar.data.repository

import androidx.lifecycle.LiveData
import com.randomcalendar.data.db.dao.MonthMemoDao
import com.randomcalendar.data.db.entity.MonthMemo

class MonthMemoRepository(private val dao: MonthMemoDao) {

    fun getByYearMonth(yearMonth: String): LiveData<MonthMemo?> = dao.getByYearMonth(yearMonth)

    suspend fun getByYearMonthOnce(yearMonth: String): MonthMemo? = dao.getByYearMonthOnce(yearMonth)

    suspend fun save(yearMonth: String, content: String) {
        val existing = getByYearMonthOnce(yearMonth)
        if (existing == null) {
            dao.insert(MonthMemo(yearMonth = yearMonth, content = content))
        } else {
            dao.update(existing.copy(content = content))
        }
    }

    suspend fun delete(yearMonth: String) = dao.deleteByYearMonth(yearMonth)
}
