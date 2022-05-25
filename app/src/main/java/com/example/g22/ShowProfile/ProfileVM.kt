package com.example.g22.ShowProfile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.LOCAL_PERSISTENT_PROFILE_PICTURE_PATH
import com.example.g22.LOCAL_TMP_PROFILE_PICTURE_PATH
import com.example.g22.NullImagePath
import com.example.g22.model.Profile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.*

class ProfileVM(application: Application) : AndroidViewModel(application) {

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage("gs://time-banking-9318d.appspot.com").reference

    private var profileListenerRegistration: ListenerRegistration? = null
    private var otherProfileListenerRegistration: ListenerRegistration? = null

    // Live data
    // PERSISTENT: Profile data (always kept synchronized with the database snapshot)
    private val _profileLD: MutableLiveData<Profile> = MutableLiveData<Profile>()
    // PERSISTENT: Profile picture (always kept synchronized with the persistent state)
    private val _profileImageLD: MutableLiveData<String> = MutableLiveData<String>(String.NullImagePath())

    private val _otherProfileLD = MutableLiveData<Profile>()
    private val _otherProfileImageLD: MutableLiveData<String> = MutableLiveData(String.NullImagePath())


    // EDIT-MODE: For temporary skills during edit mode
    private val _editSkillsLD = MutableLiveData<List<String>>()

    val profileLoadedLD = MutableLiveData<Boolean>(false)

    val editProfileLoadedLD = MutableLiveData<Boolean>(false)

    /**
     * Exposed functionalities to activities / fragments
     */

    val profileLD: LiveData<Profile> = _profileLD
    val profileImageLD: LiveData<String> = _profileImageLD
    val editSkillsLD: LiveData<List<String>> = _editSkillsLD
    val otherProfileLD: LiveData<Profile> = _otherProfileLD
    val otherProfileImageLD: LiveData<String> = _otherProfileImageLD

