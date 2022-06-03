package com.example.g22.chat

import android.app.Application
import android.content.BroadcastReceiver
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.Conversation
import com.example.g22.model.Message
import com.example.g22.model.Status
import com.example.g22.model.TimeSlot
import com.example.g22.utils.Duration
import com.google.firebase.Timestamp
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
    private var convStatusListenerRegistration: ListenerRegistration? = null

    private val _messageListLD: MutableLiveData<List<Message>> =
        MutableLiveData<List<Message>>().also {
            it.value = emptyList()
        }


    val messageListLD: LiveData<List<Message>> = _messageListLD

    var conversationId: MutableLiveData<String> = MutableLiveData<String>("")

    private val _conversationStatusLD: MutableLiveData<Status?> = MutableLiveData<Status?>()

    val conversationStatusLD = _conversationStatusLD


    fun observeMessages(receiver: String, timeSlotId: String) {
        val users = listOf<String>("${Firebase.auth.currentUser!!.uid}", receiver)
        msgListListenerRegistration?.remove()
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
                    if (!result.isEmpty()) {
                        conversationId.value = result[0].conversationId
                        _messageListLD.value = result.sortedBy { it.time.toString() }
                    } else {
                        _messageListLD.value = emptyList()
                    }
                } else {
                    _messageListLD.value = emptyList()
                }

            }
    }

    fun observeConversationStatus() {
        convStatusListenerRegistration?.remove()
        if (conversationId.value != "") {
            convStatusListenerRegistration = db.collection("conversations")
                .document(conversationId.value!!)
                .addSnapshotListener { value, error ->
                    val tmpStatus = value!!.get("status").toString()
                    if (tmpStatus == "CONFIRMED")
                        _conversationStatusLD.value = Status.CONFIRMED
                    else if (tmpStatus == "REJECTED")
                        _conversationStatusLD.value = Status.REJECTED
                    else if (tmpStatus == "REJECTED_BALANCE")
                        _conversationStatusLD.value = Status.REJECTED_BALANCE
                    else
                        _conversationStatusLD.value = Status.PENDING
                }
        }
    }

    fun resetNotifications() {
        db.runTransaction { transaction ->
            val convRef = db.collection("conversations").document(conversationId.value!!)
            val requestorUid = transaction.get(convRef).get("requestorUid")
            transaction.update(
                convRef,
                if (requestorUid == Firebase.auth.currentUser!!.uid) "requestorUnseen" else "receiverUnseen",
                0
            )
        }
    }

    fun resetConversationStatus() {
        _conversationStatusLD.value = null
    }

    fun createMessage(
        receiver: String,
        timeSlotId: String,
        message: String,
        offerTitle: String,
        receiverName: String
    ) {
        val user = Firebase.auth.currentUser
        val ref = db.collection("conversations").document()
        db.runTransaction { transaction ->
            var requestorUid = ""
            var oldReceiverUnseen = 0
            var oldRequestorUnseen = 0
            if (user != null) {
                val oldProposalsCounter = transaction.get(
                    db.collection("offers").document(timeSlotId),
                ).getLong("proposalsCounter")

                if (conversationId.value!! != "") {
                    val updateNotRef =
                        db.collection("conversations").document(conversationId.value!!)
                    val queryResult = transaction.get(updateNotRef)
                    requestorUid = queryResult.get("requestorUid").toString()
                    oldReceiverUnseen = queryResult.get("receiverUnseen").toString().toInt()
                    oldRequestorUnseen = queryResult.get("requestorUnseen").toString().toInt()
                }
                val chatRef = db.collection("chats").document()
                transaction.set(
                    chatRef, Message(
                        chatRef.id,
                        timeSlotId,
                        receiver,
                        "${Firebase.auth.currentUser!!.uid}",
                        message,
                        Timestamp.now().toDate(),
                        if (conversationId.value!! == "") ref.id else conversationId.value!!
                    )
                )
                if (messageListLD.value?.size == 0) {
                    transaction.set(
                        ref,
                        Conversation(
                            ref.id,
                            timeSlotId,
                            user.uid,
                            receiver,
                            offerTitle,
                            user.displayName.toString(),
                            receiverName,
                            1,
                            0,
                            Status.PENDING
                        )
                    )
                    transaction.update(
                        db.collection("offers").document(timeSlotId),
                        "proposalsCounter",
                        oldProposalsCounter?.plus(1)
                    )
                } else {
                    val updateNotRef =
                        db.collection("conversations").document(conversationId.value!!)
                    val userToNotify =
                        if (receiver == requestorUid) "requestorUnseen" else "receiverUnseen"
                    transaction.update(
                        updateNotRef,
                        userToNotify,
                        if (userToNotify == "requestorUnseen") oldRequestorUnseen + 1 else oldReceiverUnseen + 1
                    )
                }
            }
        }.addOnSuccessListener {
            if (conversationId.value!! == "")
                conversationId.value = ref.id
        }.addOnFailureListener {
            Log.d("Creazione:errore", it.message.toString())
        }


    }

    fun clearList() {
        _messageListLD.value = emptyList()
    }

    fun confirmRequest() {
        db.runTransaction { transaction ->
            val convRef = db.collection("conversations").document(conversationId.value!!)
            val convResult = transaction.get(convRef)
            val requestorUid: String = convResult.getString("requestorUid") ?: return@runTransaction
            val receiverUid: String = convResult.getString("receiverUid") ?: return@runTransaction
            val offerId: String = convResult.getString("offerId") ?: return@runTransaction

            val requestorRef = db.collection("users").document(requestorUid)
            val creditReq: Int = transaction.get(requestorRef).get("credit").toString().toInt()

            val receiverRef = db.collection("users").document(receiverUid)
            val creditRec: Int = transaction.get(receiverRef).get("credit").toString().toInt()

            val offerRef = db.collection("offers").document(offerId)
            val offerDuration =
                transaction.get(offerRef).get("duration", Duration::class.java)?.minutes
                    ?: return@runTransaction
            if (creditReq >= offerDuration) {
                // the user has enough credits
                transaction.update(requestorRef, "credit", creditReq - offerDuration)
                transaction.update(convRef, "status", Status.CONFIRMED)
                transaction.update(offerRef, "isAccepted", true)
                transaction.update(receiverRef, "credit", creditRec + offerDuration)
            } else {
                val rejRef = db.collection("conversations")
                    .document(conversationId.value!!)
                transaction.update(rejRef, "status", Status.REJECTED_BALANCE)
            }

        }

    }

    fun rejectRequest() {
        db.runTransaction { transaction ->
            val convRef = db.collection("conversations")
                .document(conversationId.value!!)

            val offerRef = db.collection("offers").document(_messageListLD.value!!.first().offer)
            val oldCounter = transaction.get(offerRef)
                .getLong("proposalsCounter")

            transaction.update(convRef,"status", Status.REJECTED)

            transaction.update(offerRef, "proposalsCounter", oldCounter!!.minus(1))

        }
            .addOnSuccessListener {
                _conversationStatusLD.value = Status.REJECTED
            }

    }

    override fun onCleared() {
        super.onCleared()
        convStatusListenerRegistration?.remove()
        msgListListenerRegistration?.remove()
    }

}