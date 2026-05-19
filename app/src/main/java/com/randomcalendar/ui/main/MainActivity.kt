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
import com.randomcalendar.R
import com.randomcalendar.databinding.ActivityMainBinding
import com.randomcalendar.ui.calendar.CalendarAdapter
import com.randomcalendar.ui.calendar.CalendarBuilder
import com.randomcalendar.ui.common.ThemeHelper
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
        setupFontSize()
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
            if (date == calendarAdapter.selectedDate) {
                // 두 번째 탭: TODO 창 열기
                val sheet = DayDetailBottomSheet.newInstance(date)
                sheet.onDataChanged = { viewModel.refreshMonthData() }
                sheet.show(supportFragmentManager, DayDetailBottomSheet.TAG)
            } else {
                // 첫 번째 탭: 날짜 선택만
                calendarAdapter.selectedDate = date
                calendarAdapter.notifyDataSetChanged()
                viewModel.selectDate(date)
            }
        }
        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = calendarAdapter
            itemAnimator = null
        }
        calendarAdapter.applyTheme(com.randomcalendar.ui.common.ThemeHelper.load(this))
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

        binding.seekAchievement.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE ->
                    binding.drawerLayout.requestDisallowInterceptTouchEvent(true)
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL ->
                    binding.drawerLayout.requestDisallowInterceptTouchEvent(false)
            }
            false
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

        binding.etMemo.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.saveMemo(s.toString())
            }
        })

        binding.etMemo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) collapseMemoField()
        }
    }

    private fun collapseMemoField() {
        val text = binding.etMemo.text.toString()
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

    override fun attachBaseContext(newBase: Context) {
        val fontScale = ThemeHelper.loadFontScale(newBase)
        val config = newBase.resources.configuration
        config.fontScale = fontScale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private fun setupFontSize() {
        val saved = prefs.getString("font_size", "medium")
        when (saved) {
            "small" -> binding.chipFontSmall.isChecked = true
            "large" -> binding.chipFontLarge.isChecked = true
            else    -> binding.chipFontMedium.isChecked = true
        }
        binding.chipGroupFontSize.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val size = when (checkedIds[0]) {
                R.id.chipFontSmall -> "small"
                R.id.chipFontLarge -> "large"
                else               -> "medium"
            }
            if (size != prefs.getString("font_size", "medium")) {
                prefs.edit().putString("font_size", size).apply()
                recreate()
            }
        }
    }

    private fun applyThemeColors() {
        try {
            val c = com.randomcalendar.ui.common.ThemeHelper.load(this)
            val primary = prefs.getString("theme_primary", "#1976D2")!!
            val sidebar = prefs.getString("theme_sidebar", "#F5F5F5")!!
            val primaryColor = Color.parseColor(primary)
            val sidebarColor = Color.parseColor(sidebar)

            // 메인 콘텐츠 배경 (이게 없으면 다크테마에서 달력 글씨가 안 보임)
            binding.mainContent.setBackgroundColor(c.bgColor)
            binding.rvCalendar.setBackgroundColor(c.bgColor)

            // 슬로건 바
            binding.sloganBar.setBackgroundColor(primaryColor)
            binding.sidebarLayout.setBackgroundColor(sidebarColor)
            val onPrimary = if (isColorDark(primaryColor)) Color.WHITE else Color.BLACK
            binding.tvSlogan.setTextColor(onPrimary)
            binding.etSlogan.setTextColor(onPrimary)
            binding.btnMenu.setColorFilter(onPrimary)
            binding.btnEditSlogan.setColorFilter(onPrimary)

            // 월 네비게이션 카드
            binding.monthNavCard.setCardBackgroundColor(c.bgColor)
            binding.tvYearMonth.setTextColor(c.textColor)
            binding.btnPrevMonth.setColorFilter(c.textColor)
            binding.btnNextMonth.setColorFilter(c.textColor)

            // 메모 카드
            binding.memoCard.setCardBackgroundColor(c.bgColor)
            binding.tvMemo.setTextColor(Color.argb(140,
                Color.red(c.textColor), Color.green(c.textColor), Color.blue(c.textColor)))
            binding.etMemo.setTextColor(c.textColor)

            // SeekBar accent color
            val accentColor = Color.parseColor(prefs.getString("theme_accent", "#FF9800")!!)
            binding.seekAchievement.progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            binding.seekAchievement.thumbTintList = android.content.res.ColorStateList.valueOf(accentColor)

            // Sidebar text/button colors
            val onSidebar = if (isColorDark(sidebarColor)) Color.WHITE else Color.parseColor("#424242")
            binding.tvSidebarTitle.setTextColor(onSidebar)
            binding.tvAchievementLabel.setTextColor(onSidebar)
            binding.tvAchievementValue.setTextColor(onSidebar)
            binding.tvFontSizeLabel.setTextColor(onSidebar)
            (binding.btnRandomList as? com.google.android.material.button.MaterialButton)?.let { mb ->
                mb.strokeColor = android.content.res.ColorStateList.valueOf(onSidebar)
                mb.setTextColor(onSidebar)
            }
            (binding.btnTheme as? com.google.android.material.button.MaterialButton)?.let { mb ->
                mb.strokeColor = android.content.res.ColorStateList.valueOf(onSidebar)
                mb.setTextColor(onSidebar)
            }
            if (::calendarAdapter.isInitialized) {
                calendarAdapter.applyTheme(c)
            }
        } catch (_: Exception) {}
    }

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
    }
}
