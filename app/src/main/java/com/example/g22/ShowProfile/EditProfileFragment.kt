package com.example.g22.ShowProfile

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.g22.*
import com.example.g22.model.Profile
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.io.File


class EditProfileFragment : Fragment(R.layout.edit_profile_frag) {

    // ViewModels
    val profileVM by activityViewModels<ProfileVM>()

    // View references
    private lateinit var fullnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var nicknameEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var skillsCG: ChipGroup
    private lateinit var descriptionEditText: EditText
    private lateinit var newSkillsEditText: EditText
    private lateinit var addSkillButton: Button
    private lateinit var profileImageImgView: ImageView
    private lateinit var cameraImgButton: ImageButton

    // Others
    private lateinit var navController: NavController

    private val REQUEST_CODE_IMAGE_CAPTURE = 1
    private val REQUEST_CODE_PICK_FROM_GALLERY = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intercept Back button pressed
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBack()
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Find view references
        fullnameEditText = view.findViewById(R.id.edit_profile_fullname_edittext)
        emailEditText = view.findViewById(R.id.edit_profile_email_edittext)
        nicknameEditText = view.findViewById(R.id.edit_profile_nickname_edittext)
        locationEditText = view.findViewById(R.id.edit_profile_location_edittext)
        phoneEditText = view.findViewById(R.id.edit_profile_phone_edittext)
        skillsCG = view.findViewById(R.id.edit_profile_skills_chipgroup)
        descriptionEditText = view.findViewById(R.id.edit_profile_description_edittext)
        newSkillsEditText = view.findViewById(R.id.edit_profile_new_skill_edittext)
        addSkillButton = view.findViewById(R.id.edit_profile_add_skill_button)
        profileImageImgView =
            view.findViewById(R.id.edit_profile_profile_image_imgview)
        cameraImgButton = view.findViewById(R.id.edit_profile_photo_edit_button_imgbtn)

        navController = findNavController()

        addSkillButton.setOnClickListener { addSkill() }
        registerForContextMenu(cameraImgButton)
        cameraImgButton.setOnClickListener {
            requireActivity().openContextMenu(cameraImgButton)
        }

        setScrollableImage()

        if (savedInstanceState == null) {
            profileVM.editProfileLoadedLD.value = false
            profileVM.hasBeenModifiedProfileImageLD.value = false
        }

        if (profileVM.profileLoadedLD.value == false) {
            profileVM.profileLD.observe(viewLifecycleOwner) {
                if (profileVM.profileLoadedLD.value == false && profileVM.editProfileLoadedLD.value == false)
                    updateExternalProfileChange(it)
            }
        }
        else {
            if (profileVM.editProfileLoadedLD.value == false)
                updateExternalProfileChange(profileVM.profileLD.value!!)
        }

        profileVM.profileImageLD.observe(viewLifecycleOwner) {
            // Load image from temporary image
            if(profileVM.hasBeenModifiedProfileImageLD.value!!) {
                updateProfileImage(LOCAL_TMP_PROFILE_PICTURE_PATH)
            }
            // Load image from persistent image
            else {
                updateProfileImage(it)
            }
        }

        // Observe editing image / skills
        profileVM.editSkillsLD.observe(viewLifecycleOwner) {
            if (it != null)
                bindSkills(it)
        }

