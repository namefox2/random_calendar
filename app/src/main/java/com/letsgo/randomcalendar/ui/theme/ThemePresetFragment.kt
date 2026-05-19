package com.letsgo.randomcalendar.ui.theme

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.letsgo.randomcalendar.databinding.FragmentThemePresetBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper

class ThemePresetFragment : Fragment() {

    private var _binding: FragmentThemePresetBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("random_calendar_prefs", Context.MODE_PRIVATE)
    }

    data class ThemePreset(
        val name: String,
        val primary: String,   // 슬로건바 배경
        val sidebar: String,   // 사이드바 배경
        val accent: String,    // 강조색
        val bg: String,        // 앱 배경
        val text: String       // 본문 텍스트
    )

    private val presets = listOf(
        ThemePreset("핑크",  "#F48FB1", "#FCE4EC", "#E91E63", "#FFFFFF", "#880E4F"),
        ThemePreset("파랑",  "#90CAF9", "#E3F2FD", "#1976D2", "#FFFFFF", "#0D47A1"),
        ThemePreset("녹색",  "#A5D6A7", "#E8F5E9", "#388E3C", "#FFFFFF", "#1B5E20"),
        ThemePreset("노랑",  "#FFF59D", "#FFFDE7", "#F9A825", "#FFFFFF", "#E65100"),
        ThemePreset("블랙",  "#424242", "#212121", "#BDBDBD", "#121212", "#FFFFFF"),
        ThemePreset("주황",  "#FF7043", "#FBE9E7", "#E64A19", "#FFFFFF", "#BF360C")
    )

    private val defaultPreset = ThemePreset("기본", "#1976D2", "#F5F5F5", "#FF9800", "#FFFFFF", "#212121")

    private var currentPreset: ThemePreset = defaultPreset

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemePresetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPink.setOnClickListener   { selectPreset(presets[0]) }
        binding.btnBlue.setOnClickListener   { selectPreset(presets[1]) }
        binding.btnGreen.setOnClickListener  { selectPreset(presets[2]) }
        binding.btnYellow.setOnClickListener { selectPreset(presets[3]) }
        binding.btnBlack.setOnClickListener  { selectPreset(presets[4]) }
        binding.btnOrange.setOnClickListener { selectPreset(presets[5]) }

        binding.btnReset.setOnClickListener { selectPreset(defaultPreset) }
        binding.btnApply.setOnClickListener { saveAndFinish(currentPreset) }

        val saved = ThemePreset(
            name = "",
            primary = prefs.getString("theme_primary", defaultPreset.primary)!!,
            sidebar = prefs.getString("theme_sidebar", defaultPreset.sidebar)!!,
            accent  = prefs.getString("theme_accent",  defaultPreset.accent)!!,
            bg      = prefs.getString("theme_bg",      defaultPreset.bg)!!,
            text    = prefs.getString("theme_text",    defaultPreset.text)!!
        )
        currentPreset = saved
        updatePreview(saved)
    }

    private fun selectPreset(preset: ThemePreset) {
        currentPreset = preset
        updatePreview(preset)
    }

    private fun updatePreview(preset: ThemePreset) {
        try {
            val primaryColor = Color.parseColor(preset.primary)
            binding.previewMain.setBackgroundColor(primaryColor)
            binding.previewSidebar.setBackgroundColor(Color.parseColor(preset.sidebar))
            binding.previewAccent.setBackgroundColor(Color.parseColor(preset.accent))

            val onPrimary = if (ThemeHelper.isColorDark(primaryColor)) Color.WHITE else Color.BLACK
            binding.previewText.setTextColor(onPrimary)

            binding.colorSwatches.removeAllViews()
            listOf(preset.primary, preset.sidebar, preset.accent, preset.bg, preset.text).forEach { hex ->
                val swatch = View(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT).apply { weight = 1f }
                    setBackgroundColor(Color.parseColor(hex))
                }
                binding.colorSwatches.addView(swatch)
            }
        } catch (_: Exception) {}
    }

    private fun saveAndFinish(preset: ThemePreset) {
        prefs.edit()
            .putString("theme_primary", preset.primary)
            .putString("theme_sidebar", preset.sidebar)
            .putString("theme_accent",  preset.accent)
            .putString("theme_bg",      preset.bg)
            .putString("theme_text",    preset.text)
            .apply()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
