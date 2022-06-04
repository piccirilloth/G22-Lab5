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

    private val _reviewsOffererListLD: MutableLiveData<List<Review>> =
        MutableLiveData<List<Review>>().also {
            it.value = emptyList()
        }

    private val _numOffererReviewsLD : MutableLiveData<Int> = MutableLiveData<Int>(0)
    private val _avgOffererScoreLD : MutableLiveData<Float> = MutableLiveData<Float>("0.0".toFloat())

    private val _reviewsRequestorListLD: MutableLiveData<List<Review>> =
        MutableLiveData<List<Review>>().also {
            it.value = emptyList()
        }

    private val _numRequestorReviewsLD : MutableLiveData<Int> = MutableLiveData<Int>(0)
    private val _avgRequestorScoreLD : MutableLiveData<Float> = MutableLiveData<Float>("0.0".toFloat())

    val reviewsOffererListLD: LiveData<List<Review>> = _reviewsOffererListLD
    val numOffererReviewsLD : LiveData<Int> = _numOffererReviewsLD
    val avgOffererScoreLD : LiveData<Float> = _avgOffererScoreLD

    val reviewsRequestorListLD: LiveData<List<Review>> = _reviewsRequestorListLD
    val numRequestorReviewsLD : LiveData<Int> = _numRequestorReviewsLD
    val avgRequestorScoreLD : LiveData<Float> = _avgRequestorScoreLD

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
                    _reviewsOffererListLD.value = value.toObjects(Review::class.java).filter { it.reviewType == "offerer" }
                        .sortedBy { it.date.toString() }
                    _numOffererReviewsLD.value = _reviewsOffererListLD.value!!.size
                    _avgOffererScoreLD.value = _reviewsOffererListLD.value!!.map { it -> it.rating.toFloat() }.average()
                                .toFloat().let { if(it.isNaN()) "0.0".toFloat() else it }
                    _reviewsRequestorListLD.value = value.toObjects(Review::class.java).filter { it.reviewType == "requestor" }
                        .sortedBy { it.date.toString() }
                    _numRequestorReviewsLD.value = _reviewsRequestorListLD.value!!.size
                    _avgRequestorScoreLD.value = _reviewsRequestorListLD.value!!.map { it -> it.rating.toFloat() }.average()
                        .toFloat().let { if(it.isNaN()) "0.0".toFloat() else it }
                } else {
                    _reviewsOffererListLD.value = emptyList()
                    _numOffererReviewsLD.value = 0
                    _avgOffererScoreLD.value = "0.0".toFloat()
                    _reviewsRequestorListLD.value = emptyList()
                    _numRequestorReviewsLD.value = 0
                    _avgRequestorScoreLD.value = "0.0".toFloat()
                }
            }
    }
}