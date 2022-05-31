package com.example.g22.reviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R


class UserReviewsListFragment : Fragment(R.layout.user_reviews_list_frag) {
    private val reviewsListVM by activityViewModels<UserReviewsListVM>()

    private val navArguments: UserReviewsListFragmentArgs by navArgs()

    private lateinit var rv : RecyclerView
    private lateinit var adapter: ReviewAdapter
    private lateinit var num_reviews : TextView
    private lateinit var rb : RatingBar
    private lateinit var avg_score : TextView

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
        num_reviews = requireActivity().findViewById(R.id.reviews_number_tv)
        rb = requireActivity().findViewById(R.id.ratingBar_reviews_list)
        avg_score = requireActivity().findViewById(R.id.avg_score_list_tv)

        //Recycler View Configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ReviewAdapter(emptyList())
        rv.adapter = adapter

        reviewsListVM.reviewListLD.observe(viewLifecycleOwner) {
            adapter.updateList(it)
        }
        reviewsListVM.numReviewsLD.observe(viewLifecycleOwner) {
            num_reviews.text = it.toString() + " Reviews"
        }
        reviewsListVM.avgScoreLD.observe(viewLifecycleOwner) {
            avg_score.text = it.toString()
            rb.rating = it
        }

        reviewsListVM.observeReviews(navArguments.revieweeId)
    }
}