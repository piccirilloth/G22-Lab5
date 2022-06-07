package com.example.g22.ShowProfile

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.g22.R
import com.example.g22.isValidImagePath
import com.example.g22.loadFromDisk
import com.example.g22.model.Profile
import com.example.g22.observeAndShow
import com.example.g22.reviews.UserReviewsListVM
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import java.io.File

class ShowProfileFragment : Fragment(R.layout.show_profile_frag) {

    // View models
    val profileVM by activityViewModels<ProfileVM>()
    val reviewsVM by activityViewModels<UserReviewsListVM>()

    // View references
    private lateinit var profilePictureImgView: ImageView
    private lateinit var fullnameTV: TextView
    private lateinit var emailTV: TextView
    private lateinit var nicknameTV: TextView
    private lateinit var locationTV: TextView
    private lateinit var phoneTV: TextView
    private lateinit var skillsCG: ChipGroup
    private lateinit var descriptionTV: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var creditTV: TextView
    private lateinit var showOffererReviewsButton: ImageButton
    private lateinit var showRequestorReviewsButton: ImageButton
    private lateinit var requestorReviewsAvg: TextView
    private lateinit var offererReviewsAvg: TextView
    private lateinit var requestorNumReviews: TextView
    private lateinit var offererNumReviews: TextView

    // Others
    private lateinit var navController: NavController
    private val navArguments: ShowProfileFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        navController = findNavController()

        // Find view references
        profilePictureImgView =
            view.findViewById(R.id.show_profile_profile_image_imgview)
        fullnameTV = view.findViewById(R.id.show_profile_fullname_textview)
        emailTV = view.findViewById(R.id.show_profile_email_textview)
        nicknameTV = view.findViewById(R.id.show_profile_nickname_textview)
        locationTV = view.findViewById(R.id.show_profile_location_textview)
        phoneTV = view.findViewById(R.id.show_profile_phone_textview)
        skillsCG = view.findViewById(R.id.show_profile_skills_chipgroup)
        descriptionTV = view.findViewById(R.id.show_profile_description_textview)
        toolbar = requireActivity().findViewById(R.id.toolbar)
        creditTV = view.findViewById(R.id.show_profile_credit_textview)
        showOffererReviewsButton =
            view.findViewById(R.id.show_profile_show_offerer_reviews_button)
        showRequestorReviewsButton =
            view.findViewById(R.id.show_profile_show_requestor_reviews_button)
        requestorReviewsAvg =
            view.findViewById(R.id.show_profile_requestor_reviews_avg_textview)
        requestorNumReviews =
            view.findViewById(R.id.show_profile_requestor_num_reviews_textview)
        offererReviewsAvg =
            view.findViewById(R.id.show_profile_offerer_reviews_avg_textview)
        offererNumReviews =
            view.findViewById(R.id.show_profile_offerer_num_reviews_textview)


        setScrollableImage()

        // Observe any change to the user profile (always synchronized with db)
        if (navArguments.profileId == null) {
            profileVM.profileLD.observe(viewLifecycleOwner) {
                bindProfileData(it)
                reviewsVM.observeReviews(it.id)
            }
            profileVM.profileImageLD.observe(viewLifecycleOwner) {
                updateProfileImage(it)
            }
        } else {
            profileVM.loadOtherProfile(navArguments.profileId!!)

            profileVM.otherProfileLD.observe(viewLifecycleOwner) {
                bindProfileData(it)
                toolbar.title = it.fullname
                reviewsVM.observeReviews(it.id)
            }
            profileVM.otherProfileImageLD.observe(viewLifecycleOwner) {
                updateProfileImage(it)
            }
        }

        // Snackbar handling
        profileVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)

        /*if (navController.currentDestination?.id == R.id.nav_show_profile)
            reviewsVM.observeReviews(profileVM.profileLD.value!!.id)
        else
            reviewsVM.observeReviews(profileVM.otherProfileLD.value!!.id)*/

        reviewsVM.numOffererReviewsLD.observe(viewLifecycleOwner) {
            offererNumReviews.text = " - $it Reviews"
        }

        reviewsVM.avgOffererScoreLD.observe(viewLifecycleOwner) {
            offererReviewsAvg.text = "$it/5"
        }

        reviewsVM.numRequestorReviewsLD.observe(viewLifecycleOwner) {
            requestorNumReviews.text = " - $it Reviews"
        }

        reviewsVM.avgRequestorScoreLD.observe(viewLifecycleOwner) {
            requestorReviewsAvg.text = "$it/5"
        }

        showOffererReviewsButton.setOnClickListener {
            if (navController.currentDestination?.id == R.id.nav_show_profile) {
                navController.navigate(
                    R.id.action_nav_show_profile_to_nav_reviews_list,
                    bundleOf(
                        "revieweeId" to profileVM.profileLD.value?.id,
                        "reviewType" to "offerer"
                    )
                )
            } else {
                navController.navigate(
                    R.id.action_nav_show_other_profile_to_nav_reviews_list,
                    bundleOf(
                        "revieweeId" to profileVM.otherProfileLD.value?.id,
                        "reviewType" to "offerer"
                    )
                )
            }
        }

        showRequestorReviewsButton.setOnClickListener {
            if (navController.currentDestination?.id == R.id.nav_show_profile) {
                navController.navigate(
                    R.id.action_nav_show_profile_to_nav_reviews_list,
                    bundleOf(
                        "revieweeId" to profileVM.profileLD.value?.id,
                        "reviewType" to "requestor"
                    )
                )
            } else {
                navController.navigate(
                    R.id.action_nav_show_other_profile_to_nav_reviews_list,
                    bundleOf(
                        "revieweeId" to profileVM.otherProfileLD.value?.id,
                        "reviewType" to "requestor"
                    )
                )
            }
        }
    }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.edit_menu, menu)
            menu.findItem(R.id.edit_item).isVisible = navArguments.profileId == null
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.edit_item -> {
                    navController.navigate(R.id.action_showProfileFragment_to_editProfileFragment)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }


        /**
         * Utilities
         */
        fun bindProfileData(profile: Profile) {
            fullnameTV.text = profile.fullname
            emailTV.text = profile.email
            nicknameTV.text = profile.nickname
            locationTV.text = profile.location
            phoneTV.text = profile.phone
            descriptionTV.text = profile.description

            val hours: Int = profile.credit / 60
            val minutes: Int = profile.credit % 60
            creditTV.text = "${hours}h ${minutes}m"

            skillsCG.removeAllViews()
            profile.skills.forEach { s ->
                val chip = Chip(requireActivity())
                chip.text = s
                chip.setChipBackgroundColorResource(R.color.primaryColor)
                chip.setTextColor(resources.getColor(R.color.primaryTextColor))
                chip.setCloseIconTintResource(R.color.primaryTextColor)
                skillsCG.addView(chip)
            }
        }

    private fun updateProfileImage(localPath: String) {
        profilePictureImgView.setImageBitmap(requireActivity().application.resources.getDrawable(R.drawable.ic_baseline_downloading_24).toBitmap())
        profilePictureImgView.loadFromDisk(requireActivity().application, lifecycleScope, localPath)
    }

    fun setScrollableImage() {
        val v1 = requireView().findViewById<ImageView>(R.id.show_profile_profile_image_imgview)
        val sv = requireView().findViewById<ScrollView>(R.id.show_profile_sv)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            sv.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    v1.post { v1.layoutParams = LinearLayout.LayoutParams(sv.width, sv.height / 3) }
                    sv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

}