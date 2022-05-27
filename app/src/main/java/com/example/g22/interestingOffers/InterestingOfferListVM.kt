package com.example.g22.interestingOffers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Conversation
import com.example.g22.model.Message
import com.example.g22.model.TimeSlot
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

class InterestingOfferListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var intListListenerRegistration: ListenerRegistration? = null

    private val _interOfferListLD: MutableLiveData<List<Conversation>> = MutableLiveData<List<Conversation>>().also {
        it.value = emptyList()
    }

    val interOfferListLD: LiveData<List<Conversation>> = _interOfferListLD

    fun observeIncomingRequests() {
        intListListenerRegistration?.remove()
        val user = Firebase.auth.currentUser
        if(user != null) {
            db.collection("conversations")
                .whereEqualTo("receiver", user.uid)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.d("error", "firebase failure")
                        return@addSnapshotListener
                    }
                    if (value != null && !value.isEmpty) {
                        _interOfferListLD.value = value.toObjects(Conversation::class.java)
                    }
                    else {
                        _interOfferListLD.value = emptyList()
                    }
                }
        }
    }
}