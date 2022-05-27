package com.example.g22.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
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
import com.example.g22.toAdvertisementList

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val messageListVM by activityViewModels<MessagesListVM>()

    private val navArguments: ChatFragmentArgs by navArgs()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var sendBtn: ImageButton
    private lateinit var messageEditText : EditText

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

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = MessageAdapter(emptyList())
        rv.adapter = adapter

        rv.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if(adapter.itemCount > 0 && oldBottom < bottom)
                rv.smoothScrollToPosition(adapter.itemCount - 1)
        }

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
        }

        messageListVM.observeMessages(navArguments., navArguments.offerId)
        // Observe any change of the chat

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