package com.example.g22.createReview

import android.graphics.BitmapFactory
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
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.g22.R
import com.example.g22.observeAndShow
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CreateReviewFragment : Fragment(R.layout.create_review_frag) {

    private val navArguments: CreateReviewFragmentArgs by navArgs()
    private val createReviewVM by activityViewModels<CreateReviewVM>()
    private val storage = Firebase.storage("gs://time-banking-9318d.appspot.com").reference

    lateinit var profName: TextView
    lateinit var score: TextView
    lateinit var profilePic: CircleImageView
    lateinit var overallRatingBar: RatingBar

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
        overallRatingBar = view.findViewById(R.id.create_review_frag_overall_rating)

        if(savedInstanceState == null) {
            createReviewVM.observeCurrentReviewInfo(navArguments.revieweeId, navArguments.reviewType)
        }

        lifecycleScope.launch {
            profilePic.setImageBitmap(view.resources.getDrawable(R.drawable.ic_baseline_downloading_24).toBitmap())
            try {
                val glideRes = withContext(Dispatchers.IO) {
                    val uri = storage.child("${navArguments.revieweeId}.jpg").downloadUrl.await()
                    val imageURL = uri.toString()
                    Glide.with(view)
                        .load(imageURL)
                }
                glideRes.into(profilePic)
            } catch(e: Exception) {
                // Pick from default icon
                profilePic.setImageBitmap(BitmapFactory.decodeResource(view.resources, R.drawable.user_icon))
            }
        }

        createReviewVM.currentRevieweeLD.observe(viewLifecycleOwner) {
            profName.text = it
        }

        createReviewVM.currentRevieweeScoreLD.observe(viewLifecycleOwner) {
            score.text = "${it}/5 (${createReviewVM.currentRevieweeCounterReviewsLD.value} reviews)"
            overallRatingBar.rating = it.toFloat()
        }

        // Snackbar handling
        createReviewVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_only_menu, menu)
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