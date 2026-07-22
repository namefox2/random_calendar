package com.letsgo.randomcalendar.ui.daydetail

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
import com.letsgo.randomcalendar.data.db.entity.TodoItem
import com.letsgo.randomcalendar.databinding.FragmentTodoTabBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper
import com.letsgo.randomcalendar.ui.timer.TimerManager
import com.letsgo.randomcalendar.ui.timer.TimerService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodoTabFragment : Fragment() {

    private var _binding: FragmentTodoTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DayDetailViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var todoAdapter: TodoAdapter
    private var pickedDate: LocalDate? = null

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
        val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN)
        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date.format(formatter)
        }
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

        binding.chipPickDate.setOnCheckedChangeListener { _, checked ->
            binding.btnPickDate.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked && pickedDate == null) openDatePicker()
        }
        binding.chipRepeat.setOnCheckedChangeListener { _, checked ->
            binding.chipGroupWeekdays.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.btnPickDate.setOnClickListener { openDatePicker() }

        binding.btnAdd.setOnClickListener { submitNewTodo() }

        binding.etNewName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitNewTodo()
                true
            } else false
        }
    }

    private fun openDatePicker() {
        val base = viewModel.currentDate.value ?: LocalDate.now()
        val today = LocalDate.now()
        val initial = if (base.isBefore(today)) today else base
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                pickedDate = LocalDate.of(year, month + 1, day)
                val fmt = DateTimeFormatter.ofPattern("M월 d일")
                binding.btnPickDate.text = pickedDate!!.format(fmt)
            },
            initial.year, initial.monthValue - 1, initial.dayOfMonth
        ).apply {
            datePicker.minDate = java.util.Calendar.getInstance().timeInMillis
        }.show()
    }

    private fun computeTargetDates(currentDate: LocalDate): List<LocalDate> {
        val today = LocalDate.now()
        val year = currentDate.year
        val month = currentDate.month
        val daysInMonth = month.length(currentDate.isLeapYear)
        val futureDays = (1..daysInMonth).map { LocalDate.of(year, month, it) }
            .filter { !it.isBefore(today) }
        return when {
            binding.chipEveryDay.isChecked -> futureDays
            binding.chipOddDays.isChecked -> futureDays.filter { it.dayOfMonth % 2 == 1 }
            binding.chipEvenDays.isChecked -> futureDays.filter { it.dayOfMonth % 2 == 0 }
            binding.chipPickDate.isChecked -> listOf(pickedDate ?: currentDate)
            binding.chipRepeat.isChecked -> {
                val weekdays = buildSet<DayOfWeek> {
                    if (binding.chipMon.isChecked) add(DayOfWeek.MONDAY)
                    if (binding.chipTue.isChecked) add(DayOfWeek.TUESDAY)
                    if (binding.chipWed.isChecked) add(DayOfWeek.WEDNESDAY)
                    if (binding.chipThu.isChecked) add(DayOfWeek.THURSDAY)
                    if (binding.chipFri.isChecked) add(DayOfWeek.FRIDAY)
                    if (binding.chipSat.isChecked) add(DayOfWeek.SATURDAY)
                    if (binding.chipSun.isChecked) add(DayOfWeek.SUNDAY)
                }
                if (weekdays.isEmpty()) listOf(currentDate)
                else futureDays.filter { it.dayOfWeek in weekdays }
            }
            else -> listOf(currentDate) // chipToday
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

        val currentDate = viewModel.currentDate.value ?: LocalDate.now()
        val targetDates = computeTargetDates(currentDate)
        viewModel.addTodoToMultipleDates(targetDates, name, url, timerType, goalSeconds, workSec, restSec, sets)

        binding.etNewName.text?.clear()
        binding.etNewUrl.text?.clear()
        binding.etGoalMinutes.text?.clear()
        binding.etWorkSeconds.text?.clear()
        binding.etRestSeconds.text?.clear()
        binding.etSetCount.text?.clear()
        binding.chipUrl.isChecked = false
        binding.chipNormalTimer.isChecked = false
        binding.chipSetTimer.isChecked = false
        binding.chipToday.isChecked = true
        pickedDate = null
        binding.btnPickDate.text = "날짜를 선택하세요"

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
                if (summary.elapsedText.isNotEmpty()) append(" · ${summary.elapsedText}")
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

            // Chip text + background: adapt to theme
            val chipText = if (ThemeHelper.isColorDark(c.bgColor)) android.graphics.Color.WHITE
                           else android.graphics.Color.parseColor("#212121")
            val chipCsl = android.content.res.ColorStateList.valueOf(chipText)
            val chipBgColor = if (ThemeHelper.isColorDark(c.bgColor)) {
                android.graphics.Color.argb(255,
                    minOf(android.graphics.Color.red(c.bgColor) + 70, 255),
                    minOf(android.graphics.Color.green(c.bgColor) + 70, 255),
                    minOf(android.graphics.Color.blue(c.bgColor) + 70, 255))
            } else {
                android.graphics.Color.parseColor("#E0E0E0")
            }
            val chipBgCsl = android.content.res.ColorStateList.valueOf(chipBgColor)
            listOf(
                binding.chipUrl, binding.chipNormalTimer, binding.chipSetTimer,
                binding.chipToday, binding.chipEveryDay, binding.chipOddDays,
                binding.chipEvenDays, binding.chipPickDate, binding.chipRepeat,
                binding.chipMon, binding.chipTue, binding.chipWed, binding.chipThu,
                binding.chipFri, binding.chipSat, binding.chipSun
            ).forEach { chip ->
                chip.setTextColor(chipCsl)
                chip.chipBackgroundColor = chipBgCsl
            }

            // Form labels
            val labelText = if (ThemeHelper.isColorDark(c.bgColor)) android.graphics.Color.WHITE
                            else android.graphics.Color.parseColor("#757575")
            binding.tvGoalMinutesLabel.setTextColor(labelText)
            binding.tvWorkLabel.setTextColor(labelText)
            binding.tvRestLabel.setTextColor(labelText)
            binding.tvSetCountLabel.setTextColor(labelText)

            // EditText text/hint/background colors
            val hintColor = if (ThemeHelper.isColorDark(c.bgColor)) 0xFFBDBDBD.toInt() else 0xFF9E9E9E.toInt()
            val density = resources.displayMetrics.density
            listOf(
                binding.etNewName, binding.etNewUrl,
                binding.etGoalMinutes, binding.etWorkSeconds,
                binding.etRestSeconds, binding.etSetCount
            ).forEach { et ->
                et.setTextColor(c.textColor)
                et.setHintTextColor(hintColor)
                val inputBg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 8 * density
                    setColor(android.graphics.Color.argb(25,
                        android.graphics.Color.red(c.textColor),
                        android.graphics.Color.green(c.textColor),
                        android.graphics.Color.blue(c.textColor)))
                    setStroke((1 * density).toInt(), android.graphics.Color.argb(60,
                        android.graphics.Color.red(c.textColor),
                        android.graphics.Color.green(c.textColor),
                        android.graphics.Color.blue(c.textColor)))
                }
                et.background = inputBg
            }

            // RecyclerView background
            binding.rvTodos.setBackgroundColor(c.bgColor)

            // Apply typeface to option chips
            val chipTypeface = ThemeHelper.resolveTypeface(requireContext())
            listOf(
                binding.chipUrl, binding.chipNormalTimer, binding.chipSetTimer,
                binding.chipToday, binding.chipEveryDay, binding.chipOddDays,
                binding.chipEvenDays, binding.chipPickDate, binding.chipRepeat,
                binding.chipMon, binding.chipTue, binding.chipWed, binding.chipThu,
                binding.chipFri, binding.chipSat, binding.chipSun
            ).forEach {
                it.typeface = chipTypeface
            }
            binding.tvScheduleLabel.setTextColor(labelText)

            // Update adapter
            todoAdapter.applyThemeColors(c, chipTypeface)
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
