package com.randomcalendar.ui.randomlist

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.R
import com.randomcalendar.databinding.ActivityRandomListBinding
import com.randomcalendar.ui.common.ViewModelFactory

class RandomListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRandomListBinding

    private val app get() = application as RandomCalendarApp

    val viewModel: RandomListViewModel by viewModels {
        ViewModelFactory(app.categoryRepository, app.randomItemRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRandomListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CategoryTreeFragment())
                .commit()
        }
    }
}
