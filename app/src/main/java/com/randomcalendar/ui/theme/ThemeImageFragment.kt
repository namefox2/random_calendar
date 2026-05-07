package com.randomcalendar.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.randomcalendar.databinding.FragmentThemeImageBinding

class ThemeImageFragment : Fragment() {

    private var _binding: FragmentThemeImageBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("random_calendar_prefs", Context.MODE_PRIVATE)
    }

    private var extractedColors: List<String> = emptyList()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        binding.ivPreviewImage.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(binding.ivPreviewImage)
        extractColors(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemeImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnApplyImageTheme.setOnClickListener {
            if (extractedColors.size >= 5) {
                prefs.edit()
                    .putString("theme_primary", extractedColors[0])
                    .putString("theme_sidebar", extractedColors[1])
                    .putString("theme_accent", extractedColors[2])
                    .putString("theme_bg", extractedColors[3])
                    .putString("theme_text", extractedColors[4])
                    .apply()
                requireActivity().finish()
            }
        }
    }

    private fun extractColors(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            Palette.from(bitmap).generate { palette ->
                palette ?: return@generate
                val swatches = listOfNotNull(
                    palette.vibrantSwatch,
                    palette.mutedSwatch,
                    palette.darkVibrantSwatch,
                    palette.lightVibrantSwatch,
                    palette.darkMutedSwatch,
                    palette.lightMutedSwatch
                ).take(5)

                if (swatches.isEmpty()) return@generate

                val colorList = swatches.map { swatch ->
                    String.format("#%06X", 0xFFFFFF and swatch.rgb)
                }.toMutableList()
                while (colorList.size < 5) colorList.add(colorList.last())
                extractedColors = colorList.take(5)

                // 스와치 UI 표시
                binding.swatchContainer.removeAllViews()
                swatches.forEach { swatch ->
                    val v = View(requireContext()).apply {
                        val hex = String.format("#%06X", 0xFFFFFF and swatch.rgb)
                        setBackgroundColor(Color.parseColor(hex))
                        layoutParams = android.widget.LinearLayout.LayoutParams(0,
                            resources.getDimensionPixelSize(com.google.android.material.R.dimen.m3_btn_height)).apply {
                            weight = 1f
                        }
                    }
                    binding.swatchContainer.addView(v)
                }

                binding.tvSwatchLabel.visibility = View.VISIBLE
                binding.swatchContainer.visibility = View.VISIBLE
                binding.btnApplyImageTheme.visibility = View.VISIBLE
            }
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