        profileVM.snackbarMessages.observeAndShow(viewLifecycleOwner, requireView(), lifecycleScope)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Choose an option")
        requireActivity().menuInflater.inflate(R.menu.picture_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.camera_item -> takePicture()
            R.id.gallery_item -> loadImageFromGallery()
        }
        return super.onContextItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Handle bitmap coming from the camera (in a background thread)
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    return@withContext data?.extras?.get("data") as Bitmap?
                }

                if (bitmap != null)
                    setAndSaveTmpPicture(bitmap)
            }

        } else if (requestCode == REQUEST_CODE_PICK_FROM_GALLERY && resultCode == RESULT_OK) {
            // Handle Uri of the picture coming from the gallery
            lifecycleScope.launch {
                // Get bitmap in a background thread
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val uri = data?.data
                        uri?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source =
                                    ImageDecoder.createSource(
                                        requireActivity().contentResolver,
                                        uri
                                    )
                                val bitmap = ImageDecoder.decodeBitmap(source)
                                return@withContext bitmap
                            } else {
                                val bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().contentResolver,
                                    uri
                                )
                                return@withContext bitmap
                            }
                        }
                    } catch (e: Exception) {
                        return@withContext null
                    }
                }

                if (bitmap != null)
                    setAndSaveTmpPicture(bitmap)
            }
        }
    }

    private suspend fun setAndSaveTmpPicture(bitmap: Bitmap) {
        profileImageImgView.setImageBitmap(bitmap)
        profileVM.hasBeenModifiedProfileImageLD.value = true
        saveTmpProfilePicture(bitmap)
    }

    private suspend fun saveTmpProfilePicture(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val appDir = requireActivity().filesDir
            val localFile = File(appDir.path, LOCAL_TMP_PROFILE_PICTURE_PATH)
            val outStream = localFile.outputStream()
            outStream.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.save_changes_menu, menu)
        menu.findItem(R.id.save_changes_item).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.discard_changes_item -> {
                backCancelAlert()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Utilities
     */

    private fun onBack() {
        updateProfile()
    }

    private fun areFieldsValid(): Boolean {
        return true
    }

    private fun updateProfile() {
        if (!areFieldsValid())
            return

        profileVM.saveEditedProfile(
            fullnameEditText.text.toString(),
            emailEditText.text.toString(),
            nicknameEditText.text.toString(),
            locationEditText.text.toString(),
            phoneEditText.text.toString(),
            descriptionEditText.text.toString()
        )

        navController.popBackStack()
    }

    private fun bindProfileData(profile: Profile) {
        fullnameEditText.setText(profile.fullname)
        emailEditText.setText(profile.email)
        nicknameEditText.setText(profile.nickname)
        locationEditText.setText(profile.location)
        phoneEditText.setText(profile.phone)
        descriptionEditText.setText(profile.description)
    }

    private fun bindSkills(skills: List<String>) {
        skillsCG.removeAllViews()
        skills.forEach { s ->
            val chip = Chip(requireActivity())
            chip.text = s
            chip.isCloseIconVisible = true
            chip.setChipBackgroundColorResource(R.color.primaryColor)
            chip.setTextColor(resources.getColor(R.color.primaryTextColor))
            chip.setCloseIconTintResource(R.color.primaryTextColor)

            chip.setOnCloseIconClickListener {
                profileVM.removeSkill((it as Chip).text.toString())
            }

            skillsCG.addView(chip)
        }
    }

    fun addSkill() {
        val newSkill = newSkillsEditText.text.toString()
        if (newSkill == "") {
            newSkillsEditText.setError("Skill cannot be empty!")
            return
        }

        val alreadyExist = skillsCG.children.any { (it as Chip).text == newSkill }
        if (alreadyExist)
            newSkillsEditText.error = "'${newSkill}' skill already exists!"
        else {
            newSkillsEditText.setText("")
            profileVM.addSkill(newSkill)
        }
    }

    private fun backCancelAlert() {
        MaterialAlertDialogBuilder(
            requireActivity(),
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
            .setMessage("Discard changes?")
            .setNeutralButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton("Discard") { dialog, which ->
                dialog.cancel()
//                profileVM.downloadProfileImageFromFirebase(null)
                navController.popBackStack()
            }
            .show()
    }

    private fun updateExternalProfileChange(profile: Profile) {
        bindProfileData(profile)
        profileVM.updateEditingProfileData(profile)
        profileVM.editProfileLoadedLD.value = true
    }

    private fun updateProfileImage(localPath: String) {
        profileImageImgView.setImageBitmap(requireActivity().application.resources.getDrawable(R.drawable.ic_baseline_downloading_24).toBitmap())
        profileImageImgView.loadFromDisk(requireActivity().application, lifecycleScope, localPath)
    }

    private fun takePicture() {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(i, REQUEST_CODE_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {

        }
    }

    private fun loadImageFromGallery() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        startActivityForResult(i, REQUEST_CODE_PICK_FROM_GALLERY)
    }

    fun setScrollableImage() {
        val v1 = requireView().findViewById<RelativeLayout>(R.id.edit_profile_rl)
        val sv = requireView().findViewById<ScrollView>(R.id.edit_profile_sv)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            sv.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    v1.post { v1.layoutParams = LinearLayout.LayoutParams(sv.width, sv.height / 3) }
                    sv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }
}
