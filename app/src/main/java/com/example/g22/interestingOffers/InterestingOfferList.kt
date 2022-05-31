package com.example.g22.interestingOffers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.SkillsList.SkillsListFragmentDirections
import com.example.g22.model.Conversation
import com.google.android.material.chip.Chip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class InterestingOfferList {
    class InterestingOfferAdapter(private var data: List<Conversation>): RecyclerView.Adapter<InterestingOfferAdapter.InterestingOfferViewHolder>() {
        class InterestingOfferViewHolder(v: View): RecyclerView.ViewHolder(v) {
            private val cardView: CardView = v.findViewById(R.id.interesting_offer_list_item_card)
            private val titleTV : TextView = v.findViewById(R.id.interesting_offer_list_item_offerTitle_text_view)
            private val fullnameTV: TextView = v.findViewById(R.id.interesting_offer_list_item_fullname_text_view)
            private val notChip: Chip = v.findViewById(R.id.interesting_offers_list_item_count_notification_chip)

            fun bind(item: Conversation, onCardViewClickCallback: (Int) -> Unit) {
                titleTV.text = item.offerTitle
                fullnameTV.text = if(item.requestorUid == Firebase.auth.currentUser!!.uid) item.receiverName else item.requestorName
                notChip.text = if(item.requestorUid == Firebase.auth.currentUser!!.uid) item.requestorUnseen.toString() else item.receiverUnseen.toString()
                cardView.setOnClickListener { onCardViewClickCallback(bindingAdapterPosition) }
            }

            fun unbind() {
                cardView.setOnClickListener(null)
            }
        }

        private lateinit var navController: NavController

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestingOfferViewHolder {
            val vg = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.interesting_offer_list_item, parent, false)
            navController = parent.findNavController()
            return InterestingOfferViewHolder(vg)
        }

        override fun onBindViewHolder(holder: InterestingOfferViewHolder, position: Int) {
            val item = data[position]

            holder.bind(item, ::showChat)
        }

        override fun getItemCount(): Int = data.size

        override fun onViewRecycled(holder: InterestingOfferViewHolder) {
            super.onViewRecycled(holder)
            holder.unbind()
        }

        fun updateList(interOfferList: List<Conversation>) {
            // TODO: provide a way to handle list modifications better
            val diffs = DiffUtil.calculateDiff(ConversationListCallback(data, interOfferList))
            data = interOfferList
            diffs.dispatchUpdatesTo(this)
        }

        fun addConversation(c: Conversation) {
            data = data.plus(c)
            notifyItemInserted(itemCount)
        }

        /**
         * Utilities
         */

        class ConversationListCallback(
            private val oldList: List<Conversation>,
            private val newList: List<Conversation>
        ): DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].offerId == newList[newItemPosition].offerId
                        && oldList[oldItemPosition].requestorUid == newList[newItemPosition].requestorUid
                        && oldList[oldItemPosition].receiverUid == newList[newItemPosition].receiverUid
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val isRequestor = oldList[oldItemPosition].receiverUid == Firebase.auth.currentUser!!.uid
                val oldName = if(isRequestor) oldList[oldItemPosition].receiverName else oldList[oldItemPosition].requestorName
                val newName = if(isRequestor) newList[newItemPosition].receiverName else newList[newItemPosition].requestorName
                val oldNot = if(isRequestor) oldList[oldItemPosition].receiverUnseen else oldList[oldItemPosition].requestorUnseen
                val newNot = if(isRequestor) newList[newItemPosition].receiverUnseen else newList[newItemPosition].requestorUnseen
                return oldList[oldItemPosition].offerTitle == newList[newItemPosition].offerTitle
                        && oldName == newName
                        && oldNot == newNot

            }
        }

        private fun showChat(adapterPos: Int) {
            //TODO: show chat
            val currentUser = Firebase.auth.currentUser
            var receiver = ""
            if(currentUser != null)
                receiver = if(currentUser.uid == data[adapterPos].receiverUid)
                    data[adapterPos].requestorUid
                else
                    data[adapterPos].receiverUid
            navController.navigate(
                InterestingOfferListFragmentDirections
                    .actionNavInterestingOffersToChatFragment(
                        receiver = receiver,
                        offerId = data[adapterPos].offerId,
                        offerTitle = data[adapterPos].offerTitle,
                        receiverName = data[adapterPos].requestorName
                    )
            )
        }

    }
}