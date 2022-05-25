package com.example.g22.TimeSlotList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.custom_format
import com.example.g22.model.TimeSlot
import com.example.g22.toAdvertisementList

data class Advertisement(val id: String, val title: String, val datetime: String, val duration: String) {
    companion object {
        fun FromTimeSlot(ts: TimeSlot) : Advertisement {
            return Advertisement(ts.id, ts.title, ts.date.custom_format(), ts.duration.toShortString())
        }
    }
}

class AdvertisementAdapter(private var data: List<Advertisement>, private val skill: String?): RecyclerView.Adapter<AdvertisementAdapter.AdvertisementViewHolder>() {
    class AdvertisementViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val cardView: CardView = v.findViewById(R.id.timeslot_item_card)
        private val titleTV : TextView = v.findViewById(R.id.timeslot_item_title_textview)
        private val datetimeTV : TextView = v.findViewById(R.id.timeslot_item_datetime_textview)
        private val durationTV : TextView = v.findViewById(R.id.timeslot_item_duration_textview)
        private val editButtonImgBtn : ImageButton = v.findViewById(R.id.timeslot_item_edit_button_imgbtn)

        fun bind(item: Advertisement, skill: String?, onCardViewClickCallback: (Int) -> Unit, onEditButtonClickCallback: (Int) -> Unit) {
            titleTV.text = item.title
            datetimeTV.text = item.datetime
            durationTV.text = item.duration
            cardView.setOnClickListener { onCardViewClickCallback(bindingAdapterPosition) }
            if (skill == null ) {
                editButtonImgBtn.setOnClickListener { onEditButtonClickCallback(bindingAdapterPosition) }
                editButtonImgBtn.visibility = View.VISIBLE
            }
            else {
                editButtonImgBtn.visibility = View.INVISIBLE
            }
        }

        fun unbind() {
            cardView.setOnClickListener(null)
            editButtonImgBtn.setOnClickListener(null)
        }
    }

    private lateinit var navController: NavController

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertisementViewHolder {
        val vg = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.time_slot_item, parent, false)
        navController = parent.findNavController()
        return AdvertisementViewHolder(vg)
    }

    override fun onBindViewHolder(holder: AdvertisementViewHolder, position: Int) {
        val item = data[position]

        holder.bind(item, skill, ::showTimeSlotDetails, ::editTimeSlot)
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: AdvertisementViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateList(tsList: List<TimeSlot>) {
        data = tsList.toAdvertisementList()

        // TODO: provide a way to handle list modifications better
        notifyDataSetChanged()
    }

    /**
     * Utilities
     */
    private fun showTimeSlotDetails(adapterPos: Int) {
        if (skill == null) {
            navController.navigate(
                R.id.action_nav_timeslot_list_my_offers_to_nav_timeslot_show_my_offers,
                bundleOf("timeSlotId" to data[adapterPos].id, "readOnly" to false)
            )
        }
        else {
            navController.navigate(
                R.id.action_nav_timeslot_list_by_skills_to_nav_timeslot_show,
                bundleOf("timeSlotId" to data[adapterPos].id, "readOnly" to true)
            )
        }
    }

    private fun editTimeSlot(adapterPos: Int) {
        navController.navigate(
            R.id.action_timeSlotListFragment_to_timeSlotEditFragment2,
            bundleOf("timeSlotId" to data[adapterPos].id)
        )
    }
}