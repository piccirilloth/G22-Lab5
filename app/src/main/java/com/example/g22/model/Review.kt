package com.example.g22.model

import java.util.Date

data class Review (
    var reviewType : String,
    var reviewer : String,
    var reviewerId : String,
    var reviewee : String,
    var revieweeId : String,
    var rating : String,
    var description : String,
    var timeSlotTitle : String,
    var date : Date
    ) {
    constructor(): this(
        "",
        "",
        "",
        "" ,
        "",
        "",
        "",
        "",
        Date()
    )
}