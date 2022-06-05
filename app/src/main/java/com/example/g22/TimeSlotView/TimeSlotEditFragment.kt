package com.example.g22.TimeSlotView

import android.opengl.Visibility
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.g22.R
import com.example.g22.ShowProfile.ProfileVM
import com.example.g22.TimeSlotList.TimeSlotListVM
import com.example.g22.custom_format
import com.example.g22.model.TimeSlot
import com.example.g22.utils.Duration
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TimeSlotEditFragment: Fragment(R.layout.time_slot_edit_frag) {

    // ViewModels
    private val timeslotVM by activityViewModels<TimeSlotVM>()
    private val profileVM by activityViewModels<ProfileVM>()
    private val timeslotListVM by activityViewModels<TimeSlotListVM>()

    // View references
    private lateinit var sv: ScrollView
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var datetimeTV: TextView
    private lateinit var durationHoursEditText: EditText
    private lateinit var durationMinutesEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var skillsMenu: TextInputLayout
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var progressBar: ProgressBar

    // Others
    private lateinit var navController: NavController
    private val navArguments: TimeSlotEditFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intercept Back button (system back button) pressed
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Find view references
        sv = requireActivity().findViewById(R.id.timeslot_edit_sv)
        titleEditText = requireActivity().findViewById(R.id.timeslot_edit_title_edittext)
        datetimeTV = requireActivity().findViewById(R.id.timeslot_edit_datetime_textview)
        durationHoursEditText = requireActivity().findViewById(R.id.timeslot_edit_duration_hour)
        durationMinutesEditText = requireActivity().findViewById(R.id.timeslot_edit_duration_minute)
        locationEditText = requireActivity().findViewById(R.id.timeslot_edit_location_edittext)
        descriptionEditText = requireActivity().findViewById(R.id.timeslot_edit_description_edittext)
        skillsMenu = requireActivity().findViewById(R.id.timeslot_edit_skills_menu)
        skillsChipGroup = requireActivity().findViewById(R.id.timeslot_edit_skills_chipgroup)
        progressBar = requireActivity().findViewById(R.id.timeslot_edit_progress_bar)

        navController = findNavController()

        skillsMenu.editText!!.doOnTextChanged { text, start, before, count ->
            if (text.toString() != "") {
                timeslotVM.addSkill(text.toString())
                skillsMenu.editText!!.setText("")
                skillsMenu.error = ""
            }
        }

        datetimeTV.setOnClickListener { openDateTimePicker() }

        // Initialization
        if (savedInstanceState == null) {
            // Set the current timeslot shown (either empty or coming from the server)
            if (navArguments.isAdd) {
                timeslotVM.setCurrentTimeSlotEmpty()
                bindTimeSlotData(timeslotVM.currTimeSlotLD.value!!)
                progressBar.visibility = View.GONE
            } else {
                if (navArguments.timeSlotId != "") {
                    timeslotVM.setCurrentTimeSlot(navArguments.timeSlotId, false)
                } else {
                    navController.popBackStack()
                }
            }
        }

        // Observers
        if (!navArguments.isAdd) {
            timeslotVM.timeslotLoadedLD.observe(viewLifecycleOwner) {
                val contentVisibility = if (it) View.VISIBLE else View.GONE
                val loadingVisibility = if (it) View.GONE else View.VISIBLE

//                sv.visibility = contentVisibility
                progressBar.visibility = loadingVisibility
            }
        }

        timeslotVM.skillsLD.observe(viewLifecycleOwner) {
            skillsChipGroup.removeAllViews()
            for (skill in it) {
                val chip = Chip(requireActivity())
                chip.text = skill
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener { chipView ->
                    skillsChipGroup.removeView(chipView)
                    timeslotVM.removeSkill((chipView as Chip).text.toString())
                }
                chip.setChipBackgroundColorResource(R.color.primaryColor)
                chip.setTextColor(resources.getColor(R.color.primaryTextColor))
                chip.setCloseIconTintResource(R.color.primaryTextColor)
                skillsChipGroup.addView(chip)
            }
            val items = profileVM.profileLD.value!!.skills.minus(it)
            adapter = ArrayAdapter(requireContext(), R.layout.skills_list_item, items)
            (skillsMenu.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        }

        if (!navArguments.isAdd) {
            timeslotVM.currTimeSlotLD.observe(viewLifecycleOwner) {
                if (timeslotVM.timeslotLoadedLD.value == false) {
                    bindTimeSlotData(it)
                }
            }
        }

        timeslotVM.dateTimeLD.observe(viewLifecycleOwner) {
            datetimeTV.text = it.custom_format()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.save_changes_menu, menu)
        if (!navArguments.isAdd)
            menu.findItem(R.id.save_changes_item).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_changes_item -> {
                assert(navArguments.isAdd)
                saveNewTimeSlot()
                true
            }
            R.id.discard_changes_item -> {
                backCancelAlert()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Utilities
     */
    private fun openDateTimePicker() {
        val newFragment = DateTimePickerFragment(true)
        newFragment.show(parentFragmentManager, "datePicker")
    }

    private fun onBack()  {
        if(navArguments.isAdd) {
            backCancelAlert()
        }
        else
            updateExistingTimeSlot()
    }

    private fun backCancelAlert() {
        MaterialAlertDialogBuilder(requireActivity(),
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
            .setMessage("Discard changes?")
            .setNeutralButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Discard") { dialog, which ->
                dialog.cancel()
                navController.popBackStack()
            }
            .show()
    }

    private fun areFieldsValid(): Boolean {
        var err = false

        if(titleEditText.text.toString() == "") {
            titleEditText.error = "Title is empty!"
            err = true
        }
        if(datetimeTV.text.toString() == "") {
            datetimeTV.error = "Date is empty!"
            err = true
        }

        if (durationHoursEditText.text.toString() != "" && durationHoursEditText.text.toString().toInt() == 0 &&
            durationMinutesEditText.text.toString() != "" && durationMinutesEditText.text.toString().toInt() == 0) {
            durationHoursEditText.error = "Duration cannot be zero!"
            durationMinutesEditText.error = "Duration cannot be zero!"
            err = true
        } else {

            if (durationHoursEditText.text.toString() == "") {
                durationHoursEditText.error = "Hours cannot be empty"
                err = true
            } else if (durationHoursEditText.text.toString().toInt() < 0) {
                durationHoursEditText.error = "Hours cannot be a negative number!"
                err = true
            }

            if (durationMinutesEditText.text.toString() == "") {
                durationMinutesEditText.error = "Minutes cannot be empty!"
                err = true
            } else if (durationMinutesEditText.text.toString()
                    .toInt() < 0 || durationMinutesEditText.text.toString().toInt() > 59
            ) {
                durationMinutesEditText.error = "Minutes must be a positive number lower than 60!"
                err = true
            }
        }

        if(locationEditText.text.toString() == "") {
            locationEditText.error = "Location is empty!"
            err = true
        }
        if (descriptionEditText.text.toString() == "") {
            descriptionEditText.error = "Description is empty!"
            err = true
        }
        if (timeslotVM.skillsLD.value?.isEmpty() != false) {
            skillsMenu.error = "There must be at least one skill!"
            err = true
        }

        return !err
    }

    private fun updateExistingTimeSlot() {
        if(!areFieldsValid())
            return

        val hours = durationHoursEditText.text.toString().toInt()
        val minutes = durationMinutesEditText.text.toString().toInt() + hours * 60

        timeslotVM.saveEditedTimeSlot(titleEditText.text.toString(),
                                        minutes,
                                        locationEditText.text.toString(),
                                        descriptionEditText.text.toString())

        // TODO: remove this - we are asynchronously updating the db and we must notify the snackbar asynchronously when the edit is completed successfully
        timeslotListVM.hasBeenEdited.value = true

        navController.popBackStack()
    }

    private fun saveNewTimeSlot() {
        if(!areFieldsValid())
            return

        val hours = durationHoursEditText.text.toString().toInt()
        val minutes = durationMinutesEditText.text.toString().toInt() + hours * 60

        TimeSlot()
        val newTS = TimeSlot(titleEditText.text.toString(),
            descriptionEditText.text.toString(),
            timeslotVM.dateTimeLD.value!!,
            Duration(minutes),
            locationEditText.text.toString(),
            "${Firebase.auth.currentUser?.uid}",
            timeslotVM.skillsLD.value ?: emptyList(),
            false,
            0
        )
        timeslotListVM.addTimeslot(newTS)
        timeslotListVM.hasBeenAdded.value = true

        findNavController().popBackStack()
    }

    private fun bindTimeSlotData(ts: TimeSlot) {
        titleEditText.setText(ts.title)

        var (days, hours, mins) = ts.duration.toUnits()
        hours += days * 24
        durationHoursEditText.setText(hours.toString())
        durationMinutesEditText.setText(mins.toString())

        locationEditText.setText(ts.location)
        descriptionEditText.setText(ts.description)
    }
}