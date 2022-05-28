package com.example.g22.model

import java.util.Date

data class Review (
    var reviewer : String,
    var reviewee : String
    var rating : String,
    var description : String,
    var date : Date
    ) {
    constructor(): this(
        "",
        "",
        "" ,
        "",
        Date()
    )
}