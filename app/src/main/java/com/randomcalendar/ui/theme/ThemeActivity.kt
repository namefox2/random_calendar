package com.randomcalendar.ui.theme

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.randomcalendar.R
import com.randomcalendar.databinding.ActivityThemeBinding
import com.randomcalendar.ui.common.ThemeHelper

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding

    override fun attachBaseContext(newBase: Context) {
        val fontScale = ThemeHelper.loadFontScale(newBase)
        val config = newBase.resources.configuration
        config.fontScale = fontScale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (com.randomcalendar.ui.common.ThemeHelper.isHandwritingFont(this))
            setTheme(com.randomcalendar.R.style.Theme_RandomCalendar_Gaegu)
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
