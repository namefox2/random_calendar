package com.letsgo.randomcalendar.ui.theme

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.letsgo.randomcalendar.R
import com.letsgo.randomcalendar.databinding.ActivityThemeBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding

    override fun attachBaseContext(newBase: Context) {
        val fontScale = ThemeHelper.loadFontScale(newBase)
        val config = newBase.resources.configuration
        config.fontScale = fontScale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (com.letsgo.randomcalendar.ui.common.ThemeHelper.isHandwritingFont(this))
            setTheme(com.letsgo.randomcalendar.R.style.Theme_RandomCalendar_Gaegu)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ThemePresetFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val c = ThemeHelper.load(this)
            ThemeHelper.applyToolbar(binding.toolbar, c)
        } catch (_: Exception) {}
    }
}
