package com.randomcalendar.ui.daydetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.randomcalendar.R
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.data.db.entity.Category
import com.randomcalendar.data.db.entity.RandomItem
import com.randomcalendar.databinding.FragmentRandomTabBinding
import com.randomcalendar.databinding.ItemPickedBinding
import com.randomcalendar.ui.common.ThemeHelper
import com.randomcalendar.ui.common.ViewModelFactory
import com.randomcalendar.ui.randomlist.RandomListViewModel

class RandomTabFragment : Fragment() {

    private var _binding: FragmentRandomTabBinding? = null
    private val binding get() = _binding!!

    private val dayViewModel: DayDetailViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val randomViewModel: RandomListViewModel by viewModels(
        ownerProducer = { requireActivity() },
        factoryProducer = {
            val app = requireActivity().application as RandomCalendarApp
            ViewModelFactory(app.categoryRepository, app.randomItemRepository)
        }
    )

    private var allCategories: List<Category> = emptyList()
    private var allItems: List<RandomItem> = emptyList()
    private var selectedTopId: Long? = null
    private var selectedMidId: Long? = null
    private var selectedSmallId: Long? = null

    private val pickedAdapter = PickedItemAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRandomTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPickedItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPickedItems.adapter = pickedAdapter

        randomViewModel.allCategories.observe(viewLifecycleOwner) { cats ->
            allCategories = cats
            buildTopChips(cats)
        }

        randomViewModel.allItems.observe(viewLifecycleOwner) { items ->
            allItems = items
        }

        binding.btnPickRandom.setOnClickListener { doPick() }

        binding.btnAddToTodo.setOnClickListener { addPickedToTodo() }

