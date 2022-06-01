package com.example.g22.interestingOffers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.TimeSlotList.MessageAdapter
import com.example.g22.chat.MessagesListVM

class InterestingOfferListFragment : Fragment(R.layout.fragment_interesting_offer_list) {
    private val intOfferVM by activityViewModels<InterestingOfferListVM>()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: InterestingOfferList.InterestingOfferAdapter
    private lateinit var incomingBtn: Button
    private lateinit var outcomingBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.interesting_offer_list_rv)
        incomingBtn = view.findViewById(R.id.interesting_offer_list_incoming_button)
        outcomingBtn = view.findViewById(R.id.interesting_offer_list_outcoming_button)
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)

        if (savedInstanceState == null) {

        }

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = InterestingOfferList.InterestingOfferAdapter(emptyList())
        rv.adapter = adapter

        intOfferVM.isIncoming.observe(viewLifecycleOwner) {
            if(it == true) {
                toolbar.title = "Incoming"
                incomingBtn.setBackgroundColor(resources.getColor(R.color.primaryDarkColor))
                outcomingBtn.setBackgroundColor(resources.getColor(R.color.primaryColor))
            } else {
                toolbar.title = "Outcoming"
                outcomingBtn.setBackgroundColor(resources.getColor(R.color.primaryDarkColor))
                incomingBtn.setBackgroundColor(resources.getColor(R.color.primaryColor))
            }
            intOfferVM.observeRequests(it, findNavController().currentDestination!!.id == R.id.nav_accepted_offers)
        }

        incomingBtn.setOnClickListener {
            if(intOfferVM.isIncoming.value == false) {
                intOfferVM.isStatusChanged.value = true
                intOfferVM.isIncoming.value = true
            }
        }

        outcomingBtn.setOnClickListener {
            if(intOfferVM.isIncoming.value == true) {
                intOfferVM.isStatusChanged.value = true
                intOfferVM.isIncoming.value = false
            }
        }

        intOfferVM.interOfferListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
        }
    }

}