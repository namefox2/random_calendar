package com.randomcalendar.ui.daydetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.databinding.FragmentDayDetailBinding
import com.randomcalendar.ui.common.ThemeHelper
import com.randomcalendar.ui.common.ViewModelFactory
import java.time.LocalDate

class DayDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentDayDetailBinding? = null
    private val binding get() = _binding!!

    val viewModel: DayDetailViewModel by viewModels {
        val app = requireActivity().application as RandomCalendarApp
        val date = LocalDate.parse(requireArguments().getString(ARG_DATE)!!)
        ViewModelFactory(app.todoItemRepository, app.dayMemoRepository, date)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDayDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    var onDataChanged: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.todos.observe(viewLifecycleOwner) { onDataChanged?.invoke() }

        dialog?.setOnShowListener {
            val bottomSheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val pagerAdapter = DayDetailPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "TODO"
                1 -> "랜덤 선택"
                else -> "메모"
            }
        }.attach()

        try {
            val c = ThemeHelper.load(requireContext())
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
