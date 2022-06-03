package com.example.g22.interestingOffers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Conversation
import com.example.g22.model.Message
import com.example.g22.model.Status
import com.example.g22.model.TimeSlot
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.lang.Integer.max

class InterestingOfferListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var intListListenerRegistration: ListenerRegistration? = null

    var isIncoming: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    private val _interOfferListLD: MutableLiveData<MutableList<Conversation>> = MutableLiveData<MutableList<Conversation>>().also {
        it.value = emptyList<Conversation>().toMutableList()
    }

    val interOfferListLD: LiveData<MutableList<Conversation>> = _interOfferListLD

    fun observeRequests(isIncoming: Boolean, isAccepted: Boolean) {
        intListListenerRegistration?.remove()

        val uid = if(isIncoming) "receiverUid" else "requestorUid"
        val status = if (isAccepted) listOf(Status.CONFIRMED) else listOf(Status.PENDING, Status.REJECTED)
        val user = Firebase.auth.currentUser

        if(user != null) {
            intListListenerRegistration = db.collection("conversations")
                .whereEqualTo(uid, user.uid)
                .whereIn("status", status)
                .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (value != null && !value.isEmpty) {
                        _interOfferListLD.postValue(value.toObjects(Conversation::class.java).sortedBy {
                            if (it.status == Status.REJECTED)
                                 1
                            else
                                 0
                        }.toMutableList())
                    }
                    else {
                        _interOfferListLD.postValue(emptyList<Conversation>().toMutableList())
                    }
                }
        } else {
            _interOfferListLD.value = emptyList<Conversation>().toMutableList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        intListListenerRegistration?.remove()
    }
}