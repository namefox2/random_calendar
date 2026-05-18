package com.randomcalendar.ui.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.databinding.ActivityMainBinding
import com.randomcalendar.ui.calendar.CalendarAdapter
import com.randomcalendar.ui.calendar.CalendarBuilder
import com.randomcalendar.ui.common.ViewModelFactory
import com.randomcalendar.ui.daydetail.DayDetailBottomSheet
import com.randomcalendar.ui.randomlist.RandomListActivity
import com.randomcalendar.ui.theme.ThemeActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var adView: AdView

    private val app get() = application as RandomCalendarApp
    private val prefs by lazy { getSharedPreferences("random_calendar_prefs", Context.MODE_PRIVATE) }

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(app.todoItemRepository, app.monthMemoRepository)
    }

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")
    private val yearMonthKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar()
        setupMonthNav()
        setupSlogan()
        setupSidebar()
        setupMemo()
        observeViewModel()
        applyThemeColors()
        setupAd()

        val savedThreshold = prefs.getInt("achievement_threshold", 80)
        binding.seekAchievement.progress = savedThreshold
        binding.tvAchievementValue.text = "${savedThreshold}%"
        viewModel.setAchievementThreshold(savedThreshold)
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { date ->
            viewModel.selectDate(date)
            val sheet = DayDetailBottomSheet.newInstance(date)
            sheet.onDataChanged = { viewModel.refreshMonthData() }
            sheet.show(supportFragmentManager, DayDetailBottomSheet.TAG)
        }
        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = calendarAdapter
            itemAnimator = null
        }
    }

    private fun setupMonthNav() {
        binding.btnPrevMonth.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.goToNextMonth() }
    }

    private fun setupSlogan() {
        val savedSlogan = prefs.getString("slogan", getString(com.randomcalendar.R.string.default_slogan))
        binding.tvSlogan.text = savedSlogan

        binding.btnEditSlogan.setOnClickListener {
            binding.tvSlogan.visibility = android.view.View.GONE
            binding.btnEditSlogan.visibility = android.view.View.GONE
            binding.etSlogan.visibility = android.view.View.VISIBLE
            binding.etSlogan.setText(binding.tvSlogan.text)
            binding.etSlogan.requestFocus()
            binding.etSlogan.selectAll()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSlogan, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.etSlogan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveSlogan()
                true
            } else false
        }

        binding.etSlogan.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveSlogan()
        }
    }

    private fun saveSlogan() {
        val text = binding.etSlogan.text.toString().ifBlank {
            getString(com.randomcalendar.R.string.default_slogan)
        }
        prefs.edit().putString("slogan", text).apply()
        binding.tvSlogan.text = text
        binding.tvSlogan.visibility = android.view.View.VISIBLE
        binding.btnEditSlogan.visibility = android.view.View.VISIBLE
        binding.etSlogan.visibility = android.view.View.GONE
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSlogan.windowToken, 0)
    }

    private fun setupSidebar() {
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(binding.sidebarLayout)) {
                binding.drawerLayout.closeDrawer(binding.sidebarLayout)
            } else {
                binding.drawerLayout.openDrawer(binding.sidebarLayout)
            }
        }

        binding.seekAchievement.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvAchievementValue.text = "${progress}%"
                if (fromUser) viewModel.setAchievementThreshold(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                prefs.edit().putInt("achievement_threshold", seekBar.progress).apply()
            }
        })

        binding.btnRandomList.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebarLayout)
            startActivity(android.content.Intent(this, RandomListActivity::class.java))
        }

        binding.btnTheme.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.sidebarLayout)
            startActivity(android.content.Intent(this, ThemeActivity::class.java))
        }
    }

    private fun setupMemo() {
        binding.tvMemo.setOnClickListener {
            binding.tvMemo.visibility = android.view.View.GONE
            binding.etMemo.visibility = android.view.View.VISIBLE
            binding.etMemo.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etMemo, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.etMemo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveMemo()
        }
    }

    private fun saveMemo() {
        val text = binding.etMemo.text.toString()
        viewModel.saveMemo(text)
        binding.tvMemo.text = text.ifBlank { "이달의 메모를 입력하세요..." }
        binding.tvMemo.setTextColor(
            getColor(if (text.isBlank()) com.randomcalendar.R.color.text_secondary
                     else com.randomcalendar.R.color.text_primary)
        )
        binding.tvMemo.visibility = android.view.View.VISIBLE
        binding.etMemo.visibility = android.view.View.GONE
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etMemo.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.currentYearMonth.observe(this) { ym ->
            binding.tvYearMonth.text = ym.format(monthFormatter)
            refreshCalendarCells(ym, viewModel.monthDayData.value)
        }

        viewModel.monthDayData.observe(this) { dataMap ->
            val ym = viewModel.currentYearMonth.value ?: return@observe
            refreshCalendarCells(ym, dataMap)
        }

        viewModel.currentMonthMemo.observe(this) { memo ->
            if (binding.etMemo.visibility == android.view.View.GONE) {
                val content = memo?.content ?: ""
                binding.tvMemo.text = content.ifBlank { "이달의 메모를 입력하세요..." }
                binding.etMemo.setText(content)
                binding.tvMemo.setTextColor(
                    getColor(if (content.isBlank()) com.randomcalendar.R.color.text_secondary
                             else com.randomcalendar.R.color.text_primary)
                )
            }
        }
    }

    private fun refreshCalendarCells(
        ym: YearMonth,
        dataMap: Map<String, MainViewModel.DayData>?
    ) {
        val cells = CalendarBuilder.build(
            yearMonth = ym,
            today = LocalDate.now(),
            dayDataMap = dataMap ?: emptyMap(),
            elapsedFormatter = { seconds ->
                app.todoItemRepository.formatElapsedTime(seconds)
            }
        )
        calendarAdapter.submitList(cells)
    }

    private fun setupAd() {
        adView = binding.adView
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun onResume() {
        super.onResume()
        applyThemeColors()
        if (::adView.isInitialized) adView.resume()
    }

    override fun onPause() {
        if (::adView.isInitialized) adView.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::adView.isInitialized) adView.destroy()
        super.onDestroy()
    }

    private fun applyThemeColors() {
        try {
            val primary = prefs.getString("theme_primary", "#1976D2")!!
            val sidebar = prefs.getString("theme_sidebar", "#F5F5F5")!!
            val primaryColor = Color.parseColor(primary)
            val sidebarColor = Color.parseColor(sidebar)
            binding.sloganBar.setBackgroundColor(primaryColor)
            binding.sidebarLayout.setBackgroundColor(sidebarColor)

            val onPrimary = if (isColorDark(primaryColor)) Color.WHITE else Color.BLACK
            binding.tvSlogan.setTextColor(onPrimary)
            binding.etSlogan.setTextColor(onPrimary)
            binding.btnMenu.setColorFilter(onPrimary)
            binding.btnEditSlogan.setColorFilter(onPrimary)
        } catch (_: Exception) {}
    }

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
    }
}
