package com.example.g22.reviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.g22.R
import com.example.g22.model.Review

class ReviewAdapter(private var data: List<Review>): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    class ReviewViewHolder(v: View): RecyclerView.ViewHolder(v) {
        //private val cardView: CardView = v.findViewById(R.id.review_item_card)
        private val reviewerName : TextView = v.findViewById(R.id.reviewer_name_review_item)
        private val reviewDescription : TextView = v.findViewById(R.id.comment_review_item)
        private val ratingBar : RatingBar = v.findViewById(R.id.ratingBar_review_item)
        private val skill : TextView = v.findViewById(R.id.skill_review_item)

        fun bind(reviewer: String, description: String, rating: String, skill: String) {
            reviewerName.text = reviewer
            reviewDescription.text = description
            ratingBar.rating = rating.toFloat()
            this.skill.text = skill
        }

        fun unbind() {
        }
    }

    private lateinit var navController: NavController

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val vg = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.review_item, parent, false)
        navController = parent.findNavController()
        return ReviewViewHolder(vg)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = data[position]

        holder.bind(item.reviewer, item.description, item.rating, item.skill)
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: ReviewViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateList(reviewList: List<Review>) {
        data = reviewList
        notifyDataSetChanged()
    }
}