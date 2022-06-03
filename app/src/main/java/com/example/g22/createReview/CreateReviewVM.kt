package com.example.g22.createReview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Review
import com.google.firebase.firestore.FirebaseFirestore

class CreateReviewVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private val _currentRevieweeLD: MutableLiveData<String> = MutableLiveData("")
    private val _currentRevieweeScoreLD: MutableLiveData<Double> = MutableLiveData(0.0)
    private val _currentRevieweeCounterReviewsLD: MutableLiveData<Int> = MutableLiveData(0)

    val currentRevieweeLD: LiveData<String> = _currentRevieweeLD
    val currentRevieweeScoreLD: LiveData<Double> = _currentRevieweeScoreLD
    val currentRevieweeCounterReviewsLD: LiveData<Int> = _currentRevieweeCounterReviewsLD


    fun observeCurrentReviewInfo(revieweeId: String, reviewType: String) {
        var revieweeName = ""
        var revieweeScore = 0.0
        db.collection("users").document(revieweeId)
            .get()
            .addOnSuccessListener {
                revieweeName = it.get("fullname").toString()
                _currentRevieweeLD.value = revieweeName
            }
        db.collection("reviews")
            .whereEqualTo("revieweeId", revieweeId)
            .whereEqualTo("reviewType", reviewType)
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    val size = it.size()
                    for (snap in it) {
                        revieweeScore += snap.getDouble("rating")!!
                    }
                    revieweeScore = revieweeScore / size
                    _currentRevieweeScoreLD.value = revieweeScore
                    _currentRevieweeCounterReviewsLD.value = size
                }
                else {
                    _currentRevieweeCounterReviewsLD.value = 0
                }
            }

    }


}