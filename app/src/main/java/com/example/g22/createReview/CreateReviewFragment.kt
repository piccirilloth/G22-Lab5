package com.example.g22.createReview

import android.media.ImageReader
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.g22.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView

class CreateReviewFragment : Fragment(R.layout.create_review_frag) {

    private val navArguments: CreateReviewFragmentArgs by navArgs()
    private val createReviewVM by activityViewModels<CreateReviewVM>()
    private val storage = Firebase.storage("gs://time-banking-9318d.appspot.com").reference

    lateinit var profName: TextView
    lateinit var score: TextView
    lateinit var profilePic: CircleImageView

    lateinit var descriptionEdit: EditText
    lateinit var rate: RatingBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        profName = view.findViewById(R.id.create_review_frag_profile_name)
        score = view.findViewById(R.id.create_review_frag_profile_score)
        profilePic = view.findViewById(R.id.create_review_frag_profile_picture)
        descriptionEdit = view.findViewById(R.id.create_review_frag_edit_text)
        rate = view.findViewById(R.id.create_review_frag_rating_bar)

        if(savedInstanceState == null) {
            createReviewVM.observeCurrentReviewInfo(navArguments.revieweeId, navArguments.reviewType)
        }

        storage.child("${navArguments.revieweeId}.jpg").downloadUrl.addOnSuccessListener {
            val imageURL = it.toString()
            Glide.with(view)
                .load(imageURL)
                .into(profilePic)
        }

        createReviewVM.currentRevieweeLD.observe(viewLifecycleOwner) {
            profName.text = it
        }

        createReviewVM.currentRevieweeScoreLD.observe(viewLifecycleOwner) {
            score.text = "${it}/5 (${createReviewVM.currentRevieweeCounterReviewsLD.value} reviews)"
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_changes_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_changes_item -> {
                createReviewVM.createReview(navArguments.revieweeId, navArguments.reviewType, navArguments.offerId,
                    rate.rating.toDouble(), descriptionEdit.text.toString(), navArguments.conversationId)
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}