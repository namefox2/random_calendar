package com.letsgo.randomcalendar.ui.daydetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.letsgo.randomcalendar.RandomCalendarApp
import com.letsgo.randomcalendar.data.db.entity.Category
import com.letsgo.randomcalendar.data.db.entity.RandomItem
import com.letsgo.randomcalendar.databinding.FragmentRandomTabBinding
import com.letsgo.randomcalendar.databinding.ItemPickedBinding
import com.letsgo.randomcalendar.ui.common.ThemeHelper
import com.letsgo.randomcalendar.ui.common.ViewModelFactory
import com.letsgo.randomcalendar.ui.randomlist.RandomListViewModel

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
    private var validSmallIds: Set<Long> = emptySet()
    private val selectedTopIds: MutableSet<Long> = mutableSetOf()
    private val selectedMidIds: MutableSet<Long> = mutableSetOf()
    private val selectedSmallIds: MutableSet<Long> = mutableSetOf()

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
            validSmallIds = cats.filter { it.level == 1 || it.level == 2 }.map { it.id }.toSet()
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
            val hintColor = if (ThemeHelper.isColorDark(c.bgColor)) 0xFFBDBDBD.toInt() else 0xFF9E9E9E.toInt()
            binding.etPickCount.setHintTextColor(hintColor)
            val labelColor = if (ThemeHelper.isColorDark(c.bgColor)) android.graphics.Color.WHITE
                             else android.graphics.Color.parseColor("#757575")
            binding.tvCategoryLabel.setTextColor(labelColor)
            binding.tvCountLabel.setTextColor(labelColor)
        } catch (_: Exception) {}
    }

    private fun buildTopChips(categories: List<Category>) {
        val tops = categories.filter { it.level == 0 }
        binding.chipGroupTop.removeAllViews()
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupMid.visibility = View.GONE
        binding.chipGroupSmall.visibility = View.GONE
        selectedTopIds.clear(); selectedMidIds.clear(); selectedSmallIds.clear()
        updatePath()

        tops.forEach { top ->
            val chip = makeChip(top.name) { _, checked ->
                if (checked) selectedTopIds.add(top.id) else selectedTopIds.remove(top.id)
                selectedMidIds.clear()
                selectedSmallIds.clear()
                rebuildMidChips(selectedTopIds, categories)
                updatePath()
            }
            binding.chipGroupTop.addView(chip)
        }
    }

    private fun rebuildMidChips(topIds: Set<Long>, categories: List<Category>) {
        val mids = categories.filter { it.parentId in topIds && it.level == 1 }
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupSmall.visibility = View.GONE
        selectedMidIds.clear()
        selectedSmallIds.clear()

        if (mids.isEmpty() || topIds.isEmpty()) { binding.chipGroupMid.visibility = View.GONE; return }
        binding.chipGroupMid.visibility = View.VISIBLE
        mids.forEach { mid ->
            val chip = makeChip(mid.name) { _, checked ->
                if (checked) selectedMidIds.add(mid.id) else selectedMidIds.remove(mid.id)
                selectedSmallIds.clear()
                rebuildSmallChips(selectedMidIds, categories)
                updatePath()
            }
            binding.chipGroupMid.addView(chip)
        }
    }

    private fun rebuildSmallChips(midIds: Set<Long>, categories: List<Category>) {
        val smalls = categories.filter { it.parentId in midIds && it.level == 2 }
        binding.chipGroupSmall.removeAllViews()
        selectedSmallIds.clear()
        if (smalls.isEmpty() || midIds.isEmpty()) { binding.chipGroupSmall.visibility = View.GONE; return }
        binding.chipGroupSmall.visibility = View.VISIBLE
        smalls.forEach { small ->
            val chip = makeChip(small.name) { _, checked ->
                if (checked) selectedSmallIds.add(small.id)
                else selectedSmallIds.remove(small.id)
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
                ThemeHelper.applyChip(this, ThemeHelper.load(requireContext()), ThemeHelper.resolveTypeface(context))
            } catch (_: Exception) {}
            setOnCheckedChangeListener { chip, checked -> listener(chip as com.google.android.material.chip.Chip, checked) }
        }
    }

    private fun updatePath() {
        val topNames = selectedTopIds.mapNotNull { id -> allCategories.find { it.id == id }?.name }
        val midNames = selectedMidIds.mapNotNull { id -> allCategories.find { it.id == id }?.name }
        val smallNames = selectedSmallIds.mapNotNull { id -> allCategories.find { it.id == id }?.name }
        val parts = mutableListOf<String>()
        if (topNames.isNotEmpty()) parts.add(topNames.joinToString(", "))
        if (midNames.isNotEmpty()) parts.add(midNames.joinToString(", "))
        if (smallNames.isNotEmpty()) parts.add(smallNames.joinToString(", "))
        if (parts.isNotEmpty()) {
            binding.tvSelectedPath.visibility = View.VISIBLE
            binding.tvSelectedPath.text = parts.joinToString(" > ")
        } else {
            binding.tvSelectedPath.visibility = View.GONE
        }
    }

    private fun doPick() {
        val count = binding.etPickCount.text.toString().toIntOrNull() ?: 3
        val validItems = allItems.filter { it.categorySmallId in validSmallIds }
        val filtered = when {
            selectedSmallIds.isNotEmpty() -> validItems.filter { it.categorySmallId in selectedSmallIds }
            selectedMidIds.isNotEmpty() -> {
                val smallIds = allCategories.filter { it.parentId in selectedMidIds }.map { it.id }.toSet()
                validItems.filter { it.categorySmallId in smallIds || it.categorySmallId in selectedMidIds }
            }
            selectedTopIds.isNotEmpty() -> {
                val midIds = allCategories.filter { it.parentId in selectedTopIds }.map { it.id }.toSet()
                val smallIds = allCategories.filter { it.parentId in midIds }.map { it.id }.toSet()
                validItems.filter { it.categorySmallId in smallIds || it.categorySmallId in midIds }
            }
            else -> validItems
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
        (requireParentFragment() as? DayDetailBottomSheet)
            ?.let { sheet ->
                sheet.view?.findViewById<com.google.android.material.tabs.TabLayout>(
                    com.letsgo.randomcalendar.R.id.tabLayout
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
