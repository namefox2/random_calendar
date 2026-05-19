package com.letsgo.randomcalendar.data.repository

import com.letsgo.randomcalendar.data.db.dao.DayMemoDao
import com.letsgo.randomcalendar.data.db.entity.DayMemo

class DayMemoRepository(private val dao: DayMemoDao) {

    fun getByDate(date: String) = dao.getByDate(date)

    suspend fun save(date: String, content: String, photoPaths: String) {
        dao.insertOrReplace(DayMemo(date = date, content = content, photoPaths = photoPaths))
    }

    suspend fun getOnce(date: String): DayMemo? = dao.getByDateOnce(date)
}
