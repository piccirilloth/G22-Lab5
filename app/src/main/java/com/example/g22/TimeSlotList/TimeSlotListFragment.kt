package com.example.g22.TimeSlotList

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.TimeSlotView.DateTimePickerFragment
import com.example.g22.TimeSlotView.TimeSlotVM
import com.example.g22.observeAndShow
import com.example.g22.toAdvertisementList
import com.google.android.material.textfield.TextInputLayout

class TimeSlotListFragment: Fragment(R.layout.time_slot_list_frag) {

    // ViewModels
    private val listVM by activityViewModels<TimeSlotListVM>()
    private val timeslotVM by activityViewModels<TimeSlotVM>()

    // View references
    private lateinit var contentCl: ConstraintLayout
    private lateinit var rv: RecyclerView
    private lateinit var adapter: AdvertisementAdapter
    private lateinit var addFab: View
    private lateinit var msgEmptyTimeSlotsTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageButton
    private lateinit var sortMenu: TextInputLayout
    private lateinit var adapterSortMenu: ArrayAdapter<String>
    private lateinit var progressBar: ProgressBar

    // Others
    private val navArguments: TimeSlotListFragmentArgs by navArgs()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Find view references
        contentCl = requireActivity().findViewById(R.id.timeslot_list_content_cl)
        addFab = requireActivity().findViewById(R.id.timeslot_list_add_btn_fab)
        rv = requireActivity().findViewById(R.id.timeslot_list_rv)
        msgEmptyTimeSlotsTextView = requireActivity().findViewById(R.id.timeslot_list_empty_ts_message)
        searchEditText = requireActivity().findViewById(R.id.time_slot_list_search_edit_text)
        clearButton = requireActivity().findViewById(R.id.time_slot_list_clear_button)
        sortMenu = requireActivity().findViewById(R.id.timeslot_edit_sort_menu)
        progressBar = requireActivity().findViewById(R.id.timeslot_list_progress_bar)

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = AdvertisementAdapter(listVM.tsListLD.value?.toAdvertisementList() ?: emptyList(), navArguments.skill)
        rv.adapter = adapter

        // Layout customization
        if (navArguments.skill != null) {
            addFab.visibility = View.INVISIBLE
            listVM.observeSkillOffers(navArguments.skill!!)
            val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
            toolbar.title = navArguments.skill
            searchEditText.visibility = View.GONE
            clearButton.visibility = View.GONE
            sortMenu.visibility = View.VISIBLE
            clearButton.setOnClickListener {
                searchEditText.text.clear()
            }

            searchEditText.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // Do Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    listVM.titleSearched = searchEditText.text.toString()
                    listVM.searchByTitle(searchEditText.text.toString(), navArguments.skill.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    // Do Nothing
                }
            })

            sortMenu.editText!!.doOnTextChanged { text, start, before, count ->
                listVM.sortParam = text.toString()
                applySort()
            }

            var items = listOf("Date", "Title", "Location")
            adapterSortMenu = ArrayAdapter(requireContext(), R.layout.skills_list_item, items)
            (sortMenu.editText as? AutoCompleteTextView)?.setAdapter(adapterSortMenu)
        }
        else {
            searchEditText.visibility = View.GONE
            clearButton.visibility = View.GONE
            sortMenu.visibility = View.GONE
            addFab.visibility = View.VISIBLE
            listVM.observeMyOffers()
        }

        // Initialization
        if (savedInstanceState == null) {
            if (navArguments.skill != null) {
                listVM.observeSkillOffers(navArguments.skill!!)
            } else {
                listVM.observeMyOffers()
            }
        }

        // Listeners
        addFab.setOnClickListener {
            addNewTimeSlot()
        }

        // Observe any change of the timeslot list
        listVM.tsListLoadedLD.observe(viewLifecycleOwner) {
//            val contentVisibility = if (it) View.VISIBLE else View.GONE
            val loadingVisibility = if (it) View.GONE else View.VISIBLE
            if(it && navArguments.skill != null)
                listVM.restoreFilters(navArguments.skill!!)

//            contentCl.visibility = contentVisibility
            progressBar.visibility = loadingVisibility
        }

        listVM.tsListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it, lifecycleScope)
            if (it.isEmpty()) {
                msgEmptyTimeSlotsTextView.visibility = View.VISIBLE
            } else {
                msgEmptyTimeSlotsTextView.visibility = View.INVISIBLE
            }
        }

        // Snackbar handling
        listVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)
        timeslotVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)

    }

    /**
     * Utilities
     */
    private fun addNewTimeSlot() {
        findNavController().navigate(
            R.id.action_timeSlotListFragment_to_timeSlotEditFragment2,
            bundleOf("isAdd" to true, "timeSlotId" to "")
        )
    }

    private fun applySort() {
        var sortFilter = ""

        if(sortMenu.editText != null)
            sortFilter = sortMenu.editText!!.text.toString()

        listVM.sort(sortFilter)
        listVM.sortParam = sortFilter
    }

    private fun openDatePicker() {
        val newFragment = DateTimePickerFragment(false)
        newFragment.show(parentFragmentManager, "datePicker")
    }

    private fun showSelectFiltersPopup() {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.filter_dialog, null)
        val locationEditText = dialogLayout.findViewById<TextInputLayout>(R.id.filter_dialog_location)
        val ownerEditText = dialogLayout.findViewById<TextInputLayout>(R.id.filter_dialog_owner)
        val dateTextView = dialogLayout.findViewById<TextView>(R.id.filter_dialog_date)

        // TODO: ???
        timeslotVM.dateTimeLD.observe(viewLifecycleOwner) {
            dateTextView.text = "${it.year+1900}-${it.month+1}-${it.date}"
        }

        dateTextView.text = ""

        dateTextView.setOnClickListener { openDatePicker() }

        with(builder) {
            setTitle("Insert the filters you prefer")
            setPositiveButton("OK") { dialog, which ->
                val owner = ownerEditText.editText?.text.toString()
                val location = locationEditText.editText?.text.toString()
                val date = dateTextView.text.toString()

                listVM.applyFilters(owner, location, date, navArguments.skill!!)
                listVM.dateFilter = date
                listVM.ownerFilter = owner
                listVM.locationFilter = location
                dialog.cancel()
            }
            setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            setView(dialogLayout)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (navArguments.skill != null) {
            inflater.inflate(R.menu.filter_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_item -> {
                showSelectFiltersPopup()
                true
            }
            R.id.search_item -> {
                if(searchEditText.isVisible)
                    searchEditText.visibility = View.GONE
                else
                    searchEditText.visibility = View.VISIBLE
                if(clearButton.isVisible)
                    clearButton.visibility = View.GONE
                else
                    clearButton.visibility = View.VISIBLE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}