package com.example.g22.TimeSlotList

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.TimeSlotView.DateTimePickerFragment
import com.example.g22.TimeSlotView.TimeSlotVM
import com.example.g22.custom_format
import com.example.g22.toAdvertisementList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp

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
    private lateinit var searchButton: ImageButton
    private lateinit var sortMenu: TextInputLayout
    private lateinit var adapterSortMenu: ArrayAdapter<String>
    private lateinit var applySortButton: ImageButton
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
        searchButton = requireActivity().findViewById(R.id.time_slot_list_search_button)
        sortMenu = requireActivity().findViewById(R.id.timeslot_edit_sort_menu)
        applySortButton = requireActivity().findViewById(R.id.time_slot_list_apply_sort_button)
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
            searchEditText.visibility = View.VISIBLE
            searchButton.visibility = View.VISIBLE
            sortMenu.visibility = View.VISIBLE
            applySortButton.visibility = View.VISIBLE
            searchButton.setOnClickListener {
                listVM.titleSearched = searchEditText.text.toString()
                listVM.searchByTitle(searchEditText.text.toString(), navArguments.skill.toString())
            }

            var items = listOf("Date", "Title", "Location")
            adapterSortMenu = ArrayAdapter(requireContext(), R.layout.skills_list_item, items)
            (sortMenu.editText as? AutoCompleteTextView)?.setAdapter(adapterSortMenu)

            applySortButton.setOnClickListener { applySort() }

        }
        else {
            searchEditText.visibility = View.GONE
            searchButton.visibility = View.GONE
            sortMenu.visibility = View.GONE
            applySortButton.visibility = View.GONE
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
            val contentVisibility = if (it) View.VISIBLE else View.GONE
            val loadingVisibility = if (it) View.GONE else View.VISIBLE

//            contentCl.visibility = contentVisibility
            progressBar.visibility = loadingVisibility
        }

        listVM.tsListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
            if (it.isEmpty()) {
                msgEmptyTimeSlotsTextView.visibility = View.VISIBLE
            } else {
                msgEmptyTimeSlotsTextView.visibility = View.INVISIBLE
            }
        }

        listVM.hasBeenAdded.observe(viewLifecycleOwner) {
            if(it) {
                Snackbar.make(requireView(), "New offer added", Snackbar.LENGTH_LONG).show()
                listVM.hasBeenAdded.value = false
            }
        }

        listVM.hasBeenEdited.observe(viewLifecycleOwner) {
            if(it) {
                Snackbar.make(requireView(), "Offer successfully edited", Snackbar.LENGTH_LONG).show()
                listVM.hasBeenEdited.value = false
            }
        }


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
            else -> super.onOptionsItemSelected(item)
        }
    }

}