package com.randomcalendar.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.randomcalendar.R
import com.randomcalendar.databinding.ItemDayCellBinding
import com.randomcalendar.ui.common.ThemeHelper
import java.time.LocalDate

class CalendarAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : ListAdapter<DayCell, CalendarAdapter.DayCellViewHolder>(DayCellDiffCallback()) {

    var selectedDate: LocalDate? = null
    private var themeColors: ThemeHelper.Colors? = null

    fun applyTheme(colors: ThemeHelper.Colors) {
        themeColors = colors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayCellViewHolder {
        val binding = ItemDayCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayCellViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun makeCellBg(fillColor: Int, strokeColor: Int, strokeDp: Float = 1f, context: android.content.Context): GradientDrawable {
        val strokePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, strokeDp, context.resources.displayMetrics).toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 0f
            setColor(fillColor)
            setStroke(strokePx, strokeColor)
        }
    }

    private fun withAlpha(color: Int, alpha: Int) =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    inner class DayCellViewHolder(
        private val binding: ItemDayCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: DayCell) {
            val ctx = binding.root.context
            val c = themeColors

            if (cell.date == null) {
                // 빈 칸: 격자선만 표시
                binding.root.visibility = View.VISIBLE
                binding.tvDay.text = ""
                binding.tvElapsed.visibility = View.GONE
                binding.root.isClickable = false
                if (c != null) {
                    binding.root.background = makeCellBg(Color.TRANSPARENT, withAlpha(c.textColor, 30), 1f, ctx)
                } else {
                    binding.root.setBackgroundResource(R.drawable.bg_day_cell)
                }
                return
            }

            binding.root.visibility = View.VISIBLE
            binding.root.isClickable = true
            binding.tvDay.text = cell.date.dayOfMonth.toString()

            // 요일별 텍스트 색상 (일=빨강, 토=파랑)
            binding.tvDay.setTextColor(
                ctx.getColor(
                    when (cell.date.dayOfWeek.value % 7) {
                        0 -> R.color.calendar_red
                        6 -> R.color.day_saturday
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

            // 배경: 테마 색상 기반 programmatic 또는 drawable fallback
            val isSelected = cell.date == selectedDate
            if (c != null) {
                binding.root.background = when {
                    isSelected -> makeCellBg(withAlpha(c.primaryColor, 60), c.primaryColor, 2f, ctx)
                    cell.isGreen -> makeCellBg(Color.parseColor("#81C784"), withAlpha(c.textColor, 50), 1f, ctx)
                    cell.isRed -> makeCellBg(Color.parseColor("#E57373"), withAlpha(c.textColor, 50), 1f, ctx)
                    cell.isToday -> makeCellBg(withAlpha(c.primaryColor, 20), c.primaryColor, 2f, ctx)
                    else -> makeCellBg(Color.TRANSPARENT, withAlpha(c.textColor, 30), 1f, ctx)
                }
            } else {
                val bgRes = when {
                    isSelected -> R.drawable.bg_day_cell_selected
                    cell.isGreen -> R.drawable.bg_day_cell_green
                    cell.isRed -> R.drawable.bg_day_cell_red
                    cell.isToday -> R.drawable.bg_day_cell_today
                    else -> R.drawable.bg_day_cell
                }
                binding.root.setBackgroundResource(bgRes)
            }

            binding.root.setOnClickListener { onDayClick(cell.date) }
        }
    }

    private class DayCellDiffCallback : DiffUtil.ItemCallback<DayCell>() {
        override fun areItemsTheSame(old: DayCell, new: DayCell) = old.date == new.date
        override fun areContentsTheSame(old: DayCell, new: DayCell) = old == new
    }
}

data class DayCell(
    val date: LocalDate?,
    val isToday: Boolean = false,
    val isGreen: Boolean = false,
    val isRed: Boolean = false,
    val elapsedText: String = ""
)
