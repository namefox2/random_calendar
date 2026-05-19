package com.randomcalendar.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.randomcalendar.data.db.entity.DayMemo

@Dao
interface DayMemoDao {

    @Query("SELECT * FROM day_memos WHERE date = :date LIMIT 1")
    fun getByDate(date: String): LiveData<DayMemo?>

    @Query("SELECT * FROM day_memos WHERE date = :date LIMIT 1")
    suspend fun getByDateOnce(date: String): DayMemo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(memo: DayMemo): Long
}
