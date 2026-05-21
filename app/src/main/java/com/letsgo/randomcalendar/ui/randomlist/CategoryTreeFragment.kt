package com.letsgo.randomcalendar.ui.randomlist

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.letsgo.randomcalendar.data.db.entity.Category
import com.letsgo.randomcalendar.databinding.FragmentCategoryTreeBinding
import com.letsgo.randomcalendar.databinding.ItemAddCategoryBinding
import com.letsgo.randomcalendar.databinding.ItemCategoryBinding
import kotlinx.coroutines.launch

class CategoryTreeFragment : Fragment() {

    private var _binding: FragmentCategoryTreeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RandomListViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var adapter: CategoryTreeAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var allCategories: List<Category> = emptyList()
    private var searchQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryTreeAdapter(
            onAddCategory = { parentId, level, name -> viewModel.addCategory(name, parentId, level) },
            onAddSmallWithItem = { parentMidId, name -> showAddSmallWithItemDialog(parentMidId, name) },
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> showDeleteConfirm(category) },
            onMoveCategory = { category, newParentId -> viewModel.moveToCategory(category, newParentId) },
            onStartDrag = { vh -> itemTouchHelper.startDrag(vh) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
        setupDragAndDrop()

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            allCategories = categories
            applyFilter()
        }

