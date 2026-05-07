package com.randomcalendar.ui.randomlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.data.db.entity.Category
import com.randomcalendar.databinding.FragmentCategoryTreeBinding
import com.randomcalendar.databinding.ItemAddCategoryBinding
import com.randomcalendar.databinding.ItemCategoryBinding
import com.randomcalendar.ui.common.ViewModelFactory

class CategoryTreeFragment : Fragment() {

    private var _binding: FragmentCategoryTreeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RandomListViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var adapter: CategoryTreeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryTreeAdapter(
            onAdd = { parentId, level -> showAddDialog(parentId, level) },
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> showDeleteConfirm(category) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitCategories(categories)
        }
    }

    private fun showAddDialog(parentId: Long?, level: Int) {
        val levelLabel = when (level) { 0 -> "대분류"; 1 -> "중분류"; else -> "소분류" }
        val et = EditText(requireContext()).apply {
            hint = "$levelLabel 이름 입력"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("$levelLabel 추가")
            .setView(et)
            .setPositiveButton("추가") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotBlank()) viewModel.addCategory(name, parentId, level)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditDialog(category: Category) {
        val et = EditText(requireContext()).apply {
            setText(category.name)
            selectAll()
        }
        AlertDialog.Builder(requireContext())
            .setTitle("분류 이름 편집")
            .setView(et)
            .setPositiveButton("저장") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotBlank()) viewModel.renameCategory(category, name)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteConfirm(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("분류 삭제")
            .setMessage("'${category.name}' 및 하위 분류/항목을 모두 삭제합니다.\n계속하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteCategory(category) }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CategoryTreeAdapter(
    private val onAdd: (parentId: Long?, level: Int) -> Unit,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class TreeRow(
        val category: Category?,  // null = "추가" 행
        val parentId: Long?,
        val level: Int,
        val indent: Int
    )

    private val rows = mutableListOf<TreeRow>()
    private val collapsedIds = mutableSetOf<Long>()

    fun submitCategories(categories: List<Category>) {
        rows.clear()
        buildTree(categories, null, 0, 0)
        notifyDataSetChanged()
    }

    private fun buildTree(all: List<Category>, parentId: Long?, level: Int, indent: Int) {
        val children = all.filter { it.parentId == parentId && it.level == level }
        children.forEach { cat ->
            rows.add(TreeRow(cat, parentId, level, indent))
            if (cat.id !in collapsedIds && level < 2) {
                buildTree(all, cat.id, level + 1, indent + 1)
            }
        }
        // 추가 버튼 행
        rows.add(TreeRow(null, parentId, level, indent))
    }

    override fun getItemViewType(position: Int) = if (rows[position].category == null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == 0) {
            CategoryViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            AddCategoryViewHolder(ItemAddCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        if (holder is CategoryViewHolder) holder.bind(row)
        else if (holder is AddCategoryViewHolder) holder.bind(row)
    }

    override fun getItemCount() = rows.size

    inner class CategoryViewHolder(private val b: ItemCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        private fun bind(row: TreeRow) {
            val cat = row.category ?: return
            b.tvName.text = cat.name
            b.tvLevel.text = when (cat.level) { 0 -> "대"; 1 -> "중"; else -> "소" }
            b.vIndent.layoutParams.width = row.indent * 32
            b.vIndent.requestLayout()

            if (cat.level < 2) {
                b.btnToggle.visibility = View.VISIBLE
                b.btnToggle.setImageResource(
                    if (cat.id in collapsedIds) com.randomcalendar.R.drawable.ic_chevron_right
                    else com.randomcalendar.R.drawable.ic_chevron_down
                )
                b.btnToggle.setOnClickListener {
                    if (cat.id in collapsedIds) collapsedIds.remove(cat.id) else collapsedIds.add(cat.id)
                    notifyDataSetChanged()
                }
            } else {
                b.btnToggle.visibility = View.INVISIBLE
            }

            b.btnEdit.setOnClickListener { onEdit(cat) }
            b.btnDelete.setOnClickListener { onDelete(cat) }
            b.editRow.visibility = View.GONE
        }
    }

    inner class AddCategoryViewHolder(private val b: ItemAddCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        private fun bind(row: TreeRow) {
            val hint = when (row.level) { 0 -> "+ 대분류 추가"; 1 -> "+ 중분류 추가"; else -> "+ 소분류 추가" }
            b.etNewCategory.hint = hint
            b.etNewCategory.text?.clear()
            b.btnAddCategory.text = "추가"
            b.btnAddCategory.setOnClickListener {
                val name = b.etNewCategory.text.toString().trim()
                if (name.isNotBlank()) {
                    onAdd(row.parentId, row.level)
                    b.etNewCategory.text?.clear()
                }
            }
            b.etNewCategory.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    b.btnAddCategory.performClick()
                    true
                } else false
            }
        }
    }
}
