package com.example.g22.interestingOffers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.createReview.CreateReviewVM
import com.example.g22.observeAndShow
import com.google.android.material.tabs.TabLayout

class InterestingOfferListFragment : Fragment(R.layout.fragment_interesting_offer_list) {
    private val intOfferVM by activityViewModels<InterestingOfferListVM>()
    private val reviewVM by activityViewModels<CreateReviewVM>()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: InterestingOfferList.InterestingOfferAdapter
    private lateinit var tabLayout: TabLayout

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
        tabLayout = view.findViewById(R.id.interesting_offer_list_tab_layout)

        if (savedInstanceState == null) {
            intOfferVM.observeRequests(
                false,
                findNavController().currentDestination!!.id == R.id.nav_accepted_offers
            )
        }

        if(findNavController().currentDestination!!.id == R.id.nav_accepted_offers) {
            tabLayout.getTabAt(0)!!.text = "Bought"
            tabLayout.getTabAt(1)!!.text = "Sold"
        } else {
            tabLayout.getTabAt(0)!!.text = "To buy"
            tabLayout.getTabAt(1)!!.text = "To sell"
        }

        tabLayout.selectTab(if (intOfferVM.isIncoming.value == false) tabLayout.getTabAt(0) else tabLayout.getTabAt(1))

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val toBuy = tab!!.text == "To buy" || tab.text == "Bought"
                intOfferVM.isIncoming.value = !toBuy
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
        })

        intOfferVM.isIncoming.observe(viewLifecycleOwner) {
            intOfferVM.observeRequests(it, findNavController().currentDestination!!.id == R.id.nav_accepted_offers)
        }

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = InterestingOfferList.InterestingOfferAdapter(emptyList())
        rv.adapter = adapter


        intOfferVM.interOfferListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
        }

        reviewVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)
    }
}