package com.example.g22.interestingOffers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
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
    private lateinit var incomingBtn: ImageButton
    private lateinit var outcomingBtn : EditText

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

        rv = requireActivity().findViewById(R.id.interesting_offer_list_rv)
        incomingBtn = requireActivity().findViewById(R.id.interesting_offer_list_incoming_button)
        outcomingBtn = requireActivity().findViewById(R.id.interesting_offer_list_outcoming_button)

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = InterestingOfferList.InterestingOfferAdapter(emptyList())
        rv.adapter = adapter

        intOfferVM.interOfferListLD.observe(viewLifecycleOwner) {
            /*
            The first time:
                - updateList is triggered with emptylist
                - db call is done and live data populated
                - updateList triggered with livedata content
            The second time:
                - updateList is triggered with livedata content (not empty this time)
                - db call is done and live data updated
                - addMessage triggered with livedata last element
            This is the motivation behind the if
             */
            if(it.size > adapter.itemCount) {
                if(adapter.itemCount == 0)
                    adapter.updateList(it)
                else
                    adapter.addConversation(it.last())
                if(adapter.itemCount > 0)
                    rv.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }
    }

}