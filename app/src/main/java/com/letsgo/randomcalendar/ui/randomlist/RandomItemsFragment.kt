package com.letsgo.randomcalendar.ui.randomlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.letsgo.randomcalendar.data.db.entity.Category
import com.letsgo.randomcalendar.data.db.entity.RandomItem
import com.letsgo.randomcalendar.databinding.FragmentRandomItemsBinding
import com.letsgo.randomcalendar.databinding.ItemRandomItemBinding

class RandomItemsFragment : Fragment() {

    private var _binding: FragmentRandomItemsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RandomListViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var itemAdapter: RandomItemAdapter
    private var allCategories: List<Category> = emptyList()
    private var allItems: List<RandomItem> = emptyList()
    private var selectedSmallId: Long? = null
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemAdapter = RandomItemAdapter(
            onDelete = { item ->
                AlertDialog.Builder(requireContext())
                    .setTitle("항목 삭제")
                    .setMessage("'${item.name}'을 삭제합니까?")
                    .setPositiveButton("삭제") { _, _ -> viewModel.deleteItem(item) }
                    .setNegativeButton("취소", null)
                    .show()
            },
            getCategoryLabel = { id -> allCategories.find { it.id == id }?.name ?: "" }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemAdapter

        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            allItems = items
            applyFilter()
        }

