package com.example.g22

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgument
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.g22.ShowProfile.ProfileVM
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navHeaderProfilePic: ImageView
    private lateinit var navHeaderProfileName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderLoginLabel: TextView
    private lateinit var navHeaderGoogleLoginButton: SignInButton
    private lateinit var myOffersItem: MenuItem
    private lateinit var profileItem: MenuItem
    private lateinit var intOffersItem: MenuItem

    lateinit var oneTapClient: SignInClient
    lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 2
    private var showOneTapUI = true
    private lateinit var auth: FirebaseAuth


    private val profileVM by viewModels<ProfileVM>()

    // The listed fragments will implement a confirmation alert before popping the back stack
    private val fragmentsRequiringCustomBack = setOf(
        R.id.nav_edit_profile,
        R.id.nav_timeslot_edit
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        auth = Firebase.auth

        // View & nav controller references
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        navController = navHostFragment.navController
        myOffersItem = navView.menu.findItem(R.id.nav_timeslot_list_my_offers)
        profileItem = navView.menu.findItem(R.id.nav_show_profile)
        intOffersItem = navView.menu.findItem(R.id.nav_interesting_offers)

        // Set the navigation drawer header
        val navHeader: View = navView.getHeaderView(0)
        navHeaderProfilePic = navHeader.findViewById(R.id.nav_header_imageView)
        navHeaderProfileName = navHeader.findViewById(R.id.nav_header_profile_name)
        navHeaderEmail = navHeader.findViewById(R.id.nav_header_email)
        navHeaderLoginLabel = navHeader.findViewById(R.id.nav_header_login_label)
        navHeaderGoogleLoginButton = navHeader.findViewById(R.id.nav_header_google_login_button)
        googleBtnUI()

        profileVM.profileLD.observe(this) {
            navHeaderProfileName.text = it.fullname
            navHeaderEmail.text = it.email
        }
        profileVM.profileImageLD.observe(this) {
            updateProfileImage(it)
        }

        // Set the toolbar as the main action bar
        setSupportActionBar(toolbar)

        // Create the app bar configuration
        // Pass each ID as a set of Ids because each
        // menu should be considered as top level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_timeslot_list_my_offers,
                R.id.nav_show_profile,
                R.id.nav_skills_list,
                R.id.nav_interesting_offers
            ), drawerLayout
        )

        // Link nav controller to the app bar configuration
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Link nav view with the nav controller
        navView.setupWithNavController(navController)

        Firebase.auth.addAuthStateListener {
            if (it.currentUser == null) {
                myOffersItem.isEnabled = false
                profileItem.isEnabled = false
                intOffersItem.isEnabled = false

                navHeaderProfilePic.visibility = View.GONE
                navHeaderProfileName.visibility = View.GONE
                navHeaderEmail.visibility = View.GONE

                navHeaderLoginLabel.visibility = View.VISIBLE
                navHeaderGoogleLoginButton.visibility = View.VISIBLE
                if (navController.currentDestination?.id == R.id.nav_timeslot_list_my_offers ||
                    navController.currentDestination?.id == R.id.nav_timeslot_edit ||
                    navController.currentDestination?.id == R.id.nav_timeslot_show_my_offers ||
                    navController.currentDestination?.id == R.id.nav_interesting_offers
                ) {
                    // if (navController.currentDestination?.id != R.id.nav_skills_list)
                    navController.navigate(R.id.nav_to_home)
                }
            } else {
                myOffersItem.isEnabled = true
                profileItem.isEnabled = true
                intOffersItem.isEnabled = true

                navHeaderProfilePic.visibility = View.VISIBLE
                navHeaderProfileName.visibility = View.VISIBLE
                navHeaderEmail.visibility = View.VISIBLE

                navHeaderLoginLabel.visibility = View.GONE
                navHeaderGoogleLoginButton.visibility = View.GONE
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        if (auth.currentUser == null)
            inflater.inflate(R.menu.google_login_menu, menu)
        else
            inflater.inflate(R.menu.google_logout_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This will support custom back navigation also from the toolbar back button
        if (item.itemId == android.R.id.home &&
            fragmentsRequiringCustomBack.contains(navController.currentDestination?.id)
        ) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.google_login_item) {
            googleLogin()
            return true
        } else if (item.itemId == R.id.google_logout_item) {
            auth.signOut()
            invalidateMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateProfileImage(localPath: String) {
        if (!localPath.isValidImagePath()) {
            navHeaderProfilePic.setImageBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.user_icon)
            )
            return
        }

        // TODO: use coroutine
        val localFile = File(filesDir.path, localPath)
        val inputStream = localFile.inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        navHeaderProfilePic.setImageBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with Firebase.
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithCredential:success")
                                        invalidateMenu()

                                        // TODO: improve firestore observe management
                                        // profileVM.observeAuthenticatedUser()
                                        // tsListVM.observeMyOffers()

                                        // TODO: updateUI(user)
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                        // TODO: updateUI(null) you can sign out with Firebase.auth.signOut()
                                    }
                                }
                            Log.d(TAG, "Got ID token.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token!")
                        }
                    }
                } catch (e: ApiException) {
                    // TODO: if the user close the sign in prompt...
                }
            }
        }
    }

    private fun googleLogin() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, 2,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage)
            }
    }

    private fun googleBtnUI() {
        navHeaderGoogleLoginButton.setOnClickListener {
            googleLogin()
        }

        for (v in navHeaderGoogleLoginButton.children) {
            if (v is TextView) {
                v.text = getString(R.string.nav_header_login_button)
                v.setPadding(15, 15, 15, 15)
                v.isSingleLine = true
            }
        }
    }
}