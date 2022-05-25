package com.example.g22.TimeSlotList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.model.Message

class MessageAdapter(private var data: List<Message>): RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    class MessageViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val messageCard: CardView = v.findViewById(R.id.message_item_card)
        private val messageTV : TextView = v.findViewById(R.id.message_item_textview)
        private val timeTV : TextView = v.findViewById(R.id.message_time_textview)

        fun bind(message: String, time: String) {
            messageTV.text = message
            timeTV.text = time
        }

        fun unbind() {

        }
    }

    private lateinit var navController: NavController

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val vg = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        navController = parent.findNavController()
        return MessageViewHolder(vg)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = data[position]

        holder.bind(item.text, item.time.toString())
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

}