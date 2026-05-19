package com.letsgo.randomcalendar.ui.daydetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.letsgo.randomcalendar.RandomCalendarApp
import com.letsgo.randomcalendar.databinding.FragmentDayDetailBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper
import com.letsgo.randomcalendar.ui.common.ViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentDayDetailBinding? = null
    private val binding get() = _binding!!

    val viewModel: DayDetailViewModel by viewModels {
        val app = requireActivity().application as RandomCalendarApp
        val date = LocalDate.parse(requireArguments().getString(ARG_DATE)!!)
        ViewModelFactory(app.todoItemRepository, app.dayMemoRepository, date)
    }

    var onDataChanged: (() -> Unit)? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", java.util.Locale.KOREAN)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDayDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = resources.displayMetrics.heightPixels
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.todos.observe(viewLifecycleOwner) { onDataChanged?.invoke() }

        val pagerAdapter = DayDetailPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "TODO"
                1 -> "랜덤 선택"
                else -> "메모"
            }
        }.attach()

        // Date navigation — update ViewModel directly (no dismiss/reopen)
        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDetailDate.text = date.format(dateFormatter)
        }
        binding.btnPrevDay.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value!!.minusDays(1))
        }
        binding.btnNextDay.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value!!.plusDays(1))
        }

        try {
            val c = ThemeHelper.load(requireContext())
            binding.root.setBackgroundColor(c.bgColor)
            binding.tvDetailDate.setTextColor(c.textColor)
            binding.btnPrevDay.setColorFilter(c.textColor)
            binding.btnNextDay.setColorFilter(c.textColor)
            ThemeHelper.applyTabLayout(binding.tabLayout, c)
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DayDetailBottomSheet"
        private const val ARG_DATE = "date"

        fun newInstance(date: LocalDate): DayDetailBottomSheet {
            return DayDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATE, date.toString())
                }
            }
        }
    }
}
