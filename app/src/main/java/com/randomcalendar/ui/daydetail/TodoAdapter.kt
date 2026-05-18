package com.randomcalendar.ui.daydetail

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.randomcalendar.R
import com.randomcalendar.data.db.entity.TodoItem
import com.randomcalendar.databinding.ItemTodoBinding
import com.randomcalendar.ui.common.ThemeHelper
import com.randomcalendar.ui.timer.TimerManager

class TodoAdapter(
    private val onToggleDone: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit,
    private val onTimerTypeChange: (TodoItem, String) -> Unit,
    private val onTimerStart: (TodoItem) -> Unit,
    private val onTimerPause: () -> Unit,
    private val onTimerReset: (Long) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(TodoDiffCallback()) {

    private val expandedIds = mutableSetOf<Long>()

    private var themeColors: ThemeHelper.Colors? = null

    fun applyThemeColors(colors: ThemeHelper.Colors) {
        themeColors = colors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodoItem) {
            themeColors?.let { c ->
                binding.root.setBackgroundColor(c.bgColor)
                binding.tvName.setTextColor(c.textColor)
                binding.tvElapsed.setTextColor(c.textColor)
            }

            val isExpanded = item.id in expandedIds

            // 체크박스
            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = item.isDone
            binding.cbDone.setOnCheckedChangeListener { _, _ -> onToggleDone(item) }

            // 이름 (완료 시 취소선)
            binding.tvName.text = item.name
            binding.tvName.paintFlags = if (item.isDone) {
                binding.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // 경과 시간
            val elapsedText = TimerManager.formatSeconds(item.elapsedSeconds)
            if (item.elapsedSeconds > 0) {
                binding.tvElapsed.visibility = View.VISIBLE
                binding.tvElapsed.text = elapsedText
            } else {
                binding.tvElapsed.visibility = View.GONE
            }

            // 펼치기/접기
            binding.btnExpand.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            binding.expandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

            binding.headerRow.setOnClickListener {
                if (item.id in expandedIds) expandedIds.remove(item.id)
                else expandedIds.add(item.id)
                val pos = currentList.indexOfFirst { it.id == item.id }
                if (pos != -1) notifyItemChanged(pos)
            }
            binding.btnExpand.setOnClickListener {
                binding.headerRow.performClick()
            }

            if (isExpanded) bindExpanded(item)
        }

        private fun bindExpanded(item: TodoItem) {
            // URL
            if (item.url.isNotBlank()) {
                binding.tvUrl.visibility = View.VISIBLE
                binding.tvUrl.text = item.url
                binding.tvUrl.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        it.context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            } else {
                binding.tvUrl.visibility = View.GONE
            }

            // 타이머 타입 칩 (리스너 해제 후 상태 설정, 재등록)
            binding.chipGroupTimerType.setOnCheckedStateChangeListener(null)
            binding.chipTimerNone.isChecked = item.timerType == "NONE"
            binding.chipTimerNormal.isChecked = item.timerType == "NORMAL"
            binding.chipTimerSet.isChecked = item.timerType == "SET"
            binding.chipGroupTimerType.setOnCheckedStateChangeListener { _, checkedIds ->
                val type = when {
                    R.id.chipTimerNormal in checkedIds -> "NORMAL"
                    R.id.chipTimerSet in checkedIds -> "SET"
                    else -> "NONE"
                }
                if (type != item.timerType) onTimerTypeChange(item, type)
            }

            // 타이머 섹션 표시
            binding.sectionNormalTimer.visibility =
                if (item.timerType == "NORMAL") View.VISIBLE else View.GONE
            binding.sectionSetTimer.visibility =
                if (item.timerType == "SET") View.VISIBLE else View.GONE

            // 현재 실행 중인 타이머 상태 구독
            val timerState = TimerManager.state.value

            when (item.timerType) {
                "NORMAL" -> bindNormalTimer(item, timerState)
                "SET" -> bindSetTimer(item, timerState)
            }

            // 삭제
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }

        private fun bindNormalTimer(item: TodoItem, state: TimerManager.TimerState?) {
            val isActive = state?.todoId == item.id
            val elapsed = if (isActive) state!!.elapsedSeconds else item.elapsedSeconds
            binding.tvTimerDisplay.text = TimerManager.formatSeconds(elapsed)

            if (item.timerGoalSeconds != null) {
                binding.tvTimerGoal.visibility = View.VISIBLE
                val remaining = (item.timerGoalSeconds - elapsed).coerceAtLeast(0)
                binding.tvTimerGoal.text = "목표: ${TimerManager.formatSeconds(item.timerGoalSeconds)} · 남은시간: ${TimerManager.formatSeconds(remaining)}"
            } else {
                binding.tvTimerGoal.visibility = View.GONE
            }

            val isRunning = isActive && state!!.isRunning
            binding.btnTimerStart.isEnabled = !isRunning
            binding.btnTimerPause.isEnabled = isRunning

            binding.btnTimerStart.setOnClickListener { onTimerStart(item) }
            binding.btnTimerPause.setOnClickListener { onTimerPause() }
            binding.btnTimerReset.setOnClickListener { onTimerReset(item.id) }
        }

        private fun bindSetTimer(item: TodoItem, state: TimerManager.TimerState?) {
            val isActive = state?.todoId == item.id
            val elapsed = if (isActive) state!!.elapsedSeconds else item.elapsedSeconds

            val currentSet = if (isActive) state!!.currentSet else 1
            val isWorkPhase = if (isActive) state!!.isWorkPhase else true
            val phaseRemaining = if (isActive) state!!.phaseRemaining else item.setWorkSeconds
            val phaseProgress = if (isActive) state!!.phaseProgress else 100

            binding.tvSetPhase.text = if (isWorkPhase) "운동중" else "휴식중"
            binding.tvSetPhase.setTextColor(
                binding.root.context.getColor(
                    if (isWorkPhase) R.color.primary else R.color.accent
                )
            )
            binding.tvSetCount.text = "$currentSet / ${item.setCount} 세트"
            binding.tvSetTimerDisplay.text = TimerManager.formatSeconds(phaseRemaining)
            binding.pbSetPhase.progress = phaseProgress

            val isRunning = isActive && state!!.isRunning
            binding.btnSetStart.isEnabled = !isRunning
            binding.btnSetPause.isEnabled = isRunning

            binding.btnSetStart.setOnClickListener { onTimerStart(item) }
            binding.btnSetPause.setOnClickListener { onTimerPause() }
            binding.btnSetReset.setOnClickListener { onTimerReset(item.id) }
        }
    }

    private class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(old: TodoItem, new: TodoItem) = old.id == new.id
        override fun areContentsTheSame(old: TodoItem, new: TodoItem) = old == new
    }
}
