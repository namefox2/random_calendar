package com.letsgo.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.letsgo.randomcalendar.data.db.entity.MonthMemo

@Dao
interface MonthMemoDao {

    @Query("SELECT * FROM month_memos WHERE yearMonth = :yearMonth")
    fun getByYearMonth(yearMonth: String): LiveData<MonthMemo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: MonthMemo): Long
}
