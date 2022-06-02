package com.example.g22.model

import com.example.g22.utils.Duration
import org.json.JSONObject
import java.util.Date

data class TimeSlot (
    var id: String,
    var title: String,
    var description: String,
    var date: Date,
    var duration: Duration,
    var location: String,
    var owner: String,
    var skills: List<String>,
    var accepted: Boolean,
    var proposalsCounter: Int
        ) {

    constructor(title: String, description: String, date: Date, duration: Duration, location:String, owner: String, skills: List<String>, accepted: Boolean, proposalsCounter: Int)
    : this("", title, description, date, duration, location, owner, skills, accepted, proposalsCounter)

    constructor() : this("", "", "", Date(), Duration(0), "", "", emptyList(), false, 0)

    fun hasValidId() = id != ""

    companion object {
        fun Empty() : TimeSlot {
            return TimeSlot("", "", "", Date(), Duration(0), "", "", emptyList(), false, 0)
        }
    }
}
