package com.letsgo.randomcalendar.ui.randomlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.letsgo.randomcalendar.RandomCalendarApp
import com.letsgo.randomcalendar.R
import com.letsgo.randomcalendar.databinding.ActivityRandomListBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper
import com.letsgo.randomcalendar.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

class RandomListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRandomListBinding

    private val app get() = application as RandomCalendarApp

    val viewModel: RandomListViewModel by viewModels {
        ViewModelFactory(app.categoryRepository, app.randomItemRepository)
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val json = viewModel.exportToJson()
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(this@RandomListActivity, "내보내기 완료!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@RandomListActivity, "내보내기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        AlertDialog.Builder(this)
            .setTitle("가져오기")
            .setMessage("선택한 파일의 항목을 기존 데이터에 추가합니다. 계속하시겠습니까?")
            .setPositiveButton("가져오기") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val json = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                            ?: return@launch
                        val result = viewModel.importFromJson(json)
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@RandomListActivity,
                                "가져오기 완료! ${result.getOrDefault(0)}개 항목 추가됨",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(this@RandomListActivity, "파일 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RandomListActivity, "가져오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

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
        binding = ActivityRandomListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fragment이 Activity의 ViewModelStore에서 ViewModel을 가져오므로
        // Fragment 생성 전에 반드시 먼저 초기화해야 함
        viewModel

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnExport.setOnClickListener {
            exportLauncher.launch("random_calendar_export.json")
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CategoryTreeFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val c = ThemeHelper.load(this)
            ThemeHelper.applyToolbar(binding.toolbar, c)
            ThemeHelper.applyButton(binding.btnExport, c)
            ThemeHelper.applyButton(binding.btnImport, c)
        } catch (_: Exception) {}
    }
}