    // For snackbar handling
    var snackbarMessageLD: MutableLiveData<String> = MutableLiveData("")
    val hasBeenModifiedProfileImageLD: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)


    init {
        Firebase.auth.addAuthStateListener {
            // It gets called when subscribed and whenever a login/logout happens
            observeRemoteUser()
            if (it.currentUser != null)
                downloadProfileImageFromFirebase(null)
        }
    }

    fun saveEditedProfile(fullname: String, email: String, nickname: String, location: String,
                          phone: String, description: String) {
        if (Firebase.auth.currentUser == null)
            return

        // Prepare profile object
        val tmp = _profileLD.value?.copy() ?: return
        tmp.fullname = fullname
        tmp.email = email
        tmp.nickname = nickname
        tmp.phone = phone
        tmp.location = location
        tmp.skills = _editSkillsLD.value?.map { it.lowercase() } ?: emptyList()
        tmp.description = description

        db.collection("users")
            .document(Firebase.auth.currentUser?.uid ?: return)
            .set(tmp)
            .addOnSuccessListener {
                uploadNewProfileImageToFirebase()
                snackbarMessageLD.value = snackbarMessageLD.value + "Profile has been updated!"
            }.addOnFailureListener {
                snackbarMessageLD.value = snackbarMessageLD.value + "Error while saving profile to database!"
            }
    }

    fun addSkill(skill: String) {
        _editSkillsLD.value = _editSkillsLD.value?.plus(skill)
    }

    fun removeSkill(skill: String) {
        _editSkillsLD.value = _editSkillsLD.value?.minus(skill)
    }

    fun updateEditingProfileData(profile: Profile) {
        _editSkillsLD.value = profile.skills
    }

    private fun observeRemoteUser() {
        if (Firebase.auth.currentUser != null) {
            profileListenerRegistration?.remove()

            val uid = Firebase.auth.currentUser!!.uid
            profileListenerRegistration = db.collection("users")
                .document(uid)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        snackbarMessageLD.value = snackbarMessageLD.value +
                                "Error while retrieving user from database!"
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        // Profile found
                        onFirebaseProfileUpdate(value)
                    } else {
                        // Create profile
                        createProfileFromAuthUser()
                    }
                    profileLoadedLD.value = true
                }
        } else {
            _profileLD.value = Profile()
            _profileImageLD.value = String.NullImagePath()
        }
    }

    private fun uploadNewProfileImageToFirebase() {
        if (hasBeenModifiedProfileImageLD.value == false)
            return

        // Firebase storage file reference
        val storageImgRef = storage.child("${Firebase.auth.currentUser!!.uid}.jpg")

        // Upload temporary edited image to Firebase
        val appDir = getApplication<Application>().filesDir
        val localFile = File(appDir.path, LOCAL_TMP_PROFILE_PICTURE_PATH)
        val localFileUri = Uri.fromFile(localFile)
        storageImgRef.putFile(localFileUri)
            .addOnSuccessListener {
                // If the upload has succeed, copy the temporary image in the persistent one
                localFile.copyTo(File(appDir.path, LOCAL_PERSISTENT_PROFILE_PICTURE_PATH), true)
                _profileImageLD.value = LOCAL_PERSISTENT_PROFILE_PICTURE_PATH
            }
            .addOnFailureListener {
                snackbarMessageLD.value = snackbarMessageLD.value +
                        "\nError while uploading profile image!"
            }
    }

    fun downloadProfileImageFromFirebase(profileId: String?) {
        val localPath = if (profileId == null) {
            LOCAL_PERSISTENT_PROFILE_PICTURE_PATH
        } else {
            "$profileId.jpg"
        }

        // Reference to Firebase storage profile picture
//        val storagePathRef = storage.child("${Firebase.auth.currentUser?.uid}.jpg")
        val storagePathRef = if (profileId == null) {
            storage.child("${Firebase.auth.currentUser?.uid}.jpg")
        } else {
            storage.child(localPath)
        }

        // Create or overwrite profile image on disk
        val appDir = getApplication<Application>().filesDir
        val localFile = File(appDir.path, localPath)
        localFile.createNewFile()

        // Download
        storagePathRef.getFile(localFile)
            .addOnSuccessListener {
                if (it.error == null) {
                    if (profileId == null)
                        _profileImageLD.value = localPath
                    else
                        _otherProfileImageLD.value = localPath
                } else {
                    if (profileId == null)
                        _profileImageLD.value = String.NullImagePath()
                    else
                        _otherProfileImageLD.value = String.NullImagePath()
                }
            }
            .addOnFailureListener {
                if (profileId == null)
                    _profileImageLD.value = String.NullImagePath()
                else
                    _otherProfileImageLD.value = String.NullImagePath()
            }
    }

    private fun onFirebaseProfileUpdate(snapshot: DocumentSnapshot) {
        val profile = snapshot.toObject(Profile::class.java)
        _profileLD.value = profile!!
    }

    private fun createProfileFromAuthUser() {
        val currUser = Firebase.auth.currentUser!!
        val uid = currUser.uid

        // Set user document
        db.collection("users")
            .document(uid)
            .set(Profile.FromFirebaseUser(currUser))
            .addOnFailureListener {
                snackbarMessageLD.value = snackbarMessageLD.value +
                        "Unable to create profile from Google account!"
            }

        // Load google profile image to firebase storage
//        val storageImgRef = storage.child("${uid}.jpg")
//        val urlStream = URL(currUser.photoUrl.toString()).openStream()
//        storageImgRef.putStream(urlStream)
//            .addOnSuccessListener {
//                // upload ok
//            }
//            .addOnFailureListener {
//                // TODO: snackback problem with uploading of picture
//            }
    }

    fun loadOtherProfile(profileId: String) {
        _otherProfileLD.value = Profile()
        _otherProfileImageLD.value = String.NullImagePath()

        otherProfileListenerRegistration?.remove()

        otherProfileListenerRegistration = db.collection("users")
            .document(profileId)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (value != null && value.exists()) {
                    _otherProfileLD.value = value.toObject(Profile::class.java)
                }
            }

        downloadProfileImageFromFirebase(profileId)
    }


    /**
     * ViewModel callbacks
     */
    override fun onCleared() {
        super.onCleared()

        // Clear all snapshot listeners
        profileListenerRegistration?.remove()
        otherProfileListenerRegistration?.remove()
    }
}