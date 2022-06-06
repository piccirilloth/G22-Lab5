package com.example.g22.TimeSlotView

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.g22.R
import com.example.g22.TimeSlotList.TimeSlotListVM
import com.example.g22.custom_format
import com.example.g22.model.TimeSlot
import com.example.g22.observeAndShow
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TimeSlotShowFragment : Fragment(R.layout.time_slot_show_frag) {

    // ViewModels
    private val timeslotVM by activityViewModels<TimeSlotVM>()

    // View references
    private lateinit var sv: ScrollView
    private lateinit var titleTV: TextView
    private lateinit var datetimeTV: TextView
    private lateinit var durationTV: TextView
    private lateinit var locationTV: TextView
    private lateinit var descriptionTV: TextView
    private lateinit var skillsCG: ChipGroup
    private lateinit var ownerBtn: Button
    private lateinit var contactButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar

    // Others
    private lateinit var navController: NavController
    private val navArguments: TimeSlotShowFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        navController = findNavController()

        // Find view references
        sv = requireActivity().findViewById(R.id.timeslot_show_sv)
        titleTV = requireActivity().findViewById(R.id.timeslot_show_title_textview)
        datetimeTV = requireActivity().findViewById(R.id.timeslot_show_datetime_textview)
        durationTV = requireActivity().findViewById(R.id.timeslot_show_duration_textview)
        locationTV = requireActivity().findViewById(R.id.timeslot_show_location_textview)
        descriptionTV = requireActivity().findViewById(R.id.timeslot_show_description_textview)
        skillsCG = requireActivity().findViewById(R.id.timeslot_show_skills_chipgroup)
        ownerBtn = requireActivity().findViewById(R.id.timeslot_show_owner_button)
        contactButton = requireActivity().findViewById(R.id.timeslot_show_contact_button)
        progressBar = requireActivity().findViewById(R.id.timeslot_show_progress_bar)
        toolbar = requireActivity().findViewById(R.id.toolbar)

        // Other profile button listener
        ownerBtn.setOnClickListener {
            navController.navigate(
                R.id.action_nav_timeslot_show_to_nav_show_other_profile,
                bundleOf("profileId" to timeslotVM.currTimeSlotLD.value?.owner)
            )
        }

        if (savedInstanceState == null) {
            // Set the current timeslot shown using the received id from the list (if this is the case)
            if (navArguments.timeSlotId != "") {
                timeslotVM.invalidTimeslotLD.value = false
                timeslotVM.setCurrentTimeSlot(navArguments.timeSlotId, true)
            } else {
                navController.popBackStack()
            }
        }

        Firebase.auth.addAuthStateListener {
            contactButton.isEnabled = it.currentUser != null
        }

        // Observe any change to the current timeslot to update the views
        timeslotVM.invalidTimeslotLD.observe(viewLifecycleOwner) {
            if (it == true) {
                timeslotVM.invalidTimeslotLD.value = false
                navController.popBackStack()
            }
        }

        timeslotVM.timeslotLoadedLD.observe(viewLifecycleOwner) {
//            val contentVisibility = if (it) View.VISIBLE else View.GONE
            val loadingVisibility = if (it) View.GONE else View.VISIBLE

//            sv.visibility = contentVisibility
            progressBar.visibility = loadingVisibility
        }

        timeslotVM.currTimeSlotLD.observe(viewLifecycleOwner) {
            val menu = toolbar.menu
            val editIcon = menu.findItem(R.id.edit_item)
            if (navArguments.readOnly == true)
                editIcon?.isVisible = false
            else {
                if (it.proposalsCounter > 0 || it.accepted || editIcon == null)
                    editIcon?.isVisible = false
                else
                    editIcon?.isVisible = true
            }
            bindTimeSlotData(it)
        }

        timeslotVM.ownerLD.observe(viewLifecycleOwner) {
            if (Firebase.auth.currentUser != null &&
                timeslotVM.currTimeSlotLD.value!!.owner == Firebase.auth.currentUser!!.uid
            ) {
                ownerBtn.text = "Myself"
                ownerBtn.isEnabled = false
                contactButton.visibility = View.GONE
            } else {
                ownerBtn.text = it
                ownerBtn.isEnabled = true
                contactButton.visibility = View.VISIBLE
                contactButton.setOnClickListener {
                    navController.navigate(
                        R.id.action_nav_timeslot_show_to_chatFragment,
                        bundleOf(
                            "receiver" to timeslotVM.currTimeSlotLD.value?.owner,
                            "offerId" to timeslotVM.currTimeSlotLD.value?.id,
                            "receiverName" to ownerBtn.text.toString(),
                            "offerTitle" to timeslotVM.currTimeSlotLD.value?.title
                        )
                    )
                }
            }
        }

        // Snackbar handling
        timeslotVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_menu, menu)
        val editIcon = menu.findItem(R.id.edit_item)
        editIcon.setVisible(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_item -> {
                editTimeSlot()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    /**
     * Utilities
     */
    private fun bindTimeSlotData(ts: TimeSlot) {
        titleTV.text = ts.title
        datetimeTV.text = ts.date.custom_format()
        durationTV.text = ts.duration.toShortString()
        locationTV.text = ts.location
        descriptionTV.text = ts.description

        skillsCG.removeAllViews()
        for (skill in ts.skills) {
            val chip = Chip(requireActivity())
            chip.text = skill
            chip.setOnCloseIconClickListener {
                skillsCG.removeView(it)
                timeslotVM.removeSkill((it as Chip).text.toString())
            }
            chip.setChipBackgroundColorResource(R.color.primaryColor)
            chip.setTextColor(resources.getColor(R.color.primaryTextColor))
            skillsCG.addView(chip)
        }
    }

    private fun editTimeSlot() {
        //val action = TimeSlotShowFragmentDirections
        navController.navigate(
            R.id.action_timeSlotShowMyOffersFragment_to_timeSlotEditFragment,
            bundleOf("isAdd" to false, "timeSlotId" to timeslotVM.currTimeSlotLD.value!!.id)
        )
        //.actionTimeSlotShowFragmentToTimeSlotEditFragment(isAdd = false, timeSlotId = timeslotVM.currTimeSlotLD.value!!.id)
        //navController.navigate(action)
    }
}