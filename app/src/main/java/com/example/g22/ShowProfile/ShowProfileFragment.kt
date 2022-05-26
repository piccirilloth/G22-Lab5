package com.example.g22.ShowProfile

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.g22.R
import com.example.g22.isValidImagePath
import com.example.g22.model.Profile
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import java.io.File

class ShowProfileFragment : Fragment(R.layout.show_profile_frag) {

    // View models
    val profileVM by activityViewModels<ProfileVM>()

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

    // Others
    private lateinit var navController: NavController
    private val navArguments: ShowProfileFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        navController = findNavController()

        // Find view references
        profilePictureImgView = requireActivity().findViewById(R.id.show_profile_profile_image_imgview)
        fullnameTV = requireActivity().findViewById(R.id.show_profile_fullname_textview)
        emailTV = requireActivity().findViewById(R.id.show_profile_email_textview)
        nicknameTV = requireActivity().findViewById(R.id.show_profile_nickname_textview)
        locationTV = requireActivity().findViewById(R.id.show_profile_location_textview)
        phoneTV = requireActivity().findViewById(R.id.show_profile_phone_textview)
        skillsCG = requireActivity().findViewById(R.id.show_profile_skills_chipgroup)
        descriptionTV = requireActivity().findViewById(R.id.show_profile_description_textview)
        toolbar = requireActivity().findViewById(R.id.toolbar)
        creditTV = requireActivity().findViewById(R.id.show_profile_credit_textview)

        setScrollableImage()

        // Observe any change to the user profile (always synchronized with db)
        if (navArguments.profileId == null) {
            profileVM.profileLD.observe(viewLifecycleOwner) {
                bindProfileData(it)
            }
            profileVM.profileImageLD.observe(viewLifecycleOwner) {
                updateProfileImage(it)
            }
        } else {
            profileVM.loadOtherProfile(navArguments.profileId!!)

            profileVM.otherProfileLD.observe(viewLifecycleOwner) {
                bindProfileData(it)
                toolbar.title = it.fullname
            }
            profileVM.otherProfileImageLD.observe(viewLifecycleOwner) {
                updateProfileImage(it)
            }
        }

        // Snackbar handling
        profileVM.snackbarMessageLD.observe(viewLifecycleOwner) {
            if(it != "") {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                profileVM.snackbarMessageLD.value = ""
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
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
        if (!localPath.isValidImagePath()) {
            profilePictureImgView.setImageBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.user_icon)
            )
            return
        }

        // TODO: use coroutine
        val appDir = requireActivity().filesDir
        val localFile = File(appDir.path, localPath)
        val inputStream = localFile.inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        profilePictureImgView.setImageBitmap(bitmap)
    }

    fun setScrollableImage() {
        val v1 = getView()?.findViewById<ImageView>(R.id.show_profile_profile_image_imgview)
        val sv = getView()?.findViewById<ScrollView>(R.id.show_profile_sv)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            sv?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    v1?.post { v1.layoutParams = LinearLayout.LayoutParams(sv.width, sv.height / 3) }
                    sv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

}