package com.example.g22.reviews

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.g22.R
import com.example.g22.TimeSlotList.AdvertisementAdapter
import com.example.g22.model.Review
import com.example.g22.toAdvertisementList
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReviewAdapter(private var context: Context, private val lifecycleCoroutineScope: LifecycleCoroutineScope, private var data: List<Review>): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    class ReviewViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val reviewerName : TextView = v.findViewById(R.id.reviewer_name_review_item)
        private val reviewDescription : TextView = v.findViewById(R.id.comment_review_item)
        private val ratingBar : RatingBar = v.findViewById(R.id.ratingBar_review_item)
        private val skill : TextView = v.findViewById(R.id.skill_review_item)
        private val reviewerImage : CircleImageView = v.findViewById(R.id.reviewer_image_review_item)
        private val dateReview: TextView = v.findViewById(R.id.date_review_item)
        private val score: TextView = v.findViewById(R.id.avg_score_review_item)
        val storage = Firebase.storage("gs://time-banking-9318d.appspot.com").reference

        fun bind(reviewerId: String, reviewer: String, description: String, rating: Double, timeSlotTitle: String, date: String, context: Context, lifecycleCoroutineScope: LifecycleCoroutineScope) {
            reviewerName.text = reviewer
            reviewDescription.text = description
            ratingBar.rating = rating.toFloat()
            dateReview.text = date
            score.text = String.format("%.2f", rating)
            this.skill.text = timeSlotTitle

            lifecycleCoroutineScope.launch {
                try {
                    val glideRes = withContext(Dispatchers.IO) {
                        val uri = storage.child("$reviewerId.jpg").downloadUrl.await()
                        val imageURL = uri.toString()
                        Glide.with(context)
                            .load(imageURL)
                    }
                    glideRes.into(reviewerImage)
                } catch(e: Exception) {

                }
            }
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
        val d = item.date
        val strDate = "${d.date}/${d.month+1}/${d.year-100}"

        holder.bind(item.reviewerId, item.reviewer, item.description, item.rating, item.timeSlotTitle, strDate, context, lifecycleCoroutineScope)
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: ReviewViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateList(reviewList: List<Review>, lifecycleCoroutineScope: LifecycleCoroutineScope) {
        val adapter = this
        lifecycleCoroutineScope.launch {
            val diffs = withContext(Dispatchers.Default) {
                return@withContext DiffUtil.calculateDiff(ReviewAdapter.ReviewListCallback(data, reviewList))
            }
            data = reviewList
            diffs.dispatchUpdatesTo(adapter)
        }
    }

    class ReviewListCallback(
        private val oldList: List<Review>,
        private val newList: List<Review>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].reviewer == newList[newItemPosition].reviewer &&
                    oldList[oldItemPosition].description == newList[newItemPosition].description &&
                    oldList[oldItemPosition].rating == newList[newItemPosition].rating &&
                    oldList[oldItemPosition].date == newList[newItemPosition].date &&
                    oldList[oldItemPosition].timeSlotTitle == newList[newItemPosition].timeSlotTitle
        }
    }
}