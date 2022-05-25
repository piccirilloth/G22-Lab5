package com.example.g22.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.TimeSlotList.AdvertisementAdapter
import com.example.g22.TimeSlotList.MessageAdapter
import com.example.g22.TimeSlotList.TimeSlotListVM
import com.example.g22.toAdvertisementList

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val messageListVM by activityViewModels<MessagesListVM>()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MessageAdapter

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

        // Recycler View configuration
        rv.layoutManager = LinearLayoutManager(requireActivity())
        adapter = MessageAdapter(messageListVM.messageListLD.value ?: emptyList())
        rv.adapter = adapter
    }

}