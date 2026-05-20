package com.letsgo.randomcalendar.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.letsgo.randomcalendar.R

object ThemeHelper {

    private const val PREFS = "random_calendar_prefs"

    data class Colors(
        val primaryColor: Int,
        val accentColor: Int,
        val bgColor: Int,
        val textColor: Int
    ) {
        val onPrimary: Int get() = if (isColorDark(primaryColor)) Color.WHITE else Color.BLACK
        val onAccent: Int  get() = if (isColorDark(accentColor))  Color.WHITE else Color.BLACK
    }

    fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
    }

    fun isHandwritingFont(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("font_style", "handwriting") == "handwriting"
    }

    fun resolveTypeface(context: Context): Typeface =
        if (isHandwritingFont(context))
            ResourcesCompat.getFont(context, R.font.gaegu) ?: Typeface.DEFAULT
        else Typeface.DEFAULT

    fun loadFontScale(context: Context): Float {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (p.getString("font_size", "medium")) {
            "small" -> 0.85f
            "large" -> 1.2f
            else    -> 1.0f
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
        tabLayout.setBackgroundColor(colors.bgColor)
        val unselected = Color.argb(
            (255 * 0.6).toInt(),
            Color.red(colors.textColor),
            Color.green(colors.textColor),
            Color.blue(colors.textColor)
        )
        tabLayout.setTabTextColors(unselected, colors.primaryColor)
    }

    fun applyButton(button: android.widget.Button, colors: Colors) {
        button.backgroundTintList = ColorStateList.valueOf(colors.accentColor)
        button.setTextColor(colors.onAccent)
    }

    fun applyOutlinedButton(button: MaterialButton, colors: Colors) {
        button.strokeColor = ColorStateList.valueOf(colors.primaryColor)
        button.setTextColor(colors.primaryColor)
    }

    fun applyChip(chip: Chip, colors: Colors, typeface: Typeface) {
        val text = if (isColorDark(colors.bgColor)) Color.WHITE else Color.parseColor("#212121")
        chip.setTextColor(ColorStateList.valueOf(text))
        val bg = if (isColorDark(colors.bgColor)) Color.argb(255,
            minOf(Color.red(colors.bgColor) + 70, 255),
            minOf(Color.green(colors.bgColor) + 70, 255),
            minOf(Color.blue(colors.bgColor) + 70, 255))
        else Color.parseColor("#E0E0E0")
        chip.chipBackgroundColor = ColorStateList.valueOf(bg)
        chip.typeface = typeface
    }
}
