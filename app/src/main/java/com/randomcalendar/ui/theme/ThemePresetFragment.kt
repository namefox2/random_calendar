package com.randomcalendar.ui.theme

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.randomcalendar.databinding.FragmentThemePresetBinding

class ThemePresetFragment : Fragment() {

    private var _binding: FragmentThemePresetBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("random_calendar_prefs", Context.MODE_PRIVATE)
    }

    data class ThemePreset(
        val name: String,
        val primary: String,
        val sidebar: String,
        val accent: String,
        val bg: String,
        val text: String
    )

    private val presets = listOf(
        ThemePreset("미드나잇", "#1A237E", "#283593", "#FFD54F", "#121212", "#FFFFFF"),
        ThemePreset("오션", "#006064", "#00838F", "#4DD0E1", "#E0F7FA", "#004D40"),
        ThemePreset("포레스트", "#1B5E20", "#2E7D32", "#A5D6A7", "#F1F8E9", "#1B5E20"),
        ThemePreset("로즈", "#880E4F", "#AD1457", "#F48FB1", "#FCE4EC", "#4A0026")
    )

    private var currentPreset: ThemePreset = presets[0]

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemePresetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMidnight.setOnClickListener { applyPreset(presets[0]) }
        binding.btnOcean.setOnClickListener { applyPreset(presets[1]) }
        binding.btnForest.setOnClickListener { applyPreset(presets[2]) }
        binding.btnRose.setOnClickListener { applyPreset(presets[3]) }

        binding.btnReset.setOnClickListener {
            applyPreset(ThemePreset("기본", "#1976D2", "#F5F5F5", "#FF9800", "#FFFFFF", "#212121"))
        }

        binding.btnApply.setOnClickListener { saveTheme(currentPreset) }

        // 현재 저장된 테마로 미리보기 초기화
        val saved = ThemePreset(
            name = "",
            primary = prefs.getString("theme_primary", "#1976D2")!!,
            sidebar = prefs.getString("theme_sidebar", "#F5F5F5")!!,
            accent = prefs.getString("theme_accent", "#FF9800")!!,
            bg = prefs.getString("theme_bg", "#FFFFFF")!!,
            text = prefs.getString("theme_text", "#212121")!!
        )
        updatePreview(saved)
        currentPreset = saved
    }

    private fun applyPreset(preset: ThemePreset) {
        currentPreset = preset
        updatePreview(preset)
    }

    private fun updatePreview(preset: ThemePreset) {
        try {
            binding.previewSidebar.setBackgroundColor(Color.parseColor(preset.sidebar))
            binding.previewMain.setBackgroundColor(Color.parseColor(preset.primary))
            binding.previewAccent.setBackgroundColor(Color.parseColor(preset.accent))
            binding.previewText.setTextColor(Color.parseColor(preset.text))

            // 스와치 표시
            binding.colorSwatches.removeAllViews()
            listOf(preset.primary, preset.sidebar, preset.accent, preset.bg, preset.text).forEach { hex ->
                val swatch = View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
                        .also { (it as android.widget.LinearLayout.LayoutParams).weight = 1f }
                    setBackgroundColor(Color.parseColor(hex))
                }
                binding.colorSwatches.addView(swatch)
            }
        } catch (_: Exception) {}
    }

    private fun saveTheme(preset: ThemePreset) {
        prefs.edit()
            .putString("theme_primary", preset.primary)
            .putString("theme_sidebar", preset.sidebar)
            .putString("theme_accent", preset.accent)
            .putString("theme_bg", preset.bg)
            .putString("theme_text", preset.text)
            .apply()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
