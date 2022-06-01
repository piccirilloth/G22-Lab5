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
import java.lang.Integer.max

class InterestingOfferListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var intListListenerRegistration: ListenerRegistration? = null

    var isStatusChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    var isIncoming: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    private val _interOfferListLD: MutableLiveData<MutableList<Conversation>> = MutableLiveData<MutableList<Conversation>>().also {
        it.value = emptyList<Conversation>().toMutableList()
    }

    val interOfferListLD: LiveData<MutableList<Conversation>> = _interOfferListLD

    fun observeRequests(isIncoming: Boolean) {
        val uid = if(isIncoming) "receiverUid" else "requestorUid"
        intListListenerRegistration?.remove()
        val user = Firebase.auth.currentUser
        if(user != null) {
            db.collection("conversations")
                .whereEqualTo(uid, user.uid)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.d("error", "firebase failure")
                        return@addSnapshotListener
                    }
                    if (value != null && !value.isEmpty) {
                        val result = emptyList<Conversation>().toMutableList()
                        val tmp = value.toObjects(Conversation::class.java)
                        for (conv in tmp) {
                            result.add(if (conv.status == Status.REJECTED) max(result.size, 0) else 0, conv)
                        }
                        _interOfferListLD.value = result
                    }
                    else {
                        _interOfferListLD.value = emptyList<Conversation>().toMutableList()
                    }
                }
        }
    }
}