package com.example.g22.SkillsList

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.ShowProfile.ProfileVM
import com.example.g22.TimeSlotList.SkillAdapter
import com.example.g22.TimeSlotList.TimeSlotListVM

class SkillsListFragment : Fragment(R.layout.skills_list_frag) {
    private val skillsListVM by activityViewModels<SkillsListVM>()
    private val timeslotListVM by activityViewModels<TimeSlotListVM>()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: SkillAdapter
    private lateinit var msgEmptySkillsTextView: TextView
    private lateinit var searchBar: EditText
    private lateinit var deleteTextButton: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        timeslotListVM.clearFilters()

        // Find view references
        rv = requireActivity().findViewById(R.id.skills_list_rv)
        msgEmptySkillsTextView = requireActivity().findViewById(R.id.skills_list_empty_ts_message)

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = SkillAdapter(skillsListVM.skillsListLD.value ?: emptyList())
        rv.adapter = adapter

        searchBar = requireActivity().findViewById(R.id.skills_list_search_edit_text)
        deleteTextButton = requireActivity().findViewById(R.id.skills_list_delete_text_button)

        deleteTextButton.setOnClickListener {
            searchBar.text.clear()
        }

        //live search
        searchBar.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do Nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                skillsListVM.searchBySkill(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Do Nothing
            }
        })

        // Observe any change of the skills list
        skillsListVM.skillsListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it, lifecycleScope)
            if (it.size < 1) {
                msgEmptySkillsTextView.visibility = View.VISIBLE
            } else {
                msgEmptySkillsTextView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_item -> {
                if(searchBar.isVisible)
                    searchBar.visibility = View.GONE
                else
                    searchBar.visibility = View.VISIBLE
                if(deleteTextButton.isVisible)
                    deleteTextButton.visibility = View.GONE
                else
                    deleteTextButton.visibility = View.VISIBLE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}