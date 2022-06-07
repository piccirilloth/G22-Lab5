package com.example.g22.reviews

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.g22.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class UserReviewsListFragment : Fragment(R.layout.user_reviews_list_frag) {
    private val reviewsListVM by activityViewModels<UserReviewsListVM>()
    val storage = Firebase.storage("gs://time-banking-9318d.appspot.com").reference

    private val navArguments: UserReviewsListFragmentArgs by navArgs()

    private lateinit var rv : RecyclerView
    private lateinit var adapter: ReviewAdapter
    private lateinit var num_reviews : TextView
    private lateinit var rb : RatingBar
    private lateinit var avg_score : TextView
    private lateinit var revieweeName : TextView
    private lateinit var revieweePic : ImageView

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

        rv = view.findViewById(R.id.user_reviews_rv)
        num_reviews = view.findViewById(R.id.reviews_number_tv)
        rb = view.findViewById(R.id.ratingBar_reviews_list)
        avg_score = view.findViewById(R.id.avg_score_list_tv)
        revieweeName = view.findViewById(R.id.reviews_fullname)
        revieweePic = view.findViewById(R.id.reviews_prof_pic)

        //Recycler View Configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ReviewAdapter(requireActivity(), lifecycleScope, emptyList())
        rv.adapter = adapter

        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        toolbar.title = if(navArguments.reviewType == "offerer")
            "Reviews as an Offerer"
        else
            "Reviews as a Requestor"

        reviewsListVM.revieweeNameLD.observe(viewLifecycleOwner) {
            revieweeName.text = it
        }

        if(navArguments.reviewType == "offerer") {

            reviewsListVM.reviewsOffererListLD.observe(viewLifecycleOwner) {
                adapter.updateList(it, lifecycleScope)
            }

            reviewsListVM.numOffererReviewsLD.observe(viewLifecycleOwner) {
                num_reviews.text = it.toString() + " Reviews"
            }
            reviewsListVM.avgOffererScoreLD.observe(viewLifecycleOwner) {
                avg_score.text = it.toString()
                rb.rating = it
            }
        }

        if(navArguments.reviewType == "requestor") {
            reviewsListVM.reviewsRequestorListLD.observe(viewLifecycleOwner) {
                adapter.updateList(it, lifecycleScope)
            }

            reviewsListVM.numRequestorReviewsLD.observe(viewLifecycleOwner) {
                num_reviews.text = it.toString() + " Reviews"
            }
            reviewsListVM.avgRequestorScoreLD.observe(viewLifecycleOwner) {
                avg_score.text = it.toString()
                rb.rating = it
            }
        }

        reviewsListVM.observeReviews(navArguments.revieweeId)
        reviewsListVM.observeRevieweeName(navArguments.revieweeId)

        lifecycleScope.launch {
            revieweePic.setImageBitmap(view.resources.getDrawable(R.drawable.ic_baseline_downloading_24).toBitmap())
            try {
                val glideRes = withContext(Dispatchers.IO) {
                    val uri = storage.child("${navArguments.revieweeId}.jpg").downloadUrl.await()
                    val imageURL = uri.toString()
                    Glide.with(requireActivity())
                        .load(imageURL)
                }
                glideRes.into(revieweePic)
            } catch(e: Exception) {
                // Pick from default icon
                revieweePic.setImageBitmap(BitmapFactory.decodeResource(view.resources, R.drawable.user_icon))
            }
        }
    }
}