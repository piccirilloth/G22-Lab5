package com.example.g22.SkillsList

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.ShowProfile.ProfileVM
import com.example.g22.TimeSlotList.SkillAdapter
import com.example.g22.TimeSlotList.TimeSlotListVM

class SkillsListFragment : Fragment(R.layout.skills_list_frag) {
    private val skillsListVM by activityViewModels<SkillsListVM>()
    private val profileVM by activityViewModels<ProfileVM>()
    private val timeslotListVM by activityViewModels<TimeSlotListVM>()
    private lateinit var rv: RecyclerView
    private lateinit var adapter: SkillAdapter
    private lateinit var msgEmptySkillsTextView: TextView
    private lateinit var searchBar: EditText
    private lateinit var searchButton: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timeslotListVM.clearFilters()

        // Find view references
        rv = requireActivity().findViewById(R.id.skills_list_rv)
        msgEmptySkillsTextView = requireActivity().findViewById(R.id.skills_list_empty_ts_message)

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = SkillAdapter(skillsListVM.skillsListLD.value ?: emptyList())
        rv.adapter = adapter

        searchBar = requireActivity().findViewById(R.id.skills_list_search_edit_text)
        searchButton = requireActivity().findViewById(R.id.skills_list_search_button)

        searchButton.setOnClickListener {
            val skill = searchBar.text.toString()
            skillsListVM.searchBySkill(skill.lowercase())
        }

        // Observe any change of the skills list
        skillsListVM.skillsListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
            if (it.size < 1) {
                msgEmptySkillsTextView.visibility = View.VISIBLE
            } else {
                msgEmptySkillsTextView.visibility = View.INVISIBLE
            }
        }
    }
}