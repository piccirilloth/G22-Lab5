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

    private val _numReviewsLD : MutableLiveData<Int> = MutableLiveData<Int>(0)
    private val _avgScoreLD : MutableLiveData<Float> = MutableLiveData<Float>("0.0".toFloat())

    val reviewListLD: LiveData<List<Review>> = _reviewsListLD
    val numReviewsLD : LiveData<Int> = _numReviewsLD
    val avgScoreLD : LiveData<Float> = _avgScoreLD

    fun observeReviews(revieweeId: String) {
        reviewsListListenerRegistration?.remove()
        db.collection("reviews")
            .whereEqualTo("revieweeId", revieweeId)
            .addSnapshotListener { value, error ->
                if(error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if(value != null && !value.isEmpty) {
                    _reviewsListLD.value = value.toObjects<Review?>(Review::class.java)
                        .sortedBy { it.date.toString() }
                    _numReviewsLD.value = _reviewsListLD.value!!.size
                    _avgScoreLD.value = _reviewsListLD.value!!.map { it -> it.rating.toFloat() }.average().toFloat()
                } else {
                    _reviewsListLD.value = emptyList()
                    _numReviewsLD.value = 0
                    _avgScoreLD.value = "0.0".toFloat()
                }
            }
    }

    fun createReview(reviewee: String, rating: String, description: String, skill: String){
        db.collection("reviews")
            .document()
            .set(Review("${Firebase.auth.currentUser!!.uid}", reviewee, "abc", rating,
                description, skill, Date(System.currentTimeMillis())))
    }

    /*fun numReviews(userId : String) {

        db.collection("reviews")
            .whereEqualTo("revieweeId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {
                    _numReviewsLD.value = value.size()
                }
            }
    }*/

    /*fun avgScore(userId: String) {
        db.collection("reviews")
            .whereEqualTo("revieweeId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {

                }
            }
    }*/
}