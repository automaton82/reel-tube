package com.systems.automaton.reeltube.local.subscription.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.TouchCallback
import icepick.Icepick
import icepick.State
import com.systems.automaton.reeltube.R
import com.systems.automaton.reeltube.database.feed.model.FeedGroupEntity
import com.systems.automaton.reeltube.databinding.DialogFeedGroupReorderBinding
import com.systems.automaton.reeltube.local.subscription.dialog.FeedGroupReorderDialogViewModel.DialogEvent.ProcessingEvent
import com.systems.automaton.reeltube.local.subscription.dialog.FeedGroupReorderDialogViewModel.DialogEvent.SuccessEvent
import com.systems.automaton.reeltube.local.subscription.item.FeedGroupReorderItem
import com.systems.automaton.reeltube.util.ThemeHelper
import java.util.Collections
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.map
import kotlin.collections.sortedBy

class FeedGroupReorderDialog : DialogFragment() {
    private var _binding: DialogFeedGroupReorderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FeedGroupReorderDialogViewModel

    @State
    @JvmField
    var groupOrderedIdList = ArrayList<Long>()
    private val groupAdapter = GroupieAdapter()
    private val itemTouchHelper = ItemTouchHelper(getItemTouchCallback())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)

        setStyle(STYLE_NO_TITLE, ThemeHelper.getMinWidthDialogTheme(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_feed_group_reorder, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogFeedGroupReorderBinding.bind(view)

        viewModel = ViewModelProvider(this).get(FeedGroupReorderDialogViewModel::class.java)
        viewModel.groupsLiveData.observe(viewLifecycleOwner, Observer(::handleGroups))
        viewModel.dialogEventLiveData.observe(viewLifecycleOwner) {
            when (it) {
                ProcessingEvent -> disableInput()
                SuccessEvent -> dismiss()
            }
        }

        binding.feedGroupsList.layoutManager = LinearLayoutManager(requireContext())
        binding.feedGroupsList.adapter = groupAdapter
        itemTouchHelper.attachToRecyclerView(binding.feedGroupsList)

        binding.confirmButton.setOnClickListener {
            viewModel.updateOrder(groupOrderedIdList)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    private fun handleGroups(list: List<FeedGroupEntity>) {
        val groupList: List<FeedGroupEntity>

        if (groupOrderedIdList.isEmpty()) {
            groupList = list
            groupOrderedIdList.addAll(groupList.map { it.uid })
        } else {
            groupList = list.sortedBy { groupOrderedIdList.indexOf(it.uid) }
        }

        groupAdapter.update(groupList.map { FeedGroupReorderItem(it, itemTouchHelper) })
    }

    private fun disableInput() {
        _binding?.confirmButton?.isEnabled = false
        isCancelable = false
    }

    private fun getItemTouchCallback(): SimpleCallback {
        return object : TouchCallback() {

            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val sourceIndex = source.bindingAdapterPosition
                val targetIndex = target.bindingAdapterPosition

                groupAdapter.notifyItemMoved(sourceIndex, targetIndex)
                Collections.swap(groupOrderedIdList, sourceIndex, targetIndex)

                return true
            }

            override fun isLongPressDragEnabled(): Boolean = false
            override fun isItemViewSwipeEnabled(): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {}
        }
    }
}
