package com.randomcalendar.ui.calendar

import com.randomcalendar.ui.main.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object CalendarBuilder {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun build(
        yearMonth: YearMonth,
        today: LocalDate = LocalDate.now(),
        dayDataMap: Map<String, MainViewModel.DayData> = emptyMap(),
        elapsedFormatter: (Int) -> String = { "" }
    ): List<DayCell> {
        val cells = mutableListOf<DayCell>()
        val firstDay = yearMonth.atDay(1)
        val paddingCount = firstDay.dayOfWeek.value % 7

        // 이전 달 날짜로 앞 빈 칸 채우기
        val prevMonth = yearMonth.minusMonths(1)
        val prevMonthLastDay = prevMonth.lengthOfMonth()
        for (i in paddingCount - 1 downTo 0) {
            val date = prevMonth.atDay(prevMonthLastDay - i)
            cells.add(DayCell(date = date, isCurrentMonth = false, isToday = date == today))
        }

        // 이번 달 날짜
        val daysInMonth = yearMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateKey = date.format(dateFormatter)
            val data = dayDataMap[dateKey]
            cells.add(
                DayCell(
                    date = date,
                    isCurrentMonth = true,
                    isToday = date == today,
                    isGreen = data?.isGreen == true,
                    isRed = data?.isRed == true,
                    elapsedText = if (data != null && data.totalElapsedSeconds > 0)
                        elapsedFormatter(data.totalElapsedSeconds) else ""
                )
            )
        }

        // 다음 달 날짜로 뒤 빈 칸 채우기
        val nextMonth = yearMonth.plusMonths(1)
        val remainder = cells.size % 7
        if (remainder != 0) {
            var nextDay = 1
            repeat(7 - remainder) {
                val date = nextMonth.atDay(nextDay++)
                cells.add(DayCell(date = date, isCurrentMonth = false, isToday = date == today))
            }
        }

        return cells
    }
}
