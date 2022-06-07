package com.example.g22.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review (
    val id: String,
    var reviewType : String,
    var reviewer : String,
    var reviewerId : String,
    var reviewee : String,
    var revieweeId : String,
    var rating : Double,
    var description : String,
    var timeSlotTitle : String,
    @ServerTimestamp
    var date : Date?
    ) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "" ,
        "",
        0.0,
        "",
        "",
        Date()
    )
}