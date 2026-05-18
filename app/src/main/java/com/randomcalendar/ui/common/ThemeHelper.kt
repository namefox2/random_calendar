package com.randomcalendar.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

object ThemeHelper {

    private const val PREFS = "random_calendar_prefs"

    data class Colors(
        val primaryColor: Int,
        val accentColor: Int,
        val bgColor: Int,
        val textColor: Int
    ) {
        val onPrimary: Int get() = if (isDark(primaryColor)) Color.WHITE else Color.BLACK
        val onAccent: Int  get() = if (isDark(accentColor))  Color.WHITE else Color.BLACK

        private fun isDark(color: Int): Boolean {
            val r = Color.red(color) / 255.0
            val g = Color.green(color) / 255.0
            val b = Color.blue(color) / 255.0
            return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
        }
    }

    fun load(context: Context): Colors {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Colors(
            primaryColor = Color.parseColor(p.getString("theme_primary", "#1976D2")!!),
            accentColor  = Color.parseColor(p.getString("theme_accent",  "#FF9800")!!),
            bgColor      = Color.parseColor(p.getString("theme_bg",      "#FFFFFF")!!),
            textColor    = Color.parseColor(p.getString("theme_text",    "#212121")!!)
        )
    }

    fun applyToolbar(toolbar: Toolbar, colors: Colors) {
        toolbar.setBackgroundColor(colors.primaryColor)
        toolbar.setTitleTextColor(colors.onPrimary)
        toolbar.navigationIcon?.setTint(colors.onPrimary)
    }

    fun applyTabLayout(tabLayout: TabLayout, colors: Colors) {
        tabLayout.setSelectedTabIndicatorColor(colors.primaryColor)
        tabLayout.setTabTextColors(Color.parseColor("#757575"), colors.primaryColor)
    }

    fun applyButton(button: MaterialButton, colors: Colors) {
        button.backgroundTintList = ColorStateList.valueOf(colors.accentColor)
        button.setTextColor(colors.onAccent)
    }

    fun applyOutlinedButton(button: MaterialButton, colors: Colors) {
        button.strokeColor = ColorStateList.valueOf(colors.primaryColor)
        button.setTextColor(colors.primaryColor)
    }
}
