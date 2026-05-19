package com.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.randomcalendar.data.db.entity.MonthMemo

@Dao
interface MonthMemoDao {

    @Query("SELECT * FROM month_memos WHERE yearMonth = :yearMonth")
    fun getByYearMonth(yearMonth: String): LiveData<MonthMemo?>

    @Query("SELECT * FROM month_memos WHERE yearMonth = :yearMonth")
    suspend fun getByYearMonthOnce(yearMonth: String): MonthMemo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: MonthMemo)

    @Update
    suspend fun update(memo: MonthMemo)

    @Delete
    suspend fun delete(memo: MonthMemo)

    @Query("DELETE FROM month_memos WHERE yearMonth = :yearMonth")
    suspend fun deleteByYearMonth(yearMonth: String): Int
}