        applyTheme()
    }

    private fun applyTheme() {
        try {
            val c = ThemeHelper.load(requireContext())
            ThemeHelper.applyButton(binding.btnPickRandom, c)
            ThemeHelper.applyButton(binding.btnAddToTodo, c)
            binding.root.setBackgroundColor(c.bgColor)
            binding.layoutBottomBar.setBackgroundColor(c.bgColor)
            binding.tvSelectedPath.setTextColor(c.textColor)
            binding.tvEmptyPick.setTextColor(c.textColor)
            binding.etPickCount.setTextColor(c.textColor)
            val hintColor = if (isDarkColor(c.bgColor)) 0xFFBDBDBD.toInt() else 0xFF9E9E9E.toInt()
            binding.etPickCount.setHintTextColor(hintColor)
            val labelColor = if (isDarkColor(c.bgColor)) android.graphics.Color.WHITE
                             else android.graphics.Color.parseColor("#757575")
            binding.tvCategoryLabel.setTextColor(labelColor)
            binding.tvCountLabel.setTextColor(labelColor)
        } catch (_: Exception) {}
    }

    private fun isDarkColor(color: Int): Boolean {
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.5
    }

    private fun buildTopChips(categories: List<Category>) {
        val tops = categories.filter { it.level == 0 }
        binding.chipGroupTop.removeAllViews()
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupMid.visibility = View.GONE
        binding.chipGroupSmall.visibility = View.GONE
        selectedTopId = null; selectedMidId = null; selectedSmallId = null
        updatePath()

        tops.forEach { top ->
            val chip = makeChip(top.name) { _, checked ->
                if (checked) {
                    selectedTopId = top.id; selectedMidId = null; selectedSmallId = null
                    buildMidChips(top.id, categories)
                } else if (selectedTopId == top.id) {
                    selectedTopId = null
                    binding.chipGroupMid.visibility = View.GONE
                    binding.chipGroupSmall.visibility = View.GONE
                }
                updatePath()
            }
            binding.chipGroupTop.addView(chip)
        }
    }

    private fun buildMidChips(topId: Long, categories: List<Category>) {
        val mids = categories.filter { it.parentId == topId && it.level == 1 }
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupSmall.visibility = View.GONE
        selectedMidId = null; selectedSmallId = null

        if (mids.isEmpty()) { binding.chipGroupMid.visibility = View.GONE; return }
        binding.chipGroupMid.visibility = View.VISIBLE
        mids.forEach { mid ->
            val chip = makeChip(mid.name) { _, checked ->
                if (checked) {
                    selectedMidId = mid.id; selectedSmallId = null
                    buildSmallChips(mid.id, categories)
                } else if (selectedMidId == mid.id) {
                    selectedMidId = null
                    binding.chipGroupSmall.visibility = View.GONE
                }
                updatePath()
            }
            binding.chipGroupMid.addView(chip)
        }
    }

    private fun buildSmallChips(midId: Long, categories: List<Category>) {
        val smalls = categories.filter { it.parentId == midId && it.level == 2 }
        binding.chipGroupSmall.removeAllViews()
        selectedSmallId = null
        if (smalls.isEmpty()) { binding.chipGroupSmall.visibility = View.GONE; return }
        binding.chipGroupSmall.visibility = View.VISIBLE
        smalls.forEach { small ->
            val chip = makeChip(small.name) { _, checked ->
                if (checked) selectedSmallId = small.id
                else if (selectedSmallId == small.id) selectedSmallId = null
                updatePath()
            }
            binding.chipGroupSmall.addView(chip)
        }
    }

    private fun makeChip(label: String, listener: (com.google.android.material.chip.Chip, Boolean) -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            try {
                val c = ThemeHelper.load(requireContext())
                val chipText = if (isDarkColor(c.bgColor)) android.graphics.Color.WHITE
                               else android.graphics.Color.parseColor("#212121")
                setTextColor(android.content.res.ColorStateList.valueOf(chipText))
                val chipBgColor = if (isDarkColor(c.bgColor)) {
                    android.graphics.Color.argb(255,
                        minOf(android.graphics.Color.red(c.bgColor) + 70, 255),
                        minOf(android.graphics.Color.green(c.bgColor) + 70, 255),
                        minOf(android.graphics.Color.blue(c.bgColor) + 70, 255))
                } else {
                    android.graphics.Color.parseColor("#E0E0E0")
                }
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipBgColor)
                typeface = if (ThemeHelper.isHandwritingFont(context)) {
                    ResourcesCompat.getFont(context, R.font.gaegu) ?: Typeface.DEFAULT
                } else Typeface.DEFAULT
            } catch (_: Exception) {}
            setOnCheckedChangeListener { chip, checked -> listener(chip as com.google.android.material.chip.Chip, checked) }
        }
    }

    private fun updatePath() {
        val parts = mutableListOf<String>()
        selectedTopId?.let { id -> allCategories.find { it.id == id }?.name?.let { parts.add(it) } }
        selectedMidId?.let { id -> allCategories.find { it.id == id }?.name?.let { parts.add(it) } }
        selectedSmallId?.let { id -> allCategories.find { it.id == id }?.name?.let { parts.add(it) } }
        if (parts.isNotEmpty()) {
            binding.tvSelectedPath.visibility = View.VISIBLE
            binding.tvSelectedPath.text = parts.joinToString(" > ")
        } else {
            binding.tvSelectedPath.visibility = View.GONE
        }
    }

    private fun doPick() {
        val count = binding.etPickCount.text.toString().toIntOrNull() ?: 3
        val filtered = when {
            selectedSmallId != null -> allItems.filter { it.categorySmallId == selectedSmallId }
            selectedMidId != null -> {
                val smallIds = allCategories.filter { it.parentId == selectedMidId }.map { it.id }.toSet()
                allItems.filter { it.categorySmallId in smallIds }
            }
            selectedTopId != null -> {
                val midIds = allCategories.filter { it.parentId == selectedTopId }.map { it.id }.toSet()
                val smallIds = allCategories.filter { it.parentId in midIds }.map { it.id }.toSet()
                allItems.filter { it.categorySmallId in smallIds }
            }
            else -> allItems
        }
        val picked = filtered.shuffled().take(count)
        pickedAdapter.submitList(picked)
        if (picked.isEmpty()) {
            binding.rvPickedItems.visibility = View.GONE
            binding.tvEmptyPick.visibility = View.VISIBLE
            binding.tvEmptyPick.text = "해당 분류에 항목이 없습니다\n랜덤리스트 관리에서 항목을 추가하세요"
            binding.btnAddToTodo.visibility = View.GONE
        } else {
            binding.rvPickedItems.visibility = View.VISIBLE
            binding.tvEmptyPick.visibility = View.GONE
            binding.btnAddToTodo.visibility = View.VISIBLE
        }
    }

    private fun addPickedToTodo() {
        val selected = pickedAdapter.currentList.filterIndexed { index, _ ->
            pickedAdapter.checkedStates.getOrElse(index) { true }
        }
        selected.forEach { item ->
            dayViewModel.addTodo(
                name = item.name,
                url = item.url,
                timerType = item.timerType,
                timerGoalSeconds = item.timerGoalSeconds,
                setWorkSeconds = item.setWorkSeconds,
                setRestSeconds = item.setRestSeconds,
                setCount = item.setCount
            )
        }
        // TODO 탭으로 이동
        (requireParentFragment() as? DayDetailBottomSheet)
            ?.let { sheet ->
                sheet.view?.findViewById<com.google.android.material.tabs.TabLayout>(
                    com.randomcalendar.R.id.tabLayout
                )?.getTabAt(0)?.select()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PickedItemAdapter : ListAdapter<RandomItem, PickedItemAdapter.ViewHolder>(PickedDiff()) {

    val checkedStates = mutableListOf<Boolean>()

    override fun submitList(list: List<RandomItem>?) {
        checkedStates.clear()
        list?.forEach { _ -> checkedStates.add(true) }
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPickedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val b: ItemPickedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: RandomItem, position: Int) {
            b.tvPickedName.text = item.name
            b.cbPickedSelect.isChecked = checkedStates.getOrElse(position) { true }
            b.cbPickedSelect.setOnCheckedChangeListener { _, checked ->
                if (position < checkedStates.size) checkedStates[position] = checked
            }
        }
    }

    class PickedDiff : DiffUtil.ItemCallback<RandomItem>() {
        override fun areItemsTheSame(a: RandomItem, b: RandomItem) = a.id == b.id
        override fun areContentsTheSame(a: RandomItem, b: RandomItem) = a == b
    }
}
