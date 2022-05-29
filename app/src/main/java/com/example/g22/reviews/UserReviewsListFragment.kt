package com.example.g22.reviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R


class UserReviewsListFragment : Fragment(R.layout.user_reviews_list_frag) {
    private val reviewsListVM by activityViewModels<UserReviewsListVM>()

    private lateinit var rv : RecyclerView
    private lateinit var adapter: ReviewAdapter

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

        rv = requireActivity().findViewById(R.id.user_reviews_rv)

        //Recycler View Configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ReviewAdapter(emptyList())
        rv.adapter = adapter

        reviewsListVM.reviewListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
        }

        reviewsListVM.observeReviews()
    }
}