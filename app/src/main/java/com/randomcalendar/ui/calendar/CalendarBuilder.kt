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
        // 일요일=0 기준으로 앞 빈 칸 계산 (dayOfWeek: MON=1..SUN=7 → 일=0,월=1,..)
        val paddingCount = firstDay.dayOfWeek.value % 7

        // 앞 빈 칸
        repeat(paddingCount) { cells.add(DayCell(null)) }

        // 날짜 칸
        val daysInMonth = yearMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateKey = date.format(dateFormatter)
            val data = dayDataMap[dateKey]

            cells.add(
                DayCell(
                    date = date,
                    isToday = date == today,
                    isGreen = data?.isGreen == true,
                    isRed = data?.isRed == true,
                    elapsedText = if (data != null && data.totalElapsedSeconds > 0)
                        elapsedFormatter(data.totalElapsedSeconds) else ""
                )
            )
        }

        // 뒤 빈 칸 (7의 배수 맞춤)
        val remainder = cells.size % 7
        if (remainder != 0) {
            repeat(7 - remainder) { cells.add(DayCell(null)) }
        }

        return cells
    }
}
