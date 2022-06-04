package com.example.g22.createReview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*

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

    fun createReview(revieweeId: String, reviewType: String, offerId: String, rating: Double, description: String, conversationId: String) {
        val reviewerId = Firebase.auth.currentUser!!.uid
        db.runTransaction { transaction ->
            val reviewee = currentRevieweeLD.value?: ""

            val reviewerRef = db.collection("users").document(reviewerId)
            val reviewer = transaction.get(reviewerRef).getString("fullname")?: ""

            val offerRef = db.collection("offers").document(offerId)
            val offerTitle = transaction.get(offerRef).getString("title")?: ""

            val review = Review(reviewType, reviewer, reviewerId, reviewee, revieweeId, rating,
                description, offerTitle, Timestamp.now().toDate())

            val reviewRef = db.collection("reviews").document()

            val convRef = db.collection("conversations").document(conversationId)
            /*
                reviewedOfferer = true -> offerer reviewed
                reviewedRequestor = true -> requestor reviewed
             */
            if(reviewType == "offerer") transaction.update(convRef, "reviewedOfferer", true) else transaction.update(convRef, "reviewedRequestor", true)

            transaction.set(reviewRef, review)
        }
    }

}