package com.randomcalendar.ui.randomlist

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.randomcalendar.RandomCalendarApp
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

        viewModel // Fragment가 기본 팩토리로 생성 시도하기 전에 ViewModel을 먼저 초기화

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) CategoryTreeFragment() else RandomItemsFragment()
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = if (pos == 0) "분류 관리" else "항목 목록"
        }.attach()
    }
}
