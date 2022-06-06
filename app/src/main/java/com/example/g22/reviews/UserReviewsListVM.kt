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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
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

        reviewsListListenerRegistration = db.collection("reviews")
            .whereEqualTo("revieweeId", revieweeId)
            .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                if(error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if(value != null && !value.isEmpty) {
                    val reviews = value.toObjects(Review::class.java)
                    val reviewsOfferer = reviews.filter { r: Review -> r.reviewType == "offerer" }
                        .sortedBy { r: Review -> r.date.toString() }

                    _reviewsOffererListLD.postValue(reviewsOfferer)
                    _numOffererReviewsLD.postValue(reviewsOfferer.size)
                    _avgOffererScoreLD.postValue(
                        reviewsOfferer.map { r: Review -> r.rating.toFloat() }.average()
                                .toFloat().let { f: Float -> if(f.isNaN()) "0.0".toFloat() else f })

                    val reviewsRequestor = reviews.filter { r: Review -> r.reviewType == "requestor" }
                        .sortedBy { r: Review -> r.date.toString() }
                    _reviewsRequestorListLD.postValue(reviewsRequestor)
                    _numRequestorReviewsLD.postValue(reviewsRequestor.size)
                    _avgRequestorScoreLD.postValue(reviewsRequestor.map { r: Review -> r.rating.toFloat() }.average()
                        .toFloat().let { f: Float -> if(f.isNaN()) "0.0".toFloat() else f })
                } else {
                    _reviewsOffererListLD.postValue(emptyList())
                    _numOffererReviewsLD.postValue(0)
                    _avgOffererScoreLD.postValue("0.0".toFloat())
                    _reviewsRequestorListLD.postValue(emptyList())
                    _numRequestorReviewsLD.postValue(0)
                    _avgRequestorScoreLD.postValue("0.0".toFloat())
                }
            }
    }

    /**
     * ViewModel callbacks
     */
    override fun onCleared() {
        super.onCleared()

        // Clear all snapshot listeners
        reviewsListListenerRegistration?.remove()
    }
}