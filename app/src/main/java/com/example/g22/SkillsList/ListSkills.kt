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
import com.example.g22.SkillsList.SkillsListFragmentDirections


class SkillAdapter(private var data: List<String>): RecyclerView.Adapter<SkillAdapter.SkillViewHolder>() {
    class SkillViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val cardView: CardView = v.findViewById(R.id.skill_item_card)
        private val titleTV : TextView = v.findViewById(R.id.skillName)

        fun bind(item: String, onCardViewClickCallback: (Int) -> Unit) {
            titleTV.text = item
            cardView.setOnClickListener { onCardViewClickCallback(bindingAdapterPosition) }
        }

        fun unbind() {
            cardView.setOnClickListener(null)
        }
    }

    private lateinit var navController: NavController

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val vg = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.skill_item, parent, false)
        navController = parent.findNavController()
        return SkillViewHolder(vg)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val item = data[position]

        holder.bind(item, ::showOffersBySkill)
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: SkillViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateList(skillsList: List<String>) {
        data = skillsList
        // TODO: provide a way to handle list modifications better
        notifyDataSetChanged()
    }

    /**
     * Utilities
     */
    private fun showOffersBySkill(adapterPos: Int) {
        navController.navigate(
            SkillsListFragmentDirections
                .actionNavSkillsListToNavTimeslotListBySkills(skill = data[adapterPos]))
    }

}