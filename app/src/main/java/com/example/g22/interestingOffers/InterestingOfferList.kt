package com.example.g22.interestingOffers

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.SkillsList.SkillsListFragmentDirections
import com.example.g22.model.Conversation
import com.example.g22.model.Status
import com.google.android.material.chip.Chip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.collection.LLRBNode
import com.google.firebase.ktx.Firebase

class InterestingOfferList {
    class InterestingOfferAdapter(private var data: List<Conversation>): RecyclerView.Adapter<InterestingOfferAdapter.InterestingOfferViewHolder>() {
        class InterestingOfferViewHolder(v: View): RecyclerView.ViewHolder(v) {
            private val cardView: CardView = v.findViewById(R.id.interesting_offer_list_item_card)
            private val titleTV : TextView = v.findViewById(R.id.interesting_offer_list_item_offerTitle_text_view)
            private val fullnameTV: TextView = v.findViewById(R.id.interesting_offer_list_item_fullname_text_view)
            private val notChip: Chip = v.findViewById(R.id.interesting_offers_list_item_count_notification_chip)
            private val itemCl: ConstraintLayout = v.findViewById(R.id.interesting_offer_list_card_cl)
            private val rateButton: ImageButton = v.findViewById(R.id.interesting_offer_list_rate_button)

            fun bind(item: Conversation, onCardViewClickCallback: (Int) -> Unit, onRateBtnClickCallback: (Int) -> Unit) {
                titleTV.text = item.offerTitle
                fullnameTV.text = if(item.requestorUid == Firebase.auth.currentUser!!.uid) item.receiverName else item.requestorName
                notChip.text = if(item.requestorUid == Firebase.auth.currentUser!!.uid) item.requestorUnseen.toString() else item.receiverUnseen.toString()
                if (notChip.text.toString().toInt() == 0) {
                    notChip.visibility = View.INVISIBLE
                }
                else {
                    notChip.visibility = View.VISIBLE
                }

                cardView.setOnClickListener { onCardViewClickCallback(bindingAdapterPosition) }
                if (item.status == Status.REJECTED || item.status == Status.REJECTED_BALANCE) {
                    notChip.visibility = View.INVISIBLE
                    rateButton.visibility = View.INVISIBLE
                    itemCl.setBackgroundResource(R.drawable.rounded_corner_rejected)
                }
                else if (item.status == Status.CONFIRMED) {
                    if (notChip.text.toString().toInt() > 0)
                        notChip.visibility = View.VISIBLE

                    if(item.receiverUid == Firebase.auth.currentUser!!.uid)
                        if(item.reviewedRequestor)
                            rateButton.visibility = View.INVISIBLE
                        else
                            rateButton.visibility = View.VISIBLE
                    else
                        if(item.reviewedOfferer)
                            rateButton.visibility = View.INVISIBLE
                        else
                            rateButton.visibility = View.VISIBLE
                    rateButton.setOnClickListener { onRateBtnClickCallback(bindingAdapterPosition) }
                    itemCl.setBackgroundResource(R.drawable.rounded_corner)
                }
                else {
                    rateButton.visibility = View.INVISIBLE
                    if (notChip.text.toString().toInt() > 0)
                        notChip.visibility = View.VISIBLE
                    itemCl.setBackgroundResource(R.drawable.rounded_corner)
                }
            }

            fun unbind() {
                cardView.setOnClickListener(null)
                rateButton.setOnClickListener(null)
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

            holder.bind(item, ::showChat, ::rateUser)
        }

        override fun getItemCount(): Int = data.size

        override fun onViewRecycled(holder: InterestingOfferViewHolder) {
            super.onViewRecycled(holder)
            holder.unbind()
        }

        fun updateList(interOfferList: List<Conversation>) {
            val diffs = DiffUtil.calculateDiff(ConversationListCallback(data, interOfferList))
            data = interOfferList
            diffs.dispatchUpdatesTo(this)
        }

        private fun showChat(adapterPos: Int) {
            val currentUser = Firebase.auth.currentUser
            var receiver = ""
            var receiverName = ""
            val actionId =
                if (navController.currentDestination!!.id == R.id.nav_accepted_offers)
                    R.id.action_nav_accepted_offers_to_chatFragment
                else
                    R.id.action_nav_interesting_offers_to_chatFragment


            if(currentUser != null) {
                receiver = if (currentUser.uid == data[adapterPos].receiverUid)
                    data[adapterPos].requestorUid
                else
                    data[adapterPos].receiverUid

                receiverName = if (currentUser.uid == data[adapterPos].receiverUid)
                    data[adapterPos].requestorName
                else
                    data[adapterPos].receiverName
            }

            navController.navigate(
                actionId,
                bundleOf(
                    "receiver" to receiver,
                    "offerId" to data[adapterPos].offerId,
                    "offerTitle" to data[adapterPos].offerTitle,
                    "receiverName" to receiverName
                    )
            )
        }

        fun rateUser(adapterPos: Int) {
            val revieweeId = if (Firebase.auth.currentUser!!.uid == data[adapterPos].requestorUid) data[adapterPos].receiverUid else data[adapterPos].requestorUid
            val reviewType = if (Firebase.auth.currentUser!!.uid == data[adapterPos].requestorUid) "offerer" else "requestor"
            navController.navigate(
                R.id.action_nav_accepted_offers_to_createReviewFragment,
                bundleOf("revieweeId" to revieweeId, "conversationId" to data[adapterPos].id, "reviewType" to reviewType, "offerId" to data[adapterPos].offerId)
            )
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
                        && oldList[oldItemPosition].status == newList[newItemPosition].status
                        && oldList[oldItemPosition].reviewedRequestor == newList[newItemPosition].reviewedRequestor
                        && oldList[oldItemPosition].reviewedOfferer == newList[newItemPosition].reviewedOfferer

            }
        }

    }
}