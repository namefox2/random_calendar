package com.randomcalendar.ui.daydetail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.randomcalendar.data.db.entity.TodoItem
import com.randomcalendar.databinding.FragmentTodoTabBinding
import com.randomcalendar.ui.common.ThemeHelper
import com.randomcalendar.ui.timer.TimerManager
import com.randomcalendar.ui.timer.TimerService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodoTabFragment : Fragment() {

    private var _binding: FragmentTodoTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DayDetailViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var todoAdapter: TodoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTodoTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupAddForm()
        observeViewModel()
        observeTimer()
        applyTheme()
    }

    private fun setupHeader() {
        val date = LocalDate.parse(viewModel.dateStr)
        val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN)
        binding.tvDate.text = date.format(formatter)
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onToggleDone = { viewModel.toggleDone(it) },
            onDelete = { viewModel.delete(it) },
            onTimerTypeChange = { item, type -> viewModel.updateTimerType(item, type) },
            onTimerStart = { item -> startTimer(item) },
            onTimerPause = {
                TimerManager.pause()
                todoAdapter.notifyDataSetChanged()
            },
            onTimerReset = { id ->
                TimerManager.reset(id)
                viewModel.updateElapsedSeconds(id, 0)
                todoAdapter.notifyDataSetChanged()
            }
        )
        binding.rvTodos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todoAdapter
            itemAnimator = null
        }
    }

    private fun startTimer(item: TodoItem) {
        // 다른 타이머가 실행 중이면 중단
        val current = TimerManager.state.value
        if (current != null && current.todoId != item.id) {
            TimerManager.clear()
        }

        if (item.timerType == "NORMAL") {
            TimerManager.startNormal(
                todoId = item.id,
                elapsedSeconds = item.elapsedSeconds,
                goalSeconds = item.timerGoalSeconds
            )
        } else if (item.timerType == "SET") {
            val existing = TimerManager.state.value
            val resuming = existing?.todoId == item.id && !existing.isRunning
            if (resuming) {
                TimerManager.resume()
            } else {
                TimerManager.startSet(
                    todoId = item.id,
                    elapsedSeconds = item.elapsedSeconds,
                    workSeconds = item.setWorkSeconds,
                    restSeconds = item.setRestSeconds,
                    totalSets = item.setCount
                )
            }
        }
        TimerService.start(requireContext())
        todoAdapter.notifyDataSetChanged()
    }

    private fun setupAddForm() {
        binding.chipUrl.setOnCheckedChangeListener { _, checked ->
            binding.etNewUrl.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.chipNormalTimer.setOnCheckedChangeListener { _, checked ->
            binding.layoutNormalTimerInput.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) binding.chipSetTimer.isChecked = false
        }
        binding.chipSetTimer.setOnCheckedChangeListener { _, checked ->
            binding.layoutSetTimerInput.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) binding.chipNormalTimer.isChecked = false
        }

        binding.btnAdd.setOnClickListener { submitNewTodo() }

        binding.etNewName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitNewTodo()
                true
            } else false
        }
    }

    private fun submitNewTodo() {
        val name = binding.etNewName.text.toString().trim()
        if (name.isBlank()) return

        val url = if (binding.chipUrl.isChecked)
            binding.etNewUrl.text.toString().trim() else ""

        val timerType = when {
            binding.chipNormalTimer.isChecked -> "NORMAL"
            binding.chipSetTimer.isChecked -> "SET"
            else -> "NONE"
        }

        val goalSeconds = if (timerType == "NORMAL") {
            binding.etGoalMinutes.text.toString().toIntOrNull()?.let { it * 60 }
        } else null

        val workSec = if (timerType == "SET")
            binding.etWorkSeconds.text.toString().toIntOrNull() ?: 30 else 0
        val restSec = if (timerType == "SET")
            binding.etRestSeconds.text.toString().toIntOrNull() ?: 10 else 0
        val sets = if (timerType == "SET")
            binding.etSetCount.text.toString().toIntOrNull() ?: 3 else 0

        viewModel.addTodo(name, url, timerType, goalSeconds, workSec, restSec, sets)

        // 폼 초기화
        binding.etNewName.text?.clear()
        binding.etNewUrl.text?.clear()
        binding.etGoalMinutes.text?.clear()
        binding.etWorkSeconds.text?.clear()
        binding.etRestSeconds.text?.clear()
        binding.etSetCount.text?.clear()
        binding.chipUrl.isChecked = false
        binding.chipNormalTimer.isChecked = false
        binding.chipSetTimer.isChecked = false

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etNewName.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.todos.observe(viewLifecycleOwner) { todos ->
            todoAdapter.submitList(todos.toList())
        }

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            binding.tvSummary.text = buildString {
                append("달성 ${summary.done}/${summary.total}")
                if (summary.totalElapsedSeconds > 0) {
                    val h = summary.totalElapsedSeconds / 3600
                    val m = (summary.totalElapsedSeconds % 3600) / 60
                    append(" · ")
                    if (h > 0) append("${h}h ")
                    if (m > 0) append("${m}m")
                }
            }
            binding.pbAchievement.progress = summary.achievementRate
        }
    }

    private fun observeTimer() {
        TimerManager.state.observe(viewLifecycleOwner) {
            todoAdapter.notifyDataSetChanged()
        }
    }

    private fun applyTheme() {
        try {
            val c = ThemeHelper.load(requireContext())
            ThemeHelper.applyButton(binding.btnAdd, c)
            binding.pbAchievement.progressTintList =
                android.content.res.ColorStateList.valueOf(c.primaryColor)

            // Apply theme bg to header and form sections
            binding.layoutTodoHeader.setBackgroundColor(c.bgColor)
            binding.layoutTodoForm.setBackgroundColor(c.bgColor)

            // Apply theme text colors to header views
            binding.tvDate.setTextColor(c.textColor)
            binding.tvSummary.setTextColor(c.textColor)

            // Chip text: contrast against bgColor
            val chipText = if (isDarkColor(c.bgColor)) android.graphics.Color.WHITE
                           else android.graphics.Color.parseColor("#212121")
            val chipCsl = android.content.res.ColorStateList.valueOf(chipText)
            binding.chipUrl.setTextColor(chipCsl)
            binding.chipNormalTimer.setTextColor(chipCsl)
            binding.chipSetTimer.setTextColor(chipCsl)

            // Form labels
            val labelText = if (isDarkColor(c.bgColor)) android.graphics.Color.WHITE
                            else android.graphics.Color.parseColor("#757575")
            binding.tvGoalMinutesLabel.setTextColor(labelText)
            binding.tvWorkLabel.setTextColor(labelText)
            binding.tvRestLabel.setTextColor(labelText)
            binding.tvSetCountLabel.setTextColor(labelText)

            // EditText text/hint colors
            val hintColor = if (isDarkColor(c.bgColor)) 0xFFBDBDBD.toInt() else 0xFF9E9E9E.toInt()
            listOf(
                binding.etNewName, binding.etNewUrl,
                binding.etGoalMinutes, binding.etWorkSeconds,
                binding.etRestSeconds, binding.etSetCount
            ).forEach { et ->
                et.setTextColor(c.textColor)
                et.setHintTextColor(hintColor)
            }

            // RecyclerView background
            binding.rvTodos.setBackgroundColor(c.bgColor)

            // Update adapter
            todoAdapter.applyThemeColors(c)
        } catch (_: Exception) {}
    }

    private fun isDarkColor(color: Int): Boolean {
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