        setupSearch()

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            allCategories = categories
            buildCategoryChips(categories)
        }

        setupAddForm()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(SimpleTextWatcher {
            searchQuery = binding.etSearch.text.toString().trim()
            binding.btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            applyFilter()
        })
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            searchQuery = ""
            binding.btnClearSearch.visibility = View.GONE
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (searchQuery.isEmpty()) {
            allItems
        } else {
            allItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        itemAdapter.submitList(filtered)
        binding.tvNoResults.visibility = if (filtered.isEmpty() && searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildCategoryChips(categories: List<Category>) {
        val topCats = categories.filter { it.level == 0 }
        binding.chipGroupTop.removeAllViews()
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupMid.visibility = View.GONE
        binding.chipGroupSmall.visibility = View.GONE
        selectedSmallId = null

        topCats.forEach { top ->
            val chip = Chip(requireContext()).apply {
                text = top.name
                isCheckable = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) showMidChips(top.id, categories)
                    else {
                        binding.chipGroupMid.visibility = View.GONE
                        binding.chipGroupSmall.visibility = View.GONE
                        selectedSmallId = null
                    }
                }
            }
            binding.chipGroupTop.addView(chip)
        }
    }

    private fun showMidChips(topId: Long, categories: List<Category>) {
        val midCats = categories.filter { it.parentId == topId && it.level == 1 }
        binding.chipGroupMid.removeAllViews()
        binding.chipGroupSmall.removeAllViews()
        binding.chipGroupSmall.visibility = View.GONE
        selectedSmallId = null

        if (midCats.isEmpty()) {
            binding.chipGroupMid.visibility = View.GONE
            return
        }
        binding.chipGroupMid.visibility = View.VISIBLE
        midCats.forEach { mid ->
            val chip = Chip(requireContext()).apply {
                text = mid.name
                isCheckable = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) showSmallChips(mid.id, categories)
                    else {
                        binding.chipGroupSmall.visibility = View.GONE
                        selectedSmallId = null
                    }
                }
            }
            binding.chipGroupMid.addView(chip)
        }
    }

    private fun showSmallChips(midId: Long, categories: List<Category>) {
        val smallCats = categories.filter { it.parentId == midId && it.level == 2 }
        binding.chipGroupSmall.removeAllViews()
        selectedSmallId = null

        if (smallCats.isEmpty()) {
            binding.chipGroupSmall.visibility = View.GONE
            return
        }
        binding.chipGroupSmall.visibility = View.VISIBLE
        smallCats.forEach { small ->
            val chip = Chip(requireContext()).apply {
                text = small.name
                isCheckable = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedSmallId = small.id
                    else if (selectedSmallId == small.id) selectedSmallId = null
                }
            }
            binding.chipGroupSmall.addView(chip)
        }
    }

    private fun setupAddForm() {
        binding.chipTimerNormal.setOnCheckedChangeListener { _, checked ->
            binding.layoutNormalTimer.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.chipTimerSet.setOnCheckedChangeListener { _, checked ->
            binding.layoutSetTimer.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val updateEstimate = {
            val work = binding.etWorkSec.text.toString().toIntOrNull() ?: 0
            val rest = binding.etRestSec.text.toString().toIntOrNull() ?: 0
            val sets = binding.etSets.text.toString().toIntOrNull() ?: 0
            val total = (work + rest) * sets
            binding.tvEstimatedTime.text = if (total > 0) "예상 총 ${total / 60}분 ${total % 60}초" else ""
        }
        binding.etWorkSec.addTextChangedListener(SimpleTextWatcher(updateEstimate))
        binding.etRestSec.addTextChangedListener(SimpleTextWatcher(updateEstimate))
        binding.etSets.addTextChangedListener(SimpleTextWatcher(updateEstimate))

        binding.btnAddItem.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            if (name.isBlank()) {
                binding.etItemName.error = "이름을 입력하세요"
                return@setOnClickListener
            }
            val url = binding.etItemUrl.text.toString().trim()
            val memo = binding.etItemMemo.text.toString().trim()
            val timerType = when {
                binding.chipTimerNormal.isChecked -> "NORMAL"
                binding.chipTimerSet.isChecked -> "SET"
                else -> "NONE"
            }
            val goalSec = binding.etGoalMin.text.toString().toIntOrNull()?.let { it * 60 }
            val work = binding.etWorkSec.text.toString().toIntOrNull() ?: 30
            val rest = binding.etRestSec.text.toString().toIntOrNull() ?: 10
            val sets = binding.etSets.text.toString().toIntOrNull() ?: 3

            viewModel.addItem(name, selectedSmallId, url, memo, timerType, goalSec, work, rest, sets)

            binding.etItemName.text?.clear()
            binding.etItemUrl.text?.clear()
            binding.etItemMemo.text?.clear()
            binding.etGoalMin.text?.clear()
            binding.etWorkSec.text?.clear()
            binding.etRestSec.text?.clear()
            binding.etSets.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RandomItemAdapter(
    private val onDelete: (RandomItem) -> Unit,
    private val getCategoryLabel: (Long?) -> String
) : ListAdapter<RandomItem, RandomItemAdapter.ViewHolder>(RandomItemDiff()) {

    private val expandedIds = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemRandomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemRandomItemBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: RandomItem) {
            val isExpanded = item.id in expandedIds
            b.tvItemName.text = item.name
            b.tvCategoryBadge.text = getCategoryLabel(item.categorySmallId).ifBlank { "분류 없음" }
            b.expandedDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
            b.btnItemExpand.setImageResource(
                if (isExpanded) com.letsgo.randomcalendar.R.drawable.ic_chevron_up
                else com.letsgo.randomcalendar.R.drawable.ic_chevron_down
            )

            b.headerRow.setOnClickListener {
                if (item.id in expandedIds) expandedIds.remove(item.id) else expandedIds.add(item.id)
                val pos = currentList.indexOfFirst { it.id == item.id }
                if (pos != -1) notifyItemChanged(pos)
            }

            if (isExpanded) {
                if (item.url.isNotBlank()) { b.tvItemUrl.visibility = View.VISIBLE; b.tvItemUrl.text = item.url } else b.tvItemUrl.visibility = View.GONE
                if (item.memo.isNotBlank()) { b.tvItemMemo.visibility = View.VISIBLE; b.tvItemMemo.text = item.memo } else b.tvItemMemo.visibility = View.GONE
                val timerText = when (item.timerType) {
                    "NORMAL" -> "일반 타이머" + (item.timerGoalSeconds?.let { " (${it / 60}분)" } ?: "")
                    "SET" -> "세트 타이머: ${item.setWorkSeconds}초 운동 / ${item.setRestSeconds}초 휴식 / ${item.setCount}세트"
                    else -> null
                }
                if (timerText != null) { b.tvItemTimer.visibility = View.VISIBLE; b.tvItemTimer.text = timerText } else b.tvItemTimer.visibility = View.GONE
            }

            b.btnItemDelete.setOnClickListener { onDelete(item) }
            b.btnItemEdit.setOnClickListener { /* TODO: inline edit */ }
            b.btnItemExpand.setOnClickListener { b.headerRow.performClick() }
        }
    }

    class RandomItemDiff : DiffUtil.ItemCallback<RandomItem>() {
        override fun areItemsTheSame(a: RandomItem, b: RandomItem) = a.id == b.id
        override fun areContentsTheSame(a: RandomItem, b: RandomItem) = a == b
    }
}

class SimpleTextWatcher(private val block: () -> Unit) : android.text.TextWatcher {
    override fun afterTextChanged(s: android.text.Editable?) = block()
    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
}
