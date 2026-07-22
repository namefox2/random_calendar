package com.letsgo.randomcalendar.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.letsgo.randomcalendar.R
import com.letsgo.randomcalendar.databinding.ItemDayCellBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper
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

    private fun oval(fillColor: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
    }

    private fun ovalStroke(strokeColor: Int, strokeDp: Float, ctx: android.content.Context) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
        val px = (strokeDp * ctx.resources.displayMetrics.density).toInt()
        setStroke(px, strokeColor)
    }

    private fun roundRect(fillColor: Int, radiusDp: Float, ctx: android.content.Context) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        cornerRadius = radiusDp * ctx.resources.displayMetrics.density
    }

    private fun withAlpha(color: Int, alpha: Int) =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    inner class DayCellViewHolder(
        private val binding: ItemDayCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: DayCell) {
            val ctx = binding.root.context
            val c = themeColors
            val primaryColor = c?.primaryColor ?: ctx.getColor(R.color.primary)
            val textColor = c?.textColor ?: ctx.getColor(R.color.text_primary)

            if (cell.date == null) {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
                binding.tvDay.text = ""
                binding.tvDay.background = null
                binding.tvElapsed.visibility = View.GONE
                binding.root.isClickable = false
                return
            }

            binding.root.isClickable = cell.isCurrentMonth
            binding.tvDay.text = cell.date.dayOfMonth.toString()

            // 이전/다음 달: 날짜 회색, 클릭 불가
            if (!cell.isCurrentMonth) {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
                binding.tvDay.background = null
                binding.tvDay.setTextColor(withAlpha(textColor, 70))
                binding.tvElapsed.visibility = View.GONE
                binding.root.setOnClickListener(null)
                return
            }

            // 이번 달 날짜 텍스트 색상
            val isSelected = cell.date == selectedDate
            val dayOfWeek = cell.date.dayOfWeek.value % 7 // 0=일, 6=토

            // tvDay 원형 배경 (선택/오늘/기본)
            val accentColor = c?.accentColor ?: ctx.getColor(R.color.accent)

            when {
                isSelected -> {
                    binding.tvDay.background = oval(accentColor)
                    binding.tvDay.setTextColor(
                        if (ThemeHelper.isColorDark(accentColor)) Color.WHITE else Color.BLACK
                    )
                }
                cell.isToday -> {
                    binding.tvDay.background = ovalStroke(accentColor, 2.5f, ctx)
                    binding.tvDay.setTextColor(accentColor)
                }
                else -> {
                    binding.tvDay.background = null
                    binding.tvDay.setTextColor(
                        when (dayOfWeek) {
                            0 -> ctx.getColor(R.color.calendar_red)
                            6 -> ctx.getColor(R.color.day_saturday)
                            else -> textColor
                        }
                    )
                }
            }

            // 셀 배경: 달성(녹색)/미달성(빨강) 표시
            binding.root.background = when {
                cell.isGreen -> roundRect(withAlpha(Color.parseColor("#81C784"), 55), 6f, ctx)
                cell.isRed   -> roundRect(withAlpha(Color.parseColor("#E57373"), 55), 6f, ctx)
                else         -> null
            }

            // 소요시간
            if (cell.elapsedText.isNotEmpty()) {
                binding.tvElapsed.visibility = View.VISIBLE
                binding.tvElapsed.text = cell.elapsedText
                binding.tvElapsed.setTextColor(withAlpha(textColor, 150))
            } else {
                binding.tvElapsed.visibility = View.GONE
            }

            binding.root.setOnClickListener { onDayClick(cell.date) }
        }
    }

    private class DayCellDiffCallback : DiffUtil.ItemCallback<DayCell>() {
        override fun areItemsTheSame(old: DayCell, new: DayCell) =
            old.date == new.date && old.isCurrentMonth == new.isCurrentMonth
        override fun areContentsTheSame(old: DayCell, new: DayCell) = old == new
    }
}

data class DayCell(
    val date: LocalDate?,
    val isCurrentMonth: Boolean = true,
    val isToday: Boolean = false,
    val isGreen: Boolean = false,
    val isRed: Boolean = false,
    val elapsedText: String = ""
)