        setupSearch()
    }

    private fun setupDragAndDrop() {
        var draggedCategory: Category? = null
        var currentDropTargetId: Long? = null

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = false

            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                val pos = vh.adapterPosition
                if (pos < 0 || pos >= adapter.itemCount) return 0
                val row = adapter.getRow(pos)
                return if (row.category?.level == 2)
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                else makeMovementFlags(0, 0)
            }

            override fun onMove(rv: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val pos = target.adapterPosition
                if (pos < 0 || pos >= adapter.itemCount) return false
                val targetCat = adapter.getRow(pos).category
                val newTarget: Long? = when (targetCat?.level) {
                    1 -> targetCat.id
                    2 -> targetCat.parentId
                    else -> null
                }
                if (newTarget != currentDropTargetId) {
                    currentDropTargetId = newTarget
                    adapter.setHighlightedMid(newTarget)
                }
                return false
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) {
                    val pos = vh.adapterPosition
                    if (pos >= 0) draggedCategory = adapter.getRow(pos).category
                    vh.itemView.alpha = 0.7f
                }
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.alpha = 1f
                val dragged = draggedCategory
                val targetId = currentDropTargetId
                if (dragged != null && targetId != null && targetId != dragged.parentId) {
                    viewModel.moveToCategory(dragged, targetId)
                }
                draggedCategory = null
                currentDropTargetId = null
                adapter.setHighlightedMid(null)
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvCategories)
    }

    private fun setupSearch() {
        binding.etSearchCategory.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s.toString().trim()
                binding.btnClearCategorySearch.visibility =
                    if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        binding.btnClearCategorySearch.setOnClickListener {
            binding.etSearchCategory.text?.clear()
            searchQuery = ""
            binding.btnClearCategorySearch.visibility = View.GONE
            applyFilter()
        }
    }

    private fun applyFilter() {
        if (searchQuery.isEmpty()) {
            adapter.submitCategories(allCategories)
        } else {
            val matched = allCategories.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }.toMutableSet()
            matched.toList().forEach { cat ->
                var parentId = cat.parentId
                while (parentId != null) {
                    val parent = allCategories.find { it.id == parentId } ?: break
                    matched.add(parent)
                    parentId = parent.parentId
                }
            }
            adapter.submitCategories(allCategories.filter { it in matched })
        }
    }

    private fun showAddSmallWithItemDialog(parentMidId: Long, prefillName: String) {
        showSmallItemDialog(
            title = "소분류 추가", confirmLabel = "추가",
            prefillName = prefillName, prefillUrl = "",
            prefillTimerType = "NONE", prefillGoalSec = null,
            prefillWork = 0, prefillRest = 0, prefillSets = 0
        ) { name, url, timerType, goalSec, work, rest, sets ->
            viewModel.addCategoryWithItem(name, parentMidId, url, timerType, goalSec, work, rest, sets)
        }
    }

    private fun showEditDialog(category: Category) {
        if (category.level == 2) {
            lifecycleScope.launch {
                val item = viewModel.getItemBySmallCategoryId(category.id)
                showSmallItemDialog(
                    title = "소분류 편집", confirmLabel = "저장",
                    prefillName = category.name, prefillUrl = item?.url ?: "",
                    prefillTimerType = item?.timerType ?: "NONE",
                    prefillGoalSec = item?.timerGoalSeconds,
                    prefillWork = item?.setWorkSeconds ?: 0,
                    prefillRest = item?.setRestSeconds ?: 0,
                    prefillSets = item?.setCount ?: 0
                ) { name, url, timerType, goalSec, work, rest, sets ->
                    viewModel.renameCategory(category, name)
                    item?.let {
                        viewModel.updateItem(it.copy(
                            name = name, url = url, timerType = timerType,
                            timerGoalSeconds = goalSec, setWorkSeconds = work,
                            setRestSeconds = rest, setCount = sets
                        ))
                    }
                }
            }
        } else {
            val et = EditText(requireContext()).apply { setText(category.name); selectAll() }
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
    }

    private fun showSmallItemDialog(
        title: String, confirmLabel: String,
        prefillName: String, prefillUrl: String,
        prefillTimerType: String, prefillGoalSec: Int?,
        prefillWork: Int, prefillRest: Int, prefillSets: Int,
        onConfirm: (name: String, url: String, timerType: String, goalSec: Int?, work: Int, rest: Int, sets: Int) -> Unit
    ) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val pad = (16 * dp).toInt()
        val padSm = (8 * dp).toInt()

        fun editText(hint: String, value: String = "", number: Boolean = false, weight: Float = 0f, center: Boolean = false) =
            EditText(ctx).apply {
                this.hint = hint
                if (value.isNotEmpty()) setText(value)
                setTextColor(ctx.getColor(com.letsgo.randomcalendar.R.color.text_primary))
                setHintTextColor(ctx.getColor(com.letsgo.randomcalendar.R.color.text_secondary))
                inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
                if (center) gravity = android.view.Gravity.CENTER
                if (weight > 0f) layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            }

        val etName = editText("소분류 이름 (필수)", prefillName)
        val etUrl  = editText("URL 링크 (선택)", prefillUrl).also {
            it.inputType = InputType.TYPE_TEXT_VARIATION_URI
        }

        val radioGroup = RadioGroup(ctx).apply { orientation = RadioGroup.HORIZONTAL }
        val rbNone   = RadioButton(ctx).apply { text = "없음"; id = 10 }
        val rbNormal = RadioButton(ctx).apply { text = "일반"; id = 11 }
        val rbSet    = RadioButton(ctx).apply { text = "세트"; id = 12 }
        radioGroup.addView(rbNone); radioGroup.addView(rbNormal); radioGroup.addView(rbSet)
        when (prefillTimerType) { "NORMAL" -> rbNormal.isChecked = true; "SET" -> rbSet.isChecked = true; else -> rbNone.isChecked = true }

        val etGoal = editText("목표 시간(분, 선택)", prefillGoalSec?.let { (it / 60).toString() } ?: "", number = true, weight = 1f)
        val etWork = editText("운동(초)", if (prefillWork > 0) prefillWork.toString() else "", number = true, weight = 1f, center = true).also {
            (it.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = padSm
        }
        val etRest = editText("휴식(초)", if (prefillRest > 0) prefillRest.toString() else "", number = true, weight = 1f, center = true).also {
            (it.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = padSm
        }
        val etSets = editText("세트수", if (prefillSets > 0) prefillSets.toString() else "", number = true, weight = 1f, center = true)

        val layoutNormal = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = if (prefillTimerType == "NORMAL") View.VISIBLE else View.GONE
            setPadding(0, padSm, 0, 0)
            addView(etGoal)
        }
        val layoutSet = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = if (prefillTimerType == "SET") View.VISIBLE else View.GONE
            setPadding(0, padSm, 0, 0)
            addView(etWork); addView(etRest); addView(etSets)
        }
        radioGroup.setOnCheckedChangeListener { _, id ->
            layoutNormal.visibility = if (id == 11) View.VISIBLE else View.GONE
            layoutSet.visibility    = if (id == 12) View.VISIBLE else View.GONE
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad * 2, pad, pad * 2, 0)
            addView(etName); addView(etUrl)
            addView(TextView(ctx).apply {
                text = "타이머"
                setTextColor(ctx.getColor(com.letsgo.randomcalendar.R.color.text_secondary))
                setPadding(0, padSm, 0, padSm / 2)
            })
            addView(radioGroup); addView(layoutNormal); addView(layoutSet)
        }

        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(root)
            .setPositiveButton(confirmLabel) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isBlank()) return@setPositiveButton
                val timerType = when (radioGroup.checkedRadioButtonId) {
                    11 -> "NORMAL"; 12 -> "SET"; else -> "NONE"
                }
                onConfirm(
                    name, etUrl.text.toString().trim(), timerType,
                    etGoal.text.toString().toIntOrNull()?.let { it * 60 },
                    etWork.text.toString().toIntOrNull() ?: 30,
                    etRest.text.toString().toIntOrNull() ?: 10,
                    etSets.text.toString().toIntOrNull() ?: 3
                )
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
    private val onAddCategory: (parentId: Long?, level: Int, name: String) -> Unit,
    private val onAddSmallWithItem: (parentMidId: Long, prefillName: String) -> Unit,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit,
    private val onMoveCategory: (Category, Long) -> Unit = { _, _ -> },
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class TreeRow(
        val category: Category?,
        val parentId: Long?,
        val level: Int,
        val indent: Int
    )

    private val rows = mutableListOf<TreeRow>()
    private val collapsedIds = mutableSetOf<Long>()
    private var storedCategories: List<Category> = emptyList()
    private var highlightedMidId: Long? = null

    fun getRow(position: Int): TreeRow = rows[position]

    fun setHighlightedMid(id: Long?) {
        val prev = highlightedMidId
        highlightedMidId = id
        rows.forEachIndexed { index, row ->
            if (row.category?.id == prev || row.category?.id == id) notifyItemChanged(index)
        }
    }

    fun submitCategories(categories: List<Category>) {
        storedCategories = categories
        rows.clear()
        buildTree(categories, null, 0, 0)
        notifyDataSetChanged()
    }

    private fun rebuild() {
        rows.clear()
        buildTree(storedCategories, null, 0, 0)
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
        rows.add(TreeRow(null, parentId, level, indent))
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position].category != null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> CategoryViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> AddCategoryViewHolder(ItemAddCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        when (holder) {
            is CategoryViewHolder -> holder.bind(row)
            is AddCategoryViewHolder -> holder.bind(row)
        }
    }

    override fun getItemCount() = rows.size

    inner class CategoryViewHolder(private val b: ItemCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        internal fun bind(row: TreeRow) {
            val cat = row.category ?: return
            b.tvName.text = cat.name
            b.tvLevel.text = when (cat.level) { 0 -> "대"; 1 -> "중"; else -> "소" }
            b.vIndent.layoutParams.width = row.indent * 32
            b.vIndent.requestLayout()

            if (cat.level < 2) {
                b.btnToggle.visibility = View.VISIBLE
                b.btnToggle.setImageResource(
                    if (cat.id in collapsedIds) com.letsgo.randomcalendar.R.drawable.ic_chevron_right
                    else com.letsgo.randomcalendar.R.drawable.ic_chevron_down
                )
                b.btnToggle.setOnClickListener {
                    if (cat.id in collapsedIds) collapsedIds.remove(cat.id) else collapsedIds.add(cat.id)
                    rebuild()
                }
            } else {
                b.btnToggle.visibility = View.INVISIBLE
            }

            // 소분류(level 2): 드래그 핸들 표시
            if (cat.level == 2) {
                b.ivDragHandle.visibility = View.VISIBLE
                b.ivDragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag(this)
                    }
                    false
                }
            } else {
                b.ivDragHandle.visibility = View.GONE
                b.ivDragHandle.setOnTouchListener(null)
            }

            // 중분류(level 1): 드롭 타겟일 때 하이라이트
            val isDropTarget = cat.level == 1 && cat.id == highlightedMidId
            b.root.setBackgroundColor(
                if (isDropTarget) Color.argb(50, 25, 118, 210) else Color.TRANSPARENT
            )

            b.btnEdit.setOnClickListener { onEdit(cat) }
            b.btnDelete.setOnClickListener { onDelete(cat) }
            b.editRow.visibility = View.GONE
        }
    }

    inner class AddCategoryViewHolder(private val b: ItemAddCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        internal fun bind(row: TreeRow) {
            b.etNewCategory.visibility = View.VISIBLE
            val hint = when (row.level) {
                0 -> "+ 대분류 이름 입력"
                1 -> "+ 중분류 이름 입력"
                else -> "+ 소분류 이름 입력"
            }
            b.etNewCategory.hint = hint
            b.etNewCategory.text?.clear()
            b.btnAddCategory.text = "추가"

            if (row.level == 2) {
                b.btnAddCategory.setOnClickListener {
                    val name = b.etNewCategory.text.toString().trim()
                    row.parentId?.let { parentId ->
                        onAddSmallWithItem(parentId, name)
                        b.etNewCategory.text?.clear()
                    }
                }
            } else {
                b.btnAddCategory.setOnClickListener {
                    val name = b.etNewCategory.text.toString().trim()
                    if (name.isNotBlank()) {
                        onAddCategory(row.parentId, row.level, name)
                        b.etNewCategory.text?.clear()
                    }
                }
            }
            b.etNewCategory.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    b.btnAddCategory.performClick(); true
                } else false
            }
        }
    }
}
