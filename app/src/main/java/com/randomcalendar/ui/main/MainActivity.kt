package com.randomcalendar.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.databinding.ActivityMainBinding
import com.randomcalendar.ui.common.ViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val app get() = application as RandomCalendarApp

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(app.todoItemRepository, app.monthMemoRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.refreshMonthData()
    }
}
