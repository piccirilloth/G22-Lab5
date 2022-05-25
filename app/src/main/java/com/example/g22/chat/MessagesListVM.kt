package com.example.g22.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Message
import com.example.g22.model.TimeSlot
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

class MessagesListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var tsListListenerRegistration: ListenerRegistration? = null

    private val _messageListLD: MutableLiveData<List<Message>> =
        MutableLiveData<List<Message>>().also {
            it.value = emptyList()
        }

    val messageListLD: LiveData<List<Message>> = _messageListLD

    fun observeMessages() {
        tsListListenerRegistration?.remove()
        if (Firebase.auth.currentUser == null) {
            _messageListLD.value = emptyList()
        }
    }
}