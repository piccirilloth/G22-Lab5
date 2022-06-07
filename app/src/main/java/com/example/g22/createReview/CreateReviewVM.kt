package com.example.g22.createReview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.g22.Event
import com.example.g22.SnackbarMessage
import com.example.g22.addMessage
import com.example.g22.model.Review
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

class CreateReviewVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private val _currentRevieweeLD: MutableLiveData<String> = MutableLiveData("")
    private val _currentRevieweeScoreLD: MutableLiveData<Double> = MutableLiveData(0.0)
    private val _currentRevieweeCounterReviewsLD: MutableLiveData<Int> = MutableLiveData(0)

    val currentRevieweeLD: LiveData<String> = _currentRevieweeLD
    val currentRevieweeScoreLD: LiveData<Double> = _currentRevieweeScoreLD
    val currentRevieweeCounterReviewsLD: LiveData<Int> = _currentRevieweeCounterReviewsLD

    // Snackbar handling
    private val _snackbarMessages = MutableLiveData<List<Event<SnackbarMessage>>>(emptyList())
    val snackbarMessages: LiveData<List<Event<SnackbarMessage>>>
        get() = _snackbarMessages


    fun observeCurrentReviewInfo(revieweeId: String, reviewType: String) {
        viewModelScope.launch {
            val fullnameResult = firebaseGetUserFullname(revieweeId)
            if (fullnameResult.isSuccess)
                _currentRevieweeLD.value = fullnameResult.getOrThrow()

            val ratingsResult = firebaseGetRevieweeRatings(revieweeId, reviewType)
            if (ratingsResult.isSuccess) {
                val ratings = ratingsResult.getOrThrow()
                var score = ratings.fold(0.0) { l: Double , r: Double ->
                    l+r
                }
                if (ratings.isNotEmpty() && abs(score - 0.0) > 1e-6) {
                    score /= ratings.size
                }

                _currentRevieweeScoreLD.value = score
                _currentRevieweeCounterReviewsLD.value = ratings.size
            }
        }
    }


    fun createReview(revieweeId: String, reviewType: String, offerId: String, rating: Double, description: String, conversationId: String) {
        viewModelScope.launch {
            val result = firebaseCreateReview(revieweeId, reviewType, offerId, rating, description, conversationId, Firebase.auth.currentUser!!.uid)
            if (result.isSuccess) {
                _snackbarMessages.addMessage("Review created successfully!", Snackbar.LENGTH_LONG)
            } else {
                _snackbarMessages.addMessage("Error while creating review!", Snackbar.LENGTH_LONG)
            }
        }
    }

    /**
     * Async functions
     */

    private suspend fun firebaseGetUserFullname(userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fullname = db.collection("users")
                    .document(userId)
                    .get()
                    .await()
                    .getString("fullname")
                if (fullname != null)
                    return@withContext Result.success(fullname)
                else
                    return@withContext Result.failure(Exception("invalid user in database"))
            } catch(e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private suspend fun firebaseGetRevieweeRatings(revieweeId: String, reviewType: String): Result<List<Double>> {
        return withContext(Dispatchers.IO) {
            try {
                val ratings = db.collection("reviews")
                    .whereEqualTo("revieweeId", revieweeId)
                    .whereEqualTo("reviewType", reviewType)
                    .get()
                    .await()
                    .map { it.getDouble("rating")!! }

                return@withContext Result.success(ratings)
            } catch(e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private suspend fun firebaseCreateReview(revieweeId: String, reviewType: String, offerId: String, rating: Double, description: String, conversationId: String, reviewerId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.runTransaction { transaction ->
                    val reviewee = currentRevieweeLD.value?: ""

                    val reviewerRef = db.collection("users").document(reviewerId)
                    val reviewer = transaction.get(reviewerRef).getString("fullname")?: ""

                    val offerRef = db.collection("offers").document(offerId)
                    val offerTitle = transaction.get(offerRef).getString("title")?: ""

                    val reviewRef = db.collection("reviews").document()
                    val review = Review(reviewRef.id, reviewType, reviewer, reviewerId, reviewee, revieweeId, rating,
                        description, offerTitle, null)

                    val convRef = db.collection("conversations").document(conversationId)
                    /*
                        reviewedOfferer = true -> offerer reviewed
                        reviewedRequestor = true -> requestor reviewed
                     */
                    if(reviewType == "offerer") transaction.update(convRef, "reviewedOfferer", true) else transaction.update(convRef, "reviewedRequestor", true)

                    transaction.set(reviewRef, review)
                }.await()

                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

}