package com.randomcalendar.ui.daydetail

import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.randomcalendar.databinding.FragmentDayMemoBinding
import com.randomcalendar.ui.common.ThemeHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayMemoFragment : Fragment() {

    private var _binding: FragmentDayMemoBinding? = null
    private val binding get() = _binding!!

    private val dayViewModel: DayDetailViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val photoPaths = mutableListOf<String>()
    private var pendingCameraPath: String? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingCameraPath ?: return@registerForActivityResult
        if (success) {
            photoPaths.add(path)
            addPhotoThumb(path)
        } else {
            File(path).delete()
        }
        pendingCameraPath = null
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val copied = copyToInternal(uri) ?: return@registerForActivityResult
        photoPaths.add(copied)
        addPhotoThumb(copied)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDayMemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dayViewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date.toString()
            binding.etDayMemo.tag = null  // allow memo to reload when date changes
        }

        dayViewModel.dayMemo.observe(viewLifecycleOwner) { memo ->
            if (binding.etDayMemo.tag == null) {
                binding.etDayMemo.setText(memo?.content ?: "")
                binding.etDayMemo.tag = "loaded"
                photoPaths.clear()
                binding.photoContainer.removeAllViews()
            }
            val storedPaths = memo?.photoPaths?.split(",")
                ?.filter { it.isNotBlank() } ?: emptyList()
            if (photoPaths.isEmpty() && storedPaths.isNotEmpty()) {
                photoPaths.addAll(storedPaths)
                storedPaths.forEach { addPhotoThumb(it) }
            }
        }

        binding.btnCamera.setOnClickListener {
            val hasPerm = ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) launchCamera()
            else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }

        binding.btnSaveMemo.setOnClickListener {
            val content = binding.etDayMemo.text.toString()
            val pathsStr = photoPaths.joinToString(",")
            dayViewModel.saveMemoContent(content, pathsStr)
            Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()
        }

        try {
            val c = ThemeHelper.load(requireContext())
            ThemeHelper.applyButton(binding.btnSaveMemo, c)
            ThemeHelper.applyOutlinedButton(binding.btnCamera, c)
            ThemeHelper.applyOutlinedButton(binding.btnGallery, c)
            binding.root.setBackgroundColor(c.bgColor)
            binding.tvDate.setTextColor(c.textColor)
            binding.tvPhotoLabel.setTextColor(c.textColor)
            binding.etDayMemo.setTextColor(c.textColor)
            val hintColor = if (ThemeHelper.isColorDark(c.bgColor)) 0xFFBDBDBD.toInt() else 0xFF9E9E9E.toInt()
            binding.etDayMemo.setHintTextColor(hintColor)
        } catch (_: Exception) {}
    }

    private fun launchCamera() {
        try {
            val dir = (requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: requireContext().filesDir).also { it.mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "memo_$ts.jpg")
            pendingCameraPath = file.absolutePath
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "카메라를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToInternal(uri: Uri): String? {
        return try {
            val dir = (requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: requireContext().filesDir).also { it.mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dest = File(dir, "memo_$ts.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun addPhotoThumb(path: String) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val size = (84 * dp).toInt()
        val margin = (8 * dp).toInt()
        val btnSize = (24 * dp).toInt()

        val frame = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply { marginEnd = margin }
        }

        val iv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this).load(File(path)).into(iv)
        iv.setOnClickListener { showFullscreen(path) }

        val del = ImageButton(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = (2 * dp).toInt()
                marginEnd = (2 * dp).toInt()
            }
            setImageResource(com.randomcalendar.R.drawable.ic_close)
            setBackgroundColor(android.graphics.Color.parseColor("#AA000000"))
            setPadding((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())
        }
        del.setOnClickListener {
            photoPaths.remove(path)
            binding.photoContainer.removeView(frame)
        }

        frame.addView(iv)
        frame.addView(del)
        binding.photoContainer.addView(frame)
    }

    private fun showFullscreen(path: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val iv = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        Glide.with(this).load(File(path)).into(iv)
        iv.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(iv)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
