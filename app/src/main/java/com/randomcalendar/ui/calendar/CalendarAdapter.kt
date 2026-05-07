package com.randomcalendar.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.randomcalendar.R
import com.randomcalendar.databinding.ItemDayCellBinding
import com.randomcalendar.ui.main.MainViewModel
import java.time.LocalDate

class CalendarAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : ListAdapter<DayCell, CalendarAdapter.DayCellViewHolder>(DayCellDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayCellViewHolder {
        val binding = ItemDayCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayCellViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DayCellViewHolder(
        private val binding: ItemDayCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: DayCell) {
            if (cell.date == null) {
                // 빈 칸
                binding.root.visibility = View.INVISIBLE
                binding.root.isClickable = false
                return
            }

            binding.root.visibility = View.VISIBLE
            binding.tvDay.text = cell.date.dayOfMonth.toString()

            // 요일별 텍스트 색상 (일=빨강, 토=파랑)
            binding.tvDay.setTextColor(
                binding.root.context.getColor(
                    when (cell.date.dayOfWeek.value % 7) {
                        0 -> R.color.calendar_red   // 일요일
                        6 -> R.color.day_saturday   // 토요일
                        else -> R.color.text_primary
                    }
                )
            )

            // 소요시간 표시
            if (cell.elapsedText.isNotEmpty()) {
                binding.tvElapsed.visibility = View.VISIBLE
                binding.tvElapsed.text = cell.elapsedText
            } else {
                binding.tvElapsed.visibility = View.GONE
            }

            // 배경 (녹색/빨강/오늘 테두리/흰색)
            val bgRes = when {
                cell.isGreen -> R.drawable.bg_day_cell_green
                cell.isRed -> R.drawable.bg_day_cell_red
                cell.isToday -> R.drawable.bg_day_cell_today
                else -> R.drawable.bg_day_cell
            }
            binding.root.setBackgroundResource(bgRes)

            binding.root.setOnClickListener { onDayClick(cell.date) }
        }
    }

    private class DayCellDiffCallback : DiffUtil.ItemCallback<DayCell>() {
        override fun areItemsTheSame(old: DayCell, new: DayCell) = old.date == new.date
        override fun areContentsTheSame(old: DayCell, new: DayCell) = old == new
    }
}

data class DayCell(
    val date: LocalDate?,           // null = 빈 칸 (월 첫날 앞 패딩)
    val isToday: Boolean = false,
    val isGreen: Boolean = false,
    val isRed: Boolean = false,
    val elapsedText: String = ""
)
