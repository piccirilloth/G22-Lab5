package com.example.g22.chat

import android.app.Application
import android.content.BroadcastReceiver
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
import java.time.Instant.now
import java.util.*
import java.util.Date

class MessagesListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var msgListListenerRegistration: ListenerRegistration? = null

    private val _messageListLD: MutableLiveData<List<Message>> =
        MutableLiveData<List<Message>>().also {
            it.value = emptyList()
        }

    val messageListLD: LiveData<List<Message>> = _messageListLD

    fun observeMessages(receiver: String, timeSlotId: String) {
        val users = listOf<String>("${Firebase.auth.currentUser!!.uid}", receiver)
        msgListListenerRegistration?.remove()
        clearList()
        msgListListenerRegistration = db.collection("chats")
            .whereEqualTo("offer", timeSlotId)
            .whereIn("sender", users)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {
                    val result = value.toObjects(Message::class.java).filter {
                        users.contains(it.receiver)
                    }
                    _messageListLD.value = result.sortedBy { it.time.toString() }
                }
                else {
                    _messageListLD.value = emptyList()
                }

            }
    }

    fun createMessage(receiver: String, timeSlotId: String, message: String, offerTitle: String, receiverName: String) {
        db.runTransaction { transaction ->
            val user = Firebase.auth.currentUser
            if(user != null) {
                if (messageListLD.value?.size == 0) {
                    val ref = db.collection("conversations").document()
                    transaction.set(ref, Conversation(timeSlotId, user.uid, receiver, offerTitle, user.displayName.toString(), receiverName))
                }
                val ref = db.collection("chats").document()
                transaction.set(ref, Message(timeSlotId, receiver, "${Firebase.auth.currentUser!!.uid}",
                    message, Date(System.currentTimeMillis())))
            }
        }
    }

    fun clearList() {
        _messageListLD.value = emptyList()
    }
}