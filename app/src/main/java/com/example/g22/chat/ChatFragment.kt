package com.example.g22.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.TimeSlotList.AdvertisementAdapter
import com.example.g22.TimeSlotList.MessageAdapter
import com.example.g22.TimeSlotList.TimeSlotListFragmentArgs
import com.example.g22.TimeSlotList.TimeSlotListVM
import com.example.g22.model.Message
import com.example.g22.model.Status
import com.example.g22.toAdvertisementList
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val messageListVM by activityViewModels<MessagesListVM>()

    private val navArguments: ChatFragmentArgs by navArgs()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var sendBtn: ImageButton
    private lateinit var messageEditText : EditText
    private lateinit var acceptBtn: Button
    private lateinit var rejectBtn: Button
    private lateinit var rejectMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = requireActivity().findViewById(R.id.chat_fragment_rv)
        sendBtn = requireActivity().findViewById(R.id.chat_fragment_message_send_img_button)
        messageEditText = requireActivity().findViewById(R.id.chat_fragment_message_edit_text)
        acceptBtn = requireActivity().findViewById(R.id.chat_fragment_accept_button)
        rejectBtn = requireActivity().findViewById(R.id.chat_fragment_reject_button)
        rejectMessage = requireActivity().findViewById(R.id.chat_fragment_reject_message)
        rejectMessage.visibility = View.GONE
        if (savedInstanceState == null) {
            messageListVM.clearList()
            messageListVM.resetConversationStatus()
            messageListVM.conversationId.value = ""

            if (navArguments.conversationId == null)
                messageListVM.createConversationIfNotExist(navArguments.receiver, navArguments.offerId)
            else
                messageListVM.observeMessages(navArguments.conversationId!!, navArguments.receiver, navArguments.offerId)
        }

        messageListVM.conversationStatusLD.observe(viewLifecycleOwner) {
            if (it == Status.REJECTED) {
                if (!messageListVM.messageListLD.value!!.isEmpty()) {
                    rejectMessage.visibility = View.VISIBLE
                    if (messageListVM.messageListLD.value!!.first().sender == Firebase.auth.currentUser!!.uid) {
                        rejectMessage.text = "Your proposal has been rejected."
                    } else {
                        rejectMessage.text = "You have rejected user's proposal."
                    }
                }

                messageEditText.isEnabled = false
                sendBtn.isEnabled = false
                acceptBtn.visibility = View.GONE
                rejectBtn.visibility = View.GONE
            }
            else if (it == Status.REJECTED_BALANCE) {
                if (!messageListVM.messageListLD.value!!.isEmpty()) {
                    rejectMessage.visibility = View.VISIBLE
                    if (messageListVM.messageListLD.value!!.first().sender == Firebase.auth.currentUser!!.uid) {
                        rejectMessage.text = "Your proposal can't be accepted. Insufficient credit."
                    } else {
                        rejectMessage.text = "User's proposal can't be accepted. User's credit is not sufficient."
                    }
                }
                messageEditText.isEnabled = false
                sendBtn.isEnabled = false
                acceptBtn.visibility = View.GONE
                rejectBtn.visibility = View.GONE
            }

            else if (it == Status.CONFIRMED) {
                if (!messageListVM.messageListLD.value!!.isEmpty()) {
                    rejectMessage.visibility = View.VISIBLE

                    if (messageListVM.messageListLD.value!!.first().sender == Firebase.auth.currentUser!!.uid) {
                        rejectMessage.text = "Your proposal has been accepted!"
                    }
                    else {
                        rejectMessage.text = "Your have accepted user's proposal!"
                    }
                }

                acceptBtn.visibility = View.GONE
                rejectBtn.visibility = View.GONE
            } else {
                rejectMessage.visibility = View.GONE
                if(!messageListVM.messageListLD.value!!.isEmpty()) {
                    if (messageListVM.messageListLD.value!!.first().sender == Firebase.auth.currentUser!!.uid) {
                        acceptBtn.visibility = View.GONE
                        rejectBtn.visibility = View.GONE
                    } else {
                        acceptBtn.visibility = View.VISIBLE
                        rejectBtn.visibility = View.VISIBLE
                    }
                } else {
                    acceptBtn.visibility = View.GONE
                    rejectBtn.visibility = View.GONE
                }

            }
        }

        acceptBtn.setOnClickListener {
            messageListVM.confirmRequest()
        }

        rejectBtn.setOnClickListener {
            messageListVM.rejectRequest()
        }

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = MessageAdapter(emptyList())
        rv.adapter = adapter


        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        toolbar.title = "Offer's chat" //TODO: enhance the name of the toolbar

        rv.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if(adapter.itemCount > 0)
                rv.smoothScrollToPosition(adapter.itemCount - 1)
        }


        // Observe any change of the chat
        messageListVM.messageListLD.observe(viewLifecycleOwner) {
            /*
            The first time:
                - updateList is triggered with emptylist
                - db call is done and live data populated
                - updateList triggered with livedata content
            The second time:
                - updateList is triggered with livedata content (not empty this time)
                - db call is done and live data updated
                - addMessage triggered with livedata last element
            This is the motivation behind the if
             */
            if(it.size > adapter.itemCount) {
                if(adapter.itemCount == 0)
                    adapter.updateList(it)
                else
                    adapter.addMessage(it.last())
                if(adapter.itemCount > 0)
                    rv.smoothScrollToPosition(adapter.itemCount - 1)
            }
            if (it.size > 0 && messageListVM.conversationStatusLD.value == null) {
                messageListVM.observeConversationStatus()
                if (it.first().sender == Firebase.auth.currentUser!!.uid) {
                    acceptBtn.visibility = View.GONE
                    rejectBtn.visibility = View.GONE
                }
                else {
                    acceptBtn.visibility = View.VISIBLE
                    rejectBtn.visibility = View.VISIBLE
                }
            }

        }

        messageListVM.conversationId.observe(viewLifecycleOwner) {
            if(messageListVM.conversationId.value!! != "") {
                messageListVM.observeMessages(it, navArguments.receiver, navArguments.offerId)
                messageListVM.resetNotifications()
            }
        }

        sendBtn.setOnClickListener{
            if(messageEditText.text.toString() != "") {
                messageListVM.createMessage(
                    navArguments.receiver,
                    navArguments.offerId,
                    messageEditText.text.toString(),
                    navArguments.offerTitle,
                    navArguments.receiverName
                )
                messageEditText.text.clear()
            }
        }
        //messageListVM.messageListLD.value?.size?.let { rv.scrollToPosition(it) }
    }

}