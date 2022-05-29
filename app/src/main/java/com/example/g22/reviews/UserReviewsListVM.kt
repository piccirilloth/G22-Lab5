package com.example.g22.reviews

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Review
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import java.util.*

class UserReviewsListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var reviewsListListenerRegistration: ListenerRegistration? = null

    private val _reviewsListLD: MutableLiveData<List<Review>> =
        MutableLiveData<List<Review>>().also {
            it.value = emptyList()
        }

    val reviewListLD: LiveData<List<Review>> = _reviewsListLD

    fun observeReviews() {
        val reviewee = "${Firebase.auth.currentUser!!.uid}"
        reviewsListListenerRegistration?.remove()
        db.collection("reviews")
            //.whereEqualTo("reviewee", reviewee)
            .addSnapshotListener { value, error ->
                if(error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if(value != null && !value.isEmpty) {
                    _reviewsListLD.value = value.toObjects<Review?>(Review::class.java)
                        .sortedBy { it.date.toString() }
                } else {
                    _reviewsListLD.value = emptyList()
                }
            }
    }

    fun createReview(reviewee: String, rating: String, description: String, skill: String){
        db.collection("reviews")
            .document()
            .set(Review("${Firebase.auth.currentUser!!.uid}", reviewee, rating,
                description, skill, Date(System.currentTimeMillis())))
    }
}